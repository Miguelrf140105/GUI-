package pe.edu.gui.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Verifica el tamaño y la firma del adjunto y limpia el nombre usado en la descarga. */
public final class FilePolicy {

    private FilePolicy() {}

    /**
     * Acepta PDF, PNG o JPEG de hasta 10 MB según su firma binaria. No sustituye un análisis
     * antimalware.
     */
    public static String detect(byte[] b) {
        if (b.length == 0 || b.length > 10485760) {
            throw new IllegalArgumentException("Archivo vacío o mayor de 10 MB");
        }
        if (b.length >= 5 && new String(b, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
            return "application/pdf";
        }
        if (b.length >= 8
                && Arrays.equals(
                        Arrays.copyOf(b, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10})) {
            return "image/png";
        }
        if (b.length >= 3 && b[0] == (byte) 255 && b[1] == (byte) 216 && b[2] == (byte) 255) {
            return "image/jpeg";
        }
        throw new IllegalArgumentException(
                "Solo se admiten archivos PDF, PNG o JPEG con firma válida");
    }

    /** Elimina separadores de ruta y caracteres de control del nombre que se usará al descargar. */
    public static String safeName(String name) {
        if (name == null) {
            return "archivo";
        }
        String result = name.replaceAll("[\\\\/\\r\\n\\x00-\\x1F]", "_");
        return result.substring(0, Math.min(result.length(), 200));
    }
}
