package ar.com.lamontana.pedidos;

import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.catalogo.CatalogoService;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.CoberturaPagos;
import static ar.com.lamontana.configuracion.EntregaConfiguracionController.*;
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
public class PedidoService {
 private final JdbcTemplate jdbc;
 private final CotizacionService cotizaciones;
 private final OfertaOperativaService operativa;
 private final CatalogoService catalogo;
 private final EvaluadorFinanciero financiero;
 private final CoberturaPagos cobertura;
 private final EvaluadorFranjas franjas;
 private final ArchivosPrivados privados;
 private final OrganizacionService organizacion;
 private final Clock clock=Clock.systemUTC();
 private final JsonMapper json=JsonMapper.builder().build();
 public PedidoService(JdbcTemplate jdbc,CotizacionService cotizaciones,OfertaOperativaService operativa,CatalogoService catalogo,EvaluadorFinanciero financiero,CoberturaPagos cobertura,EvaluadorFranjas franjas,ArchivosPrivados privados,OrganizacionService organizacion){this.jdbc=jdbc;this.cotizaciones=cotizaciones;this.operativa=operativa;this.catalogo=catalogo;this.financiero=financiero;this.cobertura=cobertura;this.franjas=franjas;this.privados=privados;this.organizacion=organizacion;}
 public record Pdf(UUID item,UUID archivo,String nombre,String sha256,long bytes,int paginas,Instant aceptadaEn){}
 public record Franja(Modalidad modalidad,UUID destino,UUID origen,String nombre,String zonaHoraria,LocalDate fecha,String apertura,String cierre,Instant desde,Instant hasta,int cupoConfigurado,int plazasLibres,String costo){}
 public record Condiciones(CotizacionService.Condiciones cotizadas,EvaluadorFinanciero.Simulacion actuales,boolean revisionHumana){}
 public record Revision(UUID pedido,long version,UUID configuracion,long numeroConfiguracion,String huella,boolean habilitada,List<String> bloqueos,Condiciones condiciones,Franja franja,List<Pdf> archivos){}
 public record Evento(String estado,String motivo,Instant fecha){}
 public record Detalle(UUID codigoPublico,long numero,UUID cotizacion,String estado,Instant confirmadaEn,Instant aprobadaEn,UUID configuracion,long numeroConfiguracion,CotizacionService.Oferta oferta,Condiciones condiciones,Franja reserva,PedidoController.Contacto contacto,List<Pdf> archivos,List<Evento> historial,long version,String estadoReserva,Instant finalizadaEn){}
 public record Resumen(UUID codigoPublico,long numero,String estado,Instant confirmadaEn,String sucursal,String total,Modalidad modalidad){}
 public record Pagina(List<Resumen> elementos,long total,int pagina){}
 private record Actor(long id,String rol){}

 @Transactional public Revision revisar(UUID quote,String correo){bloquear();actor(correo,false);var q=cotizaciones.detalle(quote,correo);var vinculo=vinculado(quote);return vinculo==null?evaluar(q):new Revision(vinculo,q.version(),null,0,null,false,List.of(),null,null,List.of());}
 @Transactional public Revision revisarCorreccion(UUID quote,long pedido,String correo){bloquear();actor(correo,false);return evaluar(cotizaciones.detalle(quote,correo),pedido);}
 private Revision evaluar(CotizacionService.Detalle q){return evaluar(q,null);}
 private Revision evaluar(CotizacionService.Detalle q,Long excluirPedido){
  if(q.estado().equals("CONFIRMADA"))return new Revision(vinculado(q.codigoPublico()),q.version(),null,0,null,false,List.of(),null,null,List.of());
  if(!q.estado().equals("VIGENTE")||!clock.instant().isBefore(q.vigenteHasta()))throw conflicto("La cotización ya no está vigente. Conservá los pagos y solicitá una nueva oferta para continuar.");
  catalogo.reconciliarProgramaciones();var c=operativa.leer();var b=c.configuracion();var o=q.oferta();var branch=o.sucursal().codigoPublico();
  if(b==null||c.sucursales().stream().noneMatch(s->s.codigoPublico().equals(branch)))throw conflicto("La sucursal no está disponible para confirmar este trabajo.");
  int minPreparacion=0;
  for(var item:o.items()){
   var t=item.trabajo();
   if(c.opciones().stream().noneMatch(x->x.sucursal().equals(branch)&&x.servicio().equals(t.servicio())&&x.formato().equals(t.formato())&&x.papel().equals(t.papel())&&x.color()==t.color()&&(!t.dobleFaz()||x.dobleFaz())&&x.terminaciones().containsAll(t.terminaciones())))throw conflicto("Un servicio, papel o capacidad ya no está disponible. Revisá el trabajo con la imprenta; el precio guardado no se modificó.");
   minPreparacion=Math.max(minPreparacion,item.precio().minimoPreparacionMinutos());
   for(var servicio:c.catalogo().actual().servicios())if(servicio.servicio().equals(t.servicio())||t.terminaciones().contains(servicio.servicio()))minPreparacion=Math.max(minPreparacion,servicio.preparacionMinutos());
  }
  Instant ahora=clock.instant();var estimacion=operativa.calcularEntrega(c,new SimularEntrega(b.version(),branch,o.modalidad(),ahora,o.punto(),o.direccion()==null?null:o.direccion().territorio()),minPreparacion);
  var franja=disponible(o,b.entrega(),estimacion,ahora,c.sucursales().stream().filter(s->s.codigoPublico().equals(branch)).findFirst().orElseThrow(),excluirPedido);
  var fin=financiero.evaluar(b.modelo(),b.criterio(),b.pagos(),b.version(),new BigDecimal(o.total()),o.items().stream().mapToInt(x->x.precio().carillas()).sum());
  var condiciones=new Condiciones(o.condiciones(),fin,o.condiciones().revisionHumana()||fin.revisionHumana());
  var bloqueos=new ArrayList<String>();if(q.aceptadaEn()==null)bloqueos.add("Aceptá primero los importes y condiciones de la cotización.");
  var files=archivos(q.numero());for(var i:o.items())if(files.stream().noneMatch(f->f.item().equals(i.codigoPublico())))bloqueos.add("Falta un PDF inspeccionado y su vista previa aceptada: "+i.documento().nombre()+".");
  if(o.condiciones().cargaRequiereAcreditacion()&&!cobertura.cubre(q.numero(),o.condiciones().pagoPrevioRequerido(),o.condiciones().senaRequerida(),o.condiciones().mediosAcreditacion())||fin.cargaRequiereAcreditacion()&&!cobertura.cubre(q.numero(),fin.pagoPrevioRequerido(),fin.senaRequerida(),fin.mediosAcreditacion()))bloqueos.add("Falta dinero acreditado y aplicado por los medios admitidos para esta variante de pago previo o seña.");
  // La disponibilidad numérica puede variar sin cambiar las condiciones aceptadas. El
  // instante de la consulta tampoco es parte del consentimiento: sí lo son fecha/franja y PDF.
  var terms=new Franja(franja.modalidad(),franja.destino(),franja.origen(),franja.nombre(),franja.zonaHoraria(),franja.fecha(),franja.apertura(),franja.cierre(),franja.desde(),franja.hasta(),franja.cupoConfigurado(),0,franja.costo());
  String hash=huella(List.of(q.codigoPublico(),q.version(),b.codigoPublico(),condiciones,terms,files));
  return new Revision(null,q.version(),b.codigoPublico(),b.numero(),hash,bloqueos.isEmpty(),List.copyOf(bloqueos),condiciones,franja,files);
 }
 private Franja disponible(CotizacionService.Oferta o,EntregaRepositorio.Entrega e,EvaluadorCalendario.Simulacion estimacion,Instant ahora,OfertaOperativaService.Sucursal sucursalActual,Long excluirPedido){
  UUID branch=o.sucursal().codigoPublico(),dest;String zona,nombre;List<PuntoEntregaController.Franja> slots;
  if(o.modalidad()==Modalidad.RETIRO_SUCURSAL){var h=e.horariosPorSucursal().stream().filter(x->x.sucursal().equals(branch)).findFirst().orElseThrow();dest=branch;zona=h.zonaHoraria();slots=h.franjasRetiro();nombre=sucursalActual.nombre()+" · "+sucursalActual.direccion();}
  else if(o.modalidad()==Modalidad.RETIRO_PUNTO_ENTREGA){var p=e.puntos().stream().filter(x->x.codigoPublico().equals(o.punto())).findFirst().orElseThrow();dest=p.codigoPublico();zona=p.zonaHoraria();slots=p.sucursales().stream().filter(x->x.sucursal().equals(branch)).findFirst().orElseThrow().franjas();nombre=p.nombre()+" · "+p.calle()+" "+p.numero()+", "+p.localidad()+", "+p.provincia();}
  else{var d=estimacion.destinoZona();var z=e.zonas().stream().filter(x->x.codigoPublico().equals(d.zona())).findFirst().orElseThrow();dest=z.codigoPublico();zona=z.zonaHoraria();slots=z.franjas();nombre=o.destino();}
  var v=franjas.siguiente(estimacion.disponibleDesde(),zona,slots,EvaluadorCalendario.limite(ahora,zona),w->ocupadas(o.modalidad(),dest,branch,w,excluirPedido)<w.cupoConfigurado());
  return new Franja(o.modalidad(),dest,branch,nombre,zona,v.fecha(),v.apertura(),v.cierre(),v.desde(),v.hasta(),v.cupoConfigurado(),v.cupoConfigurado()-ocupadas(o.modalidad(),dest,branch,v,excluirPedido),o.costoEntrega());
 }
 private int ocupadas(Modalidad modo,UUID dest,UUID branch,EvaluadorFranjas.Ventana v,Long excluirPedido){return jdbc.queryForObject("SELECT lamontana.reservas_ocupadas(?,?,?,?::date,?::time,?::time,?,?,?)",Integer.class,modo.name(),dest,branch,v.fecha().toString(),v.apertura(),v.cierre(),Timestamp.from(v.desde()),Timestamp.from(v.hasta()),excluirPedido);}
 @Transactional public Detalle confirmar(UUID quote,PedidoController.Confirmar in,String correo){
  bloquear();var a=actor(correo,false);var q=cotizaciones.detalle(quote,correo);if(esCorreccion(quote))throw conflicto("Esta oferta pertenece a una corrección. Respondé desde el pedido existente; no crea otro pedido.");String command=huella(List.of(quote,in));
  var old=jdbc.query("SELECT p.codigo_publico,h.id_actor,h.huella FROM lamontana.historial_estado_pedido h JOIN lamontana.pedido p USING(id_pedido) WHERE h.id_operacion=?",(r,n)->new Object[]{r.getObject(1,UUID.class),r.getLong(2),r.getString(3)},in.operacion());
  if(!old.isEmpty()){var r=old.get(0);if(a.id()!=(long)r[1]||!command.equals(r[2]))throw conflicto("La operación ya se utilizó con otros datos.");return detalle((UUID)r[0],correo,false);}
  if(!in.condicionesAceptadas())throw error(HttpStatus.BAD_REQUEST,"Revisá y aceptá las condiciones finales.");
  var revision=evaluar(q);if(revision.pedido()!=null)throw conflicto("Esta cotización ya tiene un pedido. Consultá el resultado guardado.");
  if(!revision.habilitada())throw conflicto(String.join(" ",revision.bloqueos()));
  if(q.version()!=in.version()||!revision.huella().equals(in.huella()))throw conflicto("Cambiaron los PDF, las condiciones o la franja disponible. Actualizá la revisión final y aceptá sus datos antes de confirmar.");
  boolean domicilio=q.oferta().modalidad()==Modalidad.ENVIO_DOMICILIO;
  if(domicilio!=(in.contacto()!=null))throw error(HttpStatus.BAD_REQUEST,"Indicá receptor y teléfono sólo para envío a domicilio.");
  if(in.contacto()!=null){texto(in.contacto().receptor());texto(in.contacto().telefono());if((!in.contacto().telefono().matches("[+0-9 ()-]{6,40}")||in.contacto().telefono().chars().filter(Character::isDigit).count()<6))throw error(HttpStatus.BAD_REQUEST,"Ingresá un teléfono de contacto válido.");}
  for(var f:revision.archivos())try{privados.original(f.archivo(),f.sha256(),f.bytes());}catch(java.io.IOException ex){throw conflicto("No se pudo verificar la integridad de un PDF. Volvé a cargarlo y aceptar su vista previa antes de confirmar.");}
  Instant now=clock.instant();if(!now.isBefore(q.vigenteHasta()))throw conflicto("La cotización venció durante la revisión. Solicitá una nueva oferta; el dinero se conserva.");
  UUID codigo=UUID.randomUUID();String estado=revision.condiciones().revisionHumana()?"PENDIENTE_REVISION":"APROBADO";
  long id=jdbc.queryForObject("""
   INSERT INTO lamontana.pedido(codigo_publico,id_cotizacion,id_usuario_creador,id_sucursal,id_configuracion_version,estado,modo_aprobacion,confirmada_en,aprobada_en,total,condiciones_finales,receptor,telefono)
   SELECT ?,c.id_cotizacion,c.id_usuario_creador,c.id_sucursal,(SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?),?,?,?,?,?,?::jsonb,?,?
   FROM lamontana.cotizacion c WHERE c.codigo_publico=? RETURNING id_pedido
   """,Long.class,codigo,revision.configuracion(),estado,revision.condiciones().revisionHumana()?"HUMANA":"AUTOMATICA",Timestamp.from(now),revision.condiciones().revisionHumana()?null:Timestamp.from(now),new BigDecimal(q.oferta().total()),json.writeValueAsString(revision.condiciones()),in.contacto()==null?null:in.contacto().receptor().strip(),in.contacto()==null?null:in.contacto().telefono().strip(),quote);
  for(var f:revision.archivos())jdbc.update("INSERT INTO lamontana.pedido_item(id_pedido,id_cotizacion_item,id_archivo_almacenado,id_aceptacion_vista_previa) SELECT ?,ci.id_cotizacion_item,a.id_archivo_almacenado,v.id_aceptacion_vista_previa FROM lamontana.cotizacion_item ci,lamontana.archivo_almacenado a JOIN lamontana.aceptacion_vista_previa v USING(id_archivo_almacenado) WHERE ci.codigo_publico=? AND a.codigo_publico=?",id,f.item(),f.archivo());
  var f=revision.franja();jdbc.update("INSERT INTO lamontana.reserva_entrega(id_pedido,modalidad,destino_publico,origen_publico,nombre_destino,zona_horaria,fecha_local,apertura,cierre,franja_desde,franja_hasta,cupo_confirmado,costo) VALUES (?,?,?,?,?,?,?::date,?::time,?::time,?,?,?,?)",id,f.modalidad().name(),f.destino(),f.origen(),f.nombre(),f.zonaHoraria(),f.fecha().toString(),f.apertura(),f.cierre(),Timestamp.from(f.desde()),Timestamp.from(f.hasta()),f.cupoConfigurado(),new BigDecimal(f.costo()));
  jdbc.update("INSERT INTO lamontana.historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha) VALUES (?,?,?,?,?,?,?)",id,a.id(),in.operacion(),command,estado,revision.condiciones().revisionHumana()?"Pedido confirmado por el cliente; pendiente de revisión humana.":"Pedido confirmado por el cliente; aprobado según las reglas configuradas y los controles técnicos. Producción todavía no iniciada.",Timestamp.from(now));
  jdbc.update("UPDATE lamontana.cotizacion SET estado='CONFIRMADA',confirmada_en=?,id_configuracion_confirmacion=(SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?),version=version+1 WHERE codigo_publico=?",Timestamp.from(now),revision.configuracion(),quote);
  return detalle(codigo,correo,false);
 }
 private List<Pdf> archivos(long quote){return jdbc.query("""
  SELECT ci.codigo_publico AS item,a.codigo_publico AS archivo,a.nombre_original,a.sha256,a.cantidad_bytes,a.cantidad_paginas,v.fecha
  FROM lamontana.cotizacion_item ci JOIN lamontana.archivo_trabajo t USING(id_cotizacion_item)
  JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) JOIN lamontana.aceptacion_vista_previa v USING(id_archivo_almacenado)
  WHERE ci.id_cotizacion=? AND t.activo AND a.estado='VALIDO' AND a.finalidad='TRABAJO'
  AND a.cantidad_paginas=ci.cantidad_paginas_declaradas AND a.sha256=v.sha256
  AND (SELECT count(*) FROM lamontana.validacion_archivo va WHERE va.id_archivo_almacenado=a.id_archivo_almacenado AND va.resultado='ACEPTADO')=2 ORDER BY ci.orden
  """,(r,n)->new Pdf(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getString(4),r.getLong(5),r.getInt(6),r.getTimestamp(7).toInstant()),quote);}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Detalle detalle(UUID codigo,String correo,boolean interno){
  var a=actor(correo,interno);var rows=jdbc.query("SELECT p.*,c.codigo_publico AS cotizacion,c.oferta,s.codigo_publico AS sucursal,v.codigo_publico AS config,v.numero_version FROM lamontana.pedido_actual p JOIN lamontana.cotizacion c USING(id_cotizacion) JOIN lamontana.sucursal s ON s.id_sucursal=p.id_sucursal JOIN lamontana.configuracion_version v ON v.id_configuracion_version=p.id_configuracion_version WHERE p.codigo_publico=? AND (? OR p.id_usuario_creador=?)",(r,n)->{
   if(interno&&!a.rol().equals("ADMIN_ADMIN"))organizacion.sucursalAutorizada(correo,r.getObject("sucursal",UUID.class));long id=r.getLong("id_pedido");
   var reserva=jdbc.queryForObject("SELECT * FROM lamontana.reserva_entrega WHERE id_pedido=? ORDER BY id_reserva_entrega DESC LIMIT 1",(rs,nr)->new Franja(Modalidad.valueOf(rs.getString("modalidad")),rs.getObject("destino_publico",UUID.class),rs.getObject("origen_publico",UUID.class),rs.getString("nombre_destino"),rs.getString("zona_horaria"),rs.getDate("fecha_local").toLocalDate(),rs.getTime("apertura").toLocalTime().toString(),rs.getTime("cierre").toLocalTime().toString(),rs.getTimestamp("franja_desde").toInstant(),rs.getTimestamp("franja_hasta").toInstant(),rs.getInt("cupo_confirmado"),0,rs.getBigDecimal("costo").toPlainString()),id);
   var files=jdbc.query("SELECT ci.codigo_publico,a.codigo_publico,a.nombre_original,a.sha256,a.cantidad_bytes,a.cantidad_paginas,v.fecha FROM lamontana.pedido_archivo_actual i JOIN lamontana.cotizacion_item ci USING(id_cotizacion_item) JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) JOIN lamontana.aceptacion_vista_previa v USING(id_aceptacion_vista_previa) WHERE i.id_pedido=? ORDER BY ci.orden",(rs,nr)->new Pdf(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),rs.getLong(5),rs.getInt(6),rs.getTimestamp(7).toInstant()),id);
   var history=jdbc.query("SELECT estado_destino,CASE WHEN version_pedido IS NULL THEN motivo ELSE mensaje_cliente END,fecha FROM lamontana.historial_estado_pedido WHERE id_pedido=? ORDER BY id_evento",(rs,nr)->new Evento(rs.getString(1),rs.getString(2),rs.getTimestamp(3).toInstant()),id);
   return new Detalle(codigo,id,r.getObject("cotizacion",UUID.class),r.getString("estado"),r.getTimestamp("confirmada_en").toInstant(),r.getTimestamp("aprobada_en")==null?null:r.getTimestamp("aprobada_en").toInstant(),r.getObject("config",UUID.class),r.getLong("numero_version"),json.readValue(r.getString("oferta"),CotizacionService.Oferta.class),json.readValue(r.getString("condiciones_finales"),Condiciones.class),reserva,r.getString("receptor")==null?null:new PedidoController.Contacto(r.getString("receptor"),r.getString("telefono")),files,history,r.getLong("version"),jdbc.queryForObject("SELECT estado FROM lamontana.reserva_entrega WHERE id_pedido=? ORDER BY id_reserva_entrega DESC LIMIT 1",String.class,id),r.getTimestamp("finalizada_en")==null?null:r.getTimestamp("finalizada_en").toInstant());
  },codigo,interno,a.id());if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"El pedido no está disponible en tu cuenta.");return rows.get(0);
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Pagina listar(UUID branch,int pagina,String estado,String correo){
  var a=actor(correo,branch!=null);if(branch!=null&&!a.rol().equals("ADMIN_ADMIN"))organizacion.sucursalAutorizada(correo,branch);if(pagina<0||pagina>100000)throw error(HttpStatus.BAD_REQUEST,"Página inválida.");
  if(estado!=null&&!Set.of("PENDIENTE_REVISION","CORRECCION_SOLICITADA","APROBADO","RECHAZADO","CANCELADO").contains(estado))throw error(HttpStatus.BAD_REQUEST,"Estado de pedido inválido.");
  String from=" FROM lamontana.pedido_actual p JOIN lamontana.sucursal s USING(id_sucursal) JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE "+(branch==null?"p.id_usuario_creador=?":"s.codigo_publico=?");Object filtro=branch==null?a.id():branch;
  from+=" AND (?::text IS NULL OR p.estado=?)";
  long count=jdbc.queryForObject("SELECT count(*)"+from,Long.class,filtro,estado,estado);
  var ids=jdbc.query("SELECT p.codigo_publico,p.id_pedido,p.estado,p.confirmada_en,s.nombre,p.total,c.oferta->>'modalidad' AS modalidad"+from+" ORDER BY p.id_pedido DESC LIMIT 25 OFFSET ?",(r,n)->new Resumen(r.getObject(1,UUID.class),r.getLong(2),r.getString(3),r.getTimestamp(4).toInstant(),r.getString(5),r.getBigDecimal(6).toPlainString(),Modalidad.valueOf(r.getString(7))),filtro,estado,estado,(long)pagina*25);
  return new Pagina(ids,count,pagina);
 }
 private boolean esCorreccion(UUID quote){return jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.cotizacion_correccion cc JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE c.codigo_publico=?)",Boolean.class,quote);}
 private UUID vinculado(UUID quote){var ids=jdbc.query("SELECT p.codigo_publico FROM lamontana.pedido p JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE c.codigo_publico=? UNION SELECT p.codigo_publico FROM lamontana.cotizacion_correccion cc JOIN lamontana.cotizacion c USING(id_cotizacion) JOIN lamontana.solicitud_correccion s USING(id_solicitud_correccion) JOIN lamontana.pedido p USING(id_pedido) WHERE c.codigo_publico=?",(r,n)->r.getObject(1,UUID.class),quote,quote);return ids.isEmpty()?null:ids.get(0);}
 private Actor actor(String correo,boolean interno){var rows=jdbc.query("SELECT u.id_usuario,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.activo",(r,n)->new Actor(r.getLong(1),r.getString(2)),correo);if(rows.isEmpty())throw error(HttpStatus.FORBIDDEN,"La cuenta no está activa.");var a=rows.get(0);if(interno?!List.of("ADMIN_ADMIN","EMPLEADO").contains(a.rol()):!a.rol().equals("CLIENTE"))throw error(HttpStatus.FORBIDDEN,"No tenés acceso a esta operación.");return a;}
 private void bloquear(){for(long id:new long[]{764003,764001,764002,764004})jdbc.execute("SELECT pg_advisory_xact_lock("+id+")");}
 private void texto(String texto){if(texto==null||texto.isBlank()||texto.codePoints().anyMatch(Character::isISOControl))throw error(HttpStatus.BAD_REQUEST,"Completá los datos de contacto sin caracteres de control.");}
 private String huella(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private ResponseStatusException conflicto(String msg){return error(HttpStatus.CONFLICT,msg);}
 private ResponseStatusException error(HttpStatus status,String msg){return new ResponseStatusException(status,msg);}
}
