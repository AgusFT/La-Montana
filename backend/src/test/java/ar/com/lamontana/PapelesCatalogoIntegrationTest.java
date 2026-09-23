package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.catalogo.CatalogoService;
import ar.com.lamontana.identidad.IdentidadController;
import ar.com.lamontana.identidad.IdentidadService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.*;
import java.net.http.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

class PapelesCatalogoIntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String CLAVE="CatalogoPruebas2026!",TOKEN="token-de-catalogo-unicamente-de-pruebas";
    private int port;
    @Test void seleccionAtomicaReintentosPermisosYPreciosPublicadosConservados() throws Exception {
        var files=Files.createTempDirectory("lamontana-papeles-");
        try(var pg=EmbeddedPostgres.builder().start()) {
            String[] props={"--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+files,"--lamontana.instalacion.token="+TOKEN};
            var app=iniciar(props);
            try {
                app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Admin","Catálogo","admin@example.test",CLAVE));
                var admin=cliente();login(admin,"admin@example.test");var jdbc=app.getBean(JdbcTemplate.class);
                var catalog=app.getBean(CatalogoService.class);
                assertThat(catalog.estado().papelesPredefinidos()).hasSize(21);assertThat(catalog.estado().papelesHabilitados()).isEmpty();
                status(post(admin,"/api/admin/catalogo/papeles-predefinidos",Map.of("codigo","NO-EXISTE")),400);
                var pre=Map.of("codigo","A4-COMUN-80");
                var a=admin.sendAsync(request(admin,"/api/admin/catalogo/papeles-predefinidos",pre),HttpResponse.BodyHandlers.ofString());
                var b=admin.sendAsync(request(admin,"/api/admin/catalogo/papeles-predefinidos",pre),HttpResponse.BodyHandlers.ofString());
                status(a.get(),200);status(b.get(),200);
                assertThat(count(jdbc,"formato")).isEqualTo(1);assertThat(count(jdbc,"papel")).isEqualTo(1);assertThat(count(jdbc,"catalogo_papel")).isEqualTo(1);
                var initial=catalog.estado();var pair=initial.papelesHabilitados().get(0);
                assertThat(initial.formatos().get(0).anchoMm()).isEqualByComparingTo("210");assertThat(initial.formatos().get(0).altoMm()).isEqualByComparingTo("297");
                var custom=new HashMap<String,Object>(Map.of("codigo","ilust-a3-150","nombre","Ilustración","anchoMm",297,"altoMm",420,"gramaje",150,"terminacion","Mate"));
                var invalid=new HashMap<>(custom);invalid.put("gramaje",0);
                status(post(admin,"/api/admin/catalogo/papeles-personalizados",invalid),400);assertThat(count(jdbc,"formato")).isEqualTo(1);
                status(post(admin,"/api/admin/catalogo/papeles-personalizados",custom),200);status(post(admin,"/api/admin/catalogo/papeles-personalizados",custom),200);
                assertThat(count(jdbc,"catalogo_papel")).isEqualTo(2);assertThat(count(jdbc,"formato")).isEqualTo(2);assertThat(count(jdbc,"papel")).isEqualTo(2);
                assertThat(catalog.estado().papeles()).anyMatch(p->p.codigo().equals("ILUST-A3-150"));
                invalid=new HashMap<>(custom);invalid.put("anchoMm",111);
                status(post(admin,"/api/admin/catalogo/papeles-personalizados",invalid),409);assertThat(count(jdbc,"formato")).isEqualTo(2);
                status(post(admin,"/api/admin/catalogo/servicios",Map.of("codigo","IMP","nombre","Impresión","tipo","IMPRESION")),201);
                var rev=new HashMap<String,Object>();rev.put("operacion",UUID.randomUUID());rev.put("motivo","Primera tarifa");
                rev.put("tarifas",List.of(Map.of("formato",pair.formato(),"papel",pair.papel(),"color","BLANCO_NEGRO","precio",20,"recargoDobleFaz",0,"habilitada",true)));
                rev.put("servicios",List.of(Map.of("servicio",catalog.estado().servicios().get(0).codigoPublico(),"nombreVisible","Impresión","basePrecio","POR_CARILLA","precio",0,"preparacionMinutos",1,"habilitado",true,"compatibilidades",List.of())));
                status(post(admin,"/api/admin/catalogo/revisiones",rev),200);var published=catalog.estado().actual();
                var toggle=Map.of("formato",pair.formato(),"papel",pair.papel(),"habilitado",false);
                status(put(admin,"/api/admin/catalogo/papeles-habilitados",toggle),200);status(put(admin,"/api/admin/catalogo/papeles-habilitados",toggle),200);
                assertThat(catalog.estado().actual()).isEqualTo(published);
                rev.put("versionBase",published.codigoPublico());rev.put("operacion",UUID.randomUUID());status(post(admin,"/api/admin/catalogo/revisiones",rev),400);
                status(post(admin,"/api/admin/catalogo/papeles-predefinidos",pre),200);status(post(admin,"/api/admin/catalogo/revisiones",rev),200);
                var badpair=Map.of("formato",UUID.randomUUID(),"papel",pair.papel(),"habilitado",true);status(put(admin,"/api/admin/catalogo/papeles-habilitados",badpair),404);
                var csrfMissing=HttpRequest.newBuilder(uri("/api/admin/catalogo/papeles-predefinidos")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(pre))).build();status(admin.send(csrfMissing,HttpResponse.BodyHandlers.ofString()),403);
                var publicUser=cliente();status(post(publicUser,"/api/admin/catalogo/papeles-predefinidos",pre),401);
                status(post(publicUser,"/api/auth/registro",Map.of("nombre","Cliente","apellido","Test","correo","cliente@example.test","contrasena",CLAVE)),201);login(publicUser,"cliente@example.test");
                status(post(publicUser,"/api/admin/catalogo/papeles-predefinidos",pre),403);status(post(publicUser,"/api/admin/catalogo/papeles-personalizados",custom),403);status(put(publicUser,"/api/admin/catalogo/papeles-habilitados",toggle),403);
                String before=get(admin,"/api/admin/catalogo").body();app.close();app=iniciar(props);assertThat(get(admin,"/api/admin/catalogo").body()).isEqualTo(before);
            }finally{app.close();}
        }finally{Files.deleteIfExists(files);}
    }
    @Test void migracionConservaTodosLosParesAnterioresSinCrearPapelesNuevos() throws Exception {
        try(var pg=EmbeddedPostgres.builder().start()) {
            var ds=pg.getPostgresDatabase();Flyway.configure().dataSource(ds).defaultSchema("lamontana").schemas("lamontana").target("36").load().migrate();
            var jdbc=new JdbcTemplate(ds);
            jdbc.update("INSERT INTO lamontana.formato(codigo_publico,codigo,nombre,ancho_mm,alto_mm) VALUES (?,'A','Existente A',210,297),(?,'B','Existente B',297,420)",UUID.randomUUID(),UUID.randomUUID());
            jdbc.update("INSERT INTO lamontana.papel(codigo_publico,codigo,nombre,gramaje_g_m2,terminacion_tipo) VALUES (?,'P','Existente',80,'Mate')",UUID.randomUUID());
            var formats=jdbc.queryForList("SELECT * FROM lamontana.formato ORDER BY id_formato");var papers=jdbc.queryForList("SELECT * FROM lamontana.papel ORDER BY id_papel");
            Flyway.configure().dataSource(ds).defaultSchema("lamontana").schemas("lamontana").load().migrate();
            assertThat(jdbc.queryForList("SELECT * FROM lamontana.formato ORDER BY id_formato")).isEqualTo(formats);assertThat(jdbc.queryForList("SELECT * FROM lamontana.papel ORDER BY id_papel")).isEqualTo(papers);
            assertThat(count(jdbc,"catalogo_papel")).isEqualTo(2);assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.catalogo_papel WHERE habilitado AND predefinido IS NULL",Integer.class)).isEqualTo(2);
        }
    }
    private HttpResponse<String> put(HttpClient c,String path,Object data)throws Exception{return c.send(HttpRequest.newBuilder(uri(path)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf(c)).PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(data))).build(),HttpResponse.BodyHandlers.ofString());}
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
