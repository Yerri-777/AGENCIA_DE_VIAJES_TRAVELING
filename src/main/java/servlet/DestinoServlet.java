package servlet;

import com.google.gson.Gson;
import dao.DestinoDAO;
import modelo.Destino;
import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/destinos")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 1,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 15
)
public class DestinoServlet extends HttpServlet {

    private final DestinoDAO dao = new DestinoDAO();
    private final Gson gson = new Gson();

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
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al listar: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);

        try (Connection conn = config.ConexionBD.getConnection()) {

            String nombre = req.getParameter("nombre");
            String pais = req.getParameter("pais");
            String descripcion = req.getParameter("descripcion");
            String clima = req.getParameter("clima");
            String precioStr = req.getParameter("precioBase");

            if (nombre == null || precioStr == null || precioStr.trim().isEmpty()) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Faltan datos obligatorios");
                return;
            }

            String fileName = "default.jpg";
            try {
                Part filePart = req.getPart("imagen");
                if (filePart != null && filePart.getSize() > 0) {
                    String originalName = getFileName(filePart);
                    fileName = System.currentTimeMillis() + "_" + originalName;

                    // Definir ruta en el servidor
                    String uploadPath = getServletContext().getRealPath("") + File.separator + "uploads";
                    File uploadDir = new File(uploadPath);
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }

                    filePart.write(uploadPath + File.separator + fileName);
                }
            } catch (Exception e) {
                System.out.println("No se subió imagen o error en upload: " + e.getMessage());

            }

            Destino d = new Destino();
            d.setNombre(nombre);
            d.setPais(req.getParameter("pais"));
            d.setDescripcion(req.getParameter("descripcion"));
            d.setClima(req.getParameter("clima"));
            d.setPrecioBase(Double.parseDouble(precioStr));

            enviarRespuesta(resp, HttpServletResponse.SC_CREATED, "Destino guardado con éxito");

        } catch (SQLException e) {
            e.printStackTrace();
            enviarRespuesta(resp, 500, "Error de base de datos: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, 500, "Error general: " + e.getMessage());
        }
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            enviarRespuesta(resp, 401, "Sesión expirada o no iniciada");
            return;
        }
    }

    private String getFileName(Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return "archivo_desconocido";
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        String idStr = req.getParameter("id");
        try {
            if (idStr == null || idStr.trim().isEmpty()) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "ID requerido");
                return;
            }
            dao.eliminar(Integer.parseInt(idStr));
            enviarRespuesta(resp, HttpServletResponse.SC_OK, "Destino eliminado");
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
    
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
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
