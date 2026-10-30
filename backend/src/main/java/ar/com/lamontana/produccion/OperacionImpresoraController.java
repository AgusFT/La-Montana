package ar.com.lamontana.produccion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operacion/impresoras")
public class OperacionImpresoraController {
 private final OperacionImpresoraService service;
 public OperacionImpresoraController(OperacionImpresoraService service){this.service=service;}
 public enum Accion { HABILITAR, DESHABILITAR, RECARGAR, CONTAR }
 public record Cambio(@NotNull UUID operacion,@PositiveOrZero long version,@NotNull Accion accion,@Min(0) @Max(1000000) Integer hojas,@Size(max=500) String observaciones){}
 @GetMapping public OperacionImpresoraService.Panel listar(Principal p){return service.listar(p.getName());}
 @PostMapping("/{impresora}") public OperacionImpresoraService.Estado cambiar(@PathVariable UUID impresora,@Valid @RequestBody Cambio in,Principal p){return service.cambiar(impresora,in,p.getName());}
 @GetMapping("/{impresora}/historial") public OperacionImpresoraService.Historial historial(@PathVariable UUID impresora,@RequestParam(defaultValue="0") int pagina,Principal p){return service.historial(impresora,pagina,p.getName());}
}
