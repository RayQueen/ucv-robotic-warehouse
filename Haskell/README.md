<h1 align="center">Almacén Robótico</h1>

El objetivo principal de este proyecto es diseñar e implementar un sistema de control para un robot montacargas dentro de un almacén representado por una matriz bidimensional de $6 \times 6$ coordenadas, comprendidas desde `(0,0)` hasta `(5,5)`. El sistema resuelve un laberinto con obstáculos dinámicos («Cajas de Bloqueo»). El propósito final es trasladar una «Caja Objetivo» desde su coordenada de origen hasta la zona de extracción en `(5,5)` mediante la menor secuencia posible de maniobras físicas.

## Arquitectura del Código e Implementación

La solución se estructura en cuatro módulos lógicos que se comunican de forma descendente, donde la función maestra `solveWarehouse` actúa como el punto de entrada e hilo conductor de todo el sistema. A continuación se describe en detalle cómo se implementó cada sección y cómo cooperan entre sí.

### 1. Inicialización y Validación del Entorno (`initialState`)

El estado se representa mediante el tipo de datos compuesto:

```haskell
type State = (Coord, Coord, [Coord]) -- (Robot, CajaObjetivo, [CajasDeBloqueo])
```

Para asegurar que el almacén inicie en condiciones físicamente consistentes, la función `initialState` actúa como un embudo de seguridad de la siguiente manera:

- **`isValidCoord`**: Valida que una coordenada individual (x, y) se encuentre estrictamente dentro de los límites físicos del tablero (0 󰥽 x 󰥽 5 y 0 󰥽 y 󰥽 5).
    
- **`isValidCoordList`**: Emplea recursión sobre la lista de obstáculos. Extrae la cabeza de la lista, la evalúa con `isValidCoord`, y realiza una conjunción lógica (`&&`) recursiva con el resto de la lista.
    
- **`entitiesNotOverlapped`**: Verifica que no existan colisiones espaciales en el momento inicial. Asegura que la coordenada del robot no coincida con la de la caja objetivo, y que ninguna de ellas coincida con las posiciones de la lista de obstáculos (utilizando la función predefinida `elem`).

Si todas las funciones de validación devuelven `True`, `initialState` empaqueta y retorna el `State` inicial; si falla una sola restricción, se intercepta el error y se devuelve el estado estandarizado `((-1,-1), (-1,-1), [])`.

### 2. Colisiones

La física de empuje del almacén se modela mediante la función `isValidMove`, que calcula la validez del movimiento consultando el estado actual mediante la función auxiliar de detección de colisiones `isEmptySpace`.

- **`isEmptySpace`**: Actúa como un sensor de proximidad. Determina si una coordenada de destino está completamente vacía (no coincide con el robot, la caja objetivo o ninguna caja de bloqueo) y si se encuentra dentro de los límites del tablero.
    
- **`isValidMove`**: Para que el robot pueda desplazarse a una casilla contigua en una dirección dada (por ejemplo, hacia arriba `U`), solo deben cumplirse dos escenarios lógicos excluyentes (evaluados mediante el operador OR lógico `||`):
    
    1. La casilla inmediata contigua `(rx-1, ry)` está vacía. El robot simplemente se mueve allí.
        
    2. La casilla inmediata contigua está ocupada por una caja, pero la casilla que se encuentra a dos posiciones de distancia `(rx-2, ry)` está vacía. Esto significa que el robot puede realizar un empuje válido de la caja a un espacio vacío.

Esta implementación vectorial evita de forma natural el «empuje múltiple» (dos cajas contiguas no pueden ser empujadas porque la distancia máxima de análisis queda acotada a un rango de dos casillas) y prohíbe que cualquier objeto sea empujado fuera de los límites de la matriz de 6 · 6.

### 3. Ejecución de Movimiento con Propagación de Estados

Una vez que un movimiento ha sido clasificado como válido por `isValidMove`, la función `applyMove` se encarga de proyectar la transformación matemática en el tablero:

```haskell
applyMove (robot, objBox, obsBox) m = (
    (moveCoord robot m), 
    (applyMoveBox (moveCoord robot m) objBox m), 
    (applyMoveBoxList (moveCoord robot m) obsBox m)
  )
```

- **`moveCoord`**: Desplaza una tupla de coordenadas sumando o restando una unidad en las filas o columnas según el constructor de movimiento (`U`, `D`, `L`, `R`).
    
- **`applyMoveBox` / `applyMoveBoxList`**: Determinan si el robot ha colisionado con una caja al moverse. Si la posición futura del robot coincide con la posición actual de la Caja Objetivo o con una Caja de Bloqueo, esa caja es proyectada una posición más allá en la dirección `m`. El resto de las cajas de bloqueo se dejan intactas mediante recursión sobre la lista.    

### 4. Resolución Óptima Mediante la Búsqueda en Anchura (BFS)

Para cumplir con la restricción de garantizar la secuencia de movimientos **mínima** (óptima) se implementó un algoritmo de **Búsqueda en Anchura (BFS)** de la siguiente manera:

- **Cola de Exploración**: Representada como una lista en `solveWarehouse'` donde cada elemento contiene la tupla `(State, [State], Int)`, que almacena el estado actual a analizar, el camino acumulado desde el inicio para llegar a él, y la cantidad total de movimientos ejecutados.
    
- **Gestión de Visitados**: Se pasa una lista acumuladora de estados `visited` en cada llamada recursiva. Si el estado al frente de la cola de exploración ya fue analizado (`state 'elem' visited`), se descarta de inmediato para romper ciclos infinitos causados por movimientos repetitivos de ida y vuelta.
    
- **Garantía de Solución Mínima**: Cuando se descubren nuevos estados válidos a través de la función auxiliar `nextStates`, estos son concatenados estrictamente al final de la lista de exploración empleando el operador `(queue ++ nextStates ...)`. Esto asegura un procesamiento de tipo FIFO, de modo que el algoritmo explora exhaustivamente todos los caminos de longitud k antes de proceder con caminos de longitud k+1.
    
- **Comprensión de Listas**: La función `nextStates` genera los estados sucesores evaluando los movimientos del conjunto `[U, D, L, R]`. Filtra los movimientos que violan la física del juego, genera el nuevo estado con `applyMove`, cerciorándose de que sea válido frente a `initialState` y comprueba que no pertenezca al conjunto de estados ya visitados.

## La Expresión `deriving (Show, Eq)`

En Haskell, una clase de tipos define una interfaz común o un conjunto de comportamientos que diferentes tipos de datos pueden implementar de manera polimórfica. La expresión `deriving (Show, Eq)` colocada al final de la declaración del tipo `Move` instruye al compilador de GHC para generar de manera automática e implícita las instancias correspondientes a las clases de tipos `Show` y `Eq` para nuestro nuevo tipo de datos.

- **La clase `Eq`**: Define la interfaz de comparación de igualdad para verificar si dos elementos son equivalentes o diferentes mediante los operadores `(==)` y `(/=)`. Sin `deriving Eq`, intentar evaluar la expresión `m == U` en Haskell lanzará un error de compilación debido a que el sistema no sabe cómo comparar estructuralmente los constructores de `Move`. Dos valores de tipo `Move` serán iguales si y solo si están construidos con el mismo constructor (por ejemplo, `U == U` evalúa a `True` y `U == D` a `False`). En el código, esto es crucial en `isValidMove` y `moveCoord` para comparar las acciones, y en el BFS para verificar pertenencia en listas mediante `elem`.

- **La clase `Show`**: Define la función `show :: a -> String`, la cual toma un valor del tipo de dato y devuelve su representación en forma de cadena de caracteres inteligible. Si se intentara cargar el código en el intérprete de GHCI y evaluar una expresión que retorne un valor de tipo `Move` o un `State` (que hereda indirectamente la capacidad de impresión), el intérprete fallaría con un mensaje de error indicando que no hay una instancia disponible para mostrar el tipo en pantalla. Al agregar esta clase, se genera una instancia por defecto que toma el nombre del constructor del dato y lo convierte a texto literal (por ejemplo, el constructor `U` se mostrará en consola como `"U"`). 