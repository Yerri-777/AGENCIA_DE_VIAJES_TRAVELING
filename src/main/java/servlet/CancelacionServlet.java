package servlet;

import com.google.gson.Gson;
import dao.CancelacionDAO;
import dao.ReservacionDAO;

import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/cancelaciones")
public class CancelacionServlet extends HttpServlet {

    private final CancelacionDAO dao = new CancelacionDAO();
    private final Gson gson = new Gson();
    private final ReservacionDAO reservacionDao = new ReservacionDAO();

   
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
            
            Object[] lista = dao.listar(); 
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(lista));
        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al obtener historial: " + e.getMessage());
        }
    }

  
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try (BufferedReader reader = req.getReader()) {
            CancelacionRequest c = gson.fromJson(reader, CancelacionRequest.class);

            if (c == null || c.reservacion == null) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Datos de cancelación incompletos");
                return;
            }

            try {
                double[] result = reservacionDao.cancelarConPenalizacion(c.reservacion);
                double reembolso = result[0];
                double perdida = result[1];
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write(gson.toJson(new CancelacionResponse(c.reservacion, reembolso, perdida, "Cancelación procesada exitosamente")));
            } catch (Exception ex) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al cancelar: " + e.getMessage());
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
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }

    private void enviarRespuesta(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(new ResponseMsg(mensaje)));
    }

   
    static class CancelacionRequest {
        String reservacion;
        double reembolso;
        double perdida;
    }

    static class CancelacionResponse {
        String reservacion;
        double reembolso;
        double perdida;
        String mensaje;

        CancelacionResponse(String r, double re, double pe, String m) {
            this.reservacion = r;
            this.reembolso = re;
            this.perdida = pe;
            this.mensaje = m;
        }
    }

    private static class ResponseMsg {
        @SuppressWarnings("unused")
        String mensaje;
        ResponseMsg(String m) { this.mensaje = m; }
    }
}