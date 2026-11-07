package ar.com.lamontana.configuracion;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Pausa de pedidos nuevos; no modifica identidad, permisos ni reservas confirmadas. */
@Service
public class DisponibilidadSucursalService {
    private final JdbcTemplate jdbc;
    private final ConfiguracionService configuracion;
    public DisponibilidadSucursalService(JdbcTemplate jdbc,ConfiguracionService configuracion) { this.jdbc=jdbc;this.configuracion=configuracion; }
    public record Disponibilidad(UUID sucursal,String estado,long version,Instant actualizadaEn,String actor) {}
    public record Sucursal(UUID codigoPublico,String nombre,String direccion,String estado,long version,Instant actualizadaEn,String actor) {}
    private record Anterior(long sucursal,long actor,String huella) {}
    private long administrador(String correo) {
        var ids=jdbc.query("SELECT u.id_usuario FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.codigo='ADMIN_ADMIN' AND r.activo",(r,n)->r.getLong(1),correo);
        if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Sólo un administrador activo puede gestionar las pausas de sucursales.");
        return ids.get(0);
    }
    @Transactional(readOnly=true)
    public List<Sucursal> listar(String correo) {
        administrador(correo);
        return jdbc.query("SELECT s.id_sucursal,s.codigo_publico,s.nombre,s.calle||' '||s.numero||', '||s.localidad||', '||s.provincia FROM lamontana.sucursal s WHERE s.estado='ACTIVA' ORDER BY s.nombre",(r,n)-> {
            var e=estado(r.getLong(1),r.getObject(2,UUID.class));
            return new Sucursal(e.sucursal(),r.getString(3),r.getString(4),e.estado(),e.version(),e.actualizadaEn(),e.actor());
        });
    }
    @Transactional
    public Disponibilidad cambiar(UUID codigo,DisponibilidadSucursalController.Cambiar in,String correo) {
        configuracion.bloquear();long actor=administrador(correo);
        if(in.operacion()==null||in.version()==null||in.version()<0||in.estado()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indicá operación, versión y estado de atención.");
        var ids=jdbc.query("SELECT id_sucursal FROM lamontana.sucursal WHERE codigo_publico=?",(r,n)->r.getLong(1),codigo);
        if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"La sucursal no existe.");
        long id=ids.get(0);String hash=configuracion.huella(List.of(codigo,in));
        var prev=jdbc.query("SELECT id_sucursal,id_actor,huella FROM lamontana.evento_disponibilidad_sucursal WHERE id_operacion=?",(r,n)->new Anterior(r.getLong(1),r.getLong(2),r.getString(3)),in.operacion());
        if(!prev.isEmpty()) {
            var p=prev.get(0);if(p.sucursal()!=id||p.actor()!=actor||!p.huella().equals(hash))throw conflicto("La operación ya se usó con otro destino o contenido.");
            return estado(id,codigo);
        }
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT estado='ACTIVA' FROM lamontana.sucursal WHERE id_sucursal=?",Boolean.class,id)))throw conflicto("La sucursal fue desactivada. Actualizá la lista.");
        var old=estado(id,codigo);
        if(old.version()!=in.version())throw conflicto("La disponibilidad cambió. Actualizá el estado y elegí nuevamente la acción.");
        if(old.estado().equals(in.estado().name()))throw conflicto("La sucursal ya tiene ese estado.");
        jdbc.update("INSERT INTO lamontana.disponibilidad_sucursal(id_sucursal,estado,version,id_actor) VALUES (?,?,?,?) ON CONFLICT(id_sucursal) DO UPDATE SET estado=excluded.estado,version=excluded.version,id_actor=excluded.id_actor,actualizada_en=clock_timestamp()",id,in.estado().name(),old.version()+1,actor);
        jdbc.update("INSERT INTO lamontana.evento_disponibilidad_sucursal(id_operacion,id_sucursal,id_actor,huella,estado_anterior,estado_nuevo,version_anterior,version_nueva) VALUES (?,?,?,?,?,?,?,?)",in.operacion(),id,actor,hash,old.estado(),in.estado().name(),old.version(),old.version()+1);
        return estado(id,codigo);
    }
    private Disponibilidad estado(long id,UUID codigo) {
        return jdbc.query("SELECT d.estado,d.version,d.actualizada_en,u.nombre||' '||u.apellido FROM lamontana.disponibilidad_sucursal d JOIN lamontana.usuario u ON u.id_usuario=d.id_actor WHERE d.id_sucursal=?",(r,n)->new Disponibilidad(codigo,r.getString(1),r.getLong(2),r.getTimestamp(3).toInstant(),r.getString(4)),id)
            .stream().findFirst().orElse(new Disponibilidad(codigo,"ABIERTA",0,null,null));
    }
    /** Dentro del bloqueo comercial, antes de cotizar o crear una reserva nueva. */
    public static void exigirAtencion(JdbcTemplate jdbc,UUID sucursal) {
        if(sucursal!=null&&Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.disponibilidad_sucursal d JOIN lamontana.sucursal s USING(id_sucursal) WHERE s.codigo_publico=? AND d.estado='PAUSADA')",Boolean.class,sucursal)))
            throw conflicto("La sucursal está pausada para pedidos nuevos. Elegí otra ubicación o consultá con la imprenta.");
    }
    private static ResponseStatusException conflicto(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
