# Deuda técnica y métricas de calidad

Una decisión de diseño puede acelerar una entrega y encarecer los cambios siguientes. En el Prode, repetir la fórmula de puntos en la pantalla, en el ranking y en una exportación haría que corregir una regla obligara a revisar tres lugares. Ese trabajo repetido es parte del costo de mantener la deuda. Resolverla puede consistir en compartir la regla, cubrirla con pruebas y verificar que cada consumidor recibe el mismo cálculo. [Deuda técnica, Martin Fowler](https://martinfowler.com/bliki/TechnicalDebt.html).

## Qué observar

| Señal | Qué mide o detecta | Cómo leerla en el Prode |
| --- | --- | --- |
| Duplicación | CPD encuentra secuencias de código repetidas. Este proyecto configura un mínimo de 100 tokens en Java de producción y pruebas. | Si dos lugares repiten el mismo criterio de cierre, compará el significado antes de extraerlo. Revisá también repeticiones más pequeñas durante la revisión. |
| Bugs | Problemas potenciales de confiabilidad señalados por reglas o defectos confirmados con evidencia. | Si se acepta una edición con `ahora == cierre`, contrastá el resultado observado con la regla `ahora < cierre`. |
| Cobertura | Código ejecutado al correr las pruebas. Mirá líneas y ramas por separado. | Ejecutar una validación con un partido abierto puede cubrir una línea y dejar pendiente el camino de rechazo. |
| Vulnerabilidades y Security Hotspots | Hallazgos potenciales de seguridad y puntos sensibles que deben revisarse en su contexto, respectivamente. | Comprobá que un cambio de resultado exige el rol administrador en el servidor. |
| Code smells | Señales de mantenibilidad como un método difícil de seguir o reglas repetidas. | Si el método que calcula puntos empieza a consultar usuarios y renderizar tablas, se mezclan motivos de cambio. |
| Métricas estructurales | Complejidad ciclomática, complejidad cognitiva, acoplamiento y cohesión. | La ciclomática aproxima caminos independientes. La cognitiva estima dificultad de lectura. Para acoplamiento y cohesión, mirá qué dependencias tiene una clase y qué tan relacionadas están sus funciones. |
| Métricas de tamaño | Líneas físicas, líneas de código sin comentarios ni blancos (`ncloc`), archivos, métodos y clases. | Compará el mismo alcance. Abrir cinco clases para reemplazar un método largo puede bajar la complejidad local y subir el número de clases. |

PMD, CPD, JaCoCo y Maven Enforcer se ejecutan localmente como alternativa a SonarQube y SonarLint. Cada herramienta genera evidencia de un aspecto distinto del código. Para estimar deuda, registrá el costo de corregir un hallazgo y de convivir con él. Al comparar corridas conservá el alcance, las reglas, los umbrales y la unidad de medida. [PMD para Maven](https://maven.apache.org/plugins/maven-pmd-plugin/) · [Contadores de JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/counters.html)

## Cómo sacar evidencia

Corré `mvn -B clean verify` desde la raíz. Maven descarga los plugins y dependencias de Maven Central. El análisis se ejecuta en tu máquina sin cuenta ni servidor de análisis.

1. Para lint, abrí `target/pmd.xml`. Las [reglas](../config/pmd.xml) se aplican a Java de producción y pruebas. Identifican, por ejemplo, imports sobrantes, variables sin uso y bloques `catch` vacíos.
2. Para duplicación, abrí `target/cpd.xml`. Si hay bloques repetidos, el informe indica sus archivos, posiciones y cantidad de tokens. PMD y CPD hacen fallar `verify` cuando detectan una infracción de sus reglas. [Lint con PMD](https://maven.apache.org/plugins/maven-pmd-plugin/check-mojo.html) · [Duplicación con CPD](https://maven.apache.org/plugins/maven-pmd-plugin/cpd-mojo.html)
3. Para integridad, buscá `RequireJavaVersion`, `DependencyConvergence` y el resultado de JUnit. Enforcer comprueba que el entorno usa Java 21 o superior y que las versiones del grafo de dependencias convergen en los alcances que analiza. JUnit verifica también reglas de la aplicación, como los datos válidos, los permisos y la inscripción concurrente. Podés correr solo Enforcer con `mvn -B validate`. [Convergencia de dependencias](https://maven.apache.org/enforcer/enforcer-rules/dependencyConvergence.html)
4. Para cobertura, abrí `target/site/jacoco/index.html`. Guardá el commit, la fecha y las métricas de líneas y ramas del paquete de puntaje. JaCoCo también informa complejidad ciclomática y cantidad de métodos y clases.

## Ejercicio

Elegí una observación real del reporte o del código y armá una ficha con categoría, evidencia, impacto, cambio propuesto y cómo medirlo. Si elegís cobertura en el cierre, buscá pruebas para `cierre − 1 s`, `cierre` y `cierre + 1 s`. Si encontrás un hueco, redactá el caso y explicá qué comportamiento comprobaría.

Como línea de base del prototipo, la corrida del 17/11/2026 ejecutó **39 pruebas**. JaCoCo registró **555 de 676 líneas (82,1 %)** y **188 de 297 ramas (63,3 %)**. Tomá ambos valores del reporte que genere tu commit para comparar cambios.

## Autocontrol

1. ¿100 % de líneas demuestra un sistema correcto? La métrica cuenta ejecución. La calidad de las aserciones, los casos de frontera y las condiciones particulares requieren revisión.
2. ¿Cómo diferenciás tamaño de complejidad? Contar líneas indica volumen. Contar decisiones y revisar su anidamiento ayuda a estimar caminos y dificultad de lectura.
3. ¿Cómo comparás la deuda antes y después de un cambio? Repetí las mediciones sobre commits identificados, conservá la configuración y explicá qué costo futuro baja.
