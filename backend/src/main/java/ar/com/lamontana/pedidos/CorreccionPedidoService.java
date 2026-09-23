package ar.com.lamontana.pedidos;

import static ar.com.lamontana.pedidos.CorreccionPedidoController.*;
import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.cotizaciones.*;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.CoberturaPagos;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CorreccionPedidoService {
 private final JdbcTemplate jdbc;private final PedidoService pedidos;private final CotizacionService cotizaciones;
 private final OrganizacionService organizacion;private final ArchivosPrivados privados;private final CoberturaPagos cobertura;
 private final JsonMapper json=JsonMapper.builder().build();private final Clock clock=Clock.systemUTC();
 public CorreccionPedidoService(JdbcTemplate jdbc,PedidoService pedidos,CotizacionService cotizaciones,OrganizacionService organizacion,ArchivosPrivados privados,CoberturaPagos cobertura){this.jdbc=jdbc;this.pedidos=pedidos;this.cotizaciones=cotizaciones;this.organizacion=organizacion;this.privados=privados;this.cobertura=cobertura;}
 public record Oferta(UUID codigoPublico,String total,String estado,Instant aceptadaEn,Instant vigenteHasta){}
 public record Respuesta(String mensaje,Instant fecha,UUID cotizacion,String total,List<PedidoService.Pdf> archivos){}
 public record Solicitud(UUID codigoPublico,Tipo tipo,String estado,String mensajeCliente,String motivoInterno,String solicitante,
  Instant fecha,UUID cotizacionBase,List<UUID> items,List<Oferta> cotizaciones,Respuesta respuesta){}
 public record Vista(long version,boolean puedeSolicitar,List<Solicitud> solicitudes){}
 public record Preparacion(UUID solicitud,long version,UUID cotizacion,boolean nuevaCotizacion,String totalAnterior,String totalNuevo,String diferencia,
  String huella,boolean habilitada,List<String> bloqueos,PedidoService.Revision finales,List<PedidoService.Pdf> archivos){}
 private record Actor(long id,String nombre,String rol){}
 private record Base(long id,UUID codigo,Tipo tipo,String estado,long cotizacion,UUID oferta,List<UUID> items){}

 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Vista consultar(UUID id,String correo,boolean interno){
  var p=pedidos.detalle(id,correo,interno);
  var solicitudes=jdbc.query("SELECT s.*,c.codigo_publico AS cotizacion FROM lamontana.solicitud_correccion s JOIN lamontana.cotizacion c ON c.id_cotizacion=s.id_cotizacion_base WHERE s.id_pedido=? ORDER BY s.id_solicitud_correccion DESC",(r,n)->{
   long request=r.getLong("id_solicitud_correccion");
   var ofertas=jdbc.query("SELECT c.codigo_publico,c.total,c.estado,c.aceptada_en,c.vigente_hasta FROM lamontana.cotizacion_correccion cc JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE cc.id_solicitud_correccion=? ORDER BY c.id_cotizacion DESC",(rs,k)->new Oferta(rs.getObject(1,UUID.class),rs.getBigDecimal(2).toPlainString(),rs.getString(3).equals("VIGENTE")&&!clock.instant().isBefore(rs.getTimestamp(5).toInstant())?"EXPIRADA":rs.getString(3),instant(rs.getTimestamp(4)),rs.getTimestamp(5).toInstant()),request);
   var respuestas=jdbc.query("SELECT r.*,c.codigo_publico AS cotizacion FROM lamontana.respuesta_correccion r JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE id_solicitud_correccion=?",(rs,k)->new Respuesta(rs.getString("mensaje"),rs.getTimestamp("fecha").toInstant(),rs.getObject("cotizacion",UUID.class),rs.getBigDecimal("total").toPlainString(),archivosRespuesta(rs.getLong("id_respuesta_correccion"))),request);
   return new Solicitud(r.getObject("codigo_publico",UUID.class),Tipo.valueOf(r.getString("tipo")),r.getString("estado"),r.getString("mensaje_cliente"),interno?r.getString("motivo"):null,interno?r.getString("solicitante_nombre"):"La imprenta",r.getTimestamp("fecha_solicitud").toInstant(),r.getObject("cotizacion",UUID.class),items(request),ofertas,respuestas.isEmpty()?null:respuestas.get(0));
  },p.numero());
  return new Vista(p.version(),interno&&p.estado().equals("PENDIENTE_REVISION")&&organizacion.contexto(correo).permisos().contains("GESTIONAR_PEDIDOS"),solicitudes);
 }
 @Transactional public PedidoService.Detalle solicitar(UUID id,Solicitar in,String correo){
  bloquear();var p=pedidos.detalle(id,correo,true);organizacion.exigirPermiso(correo,"GESTIONAR_PEDIDOS");organizacion.sucursalAutorizada(correo,p.oferta().sucursal().codigoPublico());var actor=actor(correo);String hash=huella(List.of(id,in));
  if(replay(in.operacion(),hash,p.numero(),actor.id()))return p;
  version(p,in.version());if(!p.estado().equals("PENDIENTE_REVISION"))throw conflicto("Sólo se solicita corrección durante la revisión, antes de aprobar.");
  texto(in.motivo(),500);texto(in.mensajeCliente(),2000);
  if(new HashSet<>(in.items()).size()!=in.items().size()||in.items().stream().anyMatch(i->p.archivos().stream().noneMatch(f->f.item().equals(i))))throw error(HttpStatus.BAD_REQUEST,"Seleccioná ítems actuales de este pedido, sin repetirlos.");
  if(in.tipo()==Tipo.ARCHIVO&&in.items().isEmpty())throw error(HttpStatus.BAD_REQUEST,"Seleccioná al menos un PDF que deba corregirse.");
  Instant now=clock.instant();jdbc.update("UPDATE lamontana.solicitud_correccion SET estado='CERRADA',fecha_cierre=? WHERE id_pedido=? AND estado='RESPONDIDA'",Timestamp.from(now),p.numero());
  long request=jdbc.queryForObject("INSERT INTO lamontana.solicitud_correccion(codigo_publico,id_pedido,id_solicitante,solicitante_nombre,tipo,motivo,mensaje_cliente,fecha_solicitud,version_pedido_inicio,id_cotizacion_base,id_configuracion_base,condiciones_base,total_base,receptor_base,telefono_base) SELECT ?,?,?,?,?,?,?,?,?,c.id_cotizacion,v.id_configuracion_version,?::jsonb,?,?,? FROM lamontana.cotizacion c,lamontana.configuracion_version v WHERE c.codigo_publico=? AND v.codigo_publico=? RETURNING id_solicitud_correccion",Long.class,UUID.randomUUID(),p.numero(),actor.id(),actor.nombre(),in.tipo().name(),in.motivo().strip(),in.mensajeCliente().strip(),Timestamp.from(now),p.version()+1,json.writeValueAsString(p.condiciones()),new BigDecimal(p.oferta().total()),p.contacto()==null?null:p.contacto().receptor(),p.contacto()==null?null:p.contacto().telefono(),p.cotizacion(),p.configuracion());
  for(var f:p.archivos())jdbc.update("INSERT INTO lamontana.solicitud_correccion_item(id_solicitud_correccion,id_cotizacion_item,id_archivo_observado,requiere_reemplazo) SELECT ?,ci.id_cotizacion_item,a.id_archivo_almacenado,? FROM lamontana.cotizacion_item ci,lamontana.archivo_almacenado a WHERE ci.codigo_publico=? AND a.codigo_publico=?",request,in.items().contains(f.item()),f.item(),f.archivo());
  jdbc.update("UPDATE lamontana.pedido SET estado='CORRECCION_SOLICITADA',revisada_en=NULL,id_revisor=NULL,version=version+1 WHERE id_pedido=?",p.numero());
  evento(p,actor,in.operacion(),hash,"PEDIR_CORRECCION","CORRECCION_SOLICITADA",in.motivo().strip(),in.mensajeCliente().strip(),now);
  return pedidos.detalle(id,correo,true);
 }
 @Transactional public CotizacionService.Detalle cotizar(UUID id,UUID solicitud,CotizacionController.Crear in,String correo){
  bloquear();var p=pedidos.detalle(id,correo,false);var req=base(p.numero(),solicitud);
  // CotizacionService recupera el mismo comando incluso cuando la solicitud ya terminó.
  return cotizaciones.crearCorreccion(in,correo,req.codigo());
 }
 @Transactional public Preparacion preparar(UUID id,String correo){bloquear();var p=pedidos.detalle(id,correo,false);return preparar(p,pendiente(p),correo);}
 private Preparacion preparar(PedidoService.Detalle p,Base req,String correo){
  if(!p.estado().equals("CORRECCION_SOLICITADA")||!req.estado().equals("PENDIENTE"))throw conflicto("La solicitud ya no admite una respuesta.");
  if(!jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.sucursal WHERE codigo_publico=? AND estado='ACTIVA')",Boolean.class,p.oferta().sucursal().codigoPublico()))throw conflicto("La sucursal está desactivada. Consultá con la imprenta o cancelá el pedido.");
  var nuevas=jdbc.query("SELECT c.codigo_publico FROM lamontana.cotizacion_correccion cc JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE cc.id_solicitud_correccion=? ORDER BY c.id_cotizacion DESC LIMIT 1",(r,n)->r.getObject(1,UUID.class),req.id());
  boolean cambio=!nuevas.isEmpty();UUID quote=cambio?nuevas.get(0):p.cotizacion();PedidoService.Revision finales=null;List<PedidoService.Pdf> files;var bloqueos=new ArrayList<String>();String total=p.oferta().total();
  if(cambio){
   var q=cotizaciones.detalle(quote,correo);total=q.oferta().total();finales=pedidos.revisarCorreccion(quote,p.numero(),correo);bloqueos.addAll(finales.bloqueos());files=finales.archivos();
  }else{
   files=archivosPropuestos(req);for(var f:p.archivos())if(files.stream().noneMatch(x->x.item().equals(f.item())))bloqueos.add("Falta un PDF corregido y su vista previa aceptada: "+f.nombre()+".");
   if(req.tipo()==Tipo.ENTREGA||req.tipo()==Tipo.CANTIDAD)bloqueos.add("Esta corrección requiere una nueva cotización aceptada, con entrega y cantidades actualizadas.");
   var a=p.condiciones().cotizadas();var b=p.condiciones().actuales();
   if(a.cargaRequiereAcreditacion()&&!cobertura.cubre(req.cotizacion(),a.pagoPrevioRequerido(),a.senaRequerida(),a.mediosAcreditacion())||b.cargaRequiereAcreditacion()&&!cobertura.cubre(req.cotizacion(),b.pagoPrevioRequerido(),b.senaRequerida(),b.mediosAcreditacion()))bloqueos.add("Falta cobertura acreditada de la variante de pago previo o seña. El dinero debe aplicarse por los medios aceptados.");
  }
  String hash=huella(Arrays.asList(p.codigoPublico(),p.version(),req.codigo(),quote,finales==null?null:finales.huella(),files));
  return new Preparacion(req.codigo(),p.version(),quote,cambio,p.oferta().total(),total,new BigDecimal(total).subtract(new BigDecimal(p.oferta().total())).toPlainString(),hash,bloqueos.isEmpty(),List.copyOf(bloqueos),finales,files);
 }
 @Transactional public PedidoService.Detalle responder(UUID id,Responder in,String correo){
  bloquear();var p=pedidos.detalle(id,correo,false);var actor=actor(correo);String hash=huella(List.of(id,in));if(replay(in.operacion(),hash,p.numero(),actor.id()))return p;
  version(p,in.version());var req=base(p.numero(),in.solicitud());var pre=preparar(p,req,correo);texto(in.mensaje(),2000);
  if(!in.condicionesAceptadas()||!pre.habilitada()||!pre.huella().equals(in.huella()))throw conflicto("Revisá los PDF y las condiciones actuales, y aceptalos antes de responder. "+String.join(" ",pre.bloqueos()));
  var q=cotizaciones.detalle(pre.cotizacion(),correo);contacto(q.oferta(),in.contacto());
  if(!pre.nuevaCotizacion()&&req.tipo()!=Tipo.DATOS&&req.tipo()!=Tipo.OTRO&&!Objects.equals(p.contacto(),in.contacto()))throw conflicto("Esta solicitud no permite cambiar los datos de contacto.");
  for(var f:pre.archivos())try{privados.original(f.archivo(),f.sha256(),f.bytes());}catch(java.io.IOException ex){throw conflicto("No se pudo comprobar la integridad de un PDF corregido. Conservá el pedido y recuperá su archivo.");}
  Instant now=clock.instant();if(pre.nuevaCotizacion()&&!now.isBefore(q.vigenteHasta()))throw conflicto("La nueva cotización venció durante la revisión. Solicitá su sucesora; el dinero se conserva.");
  var condiciones=pre.nuevaCotizacion()?new PedidoService.Condiciones(pre.finales().condiciones().cotizadas(),pre.finales().condiciones().actuales(),true):p.condiciones();UUID config=pre.nuevaCotizacion()?pre.finales().configuracion():p.configuracion();
  long response=jdbc.queryForObject("INSERT INTO lamontana.respuesta_correccion(id_solicitud_correccion,id_autor,mensaje,fecha,version_pedido,id_cotizacion,id_configuracion_version,condiciones_finales,total,receptor,telefono) SELECT ?,?,?,?,?,c.id_cotizacion,v.id_configuracion_version,?::jsonb,?,?,? FROM lamontana.cotizacion c,lamontana.configuracion_version v WHERE c.codigo_publico=? AND v.codigo_publico=? RETURNING id_respuesta_correccion",Long.class,req.id(),actor.id(),in.mensaje().strip(),Timestamp.from(now),p.version()+1,json.writeValueAsString(condiciones),new BigDecimal(pre.totalNuevo()),in.contacto()==null?null:in.contacto().receptor().strip(),in.contacto()==null?null:in.contacto().telefono().strip(),pre.cotizacion(),config);
  for(var f:pre.archivos())jdbc.update("INSERT INTO lamontana.respuesta_correccion_archivo(id_respuesta_correccion,id_cotizacion_item,id_archivo_almacenado,id_aceptacion_vista_previa) SELECT ?,i.id_cotizacion_item,a.id_archivo_almacenado,v.id_aceptacion_vista_previa FROM lamontana.cotizacion_item i,lamontana.archivo_almacenado a JOIN lamontana.aceptacion_vista_previa v USING(id_archivo_almacenado) WHERE i.codigo_publico=? AND a.codigo_publico=?",response,f.item(),f.archivo());
  if(pre.nuevaCotizacion()){
   jdbc.update("UPDATE lamontana.reserva_entrega SET estado='REEMPLAZADA' WHERE id_pedido=? AND estado='ACTIVA'",p.numero());var f=pre.finales().franja();
   jdbc.update("INSERT INTO lamontana.reserva_entrega(id_pedido,modalidad,destino_publico,origen_publico,nombre_destino,zona_horaria,fecha_local,apertura,cierre,franja_desde,franja_hasta,cupo_confirmado,costo) VALUES (?,?,?,?,?,?,?::date,?::time,?::time,?,?,?,?)",p.numero(),f.modalidad().name(),f.destino(),f.origen(),f.nombre(),f.zonaHoraria(),f.fecha().toString(),f.apertura(),f.cierre(),Timestamp.from(f.desde()),Timestamp.from(f.hasta()),f.cupoConfigurado(),new BigDecimal(f.costo()));
   jdbc.update("UPDATE lamontana.cotizacion SET estado='CONFIRMADA',confirmada_en=?,id_configuracion_confirmacion=(SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?),version=version+1 WHERE codigo_publico=?",Timestamp.from(now),config,pre.cotizacion());
  }
  // Los intentos no elegidos dejan de admitir contenido; los originales aceptados
  // y la respuesta recién capturada permanecen inmutables.
  jdbc.update("UPDATE lamontana.archivo_almacenado a SET estado='FALLIDO',fecha_fin=now(),codigo_resultado='CORRECCION_RESPONDIDA',mensaje='La corrección ya fue respondida con otra versión aceptada.' FROM lamontana.archivo_trabajo t JOIN lamontana.cotizacion_item i USING(id_cotizacion_item) WHERE t.id_archivo_almacenado=a.id_archivo_almacenado AND a.estado IN ('PENDIENTE','VALIDANDO') AND (t.id_solicitud_correccion=? OR EXISTS(SELECT 1 FROM lamontana.cotizacion_correccion cc WHERE cc.id_cotizacion=i.id_cotizacion AND cc.id_solicitud_correccion=?))",req.id(),req.id());
  jdbc.update("UPDATE lamontana.solicitud_correccion SET estado='RESPONDIDA' WHERE id_solicitud_correccion=?",req.id());
  jdbc.update("UPDATE lamontana.pedido SET estado='PENDIENTE_REVISION',revisada_en=NULL,id_revisor=NULL,version=version+1 WHERE id_pedido=?",p.numero());
  evento(p,actor,in.operacion(),hash,"RESPONDER_CORRECCION","PENDIENTE_REVISION","Respuesta del cliente incorporada como nueva versión del trabajo.","El cliente respondió la corrección. La nueva versión espera revisión de la imprenta.",now);
  return pedidos.detalle(id,correo,false);
 }
 private List<PedidoService.Pdf> archivosPropuestos(Base req){return jdbc.query("SELECT ci.codigo_publico,a.codigo_publico,a.nombre_original,a.sha256,a.cantidad_bytes,a.cantidad_paginas,v.fecha FROM lamontana.solicitud_correccion_item i JOIN lamontana.cotizacion_item ci USING(id_cotizacion_item) JOIN lamontana.archivo_trabajo t ON t.id_cotizacion_item=i.id_cotizacion_item AND ((i.requiere_reemplazo AND t.id_solicitud_correccion=? AND t.activo AND t.id_archivo_almacenado<>i.id_archivo_observado) OR (NOT i.requiere_reemplazo AND t.id_archivo_almacenado=i.id_archivo_observado)) JOIN lamontana.archivo_almacenado a ON a.id_archivo_almacenado=t.id_archivo_almacenado JOIN lamontana.aceptacion_vista_previa v ON v.id_archivo_almacenado=a.id_archivo_almacenado WHERE i.id_solicitud_correccion=? AND a.estado='VALIDO' ORDER BY ci.orden",(r,n)->pdf(r),req.id(),req.id());}
 private List<PedidoService.Pdf> archivosRespuesta(long id){return jdbc.query("SELECT i.codigo_publico,a.codigo_publico,a.nombre_original,a.sha256,a.cantidad_bytes,a.cantidad_paginas,v.fecha FROM lamontana.respuesta_correccion_archivo f JOIN lamontana.cotizacion_item i USING(id_cotizacion_item) JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) JOIN lamontana.aceptacion_vista_previa v USING(id_aceptacion_vista_previa) WHERE f.id_respuesta_correccion=? ORDER BY i.orden",(r,n)->pdf(r),id);}
 private PedidoService.Pdf pdf(java.sql.ResultSet r)throws java.sql.SQLException{return new PedidoService.Pdf(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getString(4),r.getLong(5),r.getInt(6),r.getTimestamp(7).toInstant());}
 private Base pendiente(PedidoService.Detalle p){var ids=jdbc.query("SELECT codigo_publico FROM lamontana.solicitud_correccion WHERE id_pedido=? AND estado='PENDIENTE'",(r,n)->r.getObject(1,UUID.class),p.numero());if(ids.isEmpty())throw conflicto("No hay una corrección pendiente de respuesta.");return base(p.numero(),ids.get(0));}
 private Base base(long pedido,UUID code){var rows=jdbc.query("SELECT s.*,c.codigo_publico AS cotizacion FROM lamontana.solicitud_correccion s JOIN lamontana.cotizacion c ON c.id_cotizacion=s.id_cotizacion_base WHERE s.id_pedido=? AND s.codigo_publico=?",(r,n)->new Base(r.getLong("id_solicitud_correccion"),code,Tipo.valueOf(r.getString("tipo")),r.getString("estado"),r.getLong("id_cotizacion_base"),r.getObject("cotizacion",UUID.class),items(r.getLong("id_solicitud_correccion"))),pedido,code);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"La solicitud no pertenece a este pedido.");return rows.get(0);}
 private List<UUID> items(long request){return jdbc.query("SELECT i.codigo_publico FROM lamontana.solicitud_correccion_item s JOIN lamontana.cotizacion_item i USING(id_cotizacion_item) WHERE id_solicitud_correccion=? AND requiere_reemplazo ORDER BY i.orden",(r,n)->r.getObject(1,UUID.class),request);}
 private void contacto(CotizacionService.Oferta oferta,PedidoController.Contacto contacto){boolean home=oferta.modalidad()==ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad.ENVIO_DOMICILIO;if(home!=(contacto!=null))throw error(HttpStatus.BAD_REQUEST,"Indicá receptor y teléfono sólo para domicilio.");if(contacto!=null){texto(contacto.receptor(),140);texto(contacto.telefono(),40);if(!contacto.telefono().matches("[+0-9 ()-]{6,40}")||contacto.telefono().chars().filter(Character::isDigit).count()<6)throw error(HttpStatus.BAD_REQUEST,"Ingresá un teléfono válido.");}}
 private void evento(PedidoService.Detalle p,Actor a,UUID op,String hash,String accion,String estado,String motivo,String publico,Instant fecha){jdbc.update("INSERT INTO lamontana.historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha,estado_origen,version_pedido,accion,mensaje_cliente,actor_nombre,actor_tipo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",p.numero(),a.id(),op,hash,estado,motivo,Timestamp.from(fecha),p.estado(),p.version()+1,accion,publico,a.nombre(),a.rol());}
 private boolean replay(UUID op,String hash,long pedido,long actor){var rows=jdbc.query("SELECT id_pedido,id_actor,huella FROM lamontana.historial_estado_pedido WHERE id_operacion=? UNION ALL SELECT id_pedido,id_autor,huella FROM lamontana.observacion_interna WHERE id_operacion=?",(r,n)->new Object[]{r.getLong(1),r.getLong(2),r.getString(3)},op,op);if(rows.isEmpty())return false;var r=rows.get(0);if(rows.size()!=1||(long)r[0]!=pedido||(long)r[1]!=actor||!hash.equals(r[2]))throw conflicto("La operación ya se utilizó con otros datos.");return true;}
 private Actor actor(String correo){return jdbc.queryForObject("SELECT u.id_usuario,u.nombre||' '||u.apellido,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.activo",(r,n)->new Actor(r.getLong(1),r.getString(2),r.getString(3)),correo);}
 private void version(PedidoService.Detalle p,long version){if(p.version()!=version)throw conflicto("El pedido cambió. Actualizá su estado antes de continuar.");}
 private void texto(String v,int max){if(v==null||v.isBlank()||v.length()>max||v.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw error(HttpStatus.BAD_REQUEST,"Completá los textos requeridos sin caracteres de control.");}
 private void bloquear(){for(long id:new long[]{764003,764001,764002,764004})jdbc.execute("SELECT pg_advisory_xact_lock("+id+")");}
 private Instant instant(Timestamp t){return t==null?null:t.toInstant();}
 private String huella(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private ResponseStatusException conflicto(String msg){return error(HttpStatus.CONFLICT,msg);}
 private ResponseStatusException error(HttpStatus code,String msg){return new ResponseStatusException(code,msg);}
}
