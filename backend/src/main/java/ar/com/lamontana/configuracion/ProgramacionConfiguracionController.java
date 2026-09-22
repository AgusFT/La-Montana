package ar.com.lamontana.configuracion;

import ar.com.lamontana.catalogo.CatalogoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/configuracion/borradores/{borrador}/programacion")
public class ProgramacionConfiguracionController {
    private final ProgramacionConfiguracionService servicio;
    private final CancelacionConfiguracionService cancelacion;
    private final ConfiguracionService configuracion;
    private final CatalogoService catalogo;
    private final ActivacionConfiguracionService activacion;
    private final RevisionConfiguracionService revision;
    public ProgramacionConfiguracionController(ProgramacionConfiguracionService servicio,CancelacionConfiguracionService cancelacion,ConfiguracionService configuracion,CatalogoService catalogo,ActivacionConfiguracionService activacion,RevisionConfiguracionService revision){this.servicio=servicio;this.cancelacion=cancelacion;this.configuracion=configuracion;this.catalogo=catalogo;this.activacion=activacion;this.revision=revision;}
    private void preparar(String actor){configuracion.propietario(actor);catalogo.reconciliarProgramaciones();}
    @PostMapping("/solicitar") public SeguridadConfiguracion.Solicitud solicitar(@PathVariable UUID borrador,@Valid @RequestBody Solicitar i,Principal a){preparar(a.getName());return servicio.solicitar(borrador,i,a.getName());}
    @PostMapping("/confirmar") public ConfiguracionService.Programacion confirmar(@PathVariable UUID borrador,@Valid @RequestBody Confirmar i,Principal a){preparar(a.getName());return servicio.confirmar(borrador,i,a.getName());}
    @PostMapping("/revocar") public Map<String,String> revocar(@PathVariable UUID borrador,@Valid @RequestBody ActivacionConfiguracionController.Revocar i,Principal a){servicio.revocar(borrador,i.operacion(),a.getName());return Map.of("mensaje","La autorización quedó invalidada.");}
    @PostMapping("/cancelacion/solicitar") public SeguridadConfiguracion.Solicitud solicitarCancelacion(@PathVariable UUID borrador,@Valid @RequestBody ConfiguracionController.SolicitarCancelacion i,Principal a){return cancelacion.solicitarProgramacion(borrador,i,a.getName());}
    @PostMapping("/cancelacion/confirmar") public ConfiguracionService.Borrador cancelar(@PathVariable UUID borrador,@Valid @RequestBody ConfiguracionController.ConfirmarCancelacion i,Principal a){return cancelacion.confirmarProgramacion(borrador,i,a.getName());}
    @PostMapping("/cancelacion/revocar") public Map<String,String> revocarCancelacion(@PathVariable UUID borrador,@Valid @RequestBody ActivacionConfiguracionController.Revocar i,Principal a){cancelacion.revocar(borrador,i.operacion(),a.getName());return Map.of("mensaje","La autorización quedó invalidada.");}
    @GetMapping("/revision") public RevisionConfiguracionService.Revision revisar(@PathVariable UUID borrador,Principal a){preparar(a.getName());return revision.revisarProgramada(borrador,a.getName());}
    @PostMapping("/activacion/solicitar") public SeguridadConfiguracion.Solicitud solicitarActivacion(@PathVariable UUID borrador,@Valid @RequestBody ActivacionConfiguracionController.Solicitar i,Principal a){preparar(a.getName());return activacion.solicitarProgramada(borrador,i,a.getName());}
    @PostMapping("/activacion/confirmar") public ActivacionConfiguracionService.Resultado activar(@PathVariable UUID borrador,@Valid @RequestBody ActivacionConfiguracionController.Confirmar i,Principal a){preparar(a.getName());return activacion.confirmarProgramada(borrador,i,a.getName());}
    @PostMapping("/activacion/revocar") public Map<String,String> revocarActivacion(@PathVariable UUID borrador,@Valid @RequestBody ActivacionConfiguracionController.Revocar i,Principal a){activacion.revocar(borrador,i.operacion(),a.getName());return Map.of("mensaje","La autorización quedó invalidada.");}
    public record Decision(@NotNull @Valid ActivacionConfiguracionController.Decision revision,@NotNull LocalDateTime fechaLocal,@NotBlank @Size(max=64) String zonaHoraria){}
    public record Solicitar(@NotNull UUID operacion,@NotNull @Valid Decision decision,@NotBlank @Size(max=128) String contrasena){@Override public String toString(){return "SolicitarProgramacion[privada]";}}
    public record Confirmar(@NotNull UUID operacion,@NotNull @Valid Decision decision,@NotBlank @Size(max=128) String contrasena,@NotBlank @Size(max=200) String codigo){@Override public String toString(){return "ConfirmarProgramacion[privada]";}}
}
