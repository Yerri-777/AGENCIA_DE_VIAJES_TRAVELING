package dao;

import config.ConexionBD;
import modelo.Cliente;
import java.sql.*;

public class ClienteDAO {

    public void crear(Cliente c) throws Exception {
        if (buscarPorDpi(c.getDpi()) != null) {
            throw new Exception("El cliente con DPI " + c.getDpi() + " ya está registrado.");
        }

        String sql = "INSERT INTO cliente (dpi, nombre_completo, fecha_nacimiento, telefono, email, nacionalidad) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getDpi());
            ps.setString(2, c.getNombreCompleto());
           
            ps.setString(3, c.getFechaNacimiento()); 
            ps.setString(4, c.getTelefono());
            ps.setString(5, c.getEmail());
            ps.setString(6, c.getNacionalidad());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Error al guardar el cliente: " + e.getMessage());
        }
    }

    public Cliente buscarPorDpi(String dpi) throws Exception {
        String sql = "SELECT * FROM cliente WHERE dpi = ?";
        try (Connection conn = ConexionBD.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dpi);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearCliente(rs);
            }
        }
        return null;
    }

    public Cliente[] listar() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM cliente";
        String sqlDatos = "SELECT * FROM cliente";

        try (Connection conn = ConexionBD.getConnection()) {
            int total = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlContar)) {
                if (rs.next()) total = rs.getInt(1);
            }

            Cliente[] clientes = new Cliente[total];

            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlDatos)) {
                int i = 0;
                while (rs.next()) {
                    clientes[i] = mapearCliente(rs);
                    i++;
                }
            }
            return clientes;
        }
    }

   
    public void eliminar(String dpi) throws Exception {
        String sql = "DELETE FROM cliente WHERE dpi = ?";
        try (Connection conn = ConexionBD.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dpi);
            int filas = ps.executeUpdate();
            if (filas == 0) throw new Exception("No se encontró el cliente con DPI: " + dpi);
        } catch (SQLException e) {
            throw new Exception("No se puede eliminar el cliente (posiblemente tiene viajes registrados): " + e.getMessage());
        }
    }

    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getString("dpi"),
                rs.getString("nombre_completo"),
                rs.getString("fecha_nacimiento"),
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getString("nacionalidad")
        );
    }
}