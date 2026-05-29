
-- Definición de los tipos de Datos
type Coord = (Int, Int) -- (Fila, Columna)
data Move = U | D | L | R deriving (Show, Eq) -- Tipo comparable (==, /=) e imprimible
type State = (Coord, Coord, [Coord]) -- (Robot, CajaObjetivo, CajasDeBloqueo) = (robot, objBox, obsBox)

-- Inicialización del estado
isValidCoord :: (Int, Int) -> Bool -- Valida si una coordenada esta dentro de los límites del almacen (0 a 5)
isValidCoord (x,y)
    | 0 <= x && x <= 5 &&       -- Valida que la fila este entre 0 y 5
      0 <= y && y <= 5 = True   -- Valida que la columna este entre 0 y 5
    | otherwise = False         -- De lo contrario, la coordenada no es valida

isValidCoordList :: [(Int, Int)] -> Bool -- Valida una lista de coordenadas
isValidCoordList [] = True                                      -- Si la lista esta vacia, es valida
isValidCoordList (x:xs) = isValidCoord x && isValidCoordList xs -- Valida la primera coordenada y luego valida el resto de la lista recursivamente

entitiesNotOverlapped :: Coord -> Coord -> [Coord] -> Bool -- Valida que el robot, la caja objetivo y las cajas de bloqueo no se solapen entre si
entitiesNotOverlapped robot objBox obsBox
    | robot == objBox = False                               -- Verifica que el robot y la caja objetivo no esten en la misma coordenada
    | obsBox /= [] && 
    (robot `elem` obsBox || objBox `elem` obsBox) = False   -- El robot y la caja objetivo no pueden estar en la misma coordenada que ninguna de las cajas de bloqueo
    | otherwise = True                                      -- Si no hay coordenadas iguales, el estado es valido

initialState :: Coord -> Coord -> [Coord] -> State -- Valida las coordenadas de entrada y devuelve el estado inicial si son validas, o un estado con coordenadas invalidas si no lo son
initialState robot objBox obsBox 
    |   isValidCoord robot &&                       -- Valida que la coordenada del robot sea valida
        isValidCoord objBox &&                      -- Valida que la coordenada de la caja objetivo sea valida
        isValidCoordList obsBox &&                  -- Valida que las coordenadas de las cajas de bloqueo sean validas
        entitiesNotOverlapped robot objBox obsBox   -- Valida que el robot, la caja objetivo y las cajas de bloqueo no se solapen entre si
        = (robot, objBox, obsBox)
    | otherwise = ((-1,-1), (-1,-1), [])            -- De lo contrario, devuelve un estado con coordenadas invalidas

-- Validación de Movimientos
-- Asume una coordenada no valida como un espacio ocupado
isEmptySpace :: Coord -> State -> Bool -- Valida si una coordenada es un espacio vacio, es decir, no esta ocupada por ninguna entidad (robot, caja objetivo o cajas de bloqueo)
isEmptySpace c (x,y,z)
    | not (isValidCoord c) = False  -- Si la coordenada no es valida, se considera un espacio ocupado
    | c == x = False                -- Si la coordenada es igual a la del robot, se considera un espacio ocupado
    | c == y = False                -- Si la coordenada es igual a la de la caja objetivo, se considera un espacio ocupado
    | c `elem` z = False            -- Si la coordenada esta en la lista de cajas de bloqueo, se considera un espacio ocupado
    | otherwise = True              -- De lo contrario, el espacio es vacio

isValidMove :: State -> Move -> Bool -- Valida si un movimiento es valido para el estado actual, es decir, si el robot puede moverse en la dirección indicada sin salir del almacen o si puede empujar una caja a una coordenada vacia
isValidMove ((rx,ry), objBox, obsBox) m
    | m == U && (isEmptySpace ((rx-1),ry) ((rx,ry), objBox, obsBox) || isEmptySpace ((rx-2),ry) ((rx,ry), objBox, obsBox)) = True
    | m == D && (isEmptySpace ((rx+1),ry) ((rx,ry), objBox, obsBox) || isEmptySpace ((rx+2),ry) ((rx,ry), objBox, obsBox)) = True
    | m == L && (isEmptySpace (rx,(ry-1)) ((rx,ry), objBox, obsBox) || isEmptySpace (rx,(ry-2)) ((rx,ry), objBox, obsBox)) = True
    | m == R && (isEmptySpace (rx,(ry+1)) ((rx,ry), objBox, obsBox) || isEmptySpace (rx,(ry+2)) ((rx,ry), objBox, obsBox)) = True
    | otherwise = False

-- Ejecución de Movimiento
applyMove :: State -> Move -> State -- Aplica un movimiento al estado actual, actualizando la posición del robot y alguna de las cajas si es necesario, y devuelve el nuevo estado resultante del movimiento aplicado
applyMove (robot, objBox, obsBox) m = ((moveCoord robot m), (applyMoveBox (moveCoord robot m) objBox m), (applyMoveBoxList (moveCoord robot m) obsBox m))

applyMoveBox :: Coord -> Coord -> Move -> Coord -- Aplica un movimiento a una caja, actualizando su posición si el robot la empuja, y devuelve la nueva coordenada de la caja
applyMoveBox c objBox m
    | c == objBox = moveCoord objBox m  -- Si la coordenada del robot despues de moverse es igual a la coordenada de la caja objetivo, se mueve la caja en la misma direccion del movimiento
    | otherwise = objBox                -- De lo contrario, la caja no se mueve y se devuelve su coordenada original

applyMoveBoxList :: Coord -> [Coord] -> Move -> [Coord] -- Aplica un movimiento a una lista de cajas, actualizando sus posiciones si el robot empuja alguna de ellas, y devuelve la nueva lista de coordenadas de las cajas
applyMoveBoxList _ [] _ = []                    -- Si la lista esta vacia no hay cajas que mover, asi que se devuelve una lista vacia
applyMoveBoxList c (x:xs) m 
    | c == x = (moveCoord x m):xs               -- Si la coordenada del robot despues de moverse es igual a la coordenada de una caja de bloqueo, se mueve esa caja en la misma direccion del movimiento y se devuelve la lista con la caja movida y el resto de las cajas sin cambios
    | otherwise = x:(applyMoveBoxList c xs m)   -- De lo contrario, la caja no se mueve y se devuelve la lista con esa caja sin cambios y el resto de las cajas procesadas recursivamente

moveCoord :: Coord -> Move -> Coord -- Aplica un movimiento a una coordenada, la actualiza en funcion del movimiento, y devuelve la nueva coordenada resultante
moveCoord (x,y) m
    | m == U = (x-1,y) 
    | m == D = (x+1,y)
    | m == L = (x,y-1)
    | m == R = (x,y+1)

-- Mejor solución mediante bfs

isGameSolved :: State -> Bool -- Verifica si la caja objetivo ha alcanzado la zona de extraccion
isGameSolved (_, objBox, _) = objBox == (5,5)

solveWarehouse :: State -> (Int, [State]) -- Dado un estado inicial, encuentra la secuencia minima de movimientos que soluciona el juego y devuelve el conteo de movimientos y los estados para dicha solucion
solveWarehouse (robot, objBox, obsBox)
    | initialState robot objBox obsBox == ((-1,-1), (-1,-1), []) = (0, [])                      -- Si el estado inicial no es valido, se devuelve un conteo de movimientos de 0 y una lista vacia de estados
    | otherwise = solveWarehouse' [((robot, objBox, obsBox), [(robot, objBox, obsBox)], 0)] []  -- Se inicia el algoritmo bfs para encontrar la solucion, comenzando con el estado inicial

solveWarehouse' :: [(State, [State], Int)] -> [State] -> (Int, [State]) -- Algoritmo bfs para encontrar la solucion del juego, recibe una cola de estados por explorar (con su camino recorrido y conteo de movimientos) y una lista de estados ya visitados, y devuelve el conteo de movimientos y los estados para la solucion encontrada
solveWarehouse' [] _ = (0, [])                                                          -- Si no hay más estados por explorar, no se encontró solución
solveWarehouse' ((state, path, count):queue) visited
    | state `elem` visited = solveWarehouse' queue visited                              -- Si el estado ya ha sido visitado, se omite y se continúa con la siguiente iteración
    | isGameSolved state = (count, reverse path)                                        -- Si se encuentra la solucion, se devuelve el conteo de movimientos y el camino recorrido (revertido para mostrar desde el estado inicial)
    | otherwise = solveWarehouse' (queue ++ nextStates state path count) (state:visited)-- Se agregan los nuevos estados a la cola de exploración y se marca el estado actual como visitado
    where
        nextStates currentState currentPath currentCount = -- Genera los nuevos estados a partir del estado actual, el camino recorrido y el conteo de movimientos
            [(nextState, nextState:currentPath, currentCount + 1) |                         
                m <- [U, D, L, R],                                                          -- Para cada movimiento posible (U, D, L, R)
                isValidMove currentState m,                                                 -- Verifica si el movimiento es válido para el estado actual
                let nextState = applyMove currentState m,                                   -- Aplica el movimiento para obtener el siguiente estado
                let (nextRobot, nextObjBox, nextObsBox) = nextState,                        -- Descompone el siguiente estado en sus componentes (robot, caja objetivo, cajas de bloqueo)
                initialState nextRobot nextObjBox nextObsBox /= ((-1,-1), (-1,-1), []),     -- Verifica que el siguiente estado sea válido (no tenga coordenadas inválidas ni solapamientos)
                not (nextState `elem` visited)]                                             -- Verifica que el siguiente estado no haya sido visitado previamente para evitar ciclos
