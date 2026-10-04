import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor mínimo para probar el protocolo del cliente.
 *
 * El protocolo utiliza enteros de 32 bits en orden de red:
 * <ul>
 *   <li>Colocación: el cliente envía 99 y el servidor responde 100.</li>
 *   <li>Ataque: el cliente envía x y después y; el servidor responde con
 *       el código del resultado.</li>
 *   <li>Después de los códigos 2 y 3 se envía también el ID del barco.</li>
 * </ul>
 */
public class ServidorBattleShips {
    private static final int PUERTO_POR_DEFECTO = 5000;
    private static final int TAMANO_TABLERO = 10;

    private static final int SIGNAL_LISTO = 99;
    private static final int SIGNAL_INICIO_ATAQUE = 100;

    private static final int CODIGO_AGUA = 0;
    private static final int CODIGO_IMPACTO = 1;
    private static final int CODIGO_HUNDIDO = 2;
    private static final int CODIGO_VICTORIA = 3;

    public static void main(String[] args) {
        int puerto = obtenerPuerto(args);

        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("Servidor escuchando en el puerto " + puerto + ".");
            try (Socket cliente = servidor.accept()) {
                System.out.println("Cliente conectado: " + cliente.getRemoteSocketAddress());
                atenderCliente(cliente);
            }
        } catch (IOException e) {
            System.err.println("No se pudo ejecutar el servidor: " + e.getMessage());
        }
    }

    private static int obtenerPuerto(String[] args) {
        if (args.length == 0) {
            return PUERTO_POR_DEFECTO;
        }

        try {
            int puerto = Integer.parseInt(args[0]);
            if (puerto < 1 || puerto > 65535) {
                throw new NumberFormatException();
            }
            return puerto;
        } catch (NumberFormatException e) {
            System.err.println("Puerto inválido. Se usará " + PUERTO_POR_DEFECTO + ".");
            return PUERTO_POR_DEFECTO;
        }
    }

    private static void atenderCliente(Socket cliente) throws IOException {
        DataInputStream entrada = new DataInputStream(cliente.getInputStream());
        DataOutputStream salida = new DataOutputStream(cliente.getOutputStream());

        esperarSenalListo(entrada, salida);

        while (true) {
            int x;
            try {
                x = entrada.readInt();
            } catch (EOFException e) {
                return;
            }

            int y = entrada.readInt();
            validarCoordenada(x, y);

            int codigo = obtenerCodigoDePrueba(x, y);
            salida.writeInt(codigo);

            if (codigo == CODIGO_HUNDIDO || codigo == CODIGO_VICTORIA) {
                salida.writeInt(0);
            }
            salida.flush();

            System.out.println("Tiro [" + x + "," + y + "] -> código " + codigo);
            if (codigo == CODIGO_VICTORIA) {
                return;
            }
        }
    }

    private static void esperarSenalListo(DataInputStream entrada, DataOutputStream salida)
            throws IOException {
        int señal = entrada.readInt();
        if (señal != SIGNAL_LISTO) {
            throw new IOException("Se esperaba el código 99 y se recibió " + señal + ".");
        }

        salida.writeInt(SIGNAL_INICIO_ATAQUE);
        salida.flush();
        System.out.println("Fase de ataque iniciada.");
    }

    /*
     * Coordenadas reservadas para probar cada respuesta:
     * [0,0] agua, [1,1] impacto, [2,2] hundido y [3,3] victoria.
     * Cualquier otra coordenada válida se considera agua.
     */
    private static int obtenerCodigoDePrueba(int x, int y) {
        if (x == 3 && y == 3) {
            return CODIGO_VICTORIA;
        }
        if (x == 2 && y == 2) {
            return CODIGO_HUNDIDO;
        }
        if (x == 1 && y == 1) {
            return CODIGO_IMPACTO;
        }
        return CODIGO_AGUA;
    }

    private static void validarCoordenada(int x, int y) throws IOException {
        if (x < 0 || x >= TAMANO_TABLERO || y < 0 || y >= TAMANO_TABLERO) {
            throw new IOException("Coordenada fuera del tablero: [" + x + "," + y + "].");
        }
    }
}