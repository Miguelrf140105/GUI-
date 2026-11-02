package pe.edu.gui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

class SqlServer2014ContractTest {

    @Test
    void installationDoesNotRequireNewerSqlServerFunctions() throws Exception {
        var unsupported =
                Pattern.compile(
                        "\\b(OPENJSON|JSON_VALUE|ISJSON|STRING_AGG)\\s*\\(|CREATE\\s+OR\\s+ALTER|DROP\\s+\\w+\\s+IF\\s+EXISTS",
                        Pattern.CASE_INSENSITIVE);
        try (var files = Files.list(Path.of("database"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".sql")).toList()) {
                assertFalse(
                        unsupported.matcher(Files.readString(file)).find(),
                        "Función incompatible en " + file);
            }
        }
    }
}
