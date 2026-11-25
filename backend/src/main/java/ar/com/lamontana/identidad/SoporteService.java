package ar.com.lamontana.identidad;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SoporteService {
    private final JdbcTemplate jdbc;private final PasswordEncoder encoder;
    public SoporteService(JdbcTemplate jdbc,PasswordEncoder encoder){this.jdbc=jdbc;this.encoder=encoder;}
    @Transactional
    public long crear(String nombre,String apellido,String correo,String contrasena){
        jdbc.execute("SELECT pg_advisory_xact_lock(764040)");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.usuario WHERE es_soporte)",Boolean.class)))throw error(HttpStatus.CONFLICT,"La cuenta SOPORTE ya existe; no se reemplazan sus credenciales.");
        var ids=jdbc.query("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado,es_soporte) VALUES (?,(SELECT id_rol FROM lamontana.rol WHERE codigo='ADMIN_ADMIN'),?,?,?,?,'ACTIVO',true) ON CONFLICT DO NOTHING RETURNING id_usuario",(r,n)->r.getLong(1),UUID.randomUUID(),correo.strip().toLowerCase(Locale.ROOT),encoder.encode(contrasena),nombre.strip(),apellido.strip());
        if(ids.isEmpty())throw error(HttpStatus.CONFLICT,"El correo ya está utilizado. Elegí otro para la cuenta de soporte.");
        long id=ids.get(0);
        jdbc.update("INSERT INTO lamontana.evento_soporte(id_usuario,tipo) VALUES (?,'CUENTA_CREADA_BLOQUEADA')",id);
        return id;
    }
    @Transactional
    public void crearDesdeApp(String actor,String contrasenaActual,String nombre,String apellido,String correo,String contrasena){
        long admin=administrador(actor,contrasenaActual);
        long id=crear(nombre,apellido,correo,contrasena);
        jdbc.update("INSERT INTO lamontana.evento_soporte(id_usuario,tipo,recurso,resultado) VALUES (?,'ALTA_DESDE_APP',?,200)",admin,"soporte:"+id);
    }
    public Map<String,Object> estado(String actor){administrador(actor,null);var rows=jdbc.queryForList("SELECT codigo_publico,correo,lamontana.soporte_habilitado(id_usuario) AS habilitado FROM lamontana.usuario WHERE es_soporte");return Map.of("cuenta",rows,"autorizaciones",jdbc.queryForList("SELECT a.id_autorizacion_soporte,a.motivo,CASE WHEN a.estado='VIGENTE' AND a.fecha_fin<=clock_timestamp() THEN 'VENCIDA' ELSE a.estado END AS estado,a.fecha_inicio,a.fecha_fin,a.fecha_revocacion,u.correo AS autorizante FROM lamontana.autorizacion_soporte a JOIN lamontana.usuario u ON u.id_usuario=a.id_usuario_habilitante ORDER BY a.id_autorizacion_soporte DESC LIMIT 25"));}
    @Transactional
    public void autorizar(String actor,String contrasena,String motivo,int horas){
        long admin=administrador(actor,contrasena);long soporte=bloquear();cerrarAnteriores(soporte,admin);
        jdbc.update("INSERT INTO lamontana.autorizacion_soporte(id_usuario_soporte,id_usuario_habilitante,motivo,estado,fecha_inicio,fecha_fin) VALUES (?,?,?,'VIGENTE',statement_timestamp(),statement_timestamp()+(? * interval '1 hour'))",soporte,admin,motivo.strip(),horas);
        invalidar(soporte);jdbc.update("INSERT INTO lamontana.evento_soporte(id_usuario,tipo) VALUES (?,'AUTORIZACION_OTORGADA')",admin);
    }
    @Transactional
    public void revocar(String actor,String contrasena){long admin=administrador(actor,contrasena);long soporte=bloquear();cerrarAnteriores(soporte,admin);invalidar(soporte);jdbc.update("INSERT INTO lamontana.evento_soporte(id_usuario,tipo) VALUES (?,'AUTORIZACION_REVOCADA')",admin);}
    private void cerrarAnteriores(long soporte,long admin){jdbc.update("UPDATE lamontana.autorizacion_soporte SET estado=CASE WHEN fecha_fin<=statement_timestamp() THEN 'VENCIDA' ELSE 'REVOCADA' END,id_usuario_revocante=?,fecha_revocacion=clock_timestamp() WHERE id_usuario_soporte=? AND estado='VIGENTE'",admin,soporte);}
    private void invalidar(long id){jdbc.update("UPDATE lamontana.usuario SET version_acceso=version_acceso+1 WHERE id_usuario=?",id);jdbc.update("DELETE FROM lamontana.sesion_http WHERE principal_name=(SELECT correo FROM lamontana.usuario WHERE id_usuario=?)",id);}
    private long bloquear(){var ids=jdbc.query("SELECT id_usuario FROM lamontana.usuario WHERE es_soporte FOR UPDATE",(r,n)->r.getLong(1));if(ids.isEmpty())throw error(HttpStatus.CONFLICT,"Primero creá la cuenta SOPORTE desde esta pantalla de asistencia.");return ids.get(0);}
    private long administrador(String actor,String contrasena){var rows=jdbc.queryForList("SELECT u.id_usuario,u.hash_contrasena FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.correo=? AND u.estado='ACTIVO' AND NOT u.es_soporte AND r.codigo='ADMIN_ADMIN' AND r.activo FOR UPDATE OF u",actor);if(rows.isEmpty())throw error(HttpStatus.FORBIDDEN,"Solo un administrador del negocio puede gestionar soporte.");var row=rows.get(0);if(contrasena!=null&&!encoder.matches(contrasena,(String)row.get("hash_contrasena")))throw error(HttpStatus.BAD_REQUEST,"La contraseña actual no es correcta.");return ((Number)row.get("id_usuario")).longValue();}
    private ResponseStatusException error(HttpStatus status,String mensaje){return new ResponseStatusException(status,mensaje);}
}
