# Deuda técnica, métricas y SonarQube

Una decisión de diseño puede acelerar una entrega y encarecer los cambios siguientes. En el Prode, repetir la fórmula de puntos en la pantalla, en el ranking y en una exportación haría que corregir una regla obligara a revisar tres lugares. Ese trabajo repetido es parte del costo de mantener la deuda. Resolverla puede consistir en compartir la regla, cubrirla con pruebas y verificar que cada consumidor recibe el mismo cálculo. [Deuda técnica, Martin Fowler](https://martinfowler.com/bliki/TechnicalDebt.html).

## Qué observar

| Señal | Qué mide o detecta | Cómo leerla en el Prode |
| --- | --- | --- |
| Duplicación | Líneas o bloques repetidos. En Sonar, densidad = líneas duplicadas / líneas físicas × 100. | Si dos lugares repiten el mismo criterio de cierre, compará el significado antes de extraerlo. Las herramientas pueden omitir duplicaciones pequeñas. |
| Bugs | Problemas potenciales de confiabilidad señalados por reglas o defectos confirmados con evidencia. | Si se acepta una edición con `ahora == cierre`, contrastá el resultado observado con la regla `ahora < cierre`. |
| Cobertura | Código ejecutado al correr las pruebas. Mirá líneas y ramas por separado. | Ejecutar una validación con un partido abierto puede cubrir una línea y dejar pendiente el camino de rechazo. |
| Vulnerabilidades y Security Hotspots | Hallazgos potenciales de seguridad y puntos sensibles que deben revisarse en su contexto, respectivamente. | Comprobá que un cambio de resultado exige el rol administrador en el servidor. |
| Code smells | Señales de mantenibilidad como un método difícil de seguir o reglas repetidas. | Si el método que calcula puntos empieza a consultar usuarios y renderizar tablas, se mezclan motivos de cambio. |
| Métricas estructurales | Complejidad ciclomática, complejidad cognitiva, acoplamiento y cohesión. | La ciclomática aproxima caminos independientes. La cognitiva estima dificultad de lectura. Para acoplamiento y cohesión, mirá qué dependencias tiene una clase y qué tan relacionadas están sus funciones. |
| Métricas de tamaño | Líneas físicas, líneas de código sin comentarios ni blancos (`ncloc`), archivos, métodos y clases. | Compará el mismo alcance. Abrir cinco clases para reemplazar un método largo puede bajar la complejidad local y subir el número de clases. |

En SonarQube las etiquetas de los hallazgos pueden aparecer como bugs, vulnerabilidades y code smells en Standard Experience, o como impactos de confiabilidad, seguridad y mantenibilidad en MQR. SonarQube estima el esfuerzo de remediación sumando el esfuerzo asociado a los hallazgos de mantenibilidad. Su ratio lo divide por el costo estimado de desarrollo. Para comparar dos corridas usá el mismo alcance, perfil de reglas y unidad de medida. Registrá el commit de cada corrida y, para métricas de código nuevo, compará contra la misma referencia. [Definiciones de métricas de Sonar](https://docs.sonarsource.com/sonarqube-server/user-guide/code-metrics/metrics-definition), [Contadores de JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/counters.html).

## Cómo sacar evidencia

1. Corré `mvn -B clean verify` y abrí `target/site/jacoco/index.html`. Guardá el commit, la fecha y la cobertura de líneas y ramas del paquete de puntaje. Señalá una línea parcialmente cubierta, si el reporte trae alguna.
2. Si contás con una instancia local de SonarQube y un proyecto con permiso de análisis, configurá `SONAR_TOKEN` en tu sesión y corré desde la raíz:

   ```powershell
   mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar '-Dsonar.host.url=http://localhost:9000'
   ```

   El POM usa la clave de proyecto `IngSoftwareDemo` e importa `target/site/jacoco/jacoco.xml` generado en el paso anterior. En el tablero mirá **Measures**, **Issues** y el estado del **Quality Gate**. Anotá la versión del escáner, el perfil de reglas y la definición de código nuevo. Si falta la instancia, usá JaCoCo como evidencia local y registrá el análisis de SonarQube como pendiente. [SonarScanner para Maven](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/sonarscanner-for-maven), [Importar cobertura de Java](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/test-coverage/java-test-coverage).
3. En el editor, instalá **SonarQube for IDE**, antes llamado **SonarLint**, y abrí un archivo de Java para ver alertas durante la edición. Si se conecta al proyecto, puede tomar las reglas de su perfil. El Quality Profile habilita reglas y el Quality Gate evalúa condiciones sobre el análisis. [SonarQube for IDE](https://www.sonarsource.com/products/sonarqube/ide/), [Quality Gates](https://docs.sonarsource.com/sonarqube-community-build/quality-standards-administration/managing-quality-gates/introduction-to-quality-gates).

## Ejercicio

Elegí una observación real del reporte o del código y armá una ficha con categoría, evidencia, impacto, cambio propuesto y cómo medirlo. Si elegís cobertura en el cierre, buscá pruebas para `cierre − 1 s`, `cierre` y `cierre + 1 s`. Si encontrás un hueco, redactá el caso y explicá qué comportamiento comprobaría.

Como línea de base del prototipo, la corrida del 17/11/2026 ejecutó **39 pruebas**. JaCoCo registró **555 de 676 líneas (82,1 %)** y **188 de 297 ramas (63,3 %)**. Tomá ambos valores del reporte que genere tu commit para comparar cambios.

## Autocontrol

1. ¿100 % de líneas demuestra un sistema correcto? La métrica cuenta ejecución. La calidad de las aserciones, los casos de frontera y las condiciones particulares requieren revisión.
2. ¿Cómo diferenciás tamaño de complejidad? Contar líneas indica volumen. Contar decisiones y revisar su anidamiento ayuda a estimar caminos y dificultad de lectura.
3. ¿Cómo comparás la deuda antes y después de un cambio? Repetí las mediciones sobre commits identificados, conservá la configuración y explicá qué costo futuro baja.
