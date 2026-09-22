package ar.com.lamontana.pagos;

import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Sólo aplicaciones de dinero recibido, descontando liberaciones y devoluciones. */
@Component
public class CoberturaPagos {
    private final JdbcTemplate jdbc;
    public CoberturaPagos(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public BigDecimal acreditado(long cotizacion,List<MedioPago> medios){
        return jdbc.query("SELECT p.medio,sum(a.neto) AS importe FROM lamontana.aplicacion_pago_neta a JOIN lamontana.pago p USING(id_pago) WHERE a.id_cotizacion=? GROUP BY p.medio",
            (r,n)->medios.contains(MedioPago.valueOf(r.getString("medio")))?r.getBigDecimal("importe"):BigDecimal.ZERO,cotizacion).stream().reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    public boolean cubre(long cotizacion,String previo,String sena,List<MedioPago> medios){return acreditado(cotizacion,medios).compareTo(new BigDecimal(previo).add(new BigDecimal(sena)))>=0;}
}
