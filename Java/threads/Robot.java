package threads;

import criticalResources.Board;
import dataStructures.Coord;
import dataStructures.Direction;

/**
 * Clase que representa un robot con su posicion y nivel de bateria.
 */
public class Robot extends Thread{
    private Board board;
    private int id;
    private Coord position;
    private int battery;

    public Robot(Board board, int startX, int startY, int initialBattery){
        this.board = board;
        position = new Coord(startX, startY);
        battery = initialBattery;
    }

    public Coord getPosition(){
        return position;
    }

    public int getBattery(){
        return battery;
    }

    public void run(){
        while(battery > 0){
            // Si no quedan más cajas objetivo en el tablero, el robot termina su ejecución como falta de batería
            if(!board.validateActiveTarget(id)){
                break;
            }

            // Opciones de pensamiento para el robot: Direcciones aleatorias, todo a la derecha/abajo, maybe A*?
            // Placeholder: Movimiento aleatorio
            Direction direction = Direction.values()[(int)(Math.random() * Direction.values().length)];
            board.moveCell(id, position, direction);
            battery--;
            if(battery <= 0){
                board.incrementRobotCount();
                board.emptyCell(position);
                break;
            }
        }
    }
}