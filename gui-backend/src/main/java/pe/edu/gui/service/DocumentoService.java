package pe.edu.gui.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.gui.entity.ArchivoAdjunto;
import pe.edu.gui.interfaces.IDocumentoDAO;
import pe.edu.gui.utils.FilePolicy;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Casos de uso para archivos adjuntos y sus relaciones con documentos o activos. Coordina
 * validaciones y acceso a persistencia.
 */
@Service
public class DocumentoService {

    private final IDocumentoDAO dao;

    public DocumentoService(IDocumentoDAO dao) {
        this.dao = dao;
    }

    /** Lista adjuntos de un documento o de un activo, según el padre indicado. */
    public List<Map<String, Object>> listar(int actor, Integer document, Integer asset) {
        validarPadre(document, asset);
        return dao.listar(actor, document, asset);
    }

    /**
     * Valida el archivo y calcula su hash antes de guardar contenido y metadatos en la misma
     * operación.
     */
    @Transactional
    public Map<String, Object> subir(
            int actor, Integer document, Integer asset, String name, byte[] content) {
        validarPadre(document, asset);
        String mime = FilePolicy.detect(content);
        UUID id = UUID.randomUUID();
        try {
            String hash =
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
            dao.guardar(actor, id, document, asset, FilePolicy.safeName(name), mime, hash, content);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
        return Map.of("id", id);
    }

    /** Lee un adjunto aplicando permisos y alcance; su UUID no concede acceso por sí solo. */
    public ArchivoAdjunto descargar(int actor, UUID id) {
        return dao.descargar(actor, id);
    }

    /**
     * Exige un único padre: documento o activo. Evita adjuntos sin relación o con una relación
     * ambigua.
     */
    private void validarPadre(Integer document, Integer asset) {
        if ((document == null) == (asset == null)
                || (document != null && document < 1)
                || (asset != null && asset < 1)) {
            throw new IllegalArgumentException("Indique un documento o un activo");
        }
    }
}
