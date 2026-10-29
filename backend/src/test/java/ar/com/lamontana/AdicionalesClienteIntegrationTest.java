package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import static ar.com.lamontana.catalogo.CatalogoController.*;
import ar.com.lamontana.catalogo.*;
import ar.com.lamontana.cotizaciones.*;
import ar.com.lamontana.configuracion.RevisionConfiguracionService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Configured custom services must reach the client and the actual budget, without a fixed name/count list. */
class AdicionalesClienteIntegrationTest {
 private static final String ADMIN="admin@prueba.local",CLIENT="cliente1@prueba.local",PASSWORD="MontanaPrueba2026!";
 private static final JsonMapper JSON=JsonMapper.builder().build();
 private int port;
 @Test void todosLosAdicionalesVigentesCompatiblesLleganALaApiYSeCobran() throws Exception {
  Path files=Files.createTempDirectory("lamontana-adicionales-");
  try(var pg=EmbeddedPostgres.builder().setServerConfig("unix_socket_directories", "").start();var app=new SpringApplicationBuilder(LaMontanaApplication.class).run(
   "--server.port=0","--spring.profiles.active=pruebas","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+files,"--lamontana.pruebas.clave="+PASSWORD)){
   port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));
   var catalog=app.getBean(CatalogoService.class);var quotes=app.getBean(CotizacionService.class);var pricing=app.getBean(EvaluadorPrecioItem.class);
   var before=catalog.estado();var initial=quotes.opciones(CLIENT);
   var target=initial.combinaciones().stream().max(Comparator.comparingInt(o->o.terminaciones().size())).orElseThrow();
   var pair=new Compatibilidad(target.formato(),target.papel());
   var other=before.actual().tarifas().stream().filter(t->!t.formato().equals(pair.formato())||!t.papel().equals(pair.papel())).findFirst().orElseThrow();
   var offers=new ArrayList<>(before.actual().servicios());var extras=new ArrayList<UUID>();
   for(int n=1;n<=8;n++){
    String code="ACABADO-PRUEBA-"+n;catalog.servicio(new AltaServicio(code,"Acabado personalizado "+n,TipoServicio.TERMINACION,"Servicio adicional cargado desde administración"),ADMIN);
    UUID id=catalog.estado().servicios().stream().filter(s->s.codigo().equals(code)).findFirst().orElseThrow().codigoPublico();extras.add(id);
    offers.add(new OfertaServicio(id,"Acabado visible "+n,BasePrecio.FIJO_POR_ITEM,BigDecimal.valueOf(n),n,true,List.of(pair)));
   }
   UUID disabled=extra(catalog,"ACABADO-DESHABILITADO");UUID incompatible=extra(catalog,"ACABADO-OTRO-PAPEL");
   offers.add(new OfertaServicio(disabled,"No ofrecido",BasePrecio.FIJO_POR_ITEM,BigDecimal.TEN,1,false,List.of(pair)));
   offers.add(new OfertaServicio(incompatible,"Sólo otro papel",BasePrecio.FIJO_POR_ITEM,BigDecimal.TEN,1,true,List.of(new Compatibilidad(other.formato(),other.papel()))));
   var revision=catalog.guardar(new NuevaRevision(before.actual().codigoPublico(),UUID.randomUUID(),"Verificar adicionales dinámicos en cliente",before.actual().tarifas(),offers,null),ADMIN);
   var options=quotes.opciones(CLIENT);assertThat(options.revisionComercial()).isEqualTo(revision.codigoPublico());
   var expected=new ArrayList<>(target.terminaciones());expected.addAll(extras);
   assertThat(matches(options.combinaciones(),target)).isNotEmpty().allSatisfy(o->assertThat(o.terminaciones()).containsExactlyInAnyOrderElementsOf(expected));
   for(UUID id:extras)assertThat(options.servicios()).anySatisfy(s->{assertThat(s.codigoPublico()).isEqualTo(id);assertThat(s.nombre()).startsWith("Acabado visible ");});
   assertThat(options.servicios()).noneMatch(s->s.codigoPublico().equals(disabled));
   var basic=new EvaluadorPrecioItem.Item(target.servicio(),target.formato(),target.papel(),target.color(),5,2,false,target.terminaciones());
   var all=new EvaluadorPrecioItem.Item(target.servicio(),target.formato(),target.papel(),target.color(),5,2,false,expected);
   var document=new CotizacionController.Documento("trabajo-con-adicionales.pdf",1000,"a".repeat(64));
   var budget=quotes.presupuestar(options.configuracion(),options.revisionComercial(),List.of(new CotizacionController.Item(document,all)),CLIENT);
   var price=budget.items().get(0).precio();
   assertThat(price.lineas().stream().map(EvaluadorPrecioItem.Linea::servicio)).containsAll(extras);
   assertThat(new BigDecimal(price.subtotal())).isEqualByComparingTo(new BigDecimal(pricing.calcular(before,basic).subtotal()).add(new BigDecimal("36.00")));
   assertThatThrownBy(()->pricing.calcular(catalog.estado(),new EvaluadorPrecioItem.Item(target.servicio(),target.formato(),target.papel(),target.color(),5,2,false,List.of(disabled)))).hasMessageContaining("oferta habilitada");
   assertThatThrownBy(()->pricing.calcular(catalog.estado(),new EvaluadorPrecioItem.Item(target.servicio(),target.formato(),target.papel(),target.color(),5,2,false,List.of(incompatible)))).hasMessageContaining("formato y papel");
   for(String client:List.of(CLIENT,"cliente2@prueba.local")){
    var node=get(login(client),"/api/cliente/cotizaciones/opciones");
    var matching=new ArrayList<JsonNode>();for(var row:node.get("combinaciones"))if(row.get("servicio").asString().equals(target.servicio().toString())&&row.get("formato").asString().equals(target.formato().toString())&&row.get("papel").asString().equals(target.papel().toString())&&row.get("color").asString().equals(target.color().name()))matching.add(row);
    assertThat(matching).isNotEmpty().allSatisfy(row->{var ids=new ArrayList<String>();row.get("terminaciones").forEach(x->ids.add(x.asString()));assertThat(ids).containsExactlyInAnyOrderElementsOf(expected.stream().map(UUID::toString).toList());});
   }
   UUID removed=extras.get(0);var changed=offers.stream().map(s->s.servicio().equals(removed)?new OfertaServicio(s.servicio(),s.nombreVisible(),s.basePrecio(),s.precio(),s.preparacionMinutos(),false,s.compatibilidades()):s).toList();
   catalog.guardar(new NuevaRevision(revision.codigoPublico(),UUID.randomUUID(),"Retirar un adicional de la oferta vigente",revision.tarifas(),changed,null),ADMIN);
   assertThat(matches(quotes.opciones(CLIENT).combinaciones(),target)).allSatisfy(o->assertThat(o.terminaciones()).doesNotContain(removed).containsAll(extras.subList(1,extras.size())));
   assertThat(catalog.revision(revision.codigoPublico()).servicios()).anySatisfy(s->{assertThat(s.servicio()).isEqualTo(removed);assertThat(s.habilitado()).isTrue();});
  }finally{try(var paths=Files.walk(files)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 private UUID extra(CatalogoService catalog,String code){catalog.servicio(new AltaServicio(code,code,TipoServicio.TERMINACION,null),ADMIN);return catalog.estado().servicios().stream().filter(s->s.codigo().equals(code)).findFirst().orElseThrow().codigoPublico();}
 private List<RevisionConfiguracionService.Opcion> matches(List<RevisionConfiguracionService.Opcion> options,RevisionConfiguracionService.Opcion target){return options.stream().filter(o->o.servicio().equals(target.servicio())&&o.formato().equals(target.formato())&&o.papel().equals(target.papel())&&o.color()==target.color()).toList();}
 private URI uri(String path){return URI.create("http://127.0.0.1:"+port+path);}
 private JsonNode get(HttpClient client,String path)throws Exception{var r=client.send(HttpRequest.newBuilder(uri(path)).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(r.statusCode()).describedAs(r.body()).isEqualTo(200);return JSON.readTree(r.body());}
 private HttpClient login(String email)throws Exception{var c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();String csrf=get(c,"/api/auth/csrf").get("token").asString();var r=c.send(HttpRequest.newBuilder(uri("/api/auth/login")).header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",csrf).POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(email,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(PASSWORD,StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());assertThat(r.statusCode()).isEqualTo(200);return c;}
}
