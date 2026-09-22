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
 @GetMapping("/api/cliente/pedidos") public PedidoService.Pagina listar(@RequestParam(defaultValue="0") int pagina,Principal p){return pedidos.listar(null,pagina,p.getName());}
 @GetMapping("/api/cliente/pedidos/{id}") public PedidoService.Detalle cliente(@PathVariable UUID id,Principal p){return pedidos.detalle(id,p.getName(),false);}
 @GetMapping("/api/operacion/pedidos/{id}") public PedidoService.Detalle interno(@PathVariable UUID id,Principal p){return pedidos.detalle(id,p.getName(),true);}
 @GetMapping("/api/operacion/sucursales/{id}/pedidos") public PedidoService.Pagina sucursal(@PathVariable UUID id,@RequestParam(defaultValue="0") int pagina,Principal p){return pedidos.listar(id,pagina,p.getName());}
}
