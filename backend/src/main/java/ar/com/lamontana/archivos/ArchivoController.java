package ar.com.lamontana.archivos;

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
public class ArchivoController {
    private final ArchivoService archivos;
    public ArchivoController(ArchivoService archivos){this.archivos=archivos;}
    public record Inicio(@NotNull UUID operacion,@NotNull UUID item,@Min(0)long version){}
    public record Aceptacion(@NotNull UUID operacion,@NotBlank @Pattern(regexp="[a-f0-9]{64}")String sha256){}
    @GetMapping("/api/operacion/sucursales/{branch}/archivos")
    ArchivoService.Bandeja bandeja(@PathVariable UUID branch,@RequestParam(defaultValue="0")int pagina,Principal principal){return archivos.bandeja(branch,pagina,principal.getName());}
    @GetMapping({"/api/cliente/cotizaciones/{quote}/archivos","/api/operacion/cotizaciones/{quote}/archivos"})
    ArchivoService.Vista listar(@PathVariable UUID quote,Principal principal,HttpServletRequest request){return archivos.listar(quote,principal.getName(),interno(request));}
    @GetMapping({"/api/cliente/cotizaciones/{quote}/archivos/{file}","/api/operacion/cotizaciones/{quote}/archivos/{file}"})
    ArchivoService.Archivo consultar(@PathVariable UUID quote,@PathVariable UUID file,Principal principal,HttpServletRequest request){return archivos.consultar(quote,file,principal.getName(),interno(request));}
    @GetMapping("/api/cliente/cotizaciones/{quote}/archivos/{file}/carga")
    ArchivoService.Acceso habilitada(@PathVariable UUID quote,@PathVariable UUID file,Principal principal){return archivos.puedeEnviar(quote,file,principal.getName());}
    @PostMapping("/api/cliente/cotizaciones/{quote}/archivos")
    ArchivoService.Archivo crear(@PathVariable UUID quote,@Valid @RequestBody Inicio in,Principal principal){return archivos.crear(quote,in.item(),in.operacion(),in.version(),principal.getName());}
    @PutMapping(value="/api/cliente/cotizaciones/{quote}/archivos/{file}/contenido",consumes="application/pdf")
    ArchivoService.Archivo cargar(@PathVariable UUID quote,@PathVariable UUID file,Principal principal,HttpServletRequest request)throws IOException{return archivos.recibir(quote,file,principal.getName(),request.getInputStream(),request.getContentLengthLong());}
    @PostMapping("/api/cliente/cotizaciones/{quote}/archivos/{file}/aceptar")
    ArchivoService.Archivo aceptar(@PathVariable UUID quote,@PathVariable UUID file,@Valid @RequestBody Aceptacion in,Principal principal){return archivos.aceptar(quote,file,in.operacion(),in.sha256(),principal.getName());}
    @GetMapping({"/api/cliente/cotizaciones/{quote}/archivos/{file}/original","/api/operacion/cotizaciones/{quote}/archivos/{file}/original"})
    ResponseEntity<byte[]> original(@PathVariable UUID quote,@PathVariable UUID file,Principal principal,HttpServletRequest request){return respuesta(archivos.contenido(quote,file,null,principal.getName(),interno(request)),true);}
    @GetMapping({"/api/cliente/cotizaciones/{quote}/archivos/{file}/paginas/{page}","/api/operacion/cotizaciones/{quote}/archivos/{file}/paginas/{page}"})
    ResponseEntity<byte[]> preview(@PathVariable UUID quote,@PathVariable UUID file,@PathVariable int page,Principal principal,HttpServletRequest request){return respuesta(archivos.contenido(quote,file,page,principal.getName(),interno(request)),false);}
    private boolean interno(HttpServletRequest request){return request.getRequestURI().startsWith("/api/operacion/");}
    private ResponseEntity<byte[]> respuesta(ArchivoService.Contenido content,boolean download){
        var headers=new HttpHeaders();headers.setContentType(MediaType.parseMediaType(content.tipo()));headers.setCacheControl("no-store, private");headers.set("X-Content-Type-Options","nosniff");headers.set("Content-Security-Policy","default-src 'none'; sandbox");
        headers.setContentDisposition((download?ContentDisposition.attachment():ContentDisposition.inline()).filename(content.nombre(),StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(content.bytes(),headers,HttpStatus.OK);
    }
}
