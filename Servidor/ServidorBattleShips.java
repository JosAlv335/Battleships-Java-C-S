package Servidor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Servidor de Batalla Naval.
 * Gestiona la conexión de red, la ubicación aleatoria de la flota enemiga
 * y la lógica de turnos (PC vs Jugador).
 */
public class ServidorBattleShips {
    // Configuración de red y dimensiones del juego
    private static final int PUERTO = 5000;
    private static final int TAMANO_TABLERO = 10;
    
    // Códigos de señalización para el protocolo de sincronización
    private static final int SIGNAL_LISTO = 99;
    private static final int SIGNAL_INICIO_ATAQUE = 100;
    
    // Códigos de resultado de disparos
    private static final int CODIGO_AGUA = 0;
    private static final int CODIGO_IMPACTO = 1;
    private static final int CODIGO_HUNDIDO = 2;
    private static final int CODIGO_VICTORIA = 3;

    // Tamaños: 5, 4, 4, 3, 2, 2
    private static final int[] DIMENSIONES_BARCOS = {5, 4, 4, 3, 2, 2};
    
    // Estado de la partida del Servidor
    private int[][] tablero = new int[TAMANO_TABLERO][TAMANO_TABLERO];
    private int[] hpBarcos = new int[DIMENSIONES_BARCOS.length];
    
    // Memoria de los disparos realizados por el Servidor para no repetir tiros
    private boolean[][] disparosIA = new boolean[TAMANO_TABLERO][TAMANO_TABLERO];
    private Random rand = new Random();
    
    private int ultimoIdHundido = -1;

    public static void main(String[] args) {
        new ServidorBattleShips().iniciar();
    }

    /**
     * Levanta el servidor en el puerto especificado y acepta clientes de forma infinita.
     * Por cada cliente conectado, inicializa una nueva partida.
     */
    public void iniciar() {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("[+] Comandancia central iniciada en puerto " + PUERTO);
            while (true) {
                try (Socket cliente = servidor.accept()) {
                    System.out.println("[+] Cliente detectado: " + cliente.getRemoteSocketAddress());
                    reiniciarEstadoJuego();
                    colocarFlota();
                    manejarCicloJuego(cliente);
                } catch (IOException e) {
                    System.err.println("[-] Error de red con el cliente.");
                }
            }
        } catch (IOException e) {
            System.err.println("[-] No se pudo iniciar el servidor en el puerto " + PUERTO);
        }
    }

    /**
     * Limpia la matriz del tablero y restablece los puntos de vida de la flota.
     */
    private void reiniciarEstadoJuego() {
        for (int i = 0; i < TAMANO_TABLERO; i++) {
            for (int j = 0; j < TAMANO_TABLERO; j++) {
                tablero[i][j] = -1; // -1 representa Agua limpia
                disparosIA[i][j] = false;
            }
        }
        // Restaura los "HP" (Hit Points) de cada barco basados en su tamaño
        for (int i = 0; i < DIMENSIONES_BARCOS.length; i++) hpBarcos[i] = DIMENSIONES_BARCOS[i];
    }

    /**
     * Distribuye de manera aleatoria y automática los barcos del servidor.
     */
    private void colocarFlota() {
        for (int id = 0; id < DIMENSIONES_BARCOS.length; id++) {
            boolean colocado = false;
            while (!colocado) {
                int x = rand.nextInt(TAMANO_TABLERO);
                int y = rand.nextInt(TAMANO_TABLERO);
                boolean horizontal = rand.nextBoolean();
                
                // Si el espacio es válido, inscribe el ID del barco en la matriz
                if (esPosicionValidaParaBarco(x, y, DIMENSIONES_BARCOS[id], horizontal)) {
                    if (horizontal) {
                        for (int j = y; j < y + DIMENSIONES_BARCOS[id]; j++) tablero[x][j] = id;
                    } else {
                        for (int i = x; i < x + DIMENSIONES_BARCOS[id]; i++) tablero[i][y] = id;
                    }
                    colocado = true;
                }
            }
        }
        System.out.println("[+] Flota enemiga posicionada.");
    }

    /**
     * Valida que un barco no se salga de los límites del tablero ni choque con otro ya colocado.
     */
    private boolean esPosicionValidaParaBarco(int x, int y, int tamano, boolean horizontal) {
        if (horizontal) {
            if (y + tamano > TAMANO_TABLERO) return false;
            for (int j = y; j < y + tamano; j++) if (tablero[x][j] != -1) return false;
        } else {
            if (x + tamano > TAMANO_TABLERO) return false;
            for (int i = x; i < x + tamano; i++) if (tablero[i][y] != -1) return false;
        }
        return true;
    }

    /**
     * Bucle principal de la partida. Mantiene la sincronización de lectura y escritura
     * por turnos usando sockets bloqueantes.
     */
    private void manejarCicloJuego(Socket cliente) throws IOException {
        DataInputStream entrada = new DataInputStream(cliente.getInputStream());
        DataOutputStream salida = new DataOutputStream(cliente.getOutputStream());

        // Espera a que el cliente termine de posicionar su flota
        if (entrada.readInt() == SIGNAL_LISTO) {
            salida.writeInt(SIGNAL_INICIO_ATAQUE);
            salida.flush();
        }

        boolean turnoJugador = true;

        while (true) {
            try {
                if (turnoJugador) {
                    // 1. TURNO DEL CLIENTE: Lee ataque, evalúa y responde
                    int x = entrada.readInt();
                    int y = entrada.readInt();
                    
                    int resultado = procesarTiroJugador(x, y);
                    salida.writeInt(resultado);
                    if (resultado == CODIGO_HUNDIDO || resultado == CODIGO_VICTORIA) salida.writeInt(ultimoIdHundido);
                    salida.flush();

                    if (resultado == CODIGO_VICTORIA) break; // Fin del juego (Gana el cliente)
                    if (resultado == CODIGO_AGUA) turnoJugador = false; // El jugador falla, pierde el turno
                } else {
                    // 2. TURNO DEL SERVIDOR: Pausa simulada, dispara y lee el daño causado
                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                    
                    int[] tiroIA = generarTiroIA();
                    salida.writeInt(tiroIA[0]);
                    salida.writeInt(tiroIA[1]);
                    salida.flush();
                    
                    int respuestaCliente = entrada.readInt();
                    if (respuestaCliente == CODIGO_HUNDIDO || respuestaCliente == CODIGO_VICTORIA) {
                        entrada.readInt(); // Extrae el ID del barco hundido del flujo (para limpiar el buffer)
                    }

                    if (respuestaCliente == CODIGO_VICTORIA) break; // Fin del juego (Gana el servidor)
                    if (respuestaCliente == CODIGO_AGUA) turnoJugador = true; // El servidor  falla, regresa el turno
                }
            } catch (EOFException e) {
                System.out.println("[-] El cliente cerró la conexión.");
                break;
            }
        }
    }

    /**
     * Recibe una coordenada atacada, verifica qué hay en esa celda y descuenta vida al barco si aplica.
     */
    private int procesarTiroJugador(int x, int y) {
        int idCelda = tablero[x][y];
        // Si cae en agua o en un barco ya atacado previamente (-2)
        if (idCelda == -1 || idCelda == -2) return CODIGO_AGUA;
        
        // Impacto válido
        int idBarco = idCelda;
        hpBarcos[idBarco]--;
        tablero[x][y] = -2; // Marca la casilla como destruida
        
        // Verifica si la flota completa ha sido aniquilada
        boolean todosHundidos = true;
        for (int hp : hpBarcos) { if (hp > 0) { todosHundidos = false; break; } }
        
        if (todosHundidos) { ultimoIdHundido = idBarco; return CODIGO_VICTORIA; }
        if (hpBarcos[idBarco] == 0) { ultimoIdHundido = idBarco; return CODIGO_HUNDIDO; }
        return CODIGO_IMPACTO;
    }

    /**
     * Genera un par de coordenadas (X, Y) aleatorias asegurando que no se repitan disparos anteriores.
     */
    private int[] generarTiroIA() {
        int x, y;
        do {
            x = rand.nextInt(TAMANO_TABLERO);
            y = rand.nextInt(TAMANO_TABLERO);
        } while (disparosIA[x][y]); // Repite el ciclo si esa coordenada ya fue disparada
        
        disparosIA[x][y] = true; // Registra el disparo en la memoria
        return new int[]{x, y};
    }
}