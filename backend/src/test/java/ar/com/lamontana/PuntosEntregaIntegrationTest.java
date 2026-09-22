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

class PuntosEntregaIntegrationTest {
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
    }
    @AfterEach void cerrar()throws Exception{if(app!=null)app.close();if(pg!=null)pg.close();if(archivos!=null)Files.deleteIfExists(archivos);}
    private void arrancar(){app=new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+archivos,"--lamontana.instalacion.token="+TOKEN);port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));jdbc=app.getBean(JdbcTemplate.class);}

    @Test void estructuraVaciaCompartidaPorDosOrigenesCostosCuposYDisponibilidadPendiente()throws Exception {
        assertThat(puntos().size()).isZero();assertThat(contar("punto_entrega")).isZero();assertThat(problemas()).contains("PUNTOS_PENDIENTES");
        aceptar(post(admin,ruta(),alta(" p_uno ",List.of())));String punto=primerId();assertThat(puntos().get(0).get("codigo").asString()).isEqualTo("P_UNO");assertThat(puntos().get(0).has("disponibilidadOperativa")).isFalse();assertThat(puntos().get(0).get("sucursales").size()).isZero();
        assertThat(problemas()).contains("PUNTOS_PENDIENTES");
        var cero=relacion(a,"0",true,List.of(franja(1,"09:00","10:00",0,true)));aceptar(put(admin,ruta()+"/"+punto,edicion(List.of(cero))));assertThat(problemas()).contains("PUNTOS_PENDIENTES");
        var relA=relacion(a,"0",true,List.of(franja(1,"09:00","10:00",0,true),franja(1,"10:00","11:00",5,true)));
        var relB=relacion(b,"25.50",true,List.of(franja(1,"09:00","10:00",3,true)));
        aceptar(put(admin,ruta()+"/"+punto,edicion(List.of(relA,relB))));
        assertThat(problemas()).doesNotContain("PUNTOS_PENDIENTES").contains("DISPONIBILIDAD_PUNTO_PENDIENTE");assertThat(validacion().get("valida").asBoolean()).isFalse();
        var p=puntos().get(0);assertThat(p.get("sucursales").size()).isEqualTo(2);assertThat(p.get("sucursales").get(0).get("costo").asString()).isEqualTo("0.00");assertThat(p.get("sucursales").get(1).get("costo").asString()).isEqualTo("25.50");assertThat(contar("franja_entrega")).isEqualTo(3);
        assertThat(p.get("codigoPublico").asString()).isEqualTo(punto);assertThat(p.get("zonaHoraria").asString()).isEqualTo("America/Argentina/Buenos_Aires");
        jdbc.update("UPDATE lamontana.sucursal SET estado='DESACTIVADA',fecha_desactivacion=clock_timestamp() WHERE codigo_publico=?",UUID.fromString(b));
        aceptar(put(admin,ruta()+"/"+punto,edicion(List.of(relA,relB))));status(post(admin,ruta(),alta("P_DOS",List.of(relB))),409);
        var apagada=relacion(b,"25.50",false,List.of(franja(1,"09:00","10:00",3,true)));aceptar(put(admin,ruta()+"/"+punto,edicion(List.of(relA,apagada))));status(put(admin,ruta()+"/"+punto,edicion(List.of(relA,relB))),409);
        var antes=puntos();guardarEntrega();assertThat(puntos()).isEqualTo(antes);var modelo=comando();modelo.put("modelo","CONDICIONAL");modelo.put("criterio","SENA");aceptar(put(admin,base()+"/modelo",modelo));assertThat(puntos()).isEqualTo(antes);
        aceptar(put(admin,ruta()+"/"+punto,edicion(List.of())));assertThat(puntos().get(0).get("codigoPublico").asString()).isEqualTo(punto);assertThat(contar("punto_entrega")).isEqualTo(1);assertThat(contar("configuracion_punto_entrega")).isZero();assertThat(contar("franja_entrega")).isZero();
        status(post(admin,ruta(),alta("p_uno",List.of())),409);assertThat(contar("catalogo_revision")).isZero();
    }

    @Test void validaSolapamientosReferenciasPrecisionEnterosCamposExplicitosYZonaIana()throws Exception {
        String antes=borrador.toString();
        for(var franjas:List.of(List.of(franja(1,"09:00","11:00",1,true),franja(1,"10:00","12:00",1,true)),List.of(franja(1,"09:00","12:00",1,true),franja(1,"10:00","11:00",1,false)),List.of(franja(1,"09:00","10:00",1,true),franja(1,"09:00","10:00",1,true)),List.of(franja(1,"10:00","09:00",1,true)),List.of(franja(1,"09:00","09:00",1,true)),List.of(franja(8,"09:00","10:00",1,true)),List.of(franja(1,"9:00","10:00",1,true)),List.of(franja(1,"09:00:00","10:00",1,true)),List.of(franja(1,"09:00","24:00",1,true)),List.of(franja(1,"09:00","10:00",-1,true))))
            status(post(admin,ruta(),alta("P",List.of(relacion(a,"0",true,franjas)))),400);
        for(Object cupo:List.of(1.5,2147483648L)){var f=franja(1,"09:00","10:00",1,true);f.put("capacidadPedidos",cupo);status(post(admin,ruta(),alta("P",List.of(relacion(a,"0",true,List.of(f))))),400);}
        for(String campo:List.of("dia","apertura","cierre","capacidadPedidos","habilitada")){var f=franja(1,"09:00","10:00",1,true);f.put(campo,null);status(post(admin,ruta(),alta("P",List.of(relacion(a,"0",true,List.of(f))))),400);}
        for(String costo:List.of("-1","0.001","1e2","NaN","1000000000000"))status(post(admin,ruta(),alta("P",List.of(relacion(a,costo,true,List.of())))),400);
        for(String campo:List.of("costo","habilitado","sucursal","franjas")){var r=relacion(a,"0",true,List.of());r.put(campo,null);status(post(admin,ruta(),alta("P",List.of(r))),400);}
        var r=relacion(a,"0",true,List.of());status(post(admin,ruta(),alta("P",List.of(r,r))),400);status(post(admin,ruta(),alta("P",List.of(relacion(UUID.randomUUID().toString(),"0",true,List.of())))),400);
        for(String zona:List.of("-03:00","UTC-03:00","Mars/Base","america/argentina/buenos_aires")){var p=alta("P",List.of());p.put("zonaHoraria",zona);status(post(admin,ruta(),p),400);}
        for(String campo:List.of("codigo","nombre","calle","numero","localidad","provincia","codigoPostal","zonaHoraria")){var p=alta("P",List.of());p.put(campo,"");status(post(admin,ruta(),p),400);}
        var codigo=alta("sin espacios",List.of());status(post(admin,ruta(),codigo),400);
        assertThat(estado().get("borrador").toString()).isEqualTo(antes);assertThat(contar("punto_entrega")).isZero();assertThat(contar("franja_entrega")).isZero();
        aceptar(post(admin,ruta(),alta("P",List.of(relacion(a,"0",false,List.of(franja(1,"09:00","10:00",Integer.MAX_VALUE,false),franja(2,"09:00","10:00",0,false)))))));
        assertThat(puntos().get(0).get("sucursales").get(0).get("franjas").get(0).get("capacidadPedidos").asInt()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test void idempotenciaConcurrenteDestinoPermisosRollbackRevocacionPersistenciaYSnapshot()throws Exception {
        var inicial=alta("P",List.of(relacion(a,"10",true,List.of(franja(1,"09:00","10:00",2,true)))));var req=request(admin,"POST",ruta(),inicial);
        var x=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());var y=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());var rx=x.get(15,TimeUnit.SECONDS);var ry=y.get(15,TimeUnit.SECONDS);status(rx,200);status(ry,200);assertThat(rx.body()).isEqualTo(ry.body());borrador=JSON.readTree(rx.body());String punto=primerId();assertThat(contar("punto_entrega")).isEqualTo(1);
        aceptar(post(admin,ruta(),alta("Q",List.of())));String otro=puntos().get(1).get("codigoPublico").asString();
        var cambio=edicion(List.of(relacion(a,"20",true,List.of(franja(1,"10:00","11:00",3,true)))));var segundo=edicion(List.of());
        var requestA=request(admin,"PUT",ruta()+"/"+punto,cambio);var requestB=request(admin,"PUT",ruta()+"/"+punto,segundo);x=admin.sendAsync(requestA,HttpResponse.BodyHandlers.ofString());y=admin.sendAsync(requestB,HttpResponse.BodyHandlers.ofString());rx=x.get(15,TimeUnit.SECONDS);ry=y.get(15,TimeUnit.SECONDS);assertThat(List.of(rx.statusCode(),ry.statusCode())).containsExactlyInAnyOrder(200,409);borrador=estado().get("borrador");var ganador=rx.statusCode()==200?cambio:segundo;
        assertThat(JSON.readTree(put(admin,ruta()+"/"+punto,ganador).body())).isEqualTo(borrador);status(put(admin,ruta()+"/"+otro,ganador),409);assertThat(JSON.readTree(post(admin,ruta(),inicial).body())).isEqualTo(borrador);var distinto=new HashMap<>(inicial);distinto.put("nombre","Otro nombre");status(post(admin,ruta(),distinto),409);
        var noCsrf=HttpRequest.newBuilder(uri(ruta())).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(inicial))).build();status(admin.send(noCsrf,HttpResponse.BodyHandlers.ofString()),403);
        var externo=cliente();status(post(externo,ruta(),inicial),401);status(post(externo,"/api/auth/registro",Map.of("nombre","Cliente","apellido","Test","correo","cliente-puntos@example.test","contrasena",CLAVE)),201);login(externo,"cliente-puntos@example.test");status(post(externo,ruta(),inicial),403);status(put(externo,ruta()+"/"+punto,ganador),403);
        for(String rol:List.of("EMPLEADO","ADMIN_ADMIN")){String correo=rol.toLowerCase()+"-puntos@example.test";jdbc.update("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado) SELECT ?,r.id_rol,?,u.hash_contrasena,'Interno','Test','ACTIVO' FROM lamontana.usuario u CROSS JOIN lamontana.rol r WHERE u.correo=? AND r.codigo=?",UUID.randomUUID(),correo,EMAIL,rol);var interno=cliente();login(interno,correo);status(post(interno,ruta(),inicial),403);}
        UUID autorizacion=UUID.randomUUID();jdbc.update("""
                INSERT INTO lamontana.autorizacion_configuracion(id_operacion,proposito,id_configuracion_version,version_configuracion,id_actor,version_acceso,correo_destino,hash_solicitud,hash_codigo,fecha_vencimiento)
                SELECT ?,'CANCELAR_BORRADOR',c.id_configuracion_version,c.version,u.id_usuario,u.version_acceso,u.correo,?, ?,clock_timestamp()+interval '15 minutes'
                FROM lamontana.configuracion_version c JOIN lamontana.usuario u ON u.id_usuario=c.id_usuario_creador WHERE c.codigo_publico=?
                """,autorizacion,"a".repeat(64),"b".repeat(64),UUID.fromString(id));
        var siguiente=edicion(List.of(relacion(b,"99",true,List.of(franja(2,"12:00","14:00",4,true)))));siguiente.put("nombre","Punto actualizado");var tipada=JSON.readValue(JSON.writeValueAsString(siguiente),PuntoEntregaController.EditarPunto.class);int comprobantes=contar("comprobante_configuracion");
        jdbc.execute("CREATE FUNCTION lamontana.fallar_punto_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.tipo IN ('PUNTO_CREADO','PUNTO_EDITADO') THEN RAISE EXCEPTION 'fallo transaccional de prueba'; END IF; RETURN NEW; END $$");jdbc.execute("CREATE TRIGGER fallo_punto_test BEFORE INSERT ON lamontana.evento_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_punto_test()");
        assertThatThrownBy(()->app.getBean(PuntoEntregaService.class).editar(UUID.fromString(id),UUID.fromString(punto),tipada,EMAIL)).isInstanceOf(DataAccessException.class);
        var altaFallida=alta("FALLIDA",List.of(relacion(a,"1",true,List.of(franja(1,"09:00","10:00",1,true)))));var tipadaAlta=JSON.readValue(JSON.writeValueAsString(altaFallida),PuntoEntregaController.CrearPunto.class);
        assertThatThrownBy(()->app.getBean(PuntoEntregaService.class).crear(UUID.fromString(id),tipadaAlta,EMAIL)).isInstanceOf(DataAccessException.class);
        assertThat(estado().get("borrador")).isEqualTo(borrador);assertThat(contar("punto_entrega")).isEqualTo(2);assertThat(contar("comprobante_configuracion")).isEqualTo(comprobantes);assertThat(jdbc.queryForObject("SELECT fecha_revocacion IS NULL FROM lamontana.autorizacion_configuracion WHERE id_operacion=?",Boolean.class,autorizacion)).isTrue();
        jdbc.execute("DROP TRIGGER fallo_punto_test ON lamontana.evento_configuracion");jdbc.execute("DROP FUNCTION lamontana.fallar_punto_test()");aceptar(put(admin,ruta()+"/"+punto,siguiente));assertThat(jdbc.queryForObject("SELECT fecha_revocacion IS NOT NULL FROM lamontana.autorizacion_configuracion WHERE id_operacion=?",Boolean.class,autorizacion)).isTrue();
        assertThat(puntos().get(0).get("nombre").asString()).isEqualTo("Punto actualizado");assertThat(puntos().get(0).get("codigo").asString()).isEqualTo("P");assertThat(puntos().get(0).get("codigoPublico").asString()).isEqualTo(punto);
        String antes=estado().toString();app.close();arrancar();assertThat(estado().toString()).isEqualTo(antes);assertThat(JSON.readTree(post(admin,ruta(),inicial).body())).isEqualTo(borrador);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_configuracion WHERE tipo='PUNTO_CREADO' AND codigo_recurso=?",Integer.class,UUID.fromString(punto))).isEqualTo(1);
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
