package ar.com.lamontana;
import static org.assertj.core.api.Assertions.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.*;import java.net.http.*;import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
class SoporteIntegrationTest {
    static final String TOKEN="token-de-prueba-local-solo-en-test-12345678",PASSWORD="SoloPruebasContrasena123";
    static final JsonMapper JSON=JsonMapper.builder().build();int port;
    @Test void altaLocalPermisoTemporalYRevocacionEfectiva() throws Exception {
        try(var pg=EmbeddedPostgres.builder().start();var app=new SpringApplicationBuilder(LaMontanaApplication.class).profiles("imprenta-vacia").run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.instalacion.token="+TOKEN)){
            port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));var admin=cliente();var soporte=cliente();
            String cuentas=JSON.writeValueAsString(Map.of("propietario",cuenta("admin@example.test"),"soporte",cuenta("soporte@example.test")));
            assertThat(instalar(cuentas,"incorrecto").statusCode()).isEqualTo(403);
            assertThat(instalar(cuentas,TOKEN).statusCode()).isEqualTo(200);
            assertThat(instalar(cuentas,TOKEN).statusCode()).isEqualTo(409);
            assertThat(login(soporte,"soporte@example.test").statusCode()).isEqualTo(401);
            assertThat(login(admin,"admin@example.test").statusCode()).isEqualTo(200);
            assertThat(post(admin,"/api/admin/soporte/autorizar",Map.of("contrasena",PASSWORD,"motivo","Asistencia solicitada","horas",25)).statusCode()).isEqualTo(400);
            assertThat(post(admin,"/api/admin/soporte/autorizar",Map.of("contrasena",PASSWORD,"motivo","Asistencia solicitada","horas",1)).statusCode()).isEqualTo(200);
            assertThat(login(soporte,"soporte@example.test").statusCode()).isEqualTo(200);
            assertThat(get(soporte,"/api/admin/configuracion").statusCode()).isEqualTo(200);
            assertThat(post(soporte,"/api/admin/soporte/autorizar",Map.of("contrasena",PASSWORD,"motivo","No puede autorizarse","horas",1)).statusCode()).isEqualTo(403);
            var jdbc=app.getBean(JdbcTemplate.class);
            jdbc.update("UPDATE lamontana.autorizacion_soporte SET fecha_inicio=now()-interval '2 hours',fecha_fin=now()-interval '1 hour' WHERE estado='VIGENTE'");
            assertThat(get(soporte,"/api/admin/configuracion").statusCode()).isEqualTo(401);
            assertThat(login(soporte,"soporte@example.test").statusCode()).isEqualTo(401);
            assertThat(post(admin,"/api/admin/soporte/autorizar",Map.of("contrasena",PASSWORD,"motivo","Nueva asistencia","horas",2)).statusCode()).isEqualTo(200);
            assertThat(login(soporte,"soporte@example.test").statusCode()).isEqualTo(200);
            assertThat(post(admin,"/api/admin/soporte/revocar",Map.of("contrasena",PASSWORD)).statusCode()).isEqualTo(200);
            assertThat(get(soporte,"/api/admin/configuracion").statusCode()).isEqualTo(401);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_soporte WHERE tipo='SOLICITUD_APP'",Integer.class)).isGreaterThan(0);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario WHERE es_soporte",Integer.class)).isEqualTo(1);
        }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void soporteOpcionalYAltaPosterior(boolean usarOtroAdministrador) throws Exception {
        try(var pg=EmbeddedPostgres.builder().start();var app=new SpringApplicationBuilder(LaMontanaApplication.class).profiles("imprenta-vacia").run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.instalacion.token="+TOKEN)){
            port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));var jdbc=app.getBean(JdbcTemplate.class);
            var propietario=cuenta("admin@example.test");
            assertThat(instalar(JSON.writeValueAsString(Map.of("propietario",propietario,"soporte",Map.of("correo","invalido"))),TOKEN).statusCode()).isEqualTo(400);
            assertThat(instalar(JSON.writeValueAsString(Map.of("propietario",propietario,"soporte",propietario)),TOKEN).statusCode()).isEqualTo(409);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario",Integer.class)).isZero();
            assertThat(instalar(JSON.writeValueAsString(Map.of("propietario",propietario)),TOKEN).statusCode()).isEqualTo(200);
            var estado=HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/api/instalador/estado")).header("X-LaMontana-Instalacion",TOKEN).build(),HttpResponse.BodyHandlers.ofString());
            assertThat(JSON.readTree(estado.body()).get("cuentasCompletas").asBoolean()).isTrue();
            assertThat(JSON.readTree(estado.body()).get("soporteExiste").asBoolean()).isFalse();
            // El token local no permite crear después una cuenta que se omitió.
            assertThat(instalar(JSON.writeValueAsString(Map.of("propietario",propietario,"soporte",cuenta("soporte@example.test"))),TOKEN).statusCode()).isEqualTo(409);
            for(String rol:List.of("ADMIN_ADMIN","EMPLEADO","CLIENTE"))jdbc.update("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado) SELECT ?,(SELECT id_rol FROM lamontana.rol WHERE codigo=?),?,hash_contrasena,'Otra','Persona','ACTIVO' FROM lamontana.usuario WHERE correo='admin@example.test'",UUID.randomUUID(),rol,rol.toLowerCase(Locale.ROOT)+"@example.test");
            var alta=new HashMap<>(cuenta("soporte@example.test"));alta.put("contrasenaActual",PASSWORD);
            assertThat(post(cliente(),"/api/admin/soporte",alta).statusCode()).isEqualTo(401);
            for(String rol:List.of("EMPLEADO","CLIENTE")){
                var ajeno=cliente();assertThat(login(ajeno,rol.toLowerCase(Locale.ROOT)+"@example.test").statusCode()).isEqualTo(200);
                assertThat(post(ajeno,"/api/admin/soporte",alta).statusCode()).isEqualTo(403);
            }
            String actor=usarOtroAdministrador?"admin_admin@example.test":"admin@example.test";
            var admin=cliente();assertThat(login(admin,actor).statusCode()).isEqualTo(200);
            assertThat(admin.send(HttpRequest.newBuilder(uri("/api/admin/soporte")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(alta))).build(),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
            alta.put("contrasenaActual","incorrecta");assertThat(post(admin,"/api/admin/soporte",alta).statusCode()).isEqualTo(400);alta.put("contrasenaActual",PASSWORD);
            alta.put("correo"," ADMIN@example.test ".strip());assertThat(post(admin,"/api/admin/soporte",alta).statusCode()).isEqualTo(409);alta.put("correo","soporte@example.test");
            alta.put("contrasena","corta");assertThat(post(admin,"/api/admin/soporte",alta).statusCode()).isEqualTo(400);alta.put("contrasena",PASSWORD);
            jdbc.update("UPDATE lamontana.usuario SET debe_cambiar_contrasena=true WHERE correo=?",actor);
            assertThat(post(admin,"/api/admin/soporte",alta).statusCode()).isEqualTo(403);
            jdbc.update("UPDATE lamontana.usuario SET debe_cambiar_contrasena=false WHERE correo=?",actor);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario WHERE es_soporte",Integer.class)).isZero();
            var primera=java.util.concurrent.CompletableFuture.supplyAsync(()->{try{return post(admin,"/api/admin/soporte",alta).statusCode();}catch(Exception e){throw new RuntimeException(e);}});
            var segunda=java.util.concurrent.CompletableFuture.supplyAsync(()->{try{return post(admin,"/api/admin/soporte",alta).statusCode();}catch(Exception e){throw new RuntimeException(e);}});
            assertThat(List.of(primera.join(),segunda.join())).containsExactlyInAnyOrder(200,409);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario WHERE es_soporte",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.autorizacion_soporte",Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_soporte e JOIN lamontana.usuario u USING(id_usuario) WHERE e.tipo='ALTA_DESDE_APP' AND u.correo=?",Integer.class,actor)).isEqualTo(1);
            var soporte=cliente();assertThat(login(soporte,"soporte@example.test").statusCode()).isEqualTo(401);
            assertThat(post(admin,"/api/admin/soporte/autorizar",Map.of("contrasena",PASSWORD,"motivo","Ayuda posterior","horas",1)).statusCode()).isEqualTo(200);
            assertThat(login(soporte,"soporte@example.test").statusCode()).isEqualTo(200);
            assertThat(post(soporte,"/api/admin/soporte",alta).statusCode()).isEqualTo(403);
            assertThat(post(admin,"/api/admin/soporte/revocar",Map.of("contrasena",PASSWORD)).statusCode()).isEqualTo(200);
            assertThat(get(soporte,"/api/admin/configuracion").statusCode()).isEqualTo(401);
            assertThat(login(admin,actor).statusCode()).isEqualTo(200);
        }
    }
    Map<String,String> cuenta(String correo){return Map.of("nombre","Persona","apellido","Prueba","correo",correo,"contrasena",PASSWORD);}
    HttpClient cliente(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();}
    URI uri(String p){return URI.create("http://127.0.0.1:"+port+p);}
    HttpResponse<String> get(HttpClient c,String p)throws Exception{return c.send(HttpRequest.newBuilder(uri(p)).build(),HttpResponse.BodyHandlers.ofString());}
    String csrf(HttpClient c)throws Exception{return JSON.readTree(get(c,"/api/auth/csrf").body()).get("token").asString();}
    HttpResponse<String> login(HttpClient c,String email)throws Exception{return c.send(HttpRequest.newBuilder(uri("/api/auth/login")).header("X-CSRF-TOKEN",csrf(c)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(email,java.nio.charset.StandardCharsets.UTF_8)+"&password="+PASSWORD)).build(),HttpResponse.BodyHandlers.ofString());}
    HttpResponse<String> post(HttpClient c,String p,Object value)throws Exception{return c.send(HttpRequest.newBuilder(uri(p)).header("X-CSRF-TOKEN",csrf(c)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(value))).build(),HttpResponse.BodyHandlers.ofString());}
    HttpResponse<String> instalar(String body,String token)throws Exception{return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/api/instalador/cuentas")).header("Content-Type","application/json").header("X-LaMontana-Instalacion",token).POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
}
