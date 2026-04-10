package servlet;

import dao.*;
import modelo.*;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import config.ConexionBD;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/carga")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 1, maxFileSize = 1024 * 1024 * 10, maxRequestSize = 1024 * 1024 * 15)
public class CargaDatosServlet extends HttpServlet {

    private final UsuarioDAO usuarioDao = new UsuarioDAO();
    private final ClienteDAO clienteDao = new ClienteDAO();
    private final DestinoDAO destinoDao = new DestinoDAO();
    private final PaqueteDAO paqueteDao = new PaqueteDAO();
    private final ProveedorDAO proveedorDao = new ProveedorDAO();
    private final ServicioDAO servicioDao = new ServicioDAO();
    private final PagoDAO pagoDao = new PagoDAO();
    private final ReservacionDAO reservacionDao = new ReservacionDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        configurarHeaders(resp);

        int lineasProcesadas = 0;
        int contadorErrores = 0;
        List<String> detallesErrores = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);

            Part filePart = req.getPart("archivo");
            if (filePart == null)
                throw new Exception("Archivo no encontrado.");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(filePart.getInputStream(), "UTF-8"))) {
                String linea;
                int numeroLinea = 0;
                while ((linea = reader.readLine()) != null) {
                    numeroLinea++;
                    linea = linea.trim();

                    // 1. Ignorar líneas vacías, comentarios o basura
                    if (linea.isEmpty() || linea.startsWith("//") || !linea.contains("("))
                        continue;

                    try {
                        procesarLinea(linea, conn);
                        lineasProcesadas++;
                    } catch (Exception e) {
                        detallesErrores.add("Línea " + numeroLinea + ": " + e.getMessage());
                        contadorErrores++;
                    }
                }
            }

            if (contadorErrores == 0) {
                conn.commit();
                enviarRespuesta(resp, 200, lineasProcesadas, 0, "¡Carga completada con éxito!", null);
            } else {
                conn.rollback();
                enviarRespuesta(resp, 400, 0, contadorErrores, "Carga abortada por errores",
                        detallesErrores.toArray(new String[0]));
            }
        } catch (Exception e) {
            enviarRespuesta(resp, 500, 0, 1, "Error crítico: " + e.getMessage(), null);
        }
    }

    private void procesarLinea(String linea, Connection conn) throws Exception {
        int indexOpen = linea.indexOf("(");
        int indexClose = linea.lastIndexOf(")");

        if (indexOpen == -1 || indexClose == -1)
            throw new Exception("Formato inválido (faltan paréntesis)");

        String comando = linea.substring(0, indexOpen).trim().toUpperCase();
        String contenido = linea.substring(indexOpen + 1, indexClose).trim();

        // Regex para comas fuera de comillas
        String[] params = contenido.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].trim().replace("\"", "");
        }

        switch (comando) {
            case "USUARIO":
                if (params.length < 3)
                    throw new Exception("Faltan datos");
                Usuario u = new Usuario();
                u.setNombreUsuario(params[0]);
                u.setPassword(params[1]);
                int idRol = params[2].matches("\\d+") ? Integer.parseInt(params[2])
                        : (params[2].equalsIgnoreCase("ADMINISTRADOR") ? 3
                                : (params[2].equalsIgnoreCase("AGENTE") ? 2 : 1));
                u.setRol(new Rol(idRol, params[2]));
                usuarioDao.crear(conn, u);
                break;

            case "DESTINO":
                if (params.length < 3)
                    throw new Exception("Faltan datos (mínimo: Nombre, País, Descripción)");
                Destino d = new Destino();
                d.setNombre(params[0]);
                d.setPais(params[1]);
                d.setDescripcion(params[2]);

                // Valores por defecto para evitar errores en la BD si no vienen en el archivo
                d.setClima(params.length > 3 ? params[3] : "Desconocido");
                d.setPrecioBase(params.length > 4 ? Double.parseDouble(params[4]) : 0.0);
                d.setImagenUrl(params.length > 5 ? params[5] : "");

                destinoDao.crear(conn, d);
                break;

            case "PROVEEDOR":
                if (params.length < 3) throw new Exception("Faltan datos para proveedor (Nombre, Tipo, País)");
                modelo.Proveedor prov = new modelo.Proveedor();
                prov.setNombre(params[0]);
                prov.setTipoServicio(Integer.parseInt(params[1]));
                prov.setPais(params[2]);
                prov.setContacto(params.length > 3 ? params[3] : "");
                proveedorDao.crear(conn, prov);
                break;

            case "PAQUETE":
                // PAQUETE("Caribe Mágico 7 noches", "Cancun", 7, 18500.00, 20)
                if (params.length < 5) throw new Exception("Faltan datos para paquete (Nombre, Destino, Duracion, Precio, Capacidad)");
                String nombrePaquete = params[0];
                String nombreDestino = params[1];
                int duracion = Integer.parseInt(params[2]);
                double precio = Double.parseDouble(params[3]);
                int capacidad = Integer.parseInt(params[4]);

                int idDestino = destinoDao.obtenerIdPorNombre(conn, nombreDestino);
                if (idDestino == -1) throw new Exception("Destino '" + nombreDestino + "' no encontrado para paquete '" + nombrePaquete + "'.");

                Paquete p = new Paquete();
                p.setNombre(nombrePaquete);
                p.setDuracionDias(duracion);
                p.setPrecioVenta(precio);
                p.setCapacidadMaxima(capacidad);
                p.setDestino(new Destino(idDestino));
                paqueteDao.crear(conn, p);
                break;

            case "SERVICIO_PAQUETE":
                // SERVICIO_PAQUETE("Caribe Magico 7 noches", "TACA Airlines", "Vuelo...", 5200.00)
                if (params.length < 4) throw new Exception("Faltan datos para servicio_paquete (Paquete, Proveedor, Descripción, Costo)");
                String paqueteName = params[0];
                String proveedorName = params[1];
                String descripcion = params[2];
                double costoProv = Double.parseDouble(params[3]);

                int idPaquete = paqueteDao.obtenerIdPorNombre(conn, paqueteName);
                if (idPaquete == -1) throw new Exception("Paquete '" + paqueteName + "' no encontrado.");

                int idProveedor = proveedorDao.obtenerIdPorNombre(conn, proveedorName);
                if (idProveedor == -1) throw new Exception("Proveedor '" + proveedorName + "' no encontrado.");

                modelo.Servicio s = new modelo.Servicio();
                s.setNombre(descripcion);
                s.setDescripcion(descripcion);
                s.setCostoProveedor(costoProv);
                modelo.Proveedor provObj = new modelo.Proveedor();
                provObj.setIdProveedor(idProveedor);
                s.setProveedor(provObj);

                servicioDao.agregarAServicio(conn, s, idPaquete);
                break;

            case "CLIENTE":
                if (params.length < 6) throw new Exception("Faltan datos para cliente");
                String fechaNac = convertirFecha(params[2]);
                clienteDao.crear(conn, new Cliente(params[0], params[1], fechaNac, params[3], params[4], params[5]));
                break;

            

            case "RESERVACION":
                // RESERVACION("Caribe Magico 7 noches", jperez, "10/07/2025", "123|987")
                if (params.length < 4) throw new Exception("Faltan datos para reservación (Paquete, Usuario, FechaViaje, Pasajeros)");
                String paqueteParaReserva = params[0];
                String usuarioAgente = params[1];
                String fechaViaje = convertirFecha(params[2]);
                String dpis = params[3];

                int idPaq = paqueteDao.obtenerIdPorNombre(conn, paqueteParaReserva);
                if (idPaq == -1) throw new Exception("Paquete '" + paqueteParaReserva + "' no encontrado para reservación.");

                reservacionDao.crearDesdeCarga(conn, idPaq, usuarioAgente, fechaViaje, dpis);
                break;

            case "PAGO": // PAGO("RES-00001", 18500.00, 1, "05/06/2025")
                if (params.length < 4) throw new Exception("Faltan datos para pago (Reservacion, Monto, Metodo, Fecha)");
                String numRes = params[0];
                double monto = Double.parseDouble(params[1]);
                int metodo = Integer.parseInt(params[2]);
                String fechaPago = convertirFecha(params[3]);
                pagoDao.crearCarga(conn, numRes, monto, metodo, fechaPago);
                break;

            default:
                throw new Exception("Comando '" + comando + "' no reconocido.");
        }
    }

    // --- MÉTODOS DE APOYO (Sin cambios pero integrados) ---

    private void configurarHeaders(HttpServletResponse resp) {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }

    private void enviarRespuesta(HttpServletResponse resp, int status, int p, int e, String m, String[] d)
            throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(new RespuestaCarga(p, e, m, d)));
    }

    private String convertirFecha(String fechaOriginal) {
        if (fechaOriginal == null)
            return null;
        fechaOriginal = fechaOriginal.trim();
        if (fechaOriginal.contains("/")) {
            String[] partes = fechaOriginal.split("/");
            if (partes.length == 3)
                return partes[2] + "-" + partes[1] + "-" + partes[0];
        }
        return fechaOriginal;
    }

    static class RespuestaCarga {
        int procesadas;
        int errores;
        String mensaje;
        String[] detalles;

        RespuestaCarga(int p, int e, String m, String[] d) {
            this.procesadas = p;
            this.errores = e;
            this.mensaje = m;
            this.detalles = d;
        }
    }
}