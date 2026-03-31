package modelo;

public class Proveedor {

    private int idProveedor;
    private String nombre;
    private int tipoServicio;
    private String pais;
    private String contacto;

    public Proveedor() {
    }

    public Proveedor(int idProveedor, String nombre, int tipoServicio, String pais, String contacto) {
        this.idProveedor = idProveedor;
        this.nombre = nombre;
        this.tipoServicio = tipoServicio;
        this.pais = pais;
        this.contacto = contacto;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(int tipoServicio) {
        this.tipoServicio = tipoServicio;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }
}
