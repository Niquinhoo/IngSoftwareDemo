package ar.edu.ingsoftware.prode;

import ar.edu.ingsoftware.prode.ProdeService.Equipo;
import ar.edu.ingsoftware.prode.ProdeService.EstadoPartido;
import ar.edu.ingsoftware.prode.ProdeService.EstadoTorneo;
import ar.edu.ingsoftware.prode.ProdeService.Partido;
import ar.edu.ingsoftware.prode.ProdeService.Prediccion;
import ar.edu.ingsoftware.prode.ProdeService.Torneo;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Entrada;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Marcador;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Tipo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
class ProdeDatos {
    private static final RowMapper<Torneo> TORNEO = (rs, row) -> new Torneo(
            rs.getLong("id"), rs.getString("nombre"), rs.getString("descripcion"),
            rs.getObject("inicio", LocalDate.class), rs.getObject("fin", LocalDate.class),
            EstadoTorneo.valueOf(rs.getString("estado")), Tipo.valueOf(rs.getString("regla")));

    private static final RowMapper<Equipo> EQUIPO = (rs, row) -> new Equipo(
            rs.getLong("id"), rs.getLong("torneo_id"), rs.getString("nombre"));

    private static final RowMapper<Partido> PARTIDO = (rs, row) -> {
        var estado = EstadoPartido.valueOf(rs.getString("estado"));
        Marcador resultado = estado == EstadoPartido.FINALIZADO
                ? new Marcador(rs.getInt("resultado_local"), rs.getInt("resultado_visitante")) : null;
        return new Partido(rs.getLong("id"), rs.getLong("torneo_id"), rs.getLong("local_id"),
                rs.getLong("visitante_id"), instante(rs, "inicio"), instante(rs, "cierre"),
                estado, rs.getBoolean("es_final"), resultado);
    };

    private final JdbcClient db;

    ProdeDatos(JdbcClient db) {
        this.db = db;
    }

    Optional<String> rol(String email) {
        return db.sql("SELECT rol FROM cuentas WHERE email = ?").param(email).query(String.class).optional();
    }

    List<Torneo> torneos() {
        return db.sql("SELECT * FROM torneos ORDER BY inicio, nombre, id").query(TORNEO).list();
    }

    Optional<Torneo> torneo(long id) {
        return db.sql("SELECT * FROM torneos WHERE id = ?").param(id).query(TORNEO).optional();
    }

    Optional<Torneo> bloquearTorneo(long id) {
        return db.sql("SELECT * FROM torneos WHERE id = ? FOR UPDATE").param(id).query(TORNEO).optional();
    }

    long crearTorneo(Torneo torneo) {
        return insertar(db.sql("""
                INSERT INTO torneos (nombre, descripcion, inicio, fin, estado, regla)
                VALUES (?, ?, ?, ?, ?, ?)""")
                .params(torneo.nombre(), torneo.descripcion(), torneo.inicio(), torneo.fin(),
                        torneo.estado().name(), torneo.regla().name()));
    }

    void actualizarTorneo(Torneo torneo) {
        db.sql("UPDATE torneos SET nombre=?, descripcion=?, inicio=?, fin=?, estado=?, regla=? WHERE id=?")
                .params(torneo.nombre(), torneo.descripcion(), torneo.inicio(), torneo.fin(),
                        torneo.estado().name(), torneo.regla().name(), torneo.id()).update();
    }

    List<Equipo> equipos(long torneoId) {
        return db.sql("SELECT * FROM equipos WHERE torneo_id = ? ORDER BY nombre")
                .param(torneoId).query(EQUIPO).list();
    }

    boolean existeEquipo(long torneoId, long equipoId) {
        return db.sql("SELECT id FROM equipos WHERE torneo_id = ? AND id = ?")
                .params(torneoId, equipoId).query(Long.class).optional().isPresent();
    }

    long crearEquipo(long torneo, String nombre) {
        return insertar(db.sql("INSERT INTO equipos (torneo_id, nombre) VALUES (?, ?)").params(torneo, nombre));
    }

    void actualizarEquipo(long torneo, long equipo, String nombre) {
        db.sql("UPDATE equipos SET nombre=? WHERE torneo_id=? AND id=?").params(nombre, torneo, equipo).update();
    }

    List<Partido> partidos(long torneoId) {
        return db.sql("SELECT * FROM partidos WHERE torneo_id = ? ORDER BY inicio, id")
                .param(torneoId).query(PARTIDO).list();
    }

    Optional<Partido> partido(long torneoId, long partidoId) {
        return db.sql("SELECT * FROM partidos WHERE torneo_id = ? AND id = ?")
                .params(torneoId, partidoId).query(PARTIDO).optional();
    }

    long crearPartido(long torneo, long local, long visitante, Instant inicio, boolean esFinal) {
        return insertar(db.sql("""
                INSERT INTO partidos (torneo_id, local_id, visitante_id, inicio, cierre, estado, es_final)
                VALUES (?, ?, ?, ?, ?, 'PROXIMO', ?)""")
                .params(torneo, local, visitante, utc(inicio), utc(inicio), esFinal));
    }

    void reprogramar(Partido partido, Instant inicio, Instant cierre) {
        db.sql("UPDATE partidos SET inicio=?, cierre=?, estado='PROXIMO' WHERE id=?")
                .params(utc(inicio), utc(cierre), partido.id()).update();
    }

    void estadoPartido(long id, EstadoPartido estado) {
        db.sql("UPDATE partidos SET estado=? WHERE id=?").params(estado.name(), id).update();
    }

    void resultado(long id, Marcador resultado) {
        db.sql("UPDATE partidos SET resultado_local=?, resultado_visitante=?, estado='FINALIZADO' WHERE id=?")
                .params(resultado.local(), resultado.visitante(), id).update();
    }

    boolean inscrito(long torneo, String email) {
        return db.sql("SELECT id FROM inscripciones WHERE torneo_id=? AND email=?")
                .params(torneo, email).query(Long.class).optional().isPresent();
    }

    void inscribir(long torneo, String email) {
        db.sql("INSERT INTO inscripciones (torneo_id, email) VALUES (?, ?)").params(torneo, email).update();
    }

    Optional<Prediccion> prediccion(long partido, String email) {
        return db.sql("SELECT * FROM predicciones WHERE partido_id=? AND email=?")
                .params(partido, email)
                .query((rs, row) -> new Prediccion(new Marcador(rs.getInt("goles_local"),
                        rs.getInt("goles_visitante")), instante(rs, "creada"), instante(rs, "actualizada")))
                .optional();
    }

    void altaPrediccion(long torneo, long partido, String email, Marcador marcador, Instant ahora) {
        db.sql("""
                INSERT INTO predicciones (torneo_id, partido_id, email, goles_local, goles_visitante, creada, actualizada)
                VALUES (?, ?, ?, ?, ?, ?, ?)""")
                .params(torneo, partido, email, marcador.local(), marcador.visitante(), utc(ahora), utc(ahora)).update();
    }

    void editarPrediccion(long partido, String email, Marcador marcador, Instant ahora) {
        db.sql("UPDATE predicciones SET goles_local=?, goles_visitante=?, actualizada=? WHERE partido_id=? AND email=?")
                .params(marcador.local(), marcador.visitante(), utc(ahora), partido, email).update();
    }

    List<String> participantes(long torneo) {
        return db.sql("SELECT id FROM inscripciones WHERE torneo_id=?").param(torneo)
                .query((rs, row) -> alias(rs.getLong("id"))).list();
    }

    List<Entrada> entradasRanking(long torneo) {
        return db.sql("""
                SELECT i.id AS inscripcion_id, p.goles_local, p.goles_visitante,
                    m.resultado_local, m.resultado_visitante, m.es_final
                FROM predicciones p
                JOIN partidos m ON m.id = p.partido_id
                JOIN inscripciones i ON i.torneo_id = p.torneo_id AND i.email = p.email
                WHERE p.torneo_id = ? AND m.estado = 'FINALIZADO'""")
                .param(torneo)
                .query((rs, row) -> new Entrada(alias(rs.getLong("inscripcion_id")),
                        new Marcador(rs.getInt("goles_local"), rs.getInt("goles_visitante")),
                        new Marcador(rs.getInt("resultado_local"), rs.getInt("resultado_visitante")),
                        rs.getBoolean("es_final"))).list();
    }

    private static long insertar(JdbcClient.StatementSpec sentencia) {
        var clave = new GeneratedKeyHolder();
        sentencia.update(clave, "id");
        Long id = clave.getKeyAs(Long.class);
        if (id == null) {
            throw new IllegalStateException("La base no devolvió el identificador generado");
        }
        return id;
    }

    private static Instant instante(ResultSet rs, String columna) throws SQLException {
        return rs.getObject(columna, OffsetDateTime.class).toInstant();
    }

    private static OffsetDateTime utc(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }

    private static String alias(long id) {
        return "Jugador " + id;
    }
}
