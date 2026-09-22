package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.ActivacionConfiguracionController.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivacionConfiguracionService {
    private static final String ACTIVAR="ACTIVAR_CONFIGURACION",ADELANTAR="ADELANTAR_CONFIGURACION";
    private final JdbcTemplate jdbc;
    private final ConfiguracionService configuracion;
    private final RevisionConfiguracionService revision;
    private final SeguridadConfiguracion seguridad;
    private final PublicacionConfiguracion publicacion;
    private final CoordinadorActivacion coordinador;
    private final TransactionTemplate tx;
    public ActivacionConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion,RevisionConfiguracionService revision,SeguridadConfiguracion seguridad,PublicacionConfiguracion publicacion,CoordinadorActivacion coordinador,PlatformTransactionManager manager){
        this.jdbc=jdbc;this.configuracion=configuracion;this.revision=revision;this.seguridad=seguridad;this.publicacion=publicacion;this.coordinador=coordinador;tx=new TransactionTemplate(manager);tx.setTimeout(30);
    }
    public record Resultado(String estado,ConfiguracionService.Intento intento,ConfiguracionService.Version version){}
    private record Contenido(String proposito,UUID destino,Decision decision){}
    private record Inicio(UUID intento,boolean repetido,SeguridadConfiguracion.Desafio desafio,ResponseStatusException rechazo){}
    @Transactional public SeguridadConfiguracion.Solicitud solicitar(UUID destino,Solicitar i,String actor){return solicitar(destino,i,actor,false);}
    @Transactional public SeguridadConfiguracion.Solicitud solicitarProgramada(UUID destino,Solicitar i,String actor){return solicitar(destino,i,actor,true);}
    private SeguridadConfiguracion.Solicitud solicitar(UUID destino,Solicitar i,String actor,boolean programada){
        bloquear();String proposito=proposito(programada);
        return seguridad.solicitar(i.operacion(),proposito,destino,i.decision().version(),huella(proposito,destino,i.decision()),i.contrasena(),actor,()->{configuracion.sinIntentoEnCurso();validar(destino,i.decision(),actor,programada);},"ACTIVACION_SOLICITADA",programada?"Activar ahora una configuración programada":"Activar configuración de La Montaña");
    }
    public Resultado confirmar(UUID destino,Confirmar i,String actor){return confirmar(destino,i,actor,false);}
    public Resultado confirmarProgramada(UUID destino,Confirmar i,String actor){return confirmar(destino,i,actor,true);}
    private Resultado confirmar(UUID destino,Confirmar i,String actor,boolean programada){
        // La exclusión comienza antes de tomar locks transaccionales; una confirmación concurrente espera y recupera su resultado.
        return coordinador.exclusivo(true,()->{
            Inicio inicio=tx.execute(status->{
                configuracion.bloquear();coordinador.recuperar();
                try{
                    String proposito=proposito(programada);
                    var p=seguridad.comprobar(i.operacion(),proposito,destino,i.decision().version(),huella(proposito,destino,i.decision()),i.contrasena(),i.codigo(),actor);
                    if(p.repetido())return new Inicio(null,true,p.desafio(),null);
                    var b=configuracion.cargar(destino);if(!b.estado().equals(programada?"PROGRAMADA":"EN_PREPARACION")||b.version()!=i.decision().version())throw conflicto("La configuración cambió. Consultá su estado y autorizá una nueva operación.");
                    var d=p.desafio();UUID intento=UUID.randomUUID();
                    // El comprobante representa el comando aceptado, incluso si su publicación termina fallando.
                    seguridad.consumir(p);
                    configuracion.registrar(i.operacion(),proposito,d.idConfiguracion(),d.actor(),d.huella(),"INTENTO_MANUAL_INICIADO",d.version(),intento,i.decision().motivo().strip());
                    jdbc.update("""
                        INSERT INTO lamontana.intento_activacion_configuracion(id_intento,id_version_objetivo,id_version_anterior,id_usuario_actor,id_operacion,origen,estado,fecha_inicio)
                        VALUES (?,?,(SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE estado='ACTIVA'),?,?,'MANUAL','INICIADO',clock_timestamp())
                        """,intento,d.idConfiguracion(),d.actor(),i.operacion());
                    return new Inicio(intento,false,d,null);
                }catch(ResponseStatusException ex){return new Inicio(null,false,null,ex);} // Conserva intentos de credencial fallidos; no acepta el comando.
            });
            if(inicio.rechazo()!=null)throw inicio.rechazo();
            if(!inicio.repetido()){
                try{tx.executeWithoutResult(status->publicar(destino,i,actor,programada,inicio));}
                catch(RuntimeException ex){
                    String detalle=ex instanceof ResponseStatusException r&&r.getReason()!=null?r.getReason():"No se pudo publicar la configuración. La anterior conserva su vigencia.";
                    String mensaje=detalle.substring(0,Math.min(800,detalle.length()))+(programada?" La programación se reintentará automáticamente en diez minutos; también podés cancelarla o autorizar otro intento manual.":" Revisá el borrador y autorizá una nueva operación para volver a intentar.");
                    tx.executeWithoutResult(status->{configuracion.bloquear();jdbc.update("UPDATE lamontana.intento_activacion_configuracion SET estado='FALLIDO',fecha_fin=clock_timestamp(),codigo_resultado=?,detalle_sanitizado=? WHERE id_intento=? AND estado='INICIADO'",ex instanceof ResponseStatusException?"VALIDACION_RECHAZADA":"ERROR_PUBLICACION",mensaje,inicio.intento());});
                }
            }
            return tx.execute(status->resultado(destino,i.operacion()));
        });
    }
    private void publicar(UUID destino,Confirmar i,String actor,boolean programada,Inicio inicio){
        bloquear();
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.intento_activacion_configuracion WHERE id_intento=? AND estado='INICIADO')",Boolean.class,inicio.intento())))return;
        seguridad.exigirVigente(i.operacion());validar(destino,i.decision(),actor,programada);var d=inicio.desafio();
        publicacion.publicar(d.idConfiguracion(),d.actor(),i.operacion(),i.decision().revisionComercial(),i.decision().motivo().strip());
        jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version,codigo_recurso,motivo) VALUES (?,?,'CONFIGURACION_ACTIVADA',?,?,?)",d.idConfiguracion(),d.actor(),d.version()+1,inicio.intento(),i.decision().motivo().strip());
        jdbc.update("UPDATE lamontana.intento_activacion_configuracion SET estado='EXITOSO',id_version_resultante=?,fecha_fin=clock_timestamp(),codigo_resultado='CONFIGURACION_PUBLICADA',detalle_sanitizado='La configuración se publicó completamente.' WHERE id_intento=? AND estado='INICIADO'",d.idConfiguracion(),inicio.intento());
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",d.idConfiguracion());
    }
    private Resultado resultado(UUID destino,UUID operacion){
        var intento=configuracion.intentoPorOperacion(operacion);
        // Las activaciones previas a V19 conservan su recibo real; no se inventan fechas de inicio de intentos históricos.
        if(intento==null){boolean anterior=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.activacion_configuracion WHERE id_operacion=?)",Boolean.class,operacion));if(!anterior)throw new IllegalStateException("Comando de activación sin resultado registrado.");return new Resultado("EXITOSO",null,configuracion.versionActivada(destino));}
        return new Resultado(intento.estado(),intento,intento.estado().equals("EXITOSO")?configuracion.versionActivada(destino):null);
    }
    @Transactional public void revocar(UUID destino,UUID op,String actor){seguridad.revocar(op,destino,actor);}
    private void bloquear(){configuracion.bloquear();jdbc.execute("SELECT pg_advisory_xact_lock(764001)");jdbc.execute("SELECT pg_advisory_xact_lock(764002)");}
    void validar(UUID destino,Decision i,String actor){configuracion.sinIntentoEnCurso();validar(destino,i,actor,false);}
    private void validar(UUID destino,Decision i,String actor,boolean programada){
        var r=revision.evaluar(destino,actor,programada);
        if(!r.borrador().estado().equals(programada?"PROGRAMADA":"EN_PREPARACION")||r.borrador().version()!=i.version())throw conflicto("La configuración cambió. Volvé a revisar antes de autorizar.");
        configuracion.exigirRevisionCopia(r.borrador(),r.huella(),programada);
        if(r.bloqueos()>0)throw conflicto("Hay bloqueos en la revisión integral. Corregilos antes de activar.");
        if(!r.huella().equals(i.huellaRevision())||r.catalogo().actual()==null||!r.catalogo().actual().codigoPublico().equals(i.revisionComercial()))throw conflicto("La configuración o su referencia comercial cambió. Volvé a revisar las condiciones actuales.");
        if(!Boolean.TRUE.equals(i.advertenciasRevisadas()))throw conflicto("Confirmá la revisión de las advertencias antes de activar.");
    }
    private String proposito(boolean programada){return programada?ADELANTAR:ACTIVAR;}
    private String huella(String proposito,UUID destino,Decision d){return configuracion.huella(new Contenido(proposito,destino,d));}
    private ResponseStatusException conflicto(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
