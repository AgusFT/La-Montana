//#region ENCABEZADO · EvaluadorFranjas.java
/*
 * ========================================================================
 * ARCHIVO: EvaluadorFranjas.java
 * ========================================================================
 * FUNCIÓN
 * Obtiene franjas de entrega válidas a partir de una fecha y zona horaria, respetando
 * habilitación, capacidad, disponibilidad y transiciones del reloj local.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] Ventana siguiente(Instant llegada, String zonaHoraria,
 *   List<PuntoEntregaController.Franja> franjas, Instant limite)
 * - [public] Ventana siguiente(Instant llegada, String zonaHoraria,
 *   List<PuntoEntregaController.Franja> franjas, Instant limite,
 *   java.util.function.Predicate<Ventana> disponible)
 * - [public] List<Ventana> dia(LocalDate fecha, String zonaHoraria,
 *   List<PuntoEntregaController.Franja> franjas)
 * - [private] List<Ventana> ventanas(LocalDate fecha, ZoneId zona, PuntoEntregaController.Franja
 *   franja)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - EvaluadorFranjas (clase).
 * - EvaluadorFranjas.Ventana (record).
 * ========================================================================
 */
//#endregion

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
        return siguiente(llegada,zonaHoraria,franjas,limite,v->true);
    }
    public Ventana siguiente(Instant llegada,String zonaHoraria,List<PuntoEntregaController.Franja> franjas,Instant limite,java.util.function.Predicate<Ventana> disponible){
        ZoneId zona=ZoneId.of(zonaHoraria);LocalDate fecha=llegada.atZone(zona).toLocalDate();
        while(!fecha.atStartOfDay(zona).toInstant().isAfter(limite)){
            var ventanas=new ArrayList<Ventana>();
            for(var franja:franjas)if(Boolean.TRUE.equals(franja.habilitada())&&franja.capacidadPedidos()>0&&franja.dia()==fecha.getDayOfWeek().getValue())ventanas.addAll(ventanas(fecha,zona,franja));
            ventanas.sort(Comparator.comparing(Ventana::desde).thenComparing(Ventana::hasta));
            for(var ventana:ventanas){Instant desde=llegada.isAfter(ventana.desde())?llegada:ventana.desde();if(desde.isBefore(ventana.hasta())&&!desde.isAfter(limite)&&disponible.test(ventana))return ventana;}
            fecha=fecha.plusDays(1);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No hay una franja utilizable dentro del horizonte técnico de cinco años. Revisá el calendario del destino.");
    }
    public List<Ventana> dia(LocalDate fecha,String zonaHoraria,List<PuntoEntregaController.Franja> franjas){
        return franjas.stream().filter(f->Boolean.TRUE.equals(f.habilitada())&&f.capacidadPedidos()>0&&f.dia()==fecha.getDayOfWeek().getValue())
            .flatMap(f->ventanas(fecha,ZoneId.of(zonaHoraria),f).stream()).sorted(Comparator.comparing(Ventana::desde).thenComparing(Ventana::hasta)).toList();
    }
    private List<Ventana> ventanas(LocalDate fecha,ZoneId zona,PuntoEntregaController.Franja franja){
        return VentanasLocales.calcular(fecha,zona,franja.apertura(),franja.cierre()).stream()
            .map(v->new Ventana(fecha,franja.apertura(),franja.cierre(),v.desde(),v.hasta(),franja.capacidadPedidos())).toList();
    }
}
