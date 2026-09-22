package ar.com.lamontana.organizacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SucursalController {
    private final JdbcTemplate jdbc;
    public SucursalController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/api/admin/sucursales")
    public List<Sucursal> listar() {
        return jdbc.query("SELECT * FROM lamontana.sucursal ORDER BY nombre,codigo", (rs, row) -> new Sucursal(
                rs.getObject("codigo_publico", UUID.class), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("calle"), rs.getString("numero"), rs.getString("localidad"), rs.getString("provincia"),
                rs.getString("codigo_postal"), rs.getString("correo"), rs.getString("telefono"),
                rs.getString("zona_horaria"), rs.getString("estado")));
    }

    @PostMapping("/api/admin/sucursales")
    @ResponseStatus(HttpStatus.CREATED)
    public java.util.Map<String, UUID> crear(@Valid @RequestBody NuevaSucursal input, Principal principal) {
        if (!ZoneId.getAvailableZoneIds().contains(input.zonaHoraria())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La zona horaria IANA no es válida.");
        }
        UUID publico = UUID.randomUUID();
        int created = jdbc.update("""
                INSERT INTO lamontana.sucursal
                  (codigo_publico,codigo,nombre,calle,numero,localidad,provincia,codigo_postal,correo,telefono,zona_horaria,estado,id_usuario_alta)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,'ACTIVA',(SELECT id_usuario FROM lamontana.usuario WHERE correo=?)) ON CONFLICT (codigo) DO NOTHING
                """, publico, input.codigo().toUpperCase(Locale.ROOT), input.nombre().strip(), input.calle().strip(),
                input.numero().strip(), input.localidad().strip(), input.provincia().strip(), input.codigoPostal().strip(),
                input.correo(), input.telefono(), input.zonaHoraria(), principal.getName());
        if (created == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una sucursal con ese código.");
        return java.util.Map.of("codigoPublico", publico);
    }

    public record NuevaSucursal(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String codigo,
            @NotBlank @Size(max = 140) String nombre,
            @NotBlank @Size(max = 160) String calle,
            @NotBlank @Size(max = 20) String numero,
            @NotBlank @Size(max = 120) String localidad,
            @NotBlank @Size(max = 120) String provincia,
            @NotBlank @Size(max = 12) String codigoPostal,
            @Email @Size(max = 254) String correo,
            @Size(max = 40) String telefono,
            @NotBlank @Size(max = 64) String zonaHoraria) {}
    public record Sucursal(UUID codigoPublico, String codigo, String nombre, String calle, String numero,
                           String localidad, String provincia, String codigoPostal, String correo,
                           String telefono, String zonaHoraria, String estado) {}
}
