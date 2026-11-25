//#region ENCABEZADO · CancelacionConfiguracionService.java
/*
 * ========================================================================
 * ARCHIVO: CancelacionConfiguracionService.java
 * ========================================================================
 * FUNCIÓN
 * Gestiona la autorización y confirmación de la cancelación de un borrador o una configuración
 * programada, conservando el resultado de reintentos y la trazabilidad.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] CancelacionConfiguracionService(JdbcTemplate jdbc, ConfiguracionService
 *   configuracion, SeguridadConfiguracion seguridad)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] SeguridadConfiguracion.Solicitud solicitar(UUID destino, SolicitarCancelacion i,
 *   String actor)
 * - [public] SeguridadConfiguracion.Solicitud solicitarProgramacion(UUID destino,
 *   SolicitarCancelacion i, String actor)
 * - [private] SeguridadConfiguracion.Solicitud solicitar(UUID destino, SolicitarCancelacion i,
 *   String actor, boolean programada)
 * - [public] ConfiguracionService.Borrador confirmar(UUID destino, ConfirmarCancelacion i, String
 *   actor)
 * - [public] ConfiguracionService.Borrador confirmarProgramacion(UUID destino,
 *   ConfirmarCancelacion i, String actor)
 * - [private] ConfiguracionService.Borrador confirmar(UUID destino, ConfirmarCancelacion i, String
 *   actor, boolean programada)
 * - [public] void revocar(UUID destino, UUID operacion, String actor)
 * - [private] String huella(String proposito, UUID destino, long version, String motivo)
 * - [private] void snapshot(UUID destino, long version, boolean programada)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - CancelacionConfiguracionService (clase).
 * - CancelacionConfiguracionService.Contenido (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;
import static ar.com.lamontana.configuracion.ConfiguracionController.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class CancelacionConfiguracionService {
 private static final String PROPOSITO="CANCELAR_BORRADOR";
 private final JdbcTemplate jdbc;private final ConfiguracionService configuracion;private final SeguridadConfiguracion seguridad;
 public CancelacionConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion,SeguridadConfiguracion seguridad){this.jdbc=jdbc;this.configuracion=configuracion;this.seguridad=seguridad;}
 private record Contenido(String proposito,UUID destino,long version,String motivo){}
 @Transactional
 public SeguridadConfiguracion.Solicitud solicitar(UUID destino,SolicitarCancelacion i,String actor){return solicitar(destino,i,actor,false);}
 @Transactional public SeguridadConfiguracion.Solicitud solicitarProgramacion(UUID destino,SolicitarCancelacion i,String actor){return solicitar(destino,i,actor,true);}
 private SeguridadConfiguracion.Solicitud solicitar(UUID destino,SolicitarCancelacion i,String actor,boolean programada){String proposito=programada?"CANCELAR_PROGRAMACION":PROPOSITO;return seguridad.solicitar(i.operacion(),proposito,destino,i.version(),huella(proposito,destino,i.version(),i.motivo()),i.contrasena(),actor,()->snapshot(destino,i.version(),programada),"CANCELACION_SOLICITADA",programada?"Cancelar programación de configuración":"Cancelar borrador de configuración");}
 @Transactional(noRollbackFor=ResponseStatusException.class)
 public ConfiguracionService.Borrador confirmar(UUID destino,ConfirmarCancelacion i,String actor){return confirmar(destino,i,actor,false);}
 @Transactional(noRollbackFor=ResponseStatusException.class) public ConfiguracionService.Borrador confirmarProgramacion(UUID destino,ConfirmarCancelacion i,String actor){return confirmar(destino,i,actor,true);}
 private ConfiguracionService.Borrador confirmar(UUID destino,ConfirmarCancelacion i,String actor,boolean programada){String proposito=programada?"CANCELAR_PROGRAMACION":PROPOSITO;
  var p=seguridad.comprobar(i.operacion(),proposito,destino,i.version(),huella(proposito,destino,i.version(),i.motivo()),i.contrasena(),i.codigo(),actor);if(p.repetido())return configuracion.cargar(destino);snapshot(destino,i.version(),programada);var d=p.desafio();seguridad.consumir(p);
  jdbc.update("UPDATE lamontana.configuracion_version SET estado='CANCELADA',version=version+1,fecha_actualizacion=clock_timestamp(),id_usuario_cancelador=?,fecha_cancelacion=clock_timestamp(),motivo_cancelacion=? WHERE id_configuracion_version=?",d.actor(),i.motivo().strip(),d.idConfiguracion());
  configuracion.registrar(i.operacion(),proposito,d.idConfiguracion(),d.actor(),d.huella(),programada?"PROGRAMACION_CANCELADA":"BORRADOR_CANCELADO",i.version()+1);
  jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",d.idConfiguracion());return configuracion.cargar(destino);
 }
 @Transactional public void revocar(UUID destino,UUID operacion,String actor){seguridad.revocar(operacion,destino,actor);}
 private String huella(String proposito,UUID destino,long version,String motivo){return configuracion.huella(new Contenido(proposito,destino,version,motivo));}
 private void snapshot(UUID destino,long version,boolean programada){if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.intento_activacion_configuracion WHERE estado='INICIADO')",Boolean.class)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Hay una activación en curso. Consultá su resultado antes de cancelar.");var b=configuracion.cargar(destino);if(!b.estado().equals(programada?"PROGRAMADA":"EN_PREPARACION")||b.version()!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"El borrador cambió desde la solicitud. Revisá su versión actual antes de cancelar.");}
}
