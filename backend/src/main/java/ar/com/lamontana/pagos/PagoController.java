//#region ENCABEZADO · PagoController.java
/*
 * ========================================================================
 * ARCHIVO: PagoController.java
 * ========================================================================
 * FUNCIÓN
 * Expone consultas financieras y acciones para informar, descartar, recibir, aplicar y devolver
 * pagos de cotizaciones y pedidos.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] PagoController(PagoService pagos)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] PagoService.Vista vista(UUID quote, Principal actor, HttpServletRequest req)
 *   Entrada HTTP GET · ruta del método: /api/cliente/cotizaciones/{quote}/pagos,
 *   /api/operacion/cotizaciones/{quote}/pagos.
 * - [paquete] PagoService.Bandeja bandeja(UUID branch, int pagina, Principal actor)
 *   Entrada HTTP GET · ruta del método: /api/operacion/sucursales/{branch}/pagos.
 * - [paquete] PagoService.Vista informar(UUID quote, Informar in, Principal actor)
 *   Entrada HTTP POST · ruta del método: /api/cliente/cotizaciones/{quote}/pagos/informar.
 * - [paquete] PagoService.Vista descartar(UUID quote, Descartar in, Principal actor,
 *   HttpServletRequest req)
 *   Entrada HTTP POST · ruta del método: /api/cliente/cotizaciones/{quote}/pagos/descartar,
 *   /api/operacion/cotizaciones/{quote}/pagos/descartar.
 * - [paquete] PagoService.Vista recibir(UUID quote, Recibir in, Principal actor)
 *   Entrada HTTP POST · ruta del método: /api/operacion/cotizaciones/{quote}/pagos/recibir.
 * - [paquete] PagoService.Vista aplicar(UUID quote, Aplicar in, Principal actor)
 *   Entrada HTTP POST · ruta del método: /api/operacion/cotizaciones/{quote}/pagos/aplicar.
 * - [paquete] PagoService.Vista devolver(UUID quote, Devolver in, Principal actor)
 *   Entrada HTTP POST · ruta del método: /api/operacion/cotizaciones/{quote}/pagos/devolver.
 * - [private] boolean interno(HttpServletRequest req)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PagoController (clase).
 * - PagoController.Informar (record).
 * - PagoController.Descartar (record).
 * - PagoController.Recibir (record).
 * - PagoController.Aplicar (record).
 * - PagoController.Devolver (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.pagos;

import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
public class PagoController {
    private final PagoService pagos;
    public PagoController(PagoService pagos){this.pagos=pagos;}
    public record Informar(@NotNull UUID operacion,@NotBlank @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String importe,
        @NotBlank @Size(max=160) String referencia,@NotBlank @Size(max=300) String motivo){}
    public record Descartar(@NotNull UUID operacion,@NotNull UUID intento,@NotBlank @Size(max=300) String motivo){}
    public record Recibir(@NotNull UUID operacion,UUID intento,@NotNull MedioPago medio,
        @NotBlank @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String importe,@NotBlank @Size(max=160) String referencia,
        @NotNull @PastOrPresent Instant recibidoEn,@AssertTrue boolean verificado,@NotBlank @Size(max=300) String motivo){}
    public record Aplicar(@NotNull UUID operacion,@NotNull UUID pago,@Min(1) long version,
        @NotBlank @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String importe,@NotBlank @Size(max=300) String motivo){}
    public record Devolver(@NotNull UUID operacion,@NotNull UUID pago,@Min(1) long version,@NotNull MedioPago medio,
        @NotBlank @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String importe,@NotBlank @Size(max=160) String referencia,
        @NotNull @PastOrPresent Instant devueltoEn,@AssertTrue boolean verificado,@NotBlank @Size(max=300) String motivo){}
    @GetMapping({"/api/cliente/cotizaciones/{quote}/pagos","/api/operacion/cotizaciones/{quote}/pagos"})
    PagoService.Vista vista(@PathVariable UUID quote,Principal actor,HttpServletRequest req){return pagos.vista(quote,actor.getName(),interno(req));}
    @GetMapping("/api/operacion/sucursales/{branch}/pagos")
    PagoService.Bandeja bandeja(@PathVariable UUID branch,@RequestParam(defaultValue="0") int pagina,Principal actor){return pagos.bandeja(branch,pagina,actor.getName());}
    @PostMapping("/api/cliente/cotizaciones/{quote}/pagos/informar")
    PagoService.Vista informar(@PathVariable UUID quote,@Valid @RequestBody Informar in,Principal actor){return pagos.informar(quote,in,actor.getName());}
    @PostMapping({"/api/cliente/cotizaciones/{quote}/pagos/descartar","/api/operacion/cotizaciones/{quote}/pagos/descartar"})
    PagoService.Vista descartar(@PathVariable UUID quote,@Valid @RequestBody Descartar in,Principal actor,HttpServletRequest req){return pagos.descartar(quote,in,actor.getName(),interno(req));}
    @PostMapping("/api/operacion/cotizaciones/{quote}/pagos/recibir")
    PagoService.Vista recibir(@PathVariable UUID quote,@Valid @RequestBody Recibir in,Principal actor){return pagos.recibir(quote,in,actor.getName());}
    @PostMapping("/api/operacion/cotizaciones/{quote}/pagos/aplicar")
    PagoService.Vista aplicar(@PathVariable UUID quote,@Valid @RequestBody Aplicar in,Principal actor){return pagos.aplicar(quote,in,actor.getName());}
    @PostMapping("/api/operacion/cotizaciones/{quote}/pagos/devolver")
    PagoService.Vista devolver(@PathVariable UUID quote,@Valid @RequestBody Devolver in,Principal actor){return pagos.devolver(quote,in,actor.getName());}
    private boolean interno(HttpServletRequest req){return req.getRequestURI().startsWith("/api/operacion/");}
}
