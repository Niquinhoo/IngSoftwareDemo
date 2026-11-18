package ar.edu.ingsoftware.prode.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SeguridadWeb {
    @Bean
    SecurityFilterChain filtros(HttpSecurity http) {
        return http.authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/login", "/registro", "/estilos.css", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/torneos/**").hasRole("USER")
                        .anyRequest().authenticated())
                .formLogin(formulario -> formulario
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/"))
                .logout(salida -> salida.logoutSuccessUrl("/login?salida"))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder() {
            @Override
            protected boolean matchesNonNull(String password, String hash) {
                return Cuentas.passwordValida(password) && super.matchesNonNull(password, hash);
            }
        };
    }

    @Bean
    ApplicationRunner crearAdministrador(Cuentas cuentas,
            @Value("${prode.admin.email:}") String email,
            @Value("${prode.admin.password:}") String password) {
        return args -> cuentas.prepararAdministrador(email, password);
    }
}
