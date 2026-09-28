package dataStructures;

import java.util.ArrayList;

public class Snapshot {
    public ObjectType[][] boardState;
    public ArrayList<Coord> robotPosition = new ArrayList<>();
public ArrayList<Coord> targetPosition = new ArrayList<>();
public ArrayList<Coord> obstaclePosition = new ArrayList<>();
    public int boardSize;

    public Snapshot(ObjectType[][] boardState, ArrayList<Coord> robotPosition, ArrayList<Coord> targetPosition, ArrayList<Coord> obstaclePosition, int boardSize){
        this.boardState = boardState;
        this.robotPosition = robotPosition;
        this.targetPosition = targetPosition;
        this.obstaclePosition = obstaclePosition;
        this.boardSize = boardSize;
    }

    /**
     * Determina si una celda está bloqueada para un robot en particular.
     * @param coordRobot La coordenada del robot que intenta moverse.
     * @param coord La coordenada de la celda que se desea verificar.
     * @return true si la celda está bloqueada, false en caso contrario.
     */
    public boolean isBlocked(Coord coordRobot, Coord coord){
        if(coord.getX() < 0 || coord.getX() >= boardSize || coord.getY() < 0 || coord.getY() >= boardSize){
            return true;
        }
        if(coordRobot.getX() == coord.getX() && coordRobot.getY() == coord.getY()){
            return false;
        }
        return boardState[coord.getX()][coord.getY()] != ObjectType.EMPTY;
    }

    /**
     * Determina si una celda está bloqueada para que un robot en particular mueva una caja.
     * @param coordRobot La coordenada del robot que intenta moverse.
     * @param coord La coordenada de la celda que se desea verificar.
     * @return true si la celda está bloqueada, false en caso contrario.
     */
    public boolean isBoxBlocked(Coord coordRobot, Coord coord){
        if(coord.getX() < 1 || coord.getX() >= boardSize || coord.getY() < 1 || coord.getY() >= boardSize){
            return true;
        }
        if(coordRobot.getX() == coord.getX() && coordRobot.getY() == coord.getY()){
            return false;
        }
        return boardState[coord.getX()][coord.getY()] != ObjectType.EMPTY;
    }

    /**
     * Determina si una caja está acorralada por obstáculos de cualquier tipo.
     * @param coordRobot La coordenada del robot que intenta mover la caja.
     * @param box La coordenada de la caja que se desea verificar.
     * @return true si la caja está acorralada, false en caso contrario.
     */
    public boolean isCornered(Coord coordRobot, Coord box) {
        for (Direction direction : Direction.values()) {
            Coord destination = box.move(direction);
            Coord support = box.move(direction.opositeDirection());

            boolean validDestination =
                destination.getX() > 0 &&
                destination.getX() < boardSize &&
                destination.getY() > 0 &&
                destination.getY() < boardSize &&
                boardState[destination.getX()][destination.getY()] == ObjectType.EMPTY;

            boolean validSupport =
                support.getX() >= 0 &&
                support.getX() < boardSize &&
                support.getY() >= 0 &&
                support.getY() < boardSize &&
                (boardState[support.getX()][support.getY()] == ObjectType.EMPTY
                || support.equals(coordRobot));

            if (validDestination && validSupport) {
                return false;
            }
        }

        return true;
    }
}
