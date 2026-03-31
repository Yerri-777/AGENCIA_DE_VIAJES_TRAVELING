package modelo;

public class PaqueteTuristico {

    private int idPaquete;
    private String nombre;
    private int duracionDias;
    private double precioVenta;
    private int capacidadMaxima;
    private String estado;
    private Destino destino;
private Servicio[] servicios;

public PaqueteTuristico() {
}

public PaqueteTuristico(int idPaquete, String nombre, int duracionDias, double precioVenta, 
                           int capacidadMaxima, String estado, Destino destino) {
  
    this.idPaquete = idPaquete;
    this.nombre = nombre;
    this.duracionDias = duracionDias;
    this.precioVenta = precioVenta;
    this.capacidadMaxima = capacidadMaxima;
    this.estado = estado;
    this.destino = destino;
}

    public Servicio[] getServicios() {
        return servicios;
    }

    public void setServicios(Servicio[] servicios) {
        this.servicios = servicios;
    }

    public int getIdPaquete() {
        return idPaquete;
    }

    public void setIdPaquete(int idPaquete) {
        this.idPaquete = idPaquete;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getDuracionDias() {
        return duracionDias;
    }

    public void setDuracionDias(int duracionDias) {
        this.duracionDias = duracionDias;
    }

    public double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(int capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Destino getDestino() {
        return destino;
    }

    public void setDestino(Destino destino) {
        this.destino = destino;
    }
}