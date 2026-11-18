package ar.edu.ingsoftware.prode.web;

import ar.edu.ingsoftware.prode.ProdeException;
import ar.edu.ingsoftware.prode.ProdeService;
import ar.edu.ingsoftware.prode.ProdeService.Equipo;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

@Component("vistas")
public class VistasProde {
    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d MMM uuuu", ARGENTINA);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", ARGENTINA)
            .withZone(ProdeService.ZONA);
    private static final DateTimeFormatter HORA_FORM = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm")
            .withZone(ProdeService.ZONA);

    public String fecha(LocalDate fecha) {
        return FECHA.format(fecha);
    }

    public String horario(Instant instante) {
        return HORA.format(instante);
    }

    public String horaFormulario(Instant instante) {
        return HORA_FORM.format(instante);
    }

    public String estado(Enum<?> estado) {
        return switch (estado.name()) {
            case "BORRADOR" -> "Borrador";
            case "PROXIMO" -> "Próximo";
            case "ACTIVO" -> "Activo";
            case "FINALIZADO" -> "Finalizado";
            case "SUSPENDIDO" -> "Suspendido";
            case "EN_JUEGO" -> "En juego";
            default -> estado.name();
        };
    }

    public String regla(Puntuacion.Tipo regla) {
        return switch (regla) {
            case CLASICA -> "Clásica";
            case ALTERNATIVA -> "Alternativa";
            case FINAL_DOBLE -> "Alternativa con doble final";
        };
    }

    static Map<Long, String> nombres(List<Equipo> equipos) {
        return equipos.stream().collect(Collectors.toMap(Equipo::id, Equipo::nombre));
    }

    static boolean rol(Authentication autenticacion, String rol) {
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> rol.equals(autoridad.getAuthority()));
    }
}

@ControllerAdvice
class ModelosWeb {
    @ModelAttribute("sesionIniciada")
    boolean sesionIniciada(Authentication autenticacion) {
        return autenticacion != null;
    }

    @ModelAttribute("administrador")
    boolean administrador(Authentication autenticacion) {
        return VistasProde.rol(autenticacion, "ROLE_ADMIN");
    }

    @ExceptionHandler(ProdeException.class)
    ModelAndView errorProde(ProdeException error) {
        ModelAndView vista = new ModelAndView("problema");
        vista.setStatus(error.status());
        vista.addObject("mensaje", error.getMessage());
        return vista;
    }
}
