package dataStructures;

/**
 * Enum que representa las direcciones posibles en las que un robot puede moverse.
 */
public enum Direction{
    UP, DOWN, LEFT, RIGHT;

    public Direction opositeDirection(){
        switch(this){
            case UP:
                return Direction.DOWN;
            case DOWN:
                return Direction.UP;
            case LEFT:
                return Direction.RIGHT;
            case RIGHT:
                return Direction.LEFT;
            default:
                return null;
        }
    }
}
