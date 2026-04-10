package dao;

import config.ConexionBD;
import modelo.Usuario;
import modelo.Rol;
import java.sql.*;

public class UsuarioDAO {

    public Usuario login(String user, String pass) {
        String sql = "SELECT u.*, r.nombre AS nombre_rol FROM usuario u "
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
            e.printStackTrace();
        }
        return null;
    }

    public void crear(Connection conn, Usuario u) throws Exception {
        String sqlExiste = "SELECT COUNT(*) FROM usuario WHERE nombre_usuario = ?";
        String sqlInsert = "INSERT INTO usuario (nombre_usuario, password, estado, id_rol) VALUES (?, ?, ?, ?)";

        if (existeUsuario(conn, sqlExiste, u.getNombreUsuario())) {
            throw new Exception("El usuario '" + u.getNombreUsuario() + "' ya existe.");
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
            ps.setString(1, u.getNombreUsuario());
            ps.setString(2, u.getPassword());
            ps.setInt(3, u.getEstado() == 0 ? 1 : u.getEstado()); // Default activo
            ps.setInt(4, (u.getRol() != null) ? u.getRol().getIdRol() : 1);
            ps.executeUpdate();
        }
    }

    // El "Update" que faltaba para el CRUD completo
    public void actualizar(Usuario u) throws Exception {
        String sql = "UPDATE usuario SET password = ?, estado = ?, id_rol = ? WHERE id_usuario = ?";
        try (Connection conn = ConexionBD.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getPassword());
            ps.setInt(2, u.getEstado());
            ps.setInt(3, u.getRol().getIdRol());
            ps.setInt(4, u.getIdUsuario());
            ps.executeUpdate();
        }
    }

    public Usuario[] listar() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM usuario";
        String sqlDatos = "SELECT u.*, r.nombre AS nombre_rol FROM usuario u "
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
        Rol rol = new Rol(rs.getInt("id_rol"), rs.getString("nombre_rol"));
        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nombre_usuario"),
                rs.getString("password"),
                rs.getInt("estado"),
                rol
        );
    }
}
