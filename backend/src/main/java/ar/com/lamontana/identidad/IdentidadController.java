package ar.com.lamontana.identidad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdentidadController {
    private final IdentidadService identidad;
    public IdentidadController(IdentidadService identidad) { this.identidad = identidad; }

    @GetMapping("/api/setup/estado")
    public IdentidadService.EstadoInstalacion estado() { return identidad.estado(); }

    @PostMapping("/api/setup/propietario")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> crear(@Valid @RequestBody AltaPropietario alta) {
        identidad.crearPropietario(alta);
        return Map.of("mensaje", "Propietario creado. Ya podés iniciar sesión.");
    }

    @GetMapping("/api/auth/csrf")
    public Map<String, String> csrf(CsrfToken csrf) { return Map.of("token", csrf.getToken(), "headerName", csrf.getHeaderName()); }

    @GetMapping("/api/auth/me")
    public IdentidadService.Perfil perfil(Principal principal) { return identidad.perfil(principal.getName()); }

    @GetMapping("/api/admin/estado")
    public Map<String, Boolean> administracion() { return Map.of("configuracionDisponible", false, "operacionDisponible", false); }

    public record AltaPropietario(
            @NotBlank @Size(max = 256) String token,
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 100) String apellido,
            @NotBlank @Email @Size(max = 254) String correo,
            @NotBlank @Size(min = 12, max = 128) String contrasena) {
        @Override public String toString() { return "AltaPropietario[datos privados]"; }
    }
}
