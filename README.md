Práctica de Aplicaciones para Comunicaciones en Red - Profesor Axel Ernesto

El cliente envía las coordenadas [x,y] al servidor representando su tiro en el mapa
El servidor responde con un estado dependiendo del resultado:
- Código 0 - Agua: El tiro cayó en agua
- Código 1 - Impacto: El tiro cayó en un barco
- Código 2 - Hundido: El tiro destruye la ultima parte restante de un barco
- Código 3 - Victoria: Todos los barcos fueron destruidos

El servidor también responderá con el ID del barco golpeado en caso de que la respuesta inicial sea 2 o 3.

El tablero consta de una cuadrícula de 10x10 bloques, donde se repartirán 6 barcos en total:
- 1 barco de 2x5
- 1 barco de 1x3
- 1 barco de 1x4
- 1 barco de 1x5
- 2 barcos de 1x2

