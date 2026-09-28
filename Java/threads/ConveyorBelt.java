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
        while(!board.isProductionFinished()){ // Mientras la producción no haya terminado
            try {
                Thread.sleep((long)(Math.random() * 1000)); // Esperar un tiempo aleatorio antes de intentar llenar otra celda
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Generar una coordenada aleatoria dentro del tablero que no se encuentre en los bordes
            int interiorSize = board.getSize() - 2;
            Coord coord = new Coord(1 + (int) (Math.random() * interiorSize), 1 + (int) (Math.random() * interiorSize)
            );
            board.fillCell(coord, id);
        }
        System.out.println("Productor " + id + " ha terminado de producir.");
    }
}

