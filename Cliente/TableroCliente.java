package Cliente;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

// Clase principal de la interfaz gráfica para el tablero del jugador en la Batalla Naval
public class TableroCliente extends JFrame {
    // Configuraciones generales del tablero
    private static final int TAMANO = 10;
    
    // Códigos de respuesta compartidos con El servidor para el protocolo de red
    private static final int CODIGO_AGUA = 0;
    private static final int CODIGO_IMPACTO = 1;
    private static final int CODIGO_HUNDIDO = 2;
    private static final int CODIGO_VICTORIA = 3;
    private static final int SIGNAL_INICIO_ATAQUE = 100;

    // Paleta de colores para los distintos estados de las casillas
    private final Color COLOR_AGUA_PROPIA = Color.decode("#0F172A"); 
    private final Color COLOR_AGUA_RADAR = Color.decode("#1E293B"); 
    private final Color COLOR_FALLO = Color.decode("#38BDF8"); 
    private final Color COLOR_BARCO = Color.decode("#475569"); 
    private final Color COLOR_VALIDO = Color.decode("#22C55E"); 
    private final Color COLOR_INVALIDO = Color.decode("#EF4444"); 
    private final Color COLOR_FUEGO = Color.decode("#F97316"); 
    private final Color COLOR_BG_PANEL = Color.decode("#F8FAFC"); 

    // Matrices visuales y lógicas de la cuadrícula del jugador
    private final JButton[][] casillas = new JButton[TAMANO][TAMANO];
    private final int[][] tableroLogico = new int[TAMANO][TAMANO];
    
    // Tamaños de la flota: Portaaviones(5), Acorazados(4), Cruceros(3), Submarinos/Destructores(2)
    private final int[][] dimensiones = {{5, 1}, {4, 1}, {4, 1}, {3, 1}, {2, 1}, {2, 1}}; 
    
    // Registro de cuáles barcos ya han sido fijados en el tablero
    private final boolean[] barcoColocado = new boolean[dimensiones.length];
    
    // Conexión de red con El servidor
    private final ClienteBatlleShips redCliente;
    
    // Elementos dinámicos de la interfaz y control de estado
    private JButton[][] radar;
    private int barcoActivoId;
    private boolean esHorizontal = true;
    private int barcosColocados;
    
    private JPanel panelDerechoCentral;
    private JLabel lblEstadoGeneral;
    private JButton botonListo;
    private JLabel[] estadoBarcos;

    // Control de turnos y selección en el radar
    private boolean radarHabilitado;
    private int ultimoAtaqueX = -1;
    private int ultimoAtaqueY = -1;

    // Registro de los puntos de vida de cada barco propio para calcular los hundimientos
    private final int[] hpBarcos = new int[dimensiones.length];
    private boolean turnoJugador = true;

    // Constructor: Configura la ventana principal y ensambla los paneles
    public TableroCliente(ClienteBatlleShips redCliente) {
        this.redCliente = redCliente;
        setTitle("Batalla Naval - Commander Interface");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(COLOR_BG_PANEL);
        setLayout(new BorderLayout(15, 15));
        
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Construcción de la interfaz dividida en tres áreas
        construirBarraEstado();
        construirTableroPrincipal();
        construirPanelFlota();
        
        // Seleccionamos el primer barco por defecto al iniciar
        seleccionarBarco(0);
    }

    // Crea la barra superior que informa al jugador sobre la fase o el turno actual
    private void construirBarraEstado() {
        lblEstadoGeneral = new JLabel("FASE DE PREPARACIÓN: Coloca tu flota estratégicamente.", SwingConstants.CENTER);
        lblEstadoGeneral.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblEstadoGeneral.setOpaque(true);
        lblEstadoGeneral.setBackground(COLOR_AGUA_PROPIA);
        lblEstadoGeneral.setForeground(Color.WHITE);
        lblEstadoGeneral.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(lblEstadoGeneral, BorderLayout.NORTH);
    }

    // Genera la cuadrícula de 10x10 donde el jugador posiciona sus propios barcos
    private void construirTableroPrincipal() {
        JPanel panelIzquierdo = new JPanel(new BorderLayout(0, 10));
        panelIzquierdo.setBackground(COLOR_BG_PANEL);
        
        JLabel titulo = new JLabel("TU FLOTA", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        panelIzquierdo.add(titulo, BorderLayout.NORTH);

        JPanel cuadricula = new JPanel(new GridLayout(TAMANO, TAMANO, 1, 1));
        cuadricula.setBackground(Color.BLACK); 
        cuadricula.setPreferredSize(new Dimension(450, 450));

        // Construcción interactiva de cada botón (casilla)
        for (int x = 0; x < TAMANO; x++) {
            for (int y = 0; y < TAMANO; y++) {
                JButton casilla = new JButton();
                casilla.setBackground(COLOR_AGUA_PROPIA);
                casilla.setFocusPainted(false);
                casilla.setBorder(BorderFactory.createEmptyBorder());
                casillas[x][y] = casilla;
                final int fila = x;
                final int columna = y;
                
                // Eventos del ratón para mostrar siluetas, rotar y colocar barcos
                casilla.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { pintarProyeccion(fila, columna); }
                    @Override public void mouseExited(MouseEvent e) { limpiarProyeccion(fila, columna); }
                    @Override public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            // Clic derecho: Rota el barco actual
                            limpiarProyeccion(fila, columna);
                            esHorizontal = !esHorizontal;
                            pintarProyeccion(fila, columna);
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            // Clic izquierdo: Fija el barco en el tablero
                            fijarBarco(fila, columna);
                        }
                    }
                });
                cuadricula.add(casilla);
            }
        }
        panelIzquierdo.add(cuadricula, BorderLayout.CENTER);
        add(panelIzquierdo, BorderLayout.WEST);
    }

    // Construye el panel derecho inicial con la lista de barcos disponibles para colocar
    private void construirPanelFlota() {
        panelDerechoCentral = new JPanel(new BorderLayout(0, 10));
        panelDerechoCentral.setBackground(COLOR_BG_PANEL);

        JLabel titulo = new JLabel("ARMERÍA", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        panelDerechoCentral.add(titulo, BorderLayout.NORTH);

        JPanel panelFlota = new JPanel(new GridLayout(dimensiones.length + 1, 1, 5, 5));
        panelFlota.setBackground(Color.WHITE);
        TitledBorder border = BorderFactory.createTitledBorder("Selecciona tus navíos (Clic derecho para rotar)");
        border.setTitleFont(new Font("SansSerif", Font.ITALIC, 12));
        panelFlota.setBorder(border);

        // Lista de estado visual para cada barco de la flota
        estadoBarcos = new JLabel[dimensiones.length];
        for (int id = 0; id < dimensiones.length; id++) {
            estadoBarcos[id] = new JLabel();
            estadoBarcos[id].setFont(new Font("SansSerif", Font.PLAIN, 14));
            panelFlota.add(estadoBarcos[id]);
            actualizarEstadoBarco(id);
        }

        // Botón bloqueado hasta que se posicione toda la flota
        botonListo = new JButton("INICIAR BATALLA");
        botonListo.setFont(new Font("SansSerif", Font.BOLD, 14));
        botonListo.setBackground(COLOR_VALIDO);
        botonListo.setForeground(Color.WHITE);
        botonListo.setEnabled(false);
        botonListo.addActionListener(e -> iniciarFaseAtaqueVisual());
        panelFlota.add(botonListo);

        panelDerechoCentral.add(panelFlota, BorderLayout.CENTER);
        add(panelDerechoCentral, BorderLayout.EAST);
        
        // Atajos de teclado (números del 1 al 6) para seleccionar rápidamente un barco
        for (int id = 0; id < dimensiones.length; id++) {
            final int barcoId = id;
            String accion = "sel-" + id;
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(Character.forDigit(id + 1, 10)), accion);
            getRootPane().getActionMap().put(accion, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { seleccionarBarco(barcoId); }
            });
        }
    }

    // Utilidad para modificar rápidamente el mensaje y color de la barra superior
    private void cambiarMensajeEstado(String msj, Color bgColor) {
        lblEstadoGeneral.setText(msj);
        lblEstadoGeneral.setBackground(bgColor);
    }

    // Actualiza el texto en el panel de armería (Pendiente o Desplegado)
    private void actualizarEstadoBarco(int id) {
        int tam = Math.max(dimensiones[id][0], dimensiones[id][1]);
        estadoBarcos[id].setText("Navío " + (id+1) + " (Tamaño: " + tam + ") - " + (barcoColocado[id] ? "Desplegado" : "Pendiente"));
        estadoBarcos[id].setForeground(barcoColocado[id] ? Color.GRAY : Color.BLACK);
    }

    // Cambia el barco que el usuario tiene activo en el "cursor"
    private void seleccionarBarco(int id) {
        if (id < 0 || id >= dimensiones.length || radar != null) return;
        limpiarTodasLasProyecciones();
        barcoActivoId = id;
        esHorizontal = true;
    }

    // Borra la pintura verde/roja temporal de todos lados (limpieza de siluetas)
    private void limpiarTodasLasProyecciones() {
        for (int fila = 0; fila < TAMANO; fila++) {
            for (int columna = 0; columna < TAMANO; columna++) {
                if (tableroLogico[fila][columna] == 0) casillas[fila][columna].setBackground(COLOR_AGUA_PROPIA);
            }
        }
    }

    // Obtiene las dimensiones ajustadas según si el barco está horizontal o vertical
    private int[] dimensionesBarcoActivo() {
        int d1 = dimensiones[barcoActivoId][0];
        int d2 = dimensiones[barcoActivoId][1];
        int magnitud = Math.max(d1, d2);
        return esHorizontal ? new int[]{1, magnitud} : new int[]{magnitud, 1};
    }

    // Verifica que el barco no se salga de la cuadrícula ni colisione con otro barco
    private boolean esPosicionValida(int x, int y) {
        int[] tam = dimensionesBarcoActivo();
        if (x < 0 || y < 0 || x + tam[0] > TAMANO || y + tam[1] > TAMANO) return false;
        for (int f = x; f < x + tam[0]; f++) {
            for (int c = y; c < y + tam[1]; c++) {
                int valor = tableroLogico[f][c];
                // Permitir solaparse consigo mismo (si el usuario mueve el mismo barco)
                if (valor != 0 && valor != barcoActivoId + 1) return false;
            }
        }
        return true;
    }

    // Dibuja temporalmente la silueta del barco (verde si es válido, rojo si es inválido)
    private void pintarProyeccion(int x, int y) {
        if (barcosColocados == dimensiones.length || radar != null) return;
        int[] tam = dimensionesBarcoActivo();
        Color color = esPosicionValida(x, y) ? COLOR_VALIDO : COLOR_INVALIDO;
        for (int f = x; f < x + tam[0] && f < TAMANO; f++) {
            for (int c = y; c < y + tam[1] && c < TAMANO; c++) {
                if (f >= 0 && c >= 0 && tableroLogico[f][c] == 0) casillas[f][c].setBackground(color);
            }
        }
    }

    // Borra la silueta temporal dibujada al salir el ratón de la casilla
    private void limpiarProyeccion(int x, int y) {
        if (barcosColocados == dimensiones.length || radar != null) return;
        int[] tam = dimensionesBarcoActivo();
        for (int f = x; f < x + tam[0] && f < TAMANO; f++) {
            for (int c = y; c < y + tam[1] && c < TAMANO; c++) {
                if (f >= 0 && c >= 0 && tableroLogico[f][c] == 0) casillas[f][c].setBackground(COLOR_AGUA_PROPIA);
            }
        }
    }

    // Guarda permanentemente la posición del barco en la matriz lógica y visual
    private void fijarBarco(int x, int y) {
        if (!esPosicionValida(x, y) || radar != null) return;
        
        // Si el barco ya estaba en el mapa en otra ubicación, lo borramos de allá primero
        if (barcoColocado[barcoActivoId]) limpiarBarcoAnterior(barcoActivoId);
        else { 
            barcosColocados++; 
            hpBarcos[barcoActivoId] = dimensionesBarcoActivo()[0] * dimensionesBarcoActivo()[1]; 
        }
        
        int[] tam = dimensionesBarcoActivo();
        for (int f = x; f < x + tam[0]; f++) {
            for (int c = y; c < y + tam[1]; c++) {
                tableroLogico[f][c] = barcoActivoId + 1; // ID lógica del barco
                casillas[f][c].setBackground(COLOR_BARCO); // Pintura permanente gris
            }
        }
        barcoColocado[barcoActivoId] = true;
        actualizarEstadoBarco(barcoActivoId);
        
        // Desbloquear botón de inicio si toda la flota fue colocada
        botonListo.setEnabled(barcosColocados == dimensiones.length);
        
        // Seleccionar automáticamente el siguiente barco pendiente
        for (int i = 0; i < dimensiones.length; i++) {
            if (!barcoColocado[i]) { seleccionarBarco(i); break; }
        }
    }

    // Borra un barco previamente posicionado para permitir reubicarlo
    private void limpiarBarcoAnterior(int id) {
        for (int f = 0; f < TAMANO; f++) {
            for (int c = 0; c < TAMANO; c++) {
                if (tableroLogico[f][c] == id + 1) {
                    tableroLogico[f][c] = 0;
                    casillas[f][c].setBackground(COLOR_AGUA_PROPIA);
                }
            }
        }
    }

    // Inicia la comunicación con El servidor una vez que el jugador acomodó su flota
    private void iniciarFaseAtaqueVisual() {
        if (barcosColocados != dimensiones.length) return;
        
        // Bloquear completamente la matriz del jugador (remover listeners del ratón)
        for (JButton[] fila : casillas) {
            for (JButton casilla : fila) {
                for (java.awt.event.MouseListener ml : casilla.getMouseListeners()) casilla.removeMouseListener(ml);
            }
        }
        
        cambiarMensajeEstado("CONECTANDO AL RADAR SATELITAL...", Color.DARK_GRAY);
        try {
            // Avisar a El servidor que estamos listos y esperar en un hilo secundario
            redCliente.enviarSenalListoSinEsperar();
            new ReceptorRespuestas().start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Fallo de red al conectar con El servidor.");
        }
    }

    // Transforma el panel derecho (Armería) en el panel de ataque (Radar Enemigo)
    private void construirRadar() {
        remove(panelDerechoCentral);
        
        panelDerechoCentral = new JPanel(new BorderLayout(0, 10));
        panelDerechoCentral.setBackground(COLOR_BG_PANEL);
        
        JLabel titulo = new JLabel("RADAR ENEMIGO", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        panelDerechoCentral.add(titulo, BorderLayout.NORTH);

        JPanel cuadriculaRadar = new JPanel(new GridLayout(TAMANO, TAMANO, 1, 1));
        cuadriculaRadar.setBackground(Color.BLACK);
        cuadriculaRadar.setPreferredSize(new Dimension(450, 450));
        radar = new JButton[TAMANO][TAMANO];

        // Creación interactiva de la matriz de ataque
        for (int x = 0; x < TAMANO; x++) {
            for (int y = 0; y < TAMANO; y++) {
                JButton casilla = new JButton();
                casilla.setBackground(COLOR_AGUA_RADAR);
                casilla.setFocusPainted(false);
                casilla.setBorder(BorderFactory.createEmptyBorder());
                final int f = x; final int c = y;
                
                // Acción de click para enviar el misil a esas coordenadas
                casilla.addActionListener(e -> enviarAtaque(f, c));
                radar[x][y] = casilla;
                cuadriculaRadar.add(casilla);
            }
        }
        panelDerechoCentral.add(cuadriculaRadar, BorderLayout.CENTER);
        add(panelDerechoCentral, BorderLayout.EAST);
        revalidate();
        repaint();
        
        radarHabilitado = true;
        cambiarMensajeEstado("¡SISTEMAS EN LÍNEA! Es tu turno. Selecciona coordenadas.", COLOR_VALIDO);
    }

    // Registra la coordenada clicada en el radar y envía los datos a El servidor
    private void enviarAtaque(int x, int y) {
        if (!radarHabilitado || !radar[x][y].isEnabled()) return;
        try {
            ultimoAtaqueX = x; ultimoAtaqueY = y;
            radarHabilitado = false; // Bloquear radar temporalmente para evitar spam
            actualizarEstadoRadar();
            redCliente.enviarCoordenada(x, y);
        } catch (IOException e) {
            radarHabilitado = true;
            actualizarEstadoRadar();
            JOptionPane.showMessageDialog(this, "Error de red al enviar el misil a El servidor.");
        }
    }

    // Bloquea o desbloquea las casillas del radar según si es nuestro turno
    private void actualizarEstadoRadar() {
        for (JButton[] f : radar) {
            for (JButton c : f) {
                // Solo habilitar si es nuestro turno y la casilla aún no ha sido atacada
                c.setEnabled(radarHabilitado && c.getBackground().equals(COLOR_AGUA_RADAR));
            }
        }
    }

    // Hilo encargado de escuchar ininterrumpidamente las respuestas y ataques de El servidor
    private final class ReceptorRespuestas extends Thread {
        @Override
        public void run() {
            try {
                // Esperar a que El servidor confirme el inicio de la batalla
                int senal = redCliente.leerRespuesta();
                if (senal == SIGNAL_INICIO_ATAQUE) {
                    SwingUtilities.invokeAndWait(TableroCliente.this::construirRadar);
                }

                // Ciclo principal de juego (Turnos iterativos)
                while (!isInterrupted()) {
                    if (turnoJugador) {
                        // Leer la respuesta de El servidor tras nuestro ataque
                        int codigo = redCliente.leerRespuesta();
                        int idHundido = (codigo == CODIGO_HUNDIDO || codigo == CODIGO_VICTORIA) ? redCliente.leerRespuesta() : -1;
                        
                        SwingUtilities.invokeLater(() -> procesarResultadoJugador(codigo, idHundido));

                        if (codigo == CODIGO_AGUA) {
                            turnoJugador = false; // Perdemos el turno al golpear agua
                        }
                    } else {
                        // Turno de El servidor: nos manda unas coordenadas de ataque
                        int xEnemigo = redCliente.leerRespuesta();
                        int yEnemigo = redCliente.leerRespuesta();

                        // Evaluamos localmente si el ataque nos dio
                        int respuestaLocal = evaluarTiroEnemigoLogica(xEnemigo, yEnemigo);
                        redCliente.enviarRespuesta(respuestaLocal);
                        if (respuestaLocal == CODIGO_HUNDIDO || respuestaLocal == CODIGO_VICTORIA) {
                            redCliente.enviarRespuesta(recuperarIdHundido(xEnemigo, yEnemigo));
                        }

                        // Actualizar interfaz visual
                        SwingUtilities.invokeLater(() -> procesarResultadoEnemigoVisual(xEnemigo, yEnemigo, respuestaLocal));

                        // Si El servidor golpea agua, recuperamos nuestro turno
                        if (respuestaLocal == CODIGO_AGUA) {
                            turnoJugador = true;
                            radarHabilitado = true;
                            SwingUtilities.invokeLater(() -> {
                                actualizarEstadoRadar();
                                cambiarMensajeEstado("El servidor falló el tiro. ¡ES TU TURNO!", COLOR_VALIDO);
                            });
                        }
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> cambiarMensajeEstado("Conexión perdida con El servidor.", COLOR_INVALIDO));
            }
        }
    }

    // Procesa el código recibido para actualizar nuestro radar visualmente
    private void procesarResultadoJugador(int codigo, int idHundido) {
        switch (codigo) {
            case CODIGO_AGUA:
                radar[ultimoAtaqueX][ultimoAtaqueY].setBackground(COLOR_FALLO);
                cambiarMensajeEstado("FALLO. El servidor prepara su disparo...", Color.DARK_GRAY);
                break;
            case CODIGO_IMPACTO:
                radar[ultimoAtaqueX][ultimoAtaqueY].setBackground(COLOR_FUEGO);
                radarHabilitado = true; // Conservamos el turno tras un impacto exitoso
                actualizarEstadoRadar();
                cambiarMensajeEstado("¡IMPACTO CONFIRMADO! Conservas el turno.", COLOR_VALIDO);
                break;
            case CODIGO_HUNDIDO:
                radar[ultimoAtaqueX][ultimoAtaqueY].setBackground(COLOR_FUEGO);
                radar[ultimoAtaqueX][ultimoAtaqueY].setText("X"); 
                radar[ultimoAtaqueX][ultimoAtaqueY].setForeground(Color.WHITE);
                radarHabilitado = true; // Conservamos turno
                actualizarEstadoRadar();
                cambiarMensajeEstado("¡BARCO DE EL SERVIDOR DESTRUIDO! Continúa atacando.", COLOR_VALIDO);
                break;
            case CODIGO_VICTORIA:
                radar[ultimoAtaqueX][ultimoAtaqueY].setBackground(COLOR_FUEGO);
                cambiarMensajeEstado("¡VICTORIA TOTAL! La flota de El servidor yace en el abismo.", Color.decode("#EAB308"));
                
                // Mensaje Emergente de Victoria
                JOptionPane.showMessageDialog(this, 
                    "¡Misión cumplida! Has ganado la partida.", 
                    "¡Victoria!", 
                    JOptionPane.INFORMATION_MESSAGE);
                break;
        }
    }

    private int idBarcoHundidoEnTurno = -1;

    // Analiza nuestra matriz para verificar si el misil enviado por El servidor nos dio en un barco
    private int evaluarTiroEnemigoLogica(int x, int y) {
        int idCelda = tableroLogico[x][y];
        if (idCelda == 0 || idCelda == -1) return CODIGO_AGUA;
        
        int idBarco = idCelda - 1;
        idBarcoHundidoEnTurno = idBarco; 
        hpBarcos[idBarco]--; // Restamos 1 punto de vida al barco golpeado
        tableroLogico[x][y] = -1; // Marcamos la casilla como ya impactada

        // Comprobamos si perdimos el juego
        boolean flotaDestruida = true;
        for (int hp : hpBarcos) { if (hp > 0) flotaDestruida = false; }
        if (flotaDestruida) return CODIGO_VICTORIA;
        
        // Comprobamos si el barco específico fue hundido
        if (hpBarcos[idBarco] == 0) return CODIGO_HUNDIDO;
        
        // Si no se hundió, fue solo un impacto
        return CODIGO_IMPACTO;
    }

    // Pinta rojo/azul en nuestra propia cuadrícula tras el tiro de El servidor
    private void procesarResultadoEnemigoVisual(int x, int y, int respuesta) {
        if (respuesta == CODIGO_AGUA) {
            casillas[x][y].setBackground(COLOR_FALLO); // Agua impactada por El servidor
        } else {
            casillas[x][y].setBackground(COLOR_FUEGO); // Fuego sobre nuestro barco
            if (respuesta == CODIGO_HUNDIDO) {
                cambiarMensajeEstado("¡ALERTA CRÍTICA! El servidor nos ha hundido un barco.", COLOR_FUEGO);
            } else if (respuesta == CODIGO_VICTORIA) {
                cambiarMensajeEstado("DERROTA. Tu flota ha sido destruida.", COLOR_INVALIDO);
                
                // Mensaje Emergente de Derrota
                JOptionPane.showMessageDialog(this, 
                    "El servidor ha destruido tu flota por completo.\nHas perdido la partida.", 
                    "Derrota", 
                    JOptionPane.ERROR_MESSAGE);
            } else {
                cambiarMensajeEstado("¡IMPACTO! Estamos bajo el fuego de El servidor...", COLOR_FUEGO);
            }
        }
    }

    // Devuelve la ID del barco que acabamos de perder para enviársela a El servidor
    private int recuperarIdHundido(int x, int y) { return idBarcoHundidoEnTurno; }
}