package pe.edu.gui.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite por instancia. Para múltiples réplicas, añadir limitador compartido en el reverse proxy.
 */
public class LoginThrottle extends OncePerRequestFilter {

    private record Window(long start, int attempts) {}

    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

    /**
     * Limita peticiones de login por dirección remota a diez por ventana de cinco minutos, dentro
     * de esta instancia.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (req.getMethod().equals("POST") && req.getServletPath().equals("/api/auth/login")) {
            long now = System.currentTimeMillis();
            attempts.entrySet().removeIf(e -> now - e.getValue().start() > 300000);
            if (attempts.size() > 10000) {
                res.sendError(429);
                return;
            }
            Window w =
                    attempts.compute(
                            req.getRemoteAddr(),
                            (key, old) ->
                                    old == null
                                            ? new Window(now, 1)
                                            : new Window(old.start(), old.attempts() + 1));
            if (w.attempts() > 10) {
                res.setHeader("Retry-After", "300");
                res.sendError(429);
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
