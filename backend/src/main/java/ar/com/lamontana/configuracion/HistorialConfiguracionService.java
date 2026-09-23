//#region ENCABEZADO · HistorialConfiguracionService.java
/*
 * ========================================================================
 * ARCHIVO: HistorialConfiguracionService.java
 * ========================================================================
 * FUNCIÓN
 * Consulta y compara configuraciones guardadas y su auditoría, con filtros, paginación y control
 * de acceso. Reconstruye referencias y diferencias sin modificar las versiones consultadas.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] HistorialConfiguracionService(JdbcTemplate jdbc, ConfiguracionService configuracion)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [private] void permiso(String correo)
 * - [private] void pagina(int n)
 * - [public] Listado listar(String buscar, String estado, String orden, int pagina, String correo)
 * - [private] Fila fila(ResultSet r, int n) throws SQLException
 * - [private] Fila fila(UUID id)
 * - [public] Detalle detalle(UUID id, String correo)
 * - [private] Detalle leerDetalle(UUID id, String correo)
 * - [private] Base base(Fila f, String correo)
 * - [private] Actor actor(String tabla, UUID id)
 * - [private] Referencia referencia(String sql, Object[] args)
 * - [private] Modulo modulo(String codigo, String titulo, Object valores)
 * - [private] Map<String, String> referencias()
 * - [public] Comparacion comparar(UUID origen, UUID destino, String correo)
 * - [private] String canonico(JsonNode n)
 * - [public] Pagina<Evento> auditoria(UUID id, int pagina, String correo)
 * - [private] Instant fecha(ResultSet r, String campo) throws SQLException
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - HistorialConfiguracionService (clase).
 * - HistorialConfiguracionService.Actor (record).
 * - HistorialConfiguracionService.Referencia (record).
 * - HistorialConfiguracionService.Fila (record).
 * - HistorialConfiguracionService.Pagina (record).
 * - HistorialConfiguracionService.Listado (record).
 * - HistorialConfiguracionService.Modulo (record).
 * - HistorialConfiguracionService.Base (record).
 * - HistorialConfiguracionService.Detalle (record).
 * - HistorialConfiguracionService.Evento (record).
 * - HistorialConfiguracionService.Seccion (record).
 * - HistorialConfiguracionService.Comparacion (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class HistorialConfiguracionService {
 private final JdbcTemplate jdbc; private final ConfiguracionService configuracion;
 private final JsonMapper json=JsonMapper.builder().build();
 private static final int TAMANO=25;
 public HistorialConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion){this.jdbc=jdbc;this.configuracion=configuracion;}
 public record Actor(String nombre,String rol,boolean capturado){}
 public record Referencia(UUID codigo,long numero){}
 public record Fila(UUID codigo,long numero,String estado,String modelo,String criterio,Instant creadaEn,Instant activadaEn,Instant finVigencia,Instant confirmadaEn,Instant previstaEn,String zonaHoraria,Instant canceladaEn,Actor autor,String motivo){}
 public record Pagina<T>(List<T> elementos,long total,int pagina,int tamano){}
 public record Listado(Pagina<Fila> versiones,Map<String,Long> cantidades,Referencia activa){}
 public record Modulo(String codigo,String titulo,JsonNode valores){}
 public record Base(boolean permitida,String motivo){}
 public record Detalle(Fila version,List<Modulo> modulos,Referencia predecesora,Referencia reemplazadaPor,Referencia revisionComercial,Actor activador,Actor programador,String motivoProgramacion,Map<String,String> referencias,ConfiguracionService.Copia copia,Base base){}
 public record Evento(String clave,Instant fecha,String tipo,String descripcion,Actor actor,Long edicion,String recurso,String estadoAnterior,String estadoNuevo,String origen,Instant inicio,Instant fin,Instant atraso){}
 public record Seccion(String codigo,String titulo,boolean modificada,JsonNode origen,JsonNode destino){}
 public record Comparacion(Fila origen,Fila destino,List<Seccion> secciones,long modificadas,Referencia comercialOrigen,Referencia comercialDestino,Map<String,String> referencias,Base base){}
 private static final String FILA="""
 SELECT c.*,coalesce(c.nombre_actor,u.nombre||' '||u.apellido) AS autor_nombre,c.rol_actor AS autor_rol,c.nombre_actor IS NOT NULL AS capturado,
 a.fecha_activacion,a.fin_vigencia,p.fecha_confirmacion,p.fecha_programada,p.zona_horaria,
 coalesce(c.motivo_cancelacion,a.motivo,p.motivo,'Sin motivo registrado') AS motivo
 FROM lamontana.configuracion_version c JOIN lamontana.usuario u ON u.id_usuario=c.id_usuario_creador
 LEFT JOIN lamontana.activacion_configuracion a USING(id_configuracion_version)
 LEFT JOIN lamontana.programacion_configuracion p USING(id_configuracion_version)
 """;
 private void permiso(String correo){if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.correo=? AND u.estado='ACTIVO' AND r.codigo='ADMIN_ADMIN' AND r.activo)",Boolean.class,correo)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La consulta requiere un administrador activo.");}
 private void pagina(int n){if(n<0||n>1000000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La página solicitada no es válida.");}
 public Listado listar(String buscar,String estado,String orden,int pagina,String correo){
  permiso(correo);pagina(pagina);buscar=buscar.strip();if(!buscar.matches("[vV]?[0-9]{0,18}")||!Set.of("TODOS","ACTIVA","PROGRAMADA","HISTORICA","CANCELADA").contains(estado)||!Set.of("RECIENTES","ANTIGUAS").contains(orden))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revisá la búsqueda, el estado y el orden del historial.");
  String filtro=" WHERE c.estado<>'EN_PREPARACION' AND (?='TODOS' OR c.estado=?) AND c.numero_version::text LIKE ?";Object[] args={estado,estado,"%"+buscar.replaceFirst("^[vV]","")+"%"};
  long total=jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_version c"+filtro,Long.class,args);
  var filas=jdbc.query(FILA+filtro+" ORDER BY c.numero_version "+(orden.equals("RECIENTES")?"DESC":"ASC")+" LIMIT ? OFFSET ?",(r,n)->fila(r,n),args[0],args[1],args[2],TAMANO,(long)pagina*TAMANO);
  Map<String,Long> cantidades=new LinkedHashMap<>();jdbc.query("SELECT estado,count(*) AS cantidad FROM lamontana.configuracion_version WHERE estado<>'EN_PREPARACION' GROUP BY estado",r->{cantidades.put(r.getString(1),r.getLong(2));});
  return new Listado(new Pagina<>(filas,total,pagina,TAMANO),cantidades,referencia("SELECT codigo_publico,numero_version FROM lamontana.configuracion_version WHERE estado='ACTIVA'"));
 }
 private Fila fila(ResultSet r,int n)throws SQLException{return new Fila(r.getObject("codigo_publico",UUID.class),r.getLong("numero_version"),r.getString("estado"),r.getString("modelo"),r.getString("criterio"),fecha(r,"fecha_creacion"),fecha(r,"fecha_activacion"),fecha(r,"fin_vigencia"),fecha(r,"fecha_confirmacion"),fecha(r,"fecha_programada"),r.getString("zona_horaria"),fecha(r,"fecha_cancelacion"),new Actor(r.getString("autor_nombre"),r.getString("autor_rol"),r.getBoolean("capturado")),r.getString("motivo"));}
 private Fila fila(UUID id){var rows=jdbc.query(FILA+" WHERE c.codigo_publico=? AND c.estado<>'EN_PREPARACION'",(r,n)->fila(r,n),id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"La versión cerrada no existe. Los borradores se consultan desde la configuración actual.");return rows.get(0);}
 public Detalle detalle(UUID id,String correo){permiso(correo);return leerDetalle(id,correo);}
 private Detalle leerDetalle(UUID id,String correo){
  var f=fila(id);var b=configuracion.cargar(id);var e=b.entrega();var modelo=new LinkedHashMap<String,Object>();modelo.put("modelo",b.modelo());modelo.put("criterio",b.criterio());var horarios=new LinkedHashMap<String,Object>();horarios.put("preparacionHoras",e.preparacionHoras());horarios.put("trasladoHoras",e.trasladoHoras());horarios.put("horariosPorSucursal",e.horariosPorSucursal());
  var entrega=Map.of("modalidades",e.modalidades(),"puntos",e.puntos(),"zonas",e.zonas());
  List<Modulo> modulos=List.of(modulo("modelo","Modelo operativo y aprobación",modelo),modulo("pagos","Pagos y reglas de seña",b.pagos()),modulo("recursos","Impresoras y asignación",b.recursos()),modulo("horarios","Horarios y tiempos",horarios),modulo("entrega","Entrega, puntos y zonas",entrega));
  var previa=referencia("SELECT v.codigo_publico,v.numero_version FROM lamontana.activacion_configuracion a JOIN lamontana.configuracion_version c USING(id_configuracion_version) JOIN lamontana.configuracion_version v ON v.id_configuracion_version=a.id_predecesora WHERE c.codigo_publico=?",id);
  var siguiente=referencia("SELECT v.codigo_publico,v.numero_version FROM lamontana.activacion_configuracion a JOIN lamontana.configuracion_version v USING(id_configuracion_version) JOIN lamontana.configuracion_version c ON c.id_configuracion_version=a.id_predecesora WHERE c.codigo_publico=?",id);
  var comercial=referencia("SELECT r.codigo_publico,r.id_catalogo_revision FROM lamontana.configuracion_version c LEFT JOIN lamontana.activacion_configuracion a USING(id_configuracion_version) LEFT JOIN lamontana.programacion_configuracion p USING(id_configuracion_version) JOIN lamontana.catalogo_revision r ON r.id_catalogo_revision=coalesce(a.id_catalogo_revision,p.id_catalogo_revision) WHERE c.codigo_publico=?",id);
  var motivos=jdbc.query("SELECT p.motivo FROM lamontana.programacion_configuracion p JOIN lamontana.configuracion_version c USING(id_configuracion_version) WHERE c.codigo_publico=?",(r,n)->r.getString(1),id);
  return new Detalle(f,modulos,previa,siguiente,comercial,actor("activacion_configuracion",id),actor("programacion_configuracion",id),motivos.isEmpty()?null:motivos.get(0),referencias(),b.copia(),base(f,correo));
 }
 private Base base(Fila f,String correo){
  if(!Set.of("ACTIVA","HISTORICA").contains(f.estado()))return new Base(false,"Sólo las versiones activas o históricas pueden usarse como base.");
  if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT es_administrador_propietario FROM lamontana.usuario WHERE correo=?",Boolean.class,correo)))return new Base(false,"La preparación de configuraciones requiere al propietario de la imprenta.");
  if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE estado IN ('EN_PREPARACION','PROGRAMADA'))",Boolean.class)))return new Base(false,"Ya hay un cambio pendiente. Continuá o cancelá el borrador; una programación requiere cancelación autorizada.");
  if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.intento_activacion_configuracion WHERE estado='INICIADO')",Boolean.class)))return new Base(false,"Hay una activación en curso. Consultá su resultado antes de continuar.");
  return new Base(true,"Se creará un nuevo borrador y deberás revisar nuevamente las fases 2 a 6.");
 }
 private Actor actor(String tabla,UUID id){var rows=jdbc.query("SELECT coalesce(x.nombre_actor,u.nombre||' '||u.apellido,'Sistema automático'),x.rol_actor,x.nombre_actor IS NOT NULL FROM lamontana."+tabla+" x JOIN lamontana.configuracion_version c USING(id_configuracion_version) LEFT JOIN lamontana.usuario u ON u.id_usuario=x.id_actor WHERE c.codigo_publico=?",(r,n)->new Actor(r.getString(1),r.getString(2),r.getBoolean(3)),id);return rows.isEmpty()?null:rows.get(0);}
 private Referencia referencia(String sql,Object...args){var rows=jdbc.query(sql,(r,n)->new Referencia(r.getObject(1,UUID.class),r.getLong(2)),args);return rows.isEmpty()?null:rows.get(0);}
 private Modulo modulo(String codigo,String titulo,Object valores){return new Modulo(codigo,titulo,json.valueToTree(valores));}
 private Map<String,String> referencias(){Map<String,String> refs=new LinkedHashMap<>();for(String tabla:List.of("sucursal","servicio","formato"))jdbc.query("SELECT codigo_publico::text,codigo||' · '||nombre FROM lamontana."+tabla,r->{refs.put(r.getString(1),r.getString(2));});return refs;}
 public Comparacion comparar(UUID origen,UUID destino,String correo){permiso(correo);var a=leerDetalle(origen,correo);var b=leerDetalle(destino,correo);var secciones=new ArrayList<Seccion>();for(int n=0;n<a.modulos().size();n++){var x=a.modulos().get(n);var y=b.modulos().get(n);secciones.add(new Seccion(x.codigo(),x.titulo(),!canonico(x.valores()).equals(canonico(y.valores())),x.valores(),y.valores()));}return new Comparacion(a.version(),b.version(),secciones,secciones.stream().filter(Seccion::modificada).count(),a.revisionComercial(),b.revisionComercial(),a.referencias(),a.base());}
 // Listas representan conjuntos (días, recursos, referencias y franjas), no prioridades de ejecución.
 private String canonico(JsonNode n){if(n==null||n.isNull())return "null";if(n.isArray()){var parts=new ArrayList<String>();for(var v:n)parts.add(canonico(v));Collections.sort(parts);return "["+String.join(",",parts)+"]";}if(n.isObject()){var parts=new TreeMap<String,String>();n.properties().forEach(p->parts.put(p.getKey(),canonico(p.getValue())));return json.writeValueAsString(parts);}return n.toString();}
 private static final String AUDITORIA="""
 SELECT 'evento-'||e.id_evento_configuracion AS clave,e.fecha,e.tipo,e.motivo AS descripcion,
 coalesce(e.nombre_actor,u.nombre||' '||u.apellido,'Sistema automático') AS actor,e.rol_actor,e.nombre_actor IS NOT NULL AS capturado,
 e.version AS edicion,e.codigo_recurso::text AS recurso,e.estado_anterior,e.estado_nuevo,
 NULL::text AS origen,NULL::timestamptz AS inicio,NULL::timestamptz AS fin,NULL::timestamptz AS atraso
 FROM lamontana.evento_configuracion e LEFT JOIN lamontana.usuario u ON u.id_usuario=e.id_actor JOIN lamontana.configuracion_version c USING(id_configuracion_version) WHERE c.codigo_publico=?
 UNION ALL
 SELECT 'intento-'||i.id_intento,coalesce(i.fecha_fin,i.fecha_inicio),CASE WHEN i.operacion='ROLLBACK' THEN 'ROLLBACK_' ELSE 'INTENTO_' END||i.estado,i.detalle_sanitizado,
 coalesce(i.nombre_actor,u.nombre||' '||u.apellido,'Sistema automático'),i.rol_actor,i.nombre_actor IS NOT NULL,
 NULL::bigint,i.id_intento::text,NULL::text,NULL::text,i.origen,i.fecha_inicio,i.fecha_fin,i.fecha_atraso_detectado
 FROM lamontana.intento_activacion_configuracion i LEFT JOIN lamontana.usuario u ON u.id_usuario=i.id_usuario_actor JOIN lamontana.configuracion_version c ON c.id_configuracion_version=i.id_version_objetivo OR (i.operacion='ROLLBACK' AND c.id_configuracion_version IN (i.id_version_anterior,i.id_version_resultante)) WHERE c.codigo_publico=?
 """;
 public Pagina<Evento> auditoria(UUID id,int pagina,String correo){permiso(correo);pagina(pagina);fila(id);long total=jdbc.queryForObject("SELECT count(*) FROM ("+AUDITORIA+") eventos",Long.class,id,id);var items=jdbc.query("SELECT * FROM ("+AUDITORIA+") eventos ORDER BY fecha,clave LIMIT ? OFFSET ?",(r,n)->new Evento(r.getString("clave"),fecha(r,"fecha"),r.getString("tipo"),r.getString("descripcion"),new Actor(r.getString("actor"),r.getString("rol_actor"),r.getBoolean("capturado")),r.getObject("edicion",Long.class),r.getString("recurso"),r.getString("estado_anterior"),r.getString("estado_nuevo"),r.getString("origen"),fecha(r,"inicio"),fecha(r,"fin"),fecha(r,"atraso")),id,id,TAMANO,(long)pagina*TAMANO);return new Pagina<>(items,total,pagina,TAMANO);}
 private Instant fecha(ResultSet r,String campo)throws SQLException{var t=r.getTimestamp(campo);return t==null?null:t.toInstant();}
}
