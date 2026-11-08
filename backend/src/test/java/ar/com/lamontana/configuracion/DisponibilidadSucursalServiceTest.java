package ar.com.lamontana.configuracion;

import static org.assertj.core.api.Assertions.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

class DisponibilidadSucursalServiceTest {
    static EmbeddedPostgres pg;
    JdbcTemplate jdbc;TransactionTemplate tx;DisponibilidadSucursalService service;
    UUID a=UUID.randomUUID(),b=UUID.randomUUID();
    UUID config=UUID.randomUUID(),punto=UUID.randomUUID();
    @BeforeAll static void start()throws Exception { pg=EmbeddedPostgres.builder().setServerConfig("unix_socket_directories","").start(); }
    @AfterAll static void stop()throws Exception { if(pg!=null)pg.close(); }
    @BeforeEach void setup()throws Exception {
        var ds=pg.getPostgresDatabase();jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("DROP SCHEMA IF EXISTS lamontana CASCADE; CREATE SCHEMA lamontana");
        jdbc.execute("CREATE TABLE lamontana.rol(id_rol bigint PRIMARY KEY,codigo text,activo boolean); CREATE TABLE lamontana.usuario(id_usuario bigint PRIMARY KEY,id_rol bigint,correo text,estado text,nombre text,apellido text)");
        jdbc.execute("INSERT INTO lamontana.rol VALUES(1,'ADMIN_ADMIN',true),(2,'EMPLEADO',true); INSERT INTO lamontana.usuario VALUES(1,1,'admin@prueba.local','ACTIVO','Ale','Admin'),(2,2,'empleado@prueba.local','ACTIVO','Marina','Empleado'),(3,1,'otro@prueba.local','ACTIVO','Otro','Admin')");
        jdbc.execute("CREATE TABLE lamontana.sucursal(id_sucursal bigint PRIMARY KEY,codigo_publico uuid,codigo text,nombre text,calle text,numero text,localidad text,provincia text,estado text)");
        jdbc.update("INSERT INTO lamontana.sucursal VALUES(1,?,'A','La Plata','Calle 7','1280','La Plata','Buenos Aires','ACTIVA'),(2,?,'B','Medicina','Paraguay','2155','CABA','CABA','ACTIVA')",a,b);
        jdbc.execute("CREATE TABLE lamontana.pedido(id_pedido bigint,estado text); INSERT INTO lamontana.pedido VALUES(133,'LISTO_PARA_ENTREGA')");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V62__pausa_temporal_sucursal.sql")));
        service=new DisponibilidadSucursalService(jdbc,new ConfiguracionService(jdbc,null,null));
    }
    DisponibilidadSucursalController.Cambiar change(long version,boolean paused) { return new DisponibilidadSucursalController.Cambiar(UUID.randomUUID(),version,paused?DisponibilidadSucursalController.Estado.PAUSADA:DisponibilidadSucursalController.Estado.ABIERTA); }
    DisponibilidadSucursalService.Disponibilidad send(UUID branch,DisponibilidadSucursalController.Cambiar c) { return send(branch,c,"admin@prueba.local"); }
    DisponibilidadSucursalService.Disponibilidad send(UUID branch,DisponibilidadSucursalController.Cambiar c,String actor) { return tx.execute(s->service.cambiar(branch,c,actor)); }
    int events(){return jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_disponibilidad_sucursal",Integer.class);}
    @Test void startsOpenWithoutChangingStructuralBranchState() {
        assertThat(service.listar("admin@prueba.local")).hasSize(2).allMatch(s->s.estado().equals("ABIERTA")&&s.version()==0);
    }
    @Test void pauseIsAuditedAndPreservesConfirmedOrderAndCounterAccess() {
        var result=send(a,change(0,true));assertThat(result.actor()).isEqualTo("Ale Admin");assertThat(result.version()).isEqualTo(1);
        assertThat(result.actualizadaEn()).isNotNull();assertThat(events()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT estado FROM lamontana.sucursal WHERE id_sucursal=1",String.class)).isEqualTo("ACTIVA");
        assertThat(jdbc.queryForObject("SELECT estado FROM lamontana.pedido WHERE id_pedido=133",String.class)).isEqualTo("LISTO_PARA_ENTREGA");
        assertThat(service.listar("admin@prueba.local")).hasSize(2);
    }
    @Test void pausedBranchDisappearsFromQuoteAndConfirmationOfferAndReappearsAfterResume() {
        var oferta=new OfertaOperativaService(null,null,null,jdbc);
        assertThat(oferta.sucursalesDisponibles()).hasSize(2);send(a,change(0,true));
        assertThat(oferta.sucursalesDisponibles()).extracting(OfertaOperativaService.Sucursal::codigoPublico).containsExactly(b);
        send(a,change(1,false));assertThat(oferta.sucursalesDisponibles()).hasSize(2);
    }
    @Test void pauseGuardRejectsNewReservationAtSourceOrDestinationAndAllowsOtherBranch() {
        send(a,change(0,true));
        assertThatThrownBy(()->DisponibilidadSucursalService.exigirAtencion(jdbc,a)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        assertThatCode(()->DisponibilidadSucursalService.exigirAtencion(jdbc,b)).doesNotThrowAnyException();
        send(a,change(1,false));assertThatCode(()->DisponibilidadSucursalService.exigirAtencion(jdbc,a)).doesNotThrowAnyException();
    }
    @Test void staleVersionCannotOverwriteNewDecision() {
        send(a,change(0,true));assertThatThrownBy(()->send(a,change(0,false))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        assertThat(events()).isEqualTo(1);
    }
    @Test void repeatedOperationReturnsCurrentStateAndNeverReappliesOldPause() {
        var command=change(0,true);send(a,command);send(a,change(1,false));var replay=send(a,command);
        assertThat(replay.estado()).isEqualTo("ABIERTA");assertThat(replay.version()).isEqualTo(2);assertThat(events()).isEqualTo(2);
    }
    @Test void operationCannotBeReusedWithDifferentBranchActorOrContent() {
        var command=change(0,true);send(a,command);
        assertThatThrownBy(()->send(b,command)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->send(a,command,"otro@prueba.local")).isInstanceOf(ResponseStatusException.class);
        var other=new DisponibilidadSucursalController.Cambiar(command.operacion(),0L,DisponibilidadSucursalController.Estado.ABIERTA);
        assertThatThrownBy(()->send(a,other)).isInstanceOf(ResponseStatusException.class);assertThat(events()).isEqualTo(1);
    }
    @Test void employeeAndInactiveAdminCannotManageGlobalPauses() {
        assertThatThrownBy(()->send(a,change(0,true),"empleado@prueba.local")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        jdbc.update("UPDATE lamontana.usuario SET estado='DESACTIVADO' WHERE id_usuario=1");
        assertThatThrownBy(()->service.listar("admin@prueba.local")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        assertThat(events()).isZero();
    }
    @Test void deactivatedBranchIsNotReopenedByTemporaryAvailability() {
        jdbc.update("UPDATE lamontana.sucursal SET estado='DESACTIVADA' WHERE id_sucursal=1");
        assertThatThrownBy(()->send(a,change(0,true))).isInstanceOf(ResponseStatusException.class);
        assertThat(service.listar("admin@prueba.local")).hasSize(1);assertThat(events()).isZero();
    }
    @Test void concurrentDistinctCommandsHaveOneWinner()throws Exception {
        var pool=Executors.newFixedThreadPool(2);try {
            var c1=change(0,true);var c2=change(0,true);var start=new CountDownLatch(1);
            Callable<Boolean> one=()->{start.await();try{send(a,c1);return true;}catch(ResponseStatusException e){return false;}};
            Callable<Boolean> two=()->{start.await();try{send(a,c2);return true;}catch(ResponseStatusException e){return false;}};
            var f1=pool.submit(one);var f2=pool.submit(two);start.countDown();assertThat(List.of(f1.get(10,TimeUnit.SECONDS),f2.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }finally{pool.shutdownNow();}
        assertThat(events()).isEqualTo(1);
    }
    @Test void concurrentRetriesHaveOneAuditEvent()throws Exception {
        var pool=Executors.newFixedThreadPool(2);try {
            var c=change(0,true);var f1=pool.submit(()->send(a,c));var f2=pool.submit(()->send(a,c));
            assertThat(f1.get(10,TimeUnit.SECONDS).estado()).isEqualTo("PAUSADA");assertThat(f2.get(10,TimeUnit.SECONDS).estado()).isEqualTo("PAUSADA");
        }finally{pool.shutdownNow();}
        assertThat(events()).isEqualTo(1);
    }

    private ConfiguracionService.Borrador red(boolean compartido) {
        jdbc.execute("ALTER TABLE lamontana.sucursal ADD COLUMN zona_horaria text DEFAULT 'America/Argentina/Buenos_Aires'");
        jdbc.execute("CREATE TABLE lamontana.configuracion_version(id_configuracion_version bigint PRIMARY KEY,codigo_publico uuid,numero_version bigint,estado text); CREATE TABLE lamontana.punto_entrega(id_punto_entrega bigint PRIMARY KEY,codigo_publico uuid,codigo text)");
        jdbc.update("INSERT INTO lamontana.configuracion_version VALUES(1,?,4,'ACTIVA'); INSERT INTO lamontana.punto_entrega VALUES(1,?,'CITY-BELL')",config,punto);
        jdbc.execute("CREATE TABLE lamontana.configuracion_punto_entrega(id_configuracion_punto_entrega bigint PRIMARY KEY,id_configuracion_version bigint,id_punto_entrega bigint,id_sucursal bigint,prioridad int,habilitado boolean)");
        jdbc.execute("CREATE TABLE lamontana.franja_entrega(id_configuracion_punto_entrega bigint,habilitada boolean,capacidad_pedidos int)");
        jdbc.execute("INSERT INTO lamontana.configuracion_punto_entrega VALUES(1,1,1,1,1,true); INSERT INTO lamontana.franja_entrega VALUES(1,true,16)");
        if(compartido)jdbc.execute("INSERT INTO lamontana.configuracion_punto_entrega VALUES(2,1,1,2,2,true); INSERT INTO lamontana.franja_entrega VALUES(2,true,16)");
        jdbc.execute("CREATE TABLE lamontana.disponibilidad_punto_entrega(id_punto_entrega bigint PRIMARY KEY,estado text,version bigint,id_actor bigint,fecha_actualizacion timestamptz); INSERT INTO lamontana.disponibilidad_punto_entrega VALUES(1,'HABILITADO',1,1,clock_timestamp())");
        jdbc.execute("CREATE TABLE lamontana.configuracion_definicion_punto(id_configuracion_version bigint,id_punto_entrega bigint,nombre text,calle text,numero text,localidad text,provincia text,zona_horaria text); INSERT INTO lamontana.configuracion_definicion_punto VALUES(1,1,'City Bell','Centenario','1320','City Bell','Buenos Aires','America/Argentina/Buenos_Aires')");
        var franja=new PuntoEntregaController.Franja(1,"14:00","18:00",16,true);
        var rutas=new ArrayList<PuntoEntregaController.SucursalPunto>();rutas.add(new PuntoEntregaController.SucursalPunto(a,1,true,"100",List.of(franja)));
        if(compartido)rutas.add(new PuntoEntregaController.SucursalPunto(b,2,true,"200",List.of(franja)));
        var p=new PuntosRepositorio.Punto(punto,"CITY-BELL","City Bell","Centenario","1320","City Bell","Buenos Aires","1896",null,"America/Argentina/Buenos_Aires",rutas);
        var entrega=new EntregaRepositorio.Entrega("2","1",List.of(EntregaConfiguracionController.Modalidad.RETIRO_SUCURSAL,EntregaConfiguracionController.Modalidad.RETIRO_PUNTO_ENTREGA),List.of(new EntregaRepositorio.Horario(a,"America/Argentina/Buenos_Aires",List.of(),List.of(franja)),new EntregaRepositorio.Horario(b,"America/Argentina/Buenos_Aires",List.of(),List.of(franja))),List.of(p),List.of(),a,true);
        return new ConfiguracionService.Borrador(config,4,7,"ACTIVA",ConfiguracionController.Modelo.MANUAL,null,null,null,"Ale",null,null,null,null,null,entrega,null);
    }
    private Set<UUID> disponibles(){return new HashSet<>(new OfertaOperativaService(null,null,null,jdbc).sucursalesDisponibles().stream().map(OfertaOperativaService.Sucursal::codigoPublico).toList());}
    @Test void exclusivePointAndBranchRemainVisibleButDisabledWhenSourceIsPaused() {
        var c=red(false);send(a,change(0,true));var catalogo=new DisponibilidadDestinos(jdbc).catalogo(c,disponibles(),null);
        assertThat(catalogo).filteredOn(d->d.codigoPublico().equals(punto)||d.codigoPublico().equals(a)).hasSize(2).allMatch(d->!d.disponible()&&d.motivo().equals(DisponibilidadDestinos.FUERA_DE_SERVICIO));
    }
    @Test void sharedPointRemainsAvailableOnlyThroughOpenSource() {
        var c=red(true);send(a,change(0,true));var p=new DisponibilidadDestinos(jdbc).catalogo(c,disponibles(),null).stream().filter(d->d.codigoPublico().equals(punto)).findFirst().orElseThrow();
        assertThat(p.disponible()).isTrue();assertThat(p.sucursales()).filteredOn(DisponibilidadDestinos.Origen::disponible).extracting(DisponibilidadDestinos.Origen::codigoPublico).containsExactly(b);
        var privado=new DisponibilidadDestinos(jdbc).catalogo(c,disponibles(),a);assertThat(privado).allMatch(d->!d.disponible());
    }
    @Test void allSharedSourcesPausedDisablePointAndResumingOneRestoresIt() {
        var c=red(true);send(a,change(0,true));send(b,change(0,true));
        assertThat(new DisponibilidadDestinos(jdbc).catalogo(c,disponibles(),null)).allMatch(d->!d.disponible());
        send(b,change(1,false));assertThat(new DisponibilidadDestinos(jdbc).catalogo(c,disponibles(),null)).filteredOn(d->d.codigoPublico().equals(punto)).allMatch(DisponibilidadDestinos.Destino::disponible);
    }
    @Test void explicitPointPauseSurvivesBranchResume() {
        var c=red(true);jdbc.update("UPDATE lamontana.disponibilidad_punto_entrega SET estado='DESHABILITADO'");send(a,change(0,true));send(a,change(1,false));
        assertThat(new DisponibilidadDestinos(jdbc).catalogo(c,disponibles(),null)).filteredOn(d->d.codigoPublico().equals(punto)).allMatch(d->!d.disponible());
    }
    @Test void disabledRouteOrZeroCapacityCannotBecomeImplicitBackup() {
        var c=red(true);send(a,change(0,true));jdbc.update("UPDATE lamontana.configuracion_punto_entrega SET habilitado=false WHERE id_sucursal=2");
        assertThat(new DisponibilidadDestinos(jdbc).catalogo(c,disponibles(),null)).filteredOn(d->d.codigoPublico().equals(punto)).allMatch(d->!d.disponible());
        jdbc.update("UPDATE lamontana.configuracion_punto_entrega SET habilitado=true WHERE id_sucursal=2");jdbc.update("UPDATE lamontana.franja_entrega SET capacidad_pedidos=0 WHERE id_configuracion_punto_entrega=2");
        assertThat(new DisponibilidadDestinos(jdbc).catalogo(c,disponibles(),null)).filteredOn(d->d.codigoPublico().equals(punto)).allMatch(d->!d.disponible());
    }
    @Test void quoteRoutesExcludePausedSourcesBeforeChoosingProductionOrigin() {
        var c=red(true);var entregas=new EntregaConfiguracionService(jdbc,null,null,null);
        assertThat(entregas.puntosCotizacion(c)).hasSize(2);send(a,change(0,true));
        assertThat(entregas.puntosCotizacion(c)).extracting(EntregaConfiguracionService.PuntoDisponible::sucursal).containsExactly(b);
        send(b,change(0,true));assertThat(entregas.puntosCotizacion(c)).isEmpty();
    }
    @Test void operationalPointPanelShowsInheritedPauseWithoutChangingManualDecisionOrVersion() {
        red(false);var organizacion=org.mockito.Mockito.mock(ar.com.lamontana.organizacion.OrganizacionService.class);
        var puntos=new DisponibilidadPuntoService(jdbc,new ConfiguracionService(jdbc,null,null),organizacion);
        assertThat(puntos.listar("admin@prueba.local").puntos().get(0).disponibleParaPedidos()).isTrue();send(a,change(0,true));
        var punto=puntos.listar("admin@prueba.local").puntos().get(0);
        assertThat(punto.disponibleParaPedidos()).isFalse();assertThat(punto.disponibilidad().estado()).isEqualTo("HABILITADO");assertThat(punto.disponibilidad().version()).isEqualTo(1);
        send(a,change(1,false));assertThat(puntos.listar("admin@prueba.local").puntos().get(0).disponibleParaPedidos()).isTrue();
    }
}

