# Diez patrones de diseño en el Prode

Prepará un recorrido desde la publicación de un resultado hasta el ranking. Después abrí el laboratorio Java para ver variantes que podés ejecutar con JUnit. Cada patrón responde a un tipo de cambio. [Catálogo de Gamma, Helm, Johnson y Vlissides](https://www.informit.com/store/design-patterns-elements-of-reusable-object-oriented-9780201633610).

## Quien expone el flujo web y el puntaje

| Patrón | Idea y aplicación | Costo o límite |
| --- | --- | --- |
| **Command** · comportamiento | Representa un pedido como un objeto. Seguí `PublicarResultado` desde la acción del administrador hasta el servicio: los datos del pedido viajan juntos con la operación que los usa. | La forma de comando facilita delegar y probar el pedido. Historial, reintentos y deshacer requieren su propio diseño. |
| **Strategy** · comportamiento | Encapsula algoritmos intercambiables bajo un mismo contrato. `ReglaPuntaje` toma una entrada y devuelve puntos; el ranking acumula y ordena con la regla elegida. | El sistema debe resolver qué estrategia corresponde a cada solicitud. |
| **Observer** · comportamiento | Un publicador emite un evento y uno o más suscriptores reaccionan. El flujo de resultados publica `ResultadoPublicado` y `RegistroResultados` escribe en el registro de la aplicación tras confirmar la transacción. | Los efectos secundarios pasan a estar en el suscriptor. Explicá dónde se observan y en qué momento se ejecutan. |
| **Decorator** · estructural | Envuelve una operación que conserva su contrato y agrega comportamiento. `DobleEnFinal` delega el cálculo en la alternativa y duplica los puntos en una final. | Varias capas pueden volver más difícil identificar dónde cambió un valor. |

Abrí la [acción de resultado](../src/main/java/ar/edu/ingsoftware/prode/PublicarResultado.java), el [evento](../src/main/java/ar/edu/ingsoftware/prode/ResultadoPublicado.java) el [suscriptor](../src/main/java/ar/edu/ingsoftware/prode/RegistroResultados.java) y [Puntuacion.java](../src/main/java/ar/edu/ingsoftware/prode/puntaje/Puntuacion.java). En Spring, el momento de ejecución del listener puede depender de la fase de la transacción. El ranking web se calcula al consultar y toma los resultados que ya fueron confirmados. El registro de eventos permite mostrar cuándo se completó una publicación o corrección. [Eventos vinculados a transacciones](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html).

| Pronóstico con resultado oficial 2–1 | Clásica | Alternativa | Alternativa con doble final |
| --- | ---: | ---: | ---: |
| 2–1 · exacto | 3 | 5 | 10 |
| 1–0 · misma diferencia con signo | 1 | 2 | 4 |
| 3–1 · mismo ganador, distinta diferencia | 1 | 0 | 0 |
| 0–1 · ganador opuesto | 0 | 0 | 0 |

Los exactos tienen prioridad sobre las demás condiciones. La Clásica da 1 por acertar local, empate o visitante. La Alternativa da 2 por acertar la diferencia con signo. Doble final aplica el multiplicador únicamente si el partido está marcado como final.

## Quien expone el laboratorio y la coordinación

| Patrón | Idea y aplicación | Costo o límite |
| --- | --- | --- |
| **Interpreter** · comportamiento | Define cómo evaluar una gramática chica. El laboratorio interpreta `diferencia y final` como dos condiciones sobre una entrada y las combina con una conjunción. | Al crecer la gramática aumentan los casos a validar, como sintaxis inválida, precedencia y manejo de errores. |
| **Singleton** · creación | Controla la existencia de una única instancia en un alcance definido. El enum `Configuracion` del laboratorio tiene una única constante, `INSTANCIA`. Conserva de forma inmutable la opción `FINAL_DOBLE` con la que arranca el ejemplo. | Estado mutable compartido puede causar carreras y acoplar pruebas. Una instancia compartida puede recibir llamadas concurrentes. |
| **Bridge** · estructural | Usa composición para que dos dimensiones cambien por separado. El alcance del reporte puede ser completo o podio. El formato puede ser texto o CSV. Los dos ejes se combinan. | La separación agrega tipos. Conviene cuando cada dimensión tiene cambios propios. |
| **Template Method** · comportamiento | Una clase base fija la secuencia y deja un paso a las subclases. El reporte genera el ranking, selecciona filas y delega el formato. Reporte completo y podio especializan la selección. | Los pasos se especializan por herencia. Si cambian a menudo y de manera independiente, revisá el acoplamiento a la clase base. |
| **Mediator** · comportamiento | Coordina la interacción entre colaboradores. `MediadorResultados` conecta mesa y pantalla en el laboratorio. En el flujo web, `ProdeService` concentra la coordinación del caso de uso entre permisos, datos y puntaje. | Vigilá que el coordinador mantenga responsabilidades relacionadas y delegue cálculos específicos. |
| **Visitor** · comportamiento | Modela operaciones sobre tipos distintos con despacho según el tipo visitado. Un visitante inspecciona el estado de publicación. Otro convierte partidos publicados en entradas de puntaje. Los partidos pendientes producen una lista vacía en ese segundo visitante. | Agregar una operación es directo. Agregar un tipo de partido exige revisar los visitantes. |

Abrí el [código del laboratorio](../src/main/java/ar/edu/ingsoftware/prode/puntaje/laboratorio/LaboratorioPatrones.java), sus [pruebas](../src/test/java/ar/edu/ingsoftware/prode/puntaje/laboratorio/LaboratorioPatronesTest.java) y el [servicio de aplicación](../src/main/java/ar/edu/ingsoftware/prode/ProdeService.java).

## Ejercicio

Ejecutá `mvn -B '-Dtest=PuntuacionTest,LaboratorioPatronesTest' test` y luego `java -cp target/classes ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones`. Tomá una final 2–1 y un pronóstico 1–0. Calculá a mano los tres puntajes y decidí si pasa el filtro `diferencia y final`. Después abrí la prueba del reporte y señalá qué varía al pasar de completo a podio y qué varía al pasar de texto a CSV.

Una persona muestra la publicación y el contrato de puntos. La otra muestra la consulta del laboratorio, el reporte y la colaboración entre sus componentes.

## Autocontrol

1. ¿Qué da el caso del ejercicio? Clásica 1, Alternativa 2, Doble final 4. El filtro acepta la entrada por diferencia +1 y condición de final.
2. ¿Dónde se varía el algoritmo y dónde se agrega un efecto sobre él? La estrategia aporta el algoritmo y el decorador lo envuelve para duplicar los puntos.
3. ¿Qué parte demuestra Bridge y cuál Template Method? El intercambio del formato y del alcance muestra los dos ejes del Bridge. El orden fijo con selección de filas variable muestra Template Method.
4. ¿Qué ocurre con un partido pendiente en `EntradasOficiales`? Queda fuera de las entradas al ranking hasta tener resultado publicado.
5. ¿Quién coordina el flujo y quién recibe una notificación? El mediador dirige la colaboración. El observador reacciona al evento que se le publica.
