package ar.com.lamontana;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.CobroPedidoService;
import ar.com.lamontana.pedidos.*;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.*;
import org.springframework.web.server.ResponseStatusException;

/** Exercise the service boundary, including engine denial before any review writes. */
class OperacionSimpleServiceTest {
 private final UUID id=UUID.randomUUID(),quote=UUID.randomUUID();
 private JdbcTemplate jdbc;private PedidoService pedidos;private OrganizacionService organization;private ArchivosPrivados files;private CobroPedidoService money;private RevisionPedidoService service;
 private CotizacionService.Oferta offer;private PedidoService.Pdf pdf;
 private long version;private boolean approved;private Timestamp reviewed;
 private final Map<UUID,Object[]> operations=new HashMap<>();private final List<Object[]> events=new ArrayList<>();
 @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
  jdbc=mock(JdbcTemplate.class);pedidos=mock(PedidoService.class);organization=mock(OrganizacionService.class);files=mock(ArchivosPrivados.class);money=mock(CobroPedidoService.class);
  offer=mock(CotizacionService.Oferta.class,RETURNS_DEEP_STUBS);when(offer.items()).thenReturn(List.of(mock(CotizacionService.ItemCotizado.class)));
  pdf=new PedidoService.Pdf(UUID.randomUUID(),UUID.randomUUID(),"trabajo.pdf","a".repeat(64),100,1,null);version=1;approved=false;reviewed=null;
  when(pedidos.detalle(eq(id),anyString(),eq(true))).thenAnswer(i->snapshot());
  when(jdbc.queryForObject(anyString(),eq(Boolean.class),any(Object[].class))).thenReturn(true);
  when(jdbc.queryForObject(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{
   String sql=i.getArgument(0);ResultSet rs=mock(ResultSet.class);
   when(rs.getLong(1)).thenReturn(11L);when(rs.getString(2)).thenReturn("Responsable");when(rs.getString(3)).thenReturn("ADMIN_ADMIN");
   if(sql.startsWith("SELECT p.revisada_en"))when(rs.getTimestamp(1)).thenReturn(reviewed);
   return ((RowMapper<?>)i.getArgument(1)).mapRow(rs,0);
  });
  when(jdbc.query(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{Object[] args=(Object[])i.getRawArguments()[2];Object[] op=operations.get(args[0]);return op==null?List.of():Collections.singletonList(op);});
  when(jdbc.update(anyString(),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);Object[] args=(Object[])i.getRawArguments()[1];
   if(sql.startsWith("UPDATE lamontana.pedido SET revisada_en")){reviewed=(Timestamp)args[0];version++;}
   if(sql.startsWith("UPDATE lamontana.pedido SET estado='APROBADO'")){approved=true;version++;}
   if(sql.startsWith("INSERT INTO lamontana.historial_estado_pedido")){events.add(args.clone());operations.put((UUID)args[2],new Object[]{7L,11L,args[3]});}
   return 1;
  });
  service=new RevisionPedidoService(jdbc,pedidos,organization,files,money);
 }
 private PedidoService.Detalle snapshot(){return new PedidoService.Detalle(id,7,quote,approved?"APROBADO":"PENDIENTE_REVISION",Instant.now(),null,UUID.randomUUID(),4,offer,null,null,null,List.of(pdf),List.of(),version,"ACTIVA",null,"NO_DISPONIBLE",false,null);}
 private RevisionPedidoController.Decision command(boolean confirmed,String note){return new RevisionPedidoController.Decision(UUID.randomUUID(),version,RevisionPedidoController.Accion.APROBAR,confirmed,note,null);}
 @Test void unaConfirmacionGuardaRevisionYAprobacionConAuditoriaSeparada() {
  var result=service.decidir(id,command(true,""),"admin@prueba.local");
  assertEquals("APROBADO",result.estado());assertEquals(3,result.version());assertNotNull(reviewed);assertEquals(2,events.size());
  assertEquals("REVISAR",events.get(0)[9]);assertEquals("APROBAR",events.get(1)[9]);assertEquals(2L,events.get(0)[8]);assertEquals(3L,events.get(1)[8]);assertEquals(11L,events.get(0)[1]);assertEquals(11L,events.get(1)[1]);
  verify(money,atLeastOnce()).exigirAprobable(7);
 }
 @Test void niSenaNiTotalPendientesSeOmitenPorLaConfirmacion() {
  for(String reason:List.of("Falta seña acreditada y aplicada","Falta total previo acreditado y aplicado")){
   doThrow(new ResponseStatusException(HttpStatus.CONFLICT,reason)).when(money).exigirAprobable(7);
   var error=assertThrows(ResponseStatusException.class,()->service.decidir(id,command(true,""),"admin@prueba.local"));assertEquals(reason,error.getReason());assertNull(reviewed);assertFalse(approved);assertTrue(events.isEmpty());
  }
 }
 @Test void confirmacionHumanaEsObligatoriaAunqueElPagoEsteCubierto(){assertThrows(ResponseStatusException.class,()->service.decidir(id,command(false,""),"admin@prueba.local"));assertTrue(events.isEmpty());assertNull(reviewed);}
 @Test void unPdfNoVerificableNoRegistraRevisionNiAprobacion() throws Exception {doThrow(new IOException("original ausente")).when(files).original(any(),anyString(),anyLong());assertThrows(ResponseStatusException.class,()->service.decidir(id,command(true,""),"admin@prueba.local"));assertTrue(events.isEmpty());assertNull(reviewed);}
 @Test void permisoYVersionSeVerificanAntesDeDecidir(){var c=command(true,"");version++;assertThrows(ResponseStatusException.class,()->service.decidir(id,c,"admin@prueba.local"));assertTrue(events.isEmpty());doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(organization).exigirPermiso(anyString(),eq("GESTIONAR_PEDIDOS"));assertThrows(ResponseStatusException.class,()->service.decidir(id,command(true,""),"caja@prueba.local"));assertTrue(events.isEmpty());}
 @Test void recuperarLaMismaAprobacionNoDuplicaHechos(){var c=command(true,"");service.decidir(id,c,"admin@prueba.local");assertEquals(3,service.decidir(id,c,"admin@prueba.local").version());assertEquals(2,events.size());var changed=new RevisionPedidoController.Decision(c.operacion(),c.version(),c.accion(),true,"Otro contenido",null);assertThrows(ResponseStatusException.class,()->service.decidir(id,changed,"admin@prueba.local"));assertEquals(2,events.size());}
 @Test void unaRevisionAnteriorSeConserva(){reviewed=Timestamp.from(Instant.now().minusSeconds(60));var previous=reviewed;service.decidir(id,command(true,null),"admin@prueba.local");assertEquals(previous,reviewed);assertEquals(1,events.size());assertEquals("APROBAR",events.get(0)[9]);}
 @Test void rechazarSigueExigiendoMotivo(){var c=new RevisionPedidoController.Decision(UUID.randomUUID(),version,RevisionPedidoController.Accion.RECHAZAR,false,"","Archivo ilegible");assertThrows(ResponseStatusException.class,()->service.decidir(id,c,"admin@prueba.local"));assertTrue(events.isEmpty());}
}
