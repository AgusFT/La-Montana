package ar.com.lamontana.organizacion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ar.com.lamontana.organizacion.SucursalController.*;

@Service
public class OrganizacionService {
    private final JdbcTemplate jdbc;
    public OrganizacionService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // Ordena cambios de asignaciones y bajas para conservar al menos una sucursal activa por empleado.
    void bloquearOrganizacion() {
        // Mismo orden para cambios de organización, guardado y activación del motor.
        jdbc.execute("SELECT pg_advisory_xact_lock(764003)");
        jdbc.execute("SELECT pg_advisory_xact_lock(764001)");
    }

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
        if(Boolean.TRUE.equals(in.propuestaMatriz()))proponerMatriz(codigo,actor);
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
        if ("DESACTIVADA".equals(in.estado())) validarDesactivacion(codigo);
        if(Boolean.TRUE.equals(in.propuestaMatriz())&&!"ACTIVA".equals(in.estado()))throw error(HttpStatus.CONFLICT,"La propuesta de casa matriz debe ser una sucursal activa.");
        if(Boolean.TRUE.equals(in.propuestaMatriz()))proponerMatriz(codigo,actor);
        jdbc.update("""
                UPDATE lamontana.sucursal SET nombre=?,calle=?,numero=?,localidad=?,provincia=?,codigo_postal=?,correo=?,telefono=?,zona_horaria=?,
                fecha_desactivacion=CASE WHEN ?='DESACTIVADA' THEN COALESCE(fecha_desactivacion,now()) ELSE NULL END,estado=?,version=version+1,
                propuesta_matriz=CASE WHEN ?='DESACTIVADA' THEN false ELSE COALESCE(?,propuesta_matriz) END
                WHERE codigo_publico=?
                """, in.nombre().strip(), in.calle().strip(), in.numero().strip(), in.localidad().strip(), in.provincia().strip(),
                in.codigoPostal().strip(), opcional(in.correo()), opcional(in.telefono()), zona, in.estado(), in.estado(),in.estado(),in.propuestaMatriz(), codigo);
        guardarHorario(codigo, dias);
        auditar(actor, "EDICION_SUCURSAL", codigo, actual.version()+1);
        return buscarSucursal(codigo);
    }

    @Transactional
    public void eliminarSucursal(UUID codigo, long version, String actor) {
        bloquearOrganizacion();
        Sucursal actual = buscarSucursal(codigo);
        if (actual.version() != version) throw desactualizado();
        if ("DESACTIVADA".equals(actual.estado())) throw error(HttpStatus.CONFLICT, "La sucursal ya está desactivada.");
        validarDesactivacion(codigo);
        jdbc.update("UPDATE lamontana.sucursal SET estado='DESACTIVADA',propuesta_matriz=false,fecha_desactivacion=now(),version=version+1 WHERE codigo_publico=?", codigo);
        auditar(actor, "BAJA_SUCURSAL", codigo, actual.version()+1);
    }

    private void validarDesactivacion(UUID codigo) {
        boolean matriz=Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_entrega e
            JOIN lamontana.configuracion_version c USING(id_configuracion_version)
            JOIN lamontana.sucursal s ON s.id_sucursal=e.id_sucursal_matriz
            WHERE s.codigo_publico=? AND c.estado IN ('ACTIVA','PROGRAMADA'))
            """,Boolean.class,codigo));
        if(matriz)throw error(HttpStatus.CONFLICT,"Esta sucursal es la casa matriz vigente o programada. Activá una configuración con otra matriz, o cancelá la programada, antes de desactivarla.");
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

    private void proponerMatriz(UUID codigo,String actor){
        var anteriores=jdbc.query("UPDATE lamontana.sucursal SET propuesta_matriz=false,version=version+1 WHERE propuesta_matriz AND codigo_publico<>? RETURNING codigo_publico,version",
            (rs,row)->new Object[]{rs.getObject(1,UUID.class),rs.getLong(2)},codigo);
        for(var anterior:anteriores)auditar(actor,"EDICION_SUCURSAL",(UUID)anterior[0],(Long)anterior[1]);
        // Solo una propuesta: la matriz efectiva pertenece a cada versión del motor.
        jdbc.update("UPDATE lamontana.sucursal SET propuesta_matriz=true WHERE codigo_publico=?",codigo);
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

    List<String> permisos(Long id) {
        return jdbc.query("SELECT p.codigo FROM lamontana.usuario_permiso up JOIN lamontana.permiso p USING(id_permiso) WHERE up.id_usuario=? AND up.fecha_revocacion IS NULL AND p.activo ORDER BY p.codigo", (rs,row)->rs.getString(1), id);
    }
    List<String> permisosDisponibles() { return jdbc.queryForList("SELECT codigo FROM lamontana.permiso WHERE activo ORDER BY codigo", String.class); }
    Long idUsuario(String correo) { return jdbc.queryForObject("SELECT id_usuario FROM lamontana.usuario WHERE correo=?", Long.class, correo); }
    private Sucursal buscarSucursal(UUID codigo) {
        return jdbc.query("SELECT * FROM lamontana.sucursal WHERE codigo_publico=?", this::sucursal, codigo)
                .stream().findFirst().orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Sucursal inexistente."));
    }
    private Sucursal sucursal(ResultSet rs, int row) throws SQLException {
        return new Sucursal(rs.getObject("codigo_publico", UUID.class), rs.getString("codigo"), rs.getString("nombre"), rs.getString("calle"), rs.getString("numero"), rs.getString("localidad"), rs.getString("provincia"), rs.getString("codigo_postal"), rs.getString("correo"), rs.getString("telefono"), rs.getString("zona_horaria"), rs.getString("estado"), rs.getLong("version"), horario(rs.getLong("id_sucursal")),rs.getBoolean("propuesta_matriz"),Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_entrega e JOIN lamontana.configuracion_version c USING(id_configuracion_version) WHERE e.id_sucursal_matriz=? AND c.estado='ACTIVA')",Boolean.class,rs.getLong("id_sucursal"))));
    }
    void auditar(String actor, String tipo, UUID codigo, long version) {
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
    ResponseStatusException desactualizado() { return error(HttpStatus.CONFLICT, "Los datos cambiaron. Actualizá el listado antes de guardar nuevamente."); }
    ResponseStatusException error(HttpStatus status, String mensaje) { return new ResponseStatusException(status, mensaje); }
    public record Contexto(List<Sucursal> sucursales, List<String> permisos) {}
}
