Práctica de Aplicaciones para Comunicaciones en Red - Profesor Axel Ernesto

El cliente envía las coordenadas [x,y] al servidor representando su tiro en el mapa

### Código de Respuesta
El servidor responde con un estado dependiendo del resultado:
- Código 0 - Agua: El tiro cayó en agua
- Código 1 - Impacto: El tiro cayó en un barco
- Código 2 - Hundido: El tiro destruye la ultima parte restante de un barco
- Código 3 - Victoria: Todos los barcos fueron destruidos

El servidor también responderá con el ID del barco golpeado en caso de que la respuesta inicial sea 2 o 3.

### Barcos
El tablero consta de una cuadrícula de 10x10 bloques, donde se repartirán 6 barcos en total:
- 1 barco de 2x5 [ID: 0]
- 1 barco de 1x3 [ID: 1]
- 1 barco de 1x4 [ID: 2]
- 1 barco de 1x5 [ID: 3]
- 2 barcos de 1x2 [ID: 4 y 5]

Flujo de la partida:

El juego se jugará en 2 fases: Fase de colocación y fase de ataque

### Fase de colocación
El jugador mueve sus barcos y los coloca a lo largo y ancho del mapa. Al seleccionar "Listo", envía al Servidor la señal para comenzar y el Servidor genera su propio tablero con las posiciones de los barcos aleatorizadas.
Cuando el Servidor coloca sus barcos, envía una señal al Cliente informando que se cambia de fase

### Fase de ataque
1. El jugador tira primero. Selecciona un cuadro del mapa y envía al Servidor las coordenadas [x,y] del tiro, y queda esperando con la instrucción read() a la respuesta.
2. El Servidor recibe las coordenadas [x,y] del tiro del jugador y las compara con su propio tablero secreto
3. Dependiendo del resultado del tiro, el Servidor devuelve el código de respuesta al Cliente. En caso de que el código de respuesta sea 2 o 3, se enviará el ID del barco correspondiente al tiro para renderizar el barco hundido.
    - Si el tiro acertó a un barco, el jugador continua jugando.
    - Si el tiro no acertó, el Cliente entra en modo receptor read() en espera de las jugadas del Servidor
    
El juego termina cuando el Cliente o el Servidor emitan un 3 como código de respuesta