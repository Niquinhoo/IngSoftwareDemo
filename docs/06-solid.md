# Principios SOLID con el cálculo de puntaje

Abrí [Puntuacion.java](../src/main/java/ar/edu/ingsoftware/prode/puntaje/Puntuacion.java) y buscá `ReglaPuntaje`. Usá `ProdeService` para seguir el origen de los datos y compará con el método que arma la tabla de posiciones. [El principio de responsabilidad única, Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2014/05/08/SingleReponsibilityPrinciple.html), [Discusión sobre SOLID](https://blog.cleancoder.com/uncle-bob/2020/10/18/Solid-Relevance.html).

| Principio | Qué exige | Cómo verlo o aplicarlo |
| --- | --- | --- |
| **S · Responsabilidad única** | Un módulo tiene un motivo de cambio ligado a quien solicita ese cambio. | Cambiar qué suma puntos corresponde a la regla. Cambiar cómo se despliega el ranking corresponde a la vista. Revisá por separado las funciones de acceso a datos y coordinación del servicio. |
| **O · Abierto/cerrado** | Prepará una variación concreta para que se pueda extender conservando el comportamiento ya probado. | `ranking(ReglaPuntaje, ...)` acepta una nueva regla sin editar su acumulación u orden. Si querés ofrecerla como opción persistida en la web, registrá su tipo y conectalo al selector. |
| **L · Sustitución de Liskov** | Una implementación nueva cumple el contrato que esperan sus clientes. | Clásica, alternativa y doble final aceptan entradas válidas del mismo tipo y entregan puntos. Una implementación que rechaza los empates válidos rompe ese contrato aunque Java compile. |
| **I · Segregación de interfaces** | Cada cliente depende de las operaciones que usa. | El ranking usa `puntos(Entrada)`. Una regla de puntaje puede servir al ranking sin tener que exportar CSV, consultar usuarios ni persistir resultados. |
| **D · Inversión de dependencias** | La política depende de una abstracción a la que se ajustan los detalles. | El método de acumulación conoce `ReglaPuntaje`. Las implementaciones se ajustan a ese contrato. El cierre recibe un `Clock` y las pruebas controlan el instante a través de ese mismo contrato. |

Inyectar un objeto es una técnica. La inversión de dependencias describe la dirección de la relación entre la política que consume un contrato y los detalles intercambiables que lo implementan. `Clock` permite elegir la fuente del tiempo en las pruebas. [`Instant.now(Clock)` en Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Instant.html#now(java.time.Clock)), [Pruebas de servicio](../src/test/java/ar/edu/ingsoftware/prode/ProdeServiceTest.java).

## Ejercicio

Proponé una regla de práctica: cinco puntos por acertar un empate exacto y cero en los demás casos. Escribila como expresión compatible con `ReglaPuntaje` en un borrador. Calculá los puntos esperados de 1–1 sobre 1–1, 0–0 sobre 1–1 y 2–1 sobre 2–1. Compará tu propuesta con la prueba `aceptaUnaEstrategiaNuevaSinCambiarElRanking` y explicá qué cambios requeriría habilitarla como una opción por torneo. [Pruebas de puntaje](../src/test/java/ar/edu/ingsoftware/prode/puntaje/PuntuacionTest.java).

## Autocontrol

1. ¿Qué devolvería la regla nueva en los tres casos? 5, 0 y 0. La tabla puede contar el acierto exacto de 2–1 aunque la regla le asigne cero puntos.
2. ¿Hay que cambiar la fórmula de acumulación? El contrato existente permite pasar la regla al cálculo. Exponerla en la aplicación requiere registrar y seleccionar la variante.
3. ¿Qué prueba añadirías para sustitución? La misma entrada válida llega a cada estrategia y produce un puntaje válido. Si una regla devuelve cero en ciertos casos, verificá que el ranking conserve el orden acordado y el conteo de exactos.
