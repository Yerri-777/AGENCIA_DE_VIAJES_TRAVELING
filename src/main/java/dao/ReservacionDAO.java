package dao;

import config.ConexionBD;
import modelo.Reservacion;
import java.sql.*;

public class ReservacionDAO {

    private Object dpispasajeros;
    private Iterable<String> dpisPasajeros;

   
  public Reservacion[] listar() throws Exception {
        String sqlContar = "SELECT COUNT(*) FROM reservacion";
        String sqlDatos = "SELECT * FROM reservacion ORDER BY fecha_creacion DESC";

        try (Connection conn = ConexionBD.getConnection()) {
            int total = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlContar)) {
                if (rs.next()) total = rs.getInt(1);
            }

            Reservacion[] lista = new Reservacion[total];
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlDatos)) {
                int i = 0;
                while (rs.next()) {
                    lista[i] = mapearReservacion(rs);
                    i++;
                }
            }
            return lista;
        }
    }
    
    public Reservacion buscarPorNumero(String numero) throws Exception {
        String sql = "SELECT * FROM reservacion WHERE numero_reservacion = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearReservacion(rs);
                }
            }
        }
        return null;
    }
    
    private Reservacion mapearReservacion(ResultSet rs) throws SQLException {
        Reservacion res = new Reservacion();
        res.setNumeroReservacion(rs.getString("numero_reservacion"));
        res.setFechaViaje(rs.getString("fecha_viaje"));
        res.setCantidadPasajeros(rs.getInt("cantidad_pasajeros"));
        res.setCostoTotal(rs.getDouble("costo_total"));
        res.setEstado(rs.getString("estado"));
      
        return res;
    }
    
  public String crear(Reservacion r) throws Exception {
        String sqlAgente = "SELECT COUNT(*) FROM usuario WHERE id_usuario = ?";
        String sqlPaquete = "SELECT capacidad_maxima, (SELECT COUNT(*) FROM reservacion WHERE id_paquete = ? AND estado != 'CANCELADA') as ocupados FROM paquete WHERE id_paquete = ?";
        String sqlInsertarRes = "INSERT INTO reservacion (numero_reservacion, fecha_creacion, fecha_viaje, cantidad_pasajeros, costo_total, estado, id_usuario, id_paquete) VALUES (?, NOW(), ?, ?, ?, ?, ?, ?)";
        String sqlInsertarPasajeros = "INSERT INTO reservacion_pasajeros (numero_reservacion, dpi_cliente) VALUES (?, ?)";

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false); 
            try {
                // Validar Agente y Cupo
                if (!verificarExistencia(conn, sqlAgente, r.getAgente().getIdUsuario())) {
                    throw new Exception("El agente asignado no existe.");
                }
                validarCupo(conn, sqlPaquete, r.getPaquete().getIdPaquete(), r.getCantidadPasajeros());

                // Generar número único
                String numero = generarNumero();

                //  Insertar Reservación
                try (PreparedStatement ps = conn.prepareStatement(sqlInsertarRes)) {
                    ps.setString(1, numero);
                    ps.setString(2, r.getFechaViaje());
                    ps.setInt(3, r.getCantidadPasajeros());
                    ps.setDouble(4, r.getCostoTotal());
                    ps.setString(5, "PENDIENTE");
                    ps.setInt(6, r.getAgente().getIdUsuario());
                    ps.setInt(7, r.getPaquete().getIdPaquete());
                    ps.executeUpdate();
                }

                if (r.getDpisPasajeros() != null) {
                    try (PreparedStatement psPas = conn.prepareStatement(sqlInsertarPasajeros)) {
                        for (String dpi : r.getDpisPasajeros()) {
                            psPas.setString(1, numero);
                            psPas.setString(2, dpi);
                            psPas.executeUpdate();
                        }
                    }
                }

                conn.commit(); 
                return numero;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new Exception("Error de base de datos: " + e.getMessage());
        }
    }

 
    private void validarCupo(Connection conn, String sql, int idPaquete, int nuevosPasajeros) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaquete);
            ps.setInt(2, idPaquete);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int capacidadMax = rs.getInt("capacidad_maxima");
                    int ocupados = rs.getInt("ocupados");

                    if ((ocupados + nuevosPasajeros) > capacidadMax) {
                        throw new Exception("No hay cupo suficiente. Espacios disponibles: " + (capacidadMax - ocupados));
                    }
                } else {
                    throw new Exception("El paquete seleccionado no existe.");
                }
            }
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

    private String generarNumero() {
    
        return "RES-" + (System.currentTimeMillis() % 100000) + "-" + System.nanoTime() % 1000;
    }

    public void actualizarEstado(String numero, String nuevoEstado) throws Exception {

        String sqlVerificar = "SELECT estado FROM reservacion WHERE numero_reservacion = ?";
        String sqlUpdate = "UPDATE reservacion SET estado = ? WHERE numero_reservacion = ?";

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false); 

            try {
             
                String estadoActual = "";
                try (PreparedStatement psVerificar = conn.prepareStatement(sqlVerificar)) {
                    psVerificar.setString(1, numero);
                    try (ResultSet rs = psVerificar.executeQuery()) {
                        if (rs.next()) {
                            estadoActual = rs.getString("estado");
                        } else {
                            throw new Exception("La reservación '" + numero + "' no existe.");
                        }
                    }
                }
                if (estadoActual.equals("CANCELADA") && !nuevoEstado.equals("CANCELADA")) {
                    throw new Exception("No se puede cambiar el estado de una reservación que ya ha sido CANCELADA.");
                }

              
                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                    psUpdate.setString(1, nuevoEstado.toUpperCase());
                    psUpdate.setString(2, numero);

                    int filas = psUpdate.executeUpdate();
                    if (filas == 0) {
                        throw new Exception("No se pudo actualizar la reservación.");
                    }
                }

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new Exception("Error de base de datos al actualizar estado: " + e.getMessage());
        }
    }
}
