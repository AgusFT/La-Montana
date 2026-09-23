package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.identidad.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class PaginaWebIntegrationTest {
    static final JsonMapper JSON=JsonMapper.builder().build();
    static final String EMAIL="sitio@example.test",PASSWORD="SitioPruebas2026!",TOKEN="instalacion-web-test-exclusiva-2026-09";
    EmbeddedPostgres pg;ConfigurableApplicationContext app;JdbcTemplate jdbc;Path files;HttpClient admin;int port;
    @BeforeEach void iniciar()throws Exception{
        pg=EmbeddedPostgres.builder().start();files=Files.createTempDirectory("lamontana-web-test-");arrancar();
        status(get(cliente(),"/api/admin/pagina-web"),401);
        app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Dueño","Sitio",EMAIL,PASSWORD));admin=cliente();login(admin,EMAIL);
    }
    void arrancar(){app=new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+files,"--lamontana.instalacion.token="+TOKEN);port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));jdbc=app.getBean(JdbcTemplate.class);}
    @AfterEach void cerrar()throws Exception{if(app!=null)app.close();if(pg!=null)pg.close();Files.deleteIfExists(files);}
    JsonNode estado()throws Exception{var r=get(admin,"/api/admin/pagina-web");status(r,200);return JSON.readTree(r.body());}
    Map<String,Object> contenido(){var c=new LinkedHashMap<String,Object>();c.putAll(Map.of("nombre","Imprenta propia","titulo","Trabajos de nuestra imprenta","descripcion","Presentación configurada por su propietario","logoAlt","","portadaAlt","","correo","contacto@example.test","telefono","","direccion","","secciones",List.of("PORTADA","CATALOGO","CONTACTO"),"fichas",List.of()));c.put("logo",null);c.put("portada",null);return c;}
    Map<String,Object> ficha(String nombre,boolean visible){var f=new LinkedHashMap<String,Object>();f.putAll(Map.of("codigo",UUID.randomUUID(),"tipo","PRODUCTO","nombre",nombre,"descripcion","Descripción propia","imagenes",List.of(),"orden",1,"visible",visible));f.put("servicio",null);return f;}
    Map<String,Object> guardarDatos(long v,Map<String,Object> c){return Map.of("operacion",UUID.randomUUID(),"version",v,"contenido",c,"carpeta","entrada-propia");}
    JsonNode guardar(long v,Map<String,Object> c)throws Exception{var r=mutar(admin,"PUT","/api/admin/pagina-web/borrador",guardarDatos(v,c));status(r,200);return JSON.readTree(r.body());}
    Map<String,Object> publicarDatos(long v){return Map.of("operacion",UUID.randomUUID(),"version",v,"confirmado",true);}
    HttpResponse<String> publicar(Map<String,Object> datos)throws Exception{return mutar(admin,"POST","/api/admin/pagina-web/publicar",datos);}
    JsonNode publico()throws Exception{var r=get(cliente(),"/api/publico/pagina-web");status(r,200);return JSON.readTree(r.body());}

    @Test void borradorPrivadoPublicacionExplicitaYContenidoVisible()throws Exception{
        assertThat(publico().get("configurada").asBoolean()).isFalse();
        var c=contenido();c.put("fichas",List.of(ficha("Trabajo público",true),ficha("SECRETO-OCULTO",false)));c.put("secciones",List.of("PORTADA","CATALOGO"));
        var b=guardar(1,c);assertThat(publico().get("contenido").isNull()).isTrue();
        var r=get(admin,"/api/admin/pagina-web/revision");status(r,200);assertThat(JSON.readTree(r.body()).get("publicable").asBoolean()).isTrue();
        var solicitud=publicarDatos(b.get("borrador").get("version").asLong());status(publicar(solicitud),200);
        var visible=publico();assertThat(visible.get("contenido").get("fichas").size()).isEqualTo(1);
        assertThat(visible.toString()).contains("Trabajo público").doesNotContain("SECRETO-OCULTO","entrada-propia",EMAIL,"contacto@example.test");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_version",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.catalogo_revision",Integer.class)).isZero();
        assertThatThrownBy(()->jdbc.update("UPDATE lamontana.web_publicacion SET autor='alterado'")).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void validacionNoPublicaDatosIncompletosNiReemplazaVersionAnterior()throws Exception{
        status(publicar(publicarDatos(1)),409);
        var b=guardar(1,contenido());long v=b.get("borrador").get("version").asLong();status(publicar(publicarDatos(v)),200);String publicada=publico().toString();
        var c=contenido();c.put("nombre","");c.put("fichas",List.of(ficha("",true)));b=guardar(v,c);v=b.get("borrador").get("version").asLong();
        status(publicar(publicarDatos(v)),409);assertThat(publico().toString()).isEqualTo(publicada);
        var sinConfirmar=new HashMap<>(publicarDatos(v));sinConfirmar.put("confirmado",false);status(publicar(sinConfirmar),400);
        c=contenido();c.put("logo",UUID.randomUUID());status(mutar(admin,"PUT","/api/admin/pagina-web/borrador",guardarDatos(v,c)),400);
        var ruta=new HashMap<>(guardarDatos(v,contenido()));ruta.put("carpeta","../privados");status(mutar(admin,"PUT","/api/admin/pagina-web/borrador",ruta),400);
        assertThat(publico().toString()).isEqualTo(publicada);assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.web_publicacion",Integer.class)).isEqualTo(1);
    }
    @Test void conflictosEntrePestanasReintentosYConcurrenciaNoDuplicanPublicaciones()throws Exception{
        var save=guardarDatos(1,contenido());var first=mutar(admin,"PUT","/api/admin/pagina-web/borrador",save);status(first,200);status(mutar(admin,"PUT","/api/admin/pagina-web/borrador",save),200);
        status(mutar(admin,"PUT","/api/admin/pagina-web/borrador",guardarDatos(1,contenido())),409);
        long version=estado().get("borrador").get("version").asLong();assertThat(version).isEqualTo(2);
        var command=publicarDatos(version);var request=solicitud(admin,"POST","/api/admin/pagina-web/publicar",command);
        var a=admin.sendAsync(request,HttpResponse.BodyHandlers.ofString());var b=admin.sendAsync(request,HttpResponse.BodyHandlers.ofString());status(a.get(),200);status(b.get(),200);assertThat(a.get().body()).isEqualTo(b.get().body());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.web_publicacion",Integer.class)).isEqualTo(1);
        var changed=contenido();changed.put("titulo","Segunda publicación");version=guardar(version,changed).get("borrador").get("version").asLong();status(publicar(publicarDatos(version)),200);
        status(publicar(command),200);assertThat(publico().get("version").asLong()).isEqualTo(2);assertThat(publico().toString()).contains("Segunda publicación");
        var altered=new HashMap<>(command);altered.put("version",version);status(publicar(altered),409);
    }
    @Test void permisosCsrfYRemotoNoImplementado()throws Exception{
        var anon=cliente();status(get(anon,"/api/publico/pagina-web"),200);status(get(anon,"/api/admin/pagina-web/revision"),401);
        var csrfMissing=HttpRequest.newBuilder(uri("/api/admin/pagina-web/borrador")).header("Content-Type","application/json").PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(guardarDatos(1,contenido())))).build();status(admin.send(csrfMissing,HttpResponse.BodyHandlers.ofString()),403);
        for(String rol:List.of("CLIENTE","EMPLEADO","ADMIN_ADMIN")){
            String correo=rol.toLowerCase()+"-web@example.test";
            jdbc.update("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado) SELECT ?,r.id_rol,?,u.hash_contrasena,'Interno','Test','ACTIVO' FROM lamontana.usuario u CROSS JOIN lamontana.rol r WHERE u.correo=? AND r.codigo=?",UUID.randomUUID(),correo,EMAIL,rol);
            var user=cliente();login(user,correo);status(get(user,"/api/admin/pagina-web"),403);status(mutar(user,"PUT","/api/admin/pagina-web/borrador",guardarDatos(1,contenido())),403);status(mutar(user,"POST","/api/admin/pagina-web/publicar",publicarDatos(1)),403);
        }
        status(mutar(admin,"POST","/api/admin/pagina-web/remoto",Map.of("dominio","ejemplo.test")),403);
        assertThat(estado().get("borrador").get("version").asLong()).isEqualTo(1);
    }
    @Test void reinicioConservaBorradorYPublicacionSeparados()throws Exception{
        long v=guardar(1,contenido()).get("borrador").get("version").asLong();status(publicar(publicarDatos(v)),200);
        var c=contenido();c.put("nombre","Cambio todavía privado");guardar(v,c);String antes=estado().toString(),publicada=publico().toString();
        app.close();arrancar();assertThat(estado().toString()).isEqualTo(antes);assertThat(publico().toString()).isEqualTo(publicada).doesNotContain("Cambio todavía privado");
    }
    HttpClient cliente(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();}
    URI uri(String p){return URI.create("http://127.0.0.1:"+port+p);}
    HttpResponse<String> get(HttpClient c,String p)throws Exception{return c.send(HttpRequest.newBuilder(uri(p)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    String csrf(HttpClient c)throws Exception{return JSON.readTree(get(c,"/api/auth/csrf").body()).get("token").asString();}
    HttpRequest solicitud(HttpClient c,String method,String path,Object body)throws Exception{return HttpRequest.newBuilder(uri(path)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf(c)).method(method,HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build();}
    HttpResponse<String> mutar(HttpClient c,String method,String path,Object body)throws Exception{return c.send(solicitud(c,method,path,body),HttpResponse.BodyHandlers.ofString());}
    void login(HttpClient c,String correo)throws Exception{var r=HttpRequest.newBuilder(uri("/api/auth/login")).header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",csrf(c)).POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(correo,java.nio.charset.StandardCharsets.UTF_8)+"&password="+PASSWORD)).build();status(c.send(r,HttpResponse.BodyHandlers.ofString()),200);}
    void status(HttpResponse<String> r,int expected){assertThat(r.statusCode()).withFailMessage("Esperado %s recibido %s: %s",expected,r.statusCode(),r.body()).isEqualTo(expected);}
}
