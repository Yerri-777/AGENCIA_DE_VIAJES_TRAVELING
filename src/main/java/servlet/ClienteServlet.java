package servlet;

import com.google.gson.Gson;
import dao.ClienteDAO;
import modelo.Cliente;
import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/clientes")
public class ClienteServlet extends HttpServlet {

    private final ClienteDAO dao = new ClienteDAO();
    private final Gson gson = new Gson();

    //  OBTENER CLIENTES Listar buscar por DPI
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
            String dpi = req.getParameter("dpi");

            if (dpi != null && !dpi.trim().isEmpty()) {
                // Buscar por DPI específico
                Cliente c = dao.buscarPorDpi(dpi);
                if (c != null) {
                    resp.getWriter().write(gson.toJson(c));
                } else {
                    enviarRespuesta(resp, HttpServletResponse.SC_NOT_FOUND, "Cliente no encontrado");
                }
            } else {
                
                Cliente[] lista = dao.listar();
                resp.getWriter().write(gson.toJson(lista != null ? lista : new Cliente[0]));
            }
        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

  
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try (BufferedReader reader = req.getReader()) {
            Cliente c = gson.fromJson(reader, Cliente.class);

            if (c == null || c.getDpi() == null) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Datos de cliente inválidos o incompletos");
                return;
            }

            dao.crear(c); 
            enviarRespuesta(resp, HttpServletResponse.SC_CREATED, "Cliente registrado exitosamente");

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al guardar: " + e.getMessage());
        }
    }

    // 3. ELIMINAR CLIENTE (DELETE)
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        String dpi = req.getParameter("dpi");

        try {
            if (dpi == null || dpi.trim().isEmpty()) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "DPI requerido para eliminar");
                return;
            }

            dao.eliminar(dpi); 
            enviarRespuesta(resp, HttpServletResponse.SC_OK, "Cliente con DPI " + dpi + " eliminado correctamente");

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No se pudo eliminar: " + e.getMessage());
        }
    }

    // SOPORTE CORS 
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        configurarHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void configurarHeaders(HttpServletResponse resp) {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS, PUT");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void enviarRespuesta(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(new ResponseMsg(mensaje)));
    }

    private static class ResponseMsg {

        String mensaje;

        ResponseMsg(String m) {
            this.mensaje = m;
        }
    }
}
