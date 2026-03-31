package modelo;

public class Servicio {

    private int idServicio;
    private String nombre;
    private String descripcion;
    private double costoProveedor;
    private Proveedor proveedor;

   
    public Servicio() {
    }

    public Servicio(int idServicio, String nombre, String descripcion, double costoProveedor, Proveedor proveedor) {
        this.idServicio = idServicio;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.costoProveedor = costoProveedor;
        this.proveedor = proveedor;
    }

    
    public int getIdServicio() {
        return idServicio;
    }

    public void setIdServicio(int idServicio) {
        this.idServicio = idServicio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getCostoProveedor() {
        return costoProveedor;
    }

    public void setCostoProveedor(double costoProveedor) {
        this.costoProveedor = costoProveedor;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }
}
