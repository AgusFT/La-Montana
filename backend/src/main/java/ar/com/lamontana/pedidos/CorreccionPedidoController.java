//#region ENCABEZADO · CorreccionPedidoController.java
/*
 * ========================================================================
 * ARCHIVO: CorreccionPedidoController.java
 * ========================================================================
 * FUNCIÓN
 * Expone solicitudes de corrección de pedidos, preparación de respuestas del cliente y creación de
 * cotizaciones para los cambios solicitados.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] CorreccionPedidoController(CorreccionPedidoService correcciones)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] CorreccionPedidoService.Vista listar(UUID id, Principal p, HttpServletRequest req)
 *   Entrada HTTP GET · ruta del método: /api/cliente/pedidos/{id}/correcciones,
 *   /api/operacion/pedidos/{id}/correcciones.
 * - [public] PedidoService.Detalle solicitar(UUID id, Solicitar in, Principal p)
 *   Entrada HTTP POST · ruta del método: /api/operacion/pedidos/{id}/correcciones.
 * - [public] CorreccionPedidoService.Preparacion preparar(UUID id, Principal p)
 *   Entrada HTTP GET · ruta del método: /api/cliente/pedidos/{id}/correcciones/respuesta.
 * - [public] PedidoService.Detalle responder(UUID id, Responder in, Principal p)
 *   Entrada HTTP POST · ruta del método: /api/cliente/pedidos/{id}/correcciones/respuesta.
 * - [public] CotizacionService.Detalle cotizar(UUID id, UUID solicitud, CotizacionController.Crear
 *   in, Principal p)
 *   Entrada HTTP POST · ruta del método:
 *   /api/cliente/pedidos/{id}/correcciones/{solicitud}/cotizaciones.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - CorreccionPedidoController (clase).
 * - CorreccionPedidoController.Tipo (enumeración).
 * - CorreccionPedidoController.Solicitar (record).
 * - CorreccionPedidoController.Responder (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.pedidos;

import ar.com.lamontana.cotizaciones.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class CorreccionPedidoController {
 private final CorreccionPedidoService correcciones;
 public CorreccionPedidoController(CorreccionPedidoService correcciones){this.correcciones=correcciones;}
 public enum Tipo { ARCHIVO,DATOS,ENTREGA,CANTIDAD,OTRO }
 public record Solicitar(@NotNull UUID operacion,@Min(1) long version,@NotNull Tipo tipo,
  @NotBlank @Size(max=500) String motivo,@NotBlank @Size(max=2000) String mensajeCliente,
  @NotNull @Size(max=20) List<@NotNull UUID> items){}
 public record Responder(@NotNull UUID operacion,@Min(1) long version,@NotNull UUID solicitud,
  @NotBlank @Size(max=2000) String mensaje,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String huella,
  @AssertTrue boolean condicionesAceptadas,@Valid PedidoController.Contacto contacto){}
 @GetMapping({"/api/cliente/pedidos/{id}/correcciones","/api/operacion/pedidos/{id}/correcciones"})
 public CorreccionPedidoService.Vista listar(@PathVariable UUID id,Principal p,HttpServletRequest req){return correcciones.consultar(id,p.getName(),req.getRequestURI().startsWith("/api/operacion/"));}
 @PostMapping("/api/operacion/pedidos/{id}/correcciones")
 public PedidoService.Detalle solicitar(@PathVariable UUID id,@Valid @RequestBody Solicitar in,Principal p){return correcciones.solicitar(id,in,p.getName());}
 @GetMapping("/api/cliente/pedidos/{id}/correcciones/respuesta")
 public CorreccionPedidoService.Preparacion preparar(@PathVariable UUID id,Principal p){return correcciones.preparar(id,p.getName());}
 @PostMapping("/api/cliente/pedidos/{id}/correcciones/respuesta")
 public PedidoService.Detalle responder(@PathVariable UUID id,@Valid @RequestBody Responder in,Principal p){return correcciones.responder(id,in,p.getName());}
 @PostMapping("/api/cliente/pedidos/{id}/correcciones/{solicitud}/cotizaciones")
 public CotizacionService.Detalle cotizar(@PathVariable UUID id,@PathVariable UUID solicitud,@Valid @RequestBody CotizacionController.Crear in,Principal p){return correcciones.cotizar(id,solicitud,in,p.getName());}
}
