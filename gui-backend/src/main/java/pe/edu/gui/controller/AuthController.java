package pe.edu.gui.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.gui.security.Access;
import pe.edu.gui.service.SeguridadService;

import java.util.Map;

/** Entrega el token CSRF y el perfil de la sesión. Spring Security procesa el login y el logout. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final Access access;

    private final SeguridadService service;

    public AuthController(Access access, SeguridadService service) {
        this.access = access;
        this.service = service;
    }

    /**
     * Entrega el token y el nombre de cabecera que la web debe enviar en sus peticiones de
     * escritura.
     */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    /** Devuelve el perfil del usuario de la sesión actual. */
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return service.perfil(access.user(auth));
    }
}
