package ar.com.lamontana.configuracion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
@RestController
public class CopiaConfiguracionController {
 private final CopiaConfiguracionService service;
 public CopiaConfiguracionController(CopiaConfiguracionService service){this.service=service;}
 @PostMapping("/api/admin/configuracion/historial/{origen}/base") public ConfiguracionService.Borrador copiar(@PathVariable UUID origen,@Valid @RequestBody Crear in,Principal actor){return service.copiar(origen,in,actor.getName());}
 @PostMapping("/api/admin/configuracion/borradores/{borrador}/revision/confirmar") public RevisionConfiguracionService.Revision confirmar(@PathVariable UUID borrador,@Valid @RequestBody ConfirmarRevision in,Principal actor){return service.confirmar(borrador,in,actor.getName());}
 public record Crear(@NotNull UUID operacion){}
 public record ConfirmarRevision(@NotNull UUID operacion,@NotNull @Min(1) Long version,@NotNull UUID revisionComercial,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String huellaRevision,@NotNull Boolean advertenciasRevisadas){}
}
