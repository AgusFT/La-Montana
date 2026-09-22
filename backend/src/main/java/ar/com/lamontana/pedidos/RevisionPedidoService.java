package ar.com.lamontana.pedidos;

import static ar.com.lamontana.pedidos.RevisionPedidoController.*;
import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.organizacion.OrganizacionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RevisionPedidoService {
 private final JdbcTemplate jdbc;
 private final PedidoService pedidos;
 private final OrganizacionService organizacion;
 private final ArchivosPrivados archivos;
 private final JsonMapper json=JsonMapper.builder().build();
 public RevisionPedidoService(JdbcTemplate jdbc,PedidoService pedidos,OrganizacionService organizacion,ArchivosPrivados archivos){
  this.jdbc=jdbc;this.pedidos=pedidos;this.organizacion=organizacion;this.archivos=archivos;
 }
 public record Opcion(Accion accion,boolean habilitada,String motivo){}
 public record Nota(UUID codigoPublico,String autor,Importancia importancia,String texto,Instant fecha){}
 public record Evento(String accion,String estadoOrigen,String estadoDestino,String actor,String motivo,String mensajeCliente,Instant fecha){}
 public record Gestion(long version,String cliente,String correo,Instant revisadaEn,String revisor,String aprobador,
   boolean puedeObservar,List<Opcion> acciones,List<Nota> observaciones,List<Evento> historial){}
 private record Actor(long id,String nombre,String rol){}
 private record Estado(Instant revisada,String revisor,String aprobador){}

 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Gestion consultar(UUID id,String correo){var d=pedidos.detalle(id,correo,true);return gestion(d,correo);}

 private Gestion gestion(PedidoService.Detalle d,String correo){
  var estado=estado(d.numero());boolean permiso=permiso(correo);
  var actions=Arrays.stream(Accion.values()).map(a->{String motivo=bloqueo(d,estado,a,permiso);return new Opcion(a,motivo==null,motivo);}).toList();
  var notas=jdbc.query("SELECT codigo_publico,autor_nombre,importancia,texto,fecha FROM lamontana.observacion_interna WHERE id_pedido=? ORDER BY id_observacion_interna DESC",(r,n)->new Nota(r.getObject(1,UUID.class),r.getString(2),Importancia.valueOf(r.getString(3)),r.getString(4),r.getTimestamp(5).toInstant()),d.numero());
  var historial=jdbc.query("SELECT accion,estado_origen,estado_destino,actor_nombre,motivo,mensaje_cliente,fecha FROM lamontana.historial_estado_pedido WHERE id_pedido=? AND version_pedido IS NOT NULL ORDER BY id_evento",(r,n)->new Evento(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getTimestamp(7).toInstant()),d.numero());
  var cliente=jdbc.queryForObject("SELECT u.nombre||' '||u.apellido,u.correo FROM lamontana.pedido p JOIN lamontana.usuario u ON u.id_usuario=p.id_usuario_creador WHERE p.id_pedido=?",(r,n)->new String[]{r.getString(1),r.getString(2)},d.numero());
  return new Gestion(d.version(),cliente[0],cliente[1],estado.revisada(),estado.revisor(),estado.aprobador(),permiso,actions,notas,historial);
 }
 @Transactional public PedidoService.Detalle decidir(UUID id,Decision in,String correo){
  bloquear();var d=pedidos.detalle(id,correo,true);organizacion.exigirPermiso(correo,"GESTIONAR_PEDIDOS");var a=actor(correo);
  String hash=huella(List.of(id,"DECISION",in));
  if(recuperar(in.operacion(),hash,d,a))return d;
  version(d,in.version());String bloqueo=bloqueo(d,estado(d.numero()),in.accion(),true);if(bloqueo!=null)throw conflicto(bloqueo);
  texto(in.motivo(),500);
  if(in.accion()==Accion.REVISAR||in.accion()==Accion.APROBAR){
   if(!in.archivosYDatosRevisados())throw error(HttpStatus.BAD_REQUEST,"Confirmá que revisaste los PDF y los datos del pedido.");
   verificarPdf(d);
  }
  String publico=switch(in.accion()){
   case REVISAR -> "La imprenta registró la revisión de los PDF y los datos. El pedido sigue pendiente de decisión.";
   case APROBAR -> "La imprenta aprobó el pedido. La producción requiere cumplir las condiciones de pago aceptadas y que un usuario interno la inicie.";
   case RECHAZAR,CANCELAR -> {texto(in.mensajeCliente(),500);yield in.mensajeCliente().strip();}
  };
  transicion(d,a,in.operacion(),hash,in.accion(),in.motivo().strip(),publico);
  return pedidos.detalle(id,correo,true);
 }
 @Transactional public PedidoService.Detalle cancelar(UUID id,Cancelacion in,String correo){
  bloquear();var d=pedidos.detalle(id,correo,false);var a=actor(correo);String hash=huella(List.of(id,"CANCELACION_CLIENTE",in));
  if(recuperar(in.operacion(),hash,d,a))return d;
  version(d,in.version());texto(in.motivo(),500);
  if(!Set.of("PENDIENTE_REVISION","APROBADO").contains(d.estado()))throw conflicto("El pedido ya no admite cancelación directa antes de producción.");
  transicion(d,a,in.operacion(),hash,Accion.CANCELAR,in.motivo().strip(),in.motivo().strip());
  return pedidos.detalle(id,correo,false);
 }
 @Transactional public Gestion observar(UUID id,Observacion in,String correo){
  bloquear();var d=pedidos.detalle(id,correo,true);organizacion.exigirPermiso(correo,"GESTIONAR_PEDIDOS");var a=actor(correo);String hash=huella(List.of(id,"OBSERVACION",in));
  if(recuperar(in.operacion(),hash,d,a))return gestion(d,correo);
  version(d,in.version());texto(in.texto(),2000);
  jdbc.update("INSERT INTO lamontana.observacion_interna(codigo_publico,id_pedido,id_autor,autor_nombre,id_operacion,huella,importancia,texto,fecha) VALUES (?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),d.numero(),a.id(),a.nombre(),in.operacion(),hash,in.importancia().name(),in.texto().strip(),Timestamp.from(Instant.now()));
  return gestion(d,correo);
 }
 private void transicion(PedidoService.Detalle d,Actor a,UUID op,String hash,Accion accion,String motivo,String publico){
  Instant ahora=Instant.now();String destino=switch(accion){case REVISAR->d.estado();case APROBAR->"APROBADO";case RECHAZAR->"RECHAZADO";case CANCELAR->"CANCELADO";};
  switch(accion){
   case REVISAR -> jdbc.update("UPDATE lamontana.pedido SET revisada_en=?,id_revisor=?,version=version+1 WHERE id_pedido=?",Timestamp.from(ahora),a.id(),d.numero());
   case APROBAR -> jdbc.update("UPDATE lamontana.pedido SET estado='APROBADO',aprobada_en=?,id_aprobador=?,version=version+1 WHERE id_pedido=?",Timestamp.from(ahora),a.id(),d.numero());
   case RECHAZAR,CANCELAR -> {
    jdbc.update("UPDATE lamontana.pedido SET estado=?,finalizada_en=?,version=version+1 WHERE id_pedido=?",destino,Timestamp.from(ahora),d.numero());
    jdbc.update("UPDATE lamontana.reserva_entrega SET estado='LIBERADA' WHERE id_pedido=?",d.numero());
   }
  }
  jdbc.update("INSERT INTO lamontana.historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha,estado_origen,version_pedido,accion,mensaje_cliente,actor_nombre,actor_tipo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
   d.numero(),a.id(),op,hash,destino,motivo,Timestamp.from(ahora),d.estado(),d.version()+1,accion.name(),publico,a.nombre(),a.rol());
 }
 private String bloqueo(PedidoService.Detalle d,Estado e,Accion accion,boolean permiso){
  if(!permiso)return "Se requiere el permiso de gestión de pedidos en esta sucursal.";
  if(!Set.of("PENDIENTE_REVISION","APROBADO").contains(d.estado()))return "El pedido terminó su recorrido operativo. Se conservan el historial, los archivos y los pagos.";
  if((accion==Accion.REVISAR||accion==Accion.APROBAR)&&!jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.sucursal WHERE codigo_publico=? AND estado='ACTIVA')",Boolean.class,d.oferta().sucursal().codigoPublico()))return "La sucursal está desactivada. No admite revisión ni aprobación; el administrador conserva consulta, notas y cancelación.";
  return switch(accion){
   case REVISAR -> !d.estado().equals("PENDIENTE_REVISION")?"La aprobación ya fue registrada.":e.revisada()!=null?"La revisión de estos PDF ya está registrada.":null;
   case APROBAR -> !d.estado().equals("PENDIENTE_REVISION")?"El pedido ya está aprobado.":e.revisada()==null?"Registrá primero la revisión de los PDF y los datos.":null;
   case RECHAZAR -> !d.estado().equals("PENDIENTE_REVISION")?"El rechazo corresponde a la etapa de revisión. Antes de producción podés cancelar con motivo.":null;
   case CANCELAR -> null;
  };
 }
 private void verificarPdf(PedidoService.Detalle d){
  if(d.archivos().size()!=d.oferta().items().size())throw conflicto("El pedido no tiene todos sus PDF aceptados.");
  for(var file:d.archivos()){
   if(file.aceptadaEn()==null)throw conflicto("Falta aceptar una vista previa.");
   try{archivos.original(file.archivo(),file.sha256(),file.bytes());}
   catch(java.io.IOException ex){throw conflicto("No se pudo verificar un PDF confirmado. Conservá el pedido y recuperá su original privado antes de aprobar.");}
  }
 }
 private boolean recuperar(UUID op,String hash,PedidoService.Detalle d,Actor actor){
  var rows=jdbc.query("SELECT id_pedido,id_actor,huella FROM lamontana.historial_estado_pedido WHERE id_operacion=? UNION ALL SELECT id_pedido,id_autor,huella FROM lamontana.observacion_interna WHERE id_operacion=?",(r,n)->new Object[]{r.getLong(1),r.getLong(2),r.getString(3)},op,op);
  if(rows.isEmpty())return false;var old=rows.get(0);
  if(rows.size()!=1||(long)old[0]!=d.numero()||(long)old[1]!=actor.id()||!hash.equals(old[2]))throw conflicto("La operación ya fue utilizada con otros datos.");
  return true;
 }
 private Estado estado(long id){return jdbc.queryForObject("SELECT p.revisada_en,r.nombre||' '||r.apellido,a.nombre||' '||a.apellido FROM lamontana.pedido p LEFT JOIN lamontana.usuario r ON r.id_usuario=p.id_revisor LEFT JOIN lamontana.usuario a ON a.id_usuario=p.id_aprobador WHERE p.id_pedido=?",(r,n)->new Estado(r.getTimestamp(1)==null?null:r.getTimestamp(1).toInstant(),r.getString(2),r.getString(3)),id);}
 private boolean permiso(String correo){return organizacion.contexto(correo).permisos().contains("GESTIONAR_PEDIDOS");}
 private Actor actor(String correo){return jdbc.queryForObject("SELECT u.id_usuario,u.nombre||' '||u.apellido,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.activo",(r,n)->new Actor(r.getLong(1),r.getString(2),r.getString(3)),correo);}
 private void version(PedidoService.Detalle d,long version){if(d.version()!=version)throw conflicto("El pedido cambió. Actualizá su estado antes de decidir.");}
 private void texto(String texto,int max){if(texto==null||texto.isBlank()||texto.length()>max||texto.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw error(HttpStatus.BAD_REQUEST,"Completá el texto requerido sin caracteres de control.");}
 private void bloquear(){for(long id:new long[]{764003,764001,764002,764004})jdbc.execute("SELECT pg_advisory_xact_lock("+id+")");}
 private String huella(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private ResponseStatusException conflicto(String msg){return error(HttpStatus.CONFLICT,msg);}
 private ResponseStatusException error(HttpStatus code,String msg){return new ResponseStatusException(code,msg);}
}
