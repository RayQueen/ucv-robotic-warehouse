package threads;

import criticalResources.Board;
import dataStructures.Coord;

/**
 * Clase que representa una cinta transportadora que llena el tablero de cajas.
 */
public class ConveyorBelt extends Thread{
    private int id;
    private Board board;

    public ConveyorBelt(Board board, int id){
        this.id = id;
        this.board = board;
    }

    public void run(){
        while(board.getRemainingBoxes() > 0){
            try {
                Thread.sleep((long)(Math.random() * 1000)); // Esperar un tiempo aleatorio antes de intentar llenar otra celda
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Generar una coordenada aleatoria dentro del tablero que no se encuentre en la fila 1, columna 1, o posición (5,5)
            Coord coord = new Coord((int)(Math.random() * board.getSize()), (int)(Math.random() * board.getSize()));
            if(coord.getX() == 0 || coord.getY() == 0){
                continue; // Si la coordenada es (0,y) o (x,0), generar otra
            }
            if(coord.getX() == 5 && coord.getY() == 5){
                continue; // Si la coordenada es (5,5), generar otra
            }
            board.fillCell(coord, id);
        }
    }
}

