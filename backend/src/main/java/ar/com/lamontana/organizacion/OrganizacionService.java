package ar.com.lamontana.organizacion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ar.com.lamontana.organizacion.EmpleadoController.*;
import ar.com.lamontana.organizacion.SucursalController.*;

@Service
public class OrganizacionService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final ar.com.lamontana.identidad.CredencialService credenciales;
    public OrganizacionService(JdbcTemplate jdbc, PasswordEncoder encoder, ar.com.lamontana.identidad.CredencialService credenciales) { this.jdbc = jdbc; this.encoder = encoder; this.credenciales=credenciales; }

    // Ordena cambios de asignaciones y bajas para conservar al menos una sucursal activa por empleado.
    private void bloquearOrganizacion() { jdbc.execute("SELECT pg_advisory_xact_lock(764001)"); }

    public List<Sucursal> sucursales() {
        return jdbc.query("SELECT * FROM lamontana.sucursal ORDER BY nombre,codigo", this::sucursal);
    }

    @Transactional
    public UUID crearSucursal(NuevaSucursal in, String actor) {
        bloquearOrganizacion(); HorarioAtencion.validar(in.horarioAtencion());
        String zona = UbicacionSucursal.zona(in.provincia(), in.zonaHoraria(), in.horarioAtencion() == null);
        UUID codigo = UUID.randomUUID();
        int creadas = jdbc.update("""
                INSERT INTO lamontana.sucursal
                (codigo_publico,codigo,nombre,calle,numero,localidad,provincia,codigo_postal,correo,telefono,zona_horaria,estado,id_usuario_alta)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,'ACTIVA',?) ON CONFLICT (codigo) DO NOTHING
                """, codigo, in.codigo().toUpperCase(Locale.ROOT), in.nombre().strip(), in.calle().strip(), in.numero().strip(),
                in.localidad().strip(), in.provincia().strip(), in.codigoPostal().strip(), opcional(in.correo()), opcional(in.telefono()), zona, idUsuario(actor));
        if (creadas == 0) throw error(HttpStatus.CONFLICT, "Ya existe una sucursal con ese código.");
        guardarHorario(codigo, in.horarioAtencion());
        auditar(actor, "ALTA_SUCURSAL", codigo, 0);
        return codigo;
    }

    @Transactional
    public Sucursal actualizarSucursal(UUID codigo, EdicionSucursal in, String actor) {
        bloquearOrganizacion();
        Sucursal actual = buscarSucursal(codigo);
        // Una ficha anterior sin horario puede conservarlo sin definir al usar la API v6.
        var dias = in.horarioAtencion() != null && in.horarioAtencion().isEmpty() && actual.horarioAtencion().isEmpty()
            ? null : in.horarioAtencion();
        HorarioAtencion.validar(dias);
        String zona = UbicacionSucursal.zona(in.provincia(), in.zonaHoraria(), dias == null);
        if (actual.version() != in.version()) throw desactualizado();
        if (!actual.codigo().equals(in.codigo().toUpperCase(Locale.ROOT))) throw error(HttpStatus.BAD_REQUEST, "El código de la sucursal no se modifica.");
        if ("DESACTIVADA".equals(in.estado())) {
            Integer sinAlternativa = jdbc.queryForObject("""
                    SELECT count(*) FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                    WHERE u.estado='ACTIVO' AND r.codigo='EMPLEADO'
                    AND EXISTS (SELECT 1 FROM lamontana.usuario_sucursal us JOIN lamontana.sucursal s USING(id_sucursal)
                                WHERE us.id_usuario=u.id_usuario AND us.activa AND s.codigo_publico=?)
                    AND NOT EXISTS (SELECT 1 FROM lamontana.usuario_sucursal us JOIN lamontana.sucursal s USING(id_sucursal)
                                    WHERE us.id_usuario=u.id_usuario AND us.activa AND s.estado='ACTIVA' AND s.codigo_publico<>?)
                    """, Integer.class, codigo, codigo);
            if (sinAlternativa != null && sinAlternativa > 0) throw error(HttpStatus.CONFLICT, "Reasigná o desactivá los empleados que quedarían sin una sucursal activa.");
        }
        jdbc.update("""
                UPDATE lamontana.sucursal SET nombre=?,calle=?,numero=?,localidad=?,provincia=?,codigo_postal=?,correo=?,telefono=?,zona_horaria=?,
                fecha_desactivacion=CASE WHEN ?='DESACTIVADA' THEN COALESCE(fecha_desactivacion,now()) ELSE NULL END,estado=?,version=version+1
                WHERE codigo_publico=?
                """, in.nombre().strip(), in.calle().strip(), in.numero().strip(), in.localidad().strip(), in.provincia().strip(),
                in.codigoPostal().strip(), opcional(in.correo()), opcional(in.telefono()), zona, in.estado(), in.estado(), codigo);
        guardarHorario(codigo, dias);
        auditar(actor, "EDICION_SUCURSAL", codigo, actual.version()+1);
        return buscarSucursal(codigo);
    }

    public List<Empleado> empleados() {
        return jdbc.query("""
                SELECT u.* FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                WHERE r.codigo='EMPLEADO' ORDER BY u.apellido,u.nombre,u.codigo_publico
                """, this::empleado);
    }

    @Transactional
    public UUID crearEmpleado(NuevoEmpleado in, String actor) {
        bloquearOrganizacion(); validarAsignaciones(in.sucursales(), in.permisos(), "ACTIVO", List.of());
        UUID codigo = UUID.randomUUID();
        var ids = jdbc.query("""
                INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado,debe_cambiar_contrasena)
                VALUES (?,(SELECT id_rol FROM lamontana.rol WHERE codigo='EMPLEADO'),?,?,?,?,'ACTIVO',true)
                ON CONFLICT DO NOTHING RETURNING id_usuario
                """, (rs,row)->rs.getLong(1), codigo, correo(in.correo()), encoder.encode(in.contrasena()), in.nombre().strip(), in.apellido().strip());
        if (ids.isEmpty()) throw error(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        asignar(ids.get(0), in.sucursales(), in.permisos(), actor);
        auditar(actor, "ALTA_EMPLEADO", codigo, 0);
        return codigo;
    }

    @Transactional
    public Empleado actualizarEmpleado(UUID codigo, EdicionEmpleado in, String actor) {
        bloquearOrganizacion();
        Empleado actual = buscarEmpleado(codigo);
        if (actual.version() != in.version()) throw desactualizado();
        validarAsignaciones(in.sucursales(), in.permisos(), in.estado(), actual.sucursales());
        Long id = idUsuario(actual.correo());
        jdbc.update("""
                UPDATE lamontana.usuario SET nombre=?,apellido=?,correo=?,estado=?,version=version+1,
                correo_verificado_en=CASE WHEN correo=? THEN correo_verificado_en ELSE NULL END,
                fecha_desactivacion=CASE WHEN ?='DESACTIVADO' THEN COALESCE(fecha_desactivacion,now()) ELSE NULL END
                WHERE id_usuario=?
                """, in.nombre().strip(), in.apellido().strip(), correo(in.correo()), in.estado(), correo(in.correo()), in.estado(), id);
        asignar(id, in.sucursales(), in.permisos(), actor);
        if ("DESACTIVADO".equals(in.estado()) || !actual.correo().equals(correo(in.correo()))) {
            credenciales.revocar(id);
            jdbc.update("DELETE FROM lamontana.sesion_http WHERE principal_name=?", actual.correo());
        }
        auditar(actor, "EDICION_EMPLEADO", codigo, actual.version()+1);
        return buscarEmpleado(codigo);
    }

    public Contexto contexto(String actor) {
        String rol = jdbc.queryForObject("""
                SELECT r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                WHERE u.correo=? AND u.estado='ACTIVO' AND r.activo
                """, String.class, actor);
        if ("ADMIN_ADMIN".equals(rol)) return new Contexto(
                jdbc.query("SELECT * FROM lamontana.sucursal WHERE estado='ACTIVA' ORDER BY nombre,codigo", this::sucursal), permisosDisponibles());
        if (!"EMPLEADO".equals(rol)) throw error(HttpStatus.FORBIDDEN, "No tenés acceso a la operación interna.");
        Long id = idUsuario(actor);
        return new Contexto(jdbc.query("""
                SELECT s.* FROM lamontana.sucursal s JOIN lamontana.usuario_sucursal us USING(id_sucursal)
                WHERE us.id_usuario=? AND us.activa AND s.estado='ACTIVA' ORDER BY s.nombre,s.codigo
                """, this::sucursal, id), permisos(id));
    }

    public Sucursal sucursalAutorizada(String actor, UUID codigo) {
        return contexto(actor).sucursales().stream().filter(s -> s.codigoPublico().equals(codigo)).findFirst()
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "No tenés acceso a esta sucursal o no está activa."));
    }

    public void exigirPermiso(String actor, String permiso) {
        if (!contexto(actor).permisos().contains(permiso)) throw error(HttpStatus.FORBIDDEN, "No tenés el permiso requerido.");
    }

    private void validarAsignaciones(List<UUID> sucursales, List<String> permisos, String estado, List<UUID> existentes) {
        if (new HashSet<>(sucursales).size() != sucursales.size() || new HashSet<>(permisos).size() != permisos.size()) {
            throw error(HttpStatus.BAD_REQUEST, "Las sucursales y los permisos no pueden repetirse.");
        }
        if ("ACTIVO".equals(estado) && sucursales.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "Un empleado activo necesita al menos una sucursal activa.");
        int activas = 0;
        for (UUID sucursal : sucursales) {
            boolean activa = Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.sucursal WHERE codigo_publico=? AND estado='ACTIVA')", Boolean.class, sucursal));
            if (activa) activas++;
            else if (!existentes.contains(sucursal)) throw error(HttpStatus.BAD_REQUEST, "Las nuevas asignaciones requieren sucursales activas existentes.");
        }
        if ("ACTIVO".equals(estado) && activas == 0) throw error(HttpStatus.BAD_REQUEST, "Un empleado activo necesita al menos una sucursal activa.");
        if (!permisosDisponibles().containsAll(permisos)) throw error(HttpStatus.BAD_REQUEST, "Hay permisos no admitidos.");
    }

    private void asignar(Long usuario, List<UUID> nuevasSucursales, List<String> nuevosPermisos, String actor) {
        List<UUID> actuales = asignaciones(usuario);
        for (UUID sucursal : actuales) if (!nuevasSucursales.contains(sucursal)) {
            jdbc.update("UPDATE lamontana.usuario_sucursal SET activa=false,fecha_hasta=now() WHERE id_usuario=? AND activa AND id_sucursal=(SELECT id_sucursal FROM lamontana.sucursal WHERE codigo_publico=?)", usuario, sucursal);
        }
        for (UUID sucursal : nuevasSucursales) if (!actuales.contains(sucursal)) {
            jdbc.update("INSERT INTO lamontana.usuario_sucursal(id_usuario,id_sucursal) VALUES (?,(SELECT id_sucursal FROM lamontana.sucursal WHERE codigo_publico=?))", usuario, sucursal);
        }
        List<String> anteriores = permisos(usuario);
        for (String permiso : anteriores) if (!nuevosPermisos.contains(permiso)) {
            jdbc.update("UPDATE lamontana.usuario_permiso SET fecha_revocacion=now() WHERE id_usuario=? AND fecha_revocacion IS NULL AND id_permiso=(SELECT id_permiso FROM lamontana.permiso WHERE codigo=?)", usuario, permiso);
        }
        for (String permiso : nuevosPermisos) if (!anteriores.contains(permiso)) {
            jdbc.update("INSERT INTO lamontana.usuario_permiso(id_usuario,id_permiso,id_usuario_otorgante) VALUES (?,(SELECT id_permiso FROM lamontana.permiso WHERE codigo=?),?)", usuario, permiso, idUsuario(actor));
        }
    }

    private List<UUID> asignaciones(Long id) {
        return jdbc.query("SELECT s.codigo_publico FROM lamontana.usuario_sucursal us JOIN lamontana.sucursal s USING(id_sucursal) WHERE us.id_usuario=? AND us.activa ORDER BY s.codigo", (rs,row)->rs.getObject(1, UUID.class), id);
    }
    private List<String> permisos(Long id) {
        return jdbc.query("SELECT p.codigo FROM lamontana.usuario_permiso up JOIN lamontana.permiso p USING(id_permiso) WHERE up.id_usuario=? AND up.fecha_revocacion IS NULL AND p.activo ORDER BY p.codigo", (rs,row)->rs.getString(1), id);
    }
    private List<String> permisosDisponibles() { return jdbc.queryForList("SELECT codigo FROM lamontana.permiso WHERE activo ORDER BY codigo", String.class); }
    private Long idUsuario(String correo) { return jdbc.queryForObject("SELECT id_usuario FROM lamontana.usuario WHERE correo=?", Long.class, correo); }
    private Empleado buscarEmpleado(UUID codigo) {
        return jdbc.query("SELECT u.* FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.codigo_publico=? AND r.codigo='EMPLEADO'", this::empleado, codigo)
                .stream().findFirst().orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Empleado inexistente."));
    }
    private Sucursal buscarSucursal(UUID codigo) {
        return jdbc.query("SELECT * FROM lamontana.sucursal WHERE codigo_publico=?", this::sucursal, codigo)
                .stream().findFirst().orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Sucursal inexistente."));
    }
    private Empleado empleado(ResultSet rs, int row) throws SQLException {
        long id = rs.getLong("id_usuario");
        return new Empleado(rs.getObject("codigo_publico", UUID.class), rs.getString("nombre"), rs.getString("apellido"), rs.getString("correo"), rs.getString("estado"), rs.getLong("version"), asignaciones(id), permisos(id));
    }
    private Sucursal sucursal(ResultSet rs, int row) throws SQLException {
        return new Sucursal(rs.getObject("codigo_publico", UUID.class), rs.getString("codigo"), rs.getString("nombre"), rs.getString("calle"), rs.getString("numero"), rs.getString("localidad"), rs.getString("provincia"), rs.getString("codigo_postal"), rs.getString("correo"), rs.getString("telefono"), rs.getString("zona_horaria"), rs.getString("estado"), rs.getLong("version"), horario(rs.getLong("id_sucursal")));
    }
    private void auditar(String actor, String tipo, UUID codigo, long version) {
        jdbc.update("INSERT INTO lamontana.evento_organizacion(id_usuario_actor,tipo,codigo_publico_objeto,version) VALUES (?,?,?,?)", idUsuario(actor), tipo, codigo, version);
    }
    private List<HorarioAtencion.Dia> horario(long sucursal) {
        return jdbc.query("SELECT dia_semana,habilitado,to_char(hora_desde,'HH24:MI'),to_char(hora_hasta,'HH24:MI') FROM lamontana.sucursal_horario_atencion WHERE id_sucursal=? ORDER BY dia_semana",
            (rs,row) -> new HorarioAtencion.Dia(rs.getInt(1),rs.getBoolean(2),rs.getString(3),rs.getString(4)), sucursal);
    }
    private void guardarHorario(UUID codigo, List<HorarioAtencion.Dia> dias) {
        if (dias == null) return;
        long id = jdbc.queryForObject("SELECT id_sucursal FROM lamontana.sucursal WHERE codigo_publico=?", Long.class, codigo);
        jdbc.update("DELETE FROM lamontana.sucursal_horario_atencion WHERE id_sucursal=?", id);
        for (var dia : dias) jdbc.update("INSERT INTO lamontana.sucursal_horario_atencion(id_sucursal,dia_semana,habilitado,hora_desde,hora_hasta) VALUES (?,?,?,?,?)",
            id, dia.dia(), dia.habilitado(), dia.apertura()==null?null:Time.valueOf(LocalTime.parse(dia.apertura())), dia.cierre()==null?null:Time.valueOf(LocalTime.parse(dia.cierre())));
    }
    private String opcional(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private String correo(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    private ResponseStatusException desactualizado() { return error(HttpStatus.CONFLICT, "Los datos cambiaron. Actualizá el listado antes de guardar nuevamente."); }
    private ResponseStatusException error(HttpStatus status, String mensaje) { return new ResponseStatusException(status, mensaje); }
    public record Contexto(List<Sucursal> sucursales, List<String> permisos) {}
}
