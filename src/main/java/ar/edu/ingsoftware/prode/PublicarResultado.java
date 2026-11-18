package ar.edu.ingsoftware.prode;

import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Marcador;
import org.springframework.context.ApplicationEventPublisher;

record PublicarResultado(long torneo, long partido, Marcador marcador, boolean correccion) {
    void ejecutar(ProdeDatos datos, ApplicationEventPublisher eventos) {
        datos.resultado(partido, marcador);
        eventos.publishEvent(new ResultadoPublicado(torneo, partido, marcador, correccion));
    }
}
