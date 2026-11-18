# SCA y SAST con herramientas de código abierto

Usá dos preguntas para empezar. ¿Qué componentes trae el Prode y cuáles tienen avisos conocidos? Eso se investiga con **SCA** (análisis de composición de software). ¿En qué clases podrían existir fallas de seguridad? Eso se investiga con **SAST** (análisis estático de seguridad). Ambas técnicas aportan evidencia para revisión técnica.

| | SCA | SAST |
| --- | --- | --- |
| Entrada | Versiones resueltas de dependencias directas y transitivas. En este proyecto, un inventario SBOM en formato CycloneDX. | Código fuente o artefactos compilados. SpotBugs analiza bytecode Java con detectores de Find Security Bugs. |
| Salida | Paquete, versión, aviso publicado (por ejemplo CVE u OSV) y versión corregida cuando se conoce. | Regla, prioridad y ubicación en el código que requiere revisión. |
| Caso Prode | Una biblioteca traída por un starter aparece en el inventario aunque no esté declarada directamente en el POM. | Un detector señala una operación sensible. Revisá la entrada, su validación y el contexto en el que se usa. |
| Alcance | Depende del inventario, perfiles y base de avisos. | Depende del código compilado, reglas y límites de análisis. Complementalo con revisión de permisos y pruebas de integración. |

[SCA con OSV-Scanner](https://google.github.io/osv-scanner/usage/), [CycloneDX para Maven](https://cyclonedx.github.io/cyclonedx-maven-plugin/), [Detectores de Find Security Bugs](https://find-sec-bugs.github.io/).

## Corrida de práctica

Desde la raíz del proyecto, con Java, Maven y OSV-Scanner v2 disponibles:

```powershell
mvn -B verify '-Psast,sca'
osv-scanner scan source -L target/bom.json
mvn -B dependency:tree
```

El perfil `sast` ejecuta SpotBugs con Find Security Bugs durante la verificación. El perfil `sca` genera el inventario, que incluye el alcance de pruebas del POM. Si el análisis estático detiene el build, conservá su salida y ejecutá `mvn -B verify -Psca` para generar el inventario por separado. OSV contrasta el inventario con avisos de vulnerabilidad. Generar el SBOM y consultar los avisos son dos pasos de la corrida. [Uso de SpotBugs en Maven](https://spotbugs.github.io/spotbugs-maven-plugin/usage.html), [Escaneo de un SBOM con OSV v2](https://github.com/google/osv-scanner/blob/main/docs/scan-source.md), [Árbol de dependencias Maven](https://maven.apache.org/plugins/maven-dependency-plugin/tree-mojo.html).

En la primera corrida, SpotBugs señaló que la clave generada por la base podía ser nula. Ahora esa operación falla con un error explícito y la transacción revierte. Find Security Bugs también enumeró las rutas Spring bajo `SPRING_ENDPOINT`. El [filtro](../config/spotbugs-exclude.xml) excluye ese aviso de inventario en los controladores del prototipo. Los detectores de seguridad siguen activos y las [pruebas HTTP](../src/test/java/ar/edu/ingsoftware/prode/web/FlujoWebTest.java) verifican el acceso por rol y CSRF. [Definición de SPRING_ENDPOINT](https://find-sec-bugs.github.io/bugs.htm#SPRING_ENDPOINT).

Para el ejercicio de SCA, el primer inventario con Spring Boot 3.5.16 produjo avisos en cinco componentes, entre ellos Spring Web MVC, Jackson y Tomcat. Al actualizar a 4.1.2 volvimos a correr las pruebas y escaneamos el inventario resuelto. OSV-Scanner 2.7.1 analizó 97 componentes y no devolvió avisos en la corrida del 17/11/2026. El resultado depende de las versiones y del catálogo de avisos disponible en ese momento. [Spring Boot 4.1.2](https://spring.io/blog/2026/10/23/spring-boot-4-1-2-available-now/).

Para cada corrida anotá fecha, commit, versiones, perfiles, archivo analizado, código de salida y resultado. Si una herramienta falta o si hay un error de conexión, dejá el resultado pendiente. Una corrida con cero avisos se refiere al inventario y las reglas o bases consultadas ese día.

Si aparece un aviso SCA, ubicá la cadena que trae el componente con `dependency:tree`. Confirmá que la versión y las condiciones del aviso aplican al entorno. Una corrección habitual es actualizar la dependencia directa o la que controla su versión, repetir pruebas y volver a escanear. Para una alerta SAST, revisá en qué método aparece, documentá si aplica y vinculá el cambio o la prueba de regresión. [Interpretar la salida de OSV](https://google.github.io/osv-scanner/output/).

## Ejercicio

Elegí una dependencia directa y una transitiva del árbol. Encontrá sus versiones en el inventario y anotá cómo llegaron al proyecto. Para SAST, ubicá en el servicio una operación sensible como publicar un resultado y documentá qué evidencia hay sobre la comprobación del rol. Si el escáner arroja un hallazgo, completá regla o identificador, archivo o versión, impacto y decisión. Si la corrida termina sin avisos, presentá el alcance y las comprobaciones manuales que hiciste.

## Autocontrol

1. ¿Una biblioteca transitiva aparece siempre en `pom.xml`? Puede llegar por otra dependencia. Revisá el árbol resuelto y el SBOM.
2. ¿Qué demuestra el archivo `target/bom.json`? Inventario de componentes en el alcance configurado. Las alertas conocidas se obtienen en la consulta posterior.
3. ¿Por qué puede haber errores de autorización con ambos análisis en verde? La autorización depende de reglas de negocio y caminos de ejecución. Las pruebas por rol y la revisión del flujo aportan esa evidencia.
