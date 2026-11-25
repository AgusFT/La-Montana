package ar.com.lamontana.instalacion;

import ar.com.lamontana.identidad.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** API de aprovisionamiento local; el proxy público no publica estas rutas. Token de 256 bits, sin cookie. */
@RestController
@RequestMapping("/api/instalador")
public class InstaladorController {
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;private final IdentidadService identidad;private final SoporteService soporte;private final CorreoService correo;private final String token;
    public InstaladorController(org.springframework.jdbc.core.JdbcTemplate jdbc,IdentidadService identidad,SoporteService soporte,CorreoService correo,@Value("${lamontana.instalacion.token}") String token){this.jdbc=jdbc;this.identidad=identidad;this.soporte=soporte;this.correo=correo;this.token=token;}
    private void autenticar(String recibido){if(token.length()<32||!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),recibido.getBytes(StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Token de instalación no válido.");}
    @GetMapping("/estado")
    public Map<String,Boolean> estado(@RequestHeader("X-LaMontana-Instalacion") String recibido){autenticar(recibido);return Map.of("cuentasCompletas",!identidad.estado().requierePropietario(),"soporteExiste",Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.usuario WHERE es_soporte)",Boolean.class)));}
    @PostMapping("/cuentas") @Transactional
    public Map<String,String> cuentas(@RequestHeader("X-LaMontana-Instalacion") String recibido,@Valid @RequestBody Cuentas v){
        autenticar(recibido);
        // El bloqueo del alta del propietario cierra también este canal al completarla.
        identidad.crearPropietario(new IdentidadController.AltaPropietario(token,v.propietario().nombre(),v.propietario().apellido(),v.propietario().correo(),v.propietario().contrasena()));
        if(v.soporte()!=null)soporte.crear(v.soporte().nombre(),v.soporte().apellido(),v.soporte().correo(),v.soporte().contrasena());
        return Map.of("mensaje",v.soporte()==null?"Propietario creado. Podés crear SOPORTE después desde la aplicación.":"Cuentas preparadas; SOPORTE permanece bloqueado hasta autorización del negocio.");
    }
    @PostMapping("/correo")
    public Map<String,String> probarCorreo(@RequestHeader("X-LaMontana-Instalacion") String recibido,@Valid @RequestBody PruebaCorreo v){autenticar(recibido);correo.enviarCodigo(v.destino(),"Comprobación SMTP del instalador",v.codigo());return Map.of("mensaje","Correo enviado; falta confirmar la recepción.");}
    public record Cuenta(@NotBlank @Size(max=100) String nombre,@NotBlank @Size(max=100) String apellido,@NotBlank @Email @Size(max=254) String correo,@NotBlank @Size(min=12,max=128) String contrasena){@Override public String toString(){return "Cuenta[privada]";}}
    public record Cuentas(@NotNull @Valid Cuenta propietario,@Valid Cuenta soporte){@Override public String toString(){return "Cuentas[privadas]";}}
    public record PruebaCorreo(@NotBlank @Email @Size(max=254) String destino,@Pattern(regexp="[A-Z0-9]{10,32}") @NotNull String codigo){}
}
