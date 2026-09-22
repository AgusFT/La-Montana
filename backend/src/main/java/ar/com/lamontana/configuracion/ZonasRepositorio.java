package ar.com.lamontana.configuracion;

import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ar.com.lamontana.configuracion.PuntoEntregaController.Franja;

@Repository
public class ZonasRepositorio {
    private static final DateTimeFormatter HORA=DateTimeFormatter.ofPattern("HH:mm");
    private final JdbcTemplate jdbc;
    public ZonasRepositorio(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Zona(UUID codigoPublico,String codigo,String nombre,String descripcion,String zonaHoraria,String costo,boolean habilitada,List<TerritorioEntrega> territorios,List<Franja> franjas) {
        public boolean utilizable(){return habilitada&&!territorios.isEmpty()&&franjas.stream().anyMatch(f->f.habilitada()&&f.capacidadPedidos()>0);}
    }
    public List<Zona> leer(long config){
        return jdbc.query("SELECT z.codigo_publico,z.codigo,c.* FROM lamontana.configuracion_zona_entrega c JOIN lamontana.zona_entrega z USING(id_zona_entrega) WHERE c.id_configuracion_version=? ORDER BY z.codigo",(rs,n)->{
            long id=rs.getLong("id_configuracion_zona_entrega");
            var territorios=jdbc.query("SELECT codigo_postal,localidad,provincia FROM lamontana.zona_entrega_codigo_postal WHERE id_configuracion_zona_entrega=? ORDER BY provincia,localidad,codigo_postal",(t,i)->new TerritorioEntrega(t.getString(1),t.getString(2),t.getString(3)),id);
            var franjas=jdbc.query("SELECT dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada FROM lamontana.franja_entrega WHERE id_configuracion_zona_entrega=? ORDER BY dia_semana,hora_desde",(f,i)->new Franja(f.getInt(1),HORA.format(f.getTime(2).toLocalTime()),HORA.format(f.getTime(3).toLocalTime()),f.getInt(4),f.getBoolean(5)),id);
            return new Zona(rs.getObject("codigo_publico",UUID.class),rs.getString("codigo"),rs.getString("nombre"),rs.getString("descripcion"),rs.getString("zona_horaria"),rs.getBigDecimal("costo").setScale(2).toPlainString(),rs.getBoolean("habilitada"),territorios,franjas);
        },config);
    }
}
