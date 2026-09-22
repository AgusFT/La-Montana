package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.catalogo.CatalogoController;
import ar.com.lamontana.catalogo.CatalogoService;
import ar.com.lamontana.identidad.IdentidadController;
import ar.com.lamontana.identidad.IdentidadService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class CatalogoProgramacionIntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String EMAIL="admin@example.test",CLAVE="ProgramacionCatalogo123!",TOKEN="token-exclusivo-local-programacion-catalogo";
    private EmbeddedPostgres pg;
    private ConfigurableApplicationContext app;
    private JdbcTemplate jdbc;
    private CatalogoService catalogo;
    private Path archivos;
    private HttpClient admin;
    private int port;
    private UUID formato,papel,impresion;

    @BeforeEach void iniciar() throws Exception {
        archivos=Files.createTempDirectory("lamontana-programacion-");
        pg=EmbeddedPostgres.builder().start(); arrancar(false);
        app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Admin","Programación",EMAIL,CLAVE));
        catalogo.formato(new CatalogoController.AltaFormato("F","Formato",new BigDecimal("210"),new BigDecimal("297")),EMAIL);
        catalogo.papel(new CatalogoController.AltaPapel("P","Papel",new BigDecimal("80"),"Mate"),EMAIL);
        catalogo.servicio(new CatalogoController.AltaServicio("IMP","Impresión",CatalogoController.TipoServicio.IMPRESION,null),EMAIL);
        var inicial=catalogo.estado(); formato=inicial.formatos().get(0).codigoPublico();papel=inicial.papeles().get(0).codigoPublico();impresion=inicial.servicios().get(0).codigoPublico();
        admin=cliente(); login(admin,EMAIL);
    }
    @AfterEach void cerrar() throws Exception {
        if(app!=null) app.close(); if(pg!=null) pg.close(); if(archivos!=null) Files.deleteIfExists(archivos);
    }
    private void arrancar(boolean scheduler) {
        app=new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0",
                "--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=",
                "--lamontana.archivos.directorio="+archivos,"--lamontana.instalacion.token="+TOKEN,
                "--lamontana.catalogo.reconciliacion-inicio-ms="+(scheduler?50:3600000),
                "--lamontana.catalogo.reconciliacion-intervalo-ms="+(scheduler?50:3600000));
        port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));jdbc=app.getBean(JdbcTemplate.class);catalogo=app.getBean(CatalogoService.class);
    }

    @Test void futuroConservaVigenteBloqueaOtraRevisionYCancelacionEsIdempotenteConcurrente() throws Exception {
        var inicial=guardar(revision(null,null,"10.00"));String base=inicial.get("codigoPublico").asString();
        var pasada=revision(base,Instant.now().minusSeconds(1),"11.00");status(post(admin,"/api/admin/catalogo/revisiones",pasada),400);
        assertThat(contar("catalogo_revision")).isEqualTo(1);
        var programacion=revision(base,Instant.now().plusSeconds(120),"20.00");
        var futura=guardar(programacion);String id=futura.get("codigoPublico").asString();
        assertThat(futura.get("estado").asString()).isEqualTo("PROGRAMADA");assertThat(futura.get("activadaEn").isNull()).isTrue();
        var estado=estado(); assertThat(estado.get("actual")).isEqualTo(inicial);assertThat(estado.get("programada")).isEqualTo(futura);
        assertThat(guardar(programacion)).isEqualTo(futura);
        var distinto=new HashMap<>(programacion);distinto.put("motivo","Otro contenido");status(post(admin,"/api/admin/catalogo/revisiones",distinto),409);
        status(post(admin,"/api/admin/catalogo/revisiones",revision(base,null,"30.00")),409);
        status(post(admin,"/api/admin/catalogo/revisiones",revision(base,Instant.now().plusSeconds(150),"30.00")),409);

        String cancelar="/api/admin/catalogo/programaciones/"+id+"/cancelar";
        var comando=Map.of("operacion",UUID.randomUUID().toString(),"motivo","Corregir la futura lista");
        var sinCsrf=HttpRequest.newBuilder(uri(cancelar)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(comando))).build();
        status(admin.send(sinCsrf,HttpResponse.BodyHandlers.ofString()),403);
        var publico=cliente(); status(get(publico,"/api/admin/catalogo"),401);
        status(post(publico,"/api/auth/registro",Map.of("nombre","Cliente","apellido","Prueba","correo","cliente@example.test","contrasena",CLAVE)),201);login(publico,"cliente@example.test");
        status(post(publico,cancelar,comando),403);
        var request=request(admin,cancelar,comando);
        var a=admin.sendAsync(request,HttpResponse.BodyHandlers.ofString());var b=admin.sendAsync(request,HttpResponse.BodyHandlers.ofString());
        var ra=a.get(15,TimeUnit.SECONDS);var rb=b.get(15,TimeUnit.SECONDS);status(ra,200);status(rb,200);assertThat(ra.body()).isEqualTo(rb.body());
        assertThat(JSON.readTree(ra.body()).get("estado").asString()).isEqualTo("CANCELADA");
        assertThat(contar("catalogo_cancelacion")).isEqualTo(1);assertThat(eventos("PROGRAMACION_CANCELADA")).isEqualTo(1);
        estado=estado();assertThat(estado.get("actual")).isEqualTo(inicial);assertThat(estado.get("programada").isNull()).isTrue();
        var alterado=new HashMap<>(comando);alterado.put("motivo","Motivo diferente");status(post(admin,cancelar,alterado),409);
        status(post(admin,cancelar,Map.of("operacion",UUID.randomUUID().toString(),"motivo","Otra cancelación")),409);
        // La misma identidad de comando no puede reutilizarse para otro destino ni para un guardado.
        status(post(admin,"/api/admin/catalogo/programaciones/"+base+"/cancelar",comando),409);
        var reutilizada=revision(base,null,"40.00");reutilizada.put("operacion",comando.get("operacion"));status(post(admin,"/api/admin/catalogo/revisiones",reutilizada),409);
        // Idempotencia vinculada también al actor.
        var body=JSON.readValue(JSON.writeValueAsString(comando),CatalogoController.CancelarProgramacion.class);
        assertThatThrownBy(()->catalogo.cancelar(UUID.fromString(id),body,"otro-admin@example.test"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var posterior=guardar(revision(base,null,"30.00"));assertThat(posterior.get("estado").asString()).isEqualTo("VIGENTE");
        var cancelada=JSON.readTree(get(admin,"/api/admin/catalogo/revisiones/"+id).body());
        assertThat(cancelada.get("estado").asString()).isEqualTo("CANCELADA");assertSnapshots(futura,cancelada);
    }

    @Test void programacionPersisteYSeActivaPorSchedulerTrasReiniciarSinConsultas() throws Exception {
        var primera=guardar(revision(null,null,"10.00"));
        var programacion=revision(primera.get("codigoPublico").asString(),Instant.now().plusSeconds(2),"25.00");
        var futura=guardar(programacion);UUID id=UUID.fromString(futura.get("codigoPublico").asString());
        app.close(); arrancar(true);
        // No se consulta estado: sólo el scheduler puede producir esta transición.
        esperar(()->"VIGENTE".equals(estadoDb(id)),Duration.ofSeconds(10));
        assertThat(eventos("PROGRAMACION_APLICADA")).isEqualTo(1);
        var activa=estado().get("actual");assertThat(activa.get("codigoPublico").asString()).isEqualTo(id.toString());
        assertThat(activa.get("activadaEn").isNull()).isFalse();assertSnapshots(futura,activa);
        assertThat(Instant.parse(activa.get("activadaEn").asString())).isAfterOrEqualTo(Instant.parse(activa.get("programadaPara").asString()));
        assertThat(estado().get("programada").isNull()).isTrue();
        // Repetir el guardado original después del vencimiento devuelve su resultado actual, no falla por fecha pasada.
        assertThat(guardar(programacion).get("codigoPublico")).isEqualTo(activa.get("codigoPublico"));
        catalogo.reconciliarProgramaciones();assertThat(eventos("PROGRAMACION_APLICADA")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT u.correo FROM lamontana.evento_catalogo e JOIN lamontana.usuario u ON u.id_usuario=e.id_actor WHERE e.tipo='PROGRAMACION_APLICADA'",String.class)).isEqualTo(EMAIL);
    }

    @Test void consultasYComandosReconcilianVencidasAunqueLuegoRespondanConflicto() throws Exception {
        var inicial=guardar(revision(null,null,"10.00"));String base=inicial.get("codigoPublico").asString();
        var futura=guardar(revision(base,Instant.now().plusSeconds(1),"20.00"));UUID id=UUID.fromString(futura.get("codigoPublico").asString());
        esperarVencimiento(id);
        status(post(admin,"/api/admin/catalogo/programaciones/"+id+"/cancelar",Map.of("operacion",UUID.randomUUID().toString(),"motivo","Demasiado tarde")),409);
        assertThat(estadoDb(id)).isEqualTo("VIGENTE");assertThat(contar("catalogo_cancelacion")).isZero();
        var segunda=guardar(revision(id.toString(),Instant.now().plusSeconds(1),"30.00"));UUID id2=UUID.fromString(segunda.get("codigoPublico").asString());
        esperarVencimiento(id2);
        status(post(admin,"/api/admin/catalogo/revisiones",revision(id.toString(),null,"40.00")),409);
        assertThat(estadoDb(id2)).isEqualTo("VIGENTE");
        var tercera=guardar(revision(id2.toString(),Instant.now().plusSeconds(1),"40.00"));UUID id3=UUID.fromString(tercera.get("codigoPublico").asString());
        esperarVencimiento(id3);
        assertThat(estado().get("actual").get("codigoPublico").asString()).isEqualTo(id3.toString());
        assertThat(eventos("PROGRAMACION_APLICADA")).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.catalogo_revision WHERE vigente",Integer.class)).isEqualTo(1);
    }

    @Test void fallaAlActivarNoPublicaParcialNiPierdePendienteYReintentosConcurrentesAplicanUnaVez() throws Exception {
        var inicial=guardar(revision(null,null,"10.00"));String base=inicial.get("codigoPublico").asString();
        var futura=guardar(revision(base,Instant.now().plusSeconds(1),"50.00"));UUID id=UUID.fromString(futura.get("codigoPublico").asString());
        esperarVencimiento(id);fallarEvento("PROGRAMACION_APLICADA");
        assertThatThrownBy(()->catalogo.reconciliarProgramaciones()).isInstanceOf(DataAccessException.class);
        assertThat(estadoDb(id)).isEqualTo("PROGRAMADA");assertThat(estadoDb(UUID.fromString(base))).isEqualTo("VIGENTE");
        assertThat(jdbc.queryForObject("SELECT activada_en IS NULL FROM lamontana.catalogo_revision WHERE codigo_publico=?",Boolean.class,id)).isTrue();
        assertThat(eventos("PROGRAMACION_APLICADA")).isZero();quitarFalla();
        var a=CompletableFuture.runAsync(()->catalogo.reconciliarProgramaciones());var b=CompletableFuture.runAsync(()->catalogo.reconciliarProgramaciones());
        CompletableFuture.allOf(a,b).get(15,TimeUnit.SECONDS);
        assertThat(estadoDb(id)).isEqualTo("VIGENTE");assertThat(estadoDb(UUID.fromString(base))).isEqualTo("HISTORICA");
        assertThat(eventos("PROGRAMACION_APLICADA")).isEqualTo(1);assertThat(contar("tarifa_impresion")).isEqualTo(2);
        assertSnapshots(futura,estado().get("actual"));
    }

    @Test void cancelacionFallidaRevierteComprobanteYEstadoYPermiteReintentarMismoComando() throws Exception {
        // También se permite programar la primera revisión; no se inventa una vigente de respaldo.
        var futura=guardar(revision(null,Instant.now().plusSeconds(120),"12.00"));UUID id=UUID.fromString(futura.get("codigoPublico").asString());
        assertThat(estado().get("actual").isNull()).isTrue();
        var input=new CatalogoController.CancelarProgramacion(UUID.randomUUID(),"Corregir antes de operar");fallarEvento("PROGRAMACION_CANCELADA");
        assertThatThrownBy(()->catalogo.cancelar(id,input,EMAIL)).isInstanceOf(DataAccessException.class);
        assertThat(estadoDb(id)).isEqualTo("PROGRAMADA");assertThat(contar("catalogo_cancelacion")).isZero();quitarFalla();
        assertThat(catalogo.cancelar(id,input,EMAIL).estado()).isEqualTo(CatalogoService.EstadoRevision.CANCELADA);
        assertThat(catalogo.cancelar(id,input,EMAIL).estado()).isEqualTo(CatalogoService.EstadoRevision.CANCELADA);
        assertThat(eventos("PROGRAMACION_CANCELADA")).isEqualTo(1);assertThat(contar("catalogo_cancelacion")).isEqualTo(1);
        assertThat(estado().get("actual").isNull()).isTrue();
    }

    private Map<String,Object> revision(String base,Instant fecha,String precio) {
        var data=new LinkedHashMap<String,Object>();data.put("versionBase",base);data.put("operacion",UUID.randomUUID().toString());data.put("motivo","Lista comercial "+precio);
        data.put("tarifas",List.of(Map.of("formato",formato,"papel",papel,"color","BLANCO_NEGRO","precio",precio,"recargoDobleFaz","0.00","habilitada",true)));
        data.put("servicios",List.of(Map.of("servicio",impresion,"nombreVisible","Impresión","basePrecio","POR_CARILLA","precio","0.00","preparacionMinutos",0,"habilitado",true,"compatibilidades",List.of())));
        data.put("programadaPara",fecha==null?null:fecha.toString());return data;
    }
    private JsonNode guardar(Map<String,Object> data) throws Exception {var r=post(admin,"/api/admin/catalogo/revisiones",data);status(r,200);return JSON.readTree(r.body());}
    private JsonNode estado() throws Exception {var r=get(admin,"/api/admin/catalogo");status(r,200);return JSON.readTree(r.body());}
    private String estadoDb(UUID id) {return jdbc.queryForObject("SELECT estado FROM lamontana.catalogo_revision WHERE codigo_publico=?",String.class,id);}
    private void esperarVencimiento(UUID id) throws Exception {esperar(()->Boolean.TRUE.equals(jdbc.queryForObject("SELECT programada_para<=clock_timestamp() FROM lamontana.catalogo_revision WHERE codigo_publico=?",Boolean.class,id)),Duration.ofSeconds(5));}
    private void esperar(BooleanSupplier condicion,Duration limite) throws Exception {long hasta=System.nanoTime()+limite.toNanos();while(!condicion.getAsBoolean()&&System.nanoTime()<hasta)Thread.sleep(30);assertThat(condicion.getAsBoolean()).as("Condición eventual dentro del plazo").isTrue();}
    private int contar(String tabla) {return jdbc.queryForObject("SELECT count(*) FROM lamontana."+tabla,Integer.class);}
    private int eventos(String tipo) {return jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_catalogo WHERE tipo=?",Integer.class,tipo);}
    private void assertSnapshots(JsonNode antes,JsonNode despues) {for(String campo:List.of("codigoPublico","numero","motivo","creadaEn","actor","tarifas","servicios","programadaPara"))assertThat(despues.get(campo)).isEqualTo(antes.get(campo));}
    private void fallarEvento(String tipo) {
        jdbc.execute("CREATE FUNCTION lamontana.fallar_programacion_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.tipo='"+tipo+"' THEN RAISE EXCEPTION 'fallo transaccional de prueba'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER fallo_programacion_test BEFORE INSERT ON lamontana.evento_catalogo FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_programacion_test()");
    }
    private void quitarFalla() {jdbc.execute("DROP TRIGGER fallo_programacion_test ON lamontana.evento_catalogo");jdbc.execute("DROP FUNCTION lamontana.fallar_programacion_test()");}
    private HttpClient cliente() {return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build();}
    private URI uri(String path) {return URI.create("http://127.0.0.1:"+port+path);}
    private HttpResponse<String> get(HttpClient c,String path) throws Exception {return c.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private HttpRequest request(HttpClient c,String path,Object data) throws Exception {var csrf=get(c,"/api/auth/csrf");status(csrf,200);return HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("X-CSRF-TOKEN",JSON.readTree(csrf.body()).get("token").asString()).POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(data))).build();}
    private HttpResponse<String> post(HttpClient c,String path,Object data) throws Exception {return c.send(request(c,path,data),HttpResponse.BodyHandlers.ofString());}
    private void login(HttpClient c,String mail) throws Exception {var csrf=get(c,"/api/auth/csrf");var request=HttpRequest.newBuilder(uri("/api/auth/login")).timeout(Duration.ofSeconds(15)).header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",JSON.readTree(csrf.body()).get("token").asString()).POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(mail,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(CLAVE,StandardCharsets.UTF_8))).build();status(c.send(request,HttpResponse.BodyHandlers.ofString()),200);}
    private void status(HttpResponse<String> response,int codigo) {assertThat(response.statusCode()).withFailMessage("Esperado %s, recibido %s: %s",codigo,response.statusCode(),response.body()).isEqualTo(codigo);}
}
