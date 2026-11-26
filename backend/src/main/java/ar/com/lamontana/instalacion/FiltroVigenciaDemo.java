package ar.com.lamontana.instalacion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroVigenciaDemo extends OncePerRequestFilter {
    private final VigenciaDemo vigencia;
    private final ObjectMapper json;

    public FiltroVigenciaDemo(VigenciaDemo vigencia, ObjectMapper json) {
        this.vigencia = vigencia;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String ruta = req.getServletPath();
        return "GET".equals(req.getMethod()) && (ruta.equals("/api/demo/vigencia")
                || ruta.equals("/actuator/health") || ruta.startsWith("/actuator/health/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain cadena)
            throws ServletException, IOException {
        var estado = vigencia.evaluar();
        if (estado.estado() == VigenciaDemo.Situacion.VIGENTE) {
            cadena.doFilter(req, res);
            return;
        }
        res.setStatus(estado.estado() == VigenciaDemo.Situacion.NO_DISPONIBLE ? 503 : 423);
        res.setContentType("application/json;charset=UTF-8");
        res.setHeader("Cache-Control", "no-store");
        json.writeValue(res.getOutputStream(), estado);
    }
}
