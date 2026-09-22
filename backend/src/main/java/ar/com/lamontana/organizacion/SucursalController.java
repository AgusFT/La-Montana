package ar.com.lamontana.organizacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class SucursalController {
    private final OrganizacionService organizacion;
    public SucursalController(OrganizacionService organizacion) { this.organizacion = organizacion; }

    @GetMapping("/api/admin/sucursales")
    public List<Sucursal> listar() { return organizacion.sucursales(); }

    @PostMapping("/api/admin/sucursales")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> crear(@Valid @RequestBody NuevaSucursal input, Principal principal) {
        return Map.of("codigoPublico", organizacion.crearSucursal(input, principal.getName()));
    }

    @PutMapping("/api/admin/sucursales/{codigo}")
    public Sucursal actualizar(@PathVariable UUID codigo, @Valid @RequestBody EdicionSucursal input, Principal principal) {
        return organizacion.actualizarSucursal(codigo, input, principal.getName());
    }

    public record NuevaSucursal(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String codigo,
            @NotBlank @Size(max = 140) String nombre,
            @NotBlank @Size(max = 160) String calle,
            @NotBlank @Size(max = 20) String numero,
            @NotBlank @Size(max = 120) String localidad,
            @NotBlank @Size(max = 120) String provincia,
            @NotBlank @Size(max = 12) String codigoPostal,
            @Email @Size(max = 254) String correo,
            @Size(max = 40) String telefono,
            @NotBlank @Size(max = 64) String zonaHoraria) {}
    public record EdicionSucursal(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String codigo,
            @NotBlank @Size(max = 140) String nombre,
            @NotBlank @Size(max = 160) String calle,
            @NotBlank @Size(max = 20) String numero,
            @NotBlank @Size(max = 120) String localidad,
            @NotBlank @Size(max = 120) String provincia,
            @NotBlank @Size(max = 12) String codigoPostal,
            @Email @Size(max = 254) String correo,
            @Size(max = 40) String telefono,
            @NotBlank @Size(max = 64) String zonaHoraria,
            @NotBlank @Pattern(regexp = "ACTIVA|DESACTIVADA") String estado,
            @NotNull @PositiveOrZero Long version) {}
    public record Sucursal(UUID codigoPublico, String codigo, String nombre, String calle, String numero,
                           String localidad, String provincia, String codigoPostal, String correo,
                           String telefono, String zonaHoraria, String estado, long version) {}
}
