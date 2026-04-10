package modelo;

public class Cancelacion {

    private int idCancelacion;
    private String fechaCancelacion;
    private double montoReembolso;
    private double perdidaAgencia;
    private String numeroReservacion;

    public Cancelacion() {
    }

   
    public int getIdCancelacion() {
        return idCancelacion;
    }

    public void setIdCancelacion(int idCancelacion) {
        this.idCancelacion = idCancelacion;
    }

    public String getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(String fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }

    public double getMontoReembolso() {
        return montoReembolso;
    }

    public void setMontoReembolso(double montoReembolso) {
        this.montoReembolso = montoReembolso;
    }

    public double getPerdidaAgencia() {
        return perdidaAgencia;
    }

    public void setPerdidaAgencia(double perdidaAgencia) {
        this.perdidaAgencia = perdidaAgencia;
    }

    public String getNumeroReservacion() {
        return numeroReservacion;
    }

    public void setNumeroReservacion(String numeroReservacion) {
        this.numeroReservacion = numeroReservacion;
    }
}
