package ar.com.lamontana;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.catalogo.*;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.CoberturaPagos;
import ar.com.lamontana.pedidos.PedidoService;
import ar.com.lamontana.produccion.*;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.*;
import org.springframework.web.server.ResponseStatusException;

class SeleccionImpresoraServiceTest {
 private UUID orderId=UUID.randomUUID(),itemId=UUID.randomUUID(),format=UUID.randomUUID(),first=UUID.randomUUID(),second=UUID.randomUUID();
 private JdbcTemplate jdbc;private PedidoService orders;private OrganizacionService org;private RecursosRepositorio resources;private OperacionImpresoraService runtime;private ProduccionService service;private PedidoService.Detalle order;private CotizacionService.ItemCotizado item;
 @BeforeEach @SuppressWarnings("unchecked") void setup()throws Exception{
  jdbc=mock(JdbcTemplate.class);orders=mock(PedidoService.class);org=mock(OrganizacionService.class);resources=mock(RecursosRepositorio.class);runtime=mock(OperacionImpresoraService.class);var files=mock(ArchivosPrivados.class);var coverage=mock(CoberturaPagos.class);
  order=mock(PedidoService.Detalle.class,RETURNS_DEEP_STUBS);when(order.numero()).thenReturn(7L);when(order.version()).thenReturn(1L);when(order.estado()).thenReturn("APROBADO");when(orders.detalle(eq(orderId),anyString(),eq(true))).thenReturn(order);
  item=new CotizacionService.ItemCotizado(itemId,null,new EvaluadorPrecioItem.Item(UUID.randomUUID(),format,UUID.randomUUID(),CatalogoController.ModoColor.COLOR,252,1,true,List.of()),"A4","Común",new EvaluadorPrecioItem.Precio(252,1,252,126,List.of(),"1000.00",0));
  when(order.oferta().items()).thenReturn(List.of(item));when(order.archivos()).thenReturn(List.of(new PedidoService.Pdf(itemId,UUID.randomUUID(),"catalogo.pdf","a".repeat(64),100,252,null)));
  when(order.condiciones().cotizadas().pagoPrevioRequerido()).thenReturn("0.00");when(order.condiciones().cotizadas().senaRequerida()).thenReturn("0.00");when(order.condiciones().actuales().pagoPrevioRequerido()).thenReturn("0.00");when(order.condiciones().actuales().senaRequerida()).thenReturn("0.00");
  when(coverage.acreditado(anyLong(),anyList())).thenReturn(BigDecimal.ZERO);when(org.contexto(anyString())).thenReturn(new OrganizacionService.Contexto(List.of(),List.of("GESTIONAR_PRODUCCION")));
  when(jdbc.queryForObject(anyString(),eq(Long.class),any(Object[].class))).thenReturn(1L);when(jdbc.queryForObject(anyString(),eq(Boolean.class),any(Object[].class))).thenAnswer(i->((String)i.getArgument(0)).startsWith("SELECT EXISTS"));
  when(jdbc.query(anyString(),any(RowMapper.class),any(Object[].class))).thenReturn(List.of());
  when(jdbc.queryForObject(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{ResultSet rs=mock(ResultSet.class);when(rs.getLong(1)).thenReturn(9L);when(rs.getString(2)).thenReturn("Operadora");when(rs.getString(3)).thenReturn("EMPLEADO");return ((RowMapper<?>)i.getArgument(1)).mapRow(rs,0);});
  when(runtime.estado(any())).thenAnswer(i->new OperacionImpresoraService.Estado(i.getArgument(0),true,null,0,null,null));when(runtime.formatosPorCodigo(anyList())).thenReturn(List.of(new OperacionImpresoraService.Formato(format,"A4")));
  service=new ProduccionService(jdbc,orders,org,files,coverage,resources,runtime);
 }
 private RecursosRepositorio.Impresora printer(UUID id,int capacity,boolean color){return new RecursosRepositorio.Impresora(id,null,id.equals(first)?"Equipo A":"Equipo B",List.of(format),color,true,capacity,"OPERATIVA",null,null);}
 private void machines(RecursosRepositorio.Impresora... printers){when(resources.leer(1)).thenReturn(new RecursosRepositorio.Recursos(null,List.of(printers)));}
 @Test void muestraTodosLosEquiposConRazonesYUnaRecomendacion(){machines(printer(first,250,true),printer(second,100,false));var options=service.consultar(orderId,"operadora").items().get(0).impresoras();assertEquals(2,options.size());assertTrue(options.get(0).recomendada());assertTrue(options.get(0).compatible());assertFalse(options.get(1).compatible());assertTrue(options.get(1).motivos().stream().anyMatch(m->m.contains("color")));assertTrue(options.get(1).motivos().stream().anyMatch(m->m.contains("capacidad")));}
 @Test void papelSuficienteRegistradoTienePrioridadYLaRecomendacionEsDeterminista(){machines(printer(first,250,true),printer(second,500,true));when(runtime.estado(second)).thenReturn(new OperacionImpresoraService.Estado(second,true,200,3,null,"Operadora"));var options=service.consultar(orderId,"operadora").items().get(0).impresoras();assertFalse(options.get(0).recomendada());assertTrue(options.get(1).recomendada());}
 @Test void deshabilitadaOPapelInsuficienteNoSeRecomiendanNiSePuedenIniciar(){machines(printer(first,250,true));for(var state:List.of(new OperacionImpresoraService.Estado(first,false,500,2,null,null),new OperacionImpresoraService.Estado(first,true,100,2,null,null))){when(runtime.estado(first)).thenReturn(state);var option=service.consultar(orderId,"operadora").items().get(0).impresoras().get(0);assertFalse(option.compatible());assertFalse(option.recomendada());assertThrows(ResponseStatusException.class,()->service.iniciar(orderId,new ProduccionController.Inicio(UUID.randomUUID(),1,itemId,first,"",true),"operadora"));}verify(jdbc,never()).queryForObject(startsWith("INSERT INTO lamontana.trabajo_impresion"),eq(Long.class),any(Object[].class));}
 @Test void unPagoRequeridoPendienteSigueBloqueandoAunqueLaImpresoraSeaCompatible(){machines(printer(first,250,true));when(order.condiciones().actuales().senaRequerida()).thenReturn("300.00");assertFalse(service.consultar(orderId,"operadora").items().get(0).puedeIniciar());assertThrows(ResponseStatusException.class,()->service.iniciar(orderId,new ProduccionController.Inicio(UUID.randomUUID(),1,itemId,first,"",true),"operadora"));verify(runtime,never()).asignar(any(),anyInt(),anyLong(),any(),anyString());}
}
