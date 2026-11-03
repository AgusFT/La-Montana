package ar.com.lamontana.entregas;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Manifest and stop actions scoped to the authenticated rider. */
@RestController
public class RiderController {
    private final JdbcTemplate jdbc;
    private final EntregaService entregas;
    public RiderController(JdbcTemplate jdbc,EntregaService entregas) { this.jdbc = jdbc; this.entregas=entregas; }

    public record Pedido(UUID codigoPublico,long numero,String modalidad,String destino,String cliente,
                         String receptor,String telefono,Instant desde,Instant hasta,int orden,
                         long version,String estadoLogistico,Instant presentadoEn) {}
    public record Recorrido(UUID codigoPublico,String estado,Instant asignadoEn,String asignadoPor,List<Pedido> pedidos) {}
    public record Resultado(boolean aceptado,String mensaje) {}

    @PostMapping("/api/admin/reparto/propio")
    public Resultado habilitar(Principal principal){
        int rows=jdbc.update("""
            INSERT INTO lamontana.cuenta_repartidor(id_usuario)
            SELECT u.id_usuario FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
            WHERE u.correo=? AND u.estado='ACTIVO' AND r.activo AND r.codigo='ADMIN_ADMIN'
            ON CONFLICT(id_usuario) DO UPDATE SET activa=true,actualizado_en=now()
            """,principal.getName());
        if(rows==0)throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return new Resultado(true,"Tu cuenta ya puede recibir recorridos.");
    }

    @PostMapping("/api/rider/pedidos/{pedido}/parada")
    public Resultado parada(@PathVariable UUID pedido,@Valid @RequestBody EntregaController.Movimiento in,Principal principal){
        entregas.paradaRider(pedido,in,principal.getName());
        return new Resultado(true,"Parada registrada.");
    }

    @PostMapping("/api/rider/pedidos/{pedido}/entregar")
    public Resultado entregar(@PathVariable UUID pedido,@Valid @RequestBody EntregaController.Recibir in,Principal principal){
        var resultado=entregas.recibirRider(pedido,in,principal.getName());
        return new Resultado(resultado.aceptado(),resultado.mensaje());
    }

    @GetMapping("/api/rider/recorridos/activo")
    public Recorrido activo(Principal principal) {
        return recorridos(principal).stream().findFirst().orElse(null);
    }

    @GetMapping("/api/rider/recorridos")
    public List<Recorrido> recorridos(Principal principal) {
        var routes=jdbc.query("""
                SELECT rr.id_recorrido,rr.codigo_publico,rr.estado,rr.fecha_asignacion,rr.actor_asignador_nombre
                FROM lamontana.recorrido_reparto rr
                JOIN lamontana.usuario u ON u.id_usuario=rr.id_repartidor
                JOIN lamontana.cuenta_repartidor cr ON cr.id_usuario=u.id_usuario
                WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND cr.activa AND rr.estado='EN_REPARTO'
                ORDER BY rr.fecha_asignacion DESC,rr.id_recorrido DESC
                """,(rs,row)->new Object[]{rs.getLong(1),rs.getObject(2,UUID.class),rs.getString(3),rs.getTimestamp(4).toInstant(),rs.getString(5)},principal.getName());
        if(routes.isEmpty()){
            Boolean rider=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.cuenta_repartidor cr JOIN lamontana.usuario u USING(id_usuario) WHERE lower(u.correo)=lower(?) AND cr.activa AND u.estado='ACTIVO')",Boolean.class,principal.getName());
            if(!Boolean.TRUE.equals(rider))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Esta cuenta no está habilitada como rider.");
            return List.of();
        }
        return routes.stream().map(this::manifiesto).toList();
    }

    private Recorrido manifiesto(Object[] route){
        long id=(Long)route[0];
        var orders=jdbc.query("""
                SELECT p.codigo_publico,p.id_pedido,r.modalidad,
                       COALESCE(NULLIF(concat_ws(' · ',r.nombre_destino,
                           CASE WHEN d.id_punto_entrega IS NOT NULL
                                THEN concat_ws(', ',concat_ws(' ',d.calle,d.numero),d.localidad,d.provincia)
                           END),''),r.nombre_destino),
                       concat_ws(' ',c.nombre,c.apellido),coalesce(p.receptor,concat_ws(' ',c.nombre,c.apellido)),p.telefono,
                       r.franja_desde,r.franja_hasta,rp.orden,p.version,
                       lamontana.estado_logistico(p.id_pedido),rp.presentado_en
                FROM lamontana.recorrido_reparto_pedido rp
                JOIN lamontana.pedido p USING(id_pedido)
                JOIN lamontana.usuario c ON c.id_usuario=p.id_usuario_creador
                JOIN LATERAL (SELECT * FROM lamontana.reserva_entrega re WHERE re.id_pedido=p.id_pedido
                              ORDER BY re.id_reserva_entrega DESC LIMIT 1) r ON true
                LEFT JOIN lamontana.punto_entrega pt ON r.modalidad='RETIRO_PUNTO_ENTREGA' AND pt.codigo_publico=r.destino_publico
                LEFT JOIN lamontana.configuracion_definicion_punto d
                  ON d.id_punto_entrega=pt.id_punto_entrega AND d.id_configuracion_version=p.id_configuracion_version
                WHERE rp.id_recorrido=? ORDER BY rp.orden
                """,(rs,row)->new Pedido(rs.getObject(1,UUID.class),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getTimestamp(8).toInstant(),rs.getTimestamp(9).toInstant(),rs.getInt(10),rs.getLong(11),rs.getString(12),rs.getTimestamp(13)==null?null:rs.getTimestamp(13).toInstant()),id);
        return new Recorrido((UUID)route[1],(String)route[2],(Instant)route[3],(String)route[4],orders);
    }
}
