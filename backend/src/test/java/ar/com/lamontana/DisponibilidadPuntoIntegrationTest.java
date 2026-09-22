package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.configuracion.*;
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

class DisponibilidadPuntoIntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String EMAIL="puntos@example.test",CLAVE="PuntosEntrega123!",TOKEN="token-local-puntos-entrega-exclusivo-test";
    private EmbeddedPostgres pg;private ConfigurableApplicationContext app;private JdbcTemplate jdbc;private Path archivos;private HttpClient admin;private int port;
    private String id,a,b,servicio;private JsonNode borrador;
    @BeforeEach void iniciar()throws Exception {
        archivos=Files.createTempDirectory("lamontana-puntos-");pg=EmbeddedPostgres.builder().start();arrancar();app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Admin","Puntos",EMAIL,CLAVE));admin=cliente();login(admin,EMAIL);
        var creado=post(admin,"/api/admin/configuracion/borradores",Map.of("operacion",UUID.randomUUID().toString()));status(creado,200);borrador=JSON.readTree(creado.body());id=borrador.get("codigoPublico").asString();
        a=sucursal("A");b=sucursal("B");var modelo=comando();modelo.put("modelo","MANUAL");aceptar(put(admin,base()+"/modelo",modelo));
        status(post(admin,"/api/admin/catalogo/servicios",Map.of("codigo","IMP","nombre","Impresión","tipo","IMPRESION")),201);servicio=jdbc.queryForObject("SELECT codigo_publico::text FROM lamontana.servicio WHERE codigo='IMP'",String.class);
        var recursos=comando();recursos.put("metodoAsignacion","MANUAL");recursos.put("serviciosPorSucursal",List.of(Map.of("sucursal",a,"servicios",List.of(servicio)),Map.of("sucursal",b,"servicios",List.of(servicio))));aceptar(put(admin,base()+"/recursos",recursos));
        guardarEntrega();
        aceptar(post(admin,ruta(),alta("P",List.of())));aceptar(post(admin,ruta(),alta("Q",List.of())));
    }
    @AfterEach void cerrar()throws Exception{if(app!=null)app.close();if(pg!=null)pg.close();if(archivos!=null)Files.deleteIfExists(archivos);}
    private void arrancar(){app=new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+archivos,"--lamontana.instalacion.token="+TOKEN);port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));jdbc=app.getBean(JdbcTemplate.class);}

    private String op(){return "/api/admin/puntos-entrega/disponibilidad";}
    private JsonNode panel()throws Exception{var r=get(admin,op());status(r,200);return JSON.readTree(r.body());}
    private Map<String,Object> cambio(long version,String estado){return new HashMap<>(Map.of("operacion",UUID.randomUUID().toString(),"version",version,"estado",estado));}
    private String punto(){return puntos().get(0).get("codigoPublico").asString();}

    @Test void declaracionExplicitaRecordatorioIndependenciaDeVersionYAutorizacion()throws Exception{
        assertThat(contar("disponibilidad_punto_entrega")).isZero();assertThat(panel().get("deshabilitados").asLong()).isZero();
        var inicial=panel().get("puntos").get(0).get("disponibilidad");assertThat(inicial.get("estado").asString()).isEqualTo("SIN_DEFINIR");assertThat(inicial.get("version").asLong()).isZero();assertThat(inicial.get("actor").isNull()).isTrue();
        assertThat(panel().get("contexto").asString()).isEqualTo("SIN_CONFIGURACION_ACTIVA");String antes=estado().toString();
        UUID autorizacion=UUID.randomUUID();jdbc.update("""
            INSERT INTO lamontana.autorizacion_configuracion(id_operacion,proposito,id_configuracion_version,version_configuracion,id_actor,version_acceso,correo_destino,hash_solicitud,hash_codigo,fecha_vencimiento)
            SELECT ?,'CANCELAR_BORRADOR',c.id_configuracion_version,c.version,u.id_usuario,u.version_acceso,u.correo,?,?,clock_timestamp()+interval '15 minutes'
            FROM lamontana.configuracion_version c JOIN lamontana.usuario u ON u.id_usuario=c.id_usuario_creador WHERE c.codigo_publico=?
            """,autorizacion,"a".repeat(64),"b".repeat(64),UUID.fromString(id));
        var r=put(admin,op()+"/"+punto(),cambio(0,"DESHABILITADO"));status(r,200);var estado=JSON.readTree(r.body());assertThat(estado.get("version").asLong()).isEqualTo(1);assertThat(estado.get("estado").asString()).isEqualTo("DESHABILITADO");assertThat(estado.get("actor").asString()).isEqualTo("Admin Puntos");assertThat(estado.get("actualizadaEn").isNull()).isFalse();assertThat(panel().get("deshabilitados").asLong()).isEqualTo(1);
        assertThat(estado().toString()).isEqualTo(antes);assertThat(jdbc.queryForObject("SELECT fecha_revocacion IS NULL FROM lamontana.autorizacion_configuracion WHERE id_operacion=?",Boolean.class,autorizacion)).isTrue();
        status(put(admin,op()+"/"+punto(),cambio(1,"DESHABILITADO")),409);assertThat(contar("evento_disponibilidad_punto")).isEqualTo(1);
        status(put(admin,op()+"/"+punto(),cambio(1,"HABILITADO")),200);assertThat(panel().get("deshabilitados").asLong()).isZero();assertThat(estado().toString()).isEqualTo(antes);
        var eventos=jdbc.queryForList("SELECT estado_anterior,estado_nuevo,version_anterior,version_nueva FROM lamontana.evento_disponibilidad_punto ORDER BY version_nueva");assertThat(eventos).hasSize(2);assertThat(eventos.get(0).get("estado_anterior")).isNull();assertThat(eventos.get(1).get("estado_anterior")).isEqualTo("DESHABILITADO");assertThat(eventos.get(1).get("version_nueva")).isEqualTo(2L);
    }

    @Test void repeticionConcurrenciaDestinoContenidoYPermisos()throws Exception{
        var first=cambio(0,"HABILITADO");var req=request(admin,"PUT",op()+"/"+punto(),first);var x=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());var y=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());status(x.get(15,TimeUnit.SECONDS),200);status(y.get(15,TimeUnit.SECONDS),200);assertThat(contar("evento_disponibilidad_punto")).isEqualTo(1);
        var a=cambio(1,"DESHABILITADO");var b=cambio(1,"DESHABILITADO");var reqA=request(admin,"PUT",op()+"/"+punto(),a);var reqB=request(admin,"PUT",op()+"/"+punto(),b);x=admin.sendAsync(reqA,HttpResponse.BodyHandlers.ofString());y=admin.sendAsync(reqB,HttpResponse.BodyHandlers.ofString());assertThat(List.of(x.get(15,TimeUnit.SECONDS).statusCode(),y.get(15,TimeUnit.SECONDS).statusCode())).containsExactlyInAnyOrder(200,409);assertThat(contar("evento_disponibilidad_punto")).isEqualTo(2);
        var replay=put(admin,op()+"/"+punto(),first);status(replay,200);assertThat(JSON.readTree(replay.body()).get("estado").asString()).isEqualTo("DESHABILITADO");assertThat(JSON.readTree(replay.body()).get("version").asLong()).isEqualTo(2);assertThat(contar("evento_disponibilidad_punto")).isEqualTo(2);
        String otro=puntos().get(1).get("codigoPublico").asString();status(put(admin,op()+"/"+otro,first),409);var modificado=new HashMap<>(first);modificado.put("estado","DESHABILITADO");status(put(admin,op()+"/"+punto(),modificado),409);
        for(Object v:List.of(-1,1.5)){var c=cambio(2,"HABILITADO");c.put("version",v);status(put(admin,op()+"/"+punto(),c),400);}for(String campo:List.of("operacion","version","estado")){var c=cambio(2,"HABILITADO");c.put(campo,null);status(put(admin,op()+"/"+punto(),c),400);}status(put(admin,op()+"/"+punto(),cambio(2,"SIN_DEFINIR")),400);status(put(admin,op()+"/"+UUID.randomUUID(),cambio(0,"HABILITADO")),404);
        var noCsrf=HttpRequest.newBuilder(uri(op()+"/"+punto())).header("Content-Type","application/json").PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(cambio(2,"HABILITADO")))) .build();status(admin.send(noCsrf,HttpResponse.BodyHandlers.ofString()),403);
        var externo=cliente();status(get(externo,op()),401);status(put(externo,op()+"/"+punto(),first),401);status(post(externo,"/api/auth/registro",Map.of("nombre","Cliente","apellido","Test","correo","cliente-puntos@example.test","contrasena",CLAVE)),201);login(externo,"cliente-puntos@example.test");status(get(externo,op()),403);status(put(externo,op()+"/"+punto(),first),403);
        for(String rol:List.of("EMPLEADO","ADMIN_ADMIN")){String correo=rol.toLowerCase()+"-puntos@example.test";jdbc.update("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado) SELECT ?,r.id_rol,?,u.hash_contrasena,'Interno','Test','ACTIVO' FROM lamontana.usuario u CROSS JOIN lamontana.rol r WHERE u.correo=? AND r.codigo=?",UUID.randomUUID(),correo,EMAIL,rol);var interno=cliente();login(interno,correo);status(get(interno,op()),403);status(put(interno,op()+"/"+punto(),first),403);}
        assertThat(contar("evento_disponibilidad_punto")).isEqualTo(2);
    }

    @Test void rollbackReinicioYConservacionAlCancelarPreparacion()throws Exception{
        String p=punto();var comando=cambio(0,"DESHABILITADO");var tipado=JSON.readValue(JSON.writeValueAsString(comando),DisponibilidadPuntoController.Cambiar.class);
        jdbc.execute("CREATE FUNCTION lamontana.fallar_disponibilidad_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fallo transaccional de prueba'; END $$");jdbc.execute("CREATE TRIGGER fallo_disponibilidad_test BEFORE INSERT ON lamontana.evento_disponibilidad_punto FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_disponibilidad_test()");
        assertThatThrownBy(()->app.getBean(DisponibilidadPuntoService.class).cambiar(UUID.fromString(p),tipado,EMAIL)).isInstanceOf(DataAccessException.class);assertThat(contar("disponibilidad_punto_entrega")).isZero();assertThat(contar("evento_disponibilidad_punto")).isZero();
        jdbc.execute("DROP TRIGGER fallo_disponibilidad_test ON lamontana.evento_disponibilidad_punto");jdbc.execute("DROP FUNCTION lamontana.fallar_disponibilidad_test()");status(put(admin,op()+"/"+p,comando),200);
        String antes=panel().toString();app.close();arrancar();assertThat(panel().toString()).isEqualTo(antes);status(put(admin,op()+"/"+p,comando),200);assertThat(contar("evento_disponibilidad_punto")).isEqualTo(1);
        jdbc.update("UPDATE lamontana.configuracion_version SET estado='CANCELADA',fecha_cancelacion=clock_timestamp(),id_usuario_cancelador=id_usuario_creador,motivo_cancelacion='Fixture de preparación cancelada' WHERE codigo_publico=?",UUID.fromString(id));
        assertThat(panel().get("puntos").size()).isZero();assertThat(panel().get("borrador").isNull()).isTrue();status(put(admin,op()+"/"+p,cambio(1,"HABILITADO")),409);status(put(admin,op()+"/"+p,comando),200);assertThat(contar("evento_disponibilidad_punto")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT estado FROM lamontana.disponibilidad_punto_entrega",String.class)).isEqualTo("DESHABILITADO");status(post(admin,"/api/admin/configuracion/borradores",Map.of("operacion",UUID.randomUUID().toString())),200);assertThat(panel().get("puntos").size()).isZero();
    }

    private Map<String,Object> franja(int dia,String apertura,String cierre,int capacidad,boolean habilitada){return new HashMap<>(Map.of("dia",dia,"apertura",apertura,"cierre",cierre,"capacidadPedidos",capacidad,"habilitada",habilitada));}
    private Map<String,Object> relacion(String sucursal,String costo,boolean habilitado,List<? extends Map<String,Object>> franjas){return new HashMap<>(Map.of("sucursal",sucursal,"costo",costo,"habilitado",habilitado,"franjas",franjas));}
    private Map<String,Object> edicion(List<? extends Map<String,Object>> relaciones){var p=comando();p.putAll(Map.of("nombre","Punto de prueba","calle","Calle","numero","456","localidad","Ciudad","provincia","Provincia","codigoPostal","1234","zonaHoraria","America/Argentina/Buenos_Aires","sucursales",relaciones));p.put("referencias",null);return p;}
    private Map<String,Object> alta(String codigo,List<? extends Map<String,Object>> relaciones){var p=edicion(relaciones);p.put("codigo",codigo);return p;}
    private void guardarEntrega()throws Exception{var e=comando();e.putAll(Map.of("preparacionHoras","2","trasladoHoras","1","modalidades",List.of("RETIRO_PUNTO_ENTREGA"),"horariosPorSucursal",List.of()));aceptar(put(admin,base()+"/entrega",e));}
    private JsonNode puntos(){return borrador.get("entrega").get("puntos");}
    private String primerId(){return puntos().get(0).get("codigoPublico").asString();}
    private JsonNode validacion()throws Exception{var r=get(admin,base()+"/entrega/validacion");status(r,200);return JSON.readTree(r.body());}
    private List<String> problemas()throws Exception{var result=new ArrayList<String>();for(var p:validacion().get("problemas"))result.add(p.get("codigo").asString());return result;}
    private String sucursal(String codigo)throws Exception{var r=post(admin,"/api/admin/sucursales",Map.of("codigo",codigo,"nombre","Sucursal "+codigo,"calle","Calle","numero","123","localidad","Ciudad","provincia","Provincia","codigoPostal","1234","zonaHoraria","America/Argentina/Buenos_Aires"));status(r,201);return JSON.readTree(r.body()).get("codigoPublico").asString();}
    private Map<String,Object> comando(){return new HashMap<>(Map.of("operacion",UUID.randomUUID().toString(),"version",borrador.get("version").asLong()));}
    private String base(){return "/api/admin/configuracion/borradores/"+id;}
    private String ruta(){return base()+"/entrega/puntos";}
    private int contar(String tabla){return jdbc.queryForObject("SELECT count(*) FROM lamontana."+tabla,Integer.class);}
    private JsonNode estado()throws Exception{var r=get(admin,"/api/admin/configuracion");status(r,200);return JSON.readTree(r.body());}
    private void aceptar(HttpResponse<String> r){status(r,200);borrador=JSON.readTree(r.body());}
    private HttpClient cliente(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build();}
    private URI uri(String path){return URI.create("http://127.0.0.1:"+port+path);}
    private HttpResponse<String> get(HttpClient c,String path)throws Exception{return c.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private String csrf(HttpClient c)throws Exception{var r=get(c,"/api/auth/csrf");status(r,200);return JSON.readTree(r.body()).get("token").asString();}
    private HttpRequest request(HttpClient c,String metodo,String path,Object data)throws Exception{return HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf(c)).method(metodo,HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(data))).build();}
    private HttpResponse<String> put(HttpClient c,String path,Object data)throws Exception{return c.send(request(c,"PUT",path,data),HttpResponse.BodyHandlers.ofString());}
    private HttpResponse<String> post(HttpClient c,String path,Object data)throws Exception{return c.send(request(c,"POST",path,data),HttpResponse.BodyHandlers.ofString());}
    private void login(HttpClient c,String correo)throws Exception{var r=HttpRequest.newBuilder(uri("/api/auth/login")).header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",csrf(c)).POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(correo,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(CLAVE,StandardCharsets.UTF_8))).build();status(c.send(r,HttpResponse.BodyHandlers.ofString()),200);}
    private void status(HttpResponse<String> r,int codigo){assertThat(r.statusCode()).withFailMessage("Esperado %s, recibido %s: %s",codigo,r.statusCode(),r.body()).isEqualTo(codigo);}
}
