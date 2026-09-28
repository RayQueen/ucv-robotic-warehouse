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
        while(!board.isProductionFinished() && !board.isProductionImpossible()){ // Mientras la producción sea posible
            // Generar una coordenada aleatoria dentro del tablero que no se encuentre en los bordes
            int interiorSize = board.getSize() - 2;
            Coord coord = new Coord(1 + (int) (Math.random() * interiorSize), 1 + (int) (Math.random() * interiorSize)
            );

            // Intentar llenar la celda con la coordenada generada
            board.fillCell(coord, id);

            try {
                Thread.sleep((long)(Math.random() * 1000)); // Esperar un tiempo aleatorio antes de intentar llenar otra celda
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}

