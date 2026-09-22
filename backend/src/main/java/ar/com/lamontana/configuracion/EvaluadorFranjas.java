package ar.com.lamontana.configuracion;

import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Ventanas reales de una plantilla local; las dos ocurrencias DST comparten el mismo cupo por fecha/franja. */
@Component
public class EvaluadorFranjas {
    public record Ventana(LocalDate fecha,String apertura,String cierre,Instant desde,Instant hasta,int cupoConfigurado){}
    public Ventana siguiente(Instant llegada,String zonaHoraria,List<PuntoEntregaController.Franja> franjas,Instant limite){
        ZoneId zona=ZoneId.of(zonaHoraria);LocalDate fecha=llegada.atZone(zona).toLocalDate();
        while(!fecha.atStartOfDay(zona).toInstant().isAfter(limite)){
            var ventanas=new ArrayList<Ventana>();
            for(var franja:franjas)if(Boolean.TRUE.equals(franja.habilitada())&&franja.capacidadPedidos()>0&&franja.dia()==fecha.getDayOfWeek().getValue())ventanas.addAll(ventanas(fecha,zona,franja));
            ventanas.sort(Comparator.comparing(Ventana::desde).thenComparing(Ventana::hasta));
            for(var ventana:ventanas){Instant desde=llegada.isAfter(ventana.desde())?llegada:ventana.desde();if(desde.isBefore(ventana.hasta())&&!desde.isAfter(limite))return ventana;}
            fecha=fecha.plusDays(1);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No hay una franja utilizable dentro del horizonte técnico de cinco años. Revisá el calendario del destino.");
    }
    private List<Ventana> ventanas(LocalDate fecha,ZoneId zona,PuntoEntregaController.Franja franja){
        return VentanasLocales.calcular(fecha,zona,franja.apertura(),franja.cierre()).stream()
            .map(v->new Ventana(fecha,franja.apertura(),franja.cierre(),v.desde(),v.hasta(),franja.capacidadPedidos())).toList();
    }
}
