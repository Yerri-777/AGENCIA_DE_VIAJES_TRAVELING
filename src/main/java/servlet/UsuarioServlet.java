package servlet;

import com.google.gson.Gson;
import config.ConexionBD;
import dao.UsuarioDAO;
import modelo.Usuario;
import modelo.Rol;
import java.io.*;
import java.util.stream.Collectors;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.sql.Connection;

@WebServlet("/login")
public class UsuarioServlet extends HttpServlet {

    private final UsuarioDAO dao = new UsuarioDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);    

        // Leer el body JSON
        String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Usuario input = gson.fromJson(body, Usuario.class);

        String action = req.getParameter("action");

        try {
            if ("register".equals(action)) {
                registrarUsuario(input, resp);
            } else {
                procesarLogin(input, req, resp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    private void procesarLogin(Usuario input, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (input == null || input.getNombreUsuario() == null || input.getPassword() == null) {
            enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Credenciales incompletas");
            return;
        }

        Usuario user = dao.login(input.getNombreUsuario(), input.getPassword());

        if (user != null) {
            HttpSession session = req.getSession(true);
            session.setAttribute("usuarioLogueado", user);
            session.setMaxInactiveInterval(30 * 60);

            
            java.util.Map<String, Object> respuesta = new java.util.HashMap<>();
            respuesta.put("nombre", user.getNombreUsuario());
            respuesta.put("rol", user.getRol() != null ? user.getRol().getIdRol() : null);
            respuesta.put("idUsuario", user.getIdUsuario());

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(respuesta));
        } else {
            enviarRespuesta(resp, HttpServletResponse.SC_UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }
    }

    private void registrarUsuario(Usuario input, HttpServletResponse resp) throws IOException {
        try (Connection conn = ConexionBD.getConnection()) { // Conexión para el registro
            if (input.getRol() == null) {
                input.setRol(new Rol(1, "ATENCION AL CLIENTE"));
            }
            input.setEstado(1);
            dao.crear(conn, input); // Pasamos conn
            enviarRespuesta(resp, HttpServletResponse.SC_CREATED, "Usuario registrado exitosamente");
        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);

        HttpSession session = req.getSession(false);
        Usuario logueado = (session != null) ? (Usuario) session.getAttribute("usuarioLogueado") : null;

        if (logueado == null) {
            enviarRespuesta(resp, HttpServletResponse.SC_UNAUTHORIZED, "Debe iniciar sesión");
            return;
        }

        
        if (logueado.getRol() == null || logueado.getRol().getIdRol() != 3) {
            enviarRespuesta(resp, HttpServletResponse.SC_FORBIDDEN, "Acceso denegado: Se requiere rol de Administrador");
            return;
        }

        try {
            Usuario[] lista = dao.listar();
            Usuario[] segura = (lista != null) ? lista : new Usuario[0];
            
            for (Usuario u : segura) { u.setPassword(null); }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(segura));
        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al listar");
        }
    } 
    
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        // Verificar si es administrador antes de permitir editar
        HttpSession session = req.getSession(false);
        Usuario logueado = (session != null) ? (Usuario) session.getAttribute("usuarioLogueado") : null;

        if (logueado == null || logueado.getRol().getIdRol() != 3) {
            enviarRespuesta(resp, HttpServletResponse.SC_FORBIDDEN, "No tiene permisos para editar usuarios");
            return;
        }

        try (BufferedReader reader = req.getReader()) {
            Usuario u = gson.fromJson(reader, Usuario.class);
            dao.actualizar(u);
            enviarRespuesta(resp, HttpServletResponse.SC_OK, "Usuario actualizado correctamente");
        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
            enviarRespuesta(resp, HttpServletResponse.SC_OK, "Sesión cerrada");
        } else {
            enviarRespuesta(resp, HttpServletResponse.SC_OK, "No había sesión activa");
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
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true"); 
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