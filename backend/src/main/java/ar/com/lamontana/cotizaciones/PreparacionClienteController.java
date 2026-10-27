package ar.com.lamontana.cotizaciones;

import ar.com.lamontana.archivos.*;
import ar.com.lamontana.pedidos.PedidoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cliente/preparaciones")
public class PreparacionClienteController {
    private final PreparacionClienteService preparaciones;private final ArchivoPreparadoService archivos;
    public PreparacionClienteController(PreparacionClienteService preparaciones,ArchivoPreparadoService archivos){this.preparaciones=preparaciones;this.archivos=archivos;}
    public record ArchivoNuevo(@NotNull UUID operacion,@NotBlank @Size(max=255) String nombre){}
    public record Operacion(@NotNull UUID operacion){}
    @PostMapping public PreparacionClienteService.Vista abrir(Principal p){return preparaciones.abrir(p.getName());}
    @GetMapping("/{id}") public PreparacionClienteService.Vista vista(@PathVariable UUID id,Principal p){return preparaciones.consultar(id,p.getName());}
    @PostMapping("/{id}/presupuesto") public PreparacionClienteService.Vista presupuesto(@PathVariable UUID id,@Valid @RequestBody PreparacionClienteService.Presupuestar in,Principal p){return preparaciones.presupuestar(id,in,p.getName());}
    @GetMapping("/{id}/resumen") public PreparacionClienteService.Checkout resumen(@PathVariable UUID id,Principal p){return preparaciones.checkout(id,p.getName());}
    @PostMapping("/{id}/cotizar") public PreparacionClienteService.Checkout cotizar(@PathVariable UUID id,@Valid @RequestBody PreparacionClienteService.Cotizar in,Principal p){return preparaciones.cotizar(id,in,p.getName());}
    @PostMapping("/{id}/confirmar") public PedidoService.Detalle confirmar(@PathVariable UUID id,@Valid @RequestBody PreparacionClienteService.Confirmar in,Principal p){return preparaciones.confirmar(id,in,p.getName());}
    @GetMapping("/{id}/archivos") public ArchivoService.Vista archivos(@PathVariable UUID id,Principal p){return archivos.vista(id,p.getName());}
    @GetMapping("/{id}/archivos/{file}") public ArchivoService.Archivo archivo(@PathVariable UUID id,@PathVariable UUID file,Principal p){return archivos.archivo(id,file,p.getName());}
    @GetMapping("/{id}/archivos/{file}/carga") public Object carga(@PathVariable UUID id,@PathVariable UUID file,Principal p){var a=archivos.archivo(id,file,p.getName());return java.util.Map.of("habilitada",a.activo()&&a.estado().equals("PENDIENTE"),"motivo","Sólo se cargan PDF pendientes de un trabajo abierto.");}
    @PostMapping("/{id}/archivos") public ArchivoService.Archivo iniciar(@PathVariable UUID id,@Valid @RequestBody ArchivoNuevo in,Principal p){return archivos.crear(id,in.operacion(),in.nombre(),p.getName());}
    @PostMapping("/{id}/archivos/{file}/quitar") public PreparacionClienteService.Vista quitar(@PathVariable UUID id,@PathVariable UUID file,@Valid @RequestBody Operacion in,Principal p){archivos.quitar(id,file,in.operacion(),p.getName());return preparaciones.consultar(id,p.getName());}
    @PostMapping("/{id}/archivos/{file}/aceptar") public ArchivoService.Archivo aceptar(@PathVariable UUID id,@PathVariable UUID file,@Valid @RequestBody ArchivoController.Aceptacion in,Principal p){return archivos.aceptar(id,file,in.operacion(),in.sha256(),p.getName());}
    @PutMapping(value="/{id}/archivos/{file}/contenido",consumes="application/pdf") public ArchivoService.Archivo cargar(@PathVariable UUID id,@PathVariable UUID file,HttpServletRequest r,Principal p)throws IOException{return archivos.recibir(id,file,p.getName(),r.getInputStream(),r.getContentLengthLong());}
    @GetMapping("/{id}/archivos/{file}/original") public ResponseEntity<byte[]> original(@PathVariable UUID id,@PathVariable UUID file,Principal p){return contenido(archivos.contenido(id,file,null,p.getName()),true);}
    @GetMapping("/{id}/archivos/{file}/paginas/{page}") public ResponseEntity<byte[]> pagina(@PathVariable UUID id,@PathVariable UUID file,@PathVariable int page,Principal p){return contenido(archivos.contenido(id,file,page,p.getName()),false);}
    private ResponseEntity<byte[]> contenido(ArchivoService.Contenido c,boolean descarga){var h=new HttpHeaders();h.setContentType(MediaType.parseMediaType(c.tipo()));h.setCacheControl("no-store, private");h.set("X-Content-Type-Options","nosniff");h.set("Content-Security-Policy","default-src 'none'; sandbox");h.setContentDisposition((descarga?ContentDisposition.attachment():ContentDisposition.inline()).filename(c.nombre(),StandardCharsets.UTF_8).build());return new ResponseEntity<>(c.bytes(),h,HttpStatus.OK);}
}
