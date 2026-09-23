//#region ENCABEZADO · PaginaWebController.java
/*
 * ========================================================================
 * ARCHIVO: PaginaWebController.java
 * ========================================================================
 * FUNCIÓN
 * Expone el borrador, guardado, revisión y publicación del sitio de la imprenta, además de la
 * consulta pública del contenido publicado.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] PaginaWebController(PaginaWebService sitio)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] PaginaWebService.Estado estado(Principal actor)
 *   Entrada HTTP GET · ruta del método: /api/admin/pagina-web.
 * - [public] PaginaWebService.Estado guardar(Guardar input, Principal actor)
 *   Entrada HTTP PUT · ruta del método: /api/admin/pagina-web/borrador.
 * - [public] PaginaWebService.Revision revision(Principal actor)
 *   Entrada HTTP GET · ruta del método: /api/admin/pagina-web/revision.
 * - [public] PaginaWebService.Publicacion publicar(Publicar input, Principal actor)
 *   Entrada HTTP POST · ruta del método: /api/admin/pagina-web/publicar.
 * - [public] PaginaWebService.Publico publico()
 *   Entrada HTTP GET · ruta del método: /api/publico/pagina-web.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PaginaWebController (clase).
 * - PaginaWebController.TipoFicha (enumeración).
 * - PaginaWebController.Seccion (enumeración).
 * - PaginaWebController.Imagen (record).
 * - PaginaWebController.Ficha (record).
 * - PaginaWebController.Contenido (record).
 * - PaginaWebController.Guardar (record).
 * - PaginaWebController.Publicar (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaginaWebController {
    private final PaginaWebService sitio;
    public PaginaWebController(PaginaWebService sitio){this.sitio=sitio;}
    public enum TipoFicha{PRODUCTO,SERVICIO}
    public enum Seccion{PORTADA,CATALOGO,CONTACTO}
    public record Imagen(@NotNull UUID codigo,@NotNull @Size(max=250) String alternativo){}
    public record Ficha(@NotNull UUID codigo,@NotNull TipoFicha tipo,@NotNull @Size(max=160) String nombre,
        @NotNull @Size(max=3000) String descripcion,UUID servicio,@NotNull @Size(max=8) List<@NotNull @Valid Imagen> imagenes,
        @Min(0) @Max(10000) int orden,@NotNull Boolean visible){}
    public record Contenido(@NotNull @Size(max=160) String nombre,@NotNull @Size(max=200) String titulo,
        @NotNull @Size(max=3000) String descripcion,UUID logo,@NotNull @Size(max=250) String logoAlt,
        UUID portada,@NotNull @Size(max=250) String portadaAlt,@NotNull @Email @Size(max=254) String correo,
        @NotNull @Size(max=60) String telefono,@NotNull @Size(max=600) String direccion,
        @NotNull @Size(min=1,max=3) List<@NotNull Seccion> secciones,@NotNull @Size(max=100) List<@NotNull @Valid Ficha> fichas){}
    public record Guardar(@NotNull UUID operacion,@Min(1) long version,@NotNull @Valid Contenido contenido,@NotNull @Size(max=500) String carpeta){}
    public record Publicar(@NotNull UUID operacion,@Min(1) long version,@AssertTrue boolean confirmado){}
    @GetMapping("/api/admin/pagina-web") public PaginaWebService.Estado estado(Principal actor){return sitio.estado(actor.getName());}
    @PutMapping("/api/admin/pagina-web/borrador") public PaginaWebService.Estado guardar(@Valid @RequestBody Guardar input,Principal actor){return sitio.guardar(input,actor.getName());}
    @GetMapping("/api/admin/pagina-web/revision") public PaginaWebService.Revision revision(Principal actor){return sitio.revision(actor.getName());}
    @PostMapping("/api/admin/pagina-web/publicar") public PaginaWebService.Publicacion publicar(@Valid @RequestBody Publicar input,Principal actor){return sitio.publicar(input,actor.getName());}
    @GetMapping("/api/publico/pagina-web") public PaginaWebService.Publico publico(){return sitio.publico();}
}
