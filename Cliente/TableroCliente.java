import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class TableroCliente extends JFrame {
    private static final int TAMANO = 10;
    private static final int CODIGO_AGUA = 0;
    private static final int CODIGO_IMPACTO = 1;
    private static final int CODIGO_HUNDIDO = 2;
    private static final int CODIGO_VICTORIA = 3;
    private static final int SIGNAL_INICIO_ATAQUE = 100;

    private final JButton[][] casillas = new JButton[TAMANO][TAMANO];
    private final int[][] tableroLogico = new int[TAMANO][TAMANO];
    private final int[][] dimensiones = {{2, 5}, {1, 3}, {1, 4}, {1, 5}, {1, 2}, {1, 2}};
    private final boolean[] barcoColocado = new boolean[dimensiones.length];
    private final int[] barcoFila = new int[dimensiones.length];
    private final int[] barcoColumna = new int[dimensiones.length];
    private final boolean[] barcoHorizontal = new boolean[dimensiones.length];
    private final ClienteBatlleShips redCliente;
    private final Color COLOR_AGUA = new Color(0, 150, 255);
    private final Color COLOR_VALIDO = Color.GREEN;
    private final Color COLOR_INVALIDO = Color.GRAY;
    private final Color COLOR_BARCO = Color.DARK_GRAY;
    private final Color COLOR_FUEGO = Color.RED;

    private JButton[][] radar;
    private int barcoActivoId;
    private boolean esHorizontal = true;
    private int barcosColocados;
    private JLabel[] estadoBarcos;
    private JPanel panelFlota;
    private JButton botonListo;
    private boolean radarHabilitado;
    private int ultimoAtaqueX = -1;
    private int ultimoAtaqueY = -1;

    // Control de salud para los 6 barcos
    private final int[] hpBarcos = new int[dimensiones.length];
    private boolean turnoJugador = true;
    private int idBarcoHundidoEnTurno = -1;

    public TableroCliente() {
        this(null);
    }

    /**
     * Constructor de la clase TableroCliente.
     * @param redCliente Instancia de ClienteBatlleShips para manejar la comunicación con el servidor.
     */
    public TableroCliente(ClienteBatlleShips redCliente) {
        this.redCliente = redCliente;
        setTitle("Batalla Naval - Colocación");
        setSize(850, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        for (int id = 0; id < dimensiones.length; id++) {
            barcoFila[id] = -1;
            barcoColumna[id] = -1;
        }
        construirTableroPrincipal();
        construirPanelFlota();
        seleccionarBarco(0);
    }

    /**
     * Construye el tablero principal del juego.
     */
    private void construirTableroPrincipal() {
        JPanel panelTablero = new JPanel(new GridLayout(TAMANO, TAMANO));
        for (int x = 0; x < TAMANO; x++) {
            for (int y = 0; y < TAMANO; y++) {
                JButton casilla = new JButton();
                casilla.setBackground(COLOR_AGUA);
                casilla.setOpaque(true);
                casilla.setBorderPainted(true);
                casillas[x][y] = casilla;
                final int fila = x;
                final int columna = y;
                casilla.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        pintarProyeccion(fila, columna);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        limpiarProyeccion(fila, columna);
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            limpiarProyeccion(fila, columna);
                            esHorizontal = !esHorizontal;
                            pintarProyeccion(fila, columna);
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            fijarBarco(fila, columna);
                        }
                    }
                });
                panelTablero.add(casilla);
            }
        }
        add(panelTablero, BorderLayout.CENTER);
    }

    private void construirPanelFlota() {
        panelFlota = new JPanel(new GridLayout(dimensiones.length + 1, 1, 4, 4));
        panelFlota.setBorder(BorderFactory.createTitledBorder("Flota"));
        estadoBarcos = new JLabel[dimensiones.length];
        for (int id = 0; id < dimensiones.length; id++) {
            estadoBarcos[id] = new JLabel();
            panelFlota.add(estadoBarcos[id]);
            actualizarEstadoBarco(id);
        }
        botonListo = new JButton("Listo");
        botonListo.setEnabled(false);
        botonListo.addActionListener(e -> iniciarFaseAtaqueVisual());
        panelFlota.add(botonListo);
        add(panelFlota, BorderLayout.EAST);
        configurarAtajosDeBarcos();
    }

    private void configurarAtajosDeBarcos() {
        for (int id = 0; id < dimensiones.length; id++) {
            final int barcoId = id;
            String accion = "seleccionar-barco-" + id;
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(Character.forDigit(id, 10)), accion);
            getRootPane().getActionMap().put(accion, new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    seleccionarBarco(barcoId);
                }
            });
        }
    }

    private void actualizarEstadoBarco(int id) {
        int filas = dimensiones[id][0];
        int columnas = dimensiones[id][1];
        String estado = barcoColocado[id] ? " (colocado)" : " (pendiente)";
        estadoBarcos[id].setText(id + ": " + filas + "x" + columnas + estado);
    }

    private void seleccionarBarco(int id) {
        if (id < 0 || id >= dimensiones.length || radar != null) {
            return;
        }
        limpiarTodasLasProyecciones();
        barcoActivoId = id;
        esHorizontal = true;
    }

    private void limpiarTodasLasProyecciones() {
        for (int fila = 0; fila < TAMANO; fila++) {
            for (int columna = 0; columna < TAMANO; columna++) {
                if (tableroLogico[fila][columna] == 0) {
                    casillas[fila][columna].setBackground(COLOR_AGUA);
                }
            }
        }
    }

    private int[] dimensionesBarcoActivo() {
        int filas = dimensiones[barcoActivoId][0];
        int columnas = dimensiones[barcoActivoId][1];
        return esHorizontal ? new int[] {filas, columnas} : new int[] {columnas, filas};
    }

    private boolean esPosicionValida(int x, int y) {
        int[] tamano = dimensionesBarcoActivo();
        if (x < 0 || y < 0 || x + tamano[0] > TAMANO || y + tamano[1] > TAMANO) {
            return false;
        }
        for (int fila = x; fila < x + tamano[0]; fila++) {
            for (int columna = y; columna < y + tamano[1]; columna++) {
                int valor = tableroLogico[fila][columna];
                if (valor != 0 && valor != barcoActivoId + 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private void pintarProyeccion(int x, int y) {
        if (barcosColocados == dimensiones.length) {
            return;
        }
        int[] tamano = dimensionesBarcoActivo();
        Color color = esPosicionValida(x, y) ? COLOR_VALIDO : COLOR_INVALIDO;
        for (int fila = x; fila < x + tamano[0] && fila < TAMANO; fila++) {
            for (int columna = y; columna < y + tamano[1] && columna < TAMANO; columna++) {
                if (fila >= 0 && columna >= 0 && tableroLogico[fila][columna] == 0) {
                    casillas[fila][columna].setBackground(color);
                }
            }
        }
    }

    private void limpiarProyeccion(int x, int y) {
        if (barcosColocados == dimensiones.length) {
            return;
        }
        int[] tamano = dimensionesBarcoActivo();
        for (int fila = x; fila < x + tamano[0] && fila < TAMANO; fila++) {
            for (int columna = y; columna < y + tamano[1] && columna < TAMANO; columna++) {
                if (fila >= 0 && columna >= 0 && tableroLogico[fila][columna] == 0) {
                    casillas[fila][columna].setBackground(COLOR_AGUA);
                }
            }
        }
    }

    private void fijarBarco(int x, int y) {
        if (!esPosicionValida(x, y)) {
            return;
        }
        if (barcoColocado[barcoActivoId]) {
            limpiarBarcoAnterior(barcoActivoId);
        } else {
            barcosColocados++;
            hpBarcos[barcoActivoId] = dimensionesBarcoActivo()[0] * dimensionesBarcoActivo()[1];
        }
        int[] tamano = dimensionesBarcoActivo();
        for (int fila = x; fila < x + tamano[0]; fila++) {
            for (int columna = y; columna < y + tamano[1]; columna++) {
                tableroLogico[fila][columna] = barcoActivoId + 1;
                casillas[fila][columna].setBackground(COLOR_BARCO);
            }
        }
        barcoColocado[barcoActivoId] = true;
        barcoFila[barcoActivoId] = x;
        barcoColumna[barcoActivoId] = y;
        barcoHorizontal[barcoActivoId] = esHorizontal;
        actualizarEstadoBarco(barcoActivoId);
        botonListo.setEnabled(barcosColocados == dimensiones.length);
    }

    private void limpiarBarcoAnterior(int id) {
        int filas = dimensiones[id][0];
        int columnas = dimensiones[id][1];
        if (!barcoHorizontal[id]) {
            int intercambio = filas;
            filas = columnas;
            columnas = intercambio;
        }
        for (int fila = barcoFila[id]; fila < barcoFila[id] + filas; fila++) {
            for (int columna = barcoColumna[id]; columna < barcoColumna[id] + columnas; columna++) {
                tableroLogico[fila][columna] = 0;
                casillas[fila][columna].setBackground(COLOR_AGUA);
            }
        }
    }

    private void iniciarFaseAtaqueVisual() {
        if (barcosColocados != dimensiones.length || radar != null) {
            return;
        }
        for (JButton[] fila : casillas) {
            for (JButton casilla : fila) {
                for (java.awt.event.MouseListener listener : casilla.getMouseListeners()) {
                    casilla.removeMouseListener(listener);
                }
            }
        }
        botonListo.setEnabled(false);
        if (panelFlota != null) {
            remove(panelFlota);
        }
        if (redCliente == null) {
            construirRadar();
            return;
        }
        try {
            redCliente.enviarSenalListoSinEsperar();
            new ReceptorRespuestas().start();
        } catch (IOException e) {
            mostrarError("No se pudo iniciar la fase de ataque", e);
        }
    }

    private void construirRadar() {
        if (radar != null) {
            return;
        }
        radar = new JButton[TAMANO][TAMANO];
        JPanel panelRadar = new JPanel(new GridLayout(TAMANO, TAMANO));
        for (int x = 0; x < TAMANO; x++) {
            for (int y = 0; y < TAMANO; y++) {
                JButton casilla = new JButton();
                casilla.setBackground(COLOR_AGUA);
                casilla.setOpaque(true);
                final int fila = x;
                final int columna = y;
                casilla.addActionListener(e -> enviarAtaque(fila, columna));
                radar[x][y] = casilla;
                panelRadar.add(casilla);
            }
        }
        add(panelRadar, BorderLayout.EAST);
        pack();
        radarHabilitado = true;
    }

    /**
     * Envía las coordenadas del ataque al servidor si el radar está habilitado
     * y si la casilla correspondiente está habilitada.
     * @param x
     * @param y
     */
    private void enviarAtaque(int x, int y) {
        if (!radarHabilitado || redCliente == null || !radar[x][y].isEnabled()) {
            return;
        }
        try {
            ultimoAtaqueX = x;
            ultimoAtaqueY = y;
            radar[x][y].setEnabled(false);
            radarHabilitado = false;
            redCliente.enviarAtaque(x, y);
        } catch (IOException e) {
            radar[x][y].setEnabled(true);
            radarHabilitado = true;
            mostrarError("No se pudo enviar el ataque", e);
        }
    }

    private void procesarRespuesta(int codigo) {
        if (ultimoAtaqueX < 0 || radar == null) {
            return;
        }
        switch (codigo) {
            case CODIGO_AGUA:
                radar[ultimoAtaqueX][ultimoAtaqueY].setBackground(COLOR_AGUA);
                radarHabilitado = false;
                break;
            case CODIGO_IMPACTO:
                radar[ultimoAtaqueX][ultimoAtaqueY].setBackground(COLOR_FUEGO);
                radarHabilitado = true;
                break;
            case CODIGO_HUNDIDO:
            case CODIGO_VICTORIA:
                radar[ultimoAtaqueX][ultimoAtaqueY].setBackground(COLOR_FUEGO);
                radarHabilitado = codigo != CODIGO_VICTORIA;
                break;
            default:
                break;
        }
        actualizarEstadoRadar();
    }

    private void actualizarEstadoRadar() {
        for (JButton[] fila : radar) {
            for (JButton casilla : fila) {
                casilla.setEnabled(radarHabilitado && casilla.getBackground() == COLOR_AGUA);
            }
        }
    }

    private void renderizarBarcoHundido(int id) {
        if (ultimoAtaqueX >= 0 && radar != null && id >= 0) {
            radar[ultimoAtaqueX][ultimoAtaqueY].setToolTipText("Barco hundido: ID " + id);
        }
    }

    private void mostrarError(String mensaje, IOException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, mensaje + ": " + e.getMessage(),
                "Error de red", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Clase interna que representa el hilo responsable de recibir y procesar las respuestas del servidor.
     */
    private final class ReceptorRespuestas extends Thread {
        @Override
        public void run() {
            try {
                // 1. Fase de espera: Bloqueo real hasta que el Servidor esté listo
                int senal = redCliente.leerRespuesta();
                if (senal == SIGNAL_INICIO_ATAQUE) {
                    construirRadarEnEDT();
                }

                // 2. Bucle de la máquina de estados
                while (!isInterrupted()) {
                    // Turno del jugador
                    if (turnoJugador) {
                        // Esperar la respuesta del ataque
                        int codigo = redCliente.leerRespuesta();
                        
                        // Si el ataque hunde un barco o si gana la partida, se renderiza el barco hundido
                        if (codigo == CODIGO_HUNDIDO || codigo == CODIGO_VICTORIA) {
                            int idBarco = redCliente.leerRespuesta();
                            SwingUtilities.invokeLater(() -> renderizarBarcoHundido(idBarco));
                        }
                        
                        // Procesar la respuesta del ataque
                        final int resultado = codigo;
                        SwingUtilities.invokeLater(() -> procesarRespuesta(resultado));

                        // Si fallamos, cedemos el turno
                        if (codigo == CODIGO_AGUA) {
                            turnoJugador = false; 
                        }
                        
                    } else {
                        // Turno del CPU: Esperamos sus coordenadas X e Y
                        int xEnemigo = redCliente.leerRespuesta();
                        int yEnemigo = redCliente.leerRespuesta();

                        // Evaluamos el daño visual y lógico en nuestro tablero principal
                        int respuestaLocal = evaluarTiroEnemigo(xEnemigo, yEnemigo);
                        
                        // Respondemos al servidor el resultado del ataque del CPU
                        redCliente.enviarRespuesta(respuestaLocal); 

                        // Si el ataque del CPU hunde un barco o gana la partida, enviamos el ID del barco hundido
                        if (respuestaLocal == CODIGO_HUNDIDO || respuestaLocal == CODIGO_VICTORIA) {
                            // Recuperamos el ID del barco hundido y lo enviamos al servidor
                            redCliente.enviarRespuesta(recuperarIdHundido(xEnemigo, yEnemigo)); 
                        }

                        // Si el CPU falló, recuperamos nuestro turno y habilitamos nuestro radar
                        if (respuestaLocal == CODIGO_AGUA) {
                            turnoJugador = true;
                            SwingUtilities.invokeLater(TableroCliente.this::actualizarEstadoRadar);
                        }
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> mostrarError("La conexión con el servidor terminó", e));
            } catch (ClassNotFoundException ex) {
                System.getLogger(TableroCliente.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }

        private void construirRadarEnEDT() {
            try {
                SwingUtilities.invokeAndWait(TableroCliente.this::construirRadar);
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo crear el radar", e);
            }
        }
    }

    private int evaluarTiroEnemigo(int x, int y) {
        int idCelda = tableroLogico[x][y];
        
        
        // Si es 0 (Agua) o -1 (Ya atacado previamente)
        if (idCelda == 0 || idCelda == -1) {
            casillas[x][y].setBackground(Color.WHITE); // Pintamos el agua de blanco para notar el fallo del CPU
            return CODIGO_AGUA;
        }
        
        // Si hay un barco intacto
        int idBarco = idCelda - 1;
        idBarcoHundidoEnTurno = idBarco; // Guarda el ID en memoria
        hpBarcos[idBarco]--;
        tableroLogico[x][y] = -1; // Marcamos la zona como destruida
        casillas[x][y].setBackground(COLOR_FUEGO); // Pintamos el impacto en nuestro barco

        // Verificamos condición de victoria del CPU
        boolean flotaDestruida = true;
        for (int hp : hpBarcos) {
            if (hp > 0) flotaDestruida = false;
        }
        if (flotaDestruida) return CODIGO_VICTORIA;
        
        // Verificamos si solo se hundió ese barco o fue un impacto simple
        if (hpBarcos[idBarco] == 0) return CODIGO_HUNDIDO;
        return CODIGO_IMPACTO;
    }

    /**
     * Recupera el ID del barco hundido en el turno actual.
     * @param xEnemigo La coordenada X del ataque enemigo.
     * @param yEnemigo La coordenada Y del ataque enemigo.
     * @return El ID del barco hundido.
     */
    private int recuperarIdHundido(int xEnemigo, int yEnemigo) {
        return idBarcoHundidoEnTurno;
    }
}
