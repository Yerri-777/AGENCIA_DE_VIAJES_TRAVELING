package dao;

import config.ConexionBD;
import modelo.PaqueteTuristico;
import modelo.Destino;
import java.sql.*;

public class PaqueteDAO {

    public void crear(PaqueteTuristico p) throws Exception {
        // Validaciones de negocio
        validarDatos(p);

        String sqlExiste = "SELECT COUNT(*) FROM paquete WHERE nombre = ? AND id_destino = ?";
        String sqlDestino = "SELECT COUNT(*) FROM destino WHERE id_destino = ?";
        String sqlInsertar = "INSERT INTO paquete (nombre, duracion_dias, precio_venta, capacidad_maxima, estado, id_destino) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection()) {
            //  Validar que el destino exista
            if (!verificarExistencia(conn, sqlDestino, p.getDestino().getIdDestino())) {
                throw new Exception("El destino seleccionado no existe.");
            }

            // Validar duplicados
            if (existePaquete(conn, sqlExiste, p.getNombre(), p.getDestino().getIdDestino())) {
                throw new Exception("Ya existe un paquete con ese nombre para el destino seleccionado.");
            }

            // Insertar
            try (PreparedStatement ps = conn.prepareStatement(sqlInsertar)) {
                ps.setString(1, p.getNombre());
                ps.setInt(2, p.getDuracionDias());
                ps.setDouble(3, p.getPrecioVenta());
                ps.setInt(4, p.getCapacidadMaxima());
                ps.setInt(5, 1); // Estado 1 = Activo por defecto
                ps.setInt(6, p.getDestino().getIdDestino());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new Exception("Error al guardar el paquete: " + e.getMessage());
        }
    }

    public PaqueteTuristico[] listar() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM paquete";
       
        String sqlDatos = "SELECT p.*, d.nombre as nombre_destino, d.pais, d.descripcion as desc_destino, d.clima, d.precio_base, d.imagen_url " +
                          "FROM paquete p " +
                          "INNER JOIN destino d ON p.id_destino = d.id_destino";

        try (Connection conn = ConexionBD.getConnection()) {
            int total = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlContar)) {
                if (rs.next()) total = rs.getInt(1);
            }

            PaqueteTuristico[] paquetes = new PaqueteTuristico[total];

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

    private PaqueteTuristico mapearPaquete(ResultSet rs) throws SQLException {
        
        Destino d = new Destino(
            rs.getInt("id_destino"),
            rs.getString("nombre_destino"),
            rs.getString("pais"),
            rs.getString("desc_destino"),
            rs.getString("clima"), (int) rs.getDouble("precio_base"),
            rs.getString("imagen_url")
        );

        
        return new PaqueteTuristico(
                rs.getInt("id_paquete"),
                rs.getString("nombre"),
                rs.getInt("duracion_dias"),
                rs.getDouble("precio_venta"),
                rs.getInt("capacidad_maxima"),
                String.valueOf(rs.getInt("estado")), // Convertimos a String si tu modelo usa String
                d
        );
    }

    private void validarDatos(PaqueteTuristico p) throws Exception {
        if (p.getPrecioVenta() <= 0) throw new Exception("El precio de venta debe ser mayor a 0.");
        if (p.getCapacidadMaxima() <= 0) throw new Exception("La capacidad debe ser al menos de 1 persona.");
        if (p.getDuracionDias() <= 0) throw new Exception("La duración debe ser al menos de 1 día.");
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