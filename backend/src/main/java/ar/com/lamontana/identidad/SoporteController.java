package ar.com.lamontana.identidad;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.security.Principal;import java.util.Map;import org.springframework.web.bind.annotation.*;
@RestController
public class SoporteController {
    private final SoporteService soporte;public SoporteController(SoporteService soporte){this.soporte=soporte;}
    @GetMapping("/api/admin/soporte") public Map<String,Object> estado(Principal actor){return soporte.estado(actor.getName());}
    @PostMapping("/api/admin/soporte") public Map<String,String> crear(Principal actor,@Valid @RequestBody Alta v){soporte.crearDesdeApp(actor.getName(),v.contrasenaActual(),v.nombre(),v.apellido(),v.correo(),v.contrasena());return Map.of("mensaje","Cuenta de soporte creada. El acceso permanece bloqueado hasta que lo autorices por separado.");}
    @PostMapping("/api/admin/soporte/autorizar") public Map<String,String> autorizar(Principal actor,@Valid @RequestBody Autorizacion v){soporte.autorizar(actor.getName(),v.contrasena(),v.motivo(),v.horas());return Map.of("mensaje","Soporte autorizado por el plazo indicado. Podés revocarlo en cualquier momento.");}
    @PostMapping("/api/admin/soporte/revocar") public Map<String,String> revocar(Principal actor,@Valid @RequestBody Revocacion v){soporte.revocar(actor.getName(),v.contrasena());return Map.of("mensaje","Acceso de soporte revocado y sesiones invalidadas.");}
    public record Autorizacion(@NotBlank @Size(max=128) String contrasena,@NotBlank @Size(max=2000) String motivo,@Min(1) @Max(24) int horas){@Override public String toString(){return "Autorizacion[privada]";}}
    public record Revocacion(@NotBlank @Size(max=128) String contrasena){@Override public String toString(){return "Revocacion[privada]";}}
    public record Alta(@NotBlank @Size(max=128) String contrasenaActual,@NotBlank @Size(max=100) String nombre,@NotBlank @Size(max=100) String apellido,@NotBlank @Email @Size(max=254) String correo,@NotBlank @Size(min=12,max=128) String contrasena){@Override public String toString(){return "AltaSoporte[privada]";}}
}
