package servlet;

import dao.*;
import modelo.*;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;

@WebServlet("/carga")
public class CargaDatosServlet extends HttpServlet {

    private final UsuarioDAO usuarioDao = new UsuarioDAO();
    private final ClienteDAO clienteDao = new ClienteDAO();
    private final DestinoDAO destinoDao = new DestinoDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        configurarHeaders(resp);
        int lineasProcesadas = 0;
        int errores = 0;

        try (BufferedReader reader = req.getReader()) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || !linea.contains("(") || !linea.contains(")")) {
                    continue;
                }

                try {
                    procesarLinea(linea);
                    lineasProcesadas++;
                } catch (Exception e) {
                    System.err.println("Error procesando: " + linea + " -> " + e.getMessage());
                    errores++;
                }
            }

            resp.setStatus(200);
            resp.getWriter().write(gson.toJson(new RespuestaCarga(lineasProcesadas, errores)));

        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write(gson.toJson(new RespuestaCarga(0, 1, e.getMessage())));
        }
    }

    private void procesarLinea(String linea) throws Exception {
        String comando = linea.substring(0, linea.indexOf("(")).trim().toUpperCase();
        String contenido = linea.substring(linea.indexOf("(") + 1, linea.lastIndexOf(")")).replace("\"", "");
        String[] params = contenido.split(",");

        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].trim();
        }

        switch (comando) {
            case "USUARIO":
                Usuario u = new Usuario();
                u.setNombreUsuario(params[0]);
                u.setPassword(params[1]);
                // Creamos un objeto Rol temporal para el DAO
                int idRol = params[2].equalsIgnoreCase("ADMINISTRADOR") ? 1 : 2;
                u.setRol(new Rol(idRol, params[2]));
                usuarioDao.crear(u);
                break;

            case "CLIENTE":
                Cliente c = new Cliente();
                c.setDpi(params[0]);
                c.setNombreCompleto(params[1]); 
                c.setFechaNacimiento(params[2]);
                c.setTelefono(params[3]);
                c.setEmail(params[4]);
                c.setNacionalidad(params[5]);
                clienteDao.crear(c);
                break;

            case "DESTINO":
                Destino d = new Destino();
                d.setNombre(params[0]);
                d.setPais(params[1]);      
                d.setDescripcion(params[2]);
                d.setClima(params[3]);
                d.setPrecioBase((int) Double.parseDouble(params[4])); 
                d.setImagenUrl(params[5]);
                destinoDao.crear(d);
                break;
        }
    }
    private void configurarHeaders(HttpServletResponse resp) {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    static class RespuestaCarga {

        int procesadas;
        int errores;
        String mensaje;

        RespuestaCarga(int p, int e) {
            this.procesadas = p;
            this.errores = e;
            this.mensaje = "Carga finalizada";
        }

        RespuestaCarga(int p, int e, String m) {
            this.procesadas = p;
            this.errores = e;
            this.mensaje = m;
        }
    }
}
