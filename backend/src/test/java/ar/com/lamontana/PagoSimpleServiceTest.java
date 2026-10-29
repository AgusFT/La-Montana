package ar.com.lamontana;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad;
import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** Service boundary with an in-memory JDBC double; real transaction/SQL checks live in DatosPruebaIntegrationTest. */
class PagoSimpleServiceTest {
 private final UUID quote=UUID.randomUUID(),branch=UUID.randomUUID();
 private JdbcTemplate jdbc;private OrganizacionService org;private CoberturaPagos coverage;private CobroPedidoService deadlines;private PagoService service;
 private final Map<UUID,Object[]> events=new LinkedHashMap<>();private final Map<UUID,Object[]> payments=new LinkedHashMap<>();
 private BigDecimal applied;private String state;private Instant expires;private List<MedioPago> admitted;
 @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
  events.clear();payments.clear();applied=BigDecimal.ZERO;state="VIGENTE";expires=Instant.now().plusSeconds(3600);admitted=List.of(MedioPago.TRANSFERENCIA);
  jdbc=mock(JdbcTemplate.class);org=mock(OrganizacionService.class);coverage=mock(CoberturaPagos.class);deadlines=mock(CobroPedidoService.class);
  var operational=mock(OfertaOperativaService.class);when(operational.leer()).thenReturn(new OfertaOperativaService.Contexto(null,null,List.of(),List.of(),List.of()));
  when(org.contexto(anyString())).thenReturn(new OrganizacionService.Contexto(List.of(),List.of("ACREDITAR_PAGO","REGISTRAR_COBRO")));
  when(coverage.acreditado(eq(1L),anyList())).thenAnswer(i->((List<MedioPago>)i.getArgument(1)).contains(MedioPago.TRANSFERENCIA)?applied:BigDecimal.ZERO);
  when(jdbc.query(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{
   String sql=i.getArgument(0);Object[] args=(Object[])i.getRawArguments()[2];RowMapper<?> mapper=i.getArgument(1);
   if(sql.startsWith("SELECT u.id_usuario"))return rows(mapper,Map.of(1,7L,2,"Cajero de prueba",3,"ADMIN_ADMIN"));
   if(sql.startsWith("SELECT c.*,s.codigo_publico"))return rows(mapper,Map.of("id_cotizacion",1L,"id_usuario_creador",2L,"sucursal",branch,"sucursal_estado","ACTIVA","estado",state,"vigente_hasta",Timestamp.from(expires),"aceptada_en",Timestamp.from(Instant.now().minusSeconds(3600)),"oferta",offer()));
   if(sql.startsWith("SELECT tipo,huella,id_actor")){var e=events.get(args[0]);return e==null?List.of():rows(mapper,Map.of(1,e[5],2,e[6],3,e[2]));}
   if(sql.startsWith("SELECT * FROM lamontana.pago_disponible")){var p=payments.get(args[0]);return p==null?List.of():rows(mapper,funds(p));}
   if(sql.startsWith("SELECT p.codigo_publico FROM lamontana.pago")){var result=new ArrayList<Object>();for(var p:payments.values())result.addAll(rows(mapper,Map.of(1,p[0])));return result;}
   // Other read models are empty; monetary assertions below observe the writes themselves.
   return List.of();
  });
  when(jdbc.queryForObject(anyString(),any(Class.class),any(Object[].class))).thenAnswer(i->{
   String sql=i.getArgument(0);Class<?> type=i.getArgument(1);Object[] args=(Object[])i.getRawArguments()[2];
   if(sql.startsWith("INSERT INTO lamontana.evento_financiero")){events.put((UUID)args[0],new Object[]{args[0],args[1],args[2],args[3],args[4],args[5],args[6],args[7]});return (long)events.size();}
   if(type==Boolean.class)return sql.startsWith("SELECT lamontana.cotizacion_descendiente")||sql.startsWith("SELECT EXISTS(SELECT 1 FROM lamontana.pago WHERE clave_referencia")&&payments.values().stream().anyMatch(p->p[7].equals(args[0]));
   if(type==UUID.class)return quote;
   if(type==String.class)return "Cliente de prueba";
   if(type==Long.class)return applied.signum()>0?2L:1L;
   return null;
  });
  when(jdbc.queryForObject(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{
   RowMapper<?> mapper=i.getArgument(1);var p=payments.values().iterator().next();return rows(mapper,Map.of(1,p[6],2,p[8])).get(0);
  });
  when(jdbc.update(anyString(),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);Object[] args=(Object[])i.getRawArguments()[1];
   if(sql.startsWith("INSERT INTO lamontana.pago("))payments.put((UUID)args[0],args.clone());
   if(sql.startsWith("INSERT INTO lamontana.aplicacion_pago_cotizacion"))applied=applied.add((BigDecimal)args[3]);return 1;
  });
  service=new PagoService(jdbc,org,operational,new EvaluadorFinanciero(),coverage,deadlines);
 }
 private String offer(){
  var conditions=new CotizacionService.Condiciones(true,false,"0.00","300.00","700.00","ANTES_APROBACION_Y_PRODUCCION",admitted,List.of(),null);
  return JsonMapper.builder().build().writeValueAsString(new CotizacionService.Oferta(UUID.randomUUID(),1,UUID.randomUUID(),1,new OfertaOperativaService.Sucursal(branch,"Matriz","CABA"),List.of(),Modalidad.RETIRO_SUCURSAL,null,null,"Matriz",MedioPago.TRANSFERENCIA,"1000.00","0.00","1000.00",conditions,null,List.of(MedioPago.EFECTIVO,MedioPago.TRANSFERENCIA),true));
 }
 private Map<Object,Object> funds(Object[] p){return Map.of("id_pago",10L,"codigo_publico",p[0],"id_cotizacion",1L,"medio",p[4],"importe",p[5],"devuelto",BigDecimal.ZERO,"aplicado",applied,"disponible",((BigDecimal)p[5]).subtract(applied));}
 private List<Object> rows(RowMapper<?> mapper,Map<?,?> values) throws Exception {
  ResultSet rs=mock(ResultSet.class);when(rs.getObject(anyInt(),eq(UUID.class))).thenAnswer(i->values.get(i.getArgument(0)));
  when(rs.getObject(anyString(),eq(UUID.class))).thenAnswer(i->values.get(i.getArgument(0)));
  when(rs.getLong(anyString())).thenAnswer(i->((Number)values.getOrDefault(i.getArgument(0),null)).longValue());
  when(rs.getLong(anyInt())).thenAnswer(i->((Number)values.get(i.getArgument(0))).longValue());
  when(rs.getString(anyString())).thenAnswer(i->Objects.toString(values.get(i.getArgument(0)),null));when(rs.getString(anyInt())).thenAnswer(i->Objects.toString(values.get(i.getArgument(0)),null));
  when(rs.getTimestamp(anyString())).thenAnswer(i->values.get(i.getArgument(0)));when(rs.getTimestamp(anyInt())).thenAnswer(i->values.get(i.getArgument(0)));
  when(rs.getBigDecimal(anyString())).thenAnswer(i->values.get(i.getArgument(0)));return List.of(mapper.mapRow(rs,0));
 }
 private PagoController.Recibir command(String amount,String reference,boolean apply){return new PagoController.Recibir(UUID.randomUUID(),null,MedioPago.TRANSFERENCIA,amount,reference,null,true,"",apply);}
 @Test void confirmaYAplicaConDosEventosDelMismoResponsable(){
  var result=service.recibir(quote,command("300.00","",true),"caja");assertEquals("300.00",result.aplicado());assertEquals("700.00",result.pendiente());assertEquals(2,events.size());
  var facts=new ArrayList<>(events.values());assertEquals("RECIBIR",facts.get(0)[5]);assertEquals("APLICAR",facts.get(1)[5]);for(var e:facts){assertEquals(7L,e[2]);assertEquals("Cajero de prueba",e[3]);assertFalse(((String)e[7]).isBlank());}
  var payment=payments.values().iterator().next();assertTrue(payment[6].toString().startsWith("Registro interno "));assertNotNull(payment[8]);verify(deadlines).movimiento(eq(1L),any(),eq(7L),eq("RECIBIR"),anyString());verify(deadlines).movimiento(eq(1L),any(),eq(7L),eq("APLICAR"),anyString());
 }
 @Test void recuperarLaMismaConfirmacionNoDuplicaDineroNiAplicacion(){var cmd=command("300.00",null,true);service.recibir(quote,cmd,"caja");service.recibir(quote,cmd,"caja");assertEquals(1,payments.size());assertEquals(2,events.size());assertEquals(new BigDecimal("300.00"),applied);}
 @Test void conservaExcedenteSinDescontarMasQueElSaldo(){var v=service.recibir(quote,command("1250.00","",true),"caja");assertEquals("0.00",v.pendiente());assertEquals("250.00",v.pagos().get(0).disponible());}
 @Test void unMedioNoAdmitidoParaLaSenaSoloCubreElSaldoPosterior(){var cmd=new PagoController.Recibir(UUID.randomUUID(),null,MedioPago.EFECTIVO,"1000.00","",null,true,"",true);var v=service.recibir(quote,cmd,"caja");assertEquals("700.00",v.aplicado());assertEquals("300.00",v.pagos().get(0).disponible());}
 @Test void laMismaOperacionConOtroImporteSeRechaza(){var cmd=command("300.00",null,true);service.recibir(quote,cmd,"caja");var changed=new PagoController.Recibir(cmd.operacion(),null,cmd.medio(),"400.00",null,null,true,"",true);assertThrows(ResponseStatusException.class,()->service.recibir(quote,changed,"caja"));assertEquals(2,events.size());assertEquals(1,payments.size());}
 @Test void conservaDineroDeOfertaVencidaSinAplicarlo(){expires=Instant.now().minusSeconds(60);var v=service.recibir(quote,command("300.00","",true),"caja");assertEquals("0.00",v.aplicado());assertEquals(1,events.size());}
 @Test void elModoAnteriorSoloRegistraYLaReferenciaBancariaSigueEvitandoDuplicados(){var cmd=command("300.00","BANCO-123",false);service.recibir(quote,cmd,"caja");service.recibir(quote,new PagoController.Recibir(cmd.operacion(),null,cmd.medio(),cmd.importe(),cmd.referencia(),cmd.recibidoEn(),true,cmd.motivo(),null),"caja");assertEquals(1,events.size());assertEquals(BigDecimal.ZERO,applied);assertThrows(ResponseStatusException.class,()->service.recibir(quote,command("300.00","BANCO-123",true),"caja"));assertEquals(1,payments.size());}
 @Test void exigePermisoYConfirmacionAntesDeRegistrar(){var cmd=new PagoController.Recibir(UUID.randomUUID(),null,MedioPago.TRANSFERENCIA,"300.00","",null,false,"",true);assertThrows(ResponseStatusException.class,()->service.recibir(quote,cmd,"caja"));assertTrue(events.isEmpty());doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(org).exigirPermiso("sin-permiso","ACREDITAR_PAGO");assertThrows(ResponseStatusException.class,()->service.recibir(quote,command("300.00","",true),"sin-permiso"));assertTrue(payments.isEmpty());}
}
