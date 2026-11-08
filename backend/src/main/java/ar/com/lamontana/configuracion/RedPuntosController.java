package ar.com.lamontana.configuracion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import java.util.List;
import ar.com.lamontana.configuracion.PuntoEntregaController.SucursalPunto;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/red-puntos")
public class RedPuntosController {
    public record Guardar(@NotNull UUID operacion,@NotNull @PositiveOrZero Long version,String codigo,
        @NotBlank @Size(max=160) String nombre,@NotBlank @Size(max=160) String calle,@NotBlank @Size(max=20) String numero,
        @NotBlank @Size(max=120) String localidad,@NotBlank @Size(max=120) String provincia,@NotBlank @Size(max=12) String codigoPostal,
        @Size(max=2000) String referencias,@NotBlank @Size(max=64) String zonaHoraria,
        @NotNull @Size(max=100) List<@NotNull @Valid SucursalPunto> sucursales) implements PuntoEntregaController.Definicion {}
    private final RedPuntosService service;
    public RedPuntosController(RedPuntosService service){this.service=service;}
    @GetMapping public RedPuntosService.Panel listar(Principal p){return service.listar(p.getName());}
    @PostMapping public RedPuntosService.Punto crear(@Valid @RequestBody Guardar in,Principal p){return service.crear(in,p.getName());}
    @PutMapping("/{punto}") public RedPuntosService.Punto editar(@PathVariable UUID punto,@Valid @RequestBody Guardar in,Principal p){return service.editar(punto,in,p.getName());}
}
