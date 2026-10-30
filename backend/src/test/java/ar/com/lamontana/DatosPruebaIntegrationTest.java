package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.catalogo.*;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.entregas.EntregaService;
import ar.com.lamontana.pagos.PagoService;
import ar.com.lamontana.pedidos.*;
import ar.com.lamontana.produccion.ProduccionService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class DatosPruebaIntegrationTest {
    private static final String ADMIN="admin@prueba.local",CLAVE="MontanaPrueba2026!";
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private int port;

    @Test void bandejaOperativaYDespachoConjuntoConservanCalidadVersionYAuditoria()throws Exception{
        Path files=Files.createTempDirectory("lamontana-bandeja-despacho-");
        try(var pg=postgres();var app=iniciar(pg,files,"pruebas")){
            var jdbc=app.getBean(JdbcTemplate.class);var orders=app.getBean(PedidoService.class);var delivery=app.getBean(EntregaService.class);
            var production=orders.listarGlobal(0,"EN_PRODUCCION","MOVIMIENTO_DESC",ADMIN);
            assertThat(production.elementos()).isNotEmpty();assertThat(production.elementos()).extracting(PedidoService.Resumen::situacionOperativa).contains("REIMPRESION_REQUERIDA");assertThat(production.elementos()).allSatisfy(p->{assertThat(p.ultimoMovimiento().fecha()).isNotNull();assertThat(p.situacionDesde()).isNotNull();assertThat(p.accionEntrega()).isNull();});
            var ascending=orders.listarGlobal(0,null,"MOVIMIENTO_ASC",ADMIN).elementos();
            assertThat(ascending).isSortedAccordingTo(Comparator.comparing(p->p.ultimoMovimiento().fecha()));
            var ready=orders.listarGlobal(0,"LISTO_PARA_ENTREGA",ADMIN).elementos();
            var candidates=ready.stream().filter(p->p.accionEntrega()!=null).toList();assertThat(candidates).hasSizeGreaterThanOrEqualTo(2);
            for(var p:candidates)if(p.accionEntrega().equals("PREPARAR_ENVIO"))delivery.mover(p.codigoPublico(),new ar.com.lamontana.entregas.EntregaController.Movimiento(UUID.randomUUID(),p.version(),ar.com.lamontana.entregas.EntregaController.Accion.PREPARAR_ENVIO,null,true),ADMIN);
            var prepared=orders.listarGlobal(0,"LISTO_PARA_ENTREGA",ADMIN).elementos().stream().filter(p->"SALIR_REPARTO".equals(p.accionEntrega())).limit(2).toList();assertThat(prepared).hasSize(2);
            var entries=prepared.stream().map(p->new ar.com.lamontana.entregas.EntregaController.PedidoLote(p.codigoPublico(),p.version())).toList();
            long before=count(jdbc,"movimiento_entrega");
            var invalid=new ArrayList<>(entries);var last=entries.get(1);invalid.set(1,new ar.com.lamontana.entregas.EntregaController.PedidoLote(last.pedido(),last.version()-1));
            assertThatThrownBy(()->delivery.moverLote(new ar.com.lamontana.entregas.EntregaController.MovimientoLote(UUID.randomUUID(),ar.com.lamontana.entregas.EntregaController.Accion.SALIR_REPARTO,invalid),ADMIN)).hasMessageContaining("cambió");assertThat(count(jdbc,"movimiento_entrega")).isEqualTo(before);
            var notReady=production.elementos().get(0);invalid.set(1,new ar.com.lamontana.entregas.EntregaController.PedidoLote(notReady.codigoPublico(),notReady.version()));
            assertThatThrownBy(()->delivery.moverLote(new ar.com.lamontana.entregas.EntregaController.MovimientoLote(UUID.randomUUID(),ar.com.lamontana.entregas.EntregaController.Accion.SALIR_REPARTO,invalid),ADMIN)).hasMessageContaining("calidad");assertThat(count(jdbc,"movimiento_entrega")).isEqualTo(before);
            var command=new ar.com.lamontana.entregas.EntregaController.MovimientoLote(UUID.randomUUID(),ar.com.lamontana.entregas.EntregaController.Accion.SALIR_REPARTO,entries);
            // A failure in the final group audit must roll back every order and automatic code.
            jdbc.execute("CREATE FUNCTION lamontana.fallar_lote_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fallo de auditoría de lote'; END $$");
            jdbc.execute("CREATE TRIGGER fallar_lote_test BEFORE INSERT ON lamontana.lote_movimiento_entrega FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_lote_test()");
            try{assertThatThrownBy(()->delivery.moverLote(command,ADMIN)).hasStackTraceContaining("fallo de auditoría de lote");assertThat(count(jdbc,"movimiento_entrega")).isEqualTo(before);}finally{jdbc.execute("DROP TRIGGER fallar_lote_test ON lamontana.lote_movimiento_entrega");jdbc.execute("DROP FUNCTION lamontana.fallar_lote_test()");}
            var result=delivery.moverLote(command,ADMIN);assertThat(result.cantidad()).isEqualTo(2);assertThat(delivery.moverLote(command,ADMIN)).isEqualTo(result);assertThat(count(jdbc,"movimiento_entrega")).isEqualTo(before+2);
            for(var entry:entries){var view=delivery.consultar(entry.pedido(),ADMIN,true);assertThat(view.estadoLogistico()).isEqualTo("EN_VIAJE");assertThat(view.codigo()).isNotNull();assertThat(view.historial().get(view.historial().size()-1).motivo()).contains(command.operacion().toString());}
            assertThat(jdbc.queryForObject("SELECT actor_nombre FROM lamontana.lote_movimiento_entrega WHERE id_operacion=?",String.class,command.operacion())).isNotBlank();
            assertThatThrownBy(()->delivery.moverLote(command,"cliente1@prueba.local")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        }finally{limpiar(files);}
    }

    @Test void operacionDiariaDeImpresorasYPuntosNoReescribeElMotor()throws Exception{
        Path files=Files.createTempDirectory("lamontana-operacion-diaria-");
        try(var pg=postgres();var app=iniciar(pg,files,"pruebas")){
            var jdbc=app.getBean(JdbcTemplate.class);var printers=app.getBean(ar.com.lamontana.produccion.OperacionImpresoraService.class);var production=app.getBean(ar.com.lamontana.produccion.ProduccionService.class);var points=app.getBean(DisponibilidadPuntoService.class);
            String versions=jdbc.queryForObject("SELECT jsonb_agg(to_jsonb(c) ORDER BY numero_version)::text FROM lamontana.configuracion_version c",String.class);
            var approved=jdbc.query("SELECT codigo_publico FROM lamontana.pedido WHERE estado='APROBADO' ORDER BY id_pedido DESC",(r,n)->r.getObject(1,UUID.class));
            UUID order=null,printer=null,item=null;int sheets=0;
            for(UUID candidate:approved){var view=production.consultar(candidate,ADMIN);for(var i:view.items())if(i.puedeIniciar())for(var p:i.impresoras())if(p.compatible()){order=candidate;printer=p.codigoPublico();item=i.codigoPublico();sheets=app.getBean(PedidoService.class).detalle(candidate,ADMIN,true).oferta().items().stream().filter(o->o.codigoPublico().equals(i.codigoPublico())).findFirst().orElseThrow().precio().hojas();break;}if(order!=null)break;}
            assertThat(order).isNotNull();final UUID target=printer,orderId=order,itemId=item;
            var initial=printers.estado(target);var count=new ar.com.lamontana.produccion.OperacionImpresoraController.Cambio(UUID.randomUUID(),initial.version(),ar.com.lamontana.produccion.OperacionImpresoraController.Accion.CONTAR,sheets+500,"");
            var counted=printers.cambiar(target,count,ADMIN);printers.cambiar(target,count,ADMIN);assertThat(printers.estado(target).hojas()).isEqualTo(sheets+500);
            var disabled=printers.cambiar(target,new ar.com.lamontana.produccion.OperacionImpresoraController.Cambio(UUID.randomUUID(),counted.version(),ar.com.lamontana.produccion.OperacionImpresoraController.Accion.DESHABILITAR,null,""),ADMIN);
            long orderVersion=production.consultar(orderId,ADMIN).version();var start=new ar.com.lamontana.produccion.ProduccionController.Inicio(UUID.randomUUID(),orderVersion,itemId,target,"",true);
            assertThatThrownBy(()->production.iniciar(orderId,start,ADMIN)).hasMessageContaining("disponibilidad");
            printers.cambiar(target,new ar.com.lamontana.produccion.OperacionImpresoraController.Cambio(UUID.randomUUID(),disabled.version(),ar.com.lamontana.produccion.OperacionImpresoraController.Accion.HABILITAR,null,""),ADMIN);
            production.iniciar(orderId,start,ADMIN);production.iniciar(orderId,start,ADMIN);assertThat(printers.estado(target).hojas()).isEqualTo(500);assertThat(printers.historial(target,0,ADMIN).eventos().stream().filter(e->e.accion().equals("ASIGNAR_TRABAJO"))).hasSize(1);
            var beforeFailure=printers.estado(target);jdbc.execute("CREATE FUNCTION lamontana.fallar_papel_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fallo de auditoría de papel'; END $$");jdbc.execute("CREATE TRIGGER fallar_papel_test BEFORE INSERT ON lamontana.evento_operacion_impresora FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_papel_test()");
            try{assertThatThrownBy(()->printers.cambiar(target,new ar.com.lamontana.produccion.OperacionImpresoraController.Cambio(UUID.randomUUID(),beforeFailure.version(),ar.com.lamontana.produccion.OperacionImpresoraController.Accion.RECARGAR,25,""),ADMIN)).hasStackTraceContaining("fallo de auditoría");assertThat(printers.estado(target)).isEqualTo(beforeFailure);}finally{jdbc.execute("DROP TRIGGER fallar_papel_test ON lamontana.evento_operacion_impresora");jdbc.execute("DROP FUNCTION lamontana.fallar_papel_test()");}
            var point=points.listar(ADMIN).puntos().get(0);var state=point.disponibilidad();var next=state.estado().equals("HABILITADO")?DisponibilidadPuntoController.Estado.DESHABILITADO:DisponibilidadPuntoController.Estado.HABILITADO;
            var change=new DisponibilidadPuntoController.Cambiar(UUID.randomUUID(),state.version(),next);points.cambiar(point.codigoPublico(),change,ADMIN);points.cambiar(point.codigoPublico(),change,ADMIN);assertThat(points.historial(point.codigoPublico(),0,ADMIN).eventos()).isNotEmpty();
            assertThatThrownBy(()->printers.listar("caja@prueba.local")).hasMessageContaining("acceso");assertThatThrownBy(()->points.listar("caja@prueba.local")).hasMessageContaining("permiso");
            assertThat(jdbc.queryForObject("SELECT jsonb_agg(to_jsonb(c) ORDER BY numero_version)::text FROM lamontana.configuracion_version c",String.class)).isEqualTo(versions);
        }finally{limpiar(files);}
    }

    @Test void cobroSimpleConservaAuditoriaYNoDuplicaUnaConfirmacion() throws Exception {
        Path files=Files.createTempDirectory("lamontana-cobro-simple-");
        try(var pg=postgres();var app=iniciar(pg,files,"pruebas")){
            var jdbc=app.getBean(JdbcTemplate.class);var pagos=app.getBean(PagoService.class);
            UUID quote=jdbc.queryForObject("SELECT c.codigo_publico FROM lamontana.cotizacion c JOIN lamontana.pedido_actual pa USING(id_cotizacion) JOIN lamontana.pedido p USING(id_pedido) WHERE p.estado='PENDIENTE_REVISION' AND c.total>coalesce((SELECT sum(a.neto) FROM lamontana.aplicacion_pago_neta a WHERE a.id_cotizacion=c.id_cotizacion),0) ORDER BY p.id_pedido DESC LIMIT 1",UUID.class);
            var before=pagos.vista(quote,ADMIN,true);BigDecimal amount=new BigDecimal(before.pendiente()).add(new BigDecimal("50.00"));UUID op=UUID.randomUUID();
            var command=new ar.com.lamontana.pagos.PagoController.Recibir(op,null,ar.com.lamontana.configuracion.ConfiguracionController.MedioPago.TRANSFERENCIA,amount.toPlainString(),null,null,true,"",true);
            long paymentsBeforeFailure=count(jdbc,"pago"),eventsBeforeFailure=count(jdbc,"evento_financiero");
            jdbc.execute("CREATE FUNCTION lamontana.fallar_aplicacion_prueba() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fallo de aplicación para comprobar rollback'; END $$");
            jdbc.execute("CREATE TRIGGER fallar_aplicacion_prueba BEFORE INSERT ON lamontana.aplicacion_pago_cotizacion FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_aplicacion_prueba()");
            try{assertThatThrownBy(()->pagos.recibir(quote,command,ADMIN)).hasStackTraceContaining("fallo de aplicación");assertThat(count(jdbc,"pago")).isEqualTo(paymentsBeforeFailure);assertThat(count(jdbc,"evento_financiero")).isEqualTo(eventsBeforeFailure);}
            finally{jdbc.execute("DROP TRIGGER fallar_aplicacion_prueba ON lamontana.aplicacion_pago_cotizacion");jdbc.execute("DROP FUNCTION lamontana.fallar_aplicacion_prueba()");}
            long countBefore=count(jdbc,"pago");var result=pagos.recibir(quote,command,ADMIN);
            assertThat(count(jdbc,"pago")).isEqualTo(countBefore+1);assertThat(result.pendiente()).isEqualTo("0.00");
            var received=result.pagos().stream().filter(p->p.referencia().equals("Registro interno "+op)).findFirst().orElseThrow();
            assertThat(received.disponible()).isEqualTo("50.00");assertThat(received.recibidoEn()).isNotNull();
            var receipt=result.historial().stream().filter(e->e.tipo().equals("RECIBIR")&&e.referencia().equals(received.referencia())).findFirst().orElseThrow();
            assertThat(receipt.actor()).isNotBlank();assertThat(receipt.fecha()).isNotNull();assertThat(receipt.importe()).isEqualTo(amount.setScale(2).toPlainString());assertThat(receipt.medio()).isEqualTo("TRANSFERENCIA");
            assertThat(result.historial().stream().filter(e->e.tipo().equals("APLICAR")&&received.referencia().equals(e.referencia()))).hasSize(1);
            long events=count(jdbc,"evento_financiero");pagos.recibir(quote,command,ADMIN);assertThat(count(jdbc,"pago")).isEqualTo(countBefore+1);assertThat(count(jdbc,"evento_financiero")).isEqualTo(events);
            var conflict=new ar.com.lamontana.pagos.PagoController.Recibir(op,null,command.medio(),"1.00",null,null,true,"",true);
            assertThatThrownBy(()->pagos.recibir(quote,conflict,ADMIN)).hasMessageContaining("otros datos");
        }finally{limpiar(files);}
    }

    @Test void seisMesesSonConsultablesPorAdminCajaYLosCuatroClientesYPersistenAlReiniciar() throws Exception {
        Path files=Files.createTempDirectory("lamontana-seis-meses-");
        try(var pg=postgres()) {
            ConfigurableApplicationContext app=iniciar(pg,files,"pruebas");
            try {
                var jdbc=app.getBean(JdbcTemplate.class);
                assertThat(count(jdbc,"usuario")).isEqualTo(7);
                assertThat(count(jdbc,"sucursal")).isEqualTo(3);
                assertThat(count(jdbc,"punto_entrega")).isEqualTo(2);
                assertThat(count(jdbc,"configuracion_version")).isEqualTo(4);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_version WHERE estado='HISTORICA'",Integer.class)).isEqualTo(3);
                assertThat(count(jdbc,"pedido")).isGreaterThanOrEqualTo(120);
                assertThat(jdbc.queryForObject("SELECT min(confirmada_en) FROM lamontana.pedido",java.sql.Timestamp.class).toInstant()).isBefore(Instant.now().minus(Duration.ofDays(150)));
                assertThat(jdbc.queryForList("SELECT DISTINCT estado FROM lamontana.pedido",String.class)).containsExactlyInAnyOrder("PENDIENTE_REVISION","CORRECCION_SOLICITADA","APROBADO","EN_PRODUCCION","LISTO_PARA_ENTREGA","ENTREGADO","CERRADO","CANCELADO","RECHAZADO");
                comprobarCatalogo(app);
                comprobarConfiguraciones(app);
                comprobarPedidosYCaja(app);
                comprobarHttp(app);
                comprobarContinuacion(app);
                long pedidos=count(jdbc,"pedido"),pagos=count(jdbc,"pago");
                app.close();app=iniciar(pg,files,"pruebas");
                assertThat(count(app.getBean(JdbcTemplate.class),"pedido")).isEqualTo(pedidos);
                assertThat(count(app.getBean(JdbcTemplate.class),"pago")).isEqualTo(pagos);
                assertThat(count(app.getBean(JdbcTemplate.class),"configuracion_version")).isEqualTo(4);
                System.out.println("ESCENARIO_VERIFICADO pedidos="+pedidos+", pagos="+pagos+", papeles=27 variantes, tarifas=54 por revisión, versiones=4");
            } finally {app.close();}
        } finally {limpiar(files);}
    }

    @Test void elPerfilNormalNoPrecargaDatos() throws Exception {
        Path files=Files.createTempDirectory("lamontana-sin-escenario-");
        try(var pg=postgres();var app=iniciar(pg,files,"normal")) {
            var jdbc=app.getBean(JdbcTemplate.class);
            assertThat(count(jdbc,"usuario")).isZero();assertThat(count(jdbc,"pedido")).isZero();
            assertThat(jdbc.queryForObject("SELECT to_regclass('lamontana.entorno_prueba') IS NULL",Boolean.class)).isTrue();
        } finally {limpiar(files);}
    }

    @Test void laBaseAnteriorSeConservaYRequiereReinicioExplicito() throws Exception {
        Path files=Files.createTempDirectory("lamontana-escenario-anterior-");
        try(var pg=postgres()) {
            try(var app=iniciar(pg,files,"normal")) {
                var jdbc=app.getBean(JdbcTemplate.class);
                jdbc.execute("CREATE TABLE lamontana.entorno_prueba(unica boolean PRIMARY KEY,cargado_en timestamptz DEFAULT now())");
                jdbc.update("INSERT INTO lamontana.entorno_prueba(unica) VALUES(true)");
            }
            assertThatThrownBy(()->{try(var ignored=iniciar(pg,files,"pruebas")) {}}).hasStackTraceContaining("reiniciar-datos.sh");
            try(var app=iniciar(pg,files,"normal")) {
                var jdbc=app.getBean(JdbcTemplate.class);
                assertThat(count(jdbc,"entorno_prueba")).isEqualTo(1);assertThat(count(jdbc,"usuario")).isZero();
            }
        } finally {limpiar(files);}
    }

    private void comprobarCatalogo(ConfigurableApplicationContext app) {
        var estado=app.getBean(CatalogoService.class).estado();
        assertThat(estado.formatos()).hasSize(7);assertThat(estado.papelesHabilitados()).hasSize(27);
        assertThat(estado.papeles().stream().map(p->p.gramaje().intValue())).containsExactlyInAnyOrder(75,80,90,130,170,250);
        assertThat(estado.actual().tarifas()).hasSize(54);
        var servicio=estado.servicios().stream().filter(s->s.codigo().equals("IMPRESION")).findFirst().orElseThrow().codigoPublico();
        for(var tarifa:estado.actual().tarifas()) {
            assertThat(tarifa.nombre()).isNotBlank();assertThat(tarifa.grupo()).isNotNull();assertThat(tarifa.modoDobleFaz()).isNotNull();
            for(boolean duplex:new boolean[]{false,true}) {
                var item=new EvaluadorPrecioItem.Item(servicio,tarifa.formato(),tarifa.papel(),tarifa.color(),5,2,duplex,List.of());
                assertThat(new BigDecimal(app.getBean(EvaluadorPrecioItem.class).calcular(estado,item).subtotal())).isPositive();
            }
        }
        var opciones=app.getBean(CotizacionService.class).opciones("cliente1@prueba.local");
        assertThat(opciones.disponible()).isTrue();assertThat(opciones.formatos()).hasSize(7);assertThat(opciones.papeles()).hasSize(6);
        assertThat(opciones.modalidades()).hasSize(3);assertThat(opciones.puntos()).hasSize(2);
        var activa=app.getBean(OfertaOperativaService.class).leer().configuracion();
        var revision=app.getBean(RevisionConfiguracionService.class).revisarPreparacion(activa.codigoPublico(),ADMIN);
        assertThat(revision.bloqueos()).describedAs(revision.hallazgos().toString()).isZero();
    }

    private void comprobarConfiguraciones(ConfigurableApplicationContext app) {
        var service=app.getBean(HistorialConfiguracionService.class);
        var listado=service.listar("","TODOS","ANTIGUAS",0,ADMIN);
        assertThat(listado.versiones().total()).isEqualTo(4);
        for(var fila:listado.versiones().elementos()) {
            var detalle=service.detalle(fila.codigo(),ADMIN);assertThat(detalle.modulos()).hasSize(5);
            assertThat(detalle.revisionComercial()).isNotNull();assertThat(detalle.activador()).isNotNull();
            assertThat(service.auditoria(fila.codigo(),0,ADMIN).total()).isGreaterThanOrEqualTo(6);
            if(fila.numero()>1)assertThat(detalle.predecesora()).isNotNull();
        }
        for(int i=1;i<4;i++)assertThat(service.comparar(listado.versiones().elementos().get(i-1).codigo(),listado.versiones().elementos().get(i).codigo(),ADMIN).modificadas()).isGreaterThanOrEqualTo(3);
    }

    private void comprobarPedidosYCaja(ConfigurableApplicationContext app) throws Exception {
        var jdbc=app.getBean(JdbcTemplate.class);var pedidos=app.getBean(PedidoService.class);
        var pagos=app.getBean(PagoService.class);var produccion=app.getBean(ProduccionService.class);var entregas=app.getBean(EntregaService.class);
        assertThat(pedidos.listarGlobal(0,null,ADMIN).total()).isEqualTo(count(jdbc,"pedido"));
        for(String estado:jdbc.queryForList("SELECT DISTINCT estado FROM lamontana.pedido",String.class))assertThat(pedidos.listarGlobal(0,estado,ADMIN).total()).isPositive();
        for(UUID id:jdbc.queryForList("SELECT codigo_publico FROM lamontana.pedido ORDER BY id_pedido",UUID.class)) {
            var p=pedidos.detalle(id,ADMIN,true);assertThat(p.historial()).isNotEmpty();assertThat(p.archivos()).hasSize(p.oferta().items().size());
            var caja=pagos.vista(p.cotizacion(),ADMIN,true);assertThat(new BigDecimal(caja.pendiente())).isNotNegative();
            produccion.consultar(id,ADMIN);entregas.consultar(id,ADMIN,true);app.getBean(CorreccionPedidoService.class).consultar(id,ADMIN,true);
            if(p.estado().equals("CERRADO")) {
                assertThat(new BigDecimal(caja.aplicado())).isEqualByComparingTo(p.oferta().total());
                assertThat(entregas.consultar(id,ADMIN,true).cierre()).isNotNull();
                for(var pdf:p.archivos())assertThat(app.getBean(ArchivosPrivados.class).original(pdf.archivo(),pdf.sha256(),pdf.bytes())).exists();
            }
        }
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT date_trunc('month',fecha)) FROM lamontana.evento_financiero",Integer.class)).isGreaterThanOrEqualTo(6);
        assertThat(count(jdbc,"reembolso")).isGreaterThan(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM (SELECT id_cotizacion FROM lamontana.pago GROUP BY id_cotizacion HAVING count(*)>1) p",Integer.class)).isGreaterThan(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.cotizacion c WHERE jsonb_array_length(c.oferta->'items')<>(SELECT count(*) FROM lamontana.cotizacion_item i WHERE i.id_cotizacion=c.id_cotizacion)",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM (SELECT fecha,lag(fecha) OVER (PARTITION BY id_pedido ORDER BY id_evento) anterior FROM lamontana.historial_estado_pedido) h WHERE fecha<anterior",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.pago_disponible WHERE disponible<0 OR aplicado<0 OR devuelto<0",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT sum(importe-aplicado-devuelto-disponible) FROM lamontana.pago_disponible",BigDecimal.class)).isEqualByComparingTo(BigDecimal.ZERO);
        for(UUID sucursal:jdbc.queryForList("SELECT codigo_publico FROM lamontana.sucursal",UUID.class))assertThat(pagos.bandeja(sucursal,0,"caja@prueba.local").total()).isPositive();
    }

    private void comprobarHttp(ConfigurableApplicationContext app) throws Exception {
        var admin=login(ADMIN);var catalogo=get(admin,"/api/admin/catalogo",200);
        assertThat(catalogo.get("papelesHabilitados").size()).isEqualTo(27);assertThat(catalogo.get("actual").get("tarifas").size()).isEqualTo(54);
        assertThat(get(admin,"/api/admin/configuracion/historial",200).get("versiones").get("total").asInt()).isEqualTo(4);
        var jdbc=app.getBean(JdbcTemplate.class);
        var caja=login("caja@prueba.local");var produccion=login("produccion@prueba.local");
        for(UUID sucursal:jdbc.queryForList("SELECT codigo_publico FROM lamontana.sucursal",UUID.class)) {
            assertThat(get(caja,"/api/operacion/sucursales/"+sucursal+"/pagos",200).get("total").asInt()).isPositive();
            get(produccion,"/api/operacion/sucursales/"+sucursal+"/pedidos",200);
        }
        for(int i=1;i<=4;i++) {
            var cliente=login("cliente"+i+"@prueba.local");var lista=get(cliente,"/api/cliente/pedidos",200);
            assertThat(lista.get("total").asInt()).isGreaterThan(20);
            var opciones=get(cliente,"/api/cliente/cotizaciones/opciones",200);assertThat(opciones.get("disponible").asBoolean()).isTrue();
            assertThat(opciones.get("papeles").get(0).get("gramaje").asInt()).isPositive();
            for(var pedido:lista.get("elementos"))get(cliente,"/api/cliente/pedidos/"+pedido.get("codigoPublico").asString(),200);
            var historicos=get(cliente,"/api/cliente/pedidos?estado=CERRADO",200);
            assertThat(historicos.get("total").asInt()).isPositive();
            var p=get(cliente,"/api/cliente/pedidos/"+historicos.get("elementos").get(0).get("codigoPublico").asString(),200);
            String base="/api/cliente/cotizaciones/"+p.get("cotizacion").asString()+"/archivos/"+p.get("archivos").get(0).get("archivo").asString();
            var pdf=cliente.send(HttpRequest.newBuilder(uri(base+"/original")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            assertThat(pdf.statusCode()).isEqualTo(200);assertThat(new String(Arrays.copyOf(pdf.body(),5),java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            var preview=cliente.send(HttpRequest.newBuilder(uri(base+"/paginas/1")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertThat(preview.statusCode()).isEqualTo(200);
            get(cliente,"/api/admin/pedidos",403);
            UUID ajeno=jdbc.queryForObject("SELECT p.codigo_publico FROM lamontana.pedido p JOIN lamontana.usuario u ON u.id_usuario=p.id_usuario_creador WHERE u.correo<>? LIMIT 1",UUID.class,"cliente"+i+"@prueba.local");
            get(cliente,"/api/cliente/pedidos/"+ajeno,404);
        }
    }

    private void comprobarContinuacion(ConfigurableApplicationContext app) {
        var jdbc=app.getBean(JdbcTemplate.class);var pedidos=app.getBean(PedidoService.class);var revision=app.getBean(RevisionPedidoService.class);
        UUID id=jdbc.queryForObject("SELECT codigo_publico FROM lamontana.pedido WHERE estado='APROBADO' ORDER BY id_pedido DESC LIMIT 1",UUID.class);
        var p=pedidos.detalle(id,ADMIN,true);var produccion=app.getBean(ProduccionService.class);var vista=produccion.consultar(id,ADMIN);
        var item=vista.items().get(0);assertThat(item.puedeIniciar()).isTrue();
        var iniciada=produccion.iniciar(id,new ar.com.lamontana.produccion.ProduccionController.Inicio(UUID.randomUUID(),p.version(),item.codigoPublico(),item.impresoras().get(0).codigoPublico(),"Continuación real del escenario precargado",true),ADMIN);
        assertThat(iniciada.estado()).isEqualTo("EN_PRODUCCION");
        UUID pendiente=jdbc.queryForObject("SELECT codigo_publico FROM lamontana.pedido WHERE estado='PENDIENTE_REVISION' ORDER BY id_pedido DESC LIMIT 1",UUID.class);
        var pp=pedidos.detalle(pendiente,ADMIN,true);
        var revisada=revision.decidir(pendiente,new RevisionPedidoController.Decision(UUID.randomUUID(),pp.version(),RevisionPedidoController.Accion.REVISAR,true,"PDF de prueba comprobado",null),ADMIN);
        assertThat(revisada.version()).isGreaterThan(pp.version());
    }

    private EmbeddedPostgres postgres() throws Exception {return EmbeddedPostgres.builder().setServerConfig("unix_socket_directories", "").start();}
    private ConfigurableApplicationContext iniciar(EmbeddedPostgres pg,Path files,String perfil) {
        var app=new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0","--spring.profiles.active="+perfil,"--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+files,"--lamontana.pruebas.clave="+CLAVE);
        port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));return app;
    }
    private long count(JdbcTemplate jdbc,String tabla){return jdbc.queryForObject("SELECT count(*) FROM lamontana."+tabla,Long.class);}
    private URI uri(String path){return URI.create("http://127.0.0.1:"+port+path);}
    private JsonNode get(HttpClient c,String path,int status) throws Exception {
        var r=c.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(20)).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).withFailMessage("%s: esperado %s, recibido %s: %s",path,status,r.statusCode(),r.body()).isEqualTo(status);
        return r.body().isBlank()?JSON.nullNode():JSON.readTree(r.body());
    }
    private HttpClient login(String correo) throws Exception {
        var c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();var token=get(c,"/api/auth/csrf",200).get("token").asString();
        var r=c.send(HttpRequest.newBuilder(uri("/api/auth/login")).header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",token).POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(correo,java.nio.charset.StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(CLAVE,java.nio.charset.StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());assertThat(r.statusCode()).isEqualTo(200);return c;
    }
    private void limpiar(Path files) throws Exception {try(var paths=Files.walk(files)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
}
