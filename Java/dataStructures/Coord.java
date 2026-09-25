package dataStructures;

/**
 * Clase que representa una coordenada en un plano bidimensional.
 */
public class Coord{
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
