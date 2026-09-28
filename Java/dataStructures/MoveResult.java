package dataStructures;

/**
 * <pr>
 * Enum que representa los posibles resultados del movimiento de un robot en el tablero.
 * MOVED: El robot se movió exitosamente a una celda vacía.
 * PUSHED_TARGET: El robot empujó una caja objetivo a una celda vacía.
 * PUSHED_OBSTACLE: El robot empujó una caja obstáculo a una celda vacía.
 * BLOCKED: El robot no pudo moverse porque la posición de destino está ocupada.
 * STALE_POSITION: El robot no pudo moverse porque la celda de destino quedó ocupada.
 * EXTRACTED_TARGET: El robot extrajo una caja objetivo del tablero.
 * </pr>
 */
public enum MoveResult {
    MOVED,
    PUSHED_TARGET,
    PUSHED_OBSTACLE,
    BLOCKED,
    STALE_POSITION,
    EXTRACTED_TARGET
}
