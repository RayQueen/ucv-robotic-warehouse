import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import criticalResources.*;
import threads.*;
import dataStructures.Coord;
import dataStructures.Reader;

public class Proyecto3{
    public static void main(String[] args){
        if (args.length < 1) { // Verificar que se haya proporcionado un argumento
            System.out.println("Error: Por favor, proporcione el nombre del archivo como argumento.");
            System.out.println("Uso: java Proyecto3.java <nombre_archivo.txt>");
            return;
        }

        Reader reader = new Reader(args[0], Paths.get(args[0])); // Crear una instancia del lector
        int[] arguments = reader.obtainArguments(); // Obtener los argumentos del archivo

        if (arguments.length == 0) { // Verificar si los argumentos son válidos
            return;
        }

        Board board = new Board(6, arguments[0], arguments[1], new Logger()); // Crear el tablero con los argumentos leidos

        // Crear y ejecutar los hilos productores y robots
        List<Thread> threads = new ArrayList<>(); // Lista para almacenar los hilos de productores y robots

        Robot[] robots = new Robot[arguments[2]];
        for (int i = 0; i < arguments[2]; i++) {
            Coord startCoord = board.placeRobot(i); // Colocar el robot en una celda vacia
            robots[i] = new Robot(board, i, startCoord.getX(), startCoord.getY(), arguments[3]); // Crear el robot con la posicion inicial y la bateria
            threads.add(robots[i]);
            robots[i].start(); // Iniciar el hilo del robot
        }

        ConveyorBelt[] producers = new ConveyorBelt[arguments[4]];
        for (int i = 0; i < arguments[4]; i++) {
            producers[i] = new ConveyorBelt(board, i); // Crear el productor con el identificador
            threads.add(producers[i]);
            producers[i].start(); // Iniciar el hilo del productor
        }

        // Esperar a que todos los hilos terminen
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Imprimir el reporte final del tablero
        System.out.println("=== REPORTE FINAL DEL TABLERO ===");
        System.out.println("- Cajas objetivo extraidas (llevadas a 5,5): " + board.getExtractedBoxes());
        System.out.println("- Saturaciones del tablero (intentos fallidos de productores): " + board.getSaturationCount());
        System.out.println("- Robots que finalizaron por bateria agotada: " + board.getRobotCount());

        System.out.println("=== ESTADO FINAL DEL TABLERO ===");
        board.printBoard(); // Imprimir el estado final del tablero
    }
}