package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.configuracion.CancelacionConfiguracionService;
import ar.com.lamontana.configuracion.ConfiguracionController;
import ar.com.lamontana.identidad.CredencialService;
import ar.com.lamontana.identidad.IdentidadController;
import ar.com.lamontana.identidad.IdentidadService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ConfiguracionCancelacionIntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String EMAIL="cancelacion@example.test",CLAVE="ClaveCancelacion123!",NUEVA="ClaveCancelacionNueva456!",TOKEN="token-local-cancelacion-exclusivo-test";
    private EmbeddedPostgres pg;
    private ConfigurableApplicationContext app;
    private CapturaSmtp smtp;
    private JdbcTemplate jdbc;
    private Path archivos;
    private HttpClient admin;
    private int port;

    @BeforeEach void iniciar() throws Exception {
        archivos=Files.createTempDirectory("lamontana-cancelacion-");smtp=new CapturaSmtp();pg=EmbeddedPostgres.builder().start();arrancar();
        app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Admin","Cancelacion",EMAIL,CLAVE));
        admin=cliente();login(admin,EMAIL,CLAVE);
    }
    @AfterEach void cerrar() throws Exception {if(app!=null)app.close();if(pg!=null)pg.close();if(smtp!=null)smtp.close();if(archivos!=null)Files.deleteIfExists(archivos);}
    private void arrancar() {
        app=new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),
                "--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+archivos,"--lamontana.instalacion.token="+TOKEN,
                "--spring.mail.host=127.0.0.1","--spring.mail.port="+smtp.port(),"--lamontana.correo.remitente=noreply@example.test");
        port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));jdbc=app.getBean(JdbcTemplate.class);
    }

    @Test void correoRealCancelacionConcurrenteSnapshotHistorialReinicioYReplaySinAfectarNuevoBorrador() throws Exception {
        var borrador=crear();String id=borrador.get("codigoPublico").asString();
        var editar=enviar(admin,"PUT","/api/admin/configuracion/borradores/"+id+"/modelo",Map.of("operacion",UUID.randomUUID().toString(),"version",1,"modelo","CONDICIONAL","criterio","MONTO_TOTAL"));
        status(editar,200);borrador=JSON.readTree(editar.body());
        var sucursal=post(admin,"/api/admin/sucursales",Map.of("codigo","A","nombre","Sucursal A","calle","Calle","numero","123","localidad","Ciudad","provincia","Provincia","codigoPostal","1234","zonaHoraria","America/Argentina/Buenos_Aires"));status(sucursal,201);
        status(post(admin,"/api/admin/catalogo/formatos",Map.of("codigo","A4","nombre","A4","anchoMm",210,"altoMm",297)),201);
        String formato=jdbc.queryForObject("SELECT codigo_publico::text FROM lamontana.formato WHERE codigo='A4'",String.class);
        var recurso=post(admin,"/api/admin/configuracion/borradores/"+id+"/impresoras",Map.of("operacion",UUID.randomUUID().toString(),"version",2,"sucursal",JSON.readTree(sucursal.body()).get("codigoPublico").asString(),"nombre","Equipo conservado","formatos",List.of(formato),"admiteColor",false,"admiteDobleFaz",false,"capacidadHojas",100,"estado","DESHABILITADA"));
        status(recurso,200);borrador=JSON.readTree(recurso.body());
        var solicitud=solicitud(3,CLAVE);var emitida=post(admin,ruta(id,"solicitar"),solicitud);status(emitida,200);
        var mensaje=smtp.recibir();String codigo=mensaje.token();
        assertThat(mensaje.destino()).isEqualTo(EMAIL);assertThat(mensaje.remitente()).isEqualTo("noreply@example.test");
        assertThat(mensaje.asunto()).isEqualTo("La Montaña · Cancelar borrador de configuración");
        assertThat(emitida.body()).doesNotContain(codigo,CLAVE,"hash_codigo","contrasena");
        assertThat(JSON.readTree(emitida.body()).get("operacion").asString()).isEqualTo(solicitud.get("operacion"));
        var hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.UTF_8)));
        assertThat(jdbc.queryForObject("SELECT hash_codigo FROM lamontana.autorizacion_configuracion",String.class)).isEqualTo(hash);
        assertThat(jdbc.queryForObject("SELECT row_to_json(a)::text FROM lamontana.autorizacion_configuracion a",String.class)).doesNotContain(codigo,CLAVE,"contrasena");
        assertThat(jdbc.queryForObject("SELECT proposito FROM lamontana.autorizacion_configuracion",String.class)).isEqualTo("CANCELAR_BORRADOR");
        assertThat(post(admin,ruta(id,"solicitar"),solicitud).body()).isEqualTo(emitida.body());assertThat(smtp.pendientes()).isZero();
        var distinta=new HashMap<>(solicitud);distinta.put("motivo","Otro motivo");status(post(admin,ruta(id,"solicitar"),distinta),409);
        status(post(admin,ruta(UUID.randomUUID().toString(),"solicitar"),solicitud),409);
        var confirmar=confirmacion(solicitud,codigo);
        var req=request(admin,"POST",ruta(id,"confirmar"),confirmar);
        var a=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());var b=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());
        var ra=a.get(20,TimeUnit.SECONDS);var rb=b.get(20,TimeUnit.SECONDS);status(ra,200);status(rb,200);assertThat(ra.body()).isEqualTo(rb.body());
        var cancelada=JSON.readTree(ra.body());assertThat(cancelada.get("estado").asString()).isEqualTo("CANCELADA");
        for(String campo:List.of("codigoPublico","numero","modelo","criterio","creadaEn","actor","recursos"))assertThat(cancelada.get(campo)).isEqualTo(borrador.get(campo));
        assertThat(cancelada.get("version").asLong()).isEqualTo(4);assertThat(cancelada.get("canceladaEn").isNull()).isFalse();
        assertThat(cancelada.get("motivoCancelacion").asString()).isEqualTo("Descartar este borrador");assertThat(cancelada.get("cancelador").asString()).isEqualTo("Admin Cancelacion");
        assertThat(estado().get("borrador").isNull()).isTrue();assertThat(estado().get("historial").get(0)).isEqualTo(cancelada);
        assertThat(eventos("BORRADOR_CANCELADO")).isEqualTo(1);assertThat(contar("comprobante_configuracion")).isEqualTo(4);
        app.close();arrancar();assertThat(estado().get("historial").get(0)).isEqualTo(cancelada);
        var nuevo=crear();assertThat(nuevo.get("numero").asLong()).isEqualTo(2);assertThat(nuevo.get("modelo").isNull()).isTrue();
        assertThat(nuevo.get("recursos").get("metodoAsignacion").isNull()).isTrue();assertThat(nuevo.get("recursos").get("impresoras").size()).isZero();assertThat(nuevo.get("recursos").get("serviciosPorSucursal").size()).isZero();
        var modificarCancelada=enviar(admin,"PUT","/api/admin/configuracion/borradores/"+id+"/recursos",Map.of("operacion",UUID.randomUUID().toString(),"version",4,"metodoAsignacion","MANUAL","serviciosPorSucursal",List.of()));status(modificarCancelada,409);
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_emision=clock_timestamp()-interval '20 minutes',fecha_vencimiento=clock_timestamp()-interval '1 minute'");
        assertThat(post(admin,ruta(id,"confirmar"),confirmar).body()).isEqualTo(ra.body());
        assertThat(estado().get("borrador")).isEqualTo(nuevo);assertThat(eventos("BORRADOR_CANCELADO")).isEqualTo(1);
        var nuevoComando=new HashMap<>(confirmar);nuevoComando.put("operacion",UUID.randomUUID().toString());status(post(admin,ruta(id,"confirmar"),nuevoComando),400);
        status(post(admin,ruta(nuevo.get("codigoPublico").asString(),"confirmar"),confirmar),409);
    }

    @Test void contrasenaObligatoriaCincoIntentosPersistenCooldownExpiracionYReenvio() throws Exception {
        String id=crear().get("codigoPublico").asString();var solicitud=solicitud(1,CLAVE);var mala=new HashMap<>(solicitud);mala.put("contrasena","incorrecta");
        status(post(admin,ruta(id,"solicitar"),mala),400);assertThat(contar("autorizacion_configuracion")).isZero();assertThat(smtp.pendientes()).isZero();
        status(post(admin,ruta(id,"solicitar"),solicitud),200);String primero=smtp.recibir().token();
        var sinClave=confirmacion(solicitud,primero);sinClave.put("contrasena","incorrecta");status(post(admin,ruta(id,"confirmar"),sinClave),400);assertThat(intentos()).isEqualTo(1);
        for(int n=2;n<=5;n++){status(post(admin,ruta(id,"confirmar"),confirmacion(solicitud,"incorrecto-"+n)),400);assertThat(intentos()).isEqualTo(n);}
        status(post(admin,ruta(id,"confirmar"),confirmacion(solicitud,primero)),400);assertThat(intentos()).isEqualTo(5);
        var siguiente=solicitud(1,CLAVE);status(post(admin,ruta(id,"solicitar"),siguiente),429);assertThat(smtp.pendientes()).isZero();
        envejecer();status(post(admin,ruta(id,"solicitar"),siguiente),200);String segundo=smtp.recibir().token();assertThat(segundo).isNotEqualTo(primero);
        status(post(admin,ruta(id,"confirmar"),confirmacion(siguiente,primero)),400);assertThat(intentos()).isEqualTo(1);
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_emision=clock_timestamp()-interval '20 minutes',fecha_vencimiento=clock_timestamp()-interval '1 minute' WHERE fecha_revocacion IS NULL");
        status(post(admin,ruta(id,"confirmar"),confirmacion(siguiente,segundo)),400);
        status(post(admin,ruta(id,"solicitar"),siguiente),409);
        var finalSolicitud=solicitud(1,CLAVE);status(post(admin,ruta(id,"solicitar"),finalSolicitud),200);String tercero=smtp.recibir().token();
        status(post(admin,ruta(id,"confirmar"),confirmacion(finalSolicitud,tercero)),200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.autorizacion_configuracion WHERE fecha_revocacion IS NOT NULL",Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.autorizacion_configuracion WHERE fecha_consumo IS NOT NULL",Integer.class)).isEqualTo(1);
    }

    @Test void cambiosDeVersionContrasenaYCorreoInvalidanLaAutorizacionPendiente() throws Exception {
        String id=crear().get("codigoPublico").asString();var inicial=solicitud(1,CLAVE);status(post(admin,ruta(id,"solicitar"),inicial),200);String token=smtp.recibir().token();
        status(enviar(admin,"PUT","/api/admin/configuracion/borradores/"+id+"/modelo",seleccionar(1)),200);
        status(post(admin,ruta(id,"confirmar"),confirmacion(inicial,token)),409);
        envejecer();var segunda=solicitud(2,CLAVE);status(post(admin,ruta(id,"solicitar"),segunda),200);token=smtp.recibir().token();
        app.getBean(CredencialService.class).cambiarClave(EMAIL,CLAVE,NUEVA);admin=cliente();login(admin,EMAIL,NUEVA);
        var nuevaClave=confirmacion(segunda,token);nuevaClave.put("contrasena",NUEVA);status(post(admin,ruta(id,"confirmar"),nuevaClave),409);
        envejecer();var tercera=solicitud(2,NUEVA);status(post(admin,ruta(id,"solicitar"),tercera),200);token=smtp.recibir().token();
        String cambiado="correo-cambiado@example.test";jdbc.update("UPDATE lamontana.usuario SET correo=? WHERE correo=?",cambiado,EMAIL);
        admin=cliente();login(admin,cambiado,NUEVA);status(post(admin,ruta(id,"confirmar"),confirmacion(tercera,token)),409);
        assertThat(estado().get("borrador").get("version").asLong()).isEqualTo(2);assertThat(eventos("BORRADOR_CANCELADO")).isZero();
    }

    @Test void smtpYFalloSqlRevierteEmisionCancelacionConsumoComprobanteYAuditoria() throws Exception {
        String id=crear().get("codigoPublico").asString();var primera=solicitud(1,CLAVE);status(post(admin,ruta(id,"solicitar"),primera),200);smtp.recibir();envejecer();
        var segunda=solicitud(1,CLAVE);smtp.rechazar=true;status(post(admin,ruta(id,"solicitar"),segunda),503);
        assertThat(contar("autorizacion_configuracion")).isEqualTo(1);assertThat(eventos("CANCELACION_SOLICITADA")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT fecha_revocacion IS NULL FROM lamontana.autorizacion_configuracion",Boolean.class)).isTrue();
        smtp.rechazar=false;status(post(admin,ruta(id,"solicitar"),segunda),200);String token=smtp.recibir().token();
        var confirmar=confirmacion(segunda,token);var tipada=JSON.readValue(JSON.writeValueAsString(confirmar),ConfiguracionController.ConfirmarCancelacion.class);
        jdbc.execute("CREATE FUNCTION lamontana.fallar_cancelacion_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.tipo='BORRADOR_CANCELADO' THEN RAISE EXCEPTION 'fallo transaccional de prueba'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER fallo_cancelacion_test BEFORE INSERT ON lamontana.evento_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_cancelacion_test()");
        assertThatThrownBy(()->app.getBean(CancelacionConfiguracionService.class).confirmar(UUID.fromString(id),tipada,EMAIL)).isInstanceOf(DataAccessException.class);
        assertThat(estado().get("borrador").get("version").asLong()).isEqualTo(1);assertThat(estado().get("historial").size()).isZero();
        assertThat(contar("comprobante_configuracion")).isEqualTo(1);assertThat(eventos("BORRADOR_CANCELADO")).isZero();
        assertThat(jdbc.queryForObject("SELECT fecha_consumo IS NULL FROM lamontana.autorizacion_configuracion WHERE id_operacion=?",Boolean.class,UUID.fromString(segunda.get("operacion").toString()))).isTrue();
        jdbc.execute("DROP TRIGGER fallo_cancelacion_test ON lamontana.evento_configuracion");jdbc.execute("DROP FUNCTION lamontana.fallar_cancelacion_test()");
        status(post(admin,ruta(id,"confirmar"),confirmar),200);status(post(admin,ruta(id,"confirmar"),confirmar),200);
        assertThat(eventos("BORRADOR_CANCELADO")).isEqualTo(1);assertThat(contar("comprobante_configuracion")).isEqualTo(2);
    }

    @Test void permisosCsrfPropositosYActorImpidenUsarAutorizacionesAjenas() throws Exception {
        String id=crear().get("codigoPublico").asString();var solicitud=solicitud(1,CLAVE);
        var anonimo=cliente();status(post(anonimo,ruta(id,"solicitar"),solicitud),401);
        status(post(anonimo,"/api/auth/registro",Map.of("nombre","Cliente","apellido","Test","correo","cliente@example.test","contrasena",CLAVE)),201);
        login(anonimo,"cliente@example.test",CLAVE);status(post(anonimo,ruta(id,"solicitar"),solicitud),403);
        var sinCsrf=HttpRequest.newBuilder(uri(ruta(id,"solicitar"))).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(solicitud))).build();
        status(admin.send(sinCsrf,HttpResponse.BodyHandlers.ofString()),403);
        status(post(admin,"/api/auth/correo/solicitar",Map.of()),200);String verificacion=smtp.recibir().token();
        status(post(cliente(),"/api/auth/recuperacion/solicitar",Map.of("correo",EMAIL)),200);String recuperacion=smtp.recibir().token();
        status(post(admin,ruta(id,"solicitar"),solicitud),200);String cancelacion=smtp.recibir().token();
        status(post(admin,ruta(id,"confirmar"),confirmacion(solicitud,verificacion)),400);
        status(post(admin,ruta(id,"confirmar"),confirmacion(solicitud,recuperacion)),400);
        status(post(admin,"/api/auth/correo/confirmar",Map.of("token",cancelacion)),400);
        status(post(cliente(),"/api/auth/recuperacion/confirmar",Map.of("correo",EMAIL,"token",cancelacion,"nuevaContrasena",NUEVA)),400);
        var confirmar=confirmacion(solicitud,cancelacion);status(post(anonimo,ruta(id,"confirmar"),confirmar),403);
        var sinCsrfConfirm=HttpRequest.newBuilder(uri(ruta(id,"confirmar"))).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(confirmar))).build();
        status(admin.send(sinCsrfConfirm,HttpResponse.BodyHandlers.ofString()),403);
        status(post(admin,ruta(id,"confirmar"),confirmar),200);
        String otro="otro-admin@example.test";jdbc.update("""
                INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado)
                SELECT ?,id_rol,?,hash_contrasena,'Otro','Admin','ACTIVO' FROM lamontana.usuario WHERE correo=?
                """,UUID.randomUUID(),otro,EMAIL);
        var otroAdmin=cliente();login(otroAdmin,otro,CLAVE);status(post(otroAdmin,ruta(id,"confirmar"),confirmar),403);
        // Fixture de cambio de propietario: el comprobante continúa ligado al actor original.
        jdbc.update("UPDATE lamontana.usuario SET es_administrador_propietario=false WHERE correo=?",EMAIL);
        jdbc.update("UPDATE lamontana.usuario SET es_administrador_propietario=true WHERE correo=?",otro);
        status(post(otroAdmin,ruta(id,"confirmar"),confirmar),409);assertThat(eventos("BORRADOR_CANCELADO")).isEqualTo(1);
    }

    private Map<String,Object> solicitud(long version,String contrasena){return new HashMap<>(Map.of("operacion",UUID.randomUUID().toString(),"version",version,"motivo","Descartar este borrador","contrasena",contrasena));}
    private Map<String,Object> confirmacion(Map<String,Object> solicitud,String codigo){var copia=new HashMap<>(solicitud);copia.put("codigo",codigo);return copia;}
    private Map<String,Object> seleccionar(long version){var datos=new HashMap<String,Object>();datos.put("operacion",UUID.randomUUID().toString());datos.put("version",version);datos.put("modelo","MANUAL");datos.put("criterio",null);return datos;}
    private String ruta(String id,String accion){return "/api/admin/configuracion/borradores/"+id+"/cancelacion/"+accion;}
    private JsonNode crear()throws Exception{var r=post(admin,"/api/admin/configuracion/borradores",Map.of("operacion",UUID.randomUUID().toString()));status(r,200);return JSON.readTree(r.body());}
    private JsonNode estado()throws Exception{var r=get(admin,"/api/admin/configuracion");status(r,200);return JSON.readTree(r.body());}
    private int contar(String tabla){return jdbc.queryForObject("SELECT count(*) FROM lamontana."+tabla,Integer.class);}
    private int eventos(String tipo){return jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_configuracion WHERE tipo=?",Integer.class,tipo);}
    private int intentos(){return jdbc.queryForObject("SELECT intentos FROM lamontana.autorizacion_configuracion WHERE fecha_revocacion IS NULL ORDER BY fecha_emision DESC LIMIT 1",Integer.class);}
    private void envejecer(){jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_emision=clock_timestamp()-interval '61 seconds'");}
    private HttpClient cliente(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build();}
    private URI uri(String path){return URI.create("http://127.0.0.1:"+port+path);}
    private HttpResponse<String> get(HttpClient c,String path)throws Exception{return c.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private String csrf(HttpClient c)throws Exception{var r=get(c,"/api/auth/csrf");status(r,200);return JSON.readTree(r.body()).get("token").asString();}
    private HttpRequest request(HttpClient c,String metodo,String path,Object data)throws Exception{return HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf(c)).method(metodo,HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(data))).build();}
    private HttpResponse<String> enviar(HttpClient c,String metodo,String path,Object data)throws Exception{return c.send(request(c,metodo,path,data),HttpResponse.BodyHandlers.ofString());}
    private HttpResponse<String> post(HttpClient c,String path,Object data)throws Exception{return enviar(c,"POST",path,data);}
    private void login(HttpClient c,String correo,String clave)throws Exception{var req=HttpRequest.newBuilder(uri("/api/auth/login")).timeout(Duration.ofSeconds(15)).header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",csrf(c)).POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(correo,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(clave,StandardCharsets.UTF_8))).build();status(c.send(req,HttpResponse.BodyHandlers.ofString()),200);}
    private void status(HttpResponse<String> r,int codigo){assertThat(r.statusCode()).withFailMessage("Esperado %s, recibido %s: %s",codigo,r.statusCode(),r.body()).isEqualTo(codigo);}

    /** Servidor SMTP mínimo que recibe MIME auténtico de JavaMail y puede rechazar RCPT. */
    private static final class CapturaSmtp implements AutoCloseable {
        private final ServerSocket servidor = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
        private final BlockingQueue<Mensaje> mensajes = new LinkedBlockingQueue<>();
        private final ExecutorService hilo = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r, "smtp-correo-test"); t.setDaemon(true); return t; });
        private volatile boolean rechazar;
        private volatile Exception fallo;
        CapturaSmtp() throws IOException { hilo.submit(this::aceptar); }
        int port() { return servidor.getLocalPort(); }
        int pendientes() { return mensajes.size(); }
        Mensaje recibir() throws Exception {
            var mensaje = mensajes.poll(5, TimeUnit.SECONDS);
            if (fallo != null) throw new AssertionError("Falló el servidor SMTP de prueba", fallo);
            assertThat(mensaje).as("El correo debe recibirse realmente por SMTP").isNotNull();
            return mensaje;
        }
        private void aceptar() {
            while (!servidor.isClosed()) {
                try (var socket = servidor.accept()) {
                    socket.setSoTimeout(5000);
                    var lector = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    var escritor = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                    responder(escritor, "220 localhost ESMTP test");
                    for (String linea; (linea = lector.readLine()) != null;) {
                        String comando = linea.toUpperCase(Locale.ROOT);
                        if (comando.startsWith("EHLO") || comando.startsWith("HELO")) responder(escritor, "250 localhost");
                        else if (comando.startsWith("MAIL FROM:")) responder(escritor, "250 Sender OK");
                        else if (comando.startsWith("RCPT TO:")) responder(escritor, rechazar ? "451 Temporary SMTP failure" : "250 Recipient OK");
                        else if (comando.equals("DATA")) {
                            responder(escritor, "354 End data with <CRLF>.<CRLF>");
                            var datos = new StringBuilder();
                            while ((linea = lector.readLine()) != null && !linea.equals(".")) datos.append(linea.startsWith("..") ? linea.substring(1) : linea).append("\r\n");
                            var mime = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(datos.toString().getBytes(StandardCharsets.UTF_8)));
                            mensajes.add(new Mensaje(mime.getAllRecipients()[0].toString(), mime.getFrom()[0].toString(), mime.getSubject(), mime.getContent().toString()));
                            responder(escritor, "250 Queued");
                        } else if (comando.equals("QUIT")) { responder(escritor, "221 Bye"); break; }
                        else if (comando.equals("RSET") || comando.equals("NOOP")) responder(escritor, "250 OK");
                        else responder(escritor, "502 Command not implemented");
                    }
                } catch (Exception ex) { if (!servidor.isClosed()) fallo = ex; }
            }
        }
        private void responder(BufferedWriter escritor, String respuesta) throws IOException { escritor.write(respuesta + "\r\n"); escritor.flush(); }
        @Override public void close() throws Exception { servidor.close(); hilo.shutdownNow(); assertThat(hilo.awaitTermination(5, TimeUnit.SECONDS)).isTrue(); }
    }
    private record Mensaje(String destino, String remitente, String asunto, String cuerpo) {
        String token() {
            var encontrado = Pattern.compile("(?m)^[A-Za-z0-9_-]{43}\\r?$").matcher(cuerpo);
            assertThat(encontrado.find()).as("Código opaco presente en el correo MIME").isTrue();
            return encontrado.group().strip();
        }
        @Override public String toString() { return "Mensaje SMTP [contenido privado de prueba]"; }
    }
}
