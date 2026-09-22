package ar.com.lamontana.configuracion;

import ar.com.lamontana.configuracion.PuntoEntregaController.Franja;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/configuracion/borradores/{borrador}/entrega")
public class EntregaConfiguracionController {
    private final EntregaConfiguracionService entrega;
    public EntregaConfiguracionController(EntregaConfiguracionService entrega){this.entrega=entrega;}
    @PutMapping public ConfiguracionService.Borrador guardar(@PathVariable UUID borrador,@Valid @RequestBody GuardarEntrega input,Principal actor){return entrega.guardar(borrador,input,actor.getName());}
    @GetMapping("/validacion") public EntregaConfiguracionService.Validacion validar(@PathVariable UUID borrador,Principal actor){return entrega.validar(borrador,actor.getName());}
    @PostMapping("/simular") public EvaluadorCalendario.Simulacion simular(@PathVariable UUID borrador,@Valid @RequestBody SimularEntrega input,Principal actor){return entrega.simular(borrador,input,actor.getName());}
    public enum Modalidad { RETIRO_SUCURSAL, RETIRO_PUNTO_ENTREGA, ENVIO_DOMICILIO }
    public record Dia(@NotNull @Min(1) @Max(7) Integer dia,Boolean habilitado,
                      @Pattern(regexp="(?:[01][0-9]|2[0-3]):[0-5][0-9]") String apertura,
                      @Pattern(regexp="(?:[01][0-9]|2[0-3]):[0-5][0-9]") String cierre) {}
    public record HorarioSucursal(@NotNull UUID sucursal,@NotNull @Size(max=7) List<@NotNull @Valid Dia> dias,@Size(max=100) List<@NotNull @Valid Franja> franjasRetiro) {}
    public record GuardarEntrega(@NotNull UUID operacion,@NotNull @Min(1) Long version,
                                 @Size(max=8) String preparacionHoras,@Size(max=8) String trasladoHoras,
                                 @NotNull @Size(max=3) List<@NotNull Modalidad> modalidades,
                                 @NotNull @Size(max=100) List<@NotNull @Valid HorarioSucursal> horariosPorSucursal) {}
    public record SimularEntrega(@NotNull @Min(1) Long version,@NotNull UUID sucursal,@NotNull Modalidad modalidad,@NotNull Instant recibidoEn,UUID punto,@Valid TerritorioEntrega territorio) {}
}
