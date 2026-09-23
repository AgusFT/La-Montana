package ar.com.lamontana.entregas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
@RestController
public class EntregaController {
 private final EntregaService service;
 public EntregaController(EntregaService service){this.service=service;}
 public enum Accion { PREPARAR_ENVIO, SALIR_REPARTO, LLEGAR_PUNTO }
 public record Movimiento(@NotNull UUID operacion,@Min(1) long version,@NotNull Accion accion,@NotBlank @Size(max=500) String motivo,boolean confirmado){}
 public record Emitir(@NotNull UUID operacion,@Min(1) long version){}
 public record Validar(@NotNull UUID operacion,@Min(1) long version,@NotBlank @Pattern(regexp="[A-HJ-NP-Z2-9]{8}") String codigo){@Override public String toString(){return "Validar[código privado]";}}
 public record Entregar(@NotNull UUID operacion,@Min(1) long version,@NotNull UUID validacion,@NotBlank @Size(max=140) String receptor,@NotBlank @Size(max=500) String motivo,boolean confirmado){}
 public record Cerrar(@NotNull UUID operacion,@Min(1) long version,@NotBlank @Size(max=500) String motivo,boolean confirmado){}
 @GetMapping("/api/cliente/pedidos/{pedido}/entrega") public EntregaService.Vista cliente(@PathVariable UUID pedido,Principal p){return service.consultar(pedido,p.getName(),false);}
 @GetMapping("/api/operacion/pedidos/{pedido}/entrega") public EntregaService.Vista interno(@PathVariable UUID pedido,Principal p){return service.consultar(pedido,p.getName(),true);}
 @PostMapping("/api/cliente/pedidos/{pedido}/entrega/codigo") public EntregaService.Generacion emitir(@PathVariable UUID pedido,@Valid @RequestBody Emitir in,Principal p){return service.emitir(pedido,in,p.getName());}
 @PostMapping("/api/operacion/pedidos/{pedido}/entrega/movimientos") public EntregaService.Vista mover(@PathVariable UUID pedido,@Valid @RequestBody Movimiento in,Principal p){return service.mover(pedido,in,p.getName());}
 @PostMapping("/api/operacion/pedidos/{pedido}/entrega/validar") public EntregaService.Validacion validar(@PathVariable UUID pedido,@Valid @RequestBody Validar in,Principal p){return service.validar(pedido,in,p.getName());}
 @PostMapping("/api/operacion/pedidos/{pedido}/entrega/confirmar") public EntregaService.Vista entregar(@PathVariable UUID pedido,@Valid @RequestBody Entregar in,Principal p){return service.entregar(pedido,in,p.getName());}
 @PostMapping("/api/operacion/pedidos/{pedido}/entrega/cerrar") public EntregaService.Vista cerrar(@PathVariable UUID pedido,@Valid @RequestBody Cerrar in,Principal p){return service.cerrar(pedido,in,p.getName());}
}
