package ar.edu.ingsoftware.prode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.ingsoftware.prode.ProdeService.EstadoPartido;
import ar.edu.ingsoftware.prode.ProdeService.EstadoTorneo;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Marcador;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Tipo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:prodeservicio;DB_CLOSE_DELAY=-1")
@RecordApplicationEvents
class ProdeServiceTest {
    private static final String ADMIN = "admin@prueba.test";
    private static final String ANA = "ana@prueba.test";
    private static final String LUIS = "luis@prueba.test";
    private static final Instant INICIO = Instant.parse("2030-06-15T17:00:00Z");
    private static final LocalDate FECHA = LocalDate.of(2030, 6, 15);
    private static final Marcador DOS_UNO = new Marcador(2, 1);
    private static final Marcador UNO_CERO = new Marcador(1, 0);

    @Autowired private ProdeService servicio;
    @Autowired private JdbcClient datos;
    @Autowired private RelojDePrueba reloj;
    @Autowired private ApplicationEvents eventos;
    @Autowired private TransactionTemplate transaccion;

    @BeforeEach
    void preparar() {
        datos.sql("DELETE FROM predicciones").update();
        datos.sql("DELETE FROM inscripciones").update();
        datos.sql("DELETE FROM partidos").update();
        datos.sql("DELETE FROM equipos").update();
        datos.sql("DELETE FROM torneos").update();
        datos.sql("DELETE FROM cuentas").update();
        datos.sql("INSERT INTO cuentas VALUES (?, 'hash-de-prueba', 'ADMIN'), "
                + "(?, 'hash-de-prueba', 'USER'), (?, 'hash-de-prueba', 'USER')")
                .params(ADMIN, ANA, LUIS).update();
        reloj.fijar(INICIO.minusSeconds(3600));
    }

    @Test
    void rolesYReferenciasSeValidanEnServidor() {
        falla(() -> servicio.crearTorneo(ANA, "Copa", "", FECHA, FECHA, EstadoTorneo.ACTIVO, Tipo.CLASICA),
                HttpStatus.FORBIDDEN);
        var copa = torneo("Copa", Tipo.CLASICA);
        var otra = torneo("Otra", Tipo.CLASICA);
        var local = servicio.crearEquipo(ADMIN, copa, "Azul");
        var visitanteAjeno = servicio.crearEquipo(ADMIN, otra, "Rojo");
        falla(() -> servicio.crearPartido(ADMIN, copa, local, visitanteAjeno, INICIO, false), HttpStatus.NOT_FOUND);
        falla(() -> servicio.crearPartido(ADMIN, copa, local, local, INICIO, false), HttpStatus.BAD_REQUEST);
        falla(() -> servicio.crearEquipo(ANA, copa, "Rojo"), HttpStatus.FORBIDDEN);
        assertThat(servicio.partidos(copa)).isEmpty();
    }

    @Test
    void soloElInscritoPuedeGuardarYElCierreConservaElUltimoMarcador() {
        var copa = torneo("Copa", Tipo.CLASICA);
        var partido = partido(copa, false);
        falla(() -> servicio.guardarPrediccion(ANA, copa, partido, DOS_UNO, false), HttpStatus.FORBIDDEN);
        servicio.inscribir(ANA, copa);
        servicio.inscribir(LUIS, copa);
        servicio.guardarPrediccion(ANA, copa, partido, DOS_UNO, false);
        falla(() -> servicio.guardarPrediccion(ANA, copa, partido, UNO_CERO, false), HttpStatus.CONFLICT);
        assertThat(servicio.prediccion(LUIS, copa, partido)).isEmpty();
        reloj.fijar(INICIO.minusNanos(1));
        servicio.guardarPrediccion(ANA, copa, partido, UNO_CERO, true);
        reloj.fijar(INICIO);
        falla(() -> servicio.guardarPrediccion(ANA, copa, partido, DOS_UNO, true), HttpStatus.CONFLICT);
        falla(() -> servicio.guardarPrediccion(LUIS, copa, partido, DOS_UNO, false), HttpStatus.CONFLICT);
        reloj.fijar(INICIO.plusSeconds(1));
        falla(() -> servicio.guardarPrediccion(ANA, copa, partido, DOS_UNO, true), HttpStatus.CONFLICT);
        assertThat(servicio.prediccion(ANA, copa, partido)).get().extracting(ProdeService.Prediccion::marcador)
                .isEqualTo(UNO_CERO);
    }

    @Test
    void inscripcionRepetidaEsIdempotenteMientrasElTorneoEstaHabilitado() {
        var copa = torneo("Copa", Tipo.CLASICA);
        servicio.inscribir(ANA, copa);
        servicio.inscribir(ANA, copa);
        servicio.actualizarTorneo(ADMIN, copa, "Copa", "", FECHA, FECHA, EstadoTorneo.FINALIZADO, Tipo.CLASICA);
        falla(() -> servicio.inscribir(ANA, copa), HttpStatus.CONFLICT);
        falla(() -> servicio.inscribir(LUIS, copa), HttpStatus.CONFLICT);
        assertThat(servicio.inscrito(ANA, copa)).isTrue();
        assertThat(servicio.inscrito(LUIS, copa)).isFalse();
    }

    @Test
    void suspensionConservaPronosticosYReprogramacionTardiaNoReabre() {
        var copa = torneo("Copa", Tipo.CLASICA);
        var partido = partido(copa, false);
        servicio.inscribir(ANA, copa);
        servicio.guardarPrediccion(ANA, copa, partido, DOS_UNO, false);
        servicio.suspenderPartido(ADMIN, copa, partido);
        falla(() -> servicio.guardarPrediccion(ANA, copa, partido, UNO_CERO, true), HttpStatus.CONFLICT);
        reloj.fijar(INICIO.plusSeconds(1));
        servicio.reprogramarPartido(ADMIN, copa, partido, INICIO.plusSeconds(3600));
        assertThat(servicio.partido(copa, partido).cierre()).isEqualTo(INICIO);
        falla(() -> servicio.guardarPrediccion(ANA, copa, partido, UNO_CERO, true), HttpStatus.CONFLICT);
        assertThat(servicio.prediccion(ANA, copa, partido)).isPresent();
    }

    @Test
    void dosReglasConvivenYCorregirRecalculaSinAcumular() {
        var clasico = torneo("Liga", Tipo.CLASICA);
        var alternativo = torneo("Copa", Tipo.FINAL_DOBLE);
        var primero = partido(clasico, false);
        var segundo = partido(alternativo, true);
        servicio.inscribir(ANA, clasico);
        servicio.inscribir(ANA, alternativo);
        servicio.guardarPrediccion(ANA, clasico, primero, DOS_UNO, false);
        servicio.guardarPrediccion(ANA, alternativo, segundo, DOS_UNO, false);
        falla(() -> servicio.publicarResultado(ADMIN, clasico, primero, DOS_UNO), HttpStatus.CONFLICT);
        reloj.fijar(INICIO);
        servicio.iniciarPartido(ADMIN, clasico, primero);
        servicio.iniciarPartido(ADMIN, alternativo, segundo);
        servicio.publicarResultado(ADMIN, clasico, primero, DOS_UNO);
        servicio.publicarResultado(ADMIN, alternativo, segundo, DOS_UNO);
        assertThat(servicio.ranking(clasico).getFirst().puntos()).isEqualTo(3);
        assertThat(servicio.ranking(alternativo).getFirst().puntos()).isEqualTo(10);
        servicio.publicarResultado(ADMIN, alternativo, segundo, UNO_CERO);
        servicio.publicarResultado(ADMIN, alternativo, segundo, UNO_CERO);
        assertThat(servicio.ranking(alternativo).getFirst().puntos()).isEqualTo(4);
        assertThat(servicio.partido(alternativo, segundo).estado()).isEqualTo(EstadoPartido.FINALIZADO);
        assertThat(eventos.stream(ResultadoPublicado.class).map(ResultadoPublicado::correccion))
                .containsExactly(false, false, true);
    }

    @Test
    void inscripcionesYAltasConcurrentesSonUnicas() throws Exception {
        var copa = torneo("Copa", Tipo.CLASICA);
        var partido = partido(copa, false);
        assertThat(enParalelo(() -> { servicio.inscribir(ANA, copa); return true; })).containsExactly(true, true);
        assertThat(enParalelo(() -> {
            try {
                servicio.guardarPrediccion(ANA, copa, partido, DOS_UNO, false);
                return true;
            } catch (ProdeException e) {
                assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
                return false;
            }
        })).containsExactlyInAnyOrder(true, false);
        assertThat(datos.sql("SELECT COUNT(*) FROM inscripciones").query(Long.class).single()).isEqualTo(1);
        assertThat(datos.sql("SELECT COUNT(*) FROM predicciones").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void guardadoConcurrenteUsaElCierreReprogramado() throws Exception {
        var copa = torneo("Copa", Tipo.CLASICA);
        var partido = partido(copa, false);
        servicio.inscribir(ANA, copa);
        servicio.guardarPrediccion(ANA, copa, partido, DOS_UNO, false);
        var nuevoInicio = INICIO.minusSeconds(300);
        var enviada = new CountDownLatch(1);
        try (var tareas = Executors.newSingleThreadExecutor()) {
            var edicion = transaccion.execute(status -> {
                servicio.reprogramarPartido(ADMIN, copa, partido, nuevoInicio);
                var pendiente = tareas.submit(() -> {
                    enviada.countDown();
                    falla(() -> servicio.guardarPrediccion(ANA, copa, partido, UNO_CERO, true), HttpStatus.CONFLICT);
                });
                try {
                    assertThat(enviada.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                reloj.fijar(nuevoInicio);
                return pendiente;
            });
            assertThat(edicion).isNotNull();
            edicion.get(10, TimeUnit.SECONDS);
        }
        assertThat(servicio.prediccion(ANA, copa, partido)).get().extracting(ProdeService.Prediccion::marcador)
                .isEqualTo(DOS_UNO);
    }

    private List<Boolean> enParalelo(Callable<Boolean> accion) throws Exception {
        try (var tareas = Executors.newFixedThreadPool(2)) {
            var salida = new CountDownLatch(1);
            Callable<Boolean> sincronizada = () -> {
                assertThat(salida.await(5, TimeUnit.SECONDS)).isTrue();
                return accion.call();
            };
            var primera = tareas.submit(sincronizada);
            var segunda = tareas.submit(sincronizada);
            salida.countDown();
            return List.of(primera.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS));
        }
    }

    private long torneo(String nombre, Tipo tipo) {
        return servicio.crearTorneo(ADMIN, nombre, "", FECHA, FECHA, EstadoTorneo.ACTIVO, tipo);
    }

    private long partido(long torneo, boolean esFinal) {
        var local = servicio.crearEquipo(ADMIN, torneo, "Azul");
        var visita = servicio.crearEquipo(ADMIN, torneo, "Rojo");
        return servicio.crearPartido(ADMIN, torneo, local, visita, INICIO, esFinal);
    }

    private static void falla(Runnable accion, HttpStatus status) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ProdeException.class,
                e -> assertThat(e.status()).isEqualTo(status));
    }

    @TestConfiguration
    static class Configuracion {
        @Bean
        @Primary
        RelojDePrueba relojDePrueba() {
            return new RelojDePrueba();
        }
    }

    static class RelojDePrueba extends Clock {
        private volatile Instant instante = INICIO;

        void fijar(Instant instante) {
            this.instante = instante;
        }

        @Override public Instant instant() { return instante; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instante, zone); }
    }
}
