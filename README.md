# IngSoftwareDemo · 7Asociados Prode

Prototipo web en Java para el Parcial 2 de Ingeniería en Software. El recorrido va desde el registro hasta el ranking y muestra cómo dos torneos aplican reglas distintas al mismo resultado. El laboratorio de consola usa el mismo motor de puntuación para practicar los demás patrones del programa. [Jira del proyecto](https://ingsoftap2026.atlassian.net/browse/SCRUM-53) · [Desafío 6](https://ingsoftap2026.atlassian.net/browse/SCRUM-72)

## Arrancar en tu máquina

Necesitás Git, JDK **21** y Maven **3.9**. Verificá que `mvn --version` indique Java 21. Podés clonar el repositorio público y ejecutar los controles sin iniciar sesión.

```sh
git clone https://github.com/Niquinhoo/IngSoftwareDemo.git
cd IngSoftwareDemo
mvn -B verify
```

Con `mvn spring-boot:run` el catálogo arranca vacío y podés registrar participantes. Abrí [localhost:8080](http://127.0.0.1:8080). Para habilitar administración, elegí un correo y una contraseña local, y configurá `PRODE_ADMIN_EMAIL` y `PRODE_ADMIN_PASSWORD` antes de arrancar. El arranque crea esa cuenta si todavía no existe. La contraseña se guarda con BCrypt. Para elegir otra contraseña de una cuenta de prueba existente, iniciá con otra base local o con otro correo de administrador.

La base H2 persiste en `datos/prode.mv.db`. Flyway aplica las [migraciones](src/main/resources/db/migration) al iniciar. Para volver a una versión anterior con tus datos, detené la aplicación y restaurá una copia del archivo tomada antes de migrar. Para repetir el ensayo con datos nuevos, detenela y renombrá `datos/demo.mv.db`. El próximo arranque con el perfil `demo` crea otro archivo.

## Ensayar la defensa

El perfil `demo` prepara dos torneos, participantes ficticios y un partido futuro para practicar alta y edición. Usa su propia base `datos/demo.mv.db`. Elegí dos contraseñas de ensayo de al menos ocho caracteres y hasta 72 bytes.

**PowerShell**

```powershell
$env:PRODE_ADMIN_EMAIL = 'admin@prode.test'
$env:PRODE_ADMIN_PASSWORD = Read-Host 'Contraseña local de administración' -MaskInput
$env:PRODE_DEMO_PASSWORD = Read-Host 'Contraseña local de participantes' -MaskInput
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

**Bash**

```bash
export PRODE_ADMIN_EMAIL=admin@prode.test
read -rsp 'Contraseña local de administración: ' PRODE_ADMIN_PASSWORD; printf '\n'
read -rsp 'Contraseña local de participantes: ' PRODE_DEMO_PASSWORD; printf '\n'
export PRODE_ADMIN_PASSWORD PRODE_DEMO_PASSWORD
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

1. Entrá como `ana@prode.test` o `luis@prode.test` con la contraseña de participantes. En **Liga de cátedra**, mirá el ranking del partido 2–1 y practicá guardar y editar un pronóstico en el partido abierto. Para ensayar la inscripción, registrá otra cuenta con un correo ficticio.
2. En otra ventana, entrá como administrador. Abrí **Copa de cátedra**, gestioná el partido que está **En juego** y publicá **2–1**. Compará los rankings. Ana suma **3** en Liga clásica y **5** en Copa alternativa. Luis suma **1** y **2** por pronosticar 1–0. Ese cambio de algoritmo entre torneos demuestra **Strategy**.
3. Cambiá la regla de la Copa a **Alternativa con doble final**. Ana pasa de **5** a **10** y Luis de **2** a **4**, con el mismo partido. **Decorator** agrega el bono sobre la regla alternativa. Si corregís el resultado a 1–0, el ranking se recalcula con el nuevo marcador.
4. Mostrá la acción encapsulada en [PublicarResultado.java](src/main/java/ar/edu/ingsoftware/prode/PublicarResultado.java) (**Command**) y el aviso que registra [RegistroResultados.java](src/main/java/ar/edu/ingsoftware/prode/RegistroResultados.java) después de confirmar la operación (**Observer**).
5. Ejecutá el [laboratorio](src/main/java/ar/edu/ingsoftware/prode/puntaje/laboratorio/LaboratorioPatrones.java) para mostrar **Interpreter, Singleton, Bridge, Template Method, Mediator y Visitor** con filtros, reportes y el mismo ranking.

```sh
mvn -B test
java -cp target/classes ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones
```

## Verificación y calidad

| Control | Cómo ejecutarlo | Resultado |
| --- | --- | --- |
| Integridad con Maven Enforcer | `mvn -B validate` | Versión de Java y convergencia de dependencias verificadas |
| Lint con PMD y duplicación con CPD | `mvn -B verify` | `target/pmd.xml` y `target/cpd.xml` |
| JUnit y cobertura JaCoCo | `mvn -B verify` | `target/surefire-reports/` y `target/site/jacoco/index.html` |
| SAST con SpotBugs y Find Security Bugs | `mvn -B verify -Psast` | `target/spotbugsXml.xml` |
| Inventario CycloneDX | `mvn -B verify -Psca` | `target/bom.json`, con dependencias directas, transitivas y de pruebas |
| SCA con OSV-Scanner 2 | `osv-scanner scan source -L target/bom.json` | Coincidencias entre el inventario y avisos conocidos |

Maven ejecuta los controles de calidad localmente y descarga sus dependencias de Maven Central. OSV-Scanner consulta un catálogo público de avisos. [Guía de calidad](docs/03-deuda-y-calidad.md) · [Guía de SCA y SAST](docs/04-sca-y-sast.md) · [Workflow de verificación](.github/workflows/verificacion.yml) · [20 casos de puntuación](src/test/java/ar/edu/ingsoftware/prode/puntaje/PuntuacionTest.java)

### Informes en PDF

El paquete de la corrida del 18 de noviembre de 2026 contiene 39 fichas JUnit, seis informes de controles y un índice. [Descargar el ZIP con los 46 PDF](https://github.com/Niquinhoo/IngSoftwareDemo/releases/download/informes-2026-11-18/IngSoftwareDemo-2026-11-18.zip)

## Trabajo en equipo

- Revisen el [índice de guías](docs/README.md) y elijan un tema por persona. Cada guía trae un ejercicio y un autocontrol.
- En Jira acuerden alcance y criterios antes de mover una tarjeta a **En curso**. Incluyan la clave en la rama y PR, por ejemplo `SCRUM-79-estrategia`. Agreguen en la tarjeta el enlace al PR y evidencia de prueba. El cierre requiere revisión del equipo y aceptación funcional según la [Definition of Done](https://ingsoftap2026.atlassian.net/browse/SCRUM-55).
- Para el ensayo, el catálogo requiere sesión, la inscripción admite varios torneos habilitados y cada persona ve sus pronósticos. El cierre usa el reloj del servidor y el horario del partido. Una suspensión conserva los pronósticos y reprogramar jamás extiende el plazo original. El ranking publica alias, ordenados por puntos, aciertos exactos y alias. Validar estas decisiones de prototipo con el Product Owner antes de cerrar los issues. [Decisiones abiertas](https://ingsoftap2026.atlassian.net/browse/SCRUM-52) · [Trazabilidad del prototipo](docs/02-proceso-y-prototipo.md)

El entorno está preparado para práctica local con datos ficticios. El caso cubre torneos, pronósticos, resultados y ranking. La [guía de Jira](docs/01-jira-y-scrum.md) explica el alcance por sprint y la [guía de desafíos](docs/07-desafios.md) explica qué falta medir para defender la mejora.
