//#region ENCABEZADO · IdentidadController.java
/*
 * ========================================================================
 * ARCHIVO: IdentidadController.java
 * ========================================================================
 * FUNCIÓN
 * Expone la instalación inicial del propietario, registro de clientes, estado de identidad, perfil
 * de sesión y obtención del token CSRF.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] IdentidadController(IdentidadService identidad)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] IdentidadService.EstadoInstalacion estado()
 *   Entrada HTTP GET · ruta del método: /api/setup/estado.
 * - [public] Map<String, String> crear(AltaPropietario alta)
 *   Entrada HTTP POST · ruta del método: /api/setup/propietario.
 * - [public] Map<String, String> csrf(CsrfToken csrf)
 *   Entrada HTTP GET · ruta del método: /api/auth/csrf.
 * - [public] IdentidadService.Perfil perfil(Principal principal)
 *   Entrada HTTP GET · ruta del método: /api/auth/me.
 * - [public] Map<String, Boolean> administracion()
 *   Entrada HTTP GET · ruta del método: /api/admin/estado.
 * - [public] Map<String, String> registro(RegistroCliente registro)
 *   Entrada HTTP POST · ruta del método: /api/auth/registro.
 * - [public] Map<String, Boolean> cliente()
 *   Entrada HTTP GET · ruta del método: /api/cliente/estado.
 * - [public] RegistroCliente :: String toString()
 * - [public] AltaPropietario :: String toString()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - IdentidadController (clase).
 * - IdentidadController.RegistroCliente (record).
 * - IdentidadController.AltaPropietario (record).
 * ========================================================================
 */
//#endregion

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

    @PostMapping("/api/auth/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> registro(@Valid @RequestBody RegistroCliente registro) {
        identidad.registrarCliente(registro);
        return Map.of("mensaje", "Cuenta creada. Ya podés iniciar sesión.");
    }

    @GetMapping("/api/cliente/estado")
    public Map<String, Boolean> cliente() { return Map.of("operacionDisponible", false); }

    public record RegistroCliente(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 100) String apellido,
            @NotBlank @Email @Size(max = 254) String correo,
            @NotBlank @Size(min = 12, max = 128) String contrasena) {
        @Override public String toString() { return "RegistroCliente[datos privados]"; }
    }

    public record AltaPropietario(
            @NotBlank @Size(max = 256) String token,
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 100) String apellido,
            @NotBlank @Email @Size(max = 254) String correo,
            @NotBlank @Size(min = 12, max = 128) String contrasena) {
        @Override public String toString() { return "AltaPropietario[datos privados]"; }
    }
}
