package dao;

import config.ConexionBD;
import modelo.Pago;
import java.sql.*;

public class PagoDAO {
   
    public void registrar(String reservacion, double monto, int metodo, String fecha) throws Exception {
        String sqlInfo = "SELECT estado, costo_total FROM reservacion WHERE numero_reservacion = ?";
        String sqlInsertarPago = "INSERT INTO pago (monto, metodo_pago, fecha_pago, numero_reservacion) VALUES (?, ?, ?, ?)";
        String sqlActualizarReserva = "UPDATE reservacion SET estado = 'CONFIRMADA' WHERE numero_reservacion = ?";
        String sqlSumPagos = "SELECT COALESCE(SUM(monto),0) FROM pago WHERE numero_reservacion = ?";

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String estadoActual = null;
                double costoTotal = 0;

                try (PreparedStatement ps = conn.prepareStatement(sqlInfo)) {
                    ps.setString(1, reservacion);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            estadoActual = rs.getString("estado");
                            costoTotal = rs.getDouble("costo_total");
                        } else {
                            throw new Exception("La reservación '" + reservacion + "' no existe.");
                        }
                    }
                }

                if ("CANCELADA".equalsIgnoreCase(estadoActual)) {
                    throw new Exception("No se puede pagar una reservación CANCELADA.");
                }
                if ("PAGADA".equals(estadoActual)) {
                    throw new Exception("Esta reservación ya ha sido pagada previamente.");
                }

        public void crearCarga(Connection conn, String numRes, double monto, int metodo, String fecha) throws Exception {
    String sqlInsertar = "INSERT INTO pago (monto, metodo_pago, fecha_pago, numero_reservacion) VALUES (?, ?, ?, ?)";
    String sqlUpdateRes = "UPDATE reservacion SET estado = 'CONFIRMADA' WHERE numero_reservacion = ?";

    //  Insertar el registro del pago
    try (PreparedStatement ps = conn.prepareStatement(sqlInsertar)) {
        ps.setDouble(1, monto);
        ps.setInt(2, metodo);
        ps.setString(3, fecha);
        ps.setString(4, numRes);
        
        int filasAfectadas = ps.executeUpdate();
        if (filasAfectadas == 0) {
            throw new SQLException("No se pudo insertar el pago para la reserva: " + numRes);
        }
    }
    

    // Actualizar el estado de la reservación a CONFIRMADA automáticamente
    try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateRes)) {
        psUpdate.setString(1, numRes);
        psUpdate.executeUpdate();
    }
        

                // Insertar el Pago
                try (PreparedStatement psPago = conn.prepareStatement(sqlInsertarPago)) {
                    psPago.setDouble(1, monto);
                    psPago.setInt(2, metodo);
                    psPago.setString(3, fecha);
                    psPago.setString(4, reservacion);
                    psPago.executeUpdate();
                }

                //  Actualizar estado de la Reserva
                try (PreparedStatement psUpdate = conn.prepareStatement(sqlActualizarReserva)) {
                    psUpdate.setString(1, reservacion);
                    psUpdate.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void crearCarga(Connection conn, String numRes, double monto, int metodo, String fecha) throws Exception {
        String sqlInsertar = "INSERT INTO pago (monto, metodo_pago, fecha_pago, numero_reservacion) VALUES (?, ?, ?, ?)";
        String sqlUpdateRes = "UPDATE reservacion SET estado = 'CONFIRMADA' WHERE numero_reservacion = ?";

        try (PreparedStatement ps = conn.prepareStatement(sqlInsertar)) {
            ps.setDouble(1, monto);
            ps.setInt(2, metodo);
            ps.setString(3, fecha);
            ps.setString(4, numRes);
            int filas = ps.executeUpdate();
            if (filas == 0) throw new Exception("No se pudo insertar el pago para la reservación: " + numRes);
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlUpdateRes)) {
            ps.setString(1, numRes);
            ps.executeUpdate();
        }
    }




    
    public Pago[] listar() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM pago";
        String sqlDatos = "SELECT * FROM pago ORDER BY id_pago DESC";

        try (Connection conn = ConexionBD.getConnection()) {
            int total = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlContar)) {
                if (rs.next()) total = rs.getInt(1);
            }

            Pago[] lista = new Pago[total];
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlDatos)) {
                int i = 0;
                while (rs.next()) {
                    Pago p = new Pago();
                    p.setIdPago(rs.getInt("id_pago"));
                    p.setMonto(rs.getDouble("monto"));
                    p.setMetodoPago(rs.getInt("metodo_pago"));
                    p.setFechaPago(rs.getString("fecha_pago"));
                    p.setNumeroReservacion(rs.getString("numero_reservacion"));
                    lista[i] = p;
                    i++;
                }
            }
            return lista;
        }
    }
}