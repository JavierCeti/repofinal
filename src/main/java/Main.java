import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        String ruta = args.length > 0 ? args[0] : "proyecto.txt";
        Grafo g = Grafo.cargar(ruta);
        System.out.println("Tareas cargadas: " + g.duracion.size());

        // RF2: si hay ciclo, informar y abortar
        List<String> ciclo = g.buscarCiclo();
        if (ciclo != null) {
            System.out.println("ERROR: el plan tiene un ciclo: " + String.join(" -> ", ciclo));
            return;
        }
        System.out.println("El plan es factible (no hay ciclos).");

        // RF3: dos ordenes topologicos
        List<String> kahn = g.ordenKahn();
        List<String> dfs = g.ordenDFS();
        System.out.println("\nOrden Kahn (BFS): " + kahn);
        System.out.println("Orden DFS       : " + dfs);
        System.out.println("¿Son iguales? " + kahn.equals(dfs) + " (ambos pueden ser validos)");

        // RF4 y RF5
        Grafo.Resultado r = g.calcularCamino(kahn);
        System.out.println("\nTarea  Dur  ES  EF  LS  LF  Holgura");
        for (String t : kahn) {
            System.out.printf("%-6s %3d %3d %3d %3d %3d %5d%s%n", t, g.duracion.get(t),
                    r.es.get(t), r.ef.get(t), r.ls.get(t), r.lf.get(t), r.holgura.get(t),
                    r.criticas.contains(t) ? "  <- critica" : "");
        }

        // PriorityQueue: tareas ordenadas de menor a mayor holgura
        PriorityQueue<String> porHolgura = new PriorityQueue<>(
                Comparator.comparingInt((String t) -> r.holgura.get(t)).thenComparing(t -> t));
        porHolgura.addAll(kahn);
        System.out.print("\nRanking por holgura: ");
        while (!porHolgura.isEmpty()) {
            String t = porHolgura.poll();
            System.out.print(t + "(" + r.holgura.get(t) + ") ");
        }

        System.out.println("\n\nDuracion minima total: " + r.total + " horas");
        System.out.println("Tareas criticas: " + r.criticas);

        // RF6
        g.exportarDot("proyecto.dot", r);
        System.out.println("Archivo proyecto.dot generado.");
    }
}
