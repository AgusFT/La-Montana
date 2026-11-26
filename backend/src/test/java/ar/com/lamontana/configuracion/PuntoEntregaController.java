package ar.com.lamontana.configuracion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ar.com.lamontana.configuracion.PuntosRepositorio.SucursalPunto;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@org.springframework.context.annotation.Profile("historia-organizacion")
@RestController
@RequestMapping("/fixture/api/admin/configuracion/borradores/{borrador}/entrega/puntos")
public class PuntoEntregaController {
    private final PuntoEntregaService puntos;
    public PuntoEntregaController(PuntoEntregaService puntos){this.puntos=puntos;}
    @PostMapping public ConfiguracionService.Borrador crear(@PathVariable UUID borrador,@Valid @RequestBody CrearPunto input,Principal actor){return puntos.crear(borrador,input,actor.getName());}
    @PutMapping("/{punto}") public ConfiguracionService.Borrador editar(@PathVariable UUID borrador,@PathVariable UUID punto,@Valid @RequestBody EditarPunto input,Principal actor){return puntos.editar(borrador,punto,input,actor.getName());}
    public interface Definicion {
        String nombre();String calle();String numero();String localidad();String provincia();String codigoPostal();String referencias();String zonaHoraria();List<SucursalPunto> sucursales();
    }
    public record CrearPunto(@NotNull UUID operacion,@NotNull @Min(1) Long version,@NotBlank @Size(max=80) String codigo,
                             @NotBlank @Size(max=160) String nombre,@NotBlank @Size(max=160) String calle,@NotBlank @Size(max=20) String numero,
                             @NotBlank @Size(max=120) String localidad,@NotBlank @Size(max=120) String provincia,@NotBlank @Size(max=12) String codigoPostal,
                             @Size(max=2000) String referencias,@NotBlank @Size(max=64) String zonaHoraria,
                             @NotNull @Size(max=100) List<@NotNull @Valid SucursalPunto> sucursales) implements Definicion {}
    public record EditarPunto(@NotNull UUID operacion,@NotNull @Min(1) Long version,
                              @NotBlank @Size(max=160) String nombre,@NotBlank @Size(max=160) String calle,@NotBlank @Size(max=20) String numero,
                              @NotBlank @Size(max=120) String localidad,@NotBlank @Size(max=120) String provincia,@NotBlank @Size(max=12) String codigoPostal,
                              @Size(max=2000) String referencias,@NotBlank @Size(max=64) String zonaHoraria,
                              @NotNull @Size(max=100) List<@NotNull @Valid SucursalPunto> sucursales) implements Definicion {}
}
