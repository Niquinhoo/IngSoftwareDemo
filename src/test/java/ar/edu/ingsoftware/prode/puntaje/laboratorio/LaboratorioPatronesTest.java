package ar.edu.ingsoftware.prode.puntaje.laboratorio;

import static ar.edu.ingsoftware.prode.puntaje.Puntuacion.Tipo.ALTERNATIVA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Entrada;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Marcador;
import ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones.EntradasOficiales;
import ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones.EstadoPublicacion;
import ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones.FormatoCsv;
import ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones.FormatoTexto;
import ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones.MediadorResultados;
import ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones.PartidoPendiente;
import ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones.PartidoPublicado;
import ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones.Pronostico;
import ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones.ReporteCompleto;
import ar.edu.ingsoftware.prode.puntaje.laboratorio.LaboratorioPatrones.ReportePodio;

class LaboratorioPatronesTest {
    private static final Marcador RESULTADO = new Marcador(2, 1);

    @Test
    void interpretaPredicadosYComponeLaConjuncion() {
        var exacto = LaboratorioPatrones.interpretar("exacto");
        var diferenciaEnFinal = LaboratorioPatrones.interpretar(" DIFERENCIA y final ");

        assertTrue(exacto.cumple(new Entrada("Ana", RESULTADO, RESULTADO, false)));
        assertFalse(exacto.cumple(new Entrada("Luis", new Marcador(1, 0), RESULTADO, true)));
        assertTrue(diferenciaEnFinal.cumple(new Entrada("Luis", new Marcador(1, 0), RESULTADO, true)));
        assertFalse(diferenciaEnFinal.cumple(new Entrada("Luis", new Marcador(1, 0), RESULTADO, false)));
        assertFalse(diferenciaEnFinal.cumple(new Entrada("Luis", new Marcador(2, 0), RESULTADO, true)));
    }

    @Test
    void rechazaConsultasIncompletasYTerminosDesconocidos() {
        assertThrows(IllegalArgumentException.class, () -> LaboratorioPatrones.interpretar("exacto y"));
        assertThrows(IllegalArgumentException.class, () -> LaboratorioPatrones.interpretar("final y penales"));
    }

    @Test
    void cambiaElFormatoYElAlcanceDelReporteDeFormaIndependiente() {
        String eva = "Eva, \"E\"";
        var participantes = List.of("Ana", "Luis", eva, "Mora");
        var entradas = List.of(new Entrada("Ana", RESULTADO, RESULTADO, false),
                new Entrada("Luis", new Marcador(1, 0), RESULTADO, false));

        String textoCompleto = "Ana | puntos 5 | exactos 1\nLuis | puntos 2 | exactos 0\n"
                + "Eva, \"E\" | puntos 0 | exactos 0\nMora | puntos 0 | exactos 0";
        String csvCompleto = "participante,puntos,exactos\n"
                + "\"Ana\",5,1\n\"Luis\",2,0\n\"Eva, \"\"E\"\"\",0,0\n\"Mora\",0,0";
        String textoPodio = "Ana | puntos 5 | exactos 1\nLuis | puntos 2 | exactos 0\n"
                + "Eva, \"E\" | puntos 0 | exactos 0";
        String csvPodio = "participante,puntos,exactos\n\"Ana\",5,1\n\"Luis\",2,0\n\"Eva, \"\"E\"\"\",0,0";

        assertEquals(textoCompleto, new ReporteCompleto(new FormatoTexto()).generar(ALTERNATIVA, participantes, entradas));
        assertEquals(csvCompleto, new ReporteCompleto(new FormatoCsv()).generar(ALTERNATIVA, participantes, entradas));
        assertEquals(textoPodio, new ReportePodio(new FormatoTexto()).generar(ALTERNATIVA, participantes, entradas));
        assertEquals(csvPodio, new ReportePodio(new FormatoCsv()).generar(ALTERNATIVA, participantes, entradas));
    }

    @Test
    void visitaPartidosParaInspeccionarlosYExtraerEntradasOficiales() {
        Marcador pronosticado = new Marcador(1, 0);
        var pronosticos = List.of(new Pronostico("Ana", pronosticado));
        var pendiente = new PartidoPendiente(pronosticos);
        var publicado = new PartidoPublicado(pronosticos, RESULTADO, true);
        var estados = new EstadoPublicacion();
        var oficiales = new EntradasOficiales();

        assertEquals("pendiente (1)", pendiente.aceptar(estados));
        assertEquals("publicado 2-1 (1)", publicado.aceptar(estados));
        assertEquals(List.of(), pendiente.aceptar(oficiales));
        assertEquals(List.of(new Entrada("Ana", pronosticado, RESULTADO, true)), publicado.aceptar(oficiales));
    }

    @Test
    void coordinaLaPublicacionDeResultadosYLosCambiosDeReporte() {
        var mediador = new MediadorResultados(ALTERNATIVA, List.of("Ana"), new ReporteCompleto(new FormatoTexto()));
        var mesa = mediador.mesa();
        var pantalla = mediador.pantalla();
        assertEquals("Ana | puntos 0 | exactos 0", pantalla.texto());

        mesa.publicar(List.of(new Entrada("Ana", RESULTADO, RESULTADO, false)));
        assertEquals("Ana | puntos 5 | exactos 1", pantalla.texto());

        pantalla.seleccionar(new ReportePodio(new FormatoCsv()));
        assertEquals("participante,puntos,exactos\n\"Ana\",5,1", pantalla.texto());

        mesa.publicar(List.of());
        assertEquals("participante,puntos,exactos\n\"Ana\",0,0", pantalla.texto());
    }
}
