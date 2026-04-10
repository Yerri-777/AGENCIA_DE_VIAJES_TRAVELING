package servlet;

import com.google.gson.Gson;
import config.ConexionBD;
import dao.ClienteDAO;
import modelo.Cliente;
import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.sql.Connection;

@WebServlet("/clientes")
public class ClienteServlet extends HttpServlet {

    private final ClienteDAO dao = new ClienteDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
            String dpi = req.getParameter("dpi");

            if (dpi != null && !dpi.trim().isEmpty()) {
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
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

  @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try (BufferedReader reader = req.getReader(); 
             Connection conn = ConexionBD.getConnection()) { // Obtenemos conexión
            
            Cliente c = gson.fromJson(reader, Cliente.class);
            if (c == null || c.getDpi() == null) {
                enviarRespuesta(resp, 400, "Datos incompletos");
                return;
            }

            dao.crear(conn, c); 
            enviarRespuesta(resp, 201, "Cliente registrado");
        } catch (Exception e) {
            enviarRespuesta(resp, 500, e.getMessage());
        }
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            enviarRespuesta(resp, 401, "Sesión expirada o no iniciada");
            return;
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        String dpi = req.getParameter("dpi");
        try {
            if (dpi == null) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "DPI requerido");
                return;
            }
            dao.eliminar(dpi);
            enviarRespuesta(resp, HttpServletResponse.SC_OK, "Eliminado correctamente");
        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try (BufferedReader reader = req.getReader()) {
            Cliente c = gson.fromJson(reader, Cliente.class);
            if (c == null || c.getDpi() == null) {
                enviarRespuesta(resp, 400, "DPI y datos necesarios");
                return;
            }
            dao.actualizar(c);
            enviarRespuesta(resp, 200, "Cliente actualizado correctamente");
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
        // Debe ser igual al de tu UsuarioServlet para mantener la sesión
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS, PUT");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
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
