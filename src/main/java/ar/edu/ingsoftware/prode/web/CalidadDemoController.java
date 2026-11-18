package ar.edu.ingsoftware.prode.web;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Controller
@Profile("demo")
class CalidadDemoController implements WebMvcConfigurer {
    @GetMapping("/calidad")
    String calidad() {
        return "calidad";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler("/calidad/jacoco/**").addResourceLocations("classpath:/informes/jacoco/");
    }
}
