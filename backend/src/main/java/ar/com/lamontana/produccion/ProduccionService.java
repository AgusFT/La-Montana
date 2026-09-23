//#region ENCABEZADO · ProduccionService.java
/*
 * ========================================================================
 * ARCHIVO: ProduccionService.java
 * ========================================================================
 * FUNCIÓN
 * Gestiona producción manual por ítem, selección de impresoras, cobertura de pagos, estados de
 * trabajo, reimpresiones y calidad. Revalida condiciones y registra cada transición del pedido.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] ProduccionService(JdbcTemplate jdbc, PedidoService pedidos, OrganizacionService
 *   organizacion, ArchivosPrivados archivos, CoberturaPagos cobertura, RecursosRepositorio
 *   recursos)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] Vista consultar(UUID pedido, String correo)
 * - [private] Vista vista(PedidoService.Detalle p, String correo)
 * - [public] Vista iniciar(UUID pedido, Inicio in, String correo)
 * - [public] Vista cambiar(UUID pedido, UUID trabajo, Cambio in, String correo)
 * - [public] Vista calidad(UUID pedido, UUID trabajo, Inspeccion in, String correo)
 * - [private] List<Cobertura> cobertura(PedidoService.Detalle p)
 * - [private] Cobertura cobertura(String nombre, String previo, String sena, BigDecimal aplicado)
 * - [private] List<String> bloqueos(PedidoService.Detalle p, boolean permiso)
 * - [private] String bloqueoItem(Trabajo t)
 * - [private] List<RecursosRepositorio.Impresora> impresoras(PedidoService.Detalle p)
 * - [private] boolean compatible(RecursosRepositorio.Impresora i, CotizacionService.ItemCotizado
 *   item)
 * - [private] List<Registro> trabajos(long pedido, UUID item)
 * - [private] Registro trabajo(PedidoService.Detalle p, UUID id)
 * - [private] void evento(PedidoService.Detalle p, Actor a, UUID op, String hash, long trabajo,
 *   String accion, String origenTrabajo, String destinoTrabajo, String estado, String motivo,
 *   String publico, Instant fecha)
 * - [private] boolean recuperar(UUID op, String hash, PedidoService.Detalle p, Actor a)
 * - [private] Actor actor(String correo)
 * - [private] void bloquear()
 *   Toma el bloqueo transaccional de PostgreSQL.
 * - [private] void version(PedidoService.Detalle p, long v)
 * - [private] void texto(String v)
 * - [private] void confirmar(boolean v)
 * - [private] String huella(Object v)
 *   Calcula o prepara la huella SHA-256 del contenido.
 * - [private] ResponseStatusException conflicto(String m)
 * - [private] ResponseStatusException error(HttpStatus status, String m)
 *   Construye un error HTTP controlado.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ProduccionService (clase).
 * - ProduccionService.Cobertura (record).
 * - ProduccionService.Impresora (record).
 * - ProduccionService.Calidad (record).
 * - ProduccionService.Evento (record).
 * - ProduccionService.Trabajo (record).
 * - ProduccionService.Item (record).
 * - ProduccionService.Vista (record).
 * - ProduccionService.Actor (record).
 * - ProduccionService.Registro (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.produccion;

import static ar.com.lamontana.produccion.ProduccionController.*;
import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.configuracion.RecursosRepositorio;
import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.CoberturaPagos;
import ar.com.lamontana.pedidos.PedidoService;
import java.math.BigDecimal;
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
public class ProduccionService {
 private final JdbcTemplate jdbc;private final PedidoService pedidos;private final OrganizacionService organizacion;
 private final ArchivosPrivados archivos;private final CoberturaPagos cobertura;private final RecursosRepositorio recursos;
 private final JsonMapper json=JsonMapper.builder().build();
 public ProduccionService(JdbcTemplate jdbc,PedidoService pedidos,OrganizacionService organizacion,ArchivosPrivados archivos,CoberturaPagos cobertura,RecursosRepositorio recursos){this.jdbc=jdbc;this.pedidos=pedidos;this.organizacion=organizacion;this.archivos=archivos;this.cobertura=cobertura;this.recursos=recursos;}
 public record Cobertura(String condiciones,String requerido,String aplicado,String pendiente){}
 public record Impresora(UUID codigoPublico,String nombre){}
 public record Calidad(Resultado resultado,Checklist checklist,String observaciones,String inspector,Instant fecha){}
 public record Evento(String accion,String estadoOrigen,String estadoDestino,String actor,String motivo,Instant fecha){}
 public record Trabajo(UUID codigoPublico,long numero,UUID origen,String tipo,String estado,UUID archivo,String impresora,String motivoExcepcion,String operador,Instant inicio,Instant fin,Calidad calidad,List<Evento> historial){}
 public record Item(UUID codigoPublico,UUID archivo,String nombre,List<Impresora> impresoras,boolean puedeIniciar,String bloqueo,List<Trabajo> trabajos){}
 public record Vista(long version,String estado,boolean puedeProducir,boolean puedeControlarCalidad,List<Cobertura> cobertura,List<String> bloqueos,List<Item> items){}
 private record Actor(long id,String nombre,String rol){}
 private record Registro(long id,Trabajo vista){}

 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Vista consultar(UUID pedido,String correo){return vista(pedidos.detalle(pedido,correo,true),correo);}
 private Vista vista(PedidoService.Detalle p,String correo){
  var permisos=organizacion.contexto(correo).permisos();boolean producir=permisos.contains("GESTIONAR_PRODUCCION"),calidad=permisos.contains("CONTROLAR_CALIDAD");
  var bloqueos=bloqueos(p,producir);var declaradas=impresoras(p);var items=new ArrayList<Item>();
  for(var item:p.oferta().items()){
   var trabajos=trabajos(p.numero(),item.codigoPublico()).stream().map(Registro::vista).toList();var ultimo=trabajos.isEmpty()?null:trabajos.get(trabajos.size()-1);
   String bloqueo=!bloqueos.isEmpty()?String.join(" ",bloqueos):bloqueoItem(ultimo);
   var file=p.archivos().stream().filter(f->f.item().equals(item.codigoPublico())).findFirst().orElseThrow();
   items.add(new Item(item.codigoPublico(),file.archivo(),file.nombre(),declaradas.stream().filter(i->compatible(i,item)).map(i->new Impresora(i.codigoPublico(),i.nombre())).toList(),bloqueo==null,bloqueo,trabajos));
  }
  return new Vista(p.version(),p.estado(),producir,calidad,cobertura(p),bloqueos,List.copyOf(items));
 }
 @Transactional public Vista iniciar(UUID pedido,Inicio in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,true);organizacion.exigirPermiso(correo,"GESTIONAR_PRODUCCION");var actor=actor(correo);var hash=huella(List.of(pedido,"INICIAR",in));
  if(recuperar(in.operacion(),hash,p,actor))return vista(p,correo);
  version(p,in.version());texto(in.motivo());confirmar(in.confirmacionManual());var bloqueos=bloqueos(p,true);if(!bloqueos.isEmpty())throw conflicto(String.join(" ",bloqueos));
  var item=p.oferta().items().stream().filter(i->i.codigoPublico().equals(in.item())).findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"El ítem no pertenece al trabajo efectivo de este pedido."));
  var anteriores=trabajos(p.numero(),in.item());var anterior=anteriores.isEmpty()?null:anteriores.get(anteriores.size()-1);String bloqueo=bloqueoItem(anterior==null?null:anterior.vista());if(bloqueo!=null)throw conflicto(bloqueo);
  var pdf=p.archivos().stream().filter(f->f.item().equals(in.item())).findFirst().orElseThrow();
  try{archivos.original(pdf.archivo(),pdf.sha256(),pdf.bytes());}catch(java.io.IOException ex){throw conflicto("No se pudo verificar el PDF confirmado. Recuperá su original privado antes de producir.");}
  RecursosRepositorio.Impresora impresora=null;
  if(in.impresora()!=null)impresora=impresoras(p).stream().filter(i->i.codigoPublico().equals(in.impresora())&&compatible(i,item)).findFirst().orElseThrow(()->conflicto("La impresora declarada no admite este trabajo en la versión y sucursal del pedido."));
  Instant ahora=Instant.now();UUID codigo=UUID.randomUUID();String tipo=anterior!=null&&anterior.vista().calidad()!=null?"REIMPRESION_CALIDAD":"NORMAL";
  long id=jdbc.queryForObject("""
   INSERT INTO lamontana.trabajo_impresion(codigo_publico,id_pedido,id_cotizacion_item,id_archivo_almacenado,id_aceptacion_vista_previa,id_trabajo_origen,id_impresora,id_configuracion_version,impresora_nombre,id_operador,operador_nombre,tipo,estado,motivo_excepcion,fecha_inicio)
   SELECT ?,i.id_pedido,i.id_cotizacion_item,i.id_archivo_almacenado,i.id_aceptacion_vista_previa,?,(SELECT id_impresora FROM lamontana.impresora WHERE codigo_publico=?),p.id_configuracion_version,?,?,?,?,'IMPRIMIENDO',?,?
   FROM lamontana.pedido_archivo_actual i JOIN lamontana.pedido_actual p USING(id_pedido) JOIN lamontana.cotizacion_item ci USING(id_cotizacion_item)
   WHERE i.id_pedido=? AND ci.codigo_publico=? RETURNING id_trabajo_impresion
   """,Long.class,codigo,anterior==null?null:anterior.id(),in.impresora(),impresora==null?null:impresora.nombre(),actor.id(),actor.nombre(),tipo,impresora==null?in.motivo().strip():null,Timestamp.from(ahora),p.numero(),in.item());
  evento(p,actor,in.operacion(),hash,id,"INICIAR_TRABAJO",null,"IMPRIMIENDO","EN_PRODUCCION",in.motivo(),"La imprenta inició manualmente "+(tipo.equals("NORMAL")?"un trabajo de impresión.":"una reimpresión para completar el control de calidad."),ahora);
  return vista(pedidos.detalle(pedido,correo,true),correo);
 }
 @Transactional public Vista cambiar(UUID pedido,UUID trabajo,Cambio in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,true);organizacion.exigirPermiso(correo,"GESTIONAR_PRODUCCION");var actor=actor(correo);var hash=huella(List.of(pedido,trabajo,"ESTADO",in));
  if(recuperar(in.operacion(),hash,p,actor))return vista(p,correo);
  version(p,in.version());texto(in.motivo());confirmar(in.confirmacionManual());var t=trabajo(p,trabajo);
  if(!p.estado().equals("EN_PRODUCCION")||!t.vista().estado().equals("IMPRIMIENDO"))throw conflicto("Sólo un trabajo manual en curso admite completar, registrar un error o cancelar.");
  String estado=switch(in.accion()){case COMPLETAR->"COMPLETADO";case ERROR->"ERROR";case CANCELAR->"CANCELADO";};Instant ahora=Instant.now();
  jdbc.update("UPDATE lamontana.trabajo_impresion SET estado=?,fecha_fin=? WHERE id_trabajo_impresion=?",estado,Timestamp.from(ahora),t.id());
  String publico=in.accion()==Accion.COMPLETAR?"La imprenta registró un trabajo terminado; falta controlar su calidad.":"La imprenta registró una incidencia de producción. El pedido sigue pendiente de completar el trabajo.";
  evento(p,actor,in.operacion(),hash,t.id(),in.accion().name()+"_TRABAJO","IMPRIMIENDO",estado,"EN_PRODUCCION",in.motivo(),publico,ahora);
  return vista(pedidos.detalle(pedido,correo,true),correo);
 }
 @Transactional public Vista calidad(UUID pedido,UUID trabajo,Inspeccion in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,true);organizacion.exigirPermiso(correo,"CONTROLAR_CALIDAD");var actor=actor(correo);var hash=huella(List.of(pedido,trabajo,"CALIDAD",in));
  if(recuperar(in.operacion(),hash,p,actor))return vista(p,correo);
  version(p,in.version());texto(in.observaciones());confirmar(in.confirmacionManual());var t=trabajo(p,trabajo);
  if(!p.estado().equals("EN_PRODUCCION")||!t.vista().estado().equals("COMPLETADO")||t.vista().calidad()!=null)throw conflicto("La inspección requiere un trabajo terminado y todavía sin control de calidad.");
  if(in.resultado()==Resultado.APROBADO&&!in.checklist().completo())throw error(HttpStatus.BAD_REQUEST,"Marcá todos los controles para aprobar; si existe un problema elegí reimpresión o incidencia.");
  var c=in.checklist();Instant ahora=Instant.now();
  jdbc.update("INSERT INTO lamontana.control_calidad(id_trabajo_impresion,id_inspector,inspector_nombre,resultado,impresion_completa,calidad_correcta,alineacion_correcta,orden_correcto,terminaciones_correctas,observaciones,fecha) VALUES (?,?,?,?,?,?,?,?,?,?,?)",t.id(),actor.id(),actor.nombre(),in.resultado().name(),c.impresionCompleta(),c.calidadCorrecta(),c.alineacionCorrecta(),c.ordenCorrecto(),c.terminacionesCorrectas(),in.observaciones().strip(),Timestamp.from(ahora));
  boolean lista=jdbc.queryForObject("SELECT lamontana.calidad_pedido_aprobada(?)",Boolean.class,p.numero());
  String publico=lista?(p.reserva().modalidad().name().equals("RETIRO_SUCURSAL")?"Todos los trabajos superaron calidad. El pedido está listo para entregar en la sucursal; resta verificar el saldo y registrar la entrega.":"Todos los trabajos superaron calidad. El pedido está listo para preparar su despacho; todavía no salió hacia el destino."):in.resultado()==Resultado.APROBADO?"Un trabajo superó el control de calidad. Faltan otros ítems para completar el pedido.":"La imprenta detectó una incidencia de calidad y debe repetir el trabajo antes de continuar.";
  evento(p,actor,in.operacion(),hash,t.id(),"CONTROLAR_CALIDAD","COMPLETADO","COMPLETADO",lista?"LISTO_PARA_ENTREGA":"EN_PRODUCCION",in.observaciones(),publico,ahora);
  return vista(pedidos.detalle(pedido,correo,true),correo);
 }
 private List<Cobertura> cobertura(PedidoService.Detalle p){
  long q=jdbc.queryForObject("SELECT id_cotizacion FROM lamontana.pedido_actual WHERE id_pedido=?",Long.class,p.numero());var cot=p.condiciones().cotizadas();var fin=p.condiciones().actuales();
  return List.of(cobertura("Cotización aceptada",cot.pagoPrevioRequerido(),cot.senaRequerida(),cobertura.acreditado(q,cot.mediosAcreditacion())),cobertura("Confirmación del trabajo",fin.pagoPrevioRequerido(),fin.senaRequerida(),cobertura.acreditado(q,fin.mediosAcreditacion())));
 }
 private Cobertura cobertura(String nombre,String previo,String sena,BigDecimal aplicado){var requerido=new BigDecimal(previo).add(new BigDecimal(sena));return new Cobertura(nombre,requerido.toPlainString(),aplicado.toPlainString(),requerido.subtract(aplicado).max(BigDecimal.ZERO).toPlainString());}
 private List<String> bloqueos(PedidoService.Detalle p,boolean permiso){
  var out=new ArrayList<String>();if(!permiso)out.add("Se requiere el permiso para registrar producción manual.");
  if(!Set.of("APROBADO","EN_PRODUCCION").contains(p.estado()))out.add("Sólo se inicia producción después de la aprobación y antes de superar calidad.");
  if(!jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.sucursal WHERE codigo_publico=? AND estado='ACTIVA')",Boolean.class,p.oferta().sucursal().codigoPublico()))out.add("La sucursal está desactivada: no admite nuevos trabajos.");
  if(cobertura(p).stream().anyMatch(c->new BigDecimal(c.pendiente()).signum()>0))out.add("Falta anticipo acreditado y aplicado por los medios admitidos en las condiciones aceptadas. Un comprobante o dinero sin aplicar no habilita producción.");
  return List.copyOf(out);
 }
 private String bloqueoItem(Trabajo t){if(t==null)return null;return switch(t.estado()){
  case "IMPRIMIENDO"->"Este ítem ya tiene un trabajo en curso.";
  case "COMPLETADO"->t.calidad()==null?"Controlá la calidad del trabajo terminado.":t.calidad().resultado()==Resultado.APROBADO?"Este ítem ya superó calidad.":null;
  default->null;
 };}
 private List<RecursosRepositorio.Impresora> impresoras(PedidoService.Detalle p){long version=jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.pedido_actual WHERE id_pedido=?",Long.class,p.numero());return recursos.leer(version).impresoras().stream().filter(i->i.sucursal().equals(p.oferta().sucursal().codigoPublico())&&i.estado().equals("OPERATIVA")&&i.retiradaEn()==null).toList();}
 private boolean compatible(RecursosRepositorio.Impresora i,CotizacionService.ItemCotizado item){var t=item.trabajo();return i.formatos().contains(t.formato())&&(!t.color().name().equals("COLOR")||i.admiteColor())&&(!t.dobleFaz()||i.admiteDobleFaz())&&item.precio().hojas()<=i.capacidadHojas();}
 private List<Registro> trabajos(long pedido,UUID item){return jdbc.query("""
  SELECT t.*,o.codigo_publico AS origen,a.codigo_publico AS archivo FROM lamontana.trabajo_impresion t
  JOIN lamontana.cotizacion_item ci USING(id_cotizacion_item) JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado)
  LEFT JOIN lamontana.trabajo_impresion o ON o.id_trabajo_impresion=t.id_trabajo_origen
  WHERE t.id_pedido=? AND ci.codigo_publico=? ORDER BY t.id_trabajo_impresion
  """,(r,n)->{long id=r.getLong("id_trabajo_impresion");var c=jdbc.query("SELECT * FROM lamontana.control_calidad WHERE id_trabajo_impresion=?",(s,x)->new Calidad(Resultado.valueOf(s.getString("resultado")),new Checklist(s.getBoolean("impresion_completa"),s.getBoolean("calidad_correcta"),s.getBoolean("alineacion_correcta"),s.getBoolean("orden_correcto"),s.getBoolean("terminaciones_correctas")),s.getString("observaciones"),s.getString("inspector_nombre"),s.getTimestamp("fecha").toInstant()),id);
   var history=jdbc.query("SELECT accion,estado_origen,estado_destino,actor_nombre,motivo,fecha FROM lamontana.historial_trabajo_impresion WHERE id_trabajo_impresion=? ORDER BY id_historial_trabajo",(s,x)->new Evento(s.getString(1),s.getString(2),s.getString(3),s.getString(4),s.getString(5),s.getTimestamp(6).toInstant()),id);
   return new Registro(id,new Trabajo(r.getObject("codigo_publico",UUID.class),id,r.getObject("origen",UUID.class),r.getString("tipo"),r.getString("estado"),r.getObject("archivo",UUID.class),r.getString("impresora_nombre"),r.getString("motivo_excepcion"),r.getString("operador_nombre"),r.getTimestamp("fecha_inicio").toInstant(),r.getTimestamp("fecha_fin")==null?null:r.getTimestamp("fecha_fin").toInstant(),c.isEmpty()?null:c.get(0),history));
  },pedido,item);}
 private Registro trabajo(PedidoService.Detalle p,UUID id){return p.oferta().items().stream().flatMap(i->trabajos(p.numero(),i.codigoPublico()).stream()).filter(t->t.vista().codigoPublico().equals(id)).findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"El trabajo no pertenece a este pedido."));}
 private void evento(PedidoService.Detalle p,Actor a,UUID op,String hash,long trabajo,String accion,String origenTrabajo,String destinoTrabajo,String estado,String motivo,String publico,Instant fecha){
  jdbc.update("UPDATE lamontana.pedido SET estado=?,version=version+1 WHERE id_pedido=?",estado,p.numero());
  long evento=jdbc.queryForObject("INSERT INTO lamontana.historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha,estado_origen,version_pedido,accion,mensaje_cliente,actor_nombre,actor_tipo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id_evento",Long.class,p.numero(),a.id(),op,hash,estado,motivo.strip(),Timestamp.from(fecha),p.estado(),p.version()+1,accion,publico,a.nombre(),a.rol());
  jdbc.update("INSERT INTO lamontana.historial_trabajo_impresion(id_trabajo_impresion,id_evento_pedido,accion,estado_origen,estado_destino,id_actor,actor_nombre,motivo,fecha) VALUES (?,?,?,?,?,?,?,?,?)",trabajo,evento,accion,origenTrabajo,destinoTrabajo,a.id(),a.nombre(),motivo.strip(),Timestamp.from(fecha));
 }
 private boolean recuperar(UUID op,String hash,PedidoService.Detalle p,Actor a){var old=jdbc.query("SELECT id_pedido,id_actor,huella FROM lamontana.historial_estado_pedido WHERE id_operacion=? UNION ALL SELECT id_pedido,id_autor,huella FROM lamontana.observacion_interna WHERE id_operacion=?",(r,n)->new Object[]{r.getLong(1),r.getLong(2),r.getString(3)},op,op);if(old.isEmpty())return false;var v=old.get(0);if(old.size()!=1||(long)v[0]!=p.numero()||(long)v[1]!=a.id()||!hash.equals(v[2]))throw conflicto("La operación ya se utilizó con otros datos.");return true;}
 private Actor actor(String correo){return jdbc.queryForObject("SELECT u.id_usuario,u.nombre||' '||u.apellido,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.activo",(r,n)->new Actor(r.getLong(1),r.getString(2),r.getString(3)),correo);}
 private void bloquear(){for(long id:new long[]{764003,764001,764002,764004})jdbc.execute("SELECT pg_advisory_xact_lock("+id+")");}
 private void version(PedidoService.Detalle p,long v){if(p.version()!=v)throw conflicto("El pedido cambió. Actualizá su estado antes de registrar otro hecho.");}
 private void texto(String v){if(v==null||v.isBlank()||v.length()>500||v.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw error(HttpStatus.BAD_REQUEST,"Completá el motivo sin caracteres de control.");}
 private void confirmar(boolean v){if(!v)throw error(HttpStatus.BAD_REQUEST,"Confirmá el hecho manual que estás registrando.");}
 private String huella(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private ResponseStatusException conflicto(String m){return error(HttpStatus.CONFLICT,m);}
 private ResponseStatusException error(HttpStatus status,String m){return new ResponseStatusException(status,m);}
}
