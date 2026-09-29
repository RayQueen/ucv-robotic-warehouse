<h1 align="center">Almacén Robótico</h1>

El objetivo de este proyecto es controlar múltiples robots montacargas dentro de un almacén representado por una matriz bidimensional de $6 \times 6$, con coordenadas desde `(0,0)` hasta `(5,5)`. Los robots se ejecutan como hilos concurrentes y deben trasladar `Cajas Objetivo` hasta la zona de extracción `(5,5)`, mientras comparten el tablero con productores que generan dichas `Cajas Objetivo` y `Cajas de Bloqueo`.

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

`Proyecto3` recibe como argumento la ruta de un archivo de texto con los parámetros del problema. `Reader.obtainArguments()` ignora las etiquetas y almacena únicamente los valores numéricos en el orden [`cajas objetivo`, `cajas obstáculo`, `robots`, `batería inicial`, `productores`]

`Reader.validateArguments()` valida que los argumentos sean no negativos y que la cantidad inicial de entidades (Considerando únicamente como entidades a las cajas y robots) no supere la capacidad configurada. Si el archivo no existe, tiene un formato inválido o contiene valores no numéricos, devuelve un arreglo vacío y `Proyecto3` termina sin iniciar la simulación.

El tablero se crea con tamaño `6`, por lo que sus coordenadas válidas son `0..5`. Los robots pueden ocupar cualquier coordenada libre, incluyendo los bordes. Las cajas, en cambio, se producen y se empujan dentro del área interior para evitar que queden irrecuperables en la fila o columna `0`.

Los robots se colocan e inician antes que los productores. Esto garantiza que, desde el comienzo de la producción, exista un hilo capaz de mover cajas y liberar espacio interior.

`Proyecto3` almacena productores y robots en una lista común de `Thread` y ejecuta `join()` sobre todos ellos. De esta manera el reporte final solo se imprime cuando todos los hilos han terminado sin hacer distinción sobre si terminan primero productores o robots.

### 2. Modelado del recurso crítico (`Board`)

El recurso crítico identificable en este problema se trata del tablero, en este es donde múltiples hilos intentan competir por el acceso a las celdas o información contenida en el tablero con el propósito de realizar ambas lecturas y escrituras. Las condiciones de carrera se hacen obvias y preocupantes si este recurso no es manejado correctamente, provocando estados incoherentes o hasta interbloqueos que impiden el correcto funcionamiento de los hilos involucrados.

Para implementar cualquier solución que proteja un recurso crítico, la estructura del código debe garantizar obligatoriamente:

#### Exclusión Mutua

Si un hilo está ejecutando la sección crítica ningún otro puede hacerlo. Esto se logra con los métodos `synchronized` que utilizan como monitor la instancia actual del objeto, como todos los hilos comparten una misma instancia de un objeto `Board` sólo un hilo puede ejecutar cualquier método `synchronized` a la vez. Si otro hilo intenta llamar a un método sincronizado queda bloqueado hasta que el primero termine.

#### Progreso

Si ningún hilo está en la sección crítica, la decisión de cuál entra no puede postergarse indefinidamente. Esto se cumple ya que ni los productores ni robots pueden esperar indefinidamente por acceder al tablero, los productores esperan por una celda válida para colocar las cajas, en caso de que no haya una disponible el hilo termina. Por su parte los robots solo esperan si todavía pueden producirse nuevas cajas objetivo, si no hay cajas seleccionables o una fuente real de progreso el robot termina.

#### Espera Limitada

Los productores reciben turnos FIFO al intentar insertar cajas y los robots reciben turnos FIFO en `Board.moveRobot`, que es la operación crítica que modifica el tablero. Cada turno se libera tanto después de un movimiento como cuando la operación termina sin movimiento o es interrumpida. Por ello, un hilo que permanece en la cola no puede ser adelantado indefinidamente por hilos que llegaron después. Java no ofrece por sí mismo una garantía temporal absoluta del planificador, pero estas colas FIFO eliminan la inanición causada por la competencia arbitraria por `synchronized`.

El estado físico se representa mediante una matriz de `ObjectType` donde cada celda puede contener uno de los siguientes valores: `EMPTY`, `TARGET_BOX`, `OBSTACLE_BOX`, `ROBOT`.

`Board` no sólo mantiene una representación física sino también contadores sincronizados:

- `targetBoxes[0]`: cajas objetivo todavía no producidas.
- `targetBoxes[1]`: cajas objetivo activas en el tablero.
- `targetBoxes[2]`: cajas objetivo extraídas.
- `obstacleBoxes`: cajas obstáculo que todavía faltan por producir.
- `emptyCells`: cantidad total de celdas vacías.
- `activeRobots`: robots actualmente colocados en el tablero.

Además de campos `long` que representan tickets para el correcto funcionamiento de la planficación FIFO que permite cumplir la condición de espera limitada.

### 3. Coordenadas y direcciones (`Coord` y `Direction`)

- `Coord` representa una posición `(fila, columna)`, implementa `equals`, `hashCode` y distancia Manhattan, lo que permite usar coordenadas en estructuras de datos de búsqueda heurística y proporciona desplazamientos unitarios o de varias celdas mediante `move()` dependiendo de los argumentos proporcionados:

```java
coord.move(Direction.DOWN) // Mover la coordenada actual un paso hacia abajo
coord.move(Direction.RIGHT, 2) // Mover la coordenada actual dos pasos a la derecha
``` 

- `Direction` define las cuatro direcciones ortogonales `UP`, `DOWN`, `LEFT` y `RIGHT` y proporciona `opositeDirection()`, que retorna la dirección opuesta dado una dirección de movimiento, principalmente utilizado para encontrar la posición desde la cual un robot debe empujar una caja.

### 4. Productores (`ConveyorBelt`)

Cada `ConveyorBelt` es un hilo que intenta insertar cajas hasta que la producción termina o se vuelve imposible. Las coordenadas aleatorias se generan en el intervalo `1..size-2`, evitando las filas y columnas de borde.

Los hilos `ConveyorBelt` priorizan la producción de cajas objetivo sobre cajas bloqueo para evitar saturar el tablero de cajas bloqueo, sin espacio para las cajas objetivo y que los hilos `Robot` deban retirarse sin la oportunidad de extraer ninguna caja objetivo.

#### Ciclo de vida de un Productor
1. **Posibilidad de producción**: Realiza una verificación mediante las funciones proporcionadas por el tablero `isProductionFinished()` y `isProductionImpossible` y sólo continua su ejecución si:
	- Existen cajas de cualquier tipo pendientes por producir.
	- Existe espacio en el marco interior del tablero para almacenar las cajas.
	- En el caso de que no existan dichos espacios debe haber al menor un robot activo que pueda liberar espacio en el marco interior.

2. **Selección de coordenadas**: Genera una coordenada aleatoria en la que almacenar la caja a producir. Dicha coordenada nunca será una correspondiente al borde del tablero para evitar posiciones irrecuperables.

3. **Producción de la caja**: Llama al método sincronizado `fillCell()` el cual se encarga de asignarle al hilo un ticket para simular una planificación FIFO. El hilo espera a que sea su turno, verifica nuevamente que exista la posibilidad de producción y una vez verificado si no hay celdas válidas disponibles espera de nuevo. Una vez terminada la espera verifica una vez más la posibilidad de producción y finalmente si la celda seleccionada está vacía se produce una caja (Objetivo en caso de que aún haya por producir, obstáculo en caso contrario) y se almacena en dicha celda actualizando los contadores involucrados y avisando al resto de productores que el turno se ha liberado.

4. **Espera aleatorizada**: El hilo realiza una espera durante un periodo de tiempo aleatorio para evitar saturar el tablero con una producción automática. Esta espera no se hace al comienzo del bucle while para evitar una nueva comprobación de la posibilidad de producción.

### 5. Robot
Cada `Robot` es un hilo que intenta llevar cajas objetivos desde su ubicación en el tablero hasta la coordenada o casilla de extracción, en este caso (5,5). Los robots no pueden ser empujados ni empujar cajas a casillas en las que estas se consideran irrecuperables (fila y columna 0).

#### Ciclo de vida de un Robot
1. **Inicialización de variables locales**: Inicializa una variable de tipo `MoveResult` que será útil a la hora de determinar la causa de finalización del robot, así como una variable de tipo `Direction` utilizada en la lógica de movimiento.

2. **Comprobación de batería**: Verifica que el campo de batería análogo a la cantidad de movimientos restantes sea mayor a 0 antes de intentar realizar un nuevo movimiento.

3. **Verificación de objetivo disponible**: Llama al método local `waitUntilTargetAvailable()` el cual mediante a un bloque sincronizdo con board permite:
	- Tomar una instantánea `Snapshot` del tablero sobre la que el hilo realizará su planificación para evitar acaparar el recurso crítico con motivo de consulta.
	- Verificar la **Posibilidad de Movimiento** mediante al método proporcionado por el tablero `isTargetProductionFinished()` el cual indica si ya no existen cajas objetivo en el tablero y al mismo tiempo no quedan más por producir.
	- Mediante el método local `getNextTarget()` evalua la lista de cajas objetivos obtenida en la instantánea para saber si estas son posibles de mover en el estado actual y en caso de que lo sean, encontrar aquella con la menor distancia Manhattan con respecto a la posición actual del Robot.
	- En caso de no encontrar una caja objetivo válida en el estado actual se verifica si se producirán más, en caso de que aún queden cajas objetivo por producir el hilo espera antes de repetir el proceso. En caso contrario retorna `null` y termina su ejecución.

4. **Decidir una dirección**: Una vez obtenidas las coordenadas correspondientes a la caja objetivo se llama a `directionToPush()` para seleccionar una dirección en la que proseguir. Este método local define una lista de direcciones en función a una prioridad decidida en base a la última dirección ejecutada. En caso de poder ser empujada a la derecha o hacia abajo el método prioriza estas direcciones en función de alcanzar la casilla de extracción.

5. **Camino a la posición de soporte**: En caso de que el Robot no se encuentre en la coordenada apropiada para poder empujar la caja en la dirección elegida (Coordenada de soporte) procede a calcular una nueva dirección para llegar a dicha coordenada usando el método local `chooseMoveToward()` el cual realiza una búsqueda BFS que permite rodear cajas y obstáculos, comportamiento que no sería posible con una aproximación que solo intentara reducir directamente la diferencia de filas o columnas. La búsqueda conserva el primer movimiento de cada ruta y devuelve únicamente el primer paso del camino más corto hacia la posición de soporte.

6. **Ejecución del movimiento**: Una vez hecha toda la planificación llama al método sincronizado proporcionado por el tablero `moveRobot()` que asigna un ticker al hilo de modo que debe esperar por su turno. En función a la disponibilidad de las celdas contiguas a la coordenada en la dirección seleccionada realiza uno de los siguientes movimientos:
	- **Movimiento normal**: La celda inmediatamente siguiente está vacía y el robot se desplaza a ella. Devolviendo como resultado `MOVE`.
	- **Empuje**: La celda siguiente contiene una caja y la celda posterior es un destino válido para esa caja. Devolviendo como resultado `PUSH_OBJ` o `PUSH_OBS` dependiendo de la caja empujada. Si la caja empujada fue una objetivo y su coordenada destino fue la casilla de extracción se considera como extraída y se elimina del tablero devolviendo `EXTRACTED_TARGET`
	- **Movimiento anulado**: Ninguno de los dos casos anteriores se cumplen, es decir, el movimiento se vió bloqueado y no existe un movimiento válido que pueda realizar el robot en ese instante. Devolviendo como resultado `BLOCKED`.

7. **Actualización en función del resultado**: Una vez obtenido el resultado del movimiento, si este fue exitoso (`MOVE`, `PUSH_OBJ`, `PUSH_OBS`, `EXTRACTED_TARGET`) se actualiza la posición del robot y se decrementa en 1 la batería. En caso contrario (`BLOCKED`) no se realizan actualizaciones y sólo se procede con la siguiente iteración.

8. **Terminar por extracción**: En caso de que el resultado del último movimiento haya sido `EXTRACTED_TARGET` se verifica si aún quedan cajas objetivo en el tablero o por producir, si esto se cumple, el hilo espera un tiempo aleatorio y sigue iterando desde el paso 1. En caso contrario termina su ejecucíon.

9. **Finalización**: En caso de cumplirse alguna de las condiciones para terminar la ejecución del hilo se llama a la función `removeRobot()` que se encarga de eliminar el objeto correspondiente al Robot del tablero y en caso de haber terminado por falta de batería emite una entrada en el log describiendo este hecho.

### Snapshots y colisiones

`Board.snapshot()` crea una copia del tablero y recopila las posiciones de robots, objetivos y obstáculos. Los robots utilizan esta copia para planificar sin mantener ocupado el monitor durante toda la búsqueda.

`Snapshot` diferencia dos validaciones:

- `isBlocked`: valida el movimiento de un robot y permite usar filas o columnas `0`.
- `isBoxBlocked`: valida el destino de una caja y evita colocarla en una fila o columna irrecuperable.

Una caja solo se considera acorralada cuando no existe ninguna dirección que cumpla simultáneamente:

- Destino de la caja sea libre y válido
- Posición de soporte esté dentro del tablero y libre

Esto evita confundir una caja situada junto a un borde con una caja realmente bloqueada.

### 6. Registro de eventos (`Logger`)

`Logger` serializa los mensajes mediante métodos sincronizados y asigna un tick a cada evento. Registra, entre otros:

- Inserción de objetivos
- Inserción de obstáculos
- Movimientos de robots (Incluyendo empujes y extracciones)
- Agotamiento de la batería de un robot
- Saturación del tablero

El registro permite reconstruir el orden observable de las acciones concurrentes, aunque el orden de ejecución de los hilos pueda variar entre ejecuciones.

## Guía de ejecución

Desde la raíz del proyecto, en una terminal que soporte make, utilizar:

#### Para compilar y ejecutar

```make
make run ARGS='<nombre del archivo>.txt'
```

#### Para eliminar todas las clases compiladas, volver a compilar y ejecutar

```make
make ARGS='<nombre del archivo>.txt'
```

El archivo pasado como argumento debe contener las etiquetas y valores separados por comas en el formato:

```text
Cajas_Objetivo_Iniciales, <número de cajas objetivo>
Cajas_Bloqueo_Iniciales, <número de cajas de bloqueo>
Robots, <número de hilos robots>, <cantidad de movimientos>
Productores, <número de hilos productores>
```
Por ejemplo:

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

## Referencias
1. Manhattan Distance - Algorithms for competitive programming. (n.d.). https://cp-algorithms.com/geometry/manhattan-distance.html
2. **Oracle** (2026). Concurrency. Oracle Help Center. https://docs.oracle.com/en/java/javase/26/core/concurrency.html
3. **William Stallings** (2006). Sistemas operativos: Aspectos internos y principios de diseño. Pearson Prentice Hall.
4. **Material de Apoyo Académico**. Escuela de Computación, Facultad de Ciencias, Universidad Central de Venezuela (UCV). Lenguajes de Programación - Programación Concurrente. (Guías de clase sobre sincronización de procesos y monitores).