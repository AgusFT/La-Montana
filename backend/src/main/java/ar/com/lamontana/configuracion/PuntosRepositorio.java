//#region ENCABEZADO · PuntosRepositorio.java
/*
 * ========================================================================
 * ARCHIVO: PuntosRepositorio.java
 * ========================================================================
 * FUNCIÓN
 * Lee las definiciones de puntos de entrega de una versión y reconstruye sus relaciones con
 * sucursales, costos y franjas horarias.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] PuntosRepositorio(JdbcTemplate jdbc)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] List<Punto> leer(long config)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PuntosRepositorio (clase).
 * - PuntosRepositorio.Punto (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.PuntoEntregaController.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PuntosRepositorio {
    private static final DateTimeFormatter HORA=DateTimeFormatter.ofPattern("HH:mm");
    private final JdbcTemplate jdbc;
    public PuntosRepositorio(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Punto(UUID codigoPublico,String codigo,String nombre,String calle,String numero,String localidad,String provincia,String codigoPostal,
                        String referencias,String zonaHoraria,List<SucursalPunto> sucursales){}
    public List<Punto> leer(long config) {
        return jdbc.query("SELECT p.id_punto_entrega,p.codigo_publico,p.codigo,d.nombre,d.calle,d.numero,d.localidad,d.provincia,d.codigo_postal,d.referencias,d.zona_horaria FROM lamontana.configuracion_definicion_punto d JOIN lamontana.punto_entrega p USING(id_punto_entrega) WHERE d.id_configuracion_version=? ORDER BY p.codigo",(rs,row)->new Punto(rs.getObject("codigo_publico",UUID.class),rs.getString("codigo"),rs.getString("nombre"),rs.getString("calle"),rs.getString("numero"),rs.getString("localidad"),rs.getString("provincia"),rs.getString("codigo_postal"),rs.getString("referencias"),rs.getString("zona_horaria"),
                jdbc.query("SELECT c.id_configuracion_punto_entrega,s.codigo_publico,c.habilitado,c.costo FROM lamontana.configuracion_punto_entrega c JOIN lamontana.sucursal s USING(id_sucursal) WHERE c.id_configuracion_version=? AND c.id_punto_entrega=? ORDER BY s.codigo",(s,n)->new SucursalPunto(s.getObject("codigo_publico",UUID.class),s.getBoolean("habilitado"),s.getBigDecimal("costo").setScale(2).toPlainString(),
                        jdbc.query("SELECT dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada FROM lamontana.franja_entrega WHERE id_configuracion_punto_entrega=? ORDER BY dia_semana,hora_desde",(f,i)->new Franja(f.getInt(1),HORA.format(f.getTime(2).toLocalTime()),HORA.format(f.getTime(3).toLocalTime()),f.getInt(4),f.getBoolean(5)),s.getLong("id_configuracion_punto_entrega"))),config,rs.getLong("id_punto_entrega"))),config);
    }
}
