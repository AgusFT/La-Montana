//#region ENCABEZADO · EvaluadorFranjasTest.java
/*
 * ========================================================================
 * ARCHIVO: EvaluadorFranjasTest.java
 * ========================================================================
 * FUNCIÓN
 * Comprueba el cálculo de franjas, límites de apertura y cierre, capacidad y cambios de horario o
 * fecha local entre zonas horarias.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [private] PuntoEntregaController.Franja franja(int dia, String a, String c, int cupo, boolean
 *   habilitada)
 * - [private] EvaluadorFranjas.Ventana calcular(String llegada, String zona,
 *   List<PuntoEntregaController.Franja> franjas)
 * - [paquete] void aperturaInclusivaCierreExclusivoContiguasCupoCeroYFinDeSemana()
 *   Caso de prueba.
 * - [paquete] void saltoDstOmiteHorasInexistentesSinDesplazarArtificialmenteLaApertura()
 *   Caso de prueba.
 * - [paquete] void retrocesoDstNoSuperponeFranjasNiConsumeTiempoCerradoEntreOcurrencias()
 *   Caso de prueba.
 * - [paquete] void cupoCivilOcupadoOmiteAmbasOcurrenciasDst()
 *   Caso de prueba.
 * - [paquete] void destinoConOtroDiaCivilYFechaSaltadaPorCambioDeZona()
 *   Caso de prueba.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - EvaluadorFranjasTest (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.configuracion.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class EvaluadorFranjasTest {
    private final EvaluadorFranjas evaluador=new EvaluadorFranjas();
    private PuntoEntregaController.Franja franja(int dia,String a,String c,int cupo,boolean habilitada){return new PuntoEntregaController.Franja(dia,a,c,cupo,habilitada);}
    private EvaluadorFranjas.Ventana calcular(String llegada,String zona,List<PuntoEntregaController.Franja> franjas){return evaluador.siguiente(Instant.parse(llegada),zona,franjas,Instant.parse(llegada).plus(Duration.ofDays(366*5)));}
    @Test void aperturaInclusivaCierreExclusivoContiguasCupoCeroYFinDeSemana(){
        var franjas=List.of(franja(1,"17:00","19:00",2,true),franja(1,"19:00","21:00",3,true),franja(2,"09:00","10:00",0,true),franja(2,"10:00","11:00",9,false));String zona="America/Argentina/Buenos_Aires";
        var v=calcular("2026-09-28T18:00:00Z",zona,franjas);assertThat(v.desde()).isEqualTo(Instant.parse("2026-09-28T20:00:00Z"));assertThat(v.cupoConfigurado()).isEqualTo(2);
        v=calcular("2026-09-28T22:00:00Z",zona,franjas);assertThat(v.desde()).isEqualTo(Instant.parse("2026-09-28T22:00:00Z"));assertThat(v.cupoConfigurado()).isEqualTo(3);
        v=calcular("2026-09-29T00:00:00Z",zona,franjas);assertThat(v.fecha()).isEqualTo(LocalDate.of(2026,10,5));assertThat(v.desde()).isEqualTo(Instant.parse("2026-10-05T20:00:00Z"));
        assertThatThrownBy(()->evaluador.siguiente(Instant.parse("2026-09-28T18:00:00Z"),zona,List.of(),Instant.parse("2026-09-29T18:00:00Z"))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("cinco años");
    }
    @Test void saltoDstOmiteHorasInexistentesSinDesplazarArtificialmenteLaApertura(){
        var v=calcular("2026-03-08T05:00:00Z","America/New_York",List.of(franja(7,"02:30","03:00",2,true)));assertThat(v.desde()).isEqualTo(Instant.parse("2026-03-15T06:30:00Z"));
        v=calcular("2026-03-08T05:00:00Z","America/New_York",List.of(franja(7,"02:30","04:00",2,true)));assertThat(v.desde()).isEqualTo(Instant.parse("2026-03-08T07:00:00Z"));assertThat(v.hasta()).isEqualTo(Instant.parse("2026-03-08T08:00:00Z"));
        var calendario=new EvaluadorCalendario();var origen=new EntregaRepositorio.Horario(UUID.randomUUID(),"America/New_York",List.of(new EntregaConfiguracionController.Dia(7,true,"02:30","04:00")));
        var r=calendario.evaluar(1,origen,"0.5",null,EntregaConfiguracionController.Modalidad.RETIRO_SUCURSAL,Instant.parse("2026-03-08T05:00:00Z"));assertThat(r.inicioPreparacion()).isEqualTo(Instant.parse("2026-03-08T07:00:00Z"));assertThat(r.finPreparacion()).isEqualTo(Instant.parse("2026-03-08T07:30:00Z"));
    }
    @Test void retrocesoDstNoSuperponeFranjasNiConsumeTiempoCerradoEntreOcurrencias(){
        var franjas=List.of(franja(7,"01:00","01:30",2,true),franja(7,"01:30","02:00",3,true));String zona="America/New_York";
        var v=calcular("2026-11-01T05:30:00Z",zona,franjas);assertThat(v.apertura()).isEqualTo("01:30");assertThat(v.desde()).isEqualTo(Instant.parse("2026-11-01T05:30:00Z"));assertThat(v.hasta()).isEqualTo(Instant.parse("2026-11-01T06:00:00Z"));
        v=calcular("2026-11-01T06:00:00Z",zona,franjas);assertThat(v.apertura()).isEqualTo("01:00");assertThat(v.desde()).isEqualTo(Instant.parse("2026-11-01T06:00:00Z"));assertThat(v.hasta()).isEqualTo(Instant.parse("2026-11-01T06:30:00Z"));assertThat(v.cupoConfigurado()).isEqualTo(2);
        var origen=new EntregaRepositorio.Horario(UUID.randomUUID(),zona,List.of(new EntregaConfiguracionController.Dia(7,true,"01:00","01:30")));
        var r=new EvaluadorCalendario().evaluar(1,origen,"0.75",null,EntregaConfiguracionController.Modalidad.RETIRO_SUCURSAL,Instant.parse("2026-11-01T05:00:00Z"));assertThat(r.finPreparacion()).isEqualTo(Instant.parse("2026-11-01T06:15:00Z"));
    }
    @Test void cupoCivilOcupadoOmiteAmbasOcurrenciasDst(){
        var vistas=new ArrayList<Instant>();var fecha=LocalDate.of(2026,11,1);
        var result=evaluador.siguiente(Instant.parse("2026-11-01T05:00:00Z"),"America/New_York",List.of(franja(7,"01:00","01:30",1,true)),Instant.parse("2026-11-09T00:00:00Z"),v->{vistas.add(v.desde());return !v.fecha().equals(fecha);});
        assertThat(vistas).contains(Instant.parse("2026-11-01T05:00:00Z"),Instant.parse("2026-11-01T06:00:00Z"));assertThat(result.fecha()).isEqualTo(LocalDate.of(2026,11,8));
    }
    @Test void destinoConOtroDiaCivilYFechaSaltadaPorCambioDeZona(){
        var v=calcular("2026-09-28T12:00:00Z","Pacific/Auckland",List.of(franja(2,"09:00","10:00",1,true)));assertThat(v.fecha()).isEqualTo(LocalDate.of(2026,9,29));assertThat(v.desde()).isEqualTo(Instant.parse("2026-09-28T20:00:00Z"));
        v=calcular("2011-12-30T09:00:00Z","Pacific/Apia",List.of(franja(5,"09:00","10:00",1,true)));assertThat(v.fecha()).isEqualTo(LocalDate.of(2012,1,6));
    }
}
