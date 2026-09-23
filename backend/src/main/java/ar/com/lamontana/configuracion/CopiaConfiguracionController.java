//#region ENCABEZADO · CopiaConfiguracionController.java
/*
 * ========================================================================
 * ARCHIVO: CopiaConfiguracionController.java
 * ========================================================================
 * FUNCIÓN
 * Expone la creación de un borrador a partir de una configuración guardada y la confirmación de la
 * revisión de los parámetros copiados.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] CopiaConfiguracionController(CopiaConfiguracionService service)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] ConfiguracionService.Borrador copiar(UUID origen, Crear in, Principal actor)
 *   Entrada HTTP POST · ruta del método: /api/admin/configuracion/historial/{origen}/base.
 * - [public] RevisionConfiguracionService.Revision confirmar(UUID borrador, ConfirmarRevision in,
 *   Principal actor)
 *   Entrada HTTP POST · ruta del método:
 *   /api/admin/configuracion/borradores/{borrador}/revision/confirmar.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - CopiaConfiguracionController (clase).
 * - CopiaConfiguracionController.Crear (record).
 * - CopiaConfiguracionController.ConfirmarRevision (record).
 * ========================================================================
 */
//#endregion

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
