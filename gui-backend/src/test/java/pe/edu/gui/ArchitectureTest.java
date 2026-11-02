package pe.edu.gui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/** Evita volver a introducir acceso SQL o DAOs en los controladores. */
class ArchitectureTest {

    @Test
    void controllersOnlyDelegateToServices() throws Exception {
        try (var files = Files.list(Path.of("src/main/java/pe/edu/gui/controller"))) {
            for (var p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(p);
                for (String forbidden :
                        new String[] {
                            "JdbcTemplate",
                            "ISqlExecutor",
                            "pe.edu.gui.dao.",
                            "pe.edu.gui.interfaces.",
                            "EXEC ",
                            "@Transactional",
                            "ObjectMapper"
                        }) {
                    assertFalse(
                            source.contains(forbidden),
                            p.getFileName() + " depende de persistencia: " + forbidden);
                }
            }
        }
    }
}
