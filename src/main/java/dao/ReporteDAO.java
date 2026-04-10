package dao;

import config.ConexionBD;
import java.sql.*;
import java.util.*;

public class ReporteDAO {

    // 1. Destinos más vendidos
    public List<Map<String, Object>> obtenerDestinosPopulares() throws Exception {
        String sql = "SELECT d.nombre, COUNT(r.numero_reservacion) as ventas "
                + "FROM destino d JOIN paquete p ON d.id_destino = p.id_destino "
                + "JOIN reservacion r ON p.id_paquete = r.id_paquete "
                + "GROUP BY d.nombre ORDER BY ventas DESC LIMIT 5";
        return ejecutarConsultaGrafica(sql);
    }

    // 2. Ingresos mensuales
    public List<Map<String, Object>> obtenerIngresosMensuales() throws Exception {
        String sql = "SELECT MONTHNAME(fecha_pago) as mes, SUM(monto) as total "
                + "FROM pago GROUP BY MONTH(fecha_pago) ORDER BY MONTH(fecha_pago)";
        return ejecutarConsultaGrafica(sql);
    }

    private List<Map<String, Object>> ejecutarConsultaGrafica(String sql) throws Exception {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    fila.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                lista.add(fila);
            }
        }
        return lista;
    }
}
