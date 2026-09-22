package ar.com.lamontana.configuracion;
import ar.com.lamontana.catalogo.CatalogoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/configuracion/borradores/{borrador}/activacion")
public class ActivacionConfiguracionController {
 private final ActivacionConfiguracionService activacion;private final ConfiguracionService configuracion;private final CatalogoService catalogo;
 public ActivacionConfiguracionController(ActivacionConfiguracionService activacion,ConfiguracionService configuracion,CatalogoService catalogo){this.activacion=activacion;this.configuracion=configuracion;this.catalogo=catalogo;}
 private void preparar(String correo){configuracion.propietario(correo);catalogo.reconciliarProgramaciones();}
 @PostMapping("/solicitar") public SeguridadConfiguracion.Solicitud solicitar(@PathVariable UUID borrador,@Valid @RequestBody Solicitar input,Principal actor){preparar(actor.getName());return activacion.solicitar(borrador,input,actor.getName());}
 @PostMapping("/confirmar") public ActivacionConfiguracionService.Resultado confirmar(@PathVariable UUID borrador,@Valid @RequestBody Confirmar input,Principal actor){preparar(actor.getName());return activacion.confirmar(borrador,input,actor.getName());}
 @PostMapping("/revocar") public Map<String,String> revocar(@PathVariable UUID borrador,@Valid @RequestBody Revocar input,Principal actor){activacion.revocar(borrador,input.operacion(),actor.getName());return Map.of("mensaje","La autorización quedó invalidada. El borrador se conserva.");}
 public record Decision(@NotNull @Min(1) Long version,@NotNull UUID revisionComercial,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String huellaRevision,@NotNull Boolean advertenciasRevisadas,@NotBlank @Size(max=500) String motivo){}
 public record Solicitar(@NotNull UUID operacion,@NotNull @Valid Decision decision,@NotBlank @Size(max=128) String contrasena){@Override public String toString(){return "SolicitarActivacion[privada]";}}
 public record Confirmar(@NotNull UUID operacion,@NotNull @Valid Decision decision,@NotBlank @Size(max=128) String contrasena,@NotBlank @Size(max=200) String codigo){@Override public String toString(){return "ConfirmarActivacion[privada]";}}
 public record Revocar(@NotNull UUID operacion){}
}
