# Desafíos de ingeniería del Prode

En la defensa, vinculá cada riesgo con una decisión y una prueba. Usá el desafío 6 para mostrar cómo un cambio pedido por el organizador se traduce en una medida técnica.

| Desafío | Decisión o hipótesis del prototipo | Evidencia para mostrar |
| --- | --- | --- |
| Autorización y privacidad | El servidor verifica roles y cada participante gestiona sus propios pronósticos. | Ingresá como participante y como administrador y contrastá sus permisos. [SCRUM-32](https://ingsoftap2026.atlassian.net/browse/SCRUM-32), [SCRUM-59](https://ingsoftap2026.atlassian.net/browse/SCRUM-59). |
| Reloj y reprogramación | El plazo se decide con hora del servidor. Suspender conserva el valor confirmado y reprogramar mantiene cerrado un plazo vencido. | Mostrá los casos antes y en el instante exacto del cierre y una reprogramación tardía. [SCRUM-60](https://ingsoftap2026.atlassian.net/browse/SCRUM-60), [SCRUM-61](https://ingsoftap2026.atlassian.net/browse/SCRUM-61). |
| Concurrencia y persistencia | Hay una fila por participante y partido. Los cambios de calendario y las escrituras de pronósticos toman el mismo bloqueo transaccional por torneo. | Comprobá dos altas simultáneas y que el último pronóstico aceptado se conserva. Verificá con un reinicio usando la misma base. [SCRUM-62](https://ingsoftap2026.atlassian.net/browse/SCRUM-62), [SCRUM-63](https://ingsoftap2026.atlassian.net/browse/SCRUM-63). |
| Resultados y ranking consistente | Al corregir un resultado, el ranking usa el marcador corregido. Ordena por puntos, aciertos exactos y alias del participante. | Publicá y corregí un marcador. Recalculá a mano una fila y verificá el empate en una prueba de acumulación. [Épica de resultados](https://ingsoftap2026.atlassian.net/browse/SCRUM-5). |

## Desafío 6 · Reglas que cambian

El organizador quiere comparar modalidades para incentivar que más participantes pronostiquen por fecha. Si las fórmulas están incrustadas en el ranking, cada experimento obliga a tocar el acumulador y aumenta el riesgo de puntajes inconsistentes. **Strategy** permite variar la regla por torneo mientras el cálculo general recibe el mismo tipo de entrada. **State** encaja cuando el comportamiento cambia con la etapa de un objeto, por ejemplo, un partido cerrado frente a uno abierto. **Decorator** agrega un efecto sobre una regla, como el doble en una final. La demo usa esa composición para el puntaje y conserva a Strategy como decisión principal del desafío. [Historia técnica (SCRUM-72)](https://ingsoftap2026.atlassian.net/browse/SCRUM-72).

| Criterio del desafío | Cómo obtener evidencia |
| --- | --- |
| Agregar una regla en un día como máximo, sin tocar reglas existentes ni acumulación | Hacé una variante de punta a punta en una rama de práctica. Cronometrá diseño, implementación y pruebas. Guardá el diff y verificá que la fórmula de acumulación y las estrategias anteriores quedan intactas. La prueba de una regla inyectada comprueba el punto de extensión del motor. El tiempo para incorporar una variante al producto requiere su propia medición. |
| Dos reglas activas al mismo tiempo | Corré `mvn -B '-Dtest=ProdeServiceTest#dosReglasConvivenYCorregirRecalculaSinAcumular' test` y verificá la comparación para el mismo marcador oficial. En la web, mostrales los dos rankings a tus compañeros y señalá su configuración por torneo. |
| 20 de 20 puntajes esperados y ranking coherente | Corré `mvn -B -Dtest=PuntuacionTest test`. La prueba parametrizada tiene 20 filas con resultado esperado. Revisá las pruebas extras de acumulación y desempate y el resumen de JUnit antes de citar la precisión observada. |
| Menos de 5 s al recalcular una fecha, umbral a confirmar | Definí cuántos torneos, usuarios, partidos y pronósticos hay y qué operación abarca la medición. Cronometrá varias corridas equivalentes e informá equipo, versión, volumen y tiempos. El valor de 5 s está planteado como propuesta. |
| Participación por fecha | Definí la tasa como participantes inscriptos que enviaron al menos un pronóstico válido en la fecha / inscriptos habilitados en esa fecha. Medí cada modalidad en fechas comparables y registrá metas luego de observar la línea de base. |

Para la parte de pruebas, `SCRUM-81` pide preparar 20 predicciones y menciona 10 pruebas. El prototipo incluye una matriz automatizada de 20 evaluaciones de puntaje y otras pruebas de ranking e integración. Contrastá lo verificado con ese entregable antes de presentarlo como aceptado. [Pruebas de puntaje](../src/test/java/ar/edu/ingsoftware/prode/puntaje/PuntuacionTest.java), [Pruebas de servicio](../src/test/java/ar/edu/ingsoftware/prode/ProdeServiceTest.java), [Preparación de pruebas (SCRUM-81)](https://ingsoftap2026.atlassian.net/browse/SCRUM-81).

La historia padre queda en el backlog, vinculada a la épica de resultados. Para Sprint 3, el mapa es **análisis**, **diseño de estrategias**, **integración**, **pruebas** y **documentación y pitch**, cada tarea con un punto. [SCRUM-78](https://ingsoftap2026.atlassian.net/browse/SCRUM-78), [SCRUM-79](https://ingsoftap2026.atlassian.net/browse/SCRUM-79), [SCRUM-80](https://ingsoftap2026.atlassian.net/browse/SCRUM-80), [SCRUM-81](https://ingsoftap2026.atlassian.net/browse/SCRUM-81), [SCRUM-82](https://ingsoftap2026.atlassian.net/browse/SCRUM-82).

## Ejercicio y defensa

Con la final 2–1, prepará una fila que compare Clásica y Alternativa para un acierto exacto. Agregá una segunda fila aplicando doble final. Anotá datos, regla, expectativa y valor observado. Usá este guion para ensayar el pitch.

> Queremos probar formas de puntuar por torneo sin reescribir el ranking. La demo compara las reglas con el mismo resultado oficial y las pruebas comprueban los puntajes esperados. Pedimos aprobar el alcance y acordar cómo medir el tiempo de recálculo y la participación por fecha.

## Autocontrol

1. ¿Cómo se comprueba que conviven dos reglas? Ejecutalas en la misma corrida, sobre los mismos marcadores de referencia, y verificá los resultados esperados de cada torneo.
2. ¿Qué demuestra un caso con 3, 5 y 10 puntos? El exacto con Clásica vale 3, con Alternativa 5 y con Alternativa decorada en una final vale 10.
3. ¿Alcanzan las pruebas de puntaje para afirmar que se agregan reglas en un día o que se recalcula en 5 s? Cada objetivo requiere su propia medición con alcance y condiciones registrados.
