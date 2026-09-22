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

class CatalogoIntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String CLAVE="CatalogoPruebas2026!",TOKEN="token-de-catalogo-unicamente-de-pruebas";
    private int port;
    @Test void catalogoVacioPreciosValidacionesRevisionesConcurrenciaPermisosYReinicio() throws Exception {
        var files=Files.createTempDirectory("lamontana-catalogo-");
        try(var pg=EmbeddedPostgres.builder().start()) {
            String[] props={"--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+files,"--lamontana.instalacion.token="+TOKEN};
            ConfigurableApplicationContext app=iniciar(props);
            try {
                app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Admin","Catálogo","admin@example.test",CLAVE));
                var admin=cliente();login(admin,"admin@example.test");
                var jdbc=app.getBean(JdbcTemplate.class);
                var vacio=JSON.readTree(get(admin,"/api/admin/catalogo").body());
                assertThat(vacio.get("actual").isNull()).isTrue();
                for(String k:List.of("formatos","papeles","servicios","historial"))assertThat(vacio.get(k).size()).isZero();
                var formato=new HashMap<String,Object>(Map.of("codigo","F1","nombre","Formato propio","anchoMm","210.00","altoMm","297.00"));
                var malo=new HashMap<>(formato);malo.put("anchoMm","0");status(post(admin,"/api/admin/catalogo/formatos",malo),400);
                malo.put("anchoMm","210.001");status(post(admin,"/api/admin/catalogo/formatos",malo),400);
                status(post(admin,"/api/admin/catalogo/formatos",formato),201);status(post(admin,"/api/admin/catalogo/formatos",formato),409);
                status(post(admin,"/api/admin/catalogo/papeles",Map.of("codigo","P1","nombre","Papel propio","gramaje","80.00","terminacion","Mate")),201);
                status(post(admin,"/api/admin/catalogo/servicios",Map.of("codigo","IMP","nombre","Impresión propia","tipo","IMPRESION")),201);
                status(post(admin,"/api/admin/catalogo/servicios",Map.of("codigo","TERM","nombre","Terminación propia","tipo","TERMINACION")),201);
                var datos=JSON.readTree(get(admin,"/api/admin/catalogo").body());
                String f=datos.get("formatos").get(0).get("codigoPublico").asString(),p=datos.get("papeles").get(0).get("codigoPublico").asString();
                String imp=datos.get("servicios").get(0).get("codigoPublico").asString(),term=datos.get("servicios").get(1).get("codigoPublico").asString();
                Map<String,Object> tarifa=new HashMap<>(Map.of("formato",f,"papel",p,"color","BLANCO_NEGRO","precio","12.34","recargoDobleFaz","0.55","habilitada",true));
                Map<String,Object> impresion=new HashMap<>(Map.of("servicio",imp,"nombreVisible","Impresión por carilla","basePrecio","POR_CARILLA","precio","0","preparacionMinutos",5,"habilitado",true,"compatibilidades",List.of()));
                Map<String,Object> terminacion=new HashMap<>(Map.of("servicio",term,"nombreVisible","Acabado propio","basePrecio","FIJO_POR_ITEM","precio","7.89","preparacionMinutos",10,"habilitado",true,"compatibilidades",List.of(Map.of("formato",f,"papel",p))));
                var revision=new HashMap<String,Object>();revision.put("versionBase",null);revision.put("operacion",UUID.randomUUID().toString());revision.put("motivo","Primera lista de precios");revision.put("tarifas",List.of(tarifa));revision.put("servicios",List.of(impresion,terminacion));
                var noCsrf=HttpRequest.newBuilder(uri("/api/admin/catalogo/revisiones")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(revision))).build();
                status(admin.send(noCsrf,HttpResponse.BodyHandlers.ofString()),403);
                var invalid=new HashMap<>(revision);invalid.put("tarifas",List.of());status(post(admin,"/api/admin/catalogo/revisiones",invalid),400);
                invalid.put("tarifas",Collections.singletonList(null));status(post(admin,"/api/admin/catalogo/revisiones",invalid),400);
                assertThat(count(jdbc,"catalogo_revision")).isZero();
                var guardada=post(admin,"/api/admin/catalogo/revisiones",revision);status(guardada,200);
                String primera=JSON.readTree(guardada.body()).get("codigoPublico").asString();
                var repetida=post(admin,"/api/admin/catalogo/revisiones",revision);status(repetida,200);assertThat(repetida.body()).isEqualTo(guardada.body());assertThat(count(jdbc,"catalogo_revision")).isEqualTo(1);
                invalid=new HashMap<>(revision);invalid.put("motivo","Otro contenido con mismo identificador");status(post(admin,"/api/admin/catalogo/revisiones",invalid),409);
                revision.put("versionBase",primera);revision.put("operacion",UUID.randomUUID().toString());tarifa.put("precio","19.99");
                // El mismo comando simultáneo guarda una única nueva revisión.
                var req=request(admin,"/api/admin/catalogo/revisiones",revision);
                var r1=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());var r2=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());
                status(r1.get(),200);status(r2.get(),200);assertThat(r1.get().body()).isEqualTo(r2.get().body());assertThat(count(jdbc,"catalogo_revision")).isEqualTo(2);
                String segunda=JSON.readTree(r1.get().body()).get("codigoPublico").asString();
                // La anterior permanece completa e inmutable.
                var historica=JSON.readTree(get(admin,"/api/admin/catalogo/revisiones/"+primera).body());
                var original=JSON.readTree(guardada.body());
                assertThat(historica.get("estado").asString()).isEqualTo("HISTORICA");
                for(String campo:List.of("codigoPublico","numero","motivo","creadaEn","actor","tarifas","servicios","activadaEn"))
                    assertThat(historica.get(campo)).isEqualTo(original.get(campo));
                assertThat(jdbc.queryForObject("SELECT precio_por_carilla FROM lamontana.tarifa_impresion ORDER BY id_tarifa_impresion LIMIT 1",java.math.BigDecimal.class)).isEqualByComparingTo("12.34");
                invalid=new HashMap<>(revision);invalid.put("operacion",UUID.randomUUID().toString());status(post(admin,"/api/admin/catalogo/revisiones",invalid),409);
                revision.put("versionBase",segunda);revision.put("operacion",UUID.randomUUID().toString());
                tarifa.put("precio","-1");status(post(admin,"/api/admin/catalogo/revisiones",revision),400);
                tarifa.put("precio","1.001");status(post(admin,"/api/admin/catalogo/revisiones",revision),400);tarifa.put("precio","19.99");
                tarifa.put("habilitada",false);status(post(admin,"/api/admin/catalogo/revisiones",revision),400);tarifa.put("habilitada",true);
                impresion.put("precio","1");status(post(admin,"/api/admin/catalogo/revisiones",revision),400);impresion.put("precio","0");
                terminacion.put("compatibilidades",List.of());status(post(admin,"/api/admin/catalogo/revisiones",revision),400);terminacion.put("compatibilidades",List.of(Map.of("formato",f,"papel",p)));
                tarifa.put("formato",UUID.randomUUID().toString());status(post(admin,"/api/admin/catalogo/revisiones",revision),400);tarifa.put("formato",f);
                invalid=new HashMap<>(revision);invalid.put("tarifas",List.of(tarifa,tarifa));status(post(admin,"/api/admin/catalogo/revisiones",invalid),400);
                // Falla de almacenamiento a mitad del guardado: tampoco publica ni deja filas parciales.
                jdbc.execute("CREATE FUNCTION lamontana.fallar_catalogo_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fallo inyectado en almacenamiento'; END $$");
                jdbc.execute("CREATE TRIGGER fallo_catalogo_test BEFORE INSERT ON lamontana.configuracion_servicio FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_catalogo_test()");
                var input=JSON.readValue(JSON.writeValueAsString(revision),CatalogoController.NuevaRevision.class);
                var catalogoService=app.getBean(CatalogoService.class);
                assertThatThrownBy(()->catalogoService.guardar(input,"admin@example.test")).isInstanceOf(org.springframework.dao.DataAccessException.class);
                jdbc.execute("DROP TRIGGER fallo_catalogo_test ON lamontana.configuracion_servicio");jdbc.execute("DROP FUNCTION lamontana.fallar_catalogo_test()");
                assertThat(count(jdbc,"catalogo_revision")).isEqualTo(2);assertThat(count(jdbc,"tarifa_impresion")).isEqualTo(2);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.catalogo_revision WHERE vigente",Integer.class)).isEqualTo(1);
                // Dos editores diferentes sobre la misma base: se acepta exactamente uno.
                var otra=new HashMap<>(revision);otra.put("operacion",UUID.randomUUID().toString());otra.put("motivo","Otro editor");
                var reqA=request(admin,"/api/admin/catalogo/revisiones",revision);var reqB=request(admin,"/api/admin/catalogo/revisiones",otra);
                r1=admin.sendAsync(reqA,HttpResponse.BodyHandlers.ofString());r2=admin.sendAsync(reqB,HttpResponse.BodyHandlers.ofString());
                assertThat(List.of(r1.get().statusCode(),r2.get().statusCode())).containsExactlyInAnyOrder(200,409);
                assertThat(count(jdbc,"catalogo_revision")).isEqualTo(3);
                var publico=cliente();status(get(publico,"/api/admin/catalogo"),401);
                status(post(publico,"/api/auth/registro",Map.of("nombre","Cliente","apellido","Test","correo","cliente@example.test","contrasena",CLAVE)),201);login(publico,"cliente@example.test");
                status(get(publico,"/api/admin/catalogo"),403);status(get(publico,"/api/admin/catalogo/revisiones/"+primera),403);status(post(publico,"/api/admin/catalogo/formatos",formato),403);status(post(publico,"/api/admin/catalogo/revisiones",revision),403);
                // La unicidad de impresión corresponde al ítem, no al catálogo global.
                status(post(admin,"/api/admin/catalogo/servicios",Map.of("codigo","IMP2","nombre","Otra impresión","tipo","IMPRESION")),201);
                var conDos=JSON.readTree(get(admin,"/api/admin/catalogo").body());
                String segundoServicio="";for(var servicio:conDos.get("servicios"))if(servicio.get("codigo").asString().equals("IMP2"))segundoServicio=servicio.get("codigoPublico").asString();
                var otraImpresion=new HashMap<>(impresion);otraImpresion.put("servicio",segundoServicio);otraImpresion.put("nombreVisible","Otra impresión");
                revision.put("versionBase",conDos.get("actual").get("codigoPublico").asString());revision.put("operacion",UUID.randomUUID().toString());revision.put("servicios",List.of(impresion,otraImpresion,terminacion));
                status(post(admin,"/api/admin/catalogo/revisiones",revision),200);
                assertThat(JSON.readTree(get(admin,"/api/admin/catalogo").body()).get("actual").get("servicios").size()).isEqualTo(3);
                String antes=get(admin,"/api/admin/catalogo").body();app.close();app=iniciar(props);
                var despues=get(admin,"/api/admin/catalogo");status(despues,200);assertThat(despues.body()).isEqualTo(antes);
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
