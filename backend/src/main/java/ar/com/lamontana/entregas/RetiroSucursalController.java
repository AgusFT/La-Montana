package ar.com.lamontana.entregas;

import ar.com.lamontana.pagos.PagoController;
import ar.com.lamontana.pagos.PagoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operacion/sucursales/{sucursal}/retiros")
public class RetiroSucursalController {
    private final RetiroSucursalService retiros;
    private final EntregaService entregas;

    public RetiroSucursalController(RetiroSucursalService retiros, EntregaService entregas) {
        this.retiros = retiros;
        this.entregas = entregas;
    }

    public record Llegada(@NotNull UUID operacion, @Min(1) long version, boolean confirmado) {}

    @GetMapping
    public RetiroSucursalService.Pagina buscar(@PathVariable UUID sucursal,
            @RequestParam(defaultValue="PARA_RETIRAR") RetiroSucursalService.Bandeja bandeja,
            @RequestParam(defaultValue="") String texto, @RequestParam(defaultValue="0") int pagina, Principal p) {
        return retiros.buscar(sucursal, bandeja, texto, pagina, p.getName());
    }

    @GetMapping("/{pedido}")
    public EntregaService.Vista detalle(@PathVariable UUID sucursal, @PathVariable UUID pedido, Principal p) {
        return entregas.consultarRetiro(sucursal, pedido, p.getName());
    }

    @GetMapping("/{pedido}/pagos")
    public PagoService.Vista pagos(@PathVariable UUID sucursal,@PathVariable UUID pedido,Principal p){
        return retiros.pagos(sucursal,pedido,p.getName());
    }

    @PostMapping("/{pedido}/pagos/recibir")
    public PagoService.Vista cobrarSaldo(@PathVariable UUID sucursal,@PathVariable UUID pedido,
            @Valid @RequestBody PagoController.Recibir in,Principal p){
        return retiros.cobrar(sucursal,pedido,in,p.getName());
    }

    @PostMapping("/{pedido}/llegada")
    public EntregaService.Vista llegada(@PathVariable UUID sucursal, @PathVariable UUID pedido,
            @Valid @RequestBody Llegada in, Principal p) {
        return entregas.recibirEnSucursal(sucursal, pedido, in.operacion(), in.version(), in.confirmado(), p.getName());
    }

    @PostMapping("/{pedido}/entregar")
    public EntregaService.Validacion entregar(@PathVariable UUID sucursal, @PathVariable UUID pedido,
            @Valid @RequestBody EntregaController.Recibir in, Principal p) {
        return entregas.entregarRetiro(sucursal, pedido, in, p.getName());
    }

    @PostMapping("/{pedido}/cobrar-efectivo")
    public EntregaService.Validacion cobrar(@PathVariable UUID sucursal, @PathVariable UUID pedido,
            @Valid @RequestBody EntregaController.CobrarEntregar in, Principal p) {
        return entregas.cobrarRetiro(sucursal, pedido, in, p.getName());
    }
}
