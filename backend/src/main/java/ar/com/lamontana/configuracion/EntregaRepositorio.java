package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.EntregaConfiguracionController.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EntregaRepositorio {
    private static final DateTimeFormatter HORA=DateTimeFormatter.ofPattern("HH:mm");
    private final JdbcTemplate jdbc;
    private final PuntosRepositorio puntos;
    private final ZonasRepositorio zonas;
    public EntregaRepositorio(JdbcTemplate jdbc,PuntosRepositorio puntos,ZonasRepositorio zonas){this.jdbc=jdbc;this.puntos=puntos;this.zonas=zonas;}
    public record Horario(UUID sucursal,String zonaHoraria,List<Dia> dias,List<PuntoEntregaController.Franja> franjasRetiro) {
        public Horario { franjasRetiro=franjasRetiro==null?List.of():List.copyOf(franjasRetiro); }
        public Horario(UUID sucursal,String zonaHoraria,List<Dia> dias){this(sucursal,zonaHoraria,dias,List.of());}
        public boolean retiroUtilizable(){return franjasRetiro.stream().anyMatch(f->f.habilitada()&&f.capacidadPedidos()>0)&&franjasRetiro.stream().filter(f->f.habilitada()&&f.capacidadPedidos()>0).allMatch(f->dias.stream().anyMatch(d->d.dia().equals(f.dia())&&Boolean.TRUE.equals(d.habilitado())&&d.apertura()!=null&&d.cierre()!=null&&d.apertura().compareTo(f.apertura())<=0&&d.cierre().compareTo(f.cierre())>=0));}
    }
    public record Entrega(String preparacionHoras,String trasladoHoras,List<Modalidad> modalidades,List<Horario> horariosPorSucursal,List<PuntosRepositorio.Punto> puntos,List<ZonasRepositorio.Zona> zonas) {}
    public Entrega leer(long config) {
        var tiempos=jdbc.query("SELECT preparacion_horas,traslado_horas FROM lamontana.configuracion_entrega WHERE id_configuracion_version=?",(rs,row)->new Tiempos(numero(rs.getBigDecimal(1)),numero(rs.getBigDecimal(2))),config);
        var modalidades=jdbc.query("SELECT modalidad FROM lamontana.configuracion_modalidad_entrega WHERE id_configuracion_version=? ORDER BY modalidad",(rs,row)->Modalidad.valueOf(rs.getString(1)),config);
        var horarios=jdbc.query("SELECT s.id_sucursal,s.codigo_publico,c.zona_horaria FROM lamontana.configuracion_horario_sucursal c JOIN lamontana.sucursal s USING(id_sucursal) WHERE c.id_configuracion_version=? ORDER BY s.codigo",(rs,row)->new Horario(rs.getObject("codigo_publico",UUID.class),rs.getString("zona_horaria"),
                jdbc.query("SELECT dia_semana,habilitado,hora_desde,hora_hasta FROM lamontana.horario_sucursal WHERE id_configuracion_version=? AND id_sucursal=? ORDER BY dia_semana",(d,n)->new Dia(d.getInt(1),d.getObject(2,Boolean.class),d.getTime(3)==null?null:HORA.format(d.getTime(3).toLocalTime()),d.getTime(4)==null?null:HORA.format(d.getTime(4).toLocalTime())),config,rs.getLong("id_sucursal")),
                jdbc.query("SELECT dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada FROM lamontana.franja_entrega WHERE id_configuracion_retiro=? AND id_sucursal_retiro=? ORDER BY dia_semana,hora_desde",(f,n)->new PuntoEntregaController.Franja(f.getInt(1),HORA.format(f.getTime(2).toLocalTime()),HORA.format(f.getTime(3).toLocalTime()),f.getInt(4),f.getBoolean(5)),config,rs.getLong("id_sucursal"))),config);
        return new Entrega(tiempos.isEmpty()?null:tiempos.get(0).preparacion(),tiempos.isEmpty()?null:tiempos.get(0).traslado(),modalidades,horarios,puntos.leer(config),zonas.leer(config));
    }
    private record Tiempos(String preparacion,String traslado){}
    private String numero(BigDecimal valor){return valor==null?null:valor.setScale(2).toPlainString();}
}
