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
import pe.edu.gui.service.UsuarioService;

import java.util.Map;

/**
 * Endpoints REST para cuentas, roles y alcance de acceso. La identidad del ejecutor se obtiene de
 * la sesión.
 */
@RestController
@RequestMapping("/api/users")
public class UsuarioController {

    private final Access access;

    private final UsuarioService service;

    public UsuarioController(Access access, UsuarioService service) {
        this.access = access;
        this.service = service;
    }

    /**
     * Guarda la cuenta y sus asignaciones; al editar, una contraseña vacía conserva la existente.
     */
    @PostMapping
    @PreAuthorize("@access.has(authentication,902)")
    public ResultadoRegistro guardar(Authentication auth, @Valid @RequestBody Requests.User dto) {
        return service.guardar(access.user(auth), dto);
    }

    /** Consulta los roles y las raíces de ubicación asignadas a la cuenta. */
    @GetMapping("/{id}/assignments")
    @PreAuthorize("@access.has(authentication,901)")
    public Map<String, Object> asignaciones(Authentication auth, @PathVariable int id) {
        return service.asignaciones(access.user(auth), id);
    }
}
