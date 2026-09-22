package ar.com.lamontana.catalogo;

import jakarta.validation.Valid;
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

    public record AltaFormato(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,40}") String codigo, @NotBlank @Size(max=100) String nombre,
                              @NotNull @Positive @Digits(integer=6,fraction=2) BigDecimal anchoMm, @NotNull @Positive @Digits(integer=6,fraction=2) BigDecimal altoMm) {}
    public record AltaPapel(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,40}") String codigo, @NotBlank @Size(max=120) String nombre,
                            @NotNull @Positive @Digits(integer=6,fraction=2) BigDecimal gramaje, @NotBlank @Size(max=100) String terminacion) {}
    public record AltaServicio(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,50}") String codigo, @NotBlank @Size(max=140) String nombre,
                               @NotNull TipoServicio tipo, @Size(max=1000) String descripcion) {}
    public enum TipoServicio { IMPRESION, TERMINACION }
    public enum ModoColor { BLANCO_NEGRO, COLOR }
    public enum BasePrecio { POR_COPIA, POR_HOJA, POR_CARILLA, FIJO_POR_ITEM }
    public record Tarifa(@NotNull UUID formato, @NotNull UUID papel, @NotNull ModoColor color,
                         @NotNull @DecimalMin("0.00") @Digits(integer=12,fraction=2) BigDecimal precio,
                         @NotNull @DecimalMin("0.00") @Digits(integer=12,fraction=2) BigDecimal recargoDobleFaz, @NotNull Boolean habilitada) {}
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
