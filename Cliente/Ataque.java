import java.io.Serializable;

public class Ataque implements Serializable {
    // Es buena práctica definir este ID para evitar errores de versión entre cliente y servidor
    private static final long serialVersionUID = 1L; 
    
    public int x;
    public int y;
    
    public Ataque(int x, int y) {
        this.x = x;
        this.y = y;
    }
}