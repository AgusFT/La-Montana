//#region ENCABEZADO · CopiaConfiguracionService.java
/*
 * ========================================================================
 * ARCHIVO: CopiaConfiguracionService.java
 * ========================================================================
 * FUNCIÓN
 * Crea borradores a partir de configuraciones activas o históricas, registra su origen y exige
 * reconfirmar los parámetros copiados antes de aplicarlos.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] CopiaConfiguracionService(JdbcTemplate jdbc, ConfiguracionService configuracion,
 *   RevisionConfiguracionService revision, CopiaParametrosConfiguracion parametros)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] ConfiguracionService.Borrador copiar(UUID origen, Crear in, String correo)
 * - [public] RevisionConfiguracionService.Revision confirmar(UUID codigo, ConfirmarRevision in,
 *   String correo)
 * - [private] void bloquear()
 *   Toma el bloqueo transaccional de PostgreSQL.
 * - [private] ResponseStatusException conflicto(String m)
 *   Construye un error HTTP controlado.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - CopiaConfiguracionService (clase).
 * - CopiaConfiguracionService.Contenido (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;
import static ar.com.lamontana.configuracion.CopiaConfiguracionController.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
@Service
public class CopiaConfiguracionService {
 private final JdbcTemplate jdbc;private final ConfiguracionService configuracion;private final RevisionConfiguracionService revision;private final CopiaParametrosConfiguracion parametros;
 private final JsonMapper json=JsonMapper.builder().build();
 public CopiaConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion,RevisionConfiguracionService revision,CopiaParametrosConfiguracion parametros){this.jdbc=jdbc;this.configuracion=configuracion;this.revision=revision;this.parametros=parametros;}
 private record Contenido(UUID origen,Crear solicitud){}
 @Transactional public ConfiguracionService.Borrador copiar(UUID origen,Crear in,String correo){
  bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(new Contenido(origen,in));var repetido=configuracion.reintento(in.operacion(),"USAR_COMO_BASE",null,actor,huella);if(repetido!=null)return repetido;
  var original=configuracion.cargar(origen);if(!Set.of("ACTIVA","HISTORICA").contains(original.estado()))throw conflicto("Sólo una configuración activa o histórica puede usarse como base.");
  if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE estado IN ('EN_PREPARACION','PROGRAMADA'))",Boolean.class)))throw conflicto("Ya hay un cambio pendiente. Continuá o cancelá el borrador; una programación debe cancelarse mediante su autorización antes de crear otra copia.");
  UUID codigo=UUID.randomUUID();long id=jdbc.queryForObject("INSERT INTO lamontana.configuracion_version(codigo_publico,numero_version,id_usuario_creador,estado,modelo,criterio,version) VALUES (?,(SELECT coalesce(max(numero_version),0)+1 FROM lamontana.configuracion_version),?,'EN_PREPARACION',?,?,2) RETURNING id_configuracion_version",Long.class,codigo,actor,original.modelo().name(),original.criterio()==null?null:original.criterio().name());
  long anterior=jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?",Long.class,origen);
  jdbc.update("INSERT INTO lamontana.origen_configuracion(id_configuracion_version,id_version_origen) VALUES (?,?)",id,anterior);
  parametros.copiar(anterior,id);
  var barrido=revision.evaluar(codigo,correo,false).hallazgos().stream().filter(h->!h.codigo().startsWith("RECONFIRMAR_FASE_")).toList();
  jdbc.update("UPDATE lamontana.origen_configuracion SET barrido_inicial=?::jsonb WHERE id_configuracion_version=?",json.writeValueAsString(barrido),id);
  configuracion.registrar(in.operacion(),"USAR_COMO_BASE",id,actor,huella,"BORRADOR_DESDE_BASE",2,origen,"USO_COMO_BASE de V"+original.numero());
  return configuracion.cargar(codigo);
 }
 @Transactional public RevisionConfiguracionService.Revision confirmar(UUID codigo,ConfirmarRevision in,String correo){
  bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(in);var repetido=configuracion.reintento(in.operacion(),"RECONFIRMAR_REVISION",codigo,actor,huella);
  var r=revision.evaluar(codigo,correo,false);var b=r.borrador();
  if(repetido!=null){if(!b.estado().equals("EN_PREPARACION"))throw conflicto("La confirmación ya se registró y el borrador cambió de estado. Consultá la configuración actual.");configuracion.exigirRevisionCopia(b,r.huella(),false);return r;}
  if(b.copia()==null||!b.estado().equals("EN_PREPARACION"))throw conflicto("Esta confirmación corresponde a un borrador creado desde otra versión.");
  if(b.version()!=in.version()||!r.huella().equals(in.huellaRevision())||r.catalogo().actual()==null||!r.catalogo().actual().codigoPublico().equals(in.revisionComercial()))throw conflicto("La copia o sus condiciones cambiaron. Actualizá la revisión antes de confirmar.");
  if(r.bloqueos()>0||!in.advertenciasRevisadas())throw conflicto("Revisá las fases pendientes, corregí los bloqueos y reconocé las advertencias antes de confirmar.");
  long id=jdbc.queryForObject("UPDATE lamontana.configuracion_version SET version=version+1,fecha_actualizacion=clock_timestamp() WHERE codigo_publico=? RETURNING id_configuracion_version",Long.class,codigo);
  jdbc.update("INSERT INTO lamontana.reconfirmacion_configuracion(id_configuracion_version,fase,id_actor,version) VALUES (?,6,?,?) ON CONFLICT(id_configuracion_version,fase) DO UPDATE SET id_actor=excluded.id_actor,version=excluded.version,fecha=clock_timestamp(),huella_revision=NULL",id,actor,b.version()+1);
  configuracion.registrar(in.operacion(),"RECONFIRMAR_REVISION",id,actor,huella,"REVISION_RECONFIRMADA",b.version()+1,null,"Fase 6 revisada después de usar otra versión como base");
  jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",id);
  var actual=revision.evaluar(codigo,correo,false);jdbc.update("UPDATE lamontana.reconfirmacion_configuracion SET huella_revision=? WHERE id_configuracion_version=? AND fase=6",actual.huella(),id);return actual;
 }
 private void bloquear(){configuracion.bloquear();jdbc.execute("SELECT pg_advisory_xact_lock(764001)");jdbc.execute("SELECT pg_advisory_xact_lock(764002)");}
 private ResponseStatusException conflicto(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
