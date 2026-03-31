package servlet;

import com.google.gson.Gson;
import dao.ReservacionDAO;
import modelo.Reservacion;
import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
                    enviarRespuesta(resp, HttpServletResponse.SC_NOT_FOUND, "Reservación no encontrada");
                }
            } else {
             
                Reservacion[] lista = dao.listar();
                resp.getWriter().write(gson.toJson(lista != null ? lista : new Reservacion[0]));
            }
        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al obtener datos: " + e.getMessage());
        }
    }

   
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try (BufferedReader reader = req.getReader()) {
            Reservacion r = gson.fromJson(reader, Reservacion.class);

            if (r == null || r.getPaquete() == null) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Datos de reservación incompletos");
                return;
            }

           
            String numeroGenerado = dao.crear(r);

            if (numeroGenerado != null) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"numero\":\"" + numeroGenerado + "\", \"mensaje\":\"Reservación creada exitosamente\"}");
            } else {
                enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No se pudo generar la reservación");
            }

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
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
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void enviarRespuesta(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(new ResponseMsg(mensaje)));
    }

    private static class ResponseMsg {
        String mensaje;
        ResponseMsg(String m) { this.mensaje = m; }
    }
}