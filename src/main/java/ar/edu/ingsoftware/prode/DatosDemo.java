package ar.edu.ingsoftware.prode;

import ar.edu.ingsoftware.prode.ProdeService.EstadoPartido;
import ar.edu.ingsoftware.prode.ProdeService.EstadoTorneo;
import ar.edu.ingsoftware.prode.ProdeService.Torneo;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Marcador;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Tipo;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
class DatosDemo implements ApplicationRunner {
    private static final String LIGA = "Liga de cátedra";
    private static final String ANA = "ana@prode.test";
    private static final String LUIS = "luis@prode.test";

    private final ProdeDatos datos;
    private final JdbcClient jdbc;
    private final Clock reloj;
    private final PasswordEncoder encoder;
    private final String clave;

    DatosDemo(ProdeDatos datos, JdbcClient jdbc, Clock reloj, PasswordEncoder encoder,
              @Value("${prode.demo.password:}") String clave) {
        this.datos = datos;
        this.jdbc = jdbc;
        this.reloj = reloj;
        this.encoder = encoder;
        this.clave = clave;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (datos.torneos().stream().anyMatch(torneo -> torneo.nombre().equals(LIGA))) {
            return;
        }
        if (clave.isBlank() || clave.codePointCount(0, clave.length()) < 8
                || clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Para cargar el perfil demo, configurá PRODE_DEMO_PASSWORD (8 a 72 bytes)");
        }
        for (String email : new String[] { ANA, LUIS }) {
            jdbc.sql("INSERT INTO cuentas (email, password_hash, rol) VALUES (?, ?, 'USER')")
                    .params(email, encoder.encode(clave)).update();
        }
        var ahora = reloj.instant();
        prepararTorneo(LIGA, Tipo.CLASICA, ahora, false);
        prepararTorneo("Copa de cátedra", Tipo.ALTERNATIVA, ahora, true);
    }

    private void prepararTorneo(String nombre, Tipo regla, Instant ahora, boolean esFinal) {
        var hoy = LocalDate.ofInstant(ahora, ProdeService.ZONA);
        var id = datos.crearTorneo(new Torneo(0, nombre, "Datos ficticios para el Parcial 2",
                hoy.minusDays(1), hoy.plusDays(2), EstadoTorneo.ACTIVO, regla));
        var local = datos.crearEquipo(id, "Azul");
        var visita = datos.crearEquipo(id, "Rojo");
        var inicio = ahora.minusSeconds(3600);
        var partido = datos.crearPartido(id, local, visita, inicio, esFinal);
        datos.inscribir(id, ANA);
        datos.inscribir(id, LUIS);
        datos.altaPrediccion(id, partido, ANA, new Marcador(2, 1), inicio.minusSeconds(60));
        datos.altaPrediccion(id, partido, LUIS, new Marcador(1, 0), inicio.minusSeconds(60));
        if (esFinal) {
            datos.estadoPartido(partido, EstadoPartido.EN_JUEGO);
        } else {
            datos.resultado(partido, new Marcador(2, 1));
            datos.crearPartido(id, visita, local, ahora.plusSeconds(86400), false);
        }
    }
}
