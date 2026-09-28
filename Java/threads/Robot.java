package threads;

import criticalResources.Board;
import dataStructures.Coord;
import dataStructures.Direction;
import dataStructures.MoveResult;
import dataStructures.Snapshot;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Clase que representa un robot con su posicion y nivel de bateria.
 */
public class Robot extends Thread{
    private Board board;
    private Snapshot snapshot;
    private int id;
    private Coord position;
    private int battery;

    public Robot(Board board, int id, int startX, int startY, int initialBattery){
        this.board = board;
        this.id = id;
        position = new Coord(startX, startY);
        battery = initialBattery;
    }

    /**
     * Obtiene la siguiente caja objetivo a empujar.
     * @param snapshot La instantánea del estado actual del tablero.
     * @return La coordenada de la próxima caja objetivo a empujar, o null si no hay cajas objetivo disponibles.
     */
    public Coord getNextTarget(Snapshot snapshot){
        int minDistance = Integer.MAX_VALUE;
        Coord bestTarget = null;
        for(Coord target : snapshot.targetPosition){
            if(snapshot.isCornered(position, target)){
                continue; // Si la caja objetivo está bloqueada, buscar otra caja objetivo
            }
            int distance = position.distanceTo(target);
            if(distance < minDistance){ // Si la distancia a la caja objetivo es menor que la distancia mínima encontrada hasta ahora, actualizar la distancia mínima y la mejor caja objetivo
                minDistance = distance;
                bestTarget = target;
            }
        }
        return bestTarget;
    }

    /**
     * Obtiene la dirección en la que el robot debe moverse para empujar la caja objetivo.
     * @param snapshot La instantánea del estado actual del tablero.
     * @param targetPosition La posición de la caja objetivo que se desea empujar.
     * @param lastDirection La última dirección en la que el robot se movió.
     * @return La dirección en la que el robot debe moverse para empujar la caja objetivo, o null si no hay una dirección válida.
     */
    public Direction directionToPush(Snapshot snapshot, Coord targetPosition, Direction lastDirection){
        Direction[] directions = new Direction[4];
        switch(lastDirection){
            case DOWN:
                // Intentar mover la caja en el orden: abajo, derecha, izquierda, arriba.
                directions[0] = Direction.DOWN;
                directions[1] = Direction.RIGHT;
                directions[2] = Direction.LEFT;
                directions[3] = Direction.UP;
                break;
            case LEFT:
                // Intentar mover la caja en el orden: abajo, izquierda, arriba, derecha.
                directions[0] = Direction.DOWN;
                directions[1] = Direction.LEFT;
                directions[2] = Direction.UP;
                directions[3] = Direction.RIGHT;
                break;
            case UP:
                // Intentar mover la caja en el orden: izquierda, derecha, arriba, abajo.
                directions[0] = Direction.LEFT;
                directions[1] = Direction.RIGHT;
                directions[2] = Direction.UP;
                directions[3] = Direction.DOWN;
                break;
            default: // RIGHT
                // Intentar mover la caja en el orden: derecha, abajo, arriba, izquierda.
                directions[0] = Direction.RIGHT;
                directions[1] = Direction.DOWN;
                directions[2] = Direction.UP;
                directions[3] = Direction.LEFT;
                break;
        }

        // Priorizar avanzar hacia la esquina, pero solo si la celda inmediata de la caja está libre.
        if (targetPosition.getY() < snapshot.boardSize - 1
                && canPush(snapshot, targetPosition, Direction.RIGHT)) {
            return Direction.RIGHT;
        }
        if (targetPosition.getX() < snapshot.boardSize - 1
                && canPush(snapshot, targetPosition, Direction.DOWN)) {
            return Direction.DOWN;
        }

        for (Direction direction : directions) {
            if (canPush(snapshot, targetPosition, direction)) {
                return direction;
            }
        }

        return null; // No hay una dirección válida para empujar la caja objetivo
    }

    /**
     * Determina si el robot puede empujar la caja objetivo en la dirección especificada.
     * @param snapshot La instantánea del estado actual del tablero.
     * @param targetPosition La posición de la caja objetivo que se desea empujar.
     * @param direction La dirección en la que se desea empujar la caja objetivo.
     * @return true si el robot puede empujar la caja objetivo en la dirección especificada, false en caso contrario.
     */
    private boolean canPush(Snapshot snapshot, Coord targetPosition, Direction direction) {
        Coord destination = targetPosition.move(direction);
        Coord support = targetPosition.move(direction.opositeDirection());

        return !snapshot.isBoxBlocked(targetPosition, destination) 
            && !snapshot.isBlocked(position, support);
    }

    /**
     * Determina si el robot está detrás de la caja objetivo en la dirección especificada.
     * @param snapshot La instantánea del estado actual del tablero.
     * @param targetPosition La posición de la caja objetivo que se desea empujar.
     * @param direction La dirección en la que se desea empujar la caja objetivo.
     * @return true si el robot está detrás de la caja objetivo en la dirección especificada, false en caso contrario.
     */
    public boolean isRobotBehindTarget(Snapshot snapshot, Coord targetPosition, Direction direction){
        Coord pushCoord = targetPosition.move(direction.opositeDirection());
        return position.equals(pushCoord);
    }

    /**
     * Elige la dirección en la que el robot debe moverse para acercarse a la coordenada especificada.
     * @param snapshot La instantánea del estado actual del tablero.
     * @param coord La coordenada a la que el robot desea acercarse.
     * @param lastDirection La última dirección en la que se movió el robot.
     * @return La dirección en la que el robot debe moverse para acercarse a la coordenada especificada, o null si no hay una dirección válida.
     */
    public Direction chooseMoveToward(Snapshot snapshot, Coord coord, Direction lastDirection){
        Queue<Coord> pending = new ArrayDeque<>();          // Cola para almacenar las coordenadas pendientes de explorar
        Set<Coord> visited = new HashSet<>();               // Conjunto para almacenar las coordenadas ya visitadas
        Map<Coord, Direction> firstMove = new HashMap<>();  // Mapa para almacenar la primera dirección tomada desde la posición inicial hacia cada coordenada

        pending.add(position); // Agregar la posición actual del robot a la cola de pendientes
        visited.add(position); // Agregar la posición actual del robot al conjunto de visitadas

        while (!pending.isEmpty()) { // Mientras haya coordenadas pendientes por explorar
            Coord current = pending.remove(); // Obtener la siguiente coordenada de la cola de pendientes

            for (Direction direction : Direction.values()) {
                Coord next = current.move(direction);

                // Si la coordenada ya ha sido visitada o está bloqueada, continuar con la siguiente dirección
                if (visited.contains(next) || snapshot.isBlocked(position, next)) {
                    continue;
                }

                // Registrar la primera dirección tomada desde la posición inicial hacia la coordenada actual
                Direction initialDirection = current.equals(position) ? direction : firstMove.get(current);

                if (next.equals(coord)) { // Si se ha alcanzado la coordenada deseada, devolver la primera dirección tomada desde la posición inicial
                    return initialDirection;
                }

                visited.add(next);                      // Marcar la coordenada como visitada
                firstMove.put(next, initialDirection);  // Registrar la primera dirección tomada desde la posición inicial hacia la coordenada siguiente
                pending.add(next);                      // Agregar la coordenada siguiente a la cola de pendientes para explorarla más adelante 
            }
        }

        return null; // Si no hay una dirección válida para moverse, devolver null
    }

    /**
     * Espera hasta que haya una caja objetivo disponible para empujar.
     * @return La coordenada de la caja objetivo disponible para empujar, o null si no hay cajas objetivo disponibles y la producción ha terminado.
     */
    private Coord waitUntilTargetAvailable() {
        synchronized (board) {
            while (true) {
                snapshot = board.snapshot(); // Tomar una instantánea del estado actual del tablero

                // Si la producción ha terminado y no hay cajas objetivo disponibles, retornar null
                if (board.isTargetProductionFinished()) {
                    return null;
                }

                Coord target = getNextTarget(snapshot); // Obtener la siguiente caja objetivo a empujar

                if (target != null) { // Si hay una caja objetivo disponible, retornar true
                    return target;
                }

                if (!board.canProduceTarget()) { // Si no hay cajas objetivo disponibles y la producción es imposible, retornar null
                    return null;
                }

                try { // Si no hay cajas objetivo disponibles, esperar hasta que un productor llene una celda con una caja objetivo
                    board.wait();
                } catch (InterruptedException e) { // Si el hilo del robot es interrumpido mientras espera, interrumpir el hilo actual y retornar null
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
    }

    public void run(){
        MoveResult result = null;
        Direction lastDirection = Direction.RIGHT; // Inicializar la última dirección como RIGHT para el primer movimiento
        while(battery > 0){
            Coord targetPosition = waitUntilTargetAvailable(); // Esperar hasta que haya una caja objetivo disponible para empujar
            if (targetPosition == null) { // Si no hay cajas objetivo disponibles y la producción ha terminado, salir del bucle
                break;
            }

            // Obtener la dirección en la que el robot debe moverse para empujar la caja objetivo
            Direction nextMoveDirection = directionToPush(snapshot, targetPosition, lastDirection);

            if(nextMoveDirection == null){
                continue; // Si no hay una dirección válida para empujar la caja objetivo, continuar con la siguiente iteración del bucle
            }

            // Si el robot no está detrás de la caja objetivo en la dirección especificada, elegir una dirección para moverse hacia la celda de soporte de la caja objetivo
            if (!isRobotBehindTarget(snapshot, targetPosition, nextMoveDirection)) {
                // Calcular la coordenada de soporte de la caja objetivo en la dirección opuesta a la dirección de movimiento
                Coord supportCoord = targetPosition.move(nextMoveDirection.opositeDirection());

                // Elegir la dirección en la que el robot debe moverse para acercarse a la coordenada de soporte de la caja objetivo
                nextMoveDirection = chooseMoveToward(snapshot, supportCoord, lastDirection);

                if (nextMoveDirection == null) { // Si no hay una dirección válida para moverse hacia la coordenada de soporte de la caja objetivo, continuar con la siguiente iteración del bucle
                    continue;
                }

                lastDirection = nextMoveDirection; // Actualizar la última dirección como la dirección en la que el robot se movió hacia la coordenada de soporte de la caja objetivo
            }

            // Ejecutar el siguiente movimiento del plan de ejecución
            result = board.moveRobot(id, position, nextMoveDirection);
            
            switch(result){
                // Si el resultado del movimiento es MOVED, PUSHED_TARGET, PUSHED_OBSTACLE, EXTRACTED_TARGET, actualizar la posición del robot y decrementar la batería
                case MOVED:
                case PUSHED_TARGET:
                case PUSHED_OBSTACLE:
                case EXTRACTED_TARGET:
                    position = position.move(nextMoveDirection);
                    battery--;
                    break;
                // Si el resultado del movimiento es BLOCKED o STALE_POSITION, generar un nuevo plan de ejecución
                default:
                    break;
            }

            // Si el resultado del movimiento es EXTRACTED_TARGET y la producción de cajas objetivo ha finalizado, salir del bucle
            if (result == MoveResult.EXTRACTED_TARGET && board.isTargetProductionFinished()) {
                // El robot ha extraído la última caja objetivo y la producción ha terminado, salir del bucle
                break;
            }
        }
        // Una vez que la batería del robot se agota, retirarlo del tablero
        board.removeRobot(id, position, result);
    }
}