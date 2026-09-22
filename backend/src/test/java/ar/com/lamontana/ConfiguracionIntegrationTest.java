package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.configuracion.ConfiguracionController;
import ar.com.lamontana.configuracion.ConfiguracionService;
import ar.com.lamontana.identidad.IdentidadController;
import ar.com.lamontana.identidad.IdentidadService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ConfiguracionIntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String EMAIL="propietario-config@example.test",CLAVE="ConfiguracionPruebas123!",TOKEN="token-local-exclusivo-configuracion-prueba";
    private EmbeddedPostgres pg;
    private ConfigurableApplicationContext app;
    private JdbcTemplate jdbc;
    private Path archivos;
    private HttpClient admin;
    private int port;

    @BeforeEach void iniciar() throws Exception {
        archivos=Files.createTempDirectory("lamontana-configuracion-");pg=EmbeddedPostgres.builder().start();arrancar();
        app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Admin","Configuracion",EMAIL,CLAVE));
        admin=cliente();login(admin,EMAIL);
    }
    @AfterEach void cerrar() throws Exception { if(app!=null)app.close();if(pg!=null)pg.close();if(archivos!=null)Files.deleteIfExists(archivos); }
    private void arrancar() {
        app=new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),
                "--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+archivos,"--lamontana.instalacion.token="+TOKEN);
        jdbc=app.getBean(JdbcTemplate.class);port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));
    }

    @Test void borradorVacioSeleccionValidadaIdempotenciaPermisosYPersistencia() throws Exception {
        assertThat(estado().get("borrador").isNull()).isTrue();assertThat(contar("configuracion_version")).isZero();
        status(enviar(admin,"POST","/api/admin/configuracion/borradores",Map.of()),400);
        var crear=Map.of("operacion",UUID.randomUUID().toString());
        var request=request(admin,"POST","/api/admin/configuracion/borradores",crear);
        var a=admin.sendAsync(request,HttpResponse.BodyHandlers.ofString());var b=admin.sendAsync(request,HttpResponse.BodyHandlers.ofString());
        var ra=a.get(15,TimeUnit.SECONDS);var rb=b.get(15,TimeUnit.SECONDS);status(ra,200);status(rb,200);assertThat(ra.body()).isEqualTo(rb.body());
        var borrador=JSON.readTree(ra.body());String codigo=borrador.get("codigoPublico").asString(),ruta="/api/admin/configuracion/borradores/"+codigo+"/modelo";
        assertThat(borrador.get("numero").asLong()).isEqualTo(1);assertThat(borrador.get("version").asLong()).isEqualTo(1);
        assertThat(borrador.get("estado").asString()).isEqualTo("EN_PREPARACION");
        assertThat(borrador.get("modelo").isNull()).isTrue();assertThat(borrador.get("criterio").isNull()).isTrue();
        assertThat(borrador.get("creadaEn")).isEqualTo(borrador.get("actualizadaEn"));assertThat(borrador.get("actor").asString()).isEqualTo("Admin Configuracion");
        status(enviar(admin,"POST","/api/admin/configuracion/borradores",Map.of("operacion",UUID.randomUUID().toString())),409);

        var manual=seleccion(1,"MANUAL",null);
        var sinCsrf=HttpRequest.newBuilder(uri(ruta)).header("Content-Type","application/json").PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(manual))).build();
        status(admin.send(sinCsrf,HttpResponse.BodyHandlers.ofString()),403);
        var publico=cliente();status(get(publico,"/api/admin/configuracion"),401);
        status(enviar(publico,"POST","/api/auth/registro",Map.of("nombre","Cliente","apellido","Configuracion","correo","cliente-config@example.test","contrasena",CLAVE)),201);
        login(publico,"cliente-config@example.test");status(get(publico,"/api/admin/configuracion"),403);
        status(enviar(publico,"POST","/api/admin/configuracion/borradores",crear),403);status(enviar(publico,"PUT",ruta,manual),403);
        // El rol ADMIN_ADMIN tampoco habilita preparar políticas si no es el propietario.
        String otro="admin-sin-propiedad@example.test";
        jdbc.update("""
                INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado)
                SELECT ?,id_rol,?,hash_contrasena,'Otro','Administrador','ACTIVO' FROM lamontana.usuario WHERE correo=?
                """,UUID.randomUUID(),otro,EMAIL);
        var noPropietario=cliente();login(noPropietario,otro);status(get(noPropietario,"/api/admin/configuracion"),403);
        status(enviar(noPropietario,"POST","/api/admin/configuracion/borradores",crear),403);status(enviar(noPropietario,"PUT",ruta,manual),403);

        status(enviar(admin,"PUT",ruta,seleccion(1,"MANUAL","SENA")),400);
        status(enviar(admin,"PUT",ruta,seleccion(1,"CONDICIONAL",null)),400);
        status(enviar(admin,"PUT",ruta,seleccion(1,"OTRO",null)),400);
        status(enviar(admin,"PUT",ruta,seleccion(1,"CONDICIONAL","OTRO")),400);
        status(enviar(admin,"PUT",ruta,seleccion(0,"MANUAL",null)),400);
        var sinVersion=seleccion(1,"MANUAL",null);sinVersion.remove("version");status(enviar(admin,"PUT",ruta,sinVersion),400);
        assertThat(estado().get("borrador")).isEqualTo(borrador);assertThat(contar("comprobante_configuracion")).isEqualTo(1);

        var guardado=enviar(admin,"PUT",ruta,manual);status(guardado,200);var version2=JSON.readTree(guardado.body());
        assertThat(version2.get("version").asLong()).isEqualTo(2);assertThat(version2.get("modelo").asString()).isEqualTo("MANUAL");assertThat(version2.get("criterio").isNull()).isTrue();
        assertThat(JSON.readTree(enviar(admin,"PUT",ruta,manual).body())).isEqualTo(version2);
        var cambiado=new HashMap<>(manual);cambiado.put("modelo","CONDICIONAL");cambiado.put("criterio","PAGO_PREVIO");status(enviar(admin,"PUT",ruta,cambiado),409);
        status(enviar(admin,"PUT",ruta,seleccion(1,"MANUAL",null)),409);
        var otroTipo=seleccion(2,"MANUAL",null);otroTipo.put("operacion",crear.get("operacion"));status(enviar(admin,"PUT",ruta,otroTipo),409);
        status(enviar(admin,"PUT","/api/admin/configuracion/borradores/"+UUID.randomUUID()+"/modelo",manual),409);
        status(enviar(admin,"PUT","/api/admin/configuracion/borradores/"+UUID.randomUUID()+"/modelo",seleccion(1,"MANUAL",null)),404);
        status(enviar(admin,"POST","/api/admin/configuracion/borradores",Map.of("operacion",manual.get("operacion"))),409);
        long version=2;
        for(String criterio:List.of("PAGO_PREVIO","SENA","MONTO_TOTAL")) {
            var respuesta=enviar(admin,"PUT",ruta,seleccion(version,"CONDICIONAL",criterio));status(respuesta,200);borrador=JSON.readTree(respuesta.body());
            assertThat(borrador.get("version").asLong()).isEqualTo(++version);assertThat(borrador.get("criterio").asString()).isEqualTo(criterio);
        }
        // Repetir un comando anterior devuelve lo actual, sin retroceder la selección ni crear eventos.
        assertThat(JSON.readTree(enviar(admin,"PUT",ruta,manual).body())).isEqualTo(borrador);
        assertThat(JSON.readTree(enviar(admin,"POST","/api/admin/configuracion/borradores",crear).body())).isEqualTo(borrador);
        var finalManual=enviar(admin,"PUT",ruta,seleccion(version,"MANUAL",null));status(finalManual,200);borrador=JSON.readTree(finalManual.body());
        assertThat(borrador.get("version").asLong()).isEqualTo(6);assertThat(borrador.get("criterio").isNull()).isTrue();
        assertThat(borrador.get("estado").asString()).isEqualTo("EN_PREPARACION");
        assertThat(contar("configuracion_version")).isEqualTo(1);assertThat(contar("comprobante_configuracion")).isEqualTo(6);assertThat(contar("evento_configuracion")).isEqualTo(6);
        assertThat(contar("catalogo_revision")).isZero();assertThat(contar("tarifa_impresion")).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_configuracion e JOIN lamontana.usuario u ON u.id_usuario=e.id_actor WHERE u.correo=?",Integer.class,EMAIL)).isEqualTo(6);
        String antes=estado().toString();app.close();arrancar();assertThat(estado().toString()).isEqualTo(antes);
        assertThat(JSON.readTree(enviar(admin,"PUT",ruta,manual).body())).isEqualTo(borrador);
    }

    @Test void comandosDistintosConcurrentesYFallosAtomicosNoDuplicanNiConsumenComprobantes() throws Exception {
        var servicio=app.getBean(ConfiguracionService.class);
        var crear=new ConfiguracionController.CrearBorrador(UUID.randomUUID());fallarEventos();
        assertThatThrownBy(()->servicio.crear(crear,EMAIL)).isInstanceOf(DataAccessException.class);
        assertThat(contar("configuracion_version")).isZero();assertThat(contar("comprobante_configuracion")).isZero();quitarFalla();
        var comandoA=Map.of("operacion",crear.operacion().toString());var comandoB=Map.of("operacion",UUID.randomUUID().toString());
        var a=admin.sendAsync(request(admin,"POST","/api/admin/configuracion/borradores",comandoA),HttpResponse.BodyHandlers.ofString());
        var b=admin.sendAsync(request(admin,"POST","/api/admin/configuracion/borradores",comandoB),HttpResponse.BodyHandlers.ofString());
        assertThat(List.of(a.get(15,TimeUnit.SECONDS).statusCode(),b.get(15,TimeUnit.SECONDS).statusCode())).containsExactlyInAnyOrder(200,409);
        var borrador=estado().get("borrador");String codigo=borrador.get("codigoPublico").asString(),ruta="/api/admin/configuracion/borradores/"+codigo+"/modelo";
        assertThat(borrador.get("numero").asLong()).isEqualTo(1);assertThat(contar("comprobante_configuracion")).isEqualTo(1);
        var elegir=new ConfiguracionController.SeleccionarModelo(UUID.randomUUID(),1L,ConfiguracionController.Modelo.MANUAL,null);fallarEventos();
        assertThatThrownBy(()->servicio.seleccionar(UUID.fromString(codigo),elegir,EMAIL)).isInstanceOf(DataAccessException.class);
        assertThat(estado().get("borrador")).isEqualTo(borrador);assertThat(contar("comprobante_configuracion")).isEqualTo(1);quitarFalla();
        // El comando cuya transacción falló puede reintentarse con la misma identidad.
        var guardado=servicio.seleccionar(UUID.fromString(codigo),elegir,EMAIL);assertThat(guardado.version()).isEqualTo(2);
        assertThat(servicio.seleccionar(UUID.fromString(codigo),elegir,EMAIL)).isEqualTo(guardado);
        var requestA=request(admin,"PUT",ruta,seleccion(2,"CONDICIONAL","SENA"));
        var requestB=request(admin,"PUT",ruta,seleccion(2,"CONDICIONAL","MONTO_TOTAL"));
        a=admin.sendAsync(requestA,HttpResponse.BodyHandlers.ofString());b=admin.sendAsync(requestB,HttpResponse.BodyHandlers.ofString());
        assertThat(List.of(a.get(15,TimeUnit.SECONDS).statusCode(),b.get(15,TimeUnit.SECONDS).statusCode())).containsExactlyInAnyOrder(200,409);
        assertThat(estado().get("borrador").get("version").asLong()).isEqualTo(3);
        assertThat(contar("configuracion_version")).isEqualTo(1);assertThat(contar("comprobante_configuracion")).isEqualTo(3);assertThat(contar("evento_configuracion")).isEqualTo(3);
        assertThat(jdbc.queryForList("SELECT version FROM lamontana.evento_configuracion ORDER BY version",Long.class)).containsExactly(1L,2L,3L);
    }

    private void fallarEventos() {
        jdbc.execute("CREATE FUNCTION lamontana.fallar_configuracion_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fallo transaccional de prueba'; END $$");
        jdbc.execute("CREATE TRIGGER fallo_configuracion_test BEFORE INSERT ON lamontana.evento_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_configuracion_test()");
    }
    private void quitarFalla() { jdbc.execute("DROP TRIGGER fallo_configuracion_test ON lamontana.evento_configuracion");jdbc.execute("DROP FUNCTION lamontana.fallar_configuracion_test()"); }
    private Map<String,Object> seleccion(long version,String modelo,String criterio) {var data=new HashMap<String,Object>();data.put("operacion",UUID.randomUUID().toString());data.put("version",version);data.put("modelo",modelo);data.put("criterio",criterio);return data;}
    private int contar(String tabla) { return jdbc.queryForObject("SELECT count(*) FROM lamontana."+tabla,Integer.class); }
    private JsonNode estado() throws Exception {var r=get(admin,"/api/admin/configuracion");status(r,200);return JSON.readTree(r.body());}
    private HttpClient cliente() {return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build();}
    private URI uri(String path) {return URI.create("http://127.0.0.1:"+port+path);}
    private HttpResponse<String> get(HttpClient c,String path) throws Exception {return c.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private String csrf(HttpClient c) throws Exception {var r=get(c,"/api/auth/csrf");status(r,200);return JSON.readTree(r.body()).get("token").asString();}
    private HttpRequest request(HttpClient c,String metodo,String path,Object data) throws Exception {return HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf(c)).method(metodo,HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(data))).build();}
    private HttpResponse<String> enviar(HttpClient c,String metodo,String path,Object data) throws Exception {return c.send(request(c,metodo,path,data),HttpResponse.BodyHandlers.ofString());}
    private void login(HttpClient c,String correo) throws Exception {var r=HttpRequest.newBuilder(uri("/api/auth/login")).timeout(Duration.ofSeconds(15)).header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",csrf(c)).POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(correo,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(CLAVE,StandardCharsets.UTF_8))).build();status(c.send(r,HttpResponse.BodyHandlers.ofString()),200);}
    private void status(HttpResponse<String> r,int codigo) {assertThat(r.statusCode()).withFailMessage("Esperado %s, recibido %s: %s",codigo,r.statusCode(),r.body()).isEqualTo(codigo);}
}
