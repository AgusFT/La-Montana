package ar.com.lamontana.organizacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@org.springframework.context.annotation.Profile("historia-organizacion")
@RestController
public class EmpleadoController {
    private final EmpleadosDePrueba organizacion;
    public EmpleadoController(EmpleadosDePrueba organizacion) { this.organizacion = organizacion; }

    @GetMapping("/fixture/api/admin/empleados")
    public List<Empleado> listar() { return organizacion.empleados(); }

    @PostMapping("/fixture/api/admin/empleados")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> crear(@Valid @RequestBody NuevoEmpleado input, Principal principal) {
        return Map.of("codigoPublico", organizacion.crearEmpleado(input, principal.getName()));
    }

    @PutMapping("/fixture/api/admin/empleados/{codigo}")
    public Empleado actualizar(@PathVariable UUID codigo, @Valid @RequestBody EdicionEmpleado input, Principal principal) {
        return organizacion.actualizarEmpleado(codigo, input, principal.getName());
    }

    public record NuevoEmpleado(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 100) String apellido,
            @NotBlank @Email @Size(max = 254) String correo,
            @NotBlank @Size(min = 12, max = 128) String contrasena,
            @NotNull @Size(min = 1, max = 100) List<@NotNull UUID> sucursales,
            @NotNull @Size(max = 8) List<@NotBlank String> permisos) {
        @Override public String toString() { return "NuevoEmpleado[datos privados]"; }
    }
    public record EdicionEmpleado(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 100) String apellido,
            @NotBlank @Email @Size(max = 254) String correo,
            @NotBlank @Pattern(regexp = "ACTIVO|DESACTIVADO") String estado,
            @NotNull @PositiveOrZero Long version,
            @NotNull @Size(max = 100) List<@NotNull UUID> sucursales,
            @NotNull @Size(max = 8) List<@NotBlank String> permisos) {}
    public record Empleado(UUID codigoPublico, String nombre, String apellido, String correo, String estado,
                           long version, List<UUID> sucursales, List<String> permisos) {}
}
