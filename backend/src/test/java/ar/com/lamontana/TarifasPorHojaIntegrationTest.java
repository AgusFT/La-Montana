//#region ENCABEZADO · TarifasPorHojaIntegrationTest.java
/*
 * ========================================================================
 * ARCHIVO: TarifasPorHojaIntegrationTest.java
 * ========================================================================
 * FUNCIÓN
 * Verifica por HTTP y PostgreSQL aislados el guardado, validación, nombres, agrupaciones,
 * historial, reintentos, programación y persistencia tras reiniciar.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - No declara explícitamente.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * - [] void tarifasNombradasPersistenValidanYConservanHistorial() throws Exception
 * - [private] ConfigurableApplicationContext iniciar(String[] props)
 * - [private] int count(JdbcTemplate jdbc, String tabla)
 * - [private] HttpClient cliente()
 * - [private] URI uri(String path)
 * - [private] HttpResponse<String> get(HttpClient c, String path) throws Exception
 * - [private] String csrf(HttpClient c) throws Exception
 * - [private] HttpRequest request(HttpClient c, String path, Object data) throws Exception
 * - [private] HttpResponse<String> post(HttpClient c, String path, Object data) throws Exception
 * - [private] void login(HttpClient c, String mail) throws Exception
 * - [private] void status(HttpResponse<String> response, int code)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - TarifasPorHojaIntegrationTest (class).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.catalogo.CatalogoController;
import ar.com.lamontana.catalogo.CatalogoService;
import ar.com.lamontana.identidad.IdentidadController;
import ar.com.lamontana.identidad.IdentidadService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.*;
import java.net.http.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

class TarifasPorHojaIntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String CLAVE="CatalogoPruebas2026!",TOKEN="tarifas-por-hoja-token-unicamente-aislado";
    private int port;
    @Test void tarifasNombradasPersistenValidanYConservanHistorial() throws Exception {
        var files=Files.createTempDirectory("lamontana-tarifas-hoja-");
        try(var pg=EmbeddedPostgres.builder().start()) {
            String[] props={"--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+files,"--lamontana.instalacion.token="+TOKEN};
            ConfigurableApplicationContext app=iniciar(props);
            try {
                app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Admin","Tarifas","admin@example.test",CLAVE));
                var admin=cliente();login(admin,"admin@example.test");var jdbc=app.getBean(JdbcTemplate.class);
                status(post(admin,"/api/admin/catalogo/papeles-predefinidos/habilitar-todos",Map.of()),200);
                status(post(admin,"/api/admin/catalogo/servicios",Map.of("codigo","IMP","nombre","Impresión","tipo","IMPRESION")),201);
                var base=JSON.readTree(get(admin,"/api/admin/catalogo").body());
                var papeles=base.get("papelesHabilitados");
                var pairs=new ArrayList<Map<String,Object>>();
                for(int i=0;i<2;i++)pairs.add(new HashMap<>(Map.of("formato",papeles.get(i).get("formato").asString(),"papel",papeles.get(i).get("papel").asString(),"color","BLANCO_NEGRO","precio","30","recargoDobleFaz","0","habilitada",true)));
                String service=base.get("servicios").get(0).get("codigoPublico").asString();
                var offer=Map.of("servicio",service,"nombreVisible","Impresión","basePrecio","POR_CARILLA","precio","0","preparacionMinutos",5,"habilitado",true,"compatibilidades",List.of());
                var request=new HashMap<String,Object>();request.put("versionBase",null);request.put("operacion",UUID.randomUUID());request.put("motivo","Anterior por carilla");request.put("tarifas",List.of(pairs.get(0)));request.put("servicios",List.of(offer));
                var old=post(admin,"/api/admin/catalogo/revisiones",request);status(old,200);
                var oldId=JSON.readTree(old.body()).get("codigoPublico").asString();
                status(post(admin,"/api/admin/catalogo/revisiones",request),200);
                UUID grupo=UUID.randomUUID();
                for(var rate:pairs){rate.put("grupo",grupo);rate.put("nombre","Hojas habituales");rate.put("modoDobleFaz","FIJO");rate.put("valorDobleFaz","40");}
                request.put("versionBase",oldId);request.put("operacion",UUID.randomUUID());request.put("motivo","Precios por hoja");request.put("tarifas",pairs);
                var saved=post(admin,"/api/admin/catalogo/revisiones",request);status(saved,200);
                assertThat(JSON.readTree(saved.body()).get("tarifas").size()).isEqualTo(2);
                for(var rate:JSON.readTree(saved.body()).get("tarifas")){assertThat(rate.get("nombre").asString()).isEqualTo("Hojas habituales");assertThat(rate.get("grupo").asString()).isEqualTo(grupo.toString());assertThat(rate.get("modoDobleFaz").asString()).isEqualTo("FIJO");}
                assertThat(post(admin,"/api/admin/catalogo/revisiones",request).body()).isEqualTo(saved.body());
                pairs.get(0).put("nombre","Otro nombre");status(post(admin,"/api/admin/catalogo/revisiones",request),409);pairs.get(0).put("nombre","Hojas habituales");
                request.put("versionBase",JSON.readTree(saved.body()).get("codigoPublico").asString());request.put("operacion",UUID.randomUUID());
                pairs.get(0).put("precio","31");status(post(admin,"/api/admin/catalogo/revisiones",request),400);pairs.get(0).put("precio","30");
                for(var value:List.of("-1","1.001","9999999999999")){pairs.get(0).put("valorDobleFaz",value);status(post(admin,"/api/admin/catalogo/revisiones",request),400);}pairs.get(0).put("valorDobleFaz","40");
                pairs.get(0).put("recargoDobleFaz","1");status(post(admin,"/api/admin/catalogo/revisiones",request),400);pairs.get(0).put("recargoDobleFaz","0");
                pairs.get(0).remove("valorDobleFaz");status(post(admin,"/api/admin/catalogo/revisiones",request),400);pairs.get(0).put("valorDobleFaz","40");
                pairs.get(0).put("nombre"," ");status(post(admin,"/api/admin/catalogo/revisiones",request),400);pairs.get(0).put("nombre","Hojas habituales");
                request.put("tarifas",List.of(pairs.get(0),pairs.get(0)));status(post(admin,"/api/admin/catalogo/revisiones",request),400);request.put("tarifas",pairs);
                for(var rate:pairs){rate.put("modoDobleFaz","PORCENTAJE");rate.put("valorDobleFaz","999999999999.99");rate.put("precio","999999999999.99");}
                status(post(admin,"/api/admin/catalogo/revisiones",request),400);assertThat(count(jdbc,"catalogo_revision")).isEqualTo(2);
                for(String mode:List.of("ADICIONAL","PORCENTAJE")){
                    for(var rate:pairs){rate.put("modoDobleFaz",mode);rate.put("valorDobleFaz","10");rate.put("precio","30");}
                    request.put("operacion",UUID.randomUUID());var next=post(admin,"/api/admin/catalogo/revisiones",request);status(next,200);
                    var state=app.getBean(CatalogoService.class).estado();
                    var first=state.actual().tarifas().get(0);
                    var item=new ar.com.lamontana.catalogo.EvaluadorPrecioItem.Item(UUID.fromString(service),first.formato(),first.papel(),first.color(),3,2,true,List.of());
                    assertThat(app.getBean(ar.com.lamontana.catalogo.EvaluadorPrecioItem.class).calcular(state,item).subtotal()).isEqualTo(mode.equals("ADICIONAL")?"140.00":"126.00");
                    request.put("versionBase",JSON.readTree(next.body()).get("codigoPublico").asString());
                }
                var historic=JSON.readTree(get(admin,"/api/admin/catalogo/revisiones/"+oldId).body());
                assertThat(historic.get("tarifas").get(0).has("modoDobleFaz")).isFalse();
                request.put("operacion",UUID.randomUUID());request.put("programadaPara",java.time.Instant.now().plusSeconds(3600).toString());
                var future=post(admin,"/api/admin/catalogo/revisiones",request);status(future,200);var futureNode=JSON.readTree(future.body());assertThat(futureNode.get("estado").asString()).isEqualTo("PROGRAMADA");
                assertThat(futureNode.get("tarifas").get(0).get("nombre").asString()).isEqualTo("Hojas habituales");
                status(post(admin,"/api/admin/catalogo/programaciones/"+futureNode.get("codigoPublico").asString()+"/cancelar",Map.of("operacion",UUID.randomUUID(),"motivo","Fin de prueba")),200);
                String before=get(admin,"/api/admin/catalogo").body();app.close();app=iniciar(props);
                assertThat(get(admin,"/api/admin/catalogo").body()).isEqualTo(before);
            } finally {app.close();}
        } finally {Files.deleteIfExists(files);}
    }
    private ConfigurableApplicationContext iniciar(String[] props) {var app=new SpringApplicationBuilder(LaMontanaApplication.class).run(props);port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));return app;}
    private int count(JdbcTemplate jdbc,String tabla){return jdbc.queryForObject("SELECT count(*) FROM lamontana."+tabla,Integer.class);}
    private HttpClient cliente(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build();}
    private URI uri(String path){return URI.create("http://127.0.0.1:"+port+path);}
    private HttpResponse<String> get(HttpClient c,String path)throws Exception{return c.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private String csrf(HttpClient c)throws Exception{return JSON.readTree(get(c,"/api/auth/csrf").body()).get("token").asString();}
    private HttpRequest request(HttpClient c,String path,Object data)throws Exception{return HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf(c)).POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(data))).build();}
    private HttpResponse<String> post(HttpClient c,String path,Object data)throws Exception{return c.send(request(c,path,data),HttpResponse.BodyHandlers.ofString());}
    private void login(HttpClient c,String mail)throws Exception{var r=HttpRequest.newBuilder(uri("/api/auth/login")).timeout(Duration.ofSeconds(15)).header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",csrf(c)).POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(mail,java.nio.charset.StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(CLAVE,java.nio.charset.StandardCharsets.UTF_8))).build();status(c.send(r,HttpResponse.BodyHandlers.ofString()),200);}
    private void status(HttpResponse<String> response,int code){assertThat(response.statusCode()).withFailMessage("Esperado%s recibido%s: %s",code,response.statusCode(),response.body()).isEqualTo(code);}
}
