package ar.com.lamontana.configuracion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sucursales/disponibilidad")
public class DisponibilidadSucursalController {
    private final DisponibilidadSucursalService service;
    public DisponibilidadSucursalController(DisponibilidadSucursalService service) { this.service=service; }
    public enum Estado { ABIERTA, PAUSADA }
    public record Cambiar(@NotNull UUID operacion,@NotNull @PositiveOrZero Long version,@NotNull Estado estado) {}
    @GetMapping public List<DisponibilidadSucursalService.Sucursal> listar(Principal p) { return service.listar(p.getName()); }
    @PutMapping("/{sucursal}") public DisponibilidadSucursalService.Disponibilidad cambiar(@PathVariable UUID sucursal,@Valid @RequestBody Cambiar in,Principal p) {
        return service.cambiar(sucursal,in,p.getName());
    }
}
