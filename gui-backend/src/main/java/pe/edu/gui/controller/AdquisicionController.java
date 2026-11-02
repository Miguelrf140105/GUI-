package pe.edu.gui.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.security.Access;
import pe.edu.gui.service.AdquisicionService;

import java.util.List;
import java.util.Map;

/**
 * Endpoints REST para proveedores, instituciones, comprobantes e ingresos. La identidad del
 * ejecutor se obtiene de la sesión.
 */
@RestController
@RequestMapping("/api")
public class AdquisicionController {

    private final Access access;

    private final AdquisicionService service;

    public AdquisicionController(Access access, AdquisicionService service) {
        this.access = access;
        this.service = service;
    }

    @PostMapping("/intakes/{id}/delete")
    @PreAuthorize("@access.has(authentication,1403) and @access.has(authentication,1401)")
    public ResultadoRegistro anularIngreso(Authentication auth, @PathVariable int id) {
        return service.anularIngreso(access.user(auth), id);
    }

    @PostMapping("/institutions/{id}/delete")
    @PreAuthorize("@access.has(authentication,1303)")
    public ResultadoRegistro eliminarInstitucion(Authentication auth, @PathVariable int id) {
        return service.eliminarInstitucion(access.user(auth), id);
    }

    /**
     * Registra la contraparte que podrá actuar como proveedor, donante o institución destinataria.
     */
    @PostMapping("/institutions")
    @PreAuthorize("@access.has(authentication,1302)")
    public ResultadoRegistro guardarInstitucion(
            Authentication auth, @Valid @RequestBody Requests.Institution dto) {
        return service.guardarInstitucion(access.user(auth), dto);
    }

    /**
     * Registra la cabecera del comprobante. Su vínculo con los productos se establece en el
     * ingreso.
     */
    @PostMapping("/documents")
    @PreAuthorize("@access.has(authentication,702)")
    public ResultadoRegistro guardarDocumento(
            Authentication auth, @Valid @RequestBody Requests.Document dto) {
        return service.guardarDocumento(access.user(auth), dto);
    }

    /** Consulta productos y comprobantes del ingreso dentro del alcance del usuario. */
    @GetMapping("/intakes/{id}/details")
    @PreAuthorize("@access.has(authentication,1401)")
    public Map<String, List<Map<String, Object>>> detalleIngreso(
            Authentication auth, @PathVariable int id) {
        return service.detalleIngreso(access.user(auth), id);
    }

    /** Busca la contraparte registrada; no crea ni modifica sus datos. */
    @GetMapping("/institutions/search")
    @PreAuthorize("@access.has(authentication,1301) or @access.has(authentication,1402)")
    public Map<String, Object> buscarInstitucion(
            Authentication auth,
            @org.springframework.web.bind.annotation.RequestParam String document) {
        return service.buscarInstitucion(access.user(auth), document);
    }
}
