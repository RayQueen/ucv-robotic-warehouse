import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//=======================================
//   Definicion de los tipos de datos
//=======================================

/**
 * Enum que representa las direcciones posibles en las que un robot puede moverse.
 */
enum Direction{
    UP, DOWN, LEFT, RIGHT
}
/**
 * Enum que representa los objetos que pueden estar en el tablero.
 */
enum Object{
    EMPTY, TARGET_BOX, OBSTACLE_BOX, ROBOT
}
/**
 * <pre>
 * Enum que representa los eventos de log. <br>
 * INSERT_OBJ: Un productor ha insertado un objetivo en el tablero.
 * INSERT_OBS: Un productor ha insertado un obstáculo en el tablero.
 * WAIT_SAT_PROD: Un productor ha intentado insertar un objeto en el tablero lleno.
 * WAIT_SAT_ROBOT: Un robot ha intentado moverse a una posición ocupada.
 * MOVE: Un robot se ha movido a una nueva posición.
 * PUSH_OBJ: Un robot ha empujado una caja objetivo.
 * PUSH_OBS: Un robot ha empujado una caja obstáculo.
 * BATTERY: La batería de un robot se agotó.
 * SUCCESS: Un robot ha movido un objetivo a la posición (5,5). 
 * </pre>
 */
enum Log{
    INSERT_OBJ, INSERT_OBS, WAIT_SAT_PROD, WAIT_SAT_ROBOT, MOVE, PUSH_OBJ, PUSH_OBS, BATTERY, SUCCESS
}
/**
 * Clase que representa una coordenada en un plano bidimensional.
 */
class Coord{
    private int x;
    private int y;

    public Coord(int x, int y){
        this.x = x;
        this.y = y;
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }

    public void setX(int x){
        this.x = x;
    }

    public void setY(int y){
        this.y = y;
    }

    /**
     * Mueve la coordenada en la dirección especificada y devuelve una nueva coordenada.
     * @param direction La dirección en la que se moverá la coordenada.
     * @return Una nueva coordenada después del movimiento.
     */
    public Coord move(Direction direction){
        switch(direction){
            case UP:
                return new Coord(x - 1, y);
            case DOWN:
                return new Coord(x + 1, y);
            case LEFT:
                return new Coord(x, y - 1);
            case RIGHT:
                return new Coord(x, y + 1);
            default:
                return this;
        }
    }
    /**
     * Mueve la coordenada en la dirección especificada por n unidades y devuelve una nueva coordenada.
     * @param direction La dirección en la que se moverá la coordenada.
     * @param n El número de unidades a mover.
     * @return Una nueva coordenada después del movimiento.
     */
    public Coord move(Direction direction, int n){
        switch(direction){
            case UP:
                return new Coord(x - n, y);
            case DOWN:
                return new Coord(x + n, y);
            case LEFT:
                return new Coord(x, y - n);
            case RIGHT:
                return new Coord(x, y + n);
            default:
                return this;
        }
    }
}

//=======================================
//      Definicion de los monitores
//=======================================
/**
 * Clase que representa un registrador de eventos.
 */
class Logger{
    private int tick;

    public Logger(){
        tick = 0;
    }

    private synchronized void incrementTick(){
        tick++;
    }

    private synchronized int getTick(){
        return tick;
    }

    /**
     * Imprime un mensaje de log en la consola basado en el evento, el id del productor o robot, y la coordenada.
     * @param event El evento que se está registrando.
     * @param id El identificador del productor o robot.
     * @param coord La coordenada asociada al evento.
     */
    public void printLog(Log event, int id, Coord coord){
        String message = "";
        switch(event){
            case INSERT_OBJ:
                message = "[Tick-" + getTick() + "] [Productor-" + id + "] INSERTAR_OBJETIVO -> (" + coord.getX() + "," + coord.getY() + ")";
                break;
            case INSERT_OBS:
                message = "[Tick-" + getTick() + "] [Productor-" + id + "] INSERTAR_BLOQUEO -> (" + coord.getX() + "," + coord.getY() + ")";
                break;
            case WAIT_SAT_PROD:
                message = "[Tick-" + getTick() + "] [Productor-" + id + "] ESPERA_SATURACION";
                break;
            case WAIT_SAT_ROBOT:
                message = "[Tick-" + getTick() + "] [Robot-" + id + "] ESPERA_SATURACION";
                break;
            case BATTERY:
                message = "[Tick-" + getTick() + "] [Robot-" + id + "] BATERIA_AGOTADA -> Retiro del tablero";
                break;
            case SUCCESS:
                message = "[Tick-" + getTick() + "] [Robot-" + id + "] EXTRACCION_EXITOSA -> (5,5)";
                break;
            default:
                message = "[Tick-" + getTick() + "] MENSAJE_DESCONOCIDO";
                break;
        }
        System.out.println(message);
        incrementTick();
    }

    /**
     * Imprime un mensaje de log en la consola basado en el evento, el id del productor o robot, y las coordenadas.
     * @param event El evento que se está registrando.
     * @param id El identificador del productor o robot.
     * @param coord1 La primera coordenada asociada al evento.
     * @param coord2 La segunda coordenada asociada al evento.
     * @param direction La dirección asociada al evento.
     */
    public void printLog(Log event, int id, Coord coord1, Coord coord2, Direction direction){
        String message = "";
        switch(event){
            case MOVE:
                message = "[Tick-" + getTick() + "] [Robot-" + id + "] MOVER  (" + coord1.getX() + "," + coord1.getY() + ") -> (" + coord2.getX() + "," + coord2.getY() + ")";
                break;
            case PUSH_OBJ:
                message = "[Tick-" + getTick() + "] [Robot-" + id + "] EMPUJAR_OBJETIVO (" + coord1.getX() + "," + coord1.getY() + ") -> (" + coord2.getX() + "," + coord2.getY() + ")";
                break;
            case PUSH_OBS:
                message = "[Tick-" + getTick() + "] [Robot-" + id + "] EMPUJAR_BLOQUEO (" + coord1.getX() + "," + coord1.getY() + ") -> (" + coord2.getX() + "," + coord2.getY() + ")";
                break;
            default:
                message = "[Tick-" + getTick() + "] MENSAJE_DESCONOCIDO";
                break;
        }
        System.out.println(message);
        incrementTick();
    }
}

/**
 * Clase que representa un tablero de juego con celdas que pueden ser llenadas o vaciadas.
 */
class Board{ 
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
            logger.printLog(Log.BATTERY, id, null);
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
                    logger.printLog(Log.INSERT_OBJ, id, coord);
                } else {
                    obstacleBoxes--;
                    logger.printLog(Log.INSERT_OBS, id, coord);
                }
            }
        } else { // Si no hay celdas vacías, el productor debe esperar hasta que haya espacio disponible antes de volver a intentar llenar la celda
            logger.printLog(Log.WAIT_SAT_PROD, id, coord);
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
                    logger.printLog(Log.WAIT_SAT_ROBOT, id, coord);  
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

//=======================================
//       Definicion de los hilos
//=======================================
/**
 * Clase que representa un robot con su posicion y nivel de bateria.
 */
class Robot extends Thread{
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
                board.logger.printLog(Log.BATTERY, id, position);
                break;
            }
        }
    }
}

/**
 * Clase que representa una cinta transportadora que llena el tablero de cajas.
 */
class ConveyorBelt extends Thread{
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
    public static void main(String[] args){
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