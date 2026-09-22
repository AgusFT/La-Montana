package ar.com.lamontana.configuracion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/configuracion")
public class ConfiguracionController {
    private final ConfiguracionService configuracion;
    public ConfiguracionController(ConfiguracionService configuracion) { this.configuracion=configuracion; }

    @GetMapping public ConfiguracionService.Estado estado(Principal actor) { return configuracion.estado(actor.getName()); }
    @PostMapping("/borradores")
    public ConfiguracionService.Borrador crear(@Valid @RequestBody CrearBorrador input,Principal actor) {
        return configuracion.crear(input,actor.getName());
    }
    @PutMapping("/borradores/{codigo}/modelo")
    public ConfiguracionService.Borrador seleccionar(@PathVariable UUID codigo,@Valid @RequestBody SeleccionarModelo input,Principal actor) {
        return configuracion.seleccionar(codigo,input,actor.getName());
    }

    public enum Modelo { MANUAL, CONDICIONAL }
    public enum Criterio { PAGO_PREVIO, SENA, MONTO_TOTAL }
    public record CrearBorrador(@NotNull UUID operacion) {}
    public record SeleccionarModelo(@NotNull UUID operacion,@NotNull @Min(1) Long version,
                                    @NotNull Modelo modelo,Criterio criterio) {}
}
