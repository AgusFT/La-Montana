//#region ENCABEZADO · RecursosConfiguracionController.java
/*
 * ========================================================================
 * ARCHIVO: RecursosConfiguracionController.java
 * ========================================================================
 * FUNCIÓN
 * Expone las operaciones del borrador sobre servicios por sucursal e impresoras: alta, edición,
 * cambio de estado y retiro.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] RecursosConfiguracionController(RecursosConfiguracionService recursos)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] ConfiguracionService.Borrador guardar(UUID borrador, GuardarRecursos input, Principal
 *   actor)
 *   Entrada HTTP PUT · ruta del método: /recursos.
 * - [public] ConfiguracionService.Borrador crear(UUID borrador, CrearImpresora input, Principal
 *   actor)
 *   Entrada HTTP POST · ruta del método: /impresoras.
 * - [public] ConfiguracionService.Borrador editar(UUID borrador, UUID impresora, EditarImpresora
 *   input, Principal actor)
 *   Entrada HTTP PUT · ruta del método: /impresoras/{impresora}.
 * - [public] ConfiguracionService.Borrador estado(UUID borrador, UUID impresora, CambiarEstado
 *   input, Principal actor)
 *   Entrada HTTP POST · ruta del método: /impresoras/{impresora}/estado.
 * - [public] ConfiguracionService.Borrador retirar(UUID borrador, UUID impresora, RetirarImpresora
 *   input, Principal actor)
 *   Entrada HTTP POST · ruta del método: /impresoras/{impresora}/retirar.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - RecursosConfiguracionController (clase).
 * - RecursosConfiguracionController.MetodoAsignacion (enumeración).
 * - RecursosConfiguracionController.EstadoSeleccionado (enumeración).
 * - RecursosConfiguracionController.ServiciosSucursal (record).
 * - RecursosConfiguracionController.GuardarRecursos (record).
 * - RecursosConfiguracionController.CrearImpresora (record).
 * - RecursosConfiguracionController.EditarImpresora (record).
 * - RecursosConfiguracionController.CambiarEstado (record).
 * - RecursosConfiguracionController.RetirarImpresora (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/configuracion/borradores/{borrador}")
public class RecursosConfiguracionController {
    private final RecursosConfiguracionService recursos;
    public RecursosConfiguracionController(RecursosConfiguracionService recursos){this.recursos=recursos;}
    @PutMapping("/recursos")
    public ConfiguracionService.Borrador guardar(@PathVariable UUID borrador,@Valid @RequestBody GuardarRecursos input,Principal actor){return recursos.guardar(borrador,input,actor.getName());}
    @PostMapping("/impresoras")
    public ConfiguracionService.Borrador crear(@PathVariable UUID borrador,@Valid @RequestBody CrearImpresora input,Principal actor){return recursos.crear(borrador,input,actor.getName());}
    @PutMapping("/impresoras/{impresora}")
    public ConfiguracionService.Borrador editar(@PathVariable UUID borrador,@PathVariable UUID impresora,@Valid @RequestBody EditarImpresora input,Principal actor){return recursos.editar(borrador,impresora,input,actor.getName());}
    @PostMapping("/impresoras/{impresora}/estado")
    public ConfiguracionService.Borrador estado(@PathVariable UUID borrador,@PathVariable UUID impresora,@Valid @RequestBody CambiarEstado input,Principal actor){return recursos.estado(borrador,impresora,input,actor.getName());}
    @PostMapping("/impresoras/{impresora}/retirar")
    public ConfiguracionService.Borrador retirar(@PathVariable UUID borrador,@PathVariable UUID impresora,@Valid @RequestBody RetirarImpresora input,Principal actor){return recursos.retirar(borrador,impresora,input,actor.getName());}

    public enum MetodoAsignacion { MANUAL }
    public enum EstadoSeleccionado { OPERATIVA, DESHABILITADA }
    public record ServiciosSucursal(@NotNull UUID sucursal,@NotNull @Size(max=100) List<@NotNull UUID> servicios) {}
    public record GuardarRecursos(@NotNull UUID operacion,@NotNull @Min(1) Long version,@NotNull MetodoAsignacion metodoAsignacion,
                                   @NotNull @Size(max=100) List<@NotNull @Valid ServiciosSucursal> serviciosPorSucursal) {}
    public record CrearImpresora(@NotNull UUID operacion,@NotNull @Min(1) Long version,@NotNull UUID sucursal,@NotBlank @Size(max=120) String nombre,
                                 @NotNull @Size(min=1,max=300) List<@NotNull UUID> formatos,@NotNull Boolean admiteColor,@NotNull Boolean admiteDobleFaz,
                                 @NotNull @Positive Integer capacidadHojas,@NotNull EstadoSeleccionado estado) {}
    public record EditarImpresora(@NotNull UUID operacion,@NotNull @Min(1) Long version,@NotBlank @Size(max=120) String nombre,
                                  @NotNull @Size(min=1,max=300) List<@NotNull UUID> formatos,@NotNull Boolean admiteColor,@NotNull Boolean admiteDobleFaz,
                                  @NotNull @Positive Integer capacidadHojas) {}
    public record CambiarEstado(@NotNull UUID operacion,@NotNull @Min(1) Long version,@NotNull EstadoSeleccionado estado,@NotBlank @Size(max=500) String motivo) {}
    public record RetirarImpresora(@NotNull UUID operacion,@NotNull @Min(1) Long version,@NotBlank @Size(max=500) String motivo) {}
}
