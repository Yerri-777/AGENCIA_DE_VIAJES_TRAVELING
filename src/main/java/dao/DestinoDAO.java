package dao;

import config.ConexionBD;
import modelo.Destino;
import java.sql.*;

public class DestinoDAO {

  
    public void crear(Destino d) throws Exception {
        if (existeDestino(d.getNombre(), d.getPais())) {
            throw new Exception("El destino '" + d.getNombre() + "' en '" + d.getPais() + "' ya está registrado.");
        }

      
        String sql = "INSERT INTO destino (nombre, pais, descripcion, clima, precio_base, imagen_url) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

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

    private Destino mapearDestino(ResultSet rs) throws SQLException {
        
        return new Destino(
                rs.getInt("id_destino"),
                rs.getString("nombre"),
                rs.getString("pais"),
                rs.getString("descripcion"),
                rs.getString("clima"), (int) rs.getDouble("precio_base"), // Mapeo del nuevo campo
                rs.getString("imagen_url")
        );
    }

    private boolean existeDestino(String nombre, String pais) throws Exception {
        String sql = "SELECT COUNT(*) FROM destino WHERE nombre = ? AND pais = ?";
        try (Connection conn = ConexionBD.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, pais);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
  
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
            if (e.getErrorCode() == 1451) { // Error de llave foránea en MySQL
                throw new Exception("No se puede eliminar el destino porque está asignado a un paquete activo.");
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
