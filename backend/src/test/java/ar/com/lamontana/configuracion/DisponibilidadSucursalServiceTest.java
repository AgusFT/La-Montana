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
}
