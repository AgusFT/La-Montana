package ar.com.lamontana.puntos;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Scope is derived from the authenticated identity, never from a caller-selected point. */
public final class CuentaPuntoAcceso {
 private CuentaPuntoAcceso(){}
 public static Long punto(JdbcTemplate jdbc,String actor){var ids=jdbc.query("SELECT cp.id_punto_entrega FROM lamontana.cuenta_punto cp JOIN lamontana.usuario u USING(id_usuario) WHERE u.correo=?",(r,n)->r.getLong(1),actor);return ids.isEmpty()?null:ids.get(0);}
 public static void pedido(JdbcTemplate jdbc,String actor,UUID pedido){var point=punto(jdbc,actor);if(point==null)return;if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.pedido p JOIN lamontana.reserva_entrega r USING(id_pedido) JOIN lamontana.punto_entrega pt ON pt.codigo_publico=r.destino_publico JOIN lamontana.cuenta_punto cp USING(id_punto_entrega) WHERE p.codigo_publico=? AND cp.id_punto_entrega=? AND cp.activa AND r.modalidad='RETIRO_PUNTO_ENTREGA' AND r.estado IN ('ACTIVA','CUMPLIDA'))",Boolean.class,pedido,point)))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"El pedido no está disponible en este punto.");}
 public static boolean cotizacion(JdbcTemplate jdbc,String actor,UUID quote){var point=punto(jdbc,actor);return point==null||Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.cotizacion c JOIN lamontana.pedido_actual p ON lamontana.cotizacion_descendiente(p.id_cotizacion,c.id_cotizacion) JOIN lamontana.reserva_entrega r USING(id_pedido) JOIN lamontana.punto_entrega pt ON pt.codigo_publico=r.destino_publico JOIN lamontana.cuenta_punto cp USING(id_punto_entrega) WHERE c.codigo_publico=? AND cp.id_punto_entrega=? AND cp.activa AND r.modalidad='RETIRO_PUNTO_ENTREGA' AND r.estado IN ('ACTIVA','CUMPLIDA'))",Boolean.class,quote,point));}
}
