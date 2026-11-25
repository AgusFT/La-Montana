//#region ENCABEZADO · FranjasEntrega.java
/*
 * ========================================================================
 * ARCHIVO: FranjasEntrega.java
 * ========================================================================
 * FUNCIÓN
 * Valida los días, horas, capacidades y posibles solapamientos de las franjas de entrega
 * declaradas por el administrador.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [private] FranjasEntrega()
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete, static] void validar(List<Franja> franjas)
 * - [private, static] LocalTime hora(String texto)
 * - [private, static] void exigir(boolean condicion, String mensaje)
 *   Rechaza la operación si no se cumple la condición indicada.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - FranjasEntrega (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;
import java.time.LocalTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ar.com.lamontana.configuracion.PuntoEntregaController.Franja;
final class FranjasEntrega {
    private FranjasEntrega(){}
    static void validar(List<Franja> franjas) {
        exigir(franjas!=null&&franjas.size()<=100,"Ingresá una lista válida de franjas para el destino.");
        for(var f:franjas){exigir(f!=null&&f.dia()!=null&&f.dia()>=1&&f.dia()<=7,"El día de una franja debe estar entre lunes (1) y domingo (7).");
            LocalTime desde=hora(f.apertura()),hasta=hora(f.cierre());exigir(desde.isBefore(hasta),"La apertura de cada franja debe ser anterior al cierre dentro del mismo día.");
            exigir(f.capacidadPedidos()!=null&&f.capacidadPedidos()>=0&&f.habilitada()!=null,"Cada franja exige capacidad entera no negativa y habilitación explícita.");}
        var ordenadas=new ArrayList<>(franjas);ordenadas.sort(Comparator.comparing(Franja::dia).thenComparing(Franja::apertura));
        for(int i=1;i<ordenadas.size();i++){var anterior=ordenadas.get(i-1);var actual=ordenadas.get(i);exigir(!anterior.dia().equals(actual.dia())||!LocalTime.parse(actual.apertura()).isBefore(LocalTime.parse(anterior.cierre())),"Las franjas para el mismo destino y día no pueden superponerse; pueden ser contiguas.");}
    }
    private static LocalTime hora(String texto){exigir(texto!=null&&texto.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"),"Usá horas locales HH:mm, sin segundos.");return LocalTime.parse(texto);}
    private static void exigir(boolean condicion,String mensaje){if(!condicion)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
}
