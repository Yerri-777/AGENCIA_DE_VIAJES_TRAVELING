package servlet;

import com.google.gson.Gson;
import dao.PaqueteDAO;
import modelo.PaqueteTuristico;
import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/paquetes")
public class PaqueteServlet extends HttpServlet {

    private final PaqueteDAO dao = new PaqueteDAO();
    private final Gson gson = new Gson();

    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
            PaqueteTuristico[] lista = dao.listar();

            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(lista != null ? lista : new PaqueteTuristico[0]));

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al listar: " + e.getMessage());
        }
    }

    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try (BufferedReader reader = req.getReader()) {
            PaqueteTuristico nuevo = gson.fromJson(reader, PaqueteTuristico.class);

            if (nuevo == null || nuevo.getNombre() == null) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Datos del paquete inválidos o incompletos");
                return;
            }

            dao.crear(nuevo);
            enviarRespuesta(resp, HttpServletResponse.SC_CREATED, "Paquete creado exitosamente");

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear: " + e.getMessage());
        }
    }

    
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void configurarHeaders(HttpServletResponse resp) {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void enviarRespuesta(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(new MensajeResponse(mensaje)));
    }

    private static class MensajeResponse {

        String mensaje;

        MensajeResponse(String mensaje) {
            this.mensaje = mensaje;
        }
    }
}
