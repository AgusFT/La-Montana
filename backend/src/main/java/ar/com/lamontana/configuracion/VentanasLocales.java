//#region ENCABEZADO · VentanasLocales.java
/*
 * ========================================================================
 * ARCHIVO: VentanasLocales.java
 * ========================================================================
 * FUNCIÓN
 * Convierte intervalos de apertura y cierre de un día local en intervalos reales de tiempo,
 * separando las transiciones de zona horaria para evitar horas inexistentes o superpuestas.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [private] VentanasLocales()
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete, static] List<Intervalo> calcular(LocalDate fecha, ZoneId zona, String apertura,
 *   String cierre)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - VentanasLocales (clase).
 * - VentanasLocales.Intervalo (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import java.time.*;
import java.util.*;

/** Intersección de un intervalo civil con los offsets que realmente existieron ese día. */
final class VentanasLocales {
    private VentanasLocales(){}
    record Intervalo(Instant desde,Instant hasta){}
    static List<Intervalo> calcular(LocalDate fecha,ZoneId zona,String apertura,String cierre){
        Instant inicio=fecha.atStartOfDay(zona).toInstant(),fin=fecha.plusDays(1).atStartOfDay(zona).toInstant();var partes=new ArrayList<Intervalo>();
        while(inicio.isBefore(fin)){
            var transicion=zona.getRules().nextTransition(inicio);Instant corte=transicion!=null&&transicion.getInstant().isBefore(fin)?transicion.getInstant():fin;
            ZoneOffset offset=zona.getRules().getOffset(inicio);
            Instant desde=fecha.atTime(LocalTime.parse(apertura)).toInstant(offset),hasta=fecha.atTime(LocalTime.parse(cierre)).toInstant(offset);
            if(desde.isBefore(inicio))desde=inicio;if(hasta.isAfter(corte))hasta=corte;
            if(desde.isBefore(hasta)){
                if(!partes.isEmpty()&&partes.get(partes.size()-1).hasta().equals(desde))desde=partes.remove(partes.size()-1).desde();
                partes.add(new Intervalo(desde,hasta));
            }
            inicio=corte;
        }
        return List.copyOf(partes);
    }
}
