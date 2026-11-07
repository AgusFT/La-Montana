package ar.com.lamontana.entregas;

import static org.junit.jupiter.api.Assertions.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Ejecuta la consulta real en PostgreSQL con datos mínimos de custodia y entrega. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JornadaRiderServiceTest {
    private EmbeddedPostgres postgres;
    private JdbcTemplate jdbc;
    private JornadaRiderService servicio;
    // Ya es 8 de noviembre UTC; en las sucursales argentinas todavía es día 7.
    private final Instant ahora=Instant.parse("2026-11-08T01:00:00Z");

    @BeforeAll void iniciar()throws Exception{
        postgres=EmbeddedPostgres.builder().setServerConfig("unix_socket_directories","").start();
        jdbc=new JdbcTemplate(postgres.getPostgresDatabase());servicio=new JornadaRiderService(jdbc);
    }
    @AfterAll void cerrar()throws Exception{if(postgres!=null)postgres.close();}
    @BeforeEach void datos(){
        jdbc.execute("""
            DROP SCHEMA IF EXISTS lamontana CASCADE;
            CREATE SCHEMA lamontana;
            CREATE TABLE lamontana.usuario(id_usuario bigint PRIMARY KEY,correo text,estado text);
            CREATE TABLE lamontana.cuenta_repartidor(id_usuario bigint,activa boolean);
            CREATE TABLE lamontana.sucursal(id_sucursal bigint,zona_horaria text);
            CREATE TABLE lamontana.recorrido_reparto(id_recorrido bigint,codigo_publico uuid,id_repartidor bigint,estado text,fecha_asignacion timestamptz,actor_asignador_nombre text);
            CREATE TABLE lamontana.recorrido_reparto_pedido(id_recorrido bigint,id_pedido bigint,orden int,presentado_en timestamptz);
            CREATE TABLE lamontana.pedido_actual(id_pedido bigint,codigo_publico uuid,id_cotizacion bigint,id_sucursal bigint,id_configuracion_version bigint,estado text,logistica text,receptor text,telefono text,version bigint);
            CREATE TABLE lamontana.destinatario_cotizacion(id_cotizacion bigint,nombre text);
            CREATE TABLE lamontana.reserva_entrega(id_reserva_entrega bigint,id_pedido bigint,modalidad text,nombre_destino text,franja_desde timestamptz,franja_hasta timestamptz,destino_publico uuid);
            CREATE TABLE lamontana.punto_entrega(id_punto_entrega bigint,codigo_publico uuid);
            CREATE TABLE lamontana.configuracion_definicion_punto(id_punto_entrega bigint,id_configuracion_version bigint,calle text,numero text,localidad text,provincia text);
            CREATE TABLE lamontana.entrega(id_pedido bigint,id_reserva_entrega bigint,fecha_entrega timestamptz);
            CREATE TABLE lamontana.historial_estado_pedido(id_evento bigint,fecha timestamptz);
            CREATE TABLE lamontana.movimiento_entrega(id_pedido bigint,id_reserva_entrega bigint,id_evento_pedido bigint,estado_destino text);
            CREATE FUNCTION lamontana.estado_logistico(bigint) RETURNS text LANGUAGE sql STABLE AS
              'SELECT logistica FROM lamontana.pedido_actual WHERE id_pedido=$1';
            INSERT INTO lamontana.usuario VALUES(1,'rider@prueba.local','ACTIVO'),(2,'otro@prueba.local','ACTIVO'),(3,'punto@prueba.local','ACTIVO');
            INSERT INTO lamontana.cuenta_repartidor VALUES(1,true),(2,true);
            INSERT INTO lamontana.sucursal VALUES(1,'America/Argentina/Buenos_Aires');
            INSERT INTO lamontana.configuracion_definicion_punto VALUES(1,1,'Av. Santa Fe','3200','Palermo','CABA');
            """);
        jdbc.update("INSERT INTO lamontana.recorrido_reparto VALUES(100,?,1,'EN_REPARTO',?,'Administración'),(200,?,2,'EN_REPARTO',?,'Administración')",id(100),Timestamp.from(ahora.minusSeconds(86400*5)),id(200),Timestamp.from(ahora.minusSeconds(86400*5)));
        jdbc.update("INSERT INTO lamontana.punto_entrega VALUES(1,?)",id(500));
    }

    private UUID id(long id){return new UUID(0,id);}
    private void pedido(long id,String modalidad,String logistica,String finalizado){pedido(id,modalidad,logistica,finalizado,100);}
    private void pedido(long id,String modalidad,String logistica,String finalizado,long recorrido){
        String estado=logistica.equals("ENTREGADO")?"ENTREGADO":"LISTO_PARA_ENTREGA";
        jdbc.update("INSERT INTO lamontana.pedido_actual VALUES(?,?,?,1,1,?,?,null,null,3)",id,id(id),id,estado,logistica);
        jdbc.update("INSERT INTO lamontana.destinatario_cotizacion VALUES(?,?)",id,"Cliente "+id);
        jdbc.update("INSERT INTO lamontana.recorrido_reparto_pedido VALUES(?,?,?,null)",recorrido,id,(int)id);
        jdbc.update("INSERT INTO lamontana.reserva_entrega VALUES(?,?,?,'Punto Palermo',?,?,?)",id,id,modalidad,Timestamp.from(ahora.minusSeconds(86400*2)),Timestamp.from(ahora.minusSeconds(86400)),id(500));
        if(finalizado!=null){
            var fecha=Timestamp.from(Instant.parse(finalizado));
            if(modalidad.equals("ENVIO_DOMICILIO"))jdbc.update("INSERT INTO lamontana.entrega VALUES(?,?,?)",id,id,fecha);
            else{jdbc.update("INSERT INTO lamontana.historial_estado_pedido VALUES(?,?)",id,fecha);jdbc.update("INSERT INTO lamontana.movimiento_entrega VALUES(?,?,?,'DISPONIBLE_PUNTO')",id,id,id);}
        }
    }
    private List<RiderController.Pedido> pedidos(){return servicio.consultar("rider@prueba.local",ahora).stream().flatMap(r->r.pedidos().stream()).toList();}
    private List<Long> numeros(){return pedidos().stream().map(RiderController.Pedido::numero).toList();}

    @Test void pendientesAnterioresSiguenActualesYLasFinalizacionesViejasNoSeCargan(){
        pedido(1,"ENVIO_DOMICILIO","EN_VIAJE",null);
        pedido(2,"ENVIO_DOMICILIO","ENTREGADO","2026-11-07T18:00:00Z");
        pedido(3,"ENVIO_DOMICILIO","ENTREGADO","2026-11-06T18:00:00Z");
        assertEquals(List.of(1L,2L),numeros());
        assertNull(pedidos().get(0).finalizadoEn());assertNotNull(pedidos().get(1).finalizadoEn());
    }

    @Test void hoySeDeterminaPorLaSucursalYRespetaAmbosLimitesDelDia(){
        pedido(1,"ENVIO_DOMICILIO","ENTREGADO","2026-11-07T03:00:00Z");
        pedido(2,"ENVIO_DOMICILIO","ENTREGADO","2026-11-08T00:30:00Z");
        pedido(3,"ENVIO_DOMICILIO","ENTREGADO","2026-11-07T02:59:59Z");
        pedido(4,"ENVIO_DOMICILIO","ENTREGADO","2026-11-08T03:00:00Z");
        assertEquals(List.of(1L,2L),numeros());
    }

    @Test void clienteAusenteYPresentacionPendienteSiguenEnLaJornada(){
        pedido(1,"ENVIO_DOMICILIO","CLIENTE_AUSENTE",null);
        pedido(2,"RETIRO_PUNTO_ENTREGA","EN_VIAJE",null);
        jdbc.update("UPDATE lamontana.recorrido_reparto_pedido SET presentado_en=? WHERE id_pedido=2",Timestamp.from(ahora.minusSeconds(60)));
        assertEquals(List.of(1L,2L),numeros());assertNotNull(pedidos().get(1).presentadoEn());assertNull(pedidos().get(1).finalizadoEn());
    }

    @Test void retiroDelClienteHoyNoConvierteUnaRecepcionDeAyerEnTrabajoDelRiderDeHoy(){
        pedido(1,"RETIRO_PUNTO_ENTREGA","ENTREGADO","2026-11-06T18:00:00Z");
        jdbc.update("INSERT INTO lamontana.entrega VALUES(1,1,?)",Timestamp.from(ahora.minusSeconds(60)));
        assertTrue(numeros().isEmpty());
    }

    @Test void recepcionesDePuntosYSucursalesSeConsideranCompletadasAlConfirmarseLaLlegada(){
        pedido(1,"RETIRO_PUNTO_ENTREGA","DISPONIBLE_PUNTO","2026-11-07T18:00:00Z");
        pedido(2,"RETIRO_SUCURSAL","DISPONIBLE_PUNTO","2026-11-07T19:00:00Z");
        assertEquals(List.of(1L,2L),numeros());assertNotNull(pedidos().get(1).finalizadoEn());
    }

    @Test void cadaRiderSoloConsultaSusAsignaciones(){
        pedido(1,"ENVIO_DOMICILIO","EN_VIAJE",null);
        pedido(2,"ENVIO_DOMICILIO","EN_VIAJE",null,200);
        assertEquals(List.of(1L),numeros());
        assertEquals(List.of(2L),servicio.consultar("otro@prueba.local",ahora).stream().flatMap(r->r.pedidos().stream()).map(RiderController.Pedido::numero).toList());
    }

    @Test void cuentaDePuntoYRiderDesactivadoNoPuedenConsultar(){
        var punto=assertThrows(ResponseStatusException.class,()->servicio.consultar("punto@prueba.local",ahora));assertEquals(HttpStatus.FORBIDDEN,punto.getStatusCode());
        jdbc.update("UPDATE lamontana.cuenta_repartidor SET activa=false WHERE id_usuario=1");
        assertThrows(ResponseStatusException.class,()->servicio.consultar("rider@prueba.local",ahora));
    }

    @Test void canceladosYRecorridosCanceladosNoAparecenComoEntregados(){
        pedido(1,"ENVIO_DOMICILIO","ENTREGADO","2026-11-07T18:00:00Z");
        jdbc.update("UPDATE lamontana.pedido_actual SET estado='CANCELADO' WHERE id_pedido=1");
        assertTrue(numeros().isEmpty());
        jdbc.update("UPDATE lamontana.pedido_actual SET estado='ENTREGADO' WHERE id_pedido=1");
        jdbc.update("UPDATE lamontana.recorrido_reparto SET estado='CANCELADO' WHERE id_recorrido=100");
        assertTrue(numeros().isEmpty());
    }

    @Test void recorridoFinalizadoPuedeConservarSuDetalleDeHoy(){
        pedido(1,"ENVIO_DOMICILIO","ENTREGADO","2026-11-07T18:00:00Z");
        jdbc.update("UPDATE lamontana.recorrido_reparto SET estado='FINALIZADO' WHERE id_recorrido=100");
        assertEquals(List.of(1L),numeros());
    }

    @Test void unaReservaNuevaNoReutilizaLaLlegadaDeUnDestinoAnterior(){
        pedido(1,"RETIRO_PUNTO_ENTREGA","DISPONIBLE_PUNTO","2026-11-07T18:00:00Z");
        jdbc.update("INSERT INTO lamontana.reserva_entrega SELECT 1001,id_pedido,modalidad,'Nuevo punto',franja_desde,franja_hasta,destino_publico FROM lamontana.reserva_entrega WHERE id_pedido=1");
        assertTrue(numeros().isEmpty());
    }

    @Test void invitadoUsaSuNombreDeDestinatarioYNoElUsuarioTecnico(){
        pedido(1,"ENVIO_DOMICILIO","EN_VIAJE",null);
        jdbc.update("UPDATE lamontana.destinatario_cotizacion SET nombre='Mica por WhatsApp' WHERE id_cotizacion=1");
        assertEquals("Mica por WhatsApp",pedidos().get(0).cliente());assertEquals("Mica por WhatsApp",pedidos().get(0).receptor());
    }
}
