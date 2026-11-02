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
import pe.edu.gui.service.UbicacionService;

/**
 * Endpoints REST para sedes y ambientes organizados en un árbol de ubicaciones. La identidad del
 * ejecutor se obtiene de la sesión.
 */
@RestController
@RequestMapping("/api")
public class UbicacionController {

    private final Access access;

    private final UbicacionService service;

    public UbicacionController(Access access, UbicacionService service) {
        this.access = access;
        this.service = service;
    }

    @PostMapping("/locations/{id}/delete")
    @PreAuthorize("@access.has(authentication,503)")
    public ResultadoRegistro eliminar(Authentication auth,
            @org.springframework.web.bind.annotation.PathVariable int id) {
        return service.eliminar(access.user(auth), id);
    }

    /**
     * Crea o modifica una ubicación del árbol. SQL valida ciclos y serializa la reestructuración.
     */
    @PostMapping("/locations")
    @PreAuthorize("@access.has(authentication,502)")
    public ResultadoRegistro guardar(
            Authentication auth, @Valid @RequestBody Requests.Location dto) {
        return service.guardar(access.user(auth), dto);
    }
}
