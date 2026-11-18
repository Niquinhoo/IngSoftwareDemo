package ar.edu.ingsoftware.prode.puntaje;

import static ar.edu.ingsoftware.prode.puntaje.Puntuacion.Tipo.ALTERNATIVA;
import static ar.edu.ingsoftware.prode.puntaje.Puntuacion.Tipo.CLASICA;
import static ar.edu.ingsoftware.prode.puntaje.Puntuacion.Tipo.FINAL_DOBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Entrada;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Marcador;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Puesto;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Tipo;

class PuntuacionTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("casos")
    void aplicaLaReglaAlPronostico(String caso, Tipo tipo, int pronosticoLocal, int pronosticoVisitante,
            int resultadoLocal, int resultadoVisitante, boolean finalPartido, int puntos, int exactos) {
        Entrada entrada = entrada("Ana", pronosticoLocal, pronosticoVisitante, resultadoLocal,
                resultadoVisitante, finalPartido);

        assertEquals(List.of(new Puesto("Ana", puntos, exactos)),
                Puntuacion.ranking(tipo, List.of("Ana"), List.of(entrada)));
    }

    static Stream<Arguments> casos() {
        return Stream.of(
                Arguments.of("01 clásica · victoria local exacta", CLASICA, 2, 1, 2, 1, false, 3, 1),
                Arguments.of("02 clásica · victoria visitante exacta", CLASICA, 1, 2, 1, 2, false, 3, 1),
                Arguments.of("03 clásica · empate sin goles exacto", CLASICA, 0, 0, 0, 0, false, 3, 1),
                Arguments.of("04 clásica · acierta ganador local", CLASICA, 4, 0, 2, 1, false, 1, 0),
                Arguments.of("05 clásica · acierta ganador visitante", CLASICA, 0, 3, 1, 2, false, 1, 0),
                Arguments.of("06 clásica · acierta empate con otros goles", CLASICA, 2, 2, 1, 1, false, 1, 0),
                Arguments.of("07 clásica · pronostica al ganador opuesto", CLASICA, 1, 0, 0, 1, false, 0, 0),
                Arguments.of("08 clásica · pronostica empate y hay un ganador", CLASICA, 0, 0, 1, 0, false, 0, 0),
                Arguments.of("09 alternativa · victoria local exacta", ALTERNATIVA, 3, 1, 3, 1, false, 5, 1),
                Arguments.of("10 alternativa · victoria visitante exacta", ALTERNATIVA, 1, 3, 1, 3, false, 5, 1),
                Arguments.of("11 alternativa · empate exacto", ALTERNATIVA, 1, 1, 1, 1, false, 5, 1),
                Arguments.of("12 alternativa · diferencia positiva igual", ALTERNATIVA, 1, 0, 2, 1, false, 2, 0),
                Arguments.of("13 alternativa · diferencia negativa igual", ALTERNATIVA, 0, 1, 1, 2, false, 2, 0),
                Arguments.of("14 alternativa · diferencia cero igual", ALTERNATIVA, 1, 1, 0, 0, false, 2, 0),
                Arguments.of("15 alternativa · mismo ganador con otra diferencia", ALTERNATIVA, 3, 0, 2, 1, false, 0, 0),
                Arguments.of("16 alternativa · misma magnitud con signo opuesto", ALTERNATIVA, 0, 2, 3, 1, false, 0, 0),
                Arguments.of("17 final doble · duplica marcador exacto", FINAL_DOBLE, 2, 1, 2, 1, true, 10, 1),
                Arguments.of("18 final doble · duplica diferencia acertada", FINAL_DOBLE, 1, 0, 2, 1, true, 4, 0),
                Arguments.of("19 final doble · conserva puntos en un partido común", FINAL_DOBLE, 2, 1, 2, 1, false, 5, 1),
                Arguments.of("20 final doble · no suma por pronóstico incorrecto", FINAL_DOBLE, 0, 2, 2, 0, true, 0, 0));
    }

    @Test
    void acumulaPuntosOrdenaDesempatesYConservaParticipantesSinPronosticos() {
        List<Entrada> entradas = List.of(
                entrada("Zoe", 2, 1, 2, 1, false),
                entrada("Betty", 3, 0, 2, 1, false),
                entrada("Betty", 1, 0, 2, 1, false),
                entrada("Betty", 4, 1, 2, 1, false),
                entrada("Ana", 1, 0, 1, 0, false),
                entrada("Ana", 1, 0, 2, 1, false),
                entrada("Carla", 1, 0, 1, 0, false));

        assertEquals(List.of(
                new Puesto("Ana", 4, 1),
                new Puesto("Carla", 3, 1),
                new Puesto("Zoe", 3, 1),
                new Puesto("Betty", 3, 0),
                new Puesto("Darío", 0, 0)),
                Puntuacion.ranking(CLASICA, List.of("Darío", "Zoe", "Betty", "Carla", "Ana"), entradas));
    }

    @Test
    void distingueUnaFinalDeUnPartidoComunEnElMismoRanking() {
        List<Entrada> entradas = List.of(
                entrada("Ana", 2, 1, 2, 1, false),
                entrada("Ana", 1, 0, 2, 1, true));

        assertEquals(List.of(new Puesto("Ana", 9, 1)),
                Puntuacion.ranking(FINAL_DOBLE, List.of("Ana"), entradas));
    }

    @Test
    void aceptaUnaEstrategiaNuevaSinCambiarElRanking() {
        List<Entrada> entradas = List.of(
                entrada("Ana", 0, 1, 2, 1, false),
                entrada("Luis", 2, 1, 2, 1, false));
        Puntuacion.ReglaPuntaje regla = entrada -> entrada.pronostico().local() == 0 ? 7 : 0;

        assertEquals(List.of(new Puesto("Ana", 7, 0), new Puesto("Luis", 0, 1)),
                Puntuacion.ranking(regla, List.of("Luis", "Ana"), entradas));
    }

    @Test
    void rechazaGolesNegativosDeAmbosEquipos() {
        assertThrows(IllegalArgumentException.class, () -> new Marcador(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Marcador(1, -1));
    }

    @Test
    void rechazaEntradasDeParticipantesAjenosAlTorneo() {
        Entrada ajena = entrada("Luis", 0, 1, 2, 1, false);

        assertThrows(IllegalArgumentException.class,
                () -> Puntuacion.ranking(CLASICA, List.of("Ana"), List.of(ajena)));
    }

    private static Entrada entrada(String participante, int pronosticoLocal, int pronosticoVisitante,
            int resultadoLocal, int resultadoVisitante, boolean finalPartido) {
        return new Entrada(participante, new Marcador(pronosticoLocal, pronosticoVisitante),
                new Marcador(resultadoLocal, resultadoVisitante), finalPartido);
    }
}
