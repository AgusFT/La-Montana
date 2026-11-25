//#region ENCABEZADO · PreciosTarifa.java
/*
 * ========================================================================
 * ARCHIVO: PreciosTarifa.java
 * ========================================================================
 * FUNCIÓN
 * Valida nombres y reglas compartidas de tarifas y calcula el precio final doble faz mediante
 * valor fijo, adicional o porcentaje, con redondeo decimal por hoja.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [private] PreciosTarifa()
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * - [static] void validar(Tarifa tarifa)
 * - [static] BigDecimal dobleFaz(Tarifa tarifa)
 * - [static] List<Object> firma(Tarifa tarifa)
 * - [private, static] void exigir(boolean condicion, String mensaje)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PreciosTarifa (class).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.catalogo;

import static ar.com.lamontana.catalogo.CatalogoController.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class PreciosTarifa {
    private PreciosTarifa() {}
    static void validar(Tarifa tarifa) {
        exigir((tarifa.grupo()==null)==(tarifa.nombre()==null),"La tarifa debe tener grupo y nombre juntos.");
        if(tarifa.nombre()!=null) exigir(!tarifa.nombre().isBlank()&&tarifa.nombre().length()<=140,"Ingresá un nombre de tarifa de hasta 140 caracteres.");
        exigir((tarifa.modoDobleFaz()==null)==(tarifa.valorDobleFaz()==null),"Elegí cómo calcular doble faz y completá su valor.");
        if(tarifa.modoDobleFaz()==null) return;
        exigir(tarifa.grupo()!=null,"La tarifa por hoja necesita grupo y nombre.");
        exigir(tarifa.recargoDobleFaz().signum()==0,"Una tarifa por hoja no admite el recargo antiguo por carilla.");
        exigir(tarifa.valorDobleFaz().signum()>=0,"El valor doble faz debe ser mayor o igual a cero.");
        exigir(dobleFaz(tarifa).compareTo(new BigDecimal("999999999999.99"))<=0,"El precio final doble faz supera el máximo de ARS 999999999999,99.");
    }
    static BigDecimal dobleFaz(Tarifa tarifa) {
        return switch(tarifa.modoDobleFaz()) {
            case FIJO -> tarifa.valorDobleFaz().setScale(2,RoundingMode.HALF_UP);
            case ADICIONAL -> tarifa.precio().add(tarifa.valorDobleFaz()).setScale(2,RoundingMode.HALF_UP);
            case PORCENTAJE -> tarifa.precio().multiply(BigDecimal.ONE.add(tarifa.valorDobleFaz().movePointLeft(2))).setScale(2,RoundingMode.HALF_UP);
        };
    }
    static List<Object> firma(Tarifa tarifa) {
        // Una agrupación representa una única regla, no sólo una etiqueta compartida.
        return List.of(tarifa.nombre().strip(),tarifa.color(),tarifa.precio().stripTrailingZeros(),
                tarifa.recargoDobleFaz().stripTrailingZeros(),tarifa.habilitada(),
                tarifa.modoDobleFaz()==null?"ANTERIOR":tarifa.modoDobleFaz().name(),
                tarifa.valorDobleFaz()==null?"ANTERIOR":tarifa.valorDobleFaz().stripTrailingZeros());
    }
    private static void exigir(boolean condicion,String mensaje) {
        if(!condicion)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);
    }
}
