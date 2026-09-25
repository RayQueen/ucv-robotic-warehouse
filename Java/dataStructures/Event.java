package dataStructures;

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
public enum Event {
    INSERT_OBJ, INSERT_OBS, WAIT_SAT_PROD, WAIT_SAT_ROBOT, MOVE, PUSH_OBJ, PUSH_OBS, BATTERY, SUCCESS
}
