//#region ENCABEZADO · GuiaInstalacionTest.java
/*
 * ========================================================================
 * FUNCIÓN: Verifica el orden de instalación, reapertura por requisitos faltantes
 * y liberación al activar, sin depender de preferencias del navegador.
 * MÉTODOS: sucursalEsElPrimerRequisito(), noSaltaHuecosPrevios(),
 *          recursosCompletosAntesDeEntrega(), activacionLiberaElRecorrido().
 * ========================================================================
 */
//#endregion
package ar.com.lamontana.configuracion;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GuiaInstalacionTest {
    @Test void sucursalEsElPrimerRequisito(){
        var g=GuiaInstalacion.crear(false,false,true,true,true,true,true,true,true,true,9L,List.of());
        assertThat(g.etapas().get(0).habilitada()).isTrue();
        assertThat(g.etapas().subList(1,9)).allMatch(e->!e.habilitada()&&!e.completa());
        assertThat(g.faseDisponible()).isEqualTo(1);
    }
    @Test void noSaltaHuecosPrevios(){
        var g=GuiaInstalacion.crear(false,true,true,false,true,true,true,true,true,true,9L,List.of());
        assertThat(g.etapas().get(2).habilitada()).isTrue();
        assertThat(g.etapas().get(3).habilitada()).isFalse();
        assertThat(g.faseDisponible()).isEqualTo(1);
    }
    @Test void recursosCompletosAntesDeEntrega(){
        var incompleta=GuiaInstalacion.crear(false,true,true,true,true,true,true,true,false,true,4L,List.of("Falta impresora"));
        assertThat(incompleta.faseDisponible()).isEqualTo(4);
        assertThat(incompleta.etapas().get(7).habilitada()).isFalse();
        var completa=GuiaInstalacion.crear(false,true,true,true,true,true,true,true,true,true,5L,List.of());
        assertThat(completa.faseDisponible()).isEqualTo(6);
        assertThat(completa.etapas().get(8).habilitada()).isTrue();
        assertThat(completa.primeraInstalacion()).isTrue();
    }
    @Test void activacionLiberaElRecorrido(){
        var g=GuiaInstalacion.crear(true,false,false,false,false,false,false,false,false,false,null,List.of());
        assertThat(g.primeraInstalacion()).isFalse();
        assertThat(g.faseDisponible()).isEqualTo(8);
        assertThat(g.etapas()).allMatch(e->e.habilitada()&&e.completa());
    }
}
