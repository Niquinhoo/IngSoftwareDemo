package ar.edu.ingsoftware.prode;

import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Marcador;

public record ResultadoPublicado(long torneoId, long partidoId, Marcador marcador, boolean correccion) {}
