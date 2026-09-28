package criticalResources;

import dataStructures.Coord;
import dataStructures.Direction;
import dataStructures.Event;
import dataStructures.ObjectType;
import dataStructures.Snapshot;
import dataStructures.MoveResult;

import java.util.ArrayList;

/**
 * Clase que representa un tablero de juego con celdas que pueden ser llenadas o vaciadas.
 */
public class Board{ 
    private int size;           // Tamaño del tablero (n x n)
    private ObjectType[][] board;   // Matriz de objetos que representa el tablero
    private int emptyCells;     // Contador de celdas vacías
    private int targetBoxes[];  // Contador de cajas objetivo restantes por producir [0], cajas activas (en el tablero) [1] y cajas extraídas [2]
    private int obstacleBoxes;  // Contador de cajas obstáculo restantes por producir
    private int saturationCount;// Contador de saturación del tablero
    private int robotCount;     // Contador del número total de robots con batería agotada
    public Logger logger;      // Instancia del registrador de eventos

    public Board(int n, int targetBoxes, int obstacleBoxes, Logger logger){
        size = n;
        board = new ObjectType[size][size];
        for (int i = 0; i < size; i++) { // Inicializar todas las celdas del tablero como vacías
            for (int j = 0; j < size; j++) {
                board[i][j] = ObjectType.EMPTY;
            }
        }
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

    public synchronized boolean hasActiveTarget() {
    return targetBoxes[1] > 0;
}

    public synchronized boolean isTargetProductionFinished() {
        return targetBoxes[0] == 0 && targetBoxes[1] == 0;
    }

    public synchronized boolean isProductionFinished() {
        return targetBoxes[0] == 0 && obstacleBoxes == 0;
    }

    public synchronized boolean isRobotFinished(int id){
        if(!hasActiveTarget() && isTargetProductionFinished()){
            logger.printLog(Event.BATTERY, id, null);
            return true;
        }
        return false;
    }

    public synchronized ObjectType getCell(Coord coord){
        return board[coord.getX()][coord.getY()];
    }

    public synchronized boolean isEmpty(Coord coord){
        if(coord.getX() < 0 || coord.getX() >= size || coord.getY() < 0 || coord.getY() >= size){
            return false;
        }
        return board[coord.getX()][coord.getY()] == ObjectType.EMPTY;
    }

    private boolean isValidBoxDestination(Coord coord) {
        return coord.getX() > 0 && coord.getX() < size && coord.getY() > 0 && coord.getY() < size && isEmpty(coord);
    }

    /**
     * Llena una celda del tablero con un objeto (caja objetivo o caja obstáculo) y produce una entrada descripitiva en el registro.
     * @param coord La coordenada de la celda a llenar.
     * @param id El identificador del productor que está llenando la celda.
     */
    public synchronized void fillCell(Coord coord, int id){
        // Si no hay celdas vacías y la producción no ha terminado, el productor debe esperar hasta que haya espacio disponible antes de volver a intentar llenar la celda
        if(emptyCells == 0 && !isProductionFinished()){
            logger.printLog(Event.WAIT_SAT_PROD, id, coord); // Notifica una unica vez que el productor está esperando debido a saturación del tablero
            incrementSaturationCount();
        }
        while (emptyCells == 0 && !isProductionFinished()) { 
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Si hay celdas vacías las cajas objetivo se generan con prioridad para evitar saturar el tablero con cajas obstáculo y permitir que los robots puedan extraer las cajas objetivo
        ObjectType box = (targetBoxes[0] > 0) ? ObjectType.TARGET_BOX : ObjectType.OBSTACLE_BOX;
        if(isEmpty(coord)){             // Si la celda seleccionada está vacía, llena la celda con el objeto y actualiza los contadores
            board[coord.getX()][coord.getY()] = box;
            emptyCells--;
            if(box == ObjectType.TARGET_BOX){
                targetBoxes[0]--;
                targetBoxes[1]++;
                logger.printLog(Event.INSERT_OBJ, id, coord);
                notifyAll(); // Notifica a los robots que hay una caja objetivo activa en el tablero
            } else {
                obstacleBoxes--;
                logger.printLog(Event.INSERT_OBS, id, coord);
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
                    board[coord.getX()][coord.getY()] = ObjectType.ROBOT;
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
        if(getCell(coord) != ObjectType.EMPTY){
            board[coord.getX()][coord.getY()] = ObjectType.EMPTY;
            emptyCells++;
            notifyAll(); // Notifica a los productores que hay espacio disponible
        }
    }

    /**
     * Elimina un robot sin batería del tablero.
     * @param id El identificador del robot a eliminar.
     * @param coord La coordenada de la celda donde se encuentra el robot a eliminar.
     * @param event El evento que ocurre al eliminar el robot.
     */
    public synchronized void removeRobot(int id, Coord coord, MoveResult event){
        if(getCell(coord) == ObjectType.ROBOT){
            emptyCell(coord); // Vacía la celda donde se encuentra el robot
            if(event != MoveResult.EXTRACTED_TARGET){ // Si el robot no extrajo una caja objetivo, se considera que su batería se agotó y se incrementa el contador de robots con batería agotada
                logger.printLog(Event.BATTERY, id, coord);
                incrementRobotCount(); // Incrementa el contador de robots con batería agotada
            }
        }
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
     * Mueve un objeto de una celda a otra en el tablero.
     * @param id El identificador del robot que hizo la llamada.
     * @param coord La coordenada de la celda donde se encuentra el objeto a mover.
     * @param direction La dirección en la que se moverá el objeto.
     * @return El resultado del movimiento.
     */
    public synchronized MoveResult moveCellWithoutLogging(int id, Coord coord, Direction direction){
        Coord newCoord = coord.move(direction);
        if(newCoord.getX() < 0 || newCoord.getX() >= size || newCoord.getY() < 0 || newCoord.getY() >= size){
            return MoveResult.BLOCKED; // Si la nueva coordenada está fuera del tablero, el objeto no puede moverse
        }
        if(isEmpty(newCoord)){ // Si la nueva coordenada está vacía, el objeto puede moverse
            MoveResult result;
            ObjectType obj = getCell(coord); // Obtiene el objeto que se encuentra en la celda original
            board[newCoord.getX()][newCoord.getY()] = obj; // Mueve el objeto a la nueva coordenada
            board[coord.getX()][coord.getY()] = ObjectType.EMPTY; // Vacía la celda original
            switch(obj){
                case TARGET_BOX:
                    if(newCoord.equals(new Coord(size - 1, size - 1))){ // Si la nueva coordenada es la celda de extracción, el robot extrajo una caja objetivo del tablero
                        emptyCell(newCoord); // Vacía la celda de extracción

                        targetBoxes[1]--; // objetivo activo deja de estar en el tablero
                        targetBoxes[2]++; // objetivo extraído
                        result = MoveResult.EXTRACTED_TARGET;   
                    } else {
                        result = MoveResult.PUSHED_TARGET; // Devuelve que una caja objetivo se movió exitosamente
                    }
                    break;
                case OBSTACLE_BOX:
                    result = MoveResult.PUSHED_OBSTACLE; // Devuelve que una caja obstáculo se movió exitosamente
                    break;
                default:
                    result = MoveResult.MOVED; // Devuelve que un robot se movió exitosamente
                    break;
            }
            notifyAll(); // Notifica a los productores que hay espacio disponible
            return result;
        } else {
            return MoveResult.BLOCKED; // Si la nueva coordenada no está vacía, el objeto no puede moverse
        }
    }

    /**
     * Mueve un objeto de una celda a otra en el tablero y produce una entrada descripitiva en el registro.
     * @param id El identificador del robot que hizo la llamada.
     * @param coord La coordenada de la celda donde se encuentra el objeto a mover.
     * @param direction La dirección en la que se moverá el objeto.
     * @return El resultado del movimiento.
     */
    public synchronized MoveResult moveCell(int id, Coord coord, Direction direction){
        MoveResult result = moveCellWithoutLogging(id, coord, direction); // Llama a moveCellWithLogging para mover el objeto y obtener el resultado del movimiento
        switch(result){
            case MOVED:
                logger.printLog(Event.MOVE, id, coord, coord.move(direction));
                break;
            case PUSHED_TARGET:
                logger.printLog(Event.PUSH_OBJ, id, coord, coord.move(direction));
                break;
            case PUSHED_OBSTACLE:
                logger.printLog(Event.PUSH_OBS, id, coord, coord.move(direction));
                break;
            case EXTRACTED_TARGET:
                logger.printLog(Event.SUCCESS, id, coord);
                break;
            default:
                break;
        }
        return result; // Devuelve el resultado del movimiento
    }

    /**
     * Intenta mover un robot a una nueva celda en el tablero y produce una entrada descripitiva en el registro.
     * @param id El identificador del robot que hizo la llamada.
     * @param coord La coordenada de la celda donde se encuentra el robot a mover.
     * @param direction La dirección en la que se moverá el robot.
     * @return El resultado del movimiento.
     */
    public synchronized MoveResult moveRobot(int id, Coord coord, Direction direction){
        Coord newCoord = coord.move(direction);
        if(isEmpty(newCoord)){ // Si la nueva coordenada está vacía, el robot puede moverse
            return moveCell(id, coord, direction); // Llama a moveCell para mover el robot a la nueva coordenada
        } else {
            Coord pushedBoxDestination = newCoord.move(direction);
            if(isValidBoxDestination(pushedBoxDestination) && getCell(newCoord) != ObjectType.ROBOT){ // Si la nueva coordenada inicial no esta ocupada por otro robot y la siguiente es válida, el robot puede empujar la caja a la nueva coordenada
                MoveResult result = moveCell(id, newCoord, direction); // Llama a moveCell para mover la caja a la nueva coordenada
                board[newCoord.getX()][newCoord.getY()] = ObjectType.ROBOT; // Mueve el robot a la nueva coordenada sin generar un registro de movimiento del robot, ya que el registro de movimiento de la caja ya incluye el movimiento del robot
                board[coord.getX()][coord.getY()] = ObjectType.EMPTY; // Vacía la celda original
                return result; // Devuelve el resultado del movimiento de la caja
            } else {
                return MoveResult.BLOCKED; // Si la nueva coordenada no está vacía, el robot no puede moverse
            }
        }
    }

    /**
     * Crea un snapshot del estado actual del tablero, incluyendo la posición de los robots, cajas objetivo y cajas obstáculo.
     * @return Un objeto Snapshot que contiene el estado actual del tablero.
     */
    public synchronized Snapshot snapshot(){
        Snapshot snapshot = new Snapshot(new ObjectType[0][0], new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0);
        snapshot.boardState = new ObjectType[size][size];
        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                snapshot.boardState[i][j] = board[i][j];
                switch(board[i][j]){
                    case ROBOT:
                        snapshot.robotPosition.add(new Coord(i, j));
                        break;
                    case TARGET_BOX:
                        snapshot.targetPosition.add(new Coord(i, j));
                        break;
                    case OBSTACLE_BOX:
                        snapshot.obstaclePosition.add(new Coord(i, j));
                        break;
                    default:
                        break;
                }
            }
        }
        snapshot.boardSize = size;
        return snapshot;
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