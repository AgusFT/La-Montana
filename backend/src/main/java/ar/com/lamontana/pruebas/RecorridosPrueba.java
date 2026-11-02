package ar.com.lamontana.pruebas;

import static ar.com.lamontana.pruebas.DatosPruebaConfiguration.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

/** Riders, custodia y recorridos ficticios sobre los movimientos logísticos ya generados. */
final class RecorridosPrueba {
    private static final String HUELLA="0".repeat(64);
    private final JdbcTemplate jdbc;
    private record Rider(long id,UUID codigo,String correo,String nombre) {}
    private record Pedido(long id,UUID codigo,long version,Instant salida,Instant ultimo,boolean activo) {}
    private record Grupo(Rider rider,boolean activo,List<Pedido> pedidos) {}

    RecorridosPrueba(JdbcTemplate jdbc){this.jdbc=jdbc;}

    void cargar(String hash,Instant inicio){
        var riders=List.of(
                rider("rider.oeste@prueba.local","Martina","Ruta",hash,inicio),
                rider("rider.centro@prueba.local","Nicolás","Reparto",hash,inicio));
        jdbc.update("UPDATE lamontana.cuenta_repartidor SET activa=false,actualizado_en=? WHERE id_usuario=(SELECT id_usuario FROM lamontana.usuario WHERE correo='produccion@prueba.local')",ts(Instant.now()));
        var orders=jdbc.query("""
                SELECT p.id_pedido,p.codigo_publico,p.version,s.fecha,
                       coalesce(
                         (SELECT max(h.fecha) FROM lamontana.historial_estado_pedido h WHERE h.id_pedido=p.id_pedido AND h.accion='LLEGAR_PUNTO'),
                         (SELECT max(h.fecha) FROM lamontana.historial_estado_pedido h WHERE h.id_pedido=p.id_pedido AND h.accion='ENTREGAR'),
                         (SELECT max(h.fecha) FROM lamontana.historial_estado_pedido h WHERE h.id_pedido=p.id_pedido),s.fecha) ultimo,
                       lamontana.estado_logistico(p.id_pedido) IN ('EN_VIAJE','CLIENTE_AUSENTE') activo
                FROM lamontana.pedido p
                JOIN LATERAL (SELECT h.fecha FROM lamontana.historial_estado_pedido h WHERE h.id_pedido=p.id_pedido AND h.accion='SALIR_REPARTO' ORDER BY h.id_evento LIMIT 1) s ON true
                WHERE NOT EXISTS(SELECT 1 FROM lamontana.recorrido_reparto_pedido rp WHERE rp.id_pedido=p.id_pedido)
                ORDER BY s.fecha,p.id_pedido
                """,(rs,row)->new Pedido(rs.getLong(1),rs.getObject(2,UUID.class),rs.getLong(3),rs.getTimestamp(4).toInstant(),rs.getTimestamp(5).toInstant(),rs.getBoolean(6)));
        var groups=new LinkedHashMap<String,Grupo>();int activeIndex=0;
        for(var order:orders){
            LocalDate day=order.salida().atZone(ZONA).toLocalDate();
            int riderIndex=order.activo()?activeIndex++%riders.size():Math.floorMod(day.getDayOfYear(),riders.size());
            Rider rider=riders.get(riderIndex);String key=order.activo()?"ACTIVO-"+rider.id():day+"-"+rider.id();
            groups.computeIfAbsent(key,k->new Grupo(rider,order.activo(),new ArrayList<>())).pedidos().add(order);
        }
        for(var group:groups.values())for(int from=0;from<group.pedidos().size();from+=25)
            recorrido(group.rider(),group.activo(),group.pedidos().subList(from,Math.min(from+25,group.pedidos().size())));
    }

    private Rider rider(String correo,String nombre,String apellido,String hash,Instant inicio){
        var existing=jdbc.query("SELECT id_usuario,codigo_publico,nombre||' '||apellido FROM lamontana.usuario WHERE correo=?",(rs,row)->new Rider(rs.getLong(1),rs.getObject(2,UUID.class),correo,rs.getString(3)),correo);
        Rider rider;
        if(existing.isEmpty()){
            UUID code=UUID.randomUUID();long id=jdbc.queryForObject("""
                    INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado,correo_verificado_en,debe_cambiar_contrasena,fecha_alta)
                    SELECT ?,id_rol,?,?,?,?,'ACTIVO',?,false,? FROM lamontana.rol WHERE codigo='EMPLEADO' RETURNING id_usuario
                    """,Long.class,code,correo,hash,nombre,apellido,ts(inicio),ts(inicio));
            rider=new Rider(id,code,correo,nombre+" "+apellido);
            jdbc.update("INSERT INTO lamontana.evento_organizacion(id_usuario_actor,tipo,codigo_publico_objeto,version,fecha) SELECT id_usuario,'ALTA_EMPLEADO',?,0,? FROM lamontana.usuario WHERE correo=?",code,ts(inicio),ADMIN);
        }else rider=existing.get(0);
        jdbc.update("INSERT INTO lamontana.usuario_sucursal(id_usuario,id_sucursal,fecha_desde) SELECT ?,id_sucursal,? FROM lamontana.sucursal s WHERE s.estado='ACTIVA' AND NOT EXISTS(SELECT 1 FROM lamontana.usuario_sucursal us WHERE us.id_usuario=? AND us.id_sucursal=s.id_sucursal AND us.activa)",rider.id(),ts(inicio),rider.id());
        jdbc.update("INSERT INTO lamontana.cuenta_repartidor(id_usuario,activa,creado_en,actualizado_en) VALUES (?,true,?,?) ON CONFLICT(id_usuario) DO UPDATE SET activa=true,actualizado_en=excluded.actualizado_en",rider.id(),ts(inicio),ts(Instant.now()));
        return rider;
    }

    private void recorrido(Rider rider,boolean activo,List<Pedido> orders){
        UUID operation=UUID.randomUUID(),code=UUID.randomUUID();Instant assigned=orders.stream().map(Pedido::salida).min(Comparator.naturalOrder()).orElseThrow();
        long admin=jdbc.queryForObject("SELECT id_usuario FROM lamontana.usuario WHERE correo=?",Long.class,ADMIN);
        String adminName=jdbc.queryForObject("SELECT nombre||' '||apellido FROM lamontana.usuario WHERE id_usuario=?",String.class,admin);
        long route=jdbc.queryForObject("INSERT INTO lamontana.recorrido_reparto(codigo_publico,id_operacion,id_repartidor,id_actor_asignador,actor_asignador_nombre,estado,fecha_asignacion) VALUES (?,?,?,?,?,?,?) RETURNING id_recorrido",Long.class,code,operation,rider.id(),admin,adminName,activo?"EN_REPARTO":"FINALIZADO",ts(assigned));
        int sequence=0;for(var order:orders)jdbc.update("INSERT INTO lamontana.recorrido_reparto_pedido(id_recorrido,id_pedido,orden) VALUES (?,?,?)",route,order.id(),++sequence);
        jdbc.update("INSERT INTO lamontana.evento_recorrido_reparto(id_recorrido,id_actor,actor_nombre,accion,id_repartidor,fecha) VALUES (?,?,?,'ASIGNAR_E_INICIAR',?,?)",route,admin,adminName,rider.id(),ts(assigned));
        if(!activo){Instant finished=orders.stream().map(Pedido::ultimo).max(Comparator.naturalOrder()).orElse(assigned.plusSeconds(3600));jdbc.update("INSERT INTO lamontana.evento_recorrido_reparto(id_recorrido,id_actor,actor_nombre,accion,id_repartidor,fecha) VALUES (?,?,?,'FINALIZAR',?,?)",route,rider.id(),rider.nombre(),rider.id(),ts(finished));}
        String manifest=jdbc.queryForObject("SELECT jsonb_agg(jsonb_build_object('pedido',p.codigo_publico,'version',p.version) ORDER BY rp.orden)::text FROM lamontana.recorrido_reparto_pedido rp JOIN lamontana.pedido p USING(id_pedido) WHERE rp.id_recorrido=?",String.class,route);
        jdbc.update("INSERT INTO lamontana.lote_movimiento_entrega(id_operacion,id_actor,actor_nombre,huella,accion,pedidos,fecha,id_recorrido) VALUES (?,?,?,?,'SALIR_REPARTO',?::jsonb,?,?)",operation,admin,adminName,HUELLA,manifest,ts(assigned),route);
    }

    private static Timestamp ts(Instant value){return Timestamp.from(value);}
}
