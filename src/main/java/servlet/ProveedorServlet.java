package servlet;

import com.google.gson.Gson;
import dao.ProveedorDAO;
import modelo.Proveedor;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;

@WebServlet("/proveedores")
public class ProveedorServlet extends HttpServlet {

    private final ProveedorDAO dao = new ProveedorDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
           
            Proveedor[] lista = dao.listar();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(lista != null ? lista : new Proveedor[0]));
        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al listar: " + e.getMessage());
        }
    }

    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try (BufferedReader reader = req.getReader()) {
            Proveedor p = gson.fromJson(reader, Proveedor.class);

            if (p == null || p.getNombre() == null) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Datos de proveedor incompletos");
                return;
            }

            dao.crear(p);
            enviarRespuesta(resp, HttpServletResponse.SC_CREATED, "Proveedor registrado exitosamente");

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear: " + e.getMessage());
        }
    }

    
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        String idStr = req.getParameter("id");
        try {
            if (idStr == null) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "ID requerido");
                return;
            }
            dao.eliminar(Integer.parseInt(idStr));
            enviarRespuesta(resp, HttpServletResponse.SC_OK, "Proveedor eliminado correctamente");
        } catch (Exception e) {
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
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
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
