# Proceso de desarrollo y prototipo

El prototipo sirve para recorrer el Prode con datos ficticios y obtener devoluciones sobre reglas, permisos y tiempos. Funciona como aplicación web local y adaptable a pantallas chicas. Un administrador carga los resultados manualmente. El alcance documentado contempla pocos torneos, usuarios y partidos. El ranking se consulta tras publicar el resultado. Las funciones financieras quedan fuera del MVP. [Alcance del prototipo (SCRUM-54)](https://ingsoftap2026.atlassian.net/browse/SCRUM-54).

## De la historia a la revisión

| Paso | Qué mostrar en el Prode |
| --- | --- |
| Entender la necesidad | El participante quiere registrar o corregir su pronóstico a tiempo. Acordá qué hora se usa y qué pasa justo al cierre. |
| Diseñar una porción vertical | Formulario de pronóstico, autorización e inscripción, validación de hora y persistencia. |
| Implementar | La pantalla envía los goles al servidor. El servicio decide si acepta y persiste el marcador. |
| Verificar | Cubrí antes, durante y después del cierre. Probá recarga y reinicio, roles y una carrera entre edición y cierre. |
| Entregar la demo | Prepará datos ficticios repetibles, instrucciones de arranque y una corrida de las pruebas. |
| Revisar con el Product Owner | Mostrá el flujo y anotá su devolución. |
| Mantener y adaptar | Si una regla cambia, revisá qué historias afecta, actualizá las pruebas y medí las consecuencias en el siguiente incremento. |

La app usa **Java 21, Spring Boot, Thymeleaf y JDBC** con una base **H2 en archivo** para la demo. **Maven** compila y ejecuta las pruebas **JUnit**. **JaCoCo** mide qué código atraviesan las pruebas. El flujo de escritura pasa por el servidor y la estructura persistente está en la migración. [Servicio de aplicación](../src/main/java/ar/edu/ingsoftware/prode/ProdeService.java), [Migración SQL](../src/main/resources/db/migration/V1__prode.sql).

## Guion para mostrar el prototipo

1. Arrancá con el perfil `demo` y los usuarios ficticios del [README de la aplicación](../README.md).
2. Entrá como participante. Mostrá el catálogo, una inscripción y el alta o la edición de tu propio pronóstico en un partido abierto. Recargá la pantalla para verificar que se guardó.
3. Mostrá un partido cuyo plazo ya venció. Explicá que la comprobación decisiva se hace con el reloj del servidor y complementá la UI con la prueba automatizada del instante exacto.
4. Entrá como administrador y publicá un resultado siguiendo el escenario del README. Mostrá dos torneos con el mismo marcador oficial y compará sus rankings con reglas diferentes. Si corregís el resultado, recalculá a mano una fila y contrastala con la pantalla.
5. Si vas a demostrar persistencia, reiniciá en el mismo directorio y con la misma base. Volvé a entrar y comprobá el valor guardado antes del reinicio.

## Decisiones del prototipo para validar con el Product Owner

- El catálogo requiere iniciar sesión. Un participante puede inscribirse en varios torneos próximos o activos. La baja de inscripción queda fuera del alcance de esta versión.
- Cada participante ve sus pronósticos. El plazo inicial se fija en el inicio programado y se evalúa con el reloj del servidor.
- Suspender conserva lo ya guardado. Una reprogramación toma como límite el menor entre el cierre vigente y el nuevo inicio. Reanudar antes del cierre permite continuar. Un plazo vencido permanece cerrado.
- La corrección del resultado reemplaza el marcador usado para el cálculo. El ranking se calcula por torneo y desempata por aciertos exactos y alias del participante.

Confirmá estas decisiones al preparar la Review. Para reprogramación, llevá a la Review el caso de un partido postergado y la pregunta de si debería permitir cambios a quien ya había pronosticado. El borrado de partidos y las notificaciones también figuran entre los temas a acordar. [Preguntas al Product Owner (SCRUM-52)](https://ingsoftap2026.atlassian.net/browse/SCRUM-52), [Cierre del Sprint 2 (SCRUM-59)](https://ingsoftap2026.atlassian.net/browse/SCRUM-59).

## Ejercicio

Armá la ficha de una corrida con rol, torneo, partido, hora de cierre, pronóstico, resultado esperado y resultado observado. Registrá en otra fila qué tendría que pasar al corregir el resultado real. Si hacés una captura, incluí los datos suficientes para repetir el escenario.

## Autocontrol

1. ¿Por qué sirve una porción vertical? Permite probar el comportamiento desde la interfaz hasta el almacenamiento y obtener comentarios sobre un flujo completo.
2. ¿Qué reloj decide el cierre? El del servidor. El instante permitido cumple `ahora < cierre`.
3. ¿Qué comprueba el reinicio? Que el pronóstico sobrevive al proceso cuando se abre la misma base local.
