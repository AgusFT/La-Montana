//#region ENCABEZADO · DisponibilidadPuntoService.java
/*
 * ========================================================================
 * ARCHIVO: DisponibilidadPuntoService.java
 * ========================================================================
 * FUNCIÓN
 * Gestiona la disponibilidad cotidiana de los puntos de entrega y su auditoría. Verifica
 * identidad, contenido y reintentos de cada cambio sin modificar las versiones de configuración.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] DisponibilidadPuntoService(JdbcTemplate jdbc, ConfiguracionService configuracion)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] Panel listar(String correo)
 * - [public] Disponibilidad cambiar(UUID codigo, DisponibilidadPuntoController.Cambiar input,
 *   String correo)
 * - [private] Disponibilidad estado(long id, UUID codigo)
 * - [private] ResponseStatusException conflicto(String message)
 *   Construye un error HTTP controlado.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - DisponibilidadPuntoService (clase).
 * - DisponibilidadPuntoService.Disponibilidad (record).
 * - DisponibilidadPuntoService.Punto (record).
 * - DisponibilidadPuntoService.Panel (record).
 * - DisponibilidadPuntoService.Comprobante (record).
 * - DisponibilidadPuntoService.Destino (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DisponibilidadPuntoService {
    private final JdbcTemplate jdbc;
    private final ConfiguracionService configuracion;
    public DisponibilidadPuntoService(JdbcTemplate jdbc,ConfiguracionService configuracion){this.jdbc=jdbc;this.configuracion=configuracion;}
    public record Disponibilidad(UUID punto,String estado,long version,Instant actualizadaEn,String actor){}
    public record Punto(UUID codigoPublico,String codigo,String nombre,String direccion,String zonaHoraria,Disponibilidad disponibilidad){}
    public record Panel(String contexto,UUID borrador,List<Punto> puntos,long deshabilitados){}
    private record Comprobante(long punto,long actor,String huella){}
    private record Destino(UUID punto,DisponibilidadPuntoController.Cambiar comando){}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Panel listar(String correo){
        configuracion.propietario(correo);
        var borradores=jdbc.query("SELECT codigo_publico FROM lamontana.configuracion_version WHERE estado='EN_PREPARACION'",(rs,row)->rs.getObject(1,UUID.class));
        boolean activa=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE estado='ACTIVA')",Boolean.class));
        var puntos=jdbc.query("""
                SELECT DISTINCT ON (p.id_punto_entrega) p.id_punto_entrega,p.codigo_publico,p.codigo,d.nombre,
                       d.calle||' '||d.numero||' · '||d.localidad||', '||d.provincia AS direccion,d.zona_horaria
                FROM lamontana.punto_entrega p JOIN lamontana.configuracion_definicion_punto d USING(id_punto_entrega)
                JOIN lamontana.configuracion_version c USING(id_configuracion_version)
                WHERE c.estado IN ('ACTIVA','EN_PREPARACION','PROGRAMADA') ORDER BY p.id_punto_entrega,(c.estado='ACTIVA') DESC,c.numero_version DESC
                """,(rs,row)->new Punto(rs.getObject("codigo_publico",UUID.class),rs.getString("codigo"),rs.getString("nombre"),rs.getString("direccion"),rs.getString("zona_horaria"),estado(rs.getLong("id_punto_entrega"),rs.getObject("codigo_publico",UUID.class))),new Object[0]).stream().sorted(Comparator.comparing(Punto::codigo)).toList();
        return new Panel(activa?"CONFIGURACION_ACTIVA":"SIN_CONFIGURACION_ACTIVA",borradores.isEmpty()?null:borradores.get(0),puntos,puntos.stream().filter(p->p.disponibilidad().estado().equals("DESHABILITADO")).count());
    }

    @Transactional
    public Disponibilidad cambiar(UUID codigo,DisponibilidadPuntoController.Cambiar input,String correo){
        // Compartido con alta/edición/cancelación para no operar sobre una definición retirada en paralelo.
        configuracion.bloquear();long actor=configuracion.propietario(correo);
        if(input.operacion()==null||input.version()==null||input.version()<0||input.estado()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Elegí un estado e indicá la edición de disponibilidad.");
        String huella=configuracion.huella(new Destino(codigo,input));
        var ids=jdbc.query("SELECT id_punto_entrega FROM lamontana.punto_entrega WHERE codigo_publico=?",(rs,row)->rs.getLong(1),codigo);
        if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"El punto no existe.");long id=ids.get(0);
        var anteriores=jdbc.query("SELECT id_punto_entrega,id_actor,hash_solicitud FROM lamontana.evento_disponibilidad_punto WHERE id_operacion=?",(rs,row)->new Comprobante(rs.getLong(1),rs.getLong(2),rs.getString(3)),input.operacion());
        if(!anteriores.isEmpty()){
            var r=anteriores.get(0);if(r.punto()!=id||r.actor()!=actor||!r.huella().equals(huella))throw conflicto("Esa operación ya se utilizó con otro destino o contenido.");
            return estado(id,codigo); // Recupera el estado actual sin volver a aplicar un comando anterior.
        }
        boolean pertenece=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_definicion_punto d JOIN lamontana.configuracion_version c USING(id_configuracion_version) WHERE d.id_punto_entrega=? AND c.estado IN ('ACTIVA','EN_PREPARACION','PROGRAMADA'))",Boolean.class,id));
        if(!pertenece)throw conflicto("El punto ya no pertenece a la configuración activa ni al borrador actual. Actualizá el panel antes de continuar.");
        var actual=estado(id,codigo);if(actual.version()!=input.version())throw conflicto("La disponibilidad cambió. Actualizá el estado y elegí nuevamente la acción.");
        if(actual.estado().equals(input.estado().name()))throw conflicto("El punto ya tiene ese estado. No se registró otro cambio.");
        jdbc.update("""
                INSERT INTO lamontana.disponibilidad_punto_entrega(id_punto_entrega,estado,version,id_actor,fecha_actualizacion)
                VALUES (?,?,?,?,clock_timestamp()) ON CONFLICT(id_punto_entrega) DO UPDATE SET
                estado=excluded.estado,version=excluded.version,id_actor=excluded.id_actor,fecha_actualizacion=excluded.fecha_actualizacion
                """,id,input.estado().name(),actual.version()+1,actor);
        jdbc.update("INSERT INTO lamontana.evento_disponibilidad_punto(id_operacion,id_punto_entrega,id_actor,hash_solicitud,estado_anterior,estado_nuevo,version_anterior,version_nueva) VALUES (?,?,?,?,?,?,?,?)",input.operacion(),id,actor,huella,actual.version()==0?null:actual.estado(),input.estado().name(),actual.version(),actual.version()+1);
        return estado(id,codigo);
    }
    private Disponibilidad estado(long id,UUID codigo){
        var rows=jdbc.query("SELECT d.estado,d.version,d.fecha_actualizacion,u.nombre||' '||u.apellido AS actor FROM lamontana.disponibilidad_punto_entrega d JOIN lamontana.usuario u ON u.id_usuario=d.id_actor WHERE d.id_punto_entrega=?",(rs,row)->new Disponibilidad(codigo,rs.getString("estado"),rs.getLong("version"),rs.getTimestamp("fecha_actualizacion").toInstant(),rs.getString("actor")),id);
        return rows.isEmpty()?new Disponibilidad(codigo,"SIN_DEFINIR",0,null,null):rows.get(0);
    }
    private ResponseStatusException conflicto(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
