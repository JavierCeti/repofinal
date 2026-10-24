import java.util.*;

/** Pruebas sencillas sin JUnit: cada una imprime OK o FALLO. */
public class Pruebas {
    static int fallos = 0;

    static void verificar(String nombre, boolean condicion) {
        System.out.println((condicion ? "OK     " : "FALLO  ") + nombre);
        if (!condicion) fallos++;
    }

    // Revisa que en el orden cada predecesora aparezca antes que su sucesora
    static boolean ordenValido(Grafo g, List<String> orden) {
        if (orden.size() != g.duracion.size()) return false;
        for (String t : g.duracion.keySet())
            for (String s : g.sucesores.get(t))
                if (orden.indexOf(t) > orden.indexOf(s)) return false;
        return true;
    }

    public static void main(String[] args) {
        // 1. DAG lineal: A(2) -> B(3) -> C(4)
        Grafo lineal = new Grafo();
        lineal.agregarTarea("A", 2); lineal.agregarTarea("B", 3); lineal.agregarTarea("C", 4);
        lineal.agregarDependencia("A", "B"); lineal.agregarDependencia("B", "C");
        Grafo.Resultado r = lineal.calcularCamino(lineal.ordenKahn());
        verificar("Lineal: sin ciclo", lineal.buscarCiclo() == null);
        verificar("Lineal: orden Kahn = [A, B, C]", lineal.ordenKahn().equals(List.of("A", "B", "C")));
        verificar("Lineal: orden DFS = [A, B, C]", lineal.ordenDFS().equals(List.of("A", "B", "C")));
        verificar("Lineal: total = 9", r.total == 9);
        verificar("Lineal: todas criticas", r.criticas.size() == 3);

        // 2. Diamante: A(2) -> B(3), A -> C(5), B -> D(1), C -> D
        Grafo d = new Grafo();
        d.agregarTarea("A", 2); d.agregarTarea("B", 3); d.agregarTarea("C", 5); d.agregarTarea("D", 1);
        d.agregarDependencia("A", "B"); d.agregarDependencia("A", "C");
        d.agregarDependencia("B", "D"); d.agregarDependencia("C", "D");
        r = d.calcularCamino(d.ordenKahn());
        verificar("Diamante: orden Kahn valido", ordenValido(d, d.ordenKahn()));
        verificar("Diamante: orden DFS valido", ordenValido(d, d.ordenDFS()));
        verificar("Diamante: total = 8 (A,C,D)", r.total == 8);
        verificar("Diamante: B tiene holgura 2", r.holgura.get("B") == 2);
        verificar("Diamante: criticas = A,C,D", r.criticas.equals(List.of("A", "C", "D")));

        // 3. Ciclo: A -> B -> C -> A
        Grafo c = new Grafo();
        c.agregarTarea("A", 1); c.agregarTarea("B", 1); c.agregarTarea("C", 1);
        c.agregarDependencia("A", "B"); c.agregarDependencia("B", "C"); c.agregarDependencia("C", "A");
        List<String> ciclo = c.buscarCiclo();
        verificar("Ciclo: se detecta", ciclo != null);
        verificar("Ciclo: contiene A, B y C", ciclo != null && ciclo.containsAll(List.of("A", "B", "C")));
        verificar("Ciclo: Kahn no ordena todas", c.ordenKahn().size() < 3);

        // 4. Tarea unica
        Grafo u = new Grafo();
        u.agregarTarea("X", 7);
        r = u.calcularCamino(u.ordenKahn());
        verificar("Unica: total = 7", r.total == 7);
        verificar("Unica: es critica con holgura 0", r.criticas.equals(List.of("X")) && r.holgura.get("X") == 0);

        // 5. Dependencias multiples: C depende de A y B; E depende de C y D
        Grafo m = new Grafo();
        m.agregarTarea("A", 4); m.agregarTarea("B", 6); m.agregarTarea("C", 2);
        m.agregarTarea("D", 3); m.agregarTarea("E", 5);
        m.agregarDependencia("A", "C"); m.agregarDependencia("B", "C");
        m.agregarDependencia("C", "E"); m.agregarDependencia("D", "E");
        r = m.calcularCamino(m.ordenKahn());
        verificar("Multiples: orden Kahn valido", ordenValido(m, m.ordenKahn()));
        verificar("Multiples: orden DFS valido", ordenValido(m, m.ordenDFS()));
        verificar("Multiples: C empieza en 6 (espera a B)", r.es.get("C") == 6);
        verificar("Multiples: total = 13 (B,C,E)", r.total == 13);
        verificar("Multiples: holgura A = 2", r.holgura.get("A") == 2);
        verificar("Multiples: holgura D = 5", r.holgura.get("D") == 5);

        System.out.println(fallos == 0 ? "\nTodas las pruebas pasaron." : "\nFallaron " + fallos + " pruebas.");
    }
}
