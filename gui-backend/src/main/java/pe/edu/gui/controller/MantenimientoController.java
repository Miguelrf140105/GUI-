package pe.edu.gui.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.security.Access;
import pe.edu.gui.service.MantenimientoService;

/**
 * Endpoints REST para programación de mantenimiento de activos. La identidad del ejecutor se
 * obtiene de la sesión.
 */
@RestController
@RequestMapping("/api")
public class MantenimientoController {

    private final Access access;

    private final MantenimientoService service;

    public MantenimientoController(Access access, MantenimientoService service) {
        this.access = access;
        this.service = service;
    }

    /**
     * Programa la atención del activo; esta operación todavía no representa el cierre del
     * mantenimiento.
     */
    @PostMapping("/maintenance")
    @PreAuthorize("@access.has(authentication,602)")
    public ResultadoRegistro programar(
            Authentication auth, @Valid @RequestBody Requests.Maintenance dto) {
        return service.programar(access.user(auth), dto);
    }
}
