# Definir el compilador y las banderas
JC = javac
JFLAGS = -g

# Nombre de tu clase principal (sin la extensión .java)
MAIN = Proyecto3

# Buscar todos los archivos .java en el directorio
SOURCES = $(wildcard *.java)
CLASSES = $(SOURCES:.java=.class)

# Argumentos para ejecutar el programa
ARGS ?=

# Regla predeterminada
default: clean run

# Regla para compilar archivos .java a .class
%.class: %.java
	$(JC) $(JFLAGS) $<

# Regla para ejecutar el programa
run: $(CLASSES)
	java $(MAIN) $(ARGS)

# Regla para limpiar los archivos compilados
clean:
	$(RM) *.class
