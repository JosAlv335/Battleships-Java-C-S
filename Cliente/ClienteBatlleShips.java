package Cliente;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClienteBatlleShips {
    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;

    // Códigos de señalización de estado de fase de juego
    private final int SIGNAL_LISTO = 99;
    private final int SIGNAL_INICIO_ATAQUE = 100;

    public void conectar(String ip, int puerto) {
        try {
            // Crea un socket de flujo y lo conecta al puerto en la IP definida
            socket = new Socket(ip, puerto);
            
            // Obtención de flujos orientados a byte
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());
            
            System.out.println("Conectado al servidor.");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Envía una señal al servidor indicando que el cliente está listo para iniciar la fase de ataque.
     * Este método no espera una respuesta del servidor y se ejecuta de manera asíncrona para evitar
     * bloquear la interfaz gráfica del cliente.
     * @throws IOException
     */
    public synchronized void enviarSenalListoSinEsperar() throws IOException {
        asegurarConectado();
        salida.writeInt(SIGNAL_LISTO);
        salida.flush();
    }

    /**
     * Envía al servidor las coordenadas del ataque del jugador
     * @param x
     * @param y
     * @throws IOException
     */
    public synchronized void enviarCoordenada(int x, int y) throws IOException {
        asegurarConectado();
        salida.writeInt(x);
        salida.writeInt(y);
        salida.flush();
    }

    /**
     * Envía al servidor la respuesta del cliente después de recibir un ataque.
     * @param codigo El código de respuesta que indica el resultado del ataque (impacto, hundido, victoria, etc.).
     * @throws IOException
     */
    public synchronized void enviarRespuesta(int codigo) throws IOException {
        asegurarConectado();
        salida.writeInt(codigo);
        salida.flush();
    }

    /**
     * Lee la respuesta del servidor después de enviar las coordenadas del ataque.
     * @return El código de respuesta recibido del servidor.
     * @throws IOException
     */
    public int leerRespuesta() throws IOException {
        asegurarConectado();
        return entrada.readInt();
    }

    /**
     * Verifica que el cliente esté conectado al servidor antes de realizar cualquier operación de lectura o escritura.
     * Si no está conectado, lanza una excepción IOException.
     * @throws IOException
     */
    private void asegurarConectado() throws IOException {
        if (entrada == null || salida == null) {
            throw new IOException("El cliente no está conectado al servidor.");
        }
    }

    /**
     * Cierra la conexión con el servidor.
     */
    public void cerrarConexion() {
        try {
            if (entrada != null) {
                entrada.close();
            }
            if (salida != null) {
                salida.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifica si el cliente está conectado al servidor.
     * @return true si el cliente está conectado, false en caso contrario.
     */
    public boolean estaConectado() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

}