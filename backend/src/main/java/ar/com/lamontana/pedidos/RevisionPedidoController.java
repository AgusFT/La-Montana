//#region ENCABEZADO · RevisionPedidoController.java
/*
 * ========================================================================
 * ARCHIVO: RevisionPedidoController.java
 * ========================================================================
 * FUNCIÓN
 * Expone la revisión administrativa de pedidos: consulta de gestión, decisión, observaciones y
 * cancelación.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] RevisionPedidoController(RevisionPedidoService revision)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] RevisionPedidoService.Gestion gestion(UUID id, Principal p)
 *   Entrada HTTP GET · ruta del método: /api/operacion/pedidos/{id}/gestion.
 * - [public] PedidoService.Detalle decidir(UUID id, Decision in, Principal p)
 *   Entrada HTTP POST · ruta del método: /api/operacion/pedidos/{id}/gestion.
 * - [public] RevisionPedidoService.Gestion observar(UUID id, Observacion in, Principal p)
 *   Entrada HTTP POST · ruta del método: /api/operacion/pedidos/{id}/observaciones.
 * - [public] PedidoService.Detalle cancelar(UUID id, Cancelacion in, Principal p)
 *   Entrada HTTP POST · ruta del método: /api/cliente/pedidos/{id}/cancelar.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - RevisionPedidoController (clase).
 * - RevisionPedidoController.Accion (enumeración).
 * - RevisionPedidoController.Importancia (enumeración).
 * - RevisionPedidoController.Decision (record).
 * - RevisionPedidoController.Cancelacion (record).
 * - RevisionPedidoController.Observacion (record).
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
public class RevisionPedidoController {
 private final RevisionPedidoService revision;
 public RevisionPedidoController(RevisionPedidoService revision){this.revision=revision;}
 public enum Accion { REVISAR, APROBAR, RECHAZAR, CANCELAR }
 public enum Importancia { INFORMATIVA, RELEVANTE, CRITICA }
 public record Decision(@NotNull UUID operacion,@Min(1) long version,@NotNull Accion accion,
   boolean archivosYDatosRevisados,@NotBlank @Size(max=500) String motivo,@Size(max=500) String mensajeCliente){}
 public record Cancelacion(@NotNull UUID operacion,@Min(1) long version,@NotBlank @Size(max=500) String motivo){}
 public record Observacion(@NotNull UUID operacion,@Min(1) long version,@NotNull Importancia importancia,@NotBlank @Size(max=2000) String texto){}
 @GetMapping("/api/operacion/pedidos/{id}/gestion")
 public RevisionPedidoService.Gestion gestion(@PathVariable UUID id,Principal p){return revision.consultar(id,p.getName());}
 @PostMapping("/api/operacion/pedidos/{id}/gestion")
 public PedidoService.Detalle decidir(@PathVariable UUID id,@Valid @RequestBody Decision in,Principal p){return revision.decidir(id,in,p.getName());}
 @PostMapping("/api/operacion/pedidos/{id}/observaciones")
 public RevisionPedidoService.Gestion observar(@PathVariable UUID id,@Valid @RequestBody Observacion in,Principal p){return revision.observar(id,in,p.getName());}
 @PostMapping("/api/cliente/pedidos/{id}/cancelar")
 public PedidoService.Detalle cancelar(@PathVariable UUID id,@Valid @RequestBody Cancelacion in,Principal p){return revision.cancelar(id,in,p.getName());}
}
