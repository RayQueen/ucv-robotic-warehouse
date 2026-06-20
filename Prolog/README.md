<h1 align="center">Almacén Robótico</h1>

El objetivo principal de este proyecto es diseñar e implementar un sistema de control para un robot montacargas dentro de un almacén representado por una matriz lógica bidimensional de 6 × 6 coordenadas, comprendidas desde `(0,0)` hasta `(5,5)`. El sistema resuelve un laberinto con obstáculos dinámicos («Cajas de Bloqueo»). El propósito final es trasladar una «Caja Objetivo» desde su coordenada de origen hasta la zona de extracción en `(5,5)` mediante la menor secuencia posible de maniobras físicas.

## El Paradigma Lógico y la Base de Conocimientos Dinámica

Una de las decisiones de diseño más importantes en esta solución es la coexistencia de dos formas de representación del estado del juego:

1. **Base de Conocimientos Dinámica (Física Inicial)**: Se utiliza mediante predicados dinámicos (`:- dynamic`). Es ideal para configurar el escenario inicial como una base de datos de consulta. No obstante, realizar la búsqueda en anchura alterando esta base de conocimientos de forma interactiva (usando continuamente `assert` y `retract`) introduciría efectos colaterales destructivos que harían inviable el _backtracking_ del buscador.

2. **Estructura de Términos (Búsqueda)**: Para la búsqueda de caminos, el estado se encapsula en un término estructurado `state(Robot, Target, BlockingList)`. Esto permite que el motor de Prolog evalúe múltiples hipótesis, retroceda en el árbol de búsqueda de forma limpia y mantenga un aislamiento estricto entre las diferentes ramas de exploración del BFS.

A diferencia de los enfoques estructurados tradicionales que requieren de bucles explícitos o mapas de datos para evaluar alternativas, Prolog aprovecha su **no-determinismo** nativo. Cuando se invoca el predicado `direction(Move, _, _)`, Prolog no selecciona una única opción rígida; unifica el término `Move` de manera secuencial con los hechos disponibles (`u`, `d`, `l`, `r`) a través del mecanismo de backtracking.

La unificación actúa simultáneamente como un mecanismo de asignación, de comparación espacial y de deconstrucción del término de estado. Por ejemplo, en el predicado de sustitución `replaceInList/4`:

```prolog
replaceInList(Old, New, [Old | Tail], [New | Tail]).
```

Este simple hecho unifica la cabeza de la lista directamente con la posición antigua (`Old`), y la reemplaza de inmediato con la nueva posición (`New`) en el argumento de salida, todo en una sola operación atómica del motor de inferencia de Prolog.

## Arquitectura del Código e Implementación

La solución se estructura en cuatro módulos lógicos en Prolog, donde el predicado principal `solveWarehouse/2` actúa como el punto de entrada de todo el sistema. A continuación se describe en detalle cómo se implementó cada sección y cómo cooperan entre sí.

### 1. Inicialización del Tablero (`initialBoard/3`)

En el paradigma lógico de Prolog, se interactúa directamente con una base de conocimientos dinámica que almacena hechos sobre el estado físico del tablero mediante los predicados `robot/2`, `caja_objetivo/2` y `caja_bloqueo/2`.

Para asegurar que el almacén se inicialice en condiciones físicamente consistentes, el predicado `initialBoard(RobotCoord, TargetCoord, BlockingBoxes)` actúa como un filtro de validación de la siguiente manera:

- **`isWithinBounds/1`**: Valida que una coordenada individual `(Row, Column)` se encuentre estrictamente dentro de los límites físicos del tablero (0 ≤ Row ≤ 5 y 0 ≤ Column ≤ 5).

- **`areAllWithinBounds/1`**: Evalúa de manera recursiva si todos los elementos de una lista de coordenadas cumplen con los límites establecidos.

- **`areAllUnique/1`**: Verifica que no existan colisiones espaciales en el momento inicial. Agrupa el robot, la caja objetivo y las cajas de bloqueo en una única lista y evalúa la no-pertenencia recursiva (`\+ member/2`) para garantizar que ninguna entidad se solape.

Si todas las restricciones lógicas se satisfacen con éxito, el predicado procede a limpiar la memoria activa mediante directivas `retractall/1` para evitar inconsistencias con ejecuciones anteriores. Posteriormente, registra las nuevas posiciones iniciales utilizando el predicado de inserción al final de la base de conocimientos `assertz/1` (y de manera recursiva mediante `assertBlockingBoxes/1` para la lista de obstáculos). Si alguna validación lógica o espacial falla, el predicado falla (`false`) y no altera la base de datos dinámica.

### 2. Validación de Movimientos (`isValidMove/2`)

La validación física de un movimiento en Prolog se modela de manera declarativa aprovechando el concepto de la unificación.

- **`isValidMove(CurrentState, Move)`**: En lugar de duplicar la lógica de análisis espacial o crear complejos flujos condicionales, este predicado simplemente delega la verificación al motor de transiciones. Se evalúa si existe un estado resultante S' al aplicar el movimiento M sobre el estado actual S empleando el predicado `moveRobot/3`. Si la transición es lógica y físicamente factible, `isValidMove/2` tiene éxito; en caso contrario, falla por la propia naturaleza relacional de Prolog. Esto reduce significativamente la duplicación de código y el riesgo de inconsistencias físicas en la simulación.

### 3. Ejecución de Movimiento y Transición de Estados (`moveRobot/3`)

La física de empuje del almacén se modela mediante tres cláusulas excluyentes del predicado `moveRobot(CurrentState, Move, NewState)`. Cada una de estas cláusulas realiza un «patrón de coincidencia» (pattern matching) implícito mediante unificación y valida límites antes de proceder:

1. **Movimiento a celda vacía**: Se calcula la nueva coordenada del robot con ayuda de `direction/3` y `addCoordinates/3`. Se verifica que esté dentro del tablero, que no coincida con la posición de la caja objetivo (`NewRobotPos \= TargetPos`) y que no esté ocupada por un obstáculo (`\+ member(NewRobotPos, BlockingList)`).

2. **Empuje de la Caja Objetivo**: Si el robot intenta moverse exactamente a la coordenada actual de la «Caja Objetivo», se calcula la proyección de la caja un paso más allá en la dirección del movimiento. Este movimiento es válido únicamente si la nueva posición de la caja está dentro de los límites y libre de obstáculos. El estado se actualiza trasladando al robot a la antigua posición de la caja y a la caja a su nueva ubicación.

3. **Empuje de una Caja de Bloqueo**: Si el robot se desplaza hacia una coordenada que pertenece a `BlockingList`, se calcula el desplazamiento del obstáculo. Se verifica que la celda de destino esté libre (no puede salirse del tablero, ni coincidir con la caja objetivo o con otra caja de bloqueo). La lista de bloqueos se actualiza utilizando el predicado auxiliar `replaceInList/4`, que localiza el elemento modificado mediante recursión y unificación, sustituyéndolo por la nueva posición.

Se emplea el operador cut (`!`) al final de cada cláusula exitosa para asegurar que, una vez que se determina unívocamente qué tipo de movimiento se realizó, Prolog no intente explorar de forma redundante las otras opciones en caso de reevaluación (_backtracking_).

### 4. Resolución Óptima mediante Búsqueda en Anchura (BFS)

Para cumplir con la restricción de garantizar la secuencia de movimientos **mínima** (óptima) se implementó un algoritmo de **Búsqueda en Anchura (BFS)** adaptado a la naturaleza relacional de Prolog:

- **Estandarización de Estados (`standardizeState/2`)**: Un problema clásico al representar el estado como `state(Robot, Target, BlockingList)` es que el orden de los obstáculos en la lista no altera la física del tablero, pero sí crea estados lógicamente distintos para el motor de Prolog (por ejemplo, `[(1,1), (2,2)]` y `[(2,2), (1,1)]`). Para evitar que el control de visitados falle en reconocerlos, el predicado ordena la lista de obstáculos usando `sort/2`, garantizando la unicidad del estado.

- **Cola de Exploración**: La cola del BFS se representa como una lista de listas de la forma `[[EstadoCanon, CaminoAcumulado] | RestoCola]`.

- **Garantía de Solución Mínima**: Se utiliza la meta-predicación `findall/3` para expandir el nodo actual de forma no-determinista. Esto permite recolectar todas las transiciones válidas realizables en un solo paso (`moveRobot/3`), normalizarlas a su forma canónica y filtrar aquellas que ya pertenecen al conjunto de estados `Visited`.

- **Estrategia FIFO**: Los nuevos nodos generados se anexan al final de la cola mediante `append/3`, forzando al motor de inferencia a explorar todos los caminos de longitud k antes de proceder con los de longitud k + 1. El algoritmo se detiene inmediatamente con éxito (`!`) cuando el primer elemento de la cola alcanza la coordenada de extracción `(5,5)`.

## Guía de Ejecución

Para ejecutar el programa se requiere de un intérprete de Prolog compatible con el estándar ISO, recomendándose el uso de **SWI-Prolog**. A continuación se describen los pasos para iniciar e interactuar con el código:

1. **Abrir la terminal o consola del sistema** en el directorio donde se encuentre el archivo `Proyecto2.pl`.

2. **Iniciar SWI-Prolog** cargando el archivo de código fuente:

    ```bash
    swipl Proyecto2.pl
    ```

3. Una vez que el archivo ha sido compilado exitosamente, la consola de Prolog mostrará el indicador `?-`, quedando lista para recibir consultas.

## Casos de Prueba y Salidas del Sistema

A continuación se listan consultas reales para evaluar el correcto funcionamiento de cada sección del código implementado:

- **Caso 1:** La caja objetivo se encuentra a un movimiento de la meta:

    ```prolog
    ?- solveWarehouse(state((5,3), (5,4), []), Solution).
    Solution = [r].
    ```

- **Caso 2:** El robot debe rodear la caja para empujarla hacia abajo:

    ```prolog
    ?- solveWarehouse(state((4,4), (4,5), []), Solution).
    Solution = [u, r, d].
    ```

- **Caso 3:** No hay rutas lógicas posibles para mover la caja objetivo a la meta:

    ```
    ?- solveWarehouse(state((0,0), (0,1), [(0,2), (1,1), (1,2)]), Solution).
    false.
    ```

## Referencias

1. **Sterling, L., & Shapiro, E. Y.** (1994). _The Art of Prolog: Advanced Programming Techniques_. MIT Press. (Utilizado para el diseño de acumuladores y la estructuración del algoritmo BFS mediante colas).

2. **Bratko, I.** (2011). _Prolog Programming for Artificial Intelligence_. Addison-Wesley. (Referenciado para la modelación de problemas de espacio de estados y juegos de planificación robótica).

3. **SWI-Prolog Documentation**. _Dynamic Databases & All-solutions Predicates_ (`findall/3`, `assertz/1`, `retractall/1`). Recuperado de: [https://www.swi-prolog.org/](https://www.swi-prolog.org/ "null"). (Consultado para la correcta sintaxis y manejo de efectos secundarios en predicados dinámicos).

4. **Material de Apoyo Académico**. Escuela de Computación, Facultad de Ciencias, Universidad Central de Venezuela (UCV). _Lenguajes de Programación - Paradigma Lógico_. (Guías de clase sobre unificación, corte y negación por falla `\+`).
