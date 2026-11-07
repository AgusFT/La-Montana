package ar.com.lamontana.entregas;

import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.PagoController;
import ar.com.lamontana.pagos.PagoService;
import ar.com.lamontana.pedidos.PedidoService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RetiroSucursalService {
    private final JdbcTemplate jdbc;
    private final OrganizacionService organizacion;
    private final PedidoService pedidos;
    private final PagoService pagos;

    public RetiroSucursalService(JdbcTemplate jdbc, OrganizacionService organizacion,PedidoService pedidos,PagoService pagos) {
        this.jdbc = jdbc;
        this.organizacion = organizacion;
        this.pedidos=pedidos;
        this.pagos=pagos;
    }

    @Transactional public PagoService.Vista pagos(UUID sucursal,UUID pedido,String correo){
        var p=pedidos.detalleRetiroSucursal(pedido,sucursal,correo);
        return pagos.vistaRetiro(p.cotizacion(),correo,sucursal);
    }
    @Transactional public PagoService.Vista cobrar(UUID sucursal,UUID pedido,PagoController.Recibir in,String correo){
        var p=pedidos.detalleRetiroSucursal(pedido,sucursal,correo);
        return pagos.recibirRetiro(p.cotizacion(),in,correo,sucursal);
    }

    public enum Bandeja { PARA_RETIRAR, EN_CAMINO, ENTREGADOS }
    public record Fila(UUID pedido, long numero, String cliente, boolean invitado, String estado, String saldo, String medio) {}
    public record Pagina(List<Fila> pedidos, long total, int pagina) {}

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Pagina buscar(UUID sucursal, Bandeja bandeja, String texto, int pagina, String correo) {
        organizacion.exigirPermiso(correo, "GESTIONAR_ENTREGAS");
        organizacion.sucursalAutorizada(correo, sucursal);
        if (pagina < 0 || pagina > 100000 || texto.length() > 160)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisá la búsqueda y la página solicitada.");
        String filtro = switch (bandeja) {
            case PARA_RETIRAR -> "p.estado='LISTO_PARA_ENTREGA' AND r.estado='ACTIVA' AND lamontana.estado_logistico(p.id_pedido) IN ('LISTO_RETIRO','DISPONIBLE_PUNTO')";
            case EN_CAMINO -> "p.estado='LISTO_PARA_ENTREGA' AND r.estado='ACTIVA' AND lamontana.estado_logistico(p.id_pedido)='EN_VIAJE'";
            case ENTREGADOS -> "p.estado IN ('ENTREGADO','CERRADO') AND r.estado='CUMPLIDA'";
        };
        String buscar = texto.strip().toLowerCase(Locale.ROOT);
        String origen = """
            FROM lamontana.pedido_actual p
            JOIN lamontana.destinatario_cotizacion d USING(id_cotizacion)
            JOIN lamontana.cotizacion c USING(id_cotizacion)
            JOIN LATERAL (SELECT modalidad,destino_publico,estado FROM lamontana.reserva_entrega r
              WHERE r.id_pedido=p.id_pedido ORDER BY id_reserva_entrega DESC LIMIT 1) r ON true
            WHERE r.modalidad='RETIRO_SUCURSAL' AND r.destino_publico=?
              AND (strpos(lower(d.nombre),?)>0 OR lower('PED-'||p.id_pedido)=? OR p.id_pedido::text=?)
              AND
            """ + filtro;
        long total = jdbc.queryForObject("SELECT count(*) " + origen, Long.class, sucursal, buscar, buscar, buscar);
        var filas = jdbc.query("""
            SELECT p.codigo_publico,p.id_pedido,d.nombre,d.invitado,p.estado,c.oferta->>'medioPago' AS medio,
                   greatest(p.total-coalesce((SELECT sum(n.neto) FROM lamontana.aplicacion_pago_neta n
                      WHERE n.id_cotizacion=p.id_cotizacion),0),0) AS saldo
            """ + origen + " ORDER BY p.id_pedido DESC LIMIT 25 OFFSET ?",
            (rs,n) -> new Fila(rs.getObject("codigo_publico", UUID.class), rs.getLong("id_pedido"),
                rs.getString("nombre"), rs.getBoolean("invitado"), rs.getString("estado"),
                rs.getBigDecimal("saldo").toPlainString(), rs.getString("medio")),
            sucursal, buscar, buscar, buscar, pagina * 25);
        return new Pagina(filas, total, pagina);
    }
}
