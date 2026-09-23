//#region ENCABEZADO · ConfiguracionController.java
/*
 * ========================================================================
 * ARCHIVO: ConfiguracionController.java
 * ========================================================================
 * FUNCIÓN
 * Expone el estado y las operaciones iniciales del configurador: crear borrador, elegir modelo,
 * cancelar y guardar o simular los parámetros financieros.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] ConfiguracionController(ConfiguracionService configuracion,
 *   CancelacionConfiguracionService cancelacion, PagosConfiguracionService pagos)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] ConfiguracionService.Estado estado(Principal actor)
 *   Entrada HTTP GET sobre la ruta del controlador.
 * - [public] ConfiguracionService.Borrador crear(CrearBorrador input, Principal actor)
 *   Entrada HTTP POST · ruta del método: /borradores.
 * - [public] ConfiguracionService.Borrador seleccionar(UUID codigo, SeleccionarModelo input,
 *   Principal actor)
 *   Entrada HTTP PUT · ruta del método: /borradores/{codigo}/modelo.
 * - [public] SeguridadConfiguracion.Solicitud solicitarCancelacion(UUID codigo,
 *   SolicitarCancelacion input, Principal actor)
 *   Entrada HTTP POST · ruta del método: /borradores/{codigo}/cancelacion/solicitar.
 * - [public] ConfiguracionService.Borrador confirmarCancelacion(UUID codigo, ConfirmarCancelacion
 *   input, Principal actor)
 *   Entrada HTTP POST · ruta del método: /borradores/{codigo}/cancelacion/confirmar.
 * - [public] java.util.Map<String, String> revocarCancelacion(UUID codigo,
 *   ActivacionConfiguracionController.Revocar input, Principal actor)
 *   Entrada HTTP POST · ruta del método: /borradores/{codigo}/cancelacion/revocar.
 * - [public] ConfiguracionService.Borrador guardarPagos(UUID codigo, GuardarPagos input, Principal
 *   actor)
 *   Entrada HTTP PUT · ruta del método: /borradores/{codigo}/pagos.
 * - [public] EvaluadorFinanciero.Simulacion simularPagos(UUID codigo, SimularPagos input,
 *   Principal actor)
 *   Entrada HTTP POST · ruta del método: /borradores/{codigo}/pagos/simular.
 * - [public] SolicitarCancelacion :: String toString()
 * - [public] ConfirmarCancelacion :: String toString()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ConfiguracionController (clase).
 * - ConfiguracionController.Modelo (enumeración).
 * - ConfiguracionController.Criterio (enumeración).
 * - ConfiguracionController.MedioPago (enumeración).
 * - ConfiguracionController.CondicionSena (enumeración).
 * - ConfiguracionController.TipoSena (enumeración).
 * - ConfiguracionController.Pagos (record).
 * - ConfiguracionController.GuardarPagos (record).
 * - ConfiguracionController.SimularPagos (record).
 * - ConfiguracionController.CrearBorrador (record).
 * - ConfiguracionController.SeleccionarModelo (record).
 * - ConfiguracionController.SolicitarCancelacion (record).
 * - ConfiguracionController.ConfirmarCancelacion (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/configuracion")
public class ConfiguracionController {
    private final ConfiguracionService configuracion;
    private final CancelacionConfiguracionService cancelacion;
    private final PagosConfiguracionService pagos;
    public ConfiguracionController(ConfiguracionService configuracion,CancelacionConfiguracionService cancelacion,PagosConfiguracionService pagos) { this.configuracion=configuracion;this.cancelacion=cancelacion;this.pagos=pagos; }

    @GetMapping public ConfiguracionService.Estado estado(Principal actor) { return configuracion.estado(actor.getName()); }
    @PostMapping("/borradores")
    public ConfiguracionService.Borrador crear(@Valid @RequestBody CrearBorrador input,Principal actor) {
        return configuracion.crear(input,actor.getName());
    }
    @PutMapping("/borradores/{codigo}/modelo")
    public ConfiguracionService.Borrador seleccionar(@PathVariable UUID codigo,@Valid @RequestBody SeleccionarModelo input,Principal actor) {
        return configuracion.seleccionar(codigo,input,actor.getName());
    }
    @PostMapping("/borradores/{codigo}/cancelacion/solicitar")
    public SeguridadConfiguracion.Solicitud solicitarCancelacion(@PathVariable UUID codigo,@Valid @RequestBody SolicitarCancelacion input,Principal actor) {
        return cancelacion.solicitar(codigo,input,actor.getName());
    }
    @PostMapping("/borradores/{codigo}/cancelacion/confirmar")
    public ConfiguracionService.Borrador confirmarCancelacion(@PathVariable UUID codigo,@Valid @RequestBody ConfirmarCancelacion input,Principal actor) {
        return cancelacion.confirmar(codigo,input,actor.getName());
    }
    @PostMapping("/borradores/{codigo}/cancelacion/revocar") public java.util.Map<String,String> revocarCancelacion(@PathVariable UUID codigo,@Valid @RequestBody ActivacionConfiguracionController.Revocar input,Principal actor){cancelacion.revocar(codigo,input.operacion(),actor.getName());return java.util.Map.of("mensaje","La autorización quedó invalidada.");}
    @PutMapping("/borradores/{codigo}/pagos")
    public ConfiguracionService.Borrador guardarPagos(@PathVariable UUID codigo,@Valid @RequestBody GuardarPagos input,Principal actor) {
        return pagos.guardar(codigo,input,actor.getName());
    }
    @PostMapping("/borradores/{codigo}/pagos/simular")
    public EvaluadorFinanciero.Simulacion simularPagos(@PathVariable UUID codigo,@Valid @RequestBody SimularPagos input,Principal actor) {
        return pagos.simular(codigo,input,actor.getName());
    }

    public enum Modelo { MANUAL, CONDICIONAL }
    public enum Criterio { PAGO_PREVIO, SENA, MONTO_TOTAL }
    public enum MedioPago { TRANSFERENCIA, EFECTIVO }
    public enum CondicionSena { SIEMPRE, DESDE_CARILLAS, DESDE_MONTO, SUPERAR_UMBRAL_APROBACION }
    public enum TipoSena { PORCENTAJE, FIJA }
    public record Pagos(@NotNull @Size(min=1,max=2) List<@NotNull MedioPago> medios,
                        @Size(max=2000) String instruccionesTransferencia,@NotNull @Positive Integer vigenciaCotizacionMinutos,
                        @NotNull Boolean exigirSena,CondicionSena condicionSena,@Size(max=32) String umbralSena,
                        TipoSena tipoSena,@Size(max=32) String valorSena,@Size(max=32) String umbralAprobacion) {}
    public record GuardarPagos(@NotNull UUID operacion,@NotNull @Min(1) Long version,@NotNull @Valid Pagos pagos) {}
    public record SimularPagos(@NotNull @Min(1) Long version,@NotBlank @Size(max=32) String total,@NotNull @Positive Integer carillas) {}
    public record CrearBorrador(@NotNull UUID operacion) {}
    public record SeleccionarModelo(@NotNull UUID operacion,@NotNull @Min(1) Long version,
                                    @NotNull Modelo modelo,Criterio criterio) {}
    public record SolicitarCancelacion(@NotNull UUID operacion,@NotNull @Min(1) Long version,
                                       @NotBlank @Size(max=500) String motivo,@NotBlank @Size(max=128) String contrasena) {
        @Override public String toString() { return "SolicitarCancelacion[privada]"; }
    }
    public record ConfirmarCancelacion(@NotNull UUID operacion,@NotNull @Min(1) Long version,
                                       @NotBlank @Size(max=500) String motivo,@NotBlank @Size(max=128) String contrasena,
                                       @NotBlank @Size(max=200) String codigo) {
        @Override public String toString() { return "ConfirmarCancelacion[privada]"; }
    }
}
