//#region ENCABEZADO · ProduccionController.java
/*
 * ========================================================================
 * ARCHIVO: ProduccionController.java
 * ========================================================================
 * FUNCIÓN
 * Expone la consulta de producción, inicio y cambio de trabajos de impresión y registro del
 * control de calidad.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] ProduccionController(ProduccionService service)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] Checklist :: boolean completo()
 * - [public] ProduccionService.Vista consultar(UUID pedido, Principal p)
 *   Entrada HTTP GET sobre la ruta del controlador.
 * - [public] ProduccionService.Vista iniciar(UUID pedido, Inicio in, Principal p)
 *   Entrada HTTP POST · ruta del método: /iniciar.
 * - [public] ProduccionService.Vista cambiar(UUID pedido, UUID trabajo, Cambio in, Principal p)
 *   Entrada HTTP POST · ruta del método: /trabajos/{trabajo}/estado.
 * - [public] ProduccionService.Vista calidad(UUID pedido, UUID trabajo, Inspeccion in, Principal
 *   p)
 *   Entrada HTTP POST · ruta del método: /trabajos/{trabajo}/calidad.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ProduccionController (clase).
 * - ProduccionController.Accion (enumeración).
 * - ProduccionController.Resultado (enumeración).
 * - ProduccionController.Inicio (record).
 * - ProduccionController.Cambio (record).
 * - ProduccionController.Checklist (record).
 * - ProduccionController.Inspeccion (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.produccion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operacion/pedidos/{pedido}/produccion")
public class ProduccionController {
 private final ProduccionService service;
 public ProduccionController(ProduccionService service){this.service=service;}
 public enum Accion { COMPLETAR, ERROR, CANCELAR }
 public enum Resultado { APROBADO, REIMPRESION_REQUERIDA, INCIDENCIA }
 public record Inicio(@NotNull UUID operacion,@Min(1) long version,@NotNull UUID item,UUID impresora,
   @NotBlank @Size(max=500) String motivo,boolean confirmacionManual){}
 public record Cambio(@NotNull UUID operacion,@Min(1) long version,@NotNull Accion accion,
   @NotBlank @Size(max=500) String motivo,boolean confirmacionManual){}
 public record Checklist(boolean impresionCompleta,boolean calidadCorrecta,boolean alineacionCorrecta,boolean ordenCorrecto,boolean terminacionesCorrectas){
  boolean completo(){return impresionCompleta&&calidadCorrecta&&alineacionCorrecta&&ordenCorrecto&&terminacionesCorrectas;}
 }
 public record Inspeccion(@NotNull UUID operacion,@Min(1) long version,@NotNull Resultado resultado,
   @NotNull @Valid Checklist checklist,@NotBlank @Size(max=500) String observaciones,boolean confirmacionManual){}
 @GetMapping public ProduccionService.Vista consultar(@PathVariable UUID pedido,Principal p){return service.consultar(pedido,p.getName());}
 @PostMapping("/iniciar") public ProduccionService.Vista iniciar(@PathVariable UUID pedido,@Valid @RequestBody Inicio in,Principal p){return service.iniciar(pedido,in,p.getName());}
 @PostMapping("/trabajos/{trabajo}/estado") public ProduccionService.Vista cambiar(@PathVariable UUID pedido,@PathVariable UUID trabajo,@Valid @RequestBody Cambio in,Principal p){return service.cambiar(pedido,trabajo,in,p.getName());}
 @PostMapping("/trabajos/{trabajo}/calidad") public ProduccionService.Vista calidad(@PathVariable UUID pedido,@PathVariable UUID trabajo,@Valid @RequestBody Inspeccion in,Principal p){return service.calidad(pedido,trabajo,in,p.getName());}
}
