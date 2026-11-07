package ar.com.lamontana.entregas;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Pendientes de cualquier fecha y paradas completadas hoy, según la zona de la sucursal. */
@Service
public class JornadaRiderService {
    private final JdbcTemplate jdbc;
    public JornadaRiderService(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private record Fila(UUID recorrido,String estado,Instant asignado,String actor,RiderController.Pedido pedido){}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public List<RiderController.Recorrido> consultar(String correo){return consultar(correo,Instant.now());}

    List<RiderController.Recorrido> consultar(String correo,Instant ahora){
        boolean activa=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.cuenta_repartidor cr JOIN lamontana.usuario u USING(id_usuario) WHERE lower(u.correo)=lower(?) AND cr.activa AND u.estado='ACTIVO')",Boolean.class,correo));
        if(!activa)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Esta cuenta no está habilitada como rider.");
        var filas=jdbc.query("""
            WITH reloj AS (SELECT ?::timestamptz AS ahora), asignadas AS (
              SELECT rr.codigo_publico AS recorrido,rr.estado AS estado_recorrido,
                rr.fecha_asignacion,rr.actor_asignador_nombre,rp.orden,
                p.codigo_publico AS pedido,p.id_pedido,p.version,r.modalidad,
                CASE WHEN strpos(r.nombre_destino,' · ')>0 THEN r.nombre_destino ELSE
                  concat_ws(' · ',r.nombre_destino,CASE WHEN d.id_punto_entrega IS NOT NULL
                    THEN concat_ws(', ',concat_ws(' ',d.calle,d.numero),d.localidad,d.provincia) END) END AS destino,
                c.nombre AS cliente,coalesce(nullif(p.receptor,''),c.nombre) AS receptor,p.telefono,
                r.franja_desde,r.franja_hasta,lamontana.estado_logistico(p.id_pedido) AS logistica,
                rp.presentado_en,r.destino_publico,s.zona_horaria,reloj.ahora,
                CASE WHEN r.modalidad='ENVIO_DOMICILIO' THEN
                  (SELECT e.fecha_entrega FROM lamontana.entrega e WHERE e.id_pedido=p.id_pedido
                    AND e.id_reserva_entrega=r.id_reserva_entrega AND e.fecha_entrega>=rr.fecha_asignacion)
                ELSE
                  (SELECT max(h.fecha) FROM lamontana.movimiento_entrega m
                    JOIN lamontana.historial_estado_pedido h ON h.id_evento=m.id_evento_pedido
                    WHERE m.id_pedido=p.id_pedido AND m.id_reserva_entrega=r.id_reserva_entrega
                      AND m.estado_destino='DISPONIBLE_PUNTO' AND h.fecha>=rr.fecha_asignacion)
                END AS finalizado_en
              FROM lamontana.recorrido_reparto rr
              JOIN lamontana.usuario u ON u.id_usuario=rr.id_repartidor
              JOIN lamontana.cuenta_repartidor cr ON cr.id_usuario=u.id_usuario
              JOIN lamontana.recorrido_reparto_pedido rp USING(id_recorrido)
              JOIN lamontana.pedido_actual p USING(id_pedido)
              JOIN lamontana.destinatario_cotizacion c USING(id_cotizacion)
              JOIN lamontana.sucursal s ON s.id_sucursal=p.id_sucursal
              JOIN LATERAL (SELECT * FROM lamontana.reserva_entrega re WHERE re.id_pedido=p.id_pedido
                ORDER BY re.id_reserva_entrega DESC LIMIT 1) r ON true
              LEFT JOIN lamontana.punto_entrega pt ON r.modalidad='RETIRO_PUNTO_ENTREGA' AND pt.codigo_publico=r.destino_publico
              LEFT JOIN lamontana.configuracion_definicion_punto d
                ON d.id_punto_entrega=pt.id_punto_entrega AND d.id_configuracion_version=p.id_configuracion_version
              CROSS JOIN reloj
              WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND cr.activa
                AND rr.estado IN ('EN_REPARTO','FINALIZADO') AND p.estado NOT IN ('CANCELADO','RECHAZADO')
            ) SELECT * FROM asignadas
              WHERE (estado_recorrido='EN_REPARTO' AND logistica IN ('EN_VIAJE','CLIENTE_AUSENTE'))
                OR (finalizado_en >= (date_trunc('day',ahora AT TIME ZONE zona_horaria) AT TIME ZONE zona_horaria)
                  AND finalizado_en < ((date_trunc('day',ahora AT TIME ZONE zona_horaria)+interval '1 day') AT TIME ZONE zona_horaria))
              ORDER BY fecha_asignacion DESC,recorrido,orden
            """,(r,n)->new Fila(r.getObject("recorrido",UUID.class),r.getString("estado_recorrido"),
                r.getTimestamp("fecha_asignacion").toInstant(),r.getString("actor_asignador_nombre"),
                new RiderController.Pedido(r.getObject("pedido",UUID.class),r.getLong("id_pedido"),r.getString("modalidad"),
                    r.getString("destino"),r.getString("cliente"),r.getString("receptor"),r.getString("telefono"),
                    r.getTimestamp("franja_desde").toInstant(),r.getTimestamp("franja_hasta").toInstant(),r.getInt("orden"),
                    r.getLong("version"),r.getString("logistica"),r.getTimestamp("presentado_en")==null?null:r.getTimestamp("presentado_en").toInstant(),
                    r.getObject("destino_publico",UUID.class),r.getTimestamp("finalizado_en")==null?null:r.getTimestamp("finalizado_en").toInstant())),
                Timestamp.from(ahora),correo);
        var grupos=new LinkedHashMap<UUID,List<Fila>>();
        for(var fila:filas)grupos.computeIfAbsent(fila.recorrido(),key->new ArrayList<>()).add(fila);
        return grupos.values().stream().map(grupo->{var r=grupo.get(0);return new RiderController.Recorrido(r.recorrido(),r.estado(),r.asignado(),r.actor(),grupo.stream().map(Fila::pedido).toList());}).toList();
    }
}
