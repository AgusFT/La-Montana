package ar.com.lamontana.configuracion;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

/** Disponibilidad efectiva: la pausa de un origen nunca modifica la decisión manual del punto. */
public final class DisponibilidadDestinos {
    public static final String FUERA_DE_SERVICIO="Momentáneamente fuera de servicio";
    public record Origen(UUID codigoPublico,String nombre,boolean disponible) {}
    public record Destino(EntregaConfiguracionController.Modalidad modalidad,UUID codigoPublico,String nombre,
                          String direccion,boolean disponible,String motivo,List<Origen> sucursales) {}
    private final JdbcTemplate jdbc;
    public DisponibilidadDestinos(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public List<Origen> origenes(UUID configuracion,UUID punto) {
        return jdbc.query("""
            SELECT s.codigo_publico,s.nombre,
                   s.estado='ACTIVA' AND coalesce(d.estado,'ABIERTA')='ABIERTA' AS disponible
            FROM lamontana.configuracion_punto_entrega r
            JOIN lamontana.configuracion_version v USING(id_configuracion_version)
            JOIN lamontana.punto_entrega p USING(id_punto_entrega)
            JOIN lamontana.sucursal s USING(id_sucursal)
            LEFT JOIN lamontana.disponibilidad_sucursal d USING(id_sucursal)
            WHERE v.codigo_publico=? AND p.codigo_publico=? AND r.habilitado
              AND EXISTS(SELECT 1 FROM lamontana.franja_entrega f
                         WHERE f.id_configuracion_punto_entrega=r.id_configuracion_punto_entrega
                           AND f.habilitada AND f.capacidad_pedidos>0)
            ORDER BY r.prioridad,s.nombre
            """,(r,n)->new Origen(r.getObject(1,UUID.class),r.getString(2),r.getBoolean(3)),configuracion,punto);
    }

    public List<Destino> catalogo(ConfiguracionService.Borrador configuracion,Set<UUID> sucursalesDisponibles,UUID mostrador) {
        if(configuracion==null)return List.of();
        var entrega=configuracion.entrega();var result=new ArrayList<Destino>();
        if(entrega.modalidades().contains(EntregaConfiguracionController.Modalidad.RETIRO_SUCURSAL)) {
            var conRetiro=new HashSet<UUID>();entrega.horariosPorSucursal().stream().filter(EntregaRepositorio.Horario::retiroUtilizable).forEach(h->conRetiro.add(h.sucursal()));
            jdbc.query("SELECT codigo_publico,nombre,calle||' '||numero||', '||localidad||', '||provincia FROM lamontana.sucursal WHERE estado='ACTIVA' ORDER BY nombre",r->{
                UUID id=r.getObject(1,UUID.class);if(!conRetiro.contains(id)||mostrador!=null&&!mostrador.equals(id))return;
                boolean disponible=sucursalesDisponibles.contains(id);
                result.add(new Destino(EntregaConfiguracionController.Modalidad.RETIRO_SUCURSAL,id,r.getString(2),r.getString(3),disponible,disponible?null:FUERA_DE_SERVICIO,List.of()));
            });
        }
        if(entrega.modalidades().contains(EntregaConfiguracionController.Modalidad.RETIRO_PUNTO_ENTREGA)) {
            var habilitados=new HashSet<UUID>();jdbc.query("SELECT p.codigo_publico FROM lamontana.disponibilidad_punto_entrega d JOIN lamontana.punto_entrega p USING(id_punto_entrega) WHERE d.estado='HABILITADO'",r->{habilitados.add(r.getObject(1,UUID.class));});
            for(var p:entrega.puntos()) {
                var origenes=origenes(configuracion.codigoPublico(),p.codigoPublico()).stream()
                    .filter(s->mostrador==null||mostrador.equals(s.codigoPublico()))
                    .map(s->new Origen(s.codigoPublico(),s.nombre(),s.disponible()&&sucursalesDisponibles.contains(s.codigoPublico()))).toList();
                if(origenes.isEmpty())continue;
                boolean disponible=habilitados.contains(p.codigoPublico())&&origenes.stream().anyMatch(Origen::disponible);
                result.add(new Destino(EntregaConfiguracionController.Modalidad.RETIRO_PUNTO_ENTREGA,p.codigoPublico(),p.nombre(),p.calle()+" "+p.numero()+", "+p.localidad()+", "+p.provincia(),disponible,disponible?null:FUERA_DE_SERVICIO,origenes));
            }
        }
        return List.copyOf(result);
    }
}
