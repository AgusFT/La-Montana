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
 public SeguridadConfiguracion.Solicitud solicitar(UUID destino,SolicitarCancelacion i,String actor){return seguridad.solicitar(i.operacion(),PROPOSITO,destino,i.version(),huella(destino,i.version(),i.motivo()),i.contrasena(),actor,()->snapshot(destino,i.version()),"CANCELACION_SOLICITADA","Cancelar borrador de configuración");}
 @Transactional(noRollbackFor=ResponseStatusException.class)
 public ConfiguracionService.Borrador confirmar(UUID destino,ConfirmarCancelacion i,String actor){
  var p=seguridad.comprobar(i.operacion(),PROPOSITO,destino,i.version(),huella(destino,i.version(),i.motivo()),i.contrasena(),i.codigo(),actor);if(p.repetido())return configuracion.cargar(destino);snapshot(destino,i.version());var d=p.desafio();seguridad.consumir(p);
  jdbc.update("UPDATE lamontana.configuracion_version SET estado='CANCELADA',version=version+1,fecha_actualizacion=clock_timestamp(),id_usuario_cancelador=?,fecha_cancelacion=clock_timestamp(),motivo_cancelacion=? WHERE id_configuracion_version=?",d.actor(),i.motivo().strip(),d.idConfiguracion());
  configuracion.registrar(i.operacion(),PROPOSITO,d.idConfiguracion(),d.actor(),d.huella(),"BORRADOR_CANCELADO",i.version()+1);
  jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",d.idConfiguracion());return configuracion.cargar(destino);
 }
 private String huella(UUID destino,long version,String motivo){return configuracion.huella(new Contenido(PROPOSITO,destino,version,motivo));}
 private void snapshot(UUID destino,long version){var b=configuracion.cargar(destino);if(!b.estado().equals("EN_PREPARACION")||b.version()!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"El borrador cambió desde la solicitud. Revisá su versión actual antes de cancelar.");}
}
