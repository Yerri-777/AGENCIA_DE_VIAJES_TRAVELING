package modelo;

public class Destino {

    private int idDestino;
    private String nombre;
    private String pais;
    private String descripcion;
    private String clima;
    private String imagenUrl;
    private int PrecioBase; 


    public Destino() {
    }

   
    public Destino(int idDestino, String nombre, String pais, String descripcion, String clima, int precioBase, String imagenUrl) {
        this.idDestino = idDestino;
        this.nombre = nombre;
        this.pais = pais;
        this.descripcion = descripcion;
        this.clima = clima;
        this.PrecioBase = precioBase;
        this.imagenUrl = imagenUrl;
    }

  
    public int getPrecioBase() {
        return PrecioBase;
    }

    public void setPrecioBase(int PrecioBase) {
        this.PrecioBase = PrecioBase;
    }

    public int getIdDestino() {
        return idDestino;
    }

    public void setIdDestino(int idDestino) {
        this.idDestino = idDestino;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getClima() {
        return clima;
    }

    public void setClima(String clima) {
        this.clima = clima;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }
}