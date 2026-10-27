//#region Conservación · revocación durable, purga idempotente y recordatorios independientes
package ar.com.lamontana.pagos;

import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.identidad.CorreoService;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Mismos locks que pagos/pedidos. Nunca limpia mientras hay informes, dinero o gestiones pendientes. */
@Component
public class ConservacionArchivosScheduler {
    private static final Logger log=LoggerFactory.getLogger(ConservacionArchivosScheduler.class);
    private final JdbcTemplate jdbc;private final TransactionTemplate tx;private final ArchivosPrivados storage;private final CorreoService correo;
    public ConservacionArchivosScheduler(JdbcTemplate jdbc,PlatformTransactionManager manager,ArchivosPrivados storage,CorreoService correo){this.jdbc=jdbc;tx=new TransactionTemplate(manager);tx.setTimeout(20);this.storage=storage;this.correo=correo;}
    private boolean bloquear(){for(long id:new long[]{764003,764001,764002,764004})if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(?)",Boolean.class,id)))return false;return true;}

    @Scheduled(fixedDelayString="${lamontana.archivos.conservacion-intervalo-ms:60000}",initialDelayString="${lamontana.archivos.conservacion-inicio-ms:15000}")
    public void ejecutar(){
        try{tx.executeWithoutResult(s->{if(!bloquear())return;avisos();preparar();});eliminar();enviarAvisos();}
        catch(RuntimeException error){log.warn("La conservación se reintentará; no se descartan filas ni auditoría. Tipo: {}",error.getClass().getSimpleName());}
    }
    private void avisos(){
        var ids=jdbc.query("SELECT c.id_pedido FROM lamontana.cobro_pedido c JOIN lamontana.pedido_actual p USING(id_pedido) WHERE c.definitivo_en IS NULL AND p.estado IN ('PENDIENTE_REVISION','APROBADO') AND c.fecha_limite>clock_timestamp() AND c.recordatorio_minutos IS NOT NULL AND c.recordatorio_emitido_en IS NULL AND clock_timestamp()>=c.fecha_limite-c.recordatorio_minutos*interval '1 minute' AND NOT lamontana.produccion_cubierta(c.id_pedido) AND NOT lamontana.cobro_informado_pendiente(c.id_pedido) ORDER BY c.fecha_limite LIMIT 20",(r,n)->r.getLong(1));
        for(var id:ids){
            jdbc.update("INSERT INTO lamontana.aviso_cobro_pedido(id_pedido,fecha_limite) SELECT id_pedido,fecha_limite FROM lamontana.cobro_pedido WHERE id_pedido=? ON CONFLICT DO NOTHING",id);
            jdbc.update("UPDATE lamontana.cobro_pedido SET recordatorio_emitido_en=clock_timestamp() WHERE id_pedido=?",id);
            evento(id,"RECORDATORIO","Aviso previo al vencimiento. No cambia el estado económico, la retención ni la autorización de producción.");
        }
    }
    private void preparar(){
        var candidatos=jdbc.query("SELECT c.id_pedido,p.estado,p.version FROM lamontana.cobro_pedido c JOIN lamontana.pedido_actual p USING(id_pedido) WHERE c.definitivo_en IS NULL AND lamontana.cobro_puede_purgar(c.id_pedido) ORDER BY c.id_pedido LIMIT 20",(r,n)->new Object[]{r.getLong(1),r.getString(2),r.getLong(3)});
        for(var fila:candidatos){
            long id=(Long)fila[0],version=(Long)fila[2];Instant ahora=Instant.now();String estado=(String)fila[1];
            // La decisión definitiva y la revocación se confirman antes de tocar Storage.
            jdbc.update("UPDATE lamontana.cobro_pedido SET definitivo_en=?,version=version+1 WHERE id_pedido=?",Timestamp.from(ahora),id);
            if(!Set.of("CANCELADO","RECHAZADO").contains(estado)){
                jdbc.update("UPDATE lamontana.pedido SET estado='CANCELADO',finalizada_en=?,version=version+1 WHERE id_pedido=?",Timestamp.from(ahora),id);
                jdbc.update("UPDATE lamontana.reserva_entrega SET estado='LIBERADA' WHERE id_pedido=? AND estado='ACTIVA'",id);
                jdbc.update("INSERT INTO lamontana.historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha,estado_origen,version_pedido,accion,mensaje_cliente,actor_nombre,actor_tipo) VALUES (?,NULL,?,?,'CANCELADO',?,?,?,?,'CANCELAR',?,'Sistema · conservación','SISTEMA')",id,UUID.randomUUID(),"0".repeat(64),"Plazo comercial y retención técnica agotados sin pago informado, dinero ni gestiones pendientes.",Timestamp.from(ahora),estado,version+1,"Terminó la ventana de recuperación de pago. El pedido se conserva en historial y sus archivos pasan a eliminación técnica auditada.");
            }
            jdbc.update("""
                INSERT INTO lamontana.eliminacion_archivo(id_archivo_almacenado,id_pedido)
                SELECT DISTINCT a.id_archivo_almacenado,? FROM lamontana.pedido_actual p
                JOIN lamontana.cotizacion_item ci ON lamontana.cotizacion_descendiente(p.id_cotizacion,ci.id_cotizacion)
                JOIN lamontana.archivo_trabajo t USING(id_cotizacion_item)
                JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado)
                WHERE p.id_pedido=? AND a.finalidad='TRABAJO' AND a.estado<>'VALIDANDO'
                AND NOT EXISTS(SELECT 1 FROM lamontana.pedido_archivo_actual otro WHERE otro.id_archivo_almacenado=a.id_archivo_almacenado AND otro.id_pedido<>p.id_pedido)
                ON CONFLICT DO NOTHING
                """,id,id);
            evento(id,"REVOCACION_ARCHIVOS","Revocados originales y vistas previas de trabajo. Se conservan pedido, importes, pagos, documentos financieros y auditoría. La eliminación física se reintenta si falla.");
        }
    }
    private void eliminar(){
        var ids=jdbc.query("SELECT a.codigo_publico FROM lamontana.eliminacion_archivo e JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) WHERE e.eliminado_en IS NULL ORDER BY e.id_archivo_almacenado LIMIT 20",(r,n)->r.getObject(1,UUID.class));
        for(var file:ids)tx.executeWithoutResult(status->{
            if(!bloquear())return;
            var trabajos=jdbc.query("SELECT e.id_pedido FROM lamontana.eliminacion_archivo e JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) WHERE a.codigo_publico=? AND e.eliminado_en IS NULL FOR UPDATE OF e",(r,n)->r.getLong(1),file);
            if(trabajos.isEmpty()||!Boolean.TRUE.equals(jdbc.queryForObject("SELECT lamontana.cobro_puede_purgar(?)",Boolean.class,trabajos.get(0))))return;
            try{
                // Un pago informado toma los mismos locks; no puede confirmarse entre comprobación y borrado.
                storage.descartar(file);
                jdbc.update("UPDATE lamontana.eliminacion_archivo SET eliminado_en=clock_timestamp(),intentos=intentos+1,ultimo_error=NULL WHERE id_archivo_almacenado=(SELECT id_archivo_almacenado FROM lamontana.archivo_almacenado WHERE codigo_publico=?)",file);
                evento(trabajos.get(0),"CONTENIDO_ELIMINADO","Eliminación física idempotente del archivo "+file+" y sus vistas previas. Se conserva la referencia histórica.");
            }catch(IOException ex){
                jdbc.update("UPDATE lamontana.eliminacion_archivo SET intentos=intentos+1,ultimo_error='STORAGE_NO_DISPONIBLE' WHERE id_archivo_almacenado=(SELECT id_archivo_almacenado FROM lamontana.archivo_almacenado WHERE codigo_publico=?)",file);
                evento(trabajos.get(0),"ELIMINACION_PENDIENTE","Storage no disponible. El acceso sigue revocado; se reintentará sin eliminar la historia.");
            }
        });
    }
    private void enviarAvisos(){
        var avisos=jdbc.query("SELECT a.id_pedido,a.fecha_limite,u.correo,p.codigo_publico FROM lamontana.aviso_cobro_pedido a JOIN lamontana.pedido p USING(id_pedido) JOIN lamontana.usuario u ON u.id_usuario=p.id_usuario_creador JOIN lamontana.cobro_pedido c USING(id_pedido) WHERE a.correo_enviado_en IS NULL AND a.fecha_limite>clock_timestamp() AND a.fecha_limite=c.fecha_limite AND c.definitivo_en IS NULL AND p.estado NOT IN ('CANCELADO','RECHAZADO','CERRADO') AND NOT lamontana.produccion_cubierta(a.id_pedido) AND NOT lamontana.cobro_informado_pendiente(a.id_pedido) ORDER BY a.creado_en LIMIT 20",(r,n)->new Object[]{r.getLong(1),r.getTimestamp(2),r.getString(3),r.getObject(4,UUID.class)});
        for(var a:avisos)try{
            correo.enviarAviso((String)a[2],"Recordatorio de pago del pedido","El plazo comercial del pedido "+a[3]+" vence el "+a[1]+". Si ya transferiste, informá el pago desde tu pedido. Este aviso no acredita dinero ni autoriza producción. El vencimiento no elimina inmediatamente los archivos.");
            jdbc.update("UPDATE lamontana.aviso_cobro_pedido SET correo_enviado_en=clock_timestamp() WHERE id_pedido=? AND fecha_limite=? AND correo_enviado_en IS NULL",a[0],a[1]);
        }catch(RuntimeException ex){log.warn("Recordatorio pendiente de reenvío para el pedido {}",a[0]);}
    }
    private void evento(long pedido,String tipo,String motivo){jdbc.update("INSERT INTO lamontana.evento_cobro_pedido(id_pedido,id_operacion,tipo,motivo,resultado) VALUES (?,?,?,?,'{}'::jsonb)",pedido,UUID.randomUUID(),tipo,motivo);}
}
//#endregion
