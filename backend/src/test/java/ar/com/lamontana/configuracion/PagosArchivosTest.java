package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.ConfiguracionController.*;
import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PagosArchivosTest {
    private final EvaluadorFinanciero evaluador=new EvaluadorFinanciero();
    private Pagos pagos(boolean sena,CondicionSena condicion,String umbral,String valor,String umbralAprobacion){
        return new Pagos(List.of(MedioPago.TRANSFERENCIA),"Cuenta",90,sena,condicion,umbral,sena?TipoSena.PORCENTAJE:null,valor,umbralAprobacion,new PoliticaPagoArchivos(60,15,72));
    }
    @Test void pagoTotalNoBloqueaCargaPeroExigeAcreditarAntesDeAprobar(){
        var s=evaluador.evaluar(Modelo.CONDICIONAL,Criterio.PAGO_PREVIO,pagos(false,null,null,null,null),2,new BigDecimal("100.00"),4);
        assertThat(s.cargaRequiereAcreditacion()).isFalse();assertThat(s.revisionHumana()).isFalse();
        assertThat(s.pagoPrevioRequerido()).isEqualTo("100.00");assertThat(s.saldo()).isEqualTo("0.00");
        assertThat(s.momento()).isEqualTo(EvaluadorFinanciero.Momento.ANTES_APROBACION_Y_PRODUCCION);
    }
    @Test void senaPreviaNoBloqueaCargaNiImplicaRevisionHumana(){
        var s=evaluador.evaluar(Modelo.CONDICIONAL,Criterio.SENA,pagos(true,CondicionSena.SIEMPRE,null,"25",null),2,new BigDecimal("100.00"),4);
        assertThat(s.cargaRequiereAcreditacion()).isFalse();assertThat(s.revisionHumana()).isFalse();
        assertThat(s.senaRequerida()).isEqualTo("25.00");assertThat(s.saldo()).isEqualTo("75.00");
        assertThat(s.momento()).isEqualTo(EvaluadorFinanciero.Momento.ANTES_APROBACION_Y_PRODUCCION);
    }
    @Test void montoAlSuperarUmbralAplicaSenaSinInventarRevisionHumana(){
        var p=pagos(true,CondicionSena.SUPERAR_UMBRAL_APROBACION,null,"25","100");
        for(String total:List.of("99","100","101")){
            var s=evaluador.evaluar(Modelo.CONDICIONAL,Criterio.MONTO_TOTAL,p,2,new BigDecimal(total),4);
            assertThat(s.revisionHumana()).isFalse();assertThat(s.cargaRequiereAcreditacion()).isFalse();
            assertThat(s.senaRequerida()).isEqualTo(total.equals("101")?"25.25":"0.00");
        }
    }
    @Test void montoSinSenaNoInventaAnticipoNiRevision(){
        var s=evaluador.evaluar(Modelo.CONDICIONAL,Criterio.MONTO_TOTAL,pagos(false,null,null,null,"100"),2,new BigDecimal("101"),4);
        assertThat(s.revisionHumana()).isFalse();assertThat(s.senaRequerida()).isEqualTo("0.00");
        assertThat(s.momento()).isEqualTo(EvaluadorFinanciero.Momento.ANTES_ENTREGA);
    }
    @Test void manualMantieneRevisionHumanaAunqueExistaSena(){
        var s=evaluador.evaluar(Modelo.MANUAL,null,pagos(true,CondicionSena.SIEMPRE,null,"20",null),2,new BigDecimal("100"),4);
        assertThat(s.revisionHumana()).isTrue();assertThat(s.cargaRequiereAcreditacion()).isFalse();assertThat(s.senaRequerida()).isEqualTo("20.00");
    }
    @Test void retencionComienzaEnVencimientoNoEnCarga(){
        Instant creado=Instant.parse("2026-10-27T17:45:00Z");var p=new PoliticaPagoArchivos(15,null,72);
        assertThat(p.limite(creado)).isEqualTo(Instant.parse("2026-10-27T18:00:00Z"));
        assertThat(p.retenerHasta(p.limite(creado))).isEqualTo(Instant.parse("2026-10-30T18:00:00Z"));
    }
    @Test void retencionSoloCertificadaYRecordatorioAnterior(){
        for(int horas:List.of(24,48,72))assertThat(new PoliticaPagoArchivos(60,30,horas).valida()).isTrue();
        for(int horas:List.of(0,1,25,96))assertThat(new PoliticaPagoArchivos(60,30,horas).valida()).isFalse();
        for(int aviso:List.of(0,60,61))assertThat(new PoliticaPagoArchivos(60,aviso,72).valida()).isFalse();
    }
    @Test void plazoExplicitoEntreUnMinutoYTreintaDias(){
        assertThat(new PoliticaPagoArchivos(1,null,72).valida()).isTrue();assertThat(new PoliticaPagoArchivos(43200,null,72).valida()).isTrue();
        for(Integer n:new Integer[]{null,0,-1,43201})assertThat(new PoliticaPagoArchivos(n,null,72).valida()).isFalse();
    }
}
