package ar.com.lamontana.caja;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operacion/sucursales/{sucursal}/caja")
public class CajaController {
    private static final String IMPORTE = "[0-9]{1,17}(\\.[0-9]{1,2})?";
    private final CajaService service;

    public CajaController(CajaService service) { this.service = service; }

    public record NuevoEgreso(@NotNull UUID operacion, @NotNull CajaService.Tipo tipo,
        @NotNull CajaService.Medio medio, @NotNull @Pattern(regexp=IMPORTE) String importe,
        @NotBlank @Size(max=140) String destinatario, @NotBlank @Size(max=300) String concepto,
        @Size(min=1,max=2097152) byte[] comprobante) {}

    public record NuevoArqueo(@NotNull UUID operacion, @NotNull @Pattern(regexp=IMPORTE) String contado,
        @NotNull @Pattern(regexp="-?[0-9]{1,25}(\\.[0-9]{1,2})?") String esperado,
        @Size(max=300) String observacion) {}

    @GetMapping
    public CajaService.Vista consultar(@PathVariable UUID sucursal, Principal actor,
            @RequestParam(required=false) LocalDate desde, @RequestParam(required=false) LocalDate hasta,
            @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="0") int paginaArqueos) {
        return service.consultar(sucursal,actor.getName(),desde,hasta,pagina,paginaArqueos);
    }

    @PostMapping("/egresos")
    public ResponseEntity<Void> egreso(@PathVariable UUID sucursal, Principal actor, @Valid @RequestBody NuevoEgreso in) {
        service.egreso(sucursal,actor.getName(),in);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/arqueos")
    public ResponseEntity<Void> arqueo(@PathVariable UUID sucursal, Principal actor, @Valid @RequestBody NuevoArqueo in) {
        service.arqueo(sucursal,actor.getName(),in);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/egresos/{movimiento}/comprobante")
    public ResponseEntity<byte[]> comprobante(@PathVariable UUID sucursal, @PathVariable UUID movimiento, Principal actor) {
        var archivo = service.comprobante(sucursal,movimiento,actor.getName());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition","attachment; filename=\"comprobante-caja."+archivo.extension()+"\"")
            .header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").body(archivo.datos());
    }
}
