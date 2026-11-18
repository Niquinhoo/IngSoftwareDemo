package ar.edu.ingsoftware.prode;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProdeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProdeApplication.class, args);
    }

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
