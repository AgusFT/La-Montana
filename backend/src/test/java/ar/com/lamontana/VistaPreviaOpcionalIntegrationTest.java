package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.archivos.*;
import ar.com.lamontana.catalogo.EvaluadorPrecioItem;
import ar.com.lamontana.cotizaciones.*;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad;
import ar.com.lamontana.pedidos.*;
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
   assertThatThrownBy(()->production.finalizarCalidad(orderId,job.codigoPublico(),new ProduccionController.Inspeccion(UUID.randomUUID(),qualityVersion,ProduccionController.Resultado.REIMPRESION_REQUERIDA,allChecks,"",true),ADMIN)).hasMessageContaining("motivo");
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
 private byte[] pdf() throws IOException {try(var doc=new PDDocument();var out=new ByteArrayOutputStream()){doc.addPage(new PDPage());doc.addPage(new PDPage());doc.save(out);return out.toByteArray();}}
}
