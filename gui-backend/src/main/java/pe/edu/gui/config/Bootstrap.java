package pe.edu.gui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.gui.service.UsuarioService;

/** Crea el administrador inicial cuando se proporcionan sus credenciales por entorno. */
@Configuration
public class Bootstrap {

    /**
     * Solicita el alta inicial solo si se proporcionó alguna variable de bootstrap. No hay una
     * contraseña de fábrica.
     */
    @Bean
    ApplicationRunner initialize(
            UsuarioService service,
            @Value("${gui.bootstrap.username}") String username,
            @Value("${gui.bootstrap.password}") String password) {
        return args -> {
            if (!username.isBlank() || !password.isBlank()) {
                service.bootstrap(username, password);
            }
        };
    }
}
