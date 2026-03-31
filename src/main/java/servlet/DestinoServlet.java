package servlet;

import com.google.gson.Gson;
import dao.DestinoDAO;
import modelo.Destino;
import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/destinos")
public class DestinoServlet extends HttpServlet {

    private final DestinoDAO dao = new DestinoDAO();
    private final Gson gson = new Gson();

    // OBTENER DESTINOS 
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
           
            Destino[] lista = dao.listar();

         
            if (lista == null) {
                lista = new Destino[0];
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(lista));

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al listar destinos: " + e.getMessage());
        }
    }

    // CREAR (POST)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try (BufferedReader reader = req.getReader()) {
            Destino d = gson.fromJson(reader, Destino.class);

            if (d == null || d.getNombre() == null) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Datos de destino inválidos o incompletos");
                return;
            }

            dao.crear(d);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            enviarRespuesta(resp, HttpServletResponse.SC_CREATED, "Destino creado correctamente");

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear destino: " + e.getMessage());
        }
    }

    // ELIMINAR (DELETE)
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        String idStr = req.getParameter("id");

        try {
            if (idStr == null || idStr.trim().isEmpty()) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "ID de destino requerido");
                return;
            }

            int id = Integer.parseInt(idStr);
            dao.eliminar(id);
            enviarRespuesta(resp, HttpServletResponse.SC_OK, "Destino eliminado correctamente");

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No se pudo eliminar: " + e.getMessage());
        }
    }

    // 4. SOPORTE CORS (OPTIONS)
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
