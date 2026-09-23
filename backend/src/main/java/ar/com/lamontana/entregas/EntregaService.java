package ar.com.lamontana.entregas;
import static ar.com.lamontana.entregas.EntregaController.*;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.CoberturaPagos;
import ar.com.lamontana.pedidos.PedidoService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
@Service
public class EntregaService {
 private final JdbcTemplate jdbc;private final PedidoService pedidos;private final OrganizacionService organizacion;private final CoberturaPagos cobertura;private final PasswordEncoder encoder;
 private final SecureRandom random=new SecureRandom();private final Clock clock=Clock.systemUTC();private final JsonMapper json=JsonMapper.builder().build();
 public EntregaService(JdbcTemplate jdbc,PedidoService pedidos,OrganizacionService organizacion,CoberturaPagos cobertura,PasswordEncoder encoder){this.jdbc=jdbc;this.pedidos=pedidos;this.organizacion=organizacion;this.cobertura=cobertura;this.encoder=encoder;}
 public record Opcion(Accion accion,boolean habilitada,String motivo){}
 public record Codigo(UUID codigoPublico,String estado,Instant emitidoEn,Instant venceEn,int intentosRestantes,Instant puedeRenovarEn){}
 public record Verificacion(UUID codigoPublico,Instant fecha,Instant validoHasta){}
 public record Constancia(UUID codigoPublico,String receptor,String responsable,Instant fecha,String motivo){}
 public record Evento(String accion,String estadoOrigen,String estadoDestino,String actor,String motivo,Instant fecha){}
 public record Cierre(Instant fecha,String responsable,String motivo,String total,String aplicado){}
 public record Vista(long version,String estadoPedido,String estadoLogistico,String cliente,String correo,boolean puedeGestionar,boolean puedeCerrar,boolean puedeEmitirCodigo,List<Opcion> acciones,
  Codigo codigo,Verificacion validacion,Constancia entrega,Cierre cierre,String aplicado,String saldo,List<String> bloqueosEntrega,List<String> bloqueosCierre,List<Evento> historial){}
 public record Generacion(Vista vista,String codigo,String aviso){@Override public String toString(){return "Generacion[código privado]";}}
 public record Validacion(Vista vista,boolean aceptado,String mensaje){}
 private record Actor(long id,String nombre,String rol){}
 private record Secreto(long id,UUID codigo,String hash,long reserva,Instant emision,Instant vence,Instant revocado,Instant utilizado,int intentos){}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Vista consultar(UUID pedido,String correo,boolean interno){return vista(pedidos.detalle(pedido,correo,interno),correo,interno);}
 private Vista vista(PedidoService.Detalle p,String correo,boolean interno){
  var a=actor(correo);var permisos=interno?organizacion.contexto(correo).permisos():List.<String>of();boolean gestiona=permisos.contains("GESTIONAR_ENTREGAS"),cierra=permisos.contains("CERRAR_PEDIDOS");String estado=logistica(p);
  var code=ultimoCodigo(p.numero());Instant ahora=clock.instant();Codigo codigo=code==null?null:new Codigo(code.codigo(),estadoCodigo(code,ahora),code.emision(),code.vence(),Math.max(0,5-code.intentos()),code.emision().plusSeconds(30));
  var validation=interno?verificacion(p,code,a.id(),ahora):null;
  var entrega=jdbc.query("SELECT e.codigo_publico,e.receptor_nombre,h.actor_nombre,e.fecha_entrega,e.nota FROM lamontana.entrega e JOIN lamontana.historial_estado_pedido h ON h.id_evento=e.id_evento_pedido WHERE e.id_pedido=?",(r,n)->new Constancia(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),interno?r.getString(5):null),p.numero());
  var cierre=jdbc.query("SELECT c.fecha_cierre,h.actor_nombre,h.motivo,c.total,c.aplicado FROM lamontana.cierre_pedido c JOIN lamontana.historial_estado_pedido h ON h.id_evento=c.id_evento_pedido WHERE c.id_pedido=?",(r,n)->new Cierre(r.getTimestamp(1).toInstant(),r.getString(2),interno?r.getString(3):null,r.getBigDecimal(4).toPlainString(),r.getBigDecimal(5).toPlainString()),p.numero());
  var history=jdbc.query("SELECT h.accion,m.estado_origen,m.estado_destino,h.actor_nombre,h.motivo,h.mensaje_cliente,h.fecha FROM lamontana.movimiento_entrega m JOIN lamontana.historial_estado_pedido h ON h.id_evento=m.id_evento_pedido WHERE m.id_pedido=? ORDER BY m.id_movimiento_entrega",(r,n)->new Evento(r.getString(1),r.getString(2),r.getString(3),interno?r.getString(4):"La imprenta",interno?r.getString(5):r.getString(6),r.getTimestamp(7).toInstant()),p.numero());
  var options=Arrays.stream(Accion.values()).map(op->{String msg=bloqueoMovimiento(p,estado,op,gestiona);return new Opcion(op,msg==null,msg);}).toList();
  var cliente=jdbc.queryForObject("SELECT u.nombre||' '||u.apellido,u.correo FROM lamontana.pedido p JOIN lamontana.usuario u ON u.id_usuario=p.id_usuario_creador WHERE p.id_pedido=?",(r,n)->new String[]{r.getString(1),r.getString(2)},p.numero());
  var b=new ArrayList<>(bloqueosEntrega(p));if(interno&&!gestiona)b.add("Se requiere permiso de gestión de entregas.");if(interno&&validation==null)b.add("Validá el código presentado por el cliente con tu propia sesión.");
  var bc=new ArrayList<>(bloqueosCierre(p));if(interno&&!cierra)bc.add("Se requiere permiso para cerrar pedidos.");
  BigDecimal aplicado=aplicado(p);return new Vista(p.version(),p.estado(),estado,cliente[0],interno?cliente[1]:null,gestiona,cierra,!interno&&p.estado().equals("LISTO_PARA_ENTREGA")&&(code==null||!ahora.isBefore(code.emision().plusSeconds(30))),options,codigo,validation,entrega.isEmpty()?null:entrega.get(0),cierre.isEmpty()?null:cierre.get(0),dinero(aplicado),dinero(new BigDecimal(p.oferta().total()).subtract(aplicado).max(BigDecimal.ZERO)),List.copyOf(b),List.copyOf(bc),history);
 }
 @Transactional public Generacion emitir(UUID pedido,Emitir in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,false);var a=actor(correo);String hash=huella(List.of(pedido,"CODIGO",in));
  if(recuperar(in.operacion(),hash,p,a))return new Generacion(vista(p,correo,false),null,"El código ya fue emitido. Por seguridad no se conserva su texto; podés generar otro cuando termine la espera indicada.");
  version(p,in.version());if(!p.estado().equals("LISTO_PARA_ENTREGA"))throw conflicto("El código está disponible después de superar calidad y antes de entregar.");
  Instant ahora=clock.instant();var anterior=ultimoCodigo(p.numero());if(anterior!=null&&ahora.isBefore(anterior.emision().plusSeconds(30)))throw conflicto("Esperá 30 segundos desde la última emisión antes de generar otro código.");
  jdbc.update("UPDATE lamontana.codigo_entrega SET revocado_en=? WHERE id_pedido=? AND revocado_en IS NULL AND utilizado_en IS NULL",Timestamp.from(ahora),p.numero());
  String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";var code=new StringBuilder();for(int i=0;i<8;i++)code.append(alphabet.charAt(random.nextInt(alphabet.length())));String texto=code.toString();
  jdbc.update("INSERT INTO lamontana.codigo_entrega(codigo_publico,id_pedido,id_reserva_entrega,id_emisor,id_operacion,huella,codigo_hash,emitido_en,vence_en) VALUES (?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),p.numero(),reserva(p),a.id(),in.operacion(),hash,encoder.encode(texto),Timestamp.from(ahora),Timestamp.from(ahora.plusSeconds(1800)));
  return new Generacion(vista(p,correo,false),texto,"Mostrá este código al personal al recibir el pedido. Vence en 30 minutos y reemplaza cualquier código anterior.");
 }
 @Transactional public Vista mover(UUID pedido,Movimiento in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,true);organizacion.exigirPermiso(correo,"GESTIONAR_ENTREGAS");var a=actor(correo);String hash=huella(List.of(pedido,"MOVIMIENTO",in));
  if(recuperar(in.operacion(),hash,p,a))return vista(p,correo,true);version(p,in.version());texto(in.motivo(),500);confirmar(in.confirmado());String estado=logistica(p),bloqueo=bloqueoMovimiento(p,estado,in.accion(),true);if(bloqueo!=null)throw conflicto(bloqueo);
  String destino=switch(in.accion()){case PREPARAR_ENVIO->"PREPARADO";case SALIR_REPARTO->"EN_VIAJE";case LLEGAR_PUNTO->"DISPONIBLE_PUNTO";};String mensaje=switch(in.accion()){case PREPARAR_ENVIO->"El pedido está preparado para salir hacia el destino acordado.";case SALIR_REPARTO->"El pedido salió de la sucursal y está en viaje hacia el destino acordado.";case LLEGAR_PUNTO->"El pedido llegó al punto de entrega y está disponible para retirar. Todavía no fue entregado al cliente.";};Instant ahora=clock.instant();
  long event=evento(p,a,in.operacion(),hash,in.accion().name(),p.estado(),in.motivo(),mensaje,ahora);
  jdbc.update("INSERT INTO lamontana.movimiento_entrega(id_pedido,id_reserva_entrega,id_evento_pedido,estado_origen,estado_destino) VALUES (?,?,?,?,?)",p.numero(),reserva(p),event,estado,destino);
  return vista(pedidos.detalle(pedido,correo,true),correo,true);
 }
 @Transactional public Validacion validar(UUID pedido,Validar in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,true);organizacion.exigirPermiso(correo,"GESTIONAR_ENTREGAS");var a=actor(correo);String hash=huella(List.of(pedido,"VALIDAR",in));
  if(recuperar(in.operacion(),hash,p,a)){
   boolean valid=jdbc.queryForObject("SELECT resultado='VALIDO' FROM lamontana.validacion_codigo_entrega WHERE id_operacion=?",Boolean.class,in.operacion());var view=vista(p,correo,true);return new Validacion(view,valid&&view.validacion()!=null,valid?"La validación está registrada; consultá si continúa vigente para entregar.":"El intento anterior no validó el código. No se volvió a descontar un intento.");
  }
  version(p,in.version());if(!disponible(p))throw conflicto("El pedido todavía no está disponible para entrega presencial según su modalidad.");var c=ultimoCodigo(p.numero());if(c==null||c.revocado()!=null||c.utilizado()!=null||c.reserva()!=reserva(p))throw conflicto("El cliente debe generar un código vigente desde su pedido.");
  Instant now=clock.instant();String resultado=!now.isBefore(c.vence())?"VENCIDO":c.intentos()>=5?"BLOQUEADO":encoder.matches(in.codigo(),c.hash())?"VALIDO":"INVALIDO";
  if(resultado.equals("INVALIDO"))jdbc.update("UPDATE lamontana.codigo_entrega SET intentos=intentos+1 WHERE id_codigo_entrega=?",c.id());
  Instant hasta=now.plusSeconds(600).isBefore(c.vence())?now.plusSeconds(600):c.vence();
  jdbc.update("INSERT INTO lamontana.validacion_codigo_entrega(codigo_publico,id_codigo_entrega,id_actor,actor_nombre,id_operacion,huella,version_pedido,resultado,fecha,valido_hasta) VALUES (?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),c.id(),a.id(),a.nombre(),in.operacion(),hash,p.version(),resultado,Timestamp.from(now),resultado.equals("VALIDO")?Timestamp.from(hasta):null);
  return new Validacion(vista(p,correo,true),resultado.equals("VALIDO"),switch(resultado){case "VALIDO"->"Código validado. Comprobá el saldo y confirmá la entrega física.";case "INVALIDO"->"Código incorrecto. Verificá el que presenta el cliente.";case "BLOQUEADO"->"Se agotaron los cinco intentos. El cliente debe generar otro código.";default->"El código venció. El cliente debe generar otro.";});
 }
 @Transactional public Vista entregar(UUID pedido,Entregar in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,true);organizacion.exigirPermiso(correo,"GESTIONAR_ENTREGAS");var a=actor(correo);String hash=huella(List.of(pedido,"ENTREGAR",in));
  if(recuperar(in.operacion(),hash,p,a))return vista(p,correo,true);version(p,in.version());texto(in.receptor(),140);texto(in.motivo(),500);confirmar(in.confirmado());var b=bloqueosEntrega(p);if(!b.isEmpty())throw conflicto(String.join(" ",b));Instant now=clock.instant();var code=ultimoCodigo(p.numero());var validation=verificacion(p,code,a.id(),now);
  if(validation==null||!validation.codigoPublico().equals(in.validacion()))throw conflicto("Validá nuevamente el código presentado por el cliente con tu propia sesión.");
  long event=evento(p,a,in.operacion(),hash,"ENTREGAR","ENTREGADO",in.motivo(),"El pedido completo fue entregado a "+in.receptor().strip()+". El cierre administrativo se verifica por separado.",now);
  jdbc.update("UPDATE lamontana.reserva_entrega SET estado='CUMPLIDA' WHERE id_pedido=? AND estado='ACTIVA'",p.numero());
  jdbc.update("UPDATE lamontana.codigo_entrega SET utilizado_en=? WHERE id_codigo_entrega=?",Timestamp.from(now),code.id());
  jdbc.update("INSERT INTO lamontana.entrega(codigo_publico,id_pedido,id_reserva_entrega,id_evento_pedido,id_validacion_codigo,receptor_nombre,nota,fecha_entrega) SELECT ?,?,?,?,?,?,?,?",UUID.randomUUID(),p.numero(),code.reserva(),event,jdbc.queryForObject("SELECT id_validacion_codigo FROM lamontana.validacion_codigo_entrega WHERE codigo_publico=?",Long.class,validation.codigoPublico()),in.receptor().strip(),in.motivo().strip(),Timestamp.from(now));
  return vista(pedidos.detalle(pedido,correo,true),correo,true);
 }
 @Transactional public Vista cerrar(UUID pedido,Cerrar in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,true);organizacion.exigirPermiso(correo,"CERRAR_PEDIDOS");var a=actor(correo);String hash=huella(List.of(pedido,"CERRAR",in));
  if(recuperar(in.operacion(),hash,p,a))return vista(p,correo,true);version(p,in.version());texto(in.motivo(),500);confirmar(in.confirmado());var b=bloqueosCierre(p);if(!b.isEmpty())throw conflicto(String.join(" ",b));Instant now=clock.instant();
  long event=evento(p,a,in.operacion(),hash,"CERRAR","CERRADO",in.motivo(),"El pedido fue cerrado después de verificar su entrega y los movimientos financieros.",now);
  jdbc.update("INSERT INTO lamontana.cierre_pedido(id_pedido,id_evento_pedido,total,aplicado,fecha_cierre) VALUES (?,?,?,?,?)",p.numero(),event,new BigDecimal(p.oferta().total()),aplicado(p),Timestamp.from(now));
  return vista(pedidos.detalle(pedido,correo,true),correo,true);
 }
 private List<String> bloqueosEntrega(PedidoService.Detalle p){var b=new ArrayList<String>();if(!disponible(p))b.add("Falta completar calidad o el recorrido de entrega de esta modalidad.");if(!jdbc.queryForObject("SELECT lamontana.saldo_entrega_cubierto(?)",Boolean.class,p.numero()))b.add("Falta acreditar y aplicar el saldo y los anticipos por los medios aceptados antes de entregar.");return List.copyOf(b);}
 private List<String> bloqueosCierre(PedidoService.Detalle p){var b=new ArrayList<String>();if(!p.estado().equals("ENTREGADO"))b.add(p.estado().equals("CERRADO")?"El pedido ya está cerrado.":"Registrá primero la entrega física completa.");if(!jdbc.queryForObject("SELECT lamontana.saldo_entrega_cubierto(?)",Boolean.class,p.numero()))b.add("La situación financiera no cubre el total y las condiciones aceptadas.");if(jdbc.queryForObject("SELECT lamontana.dinero_pendiente_cierre(?)",BigDecimal.class,p.numero()).signum()>0)b.add("Hay dinero recibido sin aplicar. Registrá la devolución del excedente antes de cerrar.");if(jdbc.queryForObject("SELECT lamontana.informes_pendientes_cierre(?)",Long.class,p.numero())>0)b.add("Hay transferencias informadas pendientes de resolver. Verificá o descartá esos informes antes de cerrar.");return List.copyOf(b);}
 private boolean disponible(PedidoService.Detalle p){if(!p.estado().equals("LISTO_PARA_ENTREGA"))return false;String l=logistica(p);return switch(p.reserva().modalidad().name()){case "RETIRO_SUCURSAL"->l.equals("LISTO_RETIRO");case "RETIRO_PUNTO_ENTREGA"->l.equals("DISPONIBLE_PUNTO");default->l.equals("EN_VIAJE");};}
 private String bloqueoMovimiento(PedidoService.Detalle p,String estado,Accion op,boolean permiso){if(!permiso)return "Se requiere permiso de gestión de entregas.";if(!p.estado().equals("LISTO_PARA_ENTREGA"))return "La logística comienza después de superar calidad y termina al entregar.";if(p.reserva().modalidad().name().equals("RETIRO_SUCURSAL"))return "El retiro en sucursal no requiere despacho ni viaje.";return switch(op){case PREPARAR_ENVIO->estado.equals("PENDIENTE_PREPARACION")?null:"La preparación ya fue registrada.";case SALIR_REPARTO->estado.equals("PREPARADO")?null:"Registrá primero la preparación del envío.";case LLEGAR_PUNTO->!p.reserva().modalidad().name().equals("RETIRO_PUNTO_ENTREGA")?"La llegada al punto corresponde sólo al retiro en punto de entrega.":estado.equals("EN_VIAJE")?null:"Registrá la salida hacia el punto antes de su llegada.";};}
 private String logistica(PedidoService.Detalle p){return jdbc.queryForObject("SELECT lamontana.estado_logistico(?)",String.class,p.numero());}
 private long reserva(PedidoService.Detalle p){return jdbc.queryForObject("SELECT id_reserva_entrega FROM lamontana.reserva_entrega WHERE id_pedido=? AND estado='ACTIVA'",Long.class,p.numero());}
 private BigDecimal aplicado(PedidoService.Detalle p){return cobertura.acreditado(jdbc.queryForObject("SELECT id_cotizacion FROM lamontana.pedido_actual WHERE id_pedido=?",Long.class,p.numero()),List.of(MedioPago.values()));}
 private Secreto ultimoCodigo(long pedido){var list=jdbc.query("SELECT * FROM lamontana.codigo_entrega WHERE id_pedido=? ORDER BY id_codigo_entrega DESC LIMIT 1",(r,n)->new Secreto(r.getLong("id_codigo_entrega"),r.getObject("codigo_publico",UUID.class),r.getString("codigo_hash"),r.getLong("id_reserva_entrega"),r.getTimestamp("emitido_en").toInstant(),r.getTimestamp("vence_en").toInstant(),r.getTimestamp("revocado_en")==null?null:r.getTimestamp("revocado_en").toInstant(),r.getTimestamp("utilizado_en")==null?null:r.getTimestamp("utilizado_en").toInstant(),r.getInt("intentos")),pedido);return list.isEmpty()?null:list.get(0);}
 private String estadoCodigo(Secreto c,Instant now){return c.utilizado()!=null?"UTILIZADO":c.revocado()!=null?"REVOCADO":!now.isBefore(c.vence())?"VENCIDO":c.intentos()>=5?"BLOQUEADO":"ACTIVO";}
 private Verificacion verificacion(PedidoService.Detalle p,Secreto c,long actor,Instant now){if(c==null||!estadoCodigo(c,now).equals("ACTIVO")||!p.estado().equals("LISTO_PARA_ENTREGA")||c.reserva()!=reserva(p))return null;var list=jdbc.query("SELECT codigo_publico,fecha,valido_hasta FROM lamontana.validacion_codigo_entrega WHERE id_codigo_entrega=? AND id_actor=? AND version_pedido=? AND resultado='VALIDO' AND valido_hasta>? ORDER BY id_validacion_codigo DESC LIMIT 1",(r,n)->new Verificacion(r.getObject(1,UUID.class),r.getTimestamp(2).toInstant(),r.getTimestamp(3).toInstant()),c.id(),actor,p.version(),Timestamp.from(now));return list.isEmpty()?null:list.get(0);}
 private long evento(PedidoService.Detalle p,Actor a,UUID op,String hash,String accion,String destino,String motivo,String mensaje,Instant ahora){jdbc.update("UPDATE lamontana.pedido SET estado=?,version=version+1,finalizada_en=CASE WHEN ?='CERRADO' THEN ? ELSE finalizada_en END WHERE id_pedido=?",destino,destino,Timestamp.from(ahora),p.numero());return jdbc.queryForObject("INSERT INTO lamontana.historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha,estado_origen,version_pedido,accion,mensaje_cliente,actor_nombre,actor_tipo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id_evento",Long.class,p.numero(),a.id(),op,hash,destino,motivo.strip(),Timestamp.from(ahora),p.estado(),p.version()+1,accion,mensaje,a.nombre(),a.rol());}
 private boolean recuperar(UUID op,String hash,PedidoService.Detalle p,Actor a){var rows=jdbc.query("SELECT id_pedido,id_actor,huella FROM lamontana.historial_estado_pedido WHERE id_operacion=? UNION ALL SELECT id_pedido,id_autor,huella FROM lamontana.observacion_interna WHERE id_operacion=? UNION ALL SELECT id_pedido,id_emisor,huella FROM lamontana.codigo_entrega WHERE id_operacion=? UNION ALL SELECT c.id_pedido,v.id_actor,v.huella FROM lamontana.validacion_codigo_entrega v JOIN lamontana.codigo_entrega c USING(id_codigo_entrega) WHERE v.id_operacion=?",(r,n)->new Object[]{r.getLong(1),r.getLong(2),r.getString(3)},op,op,op,op);if(rows.isEmpty())return false;var r=rows.get(0);if(rows.size()!=1||(long)r[0]!=p.numero()||(long)r[1]!=a.id()||!hash.equals(r[2]))throw conflicto("La operación ya fue utilizada con otros datos.");return true;}
 private Actor actor(String correo){return jdbc.queryForObject("SELECT u.id_usuario,u.nombre||' '||u.apellido,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.correo=? AND u.estado='ACTIVO' AND r.activo",(r,n)->new Actor(r.getLong(1),r.getString(2),r.getString(3)),correo);}
 private void bloquear(){for(long id:new long[]{764003,764001,764002,764004})jdbc.execute("SELECT pg_advisory_xact_lock("+id+")");}
 private void version(PedidoService.Detalle p,long v){if(p.version()!=v)throw conflicto("El pedido cambió. Actualizá su estado antes de continuar.");}
 private void texto(String v,int max){if(v==null||v.isBlank()||v.length()>max||v.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw error(HttpStatus.BAD_REQUEST,"Completá los datos sin caracteres de control.");}
 private void confirmar(boolean v){if(!v)throw error(HttpStatus.BAD_REQUEST,"Confirmá el hecho que vas a registrar.");}
 private String dinero(BigDecimal v){return v.setScale(2).toPlainString();}
 private String huella(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private ResponseStatusException conflicto(String m){return error(HttpStatus.CONFLICT,m);}
 private ResponseStatusException error(HttpStatus s,String m){return new ResponseStatusException(s,m);}
}
