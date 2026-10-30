package ar.com.lamontana.entregas;
import ar.com.lamontana.identidad.CorreoService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodigoRetiroService {
 private final JdbcTemplate jdbc;private final PasswordEncoder encoder;private final CodigoRetiroCifrado cipher;private final CorreoService mail;private final SecureRandom random=new SecureRandom();
 public CodigoRetiroService(JdbcTemplate jdbc,PasswordEncoder encoder,CodigoRetiroCifrado cipher,CorreoService mail){this.jdbc=jdbc;this.encoder=encoder;this.cipher=cipher;this.mail=mail;}
 /** Called only after the order has been authorized/read, in the same transaction as dispatch or quality. */
 @Transactional public void asegurar(long pedido){
  jdbc.execute("SELECT pg_advisory_xact_lock(764004)");
  var rows=jdbc.query("SELECT r.id_reserva_entrega,u.correo,(SELECT id_actor FROM lamontana.historial_estado_pedido h WHERE h.id_pedido=p.id_pedido AND h.accion IN ('SALIR_REPARTO','CONTROLAR_CALIDAD') ORDER BY id_evento DESC LIMIT 1) AS actor FROM lamontana.pedido p JOIN lamontana.reserva_entrega r USING(id_pedido) JOIN lamontana.usuario u ON u.id_usuario=p.id_usuario_creador WHERE p.id_pedido=? AND p.estado='LISTO_PARA_ENTREGA' AND r.estado='ACTIVA' AND lamontana.estado_logistico(p.id_pedido) IN ('LISTO_RETIRO','EN_VIAJE','DISPONIBLE_PUNTO')",(r,n)->new Object[]{r.getLong(1),r.getString(2),r.getObject(3,Long.class)},pedido);
  if(rows.isEmpty())return;var row=rows.get(0);long reservation=(long)row[0];if(row[2]==null)throw new IllegalStateException("El pedido no conserva el responsable de calidad o despacho.");
  var old=jdbc.query("SELECT id_codigo_entrega,codigo_cifrado,id_reserva_entrega FROM lamontana.codigo_entrega WHERE id_pedido=? AND revocado_en IS NULL AND utilizado_en IS NULL",(r,n)->new Object[]{r.getLong(1),r.getString(2),r.getLong(3)},pedido);
  if(!old.isEmpty()&&old.get(0)[1]!=null&&(long)old.get(0)[2]==reservation)return;
  Instant now=Instant.now();if(!old.isEmpty())jdbc.update("UPDATE lamontana.codigo_entrega SET revocado_en=? WHERE id_codigo_entrega=?",Timestamp.from(now),old.get(0)[0]);
  String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";var text=new StringBuilder();for(int i=0;i<8;i++)text.append(alphabet.charAt(random.nextInt(alphabet.length())));
  UUID id=UUID.randomUUID(),op=UUID.nameUUIDFromBytes((pedido+":"+reservation+":codigo-automatico").getBytes(StandardCharsets.UTF_8));String value=text.toString();
  String hash;try{hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(op.toString().getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  long code=jdbc.queryForObject("INSERT INTO lamontana.codigo_entrega(codigo_publico,id_pedido,id_reserva_entrega,id_emisor,id_operacion,huella,codigo_hash,emitido_en,vence_en,automatico,codigo_cifrado) VALUES (?,?,?,?,?,?,?,?,?,true,?) RETURNING id_codigo_entrega",Long.class,id,pedido,reservation,row[2],op,hash,encoder.encode(value),Timestamp.from(now),Timestamp.from(Instant.parse("9999-12-31T23:59:59Z")),cipher.cifrar(id,value));
  jdbc.update("INSERT INTO lamontana.aviso_codigo_entrega(id_codigo_entrega,correo) VALUES (?,?)",code,row[1]);
 }
 public String texto(UUID codigo,String cifrado){return cipher.descifrar(codigo,cifrado);}
 @Scheduled(fixedDelayString="${lamontana.entrega.avisos-intervalo-ms:15000}",initialDelayString="${lamontana.entrega.avisos-inicio-ms:15000}")
 @Transactional public void avisar(){
  var pending=jdbc.query("SELECT a.id_codigo_entrega,a.correo,c.codigo_publico,c.codigo_cifrado,c.id_pedido,r.nombre_destino,c.revocado_en,c.utilizado_en FROM lamontana.aviso_codigo_entrega a JOIN lamontana.codigo_entrega c USING(id_codigo_entrega) JOIN lamontana.reserva_entrega r USING(id_reserva_entrega) WHERE a.estado='PENDIENTE' AND a.proximo_intento<=clock_timestamp() ORDER BY a.id_codigo_entrega LIMIT 10 FOR UPDATE OF a SKIP LOCKED",(r,n)->new Object[]{r.getLong(1),r.getString(2),r.getObject(3,UUID.class),r.getString(4),r.getLong(5),r.getString(6),r.getTimestamp(7),r.getTimestamp(8)});
  for(var a:pending){if(a[6]!=null||a[7]!=null){jdbc.update("UPDATE lamontana.aviso_codigo_entrega SET estado='CANCELADO' WHERE id_codigo_entrega=?",a[0]);continue;}
   try{mail.enviarAviso((String)a[1],"Código de entrega · PED-"+a[4],"Tu pedido PED-"+a[4]+" tiene este código de entrega:\n\n"+texto((UUID)a[2],(String)a[3])+"\n\nDestino: "+a[5]+". Podés anotarlo y retirarlo sin llevar el celular cuando el seguimiento indique que está disponible. El código se conserva hasta entregar el pedido. El saldo debe estar resuelto para retirarlo.");jdbc.update("UPDATE lamontana.aviso_codigo_entrega SET estado='ENVIADO',enviado_en=clock_timestamp(),intentos=intentos+1 WHERE id_codigo_entrega=?",a[0]);}
   catch(RuntimeException e){jdbc.update("UPDATE lamontana.aviso_codigo_entrega SET intentos=intentos+1,proximo_intento=clock_timestamp()+interval '5 minutes' WHERE id_codigo_entrega=?",a[0]);}
  }
 }
}
