package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.archivos.*;
import ar.com.lamontana.catalogo.EvaluadorPrecioItem;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad;
import ar.com.lamontana.cotizaciones.*;
import ar.com.lamontana.entregas.*;
import ar.com.lamontana.pagos.*;
import ar.com.lamontana.pedidos.*;
import ar.com.lamontana.produccion.*;
import ar.com.lamontana.puntos.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

/** PDF real, ambos tipos de cliente, reserva con triggers y entrega en una red creada después de configurar. */
class RedPuntosFlujoIntegrationTest {
 private static final String ADMIN="admin@prueba.local",CLIENTE="cliente1@prueba.local",PUNTO="punto.red.nuevo@prueba.local";
 private RedPuntosController.Guardar datos(long version,UUID sucursal,String costo){
  var franjas=new ArrayList<PuntoEntregaController.Franja>();for(int d=1;d<=7;d++)franjas.add(new PuntoEntregaController.Franja(d,"14:00","18:00",20,true));
  return new RedPuntosController.Guardar(UUID.randomUUID(),version,"PUNTO-RED-NUEVO","Punto nuevo City Bell","Centenario","1320","City Bell","Buenos Aires","1896",null,"America/Argentina/Buenos_Aires",List.of(new PuntoEntregaController.SucursalPunto(sucursal,1,true,costo,franjas)));
 }
 @Test void clientesEInvitadosConfirmanYElPuntoRecibeAunqueSuAsignacionCambie()throws Exception{
  Path files=Files.createTempDirectory("lamontana-red-operativa-");
  try(var pg=EmbeddedPostgres.builder().setServerConfig("unix_socket_directories","").start();var av=new ClamavDePrueba();var app=new SpringApplicationBuilder(LaMontanaApplication.class).run(
   "--server.port=0","--spring.profiles.active=pruebas","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=",
   "--lamontana.archivos.directorio="+files,"--lamontana.pruebas.clave=MontanaPrueba2026!","--lamontana.archivos.antivirus.host=127.0.0.1","--lamontana.archivos.antivirus.port="+av.port())){
   var jdbc=app.getBean(JdbcTemplate.class);var red=app.getBean(RedPuntosService.class);var prep=app.getBean(PreparacionClienteService.class);var upload=app.getBean(ArchivoPreparadoService.class);
   UUID laPlata=jdbc.queryForObject("SELECT codigo_publico FROM lamontana.sucursal WHERE codigo='LAPLATA'",UUID.class),floresta=jdbc.queryForObject("SELECT codigo_publico FROM lamontana.sucursal WHERE codigo='FLORESTA'",UUID.class);
   var punto=red.crear(datos(0,laPlata,"120.00"),ADMIN);UUID destino=punto.definicion().codigoPublico();
   assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_definicion_punto d JOIN lamontana.punto_entrega p USING(id_punto_entrega) WHERE p.codigo_publico=?",Integer.class,destino)).isZero();
   var puntos=app.getBean(PuntoRetiroService.class);puntos.crear(destino,new PuntoRetiroController.Alta(UUID.randomUUID(),"punto.red.nuevo",PUNTO,"Responsable nuevo","MontanaPrueba2026!",true),ADMIN);
   assertThat(puntos.contexto(PUNTO).nombre()).isEqualTo("Punto nuevo City Bell");
   var pedidos=new ArrayList<PedidoService.Detalle>();
   for(String autor:List.of(ADMIN,CLIENTE)){
    var p=autor.equals(ADMIN)?prep.abrirInvitado(ADMIN,UUID.randomUUID(),"Invitado de WhatsApp",laPlata):prep.abrir(autor);UUID preparacion=p.codigoPublico();
    var opciones=prep.opciones(preparacion,autor);assertThat(opciones.puntos()).extracting(CotizacionService.Punto::codigoPublico).contains(destino);
    var combo=opciones.combinaciones().stream().filter(c->c.sucursal().equals(laPlata)).findFirst().orElseThrow();
    var pending=upload.crear(preparacion,UUID.randomUUID(),"red-nueva.pdf",autor);byte[] pdf=pdf();var archivo=upload.recibir(preparacion,pending.codigoPublico(),autor,new ByteArrayInputStream(pdf),pdf.length);assertThat(archivo.estado()).isEqualTo("VALIDO");
    var item=new EvaluadorPrecioItem.Item(combo.servicio(),combo.formato(),combo.papel(),combo.color(),2,1,false,List.of());
    var presupuesto=prep.presupuestar(preparacion,new PreparacionClienteService.Presupuestar(UUID.randomUUID(),prep.consultar(preparacion,autor).version(),opciones.configuracion(),opciones.revisionComercial(),List.of(new PreparacionClienteService.Entrada(archivo.codigoPublico(),item))),autor);
    var preferencias=new PreparacionClienteService.Preferencias(Modalidad.RETIRO_PUNTO_ENTREGA,null,destino,null,MedioPago.TRANSFERENCIA);
    var checkout=prep.cotizar(preparacion,new PreparacionClienteService.Cotizar(UUID.randomUUID(),presupuesto.version(),preferencias),autor);
    if(autor.equals(ADMIN)){
     punto=red.editar(destino,datos(1,laPlata,"125.00"),ADMIN);
     var anterior=new PreparacionClienteService.Confirmar(UUID.randomUUID(),checkout.preparacion().version(),checkout.cotizacion().codigoPublico(),checkout.huella(),true,null);
     assertThatThrownBy(()->prep.confirmar(preparacion,anterior,autor)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
     checkout=prep.cotizar(preparacion,new PreparacionClienteService.Cotizar(UUID.randomUUID(),prep.consultar(preparacion,autor).version(),preferencias),autor);
    }
    assertThat(checkout.cotizacion().oferta().costoEntrega()).isEqualTo("125.00");assertThat(checkout.cotizacion().oferta().revisionPunto()).isEqualTo(punto.revision());
    var order=prep.confirmar(preparacion,new PreparacionClienteService.Confirmar(UUID.randomUUID(),checkout.preparacion().version(),checkout.cotizacion().codigoPublico(),checkout.huella(),true,null),autor);pedidos.add(order);
    assertThat(order.reserva().destino()).isEqualTo(destino);assertThat(order.reserva().origen()).isEqualTo(laPlata);
    assertThat(jdbc.queryForObject("SELECT id_version_red_punto FROM lamontana.reserva_entrega WHERE id_pedido=? AND estado='ACTIVA'",Long.class,order.numero())).isNotNull();
   }
   // El cliente también puede aceptar otra franja de la misma sucursal y red.
   var cliente=pedidos.get(1);var reprogramaciones=app.getBean(ReprogramacionService.class);
   var disponibles=reprogramaciones.disponibilidad(cliente.codigoPublico(),cliente.reserva().fecha().plusDays(1),ADMIN);var franja=disponibles.franjas().get(0);
   var propuesta=reprogramaciones.proponer(cliente.codigoPublico(),new ReprogramacionController.Proponer(UUID.randomUUID(),cliente.version(),disponibles.configuracion(),franja.fecha(),franja.desde(),franja.hasta(),"Cambio acordado","Proponemos retirar mañana",true),ADMIN);
   reprogramaciones.responder(cliente.codigoPublico(),new ReprogramacionController.Responder(UUID.randomUUID(),propuesta.version(),propuesta.pendiente().codigoPublico(),true,"Acepto el horario",true),CLIENTE);
   assertThat(jdbc.queryForObject("SELECT id_version_red_punto FROM lamontana.reserva_entrega WHERE id_pedido=? AND estado='ACTIVA'",Long.class,cliente.numero())).isNotNull();
   assertThat(jdbc.queryForObject("SELECT lamontana.franja_reserva_configurada(jsonb_populate_record(null::lamontana.reserva_entrega,to_jsonb(r)||jsonb_build_object('cupo_confirmado',999999)),p.id_configuracion_version,p.id_sucursal) FROM lamontana.reserva_entrega r JOIN lamontana.pedido p USING(id_pedido) WHERE p.id_pedido=? AND r.estado='ACTIVA'",Boolean.class,cliente.numero())).isFalse();
   assertThatThrownBy(()->jdbc.update("UPDATE lamontana.reserva_entrega SET zona_horaria='UTC' WHERE id_pedido=? AND estado='ACTIVA'",cliente.numero())).hasStackTraceContaining("Liberación de reserva inválida");
   // La red cambia sólo para pedidos nuevos: los dos pedidos ya confirmados conservan su revisión.
   red.editar(destino,datos(2,floresta,"250.00"),ADMIN);
   for(var order:pedidos)assertThat(app.getBean(PedidoService.class).rutaDespachoHabilitada(order.numero())).isTrue();
   var pedido=pedidos.get(0);var pagos=app.getBean(PagoService.class);var pendiente=pagos.vista(pedido.cotizacion(),ADMIN,true).pendiente();
   pagos.recibir(pedido.cotizacion(),new PagoController.Recibir(UUID.randomUUID(),null,MedioPago.TRANSFERENCIA,pendiente,null,null,true,"",true),ADMIN);
   var aprobada=app.getBean(RevisionPedidoService.class).decidir(pedido.codigoPublico(),new RevisionPedidoController.Decision(UUID.randomUUID(),pedido.version(),RevisionPedidoController.Accion.APROBAR,true,"",null),ADMIN);
   var produccion=app.getBean(ProduccionService.class);var trabajo=produccion.consultar(pedido.codigoPublico(),ADMIN).items().get(0);
   var iniciada=produccion.iniciar(pedido.codigoPublico(),new ProduccionController.Inicio(UUID.randomUUID(),aprobada.version(),trabajo.codigoPublico(),trabajo.impresoras().get(0).codigoPublico(),"",true),ADMIN);
   produccion.finalizarCalidad(pedido.codigoPublico(),iniciada.items().get(0).trabajos().get(0).codigoPublico(),new ProduccionController.Inspeccion(UUID.randomUUID(),iniciada.version(),ProduccionController.Resultado.APROBADO,new ProduccionController.Checklist(true,true,true,true,true),"",true),ADMIN);
   var entrega=app.getBean(EntregaService.class);var vista=entrega.consultar(pedido.codigoPublico(),ADMIN,true);
   vista=entrega.mover(pedido.codigoPublico(),new EntregaController.Movimiento(UUID.randomUUID(),vista.version(),EntregaController.Accion.PREPARAR_ENVIO,null,true),ADMIN);
   var rider=entrega.repartidores(ADMIN).get(0);entrega.moverLote(new EntregaController.MovimientoLote(UUID.randomUUID(),EntregaController.Accion.SALIR_REPARTO,List.of(new EntregaController.PedidoLote(pedido.codigoPublico(),vista.version())),rider.codigoPublico()),ADMIN);
   String correoRider=jdbc.queryForObject("SELECT correo FROM lamontana.usuario WHERE codigo_publico=?",String.class,rider.codigoPublico());
   var recorrido=app.getBean(JornadaRiderService.class).consultar(correoRider).stream().filter(r->r.pedidos().stream().anyMatch(p->p.codigoPublico().equals(pedido.codigoPublico()))).findFirst().orElseThrow();
   assertThat(recorrido.pedidos().stream().filter(p->p.codigoPublico().equals(pedido.codigoPublico())).findFirst().orElseThrow().destino()).contains("Centenario 1320");
   var ficha=puntos.ficha(pedido.codigoPublico(),PUNTO);assertThat(ficha.pedido().puedeRecibir()).isFalse();
   entrega.presentarEnPunto(recorrido.codigoPublico(),List.of(new EntregaController.PedidoLote(pedido.codigoPublico(),ficha.pedido().version())),correoRider);
   ficha=puntos.ficha(pedido.codigoPublico(),PUNTO);assertThat(ficha.pedido().puedeRecibir()).isTrue();
   puntos.recibir(new PuntoRetiroController.Recepcion(UUID.randomUUID(),List.of(new EntregaController.PedidoLote(pedido.codigoPublico(),ficha.pedido().version()))),PUNTO);
   ficha=puntos.ficha(pedido.codigoPublico(),PUNTO);assertThat(ficha.pedido().recorrido()).isEqualTo("DISPONIBLE_PUNTO");
   var codigo=entrega.consultar(pedido.codigoPublico(),ADMIN,true).codigo().texto();
   var finalizada=puntos.entregar(pedido.codigoPublico(),new EntregaController.Recibir(UUID.randomUUID(),ficha.pedido().version(),codigo,"Invitado de WhatsApp",null,true),PUNTO);assertThat(finalizada.aceptado()).isTrue();
  }finally{try(var paths=Files.walk(files)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 private byte[] pdf()throws IOException{try(var doc=new PDDocument();var out=new ByteArrayOutputStream()){doc.addPage(new PDPage());doc.addPage(new PDPage());doc.save(out);return out.toByteArray();}}
}
