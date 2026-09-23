//#region ENCABEZADO · RecursosRepositorio.java
/*
 * ========================================================================
 * ARCHIVO: RecursosRepositorio.java
 * ========================================================================
 * FUNCIÓN
 * Reconstruye los recursos de una configuración: método de asignación, impresoras con sus
 * capacidades y formatos, y servicios habilitados por sucursal.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] RecursosRepositorio(JdbcTemplate jdbc)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] Recursos leer(long version)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - RecursosRepositorio (clase).
 * - RecursosRepositorio.Impresora (record).
 * - RecursosRepositorio.Recursos (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.RecursosConfiguracionController.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RecursosRepositorio {
    private final JdbcTemplate jdbc;
    public RecursosRepositorio(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Impresora(UUID codigoPublico,UUID sucursal,String nombre,List<UUID> formatos,boolean admiteColor,boolean admiteDobleFaz,
                            int capacidadHojas,String estado,Instant retiradaEn,String motivoRetiro) {}
    public record Recursos(MetodoAsignacion metodoAsignacion,List<Impresora> impresoras,List<ServiciosSucursal> serviciosPorSucursal) {}
    public Recursos leer(long version) {
        var metodo=jdbc.query("SELECT metodo_asignacion FROM lamontana.configuracion_recursos WHERE id_configuracion_version=?",(rs,row)->MetodoAsignacion.valueOf(rs.getString(1)),version);
        var impresoras=jdbc.query("""
                SELECT i.id_impresora,i.codigo_publico,s.codigo_publico AS sucursal,c.nombre,c.admite_color,c.admite_doble_faz,c.capacidad_maxima_hojas,c.estado,c.retirada_en,c.motivo_retiro
                FROM lamontana.configuracion_impresora c JOIN lamontana.impresora i USING(id_impresora) JOIN lamontana.sucursal s USING(id_sucursal)
                WHERE c.id_configuracion_version=? ORDER BY s.codigo,lower(c.nombre),i.codigo_publico
                """,(rs,row)->new Impresora(rs.getObject("codigo_publico",UUID.class),rs.getObject("sucursal",UUID.class),rs.getString("nombre"),
                        jdbc.query("SELECT f.codigo_publico FROM lamontana.configuracion_impresora_formato cf JOIN lamontana.formato f USING(id_formato) WHERE cf.id_configuracion_version=? AND cf.id_impresora=? ORDER BY f.codigo",(f,n)->f.getObject(1,UUID.class),version,rs.getLong("id_impresora")),
                        rs.getBoolean("admite_color"),rs.getBoolean("admite_doble_faz"),rs.getInt("capacidad_maxima_hojas"),rs.getString("estado"),
                        rs.getTimestamp("retirada_en")==null?null:rs.getTimestamp("retirada_en").toInstant(),rs.getString("motivo_retiro")),version);
        var servicios=jdbc.query("""
                SELECT DISTINCT s.id_sucursal,s.codigo_publico,s.codigo FROM lamontana.sucursal_servicio ss JOIN lamontana.sucursal s USING(id_sucursal)
                WHERE ss.id_configuracion_version=? ORDER BY s.codigo
                """,(rs,row)->new ServiciosSucursal(rs.getObject("codigo_publico",UUID.class),
                        jdbc.query("SELECT s.codigo_publico FROM lamontana.sucursal_servicio ss JOIN lamontana.servicio s USING(id_servicio) WHERE ss.id_configuracion_version=? AND ss.id_sucursal=? ORDER BY s.codigo",(s,n)->s.getObject(1,UUID.class),version,rs.getLong("id_sucursal"))),version);
        return new Recursos(metodo.isEmpty()?null:metodo.get(0),impresoras,servicios);
    }
}
