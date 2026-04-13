package servlet;

import com.google.gson.Gson;
import dao.ServicioDAO;
import modelo.Servicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;

@WebServlet("/servicios")
public class ServicioServlet extends HttpServlet {

    private final ServicioDAO dao = new ServicioDAO();
    private final Gson gson = new Gson();

    
    static class ServicioRequest {

        Servicio servicio;
        int idPaquete;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        String idPaqueteStr = req.getParameter("idPaquete");

        try {
            if (idPaqueteStr != null) {
                
                int idPaquete = Integer.parseInt(idPaqueteStr);
                Servicio[] lista = dao.listarPorPaquete(idPaquete);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(lista != null ? lista : new Servicio[0]));
            } else {
               
                Servicio[] lista = dao.listarTodo();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(lista != null ? lista : new Servicio[0]));
            }
        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

   
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try (BufferedReader reader = req.getReader()) {
            ServicioRequest data = gson.fromJson(reader, ServicioRequest.class);

            if (data == null || data.servicio == null || data.idPaquete <= 0) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Datos de servicio o paquete inválidos");
                return;
            }

           
            dao.agregarAServicio(data.servicio, data.idPaquete);

            enviarRespuesta(resp, HttpServletResponse.SC_CREATED, "Servicio agregado al paquete correctamente");

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al guardar servicio: " + e.getMessage());
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
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private void enviarRespuesta(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(new MsgResponse(mensaje)));
    }

    private static class MsgResponse {

        String mensaje;

        MsgResponse(String m) {
            this.mensaje = m;
        }
    }
}
