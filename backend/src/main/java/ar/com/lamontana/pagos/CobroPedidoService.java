//#region Cobro y archivos · snapshots por pedido, autorización y recuperación administrativa
package ar.com.lamontana.pagos;

import ar.com.lamontana.configuracion.PoliticaPagoArchivos;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** Reutiliza dinero/aplicaciones y estados operativos. Un aviso de pago nunca acredita fondos. */
@Service
public class CobroPedidoService {
    private final JdbcTemplate jdbc;
    private final JsonMapper json=JsonMapper.builder().build();
    public CobroPedidoService(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public enum Accion { ACEPTAR_CONTINUAR,MANTENER_PENDIENTE,EXTENDER_PLAZO,SOLICITAR_COMPROBANTE,FINALIZAR_CANCELACION }
    public record Evento(String tipo,String actor,Instant fecha,String motivo){}
    public record Vista(UUID pedido,long version,String estado,String mensaje,Instant limite,Instant retenerHasta,int retencionHoras,
        boolean puedeInformar,boolean purgaSuspendida,boolean recordatorio,boolean puedeResolver,List<Evento> historial){}
    private record Fila(long pedido,long version,Instant limite,Instant retenerHasta,int retencion,Instant cobertura,
        boolean intervencion,boolean excepcion,Instant definitivo,Integer recordatorio,String operativo){}

    public void bloquear(){for(long id:new long[]{764003,764001,764002,764004})jdbc.execute("SELECT pg_advisory_xact_lock("+id+")");}
    private Instant fecha(Timestamp value){return value==null?null:value.toInstant();}
    private Fila fila(long pedido){return jdbc.query("SELECT c.*,p.estado FROM lamontana.cobro_pedido c JOIN lamontana.pedido_actual p USING(id_pedido) WHERE c.id_pedido=?",(r,n)->new Fila(pedido,r.getLong("version"),fecha(r.getTimestamp("fecha_limite")),fecha(r.getTimestamp("retener_hasta")),r.getInt("retencion_horas"),fecha(r.getTimestamp("cobertura_confirmada_en")),r.getBoolean("intervencion_pendiente"),r.getBoolean("excepcion_aceptada"),fecha(r.getTimestamp("definitivo_en")),r.getObject("recordatorio_minutos",Integer.class),r.getString("estado")),pedido).stream().findFirst().orElse(null);}
    private boolean consulta(String funcion,long pedido){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT lamontana."+funcion+"(?)",Boolean.class,pedido));}
    public Long pedido(long cotizacion){return jdbc.query("SELECT p.id_pedido FROM lamontana.pedido_actual p WHERE lamontana.cotizacion_descendiente(p.id_cotizacion,?) ORDER BY p.id_pedido DESC LIMIT 1",(r,n)->r.getLong(1),cotizacion).stream().findFirst().orElse(null);}

    /** El reloj comercial empieza con la confirmación del pedido, no con la cotización ni la carga. */
    public void iniciar(long pedido,PoliticaPagoArchivos politica,Instant creada){
        Instant limite=politica==null?null:politica.limite(creada);
        jdbc.update("INSERT INTO lamontana.cobro_pedido(id_pedido,politica_original,fecha_limite,recordatorio_minutos,retencion_horas,retener_hasta,cobertura_confirmada_en) VALUES (?,?::jsonb,?,?,?,?,?)",
            pedido,politica==null?null:json.writeValueAsString(politica),limite==null?null:Timestamp.from(limite),politica==null?null:politica.recordatorioMinutos(),politica==null?72:politica.retencionHoras(),limite==null?null:Timestamp.from(politica.retenerHasta(limite)),consulta("produccion_cubierta",pedido)?Timestamp.from(creada):null);
        evento(pedido,UUID.randomUUID(),null,"POLITICA_CAPTURADA","Plazos y retención capturados de la versión confirmada. Sin plazo legado no se ejecuta vencimiento automático.");
    }
    public Vista vista(long pedido,boolean administrador){
        var f=fila(pedido);if(f==null)return null;Instant ahora=Instant.now();
        boolean informado=consulta("cobro_informado_pendiente",pedido),dinero=consulta("cobro_dinero_por_resolver",pedido),cubierto=consulta("produccion_cubierta",pedido);
        boolean cancelado=Set.of("CANCELADO","RECHAZADO").contains(f.operativo());
        boolean tardio=f.limite()!=null&&!ahora.isBefore(f.limite());
        boolean pendiente=f.intervencion()||informado||dinero&&(!cubierto||cancelado);
        boolean retenible=f.retenerHasta()==null||ahora.isBefore(f.retenerHasta())||pendiente;
        boolean pagado=cubierto&&!consulta("cobro_bloquea_produccion",pedido);
        boolean anticipo=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.pedido_actual p CROSS JOIN LATERAL (VALUES(p.condiciones_finales->'cotizadas'),(p.condiciones_finales->'actuales')) c(terminos) WHERE p.id_pedido=? AND (c.terminos->>'pagoPrevioRequerido')::numeric+(c.terminos->>'senaRequerida')::numeric>0)",Boolean.class,pedido));
        String estado=cancelado&&(dinero||pendiente)?"RESOLUCION_FINANCIERA_PENDIENTE":f.definitivo()!=null?"VENCIDO_DEFINITIVO":cancelado?"CANCELADO":informado?tardio?"PAGO_INFORMADO_FUERA_TERMINO":"PAGO_INFORMADO":f.intervencion()?"RESOLUCION_ADMINISTRATIVA":pagado?anticipo?"PAGO_ACREDITADO":"SIN_ANTICIPO":tardio?retenible?"PAGO_VENCIDO_RECUPERABLE":"RETENCION_AGOTADA":"PENDIENTE_PAGO";
        String mensaje=switch(estado){
            case "PAGO_INFORMADO","PAGO_INFORMADO_FUERA_TERMINO"->"Recibimos tu aviso. Tu pedido y sus archivos se conservan mientras verificamos el pago. Informar no acredita dinero ni autoriza producción.";
            case "PAGO_VENCIDO_RECUPERABLE"->"El plazo original de pago venció. Tu pedido todavía puede recuperarse; si ya transferiste, informá el pago para verificarlo.";
            case "RESOLUCION_ADMINISTRATIVA","RESOLUCION_FINANCIERA_PENDIENTE"->"La imprenta debe resolver este caso. La eliminación automática está suspendida; los pagos y la historia se conservan.";
            case "VENCIDO_DEFINITIVO","CANCELADO"->"El pedido finalizó. Los archivos sin obligación operativa de conservación se eliminan mediante un proceso auditado; el historial del pedido permanece.";
            case "RETENCION_AGOTADA"->"Terminó el período de recuperación. Consultá a la imprenta; no se puede autorizar producción con este estado.";
            case "SIN_ANTICIPO"->"Esta condición no exige un anticipo. Se mantienen las validaciones y aprobación correspondientes; el saldo debe resolverse antes de entregar.";
            case "PAGO_ACREDITADO"->"Las condiciones económicas de aprobación están cubiertas. Se mantienen los controles técnicos y de autorización; el saldo restante se verifica antes de entregar.";
            default->"Podés completar el pedido y cargar el PDF antes de pagar. La aprobación y producción esperan la acreditación correspondiente.";
        };
        if(f.limite()==null&&!cancelado)mensaje+=" Esta versión legada no fijó un plazo comercial; requiere gestión explícita, sin vencimiento automático.";
        boolean aviso=f.recordatorio()!=null&&f.limite()!=null&&!ahora.isBefore(f.limite().minusSeconds(f.recordatorio()*60L))&&ahora.isBefore(f.limite())&&!pagado&&!informado;
        var historia=jdbc.query("SELECT e.tipo,coalesce(u.nombre||' '||u.apellido,'Sistema'),e.fecha,e.motivo FROM lamontana.evento_cobro_pedido e LEFT JOIN lamontana.usuario u ON u.id_usuario=e.id_actor WHERE e.id_pedido=? ORDER BY e.id_evento DESC LIMIT 100",(r,n)->new Evento(r.getString(1),administrador?r.getString(2):"La imprenta",r.getTimestamp(3).toInstant(),r.getString(4)),pedido);
        return new Vista(jdbc.queryForObject("SELECT codigo_publico FROM lamontana.pedido WHERE id_pedido=?",UUID.class,pedido),f.version(),estado,mensaje,f.limite(),f.retenerHasta(),f.retencion(),f.definitivo()==null&&!cancelado&&retenible,pendiente,aviso,administrador&&f.definitivo()==null,List.copyOf(historia));
    }
    public void exigirInformable(long cotizacion){var p=pedido(cotizacion);if(p==null)return;var v=vista(p,false);if(v!=null&&!v.puedeInformar())throw conflicto("El período de recuperación terminó o el pedido fue cancelado definitivamente. Consultá a la imprenta; los pagos reales requieren resolución administrativa.");}

    /** Se llama dentro de la misma transacción/locks de la escritura financiera existente. */
    public void movimiento(long cotizacion,UUID operacion,long actor,String tipo,String motivo){
        Long p=pedido(cotizacion);if(p==null||fila(p)==null)return;var f=fila(p);Instant ahora=Instant.now();
        boolean cubierto=consulta("produccion_cubierta",p),tardio=f.limite()!=null&&!ahora.isBefore(f.limite());
        boolean revision=tipo.equals("DESCARTAR")||tipo.equals("INFORMAR")&&tardio||tipo.equals("APLICAR")&&cubierto&&f.cobertura()==null&&tardio&&!f.excepcion();
        jdbc.update("UPDATE lamontana.cobro_pedido SET intervencion_pendiente=intervencion_pendiente OR ?,cobertura_confirmada_en=CASE WHEN ? THEN coalesce(cobertura_confirmada_en,clock_timestamp()) ELSE NULL END,version=version+1 WHERE id_pedido=?",revision,cubierto,p);
        evento(p,operacion,actor,tipo,motivo);
        if(tipo.equals("APLICAR"))aprobarCondicional(p,actor);
    }
    /** La presentación de evidencia exige resolución; ni el PDF ni su análisis acreditan dinero. */
    public void comprobantePresentado(long cotizacion,UUID archivo,long actor){
        Long p=pedido(cotizacion);if(p==null||fila(p)==null)return;var f=fila(p);
        boolean obligaciones=consulta("cobro_informado_pendiente",p)||consulta("cobro_dinero_por_resolver",p)||f.intervencion();
        if(f.definitivo()!=null||f.retenerHasta()!=null&&!Instant.now().isBefore(f.retenerHasta())&&!obligaciones)
            throw conflicto("La recuperación terminó. Consultá a la imprenta antes de presentar un nuevo comprobante.");
        jdbc.update("UPDATE lamontana.cobro_pedido SET intervencion_pendiente=true,version=version+1 WHERE id_pedido=?",p);
        evento(p,archivo,actor,"COMPROBANTE_PRESENTADO","Comprobante presentado para inspección y verificación administrativa. Se suspende la eliminación; no acredita dinero.");
    }
    public void cancelado(long pedido,UUID operacion,long actor,String motivo){
        if(fila(pedido)==null)return;
        boolean pendiente=consulta("cobro_informado_pendiente",pedido)||consulta("cobro_dinero_por_resolver",pedido);
        jdbc.update("UPDATE lamontana.cobro_pedido SET intervencion_pendiente=?,excepcion_aceptada=false,version=version+1 WHERE id_pedido=?",pendiente,pedido);
        evento(pedido,operacion,actor,pendiente?"CANCELACION_CON_FINANZAS_PENDIENTES":"CANCELACION_DEFINITIVA",motivo);
    }
    public void exigirAprobable(long pedido){
        if(fila(pedido)==null)return;
        if(!consulta("produccion_cubierta",pedido))throw conflicto("Falta acreditar y aplicar el total previo o la seña requerida. El PDF permanece almacenado, pero no autoriza aprobación ni producción.");
        if(consulta("cobro_bloquea_produccion",pedido))throw conflicto("Resolvé primero el plazo vencido o la intervención financiera pendiente. No se autoriza producción por informar un pago.");
    }
    private void aprobarCondicional(long pedido,long actor){
        if(!consulta("produccion_cubierta",pedido)||consulta("cobro_bloquea_produccion",pedido))return;
        var datos=jdbc.query("SELECT p.*,u.nombre||' '||u.apellido AS actor_nombre,r.codigo AS actor_rol FROM lamontana.pedido p JOIN lamontana.usuario u ON u.id_usuario=? JOIN lamontana.rol r USING(id_rol) WHERE p.id_pedido=? AND p.estado='PENDIENTE_REVISION' AND p.modo_aprobacion='AUTOMATICA' AND NOT EXISTS(SELECT 1 FROM lamontana.solicitud_correccion s WHERE s.id_pedido=p.id_pedido AND s.estado IN ('PENDIENTE','RESPONDIDA'))",(r,n)->new Object[]{r.getLong("version"),r.getString("actor_nombre"),r.getString("actor_rol")},actor,pedido);
        if(datos.isEmpty())return;
        // Ni un anticipo ni una resolución administrativa omiten la inspección técnica guardada.
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.pedido_archivo_actual WHERE id_pedido=?) AND NOT EXISTS(SELECT 1 FROM lamontana.pedido_archivo_actual p JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) WHERE p.id_pedido=? AND (a.estado<>'VALIDO' OR EXISTS(SELECT 1 FROM lamontana.eliminacion_archivo e WHERE e.id_archivo_almacenado=a.id_archivo_almacenado)))",Boolean.class,pedido,pedido)))return;
        Instant ahora=Instant.now();long version=(Long)datos.get(0)[0]+1;
        jdbc.update("UPDATE lamontana.pedido SET estado='APROBADO',aprobada_en=?,id_aprobador=?,version=version+1 WHERE id_pedido=?",Timestamp.from(ahora),actor,pedido);
        jdbc.update("INSERT INTO lamontana.historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha,estado_origen,version_pedido,accion,mensaje_cliente,actor_nombre,actor_tipo) VALUES (?,?,?,?,'APROBADO',?,?,'PENDIENTE_REVISION',?,'APROBAR',?,?,?)",pedido,actor,UUID.randomUUID(),"0".repeat(64),"Aprobación condicional tras acreditación/aplicación y controles económicos.",Timestamp.from(ahora),version,"Pago requerido acreditado. Pedido aprobado según las condiciones; producción todavía no iniciada.",datos.get(0)[1],datos.get(0)[2]);
    }

    @Transactional public Vista resolver(long pedido,UUID operacion,long version,Accion accion,Instant limite,String motivo,String correo){
        bloquear();var actor=jdbc.queryForObject("SELECT u.id_usuario FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.correo=? AND u.estado='ACTIVO' AND r.codigo='ADMIN_ADMIN' AND r.activo",Long.class,correo);
        var f=fila(pedido);if(f==null)throw conflicto("El pedido pertenece al flujo anterior y conserva sus condiciones originales.");
        String solicitud=json.writeValueAsString(Arrays.asList(accion,limite,motivo,version));
        var previo=jdbc.query("SELECT id_pedido,id_actor,resultado->>'solicitud' FROM lamontana.evento_cobro_pedido WHERE id_operacion=?",(r,n)->new Object[]{r.getLong(1),r.getObject(2,Long.class),r.getString(3)},operacion);
        if(!previo.isEmpty()){var old=previo.get(0);if((Long)old[0]!=pedido||!Objects.equals(old[1],actor)||!Objects.equals(old[2],solicitud))throw conflicto("La operación ya fue usada con otros datos.");return vista(pedido,true);}
        if(f.version()!=version||f.definitivo()!=null)throw conflicto("El caso cambió o finalizó. Consultá su estado antes de resolver.");
        if(motivo==null||motivo.isBlank()||motivo.length()>2000)throw conflicto("Completá un motivo de resolución válido.");
        if(accion!=Accion.EXTENDER_PLAZO&&limite!=null)throw conflicto("Una fecha nueva sólo corresponde a Extender plazo.");
        if(Set.of(Accion.ACEPTAR_CONTINUAR,Accion.EXTENDER_PLAZO,Accion.FINALIZAR_CANCELACION).contains(accion)&&consulta("cobro_comprobante_validando",pedido))
            throw conflicto("Esperá el resultado técnico del comprobante antes de resolver o levantar la conservación.");
        if(accion==Accion.ACEPTAR_CONTINUAR&&!consulta("produccion_cubierta",pedido))throw conflicto("Acreditá y aplicá primero el total previo o la seña exigible. Aceptar la continuación no crea ni acredita dinero.");
        boolean cancelado=Set.of("CANCELADO","RECHAZADO").contains(f.operativo());
        if(cancelado!=(accion==Accion.FINALIZAR_CANCELACION))throw conflicto("Un pedido cancelado requiere finalizar su resolución financiera; no se reabre con esta acción.");
        if(accion==Accion.FINALIZAR_CANCELACION&&(consulta("cobro_informado_pendiente",pedido)||consulta("cobro_dinero_por_resolver",pedido)))throw conflicto("Resolvé los informes y devolvé el dinero pendiente antes de finalizar la cancelación.");
        if(accion==Accion.EXTENDER_PLAZO){
            if(limite==null||!limite.isAfter(Instant.now())||limite.isAfter(Instant.now().plus(Duration.ofDays(30)))||f.limite()!=null&&!limite.isAfter(f.limite()))throw conflicto("La extensión debe ser futura, posterior al plazo anterior y dentro de 30 días.");
            jdbc.update("UPDATE lamontana.cobro_pedido SET fecha_limite=?,retener_hasta=?::timestamptz+retencion_horas*interval '1 hour',intervencion_pendiente=false,excepcion_aceptada=false,recordatorio_emitido_en=NULL,version=version+1 WHERE id_pedido=?",Timestamp.from(limite),Timestamp.from(limite),pedido);
        }else{
            if(accion==Accion.ACEPTAR_CONTINUAR&&consulta("cobro_informado_pendiente",pedido))throw conflicto("Primero verificá/acreditá o rechazá los informes pendientes. Aceptar la continuación no acredita dinero.");
            jdbc.update("UPDATE lamontana.cobro_pedido SET intervencion_pendiente=?,excepcion_aceptada=?,version=version+1 WHERE id_pedido=?",accion!=Accion.ACEPTAR_CONTINUAR&&accion!=Accion.FINALIZAR_CANCELACION,accion==Accion.ACEPTAR_CONTINUAR,pedido);
        }
        jdbc.update("INSERT INTO lamontana.evento_cobro_pedido(id_pedido,id_operacion,id_actor,tipo,motivo,resultado) VALUES (?,?,?,?,?,jsonb_build_object('solicitud',?::text,'estado',to_jsonb((SELECT c FROM lamontana.cobro_pedido c WHERE id_pedido=?))))",pedido,operacion,actor,accion.name(),motivo.strip(),solicitud,pedido);
        if(accion==Accion.ACEPTAR_CONTINUAR||accion==Accion.EXTENDER_PLAZO)aprobarCondicional(pedido,actor);
        return vista(pedido,true);
    }
    private void evento(long pedido,UUID operacion,Long actor,String tipo,String motivo){jdbc.update("INSERT INTO lamontana.evento_cobro_pedido(id_pedido,id_operacion,id_actor,tipo,motivo,resultado) VALUES (?,?,?,?,?,to_jsonb((SELECT c FROM lamontana.cobro_pedido c WHERE id_pedido=?)))",pedido,operacion,actor,tipo,motivo,pedido);}
    private ResponseStatusException conflicto(String texto){return new ResponseStatusException(HttpStatus.CONFLICT,texto);}
}
//#endregion
