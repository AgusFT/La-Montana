package ar.com.lamontana;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.produccion.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.*;
import org.springframework.web.server.ResponseStatusException;

/** Service checks with a JDBC double; database rollback/concurrency are integration concerns. */
class OperacionImpresoraServiceTest {
 private final UUID id=UUID.randomUUID();private JdbcTemplate jdbc;private OrganizacionService org;private OperacionImpresoraService service;
 private OperacionImpresoraService.Estado state;private String configured;private final Map<UUID,Object[]> events=new LinkedHashMap<>();
 @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
  state=null;configured="OPERATIVA";events.clear();jdbc=mock(JdbcTemplate.class);org=mock(OrganizacionService.class);service=new OperacionImpresoraService(jdbc,org);
  when(jdbc.query(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);RowMapper<?> mapper=i.getArgument(1);Object[] args=(Object[])i.getRawArguments()[2];
   if(sql.startsWith("SELECT i.id_impresora"))return row(mapper,Map.of(1,1L,2,configured));
   if(sql.startsWith("SELECT o.*")){if(state==null)return List.of();var values=new HashMap<Object,Object>();values.put("habilitada",state.habilitada());values.put("hojas_registradas",state.hojas());values.put("version",state.version());values.put("actualizada_en",Timestamp.from(state.actualizadaEn()));values.put("actor",state.actor());return row(mapper,values);}
   if(sql.startsWith("SELECT id_impresora,id_actor,huella")){Object[] event=events.get(args[0]);return event==null?List.of():row(mapper,Map.of(1,event[1],2,event[2],3,event[6]));}
   return List.of();
  });
  when(jdbc.queryForObject(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->row(i.getArgument(1),Map.of(1,7L,2,"Operadora",3,"EMPLEADO")).get(0));
  when(jdbc.update(anyString(),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);Object[] args=(Object[])i.getRawArguments()[1];if(sql.startsWith("INSERT INTO lamontana.operacion_impresora"))state=new OperacionImpresoraService.Estado(id,(boolean)args[1],(Integer)args[2],(long)args[3],Instant.now(),"Operadora");if(sql.startsWith("INSERT INTO lamontana.evento_operacion_impresora"))events.put((UUID)args[0],args.clone());return 1;});
 }
 private List<Object> row(RowMapper<?> mapper,Map<?,?> values) throws Exception {
  ResultSet rs=mock(ResultSet.class);when(rs.getLong(anyInt())).thenAnswer(i->((Number)values.get(i.getArgument(0))).longValue());when(rs.getLong(anyString())).thenAnswer(i->((Number)values.get(i.getArgument(0))).longValue());
  when(rs.getString(anyInt())).thenAnswer(i->values.get(i.getArgument(0)));when(rs.getString(anyString())).thenAnswer(i->values.get(i.getArgument(0)));
  when(rs.getBoolean(anyString())).thenAnswer(i->values.get(i.getArgument(0)));when(rs.getObject(anyString(),eq(Integer.class))).thenAnswer(i->values.get(i.getArgument(0)));
  when(rs.getTimestamp(anyString())).thenAnswer(i->values.get(i.getArgument(0)));return List.of(mapper.mapRow(rs,0));
 }
 private OperacionImpresoraController.Cambio command(OperacionImpresoraController.Accion action,Integer hojas){return new OperacionImpresoraController.Cambio(UUID.randomUUID(),state==null?0:state.version(),action,hojas,"");}
 @Test void sinConteoNoInventaPapel(){var s=service.estado(id);assertNull(s.hojas());assertEquals(0,s.version());assertTrue(s.habilitada());assertThrows(ResponseStatusException.class,()->service.cambiar(id,command(OperacionImpresoraController.Accion.RECARGAR,100),"operator"));assertTrue(events.isEmpty());}
 @Test void contarYRecargarConservanValoresResponsableYVersion(){service.cambiar(id,command(OperacionImpresoraController.Accion.CONTAR,150),"operator");var v=service.cambiar(id,command(OperacionImpresoraController.Accion.RECARGAR,50),"operator");assertEquals(200,v.hojas());assertEquals(2,v.version());assertEquals(2,events.size());var e=new ArrayList<>(events.values()).get(1);assertEquals(7L,e[2]);assertEquals("Operadora",e[3]);assertEquals("RECARGAR",e[5]);assertEquals(150,e[9]);assertEquals(200,e[10]);assertEquals(50,e[11]);}
 @Test void reintentarUnaRecargaNoLaSumaOtraVez(){service.cambiar(id,command(OperacionImpresoraController.Accion.CONTAR,100),"operator");var cmd=command(OperacionImpresoraController.Accion.RECARGAR,50);service.cambiar(id,cmd,"operator");service.cambiar(id,cmd,"operator");assertEquals(150,state.hojas());assertEquals(2,events.size());var changed=new OperacionImpresoraController.Cambio(cmd.operacion(),cmd.version(),cmd.accion(),100,"");assertThrows(ResponseStatusException.class,()->service.cambiar(id,changed,"operator"));}
 @Test void unaVersionViejaNoSobrescribeOtraRecarga(){var old=command(OperacionImpresoraController.Accion.CONTAR,20);service.cambiar(id,command(OperacionImpresoraController.Accion.CONTAR,100),"operator");assertThrows(ResponseStatusException.class,()->service.cambiar(id,old,"operator"));assertEquals(100,state.hojas());}
 @Test void iniciarAsignaHojasUnaSolaVezYSinValoresNegativos(){service.cambiar(id,command(OperacionImpresoraController.Accion.CONTAR,50),"operator");UUID op=UUID.randomUUID();service.asignar(id,50,9,op,"operator");service.asignar(id,50,9,op,"operator");assertEquals(0,state.hojas());assertEquals(2,events.size());assertThrows(ResponseStatusException.class,()->service.asignar(id,1,10,UUID.randomUUID(),"operator"));assertEquals(2,events.size());}
 @Test void iniciarSinConteoConservaElDesconocimiento(){service.asignar(id,50,9,UUID.randomUUID(),"operator");assertNull(state.hojas());assertEquals("ASIGNAR_TRABAJO",events.values().iterator().next()[5]);}
 @Test void deshabilitarBloqueaNuevosTrabajosYHabilitarRespetaElMotor(){service.cambiar(id,command(OperacionImpresoraController.Accion.DESHABILITAR,null),"operator");assertFalse(state.habilitada());assertThrows(ResponseStatusException.class,()->service.asignar(id,1,9,UUID.randomUUID(),"operator"));configured="DESHABILITADA";assertThrows(ResponseStatusException.class,()->service.cambiar(id,command(OperacionImpresoraController.Accion.HABILITAR,null),"operator"));assertFalse(state.habilitada());}
 @Test void permisoIncorrectoYRecargaInvalidaNoEscriben(){doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(org).exigirPermiso("sin-permiso","GESTIONAR_IMPRESORAS");assertThrows(ResponseStatusException.class,()->service.cambiar(id,command(OperacionImpresoraController.Accion.CONTAR,1),"sin-permiso"));assertTrue(events.isEmpty());assertThrows(ResponseStatusException.class,()->service.cambiar(id,command(OperacionImpresoraController.Accion.CONTAR,-1),"operator"));assertTrue(events.isEmpty());}
}
