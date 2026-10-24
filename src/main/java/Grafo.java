import java.io.*;
import java.util.*;

/**
 * Grafo dirigido con pesos en los nodos (duracion de cada tarea).
 */
public class Grafo {

    // LinkedHashMap = HashMap que recuerda el orden de insercion (salidas siempre iguales)
    Map<String, Integer> duracion = new LinkedHashMap<>();
    Map<String, List<String>> sucesores = new LinkedHashMap<>();
    Map<String, List<String>> predecesores = new LinkedHashMap<>();

    public void agregarTarea(String nombre, int horas) {
        duracion.put(nombre, horas);
        sucesores.putIfAbsent(nombre, new ArrayList<>());
        predecesores.putIfAbsent(nombre, new ArrayList<>());
    }

    public void agregarDependencia(String antes, String despues) {
        if (!duracion.containsKey(antes) || !duracion.containsKey(despues)) {
            throw new IllegalArgumentException("Dependencia con tarea inexistente: " + antes + "->" + despues);
        }
        sucesores.get(antes).add(despues);
        predecesores.get(despues).add(antes);
    }

    // ---------- RF1: cargar desde archivo ----------
    public static Grafo cargar(String ruta) throws IOException {
        Grafo g = new Grafo();
        List<String> dependencias = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                if (linea.contains("->")) {
                    dependencias.add(linea);          // se procesan al final
                } else {
                    String[] p = linea.split(",");
                    g.agregarTarea(p[0].trim(), Integer.parseInt(p[1].trim()));
                }
            }
        }
        for (String d : dependencias) {
            String[] p = d.split("->");
            g.agregarDependencia(p[0].trim(), p[1].trim());
        }
        return g;
    }

    // ---------- RF2: detectar ciclos con DFS (colores) ----------
    // Devuelve la lista de tareas del ciclo, o null si no hay ciclo.
    public List<String> buscarCiclo() {
        Map<String, Integer> estado = new HashMap<>(); // 0 = sin visitar, 1 = en proceso, 2 = terminado
        for (String t : duracion.keySet()) estado.put(t, 0);
        List<String> camino = new ArrayList<>();
        for (String t : duracion.keySet()) {
            if (estado.get(t) == 0) {
                List<String> ciclo = dfsCiclo(t, estado, camino);
                if (ciclo != null) return ciclo;
            }
        }
        return null;
    }

    private List<String> dfsCiclo(String t, Map<String, Integer> estado, List<String> camino) {
        estado.put(t, 1);
        camino.add(t);
        for (String s : sucesores.get(t)) {
            if (estado.get(s) == 1) {                 // volvimos a un nodo "en proceso": ciclo
                List<String> ciclo = new ArrayList<>(camino.subList(camino.indexOf(s), camino.size()));
                ciclo.add(s);
                return ciclo;
            }
            if (estado.get(s) == 0) {
                List<String> ciclo = dfsCiclo(s, estado, camino);
                if (ciclo != null) return ciclo;
            }
        }
        camino.remove(camino.size() - 1);
        estado.put(t, 2);
        return null;
    }

    // ---------- RF3a: orden topologico con Kahn (BFS) ----------
    public List<String> ordenKahn() {
        Map<String, Integer> gradoEntrada = new HashMap<>();
        for (String t : duracion.keySet()) gradoEntrada.put(t, predecesores.get(t).size());

        Deque<String> cola = new ArrayDeque<>();
        for (String t : duracion.keySet()) {
            if (gradoEntrada.get(t) == 0) cola.add(t);
        }

        List<String> orden = new ArrayList<>();
        while (!cola.isEmpty()) {
            String t = cola.poll();
            orden.add(t);
            for (String s : sucesores.get(t)) {
                gradoEntrada.put(s, gradoEntrada.get(s) - 1);
                if (gradoEntrada.get(s) == 0) cola.add(s);
            }
        }
        return orden; // si hay ciclo, el orden queda con menos tareas que el total
    }

    // ---------- RF3b: orden topologico con DFS ----------
    public List<String> ordenDFS() {
        Set<String> visitados = new HashSet<>();
        Deque<String> pila = new ArrayDeque<>();
        for (String t : duracion.keySet()) {
            if (!visitados.contains(t)) dfsOrden(t, visitados, pila);
        }
        return new ArrayList<>(pila); // la pila ya tiene el orden correcto (push al frente)
    }

    private void dfsOrden(String t, Set<String> visitados, Deque<String> pila) {
        visitados.add(t);
        for (String s : sucesores.get(t)) {
            if (!visitados.contains(s)) dfsOrden(s, visitados, pila);
        }
        pila.push(t); // se agrega al terminar todos sus sucesores
    }

    // ---------- RF4: camino critico (forward / backward) ----------
    public Resultado calcularCamino(List<String> orden) {
        Map<String, Integer> es = new HashMap<>(), ef = new HashMap<>();
        Map<String, Integer> ls = new HashMap<>(), lf = new HashMap<>();

        // Pasada 1 (forward): ES = max(EF de predecesoras), EF = ES + duracion
        int total = 0;
        for (String t : orden) {
            int inicio = 0;
            for (String p : predecesores.get(t)) inicio = Math.max(inicio, ef.get(p));
            es.put(t, inicio);
            ef.put(t, inicio + duracion.get(t));
            total = Math.max(total, ef.get(t));
        }

        // Pasada 2 (backward): LF = min(LS de sucesoras), LS = LF - duracion
        for (int i = orden.size() - 1; i >= 0; i--) {
            String t = orden.get(i);
            int fin = total;
            for (String s : sucesores.get(t)) fin = Math.min(fin, ls.get(s));
            lf.put(t, fin);
            ls.put(t, fin - duracion.get(t));
        }

        Resultado r = new Resultado();
        r.total = total;
        r.es = es; r.ef = ef; r.ls = ls; r.lf = lf;
        for (String t : orden) {
            int holgura = ls.get(t) - es.get(t);
            r.holgura.put(t, holgura);
            if (holgura == 0) r.criticas.add(t);
        }
        return r;
    }

    // ---------- RF6: exportar DOT ----------
    public void exportarDot(String ruta, Resultado r) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ruta))) {
            pw.println("digraph Proyecto {");
            pw.println("    rankdir=LR;");
            pw.println("    node [shape=box];");
            for (String t : duracion.keySet()) {
                String color = r.criticas.contains(t) ? ", style=filled, fillcolor=lightcoral" : "";
                pw.println("    " + t + " [label=\"" + t + "\\n" + duracion.get(t) + "h\"" + color + "];");
            }
            for (String t : duracion.keySet()) {
                for (String s : sucesores.get(t)) {
                    boolean critica = r.criticas.contains(t) && r.criticas.contains(s)
                            && r.ef.get(t).equals(r.es.get(s));
                    pw.println("    " + t + " -> " + s + (critica ? " [color=red, penwidth=2]" : "") + ";");
                }
            }
            pw.println("}");
        }
    }

    /** Datos que devuelve el calculo del camino critico. */
    public static class Resultado {
        int total;
        Map<String, Integer> es, ef, ls, lf;
        Map<String, Integer> holgura = new LinkedHashMap<>();
        List<String> criticas = new ArrayList<>();
    }
}
