package servlet;

import com.google.gson.Gson;
import config.ConexionBD;
import dao.PaqueteDAO;
import modelo.Paquete;
import java.io.*;
import java.util.stream.Collectors;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.sql.Connection;

@WebServlet("/paquetes")
public class PaqueteServlet extends HttpServlet {

    private final PaqueteDAO dao = new PaqueteDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
            Paquete[] lista = dao.listar();
            resp.getWriter().write(gson.toJson(lista != null ? lista : new Paquete[0]));
        } catch (Exception e) {
            enviarRespuesta(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);

        // 1. SEGURIDAD PRIMERO
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            enviarRespuesta(resp, 401, "Sesión expirada o no iniciada");
            return;
        }

        try (Connection conn = ConexionBD.getConnection()) {
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Paquete p = gson.fromJson(body, Paquete.class);

            if (p == null || p.getDestino() == null || p.getNombre() == null) {
                enviarRespuesta(resp, 400, "Datos incompletos para crear el paquete");
                return;
            }

            dao.crear(conn, p);
            enviarRespuesta(resp, 201, "Paquete '" + p.getNombre() + "' creado exitosamente");

        } catch (Exception e) {
            enviarRespuesta(resp, 500, "Error en el servidor: " + e.getMessage());
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
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }

    private void enviarRespuesta(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(new MensajeResponse(mensaje)));
    }

    private static class MensajeResponse {

        String mensaje;

        MensajeResponse(String m) {
            this.mensaje = m;
        }
    }
}
