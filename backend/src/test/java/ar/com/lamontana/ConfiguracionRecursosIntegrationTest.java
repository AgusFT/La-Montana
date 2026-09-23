//#region ENCABEZADO · ConfiguracionRecursosIntegrationTest.java
/*
 * ========================================================================
 * ARCHIVO: ConfiguracionRecursosIntegrationTest.java
 * ========================================================================
 * FUNCIÓN
 * Comprueba el ciclo de impresoras y servicios por sucursal en los borradores, con capacidades,
 * referencias, permisos, concurrencia, rollback y reinicio.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] void iniciar() throws Exception
 *   Preparación antes de cada prueba.
 * - [paquete] void cerrar() throws Exception
 *   Limpieza después de cada prueba.
 * - [private] void arrancar()
 * - [paquete] void recursosVaciosCicloDeImpresoraServiciosPorSucursalYDesactivacion() throws
 *   Exception
 *   Caso de prueba.
 * - [paquete] void capacidadesReferenciasDuplicadosPermisosYCsrfNoAlteranBorrador() throws
 *   Exception
 *   Caso de prueba.
 * - [paquete] void concurrenciaIdempotenciaDestinoRollbackRevocacionYReinicio() throws Exception
 *   Caso de prueba.
 * - [private] String publico(String tabla, String codigo)
 * - [private] String sucursal(String codigo) throws Exception
 * - [private] JsonNode sucursalActual(String codigo) throws Exception
 * - [private] void modelo(String modelo, String criterio) throws Exception
 * - [private] Map<String, Object> comando()
 * - [private] Map<String, Object> grupo(String sucursal, String[] servicios)
 * - [private] Map<String, Object> recursosComando(List<? extends Map<String, Object>> grupos)
 * - [private] Map<String, Object> alta(String sucursal, String nombre, String estado)
 * - [private] Map<String, Object> edicion(String nombre, int capacidad)
 * - [private] Map<String, Object> motivo()
 * - [private] Map<String, Object> estadoComando(String estado)
 * - [private] JsonNode recursos()
 * - [private] String impresora(String nombre, String sucursal)
 * - [private] JsonNode porCodigo(String codigo)
 * - [private] String ruta(String sufijo)
 * - [private] int contar(String tabla)
 * - [private] JsonNode estado() throws Exception
 * - [private] void aceptar(HttpResponse<String> r)
 * - [private] HttpClient cliente()
 * - [private] URI uri(String path)
 * - [private] HttpResponse<String> get(HttpClient c, String path) throws Exception
 * - [private] String csrf(HttpClient c) throws Exception
 * - [private] HttpRequest request(HttpClient c, String metodo, String path, Object data) throws
 *   Exception
 * - [private] HttpResponse<String> put(HttpClient c, String path, Object data) throws Exception
 * - [private] HttpResponse<String> post(HttpClient c, String path, Object data) throws Exception
 * - [private] void login(HttpClient c, String correo) throws Exception
 * - [private] void status(HttpResponse<String> r, int codigo)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ConfiguracionRecursosIntegrationTest (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.configuracion.RecursosConfiguracionController;
import ar.com.lamontana.configuracion.RecursosConfiguracionService;
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

class ConfiguracionRecursosIntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String EMAIL="recursos@example.test",CLAVE="ConfiguracionRecursos123!",TOKEN="token-local-recursos-configuracion-exclusivo-test";
    private EmbeddedPostgres pg;
    private ConfigurableApplicationContext app;
    private JdbcTemplate jdbc;
    private Path archivos;
    private HttpClient admin;
    private int port;
    private String id,a,b,f,s,t;
    private JsonNode borrador;
    @BeforeEach void iniciar()throws Exception {
        archivos=Files.createTempDirectory("lamontana-recursos-");pg=EmbeddedPostgres.builder().start();arrancar();
        app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Admin","Recursos",EMAIL,CLAVE));
        admin=cliente();login(admin,EMAIL);var creado=post(admin,"/api/admin/configuracion/borradores",Map.of("operacion",UUID.randomUUID().toString()));status(creado,200);
        borrador=JSON.readTree(creado.body());id=borrador.get("codigoPublico").asString();
        a=sucursal("A");b=sucursal("B");
        status(post(admin,"/api/admin/catalogo/formatos",Map.of("codigo","A4","nombre","A4","anchoMm",210,"altoMm",297)),201);
        status(post(admin,"/api/admin/catalogo/servicios",Map.of("codigo","IMP","nombre","Impresión","tipo","IMPRESION")),201);
        status(post(admin,"/api/admin/catalogo/servicios",Map.of("codigo","TER","nombre","Terminación","tipo","TERMINACION")),201);
        f=publico("formato","A4");s=publico("servicio","IMP");t=publico("servicio","TER");
    }
    @AfterEach void cerrar()throws Exception{if(app!=null)app.close();if(pg!=null)pg.close();if(archivos!=null)Files.deleteIfExists(archivos);}
    private void arrancar(){app=new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+archivos,"--lamontana.instalacion.token="+TOKEN);port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));jdbc=app.getBean(JdbcTemplate.class);}

    @Test void recursosVaciosCicloDeImpresoraServiciosPorSucursalYDesactivacion()throws Exception {
        assertThat(recursos().get("metodoAsignacion").isNull()).isTrue();assertThat(recursos().get("impresoras").size()).isZero();assertThat(recursos().get("serviciosPorSucursal").size()).isZero();
        status(put(admin,ruta("recursos"),recursosComando(List.of())),409);modelo("MANUAL",null);
        aceptar(put(admin,ruta("recursos"),recursosComando(List.of())));assertThat(recursos().get("metodoAsignacion").asString()).isEqualTo("MANUAL");assertThat(contar("impresora")).isZero();
        aceptar(put(admin,ruta("recursos"),recursosComando(List.of(grupo(a,s),grupo(b,t)))));
        assertThat(recursos().get("serviciosPorSucursal").size()).isEqualTo(2);
        aceptar(post(admin,ruta("impresoras"),alta(a,"Equipo","OPERATIVA")));String equipo=impresora("Equipo",a);
        status(put(admin,ruta("impresoras/"+equipo),edicion("Equipo",250)),409);status(post(admin,ruta("impresoras/"+equipo+"/retirar"),motivo()),409);
        status(post(admin,ruta("impresoras"),alta(a," equipo ","DESHABILITADA")),409);
        aceptar(post(admin,ruta("impresoras"),alta(b,"Equipo","DESHABILITADA")));String otro=impresora("Equipo",b);
        assertThat(equipo).isNotEqualTo(otro);
        aceptar(post(admin,ruta("impresoras/"+equipo+"/estado"),estadoComando("DESHABILITADA")));
        aceptar(put(admin,ruta("impresoras/"+equipo),edicion("Equipo actualizado",Integer.MAX_VALUE)));
        var recurso=porCodigo(equipo);assertThat(recurso.get("sucursal").asString()).isEqualTo(a);assertThat(recurso.get("capacidadHojas").asInt()).isEqualTo(Integer.MAX_VALUE);
        assertThat(recurso.get("admiteColor").asBoolean()).isTrue();assertThat(recurso.get("formatos").get(0).asString()).isEqualTo(f);
        // La baja real de sucursal no impide conservar/quitar su configuración, pero sí habilitar nuevas capacidades.
        var suc=JSON.readValue(JSON.writeValueAsString(sucursalActual(a)),Map.class);suc.remove("codigoPublico");suc.put("estado","DESACTIVADA");status(put(admin,"/api/admin/sucursales/"+a,suc),200);
        status(post(admin,ruta("impresoras"),alta(a,"Otra","DESHABILITADA")),409);
        status(post(admin,ruta("impresoras/"+equipo+"/estado"),estadoComando("OPERATIVA")),409);
        status(put(admin,ruta("recursos"),recursosComando(List.of(grupo(a,s,t),grupo(b,t)))),409);
        aceptar(put(admin,ruta("recursos"),recursosComando(List.of(grupo(a,s),grupo(b,t)))));
        aceptar(put(admin,ruta("impresoras/"+equipo),edicion("Equipo actualizado",350)));
        aceptar(post(admin,ruta("impresoras/"+equipo+"/retirar"),motivo()));var retirada=porCodigo(equipo);
        assertThat(retirada.get("estado").asString()).isEqualTo("RETIRADA");assertThat(retirada.get("retiradaEn").isNull()).isFalse();assertThat(retirada.get("motivoRetiro").asString()).isEqualTo("Motivo documentado");
        assertThat(retirada.get("formatos").size()).isEqualTo(1);assertThat(contar("impresora")).isEqualTo(2);
        assertThat(jdbc.query("SELECT coalesce(estado_anterior,'ALTA')||'→'||estado_nuevo FROM lamontana.evento_configuracion WHERE codigo_recurso=? AND estado_nuevo IS NOT NULL ORDER BY id_evento_configuracion",(rs,n)->rs.getString(1),UUID.fromString(equipo)))
                .containsExactly("ALTA→OPERATIVA","OPERATIVA→DESHABILITADA","DESHABILITADA→RETIRADA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_configuracion e JOIN lamontana.usuario u ON e.id_actor=u.id_usuario WHERE e.codigo_recurso=? AND e.estado_anterior IS NOT NULL AND e.motivo='Motivo documentado' AND u.correo=?",Integer.class,UUID.fromString(equipo),EMAIL)).isEqualTo(2);
        status(put(admin,ruta("impresoras/"+equipo),edicion("No",50)),409);status(post(admin,ruta("impresoras/"+equipo+"/estado"),estadoComando("OPERATIVA")),409);
        var anterior=recursos();modelo("CONDICIONAL","SENA");assertThat(recursos()).isEqualTo(anterior);
        aceptar(put(admin,ruta("recursos"),recursosComando(List.of())));assertThat(recursos().get("serviciosPorSucursal").size()).isZero();assertThat(recursos().get("impresoras").size()).isEqualTo(2);
        assertThat(contar("catalogo_revision")).isZero();assertThat(borrador.get("estado").asString()).isEqualTo("EN_PREPARACION");
    }

    @Test void capacidadesReferenciasDuplicadosPermisosYCsrfNoAlteranBorrador()throws Exception {
        modelo("MANUAL",null);String antes=borrador.toString();var base=alta(a,"Equipo","DESHABILITADA");
        for(var cambio:List.<Map<String,Object>>of(Map.of("formatos",List.of()),Map.of("formatos",List.of(f,f)),Map.of("formatos",List.of(UUID.randomUUID().toString())),
                Map.of("nombre"," "),Map.of("nombre","n".repeat(121)),Map.of("capacidadHojas",0),Map.of("capacidadHojas",1.5),Map.of("capacidadHojas",2147483648L),Map.of("estado","RETIRADA"),Map.of("sucursal",UUID.randomUUID().toString()))) {
            var invalido=new HashMap<>(base);invalido.putAll(cambio);status(post(admin,ruta("impresoras"),invalido),400);
        }
        for(String campo:List.of("admiteColor","admiteDobleFaz","capacidadHojas","estado")){var invalido=new HashMap<>(base);invalido.put(campo,null);status(post(admin,ruta("impresoras"),invalido),400);}
        for(var grupos:List.of(List.of(grupo(a,s,s)),List.of(grupo(a,s),grupo(a,t)),List.of(grupo(a,UUID.randomUUID().toString())),List.of(grupo(UUID.randomUUID().toString(),s))))status(put(admin,ruta("recursos"),recursosComando(grupos)),400);
        var auto=recursosComando(List.of());auto.put("metodoAsignacion","AUTOMATICA");status(put(admin,ruta("recursos"),auto),400);
        status(put(admin,ruta("impresoras/"+UUID.randomUUID()),edicion("No existe",50)),404);
        var noCsrf=HttpRequest.newBuilder(uri(ruta("impresoras"))).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(base))).build();status(admin.send(noCsrf,HttpResponse.BodyHandlers.ofString()),403);
        var externo=cliente();status(post(externo,ruta("impresoras"),base),401);
        status(post(externo,"/api/auth/registro",Map.of("nombre","Cliente","apellido","Test","correo","cliente-recursos@example.test","contrasena",CLAVE)),201);login(externo,"cliente-recursos@example.test");
        status(post(externo,ruta("impresoras"),base),403);status(put(externo,ruta("recursos"),recursosComando(List.of())),403);
        // Usuarios internos sin propiedad tampoco pueden preparar recursos, aunque el rol ADMIN_ADMIN permita llegar al servicio.
        for(String rol:List.of("EMPLEADO","ADMIN_ADMIN")) {
            String correo=rol.toLowerCase()+"@example.test";jdbc.update("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado) SELECT ?,r.id_rol,?,u.hash_contrasena,'Interno','Test','ACTIVO' FROM lamontana.usuario u CROSS JOIN lamontana.rol r WHERE u.correo=? AND r.codigo=?",UUID.randomUUID(),correo,EMAIL,rol);
            var interno=cliente();login(interno,correo);status(post(interno,ruta("impresoras"),base),403);
        }
        assertThat(estado().get("borrador").toString()).isEqualTo(antes);assertThat(contar("impresora")).isZero();assertThat(contar("comprobante_configuracion")).isEqualTo(2);
    }

    @Test void concurrenciaIdempotenciaDestinoRollbackRevocacionYReinicio()throws Exception {
        modelo("MANUAL",null);var alta=alta(a,"Equipo","DESHABILITADA");var req=request(admin,"POST",ruta("impresoras"),alta);
        var x=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());var y=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());var rx=x.get(15,TimeUnit.SECONDS);var ry=y.get(15,TimeUnit.SECONDS);
        status(rx,200);status(ry,200);assertThat(rx.body()).isEqualTo(ry.body());borrador=JSON.readTree(rx.body());String equipo=impresora("Equipo",a);assertThat(contar("impresora")).isEqualTo(1);
        var otraAlta=new HashMap<>(alta);otraAlta.put("nombre","Distinta");status(post(admin,ruta("impresoras"),otraAlta),409);
        aceptar(post(admin,ruta("impresoras"),alta(b,"Otro","DESHABILITADA")));String otro=impresora("Otro",b);
        var editA=edicion("Equipo",100);var editB=edicion("Equipo",200);var reqA=request(admin,"PUT",ruta("impresoras/"+equipo),editA);var reqB=request(admin,"PUT",ruta("impresoras/"+equipo),editB);
        x=admin.sendAsync(reqA,HttpResponse.BodyHandlers.ofString());y=admin.sendAsync(reqB,HttpResponse.BodyHandlers.ofString());rx=x.get(15,TimeUnit.SECONDS);ry=y.get(15,TimeUnit.SECONDS);
        assertThat(List.of(rx.statusCode(),ry.statusCode())).containsExactlyInAnyOrder(200,409);borrador=estado().get("borrador");var ganador=rx.statusCode()==200?editA:editB;
        assertThat(JSON.readTree(put(admin,ruta("impresoras/"+equipo),ganador).body())).isEqualTo(borrador);
        status(put(admin,ruta("impresoras/"+otro),ganador),409);assertThat(JSON.readTree(post(admin,ruta("impresoras"),alta).body())).isEqualTo(borrador);
        UUID autorizacion=UUID.randomUUID();jdbc.update("""
                INSERT INTO lamontana.autorizacion_configuracion(id_operacion,proposito,id_configuracion_version,version_configuracion,id_actor,version_acceso,correo_destino,hash_solicitud,hash_codigo,fecha_vencimiento)
                SELECT ?,'CANCELAR_BORRADOR',c.id_configuracion_version,c.version,u.id_usuario,u.version_acceso,u.correo,?, ?,clock_timestamp()+interval '15 minutes'
                FROM lamontana.configuracion_version c JOIN lamontana.usuario u ON u.id_usuario=c.id_usuario_creador WHERE c.codigo_publico=?
                """,autorizacion,"a".repeat(64),"b".repeat(64),UUID.fromString(id));
        var fallida=alta(a,"Rollback","OPERATIVA");var tipada=JSON.readValue(JSON.writeValueAsString(fallida),RecursosConfiguracionController.CrearImpresora.class);int comprobantes=contar("comprobante_configuracion");
        jdbc.execute("CREATE FUNCTION lamontana.fallar_recursos_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.tipo='IMPRESORA_CREADA' THEN RAISE EXCEPTION 'fallo transaccional de prueba'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER fallo_recursos_test BEFORE INSERT ON lamontana.evento_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_recursos_test()");
        assertThatThrownBy(()->app.getBean(RecursosConfiguracionService.class).crear(UUID.fromString(id),tipada,EMAIL)).isInstanceOf(DataAccessException.class);
        assertThat(estado().get("borrador")).isEqualTo(borrador);assertThat(contar("impresora")).isEqualTo(2);assertThat(contar("configuracion_impresora_formato")).isEqualTo(2);assertThat(contar("comprobante_configuracion")).isEqualTo(comprobantes);
        assertThat(jdbc.queryForObject("SELECT fecha_revocacion IS NULL FROM lamontana.autorizacion_configuracion WHERE id_operacion=?",Boolean.class,autorizacion)).isTrue();
        jdbc.execute("DROP TRIGGER fallo_recursos_test ON lamontana.evento_configuracion");jdbc.execute("DROP FUNCTION lamontana.fallar_recursos_test()");
        aceptar(post(admin,ruta("impresoras"),fallida));assertThat(jdbc.queryForObject("SELECT fecha_revocacion IS NOT NULL FROM lamontana.autorizacion_configuracion WHERE id_operacion=?",Boolean.class,autorizacion)).isTrue();
        var retiro=motivo();aceptar(post(admin,ruta("impresoras/"+equipo+"/retirar"),retiro));var retirada=porCodigo(equipo);
        aceptar(post(admin,ruta("impresoras"),alta(a,"Equipo","DESHABILITADA")));assertThat(contar("impresora")).isEqualTo(4);
        assertThat(JSON.readTree(post(admin,ruta("impresoras/"+equipo+"/retirar"),retiro).body())).isEqualTo(borrador);assertThat(porCodigo(equipo)).isEqualTo(retirada);
        String antes=estado().toString();app.close();arrancar();assertThat(estado().toString()).isEqualTo(antes);
        assertThat(JSON.readTree(post(admin,ruta("impresoras"),alta).body())).isEqualTo(borrador);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_configuracion WHERE tipo='IMPRESORA_RETIRADA' AND codigo_recurso=? AND motivo='Motivo documentado'",Integer.class,UUID.fromString(equipo))).isEqualTo(1);
    }

    private String publico(String tabla,String codigo){return jdbc.queryForObject("SELECT codigo_publico::text FROM lamontana."+tabla+" WHERE codigo=?",String.class,codigo);}
    private String sucursal(String codigo)throws Exception{var r=post(admin,"/api/admin/sucursales",Map.of("codigo",codigo,"nombre","Sucursal "+codigo,"calle","Calle","numero","123","localidad","Ciudad","provincia","Provincia","codigoPostal","1234","zonaHoraria","America/Argentina/Buenos_Aires"));status(r,201);return JSON.readTree(r.body()).get("codigoPublico").asString();}
    private JsonNode sucursalActual(String codigo)throws Exception{for(var s:JSON.readTree(get(admin,"/api/admin/sucursales").body()))if(s.get("codigoPublico").asString().equals(codigo))return s;throw new AssertionError("Sucursal ausente");}
    private void modelo(String modelo,String criterio)throws Exception{var c=comando();c.put("modelo",modelo);c.put("criterio",criterio);aceptar(put(admin,ruta("modelo"),c));}
    private Map<String,Object> comando(){return new HashMap<>(Map.of("operacion",UUID.randomUUID().toString(),"version",borrador.get("version").asLong()));}
    private Map<String,Object> grupo(String sucursal,String... servicios){return Map.of("sucursal",sucursal,"servicios",List.of(servicios));}
    private Map<String,Object> recursosComando(List<? extends Map<String,Object>> grupos){var c=comando();c.put("metodoAsignacion","MANUAL");c.put("serviciosPorSucursal",grupos);return c;}
    private Map<String,Object> alta(String sucursal,String nombre,String estado){var c=edicion(nombre,100);c.put("sucursal",sucursal);c.put("estado",estado);return c;}
    private Map<String,Object> edicion(String nombre,int capacidad){var c=comando();c.putAll(Map.of("nombre",nombre,"formatos",List.of(f),"admiteColor",true,"admiteDobleFaz",false,"capacidadHojas",capacidad));return c;}
    private Map<String,Object> motivo(){var c=comando();c.put("motivo","Motivo documentado");return c;}
    private Map<String,Object> estadoComando(String estado){var c=motivo();c.put("estado",estado);return c;}
    private JsonNode recursos(){return borrador.get("recursos");}
    private String impresora(String nombre,String sucursal){for(var p:recursos().get("impresoras"))if(p.get("nombre").asString().equals(nombre)&&p.get("sucursal").asString().equals(sucursal)&&!p.get("estado").asString().equals("RETIRADA"))return p.get("codigoPublico").asString();throw new AssertionError("Impresora ausente");}
    private JsonNode porCodigo(String codigo){for(var p:recursos().get("impresoras"))if(p.get("codigoPublico").asString().equals(codigo))return p;throw new AssertionError("Impresora ausente");}
    private String ruta(String sufijo){return "/api/admin/configuracion/borradores/"+id+"/"+sufijo;}
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
