package ar.com.lamontana.identidad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdentidadService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String setupToken;

    public IdentidadService(JdbcTemplate jdbc, PasswordEncoder encoder,
                            @Value("${lamontana.instalacion.token}") String setupToken) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.setupToken = setupToken;
    }

    public EstadoInstalacion estado() {
        boolean pendiente = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT completada_en IS NULL FROM lamontana.inicializacion_sistema WHERE unica", Boolean.class));
        return new EstadoInstalacion(pendiente, pendiente && setupToken.length() >= 32);
    }

    @Transactional
    public void crearPropietario(IdentidadController.AltaPropietario alta) {
        boolean pendiente = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT completada_en IS NULL FROM lamontana.inicializacion_sistema WHERE unica FOR UPDATE", Boolean.class));
        if (!pendiente) throw new ResponseStatusException(HttpStatus.CONFLICT, "El propietario ya fue creado.");
        if (setupToken.length() < 32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Falta habilitar el alta desde la instalación local.");
        if (!MessageDigest.isEqual(setupToken.getBytes(StandardCharsets.UTF_8), alta.token().getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El token de instalación no es válido.");
        }
        Long id = jdbc.queryForObject("""
                INSERT INTO lamontana.usuario
                  (codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado,es_administrador_propietario)
                VALUES (?,(SELECT id_rol FROM lamontana.rol WHERE codigo='ADMIN_ADMIN'),?,?,?,?, 'ACTIVO',true)
                RETURNING id_usuario
                """, Long.class, UUID.randomUUID(), normalizar(alta.correo()), encoder.encode(alta.contrasena()), alta.nombre().strip(), alta.apellido().strip());
        jdbc.update("UPDATE lamontana.inicializacion_sistema SET completada_en=now(),id_usuario_propietario=? WHERE unica", id);
        jdbc.update("INSERT INTO lamontana.evento_acceso (tipo,id_usuario) VALUES ('ALTA_PROPIETARIO',?)", id);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return jdbc.query("""
                SELECT u.correo,u.hash_contrasena,u.estado,r.codigo,r.activo
                FROM lamontana.usuario u JOIN lamontana.rol r USING (id_rol) WHERE lower(u.correo)=?
                """, (rs, row) -> User.withUsername(rs.getString("correo"))
                .password(rs.getString("hash_contrasena")).roles(rs.getString("codigo"))
                .disabled(!"ACTIVO".equals(rs.getString("estado")) || !rs.getBoolean("activo")).build(), normalizar(username))
                .stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
    }

    public Perfil perfil(String correo) {
        return jdbc.queryForObject("""
                SELECT u.codigo_publico,u.nombre,u.apellido,u.correo,r.codigo
                FROM lamontana.usuario u JOIN lamontana.rol r USING (id_rol)
                WHERE lower(u.correo)=? AND u.estado='ACTIVO' AND r.activo
                """, (rs, row) -> new Perfil(rs.getObject("codigo_publico", UUID.class), rs.getString("nombre"),
                rs.getString("apellido"), rs.getString("correo"), rs.getString("codigo")), normalizar(correo));
    }

    public void registrarAcceso(String tipo, String correo) {
        jdbc.update("INSERT INTO lamontana.evento_acceso (tipo,id_usuario) VALUES (?,(SELECT id_usuario FROM lamontana.usuario WHERE lower(correo)=?))", tipo, normalizar(correo));
    }

    private String normalizar(String correo) { return correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT); }
    public record EstadoInstalacion(boolean requierePropietario, boolean altaHabilitada) {}
    public record Perfil(UUID codigoPublico, String nombre, String apellido, String correo, String rol) {}
}
