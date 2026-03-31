package servlet;

import com.google.gson.Gson;
import dao.PagoDAO;
import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/pagos")
public class PagoServlet extends HttpServlet {

    private final PagoDAO dao = new PagoDAO();
    private final Gson gson = new Gson();

    
    static class PagoRequest {

        String reservacion;
        double monto;
        int metodo;
        String fecha;
    }

    // LISTAR PAGOS 
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
            // Concatenamos con el método listar del DAO (puedes devolver Object[] o Pago[])
            Object[] lista = dao.listar();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(lista != null ? lista : new Object[0]));
        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al listar pagos: " + e.getMessage());
        }
    }

    // REGISTRAR PAGO
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try (BufferedReader reader = req.getReader()) {
            PagoRequest p = gson.fromJson(reader, PagoRequest.class);

            if (p == null || p.reservacion == null) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Datos de pago incompletos");
                return;
            }

           
            dao.registrar(p.reservacion, p.monto, p.metodo, p.fecha);

            enviarRespuesta(resp, HttpServletResponse.SC_CREATED, "Pago registrado exitosamente");

        } catch (Exception e) {
            e.printStackTrace();
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error en el servidor: " + e.getMessage());
        }
    }

    //SOPORTE CORS
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        configurarHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void configurarHeaders(HttpServletResponse resp) {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
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
