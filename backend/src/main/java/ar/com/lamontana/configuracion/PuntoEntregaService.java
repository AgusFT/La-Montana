package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.PuntoEntregaController.*;
import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PuntoEntregaService {
    private final JdbcTemplate jdbc;
    private final ConfiguracionService configuracion;
    public PuntoEntregaService(JdbcTemplate jdbc,ConfiguracionService configuracion){this.jdbc=jdbc;this.configuracion=configuracion;}
    private record Relacion(long sucursal,boolean habilitado,BigDecimal costo,List<Franja> franjas){}
    private record Sucursal(long id,boolean activa){}
    private record Destino(UUID punto,Object solicitud){}

    @Transactional
    public ConfiguracionService.Borrador crear(UUID codigo,CrearPunto input,String correo) {
        configuracion.bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(input);
        var repetido=configuracion.reintento(input.operacion(),"ALTA_PUNTO",codigo,actor,huella);if(repetido!=null)return repetido;
        var b=editable(codigo,input.version());long id=id(codigo);
        String estable=input.codigo()==null?"":input.codigo().strip().toUpperCase(Locale.ROOT);
        exigir(estable.matches("[A-Z0-9_-]{1,40}"),"Ingresá un código de 1 a 40 letras, números, guiones o guiones bajos.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.punto_entrega WHERE codigo=?)",Boolean.class,estable)))throw conflicto("Ya existe una identidad de punto con ese código. Los códigos históricos no se reutilizan.");
        var relaciones=validar(id,null,input);UUID publico=UUID.randomUUID();
        long punto=jdbc.queryForObject("INSERT INTO lamontana.punto_entrega(codigo_publico,codigo,id_usuario_creador) VALUES (?,?,?) RETURNING id_punto_entrega",Long.class,publico,estable,actor);
        guardarDefinicion(id,punto,input,relaciones);
        return terminar(codigo,b,input.operacion(),"ALTA_PUNTO",actor,huella,"PUNTO_CREADO",publico);
    }
    @Transactional
    public ConfiguracionService.Borrador editar(UUID codigo,UUID publico,EditarPunto input,String correo) {
        configuracion.bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(new Destino(publico,input));
        var repetido=configuracion.reintento(input.operacion(),"EDITAR_PUNTO",codigo,actor,huella);if(repetido!=null)return repetido;
        var b=editable(codigo,input.version());long id=id(codigo);
        var puntos=jdbc.query("SELECT p.id_punto_entrega FROM lamontana.punto_entrega p JOIN lamontana.configuracion_definicion_punto d USING(id_punto_entrega) WHERE d.id_configuracion_version=? AND p.codigo_publico=?",(rs,row)->rs.getLong(1),id,publico);
        if(puntos.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"El punto no pertenece a este borrador.");long punto=puntos.get(0);
        var relaciones=validar(id,punto,input);guardarDefinicion(id,punto,input,relaciones);
        return terminar(codigo,b,input.operacion(),"EDITAR_PUNTO",actor,huella,"PUNTO_EDITADO",publico);
    }
    private List<Relacion> validar(long config,Long punto,Definicion input) {
        texto(input.nombre(),160,"nombre");texto(input.calle(),160,"calle");texto(input.numero(),20,"número");texto(input.localidad(),120,"localidad");texto(input.provincia(),120,"provincia");texto(input.codigoPostal(),12,"código postal");
        exigir(input.referencias()==null||input.referencias().length()<=2000,"Las referencias admiten hasta 2000 caracteres.");
        exigir(input.zonaHoraria()!=null&&ZoneId.getAvailableZoneIds().contains(input.zonaHoraria()),"Elegí una zona horaria IANA válida, por ejemplo America/Argentina/Buenos_Aires; no un desplazamiento horario fijo.");
        exigir(input.sucursales()!=null&&input.sucursales().size()<=100,"Ingresá una lista válida de sucursales de origen.");
        var usadas=new HashSet<UUID>();var relaciones=new ArrayList<Relacion>();
        for(var r:input.sucursales()) {
            exigir(r!=null&&r.sucursal()!=null&&usadas.add(r.sucursal()),"Cada sucursal de origen puede aparecer una sola vez en el punto.");
            exigir(r.habilitado()!=null,"Indicá explícitamente si la relación está habilitada en el borrador.");
            exigir(r.costo()!=null&&r.costo().matches("[0-9]{1,12}(\\.[0-9]{1,2})?"),"Ingresá el costo ARS sin negativos ni exponentes, con hasta dos decimales; cero debe ser explícito.");
            var filas=jdbc.query("SELECT id_sucursal,estado='ACTIVA' FROM lamontana.sucursal WHERE codigo_publico=? FOR SHARE",(rs,row)->new Sucursal(rs.getLong(1),rs.getBoolean(2)),r.sucursal());
            exigir(!filas.isEmpty(),"La sucursal de origen no existe.");var sucursal=filas.get(0);
            boolean habilitadaAntes=punto!=null&&Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_punto_entrega WHERE id_configuracion_version=? AND id_punto_entrega=? AND id_sucursal=? AND habilitado)",Boolean.class,config,punto,sucursal.id()));
            if(r.habilitado()&&!sucursal.activa()&&!habilitadaAntes)throw conflicto("No se puede habilitar una nueva relación con una sucursal desactivada.");
            FranjasEntrega.validar(r.franjas());relaciones.add(new Relacion(sucursal.id(),r.habilitado(),new BigDecimal(r.costo()),r.franjas()));
        }
        return relaciones;
    }
    private void guardarDefinicion(long config,long punto,Definicion in,List<Relacion> relaciones) {
        jdbc.update("""
                INSERT INTO lamontana.configuracion_definicion_punto(id_configuracion_version,id_punto_entrega,nombre,calle,numero,localidad,provincia,codigo_postal,referencias,zona_horaria)
                VALUES (?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id_configuracion_version,id_punto_entrega) DO UPDATE SET
                nombre=excluded.nombre,calle=excluded.calle,numero=excluded.numero,localidad=excluded.localidad,provincia=excluded.provincia,codigo_postal=excluded.codigo_postal,referencias=excluded.referencias,zona_horaria=excluded.zona_horaria
                """,config,punto,in.nombre().strip(),in.calle().strip(),in.numero().strip(),in.localidad().strip(),in.provincia().strip(),in.codigoPostal().strip(),in.referencias()==null?null:in.referencias().strip(),in.zonaHoraria());
        // Sólo se reemplazan relaciones de una versión EN_PREPARACION: todavía no pueden tener reservas.
        jdbc.update("DELETE FROM lamontana.configuracion_punto_entrega WHERE id_configuracion_version=? AND id_punto_entrega=?",config,punto);
        for(var r:relaciones){long relacion=jdbc.queryForObject("INSERT INTO lamontana.configuracion_punto_entrega(id_configuracion_version,id_punto_entrega,id_sucursal,costo,habilitado) VALUES (?,?,?,?,?) RETURNING id_configuracion_punto_entrega",Long.class,config,punto,r.sucursal(),r.costo(),r.habilitado());
            for(var f:r.franjas())jdbc.update("INSERT INTO lamontana.franja_entrega(id_configuracion_punto_entrega,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada) VALUES (?,?,?,?,?,?)",relacion,f.dia(),Time.valueOf(hora(f.apertura())),Time.valueOf(hora(f.cierre())),f.capacidadPedidos(),f.habilitada());}
    }
    private ConfiguracionService.Borrador terminar(UUID codigo,ConfiguracionService.Borrador b,UUID operacion,String tipo,long actor,String huella,String evento,UUID punto){long id=id(codigo);jdbc.update("UPDATE lamontana.configuracion_version SET version=version+1,fecha_actualizacion=clock_timestamp() WHERE id_configuracion_version=?",id);configuracion.registrar(operacion,tipo,id,actor,huella,evento,b.version()+1,punto,null);jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",id);return configuracion.cargar(codigo);}
    private ConfiguracionService.Borrador editable(UUID codigo,Long version){var b=configuracion.cargar(codigo);if(!b.estado().equals("EN_PREPARACION")||version==null||b.version()!=version)throw conflicto("El borrador cambió o ya no se puede editar. Consultá su versión actual.");if(b.modelo()==null)throw conflicto("Seleccioná primero el modelo operativo.");return b;}
    private long id(UUID codigo){return jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?",Long.class,codigo);}
    private void texto(String texto,int max,String campo){exigir(texto!=null&&!texto.isBlank()&&texto.length()<=max,"Completá "+campo+" con hasta "+max+" caracteres.");}
    private LocalTime hora(String texto){exigir(texto!=null&&texto.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"),"Usá horas locales HH:mm, sin segundos.");return LocalTime.parse(texto);}
    private void exigir(boolean condicion,String mensaje){if(!condicion)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
    private ResponseStatusException conflicto(String mensaje){return new ResponseStatusException(HttpStatus.CONFLICT,mensaje);}
}
