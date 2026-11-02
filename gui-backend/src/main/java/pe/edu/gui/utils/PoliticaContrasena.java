package pe.edu.gui.utils;

import java.nio.charset.StandardCharsets;

/** Límites de contraseña compatibles con el hash BCrypt utilizado por la aplicación. */
public final class PoliticaContrasena {

    private PoliticaContrasena() {}

    /**
     * Exige 14 caracteres y limita el tamaño UTF-8 a 72 bytes para evitar el truncamiento de
     * BCrypt.
     */
    public static void validar(String password) {
        if (password == null
                || password.length() < 14
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Contraseña: mínimo 14 caracteres y máximo 72 bytes UTF-8");
        }
    }
}
