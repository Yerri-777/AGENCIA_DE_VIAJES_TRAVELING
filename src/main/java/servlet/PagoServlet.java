package servlet;

import com.google.gson.Gson;
import dao.PagoDAO;
import modelo.Pago;
import java.io.*;
import java.util.stream.Collectors;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/pagos")
public class PagoServlet extends HttpServlet {

    private final PagoDAO dao = new PagoDAO();
    private final Gson gson = new Gson();

        /*
         Payment endpoint notes and examples

         POST /pagos
         - Purpose: register a payment for a reservation
         - Expected JSON body:
             {
                 "reservacion": "RES-00042",
                 "monto": 150.0,
                 "metodo": 1,    // e.g., 1=efectivo, 2=tarjeta, 3=transferencia
                 "fecha": "2026-04-05"
             }

         - Server responsibilities:
             * Validate that `reservacion` exists.
             * Validate monto > 0 and fecha format.
             * Insert payment record and sum payments for the reservation.
             * If sum(pagos) >= costo_total then mark reservation as CONFIRMADA (or PAGADA) and allow PDF generation.

         Example responses:
             - Success (201): { "mensaje": "Pago registrado y reservación actualizada a PAGADA" }
             - Bad request (400): { "mensaje": "Datos de pago incompletos o inválidos" }
             - Server error (500): { "mensaje": "..." }

         Notes for DAO:
             - PagoDAO.registrar must insert payment and return the new balance or a flag if reservation is fully paid.
             - Use transaction when updating payment + reservation state to avoid race conditions.
        */

    
    static class PagoRequest {

        String reservacion;
        double monto;
        int metodo;
        String fecha;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
            Pago[] lista = dao.listar();
            resp.getWriter().write(gson.toJson(lista != null ? lista : new Pago[0]));
        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarHeaders(resp);
        try {
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            PagoRequest p = gson.fromJson(body, PagoRequest.class);

            if (p == null || p.reservacion == null || p.reservacion.trim().isEmpty()) {
                enviarRespuesta(resp, HttpServletResponse.SC_BAD_REQUEST, "Datos de pago incompletos o inválidos");
                return;
            }

            dao.registrar(p.reservacion, p.monto, p.metodo, p.fecha);
            enviarRespuesta(resp, HttpServletResponse.SC_CREATED, "Pago registrado y reservación actualizada a PAGADA");

        } catch (Exception e) {
            enviarRespuesta(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
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
        resp.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
