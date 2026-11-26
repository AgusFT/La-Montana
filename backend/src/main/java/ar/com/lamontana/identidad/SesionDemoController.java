package ar.com.lamontana.identidad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ConditionalOnProperty(name = "lamontana.instalacion.modo", havingValue = "DEMO")
public class SesionDemoController {
    private static final UUID ADMINISTRADOR = UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0071");
    private static final UUID EMPLEADO = UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0072");
    private static final UUID CLIENTE = UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0073");

    private final IdentidadService identidad;
    private final SecurityContextRepository repositorio;

    public SesionDemoController(IdentidadService identidad, SecurityContextRepository repositorio) {
        this.identidad = identidad;
        this.repositorio = repositorio;
    }

    @PostMapping(path = "/api/demo/sesion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> iniciar(@Valid @RequestBody SeleccionRol seleccion,
                                      HttpServletRequest request, HttpServletResponse response) {
        var usuario = switch (seleccion.rol()) {
            case "ADMINISTRADOR" -> identidad.usuarioDemo(ADMINISTRADOR, "ADMIN_ADMIN");
            case "EMPLEADO" -> identidad.usuarioDemo(EMPLEADO, "EMPLEADO");
            case "CLIENTE" -> identidad.usuarioDemo(CLIENTE, "CLIENTE");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elegí un rol de Demo válido.");
        };
        identidad.registrarAcceso("LOGIN_CORRECTO", usuario.getUsername());
        usuario.eraseCredentials();

        // Reemplazar toda la sesión vuelve a generar tanto la cookie como el token CSRF.
        var anterior = request.getSession(false);
        if (anterior != null) anterior.invalidate();
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities()));
        SecurityContextHolder.setContext(contexto);
        repositorio.saveContext(contexto, request, response);
        return Map.of("mensaje", "Sesión iniciada.");
    }

    public record SeleccionRol(@NotBlank String rol) {}
}
