package pe.edu.gui.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.gui.security.Access;
import pe.edu.gui.service.DocumentoService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints REST para archivos adjuntos y sus relaciones con documentos o activos. La identidad del
 * ejecutor se obtiene de la sesión.
 */
@RestController
@RequestMapping("/api/attachments")
public class DocumentoController {

    private final Access access;

    private final DocumentoService service;

    public DocumentoController(Access access, DocumentoService service) {
        this.access = access;
        this.service = service;
    }

    /** Lista adjuntos de un documento o de un activo, según el padre indicado. */
    @GetMapping
    public List<Map<String, Object>> listar(
            Authentication auth,
            @RequestParam(required = false) Integer documentId,
            @RequestParam(required = false) Integer assetId) {
        return service.listar(access.user(auth), documentId, assetId);
    }

    /**
     * Valida el archivo y calcula su hash antes de guardar contenido y metadatos en la misma
     * operación.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@access.has(authentication,702)")
    public Map<String, Object> subir(
            Authentication auth,
            @RequestParam(required = false) Integer documentId,
            @RequestParam(required = false) Integer assetId,
            @RequestParam MultipartFile file)
            throws java.io.IOException {
        return service.subir(
                access.user(auth),
                documentId,
                assetId,
                file.getOriginalFilename(),
                file.getBytes());
    }

    /** Lee un adjunto aplicando permisos y alcance; su UUID no concede acceso por sí solo. */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> descargar(Authentication auth, @PathVariable UUID id) {
        var file = service.descargar(access.user(auth), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mime()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.name(), java.nio.charset.StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.content());
    }
}
