package ar.edu.ingsoftware.prode.web;

import ar.edu.ingsoftware.prode.ProdeException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class CuentaController {
    private final Cuentas cuentas;

    CuentaController(Cuentas cuentas) {
        this.cuentas = cuentas;
    }

    @GetMapping("/login")
    String login(Authentication autenticacion) {
        return autenticacion == null ? "login" : "redirect:/";
    }

    @GetMapping("/registro")
    String registro(Authentication autenticacion) {
        return autenticacion == null ? "registro" : "redirect:/";
    }

    @PostMapping("/registro")
    String registrar(Authentication autenticacion, @RequestParam String email, @RequestParam String password,
            Model modelo, HttpServletResponse respuesta, RedirectAttributes redireccion) {
        if (autenticacion != null) {
            return "redirect:/";
        }
        try {
            cuentas.registrar(email, password);
        } catch (ProdeException e) {
            modelo.addAttribute("errorFormulario", e.getMessage());
            modelo.addAttribute("email", email);
            respuesta.setStatus(e.status().value());
            return "registro";
        }
        redireccion.addFlashAttribute("aviso", "Cuenta creada. Ya podés iniciar sesión.");
        return "redirect:/login";
    }
}
