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

    @Override
    public boolean equals(Object obj){
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;
        Coord coord = (Coord) obj;
        return x == coord.x && y == coord.y;
    }

    @Override
    public int hashCode(){
        return java.util.Objects.hash(x, y);
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

    /**
     * Calcula la distancia de Manhattan entre esta coordenada y otra coordenada.
     * @param other La otra coordenada con la que se calculará la distancia.
     * @return La distancia de Manhattan entre las dos coordenadas.
     */
    public int distanceTo(Coord other){
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }
}
