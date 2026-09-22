package ar.com.lamontana.identidad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CredencialService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final CorreoService correo;
    private final SecureRandom random=new SecureRandom();
    private enum Tipo {
        VERIFICACION("token_verificacion_correo","id_token_verificacion_correo","hash_token","Verificar correo"),
        RECUPERACION("credencial_temporal","id_credencial_temporal","hash_codigo","Recuperar acceso");
        final String tabla,id,hash,asunto;
        Tipo(String tabla,String id,String hash,String asunto) { this.tabla=tabla;this.id=id;this.hash=hash;this.asunto=asunto; }
    }
    private record Usuario(long id,String correo,String hash,boolean verificado) {}
    public CredencialService(JdbcTemplate jdbc,PasswordEncoder encoder,CorreoService correo) { this.jdbc=jdbc;this.encoder=encoder;this.correo=correo; }

    @Transactional
    public void solicitarVerificacion(String email) {
        Usuario u=usuario(email);
        if(u==null) throw error(HttpStatus.UNAUTHORIZED,"Iniciá sesión para continuar.");
        if(u.verificado()) return;
        emitir(u,Tipo.VERIFICACION);
    }
    @Transactional
    public void solicitarRecuperacion(String email) {
        Usuario u=usuario(email);
        if(u!=null) emitir(u,Tipo.RECUPERACION);
    }
    // Los intentos inválidos deben persistir aunque la respuesta sea un error de validación.
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public void confirmarVerificacion(String email,String token) {
        Usuario u=usuario(email);
        if(u==null) throw invalido();
        Desafio desafio=validar(u,Tipo.VERIFICACION,token);
        consumir(Tipo.VERIFICACION,desafio);
        jdbc.update("UPDATE lamontana.usuario SET correo_verificado_en=now() WHERE id_usuario=?",u.id());
        evento(u.id(),"CORREO_VERIFICADO");
    }
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public void recuperar(String email,String token,String nueva) {
        Usuario u=usuario(email);
        if(u==null) throw invalido();
        Desafio desafio=validar(u,Tipo.RECUPERACION,token);
        diferente(u,nueva);
        consumir(Tipo.RECUPERACION,desafio);
        actualizarClave(u,nueva,"ACCESO_RESTABLECIDO");
    }
    @Transactional
    public void cambiarClave(String email,String actual,String nueva) {
        Usuario u=usuario(email);
        if(u==null || !encoder.matches(actual,u.hash())) throw error(HttpStatus.BAD_REQUEST,"La contraseña actual no es correcta.");
        diferente(u,nueva);
        actualizarClave(u,nueva,"CONTRASENA_CAMBIADA");
    }
    private void actualizarClave(Usuario u,String nueva,String tipo) {
        jdbc.update("UPDATE lamontana.usuario SET hash_contrasena=?,debe_cambiar_contrasena=false,version=version+1 WHERE id_usuario=?",encoder.encode(nueva),u.id());
        revocar(u.id());
        jdbc.update("DELETE FROM lamontana.sesion_http WHERE principal_name=?",u.correo());
        evento(u.id(),tipo);
    }
    public void revocar(long usuario) {
        jdbc.update("UPDATE lamontana.usuario SET version_acceso=version_acceso+1 WHERE id_usuario=?",usuario);
        for(Tipo tipo:Tipo.values()) jdbc.update("UPDATE lamontana."+tipo.tabla+" SET fecha_revocacion=now() WHERE id_usuario=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",usuario);
    }
    private Usuario usuario(String email) {
        return jdbc.query("""
                SELECT u.id_usuario,u.correo,u.hash_contrasena,u.correo_verificado_en IS NOT NULL AS verificado
                FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                WHERE u.correo=? AND u.estado='ACTIVO' AND r.activo FOR UPDATE OF u
                """,(rs,row)->new Usuario(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getBoolean(4)),email.strip().toLowerCase(Locale.ROOT))
                .stream().findFirst().orElse(null);
    }
    private void emitir(Usuario u,Tipo tipo) {
        boolean reciente=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana."+tipo.tabla+" WHERE id_usuario=? AND fecha_emision>now()-interval '60 seconds')",Boolean.class,u.id()));
        if(reciente) {
            if(tipo==Tipo.VERIFICACION) throw error(HttpStatus.TOO_MANY_REQUESTS,"Esperá un minuto antes de pedir otro código.");
            return;
        }
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("UPDATE lamontana."+tipo.tabla+" SET fecha_revocacion=now() WHERE id_usuario=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",u.id());
        String proposito=tipo==Tipo.RECUPERACION ? ",proposito" : "";
        String valor=tipo==Tipo.RECUPERACION ? ",'RESTABLECIMIENTO'" : "";
        jdbc.update("INSERT INTO lamontana."+tipo.tabla+"(id_usuario,"+tipo.hash+",correo_destino,fecha_vencimiento"+proposito+") VALUES (?,?,?,now()+interval '15 minutes'"+valor+")",u.id(),hash(token),u.correo());
        correo.enviarCodigo(u.correo(),tipo.asunto,token);
        evento(u.id(),tipo==Tipo.VERIFICACION?"VERIFICACION_SOLICITADA":"RECUPERACION_SOLICITADA");
    }
    private Desafio validar(Usuario u,Tipo tipo,String token) {
        var filas=jdbc.query("SELECT "+tipo.id+","+tipo.hash+",correo_destino,intentos,fecha_vencimiento>now() AS vigente FROM lamontana."+tipo.tabla+" WHERE id_usuario=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL ORDER BY fecha_emision DESC,"+tipo.id+" DESC LIMIT 1 FOR UPDATE",
                (rs,row)->new Desafio(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getInt(4),rs.getBoolean(5)),u.id());
        if(filas.isEmpty()) throw invalido();
        Desafio d=filas.get(0);
        if(!d.vigente() || d.intentos()>=5 || !d.correo().equals(u.correo())) throw invalido();
        if(!MessageDigest.isEqual(hash(token.strip()).getBytes(StandardCharsets.US_ASCII),d.hash().getBytes(StandardCharsets.US_ASCII))) {
            jdbc.update("UPDATE lamontana."+tipo.tabla+" SET intentos=intentos+1 WHERE "+tipo.id+"=?",d.id());
            throw invalido();
        }
        return d;
    }
    private void consumir(Tipo tipo,Desafio d) {
        jdbc.update("UPDATE lamontana."+tipo.tabla+" SET fecha_consumo=now() WHERE "+tipo.id+"=?",d.id());
    }
    private record Desafio(long id,String hash,String correo,int intentos,boolean vigente) {}
    private void diferente(Usuario u,String nueva) { if(encoder.matches(nueva,u.hash())) throw error(HttpStatus.BAD_REQUEST,"Elegí una contraseña diferente de la actual."); }
    private void evento(long usuario,String tipo) { jdbc.update("INSERT INTO lamontana.evento_acceso(tipo,id_usuario) VALUES (?,?)",tipo,usuario); }
    private ResponseStatusException invalido() { return error(HttpStatus.BAD_REQUEST,"El código no es válido, venció o ya fue utilizado. Solicitá uno nuevo si es necesario."); }
    private ResponseStatusException error(HttpStatus status,String mensaje) { return new ResponseStatusException(status,mensaje); }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
