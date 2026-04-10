package modelo;

public class Reservacion {

    private String numeroReservacion;
    private String fechaCreacion;
    private String fechaViaje;
    private int cantidadPasajeros;
    private double costoTotal;
    private String estado; 

    private Usuario agente;
    private Paquete paquete;
    private Cliente[] pasajeros;

    public Reservacion() {
    }

    // Lógica para obtener solo los IDs para el DAO
    public String[] getDpisPasajeros() {
        if (pasajeros == null || pasajeros.length == 0) {
            return null;
        }
        String[] dpis = new String[pasajeros.length];
        for (int i = 0; i < pasajeros.length; i++) {
            if (pasajeros[i] != null) {
                dpis[i] = pasajeros[i].getDpi();
            }
        }
        return dpis;
    }

    
    public String getNumeroReservacion() {
        return numeroReservacion;
    }

    public void setNumeroReservacion(String numeroReservacion) {
        this.numeroReservacion = numeroReservacion;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getFechaViaje() {
        return fechaViaje;
    }

    public void setFechaViaje(String fechaViaje) {
        this.fechaViaje = fechaViaje;
    }

    public int getCantidadPasajeros() {
        return cantidadPasajeros;
    }

    public void setCantidadPasajeros(int cantidadPasajeros) {
        this.cantidadPasajeros = cantidadPasajeros;
    }

    public double getCostoTotal() {
        return costoTotal;
    }

    public void setCostoTotal(double costoTotal) {
        this.costoTotal = costoTotal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Usuario getAgente() {
        return agente;
    }

    public void setAgente(Usuario agente) {
        this.agente = agente;
    }

    public Paquete getPaquete() {
        return paquete;
    }

    public void setPaquete(Paquete paquete) {
        this.paquete = paquete;
    }

    public Cliente[] getPasajeros() {
        return pasajeros;
    }

    public void setPasajeros(Cliente[] pasajeros) {
        this.pasajeros = pasajeros;
    }
}
