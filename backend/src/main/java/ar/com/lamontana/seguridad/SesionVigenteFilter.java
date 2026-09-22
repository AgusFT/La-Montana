package ar.com.lamontana.seguridad;

import ar.com.lamontana.identidad.UsuarioSesion;
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
            boolean vigente = authentication.getPrincipal() instanceof UsuarioSesion usuario && Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                                  WHERE u.correo=? AND u.estado='ACTIVO' AND r.activo AND u.version_acceso=?)
                    """, Boolean.class, authentication.getName(), usuario.versionAcceso()));
            if (!vigente) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"mensaje\":\"Tu acceso cambió. Volvé a iniciar sesión.\"}");
                return;
            }
            boolean cambiar = Boolean.TRUE.equals(jdbc.queryForObject("SELECT debe_cambiar_contrasena FROM lamontana.usuario WHERE correo=?", Boolean.class, authentication.getName()));
            if (cambiar && !java.util.Set.of("/api/auth/me", "/api/auth/csrf", "/api/auth/contrasena", "/api/auth/correo/solicitar", "/api/auth/correo/confirmar", "/api/auth/logout").contains(request.getServletPath())) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"mensaje\":\"Cambiá la contraseña inicial desde Seguridad de tu cuenta antes de continuar.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
