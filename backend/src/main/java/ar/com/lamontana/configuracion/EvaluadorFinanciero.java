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
    public enum Momento { ANTES_APROBACION_Y_PRODUCCION, ANTES_CARGA, DESPUES_APROBACION_ANTES_PRODUCCION, ANTES_PRODUCCION, ANTES_ENTREGA }
    public record Simulacion(long version,String total,boolean revisionHumana,boolean cargaRequiereAcreditacion,
                             String pagoPrevioRequerido,String senaRequerida,String saldo,Momento momento,
                             List<MedioPago> mediosGenerales,List<MedioPago> mediosAcreditacion,List<String> instrucciones,PoliticaPagoArchivos politicaArchivos) {}
    public Simulacion evaluar(Modelo modelo,Criterio criterio,Pagos pagos,long version,BigDecimal total,int carillas) {
        BigDecimal previo=BigDecimal.ZERO,sena=BigDecimal.ZERO;boolean revision=modelo==Modelo.MANUAL;
        var instrucciones=new ArrayList<String>();
        instrucciones.add("El PDF puede cargarse antes de acreditar pagos. Se almacena en privado y se valida técnicamente; su existencia no autoriza impresión.");
        if(modelo==Modelo.CONDICIONAL&&criterio==Criterio.PAGO_PREVIO)previo=total;
        else if(pagos.exigirSena()){
            boolean corresponde=switch(pagos.condicionSena()){
                case SIEMPRE->true;
                case DESDE_CARILLAS->BigDecimal.valueOf(carillas).compareTo(new BigDecimal(pagos.umbralSena()))>=0;
                case DESDE_MONTO->total.compareTo(new BigDecimal(pagos.umbralSena()))>=0;
                case SUPERAR_UMBRAL_APROBACION->total.compareTo(new BigDecimal(pagos.umbralAprobacion()))>0;
            };
            if(corresponde){
                BigDecimal valor=new BigDecimal(pagos.valorSena());
                sena=(pagos.tipoSena()==TipoSena.FIJA?valor:total.multiply(valor).divide(BigDecimal.valueOf(100))).min(total).setScale(2,RoundingMode.HALF_UP);
                if(pagos.tipoSena()==TipoSena.FIJA&&valor.compareTo(total)>0)instrucciones.add("La seña fija se limita al total; no se cobra excedente.");
            }
        }
        BigDecimal anticipo=previo.add(sena);
        var medios=anticipo.signum()==0?List.<MedioPago>of():modelo==Modelo.MANUAL?pagos.medios():List.of(MedioPago.TRANSFERENCIA);
        if(anticipo.signum()>0)instrucciones.add("Se puede completar el pedido con sus PDF validados. Antes de aprobar y producir debe acreditarse y aplicarse "+(previo.signum()>0?"el pago total previo.":"la seña requerida."));
        else instrucciones.add("No hay anticipo exigible en este ejemplo; se mantienen las validaciones técnicas y las demás condiciones.");
        if(revision)instrucciones.add("El control manual siempre requiere revisión y aprobación humanas; pagar no aprueba el trabajo.");
        if(criterio==Criterio.MONTO_TOTAL)instrucciones.add("Aprobación según monto: se compara el total con el umbral de Fase 3. Superarlo no genera revisión humana automática; aplica la regla de seña configurada.");
        if(total.subtract(anticipo).signum()>0)instrucciones.add("El saldo se abona mediante los medios habilitados antes de entregar.");
        if(pagos.medios().contains(MedioPago.TRANSFERENCIA))instrucciones.add("Transferencia: "+pagos.instruccionesTransferencia());
        var politica=pagos.politicaArchivos();
        if(politica!=null){
            instrucciones.add("Plazo comercial: "+politica.plazoMinutos()+" minutos desde la creación del pedido; vencimiento no significa eliminación.");
            instrucciones.add("Conservación después del vencimiento: "+politica.retencionHoras()+" horas. Un pago informado o una intervención pendiente suspenden obligatoriamente la purga.");
        }
        instrucciones.add("La simulación no acredita pagos ni omite las validaciones técnicas y de aprobación.");
        return new Simulacion(version,dinero(total),revision,false,dinero(previo),dinero(sena),dinero(total.subtract(anticipo)),
            anticipo.signum()>0?Momento.ANTES_APROBACION_Y_PRODUCCION:Momento.ANTES_ENTREGA,List.copyOf(pagos.medios()),List.copyOf(medios),List.copyOf(instrucciones),politica);
    }
    private String dinero(BigDecimal valor){return valor.setScale(2,RoundingMode.HALF_UP).toPlainString();}
}
