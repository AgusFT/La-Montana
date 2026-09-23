package ar.com.lamontana.entregas;
import static ar.com.lamontana.entregas.ReprogramacionController.*;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pedidos.PedidoService;
import ar.com.lamontana.pedidos.PedidoService.Franja;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
@Service
public class ReprogramacionService {
 private final JdbcTemplate jdbc;private final PedidoService pedidos;private final OrganizacionService organizacion;private final OfertaOperativaService operativa;private final EvaluadorFranjas franjas;
 private final Clock clock=Clock.systemUTC();private final JsonMapper json=JsonMapper.builder().build();
 private static final Set<String> ESTADOS=Set.of("PENDIENTE_REVISION","APROBADO","EN_PRODUCCION","LISTO_PARA_ENTREGA");
 public ReprogramacionService(JdbcTemplate jdbc,PedidoService pedidos,OrganizacionService organizacion,OfertaOperativaService operativa,EvaluadorFranjas franjas){this.jdbc=jdbc;this.pedidos=pedidos;this.organizacion=organizacion;this.operativa=operativa;this.franjas=franjas;}
 public record Decision(String resultado,String responsable,String motivo,Instant fecha){}
 public record Propuesta(UUID codigoPublico,String estado,boolean vigente,String bloqueo,Franja anterior,Franja nueva,UUID configuracion,String responsable,String motivoInterno,String mensajeCliente,Instant fecha,Decision decision){}
 public record Vista(long version,String estadoPedido,boolean puedeProponer,boolean puedeRetirar,String bloqueo,Propuesta pendiente,List<Propuesta> historial){}
 public record Disponibilidad(UUID configuracion,long version,String zonaHoraria,LocalDate fecha,List<Franja> franjas,String mensaje){}
 private record Actor(long id,String nombre,String rol){}
 private record Registro(long id,UUID codigo,long reserva,UUID configuracion,String estado,Franja nueva){}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Vista consultar(UUID pedido,String correo,boolean interno){return vista(pedidos.detalle(pedido,correo,interno),correo,interno);}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Disponibilidad disponibilidad(UUID pedido,LocalDate fecha,String correo){var p=pedidos.detalle(pedido,correo,true);organizacion.exigirPermiso(correo,"GESTIONAR_ENTREGAS");exigirEtapa(p);return ventanas(p,fecha);}
 private Vista vista(PedidoService.Detalle p,String correo,boolean interno){
  boolean permiso=interno&&organizacion.contexto(correo).permisos().contains("GESTIONAR_ENTREGAS");String bloqueo=!ESTADOS.contains(p.estado())?"La reprogramación requiere un pedido sin corrección pendiente, cancelación, entrega ni cierre.":!p.estadoReserva().equals("ACTIVA")?"El pedido ya no tiene una reserva activa.":null;
  var list=jdbc.query("SELECT p.*,c.codigo_publico AS configuracion FROM lamontana.propuesta_reprogramacion p JOIN lamontana.configuracion_version c USING(id_configuracion_version) WHERE p.id_pedido=? ORDER BY p.id_propuesta_reprogramacion DESC",(r,n)->{
   var registro=mapear(r);var reason=invalida(p,registro);var decisions=jdbc.query("SELECT resultado,actor_nombre,motivo,fecha FROM lamontana.resolucion_reprogramacion WHERE id_propuesta_reprogramacion=?",(d,i)->new Decision(d.getString(1),interno?d.getString(2):d.getString(1).equals("RETIRADA")?"La imprenta":"Cliente",d.getString(1).equals("RETIRADA")&&!interno?"La imprenta retiró la propuesta; se conserva la reserva anterior.":d.getString(3),d.getTimestamp(4).toInstant()),registro.id());
   return new Propuesta(registro.codigo(),registro.estado(),reason==null,reason,leerReserva(registro.reserva()),registro.nueva(),registro.configuracion(),interno?r.getString("actor_nombre"):"La imprenta",interno?r.getString("motivo_interno"):null,r.getString("mensaje_cliente"),r.getTimestamp("fecha").toInstant(),decisions.isEmpty()?null:decisions.get(0));
  },p.numero());
  var pendiente=list.stream().filter(r->r.estado().equals("PENDIENTE")).findFirst().orElse(null);
  String msg=bloqueo!=null?bloqueo:pendiente!=null?"Resolvé o retirá la propuesta pendiente antes de enviar otra.":interno&&!permiso?"Se requiere permiso de gestión de entregas.":null;
  return new Vista(p.version(),p.estado(),permiso&&msg==null,permiso&&pendiente!=null,msg,pendiente,list);
 }
 private String invalida(PedidoService.Detalle p,Registro r){
  if(!r.estado().equals("PENDIENTE"))return "La propuesta ya fue resuelta.";
  if(!ESTADOS.contains(p.estado()))return "El estado actual del pedido no admite aceptar esta reprogramación.";
  if(!p.estadoReserva().equals("ACTIVA")||reserva(p)!=r.reserva())return "La reserva cambió; esta propuesta ya no puede reemplazarla.";
  if(!clock.instant().isBefore(r.nueva().hasta()))return "La franja propuesta ya terminó.";
  UUID actual=jdbc.query("SELECT codigo_publico FROM lamontana.configuracion_version WHERE estado='ACTIVA'",(x,n)->x.getObject(1,UUID.class)).stream().findFirst().orElse(null);
  if(!r.configuracion().equals(actual))return "La configuración cambió. La imprenta debe proponer una franja con la versión actual.";
  return null;
 }
 private Disponibilidad ventanas(PedidoService.Detalle p,LocalDate fecha){
  var b=operativa.configuracionActiva();if(b==null)throw conflicto("No hay configuración operativa activa.");var e=b.entrega();var old=p.reserva();UUID branch=old.origen();
  if(!jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.sucursal WHERE codigo_publico=? AND estado='ACTIVA')",Boolean.class,branch)||!e.modalidades().contains(old.modalidad()))throw conflicto("La sucursal o modalidad ya no está habilitada para reprogramar.");
  String zona,nombre;List<PuntoEntregaController.Franja> templates;
  if(old.modalidad()==EntregaConfiguracionController.Modalidad.RETIRO_SUCURSAL){
   var h=e.horariosPorSucursal().stream().filter(x->x.sucursal().equals(branch)).findFirst().orElseThrow(()->conflicto("La sucursal no tiene horarios configurados."));zona=h.zonaHoraria();
   nombre=jdbc.queryForObject("SELECT nombre||' · '||calle||' '||numero||', '||localidad||', '||provincia FROM lamontana.sucursal WHERE codigo_publico=?",String.class,branch);
   templates=h.franjasRetiro().stream().filter(f->h.dias().stream().anyMatch(d->d.dia().equals(f.dia())&&Boolean.TRUE.equals(d.habilitado())&&d.apertura()!=null&&d.cierre()!=null&&d.apertura().compareTo(f.apertura())<=0&&d.cierre().compareTo(f.cierre())>=0)).toList();
  }else if(old.modalidad()==EntregaConfiguracionController.Modalidad.RETIRO_PUNTO_ENTREGA){
   var pt=e.puntos().stream().filter(x->x.codigoPublico().equals(old.destino())).findFirst().orElseThrow(()->conflicto("El punto ya no está configurado."));
   if(!jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.disponibilidad_punto_entrega d JOIN lamontana.punto_entrega p USING(id_punto_entrega) WHERE p.codigo_publico=? AND d.estado='HABILITADO')",Boolean.class,old.destino()))throw conflicto("El punto no está disponible para reprogramar.");
   var link=pt.sucursales().stream().filter(x->x.sucursal().equals(branch)&&x.habilitado()).findFirst().orElseThrow(()->conflicto("El punto ya no sirve a esta sucursal."));zona=pt.zonaHoraria();nombre=pt.nombre()+" · "+pt.calle()+" "+pt.numero()+", "+pt.localidad()+", "+pt.provincia();templates=link.franjas();
  }else{
   var z=e.zonas().stream().filter(x->x.codigoPublico().equals(old.destino())&&x.habilitada()).findFirst().orElseThrow(()->conflicto("La zona ya no está habilitada."));
   if(p.oferta().direccion()==null||z.territorios().stream().noneMatch(t->t.normalizado().equals(p.oferta().direccion().territorio().normalizado())))throw conflicto("El domicilio ya no pertenece a la cobertura configurada.");zona=z.zonaHoraria();nombre=old.nombre();templates=z.franjas();
  }
  if(!nombre.equals(old.nombre()))throw conflicto("Los datos del destino cambiaron. Una reprogramación sólo cambia fecha y franja; revisá el destino con la imprenta.");
  Instant ahora=clock.instant();ZoneId zone=ZoneId.of(zona);LocalDate hoy=ahora.atZone(zone).toLocalDate();if(fecha==null||fecha.isBefore(hoy)||fecha.isAfter(hoy.plusYears(5)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Elegí una fecha desde hoy dentro del horizonte técnico de cinco años.");
  var options=franjas.dia(fecha,zona,templates).stream().filter(v->v.hasta().isAfter(ahora)).map(v->{int used=jdbc.queryForObject("SELECT lamontana.reservas_ocupadas(?,?,?,?::date,?::time,?::time,?,?,?)",Integer.class,old.modalidad().name(),old.destino(),branch,v.fecha().toString(),v.apertura(),v.cierre(),Timestamp.from(v.desde()),Timestamp.from(v.hasta()),p.numero());return new Franja(old.modalidad(),old.destino(),branch,old.nombre(),zona,fecha,v.apertura(),v.cierre(),v.desde(),v.hasta(),v.cupoConfigurado(),Math.max(0,v.cupoConfigurado()-used),old.costo());}).toList();
  return new Disponibilidad(b.codigoPublico(),p.version(),zona,fecha,options,"Una propuesta no reserva cupo. Se vuelve a comprobar al aceptar; se mantienen destino y costo originales.");
 }
 @Transactional public Vista proponer(UUID pedido,Proponer in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,true);organizacion.exigirPermiso(correo,"GESTIONAR_ENTREGAS");var a=actor(correo);String hash=huella(List.of(pedido,"PROPONER",in));if(replay(in.operacion(),hash,p,a))return vista(p,correo,true);
  version(p,in.version());exigirEtapa(p);texto(in.motivo());texto(in.mensajeCliente());confirmar(in.confirmado());
  if(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.propuesta_reprogramacion WHERE id_pedido=? AND estado='PENDIENTE')",Boolean.class,p.numero()))throw conflicto("Hay una propuesta pendiente. Retirala antes de proponer otra.");
  var available=ventanas(p,in.fecha());if(!available.configuracion().equals(in.configuracion()))throw conflicto("La configuración cambió. Consultá de nuevo las franjas.");var selected=seleccionar(available,in.desde(),in.hasta());
  if(selected.desde().equals(p.reserva().desde())&&selected.hasta().equals(p.reserva().hasta()))throw conflicto("Esa ya es la fecha y franja reservada.");
  jdbc.update("INSERT INTO lamontana.propuesta_reprogramacion(codigo_publico,id_pedido,id_reserva_anterior,id_configuracion_version,id_actor,actor_nombre,id_operacion,huella,version_pedido,franja_nueva,motivo_interno,mensaje_cliente,fecha) SELECT ?,?,?,id_configuracion_version,?,?,?,?,?,?::jsonb,?,?,? FROM lamontana.configuracion_version WHERE codigo_publico=?",UUID.randomUUID(),p.numero(),reserva(p),a.id(),a.nombre(),in.operacion(),hash,p.version(),json.writeValueAsString(selected),in.motivo().strip(),in.mensajeCliente().strip(),Timestamp.from(clock.instant()),in.configuracion());
  return vista(p,correo,true);
 }
 @Transactional public Vista retirar(UUID pedido,Retirar in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,true);organizacion.exigirPermiso(correo,"GESTIONAR_ENTREGAS");var a=actor(correo);String hash=huella(List.of(pedido,"RETIRAR",in));if(replay(in.operacion(),hash,p,a))return vista(p,correo,true);version(p,in.version());texto(in.motivo());confirmar(in.confirmado());var r=registro(p,in.propuesta());pendiente(r);resolver(r,a,in.operacion(),hash,"RETIRADA",in.motivo(),clock.instant(),p.version(),null,null);return vista(p,correo,true);
 }
 @Transactional public Vista responder(UUID pedido,Responder in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,false);var a=actor(correo);String hash=huella(List.of(pedido,"RESPONDER",in));if(replay(in.operacion(),hash,p,a))return vista(p,correo,false);version(p,in.version());texto(in.motivo());confirmar(in.confirmado());var r=registro(p,in.propuesta());pendiente(r);Instant now=clock.instant();
  if(!in.aceptada()){resolver(r,a,in.operacion(),hash,"RECHAZADA",in.motivo(),now,p.version(),null,null);return vista(p,correo,false);}
  String bloqueo=invalida(p,r);if(bloqueo!=null)throw conflicto(bloqueo);var available=ventanas(p,r.nueva().fecha());var f=seleccionar(available,r.nueva().desde(),r.nueva().hasta());
  if(!available.configuracion().equals(r.configuracion())||!mismaFranja(f,r.nueva()))throw conflicto("Cambió la franja propuesta. La imprenta debe revisarla antes de tu aceptación.");
  jdbc.update("UPDATE lamontana.pedido SET version=version+1 WHERE id_pedido=?",p.numero());
  long event=jdbc.queryForObject("INSERT INTO lamontana.historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha,estado_origen,version_pedido,accion,mensaje_cliente,actor_nombre,actor_tipo) VALUES (?,?,?,?,?,?,?,?,?,'REPROGRAMAR',?,?,?) RETURNING id_evento",Long.class,p.numero(),a.id(),in.operacion(),hash,p.estado(),in.motivo().strip(),Timestamp.from(now),p.estado(),p.version()+1,"El cliente aceptó reprogramar la entrega para "+f.fecha()+" de "+f.apertura()+" a "+f.cierre()+" ("+f.zonaHoraria()+"). Se conservan el precio y el recorrido físico.",a.nombre(),a.rol());
  jdbc.update("UPDATE lamontana.reserva_entrega SET estado='REPROGRAMADA' WHERE id_reserva_entrega=?",r.reserva());
  long nueva=jdbc.queryForObject("INSERT INTO lamontana.reserva_entrega(id_pedido,modalidad,destino_publico,origen_publico,nombre_destino,zona_horaria,fecha_local,apertura,cierre,franja_desde,franja_hasta,cupo_confirmado,costo,id_configuracion_reprogramacion) SELECT ?,?,?,?,?,?,?::date,?::time,?::time,?,?,?,?,id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=? RETURNING id_reserva_entrega",Long.class,p.numero(),f.modalidad().name(),f.destino(),f.origen(),f.nombre(),f.zonaHoraria(),f.fecha().toString(),f.apertura(),f.cierre(),Timestamp.from(f.desde()),Timestamp.from(f.hasta()),f.cupoConfigurado(),new BigDecimal(f.costo()),r.configuracion());
  jdbc.update("UPDATE lamontana.codigo_entrega SET revocado_en=? WHERE id_pedido=? AND revocado_en IS NULL AND utilizado_en IS NULL",Timestamp.from(now),p.numero());resolver(r,a,in.operacion(),hash,"ACEPTADA",in.motivo(),now,p.version()+1,nueva,event);
  return vista(pedidos.detalle(pedido,correo,false),correo,false);
 }
 private boolean mismaFranja(Franja a,Franja b){return a.modalidad()==b.modalidad()&&a.destino().equals(b.destino())&&a.origen().equals(b.origen())&&a.nombre().equals(b.nombre())&&a.zonaHoraria().equals(b.zonaHoraria())&&a.fecha().equals(b.fecha())&&a.apertura().equals(b.apertura())&&a.cierre().equals(b.cierre())&&a.cupoConfigurado()==b.cupoConfigurado()&&a.costo().equals(b.costo());}
 private Franja seleccionar(Disponibilidad d,Instant desde,Instant hasta){var f=d.franjas().stream().filter(x->x.desde().equals(desde)&&x.hasta().equals(hasta)).findFirst().orElseThrow(()->conflicto("La fecha y franja no corresponden a una ventana configurada y vigente."));if(f.plazasLibres()<1)throw conflicto("La franja ya no tiene cupo disponible. La reserva anterior se conserva.");return f;}
 private void resolver(Registro r,Actor a,UUID op,String hash,String estado,String motivo,Instant fecha,long version,Long nueva,Long evento){jdbc.update("UPDATE lamontana.propuesta_reprogramacion SET estado=? WHERE id_propuesta_reprogramacion=?",estado,r.id());jdbc.update("INSERT INTO lamontana.resolucion_reprogramacion(id_propuesta_reprogramacion,id_actor,actor_nombre,id_operacion,huella,resultado,motivo,fecha,version_pedido,id_reserva_nueva,id_evento_pedido) VALUES (?,?,?,?,?,?,?,?,?,?,?)",r.id(),a.id(),a.nombre(),op,hash,estado,motivo.strip(),Timestamp.from(fecha),version,nueva,evento);}
 private Registro registro(PedidoService.Detalle p,UUID codigo){var rows=jdbc.query("SELECT p.*,c.codigo_publico AS configuracion FROM lamontana.propuesta_reprogramacion p JOIN lamontana.configuracion_version c USING(id_configuracion_version) WHERE p.id_pedido=? AND p.codigo_publico=?",(r,n)->mapear(r),p.numero(),codigo);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"La propuesta no pertenece a este pedido.");return rows.get(0);}
 private Registro mapear(ResultSet r)throws SQLException{return new Registro(r.getLong("id_propuesta_reprogramacion"),r.getObject("codigo_publico",UUID.class),r.getLong("id_reserva_anterior"),r.getObject("configuracion",UUID.class),r.getString("estado"),json.readValue(r.getString("franja_nueva"),Franja.class));}
 private Franja leerReserva(long id){return jdbc.queryForObject("SELECT * FROM lamontana.reserva_entrega WHERE id_reserva_entrega=?",(r,n)->new Franja(EntregaConfiguracionController.Modalidad.valueOf(r.getString("modalidad")),r.getObject("destino_publico",UUID.class),r.getObject("origen_publico",UUID.class),r.getString("nombre_destino"),r.getString("zona_horaria"),r.getDate("fecha_local").toLocalDate(),r.getTime("apertura").toLocalTime().toString(),r.getTime("cierre").toLocalTime().toString(),r.getTimestamp("franja_desde").toInstant(),r.getTimestamp("franja_hasta").toInstant(),r.getInt("cupo_confirmado"),0,r.getBigDecimal("costo").toPlainString()),id);}
 private long reserva(PedidoService.Detalle p){return jdbc.queryForObject("SELECT id_reserva_entrega FROM lamontana.reserva_entrega WHERE id_pedido=? AND estado='ACTIVA'",Long.class,p.numero());}
 private boolean replay(UUID op,String hash,PedidoService.Detalle p,Actor a){var rows=jdbc.query("SELECT id_pedido,id_actor,huella FROM lamontana.propuesta_reprogramacion WHERE id_operacion=? UNION ALL SELECT p.id_pedido,d.id_actor,d.huella FROM lamontana.resolucion_reprogramacion d JOIN lamontana.propuesta_reprogramacion p USING(id_propuesta_reprogramacion) WHERE d.id_operacion=?",(r,n)->new Object[]{r.getLong(1),r.getLong(2),r.getString(3)},op,op);if(rows.isEmpty())return false;var r=rows.get(0);if(rows.size()!=1||(long)r[0]!=p.numero()||(long)r[1]!=a.id()||!hash.equals(r[2]))throw conflicto("La operación ya fue utilizada con otros datos.");return true;}
 private Actor actor(String correo){return jdbc.queryForObject("SELECT u.id_usuario,u.nombre||' '||u.apellido,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.correo=? AND u.estado='ACTIVO' AND r.activo",(r,n)->new Actor(r.getLong(1),r.getString(2),r.getString(3)),correo);}
 private void exigirEtapa(PedidoService.Detalle p){if(!ESTADOS.contains(p.estado())||!p.estadoReserva().equals("ACTIVA"))throw conflicto("La reprogramación requiere una reserva activa y un pedido sin corrección pendiente, cancelación, entrega ni cierre.");}
 private void pendiente(Registro r){if(!r.estado().equals("PENDIENTE"))throw conflicto("La propuesta ya fue resuelta. Consultá el historial.");}
 private void version(PedidoService.Detalle p,long v){if(p.version()!=v)throw conflicto("El pedido cambió. Actualizá sus datos antes de continuar.");}
 private void confirmar(boolean c){if(!c)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirmá la decisión antes de guardarla.");}
 private void texto(String t){if(t==null||t.isBlank()||t.length()>500||t.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Completá el motivo o mensaje sin caracteres de control.");}
 private void bloquear(){for(long id:new long[]{764003,764001,764002,764004})jdbc.execute("SELECT pg_advisory_xact_lock("+id+")");}
 private String huella(Object in){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(in).getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private ResponseStatusException conflicto(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
