//#region ENCABEZADO · EvaluadorCalendario.java
/*
 * ========================================================================
 * ARCHIVO: EvaluadorCalendario.java
 * ========================================================================
 * FUNCIÓN
 * Calcula las fechas de preparación y entrega usando calendarios locales, horas operativas,
 * tiempos de traslado y mínimos de servicios. Controla el horizonte de cálculo y las transiciones
 * horarias.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] Simulacion :: Simulacion(long version, String zonaHoraria, Instant recibidoEn,
 *   Instant inicioPreparacion, Instant finPreparacion, Instant llegadaEstimada, Instant
 *   disponibleDesde, boolean enCola, List<String> advertencias, DestinoPunto destinoPunto,
 *   DestinoZona destinoZona)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] Simulacion evaluar(long version, EntregaRepositorio.Horario horario, String
 *   preparacionHoras, String trasladoHoras, Modalidad modalidad, Instant recibidoEn)
 * - [public] Simulacion evaluar(long version, EntregaRepositorio.Horario horario, String
 *   preparacionHoras, String trasladoHoras, Modalidad modalidad, Instant recibidoEn, int
 *   minimoServiciosMinutos)
 * - [public, static] Instant limite(Instant recibidoEn, String zonaHoraria)
 * - [private] long segundos(String horas)
 * - [private] Instant siguienteApertura(Instant desde, ZoneId zona, Map<Integer, Dia> dias,
 *   Instant limite)
 * - [private] Instant consumir(Instant desde, long segundos, ZoneId zona, Map<Integer, Dia> dias,
 *   Instant limite)
 * - [private] List<VentanasLocales.Intervalo> ventanas(LocalDate fecha, ZoneId zona, Map<Integer,
 *   Dia> dias)
 * - [private] ResponseStatusException horizonte()
 * - [private] ResponseStatusException error(String mensaje)
 *   Construye un error HTTP controlado.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - EvaluadorCalendario (clase).
 * - EvaluadorCalendario.Simulacion (record).
 * - EvaluadorCalendario.DestinoSucursal (record).
 * - EvaluadorCalendario.DestinoPunto (record).
 * - EvaluadorCalendario.DestinoZona (record).
 * ========================================================================
 */
//#endregion

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
                             Instant llegadaEstimada,Instant disponibleDesde,boolean enCola,List<String> advertencias,DestinoPunto destinoPunto,DestinoZona destinoZona,DestinoSucursal destinoSucursal) {
        public Simulacion(long version,String zonaHoraria,Instant recibidoEn,Instant inicioPreparacion,Instant finPreparacion,Instant llegadaEstimada,Instant disponibleDesde,boolean enCola,List<String> advertencias,DestinoPunto destinoPunto,DestinoZona destinoZona){this(version,zonaHoraria,recibidoEn,inicioPreparacion,finPreparacion,llegadaEstimada,disponibleDesde,enCola,advertencias,destinoPunto,destinoZona,null);}
    }
    public record DestinoSucursal(UUID sucursal,String nombre,String zonaHoraria,LocalDate fecha,String apertura,String cierre,Instant franjaDesde,Instant franjaHasta,int cupoConfigurado){}
    public record DestinoPunto(UUID punto,String nombre,String zonaHoraria,String costo,long versionDisponibilidad,LocalDate fecha,
                               String apertura,String cierre,Instant franjaDesde,Instant franjaHasta,int cupoConfigurado) {}
    public record DestinoZona(UUID zona,String nombre,String zonaHoraria,String costo,TerritorioEntrega territorio,LocalDate fecha,
                              String apertura,String cierre,Instant franjaDesde,Instant franjaHasta,int cupoConfigurado) {}
    public Simulacion evaluar(long version,EntregaRepositorio.Horario horario,String preparacionHoras,String trasladoHoras,Modalidad modalidad,Instant recibidoEn) {
        return evaluar(version,horario,preparacionHoras,trasladoHoras,modalidad,recibidoEn,0);
    }
    public Simulacion evaluar(long version,EntregaRepositorio.Horario horario,String preparacionHoras,String trasladoHoras,Modalidad modalidad,Instant recibidoEn,int minimoServiciosMinutos) {
        ZoneId zona=ZoneId.of(horario.zonaHoraria());Instant limite=limite(recibidoEn,horario.zonaHoraria());var dias=new HashMap<Integer,Dia>();for(var dia:horario.dias())dias.put(dia.dia(),dia);
        Instant comienzo=siguienteApertura(recibidoEn,zona,dias,limite);
        Instant fin=consumir(comienzo,Math.max(segundos(preparacionHoras),Math.multiplyExact((long)minimoServiciosMinutos,60)),zona,dias,limite);
        Instant llegada=null,disponible=null;
        var notas=new ArrayList<String>();notas.add("La preparación y el traslado estimados consumen únicamente las ventanas operativas de la sucursal de origen.");
        notas.add("Son estimaciones del calendario semanal guardado; el avance real puede adelantarlas o demorarlas y no obliga a esperar.");
        if(modalidad==Modalidad.RETIRO_SUCURSAL)disponible=siguienteApertura(fin,zona,dias,limite);
        else if(modalidad==Modalidad.ENVIO_DOMICILIO||modalidad==Modalidad.RETIRO_PUNTO_ENTREGA){llegada=consumir(fin,segundos(trasladoHoras),zona,dias,limite);}
        else throw error("Modalidad de entrega no admitida.");
        return new Simulacion(version,zona.getId(),recibidoEn,comienzo,fin,llegada,disponible,comienzo.isAfter(recibidoEn),List.copyOf(notas),null,null);
    }
    public static Instant limite(Instant recibidoEn,String zonaHoraria){
        try{var inicio=recibidoEn.atZone(ZoneId.of(zonaHoraria));if(inicio.getYear()<1||inicio.getYear()>9994)throw new DateTimeException("Fuera del rango soportado");return inicio.plusYears(5).toInstant();}
        catch(DateTimeException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ingresá una fecha válida entre los años 1 y 9994.");}
    }
    private long segundos(String horas){return new BigDecimal(horas).multiply(BigDecimal.valueOf(3600)).longValueExact();}
    private Instant siguienteApertura(Instant desde,ZoneId zona,Map<Integer,Dia> dias,Instant limite) {
        LocalDate fecha=desde.atZone(zona).toLocalDate();
        while(!fecha.atStartOfDay(zona).toInstant().isAfter(limite)) {
            for(var ventana:ventanas(fecha,zona,dias))if(desde.isBefore(ventana.hasta())) {
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
            cursor=siguienteApertura(cursor,zona,dias,limite);var instante=cursor;var ventana=ventanas(cursor.atZone(zona).toLocalDate(),zona,dias).stream().filter(v->!instante.isBefore(v.desde())&&instante.isBefore(v.hasta())).findFirst().orElseThrow();
            Duration disponible=Duration.between(cursor,ventana.hasta());
            if(restante.compareTo(disponible)<=0){Instant resultado=cursor.plus(restante);if(resultado.isAfter(limite))throw horizonte();return resultado;}
            restante=restante.minus(disponible);cursor=ventana.hasta();
        }
        throw horizonte();
    }
    private List<VentanasLocales.Intervalo> ventanas(LocalDate fecha,ZoneId zona,Map<Integer,Dia> dias) {
        var dia=dias.get(fecha.getDayOfWeek().getValue());if(dia==null||!Boolean.TRUE.equals(dia.habilitado())||dia.apertura()==null||dia.cierre()==null)return List.of();
        return VentanasLocales.calcular(fecha,zona,dia.apertura(),dia.cierre());
    }
    private ResponseStatusException horizonte(){return error("La estimación supera el horizonte técnico de cinco años. Revisá las horas y las ventanas operativas.");}
    private ResponseStatusException error(String mensaje){return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
}
