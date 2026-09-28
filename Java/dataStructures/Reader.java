package dataStructures;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Reader {
    String fileName;
    Path filePath;

    public Reader(String fileName, Path filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    /**
     * Valida los argumentos leídos del archivo de entrada.
     * @param args Arreglo con los argumentos leídos del archivo.
     * @return true si los argumentos son válidos, false en caso contrario.
     */
    public boolean validateArguments(int[] args){
        int sum = 0;
        for (int i = 0; i < args.length; i++){
            if(args[i] < 0){ // Verificar que no haya argumentos negativos
                System.err.println("Error: Existen argumentos negativos.");
                return false;
            }
            if (i < 3) { // Solo sumar los primeros tres argumentos (cajas objetivo, cajas obstáculo y robots)
                sum += args[i];
            }
        }
        if (sum > 36) { // Verificar que el numero total de las entidades no sea mayor que las celdas totales (6x6 = 36)
            System.err.println("Error: El numero total de las entidades es mayor que las celdas totales (36).");
            return false;
        }
        return true;
    }

    /**
     * Método que obtiene los argumentos del archivo de entrada y los almacena en un arreglo.
     * @return Arreglo con los argumentos leídos del archivo con el siguiente orden: [Numero de cajas objetivo, Numero de cajas obstáculo, Numero de robots, Batería inicial de los robots, Numero de productores]. Arreglo vacío si los argumentos no son válidos.
     */
    public int[] obtainArguments(){
        List<Integer> values = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 2) {
                    throw new NumberFormatException("Formato inválido: " + line);
                }

                for (int i = 1; i < parts.length; i++) {
                    values.add(Integer.parseInt(parts[i].trim()));
                }
            }

            if (values.size() != 5) {
                System.err.println("Error: Se esperaban 5 valores numéricos.");
                return new int[0];
            }

            int[] args = new int[values.size()];
            for (int i = 0; i < values.size(); i++) {
                args[i] = values.get(i);
            }

            if (!validateArguments(args)) {
                return new int[0];
            }
            return args;
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
            return new int[0];
        }
    }

}
