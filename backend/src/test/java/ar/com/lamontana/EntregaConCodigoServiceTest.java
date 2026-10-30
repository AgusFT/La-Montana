package ar.com.lamontana;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import ar.com.lamontana.entregas.*;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.CoberturaPagos;
import ar.com.lamontana.pedidos.PedidoService;
import java.sql.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

class EntregaConCodigoServiceTest {
 private final UUID id=UUID.randomUUID(),codeId=UUID.randomUUID(),validationId=UUID.randomUUID();private JdbcTemplate jdbc;private OrganizacionService org;private EntregaService service;private CodigoRetiroService codes;private long version;private String state,logistics;private boolean paid,correct,validated,used;private int attempts;private final Map<UUID,Object[]> ops=new HashMap<>();private final List<Object[]> events=new ArrayList<>();
 @BeforeEach @SuppressWarnings("unchecked") void setup()throws Exception{
  version=4;state="LISTO_PARA_ENTREGA";logistics="DISPONIBLE_PUNTO";paid=true;correct=true;validated=false;used=false;attempts=0;
  jdbc=mock(JdbcTemplate.class);org=mock(OrganizacionService.class);var orders=mock(PedidoService.class);var coverage=mock(CoberturaPagos.class);var encoder=mock(PasswordEncoder.class);when(encoder.matches(anyString(),anyString())).thenAnswer(i->correct);when(org.contexto(anyString())).thenReturn(new OrganizacionService.Contexto(List.of(),List.of("GESTIONAR_ENTREGAS","CERRAR_PEDIDOS")));when(coverage.acreditado(anyLong(),anyList())).thenReturn(new BigDecimal("100"));
  when(orders.detalle(eq(id),anyString(),eq(true))).thenAnswer(i->{var p=mock(PedidoService.Detalle.class,RETURNS_DEEP_STUBS);when(p.numero()).thenReturn(7L);when(p.version()).thenReturn(version);when(p.estado()).thenReturn(state);when(p.oferta().total()).thenReturn("100.00");when(p.reserva().modalidad()).thenReturn(ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad.RETIRO_PUNTO_ENTREGA);return p;});
  when(jdbc.queryForObject(anyString(),eq(String.class),any(Object[].class))).thenAnswer(i->logistics);when(jdbc.queryForObject(anyString(),eq(Boolean.class),any(Object[].class))).thenAnswer(i->paid);when(jdbc.queryForObject(anyString(),eq(BigDecimal.class),any(Object[].class))).thenReturn(BigDecimal.ZERO);
  when(jdbc.queryForObject(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{var rs=mock(ResultSet.class);String sql=i.getArgument(0);when(rs.getLong(1)).thenReturn(11L);when(rs.getString(2)).thenReturn(sql.startsWith("SELECT u.nombre")?"cliente@prueba.local":"Operadora");when(rs.getString(1)).thenReturn("Cliente");when(rs.getString(3)).thenReturn("EMPLEADO");return ((RowMapper<?>)i.getArgument(1)).mapRow(rs,0);});
  when(jdbc.queryForObject(anyString(),eq(Long.class),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);if(sql.startsWith("INSERT INTO lamontana.historial_estado_pedido")){Object[] a=(Object[])i.getRawArguments()[2];events.add(a.clone());ops.put((UUID)a[2],new Object[]{7L,11L,a[3]});return 20L;}return sql.contains("informes_pendientes")?0L:1L;});
  when(jdbc.query(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);Object[] a=(Object[])i.getRawArguments()[2];if(sql.startsWith("SELECT id_pedido")){var op=ops.get(a[0]);return op==null?List.of():Collections.singletonList(op);}var rs=mock(ResultSet.class);
   if(sql.startsWith("SELECT * FROM lamontana.codigo_entrega")){when(rs.getLong("id_codigo_entrega")).thenReturn(1L);when(rs.getObject("codigo_publico",UUID.class)).thenReturn(codeId);when(rs.getString("codigo_hash")).thenReturn("HASH");when(rs.getLong("id_reserva_entrega")).thenReturn(1L);when(rs.getTimestamp("emitido_en")).thenReturn(Timestamp.from(Instant.now().minusSeconds(86400)));when(rs.getTimestamp("vence_en")).thenReturn(Timestamp.from(Instant.now().plusSeconds(86400)));when(rs.getTimestamp("utilizado_en")).thenReturn(used?Timestamp.from(Instant.now()):null);when(rs.getInt("intentos")).thenReturn(attempts);return Collections.singletonList(((RowMapper<?>)i.getArgument(1)).mapRow(rs,0));}
   if(sql.startsWith("SELECT codigo_publico,fecha,valido_hasta")&&validated){when(rs.getObject(1,UUID.class)).thenReturn(validationId);when(rs.getTimestamp(2)).thenReturn(Timestamp.from(Instant.now().minusSeconds(1)));when(rs.getTimestamp(3)).thenReturn(Timestamp.from(Instant.now().plusSeconds(300)));return Collections.singletonList(((RowMapper<?>)i.getArgument(1)).mapRow(rs,0));}
   return List.of();});
  when(jdbc.update(anyString(),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);Object[] a=(Object[])i.getRawArguments()[1];if(sql.startsWith("UPDATE lamontana.codigo_entrega SET intentos"))attempts++;if(sql.startsWith("UPDATE lamontana.codigo_entrega SET utilizado"))used=true;if(sql.startsWith("UPDATE lamontana.pedido SET estado")){state=(String)a[0];version++;}if(sql.startsWith("INSERT INTO lamontana.validacion_codigo_entrega")){validated="VALIDO".equals(a[7]);ops.put((UUID)a[4],new Object[]{7L,11L,a[5]});}return 1;});
  codes=mock(CodigoRetiroService.class);service=new EntregaService(jdbc,orders,org,coverage,encoder,codes);
 }
 private EntregaController.Recibir command(){return new EntregaController.Recibir(UUID.randomUUID(),4,"ABCD2345",null,null,true);}
 @Test void codigoSoloEntregaSinNombreNotaNiValidacionPrevia(){var in=command();var out=service.recibir(id,in,"operadora");assertTrue(out.aceptado());assertEquals("ENTREGADO",out.vista().estadoPedido());assertEquals(1,events.size());assertEquals("ENTREGAR",events.get(0)[9]);assertEquals("Operadora",events.get(0)[11]);assertTrue(used);assertTrue(service.recibir(id,in,"operadora").aceptado());assertEquals(1,events.size());}
 @Test void codigoIncorrectoNoEntregaYPreservaUnSoloIntento(){correct=false;var in=command();assertFalse(service.recibir(id,in,"operadora").aceptado());assertEquals(1,attempts);assertFalse(service.recibir(id,in,"operadora").aceptado());assertEquals(1,attempts);assertTrue(events.isEmpty());assertFalse(used);}
 @Test void saldoOLlegadaPendientesNoSeOmitenPorPresentarCodigo(){paid=false;assertThrows(ResponseStatusException.class,()->service.recibir(id,command(),"operadora"));paid=true;logistics="EN_VIAJE";assertThrows(ResponseStatusException.class,()->service.recibir(id,command(),"operadora"));assertTrue(events.isEmpty());assertFalse(validated);}
 @Test void permisoYConfirmacionSiguenSiendoNecesarios(){doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(org).exigirPermiso("operadora","GESTIONAR_ENTREGAS");assertThrows(ResponseStatusException.class,()->service.recibir(id,command(),"operadora"));doNothing().when(org).exigirPermiso("operadora","GESTIONAR_ENTREGAS");var in=command();assertThrows(ResponseStatusException.class,()->service.recibir(id,new EntregaController.Recibir(in.operacion(),4,in.codigo(),null,null,false),"operadora"));assertTrue(events.isEmpty());}
 @Test void despachoSinNotaEmiteAutomaticamenteConAuditoriaDelResponsable(){logistics="PREPARADO";service.mover(id,new EntregaController.Movimiento(UUID.randomUUID(),4,EntregaController.Accion.SALIR_REPARTO,null,true),"operadora");verify(codes).asegurar(7);assertEquals("SALIR_REPARTO",events.get(0)[9]);assertEquals("Operadora",events.get(0)[11]);assertFalse(((String)events.get(0)[5]).isBlank());}
 @Test void consultaInternaNoDevuelveElTextoDelCodigo(){assertNull(service.consultar(id,"operadora",true).codigo().texto());}
}
