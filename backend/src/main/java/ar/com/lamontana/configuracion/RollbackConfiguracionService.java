//#region ENCABEZADO · RollbackConfiguracionService.java
/*
 * ========================================================================
 * ARCHIVO: RollbackConfiguracionService.java
 * ========================================================================
 * FUNCIÓN
 * Evalúa y autoriza la recuperación de una configuración anterior bajo condiciones actuales.
 * Coordina intentos, publicación de una nueva versión y resultados de reintentos.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] RollbackConfiguracionService(JdbcTemplate jdbc, ConfiguracionService configuracion,
 *   RevisionConfiguracionService revision, SeguridadConfiguracion seguridad, CoordinadorActivacion
 *   coordinador, PublicacionRollback publicacion, PlatformTransactionManager manager)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] Revision revisar(String correo)
 * - [private] Revision evaluar(String correo, UUID propio)
 * - [public] SeguridadConfiguracion.Solicitud solicitar(Solicitar in, String correo)
 * - [public] Resultado confirmar(Confirmar in, String correo)
 * - [private] void publicar(Confirmar in, String correo, Inicio inicio)
 * - [private] Resultado resultado(UUID operacion)
 * - [private] Revision validar(Decision d, String correo, UUID propio)
 * - [private] void estructura(Decision d)
 * - [public] void revocar(UUID objetivo, UUID operacion, String correo)
 * - [private] void bloquear()
 *   Toma el bloqueo transaccional de PostgreSQL.
 * - [private] String huella(Decision d)
 * - [private] ResponseStatusException conflicto(String mensaje)
 *   Construye un error HTTP controlado.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - RollbackConfiguracionService (clase).
 * - RollbackConfiguracionService.Revision (record).
 * - RollbackConfiguracionService.Resultado (record).
 * - RollbackConfiguracionService.Referencia (record).
 * - RollbackConfiguracionService.Contenido (record).
 * - RollbackConfiguracionService.Inicio (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.RollbackConfiguracionController.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RollbackConfiguracionService {
    private static final String PROPOSITO="ROLLBACK_CONFIGURACION";
    private final JdbcTemplate jdbc;
    private final ConfiguracionService configuracion;
    private final RevisionConfiguracionService revision;
    private final SeguridadConfiguracion seguridad;
    private final CoordinadorActivacion coordinador;
    private final PublicacionRollback publicacion;
    private final TransactionTemplate tx;
    public RollbackConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion,RevisionConfiguracionService revision,SeguridadConfiguracion seguridad,CoordinadorActivacion coordinador,PublicacionRollback publicacion,PlatformTransactionManager manager){this.jdbc=jdbc;this.configuracion=configuracion;this.revision=revision;this.seguridad=seguridad;this.coordinador=coordinador;this.publicacion=publicacion;tx=new TransactionTemplate(manager);tx.setTimeout(30);}
    public record Revision(ConfiguracionService.Version activa,ConfiguracionService.Version objetivo,ConfiguracionService.Programacion programada,
        RevisionConfiguracionService.Revision condiciones,List<String> impedimentos,boolean permitida,String huella){}
    public record Resultado(String estado,ConfiguracionService.Intento intento,ConfiguracionService.Version version,ConfiguracionService.Borrador programacionCancelada){}
    private record Referencia(ConfiguracionService.Version activa,ConfiguracionService.Version objetivo,ConfiguracionService.Programacion programada,String condiciones,List<String> impedimentos){}
    private record Contenido(String proposito,Decision decision){}
    private record Inicio(UUID intento,boolean repetido,SeguridadConfiguracion.Desafio desafio,ResponseStatusException rechazo){}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Revision revisar(String correo){return evaluar(correo,null);}
    private Revision evaluar(String correo,UUID propio){
        configuracion.propietario(correo);var activa=configuracion.activa();var objetivo=activa==null||activa.predecesora()==null?null:configuracion.versionActivada(activa.predecesora());var programada=configuracion.programada();var impedimentos=new ArrayList<String>();
        if(objetivo==null)impedimentos.add("No hay una predecesora efectiva que recuperar. Se necesitan al menos dos activaciones exitosas.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE estado='EN_PREPARACION')",Boolean.class)))impedimentos.add("Cancelá el borrador abierto antes de iniciar la reversión.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.intento_activacion_configuracion WHERE estado='INICIADO' AND (?::uuid IS NULL OR id_intento<>?))",Boolean.class,propio,propio)))impedimentos.add("Hay una publicación en curso. Consultá su resultado antes de continuar.");
        var condiciones=objetivo==null?null:revision.evaluarHistorica(objetivo.configuracion().codigoPublico(),correo);
        var cerradas=List.copyOf(impedimentos);String huella=configuracion.huella(new Referencia(activa,objetivo,programada,condiciones==null?null:condiciones.huella(),cerradas));
        return new Revision(activa,objetivo,programada,condiciones,cerradas,cerradas.isEmpty()&&condiciones!=null&&condiciones.bloqueos()==0,huella);
    }
    @Transactional public SeguridadConfiguracion.Solicitud solicitar(Solicitar in,String correo){
        bloquear();var d=in.decision();
        return seguridad.solicitar(in.operacion(),PROPOSITO,d.objetivo(),d.versionObjetivo(),huella(d),in.contrasena(),correo,()->validar(d,correo,null),"ROLLBACK_SOLICITADO","Revertir configuración de La Montaña");
    }
    public Resultado confirmar(Confirmar in,String correo){
        return coordinador.exclusivo(true,()->{
            Inicio inicio=tx.execute(status->{
                configuracion.bloquear();coordinador.recuperar();var d=in.decision();
                try{
                    var permiso=seguridad.comprobar(in.operacion(),PROPOSITO,d.objetivo(),d.versionObjetivo(),huella(d),in.contrasena(),in.codigo(),correo);
                    if(permiso.repetido())return new Inicio(null,true,permiso.desafio(),null);
                    estructura(d);var desafio=permiso.desafio();UUID intento=UUID.randomUUID();seguridad.consumir(permiso);
                    configuracion.registrar(in.operacion(),PROPOSITO,desafio.idConfiguracion(),desafio.actor(),desafio.huella(),"ROLLBACK_INICIADO",desafio.version(),intento,d.motivo().strip());
                    jdbc.update("""
                        INSERT INTO lamontana.intento_activacion_configuracion(id_intento,id_version_objetivo,id_version_anterior,id_usuario_actor,id_operacion,operacion,origen,estado,fecha_inicio,id_programacion_anterior)
                        VALUES (?,?,(SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?),?,?,'ROLLBACK','MANUAL','INICIADO',clock_timestamp(),(SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?))
                        """,intento,desafio.idConfiguracion(),d.activa(),desafio.actor(),in.operacion(),d.programada());
                    return new Inicio(intento,false,desafio,null);
                }catch(ResponseStatusException ex){return new Inicio(null,false,null,ex);}
            });
            if(inicio.rechazo()!=null)throw inicio.rechazo();
            if(!inicio.repetido()){
                try{tx.executeWithoutResult(status->publicar(in,correo,inicio));}
                catch(RuntimeException ex){
                    String detalle=ex instanceof ResponseStatusException r&&r.getReason()!=null?r.getReason():"No se pudo publicar la reversión.";
                    String mensaje=detalle.substring(0,Math.min(700,detalle.length()))+" La configuración activa y la programación se conservan. Revisá las condiciones y autorizá otra operación; este comando no se repetirá automáticamente.";
                    tx.executeWithoutResult(status->{configuracion.bloquear();jdbc.update("UPDATE lamontana.intento_activacion_configuracion SET estado='FALLIDO',fecha_fin=clock_timestamp(),codigo_resultado=?,detalle_sanitizado=? WHERE id_intento=? AND estado='INICIADO'",ex instanceof ResponseStatusException?"VALIDACION_RECHAZADA":"ERROR_PUBLICACION",mensaje,inicio.intento());});
                }
            }
            return tx.execute(status->resultado(in.operacion()));
        });
    }
    private void publicar(Confirmar in,String correo,Inicio inicio){
        bloquear();if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.intento_activacion_configuracion WHERE id_intento=? AND estado='INICIADO')",Boolean.class,inicio.intento())))return;
        seguridad.exigirVigente(in.operacion());var actual=validar(in.decision(),correo,inicio.intento());var d=inicio.desafio();
        long nueva=publicacion.publicar(d.idConfiguracion(),d.actor(),in.operacion(),inicio.intento(),actual,in.decision().motivo().strip());
        jdbc.update("UPDATE lamontana.intento_activacion_configuracion SET estado='EXITOSO',id_version_resultante=?,fecha_fin=clock_timestamp(),codigo_resultado='CONFIGURACION_RECUPERADA',detalle_sanitizado='Se publicó una nueva versión con el contenido de la predecesora efectiva.' WHERE id_intento=? AND estado='INICIADO'",nueva,inicio.intento());
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",d.idConfiguracion());
    }
    private Resultado resultado(UUID operacion){
        var intento=configuracion.intentoPorOperacion(operacion);if(intento==null||!intento.operacion().equals("ROLLBACK"))throw new IllegalStateException("Comando de reversión sin resultado registrado.");
        var canceladas=jdbc.query("SELECT c.codigo_publico FROM lamontana.intento_activacion_configuracion i JOIN lamontana.configuracion_version c ON c.id_configuracion_version=i.id_programacion_anterior WHERE i.id_operacion=? AND i.estado='EXITOSO'",(r,n)->r.getObject(1,UUID.class),operacion);
        return new Resultado(intento.estado(),intento,intento.resultante()==null?null:configuracion.versionActivada(intento.resultante()),canceladas.isEmpty()?null:configuracion.cargar(canceladas.get(0)));
    }
    private Revision validar(Decision d,String correo,UUID propio){
        var actual=evaluar(correo,propio);estructura(d);
        if(!actual.permitida())throw conflicto(actual.impedimentos().isEmpty()?"Las condiciones actuales bloquean la recuperación. Corregilas o usá la histórica como base de un nuevo borrador.":String.join(" ",actual.impedimentos()));
        if(!actual.huella().equals(d.huellaRevision())||!actual.condiciones().catalogo().actual().codigoPublico().equals(d.revisionComercial()))throw conflicto("La configuración, programación o condiciones cambiaron. Actualizá la revisión y autorizá nuevamente.");
        if(!Boolean.TRUE.equals(d.advertenciasRevisadas())||Boolean.TRUE.equals(d.cancelarProgramacion())!=(actual.programada()!=null))throw conflicto("Confirmá las condiciones y, si corresponde, la cancelación de la programación pendiente.");
        return actual;
    }
    private void estructura(Decision d){
        var activa=configuracion.activa();var programada=configuracion.programada();
        if(activa==null||!activa.configuracion().codigoPublico().equals(d.activa())||!Objects.equals(activa.predecesora(),d.objetivo())||!Objects.equals(programada==null?null:programada.configuracion().codigoPublico(),d.programada()))throw conflicto("Cambió la configuración activa o la programación revisada. Consultá de nuevo antes de autorizar.");
        var b=configuracion.cargar(d.objetivo());if(!b.estado().equals("HISTORICA")||b.version()!=d.versionObjetivo())throw conflicto("La predecesora efectiva cambió. Actualizá la revisión.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE estado='EN_PREPARACION')",Boolean.class)))throw conflicto("Cancelá el borrador abierto antes de iniciar la reversión.");
    }
    @Transactional public void revocar(UUID objetivo,UUID operacion,String correo){seguridad.revocar(operacion,objetivo,correo);}
    private void bloquear(){configuracion.bloquear();jdbc.execute("SELECT pg_advisory_xact_lock(764001)");jdbc.execute("SELECT pg_advisory_xact_lock(764002)");}
    private String huella(Decision d){return configuracion.huella(new Contenido(PROPOSITO,d));}
    private ResponseStatusException conflicto(String mensaje){return new ResponseStatusException(HttpStatus.CONFLICT,mensaje);}
}
