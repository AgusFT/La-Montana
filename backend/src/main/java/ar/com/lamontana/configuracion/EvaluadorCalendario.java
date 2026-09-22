package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.EntregaConfiguracionController.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Cómputo semanal puro: no crea pedidos, reservas ni promesas de cobertura. */
@Component
public class EvaluadorCalendario {
    public record Simulacion(long version,String zonaHoraria,Instant recibidoEn,Instant inicioPreparacion,Instant finPreparacion,
                             Instant llegadaEstimada,Instant disponibleDesde,boolean enCola,List<String> advertencias) {}
    private record Ventana(Instant desde,Instant hasta) {}
    public Simulacion evaluar(long version,EntregaRepositorio.Horario horario,String preparacionHoras,String trasladoHoras,Modalidad modalidad,Instant recibidoEn) {
        ZoneId zona=ZoneId.of(horario.zonaHoraria());ZonedDateTime inicio;
        try{inicio=recibidoEn.atZone(zona);if(inicio.getYear()<1||inicio.getYear()>9994)throw new DateTimeException("Fuera del rango soportado");}
        catch(DateTimeException ex){throw error("Ingresá una fecha válida entre los años 1 y 9994.");}
        Instant limite=inicio.plusYears(5).toInstant();var dias=new HashMap<Integer,Dia>();for(var dia:horario.dias())dias.put(dia.dia(),dia);
        Instant comienzo=siguienteApertura(recibidoEn,zona,dias,limite);
        Instant fin=consumir(comienzo,segundos(preparacionHoras),zona,dias,limite);
        Instant llegada=null,disponible=null;
        var notas=new ArrayList<String>();notas.add("La preparación y el traslado estimados consumen únicamente las ventanas operativas de la sucursal de origen.");
        notas.add("Son estimaciones del calendario semanal guardado; el avance real puede adelantarlas o demorarlas y no obliga a esperar.");
        if(modalidad==Modalidad.RETIRO_SUCURSAL)disponible=siguienteApertura(fin,zona,dias,limite);
        else if(modalidad==Modalidad.ENVIO_DOMICILIO){llegada=consumir(fin,segundos(trasladoHoras),zona,dias,limite);notas.add("La llegada calculada no confirma disponibilidad: faltan cobertura domiciliaria, franjas y cupos de entrega.");}
        else throw error("La simulación de puntos requiere su definición y franjas; está en construcción.");
        return new Simulacion(version,zona.getId(),recibidoEn,comienzo,fin,llegada,disponible,comienzo.isAfter(recibidoEn),List.copyOf(notas));
    }
    private long segundos(String horas){return new BigDecimal(horas).multiply(BigDecimal.valueOf(3600)).longValueExact();}
    private Instant siguienteApertura(Instant desde,ZoneId zona,Map<Integer,Dia> dias,Instant limite) {
        LocalDate fecha=desde.atZone(zona).toLocalDate();
        while(!fecha.atStartOfDay(zona).toInstant().isAfter(limite)) {
            var ventana=ventana(fecha,zona,dias);
            if(ventana!=null&&desde.isBefore(ventana.hasta())) {
                Instant resultado=desde.isAfter(ventana.desde())?desde:ventana.desde();
                if(!resultado.isAfter(limite))return resultado;
            }
            fecha=fecha.plusDays(1);
        }
        throw horizonte();
    }
    private Instant consumir(Instant desde,long segundos,ZoneId zona,Map<Integer,Dia> dias,Instant limite) {
        if(segundos==0)return desde;
        // Duration conserva los segundos reales en cambios DST y no redondea fracciones de hora.
        Duration restante=Duration.ofSeconds(segundos);Instant cursor=desde;
        while(!cursor.isAfter(limite)) {
            cursor=siguienteApertura(cursor,zona,dias,limite);var ventana=ventana(cursor.atZone(zona).toLocalDate(),zona,dias);
            Duration disponible=Duration.between(cursor,ventana.hasta());
            if(restante.compareTo(disponible)<=0){Instant resultado=cursor.plus(restante);if(resultado.isAfter(limite))throw horizonte();return resultado;}
            restante=restante.minus(disponible);cursor=ventana.hasta();
        }
        throw horizonte();
    }
    private Ventana ventana(LocalDate fecha,ZoneId zona,Map<Integer,Dia> dias) {
        var dia=dias.get(fecha.getDayOfWeek().getValue());if(dia==null||!Boolean.TRUE.equals(dia.habilitado())||dia.apertura()==null||dia.cierre()==null)return null;
        // atZone adelanta horas inexistentes por el salto DST. Un overlap incluye ambas ocurrencias.
        Instant desde=fecha.atTime(LocalTime.parse(dia.apertura())).atZone(zona).withEarlierOffsetAtOverlap().toInstant();
        Instant hasta=fecha.atTime(LocalTime.parse(dia.cierre())).atZone(zona).withLaterOffsetAtOverlap().toInstant();
        return hasta.isAfter(desde)?new Ventana(desde,hasta):null;
    }
    private ResponseStatusException horizonte(){return error("La estimación supera el horizonte técnico de cinco años. Revisá las horas y las ventanas operativas.");}
    private ResponseStatusException error(String mensaje){return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
}
