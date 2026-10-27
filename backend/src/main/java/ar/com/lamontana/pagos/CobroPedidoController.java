package ar.com.lamontana.pagos;

import ar.com.lamontana.pedidos.PedidoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/** Resoluciones excepcionales exclusivas del administrador; no acredita dinero ni elude revisión. */
@RestController
public class CobroPedidoController {
    private final PedidoService pedidos;private final CobroPedidoService cobro;
    public CobroPedidoController(PedidoService pedidos,CobroPedidoService cobro){this.pedidos=pedidos;this.cobro=cobro;}
    public record Resolver(@NotNull UUID operacion,@Positive long version,@NotNull CobroPedidoService.Accion accion,
        Instant nuevoLimite,@NotBlank @Size(max=2000) String motivo){}
    @PostMapping("/api/operacion/pedidos/{pedido}/cobro/resolver")
    public CobroPedidoService.Vista resolver(@PathVariable UUID pedido,@Valid @RequestBody Resolver in,Principal actor){
        var p=pedidos.detalle(pedido,actor.getName(),true);
        return cobro.resolver(p.numero(),in.operacion(),in.version(),in.accion(),in.nuevoLimite(),in.motivo(),actor.getName());
    }
}
