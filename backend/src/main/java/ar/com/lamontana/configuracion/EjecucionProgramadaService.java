//#region ENCABEZADO · EjecucionProgramadaService.java
/*
 * ========================================================================
 * ARCHIVO: EjecucionProgramadaService.java
 * ========================================================================
 * FUNCIÓN
 * Reconcilia las activaciones operativas programadas: registra el intento, revalida condiciones
 * actuales y publica o conserva el resultado del fallo y la política de reintento.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] EjecucionProgramadaService(CoordinadorActivacion coordinador, JdbcTemplate jdbc,
 *   ConfiguracionService configuracion, RevisionConfiguracionService revision,
 *   PublicacionConfiguracion activacion, CatalogoService catalogo, PlatformTransactionManager
 *   manager)
 * - [paquete] CondicionesInvalidas :: CondicionesInvalidas(String mensaje)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] void reconciliar(boolean recuperando)
 * - [private] UUID iniciar(boolean recuperando)
 * - [private] void publicar(UUID intento)
 * - [private] Instant ahora()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - EjecucionProgramadaService (clase).
 * - EjecucionProgramadaService.Pendiente (record).
 * - EjecucionProgramadaService.Candidata (record).
 * - EjecucionProgramadaService.AutorizacionVencida (clase).
 * - EjecucionProgramadaService.CondicionesInvalidas (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import ar.com.lamontana.catalogo.CatalogoService;
import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Una conexión dedicada conserva la exclusión entre el inicio durable y la publicación. */
@Service
public class EjecucionProgramadaService {
    private final CoordinadorActivacion coordinador;
    private final JdbcTemplate jdbc;
    private final ConfiguracionService configuracion;
    private final RevisionConfiguracionService revision;
    private final PublicacionConfiguracion activacion;
    private final CatalogoService catalogo;
    private final TransactionTemplate tx;
    public EjecucionProgramadaService(CoordinadorActivacion coordinador,JdbcTemplate jdbc,ConfiguracionService configuracion,RevisionConfiguracionService revision,PublicacionConfiguracion activacion,CatalogoService catalogo,PlatformTransactionManager manager){
        this.coordinador=coordinador;this.jdbc=jdbc;this.configuracion=configuracion;this.revision=revision;this.activacion=activacion;this.catalogo=catalogo;tx=new TransactionTemplate(manager);tx.setTimeout(30);
    }
    public void reconciliar(boolean recuperando){
        coordinador.exclusivo(false,()->{
            UUID intento=tx.execute(status->iniciar(recuperando));
            if(intento==null)return null;
            try{
                catalogo.reconciliarProgramaciones();
                tx.executeWithoutResult(status->publicar(intento));
            }catch(RuntimeException ex){
                String codigo=ex instanceof AutorizacionVencida?"AUTORIZACION_NO_VIGENTE":ex instanceof CondicionesInvalidas?"CONDICIONES_NO_VALIDAS":"ERROR_PUBLICACION";
                String detalle=ex instanceof CondicionesInvalidas c?c.getMessage():ex instanceof AutorizacionVencida?"El usuario que autorizó programar ya no conserva la autorización vigente. Cancelá la programación y autorizá una nueva.":"No se pudo publicar la configuración. La anterior conserva su vigencia; se reintentará en diez minutos.";
                tx.executeWithoutResult(status->{configuracion.bloquear();jdbc.update("UPDATE lamontana.intento_activacion_configuracion SET estado='FALLIDO',fecha_fin=clock_timestamp(),codigo_resultado=?,detalle_sanitizado=? WHERE id_intento=? AND estado='INICIADO'",codigo,detalle,intento);});
            }
            return null;
        });
    }
    private UUID iniciar(boolean recuperando){
        configuracion.bloquear();
        // Con el lock de sesión, cualquier inicio que siga abierto pertenece a una ejecución interrumpida.
        int interrumpidos=coordinador.recuperar();
        var pendientes=jdbc.query("""
            SELECT c.id_configuracion_version,c.codigo_publico,p.fecha_programada FROM lamontana.configuracion_version c
            JOIN lamontana.programacion_configuracion p USING(id_configuracion_version) WHERE c.estado='PROGRAMADA'
            """,(r,n)->new Pendiente(r.getLong(1),r.getObject(2,UUID.class),r.getTimestamp(3).toInstant()));
        if(pendientes.isEmpty())return null;
        var p=pendientes.get(0);var vista=configuracion.programacion(p.codigo());Instant ahora=ahora(),proxima=vista.proximoIntentoEn();
        if(proxima==null||proxima.isAfter(ahora))return null;
        // Un reintento de adelanto puede vencer antes de la fecha original: todavía no hay atraso de esa fecha.
        boolean recuperar=(recuperando||interrumpidos>0)&&!p.prevista().isAfter(ahora);
        String origen=recuperar?"RECUPERACION":vista.intentos().stream().noneMatch(i->!i.iniciadoEn().isBefore(vista.confirmadaEn()))?"PROGRAMACION":"REINTENTO";
        UUID id=UUID.randomUUID();
        jdbc.update("""
            INSERT INTO lamontana.intento_activacion_configuracion(id_intento,id_version_objetivo,id_version_anterior,origen,estado,fecha_inicio,fecha_atraso_detectado)
            VALUES (?,?,(SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE estado='ACTIVA'),?,'INICIADO',?,?)
            """,id,p.id(),origen,Timestamp.from(ahora),recuperar?Timestamp.from(ahora):null);
        return id;
    }
    private record Pendiente(long id,UUID codigo,Instant prevista){}
    private record Candidata(long id,UUID codigo,UUID operacion,String correo,String motivo,boolean autorizada){}
    private void publicar(UUID intento){
        configuracion.bloquear();jdbc.execute("SELECT pg_advisory_xact_lock(764001)");jdbc.execute("SELECT pg_advisory_xact_lock(764002)");
        var candidatas=jdbc.query("""
            SELECT c.id_configuracion_version,c.codigo_publico,p.id_operacion,u.correo,p.motivo,
              (u.estado='ACTIVO' AND u.es_administrador_propietario AND r.codigo='ADMIN_ADMIN' AND r.activo
               AND u.version_acceso=a.version_acceso AND u.correo=a.correo_destino AND a.fecha_consumo IS NOT NULL AND a.fecha_revocacion IS NULL) AS autorizada
            FROM lamontana.intento_activacion_configuracion i JOIN lamontana.configuracion_version c ON c.id_configuracion_version=i.id_version_objetivo
            JOIN lamontana.programacion_configuracion p USING(id_configuracion_version)
            JOIN lamontana.autorizacion_configuracion a ON a.id_operacion=p.id_operacion JOIN lamontana.usuario u ON u.id_usuario=p.id_actor JOIN lamontana.rol r USING(id_rol)
            WHERE i.id_intento=? AND i.estado='INICIADO' AND c.estado='PROGRAMADA' FOR UPDATE OF u
            """,(r,n)->new Candidata(r.getLong(1),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getString(4),r.getString(5),r.getBoolean(6)),intento);
        if(candidatas.isEmpty())return; // Otro reconciliador ya resolvió el mismo intento; jamás repetir la publicación.
        var c=candidatas.get(0);if(!c.autorizada())throw new AutorizacionVencida();
        var actual=revision.evaluar(c.codigo(),c.correo(),true);
        if(actual.bloqueos()>0){
            String motivos=actual.hallazgos().stream().filter(h->h.nivel().equals("BLOQUEO")).map(RevisionConfiguracionService.Hallazgo::mensaje).distinct().limit(3).collect(java.util.stream.Collectors.joining(" "));
            throw new CondicionesInvalidas(motivos.substring(0,Math.min(850,motivos.length()))+" Corregí las condiciones externas o cancelá para preparar otra versión.");
        }
        activacion.publicar(c.id(),null,c.operacion(),actual.catalogo().actual().codigoPublico(),c.motivo());
        jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version,codigo_recurso,motivo) SELECT id_configuracion_version,NULL,'CONFIGURACION_ACTIVADA',version,?,? FROM lamontana.configuracion_version WHERE id_configuracion_version=?",intento,"Activación automática autorizada al programar",c.id());
        jdbc.update("UPDATE lamontana.intento_activacion_configuracion SET estado='EXITOSO',id_version_resultante=?,fecha_fin=clock_timestamp(),codigo_resultado='CONFIGURACION_PUBLICADA',detalle_sanitizado='La configuración se publicó completamente.' WHERE id_intento=? AND estado='INICIADO'",c.id(),intento);
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",c.id());
    }
    private Instant ahora(){return jdbc.queryForObject("SELECT clock_timestamp()",Timestamp.class).toInstant();}
    private static class AutorizacionVencida extends RuntimeException{}
    private static class CondicionesInvalidas extends RuntimeException{CondicionesInvalidas(String mensaje){super(mensaje);}}
}
