package dao;

import config.ConexionBD;
import modelo.Usuario;
import modelo.Rol;
import java.sql.*;

public class UsuarioDAO {

   
    public Usuario login(String user, String pass) throws Exception {
        String sql = "SELECT u.*, r.nombre AS rol_nombre "
                + "FROM usuario u "
                + "INNER JOIN rol r ON u.id_rol = r.id_rol "
                + "WHERE u.nombre_usuario = ? AND u.password = ? AND u.estado = 1";

        try (Connection conn = ConexionBD.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user);
            ps.setString(2, pass);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error en la autenticación: " + e.getMessage());
        }
        return null;
    }

   
    public void crear(Usuario u) throws Exception {
        String sqlExiste = "SELECT COUNT(*) FROM usuario WHERE nombre_usuario = ?";
        // Asegúrate de que id_rol coincida con los IDs de tu tabla Rol (1: ADMIN, 2: AGENTE, etc)
        String sqlInsert = "INSERT INTO usuario (nombre_usuario, password, estado, id_rol) VALUES (?, ?, 1, ?)";

        try (Connection conn = ConexionBD.getConnection()) {
            if (existeUsuario(conn, sqlExiste, u.getNombreUsuario())) {
                throw new Exception("El usuario '" + u.getNombreUsuario() + "' ya existe.");
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                ps.setString(1, u.getNombreUsuario());
                ps.setString(2, u.getPassword());

                int idRol = (u.getRol() != null) ? u.getRol().getIdRol() : 2; // 2 por defecto si no viene
                ps.setInt(3, idRol);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new Exception("Error al crear usuario: " + e.getMessage());
        }
    }

  
    public void desactivar(int idUsuario) throws Exception {
        String sql = "UPDATE usuario SET estado = 0 WHERE id_usuario = ?";

        try (Connection conn = ConexionBD.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            int filas = ps.executeUpdate();

            if (filas == 0) {
                throw new Exception("No se encontró el usuario con ID: " + idUsuario);
            }
        } catch (SQLException e) {
            throw new Exception("Error al desactivar usuario: " + e.getMessage());
        }
    }

    public Usuario[] listar() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM usuario";
        String sqlDatos = "SELECT u.*, r.nombre AS rol_nombre "
                + "FROM usuario u "
                + "INNER JOIN rol r ON u.id_rol = r.id_rol ORDER BY u.id_usuario ASC";

        try (Connection conn = ConexionBD.getConnection()) {
          
            int total = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlContar)) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }

            Usuario[] usuarios = new Usuario[total];

          
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlDatos)) {
                int i = 0;
                while (rs.next()) {
                    usuarios[i] = mapearUsuario(rs);
                    i++;
                }
            }
            return usuarios;
        }
    }

    
    private boolean existeUsuario(Connection conn, String sql, String nombre) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

  
    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Rol rol = new Rol(rs.getInt("id_rol"), rs.getString("rol_nombre"));
        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nombre_usuario"),
                rs.getString("password"),
                rs.getBoolean("estado"),
                rol
        );
    }
}
