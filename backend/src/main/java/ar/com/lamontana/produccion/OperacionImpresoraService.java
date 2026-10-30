package ar.com.lamontana.produccion;

import ar.com.lamontana.organizacion.OrganizacionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** Manual operational facts, global by printer identity. No sensor or CUPS state is invented. */
@Service
public class OperacionImpresoraService {
 private final JdbcTemplate jdbc;private final OrganizacionService org;
 private final JsonMapper json=JsonMapper.builder().build();
 public OperacionImpresoraService(JdbcTemplate jdbc,OrganizacionService org){this.jdbc=jdbc;this.org=org;}
 public record Estado(UUID impresora,boolean habilitada,Integer hojas,long version,Instant actualizadaEn,String actor){}
 public record Formato(UUID codigoPublico,String nombre){}
 public record Equipo(UUID codigoPublico,String nombre,String ubicacion,List<Formato> formatos,boolean color,boolean dobleFaz,int capacidadHojas,String estadoConfigurado,boolean retirada,Estado operacion){}
 public record Panel(boolean puedeGestionar,List<Equipo> impresoras){}
 public record Evento(UUID operacion,String accion,String actor,String rol,Instant fecha,boolean habilitadaAnterior,boolean habilitadaNueva,Integer hojasAnteriores,Integer hojasNuevas,Integer cantidad,String motivo){}
 public record Historial(List<Evento> eventos,long total,int pagina){}
 private record Actor(long id,String nombre,String rol){}
 private record Recurso(long id,String estado,boolean retirada){}
 private void leerPermitido(String correo){var permissions=org.contexto(correo).permisos();if(!permissions.contains("GESTIONAR_IMPRESORAS")&&!permissions.contains("GESTIONAR_PRODUCCION"))throw error(HttpStatus.FORBIDDEN,"Se requiere acceso a las impresoras.");}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Panel listar(String correo){
  leerPermitido(correo);
  var equipos=jdbc.query("""
   SELECT DISTINCT ON (i.id_impresora) i.id_impresora,i.codigo_publico,c.id_configuracion_version,c.nombre,c.admite_color,c.admite_doble_faz,c.capacidad_maxima_hojas,c.estado,c.retirada_en,s.nombre AS ubicacion
   FROM lamontana.impresora i JOIN lamontana.configuracion_impresora c USING(id_impresora)
   JOIN lamontana.configuracion_version v USING(id_configuracion_version)
   LEFT JOIN lamontana.sucursal s ON s.id_sucursal=c.id_sucursal_ubicacion
   ORDER BY i.id_impresora,(v.estado='ACTIVA') DESC,v.numero_version DESC
   """,(r,n)->new Equipo(r.getObject("codigo_publico",UUID.class),r.getString("nombre"),r.getString("ubicacion"),formatos(r.getLong("id_configuracion_version"),r.getLong("id_impresora")),r.getBoolean("admite_color"),r.getBoolean("admite_doble_faz"),r.getInt("capacidad_maxima_hojas"),r.getString("estado"),r.getTimestamp("retirada_en")!=null,estado(r.getObject("codigo_publico",UUID.class))));
  return new Panel(org.contexto(correo).permisos().contains("GESTIONAR_IMPRESORAS"),equipos.stream().sorted(Comparator.comparing(Equipo::nombre).thenComparing(Equipo::codigoPublico)).toList());
 }
 public List<Formato> formatos(long version,long impresora){return jdbc.query("SELECT f.codigo_publico,f.nombre FROM lamontana.configuracion_impresora_formato cf JOIN lamontana.formato f USING(id_formato) WHERE cf.id_configuracion_version=? AND cf.id_impresora=? ORDER BY f.nombre",(r,n)->new Formato(r.getObject(1,UUID.class),r.getString(2)),version,impresora);}
 public List<Formato> formatosPorCodigo(List<UUID> codigos){var result=new ArrayList<Formato>();for(UUID id:codigos)result.addAll(jdbc.query("SELECT codigo_publico,nombre FROM lamontana.formato WHERE codigo_publico=?",(r,n)->new Formato(r.getObject(1,UUID.class),r.getString(2)),id));return List.copyOf(result);}
 public Estado estado(UUID codigo){
  var rows=jdbc.query("SELECT o.*,u.nombre||' '||u.apellido AS actor FROM lamontana.operacion_impresora o JOIN lamontana.impresora i USING(id_impresora) JOIN lamontana.usuario u ON u.id_usuario=o.id_actor WHERE i.codigo_publico=?",(r,n)->new Estado(codigo,r.getBoolean("habilitada"),r.getObject("hojas_registradas",Integer.class),r.getLong("version"),r.getTimestamp("actualizada_en").toInstant(),r.getString("actor")),codigo);
  return rows.isEmpty()?new Estado(codigo,true,null,0,null,null):rows.get(0);
 }
 @Transactional public Estado cambiar(UUID codigo,OperacionImpresoraController.Cambio in,String correo){
  bloquear();org.exigirPermiso(correo,"GESTIONAR_IMPRESORAS");var actor=actor(correo);var resource=recurso(codigo);var hash=huella(Arrays.asList(codigo,in));
  if(recuperar(in.operacion(),resource.id(),actor,hash))return estado(codigo);
  if(resource.retirada())throw conflicto("El equipo fue retirado de la configuración consultada.");
  var old=estado(codigo);if(old.version()!=in.version())throw conflicto("El estado o el papel cambió. Actualizá la impresora antes de continuar.");
  if(in.accion()==null)throw error(HttpStatus.BAD_REQUEST,"Elegí una acción.");
  boolean enabled=old.habilitada();Integer sheets=old.hojas();String action=in.accion().name();
  switch(in.accion()){
   case HABILITAR->{if(!resource.estado().equals("OPERATIVA"))throw conflicto("El motor no declara este equipo operativo. Revisá sus capacidades antes de habilitarlo.");if(enabled)throw conflicto("La impresora ya estaba habilitada.");enabled=true;}
   case DESHABILITAR->{if(!enabled)throw conflicto("La impresora ya estaba deshabilitada.");enabled=false;}
   case CONTAR->{validarCantidad(in.hojas(),true);sheets=in.hojas();}
   case RECARGAR->{validarCantidad(in.hojas(),false);if(sheets==null)throw conflicto("Primero informá cuántas hojas hay cargadas; no se conoce el saldo anterior.");try{sheets=Math.addExact(sheets,in.hojas());}catch(ArithmeticException e){throw error(HttpStatus.BAD_REQUEST,"La cantidad de hojas es demasiado grande.");}}
  }
  String note=in.observaciones()==null?"":in.observaciones().strip();if(note.length()>500||note.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw error(HttpStatus.BAD_REQUEST,"Observación inválida.");
  guardar(resource.id(),old,enabled,sheets,actor,in.operacion(),hash,action,in.hojas(),null,note.isEmpty()?switch(in.accion()){case HABILITAR->"Equipo habilitado para nuevos trabajos.";case DESHABILITAR->"Equipo deshabilitado para nuevos trabajos; los trabajos en curso se conservan.";case CONTAR->"Conteo de papel informado por el responsable.";case RECARGAR->"Recarga de papel informada por el responsable.";}:note);
  return estado(codigo);
 }
 /** Called inside the production transaction, after its replay/authorization checks. */
 @Transactional public void asignar(UUID codigo,int hojas,long trabajo,UUID operacion,String correo){
  bloquear();org.exigirPermiso(correo,"GESTIONAR_PRODUCCION");var resource=recurso(codigo);var old=estado(codigo);
  UUID op=UUID.nameUUIDFromBytes((operacion+":papel").getBytes(StandardCharsets.UTF_8));var actor=actor(correo);String hash=huella(List.of(codigo,trabajo,hojas));
  if(recuperar(op,resource.id(),actor,hash))return;
  if(hojas<1)throw error(HttpStatus.BAD_REQUEST,"El trabajo necesita una cantidad positiva de hojas.");
  if(!old.habilitada()||resource.retirada())throw conflicto("La impresora fue deshabilitada o retirada. Elegí otro equipo.");
  if(old.hojas()!=null&&old.hojas()<hojas)throw conflicto("El papel registrado no alcanza. Registrá una recarga o corregí el conteo.");
  guardar(resource.id(),old,old.habilitada(),old.hojas()==null?null:old.hojas()-hojas,actor,op,hash,"ASIGNAR_TRABAJO",hojas,trabajo,"Hojas previstas asignadas al iniciar el trabajo manual. Saldo estimado; no es una lectura física.");
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Historial historial(UUID codigo,int pagina,String correo){
  leerPermitido(correo);long id=recurso(codigo).id();if(pagina<0||pagina>100000)throw error(HttpStatus.BAD_REQUEST,"Página inválida.");
  var events=jdbc.query("SELECT * FROM lamontana.evento_operacion_impresora WHERE id_impresora=? ORDER BY version_nueva DESC LIMIT 25 OFFSET ?",(r,n)->new Evento(r.getObject("id_operacion",UUID.class),r.getString("accion"),r.getString("actor_nombre"),r.getString("actor_rol"),r.getTimestamp("fecha").toInstant(),r.getBoolean("habilitada_anterior"),r.getBoolean("habilitada_nueva"),r.getObject("hojas_anteriores",Integer.class),r.getObject("hojas_nuevas",Integer.class),r.getObject("cantidad",Integer.class),r.getString("motivo")),id,(long)pagina*25);
  return new Historial(events,jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_operacion_impresora WHERE id_impresora=?",Long.class,id),pagina);
 }
 private void guardar(long id,Estado old,boolean enabled,Integer hojas,Actor actor,UUID op,String hash,String action,Integer cantidad,Long trabajo,String note){
  jdbc.update("INSERT INTO lamontana.operacion_impresora(id_impresora,habilitada,hojas_registradas,version,id_actor) VALUES (?,?,?,?,?) ON CONFLICT(id_impresora) DO UPDATE SET habilitada=excluded.habilitada,hojas_registradas=excluded.hojas_registradas,version=excluded.version,id_actor=excluded.id_actor,actualizada_en=clock_timestamp()",id,enabled,hojas,old.version()+1,actor.id());
  jdbc.update("INSERT INTO lamontana.evento_operacion_impresora(id_operacion,id_impresora,id_actor,actor_nombre,actor_rol,accion,huella,habilitada_anterior,habilitada_nueva,hojas_anteriores,hojas_nuevas,cantidad,version_anterior,version_nueva,id_trabajo_impresion,motivo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",op,id,actor.id(),actor.nombre(),actor.rol(),action,hash,old.habilitada(),enabled,old.hojas(),hojas,cantidad,old.version(),old.version()+1,trabajo,note);
 }
 private boolean recuperar(UUID op,long id,Actor actor,String hash){var old=jdbc.query("SELECT id_impresora,id_actor,huella FROM lamontana.evento_operacion_impresora WHERE id_operacion=?",(r,n)->new Object[]{r.getLong(1),r.getLong(2),r.getString(3)},op);if(old.isEmpty())return false;var e=old.get(0);if((long)e[0]!=id||(long)e[1]!=actor.id()||!hash.equals(e[2]))throw conflicto("La operación ya se utilizó con otros datos.");return true;}
 private Recurso recurso(UUID codigo){var rows=jdbc.query("SELECT i.id_impresora,c.estado,c.retirada_en FROM lamontana.impresora i JOIN lamontana.configuracion_impresora c USING(id_impresora) JOIN lamontana.configuracion_version v USING(id_configuracion_version) WHERE i.codigo_publico=? ORDER BY (v.estado='ACTIVA') DESC,v.numero_version DESC LIMIT 1",(r,n)->new Recurso(r.getLong(1),r.getString(2),r.getTimestamp(3)!=null),codigo);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"La impresora no existe.");return rows.get(0);}
 private Actor actor(String correo){return jdbc.queryForObject("SELECT u.id_usuario,u.nombre||' '||u.apellido,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.activo",(r,n)->new Actor(r.getLong(1),r.getString(2),r.getString(3)),correo);}
 private void validarCantidad(Integer v,boolean cero){if(v==null||v<(cero?0:1)||v>1000000)throw error(HttpStatus.BAD_REQUEST,"Indicá una cantidad de hojas válida.");}
 private void bloquear(){for(long id:new long[]{764003,764001,764002,764004})jdbc.execute("SELECT pg_advisory_xact_lock("+id+")");}
 private String huella(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private ResponseStatusException conflicto(String s){return error(HttpStatus.CONFLICT,s);}private ResponseStatusException error(HttpStatus status,String s){return new ResponseStatusException(status,s);}
}
