package servlet;

import com.google.gson.Gson;
import dao.UsuarioDAO;
import modelo.Usuario;
import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/login")
public class UsuarioServlet extends HttpServlet {

    private final UsuarioDAO dao = new UsuarioDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);

        try (BufferedReader reader = req.getReader()) {
            Usuario input = gson.fromJson(reader, Usuario.class);

       
            if (input == null || input.getNombreUsuario() == null || input.getPassword() == null) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Usuario y contraseña son requeridos");
                return;
            }

           
            Usuario user = dao.login(input.getNombreUsuario(), input.getPassword());

            if (user != null) {
              
                user.setPassword(null); 
                
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(user));
            } else {
                enviarRespuesta(resp, HttpServletResponse.SC_UNAUTHORIZED, "Credenciales incorrectas. Verifique su usuario y contraseña.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error en el servidor: " + e.getMessage());
        }
    }

  
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
            Usuario[] lista = dao.listar(); 
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(lista != null ? lista : new Usuario[0]));
        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al obtener usuarios: " + e.getMessage());
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
        resp.getWriter().write(gson.toJson(new MsgResponse(mensaje)));
    }

    private static class MsgResponse {
        String mensaje;
        MsgResponse(String m) { this.mensaje = m; }
    }
}