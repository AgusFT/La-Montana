package ar.com.lamontana.entregas;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Read-only manifest for the authenticated rider. Assignment is owned by administration. */
@RestController
public class RiderController {
    private final JdbcTemplate jdbc;
    public RiderController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Pedido(UUID codigoPublico,long numero,String modalidad,String destino,String cliente,
                         String receptor,String telefono,Instant desde,Instant hasta,int orden) {}
    public record Recorrido(UUID codigoPublico,String estado,Instant asignadoEn,String asignadoPor,List<Pedido> pedidos) {}

    @GetMapping("/api/rider/recorridos/activo")
    public Recorrido activo(Principal principal) {
        var routes=jdbc.query("""
                SELECT rr.id_recorrido,rr.codigo_publico,rr.estado,rr.fecha_asignacion,rr.actor_asignador_nombre
                FROM lamontana.recorrido_reparto rr
                JOIN lamontana.usuario u ON u.id_usuario=rr.id_repartidor
                JOIN lamontana.cuenta_repartidor cr ON cr.id_usuario=u.id_usuario
                WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND cr.activa AND rr.estado='EN_REPARTO'
                ORDER BY rr.fecha_asignacion DESC LIMIT 1
                """,(rs,row)->new Object[]{rs.getLong(1),rs.getObject(2,UUID.class),rs.getString(3),rs.getTimestamp(4).toInstant(),rs.getString(5)},principal.getName());
        if(routes.isEmpty()){
            Boolean rider=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.cuenta_repartidor cr JOIN lamontana.usuario u USING(id_usuario) WHERE lower(u.correo)=lower(?) AND cr.activa AND u.estado='ACTIVO')",Boolean.class,principal.getName());
            if(!Boolean.TRUE.equals(rider))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Esta cuenta no está habilitada como rider.");
            return null;
        }
        Object[] route=routes.get(0);long id=(Long)route[0];
        var orders=jdbc.query("""
                SELECT p.codigo_publico,p.id_pedido,r.modalidad,r.nombre_destino,
                       concat_ws(' ',c.nombre,c.apellido),coalesce(p.receptor,concat_ws(' ',c.nombre,c.apellido)),p.telefono,
                       r.franja_desde,r.franja_hasta,rp.orden
                FROM lamontana.recorrido_reparto_pedido rp
                JOIN lamontana.pedido p USING(id_pedido)
                JOIN lamontana.usuario c ON c.id_usuario=p.id_usuario_creador
                JOIN lamontana.reserva_entrega r ON r.id_pedido=p.id_pedido AND r.estado='ACTIVA'
                WHERE rp.id_recorrido=? ORDER BY rp.orden
                """,(rs,row)->new Pedido(rs.getObject(1,UUID.class),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getTimestamp(8).toInstant(),rs.getTimestamp(9).toInstant(),rs.getInt(10)),id);
        return new Recorrido((UUID)route[1],(String)route[2],(Instant)route[3],(String)route[4],orders);
    }
}
