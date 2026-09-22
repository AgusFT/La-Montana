package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.EntregaConfiguracionController.*;
import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EntregaConfiguracionService {
    private final JdbcTemplate jdbc;
    private final ConfiguracionService configuracion;
    private final EvaluadorCalendario evaluador;
    public EntregaConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion,EvaluadorCalendario evaluador){this.jdbc=jdbc;this.configuracion=configuracion;this.evaluador=evaluador;}
    public record Problema(String codigo,UUID sucursal,String mensaje){}
    public record Validacion(long version,boolean valida,List<Problema> problemas){}
    private record Sucursal(long id,UUID codigo,String nombre,String zona){}
    private record Calendario(long sucursal,String zona,List<Dia> dias){}

    @Transactional
    public ConfiguracionService.Borrador guardar(UUID codigo,GuardarEntrega input,String correo) {
        configuracion.bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(input);
        var repetido=configuracion.reintento(input.operacion(),"GUARDAR_ENTREGA",codigo,actor,huella);if(repetido!=null)return repetido;
        var borrador=configuracion.cargar(codigo);editable(borrador,input.version());
        BigDecimal preparacion=horas(input.preparacionHoras(),true),traslado=horas(input.trasladoHoras(),false);
        exigir(input.modalidades()!=null&&input.modalidades().size()<=3&&input.modalidades().stream().allMatch(Objects::nonNull)&&new HashSet<>(input.modalidades()).size()==input.modalidades().size(),"Las modalidades no pueden repetirse ni quedar nulas.");
        exigir(input.horariosPorSucursal()!=null&&input.horariosPorSucursal().size()<=100,"Ingresá calendarios de sucursal válidos.");
        var zonas=new HashMap<UUID,String>();for(var h:borrador.entrega().horariosPorSucursal())zonas.put(h.sucursal(),h.zonaHoraria());
        var vistas=new HashSet<UUID>();var calendarios=new ArrayList<Calendario>();
        for(var horario:input.horariosPorSucursal()) {
            exigir(horario!=null&&horario.sucursal()!=null&&vistas.add(horario.sucursal()),"Una sucursal no puede aparecer más de una vez en el calendario.");
            validarDias(horario.dias());var sucursal=sucursal(horario.sucursal());
            calendarios.add(new Calendario(sucursal.id(),zonas.getOrDefault(horario.sucursal(),sucursal.zona()),horario.dias()));
        }
        long id=id(codigo);
        jdbc.update("INSERT INTO lamontana.configuracion_entrega(id_configuracion_version,preparacion_horas,traslado_horas) VALUES (?,?,?) ON CONFLICT(id_configuracion_version) DO UPDATE SET preparacion_horas=excluded.preparacion_horas,traslado_horas=excluded.traslado_horas",id,preparacion,traslado);
        jdbc.update("DELETE FROM lamontana.configuracion_modalidad_entrega WHERE id_configuracion_version=?",id);
        for(var modalidad:input.modalidades())jdbc.update("INSERT INTO lamontana.configuracion_modalidad_entrega(id_configuracion_version,modalidad) VALUES (?,?)",id,modalidad.name());
        jdbc.update("DELETE FROM lamontana.configuracion_horario_sucursal WHERE id_configuracion_version=?",id);
        for(var horario:calendarios) {
            jdbc.update("INSERT INTO lamontana.configuracion_horario_sucursal(id_configuracion_version,id_sucursal,zona_horaria) VALUES (?,?,?)",id,horario.sucursal(),horario.zona());
            for(var dia:horario.dias())jdbc.update("INSERT INTO lamontana.horario_sucursal(id_configuracion_version,id_sucursal,dia_semana,habilitado,hora_desde,hora_hasta) VALUES (?,?,?,?,?,?)",id,horario.sucursal(),dia.dia(),dia.habilitado(),hora(dia.apertura()),hora(dia.cierre()));
        }
        jdbc.update("UPDATE lamontana.configuracion_version SET version=version+1,fecha_actualizacion=clock_timestamp() WHERE id_configuracion_version=?",id);
        configuracion.registrar(input.operacion(),"GUARDAR_ENTREGA",id,actor,huella,"ENTREGA_CONFIGURADA",borrador.version()+1);
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",id);
        return configuracion.cargar(codigo);
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Validacion validar(UUID codigo,String correo) {
        configuracion.propietario(correo);var b=configuracion.cargar(codigo);var e=b.entrega();var problemas=new ArrayList<Problema>();
        if(!b.estado().equals("EN_PREPARACION"))problemas.add(new Problema("BORRADOR_NO_EDITABLE",null,"La configuración ya no está en preparación."));
        if(e.preparacionHoras()==null)problemas.add(new Problema("PREPARACION_PENDIENTE",null,"Definí el tiempo estimado de preparación."));
        if(e.modalidades().isEmpty())problemas.add(new Problema("SIN_MODALIDAD",null,"Elegí al menos una modalidad real de entrega."));
        if((e.modalidades().contains(Modalidad.RETIRO_PUNTO_ENTREGA)||e.modalidades().contains(Modalidad.ENVIO_DOMICILIO))&&e.trasladoHoras()==null)
            problemas.add(new Problema("TRASLADO_PENDIENTE",null,"Definí el tiempo adicional de traslado/envío, incluso si es cero."));
        if(e.modalidades().contains(Modalidad.RETIRO_PUNTO_ENTREGA))problemas.add(new Problema("PUNTOS_PENDIENTES",null,"La modalidad de puntos requiere puntos utilizables, franjas y cupos. Su configuración está en construcción."));
        if(e.modalidades().contains(Modalidad.ENVIO_DOMICILIO))problemas.add(new Problema("COBERTURA_ENVIO_PENDIENTE",null,"El envío requiere zonas de cobertura, franjas y cupos de entrega. Su configuración está en construcción."));
        var operativas=operativas(id(codigo));
        if(operativas.isEmpty())problemas.add(new Problema("SIN_SUCURSAL_OPERATIVA",null,"Habilitá servicios en al menos una sucursal activa para ofrecer entregas."));
        for(var sucursal:operativas){var horario=e.horariosPorSucursal().stream().filter(h->h.sucursal().equals(sucursal.codigo())).findFirst().orElse(null);problemas.addAll(problemasCalendario(horario,sucursal.codigo(),sucursal.nombre()));}
        return new Validacion(b.version(),problemas.isEmpty(),List.copyOf(problemas));
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public EvaluadorCalendario.Simulacion simular(UUID codigo,SimularEntrega input,String correo) {
        configuracion.propietario(correo);var b=configuracion.cargar(codigo);editable(b,input.version());var e=b.entrega();
        exigir(input.recibidoEn()!=null&&input.sucursal()!=null&&input.modalidad()!=null,"Completá sucursal, modalidad y fecha de recepción.");
        if(!e.modalidades().contains(input.modalidad()))throw conflicto("Seleccioná y guardá esa modalidad antes de simularla.");
        if(input.modalidad()==Modalidad.RETIRO_PUNTO_ENTREGA)throw conflicto("La simulación de puntos requiere puntos y franjas; está en construcción.");
        var operativa=operativas(id(codigo)).stream().filter(s->s.codigo().equals(input.sucursal())).findFirst();
        if(operativa.isEmpty())throw conflicto("La sucursal debe estar activa y tener servicios habilitados en el borrador para simular su calendario.");
        var horario=e.horariosPorSucursal().stream().filter(h->h.sucursal().equals(input.sucursal())).findFirst().orElse(null);
        var errores=problemasCalendario(horario,input.sucursal(),operativa.get().nombre());if(!errores.isEmpty())throw conflicto(errores.get(0).mensaje());
        if(e.preparacionHoras()==null||(input.modalidad()==Modalidad.ENVIO_DOMICILIO&&e.trasladoHoras()==null))throw conflicto("Completá y guardá los tiempos estimados de la modalidad antes de simular.");
        return evaluador.evaluar(b.version(),horario,e.preparacionHoras(),e.trasladoHoras(),input.modalidad(),input.recibidoEn());
    }

    private List<Problema> problemasCalendario(EntregaRepositorio.Horario horario,UUID codigo,String nombre) {
        if(horario==null)return List.of(new Problema("HORARIO_PENDIENTE",codigo,"Configurá el calendario de "+nombre+"."));
        var problemas=new ArrayList<Problema>();
        if(horario.dias().size()!=7||horario.dias().stream().anyMatch(d->d.habilitado()==null||Boolean.TRUE.equals(d.habilitado())&&(d.apertura()==null||d.cierre()==null)))
            problemas.add(new Problema("HORARIO_INCOMPLETO",codigo,"Completá los siete días y las horas de apertura y cierre de "+nombre+"."));
        if(horario.dias().stream().noneMatch(d->Boolean.TRUE.equals(d.habilitado())&&d.apertura()!=null&&d.cierre()!=null))
            problemas.add(new Problema("SIN_DIA_OPERATIVO",codigo,nombre+" necesita al menos un día abierto con horario completo."));
        return problemas;
    }
    private List<Sucursal> operativas(long config){return jdbc.query("SELECT DISTINCT s.id_sucursal,s.codigo_publico,s.nombre,s.zona_horaria,s.codigo FROM lamontana.sucursal s JOIN lamontana.sucursal_servicio ss USING(id_sucursal) WHERE ss.id_configuracion_version=? AND s.estado='ACTIVA' ORDER BY s.codigo",(rs,row)->new Sucursal(rs.getLong("id_sucursal"),rs.getObject("codigo_publico",UUID.class),rs.getString("nombre"),rs.getString("zona_horaria")),config);}
    private Sucursal sucursal(UUID codigo){var r=jdbc.query("SELECT id_sucursal,codigo_publico,nombre,zona_horaria FROM lamontana.sucursal WHERE codigo_publico=? FOR SHARE",(rs,row)->new Sucursal(rs.getLong(1),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4)),codigo);if(r.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La sucursal del calendario no existe.");return r.get(0);}
    private long id(UUID codigo){return jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?",Long.class,codigo);}
    private void editable(ConfiguracionService.Borrador b,Long version){if(!b.estado().equals("EN_PREPARACION")||version==null||b.version()!=version)throw conflicto("El borrador cambió o ya no se puede editar. Consultá su versión actual.");if(b.modelo()==null)throw conflicto("Seleccioná primero el modelo operativo.");}
    private BigDecimal horas(String texto,boolean preparacion){if(texto==null)return null;exigir(texto.matches("[0-9]{1,5}(\\.[0-9]{1,2})?"),"Ingresá horas con hasta dos decimales, sin exponentes ni separadores.");var valor=new BigDecimal(texto);exigir((preparacion?valor.signum()>0:valor.signum()>=0)&&valor.compareTo(BigDecimal.valueOf(10000))<=0,"La preparación debe ser positiva y el traslado puede ser cero; el límite técnico es 10000 horas.");return valor;}
    private void validarDias(List<Dia> dias) {
        exigir(dias!=null&&dias.size()<=7,"El calendario tiene como máximo siete días.");var vistos=new HashSet<Integer>();
        for(var dia:dias){exigir(dia!=null&&dia.dia()!=null&&dia.dia()>=1&&dia.dia()<=7&&vistos.add(dia.dia()),"Los días deben ser únicos, de lunes (1) a domingo (7).");
            exigir(Boolean.TRUE.equals(dia.habilitado())||dia.apertura()==null&&dia.cierre()==null,"Un día cerrado o sin decidir debe conservar las horas vacías.");
            Time apertura=hora(dia.apertura()),cierre=hora(dia.cierre());if(apertura!=null&&cierre!=null)exigir(apertura.before(cierre),"La apertura debe ser anterior al cierre dentro del mismo día.");}
    }
    private Time hora(String texto){if(texto==null)return null;exigir(texto.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"),"Usá horas locales en formato HH:mm, sin segundos.");return Time.valueOf(LocalTime.parse(texto));}
    private void exigir(boolean condicion,String mensaje){if(!condicion)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
    private ResponseStatusException conflicto(String mensaje){return new ResponseStatusException(HttpStatus.CONFLICT,mensaje);}
}
