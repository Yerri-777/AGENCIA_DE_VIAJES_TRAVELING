package servlet;

import com.google.gson.Gson;
import config.ConexionBD;
import dao.ReservacionDAO;
import modelo.Reservacion;
import modelo.Usuario;
import java.io.*;
import java.util.stream.Collectors;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/reservaciones")
public class ReservacionServlet extends HttpServlet {


    private final ReservacionDAO dao = new ReservacionDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
            String numero = req.getParameter("numero");
            if (numero != null && !numero.trim().isEmpty()) {
                Reservacion r = dao.buscarPorNumero(numero);
                if (r != null) {
                    resp.getWriter().write(gson.toJson(r));
                } else {
                    enviarRespuesta(resp, 404, "Reservación no encontrada");
                }
            } else {
                Reservacion[] lista = dao.listar();
                resp.getWriter().write(gson.toJson(lista != null ? lista : new Reservacion[0]));
            }
        } catch (Exception e) {
            enviarRespuesta(resp, 500, "Error al obtener datos: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);

        // Obtenemos al usuario de la sesión para asegurar que el Agente sea el que está logueado
        HttpSession session = req.getSession(false);
        Usuario logueado = (session != null) ? (Usuario) session.getAttribute("usuarioLogueado") : null;

        if (logueado == null) {
            enviarRespuesta(resp, 401, "Debe iniciar sesión para crear reservaciones");
            return;
        }

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false); // Iniciamos Transacción

            try {
                String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                Reservacion r = gson.fromJson(body, Reservacion.class);

                if (r == null || r.getPaquete() == null) {
                    enviarRespuesta(resp, 400, "Datos del paquete o pasajeros incompletos");
                    return;
                }

                // Forzamos que el agente de la reservación sea el usuario en sesión
                r.setAgente(logueado);

                // El DAO ahora inserta Cabecera y Pasajeros
                String numeroGenerado = dao.crear(conn, r);

                conn.commit(); // Si todo sale bien, guardamos

                resp.setStatus(201);
                resp.getWriter().write("{\"numero\":\"" + numeroGenerado + "\", \"mensaje\":\"Reservación registrada y vinculada a pasajeros\"}");
            } catch (Exception e) {
                conn.rollback(); // Si falla (ej. sobreventa), deshacemos todo
                enviarRespuesta(resp, 500, "Error en registro: " + e.getMessage());
            }
        } catch (SQLException e) {
            enviarRespuesta(resp, 500, "Error de base de datos: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        // Este método sirve para cambiar el estado a CONFIRMADA o CANCELADA
        String numero = req.getParameter("numero");
        String nuevoEstado = req.getParameter("estado");

        try {
            if (numero == null || nuevoEstado == null) {
                enviarRespuesta(resp, 400, "Faltan parámetros: numero o estado");
                return;
            }
            dao.actualizarEstado(numero, nuevoEstado);
            enviarRespuesta(resp, 200, "Estado actualizado a " + nuevoEstado);
        } catch (Exception e) {
            enviarRespuesta(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        configurarHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void configurarHeaders(HttpServletResponse resp) {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }

    private void enviarRespuesta(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(new ResponseMsg(mensaje)));
    }

    private static class ResponseMsg {
        @SuppressWarnings("unused")
        String mensaje;

        ResponseMsg(String m) {
            this.mensaje = m;
        }
    }
}
