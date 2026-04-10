package servlet;

import com.google.gson.Gson;
import dao.ReservacionDAO;
import modelo.Reservacion;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/comprobante")
public class ComprobanteServlet extends HttpServlet {

    private final ReservacionDAO reservDao = new ReservacionDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=comprobante.pdf");

        String numero = req.getParameter("numero");
        if (numero == null || numero.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"mensaje\": \"Debe indicar el numero de reservacion\"}");
            return;
        }

        try {
            Reservacion r = reservDao.buscarPorNumero(numero);
            if (r == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"mensaje\": \"Reservacion no encontrada\"}");
                return;
            }
            if (!"CONFIRMADA".equalsIgnoreCase(r.getEstado())) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write("{\"mensaje\": \"La reservacion no está confirmada. No se puede generar comprobante.\"}");
                return;
            }

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                    cs.newLineAtOffset(70, 700);
                    cs.showText("Comprobante de Pago - Horizontes sin Límites");
                    cs.endText();

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(70, 660);
                    cs.showText("Número de reservación: " + r.getNumeroReservacion());
                    cs.newLineAtOffset(0, -16);
                    cs.showText("Fecha de viaje: " + r.getFechaViaje());
                    cs.newLineAtOffset(0, -16);
                    cs.showText("Cantidad de pasajeros: " + r.getCantidadPasajeros());
                    cs.newLineAtOffset(0, -16);
                    cs.showText("Costo total: " + r.getCostoTotal());
                    cs.endText();
                }

                doc.save(resp.getOutputStream());
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new ErrorMsg("Error generando comprobante: " + e.getMessage())));
        }
    }

    static class ErrorMsg {
        @SuppressWarnings("unused")
        public String mensaje;
        ErrorMsg(String m) { this.mensaje = m; }
    }
}
