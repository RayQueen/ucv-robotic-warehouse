<h1 align="center">Almacén Robótico</h1>

El objetivo de este proyecto es controlar múltiples robots montacargas dentro de un almacén representado por una matriz bidimensional de $6 \times 6$, con coordenadas desde `(0,0)` hasta `(5,5)`. Los robots se ejecutan como hilos concurrentes y deben trasladar «Cajas Objetivo» hasta la zona de extracción `(5,5)`, mientras comparten el tablero con productores que generan dichas «Cajas Objetivo» y «Cajas de Bloqueo».

## Arquitectura del Código e Implementación

La solución está organizada alrededor del recurso crítico `Board`. La clase `Proyecto3` que actúa como `main` construye el tablero, crea los hilos y espera su finalización. Los productores insertan cajas y los robots consultan snapshots, calculan movimientos y modifican el tablero mediante operaciones sincronizadas.

La estructura principal del proyecto es:

```text
Java/
├── Proyecto3.java                 Punto de entrada de la aplicación
├── criticalResources/
│   ├── Board.java                 Estado y operaciones atómicas del tablero
│   └── Logger.java                Registro de eventos y ticks
├── dataStructures/
│   ├── Coord.java                 Coordenadas y desplazamientos
│   ├── Direction.java             Direcciones ortogonales
│   ├── Event.java                 Tipos de eventos del registro
│   ├── MoveResult.java             Resultado de un movimiento
│   ├── ObjectType.java             Tipos de objetos del tablero
│   ├── Reader.java                 Lectura y validación del archivo de entrada
│   └── Snapshot.java               Copia consultable del estado del tablero
└── threads/
	├── ConveyorBelt.java           Hilos productores
	└── Robot.java                  Hilos robots
```

### 1. Entrada e inicialización (`Proyecto3` y `Reader`)

`Proyecto3` recibe como argumento la ruta de un archivo de texto con los parámetros del problema. `Reader.obtainArguments()` ignora las etiquetas y almacena únicamente los valores numéricos. El formato utilizado es:

```text
Cajas_Objetivo_Iniciales, <número de cajas objetivo>
Cajas_Bloqueo_Iniciales, <número de cajas de bloqueo>
Robots, <número de hilos robots>, <cantidad de movimientos>
Productores, <número de hilos productores>
```

Los valores se almacenan en el siguiente orden:

```text
[cajas objetivo, cajas obstáculo, robots, batería inicial, productores]
```

La clase `Reader` valida que los números sean no negativos y que la cantidad inicial de entidades no supere la capacidad configurada. Si el archivo no existe, tiene un formato inválido o contiene valores no numéricos, devuelve un arreglo vacío y `Proyecto3` termina sin iniciar la simulación.

El tablero se crea con tamaño `6`, por lo que sus coordenadas válidas son `0..5`. Los robots pueden ocupar cualquier coordenada libre, incluyendo los bordes. Las cajas, en cambio, se producen y se empujan dentro del área interior para evitar que queden irrecuperables en la fila o columna `0`.

Los robots se colocan e inician antes que los productores. Esto garantiza que, desde el comienzo de la producción, exista un hilo capaz de mover cajas y liberar espacio interior.

`Proyecto3` almacena productores y robots en una lista común de `Thread` y ejecuta `join()` sobre todos ellos. De esta manera el reporte final solo se imprime cuando todos los hilos han terminado.

### 2. Modelado del recurso crítico (`Board`)

El recurso crítico principal identificable en este problema se trata del tablero, en el que múltiples hilos intentan competir por el acceso a las celdas o información contenida en el tablero con el propósito de realizar ambas lecturas y escrituras sobre este. Las condiciones de carrera se hacen obvias y preocupantes si este recurso no es manejado de la manera correcta, provocando estados incoherentes o hasta interbloqueos que impiden el correcto funcionamiento de los hilos involucrados.

Para implementar cualquier solución que proteja un recurso crítico, la estructura del código debe garantizar obligatoriamente:

#### Exclusión Mutua

Si un hilo está ejecutando la sección crítica ningún otro puede hacerlo. Esto se logra con los métodos `synchronized` que utilizan como monitor la instancia actual del objeto, como todos los hilos comparten una misma instancia de un objeto `Board` sólo un hilo puede ejecutar cualquier método `synchronized` a la vez. Si otro hilo intenta llamar a un método sincronizado queda bloqueado hasta que el primero termine.

#### Progreso

Si ningún hilo está en la sección crítica, la decisión de cuál entra no puede postergarse indefinidamente. Esto se cumple ya que ni los productores ni robots pueden esperar indefinidamente por acceder al tablero, los productores esperan por una celda válida para colocar las cajas, en caso de que no haya una disponible el hilo termina. Por su parte los robots solo esperan si todavía pueden producirse nuevas cajas objetivo, si no hay cajas seleccionables o una fuente real de progreso el robot termina.

#### Espera Limitada

Los productores reciben turnos FIFO al intentar insertar cajas y los robots reciben turnos FIFO en `Board.moveRobot`, que es la operación crítica que modifica el tablero. Cada turno se libera tanto después de un movimiento como cuando la operación termina sin movimiento o es interrumpida. Por ello, un hilo que permanece en la cola no puede ser adelantado indefinidamente por hilos que llegaron después. Java no ofrece por sí mismo una garantía temporal absoluta del planificador, pero estas colas FIFO eliminan la inanición causada por la competencia arbitraria por `synchronized`.

El estado físico se representa mediante una matriz de `ObjectType` donde cada celda puede contener uno de los siguientes valores:

```java
EMPTY, TARGET_BOX, OBSTACLE_BOX, ROBOT
```

`Board` mantiene también contadores sincronizados:

- `targetBoxes[0]`: cajas objetivo todavía no producidas.
- `targetBoxes[1]`: cajas objetivo activas en el tablero.
- `targetBoxes[2]`: cajas objetivo extraídas.
- `obstacleBoxes`: cajas obstáculo que todavía faltan por producir.
- `emptyCells`: cantidad total de celdas vacías.
- `activeRobots`: robots actualmente colocados en el tablero.

La clase expone dos condiciones de finalización diferentes:

```java
isTargetProductionFinished()
isProductionFinished()
```

La primera indica que no quedan cajas objetivo por producir ni activas. La segunda indica que ya no quedan cajas de ningún tipo por producir.

### 3. Coordenadas y direcciones (`Coord` y `Direction`)

`Coord` representa una posición `(fila, columna)` y proporciona desplazamientos unitarios o de varias celdas mediante `move`:

```java
coord.move(Direction.DOWN)
coord.move(Direction.RIGHT, 2)
```

También implementa `equals`, `hashCode` y distancia Manhattan, lo que permite usar coordenadas en estructuras de datos de búsqueda:

$$
d((x_1,y_1),(x_2,y_2)) = |x_1-x_2| + |y_1-y_2|
$$

`Direction` define las cuatro direcciones ortogonales:

```java
UP, DOWN, LEFT, RIGHT
```

y proporciona `opositeDirection()`, utilizado para encontrar la posición desde la cual un robot debe empujar una caja.

### 4. Productores (`ConveyorBelt`)

Cada `ConveyorBelt` es un hilo que intenta insertar cajas hasta que la producción termina o se vuelve imposible. Las coordenadas aleatorias se generan en el intervalo `1..size-2`, evitando las filas y columnas de borde.

La inserción real no se delega completamente al azar. `Board.fillCell()` busca una celda interior libre y realiza la operación bajo el monitor del tablero. Esto evita que varios productores seleccionen simultáneamente la misma celda.

Los hilos `ConveyorBelt` priorizan la producción de cajas objetivo sobre cajas bloqueo para evitar saturar el tablero de cajas bloqueo y terminar sin espacio para las cajas objetivo y que los hilos `Robot` deban retirarse sin la oportunidad de extraer ninguna caja objetivo. 

Si el área interior está llena, el productor ejecuta `wait()` mientras exista algún robot activo que pueda liberar una celda. Cuando un robot mueve o retira un objeto, `Board` ejecuta `notifyAll()` y los productores vuelven a comprobar la condición.

La condición `isProductionImpossible()` evita esperas infinitas cuando:


- No quedan celdas interiores libres
- No quedan robots activos capaces de liberar espacio
- Todavía quedan cajas por producir


En ese caso los productores terminan de forma controlada.

### 5. Robot

El ciclo principal de un robot es:

```text
1. Tomar un snapshot.
2. Esperar hasta que exista un objetivo válido o terminar.
3. Elegir una dirección de empuje.
4. Encontrar la posición de soporte mediante BFS.
5. Ejecutar un único movimiento atómico en Board.
6. Actualizar posición y batería si el movimiento fue exitoso.
7. Repetir hasta extraer objetivos, agotar batería o quedar sin movimientos válidos.
```

Todas las modificaciones del tablero se realizan mediante métodos `synchronized`. `moveRobot` contempla dos casos:

1. **Movimiento normal**: La celda inmediatamente siguiente está vacía y el robot se desplaza a ella. Devolviendo como resultado `MOVE`.
2. **Empuje**: La celda siguiente contiene una caja y la celda posterior es un destino válido para esa caja. Devolviendo como resultado `PUSH_OBJ` o `PUSH_OBS` dependiendo de la caja empujada.

Los robots no pueden ser empujados. Tampoco se permite empujar una caja fuera del tablero, contra otra caja o hacia las filas y columnas reservadas como irrecuperables (`(0,X)` `(X,0)`).

Cuando una caja objetivo llega a `(5,5)` la caja se retira del tablero, se libera la celda y se considera extraída devolviendo `EXTRACTED_TARGET`.

Ante cualquier resultado de un movimiento exceptuando `BLOCKED` dicho movimiento se considera válido y decrementa en 1 la batería (Movimientos restantes) del robot que lo ejecutó.

### Snapshots y colisiones (`Snapshot`)

`Board.snapshot()` crea una copia del tablero y recopila las posiciones de robots, objetivos y obstáculos. Los robots utilizan esta copia para planificar sin mantener ocupado el monitor durante toda la búsqueda.

`Snapshot` diferencia dos validaciones:

- `isBlocked`: valida el movimiento de un robot y permite usar filas o columnas `0`.
- `isBoxBlocked`: valida el destino de una caja y evita colocarla en una fila o columna irrecuperable.

Una caja solo se considera acorralada cuando no existe ninguna dirección que cumpla simultáneamente:

- Destino de la caja sea libre y válido
- Posición de soporte esté dentro del tablero y libre

Esto evita confundir una caja situada junto a un borde con una caja realmente bloqueada.

### Estrategia de los robots (`Robot`)

La implementación actual combina una estrategia heurística con una búsqueda BFS local.

#### Selección de objetivo

`getNextTarget` recorre las cajas objetivo del snapshot, descarta las que están acorraladas y elige la más cercana al robot mediante distancia Manhattan.

#### Dirección de empuje

`directionToPush` implementa un orden de prioridad:

1. `RIGHT`, mientras la caja no esté en la última columna.
2. `DOWN`, mientras la caja no esté en la última fila.
3. Direcciones alternativas válidas.

Una dirección solo se acepta si el destino inmediato de la caja está libre y la posición de soporte es válida. Es decir, la caja no se encuentra acorralada.

#### Ruta hacia la posición de soporte

Cuando el robot no está detrás de la caja, `chooseMoveToward` ejecuta una BFS sobre las celdas libres. La búsqueda conserva el primer movimiento de cada ruta y devuelve únicamente el primer paso del camino más corto hacia la posición de soporte.

La BFS permite rodear cajas y obstáculos, comportamiento que no sería posible con una aproximación que solo intentara reducir directamente la diferencia de filas o columnas.

### 10. Registro de eventos (`Logger`)

`Logger` serializa los mensajes mediante métodos sincronizados y asigna un tick a cada evento. Registra, entre otros:

- Inserción de objetivos
- Inserción de obstáculos
- Movimientos de robots (Incluyendo empujes y extracciones)
- Agotamiento de la batería de un robot
- Saturación del tablero

El registro permite reconstruir el orden observable de las acciones concurrentes, aunque el orden de ejecución de los hilos pueda variar entre ejecuciones.

## Guía de ejecución

Desde la raíz del proyecto, compilar todas las clases Java:

```powershell
javac Java\Proyecto3.java Java\criticalResources\*.java Java\dataStructures\*.java Java\threads\*.java
```

Ejecutar el programa proporcionando el archivo de configuración:

```powershell
java -cp Java Proyecto3 test\prueba.txt
```

El archivo debe contener las etiquetas y valores separados por comas, como en el ejemplo:

```text
Cajas_Objetivo_Iniciales, 3
Cajas_Bloqueo_Iniciales, 5
Robots, 1, 50
Productores, 2
```

Al finalizar se muestran las cajas objetivo extraídas, las saturaciones registradas, los robots retirados y el estado final del tablero.

## Consideraciones de ejecución

La estrategia de navegación es heurística y no garantiza encontrar una solución global en todos los escenarios. La BFS solo calcula el camino local del robot hasta la posición de soporte de la caja seleccionada. La concurrencia puede invalidar un snapshot antes de que se ejecute el siguiente movimiento; por eso `Board.moveRobot` vuelve a validar la operación bajo el monitor.

Una configuración con muchas cajas, pocos robots o poca batería puede dejar objetivos sin extraer. Si no quedan robots activos para liberar espacio interior, la producción termina como imposible en lugar de mantener productores esperando indefinidamente.