package ar.com.lamontana.puntos;
import ar.com.lamontana.entregas.EntregaController.*;
import ar.com.lamontana.entregas.EntregaService;
import ar.com.lamontana.pagos.PagoService.DestinoCobro;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class PuntoRetiroController {
 private final PuntoRetiroService service;
 public PuntoRetiroController(PuntoRetiroService service){this.service=service;}
 public record Alta(@NotNull UUID operacion,@NotBlank @Pattern(regexp="[a-z0-9][a-z0-9._-]{2,79}") String usuario,@NotBlank @Email @Size(max=254) String correo,@NotBlank @Size(max=100) String responsable,@NotBlank @Size(min=12,max=128) String contrasena,boolean cobros){@Override public String toString(){return "Alta[credenciales privadas]";}}
 public record Cambio(@NotNull UUID operacion,@Min(0) long version,@NotBlank @Size(max=100) String responsable,boolean activa,boolean cobros){}
 public record Buscar(@NotBlank @Pattern(regexp="POR_LLEGAR|DISPONIBLES|ENTREGADOS") String estado,@Size(max=160) String texto,@Min(0) int pagina){@Override public String toString(){return "Buscar[consulta privada]";}}
 public record Recepcion(@NotNull UUID operacion,@NotEmpty @Size(max=25) List<@NotNull @Valid PedidoLote> pedidos){}
 public record Cobro(@NotNull UUID operacion,@Min(1) long version,@NotNull MedioPago medio,@NotBlank @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String importe,@Size(max=160) String referencia,boolean confirmado,DestinoCobro destino){
  public Cobro(UUID operacion,long version,MedioPago medio,String importe,String referencia,boolean confirmado){
   this(operacion,version,medio,importe,referencia,confirmado,null);
  }
 }
 public record CobrarEntrega(@NotNull UUID operacion,@Min(1) long version,
  @NotBlank @Pattern(regexp="[A-HJ-NP-Z2-9]{8}") String codigo,@NotNull MedioPago medio,
  @NotBlank @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String importe,
  DestinoCobro destino,@Size(max=160) String referencia,@Size(max=140) String receptor,boolean confirmado){
  @Override public String toString(){return "CobrarEntrega[código privado]";}
 }
 @GetMapping("/api/admin/puntos-entrega/cuentas") public List<PuntoRetiroService.Punto> cuentas(Principal p){return service.cuentas(p.getName());}
 @PostMapping("/api/admin/puntos-entrega/cuentas/{punto}") public PuntoRetiroService.Punto crear(@PathVariable UUID punto,@Valid @RequestBody Alta in,Principal p){return service.crear(punto,in,p.getName());}
 @PutMapping("/api/admin/puntos-entrega/cuentas/{punto}") public PuntoRetiroService.Punto cambiar(@PathVariable UUID punto,@Valid @RequestBody Cambio in,Principal p){return service.cambiar(punto,in,p.getName());}
 @GetMapping("/api/punto/contexto") public PuntoRetiroService.Contexto contexto(Principal p){return service.contexto(p.getName());}
 @PostMapping("/api/punto/pedidos/buscar") public PuntoRetiroService.Bandeja buscar(@Valid @RequestBody Buscar in,Principal p){return service.buscar(in,p.getName());}
 @GetMapping("/api/punto/pedidos/{pedido}") public PuntoRetiroService.Ficha pedido(@PathVariable UUID pedido,Principal p){return service.ficha(pedido,p.getName());}
 @PostMapping("/api/punto/recibir") public PuntoRetiroService.Bandeja recibir(@Valid @RequestBody Recepcion in,Principal p){return service.recibir(in,p.getName());}
 @PostMapping("/api/punto/pedidos/{pedido}/entregar") public EntregaService.Validacion entregar(@PathVariable UUID pedido,@Valid @RequestBody Recibir in,Principal p){return service.entregar(pedido,in,p.getName());}
 @PostMapping("/api/punto/pedidos/{pedido}/efectivo-entrega") public EntregaService.Validacion efectivo(@PathVariable UUID pedido,@Valid @RequestBody CobrarEntregar in,Principal p){return service.efectivo(pedido,in,p.getName());}
 @PostMapping("/api/punto/pedidos/{pedido}/cobro-entrega") public EntregaService.Validacion cobrarEntrega(@PathVariable UUID pedido,@Valid @RequestBody CobrarEntrega in,Principal p){return service.cobrarEntrega(pedido,in,p.getName());}
 @PostMapping("/api/punto/pedidos/{pedido}/cobrar") public PuntoRetiroService.Ficha cobrar(@PathVariable UUID pedido,@Valid @RequestBody Cobro in,Principal p){return service.cobrar(pedido,in,p.getName());}
 @GetMapping("/api/punto/finanzas") public PuntoRetiroService.Finanzas finanzas(@RequestParam(defaultValue="") String desde,@RequestParam(defaultValue="") String hasta,@RequestParam(defaultValue="0") int pagina,Principal p){return service.finanzas(desde,hasta,pagina,p.getName());}
}
