package ar.edu.ingsoftware.prode.web;

import ar.edu.ingsoftware.prode.ProdeService;
import ar.edu.ingsoftware.prode.ProdeService.EstadoPartido;
import ar.edu.ingsoftware.prode.ProdeService.EstadoTorneo;
import ar.edu.ingsoftware.prode.ProdeService.Partido;
import ar.edu.ingsoftware.prode.ProdeService.Torneo;
import ar.edu.ingsoftware.prode.puntaje.Puntuacion.Tipo;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/torneos")
class AdminController {
    private final ProdeService prode;
    private final Clock reloj;

    AdminController(ProdeService prode, Clock reloj) {
        this.prode = prode;
        this.reloj = reloj;
    }

    @GetMapping("/nuevo")
    String nuevoTorneo(Model modelo) {
        return formularioTorneo(modelo, List.of(EstadoTorneo.BORRADOR, EstadoTorneo.PROXIMO, EstadoTorneo.ACTIVO));
    }

    @GetMapping("/{torneo}/editar")
    String editarTorneo(@PathVariable long torneo, Model modelo) {
        Torneo actual = prode.torneo(torneo);
        modelo.addAttribute("torneo", actual);
        return formularioTorneo(modelo, switch (actual.estado()) {
            case BORRADOR -> List.of(EstadoTorneo.BORRADOR, EstadoTorneo.PROXIMO, EstadoTorneo.ACTIVO);
            case PROXIMO -> List.of(EstadoTorneo.PROXIMO, EstadoTorneo.ACTIVO);
            case ACTIVO -> List.of(EstadoTorneo.ACTIVO, EstadoTorneo.FINALIZADO);
            case FINALIZADO -> List.of(EstadoTorneo.FINALIZADO);
        });
    }

    @PostMapping
    String crearTorneo(@RequestParam String nombre, @RequestParam(defaultValue = "") String descripcion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam EstadoTorneo estado, @RequestParam Tipo regla, Authentication autenticacion,
            RedirectAttributes redireccion) {
        long torneo = prode.crearTorneo(autenticacion.getName(), nombre, descripcion, inicio, fin, estado, regla);
        redireccion.addFlashAttribute("aviso", "Torneo creado.");
        return gestionar(torneo);
    }

    @PostMapping("/{torneo}")
    String actualizarTorneo(@PathVariable long torneo, @RequestParam String nombre,
            @RequestParam(defaultValue = "") String descripcion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam EstadoTorneo estado, @RequestParam Tipo regla, Authentication autenticacion,
            RedirectAttributes redireccion) {
        prode.actualizarTorneo(autenticacion.getName(), torneo, nombre, descripcion, inicio, fin, estado, regla);
        redireccion.addFlashAttribute("aviso", "Torneo actualizado.");
        return gestionar(torneo);
    }

    @GetMapping("/{torneo}")
    String torneo(@PathVariable long torneo, Model modelo) {
        Torneo actual = prode.torneo(torneo);
        var equipos = prode.equipos(torneo);
        modelo.addAttribute("torneo", actual);
        modelo.addAttribute("editable", actual.estado() != EstadoTorneo.FINALIZADO);
        modelo.addAttribute("equipos", equipos);
        modelo.addAttribute("nombres", VistasProde.nombres(equipos));
        modelo.addAttribute("partidos", prode.partidos(torneo));
        return "admin-torneo";
    }

    @PostMapping("/{torneo}/equipos")
    String crearEquipo(@PathVariable long torneo, @RequestParam String nombre, Authentication autenticacion,
            RedirectAttributes redireccion) {
        prode.crearEquipo(autenticacion.getName(), torneo, nombre);
        redireccion.addFlashAttribute("aviso", "Equipo agregado.");
        return gestionar(torneo) + "#equipos";
    }

    @PostMapping("/{torneo}/equipos/{equipo}")
    String actualizarEquipo(@PathVariable long torneo, @PathVariable long equipo,
            @RequestParam String nombre, Authentication autenticacion, RedirectAttributes redireccion) {
        prode.actualizarEquipo(autenticacion.getName(), torneo, equipo, nombre);
        redireccion.addFlashAttribute("aviso", "Equipo actualizado.");
        return gestionar(torneo) + "#equipos";
    }

    @PostMapping("/{torneo}/partidos")
    String crearPartido(@PathVariable long torneo, @RequestParam long local, @RequestParam long visitante,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(defaultValue = "false") boolean esFinal, Authentication autenticacion,
            RedirectAttributes redireccion) {
        long partido = prode.crearPartido(autenticacion.getName(), torneo, local, visitante,
                inicio.atZone(ProdeService.ZONA).toInstant(), esFinal);
        redireccion.addFlashAttribute("aviso", "Partido programado.");
        return gestionar(torneo, partido);
    }

    @GetMapping("/{torneo}/partidos/{partido}")
    String partido(@PathVariable long torneo, @PathVariable long partido, Model modelo) {
        Torneo copa = prode.torneo(torneo);
        Partido detalle = prode.partido(torneo, partido);
        EstadoPartido estado = detalle.estado();
        boolean editable = copa.estado() != EstadoTorneo.FINALIZADO;
        boolean habilitado = copa.estado() == EstadoTorneo.PROXIMO || copa.estado() == EstadoTorneo.ACTIVO;
        boolean porComenzar = habilitado && estado == EstadoPartido.PROXIMO;
        modelo.addAttribute("torneo", copa);
        modelo.addAttribute("partido", detalle);
        modelo.addAttribute("nombres", VistasProde.nombres(prode.equipos(torneo)));
        modelo.addAttribute("porComenzar", porComenzar);
        modelo.addAttribute("puedeIniciar", porComenzar && !reloj.instant().isBefore(detalle.inicio()));
        modelo.addAttribute("puedeSuspender", editable && (estado == EstadoPartido.PROXIMO || estado == EstadoPartido.EN_JUEGO));
        modelo.addAttribute("puedeReprogramar", editable && (estado == EstadoPartido.PROXIMO || estado == EstadoPartido.SUSPENDIDO));
        modelo.addAttribute("puedePublicar", estado == EstadoPartido.EN_JUEGO || estado == EstadoPartido.FINALIZADO);
        return "admin-partido";
    }

    @PostMapping("/{torneo}/partidos/{partido}/reprogramar")
    String reprogramar(@PathVariable long torneo, @PathVariable long partido,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            Authentication autenticacion, RedirectAttributes redireccion) {
        prode.reprogramarPartido(autenticacion.getName(), torneo, partido, inicio.atZone(ProdeService.ZONA).toInstant());
        redireccion.addFlashAttribute("aviso", "Partido reprogramado.");
        return gestionar(torneo, partido);
    }

    @PostMapping("/{torneo}/partidos/{partido}/iniciar")
    String iniciar(@PathVariable long torneo, @PathVariable long partido, Authentication autenticacion,
            RedirectAttributes redireccion) {
        prode.iniciarPartido(autenticacion.getName(), torneo, partido);
        redireccion.addFlashAttribute("aviso", "Partido iniciado.");
        return gestionar(torneo, partido);
    }

    @PostMapping("/{torneo}/partidos/{partido}/suspender")
    String suspender(@PathVariable long torneo, @PathVariable long partido, Authentication autenticacion,
            RedirectAttributes redireccion) {
        prode.suspenderPartido(autenticacion.getName(), torneo, partido);
        redireccion.addFlashAttribute("aviso", "Partido suspendido.");
        return gestionar(torneo, partido);
    }

    @PostMapping("/{torneo}/partidos/{partido}/resultado")
    String resultado(@PathVariable long torneo, @PathVariable long partido, @RequestParam int golesLocal,
            @RequestParam int golesVisitante, Authentication autenticacion, RedirectAttributes redireccion) {
        prode.publicarResultado(autenticacion.getName(), torneo, partido,
                TorneosController.marcador(golesLocal, golesVisitante));
        redireccion.addFlashAttribute("aviso", "Resultado actualizado. Ya se refleja en la clasificación.");
        return gestionar(torneo, partido);
    }

    private static String formularioTorneo(Model modelo, List<EstadoTorneo> estados) {
        modelo.addAttribute("estados", estados);
        modelo.addAttribute("reglas", Tipo.values());
        return "admin-torneo-form";
    }

    private static String gestionar(long torneo) {
        return "redirect:/admin/torneos/" + torneo;
    }

    private static String gestionar(long torneo, long partido) {
        return gestionar(torneo) + "/partidos/" + partido;
    }
}
