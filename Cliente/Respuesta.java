import java.io.Serializable;

public class Respuesta implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int codigoEstado;
    public int idBarco; // -1 si no aplica
    
    public Respuesta(int codigoEstado, int idBarco) {
        this.codigoEstado = codigoEstado;
        this.idBarco = idBarco;
    }
}