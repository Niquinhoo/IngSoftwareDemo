package ar.edu.ingsoftware.prode.puntaje.laboratorio;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import ar.edu.ingsoftware.prode.puntaje.Puntuacion;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Entrada;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Marcador;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Puesto;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Tipo;

public final class LaboratorioPatrones {
    private LaboratorioPatrones() {}

    // Singleton: el enum conserva una única configuración inmutable para el laboratorio.
    private enum Configuracion {
        INSTANCIA;

        private final Tipo regla = Tipo.FINAL_DOBLE;
    }

    // Interpreter: los términos y la conjunción se evalúan sobre una misma entrada.
    interface Expresion {
        boolean cumple(Entrada entrada);
    }

    private record Y(Expresion izquierda, Expresion derecha) implements Expresion {
        @Override
        public boolean cumple(Entrada entrada) {
            return izquierda.cumple(entrada) && derecha.cumple(entrada);
        }
    }

    static Expresion interpretar(String consulta) {
        String[] terminos = consulta.strip().toLowerCase(Locale.ROOT).split("\\s+y\\s+", -1);
        Expresion expresion = interpretarTermino(terminos[0]);
        for (int i = 1; i < terminos.length; i++) {
            expresion = new Y(expresion, interpretarTermino(terminos[i]));
        }
        return expresion;
    }

    private static Expresion interpretarTermino(String termino) {
        return switch (termino) {
            case "exacto" -> entrada -> entrada.pronostico().equals(entrada.resultado());
            case "diferencia" -> entrada -> entrada.pronostico().diferencia() == entrada.resultado().diferencia();
            case "final" -> Entrada::finalPartido;
            default -> throw new IllegalArgumentException("Término desconocido: " + termino);
        };
    }

    // Bridge: el alcance del reporte y la forma de presentarlo tienen variantes independientes.
    interface FormatoRanking {
        String presentar(List<Puesto> puestos);
    }

    static final class FormatoTexto implements FormatoRanking {
        @Override
        public String presentar(List<Puesto> puestos) {
            return puestos.stream()
                    .map(puesto -> puesto.participante() + " | puntos " + puesto.puntos() + " | exactos " + puesto.exactos())
                    .collect(Collectors.joining("\n"));
        }
    }

    static final class FormatoCsv implements FormatoRanking {
        @Override
        public String presentar(List<Puesto> puestos) {
            var filas = new StringJoiner("\n").add("participante,puntos,exactos");
            for (Puesto puesto : puestos) {
                String nombre = puesto.participante().replace("\"", "\"\"");
                filas.add("\"" + nombre + "\"," + puesto.puntos() + "," + puesto.exactos());
            }
            return filas.toString();
        }
    }

    abstract static class ReporteRanking {
        private final FormatoRanking formato;

        ReporteRanking(FormatoRanking formato) {
            this.formato = formato;
        }

        // Template Method: fija el orden de cálculo, selección y presentación.
        final String generar(Tipo tipo, Collection<String> participantes, List<Entrada> entradas) {
            List<Puesto> ranking = Puntuacion.ranking(tipo, participantes, entradas);
            return formato.presentar(seleccionar(ranking));
        }

        abstract List<Puesto> seleccionar(List<Puesto> ranking);
    }

    static final class ReporteCompleto extends ReporteRanking {
        ReporteCompleto(FormatoRanking formato) {
            super(formato);
        }

        @Override
        List<Puesto> seleccionar(List<Puesto> ranking) {
            return ranking;
        }
    }

    static final class ReportePodio extends ReporteRanking {
        ReportePodio(FormatoRanking formato) {
            super(formato);
        }

        @Override
        List<Puesto> seleccionar(List<Puesto> ranking) {
            return ranking.stream().limit(3).toList();
        }
    }

    // Visitor: el tipo de partido selecciona la operación correspondiente en el visitante.
    interface VisitantePartido<T> {
        T visitar(PartidoPendiente partido);

        T visitar(PartidoPublicado partido);
    }

    interface Partido {
        <T> T aceptar(VisitantePartido<T> visitante);
    }

    record Pronostico(String participante, Marcador marcador) {}

    record PartidoPendiente(List<Pronostico> pronosticos) implements Partido {
        @Override
        public <T> T aceptar(VisitantePartido<T> visitante) {
            return visitante.visitar(this);
        }
    }

    record PartidoPublicado(List<Pronostico> pronosticos, Marcador resultado, boolean finalPartido) implements Partido {
        @Override
        public <T> T aceptar(VisitantePartido<T> visitante) {
            return visitante.visitar(this);
        }
    }

    static final class EstadoPublicacion implements VisitantePartido<String> {
        @Override
        public String visitar(PartidoPendiente partido) {
            return "pendiente (" + partido.pronosticos().size() + ")";
        }

        @Override
        public String visitar(PartidoPublicado partido) {
            Marcador resultado = partido.resultado();
            return "publicado " + resultado.local() + "-" + resultado.visitante() + " (" + partido.pronosticos().size() + ")";
        }
    }

    static final class EntradasOficiales implements VisitantePartido<List<Entrada>> {
        @Override
        public List<Entrada> visitar(PartidoPendiente partido) {
            return List.of();
        }

        @Override
        public List<Entrada> visitar(PartidoPublicado partido) {
            return partido.pronosticos().stream()
                    .map(pronostico -> new Entrada(pronostico.participante(), pronostico.marcador(),
                            partido.resultado(), partido.finalPartido()))
                    .toList();
        }
    }

    // Mediator: la mesa y la pantalla solo se comunican con el coordinador.
    record MesaResultados(MediadorResultados mediador) {
        void publicar(List<Entrada> entradas) {
            mediador.publicar(entradas);
        }
    }

    static final class PantallaRanking {
        private final MediadorResultados mediador;
        private String texto;

        PantallaRanking(MediadorResultados mediador) {
            this.mediador = mediador;
        }

        void seleccionar(ReporteRanking reporte) {
            mediador.seleccionar(reporte);
        }

        String texto() {
            return texto;
        }
    }

    static final class MediadorResultados {
        private final MesaResultados mesa = new MesaResultados(this);
        private final PantallaRanking pantalla = new PantallaRanking(this);
        private final Tipo tipo;
        private final List<String> participantes;
        private ReporteRanking reporte;
        private List<Entrada> entradas;

        MediadorResultados(Tipo tipo, List<String> participantes, ReporteRanking reporte) {
            this.tipo = tipo;
            this.participantes = List.copyOf(participantes);
            actualizar(reporte, List.of());
        }

        MesaResultados mesa() {
            return mesa;
        }

        PantallaRanking pantalla() {
            return pantalla;
        }

        private void publicar(List<Entrada> nuevasEntradas) {
            actualizar(reporte, List.copyOf(nuevasEntradas));
        }

        private void seleccionar(ReporteRanking nuevoReporte) {
            actualizar(nuevoReporte, entradas);
        }

        private void actualizar(ReporteRanking reporte, List<Entrada> entradas) {
            pantalla.texto = reporte.generar(tipo, participantes, entradas);
            this.reporte = reporte;
            this.entradas = entradas;
        }
    }

    public static void main(String[] args) {
        List<String> participantes = List.of("Ana", "Eva", "Luis", "Mora");
        List<Partido> partidos = List.of(
                new PartidoPublicado(List.of(
                        new Pronostico("Ana", new Marcador(2, 1)),
                        new Pronostico("Luis", new Marcador(1, 0)),
                        new Pronostico("Eva", new Marcador(3, 0))), new Marcador(2, 1), true),
                new PartidoPendiente(List.of(new Pronostico("Mora", new Marcador(2, 0)))));

        var estados = new EstadoPublicacion();
        var oficiales = new EntradasOficiales();
        System.out.println("Visitor · estado de cada partido y cantidad de pronósticos");
        partidos.forEach(partido -> System.out.println(partido.aceptar(estados)));
        List<Entrada> entradas = partidos.stream().flatMap(partido -> partido.aceptar(oficiales).stream()).toList();

        String consulta = "diferencia y final";
        Expresion expresion = interpretar(consulta);
        System.out.println("\nInterpreter · " + consulta);
        System.out.println(entradas.stream().filter(expresion::cumple).map(Entrada::participante).toList());

        Tipo regla = Configuracion.INSTANCIA.regla;
        System.out.println("\nSingleton · regla del laboratorio " + regla);
        var mediador = new MediadorResultados(regla, participantes, new ReporteCompleto(new FormatoTexto()));
        mediador.mesa().publicar(entradas);
        System.out.println("\nMediator · la mesa actualiza el ranking");
        System.out.println(mediador.pantalla().texto());

        mediador.pantalla().seleccionar(new ReportePodio(new FormatoCsv()));
        System.out.println("\nBridge y Template Method · podio en CSV");
        System.out.println(mediador.pantalla().texto());
    }
}
