package dao;

import config.ConexionBD;
import java.sql.*;

public class PagoDAO {

    public void registrar(String reservacion, double monto, int metodo, String fecha) throws Exception {

        // Consultas de validación y acción
        String sqlExiste = "SELECT COUNT(*) FROM reservacion WHERE numero_reservacion = ?";
        String sqlYaCancelada = "SELECT COUNT(*) FROM cancelacion WHERE numero_reservacion = ?";
        String sqlInsertarPago = "INSERT INTO pago (monto, metodo_pago, fecha_pago, numero_reservacion) VALUES (?, ?, ?, ?)";
        String sqlActualizarReserva = "UPDATE reservacion SET estado = 'PAGADA' WHERE numero_reservacion = ?";

        Connection conn = null;
        try {
            conn = ConexionBD.getConnection();
          
            conn.setAutoCommit(false);

            // Validar que la reservación exista
            if (!verificarExistencia(conn, sqlExiste, reservacion)) {
                throw new Exception("Error: El número de reservación '" + reservacion + "' no existe.");
            }

            //  Validar que no esté cancelada
            if (verificarExistencia(conn, sqlYaCancelada, reservacion)) {
                throw new Exception("Error: No se puede pagar una reservación cancelada.");
            }

            //  Insertar el Pago
            try (PreparedStatement psPago = conn.prepareStatement(sqlInsertarPago)) {
                psPago.setDouble(1, monto);
                psPago.setInt(2, metodo);
                psPago.setString(3, fecha);
                psPago.setString(4, reservacion);
                psPago.executeUpdate();
            }

            // Actualizar el estado de la Reservación
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlActualizarReserva)) {
                psUpdate.setString(1, reservacion);
                int filasAfectadas = psUpdate.executeUpdate();

                if (filasAfectadas == 0) {
                    throw new Exception("No se pudo actualizar el estado de la reservación.");
                }
            }

            // Si todo salió bien, guardamos los cambios en la DB
            conn.commit();

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                     }
            }
            throw new Exception("Error en la transacción de pago: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                     }
            }
        }
    }

    public Object[] listar() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM pago";
        String sqlDatos = "SELECT id_pago, monto, metodo_pago, fecha_pago, numero_reservacion FROM pago";

        try (Connection conn = ConexionBD.getConnection()) {
           
            int total = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlContar)) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }

            
            Object[] lista = new Object[total];

           
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlDatos)) {
                int i = 0;
                while (rs.next()) {
                
                    final int id = rs.getInt("id_pago");
                    final double m = rs.getDouble("monto");
                    final int met = rs.getInt("metodo_pago");
                    final String fec = rs.getString("fecha_pago");
                    final String res = rs.getString("numero_reservacion");

                    lista[i] = new Object() {
                        int idPago = id;
                        double monto = m;
                        int metodo = met;
                        String fecha = fec;
                        String reservacion = res;
                    };
                    i++;
                }
            }
            return lista;

        } catch (SQLException e) {
            throw new Exception("Error al obtener el historial de pagos: " + e.getMessage());
        }
    }

    private boolean verificarExistencia(Connection conn, String sql, String parametro) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
