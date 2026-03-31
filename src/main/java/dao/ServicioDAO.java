package dao;

import config.ConexionBD;
import modelo.Servicio;
import modelo.Proveedor;
import java.sql.*;

public class ServicioDAO {

    public void agregarAServicio(Servicio s, int idPaquete) throws Exception {
        
        String sql = "INSERT INTO servicio (nombre, descripcion, costo_proveedor, id_proveedor, id_paquete) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, s.getNombre());
            ps.setString(2, s.getDescripcion());
            ps.setDouble(3, s.getCostoProveedor());
            ps.setInt(4, s.getProveedor().getIdProveedor());
            ps.setInt(5, idPaquete);
            
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Error al insertar servicio: " + e.getMessage());
        }
    }

    public Servicio[] listarPorPaquete(int idPaquete) throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM servicio WHERE id_paquete = ?";
        String sqlDatos = "SELECT s.*, p.nombre as nombre_prov, p.tipo_servicio FROM servicio s " +
                          "INNER JOIN proveedor p ON s.id_proveedor = p.id_proveedor " +
                          "WHERE s.id_paquete = ?";

        try (Connection conn = ConexionBD.getConnection()) {
            int total = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlContar)) {
                ps.setInt(1, idPaquete);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) total = rs.getInt(1);
                }
            }

            Servicio[] lista = new Servicio[total];
            try (PreparedStatement ps = conn.prepareStatement(sqlDatos)) {
                ps.setInt(1, idPaquete);
                try (ResultSet rs = ps.executeQuery()) {
                    int i = 0;
                    while (rs.next()) {
                        lista[i] = mapearServicio(rs);
                        i++;
                    }
                }
            }
            return lista;
        }
    }

 
    public Servicio[] listarTodo() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM servicio";
        String sqlDatos = "SELECT s.*, p.nombre as nombre_prov, p.tipo_servicio FROM servicio s " +
                          "INNER JOIN proveedor p ON s.id_proveedor = p.id_proveedor";

        try (Connection conn = ConexionBD.getConnection()) {
            int total = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlContar)) {
                if (rs.next()) total = rs.getInt(1);
            }

            Servicio[] lista = new Servicio[total];
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlDatos)) {
                int i = 0;
                while (rs.next()) {
                    lista[i] = mapearServicio(rs);
                    i++;
                }
            }
            return lista;
        }
    }

   private Servicio mapearServicio(ResultSet rs) throws SQLException {
    
    Proveedor p = new Proveedor();
    p.setIdProveedor(rs.getInt("id_proveedor"));
    p.setNombre(rs.getString("nombre_prov"));
   
   p.setTipoServicio(rs.getInt("tipo_servicio")); 

    
    Servicio s = new Servicio();
    s.setIdServicio(rs.getInt("id_servicio"));
    s.setNombre(rs.getString("nombre"));
    s.setDescripcion(rs.getString("descripcion"));
    s.setCostoProveedor(rs.getDouble("costo_proveedor"));
    

    s.setProveedor(p);
    
    return s;
}
}