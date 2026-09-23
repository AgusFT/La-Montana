package ar.com.lamontana.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class ImagenWebController {
    private final ImagenWebService imagenes;
    public ImagenWebController(ImagenWebService imagenes){this.imagenes=imagenes;}
    public record Importar(@NotNull UUID operacion,@NotBlank @Size(max=500) String ruta,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String sha256){}
    @GetMapping("/api/admin/pagina-web/origen") public AlmacenImagenWeb.Origen origen(@RequestParam(defaultValue="") String carpeta,Principal actor){return imagenes.origen(carpeta,actor.getName());}
    @GetMapping("/api/admin/pagina-web/origen/miniatura") public ResponseEntity<byte[]> candidata(@RequestParam String ruta,@RequestParam String sha256,Principal actor){return ResponseEntity.ok().headers(headers()).body(imagenes.miniaturaEntrada(ruta,sha256,actor.getName()));}
    @GetMapping("/api/admin/pagina-web/imagenes") public List<ImagenWebService.Imagen> biblioteca(Principal actor){return imagenes.biblioteca(actor.getName());}
    @PostMapping("/api/admin/pagina-web/imagenes/importar") public ImagenWebService.Importacion importar(@Valid @RequestBody Importar input,Principal actor){return imagenes.importar(input,actor.getName());}
    @GetMapping("/api/admin/pagina-web/imagenes/{codigo}") public ResponseEntity<FileSystemResource> privada(@PathVariable UUID codigo,Principal actor){return archivo(codigo,false,false,actor.getName());}
    @GetMapping("/api/admin/pagina-web/imagenes/{codigo}/miniatura") public ResponseEntity<FileSystemResource> privadaMiniatura(@PathVariable UUID codigo,Principal actor){return archivo(codigo,true,false,actor.getName());}
    @GetMapping("/api/publico/pagina-web/imagenes/{codigo}") public ResponseEntity<FileSystemResource> publica(@PathVariable UUID codigo){return archivo(codigo,false,true,null);}
    @GetMapping("/api/publico/pagina-web/imagenes/{codigo}/miniatura") public ResponseEntity<FileSystemResource> publicaMiniatura(@PathVariable UUID codigo){return archivo(codigo,true,true,null);}
    private ResponseEntity<FileSystemResource> archivo(UUID id,boolean thumb,boolean publico,String correo){return ResponseEntity.ok().headers(headers()).body(new FileSystemResource(imagenes.archivo(id,thumb,publico,correo)));}
    private static HttpHeaders headers(){var h=new HttpHeaders();h.setContentType(MediaType.IMAGE_PNG);h.setCacheControl("no-store");h.set("X-Content-Type-Options","nosniff");h.set("Content-Security-Policy","default-src 'none'; sandbox");return h;}
}
