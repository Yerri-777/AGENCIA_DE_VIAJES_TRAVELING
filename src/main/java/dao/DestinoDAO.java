package dao;

import config.ConexionBD;
import modelo.Destino;
import java.sql.*;

public class DestinoDAO {

 

    
  public void crear(Connection conn, Destino d) throws Exception {
       
        if (conn == null) throw new Exception("Error: Conexión a la base de datos es nula.");
        
        if (this.existeDestino(conn, d.getNombre(), d.getPais())) {
            throw new Exception("El destino '" + d.getNombre() + "' en '" + d.getPais() + "' ya está registrado.");
        }

        String sql = "INSERT INTO destino (nombre, pais, descripcion, clima, precio_base, imagen_url) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getNombre());
            ps.setString(2, d.getPais());
            ps.setString(3, d.getDescripcion());
            ps.setString(4, d.getClima());
            ps.setDouble(5, d.getPrecioBase());
            ps.setString(6, d.getImagenUrl());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Error al insertar el destino: " + e.getMessage());
        }
    }
    // Método auxiliar que usa la conexión activa
    private boolean existeDestino(Connection conn, String nombre, String pais) throws SQLException {
        String sql = "SELECT COUNT(*) FROM destino WHERE nombre = ? AND pais = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, pais);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public Destino[] listar() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM destino";
        String sqlDatos = "SELECT * FROM destino";

        try (Connection conn = ConexionBD.getConnection()) {
            int total = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlContar)) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }

            Destino[] destinos = new Destino[total];
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlDatos)) {
                int i = 0;
                while (rs.next()) {
                    destinos[i] = mapearDestino(rs);
                    i++;
                }
            }
            return destinos;
        }
    }

    public class PagoDAO {

        public void registrarPago(double monto, int metodo, String numRes, int idUsuario) throws Exception {
            String sqlPago = "INSERT INTO pago (monto, metodo_pago, numero_reservacion, id_usuario) VALUES (?, ?, ?, ?)";
            String sqlUpdateRes = "UPDATE reservacion SET estado = 'CONFIRMADA' WHERE numero_reservacion = ?";
            String sqlVerificar = "SELECT costo_total FROM reservacion WHERE numero_reservacion = ?";

            try (Connection conn = ConexionBD.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // 1. Validar que el monto sea correcto
                    try (PreparedStatement ps = conn.prepareStatement(sqlVerificar)) {
                        ps.setString(1, numRes);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                double total = rs.getDouble("costo_total");
                                if (monto < total) {
                                    throw new Exception("El monto no cubre el total de la reservación.");
                                }
                            } else {
                                throw new Exception("Reservación no encontrada.");
                            }
                        }
                    }

                    // 2. Insertar Pago
                    try (PreparedStatement ps = conn.prepareStatement(sqlPago)) {
                        ps.setDouble(1, monto);
                        ps.setInt(2, metodo);
                        ps.setString(3, numRes);
                        ps.setInt(4, idUsuario);
                        ps.executeUpdate();
                    }

                    // 3. Actualizar Reservación
                    try (PreparedStatement ps = conn.prepareStatement(sqlUpdateRes)) {
                        ps.setString(1, numRes);
                        ps.executeUpdate();
                    }

                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            }
        }
    }

    private Destino mapearDestino(ResultSet rs) throws SQLException {
        return new Destino(
                rs.getInt("id_destino"),
                rs.getString("nombre"),
                rs.getString("pais"),
                rs.getString("descripcion"),
                rs.getString("clima"),
                rs.getDouble("precio_base"),
                rs.getString("imagen_url")
        );
    }

    public void eliminar(int id) throws Exception {
        String sqlVerificar = "SELECT COUNT(*) FROM destino WHERE id_destino = ?";
        String sqlDelete = "DELETE FROM destino WHERE id_destino = ?";

        try (Connection conn = ConexionBD.getConnection()) {
            try (PreparedStatement psVer = conn.prepareStatement(sqlVerificar)) {
                psVer.setInt(1, id);
                try (ResultSet rs = psVer.executeQuery()) {
                    if (!(rs.next() && rs.getInt(1) > 0)) {
                        throw new Exception("El destino con ID " + id + " no existe.");
                    }
                }
            }

            try (PreparedStatement psDel = conn.prepareStatement(sqlDelete)) {
                psDel.setInt(1, id);
                psDel.executeUpdate();
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                throw new Exception("No se puede eliminar: el destino tiene paquetes asociados.");
            }
            throw new Exception("Error al eliminar: " + e.getMessage());
        }
    }

    public Destino buscarPorId(int id) throws Exception {
        String sql = "SELECT * FROM destino WHERE id_destino = ?";
        try (Connection conn = ConexionBD.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearDestino(rs);
                }
            }
        }
        return null;
    }
}
