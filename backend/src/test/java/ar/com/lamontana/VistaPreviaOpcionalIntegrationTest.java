package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.archivos.*;
import ar.com.lamontana.catalogo.EvaluadorPrecioItem;
import ar.com.lamontana.cotizaciones.*;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad;
import ar.com.lamontana.pedidos.*;
import ar.com.lamontana.organizacion.EmpleadoController.EdicionEmpleado;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.PagoService;
import ar.com.lamontana.pagos.PagoController;
import ar.com.lamontana.entregas.EntregaService;
import ar.com.lamontana.entregas.EntregaController;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import tools.jackson.databind.json.JsonMapper;
import ar.com.lamontana.produccion.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real migrations, private PDF inspection, checkout, correction and production; no preview acceptance call. */
class VistaPreviaOpcionalIntegrationTest {
 private static final String CLIENTE="cliente1@prueba.local",ADMIN="admin@prueba.local";
 @Test void permiteEnviarYCorregirSinAbrirPaginasNiInventarAceptaciones() throws Exception {
  Path files=Files.createTempDirectory("lamontana-preview-opcional-");
  try(var pg=EmbeddedPostgres.builder().setServerConfig("unix_socket_directories", "").start();var av=new ClamavDePrueba();var app=new SpringApplicationBuilder(LaMontanaApplication.class).run(
   "--server.port=0","--spring.profiles.active=pruebas","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=",
   "--lamontana.archivos.directorio="+files,"--lamontana.pruebas.clave=MontanaPrueba2026!","--lamontana.archivos.antivirus.host=127.0.0.1","--lamontana.archivos.antivirus.port="+av.port())) {
   var jdbc=app.getBean(JdbcTemplate.class);var prep=app.getBean(PreparacionClienteService.class);var upload=app.getBean(ArchivoPreparadoService.class);
   var historicos=jdbc.queryForList("SELECT id_aceptacion_vista_previa,fecha,sha256,tipo_registro FROM lamontana.aceptacion_vista_previa ORDER BY id_aceptacion_vista_previa");
   var p=prep.abrir(CLIENTE);UUID id=p.codigoPublico();var pending=upload.crear(id,UUID.randomUUID(),"trabajo.pdf",CLIENTE);
   byte[] pdf=pdf();var f=upload.recibir(id,pending.codigoPublico(),CLIENTE,new ByteArrayInputStream(pdf),pdf.length);
   assertThat(f.estado()).isEqualTo("VALIDO");assertThat(f.paginas()).isEqualTo(2);assertThat(f.aceptadaEn()).isNull();assertThat(av.ultimo).isEqualTo(pdf);
   assertThatThrownBy(()->upload.archivo(id,f.codigoPublico(),"cliente2@prueba.local")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
   var tx=new TransactionTemplate(app.getBean(PlatformTransactionManager.class));long owner=jdbc.queryForObject("SELECT id_usuario FROM lamontana.usuario WHERE correo=?",Long.class,CLIENTE);
   assertThatThrownBy(()->tx.executeWithoutResult(s->RegistroEnvioPdf.registrar(jdbc,f.codigoPublico(),owner,UUID.randomUUID(),f.sha256()))).hasStackTraceContaining("requiere su pedido o respuesta");
   var opts=app.getBean(CotizacionService.class).opciones(CLIENTE);var combo=opts.combinaciones().stream().filter(c->c.sucursal().equals(opts.casaMatriz().codigoPublico())).findFirst().orElseThrow();
   var work=new EvaluadorPrecioItem.Item(combo.servicio(),combo.formato(),combo.papel(),combo.color(),2,1,false,List.of());
   p=prep.consultar(id,CLIENTE);p=prep.presupuestar(id,new PreparacionClienteService.Presupuestar(UUID.randomUUID(),p.version(),opts.configuracion(),opts.revisionComercial(),List.of(new PreparacionClienteService.Entrada(f.codigoPublico(),work))),CLIENTE);
   var checkout=prep.cotizar(id,new PreparacionClienteService.Cotizar(UUID.randomUUID(),p.version(),new PreparacionClienteService.Preferencias(Modalidad.RETIRO_SUCURSAL,opts.casaMatriz().codigoPublico(),null,null,MedioPago.EFECTIVO)),CLIENTE);
   assertThat(checkout.revision().archivos()).hasSize(1);assertThat(checkout.revision().archivos().get(0).aceptadaEn()).isNull();assertThat(checkout.huella()).isNotBlank();
   var wrong=new PreparacionClienteService.Confirmar(UUID.randomUUID(),checkout.preparacion().version(),checkout.cotizacion().codigoPublico(),"0".repeat(64),true,null);
   assertThatThrownBy(()->prep.confirmar(id,wrong,CLIENTE)).hasMessageContaining("resumen actualizado");
   var command=new PreparacionClienteService.Confirmar(UUID.randomUUID(),checkout.preparacion().version(),checkout.cotizacion().codigoPublico(),checkout.huella(),true,null);
   // A changed original must still fail, even though preview is optional.
   Path original=app.getBean(ArchivosPrivados.class).original(f.codigoPublico(),f.sha256(),f.bytes());Files.write(original,new byte[]{1,2,3});
   assertThatThrownBy(()->prep.confirmar(id,command,CLIENTE)).hasMessageContaining("integridad");Files.write(original,pdf);
   var order=prep.confirmar(id,command,CLIENTE);assertThat(order.archivos()).hasSize(1);assertThat(order.archivos().get(0).archivo()).isEqualTo(f.codigoPublico());assertThat(order.archivos().get(0).aceptadaEn()).isNull();
   assertThat(prep.confirmar(id,command,CLIENTE).codigoPublico()).isEqualTo(order.codigoPublico());
   assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.aceptacion_vista_previa WHERE tipo_registro='ENVIO_PEDIDO'",Integer.class)).isEqualTo(1);
   var archive=app.getBean(ArchivoService.class);assertThat(archive.contenido(order.cotizacion(),f.codigoPublico(),null,CLIENTE,false).bytes()).isEqualTo(pdf);
   var corrections=app.getBean(CorreccionPedidoService.class);UUID orderId=order.codigoPublico();
   order=corrections.solicitar(orderId,new CorreccionPedidoController.Solicitar(UUID.randomUUID(),order.version(),CorreccionPedidoController.Tipo.ARCHIVO,"Reemplazar PDF","Enviá la versión corregida",List.of(order.archivos().get(0).item())),ADMIN);
   var request=corrections.consultar(orderId,CLIENTE,false).solicitudes().get(0);
   var next=archive.crear(order.cotizacion(),order.archivos().get(0).item(),UUID.randomUUID(),order.version(),request.codigoPublico(),CLIENTE);
   var replacement=archive.recibir(order.cotizacion(),next.codigoPublico(),CLIENTE,new ByteArrayInputStream(pdf),pdf.length);assertThat(replacement.estado()).isEqualTo("VALIDO");assertThat(replacement.aceptadaEn()).isNull();
   var response=corrections.preparar(orderId,CLIENTE);assertThat(response.habilitada()).describedAs(response.bloqueos().toString()).isTrue();
   var answer=new CorreccionPedidoController.Responder(UUID.randomUUID(),response.version(),response.solicitud(),"Adjunto el PDF corregido",response.huella(),true,null);
   order=corrections.responder(orderId,answer,CLIENTE);assertThat(order.archivos().get(0).archivo()).isEqualTo(next.codigoPublico());assertThat(order.archivos().get(0).aceptadaEn()).isNull();
   assertThat(corrections.responder(orderId,answer,CLIENTE).version()).isEqualTo(order.version());assertThat(corrections.consultar(orderId,CLIENTE,false).solicitudes().get(0).respuesta().archivos().get(0).aceptadaEn()).isNull();
   assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.aceptacion_vista_previa WHERE tipo_registro='ENVIO_PEDIDO'",Integer.class)).isEqualTo(2);
   var review=app.getBean(RevisionPedidoService.class);var approve=new RevisionPedidoController.Decision(UUID.randomUUID(),order.version(),RevisionPedidoController.Accion.APROBAR,true,"",null);
   long beforeApproval=order.version();order=review.decidir(orderId,approve,ADMIN);
   assertThat(order.version()).isEqualTo(beforeApproval+2);assertThat(review.decidir(orderId,approve,ADMIN).version()).isEqualTo(order.version());
   assertThat(review.consultar(orderId,ADMIN).revisadaEn()).isNotNull();
   var production=app.getBean(ProduccionService.class);var item=production.consultar(orderId,ADMIN).items().get(0);assertThat(item.puedeIniciar()).isTrue();
   var started=production.iniciar(orderId,new ProduccionController.Inicio(UUID.randomUUID(),order.version(),item.codigoPublico(),item.impresoras().get(0).codigoPublico(),"",true),ADMIN);assertThat(started.estado()).isEqualTo("EN_PRODUCCION");
   var job=started.items().get(0).trabajos().get(0);
   var allChecks=new ProduccionController.Checklist(true,true,true,true,true);
   long qualityVersion=started.version();
   assertThatThrownBy(()->production.finalizarCalidad(orderId,job.codigoPublico(),new ProduccionController.Inspeccion(UUID.randomUUID(),qualityVersion,ProduccionController.Resultado.REIMPRESION_REQUERIDA,allChecks,"",false),ADMIN)).hasMessageContaining("Confirm");
   var finish=new ProduccionController.Inspeccion(UUID.randomUUID(),qualityVersion,ProduccionController.Resultado.APROBADO,allChecks,"",true);
   // A failure in the second event must roll back the first event and the job's completion.
   jdbc.execute("CREATE FUNCTION lamontana.qa_fallo_calidad() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.accion='CONTROLAR_CALIDAD' THEN RAISE EXCEPTION 'qa fallo calidad'; END IF; RETURN NEW; END $$");
   jdbc.execute("CREATE TRIGGER qa_fallo_calidad BEFORE INSERT ON lamontana.historial_estado_pedido FOR EACH ROW EXECUTE FUNCTION lamontana.qa_fallo_calidad()");
   try{assertThatThrownBy(()->production.finalizarCalidad(orderId,job.codigoPublico(),finish,ADMIN)).hasMessageContaining("qa fallo calidad");
    var unchanged=production.consultar(orderId,ADMIN);assertThat(unchanged.version()).isEqualTo(qualityVersion);assertThat(unchanged.items().get(0).trabajos().get(0).estado()).isEqualTo("IMPRIMIENDO");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.historial_estado_pedido WHERE id_operacion=?",Integer.class,finish.operacion())).isZero();
   }finally{jdbc.execute("DROP TRIGGER qa_fallo_calidad ON lamontana.historial_estado_pedido");jdbc.execute("DROP FUNCTION lamontana.qa_fallo_calidad()");}
   var finished=production.finalizarCalidad(orderId,job.codigoPublico(),finish,ADMIN);assertThat(finished.estado()).isEqualTo("LISTO_PARA_ENTREGA");assertThat(finished.version()).isEqualTo(qualityVersion+2);
   assertThat(production.finalizarCalidad(orderId,job.codigoPublico(),finish,ADMIN).version()).isEqualTo(finished.version());
   assertThat(finished.items().get(0).trabajos().get(0).historial()).extracting(ProduccionService.Evento::accion).containsExactly("INICIAR_TRABAJO","COMPLETAR_TRABAJO","CONTROLAR_CALIDAD");
   assertThat(jdbc.queryForList("SELECT id_aceptacion_vista_previa,fecha,sha256,tipo_registro FROM lamontana.aceptacion_vista_previa WHERE tipo_registro='VISTA_PREVIA' ORDER BY id_aceptacion_vista_previa")).isEqualTo(historicos);
   // Invalid PDFs and antivirus rejection never become printable files.
   UUID fresh=prep.abrir("cliente2@prueba.local").codigoPublico();var invalid=upload.crear(fresh,UUID.randomUUID(),"invalido.pdf","cliente2@prueba.local");
   var rejected=upload.recibir(fresh,invalid.codigoPublico(),"cliente2@prueba.local",new ByteArrayInputStream(new byte[]{1,2,3}),3);assertThat(rejected.estado()).isEqualTo("RECHAZADO");
   av.response="stream: Test FOUND";var infected=upload.crear(fresh,UUID.randomUUID(),"infectado.pdf","cliente2@prueba.local");assertThat(upload.recibir(fresh,infected.codigoPublico(),"cliente2@prueba.local",new ByteArrayInputStream(pdf),pdf.length).estado()).isEqualTo("RECHAZADO");
  } finally {try(var paths=Files.walk(files)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 @Test void altaInternaSeparaBorradoresRegistraOperadorYAplicaElUmbralDeSena() throws Exception {
  Path files=Files.createTempDirectory("lamontana-alta-interna-");
  try(var pg=EmbeddedPostgres.builder().setServerConfig("unix_socket_directories", "").start();var av=new ClamavDePrueba();var app=new SpringApplicationBuilder(LaMontanaApplication.class).run(
   "--server.port=0","--spring.profiles.active=pruebas","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=",
   "--lamontana.archivos.directorio="+files,"--lamontana.pruebas.clave=MontanaPrueba2026!","--lamontana.archivos.antivirus.host=127.0.0.1","--lamontana.archivos.antivirus.port="+av.port())) {
   var jdbc=app.getBean(JdbcTemplate.class);
   var prep=app.getBean(PreparacionClienteService.class);
   var upload=app.getBean(ArchivoPreparadoService.class);
   var cotizaciones=app.getBean(CotizacionService.class);
   var pedidos=app.getBean(PedidoService.class);
   var revision=app.getBean(RevisionPedidoService.class);
   var org=app.getBean(OrganizacionService.class);
   var personal=prep.abrir(CLIENTE);
   String operador="produccion@prueba.local";
   assertThatThrownBy(()->prep.abrirInterna(operador,CLIENTE)).hasMessageContaining("permiso");
   assertThatThrownBy(()->prep.abrirInterna(CLIENTE,CLIENTE)).hasMessageContaining("cuenta interna");
   var base=URI.create("http://127.0.0.1:"+app.getEnvironment().getProperty("local.server.port"));
   var employeeSession=login(base,operador);
   var clientSession=login(base,CLIENTE);
   assertThat(abrirInternaHttp(base,employeeSession).statusCode()).isEqualTo(403);
   assertThat(abrirInternaHttp(base,clientSession).statusCode()).isEqualTo(403);
   var empleado=org.empleados().stream().filter(e->e.correo().equals(operador)).findFirst().orElseThrow();
   var matriz=cotizaciones.opciones(CLIENTE).casaMatriz().codigoPublico();
   org.actualizarEmpleado(empleado.codigoPublico(),new EdicionEmpleado(empleado.nombre(),empleado.apellido(),operador,"ACTIVO",
    empleado.version(),List.of(matriz),List.of("CREAR_PEDIDOS"),empleado.repartidor()),ADMIN);
   byte[] pdf=pdf(4);
   for(int copias:List.of(50,60)){
    String autor=copias==50?operador:ADMIN;
    var trabajo=prep.abrirInterna(autor,CLIENTE);
    UUID id=trabajo.codigoPublico();
    if(autor.equals(operador)){
     var result=abrirInternaHttp(base,employeeSession);
     assertThat(result.statusCode()).describedAs(result.body()).isEqualTo(200);
     assertThat(JsonMapper.builder().build().readTree(result.body()).get("codigoPublico").asString()).isEqualTo(id.toString());
    }
    assertThat(id).isNotEqualTo(personal.codigoPublico());
    assertThat(prep.abrirInterna(autor,CLIENTE).codigoPublico()).isEqualTo(id);
    assertThat(trabajo.cliente().correo()).isEqualTo(CLIENTE);
    assertThatThrownBy(()->prep.consultar(id,CLIENTE)).hasMessageContaining("no está disponible");
    var opciones=prep.opciones(id,autor);
    var combo=opciones.combinaciones().stream().filter(c->c.sucursal().equals(matriz)&&c.dobleFaz()).findFirst().orElseThrow();
    var pending=upload.crear(id,UUID.randomUUID(),"trabajo.pdf",autor);
    var archivo=upload.recibir(id,pending.codigoPublico(),autor,new ByteArrayInputStream(pdf),pdf.length);
    assertThat(archivo.estado()).isEqualTo("VALIDO");
    assertThatThrownBy(()->upload.archivo(id,archivo.codigoPublico(),CLIENTE)).hasMessageContaining("no está disponible");
    var item=new EvaluadorPrecioItem.Item(combo.servicio(),combo.formato(),combo.papel(),combo.color(),4,copias,true,List.of());
    var budget=prep.presupuestar(id,new PreparacionClienteService.Presupuestar(UUID.randomUUID(),prep.consultar(id,autor).version(),
     opciones.configuracion(),opciones.revisionComercial(),List.of(new PreparacionClienteService.Entrada(archivo.codigoPublico(),item))),autor);
    var checkout=prep.cotizar(id,new PreparacionClienteService.Cotizar(UUID.randomUUID(),budget.version(),
     new PreparacionClienteService.Preferencias(Modalidad.RETIRO_SUCURSAL,matriz,null,null,MedioPago.EFECTIVO)),autor);
    UUID cotizacion=checkout.cotizacion().codigoPublico();
    assertThatThrownBy(()->cotizaciones.detalle(cotizacion,CLIENTE)).hasMessageContaining("no está disponible");
    assertThatThrownBy(()->app.getBean(ArchivoService.class).listar(cotizacion,CLIENTE,false)).hasMessageContaining("no está disponible");
    assertThatThrownBy(()->app.getBean(PagoService.class).vista(cotizacion,CLIENTE,false)).hasMessageContaining("no está disponible");
    var comando=new PreparacionClienteService.Confirmar(UUID.randomUUID(),checkout.preparacion().version(),cotizacion,checkout.huella(),true,null);
    var pedido=prep.confirmar(id,comando,autor);
    assertThat(pedido.estado()).isEqualTo("PENDIENTE_REVISION");
    assertThat(prep.confirmar(id,comando,autor).codigoPublico()).isEqualTo(pedido.codigoPublico());
    assertThat(pedidos.detalle(pedido.codigoPublico(),CLIENTE,false).numero()).isEqualTo(pedido.numero());
    assertThat(pedidos.detalle(pedido.codigoPublico(),autor,true).historial().get(0).actor()).isNotBlank();
    assertThat(cotizaciones.detalle(cotizacion,CLIENTE).preparacion()).isNull();
    assertThat(jdbc.queryForObject("""
     SELECT u.correo FROM lamontana.historial_estado_pedido h JOIN lamontana.usuario u ON u.id_usuario=h.id_actor
     WHERE h.id_pedido=? ORDER BY h.id_evento LIMIT 1
     """,String.class,pedido.numero())).isEqualTo(autor);
    var aprobar=revision.consultar(pedido.codigoPublico(),ADMIN).acciones().stream()
     .filter(a->a.accion()==RevisionPedidoController.Accion.APROBAR).findFirst().orElseThrow();
    assertThat(pedido.condiciones().actuales().pagoPrevioRequerido()).isEqualTo("0.00");
    if(copias==50){
     assertThat(pedido.condiciones().actuales().senaRequerida()).isEqualTo("0.00");
     assertThat(aprobar.habilitada()).isTrue();
     assertThat(revision.decidir(pedido.codigoPublico(),new RevisionPedidoController.Decision(UUID.randomUUID(),pedido.version(),
      RevisionPedidoController.Accion.APROBAR,true,"",null),ADMIN).estado()).isEqualTo("APROBADO");
    }else{
     assertThat(new BigDecimal(pedido.condiciones().actuales().senaRequerida())).isEqualByComparingTo(new BigDecimal(pedido.oferta().total()).multiply(new BigDecimal("0.30")));
     assertThat(aprobar.habilitada()).isFalse();
     assertThat(aprobar.motivo()).contains("240 carillas","seña");
    }
   }
   assertThat(prep.abrir(CLIENTE).codigoPublico()).isEqualTo(personal.codigoPublico());
  } finally {try(var paths=Files.walk(files)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 @Test void invitadoDeMostradorCompraYRetiraEnLaSucursalSinCuentaNiCodigo() throws Exception {
  Path files=Files.createTempDirectory("lamontana-mostrador-");
  try(var pg=EmbeddedPostgres.builder().setServerConfig("unix_socket_directories", "").start();var av=new ClamavDePrueba();var app=new SpringApplicationBuilder(LaMontanaApplication.class).run(
   "--server.port=0","--spring.profiles.active=pruebas","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=",
   "--lamontana.archivos.directorio="+files,"--lamontana.pruebas.clave=MontanaPrueba2026!","--lamontana.archivos.antivirus.host=127.0.0.1","--lamontana.archivos.antivirus.port="+av.port())) {
   var jdbc=app.getBean(JdbcTemplate.class);
   var prep=app.getBean(PreparacionClienteService.class);
   var upload=app.getBean(ArchivoPreparadoService.class);
   var revision=app.getBean(RevisionPedidoService.class);
   var production=app.getBean(ProduccionService.class);
   var entrega=app.getBean(EntregaService.class);
   var pagos=app.getBean(PagoService.class);
   var opciones=app.getBean(CotizacionService.class).opciones(CLIENTE);
   var sucursal=opciones.destinosRetiro().stream().filter(b->!b.codigoPublico().equals(opciones.casaMatriz().codigoPublico())
    &&opciones.combinaciones().stream().anyMatch(o->o.sucursal().equals(b.codigoPublico()))).findFirst().orElseThrow();
   long usuarios=jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE r.codigo<>'INVITADO_MOSTRADOR'",Long.class);
   var http=URI.create("http://127.0.0.1:"+app.getEnvironment().getProperty("local.server.port"));
   var sesion=login(http,ADMIN);
   byte[] pdf=pdf();
   for(var medio:List.of(MedioPago.EFECTIVO,MedioPago.TRANSFERENCIA)){
    UUID operacion=UUID.randomUUID();
    var alta=sesion.send(HttpRequest.newBuilder(http.resolve("/api/operacion/preparaciones"))
     .header("X-CSRF-TOKEN",csrf(http,sesion)).header("Content-Type","application/json")
     .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(Map.of(
      "nombreInvitado","Mica del mostrador","sucursal",sucursal.codigoPublico(),"operacion",operacion)))).build(),HttpResponse.BodyHandlers.ofString());
    assertThat(alta.statusCode()).describedAs(alta.body()).isEqualTo(200);
    var p=prep.abrirInvitado(ADMIN,operacion,"Mica del mostrador",sucursal.codigoPublico());
    assertThat(p.codigoPublico()).isEqualTo(operacion);
    assertThat(p.cliente().invitado()).isTrue();
    assertThat(p.cliente().correo()).isNull();
    var opts=prep.opciones(operacion,ADMIN);
    assertThat(opts.destinosRetiro()).extracting(b->b.codigoPublico()).containsExactly(sucursal.codigoPublico());
    var combo=opts.combinaciones().get(0);
    var intento=upload.crear(operacion,UUID.randomUUID(),"trabajo.pdf",ADMIN);
    var archivo=upload.recibir(operacion,intento.codigoPublico(),ADMIN,new ByteArrayInputStream(pdf),pdf.length);
    assertThat(archivo.estado()).describedAs(archivo.mensaje()).isEqualTo("VALIDO");
    var item=new EvaluadorPrecioItem.Item(combo.servicio(),combo.formato(),combo.papel(),combo.color(),2,1,false,List.of());
    var budget=prep.presupuestar(operacion,new PreparacionClienteService.Presupuestar(UUID.randomUUID(),prep.consultar(operacion,ADMIN).version(),
      opts.configuracion(),opts.revisionComercial(),List.of(new PreparacionClienteService.Entrada(archivo.codigoPublico(),item))),ADMIN);
    var checkout=prep.cotizar(operacion,new PreparacionClienteService.Cotizar(UUID.randomUUID(),budget.version(),
     new PreparacionClienteService.Preferencias(Modalidad.RETIRO_SUCURSAL,sucursal.codigoPublico(),null,null,medio)),ADMIN);
    assertThat(checkout.revision().franja().origen()).isEqualTo(sucursal.codigoPublico());
    assertThat(checkout.cotizacion().oferta().entrega().llegadaEstimada()).isNull();
    var pedido=prep.confirmar(operacion,new PreparacionClienteService.Confirmar(UUID.randomUUID(),checkout.preparacion().version(),
     checkout.cotizacion().codigoPublico(),checkout.huella(),true,null),ADMIN);
    assertThat(pedido.nombreInvitado()).isEqualTo("Mica del mostrador");
    assertThat(pedido.retiroConTraslado()).isFalse();
    assertThat(app.getBean(PedidoService.class).listarGlobal(0,null,"CONFIRMADO_DESC",ADMIN).elementos())
     .filteredOn(o->o.numero()==pedido.numero()).extracting(PedidoService.Resumen::nombreInvitado).containsExactly(pedido.nombreInvitado());
    UUID id=pedido.codigoPublico();
    var datos=revision.consultar(id,ADMIN);
    assertThat(datos.cliente()).isEqualTo(pedido.nombreInvitado());
    assertThat(datos.correo()).isNull();
    assertThatThrownBy(()->app.getBean(PedidoService.class).detalle(id,CLIENTE,false)).hasMessageContaining("no está disponible");
    var aprobado=revision.decidir(id,new RevisionPedidoController.Decision(UUID.randomUUID(),pedido.version(),RevisionPedidoController.Accion.APROBAR,true,"",null),ADMIN);
    var trabajo=production.consultar(id,ADMIN).items().get(0);
    var iniciada=production.iniciar(id,new ProduccionController.Inicio(UUID.randomUUID(),aprobado.version(),trabajo.codigoPublico(),trabajo.impresoras().get(0).codigoPublico(),"",true),ADMIN);
    production.finalizarCalidad(id,iniciada.items().get(0).trabajos().get(0).codigoPublico(),new ProduccionController.Inspeccion(
     UUID.randomUUID(),iniciada.version(),ProduccionController.Resultado.APROBADO,new ProduccionController.Checklist(true,true,true,true,true),"",true),ADMIN);
    var listo=entrega.consultar(id,ADMIN,true);
    assertThat(listo.estadoLogistico()).isEqualTo("LISTO_RETIRO");
    assertThat(listo.codigo().texto()).isNull();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.aviso_codigo_entrega a JOIN lamontana.codigo_entrega c USING(id_codigo_entrega) WHERE c.id_pedido=?",
     Integer.class,pedido.numero())).isZero();
    String mostrador="/api/operacion/sucursales/"+sucursal.codigoPublico()+"/retiros";
    String otraSucursal="/api/operacion/sucursales/"+opciones.casaMatriz().codigoPublico()+"/retiros";
    var lista=sesion.send(HttpRequest.newBuilder(http.resolve(mostrador+"?texto=PED-"+pedido.numero())).GET().build(),HttpResponse.BodyHandlers.ofString());
    assertThat(lista.statusCode()).describedAs(lista.body()).isEqualTo(200);
    assertThat(JsonMapper.builder().build().readTree(lista.body()).get("pedidos").get(0).get("pedido").asString()).isEqualTo(id.toString());
    assertThat(sesion.send(HttpRequest.newBuilder(http.resolve(otraSucursal+"/"+id)).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(404);
    if(medio==MedioPago.EFECTIVO){
     var cobro=new EntregaController.CobrarEntregar(UUID.randomUUID(),listo.version(),null,listo.saldo(),null,null,true,true);
     var request=HttpRequest.newBuilder(http.resolve(mostrador+"/"+id+"/cobrar-efectivo"))
      .header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf(http,sesion))
      .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(cobro))).build();
     for(int repeticion=0;repeticion<2;repeticion++){
      var respuesta=sesion.send(request,HttpResponse.BodyHandlers.ofString());
      assertThat(respuesta.statusCode()).describedAs(respuesta.body()).isEqualTo(200);
      var vista=JsonMapper.builder().build().readTree(respuesta.body()).get("vista");
      assertThat(vista.get("estadoPedido").asString()).isEqualTo("CERRADO");
      assertThat(vista.get("saldo").asString()).isEqualTo("0.00");
     }
    }else{
     String rutaCobro=mostrador+"/"+id+"/pagos";
     var estadoCobro=sesion.send(HttpRequest.newBuilder(http.resolve(rutaCobro)).GET().build(),HttpResponse.BodyHandlers.ofString());
     assertThat(estadoCobro.statusCode()).describedAs(estadoCobro.body()).isEqualTo(200);
     assertThat(sesion.send(HttpRequest.newBuilder(http.resolve(otraSucursal+"/"+id+"/pagos")).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(404);
     var pago=new PagoController.Recibir(UUID.randomUUID(),null,medio,listo.saldo(),"Mostrador "+id,null,true,"Transferencia acreditada en mostrador",true);
     var request=HttpRequest.newBuilder(http.resolve(rutaCobro+"/recibir"))
      .header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf(http,sesion))
      .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(pago))).build();
     long cantidad=jdbc.queryForObject("SELECT count(*) FROM lamontana.pago",Long.class);
     for(int repeticion=0;repeticion<2;repeticion++){
      var resultado=sesion.send(request,HttpResponse.BodyHandlers.ofString());
      assertThat(resultado.statusCode()).describedAs(resultado.body()).isEqualTo(200);
      assertThat(JsonMapper.builder().build().readTree(resultado.body()).get("pendiente").asString()).isEqualTo("0.00");
     }
     assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.pago",Long.class)).isEqualTo(cantidad+1);
     var entregaInvitado=entrega.recibir(id,new EntregaController.Recibir(UUID.randomUUID(),listo.version(),null,null,null,true),ADMIN);
     assertThat(entregaInvitado.vista().entrega().receptor()).isEqualTo(pedido.nombreInvitado());
     assertThat(entregaInvitado.vista().saldo()).isEqualTo("0.00");
    }
   }
   assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE r.codigo<>'INVITADO_MOSTRADOR'",Long.class)).isEqualTo(usuarios);
  } finally {try(var paths=Files.walk(files)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 @Test void invitadoDeWhatsAppPuedeElegirCityBellYLaSucursalComparteSuCodigo() throws Exception {
  Path files=Files.createTempDirectory("lamontana-invitado-punto-");
  try(var pg=EmbeddedPostgres.builder().setServerConfig("unix_socket_directories", "").start();var av=new ClamavDePrueba();var app=new SpringApplicationBuilder(LaMontanaApplication.class).run(
   "--server.port=0","--spring.profiles.active=pruebas","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=",
   "--lamontana.archivos.directorio="+files,"--lamontana.pruebas.clave=MontanaPrueba2026!","--lamontana.archivos.antivirus.host=127.0.0.1","--lamontana.archivos.antivirus.port="+av.port())) {
   var jdbc=app.getBean(JdbcTemplate.class);
   var prep=app.getBean(PreparacionClienteService.class);
   var upload=app.getBean(ArchivoPreparadoService.class);
   var revision=app.getBean(RevisionPedidoService.class);
   var produccion=app.getBean(ProduccionService.class);
   var entrega=app.getBean(EntregaService.class);
   UUID sucursal=jdbc.queryForObject("SELECT codigo_publico FROM lamontana.sucursal WHERE codigo='LAPLATA'",UUID.class);
   UUID cityBell=jdbc.queryForObject("SELECT codigo_publico FROM lamontana.punto_entrega WHERE codigo='PUNTO-CITY-BELL'",UUID.class);
   var p=prep.abrirInvitado(ADMIN,UUID.randomUUID(),"Matías por WhatsApp",sucursal);
   var opciones=prep.opciones(p.codigoPublico(),ADMIN);
   assertThat(opciones.modalidades()).contains(Modalidad.RETIRO_PUNTO_ENTREGA);
   assertThat(opciones.puntos()).extracting(CotizacionService.Punto::codigoPublico).contains(cityBell);
   assertThat(opciones.puntos()).allSatisfy(punto->assertThat(punto.sucursal()).isEqualTo(sucursal));
   var combo=opciones.combinaciones().get(0);
   byte[] pdf=pdf();
   var carga=upload.crear(p.codigoPublico(),UUID.randomUUID(),"pedido-whatsapp.pdf",ADMIN);
   var archivo=upload.recibir(p.codigoPublico(),carga.codigoPublico(),ADMIN,new ByteArrayInputStream(pdf),pdf.length);
   var item=new EvaluadorPrecioItem.Item(combo.servicio(),combo.formato(),combo.papel(),combo.color(),2,1,false,List.of());
   p=prep.presupuestar(p.codigoPublico(),new PreparacionClienteService.Presupuestar(UUID.randomUUID(),prep.consultar(p.codigoPublico(),ADMIN).version(),
    opciones.configuracion(),opciones.revisionComercial(),List.of(new PreparacionClienteService.Entrada(archivo.codigoPublico(),item))),ADMIN);
   var checkout=prep.cotizar(p.codigoPublico(),new PreparacionClienteService.Cotizar(UUID.randomUUID(),p.version(),
    new PreparacionClienteService.Preferencias(Modalidad.RETIRO_PUNTO_ENTREGA,null,cityBell,null,MedioPago.TRANSFERENCIA)),ADMIN);
   assertThat(checkout.revision().franja().origen()).isEqualTo(sucursal);
   assertThat(checkout.revision().franja().destino()).isEqualTo(cityBell);
   var pedido=prep.confirmar(p.codigoPublico(),new PreparacionClienteService.Confirmar(UUID.randomUUID(),checkout.preparacion().version(),
    checkout.cotizacion().codigoPublico(),checkout.huella(),true,null),ADMIN);
   assertThat(pedido.nombreInvitado()).isEqualTo("Matías por WhatsApp");
   assertThat(pedido.reserva().modalidad()).isEqualTo(Modalidad.RETIRO_PUNTO_ENTREGA);
   var aprobada=revision.decidir(pedido.codigoPublico(),new RevisionPedidoController.Decision(UUID.randomUUID(),pedido.version(),RevisionPedidoController.Accion.APROBAR,true,"",null),ADMIN);
   var trabajo=produccion.consultar(pedido.codigoPublico(),ADMIN).items().get(0);
   var iniciada=produccion.iniciar(pedido.codigoPublico(),new ProduccionController.Inicio(UUID.randomUUID(),aprobada.version(),trabajo.codigoPublico(),trabajo.impresoras().get(0).codigoPublico(),"",true),ADMIN);
   produccion.finalizarCalidad(pedido.codigoPublico(),iniciada.items().get(0).trabajos().get(0).codigoPublico(),new ProduccionController.Inspeccion(
    UUID.randomUUID(),iniciada.version(),ProduccionController.Resultado.APROBADO,new ProduccionController.Checklist(true,true,true,true,true),"",true),ADMIN);
   var vista=entrega.consultar(pedido.codigoPublico(),ADMIN,true);
   vista=entrega.mover(pedido.codigoPublico(),new EntregaController.Movimiento(UUID.randomUUID(),vista.version(),EntregaController.Accion.PREPARAR_ENVIO,null,true),ADMIN);
   entrega.moverLote(new EntregaController.MovimientoLote(UUID.randomUUID(),EntregaController.Accion.SALIR_REPARTO,
    List.of(new EntregaController.PedidoLote(pedido.codigoPublico(),vista.version())),entrega.repartidores(ADMIN).get(0).codigoPublico()),ADMIN);
   vista=entrega.consultar(pedido.codigoPublico(),ADMIN,true);
   assertThat(vista.codigo().texto()).matches("[A-HJ-NP-Z2-9]{8}");
   var ficha=app.getBean(ar.com.lamontana.puntos.PuntoRetiroService.class).ficha(pedido.codigoPublico(),"punto.city-bell.retiro@prueba.local");
   assertThat(ficha.pedido().cliente()).isEqualTo("Matías por WhatsApp");
   assertThat(ficha.pedido().correo()).isNull();
   assertThat(ficha.entrega().codigo().texto()).isNull();
  } finally {try(var paths=Files.walk(files)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 private HttpClient login(URI base,String correo) throws Exception {
  var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
  var response=client.send(HttpRequest.newBuilder(base.resolve("/api/auth/login"))
   .header("X-CSRF-TOKEN",csrf(base,client)).header("Content-Type","application/x-www-form-urlencoded")
   .POST(HttpRequest.BodyPublishers.ofString("username="+correo+"&password=MontanaPrueba2026!")).build(),HttpResponse.BodyHandlers.ofString());
  assertThat(response.statusCode()).isEqualTo(200);
  return client;
 }
 private String csrf(URI base,HttpClient client) throws Exception {
  var response=client.send(HttpRequest.newBuilder(base.resolve("/api/auth/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
  assertThat(response.statusCode()).isEqualTo(200);
  return JsonMapper.builder().build().readTree(response.body()).get("token").asString();
 }
 private HttpResponse<String> abrirInternaHttp(URI base,HttpClient client) throws Exception {
  return client.send(HttpRequest.newBuilder(base.resolve("/api/operacion/preparaciones"))
   .header("X-CSRF-TOKEN",csrf(base,client)).header("Content-Type","application/json")
   .POST(HttpRequest.BodyPublishers.ofString("{\"correoCliente\":\""+CLIENTE+"\"}")).build(),HttpResponse.BodyHandlers.ofString());
 }
 private byte[] pdf() throws IOException {return pdf(2);}
 private byte[] pdf(int pages) throws IOException {try(var doc=new PDDocument();var out=new ByteArrayOutputStream()){
  for(int n=0;n<pages;n++)doc.addPage(new PDPage());
  doc.save(out);return out.toByteArray();
 }}
}
