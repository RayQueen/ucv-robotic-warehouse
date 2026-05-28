
-- Definición de los tipos de Datos
type Coord = (Int, Int) -- (Fila, Columna)
data Move = U | D | L | R deriving (Show, Eq) -- Tipo comparable (==, /=) e imprimible
type State = (Coord, Coord, [Coord]) -- (Robot, CajaObjetivo, CajasDeBloqueo)
data GameTree a = Vacio
                | Nodo a [GameTree a]
                deriving (Show, Eq) -- Tipo comparable (==, /=) e imprimible

isValidCoord :: (Int, Int) -> Bool
isValidCoord (x,y)
    | 0 <= x && x <= 5 && 0 <= y && y <= 5 = True
    | otherwise = False

isValidCoordList :: [(Int, Int)] -> Bool
isValidCoordList [] = True
isValidCoordList (x:xs) = isValidCoord x && isValidCoordList xs

notOverlapped :: Coord -> Coord -> [Coord] -> Bool
notOverlapped robot objBox obsBox
    | robot == objBox = False
    | notOverlappedList robot objBox obsBox = True
    | otherwise = False
        where
            notOverlappedList _ _ [] = True
            notOverlappedList x y (z:zs)
                | x == z = False
                | y == z = False
                | otherwise = notOverlappedList x y zs

-- Inicialización del estado
initialState :: Coord -> Coord -> [Coord] -> State
initialState robot objBox obsBox 
    |   isValidCoord robot && 
        isValidCoord objBox && 
        isValidCoordList obsBox &&
        notOverlapped robot objBox obsBox 
        = (robot, objBox, obsBox)
    | otherwise = ((-1,-1), (-1,-1), [])

-- Validación de Movimientos
-- Asume una coordenada no valida como un espacio ocupado
isEmptySpace :: Coord -> State -> Bool
isEmptySpace c (x,y,z)
    | not (isValidCoord c) = False
    | c == x = False
    | c == y = False
    | not (isEmptySpaceList c z) = False
    | otherwise = True

isEmptySpaceList :: Coord -> [Coord] -> Bool
isEmptySpaceList _ [] = True
isEmptySpaceList x (y:ys)
    | x == y = False
    | otherwise = isEmptySpaceList x ys

isValidMove :: State -> Move -> Bool
isValidMove ((rx,ry), objBox, obsBox) m
    | m == U && (isEmptySpace ((rx-1),ry) ((rx,ry), objBox, obsBox) || isEmptySpace ((rx-2),ry) ((rx,ry), objBox, obsBox)) = True
    | m == D && (isEmptySpace ((rx+1),ry) ((rx,ry), objBox, obsBox) || isEmptySpace ((rx+2),ry) ((rx,ry), objBox, obsBox)) = True
    | m == L && (isEmptySpace (rx,(ry-1)) ((rx,ry), objBox, obsBox) || isEmptySpace (rx,(ry-2)) ((rx,ry), objBox, obsBox)) = True
    | m == R && (isEmptySpace (rx,(ry+1)) ((rx,ry), objBox, obsBox) || isEmptySpace (rx,(ry+2)) ((rx,ry), objBox, obsBox)) = True
    | otherwise = False

-- Ejecución de Movimiento
applyMove :: State -> Move -> State
applyMove (robot, objBox, obsBox) m = ((moveCoord robot m), (applyMoveBox (moveCoord robot m) objBox m), (applyMoveBoxList (moveCoord robot m) obsBox m))

applyMoveBox :: Coord -> Coord -> Move -> Coord
applyMoveBox c objBox m
    | c == objBox = moveCoord objBox m
    | otherwise = objBox

applyMoveBoxList :: Coord -> [Coord] -> Move -> [Coord]
applyMoveBoxList _ [] _ = []
applyMoveBoxList c (x:xs) m
    | c == x = (moveCoord x m):xs
    | otherwise = x:(applyMoveBoxList c xs m)

moveCoord :: Coord -> Move -> Coord
moveCoord (x,y) m
    | m == U = (x-1,y)
    | m == D = (x+1,y)
    | m == L = (x,y-1)
    | m == R = (x,y+1)

-- Mejor solución
-- Se usa BFS
solveWarehouse :: State -> (Int, [State])
solveWarehouse st
    | not (isValidState st) = (0, [])
    | otherwise = bfsWarehouse [(st, [st])] [st]
    where
        isValidState (robot, objBox, obsBox) =
            isValidCoord robot &&
            isValidCoord objBox &&
            isValidCoordList obsBox &&
            notOverlapped robot objBox obsBox

bfsWarehouse :: [(State, [State])] -> [State] -> (Int, [State])
bfsWarehouse [] _ = (0, [])
bfsWarehouse ((st, path):queue) visited
    | isGoalState st = (length path - 1, path)
    | otherwise = bfsWarehouse (queue ++ enqueued) (visited ++ newStates)
    where
        isGoalState (_, objBox, _) = objBox == (5,5)
        -- successors of current state
        succs = [ applyMove st m | m <- [U, D, L, R], isValidMove st m ]
        -- states already known (visited or queued)
        queued = map fst queue
        known = visited ++ queued
        -- filter new states that are not known
        newStates = filter (`notElem` known) succs
        -- enqueue new states with their paths
        enqueued = [ (s, path ++ [s]) | s <- newStates ]
        
