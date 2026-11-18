# Jira y eventos de Scrum

Usá una historia del Sprint 2 para explicar cómo una necesidad del usuario llega al tablero y termina en una prueba verificable.

## Caso del Prode

| Iteración | Qué entrega el plan | Dónde verlo |
| --- | --- | --- |
| Sprint 1 · activo al 17/11/2026 | Registro, roles, ingreso y visualización de un torneo administrado, con alcance inicial para equipos y partidos | [SCRUM-10](https://ingsoftap2026.atlassian.net/browse/SCRUM-10), [SCRUM-11](https://ingsoftap2026.atlassian.net/browse/SCRUM-11), [SCRUM-12](https://ingsoftap2026.atlassian.net/browse/SCRUM-12), [SCRUM-13](https://ingsoftap2026.atlassian.net/browse/SCRUM-13), [SCRUM-14](https://ingsoftap2026.atlassian.net/browse/SCRUM-14) |
| Sprint 2 · futuro en ese corte | Inscripción y carga o edición de pronósticos, cierre según reloj del servidor, concurrencia y persistencia | [SCRUM-57](https://ingsoftap2026.atlassian.net/browse/SCRUM-57), [SCRUM-58](https://ingsoftap2026.atlassian.net/browse/SCRUM-58), [SCRUM-59](https://ingsoftap2026.atlassian.net/browse/SCRUM-59), [SCRUM-60](https://ingsoftap2026.atlassian.net/browse/SCRUM-60), [SCRUM-61](https://ingsoftap2026.atlassian.net/browse/SCRUM-61), [SCRUM-62](https://ingsoftap2026.atlassian.net/browse/SCRUM-62), [SCRUM-63](https://ingsoftap2026.atlassian.net/browse/SCRUM-63), [SCRUM-64](https://ingsoftap2026.atlassian.net/browse/SCRUM-64) |
| Sprint 3 · futuro en ese corte | Resultado, puntaje y ranking en el plan. Cinco tareas del desafío de reglas figuran en el Sprint con un punto cada una | [SCRUM-78](https://ingsoftap2026.atlassian.net/browse/SCRUM-78), [SCRUM-79](https://ingsoftap2026.atlassian.net/browse/SCRUM-79), [SCRUM-80](https://ingsoftap2026.atlassian.net/browse/SCRUM-80), [SCRUM-81](https://ingsoftap2026.atlassian.net/browse/SCRUM-81), [SCRUM-82](https://ingsoftap2026.atlassian.net/browse/SCRUM-82) |

El proyecto tenía 48 ítems, con registro y roles en curso, 46 por hacer y 0 hechos en ese corte. Una estimación de un punto expresa esfuerzo relativo. Para explicar el Sprint 3, usá los ítems 78 a 82 y la historia padre del Product Backlog. Los ítems 73 a 77 corresponden al registro histórico. [Plan de tres sprints (SCRUM-53)](https://ingsoftap2026.atlassian.net/browse/SCRUM-53), [Desafío 6 (SCRUM-72)](https://ingsoftap2026.atlassian.net/browse/SCRUM-72).

## Cómo leer el tablero

Abrí el backlog para identificar el objetivo, las historias seleccionadas y el estado del sprint. La propuesta de flujo contempla Por hacer, En curso, Testing / Review, Blocked y Done. En este corte, los ítems tienen los estados indicados arriba. Para revisar el alcance de Sprint 2, abrí también la tarea de planificación y sus dependencias con Sprint 1. [Propuesta de flujo (SCRUM-56)](https://ingsoftap2026.atlassian.net/browse/SCRUM-56), [Dependencias del Sprint 2 (SCRUM-65)](https://ingsoftap2026.atlassian.net/browse/SCRUM-65).

En la búsqueda avanzada podés usar `project = SCRUM AND sprint IN openSprints()` para ver ítems del sprint activo. Usá `project = SCRUM AND sprint IN futureSprints()` para los futuros. [Funciones JQL](https://support.atlassian.com/jira-software-cloud/docs/jql-functions/).

## Qué pasa en cada evento

- **Sprint Planning.** El Scrum Team define por qué vale la pena el Sprint, qué se puede entregar y cómo se realizará el trabajo. Para Sprint 2, un objetivo para el ensayo es permitir que un participante inscripto gestione su pronóstico hasta el cierre.
- **Daily Scrum.** Los Developers inspeccionan el avance hacia el objetivo y adaptan el plan. Para ensayar, imaginen que el alta de pronósticos ya funciona y falta comprobar que una edición concurrente al cierre se decida con hora del servidor. El equipo acuerda quién prepara datos y quién verifica el caso.
- **Sprint Review.** El Scrum Team y las partes interesadas inspeccionan el incremento y adaptan el Product Backlog. Mostrá la edición antes del cierre y el rechazo a la hora exacta. Una pregunta para el Product Owner es qué hacer al reprogramar.
- **Sprint Retrospective.** El Scrum Team revisa cómo trabajó y elige una mejora comprobable para el siguiente Sprint. Una mejora sería agregar las pruebas de frontera a la revisión de cada historia que dependa del reloj.

El Sprint contiene esos cuatro eventos. El refinamiento del Product Backlog es una actividad continua. El Product Owner responde por su orden, el Scrum Master ayuda a que Scrum funcione y los Developers crean el incremento y adaptan el Sprint Backlog. [Guía de Scrum en español latinoamericano](https://scrumguides.org/docs/scrumguide/v2020/2020-Scrum-Guide-Spanish-Latin-South-American.pdf).

El **criterio de aceptación** describe el comportamiento de la historia concreta. Por ejemplo, si el reloj del servidor marca exactamente la hora de cierre, la edición se rechaza. La **Definition of Done** fija el estándar que debe cumplir cualquier incremento. SCRUM-55 propone cumplir la aceptación, probar regresiones y errores, revisar con otra persona, obtener aceptación funcional y actualizar la documentación. El acuerdo final del equipo figura como pendiente en la tarea. Jira registra el estado del equipo y el prototipo aporta evidencia para el ensayo. [Criterios del Sprint 2](https://ingsoftap2026.atlassian.net/browse/SCRUM-59), [Propuesta de DoD (SCRUM-55)](https://ingsoftap2026.atlassian.net/browse/SCRUM-55).

## Ejercicio

Elegí una historia del Sprint 2 y escribí una tarjeta para el ensayo con usuario, necesidad y beneficio. Agregá tres casos con el reloj en `cierre − 1 s`, `cierre` y `cierre + 1 s`. Usá la primera fila para Planning, el riesgo de concurrencia para Daily, los resultados para Review y una mejora de proceso para Retro. Hacé el ejercicio en estas notas o en un borrador.

## Autocontrol

1. ¿Qué casos deberían aceptar la edición? Solo el anterior al cierre, si también se cumplen inscripción, autorización y estado del partido.
2. ¿Quién adapta su plan en la Daily? Los Developers.
3. ¿Cómo mostrarías que se cumplió la DoD? Abrí SCRUM-55, recorré sus condiciones y señalá la evidencia disponible para cada una.
