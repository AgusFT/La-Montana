//#region ENCABEZADO · CatalogoController.java
/*
 * ========================================================================
 * ARCHIVO: CatalogoController.java
 * ========================================================================
 * FUNCIÓN
 * Expone el catálogo y las configuraciones comerciales, incluidas tarifas agrupadas y precios
 * finales doble faz por hoja; valida la entrada HTTP y conserva el contrato anterior.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] CatalogoController(CatalogoService catalogo)
 * - [public] CatalogoController.Tarifa :: Tarifa(UUID formato, UUID papel, ModoColor color,
 *   BigDecimal precio, BigDecimal recargoDobleFaz, Boolean habilitada)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * - [public] CatalogoService.Estado estado()
 * - [public] Map<String, String> formato(AltaFormato data, Principal actor)
 * - [public] Map<String, String> papel(AltaPapel data, Principal actor)
 * - [public] Map<String, String> servicio(AltaServicio data, Principal actor)
 * - [public] CatalogoService.Revision guardar(NuevaRevision data, Principal actor)
 * - [public] CatalogoService.Revision revision(UUID codigo)
 * - [public] CatalogoService.Revision cancelar(UUID codigo, CancelarProgramacion data, Principal
 *   actor)
 * - [public] Map<String, String> predefinido(HabilitarPredefinido data, Principal actor)
 * - [public] Map<String, String> todosPredefinidos(Principal actor)
 * - [public] Map<String, String> personalizado(PapelPersonalizado data, Principal actor)
 * - [public] Map<String, String> seleccion(SeleccionPapel data, Principal actor)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - CatalogoController (class).
 * - CatalogoController.HabilitarPredefinido (record).
 * - CatalogoController.SeleccionPapel (record).
 * - CatalogoController.PapelPersonalizado (record).
 * - CatalogoController.AltaFormato (record).
 * - CatalogoController.AltaPapel (record).
 * - CatalogoController.AltaServicio (record).
 * - CatalogoController.TipoServicio (enum).
 * - CatalogoController.ModoColor (enum).
 * - CatalogoController.BasePrecio (enum).
 * - CatalogoController.ModoDobleFaz (enum).
 * - CatalogoController.Tarifa (record).
 * - CatalogoController.Compatibilidad (record).
 * - CatalogoController.OfertaServicio (record).
 * - CatalogoController.NuevaRevision (record).
 * - CatalogoController.CancelarProgramacion (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.catalogo;

import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/catalogo")
public class CatalogoController {
    private final CatalogoService catalogo;
    public CatalogoController(CatalogoService catalogo) { this.catalogo = catalogo; }
    @GetMapping public CatalogoService.Estado estado() { return catalogo.estado(); }
    @PostMapping("/formatos") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> formato(@Valid @RequestBody AltaFormato data, Principal actor) { catalogo.formato(data, actor.getName()); return Map.of("mensaje","Formato creado."); }
    @PostMapping("/papeles") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> papel(@Valid @RequestBody AltaPapel data, Principal actor) { catalogo.papel(data, actor.getName()); return Map.of("mensaje","Papel creado."); }
    @PostMapping("/servicios") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> servicio(@Valid @RequestBody AltaServicio data, Principal actor) { catalogo.servicio(data, actor.getName()); return Map.of("mensaje","Servicio creado."); }
    @PostMapping("/revisiones") public CatalogoService.Revision guardar(@Valid @RequestBody NuevaRevision data, Principal actor) { return catalogo.guardar(data, actor.getName()); }
    @GetMapping("/revisiones/{codigo}") public CatalogoService.Revision revision(@PathVariable UUID codigo) { return catalogo.revision(codigo); }
    @PostMapping("/programaciones/{codigo}/cancelar")
    public CatalogoService.Revision cancelar(@PathVariable UUID codigo, @Valid @RequestBody CancelarProgramacion data, Principal actor) {
        return catalogo.cancelar(codigo, data, actor.getName());
    }

    @PostMapping("/papeles-predefinidos")
    public Map<String,String> predefinido(@Valid @RequestBody HabilitarPredefinido data,Principal actor) { catalogo.predefinido(data.codigo(),actor.getName());return Map.of("mensaje","Papel habilitado en el catálogo base."); }
    @PostMapping("/papeles-predefinidos/habilitar-todos")
    public Map<String,String> todosPredefinidos(Principal actor) { catalogo.todosPredefinidos(actor.getName());return Map.of("mensaje","Todos los papeles precargados están habilitados."); }
    @PostMapping("/papeles-personalizados")
    public Map<String,String> personalizado(@Valid @RequestBody PapelPersonalizado data,Principal actor) { catalogo.personalizado(data,actor.getName());return Map.of("mensaje","Papel personalizado guardado."); }
    @PutMapping("/papeles-habilitados")
    public Map<String,String> seleccion(@Valid @RequestBody SeleccionPapel data,Principal actor) { catalogo.seleccion(data,actor.getName());return Map.of("mensaje","Selección de papel guardada."); }
    public record HabilitarPredefinido(@NotBlank @Size(max=40) String codigo) {}
    public record SeleccionPapel(@NotNull UUID formato,@NotNull UUID papel,@NotNull Boolean habilitado) {}
    public record PapelPersonalizado(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,40}") String codigo,
        @NotBlank @Size(max=120) String nombre,@NotNull @Positive @Digits(integer=6,fraction=2) BigDecimal anchoMm,
        @NotNull @Positive @Digits(integer=6,fraction=2) BigDecimal altoMm,@NotNull @Positive @Digits(integer=6,fraction=2) BigDecimal gramaje,
        @NotBlank @Size(max=100) String terminacion) {}

    public record AltaFormato(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,40}") String codigo, @NotBlank @Size(max=100) String nombre,
                              @NotNull @Positive @Digits(integer=6,fraction=2) BigDecimal anchoMm, @NotNull @Positive @Digits(integer=6,fraction=2) BigDecimal altoMm) {}
    public record AltaPapel(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,40}") String codigo, @NotBlank @Size(max=120) String nombre,
                            @NotNull @Positive @Digits(integer=6,fraction=2) BigDecimal gramaje, @NotBlank @Size(max=100) String terminacion) {}
    public record AltaServicio(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,50}") String codigo, @NotBlank @Size(max=140) String nombre,
                               @NotNull TipoServicio tipo, @Size(max=1000) String descripcion) {}
    public enum TipoServicio { IMPRESION, TERMINACION }
    public enum ModoColor { BLANCO_NEGRO, COLOR }
    public enum BasePrecio { POR_COPIA, POR_HOJA, POR_CARILLA, FIJO_POR_ITEM }
    public enum ModoDobleFaz { FIJO, ADICIONAL, PORCENTAJE }
    public record Tarifa(@NotNull UUID formato, @NotNull UUID papel, @NotNull ModoColor color,
                         @NotNull @DecimalMin("0.00") @Digits(integer=12,fraction=2) BigDecimal precio,
                         @NotNull @DecimalMin("0.00") @Digits(integer=12,fraction=2) BigDecimal recargoDobleFaz,
                         @NotNull Boolean habilitada,
                         @JsonInclude(JsonInclude.Include.NON_NULL) UUID grupo,
                         @JsonInclude(JsonInclude.Include.NON_NULL) @Size(max=140) String nombre,
                         @JsonInclude(JsonInclude.Include.NON_NULL) ModoDobleFaz modoDobleFaz,
                         @JsonInclude(JsonInclude.Include.NON_NULL) @DecimalMin("0.00") @Digits(integer=12,fraction=2) BigDecimal valorDobleFaz) {
        // Los comandos e historiales anteriores siguen representando cobro por carilla.
        public Tarifa(UUID formato,UUID papel,ModoColor color,BigDecimal precio,BigDecimal recargoDobleFaz,Boolean habilitada) {
            this(formato,papel,color,precio,recargoDobleFaz,habilitada,null,null,null,null);
        }
    }
    public record Compatibilidad(@NotNull UUID formato, @NotNull UUID papel) {}
    public record OfertaServicio(@NotNull UUID servicio, @NotBlank @Size(max=140) String nombreVisible, @NotNull BasePrecio basePrecio,
                                 @NotNull @DecimalMin("0.00") @Digits(integer=12,fraction=2) BigDecimal precio,
                                 @NotNull @Min(0) @Max(10080) Integer preparacionMinutos, @NotNull Boolean habilitado,
                                 @NotNull @Size(max=300) List<@NotNull @Valid Compatibilidad> compatibilidades) {}
    public record NuevaRevision(UUID versionBase, @NotNull UUID operacion, @NotBlank @Size(max=500) String motivo,
                                @NotNull @Size(max=300) List<@NotNull @Valid Tarifa> tarifas, @NotNull @Size(max=100) List<@NotNull @Valid OfertaServicio> servicios,
                                Instant programadaPara) {}
    public record CancelarProgramacion(@NotNull UUID operacion, @NotBlank @Size(max=500) String motivo) {}
}
