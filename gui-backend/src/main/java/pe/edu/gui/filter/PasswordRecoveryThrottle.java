package pe.edu.gui.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;

/** Ignora cabeceras de IP enviadas por clientes; limita cada dirección remota por instancia. */
public class PasswordRecoveryThrottle extends OncePerRequestFilter {
  private record Window(long start, int attempts) {}

  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    if (req.getMethod().equals("POST") && req.getServletPath().startsWith("/api/auth/recovery/")) {
      long now = System.currentTimeMillis();
      windows.entrySet().removeIf(e -> now - e.getValue().start() > 300000);
      if (windows.size() >= 10000) {
        res.sendError(429);
        return;
      }
      var key = req.getRemoteAddr() + "|" + req.getServletPath();
      var window =
          windows.compute(
              key,
              (k, v) -> v == null ? new Window(now, 1) : new Window(v.start(), v.attempts() + 1));
      if (window.attempts() > 10) {
        res.setHeader("Retry-After", "300");
        res.sendError(429);
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
