package dao;

import config.ConexionBD;
import java.sql.*;

public class CancelacionDAO {

    public void cancelacion(String reservacion, double reembolso, double perdida) throws Exception {
        
        String sqlExiste = "SELECT COUNT(*) FROM reservacion WHERE numero_reservacion = ?";
        String sqlYaCancelada = "SELECT estado FROM reservacion WHERE numero_reservacion = ?";
        String sqlInsertar = "INSERT INTO cancelacion (monto_reembolso, perdida_agencia, numero_reservacion) VALUES (?, ?, ?)";
       
        String sqlActualizarEstado = "UPDATE reservacion SET estado = 'CANCELADA' WHERE numero_reservacion = ?";

        try (Connection conn = ConexionBD.getConnection()) {
            
            conn.setAutoCommit(false);

            try {
                // 1. Validar existencia
                if (!existeRegistro(conn, sqlExiste, reservacion)) {
                    throw new Exception("La reservación '" + reservacion + "' no existe.");
                }

                // 2. Validar si ya está cancelada (viendo el estado actual)
                if (estaCancelada(conn, sqlYaCancelada, reservacion)) {
                    throw new Exception("La reservación '" + reservacion + "' ya se encuentra CANCELADA.");
                }

                // 3. Insertar registro de cancelación
                try (PreparedStatement ps = conn.prepareStatement(sqlInsertar)) {
                    ps.setDouble(1, reembolso);
                    ps.setDouble(2, perdida);
                    ps.setString(3, reservacion);
                    ps.executeUpdate();
                }

                // 4. Actualizar el estado de la reservación a 'CANCELADA'
                try (PreparedStatement ps = conn.prepareStatement(sqlActualizarEstado)) {
                    ps.setString(1, reservacion);
                    ps.executeUpdate();
                }

           
                conn.commit();

            } catch (Exception e) {
            
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            throw new Exception("Error de base de datos: " + e.getMessage());
        }
    }
    
    public Object[] listar() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM cancelacion";
        String sqlDatos = "SELECT * FROM cancelacion";

        try (Connection conn = ConexionBD.getConnection()) {
            // 1. Contar para dimensionar el arreglo
            int total = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlContar)) {
                if (rs.next()) total = rs.getInt(1);
            }

            // 2. Crear el arreglo (puedes usar el modelo Cancelacion si lo tienes)
            Object[] lista = new Object[total];

            // 3. Llenar el arreglo
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlDatos)) {
                int i = 0;
                while (rs.next()) {
                  
                    lista[i] = mapearCancelacion(rs);
                    i++;
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new Exception("Error al listar cancelaciones: " + e.getMessage());
        }
    }
    
    
    private Object mapearCancelacion(ResultSet rs) throws SQLException {
     
        return new Object() {
            int id = rs.getInt("id_cancelacion");
            double reembolso = rs.getDouble("monto_reembolso");
            double perdida = rs.getDouble("perdida_agencia");
            String reservacion = rs.getString("numero_reservacion");
        };
    }
    
    
    private boolean existeRegistro(Connection conn, String sql, String parametro) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    
    private boolean estaCancelada(Connection conn, String sql, String reservacion) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reservacion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "CANCELADA".equals(rs.getString("estado"));
                }
            }
        }
        return false;
    }
}