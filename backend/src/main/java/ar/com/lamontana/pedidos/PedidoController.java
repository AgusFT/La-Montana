//#region ENCABEZADO · PedidoController.java
/*
 * ========================================================================
 * ARCHIVO: PedidoController.java
 * ========================================================================
 * FUNCIÓN
 * Expone la revisión previa y confirmación de pedidos desde cotizaciones, y consultas de pedidos
 * para clientes y personal de sucursales.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] PedidoController(PedidoService pedidos)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] PedidoService.Revision revisar(UUID id, Principal p)
 *   Entrada HTTP GET · ruta del método: /api/cliente/cotizaciones/{id}/confirmacion.
 * - [public] PedidoService.Detalle confirmar(UUID id, Confirmar in, Principal p)
 *   Entrada HTTP POST · ruta del método: /api/cliente/cotizaciones/{id}/confirmacion.
 * - [public] PedidoService.Pagina listar(int pagina, String estado, Principal p)
 *   Entrada HTTP GET · ruta del método: /api/cliente/pedidos.
 * - [public] PedidoService.Detalle cliente(UUID id, Principal p)
 *   Entrada HTTP GET · ruta del método: /api/cliente/pedidos/{id}.
 * - [public] PedidoService.Detalle interno(UUID id, Principal p)
 *   Entrada HTTP GET · ruta del método: /api/operacion/pedidos/{id}.
 * - [public] PedidoService.Pagina sucursal(UUID id, int pagina, String estado, Principal p)
 *   Entrada HTTP GET · ruta del método: /api/operacion/sucursales/{id}/pedidos.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PedidoController (clase).
 * - PedidoController.Contacto (record).
 * - PedidoController.Confirmar (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.pedidos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
public class PedidoController {
 private final PedidoService pedidos;
 public PedidoController(PedidoService pedidos){this.pedidos=pedidos;}
 public record Contacto(@NotBlank @Size(max=140) String receptor,@NotBlank @Size(max=40) String telefono){}
 public record Confirmar(@NotNull UUID operacion,@Min(1) long version,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String huella,@AssertTrue boolean condicionesAceptadas,@Valid Contacto contacto){}
 @GetMapping("/api/cliente/cotizaciones/{id}/confirmacion") public PedidoService.Revision revisar(@PathVariable UUID id,Principal p){return pedidos.revisar(id,p.getName());}
 @PostMapping("/api/cliente/cotizaciones/{id}/confirmacion") public PedidoService.Detalle confirmar(@PathVariable UUID id,@Valid @RequestBody Confirmar in,Principal p){return pedidos.confirmar(id,in,p.getName());}
 @GetMapping("/api/cliente/pedidos") public PedidoService.Pagina listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(required=false) String estado,Principal p){return pedidos.listar(null,pagina,estado,p.getName());}
 @GetMapping("/api/cliente/pedidos/{id}") public PedidoService.Detalle cliente(@PathVariable UUID id,Principal p){return pedidos.detalle(id,p.getName(),false);}
 @GetMapping("/api/operacion/pedidos/{id}") public PedidoService.Detalle interno(@PathVariable UUID id,Principal p){return pedidos.detalle(id,p.getName(),true);}
 @GetMapping("/api/operacion/sucursales/{id}/pedidos") public PedidoService.Pagina sucursal(@PathVariable UUID id,@RequestParam(defaultValue="0") int pagina,@RequestParam(required=false) String estado,Principal p){return pedidos.listar(id,pagina,estado,p.getName());}
}
