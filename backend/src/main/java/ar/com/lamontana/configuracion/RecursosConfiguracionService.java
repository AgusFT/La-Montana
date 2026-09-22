package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.RecursosConfiguracionController.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecursosConfiguracionService {
    private final JdbcTemplate jdbc;
    private final ConfiguracionService configuracion;
    public RecursosConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion){this.jdbc=jdbc;this.configuracion=configuracion;}

    @Transactional
    public ConfiguracionService.Borrador guardar(UUID codigo,GuardarRecursos input,String correo) {
        configuracion.bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(input);
        var repetido=configuracion.reintento(input.operacion(),"GUARDAR_RECURSOS",codigo,actor,huella);if(repetido!=null)return repetido;
        var actual=editable(codigo,input.version());long id=id(codigo);
        if(input.metodoAsignacion()!=MetodoAsignacion.MANUAL||input.serviciosPorSucursal()==null)throw invalido("Elegí asignación manual y los servicios por sucursal.");
        Set<UUID> sucursales=new HashSet<>();List<ServicioSucursal> seleccion=new ArrayList<>();
        for(var grupo:input.serviciosPorSucursal()) {
            if(grupo==null||grupo.sucursal()==null||!sucursales.add(grupo.sucursal()))throw invalido("Una sucursal no puede aparecer más de una vez.");
            var sucursal=sucursal(grupo.sucursal());
            if(grupo.servicios()==null||new HashSet<>(grupo.servicios()).size()!=grupo.servicios().size())throw invalido("No repitas servicios en una sucursal.");
            for(UUID servicio:grupo.servicios()) {
                long idServicio=referencia("servicio",servicio);
                boolean existente=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.sucursal_servicio WHERE id_configuracion_version=? AND id_sucursal=? AND id_servicio=?)",Boolean.class,id,sucursal.id(),idServicio));
                if(!sucursal.activa()&&!existente)throw conflicto("No se pueden habilitar servicios nuevos en una sucursal desactivada.");
                seleccion.add(new ServicioSucursal(sucursal.id(),idServicio));
            }
        }
        jdbc.update("INSERT INTO lamontana.configuracion_recursos(id_configuracion_version,metodo_asignacion) VALUES (?,'MANUAL') ON CONFLICT(id_configuracion_version) DO UPDATE SET metodo_asignacion=excluded.metodo_asignacion",id);
        jdbc.update("DELETE FROM lamontana.sucursal_servicio WHERE id_configuracion_version=?",id);
        for(var s:seleccion)jdbc.update("INSERT INTO lamontana.sucursal_servicio(id_configuracion_version,id_sucursal,id_servicio) VALUES (?,?,?)",id,s.sucursal(),s.servicio());
        return terminar(codigo,actual,input.operacion(),"GUARDAR_RECURSOS",actor,huella,"RECURSOS_CONFIGURADOS",null,null);
    }

    @Transactional
    public ConfiguracionService.Borrador crear(UUID codigo,CrearImpresora input,String correo) {
        configuracion.bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(input);
        var repetido=configuracion.reintento(input.operacion(),"ALTA_IMPRESORA",codigo,actor,huella);if(repetido!=null)return repetido;
        var actual=editable(codigo,input.version());long id=id(codigo);var sucursal=sucursal(input.sucursal());
        if(!sucursal.activa())throw conflicto("Activá la sucursal antes de agregar una impresora.");
        String nombre=nombre(id,sucursal.id(),null,input.nombre());
        var formatos=formatos(input.formatos());capacidades(input.admiteColor(),input.admiteDobleFaz(),input.capacidadHojas());
        if(input.estado()==null)throw invalido("Elegí explícitamente el estado previsto de la impresora.");
        UUID impresora=UUID.randomUUID();
        long recurso=jdbc.queryForObject("INSERT INTO lamontana.impresora(codigo_publico,id_sucursal,id_usuario_creador) VALUES (?,?,?) RETURNING id_impresora",Long.class,impresora,sucursal.id(),actor);
        jdbc.update("INSERT INTO lamontana.configuracion_impresora(id_configuracion_version,id_impresora,nombre,admite_color,admite_doble_faz,capacidad_maxima_hojas,estado) VALUES (?,?,?,?,?,?,?)",id,recurso,nombre,input.admiteColor(),input.admiteDobleFaz(),input.capacidadHojas(),input.estado().name());
        guardarFormatos(id,recurso,formatos);
        var resultado=terminar(codigo,actual,input.operacion(),"ALTA_IMPRESORA",actor,huella,"IMPRESORA_CREADA",impresora,null);
        registrarTransicion(id,actual.version()+1,null,input.estado().name());return resultado;
    }

    @Transactional
    public ConfiguracionService.Borrador editar(UUID codigo,UUID impresora,EditarImpresora input,String correo) {
        configuracion.bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(new Destino(impresora,input));
        var repetido=configuracion.reintento(input.operacion(),"EDITAR_IMPRESORA",codigo,actor,huella);if(repetido!=null)return repetido;
        var actual=editable(codigo,input.version());long id=id(codigo);var recurso=impresora(id,impresora);deshabilitada(recurso);
        String nombre=nombre(id,recurso.sucursal(),recurso.id(),input.nombre());
        var formatos=formatos(input.formatos());capacidades(input.admiteColor(),input.admiteDobleFaz(),input.capacidadHojas());
        jdbc.update("UPDATE lamontana.configuracion_impresora SET nombre=?,admite_color=?,admite_doble_faz=?,capacidad_maxima_hojas=? WHERE id_configuracion_version=? AND id_impresora=?",nombre,input.admiteColor(),input.admiteDobleFaz(),input.capacidadHojas(),id,recurso.id());
        guardarFormatos(id,recurso.id(),formatos);
        return terminar(codigo,actual,input.operacion(),"EDITAR_IMPRESORA",actor,huella,"IMPRESORA_EDITADA",impresora,null);
    }

    @Transactional
    public ConfiguracionService.Borrador estado(UUID codigo,UUID impresora,CambiarEstado input,String correo) {
        configuracion.bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(new Destino(impresora,input));
        var repetido=configuracion.reintento(input.operacion(),"ESTADO_IMPRESORA",codigo,actor,huella);if(repetido!=null)return repetido;
        var actual=editable(codigo,input.version());long id=id(codigo);var recurso=impresora(id,impresora);String motivo=motivo(input.motivo());
        if(input.estado()==null)throw invalido("Elegí un estado previsto para la impresora.");
        if(recurso.estado().equals("RETIRADA"))throw conflicto("La impresora fue retirada de esta configuración.");
        if(recurso.estado().equals(input.estado().name()))throw conflicto("La impresora ya tiene ese estado previsto.");
        if(input.estado()==EstadoSeleccionado.OPERATIVA&&!sucursal(recurso.codigoSucursal()).activa())throw conflicto("Activá la sucursal antes de habilitar su impresora.");
        jdbc.update("UPDATE lamontana.configuracion_impresora SET estado=? WHERE id_configuracion_version=? AND id_impresora=?",input.estado().name(),id,recurso.id());
        var resultado=terminar(codigo,actual,input.operacion(),"ESTADO_IMPRESORA",actor,huella,"IMPRESORA_ESTADO_CAMBIADO",impresora,motivo);
        registrarTransicion(id,actual.version()+1,recurso.estado(),input.estado().name());return resultado;
    }

    @Transactional
    public ConfiguracionService.Borrador retirar(UUID codigo,UUID impresora,RetirarImpresora input,String correo) {
        configuracion.bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(new Destino(impresora,input));
        var repetido=configuracion.reintento(input.operacion(),"RETIRAR_IMPRESORA",codigo,actor,huella);if(repetido!=null)return repetido;
        var actual=editable(codigo,input.version());long id=id(codigo);var recurso=impresora(id,impresora);deshabilitada(recurso);String motivo=motivo(input.motivo());
        jdbc.update("UPDATE lamontana.configuracion_impresora SET estado='RETIRADA',retirada_en=clock_timestamp(),motivo_retiro=? WHERE id_configuracion_version=? AND id_impresora=?",motivo,id,recurso.id());
        var resultado=terminar(codigo,actual,input.operacion(),"RETIRAR_IMPRESORA",actor,huella,"IMPRESORA_RETIRADA",impresora,motivo);
        registrarTransicion(id,actual.version()+1,recurso.estado(),"RETIRADA");return resultado;
    }

    private ConfiguracionService.Borrador editable(UUID codigo,Long version) {
        var actual=configuracion.cargar(codigo);
        if(!actual.estado().equals("EN_PREPARACION"))throw conflicto("Sólo se pueden editar los recursos de una configuración en preparación.");
        if(version==null||actual.version()!=version)throw conflicto("El borrador cambió. Consultá su versión actual antes de guardar.");
        if(actual.modelo()==null)throw conflicto("Elegí el modelo de revisión antes de configurar los recursos.");
        return actual;
    }
    private ConfiguracionService.Borrador terminar(UUID codigo,ConfiguracionService.Borrador actual,UUID operacion,String tipo,long actor,String huella,String evento,UUID recurso,String motivo) {
        long id=jdbc.queryForObject("UPDATE lamontana.configuracion_version SET version=version+1,fecha_actualizacion=clock_timestamp() WHERE codigo_publico=? RETURNING id_configuracion_version",Long.class,codigo);
        configuracion.registrar(operacion,tipo,id,actor,huella,evento,actual.version()+1,recurso,motivo);
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",id);
        return configuracion.cargar(codigo);
    }
    private long id(UUID codigo){return jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?",Long.class,codigo);}
    private void registrarTransicion(long config,long version,String anterior,String nuevo){jdbc.update("UPDATE lamontana.evento_configuracion SET estado_anterior=?,estado_nuevo=? WHERE id_configuracion_version=? AND version=? AND tipo IN ('IMPRESORA_CREADA','IMPRESORA_ESTADO_CAMBIADO','IMPRESORA_RETIRADA')",anterior,nuevo,config,version);}
    private Sucursal sucursal(UUID codigo) {
        var filas=jdbc.query("SELECT id_sucursal,estado='ACTIVA' FROM lamontana.sucursal WHERE codigo_publico=? FOR SHARE",(rs,row)->new Sucursal(rs.getLong(1),rs.getBoolean(2)),codigo);
        if(filas.isEmpty())throw invalido("La sucursal seleccionada no existe.");return filas.get(0);
    }
    private long referencia(String tabla,UUID codigo) {
        // Sólo se invoca con nombres internos constantes; los UUID se parametrizan.
        var ids=jdbc.query("SELECT id_"+tabla+" FROM lamontana."+tabla+" WHERE codigo_publico=?",(rs,row)->rs.getLong(1),codigo);
        if(ids.isEmpty())throw invalido("El "+tabla+" seleccionado no existe en el catálogo.");return ids.get(0);
    }
    private List<Long> formatos(List<UUID> codigos) {
        if(codigos==null||codigos.isEmpty()||codigos.size()>300||new HashSet<>(codigos).size()!=codigos.size())throw invalido("Elegí formatos válidos sin repetirlos.");
        return codigos.stream().map(codigo->referencia("formato",codigo)).toList();
    }
    private String nombre(long config,long sucursal,Long excluir,String nombre) {
        if(nombre==null||nombre.isBlank()||nombre.length()>120)throw invalido("El nombre debe contener entre 1 y 120 caracteres.");String limpio=nombre.strip();
        int duplicados=jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_impresora c JOIN lamontana.impresora i USING(id_impresora) WHERE c.id_configuracion_version=? AND i.id_sucursal=? AND c.estado<>'RETIRADA' AND lower(c.nombre)=lower(?) AND c.id_impresora<>?",Integer.class,config,sucursal,limpio,excluir==null?-1:excluir);
        if(duplicados>0)throw conflicto("Ya hay una impresora con ese nombre en la sucursal.");return limpio;
    }
    private void capacidades(Boolean color,Boolean duplex,Integer hojas){if(color==null||duplex==null||hojas==null||hojas<=0)throw invalido("Declarar color, doble faz y capacidad positiva es obligatorio.");}
    private String motivo(String valor){if(valor==null||valor.isBlank()||valor.length()>500)throw invalido("El motivo debe contener entre 1 y 500 caracteres.");return valor.strip();}
    private Impresora impresora(long config,UUID codigo) {
        var filas=jdbc.query("SELECT i.id_impresora,i.id_sucursal,s.codigo_publico,c.estado FROM lamontana.configuracion_impresora c JOIN lamontana.impresora i USING(id_impresora) JOIN lamontana.sucursal s USING(id_sucursal) WHERE c.id_configuracion_version=? AND i.codigo_publico=?",(rs,row)->new Impresora(rs.getLong(1),rs.getLong(2),rs.getObject(3,UUID.class),rs.getString(4)),config,codigo);
        if(filas.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"La impresora no pertenece a este borrador.");return filas.get(0);
    }
    private void deshabilitada(Impresora impresora){if(!impresora.estado().equals("DESHABILITADA"))throw conflicto("Primero deshabilitá la impresora; sólo entonces se puede editar o retirar.");}
    private void guardarFormatos(long config,long impresora,List<Long> formatos){jdbc.update("DELETE FROM lamontana.configuracion_impresora_formato WHERE id_configuracion_version=? AND id_impresora=?",config,impresora);for(long formato:formatos)jdbc.update("INSERT INTO lamontana.configuracion_impresora_formato(id_configuracion_version,id_impresora,id_formato) VALUES (?,?,?)",config,impresora,formato);}
    private record Sucursal(long id,boolean activa){}
    private record ServicioSucursal(long sucursal,long servicio){}
    private record Impresora(long id,long sucursal,UUID codigoSucursal,String estado){}
    private record Destino(UUID impresora,Object solicitud){}
    private ResponseStatusException invalido(String mensaje){return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
    private ResponseStatusException conflicto(String mensaje){return new ResponseStatusException(HttpStatus.CONFLICT,mensaje);}
}
