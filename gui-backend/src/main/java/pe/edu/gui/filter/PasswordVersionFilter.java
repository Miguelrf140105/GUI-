package pe.edu.gui.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.gui.security.GuiPrincipal;
import pe.edu.gui.service.SeguridadService;

/** Un cambio de contraseña invalida las sesiones anteriores sin depender del navegador. */
public class PasswordVersionFilter extends OncePerRequestFilter {
  private final SeguridadService security;

  public PasswordVersionFilter(SeguridadService security) {
    this.security = security;
  }

  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof GuiPrincipal principal) {
      int version;
      try {
        version = security.passwordVersion(principal.id());
      } catch (org.springframework.dao.DataAccessException e) {
        res.sendError(503);
        return;
      }
      if (version != principal.passwordVersion()) {
        var session = req.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        res.sendError(401, "La sesión terminó. Inicia sesión nuevamente.");
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
