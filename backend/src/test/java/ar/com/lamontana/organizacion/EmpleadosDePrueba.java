package ar.com.lamontana.organizacion;

import ar.com.lamontana.identidad.CredencialService;
import ar.com.lamontana.organizacion.EmpleadoController.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Prepara identidades y asignaciones para los escenarios heredados de organización. */
@org.springframework.context.annotation.Profile("historia-organizacion")
@Service
public class EmpleadosDePrueba {
    private final JdbcTemplate jdbc;
    private final OrganizacionService organizacion;
    private final PasswordEncoder encoder;
    private final CredencialService credenciales;

    public EmpleadosDePrueba(JdbcTemplate jdbc, OrganizacionService organizacion, PasswordEncoder encoder,
                             CredencialService credenciales) {
        this.jdbc = jdbc;
        this.organizacion = organizacion;
        this.encoder = encoder;
        this.credenciales = credenciales;
    }

    public List<Empleado> empleados() {
        return jdbc.query("""
                SELECT u.* FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                WHERE r.codigo='EMPLEADO' ORDER BY u.apellido,u.nombre,u.codigo_publico
                """, this::empleado);
    }

    @Transactional
    public UUID crearEmpleado(NuevoEmpleado in, String actor) {
        organizacion.bloquearOrganizacion(); validarAsignaciones(in.sucursales(), in.permisos(), "ACTIVO", List.of());
        UUID codigo = UUID.randomUUID();
        var ids = jdbc.query("""
                INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado,debe_cambiar_contrasena)
                VALUES (?,(SELECT id_rol FROM lamontana.rol WHERE codigo='EMPLEADO'),?,?,?,?,'ACTIVO',true)
                ON CONFLICT DO NOTHING RETURNING id_usuario
                """, (rs,row)->rs.getLong(1), codigo, in.correo().strip().toLowerCase(Locale.ROOT), encoder.encode(in.contrasena()), in.nombre().strip(), in.apellido().strip());
        if (ids.isEmpty()) throw organizacion.error(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        asignar(ids.get(0), in.sucursales(), in.permisos(), actor);
        organizacion.auditar(actor, "ALTA_EMPLEADO", codigo, 0);
        return codigo;
    }

    @Transactional
    public Empleado actualizarEmpleado(UUID codigo, EdicionEmpleado in, String actor) {
        organizacion.bloquearOrganizacion();
        Empleado actual = buscarEmpleado(codigo);
        if (actual.version() != in.version()) throw organizacion.desactualizado();
        validarAsignaciones(in.sucursales(), in.permisos(), in.estado(), actual.sucursales());
        Long id = organizacion.idUsuario(actual.correo());
        jdbc.update("""
                UPDATE lamontana.usuario SET nombre=?,apellido=?,correo=?,estado=?,version=version+1,
                correo_verificado_en=CASE WHEN correo=? THEN correo_verificado_en ELSE NULL END,
                fecha_desactivacion=CASE WHEN ?='DESACTIVADO' THEN COALESCE(fecha_desactivacion,now()) ELSE NULL END
                WHERE id_usuario=?
                """, in.nombre().strip(), in.apellido().strip(), in.correo().strip().toLowerCase(Locale.ROOT), in.estado(), in.correo().strip().toLowerCase(Locale.ROOT), in.estado(), id);
        asignar(id, in.sucursales(), in.permisos(), actor);
        if ("DESACTIVADO".equals(in.estado()) || !actual.correo().equals(in.correo().strip().toLowerCase(Locale.ROOT))) {
            credenciales.revocar(id);
            jdbc.update("DELETE FROM lamontana.sesion_http WHERE principal_name=?", actual.correo());
        }
        organizacion.auditar(actor, "EDICION_EMPLEADO", codigo, actual.version()+1);
        return buscarEmpleado(codigo);
    }

    private void validarAsignaciones(List<UUID> sucursales, List<String> permisos, String estado, List<UUID> existentes) {
        if (new HashSet<>(sucursales).size() != sucursales.size() || new HashSet<>(permisos).size() != permisos.size()) {
            throw organizacion.error(HttpStatus.BAD_REQUEST, "Las sucursales y los permisos no pueden repetirse.");
        }
        if ("ACTIVO".equals(estado) && sucursales.isEmpty()) throw organizacion.error(HttpStatus.BAD_REQUEST, "Un empleado activo necesita al menos una sucursal activa.");
        int activas = 0;
        for (UUID sucursal : sucursales) {
            boolean activa = Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.sucursal WHERE codigo_publico=? AND estado='ACTIVA')", Boolean.class, sucursal));
            if (activa) activas++;
            else if (!existentes.contains(sucursal)) throw organizacion.error(HttpStatus.BAD_REQUEST, "Las nuevas asignaciones requieren sucursales activas existentes.");
        }
        if ("ACTIVO".equals(estado) && activas == 0) throw organizacion.error(HttpStatus.BAD_REQUEST, "Un empleado activo necesita al menos una sucursal activa.");
        if (!organizacion.permisosDisponibles().containsAll(permisos)) throw organizacion.error(HttpStatus.BAD_REQUEST, "Hay permisos no admitidos.");
    }

    private void asignar(Long usuario, List<UUID> nuevasSucursales, List<String> nuevosPermisos, String actor) {
        List<UUID> actuales = asignaciones(usuario);
        for (UUID sucursal : actuales) if (!nuevasSucursales.contains(sucursal)) {
            jdbc.update("UPDATE lamontana.usuario_sucursal SET activa=false,fecha_hasta=now() WHERE id_usuario=? AND activa AND id_sucursal=(SELECT id_sucursal FROM lamontana.sucursal WHERE codigo_publico=?)", usuario, sucursal);
        }
        for (UUID sucursal : nuevasSucursales) if (!actuales.contains(sucursal)) {
            jdbc.update("INSERT INTO lamontana.usuario_sucursal(id_usuario,id_sucursal) VALUES (?,(SELECT id_sucursal FROM lamontana.sucursal WHERE codigo_publico=?))", usuario, sucursal);
        }
        List<String> anteriores = organizacion.permisos(usuario);
        for (String permiso : anteriores) if (!nuevosPermisos.contains(permiso)) {
            jdbc.update("UPDATE lamontana.usuario_permiso SET fecha_revocacion=now() WHERE id_usuario=? AND fecha_revocacion IS NULL AND id_permiso=(SELECT id_permiso FROM lamontana.permiso WHERE codigo=?)", usuario, permiso);
        }
        for (String permiso : nuevosPermisos) if (!anteriores.contains(permiso)) {
            jdbc.update("INSERT INTO lamontana.usuario_permiso(id_usuario,id_permiso,id_usuario_otorgante) VALUES (?,(SELECT id_permiso FROM lamontana.permiso WHERE codigo=?),?)", usuario, permiso, organizacion.idUsuario(actor));
        }
    }

    private List<UUID> asignaciones(Long id) {
        return jdbc.query("SELECT s.codigo_publico FROM lamontana.usuario_sucursal us JOIN lamontana.sucursal s USING(id_sucursal) WHERE us.id_usuario=? AND us.activa ORDER BY s.codigo", (rs,row)->rs.getObject(1, UUID.class), id);
    }
    private Empleado buscarEmpleado(UUID codigo) {
        return jdbc.query("SELECT u.* FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.codigo_publico=? AND r.codigo='EMPLEADO'", this::empleado, codigo)
                .stream().findFirst().orElseThrow(() -> organizacion.error(HttpStatus.NOT_FOUND, "Empleado inexistente."));
    }
    private Empleado empleado(ResultSet rs, int row) throws SQLException {
        long id = rs.getLong("id_usuario");
        return new Empleado(rs.getObject("codigo_publico", UUID.class), rs.getString("nombre"), rs.getString("apellido"), rs.getString("correo"), rs.getString("estado"), rs.getLong("version"), asignaciones(id), organizacion.permisos(id));
    }
}
