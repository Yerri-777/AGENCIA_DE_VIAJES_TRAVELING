package modelo;

public class Usuario {

    private int idUsuario;
    private String nombreUsuario;
    private String password;
    private int estado;
    private Rol rol;

    public Usuario() {
    }

    // Constructor para asociaciones rápidas (Carga de Datos)
    public Usuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Usuario(int idUsuario, String nombreUsuario, String password, int estado, Rol rol) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.password = password;
        this.estado = estado;
        this.rol = rol;
    }

    // Métodos de utilidad para permisos en el Front/Servlets
    public boolean esAtencion() {
        return rol != null && rol.getIdRol() == 1;
    }

    public boolean esOperaciones() {
        return rol != null && rol.getIdRol() == 2;
    }

    public boolean esAdmin() {
        return rol != null && rol.getIdRol() == 3;
    }

    // Getters y Setters
    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }
}
