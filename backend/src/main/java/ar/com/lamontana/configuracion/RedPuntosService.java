package ar.com.lamontana.configuracion;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.organizacion.SucursalController.Sucursal;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RedPuntosService {
    private final JdbcTemplate jdbc;private final ConfiguracionService configuracion;
    private final PuntoEntregaService validacion;private final OrganizacionService organizacion;
    private final JsonMapper json=JsonMapper.builder().build();
    public RedPuntosService(JdbcTemplate jdbc,ConfiguracionService configuracion,PuntoEntregaService validacion,OrganizacionService organizacion){this.jdbc=jdbc;this.configuracion=configuracion;this.validacion=validacion;this.organizacion=organizacion;}
    public record Punto(PuntosRepositorio.Punto definicion,long version,UUID revision,String actor,Instant fecha){}
    public record Panel(List<Punto> puntos,List<Sucursal> sucursales,boolean configuracionActiva){}
    private long actor(String correo){
        organizacion.exigirPermiso(correo,"GESTIONAR_PUNTOS_ENTREGA");
        var ids=jdbc.query("SELECT u.id_usuario FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.codigo='ADMIN_ADMIN' AND r.activo",(r,n)->r.getLong(1),correo);
        if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Sólo un administrador activo puede editar la red de entrega.");return ids.get(0);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Panel listar(String correo){actor(correo);var repo=new RedPuntosRepositorio(jdbc);return new Panel(repo.leer(repo.configuracionActual()).stream().map(p->vista(p.codigoPublico())).toList(),organizacion.sucursales(),repo.configuracionActual()>0);}
    @Transactional public Punto crear(RedPuntosController.Guardar in,String correo){
        configuracion.bloquear();long actor=actor(correo);String hash=configuracion.huella(List.of("CREAR_RED_PUNTO",in));
        var replay=repetido(in.operacion(),actor,hash);if(replay!=null)return replay;
        if(in.version()==null||in.version()!=0)throw conflicto("Actualizá la red antes de agregar el punto.");
        validacion.validarRed(in);String codigo=in.codigo()==null?"":in.codigo().trim().toUpperCase(Locale.ROOT);
        if(!codigo.matches("[A-Z0-9_-]{1,40}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ingresá un código de punto válido.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.punto_entrega WHERE codigo=?)",Boolean.class,codigo)))throw conflicto("Ya existe un punto con ese código.");
        UUID punto=UUID.randomUUID();long id=jdbc.queryForObject("INSERT INTO lamontana.punto_entrega(codigo_publico,codigo,id_usuario_creador) VALUES (?,?,?) RETURNING id_punto_entrega",Long.class,punto,codigo,actor);
        guardar(id,definicion(punto,codigo,in),0,actor,in.operacion(),hash);
        jdbc.update("INSERT INTO lamontana.disponibilidad_punto_entrega(id_punto_entrega,estado,version,id_actor,fecha_actualizacion) VALUES (?,'HABILITADO',1,?,clock_timestamp())",id,actor);
        jdbc.update("INSERT INTO lamontana.evento_disponibilidad_punto(id_operacion,id_punto_entrega,id_actor,hash_solicitud,estado_anterior,estado_nuevo,version_anterior,version_nueva) VALUES (?,?,?,?,null,'HABILITADO',0,1)",in.operacion(),id,actor,hash);
        return vista(punto);
    }
    @Transactional public Punto editar(UUID punto,RedPuntosController.Guardar in,String correo){
        configuracion.bloquear();long actor=actor(correo);String hash=configuracion.huella(List.of("EDITAR_RED_PUNTO",punto,in));
        var replay=repetido(in.operacion(),actor,hash);if(replay!=null)return replay;
        var actual=vista(punto);if(in.version()==null||actual.version()!=in.version())throw conflicto("El punto cambió. Actualizá la red y revisá tus datos antes de guardar.");
        validacion.validarRed(in);long id=jdbc.queryForObject("SELECT id_punto_entrega FROM lamontana.punto_entrega WHERE codigo_publico=?",Long.class,punto);
        guardar(id,definicion(punto,actual.definicion().codigo(),in),actual.version(),actor,in.operacion(),hash);return vista(punto);
    }
    private PuntosRepositorio.Punto definicion(UUID id,String codigo,PuntoEntregaController.Definicion in){
        return new PuntosRepositorio.Punto(id,codigo,in.nombre().trim(),in.calle().trim(),in.numero().trim(),in.localidad().trim(),in.provincia().trim(),in.codigoPostal().trim(),in.referencias(),in.zonaHoraria(),List.copyOf(in.sucursales()));
    }
    private void guardar(long punto,PuntosRepositorio.Punto definicion,long anterior,long actor,UUID operacion,String hash){
        long revision=jdbc.queryForObject("INSERT INTO lamontana.version_red_punto(id_punto_entrega,version,definicion,id_actor,id_operacion,huella) VALUES (?,?,?::jsonb,?,?,?) RETURNING id_version_red_punto",Long.class,punto,anterior+1,json.writeValueAsString(definicion),actor,operacion,hash);
        jdbc.update("INSERT INTO lamontana.red_punto_actual(id_punto_entrega,id_version_red_punto) VALUES (?,?) ON CONFLICT(id_punto_entrega) DO UPDATE SET id_version_red_punto=excluded.id_version_red_punto",punto,revision);
    }
    private Punto repetido(UUID operacion,long actor,String hash){
        var prev=jdbc.query("SELECT p.codigo_publico,v.id_actor,v.huella FROM lamontana.version_red_punto v JOIN lamontana.punto_entrega p USING(id_punto_entrega) WHERE v.id_operacion=?",(r,n)->new Object[]{r.getObject(1,UUID.class),r.getLong(2),r.getString(3)},operacion);
        if(prev.isEmpty())return null;var p=prev.get(0);if((long)p[1]!=actor||!hash.equals(p[2]))throw conflicto("La operación ya se utilizó con otros datos.");return vista((UUID)p[0]);
    }
    private Punto vista(UUID punto){
        var rows=jdbc.query("SELECT v.definicion,v.version,v.codigo_publico,u.nombre||' '||u.apellido,v.fecha FROM lamontana.red_punto_actual a JOIN lamontana.version_red_punto v USING(id_version_red_punto) JOIN lamontana.punto_entrega p ON p.id_punto_entrega=a.id_punto_entrega JOIN lamontana.usuario u ON u.id_usuario=v.id_actor WHERE p.codigo_publico=?",(r,n)->new Punto(json.readValue(r.getString(1),PuntosRepositorio.Punto.class),r.getLong(2),r.getObject(3,UUID.class),r.getString(4),r.getTimestamp(5).toInstant()),punto);
        if(!rows.isEmpty())return rows.get(0);var previo=new RedPuntosRepositorio(jdbc).punto(punto);
        if(previo==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"El punto no existe en la red de entrega.");return new Punto(previo,0,null,null,null);
    }
    private ResponseStatusException conflicto(String texto){return new ResponseStatusException(HttpStatus.CONFLICT,texto);}
}
