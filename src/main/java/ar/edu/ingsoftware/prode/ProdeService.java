package ar.edu.ingsoftware.prode;

import ar.edu.ingsoftware.prode.puntaje.Puntuacion;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Marcador;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Puesto;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Tipo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdeService {
    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    public enum EstadoTorneo { BORRADOR, PROXIMO, ACTIVO, FINALIZADO }
    public enum EstadoPartido { PROXIMO, EN_JUEGO, SUSPENDIDO, FINALIZADO }

    public record Torneo(long id, String nombre, String descripcion, LocalDate inicio, LocalDate fin,
                         EstadoTorneo estado, Tipo regla) {}
    public record Equipo(long id, long torneoId, String nombre) {}
    public record Partido(long id, long torneoId, long localId, long visitanteId, Instant inicio, Instant cierre,
                          EstadoPartido estado, boolean finalPartido, Marcador resultado) {}
    public record Prediccion(Marcador marcador, Instant creada, Instant actualizada) {}

    private final ProdeDatos datos;
    private final Clock reloj;
    private final ApplicationEventPublisher eventos;

    ProdeService(ProdeDatos datos, Clock reloj, ApplicationEventPublisher eventos) {
        this.datos = datos;
        this.reloj = reloj;
        this.eventos = eventos;
    }

    public List<Torneo> torneos() {
        return datos.torneos();
    }

    public Torneo torneo(long id) {
        return datos.torneo(id).orElseThrow(() -> noExiste("El torneo"));
    }

    public List<Equipo> equipos(long torneo) {
        torneo(torneo);
        return datos.equipos(torneo);
    }

    public List<Partido> partidos(long torneo) {
        torneo(torneo);
        return datos.partidos(torneo);
    }

    public Partido partido(long torneo, long partido) {
        return datos.partido(torneo, partido).orElseThrow(() -> noExiste("El partido en este torneo"));
    }

    public boolean inscrito(String actor, long torneo) {
        torneo(torneo);
        return datos.inscrito(torneo, actor);
    }

    public Optional<Prediccion> prediccion(String actor, long torneo, long partido) {
        partido(torneo, partido);
        return datos.prediccion(partido, actor);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<Puesto> ranking(long torneo) {
        var regla = torneo(torneo).regla();
        return Puntuacion.ranking(regla, datos.participantes(torneo), datos.entradasRanking(torneo));
    }

    @Transactional
    public long crearTorneo(String actor, String nombre, String descripcion, LocalDate inicio,
                            LocalDate fin, EstadoTorneo estado, Tipo regla) {
        exigirRol(actor, "ADMIN");
        if (estado == EstadoTorneo.FINALIZADO) {
            throw new ProdeException(HttpStatus.BAD_REQUEST, "Un torneo nuevo no puede estar finalizado");
        }
        return datos.crearTorneo(datosTorneo(0, nombre, descripcion, inicio, fin, estado, regla));
    }

    @Transactional
    public void actualizarTorneo(String actor, long id, String nombre, String descripcion, LocalDate inicio,
                                  LocalDate fin, EstadoTorneo estado, Tipo regla) {
        exigirRol(actor, "ADMIN");
        var actual = bloquearTorneo(id);
        var actualizado = datosTorneo(id, nombre, descripcion, inicio, fin, estado, regla);
        if (!transicionValida(actual.estado(), estado)) {
            throw conflicto("Ese cambio de estado no está permitido");
        }
        var partidos = datos.partidos(id);
        if (partidos.stream().anyMatch(partido -> !fechaDentro(actualizado, partido.inicio()))) {
            throw conflicto("Las fechas nuevas deben incluir todos los partidos existentes");
        }
        if (estado == EstadoTorneo.FINALIZADO && partidos.stream().anyMatch(
                partido -> partido.estado() == EstadoPartido.PROXIMO || partido.estado() == EstadoPartido.EN_JUEGO)) {
            throw conflicto("Finalizá o suspendé los partidos pendientes antes de cerrar el torneo");
        }
        datos.actualizarTorneo(actualizado);
    }

    @Transactional
    public long crearEquipo(String actor, long torneo, String nombre) {
        exigirRol(actor, "ADMIN");
        exigirEditable(bloquearTorneo(torneo));
        try {
            return datos.crearEquipo(torneo, nombreValido(nombre));
        } catch (DuplicateKeyException duplicado) {
            throw conflicto("Ya existe un equipo con ese nombre en el torneo");
        }
    }

    @Transactional
    public void actualizarEquipo(String actor, long torneo, long equipo, String nombre) {
        exigirRol(actor, "ADMIN");
        exigirEditable(bloquearTorneo(torneo));
        exigirEquipo(torneo, equipo);
        try {
            datos.actualizarEquipo(torneo, equipo, nombreValido(nombre));
        } catch (DuplicateKeyException duplicado) {
            throw conflicto("Ya existe un equipo con ese nombre en el torneo");
        }
    }

    @Transactional
    public long crearPartido(String actor, long torneo, long local, long visitante, Instant inicio, boolean esFinal) {
        exigirRol(actor, "ADMIN");
        var copa = bloquearTorneo(torneo);
        exigirEditable(copa);
        if (local == visitante) {
            throw new ProdeException(HttpStatus.BAD_REQUEST, "El local y el visitante deben ser equipos distintos");
        }
        exigirEquipo(torneo, local);
        exigirEquipo(torneo, visitante);
        validarHorario(copa, inicio);
        return datos.crearPartido(torneo, local, visitante, inicio, esFinal);
    }

    @Transactional
    public void reprogramarPartido(String actor, long torneo, long partido, Instant nuevoInicio) {
        exigirRol(actor, "ADMIN");
        var copa = bloquearTorneo(torneo);
        exigirEditable(copa);
        var actual = partido(torneo, partido);
        if (actual.estado() != EstadoPartido.PROXIMO && actual.estado() != EstadoPartido.SUSPENDIDO) {
            throw conflicto("Solo se pueden reprogramar partidos próximos o suspendidos");
        }
        validarHorario(copa, nuevoInicio);
        Instant cierre = actual.cierre().isBefore(nuevoInicio) ? actual.cierre() : nuevoInicio;
        datos.reprogramar(actual, nuevoInicio, cierre);
    }

    @Transactional
    public void iniciarPartido(String actor, long torneo, long partido) {
        exigirRol(actor, "ADMIN");
        exigirHabilitado(bloquearTorneo(torneo));
        var actual = partido(torneo, partido);
        if (actual.estado() != EstadoPartido.PROXIMO || reloj.instant().isBefore(actual.inicio())) {
            throw conflicto("Solo se puede iniciar un partido próximo al alcanzar su horario programado");
        }
        datos.estadoPartido(partido, EstadoPartido.EN_JUEGO);
    }

    @Transactional
    public void suspenderPartido(String actor, long torneo, long partido) {
        exigirRol(actor, "ADMIN");
        exigirEditable(bloquearTorneo(torneo));
        var actual = partido(torneo, partido);
        if (actual.estado() != EstadoPartido.PROXIMO && actual.estado() != EstadoPartido.EN_JUEGO) {
            throw conflicto("Solo se pueden suspender partidos próximos o en juego");
        }
        datos.estadoPartido(partido, EstadoPartido.SUSPENDIDO);
    }

    @Transactional
    public void publicarResultado(String actor, long torneo, long partido, Marcador marcador) {
        exigirRol(actor, "ADMIN");
        bloquearTorneo(torneo);
        var actual = partido(torneo, partido);
        validarMarcador(marcador);
        boolean correccion = actual.estado() == EstadoPartido.FINALIZADO;
        if (!correccion && actual.estado() != EstadoPartido.EN_JUEGO) {
            throw conflicto("El partido debe estar en juego para publicar el resultado");
        }
        if (!marcador.equals(actual.resultado())) {
            new PublicarResultado(torneo, partido, marcador, correccion).ejecutar(datos, eventos);
        }
    }

    @Transactional
    public void inscribir(String actor, long torneo) {
        exigirRol(actor, "USER");
        var copa = bloquearTorneo(torneo);
        exigirHabilitado(copa);
        if (LocalDate.ofInstant(reloj.instant(), ZONA).isAfter(copa.fin())) {
            throw conflicto("Ya pasó la fecha de finalización del torneo");
        }
        if (!datos.inscrito(torneo, actor)) {
            datos.inscribir(torneo, actor);
        }
    }

    @Transactional
    public void guardarPrediccion(String actor, long torneo, long partido, Marcador marcador, boolean editar) {
        exigirRol(actor, "USER");
        var copa = bloquearTorneo(torneo);
        var encuentro = partido(torneo, partido);
        if (!datos.inscrito(torneo, actor)) {
            throw new ProdeException(HttpStatus.FORBIDDEN, "Inscribite en el torneo para cargar pronósticos");
        }
        exigirHabilitado(copa);
        validarMarcador(marcador);
        var anterior = datos.prediccion(partido, actor);
        var ahora = reloj.instant();
        if (encuentro.estado() != EstadoPartido.PROXIMO || !ahora.isBefore(encuentro.cierre())) {
            throw conflicto("Los pronósticos de este partido están cerrados");
        }
        if (!editar && anterior.isPresent()) {
            throw conflicto("Ya tenés un pronóstico. Usá la opción Editar");
        }
        if (editar) {
            var previa = anterior.orElseThrow(() -> conflicto("Primero cargá un pronóstico"));
            Instant actualizada = ahora.isBefore(previa.actualizada()) ? previa.actualizada() : ahora;
            datos.editarPrediccion(partido, actor, marcador, actualizada);
        } else {
            datos.altaPrediccion(torneo, partido, actor, marcador, ahora);
        }
    }

    // Los cambios de calendario y los pronósticos toman el mismo bloqueo hasta el commit.
    private Torneo bloquearTorneo(long id) {
        return datos.bloquearTorneo(id).orElseThrow(() -> noExiste("El torneo"));
    }

    private void exigirRol(String actor, String esperado) {
        if (actor == null || datos.rol(actor).filter(esperado::equals).isEmpty()) {
            throw new ProdeException(HttpStatus.FORBIDDEN, "No tenés permiso para realizar esa operación");
        }
    }

    private void exigirEquipo(long torneo, long equipo) {
        if (!datos.existeEquipo(torneo, equipo)) {
            throw noExiste("El equipo en este torneo");
        }
    }

    private void validarHorario(Torneo torneo, Instant inicio) {
        if (inicio == null || !inicio.isAfter(reloj.instant()) || !fechaDentro(torneo, inicio)) {
            throw new ProdeException(HttpStatus.BAD_REQUEST,
                    "Programá el partido a futuro, dentro de las fechas del torneo");
        }
    }

    private static boolean fechaDentro(Torneo torneo, Instant inicio) {
        var fecha = LocalDate.ofInstant(inicio, ZONA);
        return !fecha.isBefore(torneo.inicio()) && !fecha.isAfter(torneo.fin());
    }

    private static Torneo datosTorneo(long id, String nombre, String descripcion, LocalDate inicio,
                                      LocalDate fin, EstadoTorneo estado, Tipo regla) {
        String texto = descripcion == null ? "" : descripcion.strip();
        if (inicio == null || fin == null || fin.isBefore(inicio) || estado == null || regla == null) {
            throw new ProdeException(HttpStatus.BAD_REQUEST, "Revisá las fechas, el estado y la regla de puntuación");
        }
        if (texto.length() > 500) {
            throw new ProdeException(HttpStatus.BAD_REQUEST, "La descripción admite hasta 500 caracteres");
        }
        return new Torneo(id, nombreValido(nombre), texto, inicio, fin, estado, regla);
    }

    private static String nombreValido(String nombre) {
        String limpio = nombre == null ? "" : nombre.strip();
        if (limpio.isEmpty() || limpio.length() > 100) {
            throw new ProdeException(HttpStatus.BAD_REQUEST, "Ingresá un nombre de 1 a 100 caracteres");
        }
        return limpio;
    }

    private static boolean transicionValida(EstadoTorneo anterior, EstadoTorneo siguiente) {
        return anterior == siguiente || switch (anterior) {
            case BORRADOR -> siguiente == EstadoTorneo.PROXIMO || siguiente == EstadoTorneo.ACTIVO;
            case PROXIMO -> siguiente == EstadoTorneo.ACTIVO;
            case ACTIVO -> siguiente == EstadoTorneo.FINALIZADO;
            case FINALIZADO -> false;
        };
    }

    private static void exigirEditable(Torneo torneo) {
        if (torneo.estado() == EstadoTorneo.FINALIZADO) {
            throw conflicto("El torneo ya finalizó");
        }
    }

    private static void exigirHabilitado(Torneo torneo) {
        if (torneo.estado() != EstadoTorneo.PROXIMO && torneo.estado() != EstadoTorneo.ACTIVO) {
            throw conflicto("El torneo debe estar próximo o activo");
        }
    }

    private static void validarMarcador(Marcador marcador) {
        if (marcador == null || marcador.local() > 99 || marcador.visitante() > 99) {
            throw new ProdeException(HttpStatus.BAD_REQUEST, "Ingresá goles enteros entre 0 y 99");
        }
    }

    private static ProdeException noExiste(String entidad) {
        return new ProdeException(HttpStatus.NOT_FOUND, entidad + " no existe");
    }

    private static ProdeException conflicto(String mensaje) {
        return new ProdeException(HttpStatus.CONFLICT, mensaje);
    }
}
