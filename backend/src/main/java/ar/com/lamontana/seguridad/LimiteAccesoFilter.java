package ar.com.lamontana.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Límite global del acceso inicial local; no almacena ni registra credenciales. */
final class LimiteAccesoFilter extends OncePerRequestFilter {
    private long inicio = System.nanoTime();
    private int intentos;
    private synchronized boolean admitir() {
        long ahora = System.nanoTime();
        if (ahora - inicio >= 60_000_000_000L) { inicio = ahora; intentos = 0; }
        return ++intentos <= 30;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) ||
                !("/api/auth/login".equals(request.getServletPath()) || "/api/setup/propietario".equals(request.getServletPath()));
    }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        if (!admitir()) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"mensaje\":\"Demasiados intentos. Esperá un minuto y volvé a intentar.\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
