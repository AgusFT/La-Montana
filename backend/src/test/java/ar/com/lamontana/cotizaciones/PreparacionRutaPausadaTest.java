package ar.com.lamontana.cotizaciones;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import ar.com.lamontana.archivos.ArchivoPreparadoService;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.pedidos.PedidoService;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

class PreparacionRutaPausadaTest {
    UUID preparacion=UUID.randomUUID(),punto=UUID.randomUUID(),laPlata=UUID.randomUUID(),floresta=UUID.randomUUID(),quote=UUID.randomUUID();
    PreparacionClienteStore store;CotizacionService cotizaciones;OfertaOperativaService operativa;PedidoService pedidos;
    PreparacionClienteService service;PreparacionClienteStore.Datos datos;PreparacionClienteService.Cotizar comando;
    CotizacionService.Detalle anterior;CotizacionService.Oferta oferta;
    @BeforeEach void setup() {
        store=mock(PreparacionClienteStore.class);cotizaciones=mock(CotizacionService.class);operativa=mock(OfertaOperativaService.class);pedidos=mock(PedidoService.class);
        var json=JsonMapper.builder().build();when(store.mapper()).thenReturn(json);
        var preferencias=new PreparacionClienteService.Preferencias(EntregaConfiguracionController.Modalidad.RETIRO_PUNTO_ENTREGA,null,punto,null,ConfiguracionController.MedioPago.EFECTIVO);
        comando=new PreparacionClienteService.Cotizar(UUID.randomUUID(),3,preferencias);
        var presupuesto=new CotizacionService.Presupuesto(UUID.randomUUID(),UUID.randomUUID(),List.of(),"0.00");
        datos=new PreparacionClienteStore.Datos(1,preparacion,1,null,null,null,3,"ABIERTA","[]",json.writeValueAsString(presupuesto),json.writeValueAsString(preferencias),quote,null);
        when(store.datos(preparacion,"cliente@prueba.local",false)).thenReturn(datos);
        anterior=mock(CotizacionService.Detalle.class);oferta=mock(CotizacionService.Oferta.class);when(anterior.oferta()).thenReturn(oferta);
        when(anterior.estado()).thenReturn("VIGENTE");when(anterior.vigenteHasta()).thenReturn(Instant.now().plusSeconds(3600));when(anterior.codigoPublico()).thenReturn(quote);
        when(oferta.sucursal()).thenReturn(new OfertaOperativaService.Sucursal(laPlata,"La Plata","Calle 7"));
        when(cotizaciones.detallePreparada(quote,datos)).thenReturn(anterior);
        when(operativa.leer()).thenReturn(new OfertaOperativaService.Contexto(null,null,List.of(new OfertaOperativaService.Sucursal(floresta,"Floresta","Rivadavia")),List.of(),List.of(new EntregaConfiguracionService.PuntoDisponible(punto,floresta,2,"City Bell","America/Argentina/Buenos_Aires","200",1))));
        service=new PreparacionClienteService(store,mock(ArchivoPreparadoService.class),cotizaciones,operativa,pedidos,null,null);
    }
    private static class NuevaCotizacion extends RuntimeException {}
    @Test void sharedPointRecalculatesInsteadOfReusingQuoteFromPausedBranch() {
        when(cotizaciones.crearPreparada(any(),same(datos))).thenThrow(new NuevaCotizacion());
        assertThatThrownBy(()->service.cotizar(preparacion,comando,"cliente@prueba.local")).isInstanceOf(NuevaCotizacion.class);
        var input=ArgumentCaptor.forClass(CotizacionController.Crear.class);verify(cotizaciones).crearPreparada(input.capture(),same(datos));
        assertThat(input.getValue().sucursal()).isEqualTo(floresta);assertThat(input.getValue().punto()).isEqualTo(punto);
        verify(pedidos,never()).revisarPreparado(anterior);
    }
    @Test void unchangedAvailableSourceKeepsSavedQuote() {
        when(oferta.sucursal()).thenReturn(new OfertaOperativaService.Sucursal(floresta,"Floresta","Rivadavia"));
        when(pedidos.revisarPreparado(anterior)).thenReturn(mock(PedidoService.Revision.class));
        assertThat(service.cotizar(preparacion,comando,"cliente@prueba.local").cotizacion()).isSameAs(anterior);
        verify(cotizaciones,never()).crearPreparada(any(),any());
    }
    @Test void pointWithoutAvailableSourceCannotReuseOldQuoteOrCreateNewOne() {
        when(operativa.leer()).thenReturn(new OfertaOperativaService.Contexto(null,null,List.of(),List.of(),List.of()));
        assertThatThrownBy(()->service.cotizar(preparacion,comando,"cliente@prueba.local")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        verify(cotizaciones,never()).crearPreparada(any(),any());verify(pedidos,never()).revisarPreparado(any());
    }
}
