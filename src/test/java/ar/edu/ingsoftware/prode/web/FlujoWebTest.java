package ar.edu.ingsoftware.prode.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.ingsoftware.prode.ProdeService;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:flujo-web;DB_CLOSE_DELAY=-1",
    "prode.admin.email=admin@web.test",
    "prode.admin.password=clave-admin-test"
})
@AutoConfigureMockMvc
class FlujoWebTest {
    private static final String PARTICIPANTE = "ana@web.test";
    private static final String CLAVE = "clave-participante";
    private static final Instant INICIO = Instant.parse("2030-03-01T16:00:00Z");

    @Autowired private MockMvc mvc;
    @Autowired private ProdeService prode;
    @MockitoBean private Clock reloj;

    @Test
    void registroAplicaCsrfYValidaLaLongitudUtf8() throws Exception {
        mvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(post("/registro").param("email", "nueva@web.test").param("password", CLAVE))
                .andExpect(status().isForbidden());
        mvc.perform(post("/registro").with(csrf()).param("email", "nueva@web.test")
                        .param("password", "á".repeat(37)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("72 bytes")));
        mvc.perform(post("/login").with(csrf()).param("email", "nueva@web.test").param("password", CLAVE))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void participanteYAdministradorRecorrenElFlujoCompleto() throws Exception {
        when(reloj.instant()).thenReturn(INICIO.minusSeconds(3600));
        mvc.perform(get("/")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(post("/registro").with(csrf()).param("email", PARTICIPANTE.toUpperCase(java.util.Locale.ROOT))
                        .param("password", CLAVE))
                .andExpect(redirectedUrl("/login"));
        MockHttpSession ana = login(PARTICIPANTE, CLAVE, "USER");
        MockHttpSession admin = login("admin@web.test", "clave-admin-test", "ADMIN");

        mvc.perform(get("/admin/torneos/nuevo").session(ana)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/torneos/nuevo").session(admin)).andExpect(status().isOk());
        mvc.perform(post("/admin/torneos").session(admin).with(csrf())
                        .param("nombre", "Copa Web")
                        .param("descripcion", "Torneo de prueba")
                        .param("inicio", "2030-03-01")
                        .param("fin", "2030-03-03")
                        .param("estado", "ACTIVO")
                        .param("regla", "ALTERNATIVA"))
                .andExpect(status().is3xxRedirection());
        long torneo = prode.torneos().getFirst().id();
        String administrar = "/admin/torneos/" + torneo;
        String ver = "/torneos/" + torneo;
        mvc.perform(get(administrar).session(admin)).andExpect(status().isOk());
        mvc.perform(get(administrar + "/editar").session(admin)).andExpect(status().isOk());
        for (String equipo : new String[] {"Atlético Sur", "Unión Norte"}) {
            mvc.perform(post(administrar + "/equipos").session(admin).with(csrf()).param("nombre", equipo))
                    .andExpect(redirectedUrl(administrar + "#equipos"));
        }
        var equipos = prode.equipos(torneo);
        mvc.perform(get(administrar).session(admin)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Unión Norte")));
        mvc.perform(post(administrar + "/partidos").session(admin).with(csrf())
                        .param("local", Long.toString(equipos.get(0).id()))
                        .param("visitante", Long.toString(equipos.get(1).id()))
                        .param("inicio", "2030-03-01T13:00"))
                .andExpect(status().is3xxRedirection());
        var partido = prode.partidos(torneo).getFirst();
        assertThat(partido.inicio()).isEqualTo(INICIO);
        String partidoWeb = ver + "/partidos/" + partido.id();
        String partidoAdmin = administrar + "/partidos/" + partido.id();
        String prediccion = partidoWeb + "/prediccion";
        mvc.perform(get(partidoAdmin).session(admin)).andExpect(status().isOk());

        mvc.perform(get("/").session(ana)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Copa Web")));
        mvc.perform(get(ver).session(ana)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Inscribirme")));
        mvc.perform(post(ver + "/inscripcion").session(admin).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post(ver + "/inscripcion").session(ana).with(csrf()))
                .andExpect(redirectedUrl(ver));
        mvc.perform(get(partidoWeb).session(ana)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Guardar pronóstico")));
        mvc.perform(marcador(prediccion, ana, 1, 0)).andExpect(redirectedUrl(partidoWeb));
        mvc.perform(get(partidoWeb).session(ana)).andExpect(status().isOk())
                .andExpect(content().string(containsString("1 – 0")))
                .andExpect(content().string(containsString("Actualizar pronóstico")));
        mvc.perform(marcador(prediccion + "/editar", ana, 2, 1)).andExpect(redirectedUrl(partidoWeb));

        when(reloj.instant()).thenReturn(INICIO);
        mvc.perform(get(partidoWeb).session(ana)).andExpect(status().isOk())
                .andExpect(content().string(containsString("2 – 1")))
                .andExpect(content().string(not(containsString("Actualizar pronóstico"))));
        mvc.perform(marcador(prediccion + "/editar", ana, 5, 4)).andExpect(status().isConflict())
                .andExpect(content().string(containsString("están cerrados")));

        mvc.perform(post(partidoAdmin + "/iniciar").session(admin).with(csrf()))
                .andExpect(redirectedUrl(partidoAdmin));
        mvc.perform(get(partidoAdmin).session(admin)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Publicar resultado")));
        mvc.perform(marcador(partidoAdmin + "/resultado", admin, 2, 1))
                .andExpect(redirectedUrl(partidoAdmin));
        mvc.perform(get(ver + "/ranking").session(ana)).andExpect(status().isOk())
                .andExpect(content().string(containsString("<td>5</td>")))
                .andExpect(content().string(not(containsString(PARTICIPANTE))));
        mvc.perform(get(partidoAdmin).session(admin)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Corregir resultado")));
        mvc.perform(marcador(partidoAdmin + "/resultado", admin, 0, 0))
                .andExpect(redirectedUrl(partidoAdmin));
        mvc.perform(get(ver + "/ranking").session(ana)).andExpect(status().isOk())
                .andExpect(content().string(containsString("<td>0</td>")))
                .andExpect(content().string(not(containsString("<td>5</td>"))));
        mvc.perform(post("/logout").session(ana).with(csrf())).andExpect(redirectedUrl("/login?salida"));
    }

    private MockHttpSession login(String email, String clave, String rol) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("email", email).param("password", clave))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withRoles(rol))
                .andReturn().getRequest().getSession(false);
    }

    private static MockHttpServletRequestBuilder marcador(String ruta, MockHttpSession sesion, int local, int visitante) {
        return post(ruta).session(sesion).with(csrf())
                .param("golesLocal", Integer.toString(local)).param("golesVisitante", Integer.toString(visitante));
    }
}
