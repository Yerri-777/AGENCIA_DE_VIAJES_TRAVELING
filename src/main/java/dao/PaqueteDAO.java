package dao;

import config.ConexionBD;
import modelo.Paquete;
import modelo.Destino;
import java.sql.*;

public class PaqueteDAO {


    public void crear(Connection conn, Paquete p) throws Exception {
        validarDatos(p);
        String sqlExiste = "SELECT COUNT(*) FROM paquete WHERE nombre = ? AND id_destino = ?";
        String sqlDestino = "SELECT COUNT(*) FROM destino WHERE id_destino = ?";
        String sqlInsertar = "INSERT INTO paquete (nombre, duracion_dias, precio_venta, capacidad_maxima, estado, id_destino) VALUES (?, ?, ?, ?, ?, ?)";

        if (!verificarExistencia(conn, sqlDestino, p.getDestino().getIdDestino())) {
            throw new Exception("El destino ID " + p.getDestino().getIdDestino() + " no existe.");
        }

        if (existePaquete(conn, sqlExiste, p.getNombre(), p.getDestino().getIdDestino())) {
            throw new Exception("Ya existe el paquete '" + p.getNombre() + "' en este destino.");
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlInsertar)) {
            ps.setString(1, p.getNombre());
            ps.setInt(2, p.getDuracionDias());
            ps.setDouble(3, p.getPrecioVenta());
            ps.setInt(4, p.getCapacidadMaxima());
            ps.setInt(5, 1); // Estado activo
            ps.setInt(6, p.getDestino().getIdDestino());
            ps.executeUpdate();
        }
    }

    public Paquete[] listar() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM paquete";
        String sqlDatos = "SELECT p.*, d.nombre as nombre_destino, d.pais, d.descripcion as desc_destino, "
                + "d.clima, d.precio_base, d.imagen_url "
                + "FROM paquete p "
                + "INNER JOIN destino d ON p.id_destino = d.id_destino";

        try (Connection conn = ConexionBD.getConnection()) {
            int total = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlContar)) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }

            Paquete[] paquetes = new Paquete[total];
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlDatos)) {
                int i = 0;
                while (rs.next()) {
                    paquetes[i] = mapearPaquete(rs);
                    i++;
                }
            }
            return paquetes;
        }
    }
  
    public int obtenerIdPorNombre(Connection conn, String nombre) throws SQLException {
    String sql = "SELECT id_paquete FROM paquete WHERE nombre = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, nombre);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("id_paquete");
        }
    }
    return -1;
}

    private Paquete mapearPaquete(ResultSet rs) throws SQLException {
        Destino d = new Destino();
        d.setIdDestino(rs.getInt("id_destino"));
        d.setNombre(rs.getString("nombre_destino"));
        d.setPais(rs.getString("pais"));
        d.setDescripcion(rs.getString("desc_destino"));
        d.setClima(rs.getString("clima"));
        d.setPrecioBase((Double) rs.getDouble("precio_base"));
        d.setImagenUrl(rs.getString("imagen_url"));

        return new Paquete(
                rs.getInt("id_paquete"),
                rs.getString("nombre"),
                rs.getInt("duracion_dias"),
                rs.getDouble("precio_venta"),
                rs.getInt("capacidad_maxima"),
                rs.getInt("estado"),
                d
        );
    }

    private void validarDatos(Paquete p) throws Exception {
        if (p == null) {
            throw new Exception("El paquete es nulo.");
        }
        if (p.getNombre() == null || p.getNombre().trim().isEmpty()) {
            throw new Exception("Nombre obligatorio.");
        }
        if (p.getPrecioVenta() <= 0) {
            throw new Exception("El precio debe ser mayor a 0.");
        }
        if (p.getDestino() == null || p.getDestino().getIdDestino() <= 0) {
            throw new Exception("ID de destino no válido.");
        }
    }

    private boolean verificarExistencia(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean existePaquete(Connection conn, String sql, String nombre, int idDestino) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, idDestino);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
