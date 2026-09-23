//#region ENCABEZADO · EvaluadorFinanciero.java
/*
 * ========================================================================
 * ARCHIVO: EvaluadorFinanciero.java
 * ========================================================================
 * FUNCIÓN
 * Evalúa las reglas financieras del modelo operativo: pago previo, seña, saldo, medios de
 * acreditación y condiciones necesarias para continuar el trabajo.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] Simulacion evaluar(Modelo modelo, Criterio criterio, Pagos pagos, long version,
 *   BigDecimal total, int carillas)
 * - [private] String dinero(BigDecimal valor)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - EvaluadorFinanciero (clase).
 * - EvaluadorFinanciero.Momento (enumeración).
 * - EvaluadorFinanciero.Simulacion (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.ConfiguracionController.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Evaluación pura reutilizable con una configuración guardada; no acredita dinero ni aprueba pedidos. */
@Component
public class EvaluadorFinanciero {
    public enum Momento { ANTES_CARGA, DESPUES_APROBACION_ANTES_PRODUCCION, ANTES_PRODUCCION, ANTES_ENTREGA }
    public record Simulacion(long version,String total,boolean revisionHumana,boolean cargaRequiereAcreditacion,
                             String pagoPrevioRequerido,String senaRequerida,String saldo,Momento momento,
                             List<MedioPago> mediosGenerales,List<MedioPago> mediosAcreditacion,List<String> instrucciones) {}
    public Simulacion evaluar(Modelo modelo,Criterio criterio,Pagos pagos,long version,BigDecimal total,int carillas) {
        BigDecimal previo=BigDecimal.ZERO,sena=BigDecimal.ZERO;
        boolean superaUmbral=modelo==Modelo.CONDICIONAL&&criterio==Criterio.MONTO_TOTAL&&total.compareTo(new BigDecimal(pagos.umbralAprobacion()))>0;
        boolean revision=modelo==Modelo.MANUAL||superaUmbral,carga=false;
        Momento momento=Momento.ANTES_ENTREGA;
        var instrucciones=new ArrayList<String>();
        if(modelo==Modelo.CONDICIONAL&&criterio==Criterio.PAGO_PREVIO) {
            previo=total;carga=total.signum()>0;momento=Momento.ANTES_CARGA;
            instrucciones.add(carga?"Acreditar el total por transferencia antes de habilitar la carga del PDF.":"El total es cero: no hay importe que acreditar antes de cargar el PDF. Las validaciones técnicas siguen siendo obligatorias.");
        } else {
            boolean corresponde=pagos.exigirSena()&&switch(pagos.condicionSena()) {
                case SIEMPRE -> true;
                case DESDE_CARILLAS -> BigDecimal.valueOf(carillas).compareTo(new BigDecimal(pagos.umbralSena()))>=0;
                case DESDE_MONTO -> total.compareTo(new BigDecimal(pagos.umbralSena()))>=0;
                case SUPERAR_UMBRAL_APROBACION -> total.compareTo(new BigDecimal(pagos.umbralAprobacion()))>0;
            };
            if(corresponde) {
                BigDecimal valor=new BigDecimal(pagos.valorSena());
                sena=(pagos.tipoSena()==TipoSena.FIJA?valor:total.multiply(valor).divide(BigDecimal.valueOf(100))).min(total).setScale(2,RoundingMode.HALF_UP);
                if(modelo==Modelo.CONDICIONAL&&criterio==Criterio.SENA) {
                    carga=sena.signum()>0;momento=Momento.ANTES_CARGA;
                    instrucciones.add(sena.signum()>0?"Acreditar la seña por transferencia antes de habilitar la carga del PDF; efectivo queda disponible sólo para el saldo.":"La seña calculada redondea a 0.00: no hay importe exigible ni acreditación pendiente para cargar el PDF.");
                } else if(modelo==Modelo.CONDICIONAL&&criterio==Criterio.MONTO_TOTAL) {
                    revision=true;momento=Momento.DESPUES_APROBACION_ANTES_PRODUCCION;
                    instrucciones.add(sena.signum()>0?"Recibir y revisar el PDF; pedir correcciones si hacen falta. Después de aprobar, acreditar la seña por transferencia antes de producir.":"Recibir y revisar el PDF; pedir correcciones si hacen falta. Después de aprobar, la seña redondeada a 0.00 no agrega una acreditación pendiente para producir.");
                } else {
                    momento=Momento.ANTES_PRODUCCION;
                    instrucciones.add(sena.signum()>0?"La seña configurada debe acreditarse antes de producir; se conserva la decisión humana del modelo manual.":"Se conserva la revisión humana; la seña calculada redondea a 0.00 y no agrega una acreditación pendiente para producir.");
                }
                if(pagos.tipoSena()==TipoSena.FIJA&&valor.compareTo(total)>0)instrucciones.add("La seña fija se limita al total: no se cobra un excedente.");
            } else instrucciones.add(revision?"El pedido requiere revisión humana. Este ejemplo no exige seña; después de aprobar se puede producir sin anticipo.":"El total no supera el umbral: este ejemplo no exige seña ni revisión humana por monto.");
        }
        BigDecimal anticipo=previo.add(sena);
        var mediosAcreditacion=anticipo.signum()==0?List.<MedioPago>of():modelo==Modelo.MANUAL?pagos.medios():List.of(MedioPago.TRANSFERENCIA);
        if(total.subtract(anticipo).signum()>0)instrucciones.add("El saldo se abona mediante los medios generales habilitados antes de entregar.");
        if(pagos.medios().contains(MedioPago.TRANSFERENCIA))instrucciones.add("Transferencia: "+pagos.instruccionesTransferencia());
        instrucciones.add("La simulación evalúa sólo condiciones financieras; no acredita pagos ni omite la inspección técnica del PDF y los demás controles del pedido.");
        return new Simulacion(version,dinero(total),revision,carga,dinero(previo),dinero(sena),dinero(total.subtract(anticipo)),momento,
                List.copyOf(pagos.medios()),List.copyOf(mediosAcreditacion),List.copyOf(instrucciones));
    }
    private String dinero(BigDecimal valor){return valor.setScale(2,RoundingMode.HALF_UP).toPlainString();}
}
