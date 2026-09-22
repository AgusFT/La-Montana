package ar.com.lamontana.configuracion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import ar.com.lamontana.configuracion.PuntoEntregaController.Franja;

@RestController
@RequestMapping("/api/admin/configuracion/borradores/{borrador}/entrega/zonas")
public class ZonaEntregaController {
    private final ZonaEntregaService zonas;
    public ZonaEntregaController(ZonaEntregaService zonas){this.zonas=zonas;}
    @PostMapping public ConfiguracionService.Borrador crear(@PathVariable UUID borrador,@Valid @RequestBody CrearZona input,Principal actor){return zonas.crear(borrador,input,actor.getName());}
    @PutMapping("/{zona}") public ConfiguracionService.Borrador editar(@PathVariable UUID borrador,@PathVariable UUID zona,@Valid @RequestBody EditarZona input,Principal actor){return zonas.editar(borrador,zona,input,actor.getName());}
    public interface Definicion {String nombre();String descripcion();String zonaHoraria();String costo();Boolean habilitada();List<TerritorioEntrega> territorios();List<Franja> franjas();}
    public record CrearZona(@NotNull UUID operacion,@NotNull @Min(1) Long version,@NotBlank @Size(max=80) String codigo,
        @NotBlank @Size(max=140) String nombre,@Size(max=2000) String descripcion,@NotBlank @Size(max=64) String zonaHoraria,
        @NotNull @Size(max=15) String costo,@NotNull Boolean habilitada,
        @NotNull @Size(max=100) List<@NotNull @Valid TerritorioEntrega> territorios,@NotNull @Size(max=100) List<@NotNull @Valid Franja> franjas) implements Definicion {}
    public record EditarZona(@NotNull UUID operacion,@NotNull @Min(1) Long version,
        @NotBlank @Size(max=140) String nombre,@Size(max=2000) String descripcion,@NotBlank @Size(max=64) String zonaHoraria,
        @NotNull @Size(max=15) String costo,@NotNull Boolean habilitada,
        @NotNull @Size(max=100) List<@NotNull @Valid TerritorioEntrega> territorios,@NotNull @Size(max=100) List<@NotNull @Valid Franja> franjas) implements Definicion {}
}
