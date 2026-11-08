package ar.com.lamontana.configuracion;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.puntos.PuntoRetiroService;
import ar.com.lamontana.pedidos.PedidoService;
import ar.com.lamontana.catalogo.CatalogoService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Path;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Contrato con el esquema completo, incluidas migración, rutas, pausas y versiones inmutables. */
class RedPuntosIntegrationTest {
 static EmbeddedPostgres pg;static JdbcTemplate jdbc;static TransactionTemplate tx;
 ConfiguracionService configuracion;OrganizacionService organizacion;RedPuntosService red;
 UUID a,b;long actor,branchA,branchB;final String correo="admin@red.test";
 @BeforeAll static void start()throws Exception{
  pg=EmbeddedPostgres.builder().setServerConfig("unix_socket_directories","").start();
  Flyway.configure().dataSource(pg.getPostgresDatabase()).schemas("lamontana").defaultSchema("lamontana").locations("filesystem:"+Path.of("src/main/resources/db/migration").toAbsolutePath()).load().migrate();
  jdbc=new JdbcTemplate(pg.getPostgresDatabase());tx=new TransactionTemplate(new DataSourceTransactionManager(pg.getPostgresDatabase()));
 }
 @AfterAll static void stop()throws Exception{if(pg!=null)pg.close();}
 @BeforeEach void setup(){
  jdbc.execute("TRUNCATE lamontana.usuario RESTART IDENTITY CASCADE");a=UUID.randomUUID();b=UUID.randomUUID();
  actor=jdbc.queryForObject("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado,es_administrador_propietario) SELECT ?,id_rol,?,'hash','Admin','Red','ACTIVO',true FROM lamontana.rol WHERE codigo='ADMIN_ADMIN' RETURNING id_usuario",Long.class,UUID.randomUUID(),correo);
  branchA=branch(a,"A","La Plata");branchB=branch(b,"B","Floresta");
  configuracion=new ConfiguracionService(jdbc,new RecursosRepositorio(jdbc),new EntregaRepositorio(jdbc,new PuntosRepositorio(jdbc),new ZonasRepositorio(jdbc)));
  organizacion=new OrganizacionService(jdbc,null,null);red=new RedPuntosService(jdbc,configuracion,new PuntoEntregaService(jdbc,configuracion),organizacion);
 }
 long branch(UUID id,String code,String name){return jdbc.queryForObject("INSERT INTO lamontana.sucursal(id_usuario_alta,codigo_publico,codigo,nombre,calle,numero,localidad,provincia,codigo_postal,zona_horaria,estado) VALUES (?,?,?,?,'Calle','1','Localidad','Buenos Aires','1900','America/Argentina/Buenos_Aires','ACTIVA') RETURNING id_sucursal",Long.class,actor,id,code,name);}
 PuntoEntregaController.SucursalPunto ruta(UUID branch,int priority){return new PuntoEntregaController.SucursalPunto(branch,priority,true,"100.00",List.of(new PuntoEntregaController.Franja(1,"09:00","18:00",10,true)));}
 RedPuntosController.Guardar input(UUID op,long version,String nombre,List<PuntoEntregaController.SucursalPunto> rutas){return new RedPuntosController.Guardar(op,version,"CITY-BELL",nombre,"Centenario","1320","City Bell","Buenos Aires","1896",null,"America/Argentina/Buenos_Aires",rutas);}
 RedPuntosService.Punto create(List<PuntoEntregaController.SucursalPunto> rutas){return tx.execute(s->red.crear(input(UUID.randomUUID(),0,"City Bell",rutas),correo));}
 long versions(){return jdbc.queryForObject("SELECT count(*) FROM lamontana.version_red_punto",Long.class);}
 UUID active(){var id=UUID.randomUUID();jdbc.update("INSERT INTO lamontana.configuracion_version(codigo_publico,numero_version,id_usuario_creador,estado,modelo,version) VALUES (?,1,?,'ACTIVA','MANUAL',7)",id,actor);return id;}
 @Test void createsPointAfterBranchesWithoutRequiringAnyConfiguration(){
  var p=create(List.of(ruta(a,1)));assertThat(p.version()).isEqualTo(1);assertThat(p.revision()).isNotNull();
  assertThat(red.listar(correo).puntos()).hasSize(1);assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_version",Integer.class)).isZero();
  assertThat(new PuntoRetiroService(jdbc,null,null,null,null,null,null).cuentas(correo)).extracting(PuntoRetiroService.Punto::nombre).contains("City Bell");
 }
 @Test void replayOfCreateAndEditReturnsCurrentVersionWithoutDuplicatingIdentity(){
  var command=input(UUID.randomUUID(),0,"City Bell",List.of(ruta(a,1)));var p=tx.execute(s->red.crear(command,correo));
  var edit=input(UUID.randomUUID(),1,"City Bell nuevo",List.of(ruta(b,1)));tx.execute(s->red.editar(p.definicion().codigoPublico(),edit,correo));
  assertThat(tx.execute(s->red.crear(command,correo)).version()).isEqualTo(2);assertThat(versions()).isEqualTo(2);
  assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.punto_entrega",Integer.class)).isEqualTo(1);
 }
 @Test void sharedRoutesAndPrioritiesAreStoredAndCanBeReassigned(){
  var p=create(List.of(ruta(a,1),ruta(b,2)));var edit=input(UUID.randomUUID(),1,"City Bell",List.of(ruta(b,1)));
  tx.execute(s->red.editar(p.definicion().codigoPublico(),edit,correo));
  assertThat(new RedPuntosRepositorio(jdbc).punto(p.definicion().codigoPublico()).sucursales()).extracting(PuntoEntregaController.SucursalPunto::sucursal).containsExactly(b);
  assertThat(jdbc.queryForObject("SELECT definicion->'sucursales'->0->>'sucursal' FROM lamontana.version_red_punto WHERE version=1",String.class)).isEqualTo(a.toString());
 }
 @Test void rejectsStaleEditAndReusedOperationWithDifferentContent(){
  var p=create(List.of(ruta(a,1)));var edit=input(UUID.randomUUID(),1,"Editado",List.of(ruta(a,1)));tx.execute(s->red.editar(p.definicion().codigoPublico(),edit,correo));
  assertThatThrownBy(()->tx.execute(s->red.editar(p.definicion().codigoPublico(),input(UUID.randomUUID(),1,"Obsoleto",List.of(ruta(b,1))),correo))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
  assertThatThrownBy(()->tx.execute(s->red.editar(p.definicion().codigoPublico(),input(edit.operacion(),1,"Otro contenido",List.of(ruta(a,1))),correo))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");assertThat(versions()).isEqualTo(2);
 }
 @Test void pausesCascadeToExclusivePointAndSharedPointKeepsAnotherSource(){
  active();var p=create(List.of(ruta(a,1),ruta(b,2)));var pauses=new DisponibilidadSucursalService(jdbc,configuracion);
  tx.execute(s->pauses.cambiar(a,new DisponibilidadSucursalController.Cambiar(UUID.randomUUID(),0L,DisponibilidadSucursalController.Estado.PAUSADA),correo));
  var availability=new DisponibilidadPuntoService(jdbc,configuracion,organizacion);assertThat(availability.listar(correo).puntos().get(0).disponibleParaPedidos()).isTrue();
  tx.execute(s->red.editar(p.definicion().codigoPublico(),input(UUID.randomUUID(),1,"City Bell",List.of(ruta(a,1))),correo));
  assertThat(availability.listar(correo).puntos().get(0).disponibleParaPedidos()).isFalse();
 }
 @Test void newPointCanEnableDeliveryChannelWithoutRewritingGlobalConfiguration(){
  var code=active();create(List.of(ruta(a,1)));var base=configuracion.cargar(code);var actual=new RedPuntosRepositorio(jdbc).aplicar(base);
  assertThat(actual.entrega().puntos()).hasSize(1);assertThat(actual.entrega().modalidades()).contains(EntregaConfiguracionController.Modalidad.RETIRO_PUNTO_ENTREGA);
  assertThat(configuracion.cargar(code).version()).isEqualTo(7);assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_definicion_punto",Integer.class)).isZero();
 }
 @Test void changingNetworkInvalidatesPendingQuoteButPreservesOldRevision(){
  var p=create(List.of(ruta(a,1)));var oferta=mock(ar.com.lamontana.cotizaciones.CotizacionService.Oferta.class);
  when(oferta.modalidad()).thenReturn(EntregaConfiguracionController.Modalidad.RETIRO_PUNTO_ENTREGA);when(oferta.punto()).thenReturn(p.definicion().codigoPublico());when(oferta.revisionPunto()).thenReturn(p.revision());
  var cotizaciones=new ar.com.lamontana.cotizaciones.CotizacionService(jdbc,null,null,null,null);assertThat(cotizaciones.puntoVigente(oferta)).isTrue();
  tx.execute(s->red.editar(p.definicion().codigoPublico(),input(UUID.randomUUID(),1,"City Bell",List.of(ruta(b,1))),correo));assertThat(cotizaciones.puntoVigente(oferta)).isFalse();
  assertThatThrownBy(()->jdbc.update("UPDATE lamontana.version_red_punto SET version=5 WHERE version=1")).isInstanceOf(org.springframework.dao.DataAccessException.class);
 }
 @Test void invalidRoutesNeverPublishPartialPoint(){
  assertThatThrownBy(()->create(List.of())).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->create(List.of(ruta(a,1),ruta(b,1)))).isInstanceOf(ResponseStatusException.class);
  assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.punto_entrega",Integer.class)).isZero();
 }
 @Test void confirmedDispatchUsesItsImmutableRouteAfterPointIsReassigned(){
  active();var p=create(List.of(ruta(a,1)));long saved=new RedPuntosRepositorio(jdbc).idRevision(p.definicion().codigoPublico());
  tx.execute(s->red.editar(p.definicion().codigoPublico(),input(UUID.randomUUID(),1,"City Bell",List.of(ruta(b,1))),correo));
  assertThat(dispatch(saved,p.definicion().codigoPublico())).isTrue();
  assertThat(dispatch(new RedPuntosRepositorio(jdbc).idRevision(p.definicion().codigoPublico()),p.definicion().codigoPublico())).isFalse();
 }
 private boolean dispatch(long revision,UUID punto){
  var sql=mock(JdbcTemplate.class);when(sql.queryForObject(anyString(),eq(Boolean.class),eq(99L))).thenAnswer(call->{
   String query=call.getArgument(0);
   query=query.replace("lamontana.pedido_actual p","(SELECT 99::bigint AS id_pedido,1::bigint AS id_configuracion_version,"+branchA+"::bigint AS id_sucursal) p")
    .replace("lamontana.reserva_entrega r","(SELECT 99::bigint AS id_pedido,'ACTIVA'::text AS estado,'RETIRO_PUNTO_ENTREGA'::text AS modalidad,"+revision+"::bigint AS id_version_red_punto,'"+a+"'::uuid AS origen_publico,'"+punto+"'::uuid AS destino_publico) r");
   return jdbc.queryForObject(query,Boolean.class,99L);
  });
  return new PedidoService(sql,null,null,null,null,null,null,null,null,null).rutaDespachoHabilitada(99L);
 }
 @Test void migrationImportsLegacyNetworkWithoutChangingDraftOrIdentity()throws Exception{
  active();jdbc.update("INSERT INTO lamontana.configuracion_modalidad_entrega VALUES (1,'RETIRO_PUNTO_ENTREGA')");
  var id=UUID.randomUUID();long punto=jdbc.queryForObject("INSERT INTO lamontana.punto_entrega(codigo_publico,codigo,id_usuario_creador) VALUES (?,'CITY-BELL',?) RETURNING id_punto_entrega",Long.class,id,actor);
  jdbc.update("INSERT INTO lamontana.configuracion_definicion_punto VALUES (1,?,'City Bell','Centenario','1320','City Bell','Buenos Aires','1896',null,'America/Argentina/Buenos_Aires')",punto);
  long ruta=jdbc.queryForObject("INSERT INTO lamontana.configuracion_punto_entrega(id_configuracion_version,id_punto_entrega,id_sucursal,prioridad,costo,habilitado) VALUES (1,?,?,1,100,true) RETURNING id_configuracion_punto_entrega",Long.class,punto,branchA);
  jdbc.update("INSERT INTO lamontana.franja_entrega(id_configuracion_punto_entrega,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada) VALUES (?,1,'09:00','18:00',10,true)",ruta);
  jdbc.update("INSERT INTO lamontana.configuracion_version(codigo_publico,numero_version,id_usuario_creador,estado,modelo,version) VALUES (?,2,?,'EN_PREPARACION','MANUAL',9)",UUID.randomUUID(),actor);
  String migration=java.nio.file.Files.readString(Path.of("src/main/resources/db/migration/V63__red_operativa_puntos_entrega.sql"));jdbc.execute(migration.substring(migration.indexOf("INSERT INTO lamontana.version_red_punto"),migration.indexOf("CREATE FUNCTION")));
  assertThat(new RedPuntosRepositorio(jdbc).punto(id).sucursales()).extracting(PuntoEntregaController.SucursalPunto::sucursal).containsExactly(a);
  assertThat(jdbc.queryForObject("SELECT version FROM lamontana.configuracion_version WHERE estado='EN_PREPARACION'",Integer.class)).isEqualTo(9);assertThat(versions()).isEqualTo(1);
 }
 @Test void employeeCannotChangeNetworkEvenWithOperationalPermission(){
  jdbc.update("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado) SELECT ?,id_rol,'empleado@red.test','hash','Empleado','Red','ACTIVO' FROM lamontana.rol WHERE codigo='EMPLEADO'",UUID.randomUUID());
  assertThatThrownBy(()->tx.execute(s->red.crear(input(UUID.randomUUID(),0,"City Bell",List.of(ruta(a,1))),"empleado@red.test"))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");assertThat(versions()).isZero();
 }
 @Test void completingPrintersAddsMissingBranchesAndPreservesDraftEdits(){
  var source=active();UUID target=UUID.randomUUID();jdbc.update("INSERT INTO lamontana.configuracion_version(codigo_publico,numero_version,id_usuario_creador,estado,modelo,version) VALUES (?,2,?,'EN_PREPARACION','MANUAL',3)",target,actor);
  new CatalogoService(jdbc).todosPredefinidos(correo);long format=jdbc.queryForObject("SELECT min(id_formato) FROM lamontana.formato",Long.class);
  long printerA=printer(1,branchA,"Equipo A",format);long printerB=printer(1,branchB,"Equipo B",format);
  jdbc.update("INSERT INTO lamontana.configuracion_impresora(id_configuracion_version,id_impresora,nombre,admite_color,admite_doble_faz,capacidad_maxima_hojas,estado,id_sucursal_ubicacion) VALUES (2,?,'Equipo A editado',false,false,1200,'DESHABILITADA',?)",printerA,branchA);
  var copy=new CopiaParametrosConfiguracion(jdbc);assertThat(copy.completarImpresoras(1,2)).isEqualTo(1);assertThat(copy.completarImpresoras(1,2)).isZero();
  assertThat(jdbc.queryForObject("SELECT nombre FROM lamontana.configuracion_impresora WHERE id_configuracion_version=2 AND id_impresora=?",String.class,printerA)).isEqualTo("Equipo A editado");
  assertThat(jdbc.queryForObject("SELECT id_sucursal_ubicacion FROM lamontana.configuracion_impresora WHERE id_configuracion_version=2 AND id_impresora=?",Long.class,printerB)).isEqualTo(branchB);
  assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_impresora_formato WHERE id_configuracion_version=2 AND id_impresora=?",Integer.class,printerB)).isEqualTo(1);
 }
 long printer(long config,long branch,String name,long format){long id=jdbc.queryForObject("INSERT INTO lamontana.impresora(codigo_publico,id_usuario_creador) VALUES (?,?) RETURNING id_impresora",Long.class,UUID.randomUUID(),actor);jdbc.update("INSERT INTO lamontana.configuracion_impresora(id_configuracion_version,id_impresora,nombre,admite_color,admite_doble_faz,capacidad_maxima_hojas,estado,id_sucursal_ubicacion) VALUES (?,?,?,true,true,2000,'OPERATIVA',?)",config,id,name,branch);jdbc.update("INSERT INTO lamontana.configuracion_impresora_formato VALUES (?,?,?)",config,id,format);return id;}
}
