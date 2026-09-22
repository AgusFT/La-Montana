package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.ConfiguracionController.*;
import ar.com.lamontana.identidad.CorreoService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CancelacionConfiguracionService {
    private static final String PROPOSITO="CANCELAR_BORRADOR";
    private final JdbcTemplate jdbc;
    private final ConfiguracionService configuracion;
    private final PasswordEncoder encoder;
    private final CorreoService correo;
    private final JsonMapper json=JsonMapper.builder().build();
    private final SecureRandom random=new SecureRandom();
    public CancelacionConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion,PasswordEncoder encoder,CorreoService correo) {
        this.jdbc=jdbc;this.configuracion=configuracion;this.encoder=encoder;this.correo=correo;
    }
    public record Solicitud(String mensaje,UUID operacion,Instant vencimiento) {}
    private record Propietario(long id,String correo,String hash,long versionAcceso) {}
    private record Contenido(String proposito,UUID destino,long version,String motivo) {}
    private record Desafio(UUID operacion,long idConfiguracion,UUID destino,long version,long actor,long versionAcceso,String correo,
                            String huella,String hashCodigo,int intentos,Instant vencimiento,boolean consumido,boolean revocado) {}

    @Transactional
    public Solicitud solicitar(UUID destino,SolicitarCancelacion input,String actor) {
        configuracion.bloquear();Propietario u=propietario(actor);
        exigirContrasena(u,input.contrasena());
        String huella=contenido(destino,input.version(),input.motivo());
        Desafio anterior=desafio(input.operacion());
        if(anterior!=null) {
            exigirContenido(anterior,destino,input.version(),u.id(),huella);
            if(!anterior.consumido()) {
                exigirIdentidad(anterior,u);
                exigirSnapshot(configuracion.cargar(destino),input.version());
                if(anterior.intentos()>=5||!anterior.vencimiento().isAfter(ahora()))
                    throw error(HttpStatus.CONFLICT,"La autorización anterior ya no puede utilizarse. Solicitá otra con una nueva operación.");
            }
            return respuesta(anterior);
        }
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.comprobante_configuracion WHERE id_operacion=?)",Boolean.class,input.operacion())))
            throw error(HttpStatus.CONFLICT,"Esa operación ya se usó para otro comando.");
        exigirSnapshot(configuracion.cargar(destino),input.version());
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.autorizacion_configuracion WHERE id_actor=? AND fecha_emision>clock_timestamp()-interval '60 seconds')",Boolean.class,u.id())))
            throw error(HttpStatus.TOO_MANY_REQUESTS,"Esperá un minuto antes de pedir otro código.");
        byte[] bytes=new byte[32];random.nextBytes(bytes);String codigo=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_actor=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",u.id());
        jdbc.update("""
                INSERT INTO lamontana.autorizacion_configuracion(id_operacion,proposito,id_configuracion_version,version_configuracion,id_actor,
                    version_acceso,correo_destino,hash_solicitud,hash_codigo,fecha_vencimiento)
                VALUES (?,'CANCELAR_BORRADOR',(SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?),?,?,?,?,?,?,clock_timestamp()+interval '15 minutes')
                """,input.operacion(),destino,input.version(),u.id(),u.versionAcceso(),u.correo(),huella,hash(codigo));
        Desafio emitido=desafio(input.operacion());
        jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version) VALUES (?,?,'CANCELACION_SOLICITADA',?)",emitido.idConfiguracion(),u.id(),input.version());
        correo.enviarCodigo(u.correo(),"Cancelar borrador de configuración",codigo);
        return respuesta(emitido);
    }

    // Sólo los rechazos de validación conservan los intentos; fallos de almacenamiento revierten todos los cambios.
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public ConfiguracionService.Borrador confirmar(UUID destino,ConfirmarCancelacion input,String actor) {
        configuracion.bloquear();Propietario u=propietario(actor);
        Desafio d=desafio(input.operacion());
        if(!encoder.matches(input.contrasena(),u.hash())) {
            if(d!=null&&d.actor()==u.id()) intentoFallido(d);
            throw error(HttpStatus.BAD_REQUEST,"La contraseña actual no es correcta.");
        }
        if(d==null) throw invalido();
        String huella=contenido(destino,input.version(),input.motivo());
        exigirContenido(d,destino,input.version(),u.id(),huella);
        boolean coincide=MessageDigest.isEqual(hash(input.codigo().strip()).getBytes(StandardCharsets.US_ASCII),d.hashCodigo().getBytes(StandardCharsets.US_ASCII));
        if(d.consumido()) {
            // Un resultado perdido puede recuperarse con su código original: no vuelve a autorizar ninguna operación.
            if(!coincide) throw invalido();
            boolean comprobado=Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM lamontana.comprobante_configuracion
                    WHERE id_operacion=? AND tipo='CANCELAR_BORRADOR' AND id_configuracion_version=? AND id_actor=? AND hash_solicitud=?)
                    """,Boolean.class,input.operacion(),d.idConfiguracion(),u.id(),huella));
            if(!comprobado) throw new IllegalStateException("Cancelación consumida sin comprobante transaccional.");
            return configuracion.cargar(destino);
        }
        exigirIdentidad(d,u);
        var borrador=configuracion.cargar(destino);exigirSnapshot(borrador,input.version());
        if(d.intentos()>=5||!d.vencimiento().isAfter(ahora())) throw invalido();
        if(!coincide) { intentoFallido(d);throw invalido(); }
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_consumo=clock_timestamp() WHERE id_operacion=?",input.operacion());
        jdbc.update("""
                UPDATE lamontana.configuracion_version SET estado='CANCELADA',version=version+1,fecha_actualizacion=clock_timestamp(),
                    id_usuario_cancelador=?,fecha_cancelacion=clock_timestamp(),motivo_cancelacion=? WHERE id_configuracion_version=?
                """,u.id(),input.motivo().strip(),d.idConfiguracion());
        configuracion.registrar(input.operacion(),PROPOSITO,d.idConfiguracion(),u.id(),huella,"BORRADOR_CANCELADO",borrador.version()+1);
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",d.idConfiguracion());
        return configuracion.cargar(destino);
    }

    private Propietario propietario(String actor) {
        var propietarios=jdbc.query("""
                SELECT u.id_usuario,u.correo,u.hash_contrasena,u.version_acceso FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                WHERE u.correo=? AND u.estado='ACTIVO' AND u.es_administrador_propietario AND r.codigo='ADMIN_ADMIN' AND r.activo FOR UPDATE OF u
                """,(rs,row)->new Propietario(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getLong(4)),actor);
        if(propietarios.isEmpty()) throw error(HttpStatus.FORBIDDEN,"Sólo el propietario activo puede cancelar la configuración operativa.");
        return propietarios.get(0);
    }
    private void exigirContrasena(Propietario u,String contrasena) {
        if(!encoder.matches(contrasena,u.hash())) throw error(HttpStatus.BAD_REQUEST,"La contraseña actual no es correcta.");
    }
    private void exigirContenido(Desafio d,UUID destino,long version,long actor,String huella) {
        if(!d.destino().equals(destino)||d.version()!=version||d.actor()!=actor||!d.huella().equals(huella))
            throw error(HttpStatus.CONFLICT,"La autorización corresponde a otro contenido, versión, destino o actor.");
    }
    private void exigirIdentidad(Desafio d,Propietario u) {
        if(d.revocado()||d.versionAcceso()!=u.versionAcceso()||!d.correo().equals(u.correo()))
            throw error(HttpStatus.CONFLICT,"La autorización quedó invalidada. Revisá el borrador y solicitá un nuevo código.");
    }
    private void exigirSnapshot(ConfiguracionService.Borrador borrador,long version) {
        if(!borrador.estado().equals("EN_PREPARACION")||borrador.version()!=version)
            throw error(HttpStatus.CONFLICT,"El borrador cambió desde la solicitud. Revisá su versión actual antes de cancelar.");
    }
    private void intentoFallido(Desafio d) {
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET intentos=intentos+1 WHERE id_operacion=? AND intentos<5 AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",d.operacion());
    }
    private Desafio desafio(UUID operacion) {
        var encontrados=jdbc.query("""
                SELECT a.*,c.codigo_publico FROM lamontana.autorizacion_configuracion a
                JOIN lamontana.configuracion_version c USING(id_configuracion_version)
                WHERE a.id_operacion=? AND a.proposito='CANCELAR_BORRADOR'
                """,(rs,row)->new Desafio(rs.getObject("id_operacion",UUID.class),rs.getLong("id_configuracion_version"),rs.getObject("codigo_publico",UUID.class),
                        rs.getLong("version_configuracion"),rs.getLong("id_actor"),rs.getLong("version_acceso"),rs.getString("correo_destino"),rs.getString("hash_solicitud"),
                        rs.getString("hash_codigo"),rs.getInt("intentos"),rs.getTimestamp("fecha_vencimiento").toInstant(),
                        rs.getTimestamp("fecha_consumo")!=null,rs.getTimestamp("fecha_revocacion")!=null),operacion);
        return encontrados.isEmpty()?null:encontrados.get(0);
    }
    private Solicitud respuesta(Desafio d) { return new Solicitud("Enviamos un código al correo del propietario para confirmar la cancelación.",d.operacion(),d.vencimiento()); }
    private Instant ahora() { return jdbc.queryForObject("SELECT clock_timestamp()",Timestamp.class).toInstant(); }
    private String contenido(UUID destino,long version,String motivo) { return hash(json.writeValueAsString(new Contenido(PROPOSITO,destino,version,motivo))); }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private ResponseStatusException invalido() { return error(HttpStatus.BAD_REQUEST,"El código no es válido, venció o agotó sus intentos. Solicitá uno nuevo si es necesario."); }
    private ResponseStatusException error(HttpStatus status,String mensaje) { return new ResponseStatusException(status,mensaje); }
}
