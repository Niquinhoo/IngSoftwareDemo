package ar.edu.ingsoftware.prode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class RegistroResultados {
    private static final Logger LOG = LoggerFactory.getLogger(RegistroResultados.class);

    @TransactionalEventListener
    void registrar(ResultadoPublicado evento) {
        LOG.info("Resultado confirmado: torneo={}, partido={}, marcador={}-{}, corrección={}",
                evento.torneoId(), evento.partidoId(), evento.marcador().local(), evento.marcador().visitante(),
                evento.correccion());
    }
}
