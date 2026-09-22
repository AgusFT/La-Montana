package ar.com.lamontana.cotizaciones;

import ar.com.lamontana.catalogo.EvaluadorPrecioItem;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad;
import ar.com.lamontana.configuracion.TerritorioEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cliente/cotizaciones")
public class CotizacionController {
    private final CotizacionService cotizaciones;
    public CotizacionController(CotizacionService cotizaciones){this.cotizaciones=cotizaciones;}
    @GetMapping("/opciones") public CotizacionService.Opciones opciones(Principal actor){return cotizaciones.opciones(actor.getName());}
    @GetMapping public CotizacionService.Pagina listar(@RequestParam(defaultValue="0") int pagina,Principal actor){return cotizaciones.listar(pagina,actor.getName());}
    @GetMapping("/{id}") public CotizacionService.Detalle detalle(@PathVariable UUID id,Principal actor){return cotizaciones.detalle(id,actor.getName());}
    @PostMapping public CotizacionService.Detalle crear(@Valid @RequestBody Crear in,Principal actor){return cotizaciones.crear(in,actor.getName());}
    @PostMapping("/{id}/aceptar") public CotizacionService.Detalle aceptar(@PathVariable UUID id,@Valid @RequestBody Decision in,Principal actor){return cotizaciones.decidir(id,in,true,actor.getName());}
    @PostMapping("/{id}/cancelar") public CotizacionService.Detalle cancelar(@PathVariable UUID id,@Valid @RequestBody Decision in,Principal actor){return cotizaciones.decidir(id,in,false,actor.getName());}
    public record Documento(@NotBlank @Size(max=255) String nombre,@Min(1) @Max(10485760) long bytes,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String sha256){}
    public record Item(@NotNull @Valid Documento documento,@NotNull @Valid EvaluadorPrecioItem.Item trabajo){}
    public record Direccion(@NotBlank @Size(max=140) String calle,@NotBlank @Size(max=30) String numero,@Size(max=300) String referencias,@NotNull @Valid TerritorioEntrega territorio){}
    public record Crear(@NotNull UUID operacion,@NotNull UUID configuracion,@NotNull UUID revisionComercial,@NotNull UUID sucursal,
        @NotNull @Size(min=1,max=20) List<@NotNull @Valid Item> items,@NotNull Modalidad modalidad,UUID punto,@Valid Direccion direccion,
        @NotNull MedioPago medioPago,UUID reemplaza){}
    public record Decision(@NotNull UUID operacion,@Min(1) long version,@Size(max=300) String motivo){}
}
