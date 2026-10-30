package ar.com.lamontana;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.configuracion.RecursosRepositorio;
import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.CoberturaPagos;
import ar.com.lamontana.pedidos.PedidoService;
import ar.com.lamontana.produccion.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.*;
import org.springframework.web.server.ResponseStatusException;

/** Real service transitions against a JDBC double; transaction rollback is covered by the integration scenario. */
class FinalizarCalidadServiceTest {
 private final UUID orderId=UUID.randomUUID(),jobId=UUID.randomUUID(),itemId=UUID.randomUUID();
 private JdbcTemplate jdbc;private OrganizacionService org;private ProduccionService service;
 private long version;private String state,jobState,quality;private boolean belongs;
 private final Map<UUID,Object[]> operations=new HashMap<>();private final List<Object[]> events=new ArrayList<>();
 private final ProduccionController.Checklist checks=new ProduccionController.Checklist(true,true,true,true,true);
 @BeforeEach @SuppressWarnings("unchecked") void setup()throws Exception{
  version=4;state="EN_PRODUCCION";jobState="IMPRIMIENDO";quality=null;belongs=true;
  jdbc=mock(JdbcTemplate.class);org=mock(OrganizacionService.class);var orders=mock(PedidoService.class);var resources=mock(RecursosRepositorio.class);var coverage=mock(CoberturaPagos.class);
  var p=mock(PedidoService.Detalle.class,RETURNS_DEEP_STUBS);when(p.numero()).thenReturn(7L);when(p.version()).thenAnswer(i->version);when(p.estado()).thenAnswer(i->state);
  var item=mock(CotizacionService.ItemCotizado.class);when(item.codigoPublico()).thenReturn(itemId);when(p.oferta().items()).thenReturn(List.of(item));when(p.archivos()).thenReturn(List.of(new PedidoService.Pdf(itemId,UUID.randomUUID(),"trabajo.pdf","a".repeat(64),100,1,null)));
  when(p.condiciones().cotizadas().pagoPrevioRequerido()).thenReturn("0.00");when(p.condiciones().cotizadas().senaRequerida()).thenReturn("0.00");when(p.condiciones().actuales().pagoPrevioRequerido()).thenReturn("0.00");when(p.condiciones().actuales().senaRequerida()).thenReturn("0.00");
  when(p.reserva().modalidad()).thenReturn(ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad.RETIRO_SUCURSAL);
  var offer=p.oferta();var pdfs=p.archivos();var conditions=p.condiciones();var reservation=p.reserva();
  when(orders.detalle(eq(orderId),anyString(),eq(true))).thenAnswer(i->{var snapshot=mock(PedidoService.Detalle.class,RETURNS_DEEP_STUBS);when(snapshot.numero()).thenReturn(7L);when(snapshot.version()).thenReturn(version);when(snapshot.estado()).thenReturn(state);when(snapshot.oferta()).thenReturn(offer);when(snapshot.archivos()).thenReturn(pdfs);when(snapshot.condiciones()).thenReturn(conditions);when(snapshot.reserva()).thenReturn(reservation);return snapshot;});when(org.contexto(anyString())).thenReturn(new OrganizacionService.Contexto(List.of(),List.of("GESTIONAR_PRODUCCION","CONTROLAR_CALIDAD")));
  when(coverage.acreditado(anyLong(),anyList())).thenReturn(BigDecimal.ZERO);when(resources.leer(anyLong())).thenReturn(new RecursosRepositorio.Recursos(null,List.of()));
  when(jdbc.queryForObject(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{var rs=mock(ResultSet.class);when(rs.getLong(1)).thenReturn(11L);when(rs.getString(2)).thenReturn("Inspectora");when(rs.getString(3)).thenReturn("EMPLEADO");return ((RowMapper<?>)i.getArgument(1)).mapRow(rs,0);});
  when(jdbc.queryForObject(anyString(),eq(Boolean.class),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);return sql.startsWith("SELECT EXISTS")||sql.contains("calidad_pedido_aprobada")&&"APROBADO".equals(quality);});
  when(jdbc.queryForObject(anyString(),eq(Long.class),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);if(sql.startsWith("INSERT INTO lamontana.historial_estado_pedido")){Object[] a=(Object[])i.getRawArguments()[2];events.add(a.clone());operations.put((UUID)a[2],new Object[]{7L,11L,a[3]});return (long)events.size();}return 1L;});
  when(jdbc.query(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);Object[] a=(Object[])i.getRawArguments()[2];
   if(sql.startsWith("SELECT id_pedido")){var op=operations.get(a[0]);return op==null?List.of():Collections.singletonList(op);}
   if(sql.startsWith("SELECT t.*")){if(!belongs)return List.of();var rs=mock(ResultSet.class);when(rs.getLong("id_trabajo_impresion")).thenReturn(20L);when(rs.getObject("codigo_publico",UUID.class)).thenReturn(jobId);when(rs.getString("estado")).thenReturn(jobState);when(rs.getTimestamp("fecha_inicio")).thenReturn(Timestamp.from(Instant.now()));return Collections.singletonList(((RowMapper<?>)i.getArgument(1)).mapRow(rs,0));}
   if(sql.startsWith("SELECT * FROM lamontana.control_calidad")&&quality!=null){var rs=mock(ResultSet.class);when(rs.getString("resultado")).thenReturn(quality);when(rs.getTimestamp("fecha")).thenReturn(Timestamp.from(Instant.now()));return Collections.singletonList(((RowMapper<?>)i.getArgument(1)).mapRow(rs,0));}
   return List.of();});
  when(jdbc.update(anyString(),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);Object[] a=(Object[])i.getRawArguments()[1];if(sql.startsWith("UPDATE lamontana.trabajo_impresion"))jobState=(String)a[0];if(sql.startsWith("UPDATE lamontana.pedido SET estado")){state=(String)a[0];version++;}if(sql.startsWith("INSERT INTO lamontana.control_calidad"))quality=(String)a[3];return 1;});
  service=new ProduccionService(jdbc,orders,org,mock(ArchivosPrivados.class),coverage,resources,mock(OperacionImpresoraService.class));
 }
 private ProduccionController.Inspeccion command(){return new ProduccionController.Inspeccion(UUID.randomUUID(),version,ProduccionController.Resultado.APROBADO,checks,"",true);}
 @Test void unaConfirmacionTerminaEInspeccionaConDosEventosYVersionesConsecutivas(){var result=service.finalizarCalidad(orderId,jobId,command(),"operadora");assertEquals("LISTO_PARA_ENTREGA",result.estado());assertEquals(6,result.version());assertEquals("COMPLETADO",jobState);assertEquals("APROBADO",quality);assertEquals(2,events.size());assertEquals("COMPLETAR_TRABAJO",events.get(0)[9]);assertEquals("CONTROLAR_CALIDAD",events.get(1)[9]);assertEquals(5L,events.get(0)[8]);assertEquals(6L,events.get(1)[8]);for(var e:events){assertEquals(11L,e[1]);assertNotNull(e[6]);assertEquals("Inspectora",e[11]);}}
 @Test void recuperarNoDuplicaFinNiInspeccionYCambiarContenidoGeneraConflicto(){var in=command();service.finalizarCalidad(orderId,jobId,in,"operadora");assertEquals(6,service.finalizarCalidad(orderId,jobId,in,"operadora").version());assertEquals(2,events.size());var changed=new ProduccionController.Inspeccion(in.operacion(),in.version(),in.resultado(),checks,"Cambio",true);assertThrows(ResponseStatusException.class,()->service.finalizarCalidad(orderId,jobId,changed,"operadora"));assertEquals(2,events.size());}
 @Test void ambosPermisosSonNecesariosAntesDeTerminar(){for(String permission:List.of("GESTIONAR_PRODUCCION","CONTROLAR_CALIDAD")){doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(org).exigirPermiso("operadora",permission);assertThrows(ResponseStatusException.class,()->service.finalizarCalidad(orderId,jobId,command(),"operadora"));assertTrue(events.isEmpty());assertEquals("IMPRIMIENDO",jobState);reset(org);}}
 @Test void checklistConfirmacionYVersionInvalidosNoTerminanLaImpresion(){var in=command();for(var bad:List.of(new ProduccionController.Inspeccion(in.operacion(),3,in.resultado(),checks,"",true),new ProduccionController.Inspeccion(in.operacion(),4,in.resultado(),checks,"",false),new ProduccionController.Inspeccion(in.operacion(),4,in.resultado(),new ProduccionController.Checklist(true,false,true,true,true),"",true))){assertThrows(ResponseStatusException.class,()->service.finalizarCalidad(orderId,jobId,bad,"operadora"));assertTrue(events.isEmpty());assertEquals("IMPRIMIENDO",jobState);}}
 @Test void inspeccionConProblemaExigeMotivoYPermiteReimpresion(){var in=command();var missing=new ProduccionController.Inspeccion(in.operacion(),4,ProduccionController.Resultado.REIMPRESION_REQUERIDA,checks,"",true);assertThrows(ResponseStatusException.class,()->service.finalizarCalidad(orderId,jobId,missing,"operadora"));assertTrue(events.isEmpty());var noted=new ProduccionController.Inspeccion(in.operacion(),4,missing.resultado(),checks,"Hojas manchadas",true);var out=service.finalizarCalidad(orderId,jobId,noted,"operadora");assertEquals("EN_PRODUCCION",out.estado());assertTrue(out.items().get(0).puedeIniciar());assertEquals(2,events.size());}
 @Test void noTerminaUnTrabajoAjenoNiUnTrabajoYaTerminado(){belongs=false;assertThrows(ResponseStatusException.class,()->service.finalizarCalidad(orderId,jobId,command(),"operadora"));assertTrue(events.isEmpty());belongs=true;jobState="COMPLETADO";assertThrows(ResponseStatusException.class,()->service.finalizarCalidad(orderId,jobId,command(),"operadora"));assertTrue(events.isEmpty());}
 @Test void laInspeccionSeparadaSigueDisponibleParaTrabajosTerminadosAntes(){jobState="COMPLETADO";var out=service.calidad(orderId,jobId,command(),"inspectora");assertEquals("LISTO_PARA_ENTREGA",out.estado());assertEquals(1,events.size());assertEquals("CONTROLAR_CALIDAD",events.get(0)[9]);}
}
