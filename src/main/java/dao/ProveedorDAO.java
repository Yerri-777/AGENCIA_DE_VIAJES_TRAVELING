package dao;

import config.ConexionBD;
import modelo.Proveedor;
import java.sql.*;

public class ProveedorDAO {

    
    public void crear(Proveedor p) throws Exception {
        // 1. Validaciones de negocio
        if (p.getNombre() == null || p.getNombre().trim().isEmpty()) {
            throw new Exception("El nombre del proveedor es obligatorio.");
        }

       
        if (existeProveedor(p.getNombre(), p.getPais())) {
            throw new Exception("El proveedor '" + p.getNombre() + "' ya está registrado en " + p.getPais() + ".");
        }

        String sql = "INSERT INTO proveedor (nombre, tipo_servicio, pais, contacto) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getNombre());
            ps.setInt(2, p.getTipoServicio());
            ps.setString(3, p.getPais());
            ps.setString(4, p.getContacto());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Error al registrar el proveedor: " + e.getMessage());
        }
    }

  
    public Proveedor[] listar() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM proveedor";
        String sqlDatos = "SELECT * FROM proveedor";

        try (Connection conn = ConexionBD.getConnection()) {

            int total = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlContar)) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }

            Proveedor[] proveedores = new Proveedor[total];

            
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlDatos)) {
                int i = 0;
                while (rs.next()) {
                    proveedores[i] = mapearProveedor(rs);
                    i++;
                }
            }
            return proveedores;
        }
    }
    
     public void eliminar(int idProveedor) throws Exception {
   
    String sqlTieneServicios = "SELECT COUNT(*) FROM servicio_paquete WHERE id_proveedor = ?";
    String sqlEliminar = "DELETE FROM proveedor WHERE id_proveedor = ?";

    try (Connection conn = ConexionBD.getConnection()) {


        if (verificarRelaciones(conn, sqlTieneServicios, idProveedor)) {
            throw new Exception("No se puede eliminar el proveedor. "
                    + "Está asignado a servicios dentro de paquetes turísticos.");
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlEliminar)) {
            ps.setInt(1, idProveedor);
            int filasAfectadas = ps.executeUpdate();

            if (filasAfectadas == 0) {
                throw new Exception("El proveedor con ID " + idProveedor + " no existe.");
            }
        }
    } catch (SQLException e) {
        throw new Exception("Error de base de datos al eliminar: " + e.getMessage());
    }
}
    private boolean verificarRelaciones(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }


  
    private boolean existeProveedor(String nombre, String pais) throws Exception {
        String sql = "SELECT COUNT(*) FROM proveedor WHERE nombre = ? AND pais = ?";
        try (Connection conn = ConexionBD.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, pais);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

   
    private Proveedor mapearProveedor(ResultSet rs) throws SQLException {
        return new Proveedor(
                rs.getInt("id_proveedor"),
                rs.getString("nombre"),
                rs.getInt("tipo_servicio"),
                rs.getString("pais"),
                rs.getString("contacto")
        );
    }
}
