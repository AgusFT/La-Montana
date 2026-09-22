package ar.com.lamontana.configuracion;

import ar.com.lamontana.identidad.CorreoService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Participa en la transacción de la decisión final; nunca aplica cambios de negocio. */
@Component
public class SeguridadConfiguracion {
    private final JdbcTemplate jdbc;private final ConfiguracionService configuracion;private final PasswordEncoder encoder;private final CorreoService correo;
    private final SecureRandom random=new SecureRandom();
    public SeguridadConfiguracion(JdbcTemplate jdbc,ConfiguracionService configuracion,PasswordEncoder encoder,CorreoService correo){this.jdbc=jdbc;this.configuracion=configuracion;this.encoder=encoder;this.correo=correo;}
    public record Solicitud(String mensaje,UUID operacion,Instant vencimiento){}
    record Propietario(long id,String correo,String hash,long versionAcceso){}
    record Desafio(UUID operacion,String proposito,long idConfiguracion,UUID destino,long version,long actor,long versionAcceso,String correo,String huella,String hashCodigo,int intentos,Instant vencimiento,boolean consumido,boolean revocado){}
    record Permiso(Desafio desafio,boolean repetido){}

    Solicitud solicitar(UUID op,String proposito,UUID destino,long version,String huella,String contrasena,String actor,Runnable validar,String evento,String asunto){
        configuracion.bloquear();var u=propietario(actor);
        if(!encoder.matches(contrasena,u.hash()))throw error(HttpStatus.BAD_REQUEST,"La contraseña actual no es correcta.");
        var anterior=desafio(op);
        if(anterior!=null){
            contenido(anterior,proposito,destino,version,u.id(),huella);
            if(!anterior.consumido()){identidad(anterior,u);validar.run();if(anterior.intentos()>=5||!anterior.vencimiento().isAfter(ahora()))throw error(HttpStatus.CONFLICT,"La autorización anterior ya no puede utilizarse. Solicitá otra con una nueva operación.");}
            return respuesta(anterior);
        }
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.comprobante_configuracion WHERE id_operacion=?)",Boolean.class,op)))throw error(HttpStatus.CONFLICT,"Esa operación ya se usó para otro comando.");
        validar.run();
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.autorizacion_configuracion WHERE id_actor=? AND fecha_emision>clock_timestamp()-interval '60 seconds')",Boolean.class,u.id())))throw error(HttpStatus.TOO_MANY_REQUESTS,"Esperá un minuto antes de pedir otro código.");
        byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_actor=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",u.id());
        jdbc.update("""
            INSERT INTO lamontana.autorizacion_configuracion(id_operacion,proposito,id_configuracion_version,version_configuracion,id_actor,version_acceso,correo_destino,hash_solicitud,hash_codigo,fecha_vencimiento)
            VALUES (?,?,(SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?),?,?,?,?,?,?,clock_timestamp()+interval '15 minutes')
            """,op,proposito,destino,version,u.id(),u.versionAcceso(),u.correo(),huella,hash(token));
        var d=desafio(op);jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version) VALUES (?,?,?,?)",d.idConfiguracion(),u.id(),evento,version);
        correo.enviarCodigo(u.correo(),asunto,token);return respuesta(d);
    }
    Permiso comprobar(UUID op,String proposito,UUID destino,long version,String huella,String contrasena,String codigo,String actor){
        configuracion.bloquear();var u=propietario(actor);var d=desafio(op);
        if(!encoder.matches(contrasena,u.hash())){if(d!=null&&d.actor()==u.id())fallido(d);throw error(HttpStatus.BAD_REQUEST,"La contraseña actual no es correcta.");}
        if(d==null)throw invalido();contenido(d,proposito,destino,version,u.id(),huella);
        boolean coincide=MessageDigest.isEqual(hash(codigo.strip()).getBytes(StandardCharsets.US_ASCII),d.hashCodigo().getBytes(StandardCharsets.US_ASCII));
        if(d.consumido()){
            if(!coincide)throw invalido();
            if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.comprobante_configuracion WHERE id_operacion=? AND tipo=? AND id_configuracion_version=? AND id_actor=? AND hash_solicitud=?)",Boolean.class,op,proposito,d.idConfiguracion(),u.id(),huella)))throw new IllegalStateException("Autorización consumida sin comprobante transaccional.");
            return new Permiso(d,true);
        }
        identidad(d,u);if(d.intentos()>=5||!d.vencimiento().isAfter(ahora()))throw invalido();
        if(!coincide){fallido(d);throw invalido();}return new Permiso(d,false);
    }
    void exigirVigente(UUID op){var d=desafio(op);if(d==null)throw error(HttpStatus.CONFLICT,"No existe la autorización del intento.");var u=propietario(d.correo());identidad(d,u);if(!d.consumido())throw error(HttpStatus.CONFLICT,"El intento todavía no tiene autorización confirmada.");}
    void consumir(Permiso permiso){jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_consumo=clock_timestamp() WHERE id_operacion=?",permiso.desafio().operacion());}
    void revocar(UUID op,UUID destino,String actor){configuracion.bloquear();long usuario=configuracion.propietario(actor);var d=desafio(op);if(d==null)return;if(d.actor()!=usuario||!d.destino().equals(destino))throw error(HttpStatus.CONFLICT,"La autorización corresponde a otro destino o actor.");if(d.consumido())throw error(HttpStatus.CONFLICT,"La operación ya fue aplicada. Consultá el estado actual.");jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=coalesce(fecha_revocacion,clock_timestamp()) WHERE id_operacion=?",op);}
    private Propietario propietario(String actor){var rows=jdbc.query("SELECT u.id_usuario,u.correo,u.hash_contrasena,u.version_acceso FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.correo=? AND u.estado='ACTIVO' AND u.es_administrador_propietario AND r.codigo='ADMIN_ADMIN' AND r.activo FOR UPDATE OF u",(r,n)->new Propietario(r.getLong(1),r.getString(2),r.getString(3),r.getLong(4)),actor);if(rows.isEmpty())throw error(HttpStatus.FORBIDDEN,"Sólo el propietario activo puede autorizar la configuración operativa.");return rows.get(0);}
    private void contenido(Desafio d,String proposito,UUID destino,long version,long actor,String huella){if(!d.proposito().equals(proposito)||!d.destino().equals(destino)||d.version()!=version||d.actor()!=actor||!d.huella().equals(huella))throw error(HttpStatus.CONFLICT,"La autorización corresponde a otro contenido, versión, destino o actor.");}
    private void identidad(Desafio d,Propietario u){if(d.revocado()||d.versionAcceso()!=u.versionAcceso()||!d.correo().equals(u.correo()))throw error(HttpStatus.CONFLICT,"La autorización quedó invalidada. Revisá el borrador y solicitá un nuevo código.");}
    private void fallido(Desafio d){int changed=jdbc.update("UPDATE lamontana.autorizacion_configuracion SET intentos=intentos+1 WHERE id_operacion=? AND intentos<5 AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",d.operacion());if(changed>0)jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version,motivo) VALUES (?,?,'AUTORIZACION_RECHAZADA',?,'Credencial incorrecta')",d.idConfiguracion(),d.actor(),d.version());}
    private Desafio desafio(UUID op){var rows=jdbc.query("SELECT a.*,c.codigo_publico FROM lamontana.autorizacion_configuracion a JOIN lamontana.configuracion_version c USING(id_configuracion_version) WHERE a.id_operacion=?",(r,n)->new Desafio(r.getObject("id_operacion",UUID.class),r.getString("proposito"),r.getLong("id_configuracion_version"),r.getObject("codigo_publico",UUID.class),r.getLong("version_configuracion"),r.getLong("id_actor"),r.getLong("version_acceso"),r.getString("correo_destino"),r.getString("hash_solicitud"),r.getString("hash_codigo"),r.getInt("intentos"),r.getTimestamp("fecha_vencimiento").toInstant(),r.getTimestamp("fecha_consumo")!=null,r.getTimestamp("fecha_revocacion")!=null),op);return rows.isEmpty()?null:rows.get(0);}
    private Solicitud respuesta(Desafio d){return new Solicitud("Enviamos un código al correo del propietario para confirmar la operación.",d.operacion(),d.vencimiento());}
    private Instant ahora(){return jdbc.queryForObject("SELECT clock_timestamp()",Timestamp.class).toInstant();}
    private String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
    private ResponseStatusException invalido(){return error(HttpStatus.BAD_REQUEST,"El código no es válido, venció o agotó sus intentos. Solicitá uno nuevo si es necesario.");}
    private ResponseStatusException error(HttpStatus status,String m){return new ResponseStatusException(status,m);}
}
