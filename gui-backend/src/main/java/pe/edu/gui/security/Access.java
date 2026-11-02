package pe.edu.gui.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import pe.edu.gui.service.SeguridadService;

/** Obtiene el usuario de la sesión y consulta sus permisos para las anotaciones de seguridad. */
@Component("access")
public class Access {

    private final SeguridadService seguridad;

    public Access(SeguridadService seguridad) {
        this.seguridad = seguridad;
    }

    /** Extrae el ID de la identidad autenticada; rechaza peticiones sin una sesión válida. */
    public int user(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof GuiPrincipal p)) {
            throw new AccessDeniedException("Sesión requerida");
        }
        return p.id();
    }

    /** Consulta el permiso vigente al evaluar una acción protegida con PreAuthorize. */
    public boolean has(Authentication auth, int permission) {
        return seguridad.tienePermiso(user(auth), permission);
    }
}
