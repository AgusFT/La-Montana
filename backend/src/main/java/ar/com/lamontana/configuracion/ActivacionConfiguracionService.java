package ar.com.lamontana.configuracion;
import static ar.com.lamontana.configuracion.ActivacionConfiguracionController.*;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ActivacionConfiguracionService {
 private static final String PROPOSITO="ACTIVAR_CONFIGURACION";
 private final JdbcTemplate jdbc;private final ConfiguracionService configuracion;private final RevisionConfiguracionService revision;private final SeguridadConfiguracion seguridad;
 public ActivacionConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion,RevisionConfiguracionService revision,SeguridadConfiguracion seguridad){this.jdbc=jdbc;this.configuracion=configuracion;this.revision=revision;this.seguridad=seguridad;}
 private record Contenido(String proposito,UUID destino,Decision decision){}
 @Transactional
 public SeguridadConfiguracion.Solicitud solicitar(UUID destino,Solicitar i,String actor){bloquear();return seguridad.solicitar(i.operacion(),PROPOSITO,destino,i.decision().version(),huella(destino,i.decision()),i.contrasena(),actor,()->validar(destino,i.decision(),actor),"ACTIVACION_SOLICITADA","Activar configuración de La Montaña");}
 @Transactional(noRollbackFor=ResponseStatusException.class)
 public ConfiguracionService.Version confirmar(UUID destino,Confirmar i,String actor){
  bloquear();var p=seguridad.comprobar(i.operacion(),PROPOSITO,destino,i.decision().version(),huella(destino,i.decision()),i.contrasena(),i.codigo(),actor);if(p.repetido())return configuracion.versionActivada(destino);
  validar(destino,i.decision(),actor);var d=p.desafio();seguridad.consumir(p);publicar(d.idConfiguracion(),d.actor(),i.operacion(),i.decision().revisionComercial(),i.decision().motivo().strip());
  configuracion.registrar(i.operacion(),PROPOSITO,d.idConfiguracion(),d.actor(),d.huella(),"CONFIGURACION_ACTIVADA",i.decision().version()+1,null,i.decision().motivo().strip());
  jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",d.idConfiguracion());return configuracion.versionActivada(destino);
 }
 void publicar(long id,Long actorId,UUID operacion,UUID comercial,String motivo){var ahora=jdbc.queryForObject("SELECT clock_timestamp()",Timestamp.class);var anteriores=jdbc.query("SELECT id_configuracion_version,version FROM lamontana.configuracion_version WHERE estado='ACTIVA'",(r,n)->new long[]{r.getLong(1),r.getLong(2)});Long anterior=anteriores.isEmpty()?null:anteriores.get(0)[0];
  // Desde aquí no hay rechazos de negocio: cualquier error de almacenamiento revierte toda la transición.
  if(anterior!=null){jdbc.update("UPDATE lamontana.configuracion_version SET estado='HISTORICA',fecha_actualizacion=? WHERE id_configuracion_version=?",ahora,anterior);jdbc.update("UPDATE lamontana.activacion_configuracion SET fin_vigencia=? WHERE id_configuracion_version=?",ahora,anterior);jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version,motivo) VALUES (?,?,'CONFIGURACION_HISTORICA',?,'Reemplazada por nueva configuración activa')",anterior,actorId,anteriores.get(0)[1]);}
  jdbc.update("UPDATE lamontana.configuracion_version SET estado='ACTIVA',version=version+1,fecha_actualizacion=? WHERE id_configuracion_version=?",ahora,id);
  jdbc.update("INSERT INTO lamontana.activacion_configuracion(id_configuracion_version,id_predecesora,id_catalogo_revision,id_actor,id_operacion,motivo,fecha_activacion) VALUES (?,?,(SELECT id_catalogo_revision FROM lamontana.catalogo_revision WHERE codigo_publico=?),?,?,?,?)",id,anterior,comercial,actorId,operacion,motivo,ahora);
 }
 @Transactional public void revocar(UUID destino,UUID op,String actor){seguridad.revocar(op,destino,actor);}
 private void bloquear(){configuracion.bloquear();jdbc.execute("SELECT pg_advisory_xact_lock(764001)");jdbc.execute("SELECT pg_advisory_xact_lock(764002)");}
 void validar(UUID destino,Decision i,String actor){var r=revision.revisar(destino,actor);if(!r.borrador().estado().equals("EN_PREPARACION")||r.borrador().version()!=i.version())throw conflicto("El borrador cambió. Volvé a la fase 6 antes de autorizar.");if(r.bloqueos()>0)throw conflicto("Hay bloqueos en la revisión integral. Corregilos antes de activar.");if(!r.huella().equals(i.huellaRevision())||r.catalogo().actual()==null||!r.catalogo().actual().codigoPublico().equals(i.revisionComercial()))throw conflicto("La configuración o su referencia comercial cambió. Volvé a revisar la fase 6.");if(!Boolean.TRUE.equals(i.advertenciasRevisadas()))throw conflicto("Confirmá la revisión de las advertencias antes de activar.");}
 private String huella(UUID destino,Decision d){return configuracion.huella(new Contenido(PROPOSITO,destino,d));}
 private ResponseStatusException conflicto(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
