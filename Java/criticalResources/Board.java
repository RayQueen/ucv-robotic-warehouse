package criticalResources;

import dataStructures.Coord;
import dataStructures.Direction;
import dataStructures.Event;
import dataStructures.Object;

/**
 * Clase que representa un tablero de juego con celdas que pueden ser llenadas o vaciadas.
 */
public class Board{ 
    private int size;           // Tamaño del tablero (n x n)
    private Object[][] board;   // Matriz de objetos que representa el tablero
    private int emptyCells;     // Contador de celdas vacías
    private int targetBoxes[];  // Contador de cajas objetivo restantes por producir [0], cajas activas (en el tablero) [1] y cajas extraídas [2]
    private int obstacleBoxes;  // Contador de cajas obstáculo restantes por producir
    private int saturationCount;// Contador de saturación del tablero
    private int robotCount;     // Contador del número total de robots con batería agotada
    public Logger logger;      // Instancia del registrador de eventos

    public Board(int n, int targetBoxes, int obstacleBoxes, Logger logger){
        size = n;
        board = new Object[size][size];
        emptyCells = size * size;
        this.targetBoxes = new int[]{targetBoxes, 0, 0};
        this.obstacleBoxes = obstacleBoxes;
        this.saturationCount = 0;
        this.robotCount = 0;
        this.logger = logger;
    }

    public int getSize(){
        return size;
    }

    public int getEmptyCells(){
        return emptyCells;
    }

    public int getExtractedBoxes(){
        return targetBoxes[2];
    }

    public int getSaturationCount(){
        return saturationCount;
    }

    public int getRobotCount(){
        return robotCount;
    }

    /**
     * Devuelve el número total de cajas restantes por producir (cajas objetivo + cajas obstáculo).
     */
    public synchronized int getRemainingBoxes(){
        return targetBoxes[0] + obstacleBoxes;
    }

    /**
     * Devuelve el número de cajas objetivo activas en el tablero.
     */
    public synchronized boolean validateActiveTarget(int id){
        if (targetBoxes[1] <= 0 && getRemainingBoxes() <= 0) {
            logger.printLog(Event.BATTERY, id, null);
        }
        return targetBoxes[1] > 0;
    }

    public synchronized Object getCell(Coord coord){
        return board[coord.getX()][coord.getY()];
    }

    public synchronized boolean isEmpty(Coord coord){
        if(coord.getX() < 0 || coord.getX() >= size || coord.getY() < 0 || coord.getY() >= size){
            return false;
        }
        return board[coord.getX()][coord.getY()] == Object.EMPTY;
    }

    /**
     * Llena una celda del tablero con un objeto (caja objetivo o caja obstáculo) y produce una entrada descripitiva en el registro.
     * @param coord La coordenada de la celda a llenar.
     * @param id El identificador del productor que está llenando la celda.
     */
    public synchronized void fillCell(Coord coord, int id){
        if(getRemainingBoxes() <= 0) return; // Si no hay más cajas para producir retorna
        Object box = (int)(Math.random() * (targetBoxes[0] + obstacleBoxes)) < targetBoxes[0] ? Object.TARGET_BOX : Object.OBSTACLE_BOX;
        if(emptyCells > 0){                 // Si hay celdas vacías, intenta llenar la celda especificada
            if(isEmpty(coord)){             // Si la celda seleccionada está vacía, llena la celda con el objeto y actualiza los contadores
                board[coord.getX()][coord.getY()] = box;
                emptyCells--;
                if(box == Object.TARGET_BOX){
                    targetBoxes[0]--;
                    targetBoxes[1]++;
                    logger.printLog(Event.INSERT_OBJ, id, coord);
                } else {
                    obstacleBoxes--;
                    logger.printLog(Event.INSERT_OBS, id, coord);
                }
            }
        } else { // Si no hay celdas vacías, el productor debe esperar hasta que haya espacio disponible antes de volver a intentar llenar la celda
            logger.printLog(Event.WAIT_SAT_PROD, id, coord);
            incrementSaturationCount();
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Coloca un robot en una celda vacía del tablero y devuelve la coordenada de la celda donde se colocó el robot.
     * @return La coordenada de la celda donde se colocó el robot, o null si no se pudo colocar.
     */
    public synchronized Coord placeRobot(int id){
        boolean placed = false; // Bandera para indicar si el robot ha sido colocado
        Coord coord = new Coord((int)(Math.random() * size), (int)(Math.random() * size));
        while(!placed){
            if(emptyCells > 0){ // Si hay celdas vacías, intenta colocar el robot en la celda especificada
                if(isEmpty(coord)){ // Si la celda seleccionada está vacía, coloca el robot en la celda y actualiza el contador de celdas vacías
                    board[coord.getX()][coord.getY()] = Object.ROBOT;
                    emptyCells--;
                    placed = true; // Cambia la bandera a true para salir del bucle
                } else { // Si la celda seleccionada no está vacía, selecciona otra celda aleatoria
                    coord = new Coord((int)(Math.random() * size), (int)(Math.random() * size));
                }
            } else { // Si no hay celdas vacías, el robot debe esperar hasta que haya espacio disponible antes de volver a intentar colocar el robot
                try {
                    logger.printLog(Event.WAIT_SAT_ROBOT, id, coord);  
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return coord; // Devuelve la coordenada de la celda donde se colocó el robot, o null si no se pudo colocar
    }
    
    /**
     * Vacía una celda del tablero y notifica a los hilos que están esperando.
     * @param coord La coordenada de la celda a vaciar.
     */
    public synchronized void emptyCell(Coord coord){
        if(board[coord.getX()][coord.getY()] != Object.EMPTY){
            board[coord.getX()][coord.getY()] = Object.EMPTY;
            emptyCells++;
            notifyAll(); // Notifica a los productores que hay espacio disponible
        }
    }

    /**
     * Incrementa el contador de cajas objetivo extraídas por los robots.
     */
    public synchronized void incrementExtractedBoxes(){
        targetBoxes[2]++;
    }

    /**
     * Incrementa el contador de saturación del tablero.
     */
    public synchronized void incrementSaturationCount(){
        saturationCount++;
    }

    /**
     * Incrementa el contador de robots con batería agotada.
     */
    public synchronized void incrementRobotCount(){
        robotCount++;
    }

    /**
     * Mueve un objeto de una celda a otra en el tablero y produce una entrada descripitiva en el registro.
     * @param id El identificador del robot que está moviendo el objeto.
     * @param coord La coordenada de la celda donde se encuentra el objeto a mover.
     * @param direction La dirección en la que se moverá el objeto.
     * @return El objeto que se movió, o null si no se pudo mover.
     */
    public synchronized Object moveCell(int id, Coord coord, Direction direction){
        Coord newCoord = coord.move(direction);
        if(newCoord.getX() < 0 || newCoord.getX() >= size || newCoord.getY() < 0 || newCoord.getY() >= size){
            return null; // Si la nueva coordenada está fuera del tablero, no se realiza ningún movimiento
        }
        if(isEmpty(newCoord)){
            Object obj = board[coord.getX()][coord.getY()]; // Guarda el objeto que se va a mover
            board[newCoord.getX()][newCoord.getY()] = obj; // Mueve el objeto a la nueva coordenada
            board[coord.getX()][coord.getY()] = Object.EMPTY; // Vacía la celda original
            return obj; // Devuelve el objeto que se movió
        } else {
            return null; // Si la nueva coordenada no está vacía, no se realiza ningún movimiento
        }
    }

    /**
     * Devuelve el carácter que representa el objeto en la celda especificada.
     * @param row La fila de la celda.
     * @param col La columna de la celda.
     * @return El carácter que representa el objeto en la celda.
     */
    public char getObject(int row, int col){
        switch(board[row][col]){
            case EMPTY:
                return '.';
            case TARGET_BOX:
                return '0';
            case OBSTACLE_BOX:
                return 'X';
            case ROBOT:
                return 'R';
            default:
                return '?';
        }
    }

    /**
     * Imprime el estado actual del tablero en la consola.
     */
    public void printBoard(){
        for(int i = 0; i < board.length; i++){
            for(int j = 0; j < board[i].length; j++){
                System.out.print(getObject(i, j) + " ");
            }
            System.out.println();
        }
    }
}