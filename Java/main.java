import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import criticalResources.*;
import threads.*;
import dataStructures.Coord;

public class main{
    private static boolean validateArguments(int[] args){
        int sum = 0;
        for (int i = 0; i < args.length; i++){
            if(args[i] < 0){ // Verificar que no haya argumentos negativos
                System.err.println("Error: Existen argumentos negativos.");
                return false;
            }
            if (i < 3) { // Solo sumar los primeros tres argumentos (cajas objetivo, cajas obstáculo y robots)
                sum += args[i];
            }
        }
        if (sum > 25) { // Verificar que el número total de las entidades no sea mayor que las celdas totales (5x5 = 25)
            System.err.println("Error: El número total de las entidades es mayor que las celdas totales (25).");
            return false;
        }
        return true;
    }

    /**
     * Método que obtiene los argumentos del archivo de entrada y los almacena en un arreglo.
     * @param args Arreglo donde se almacenarán los argumentos leídos del archivo.
     * @param fileName Nombre del archivo de entrada.
     * @param filePath Ruta del archivo de entrada.
     * @return Arreglo con los argumentos leídos del archivo con el siguiente orden: [Número de cajas objetivo, Número de cajas obstáculo, Número de robots, Batería inicial de los robots, Número de productores].
     */
    private static int[] obtainArguments(String fileName, Path filePath){
        int[] args = new int[5];
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            for (int i = 0; i < args.length; i++) {
                line = reader.readLine();
                if (line != null) {
                    args[i] = Integer.parseInt(line.trim());
                } else {
                    System.err.println("Error: El archivo no contiene suficientes líneas para los argumentos esperados.");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
        }
        if (!validateArguments(args)) {
            return new int[0]; // Retorna un arreglo vacío si los argumentos no son válidos
        }
        return args;
    }
    public static void run(String[] args){
        if (args.length < 1) { // Verificar que se haya proporcionado un argumento
            System.out.println("Error: Por favor, proporcione el nombre del archivo como argumento.");
            System.out.println("Uso: java Proyecto3 <nombre_archivo.txt>");
            return;
        }

        String fileName = args[0]; // Obtener el nombre del archivo desde los argumentos de la línea de comandos
        Path filePath = Paths.get(fileName); // Ruta del archivo
        int arguments[]; // Arreglo para almacenar los argumentos leídos del archivo

        arguments = obtainArguments(fileName, filePath); // Obtener los argumentos del archivo

        Board board = new Board(5, arguments[0], arguments[1], new Logger()); // Crear el tablero con los argumentos leídos

        // Crear y ejecutar los hilos productores y robots
        ConveyorBelt[] producers = new ConveyorBelt[arguments[4]];
        for (int i = 0; i < arguments[4]; i++) {
            producers[i] = new ConveyorBelt(board, i); // Crear el productor con el identificador
            producers[i].start(); // Iniciar el hilo del productor
        }
        
        Robot[] robots = new Robot[arguments[2]];
        for (int i = 0; i < arguments[2]; i++) {
            Coord startCoord = board.placeRobot(i); // Colocar el robot en una celda vacía
            robots[i] = new Robot(board, startCoord.getX(), startCoord.getY(), arguments[3]); // Crear el robot con la posición inicial y la batería
            robots[i].start(); // Iniciar el hilo del robot
        }

        // Esperar a que todos los hilos terminen
        for (int i = 0; i < arguments[4]; i++) {
            try {
                producers[i].join(); // Esperar a que el productor termine
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < arguments[2]; i++) {
            try {
                robots[i].join(); // Esperar a que el robot termine
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Imprimir el reporte final del tablero
        System.out.println("=== REPORTE FINAL DEL TABLERO ===");
        System.out.println("- Cajas objetivo extraídas (llevadas a 5,5): " + board.getExtractedBoxes());
        System.out.println("- Saturaciones del tablero (intentos fallidos de productores): " + board.getSaturationCount());
        System.out.println("- Robots que finalizaron por batería agotada: " + board.getRobotCount());

        System.out.println("=== ESTADO FINAL DEL TABLERO ===");
        board.printBoard(); // Imprimir el estado final del tablero
    }
}