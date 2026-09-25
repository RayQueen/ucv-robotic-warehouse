package criticalResources;

import dataStructures.Event;
import dataStructures.Coord;
import dataStructures.Direction;

/**
 * Clase que representa un registrador de eventos.
 */
public class Logger{
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
    public void printLog(Event event, int id, Coord coord){
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
    public void printLog(Event event, int id, Coord coord1, Coord coord2, Direction direction){
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
