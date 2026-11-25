package ar.com.lamontana.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class RecursosGlobalesTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final ConfiguracionService configuracion=mock(ConfiguracionService.class);
    private final CopiaParametrosConfiguracion copias=mock(CopiaParametrosConfiguracion.class);
    private final RecursosConfiguracionService service=new RecursosConfiguracionService(jdbc,configuracion,copias);
    private final ConfiguracionService.Borrador draft=mock(ConfiguracionService.Borrador.class);
    private final UUID borrador=UUID.randomUUID(),formato=UUID.randomUUID();
    private final String correo="propietario@example.test";

    @BeforeEach void preparar(){
        when(configuracion.propietario(correo)).thenReturn(7L);
        when(configuracion.huella(any())).thenReturn("huella");
        when(configuracion.cargar(borrador)).thenReturn(draft);
        when(draft.estado()).thenReturn("EN_PREPARACION");
        when(draft.version()).thenReturn(3L);
        when(draft.modelo()).thenReturn(ConfiguracionController.Modelo.MANUAL);
        when(jdbc.queryForObject(startsWith("SELECT id_configuracion_version"),eq(Long.class),eq(borrador))).thenReturn(2L);
        when(jdbc.queryForObject(startsWith("SELECT count(*) FROM lamontana.configuracion_impresora"),eq(Integer.class),anyLong(),anyString(),anyLong())).thenReturn(0);
        when(jdbc.query(startsWith("SELECT id_formato"),any(org.springframework.jdbc.core.RowMapper.class),eq(formato))).thenReturn(List.of(11L));
        when(jdbc.queryForObject(startsWith("INSERT INTO lamontana.impresora"),eq(Long.class),any(UUID.class),eq(7L))).thenReturn(21L);
        when(jdbc.queryForObject(startsWith("UPDATE lamontana.configuracion_version"),eq(Long.class),eq(borrador))).thenReturn(2L);
    }

    @Test void creaImpresoraGlobalSinExigirSucursal(){
        var input=new RecursosConfiguracionController.CrearImpresora(UUID.randomUUID(),3L,null,"Brother global",List.of(formato),true,true,500,RecursosConfiguracionController.EstadoSeleccionado.OPERATIVA);
        assertThat(service.crear(borrador,input,correo)).isSameAs(draft);
        verify(jdbc,never()).query(startsWith("SELECT id_sucursal"),any(org.springframework.jdbc.core.RowMapper.class),any());
        verify(jdbc).update(contains("id_sucursal_ubicacion"),eq(2L),eq(21L),eq("Brother global"),eq(true),eq(true),eq(500),eq("OPERATIVA"),isNull());
    }

    @Test void conservaUbicacionComoDatoVersionadoSinUsarlaParaHabilitar(){
        UUID ubicacion=UUID.randomUUID();
        when(jdbc.query(startsWith("SELECT id_sucursal"),any(org.springframework.jdbc.core.RowMapper.class),eq(ubicacion))).thenReturn(List.of(9L));
        var input=new RecursosConfiguracionController.CrearImpresora(UUID.randomUUID(),3L,ubicacion,"Epson interna",List.of(formato),false,false,250,RecursosConfiguracionController.EstadoSeleccionado.DESHABILITADA);
        service.crear(borrador,input,correo);
        verify(jdbc).update(contains("id_sucursal_ubicacion"),eq(2L),eq(21L),eq("Epson interna"),eq(false),eq(false),eq(250),eq("DESHABILITADA"),eq(9L));
        verify(jdbc,never()).query(contains("estado='ACTIVA'"),any(org.springframework.jdbc.core.RowMapper.class),any());
    }

    @Test void guardarAsignacionRetiraLaHabilitacionComercialPorSucursal(){
        var input=new RecursosConfiguracionController.GuardarRecursos(UUID.randomUUID(),3L,RecursosConfiguracionController.MetodoAsignacion.MANUAL);
        assertThat(service.guardar(borrador,input,correo)).isSameAs(draft);
        verify(jdbc).update("DELETE FROM lamontana.sucursal_servicio WHERE id_configuracion_version=?",2L);
    }
}
