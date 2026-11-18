package ar.edu.ingsoftware.prode.puntaje;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Puntuacion {
    private Puntuacion() {}

    public enum Tipo {
        CLASICA, ALTERNATIVA, FINAL_DOBLE
    }

    public record Marcador(int local, int visitante) {
        public Marcador {
            if (local < 0 || visitante < 0) {
                throw new IllegalArgumentException("Los goles no pueden ser negativos.");
            }
        }

        public int diferencia() {
            return local - visitante;
        }
    }

    public record Entrada(String participante, Marcador pronostico, Marcador resultado, boolean finalPartido) {
        public Entrada {
            Objects.requireNonNull(participante);
            Objects.requireNonNull(pronostico);
            Objects.requireNonNull(resultado);
        }
    }

    public record Puesto(String participante, int puntos, int exactos) {}

    @FunctionalInterface
    public interface ReglaPuntaje {
        int puntos(Entrada entrada);
    }

    private static final ReglaPuntaje CLASICA = entrada -> {
        if (entrada.pronostico().equals(entrada.resultado())) {
            return 3;
        }
        int pronostico = Integer.signum(entrada.pronostico().diferencia());
        int resultado = Integer.signum(entrada.resultado().diferencia());
        return pronostico == resultado ? 1 : 0;
    };

    private static final ReglaPuntaje ALTERNATIVA = entrada -> {
        if (entrada.pronostico().equals(entrada.resultado())) {
            return 5;
        }
        return entrada.pronostico().diferencia() == entrada.resultado().diferencia() ? 2 : 0;
    };

    private record DobleEnFinal(ReglaPuntaje base) implements ReglaPuntaje {
        @Override
        public int puntos(Entrada entrada) {
            int puntos = base.puntos(entrada);
            return entrada.finalPartido() ? 2 * puntos : puntos;
        }
    }

    private static final ReglaPuntaje FINAL_DOBLE = new DobleEnFinal(ALTERNATIVA);

    public static List<Puesto> ranking(Tipo tipo, Collection<String> participantes, List<Entrada> entradas) {
        ReglaPuntaje regla = switch (tipo) {
            case CLASICA -> CLASICA;
            case ALTERNATIVA -> ALTERNATIVA;
            case FINAL_DOBLE -> FINAL_DOBLE;
        };
        return ranking(regla, participantes, entradas);
    }

    public static List<Puesto> ranking(ReglaPuntaje regla, Collection<String> participantes, List<Entrada> entradas) {
        Objects.requireNonNull(regla);
        Map<String, Puesto> totales = new HashMap<>();
        for (String participante : participantes) {
            totales.put(Objects.requireNonNull(participante), new Puesto(participante, 0, 0));
        }
        for (Entrada entrada : entradas) {
            Puesto anterior = totales.get(entrada.participante());
            if (anterior == null) {
                throw new IllegalArgumentException("Participante fuera del torneo: " + entrada.participante());
            }
            int puntos = Math.addExact(anterior.puntos(), regla.puntos(entrada));
            int exactos = anterior.exactos() + (entrada.pronostico().equals(entrada.resultado()) ? 1 : 0);
            totales.put(entrada.participante(), new Puesto(entrada.participante(), puntos, exactos));
        }
        return totales.values().stream()
                .sorted(Comparator.comparingInt(Puesto::puntos).reversed()
                        .thenComparing(Comparator.comparingInt(Puesto::exactos).reversed())
                        .thenComparing(Puesto::participante))
                .toList();
    }
}
