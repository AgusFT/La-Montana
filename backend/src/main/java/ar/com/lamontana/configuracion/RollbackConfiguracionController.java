//#region ENCABEZADO · RollbackConfiguracionController.java
/*
 * ========================================================================
 * ARCHIVO: RollbackConfiguracionController.java
 * ========================================================================
 * FUNCIÓN
 * Expone la revisión, solicitud, confirmación y revocación de una reversión manual hacia una
 * configuración operativa anterior.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] RollbackConfiguracionController(RollbackConfiguracionService rollback,
 *   ConfiguracionService configuracion, CatalogoService catalogo)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [private] void preparar(String correo)
 * - [public] RollbackConfiguracionService.Revision revisar(Principal actor)
 *   Entrada HTTP GET · ruta del método: /revision.
 * - [public] SeguridadConfiguracion.Solicitud solicitar(Solicitar in, Principal actor)
 *   Entrada HTTP POST · ruta del método: /solicitar.
 * - [public] RollbackConfiguracionService.Resultado confirmar(Confirmar in, Principal actor)
 *   Entrada HTTP POST · ruta del método: /confirmar.
 * - [public] Map<String, String> revocar(Revocar in, Principal actor)
 *   Entrada HTTP POST · ruta del método: /revocar.
 * - [public] Solicitar :: String toString()
 * - [public] Confirmar :: String toString()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - RollbackConfiguracionController (clase).
 * - RollbackConfiguracionController.Decision (record).
 * - RollbackConfiguracionController.Solicitar (record).
 * - RollbackConfiguracionController.Confirmar (record).
 * - RollbackConfiguracionController.Revocar (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import ar.com.lamontana.catalogo.CatalogoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/configuracion/rollback")
public class RollbackConfiguracionController {
    private final RollbackConfiguracionService rollback;
    private final ConfiguracionService configuracion;
    private final CatalogoService catalogo;
    public RollbackConfiguracionController(RollbackConfiguracionService rollback,ConfiguracionService configuracion,CatalogoService catalogo){this.rollback=rollback;this.configuracion=configuracion;this.catalogo=catalogo;}
    private void preparar(String correo){configuracion.propietario(correo);catalogo.reconciliarProgramaciones();}
    @GetMapping("/revision") public RollbackConfiguracionService.Revision revisar(Principal actor){preparar(actor.getName());return rollback.revisar(actor.getName());}
    @PostMapping("/solicitar") public SeguridadConfiguracion.Solicitud solicitar(@Valid @RequestBody Solicitar in,Principal actor){preparar(actor.getName());return rollback.solicitar(in,actor.getName());}
    @PostMapping("/confirmar") public RollbackConfiguracionService.Resultado confirmar(@Valid @RequestBody Confirmar in,Principal actor){preparar(actor.getName());return rollback.confirmar(in,actor.getName());}
    @PostMapping("/revocar") public Map<String,String> revocar(@Valid @RequestBody Revocar in,Principal actor){rollback.revocar(in.objetivo(),in.operacion(),actor.getName());return Map.of("mensaje","La autorización de reversión quedó invalidada. La configuración se conserva.");}
    public record Decision(@NotNull UUID activa,@NotNull UUID objetivo,@NotNull @Min(1) Long versionObjetivo,
        UUID programada,@NotNull UUID revisionComercial,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String huellaRevision,
        @NotNull Boolean advertenciasRevisadas,@NotNull Boolean cancelarProgramacion,@NotBlank @Size(max=500) String motivo){}
    public record Solicitar(@NotNull UUID operacion,@NotNull @Valid Decision decision,@NotBlank @Size(max=128) String contrasena){@Override public String toString(){return "SolicitarRollback[privada]";}}
    public record Confirmar(@NotNull UUID operacion,@NotNull @Valid Decision decision,@NotBlank @Size(max=128) String contrasena,@NotBlank @Size(max=200) String codigo){@Override public String toString(){return "ConfirmarRollback[privada]";}}
    public record Revocar(@NotNull UUID operacion,@NotNull UUID objetivo){}
}
