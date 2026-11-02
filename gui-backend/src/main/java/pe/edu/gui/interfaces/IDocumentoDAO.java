package pe.edu.gui.interfaces;

import pe.edu.gui.entity.ArchivoAdjunto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Contrato de persistencia para archivos adjuntos y sus relaciones con documentos o activos. */
public interface IDocumentoDAO {

    /** Lista adjuntos de un documento o de un activo, según el padre indicado. */
    List<Map<String, Object>> listar(int actor, Integer document, Integer asset);

    /**
     * Guarda contenido en GUI_BF y metadatos en GUI_BP a través del SP y la transacción de la
     * conexión actual.
     */
    void guardar(
            int actor,
            UUID id,
            Integer document,
            Integer asset,
            String name,
            String mime,
            String hash,
            byte[] bytes);

    /** Lee un adjunto aplicando permisos y alcance; su UUID no concede acceso por sí solo. */
    ArchivoAdjunto descargar(int actor, UUID id);
}
