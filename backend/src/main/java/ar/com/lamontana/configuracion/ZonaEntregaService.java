//#region ENCABEZADO · ZonaEntregaService.java
/*
 * ========================================================================
 * ARCHIVO: ZonaEntregaService.java
 * ========================================================================
 * FUNCIÓN
 * Valida y guarda zonas de envío versionadas, con territorios normalizados, costos y franjas.
 * Aplica control de edición, exclusividad de cobertura e idempotencia.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] ZonaEntregaService(JdbcTemplate jdbc, ConfiguracionService configuracion)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] ConfiguracionService.Borrador crear(UUID codigo, CrearZona in, String correo)
 * - [public] ConfiguracionService.Borrador editar(UUID codigo, UUID publico, EditarZona in, String
 *   correo)
 * - [private] List<TerritorioEntrega> validar(long config, Long zona, Definicion in)
 * - [private] void guardar(long config, long zona, Definicion in, List<TerritorioEntrega>
 *   territorios)
 * - [private] ConfiguracionService.Borrador terminar(UUID codigo, ConfiguracionService.Borrador b,
 *   UUID operacion, String tipo, long actor, String huella, String evento, UUID zona)
 * - [private] ConfiguracionService.Borrador editable(UUID codigo, Long version)
 * - [private] long id(UUID codigo)
 * - [private] void exigir(boolean condicion, String mensaje)
 *   Rechaza la operación si no se cumple la condición indicada.
 * - [private] ResponseStatusException conflicto(String mensaje)
 *   Construye un error HTTP controlado.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ZonaEntregaService (clase).
 * - ZonaEntregaService.Destino (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.ZonaEntregaController.*;
import java.math.BigDecimal;
import java.sql.Time;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ZonaEntregaService {
    private final JdbcTemplate jdbc;private final ConfiguracionService configuracion;
    public ZonaEntregaService(JdbcTemplate jdbc,ConfiguracionService configuracion){this.jdbc=jdbc;this.configuracion=configuracion;}
    private record Destino(UUID zona,Object solicitud){}
    @Transactional public ConfiguracionService.Borrador crear(UUID codigo,CrearZona in,String correo){
        configuracion.bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(in);
        var replay=configuracion.reintento(in.operacion(),"ALTA_ZONA",codigo,actor,huella);if(replay!=null)return replay;
        var b=editable(codigo,in.version());long config=id(codigo);String estable=in.codigo()==null?"":in.codigo().strip().toUpperCase(Locale.ROOT);
        exigir(estable.matches("[A-Z0-9_-]{1,40}"),"Ingresá un código de 1 a 40 letras, números, guiones o guiones bajos.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.zona_entrega WHERE codigo=?)",Boolean.class,estable)))throw conflicto("Ya existe una identidad de zona con ese código. Los códigos históricos no se reutilizan.");
        var territorios=validar(config,null,in);UUID publico=UUID.randomUUID();long zona=jdbc.queryForObject("INSERT INTO lamontana.zona_entrega(codigo_publico,codigo,id_usuario_creador) VALUES (?,?,?) RETURNING id_zona_entrega",Long.class,publico,estable,actor);
        guardar(config,zona,in,territorios);return terminar(codigo,b,in.operacion(),"ALTA_ZONA",actor,huella,"ZONA_CREADA",publico);
    }
    @Transactional public ConfiguracionService.Borrador editar(UUID codigo,UUID publico,EditarZona in,String correo){
        configuracion.bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(new Destino(publico,in));
        var replay=configuracion.reintento(in.operacion(),"EDITAR_ZONA",codigo,actor,huella);if(replay!=null)return replay;
        var b=editable(codigo,in.version());long config=id(codigo);var ids=jdbc.query("SELECT z.id_zona_entrega FROM lamontana.zona_entrega z JOIN lamontana.configuracion_zona_entrega c USING(id_zona_entrega) WHERE c.id_configuracion_version=? AND z.codigo_publico=?",(rs,n)->rs.getLong(1),config,publico);
        if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"La zona no pertenece a este borrador.");long zona=ids.get(0);
        var territorios=validar(config,zona,in);guardar(config,zona,in,territorios);return terminar(codigo,b,in.operacion(),"EDITAR_ZONA",actor,huella,"ZONA_EDITADA",publico);
    }
    private List<TerritorioEntrega> validar(long config,Long zona,Definicion in){
        exigir(in.nombre()!=null&&!in.nombre().isBlank()&&in.nombre().length()<=140,"Completá el nombre de la zona con hasta 140 caracteres.");
        exigir(in.descripcion()==null||in.descripcion().length()<=2000,"La descripción admite hasta 2000 caracteres.");
        exigir(in.zonaHoraria()!=null&&ZoneId.getAvailableZoneIds().contains(in.zonaHoraria()),"Elegí una zona horaria IANA válida para las franjas.");
        exigir(in.costo()!=null&&in.costo().matches("[0-9]{1,12}(\\.[0-9]{1,2})?"),"Ingresá costo ARS no negativo, con hasta dos decimales; cero debe ser explícito.");
        exigir(in.habilitada()!=null,"Indicá el estado de la zona en el borrador.");
        exigir(in.territorios()!=null&&in.territorios().size()<=100&&in.territorios().stream().allMatch(Objects::nonNull),"Ingresá hasta 100 combinaciones territoriales válidas.");
        var territorios=in.territorios().stream().map(TerritorioEntrega::normalizado).toList();exigir(new HashSet<>(territorios).size()==territorios.size(),"La cobertura no puede repetir código postal, localidad y provincia.");
        if(in.habilitada())for(var t:territorios){
            var otras=jdbc.query("SELECT c.nombre FROM lamontana.zona_entrega_codigo_postal t JOIN lamontana.configuracion_zona_entrega c USING(id_configuracion_zona_entrega) WHERE t.id_configuracion_version=? AND t.habilitada AND t.codigo_postal=? AND t.localidad=? AND t.provincia=? AND c.id_zona_entrega<>?",(rs,n)->rs.getString(1),config,t.codigoPostal(),t.localidad(),t.provincia(),zona==null?-1:zona);
            exigir(otras.isEmpty(),"La cobertura se superpone con otra zona habilitada. Quitá la combinación o deshabilitá la otra zona antes de guardarla.");
        }
        FranjasEntrega.validar(in.franjas());return territorios;
    }
    private void guardar(long config,long zona,Definicion in,List<TerritorioEntrega> territorios){
        // Reemplazo sólo en preparación; no existen reservas para esta definición aún.
        jdbc.update("DELETE FROM lamontana.configuracion_zona_entrega WHERE id_configuracion_version=? AND id_zona_entrega=?",config,zona);
        long relacion=jdbc.queryForObject("INSERT INTO lamontana.configuracion_zona_entrega(id_configuracion_version,id_zona_entrega,nombre,descripcion,zona_horaria,costo,habilitada) VALUES (?,?,?,?,?,?,?) RETURNING id_configuracion_zona_entrega",Long.class,config,zona,in.nombre().strip(),in.descripcion()==null?null:in.descripcion().strip(),in.zonaHoraria(),new BigDecimal(in.costo()),in.habilitada());
        for(var t:territorios)jdbc.update("INSERT INTO lamontana.zona_entrega_codigo_postal(id_configuracion_zona_entrega,id_configuracion_version,habilitada,codigo_postal,localidad,provincia) VALUES (?,?,?,?,?,?)",relacion,config,in.habilitada(),t.codigoPostal(),t.localidad(),t.provincia());
        for(var f:in.franjas())jdbc.update("INSERT INTO lamontana.franja_entrega(id_configuracion_zona_entrega,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada) VALUES (?,?,?,?,?,?)",relacion,f.dia(),Time.valueOf(LocalTime.parse(f.apertura())),Time.valueOf(LocalTime.parse(f.cierre())),f.capacidadPedidos(),f.habilitada());
    }
    private ConfiguracionService.Borrador terminar(UUID codigo,ConfiguracionService.Borrador b,UUID operacion,String tipo,long actor,String huella,String evento,UUID zona){long id=id(codigo);jdbc.update("UPDATE lamontana.configuracion_version SET version=version+1,fecha_actualizacion=clock_timestamp() WHERE id_configuracion_version=?",id);configuracion.registrar(operacion,tipo,id,actor,huella,evento,b.version()+1,zona,null);jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",id);return configuracion.cargar(codigo);}
    private ConfiguracionService.Borrador editable(UUID codigo,Long version){var b=configuracion.cargar(codigo);if(!b.estado().equals("EN_PREPARACION")||version==null||b.version()!=version)throw conflicto("El borrador cambió o ya no se puede editar. Consultá su versión actual.");if(b.modelo()==null)throw conflicto("Seleccioná primero el modelo operativo.");return b;}
    private long id(UUID codigo){return jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?",Long.class,codigo);}
    private void exigir(boolean condicion,String mensaje){if(!condicion)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
    private ResponseStatusException conflicto(String mensaje){return new ResponseStatusException(HttpStatus.CONFLICT,mensaje);}
}
