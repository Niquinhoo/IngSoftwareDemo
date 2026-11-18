package ar.edu.ingsoftware.prode.web;

import ar.edu.ingsoftware.prode.ProdeException;
import ar.edu.ingsoftware.prode.ProdeService;
import ar.edu.ingsoftware.prode.ProdeService.EstadoPartido;
import ar.edu.ingsoftware.prode.ProdeService.EstadoTorneo;
import ar.edu.ingsoftware.prode.ProdeService.Partido;
import ar.edu.ingsoftware.prode.ProdeService.Torneo;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Marcador;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class TorneosController {
    private final ProdeService prode;
    private final Clock reloj;

    TorneosController(ProdeService prode, Clock reloj) {
        this.prode = prode;
        this.reloj = reloj;
    }

    @GetMapping("/")
    String inicio(Model modelo) {
        modelo.addAttribute("torneos", prode.torneos());
        return "torneos";
    }

    @GetMapping("/torneos/{torneo}")
    String torneo(@PathVariable long torneo, Authentication autenticacion, Model modelo) {
        Torneo detalle = prode.torneo(torneo);
        boolean participante = VistasProde.rol(autenticacion, "ROLE_USER");
        modelo.addAttribute("torneo", detalle);
        modelo.addAttribute("partidos", prode.partidos(torneo));
        modelo.addAttribute("nombres", VistasProde.nombres(prode.equipos(torneo)));
        modelo.addAttribute("inscrito", participante && prode.inscrito(autenticacion.getName(), torneo));
        modelo.addAttribute("inscripcionAbierta", participante && abierto(detalle)
                && !LocalDate.ofInstant(reloj.instant(), ProdeService.ZONA).isAfter(detalle.fin()));
        return "torneo";
    }

    @PostMapping("/torneos/{torneo}/inscripcion")
    String inscribir(@PathVariable long torneo, Authentication autenticacion, RedirectAttributes redireccion) {
        prode.inscribir(autenticacion.getName(), torneo);
        redireccion.addFlashAttribute("aviso", "Te inscribiste en el torneo.");
        return "redirect:/torneos/" + torneo;
    }

    @GetMapping("/torneos/{torneo}/partidos/{partido}")
    String partido(@PathVariable long torneo, @PathVariable long partido, Authentication autenticacion, Model modelo) {
        Torneo competencia = prode.torneo(torneo);
        Partido detalle = prode.partido(torneo, partido);
        boolean participante = VistasProde.rol(autenticacion, "ROLE_USER");
        boolean inscrito = participante && prode.inscrito(autenticacion.getName(), torneo);
        modelo.addAttribute("torneo", competencia);
        modelo.addAttribute("partido", detalle);
        modelo.addAttribute("nombres", VistasProde.nombres(prode.equipos(torneo)));
        modelo.addAttribute("participante", participante);
        modelo.addAttribute("inscrito", inscrito);
        modelo.addAttribute("pronosticoAbierto", abierto(competencia) && detalle.estado() == EstadoPartido.PROXIMO
                && reloj.instant().isBefore(detalle.cierre()));
        if (inscrito) {
            prode.prediccion(autenticacion.getName(), torneo, partido)
                    .ifPresent(prediccion -> modelo.addAttribute("prediccion", prediccion));
        }
        return "partido";
    }

    @PostMapping("/torneos/{torneo}/partidos/{partido}/prediccion")
    String pronosticar(@PathVariable long torneo, @PathVariable long partido,
            @RequestParam int golesLocal, @RequestParam int golesVisitante, Authentication autenticacion,
            RedirectAttributes redireccion) {
        prode.guardarPrediccion(autenticacion.getName(), torneo, partido, marcador(golesLocal, golesVisitante), false);
        redireccion.addFlashAttribute("aviso", "Pronóstico guardado.");
        return "redirect:/torneos/" + torneo + "/partidos/" + partido;
    }

    @PostMapping("/torneos/{torneo}/partidos/{partido}/prediccion/editar")
    String editarPronostico(@PathVariable long torneo, @PathVariable long partido,
            @RequestParam int golesLocal, @RequestParam int golesVisitante, Authentication autenticacion,
            RedirectAttributes redireccion) {
        prode.guardarPrediccion(autenticacion.getName(), torneo, partido, marcador(golesLocal, golesVisitante), true);
        redireccion.addFlashAttribute("aviso", "Pronóstico actualizado.");
        return "redirect:/torneos/" + torneo + "/partidos/" + partido;
    }

    @GetMapping("/torneos/{torneo}/ranking")
    String ranking(@PathVariable long torneo, Model modelo) {
        modelo.addAttribute("torneo", prode.torneo(torneo));
        modelo.addAttribute("ranking", prode.ranking(torneo));
        return "ranking";
    }

    private static boolean abierto(Torneo torneo) {
        return torneo.estado() == EstadoTorneo.PROXIMO || torneo.estado() == EstadoTorneo.ACTIVO;
    }

    static Marcador marcador(int local, int visitante) {
        if (local < 0 || local > 99 || visitante < 0 || visitante > 99) {
            throw new ProdeException(HttpStatus.BAD_REQUEST, "Ingresá entre 0 y 99 goles para cada equipo.");
        }
        return new Marcador(local, visitante);
    }
}
