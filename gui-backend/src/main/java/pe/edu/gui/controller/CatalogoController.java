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
import pe.edu.gui.service.CatalogoService;

/**
 * Endpoints REST para parámetros que alimentan los catálogos del sistema. La identidad del ejecutor
 * se obtiene de la sesión.
 */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final Access access;

    private final CatalogoService service;

    public CatalogoController(Access access, CatalogoService service) {
        this.access = access;
        this.service = service;
    }

    /** Registra un valor de catálogo dentro de su grupo, identificado por ID. */
    @PostMapping("/parameters")
    @PreAuthorize("@access.has(authentication,1102)")
    public ResultadoRegistro guardar(
            Authentication auth, @Valid @RequestBody Requests.Parameter dto) {
        return service.guardar(access.user(auth), dto);
    }

    /** Crea un tipo de producto desde Configuración. */
    @PostMapping("/families")
    @PreAuthorize("@access.has(authentication,1102)")
    public ResultadoRegistro guardarFamilia(
            Authentication auth, @Valid @RequestBody Requests.Family dto) {
        return service.guardarFamilia(access.user(auth), dto);
    }
}
