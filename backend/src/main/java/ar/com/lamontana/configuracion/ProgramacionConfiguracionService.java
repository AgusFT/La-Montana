package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.ProgramacionConfiguracionController.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProgramacionConfiguracionService {
    private static final String PROPOSITO="PROGRAMAR_CONFIGURACION";
    private final JdbcTemplate jdbc;
    private final ConfiguracionService configuracion;
    private final ActivacionConfiguracionService activacion;
    private final SeguridadConfiguracion seguridad;
    public ProgramacionConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion,ActivacionConfiguracionService activacion,SeguridadConfiguracion seguridad){this.jdbc=jdbc;this.configuracion=configuracion;this.activacion=activacion;this.seguridad=seguridad;}
    private record Contenido(String proposito,UUID destino,Decision decision){}
    @Transactional
    public SeguridadConfiguracion.Solicitud solicitar(UUID destino,Solicitar i,String actor){
        bloquear();return seguridad.solicitar(i.operacion(),PROPOSITO,destino,i.decision().revision().version(),huella(destino,i.decision()),i.contrasena(),actor,()->validar(destino,i.decision(),actor),"PROGRAMACION_SOLICITADA","Programar configuración de La Montaña");
    }
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public ConfiguracionService.Programacion confirmar(UUID destino,Confirmar i,String actor){
        bloquear();var permiso=seguridad.comprobar(i.operacion(),PROPOSITO,destino,i.decision().revision().version(),huella(destino,i.decision()),i.contrasena(),i.codigo(),actor);
        if(permiso.repetido())return configuracion.programacion(destino);
        Instant prevista=validar(destino,i.decision(),actor);var d=permiso.desafio();var ahora=jdbc.queryForObject("SELECT clock_timestamp()",Timestamp.class);
        if(!prevista.isAfter(ahora.toInstant()))throw error("La fecha prevista ya pasó. Volvé a elegirla y autorizá nuevamente.");
        seguridad.consumir(permiso);
        jdbc.update("INSERT INTO lamontana.programacion_configuracion(id_configuracion_version,id_actor,id_operacion,fecha_confirmacion,fecha_programada,zona_horaria,motivo,id_catalogo_revision) VALUES (?,?,?,?,?,?,?,(SELECT id_catalogo_revision FROM lamontana.catalogo_revision WHERE codigo_publico=?))",d.idConfiguracion(),d.actor(),i.operacion(),ahora,Timestamp.from(prevista),i.decision().zonaHoraria(),i.decision().revision().motivo().strip(),i.decision().revision().revisionComercial());
        jdbc.update("UPDATE lamontana.configuracion_version SET estado='PROGRAMADA',version=version+1,fecha_actualizacion=? WHERE id_configuracion_version=?",ahora,d.idConfiguracion());
        configuracion.registrar(i.operacion(),PROPOSITO,d.idConfiguracion(),d.actor(),d.huella(),"CONFIGURACION_PROGRAMADA",d.version()+1,null,i.decision().revision().motivo().strip());
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",d.idConfiguracion());
        return configuracion.programacion(destino);
    }
    @Transactional public void revocar(UUID destino,UUID operacion,String actor){seguridad.revocar(operacion,destino,actor);}
    private void bloquear(){configuracion.bloquear();jdbc.execute("SELECT pg_advisory_xact_lock(764001)");jdbc.execute("SELECT pg_advisory_xact_lock(764002)");}
    private Instant validar(UUID destino,Decision d,String actor){
        activacion.validar(destino,d.revision(),actor);
        if(!ZoneId.getAvailableZoneIds().contains(d.zonaHoraria()))throw error("Ingresá una zona horaria IANA válida.");
        var offsets=ZoneId.of(d.zonaHoraria()).getRules().getValidOffsets(d.fechaLocal());
        if(offsets.size()!=1)throw error("La hora elegida es inexistente o ambigua por un cambio de horario. Elegí otra hora.");
        Instant instante=d.fechaLocal().toInstant(offsets.get(0));
        if(!instante.isAfter(jdbc.queryForObject("SELECT clock_timestamp()",Timestamp.class).toInstant()))throw error("La fecha de activación debe ser futura.");
        return instante;
    }
    private String huella(UUID destino,Decision d){return configuracion.huella(new Contenido(PROPOSITO,destino,d));}
    private ResponseStatusException error(String mensaje){return new ResponseStatusException(HttpStatus.CONFLICT,mensaje);}
}
