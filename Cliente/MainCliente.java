package Cliente;

public class MainCliente {
    public static void main(String[] args) {
        // Clase que maneja la comunicación con el servidor
        ClienteBatlleShips redCliente = new ClienteBatlleShips();
        
        // Conexión con el servidor
        System.out.println("Intentando conectar al servidor...");
        redCliente.conectar("127.0.0.1", 5000); 
        
        // Verificar que la conexión fue exitosa antes de continuar
        if (redCliente.estaConectado()){
            System.out.println("Conexión exitosa. Iniciando juego...");
            // Levantar la interfaz gráfica de forma segura en el Event Dispatch Thread
            javax.swing.SwingUtilities.invokeLater(() -> {
                TableroCliente ventana = new TableroCliente(redCliente);
                ventana.setLocationRelativeTo(null); // Centra la ventana en la pantalla
                ventana.setVisible(true);
            });
        }else{
            System.err.println("No se pudo conectar al servidor. Por favor, verifica que el servidor esté en ejecución y que la IP/puerto sean correctos.");
                System.err.println("Cerrando conexión...");
            redCliente.cerrarConexion(); // Cierra la conexión si no se pudo establecer
        }

    }
}