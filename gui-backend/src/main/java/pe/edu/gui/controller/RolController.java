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
import pe.edu.gui.service.RolService;

import java.util.List;
import java.util.Map;

/**
 * Endpoints REST para roles y asignación de permisos. La identidad del ejecutor se obtiene de la
 * sesión.
 */
@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final Access access;

    private final RolService service;

    public RolController(Access access, RolService service) {
        this.access = access;
        this.service = service;
    }

    /**
     * Guarda los permisos del rol editable. El rol de recuperación se conserva para no perder el
     * acceso administrativo.
     */
    @PostMapping
    @PreAuthorize("@access.has(authentication,1002)")
    public ResultadoRegistro guardar(Authentication auth, @Valid @RequestBody Requests.Role dto) {
        return service.guardar(access.user(auth), dto);
    }

    /** Consulta los permisos asignados al rol solicitado. */
    @GetMapping("/{id}/permissions")
    public List<Map<String, Object>> permisos(Authentication auth, @PathVariable int id) {
        return service.permisos(access.user(auth), id);
    }
}
