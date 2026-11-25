//#region ENCABEZADO · RevisionConfiguracionController.java
/*
 * ========================================================================
 * ARCHIVO: RevisionConfiguracionController.java
 * ========================================================================
 * FUNCIÓN
 * Expone la revisión integral y la simulación del recorrido operativo para detectar bloqueos antes
 * de activar una configuración.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] RevisionConfiguracionController(RevisionConfiguracionService revision,
 *   ConfiguracionService configuracion, CatalogoService catalogo)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [private] void preparar(String correo)
 * - [public] RevisionConfiguracionService.Revision revisar(UUID borrador, Principal actor)
 *   Entrada HTTP GET sobre la ruta del controlador.
 * - [public] RevisionConfiguracionService.Simulacion simular(UUID borrador, Ejemplo in, Principal
 *   actor)
 *   Entrada HTTP POST · ruta del método: /simular.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - RevisionConfiguracionController (clase).
 * - RevisionConfiguracionController.Ejemplo (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import ar.com.lamontana.catalogo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/configuracion/borradores/{borrador}/revision")
public class RevisionConfiguracionController {
    private final RevisionConfiguracionService revision;private final ConfiguracionService configuracion;private final CatalogoService catalogo;
    public RevisionConfiguracionController(RevisionConfiguracionService revision,ConfiguracionService configuracion,CatalogoService catalogo){this.revision=revision;this.configuracion=configuracion;this.catalogo=catalogo;}
    private void preparar(String correo){configuracion.propietario(correo);catalogo.reconciliarProgramaciones();}
    @GetMapping public RevisionConfiguracionService.Revision revisar(@PathVariable UUID borrador,Principal actor){preparar(actor.getName());return revision.revisar(borrador,actor.getName());}
    @PostMapping("/simular") public RevisionConfiguracionService.Simulacion simular(@PathVariable UUID borrador,@Valid @RequestBody Ejemplo in,Principal actor){preparar(actor.getName());return revision.simular(borrador,in,actor.getName());}
    public record Ejemplo(@NotNull @Min(1) Long version,@NotNull UUID revisionComercial,@NotNull UUID sucursal,@NotNull @Valid EvaluadorPrecioItem.Item item,
        @NotNull EntregaConfiguracionController.Modalidad modalidad,@NotNull Instant recibidoEn,UUID punto,@Valid TerritorioEntrega territorio){}
}
