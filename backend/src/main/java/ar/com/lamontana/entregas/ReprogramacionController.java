//#region ENCABEZADO · ReprogramacionController.java
/*
 * ========================================================================
 * ARCHIVO: ReprogramacionController.java
 * ========================================================================
 * FUNCIÓN
 * Expone la consulta de franjas y propuestas de reprogramación, su retiro por operación y la
 * respuesta del cliente.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] ReprogramacionController(ReprogramacionService service)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] ReprogramacionService.Vista cliente(UUID pedido, Principal p)
 *   Entrada HTTP GET · ruta del método: /api/cliente/pedidos/{pedido}/reprogramacion.
 * - [public] ReprogramacionService.Vista interno(UUID pedido, Principal p)
 *   Entrada HTTP GET · ruta del método: /api/operacion/pedidos/{pedido}/reprogramacion.
 * - [public] ReprogramacionService.Disponibilidad franjas(UUID pedido, LocalDate fecha, Principal
 *   p)
 *   Entrada HTTP GET · ruta del método: /api/operacion/pedidos/{pedido}/reprogramacion/franjas.
 * - [public] ReprogramacionService.Vista proponer(UUID pedido, Proponer in, Principal p)
 *   Entrada HTTP POST · ruta del método: /api/operacion/pedidos/{pedido}/reprogramacion.
 * - [public] ReprogramacionService.Vista retirar(UUID pedido, Retirar in, Principal p)
 *   Entrada HTTP POST · ruta del método: /api/operacion/pedidos/{pedido}/reprogramacion/retirar.
 * - [public] ReprogramacionService.Vista responder(UUID pedido, Responder in, Principal p)
 *   Entrada HTTP POST · ruta del método: /api/cliente/pedidos/{pedido}/reprogramacion/responder.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ReprogramacionController (clase).
 * - ReprogramacionController.Proponer (record).
 * - ReprogramacionController.Responder (record).
 * - ReprogramacionController.Retirar (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.entregas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.*;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
@RestController
public class ReprogramacionController {
 private final ReprogramacionService service;
 public ReprogramacionController(ReprogramacionService service){this.service=service;}
 public record Proponer(@NotNull UUID operacion,@Min(1) long version,@NotNull UUID configuracion,@NotNull LocalDate fecha,@NotNull Instant desde,@NotNull Instant hasta,@NotBlank @Size(max=500) String motivo,@NotBlank @Size(max=500) String mensajeCliente,boolean confirmado){}
 public record Responder(@NotNull UUID operacion,@Min(1) long version,@NotNull UUID propuesta,@NotNull Boolean aceptada,@NotBlank @Size(max=500) String motivo,boolean confirmado){}
 public record Retirar(@NotNull UUID operacion,@Min(1) long version,@NotNull UUID propuesta,@NotBlank @Size(max=500) String motivo,boolean confirmado){}
 @GetMapping("/api/cliente/pedidos/{pedido}/reprogramacion") public ReprogramacionService.Vista cliente(@PathVariable UUID pedido,Principal p){return service.consultar(pedido,p.getName(),false);}
 @GetMapping("/api/operacion/pedidos/{pedido}/reprogramacion") public ReprogramacionService.Vista interno(@PathVariable UUID pedido,Principal p){return service.consultar(pedido,p.getName(),true);}
 @GetMapping("/api/operacion/pedidos/{pedido}/reprogramacion/franjas") public ReprogramacionService.Disponibilidad franjas(@PathVariable UUID pedido,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fecha,Principal p){return service.disponibilidad(pedido,fecha,p.getName());}
 @PostMapping("/api/operacion/pedidos/{pedido}/reprogramacion") public ReprogramacionService.Vista proponer(@PathVariable UUID pedido,@Valid @RequestBody Proponer in,Principal p){return service.proponer(pedido,in,p.getName());}
 @PostMapping("/api/operacion/pedidos/{pedido}/reprogramacion/retirar") public ReprogramacionService.Vista retirar(@PathVariable UUID pedido,@Valid @RequestBody Retirar in,Principal p){return service.retirar(pedido,in,p.getName());}
 @PostMapping("/api/cliente/pedidos/{pedido}/reprogramacion/responder") public ReprogramacionService.Vista responder(@PathVariable UUID pedido,@Valid @RequestBody Responder in,Principal p){return service.responder(pedido,in,p.getName());}
}
