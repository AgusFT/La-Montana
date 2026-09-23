//#region ENCABEZADO · HorarioSucursalTest.java
/*
 * ========================================================================
 * ARCHIVO: HorarioSucursalTest.java
 * ========================================================================
 * FUNCIÓN
 * Comprueba resolución de zona horaria argentina y validación de la semana de atención, rechazando
 * días repetidos y horas incoherentes.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] void provinciasYAliasResuelvenSinAceptarLaZonaEnviadaPorElNavegador()
 *   Caso de prueba.
 * - [paquete] void semanaCompletaAbiertaYCerradaEsValida()
 *   Caso de prueba.
 * - [paquete] void rechazaVaciosRepetidosHorasInvertidasYDiasCerradosConHoras()
 *   Caso de prueba.
 * - [private] ArrayList<HorarioAtencion.Dia> semana()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - HorarioSucursalTest (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.organizacion.HorarioAtencion;
import ar.com.lamontana.organizacion.UbicacionSucursal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class HorarioSucursalTest {
    @Test void provinciasYAliasResuelvenSinAceptarLaZonaEnviadaPorElNavegador() {
        assertThat(UbicacionSucursal.PROVINCIAS).hasSize(24);
        for (var p : UbicacionSucursal.PROVINCIAS) {
            assertThat(ZoneId.getAvailableZoneIds()).contains(p.zonaHoraria());
            assertThat(UbicacionSucursal.zona(p.nombre(), "Asia/Tokyo", false)).isEqualTo(p.zonaHoraria());
        }
        for (String caba : List.of("CABA", "C.A.B.A.", "Capital Federal", "  ciudad autonoma de buenos aires  "))
            assertThat(UbicacionSucursal.zona(caba, null, false)).isEqualTo("America/Argentina/Buenos_Aires");
        assertThat(UbicacionSucursal.zona("NEUQUEN", null, false)).isEqualTo("America/Argentina/Salta");
        assertThat(UbicacionSucursal.zona("Córdoba", null, false)).isEqualTo("America/Argentina/Cordoba");
        assertThatThrownBy(() -> UbicacionSucursal.zona("Lugar desconocido", "UTC", false)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void semanaCompletaAbiertaYCerradaEsValida() { HorarioAtencion.validar(semana()); }
    @Test void rechazaVaciosRepetidosHorasInvertidasYDiasCerradosConHoras() {
        assertThatThrownBy(() -> HorarioAtencion.validar(List.of())).isInstanceOf(ResponseStatusException.class);
        var dup=semana();dup.set(1,dup.get(0));
        assertThatThrownBy(() -> HorarioAtencion.validar(dup)).isInstanceOf(ResponseStatusException.class);
        for (var invalido : List.of(new HorarioAtencion.Dia(1,true,null,"18:00"),new HorarioAtencion.Dia(1,true,"18:00","09:00"),
            new HorarioAtencion.Dia(1,true,"09:00","09:00"),new HorarioAtencion.Dia(1,true,"25:00","26:00"),new HorarioAtencion.Dia(1,false,"09:00","18:00"))) {
            var dias=semana();dias.set(0,invalido);
            assertThatThrownBy(() -> HorarioAtencion.validar(dias)).isInstanceOf(ResponseStatusException.class);
        }
        var cerrada=semana();cerrada.set(0,new HorarioAtencion.Dia(1,false,null,null));
        assertThatThrownBy(() -> HorarioAtencion.validar(cerrada)).isInstanceOf(ResponseStatusException.class);
    }
    private ArrayList<HorarioAtencion.Dia> semana() {
        var dias=new ArrayList<HorarioAtencion.Dia>();
        for(int i=1;i<=7;i++)dias.add(new HorarioAtencion.Dia(i,i==1,i==1?"09:00":null,i==1?"18:00":null));
        return dias;
    }
}
