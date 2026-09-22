package ar.com.lamontana.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Las cookies persistentes no conservan acceso a una cuenta desactivada. */
public class SesionVigenteFilter extends OncePerRequestFilter {
    private final JdbcTemplate jdbc;
    public SesionVigenteFilter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            boolean vigente = Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                                  WHERE u.correo=? AND u.estado='ACTIVO' AND r.activo)
                    """, Boolean.class, authentication.getName()));
            if (!vigente) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"mensaje\":\"Tu acceso cambió. Volvé a iniciar sesión.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
