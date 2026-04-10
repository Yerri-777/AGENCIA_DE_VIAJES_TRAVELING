package dao;

import config.ConexionBD;
import modelo.Reservacion;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import modelo.Cliente;
import modelo.Paquete;

public class ReservacionDAO {

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

   public String crear(Connection conn, Reservacion r) throws Exception {
        String sqlInsertarRes = "INSERT INTO reservacion (numero_reservacion, fecha_creacion, fecha_viaje, cantidad_pasajeros, costo_total, estado, id_usuario, id_paquete) VALUES (?, NOW(), ?, ?, ?, ?, ?, ?)";
        String sqlInsertarPasajeros = "INSERT INTO reservacion_pasajeros (numero_reservacion, dpi_cliente) VALUES (?, ?)";

        // 1. Validar Cupo (Regla de negocio Operación)
        validarCupo(conn, r.getPaquete().getIdPaquete(), r.getCantidadPasajeros());

        String numero = (r.getNumeroReservacion() != null && !r.getNumeroReservacion().isEmpty()) 
                        ? r.getNumeroReservacion() : generarNumero();

        // 2. Insertar Cabecera
        try (PreparedStatement ps = conn.prepareStatement(sqlInsertarRes)) {
            ps.setString(1, numero);
            ps.setString(2, r.getFechaViaje());
            ps.setInt(3, r.getCantidadPasajeros());
            ps.setDouble(4, r.getCostoTotal());
            ps.setString(5, (r.getEstado() != null) ? r.getEstado().toUpperCase() : "PENDIENTE");
            ps.setInt(6, r.getAgente().getIdUsuario());
            ps.setInt(7, r.getPaquete().getIdPaquete());
            ps.executeUpdate();
        }

        // 3. Insertar Pasajeros (Si existen)
        if (r.getPasajeros() != null) {
            try (PreparedStatement psPas = conn.prepareStatement(sqlInsertarPasajeros)) {
                for (Cliente p : r.getPasajeros()) {
                    if (p != null) {
                        psPas.setString(1, numero);
                        psPas.setString(2, p.getDpi());
                        psPas.executeUpdate();
                    }
                }
            }
        }
        return numero;
    }  

    public void crearDesdeCarga(Connection conn, int idPaquete, String nombreUsuarioAgente, String fechaViaje, String dpisConcatenados) throws Exception {
    // 1. Obtener ID del agente por su nombre de usuario
    int idAgente = -1;
    try (PreparedStatement ps = conn.prepareStatement("SELECT id_usuario FROM usuario WHERE nombre_usuario = ?")) {
        ps.setString(1, nombreUsuarioAgente);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) idAgente = rs.getInt(1);
        }
    }
    if (idAgente == -1) throw new Exception("Agente '" + nombreUsuarioAgente + "' no encontrado.");

    // 2. Calcular costo total (Precio del paquete * cantidad de DPIs)
    String[] dpis = dpisConcatenados.split("\\|");
    double precioPaquete = 0;
    try (PreparedStatement ps = conn.prepareStatement("SELECT precio_venta FROM paquete WHERE id_paquete = ?")) {
        ps.setInt(1, idPaquete);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) precioPaquete = rs.getDouble(1);
        }
    }

    // 3. Crear objeto Reservacion para reusar tu lógica de crear()
    Reservacion res = new Reservacion();
    res.setNumeroReservacion(null); // Que genere uno nuevo
    res.setFechaViaje(fechaViaje);
    res.setCantidadPasajeros(dpis.length);
    res.setCostoTotal(precioPaquete * dpis.length);
    res.setEstado("PENDIENTE");
    res.setAgente(new modelo.Usuario(idAgente));
    res.setPaquete(new Paquete(idPaquete));
    
    modelo.Cliente[] pasajeros = new modelo.Cliente[dpis.length];
    for(int i=0; i<dpis.length; i++) {
        pasajeros[i] = new modelo.Cliente();
        pasajeros[i].setDpi(dpis[i].trim());
    }
    res.setPasajeros(pasajeros);

    this.crear(conn, res); // Llama a tu método existente
}

        public void registrarPago(int metodo, double monto, String numRes, int idUsuario) throws Exception {
        String sqlPago = "INSERT INTO pago (monto, metodo_pago, numero_reservacion, id_usuario) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insertar el pago
                try (PreparedStatement ps = conn.prepareStatement(sqlPago)) {
                    ps.setDouble(1, monto);
                    ps.setInt(2, metodo);
                    ps.setString(3, numRes);
                    ps.setInt(4, idUsuario);
                    ps.executeUpdate();
                }

                //  Actualizar el estado de la reservación a CONFIRMADA
                actualizarEstadoEnTransaccion(conn, numRes, "CONFIRMADA");

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void actualizarEstadoEnTransaccion(Connection conn, String numero, String estado) throws SQLException {
        String sql = "UPDATE reservacion SET estado = ? WHERE numero_reservacion = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.toUpperCase());
            ps.setString(2, numero);
            ps.executeUpdate();
        }
    }

    
    
    private void validarCupo(Connection conn, int idPaquete, int nuevosPasajeros) throws Exception {
        String sql = "SELECT p.capacidad_maxima, "
                + "(SELECT COALESCE(SUM(r.cantidad_pasajeros), 0) FROM reservacion r "
                + " WHERE r.id_paquete = ? AND r.estado != 'CANCELADA') as ocupados "
                + "FROM paquete p WHERE p.id_paquete = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaquete);
            ps.setInt(2, idPaquete);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int max = rs.getInt("capacidad_maxima");
                    int ocupados = rs.getInt("ocupados");
                    if ((ocupados + nuevosPasajeros) > max) {
                        throw new Exception("Sobreventa en paquete ID " + idPaquete + ": Solo quedan " + (max - ocupados) + " espacios.");
                    }
                } else {
                    throw new Exception("El paquete con ID " + idPaquete + " no existe.");
                }
            }
        }
    }

    private String generarNumero() {
        return "RES-" + (System.currentTimeMillis() % 1000000);
    }

    
    
    public void actualizarEstado(String numero, String nuevoEstado) throws Exception {
        String sqlVerificar = "SELECT estado FROM reservacion WHERE numero_reservacion = ?";
        String sqlUpdate = "UPDATE reservacion SET estado = ? WHERE numero_reservacion = ?";

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String estadoActual = "";
                try (PreparedStatement ps = conn.prepareStatement(sqlVerificar)) {
                    ps.setString(1, numero);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            estadoActual = rs.getString("estado");
                        } else {
                            throw new Exception("Reservación no encontrada.");
                        }
                    }
                }

                if (estadoActual.equals("CANCELADA")) {
                    throw new Exception("No se puede modificar una reservación CANCELADA.");
                }

                try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                    ps.setString(1, nuevoEstado.toUpperCase());
                    ps.setString(2, numero);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
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

    private boolean verificarExistencia(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

public double[] cancelarConPenalizacion(String numero) throws Exception {
        String sqlGet = "SELECT fecha_viaje, costo_total, estado FROM reservacion WHERE numero_reservacion = ?";
        String sqlUpdate = "UPDATE reservacion SET estado = 'CANCELADA' WHERE numero_reservacion = ?";
        String sqlInsertCancel = "INSERT INTO cancelacion (fecha_cancelacion, monto_reembolso, perdida_agencia, numero_reservacion) VALUES (NOW(), ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double costoTotal = 0;
                LocalDate fechaViaje = null;

                try (PreparedStatement ps = conn.prepareStatement(sqlGet)) {
                    ps.setString(1, numero);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            if (rs.getString("estado").equals("CANCELADA")) throw new Exception("La reservación ya estaba cancelada.");
                            costoTotal = rs.getDouble("costo_total");
                            fechaViaje = LocalDate.parse(rs.getString("fecha_viaje"));
                        } else {
                            throw new Exception("No existe la reservación: " + numero);
                        }
                    }
                }

                // Cálculo de días restantes según especificación
                long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaViaje);
                double reembolso = 0;
                if (dias > 30) reembolso = costoTotal; // 100%
                else if (dias >= 15) reembolso = costoTotal * 0.7; // 70%
                else if (dias >= 7) reembolso = costoTotal * 0.4; // 40%
                else {
                    throw new Exception("No se permite la cancelación: falta menos de 7 días para la fecha de viaje.");
                }

                double perdida = costoTotal - reembolso;

                // 1. Actualizar estado a CANCELADA
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                    ps.setString(1, numero);
                    ps.executeUpdate();
                }

                // 2. Registrar en la tabla de cancelaciones
                try (PreparedStatement ps = conn.prepareStatement(sqlInsertCancel)) {
                    ps.setDouble(1, reembolso);
                    ps.setDouble(2, perdida);
                    ps.setString(3, numero);
                    ps.executeUpdate();
                }

                conn.commit();
                // retornar reembolso y perdida
                return new double[]{reembolso, perdida};
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}