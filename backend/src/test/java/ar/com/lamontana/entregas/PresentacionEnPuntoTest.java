package ar.com.lamontana.entregas;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import ar.com.lamontana.entregas.EntregaController.PedidoLote;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PresentacionEnPuntoTest {
    private static final String RIDER = "rider@prueba.local";
    private static final String UPDATE = "UPDATE lamontana.recorrido_reparto_pedido SET presentado_en=now() WHERE id_pedido=? AND presentado_en IS NULL";
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final EntregaService service = new EntregaService(jdbc, null, null, null, null, null, null);
    private final UUID route = UUID.randomUUID();
    private final UUID point = UUID.randomUUID();
    private final PedidoLote first = new PedidoLote(UUID.randomUUID(), 5);
    private final PedidoLote second = new PedidoLote(UUID.randomUUID(), 6);

    @Test void presentaDosPedidosSinCambiarSuEstadoYAdmiteReintentos() throws SQLException {
        row(first, 11, point, false);
        row(second, 12, point, false);
        service.presentarEnPunto(route, List.of(first, second), RIDER);
        row(first, 11, point, true);
        row(second, 12, point, true);
        service.presentarEnPunto(route, List.of(first, second), RIDER);
        verify(jdbc).update(UPDATE, 11L);
        verify(jdbc).update(UPDATE, 12L);
        verify(jdbc, never()).update(contains("movimiento_entrega"), any(Object[].class));
    }

    @Test void rechazaElLoteCompletoSiLosDestinosSonDistintos() throws SQLException {
        row(first, 11, point, false);
        row(second, 12, UUID.randomUUID(), false);
        assertThatThrownBy(() -> service.presentarEnPunto(route, List.of(first, second), RIDER))
                .hasMessageContaining("mismo punto");
        verify(jdbc, never()).update(eq(UPDATE), anyLong());
    }

    @Test void rechazaElLoteCompletoSiUnPedidoEsAjenoAlRecorrido() throws SQLException {
        row(first, 11, point, false);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Object>>any(),
                eq(second.pedido()), eq(route), eq(route), eq(RIDER))).thenReturn(List.of());
        assertThatThrownBy(() -> service.presentarEnPunto(route, List.of(first, second), RIDER))
                .hasMessageContaining("no pertenece");
        verify(jdbc, never()).update(eq(UPDATE), anyLong());
    }

    private void row(PedidoLote item, long number, UUID destination, boolean presented) throws SQLException {
        ResultSet result = mock(ResultSet.class);
        when(result.getLong(1)).thenReturn(number);
        when(result.getObject(2, UUID.class)).thenReturn(destination);
        when(result.getLong(3)).thenReturn(item.version());
        when(result.getBoolean(4)).thenReturn(presented);
        when(result.getBoolean(5)).thenReturn(!presented);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Object>>any(),
                eq(item.pedido()), eq(route), eq(route), eq(RIDER)))
                .thenAnswer(call -> List.of(call.<RowMapper<Object>>getArgument(1).mapRow(result, 0)));
    }
}
