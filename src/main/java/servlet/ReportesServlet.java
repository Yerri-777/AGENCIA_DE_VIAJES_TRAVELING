package servlet;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import config.ConexionBD;

@WebServlet("/reportes")
public class ReporteServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        configurarHeaders(resp);
        String tipo = req.getParameter("tipo");
        List<Map<String, Object>> resultado = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnection()) {
            String sql = switch (tipo != null ? tipo : "") {
                case "ventas" ->
                    "SELECT r.numero_reservacion, r.fecha_viaje, r.costo_total, p.nombre AS paquete, u.nombre_usuario AS agente "
                    + "FROM reservacion r "
                    + "INNER JOIN paquete p ON r.id_paquete = p.id_paquete "
                    + "INNER JOIN usuario u ON r.id_usuario = u.id_usuario "
                    + "WHERE r.estado = 'CONFIRMADA' ORDER BY r.fecha_viaje DESC";

                case "cancelaciones" ->
                    "SELECT c.id_cancelacion, c.fecha_cancelacion, c.monto_reembolso, c.perdida_agencia, r.numero_reservacion "
                    + "FROM cancelacion c "
                    + "INNER JOIN reservacion r ON c.numero_reservacion = r.numero_reservacion";

                case "destinos_populares" ->
                    "SELECT d.nombre, COUNT(r.numero_reservacion) as total_ventas "
                    + "FROM destino d JOIN paquete p ON d.id_destino = p.id_destino "
                    + "JOIN reservacion r ON p.id_paquete = r.id_paquete "
                    + "GROUP BY d.nombre ORDER BY total_ventas DESC LIMIT 5";

                default ->
                    "";
            };

            if (sql.isEmpty()) {
                enviarError(resp, 400, "Tipo de reporte no válido");
                return;
            }

            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        fila.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    resultado.add(fila);
                }
            }
            resp.getWriter().write(gson.toJson(resultado));

        } catch (Exception e) {
            enviarError(resp, 500, "Error: " + e.getMessage());
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
         resp.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");

        resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private void enviarError(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        resp.getWriter().write(gson.toJson(error));
    }
}