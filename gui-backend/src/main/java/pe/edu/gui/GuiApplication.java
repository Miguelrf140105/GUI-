package pe.edu.gui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada de la API de inventario universitario. */
@SpringBootApplication
public class GuiApplication {

    /** Inicia Spring Boot y registra los componentes del backend. */
    public static void main(String[] args) {
        SpringApplication.run(GuiApplication.class, args);
    }
}
