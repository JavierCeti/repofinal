# Planificador de proyecto (DAG, orden topológico y camino crítico)

## 1. ¿Qué problema resuelve?
Una startup tiene tareas con duración (horas) y dependencias ("B no empieza hasta que termine A").
El programa responde:
1. ¿El plan se puede ejecutar? (no debe haber ciclos)
2. ¿En qué orden hago las tareas?
3. ¿Cuál es el tiempo mínimo del proyecto y qué tareas no pueden retrasarse?

## 2. Archivos
| Archivo | Qué hace |
|---|---|
| `src/main/java/Grafo.java` | El grafo y todos los algoritmos |
| `src/main/java/Main.java` | Lee `proyecto.txt`, ejecuta todo e imprime resultados |
| `src/test/java/Pruebas.java` | 5 casos de prueba (sin JUnit, imprime OK/FALLO) |
| `proyecto.txt` | Ejemplo de entrada (12 tareas) |
| `proyecto.dot` | Salida para Graphviz (se genera al ejecutar) |

**Compilar y ejecutar**
```
javac -d out src/main/java/*.java src/test/java/*.java
java -cp out Main          # usa proyecto.txt (ejecutar desde la raíz del repo)
java -cp out Pruebas
```

## 3. Formato de `proyecto.txt`
```
Requisitos,8            <- tarea,duracion
Diseno,16
Requisitos->Diseno      <- predecesora->sucesora
```
Las líneas vacías y las que empiezan con `#` se ignoran.

## 4. Estructuras de datos usadas (y por qué)
| Estructura | Dónde | Justificación |
|---|---|---|
| `HashMap` (`LinkedHashMap`) | duración, sucesores, predecesores, ES/EF/LS/LF | Buscar una tarea por nombre en O(1). Uso `LinkedHashMap` (es un HashMap) para que la salida salga siempre en el mismo orden. |
| `ArrayDeque` | Cola de Kahn y pila del DFS | Es más rápida que `Queue`/`Stack` clásicas; sirve como cola (FIFO) y como pila (LIFO). |
| `PriorityQueue` | Ranking de tareas por holgura en `Main` | Saca siempre la tarea con menor holgura primero, así las críticas (holgura 0) salen arriba. |
| `HashSet` | Nodos visitados en DFS | Preguntar "¿ya lo visité?" en O(1). |

El grafo se guarda con **listas de adyacencia**: para cada tarea, la lista de sucesoras y la de predecesoras.
Complejidad de todo: **O(V + E)** (V = tareas, E = dependencias).

## 5. Los algoritmos

### RF2 – Detectar ciclos (DFS con 3 colores)
Cada nodo está en estado 0 (sin visitar), 1 (en proceso) o 2 (terminado).
Si desde un nodo "en proceso" llego a otro que también está "en proceso", encontré un ciclo.
Guardo el camino actual para poder decir qué tareas lo forman, por ejemplo `A -> B -> C -> A`.
Si hay ciclo, el programa lo informa y termina.

### RF3 – Orden topológico
Un orden topológico es una lista donde **cada tarea aparece después de todas sus predecesoras**.

**Kahn (BFS)**
1. Calcular el grado de entrada (cuántas predecesoras tiene cada tarea).
2. Meter a la cola las de grado 0.
3. Sacar una, agregarla al orden y restar 1 al grado de sus sucesoras; si alguna llega a 0, entra a la cola.
4. Si al final el orden tiene menos tareas que el total → hay ciclo.

**DFS**
1. Visitar cada tarea; primero se visitan todas sus sucesoras.
2. Al terminar una tarea, se hace `push` a una pila.
3. La pila, leída de arriba a abajo, es el orden topológico.

**Comparación (resultado real con proyecto.txt)**
```
Kahn: Requisitos, Diseno, BaseDatos, Infraestructura, Backend, API, Documentacion, Frontend, Capacitacion, Pruebas, Despliegue, Lanzamiento
DFS : Requisitos, Infraestructura, BaseDatos, Backend, Documentacion, Capacitacion, API, Diseno, Frontend, Pruebas, Despliegue, Lanzamiento
```
- Los dos órdenes son **distintos pero ambos válidos**: un DAG casi siempre tiene varios órdenes topológicos.
- Kahn va "por niveles" (todas las tareas listas a la vez) y detecta ciclos al contar.
- DFS profundiza por una rama antes de pasar a otra y necesita recursión (puede desbordar la pila en grafos enormes).
- Ambos cuestan O(V + E).

### RF4 – Camino crítico (dos pasadas sobre el orden topológico)

**Pasada 1, forward** (recorriendo el orden de inicio a fin):
- `ES` (early start) = máximo de los `EF` de las predecesoras (0 si no tiene).
- `EF` (early finish) = `ES + duración`.
- El **tiempo total** = el mayor `EF`.

**Pasada 2, backward** (recorriendo el orden al revés):
- `LF` (late finish) = mínimo de los `LS` de las sucesoras (el tiempo total si no tiene).
- `LS` (late start) = `LF - duración`.

**Holgura** = `LS - ES`: cuánto se puede retrasar una tarea sin retrasar el proyecto.
Las tareas con **holgura 0 son críticas**, y unidas forman el camino crítico.

Ejemplo (diamante: A(2) → B(3), A → C(5), B → D(1), C → D):
- Camino A-C-D = 2+5+1 = **8 h** (el más largo), B tiene holgura 2.

Se usa programación dinámica porque el valor de cada tarea se calcula con los ya calculados de las anteriores (en orden topológico), sin recalcular nada.

## 6. Resultado del ejemplo (12 tareas)
| Tarea | Dur | ES | EF | LS | LF | Holgura |
|---|---|---|---|---|---|---|
| Requisitos | 8 | 0 | 8 | 0 | 8 | 0 ★ |
| Diseno | 16 | 8 | 24 | 38 | 54 | 30 |
| BaseDatos | 12 | 8 | 20 | 8 | 20 | 0 ★ |
| Infraestructura | 8 | 8 | 16 | 80 | 88 | 72 |
| Backend | 24 | 20 | 44 | 20 | 44 | 0 ★ |
| API | 10 | 44 | 54 | 44 | 54 | 0 ★ |
| Documentacion | 6 | 44 | 50 | 80 | 86 | 36 |
| Frontend | 20 | 54 | 74 | 54 | 74 | 0 ★ |
| Capacitacion | 6 | 50 | 56 | 86 | 92 | 36 |
| Pruebas | 14 | 74 | 88 | 74 | 88 | 0 ★ |
| Despliegue | 4 | 88 | 92 | 88 | 92 | 0 ★ |
| Lanzamiento | 2 | 92 | 94 | 92 | 94 | 0 ★ |

- **Duración mínima: 94 horas**
- **Camino crítico:** Requisitos → BaseDatos → Backend → API → Frontend → Pruebas → Despliegue → Lanzamiento

## 7. RF6 – `proyecto.dot`
Se escribe un archivo de texto en formato DOT: un nodo por tarea (con su duración) y una flecha por dependencia.
Las tareas críticas se pintan de rojo claro y las flechas del camino crítico van en rojo.
No se renderiza; si se quiere ver: `dot -Tpng proyecto.dot -o proyecto.png`.

## 8. Pruebas (`Pruebas.java`)
| Caso | Qué verifica |
|---|---|
| DAG lineal A→B→C | Orden `[A,B,C]` con ambos métodos, total 9, todas críticas |
| Diamante | Órdenes válidos, total 8, B con holgura 2 |
| Con ciclo A→B→C→A | Se detecta el ciclo y Kahn no logra ordenar todo |
| Tarea única | Total = su duración, holgura 0 |
| Dependencias múltiples | Una tarea espera a la predecesora más lenta; holguras correctas |

Las 21 verificaciones pasan.

## 9. Restricciones de la práctica
Java 17+, solo `java.util`/`java.io` (sin librerías de grafos) y código propio; los algoritmos están basados en Cormen et al..
