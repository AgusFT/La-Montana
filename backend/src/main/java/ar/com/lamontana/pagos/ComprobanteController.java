//#region ENCABEZADO · ComprobanteController.java
/*
 * ========================================================================
 * ARCHIVO: ComprobanteController.java
 * ========================================================================
 * FUNCIÓN
 * Expone carga, consulta, descarga y vista previa de comprobantes PDF privados vinculados a
 * intentos o pagos, respetando el acceso de cliente y operación.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] ComprobanteController(ComprobanteService comprobantes)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] ComprobanteService.Lista listar(UUID quote, UUID intento, UUID pago, int pagina,
 *   Principal actor, HttpServletRequest req)
 *   Entrada HTTP GET sobre la ruta del controlador.
 * - [public] ComprobanteService.Archivo crear(UUID quote, Inicio in, Principal actor,
 *   HttpServletRequest req)
 *   Entrada HTTP POST sobre la ruta del controlador.
 * - [public] ComprobanteService.Archivo consultar(UUID quote, UUID file, Principal actor,
 *   HttpServletRequest req)
 *   Entrada HTTP GET · ruta del método: /{file}.
 * - [public] ComprobanteService.Carga carga(UUID quote, UUID file, Principal actor,
 *   HttpServletRequest req)
 *   Entrada HTTP GET · ruta del método: /{file}/carga.
 * - [public] ComprobanteService.Archivo recibir(UUID quote, UUID file, Principal actor,
 *   HttpServletRequest req) throws IOException
 *   Entrada HTTP PUT · ruta del método: /{file}/contenido.
 * - [public] ResponseEntity<byte[]> original(UUID quote, UUID file, Principal actor,
 *   HttpServletRequest req)
 *   Entrada HTTP GET · ruta del método: /{file}/original.
 * - [public] ResponseEntity<byte[]> preview(UUID quote, UUID file, int page, Principal actor,
 *   HttpServletRequest req)
 *   Entrada HTTP GET · ruta del método: /{file}/paginas/{page}.
 * - [private] boolean interno(HttpServletRequest req)
 * - [private] ResponseEntity<byte[]> contenido(ArchivoService.Contenido c, boolean download)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ComprobanteController (clase).
 * - ComprobanteController.Inicio (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.pagos;

import ar.com.lamontana.archivos.ArchivoService;
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
@RequestMapping({"/api/cliente/cotizaciones/{quote}/comprobantes","/api/operacion/cotizaciones/{quote}/comprobantes"})
public class ComprobanteController {
    private final ComprobanteService comprobantes;
    public ComprobanteController(ComprobanteService comprobantes){this.comprobantes=comprobantes;}
    public record Inicio(@NotNull UUID operacion,UUID intento,UUID pago,@NotBlank @Size(max=255) String nombre,@NotBlank @Size(max=300) String detalle){}
    @GetMapping public ComprobanteService.Lista listar(@PathVariable UUID quote,@RequestParam(required=false)UUID intento,@RequestParam(required=false)UUID pago,@RequestParam(defaultValue="0")int pagina,Principal actor,HttpServletRequest req){return comprobantes.listar(quote,intento,pago,pagina,actor.getName(),interno(req));}
    @PostMapping public ComprobanteService.Archivo crear(@PathVariable UUID quote,@Valid @RequestBody Inicio in,Principal actor,HttpServletRequest req){return comprobantes.crear(quote,in,actor.getName(),interno(req));}
    @GetMapping("/{file}") public ComprobanteService.Archivo consultar(@PathVariable UUID quote,@PathVariable UUID file,Principal actor,HttpServletRequest req){return comprobantes.consultar(quote,file,actor.getName(),interno(req));}
    @GetMapping("/{file}/carga") public ComprobanteService.Carga carga(@PathVariable UUID quote,@PathVariable UUID file,Principal actor,HttpServletRequest req){return comprobantes.puedeEnviar(quote,file,actor.getName(),interno(req));}
    @PutMapping(value="/{file}/contenido",consumes="application/pdf") public ComprobanteService.Archivo recibir(@PathVariable UUID quote,@PathVariable UUID file,Principal actor,HttpServletRequest req)throws IOException{return comprobantes.recibir(quote,file,actor.getName(),interno(req),req.getInputStream(),req.getContentLengthLong());}
    @GetMapping("/{file}/original") public ResponseEntity<byte[]> original(@PathVariable UUID quote,@PathVariable UUID file,Principal actor,HttpServletRequest req){return contenido(comprobantes.contenido(quote,file,null,actor.getName(),interno(req)),true);}
    @GetMapping("/{file}/paginas/{page}") public ResponseEntity<byte[]> preview(@PathVariable UUID quote,@PathVariable UUID file,@PathVariable int page,Principal actor,HttpServletRequest req){return contenido(comprobantes.contenido(quote,file,page,actor.getName(),interno(req)),false);}
    private boolean interno(HttpServletRequest req){return req.getRequestURI().startsWith("/api/operacion/");}
    private ResponseEntity<byte[]> contenido(ArchivoService.Contenido c,boolean download){
        var h=new HttpHeaders();h.setContentType(MediaType.parseMediaType(c.tipo()));h.setCacheControl("no-store, private");h.set("X-Content-Type-Options","nosniff");h.set("Content-Security-Policy","default-src 'none'; sandbox");h.setContentDisposition((download?ContentDisposition.attachment():ContentDisposition.inline()).filename(c.nombre(),StandardCharsets.UTF_8).build());return new ResponseEntity<>(c.bytes(),h,HttpStatus.OK);
    }
}
