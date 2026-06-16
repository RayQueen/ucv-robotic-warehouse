:- dynamic robotPosition/2.
:- dynamic targetBoxPosition/2.
:- dynamic blockingBoxPosition/2.

% 1) Direcciones y Cálculos de Coordenadas

% dirección(Nombre, DeltaFila, DeltaColumna)
direction(u, -1, 0).
direction(d,  1, 0).
direction(l,  0,-1).
direction(r,  0, 1).

addCoordinates((Row, Column), (DeltaRow, DeltaColumn), (NewRow, NewColumn)) :-
	NewRow is Row + DeltaRow,
	NewColumn is Column + DeltaColumn.

% 2) Inicialización del Tablero

% Verifica que las coordenadas estén en el rango [0,5]
isWithinBounds((Row, Column)) :-
	Row >= 0, Row =< 5,
	Column >= 0, Column =< 5.

% Verifica que todas las coordenadas en una lista estén dentro de los límites del tablero
areAllWithinBounds([]).
areAllWithinBounds([Head | Tail]) :-
	isWithinBounds(Head),      % Verifica el primer elemento
	areAllWithinBounds(Tail).  % Llamada recursiva para el resto de la lista

% Verifica que todas las coordenadas en una lista sean únicas
areAllUnique([]).
areAllUnique([Head | Tail]) :-
	\+ member(Head, Tail),  % \+ es el operador de negación (Gracia a deux que esto existe)
	areAllUnique(Tail).

% Auxiliar para agregar hechos de cajas ladillas a la base de datos dinámica
assertBlockingBoxes([]).
assertBlockingBoxes([(Row, Column) | Tail]) :-
	assertz(blockingBoxPosition(Row, Column)), % Añade el hecho a la base de datos dinámica (machine learning moment)
	assertBlockingBoxes(Tail).

% Predicado principal para inicializar el estado del tablero (no shit sherlock)
initializeWarehouse(RobotCoord, TargetCoord, BlockingBoxes) :-
	% 1. Validar que todas las entidades estén dentro del tablero
	areAllWithinBounds([RobotCoord, TargetCoord]),
	areAllWithinBounds(BlockingBoxes),
	areAllUnique([RobotCoord, TargetCoord | BlockingBoxes]),

	% 2. Si todo es válido, limpiar la base de conocimientos anterior
	retractall(robotPosition(_, _)),        % Borra posiciones previas del robot
	retractall(targetBoxPosition(_, _)),    % Borra posición previa de la caja objetivo
	retractall(blockingBoxPosition(_, _)),  % Borra todas las cajas ladillas

	% 3. Explicar que carajo hace esto
	RobotCoord = (RobotRow, RobotColumn),                 % Descompone la tupla del robot en variables individuales
	assertz(robotPosition(RobotRow, RobotColumn)),        % Registra la nueva posición del robot como un hecho
	TargetCoord = (TargetRow, TargetColumn),              % Descompone la tupla de la caja objetivo
	assertz(targetBoxPosition(TargetRow, TargetColumn)),  % Registra la nueva posición de la caja objetivo como un hecho
	assertBlockingBoxes(BlockingBoxes).                   % Almacena cada una de las cajas ladillas individualmente

% 3) Validación de Movimiento y Transición de Estados

% Caso 1: El robot se mueve a una celda vacía
applyMovement(state(RobotPos, TargetPos, BlockingList), Move, state(NewRobotPos, TargetPos, BlockingList)) :-
	direction(Move, DeltaRow, DeltaColumn),                          % Obtiene el desplazamiento asociado al movimiento
	addCoordinates(RobotPos, (DeltaRow, DeltaColumn), NewRobotPos),  % Calcula la nueva posición del robot
	isWithinBounds(NewRobotPos),                                     % Verifica que la nueva posición esté dentro del tablero
	NewRobotPos \= TargetPos,                                        % Se asegura de que no haya una caja objetivo en esa celda
	\+ member(NewRobotPos, BlockingList).                            % Verifica que no haya una caja ladilla en esa posición

% Caso 2: El robot empuja la Caja Objetivo
applyMovement(state(RobotPos, TargetPos, BlockingList), Move, state(TargetPos, NewTargetPos, BlockingList)) :-
	direction(Move, DeltaRow, DeltaColumn),                            % Obtiene el desplazamiento del movimiento
	addCoordinates(RobotPos, (DeltaRow, DeltaColumn), TargetPos),      % El robot debe estar en la posición actual de la caja
	addCoordinates(TargetPos, (DeltaRow, DeltaColumn), NewTargetPos),  % Calcula la nueva posición de la caja tras el empuje
	isWithinBounds(NewTargetPos),                                      % Verifica que la caja no salga del tablero
	\+ member(NewTargetPos, BlockingList).                             % Se asegura que no se empuje la caja hacia otra caja ladilla (como jodió esto)

% Caso 3: El robot empuja una Caja Ladilla
applyMovement(state(RobotPos, TargetPos, BlockingList), Move, state(NewRobotPos, TargetPos, NewBlockingList)) :-
	direction(Move, DeltaRow, DeltaColumn),                                  % Obtiene el desplazamiento del movimiento
	addCoordinates(RobotPos, (DeltaRow, DeltaColumn), NewRobotPos),          % Calcula a dónde se mueve el robot
	isWithinBounds(NewRobotPos),                                             % Verifica límites para el robot
	member(NewRobotPos, BlockingList),                                       % Confirma que efectivamente hay una caja ladilla ahí
	addCoordinates(NewRobotPos, (DeltaRow, DeltaColumn), PushedBoxPos),      % Calcula la nueva posición de la caja ladilla
	isWithinBounds(PushedBoxPos),                                            % Verifica que la caja ladilla no salga del tablero
	PushedBoxPos \= TargetPos,                                               % No se puede empujar hacia donde está la caja objetivo
	\+ member(PushedBoxPos, BlockingList),                                   % No se puede empujar hacia donde hay otra caja ladilla
	replaceInList(NewRobotPos, PushedBoxPos, BlockingList, NewBlockingList). % Actualiza la lista de posiciones de cajas

% Vaina auxiliar para actualizar la posición de una caja en la lista
replaceInList(Old, New, [Old | Tail], [New | Tail]).                % Si el primer elemento es el buscado, lo reemplaza
replaceInList(Old, New, [Head | Tail], [Head | RefactoredTail]) :-  % Si no, mantiene la cabeza
	Head \= Old,                                                    % Verifica que no sea el elemento a cambiar
	replaceInList(Old, New, Tail, RefactoredTail).                  % Sigue buscando en el resto de la lista

% 4) Lógica de Búsqueda (BFS)

% Normaliza el estado para el control de visitados
standardizeState(state(RobotPos, TargetPos, BlockingList), state(RobotPos, TargetPos, SortedBlockingList)) :-
	sort(BlockingList, SortedBlockingList).  % Ordena la lista de cajas para que el estado sea único e independiente del orden

% Punto de entrada para encontrar el camino más corto
solveWarehouse(StartState, Solution) :-
	standardizeState(StartState, CanonStart),                                % Convierte el estado inicial a su forma canónica (uou discretas ii)
	breadthFirstSearch([[CanonStart, []]], [CanonStart], ReversedSolution),  % Inicia BFS con cola y lista de visitados
	reverse(ReversedSolution, Solution).                                     % Al final invierte el camino para que vaya de Inicio -> Fin

% Si la cabeza de la cola llegó a la meta, retorna el camino
breadthFirstSearch([[state(_, (5, 5), _), Path] | _], _, Path).

% Paso recursivo de la búsqueda en anchura (magia negra)
breadthFirstSearch([[CurrentState, Path] | RemainingQueue], Visited, FinalSolution) :-
	findall(                                                         % Encuentra todos los estados sucesores posibles
		[NextStateCanon, [Move | Path]],                             % Estructura del nuevo nodo: [NuevoEstado, NuevoCamino]
		(
			direction(Move, _, _),                                   % Intenta con cada dirección posible
			applyMovement(CurrentState, Move, NextState),            % Aplica el movimiento según las reglas anteriores
			standardizeState(NextState, NextStateCanon),             % Normaliza el nuevo estado
			\+ member(NextStateCanon, Visited)                       % Solo continúa si el estado no ha sido visitado antes
		),
		NewNodes                                                     % Almacena los nuevos nodos generados
	),
	getStatesFromNodes(NewNodes, NewStatesFound),                    % Extrae solo los estados de los nuevos nodos
	append(Visited, NewStatesFound, UpdatedVisited),                 % Añade los nuevos estados a la lista de visitados
	append(RemainingQueue, NewNodes, UpdatedQueue),                  % Añade los nuevos nodos al final de la cola (FIFO para BFS)
	breadthFirstSearch(UpdatedQueue, UpdatedVisited, FinalSolution). % Llamada recursiva con la cola actualizada

% Predicado auxiliar para extraer estados de los nodos [Estado, Camino]
getStatesFromNodes([], []).                                      % Caso base: lista vacía
getStatesFromNodes([[State, _] | Tail], [State | StateTail]) :-  % Extrae el estado e ignora el camino
	getStatesFromNodes(Tail, StateTail).                         % Procesa recursivamente el resto de la lista
