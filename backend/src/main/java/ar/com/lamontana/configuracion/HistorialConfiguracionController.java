package ar.com.lamontana.configuracion;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/configuracion/historial")
public class HistorialConfiguracionController {
 private final HistorialConfiguracionService service;
 public HistorialConfiguracionController(HistorialConfiguracionService service){this.service=service;}
 @GetMapping public HistorialConfiguracionService.Listado listar(@RequestParam(defaultValue="") String buscar,@RequestParam(defaultValue="TODOS") String estado,@RequestParam(defaultValue="RECIENTES") String orden,@RequestParam(defaultValue="0") int pagina,Principal actor){return service.listar(buscar,estado,orden,pagina,actor.getName());}
 @GetMapping("/{id}") public HistorialConfiguracionService.Detalle detalle(@PathVariable UUID id,Principal actor){return service.detalle(id,actor.getName());}
 @GetMapping("/{id}/auditoria") public HistorialConfiguracionService.Pagina<HistorialConfiguracionService.Evento> auditoria(@PathVariable UUID id,@RequestParam(defaultValue="0") int pagina,Principal actor){return service.auditoria(id,pagina,actor.getName());}
 @GetMapping("/{id}/comparacion/{destino}") public HistorialConfiguracionService.Comparacion comparar(@PathVariable UUID id,@PathVariable UUID destino,Principal actor){return service.comparar(id,destino,actor.getName());}
}
