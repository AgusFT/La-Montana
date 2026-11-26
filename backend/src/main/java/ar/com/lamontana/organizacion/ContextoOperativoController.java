package ar.com.lamontana.organizacion;

import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
public class ContextoOperativoController {
    private final OrganizacionService organizacion;

    public ContextoOperativoController(OrganizacionService organizacion) {
        this.organizacion = organizacion;
    }

    @GetMapping("/api/operacion/contexto")
    public OrganizacionService.Contexto contexto(Principal principal) { return organizacion.contexto(principal.getName()); }

    @GetMapping("/api/operacion/sucursales/{codigo}")
    public SucursalController.Sucursal sucursal(@PathVariable UUID codigo, Principal principal) {
        return organizacion.sucursalAutorizada(principal.getName(), codigo);
    }

}
