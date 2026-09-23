//#region ENCABEZADO · ZonasEntregaIntegrationTest.java
/*
 * ========================================================================
 * ARCHIVO: ZonasEntregaIntegrationTest.java
 * ========================================================================
 * FUNCIÓN
 * Comprueba cobertura territorial normalizada, exclusividad, estados, capacidad compartida entre
 * sucursales y simulaciones de envío, con permisos, reintentos y rollback.
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
 * - [private] Map<String, Object> franja(int dia, String a, String c, int cupo, boolean
 *   habilitada)
 * - [private] Map<String, Object> territorio(String cp, String l, String p)
 * - [private] Map<String, Object> territorio()
 * - [private] Map<String, Object> zona(String codigo, boolean habilitada, List<?> territorios,
 *   List<?> franjas)
 * - [private] Map<String, Object> zona(String codigo)
 * - [private] String zonaId(String codigo)
 * - [private] Map<String, Object> editar(Map<String, Object> c)
 * - [private] Map<String, Object> envio(String origen)
 * - [paquete] void coberturaVaciaNormalizadaGlobalExclusivaYEstadosExplicitos() throws Exception
 *   Caso de prueba.
 * - [paquete] void simulacionResuelveTerritorioDosOrigenesYCupoGlobalSinEscribir() throws
 *   Exception
 *   Caso de prueba.
 * - [paquete] void replayConcurrenciaRollbackRevocacionYConservacionHistorica() throws Exception
 *   Caso de prueba.
 * - [paquete] void referenciasPermisosYCsrf() throws Exception
 *   Caso de prueba.
 * - [private] void servicios(List<String> sucursales) throws Exception
 * - [private] Map<String, Object> dia(int dia, Boolean habilitado, String apertura, String cierre)
 * - [private] List<Map<String, Object>> semana(String apertura, String cierre)
 * - [private] List<Map<String, Object>> soloDomingo(String apertura, String cierre)
 * - [private] Map<String, Object> horario(String sucursal, List<? extends Map<String, Object>>
 *   dias)
 * - [private] Map<String, Object> entrega(String preparacion, String traslado, List<String>
 *   modalidades, List<? extends Map<String, Object>> horarios)
 * - [private] void guardar(String preparacion, String traslado, List<String> modalidades, List<?
 *   extends Map<String, Object>> horarios) throws Exception
 * - [private] Map<String, Object> simulacion(String sucursal, String modalidad, String recibido)
 * - [private] JsonNode simular(String sucursal, String modalidad, String recibido) throws
 *   Exception
 * - [private] JsonNode validacion() throws Exception
 * - [private] void assertProblemas(String[] esperados) throws Exception
 * - [private] void assertInstante(JsonNode respuesta, String campo, String esperado)
 * - [private] String sucursal(String codigo) throws Exception
 * - [private] Map<String, Object> comando()
 * - [private] String base()
 * - [private] String ruta()
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
 * - ZonasEntregaIntegrationTest (clase).
 * ========================================================================
 */
//#endregion

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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ZonasEntregaIntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String EMAIL="entrega@example.test",CLAVE="ConfiguracionEntrega123!",TOKEN="token-local-entrega-configuracion-exclusivo-test";
    private EmbeddedPostgres pg;
    private ConfigurableApplicationContext app;
    private JdbcTemplate jdbc;
    private Path archivos;
    private HttpClient admin;
    private int port;
    private String id,a,b,servicio;
    private JsonNode borrador;
    @BeforeEach void iniciar()throws Exception {
        archivos=Files.createTempDirectory("lamontana-entrega-");pg=EmbeddedPostgres.builder().start();arrancar();
        app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Admin","Entrega",EMAIL,CLAVE));admin=cliente();login(admin,EMAIL);
        var creado=post(admin,"/api/admin/configuracion/borradores",Map.of("operacion",UUID.randomUUID().toString()));status(creado,200);borrador=JSON.readTree(creado.body());id=borrador.get("codigoPublico").asString();
        a=sucursal("A");b=sucursal("B");status(post(admin,"/api/admin/catalogo/servicios",Map.of("codigo","IMP","nombre","Impresión","tipo","IMPRESION")),201);servicio=jdbc.queryForObject("SELECT codigo_publico::text FROM lamontana.servicio WHERE codigo='IMP'",String.class);
        var modelo=comando();modelo.put("modelo","MANUAL");aceptar(put(admin,base()+"/modelo",modelo));servicios(List.of(a));
    }
    @AfterEach void cerrar()throws Exception{if(app!=null)app.close();if(pg!=null)pg.close();if(archivos!=null)Files.deleteIfExists(archivos);}
    private void arrancar(){app=new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+archivos,"--lamontana.instalacion.token="+TOKEN);port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));jdbc=app.getBean(JdbcTemplate.class);}


    private Map<String,Object> franja(int dia,String a,String c,int cupo,boolean habilitada){return Map.of("dia",dia,"apertura",a,"cierre",c,"capacidadPedidos",cupo,"habilitada",habilitada);}
    private Map<String,Object> territorio(String cp,String l,String p){return Map.of("codigoPostal",cp,"localidad",l,"provincia",p);}
    private Map<String,Object> territorio(){return territorio("5600","San Rafael","Mendoza");}
    private Map<String,Object> zona(String codigo,boolean habilitada,List<?> territorios,List<?> franjas){var c=comando();c.putAll(Map.of("codigo",codigo,"nombre","Zona "+codigo,"zonaHoraria","America/New_York","costo","12.50","habilitada",habilitada,"territorios",territorios,"franjas",franjas));return c;}
    private Map<String,Object> zona(String codigo){return zona(codigo,true,List.of(territorio()),List.of(franja(1,"17:00","19:00",7,true)));}
    private String zonaId(String codigo){for(var z:borrador.get("entrega").get("zonas"))if(z.get("codigo").asString().equals(codigo))return z.get("codigoPublico").asString();throw new AssertionError(codigo);}
    private Map<String,Object> editar(Map<String,Object> c){var result=new HashMap<>(c);result.remove("codigo");result.putAll(comando());return result;}
    private Map<String,Object> envio(String origen){var c=simulacion(origen,"ENVIO_DOMICILIO","2026-09-28T12:00:00Z");c.put("territorio",territorio());return c;}

    @Test void coberturaVaciaNormalizadaGlobalExclusivaYEstadosExplicitos()throws Exception{
        assertThat(borrador.get("entrega").get("zonas").size()).isZero();assertThat(contar("zona_entrega")).isZero();
        var parcial=zona(" z0 ",true,List.of(),List.of());aceptar(post(admin,ruta()+"/zonas",parcial));assertThat(zonaId("Z0")).isNotBlank();
        guardar("2","1",List.of("ENVIO_DOMICILIO"),List.of(horario(a,semana("09:00","18:00"))));assertProblemas("COBERTURA_ENVIO_PENDIENTE");
        var alta=zona("CENTRO",true,List.of(territorio("5 600"," san   rafael ","mendoza")),List.of(franja(1,"17:00","19:00",7,true)));aceptar(post(admin,ruta()+"/zonas",alta));String centro=zonaId("CENTRO");
        var datos=borrador.get("entrega").get("zonas").get(0);assertThat(datos.get("territorios").get(0).get("codigoPostal").asString()).isEqualTo("5600");assertThat(datos.get("territorios").get(0).get("localidad").asString()).isEqualTo("SAN RAFAEL");assertThat(validacion().get("valida").asBoolean()).isTrue();
        String antes=estado().toString();status(post(admin,ruta()+"/zonas",zona("OTRA")),400);assertThat(estado().toString()).isEqualTo(antes);
        var deshabilitada=zona("OTRA",false,List.of(territorio()),List.of(franja(1,"17:00","19:00",7,true)));aceptar(post(admin,ruta()+"/zonas",deshabilitada));String otra=zonaId("OTRA");var habilitar=editar(deshabilitada);habilitar.put("habilitada",true);status(put(admin,ruta()+"/zonas/"+otra,habilitar),400);
        assertThatThrownBy(()->jdbc.update("UPDATE lamontana.configuracion_zona_entrega SET habilitada=true WHERE id_zona_entrega=(SELECT id_zona_entrega FROM lamontana.zona_entrega WHERE codigo='OTRA')")).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        var apagar=editar(alta);apagar.put("habilitada",false);aceptar(put(admin,ruta()+"/zonas/"+centro,apagar));habilitar.putAll(comando());aceptar(put(admin,ruta()+"/zonas/"+otra,habilitar));assertThat(validacion().get("valida").asBoolean()).isTrue();
        aceptar(post(admin,ruta()+"/zonas",zona("MISMO_CP_OTRA_LOCALIDAD",true,List.of(territorio("5600","Otra ciudad","Mendoza")),List.of())));assertThat(contar("zona_entrega")).isEqualTo(4);
        var duplicado=zona("DUP",false,List.of(territorio(),territorio("5 600"," san rafael ","MENDOZA")),List.of());status(post(admin,ruta()+"/zonas",duplicado),400);
        for(String campo:List.of("habilitada","costo","territorios","franjas")){var malo=zona("MAL");malo.put(campo,null);status(post(admin,ruta()+"/zonas",malo),400);}
        for(String costo:List.of("-1","1e2","0.001","1000000000000","NaN")){var malo=zona("MAL");malo.put("costo",costo);status(post(admin,ruta()+"/zonas",malo),400);}
        for(var franjas:List.of(List.of(franja(1,"17:00","17:00",1,true)),List.of(franja(1,"17:00","19:00",1,true),franja(1,"18:00","20:00",1,false)),List.of(franja(8,"17:00","19:00",1,true)),List.of(franja(1,"17:00","19:00",-1,true))))status(post(admin,ruta()+"/zonas",zona("MAL",false,List.of(),franjas)),400);
        var fraccion=new HashMap<>(franja(1,"17:00","19:00",1,true));fraccion.put("capacidadPedidos",1.5);status(post(admin,ruta()+"/zonas",zona("MAL",false,List.of(),List.of(fraccion))),400);
        status(post(admin,ruta()+"/zonas",zona("MAL",false,List.of(territorio("56*","Ciudad","Provincia")),List.of())),400);
        var contiguas=zona("CONTIGUAS",false,List.of(),List.of(franja(1,"17:00","19:00",0,true),franja(1,"19:00","21:00",2,true)));contiguas.put("costo","0");aceptar(post(admin,ruta()+"/zonas",contiguas));
    }

    @Test void simulacionResuelveTerritorioDosOrigenesYCupoGlobalSinEscribir()throws Exception{
        servicios(List.of(a,b));guardar("2","1",List.of("ENVIO_DOMICILIO"),List.of(horario(a,semana("09:00","18:00")),horario(b,semana("10:00","18:00"))));aceptar(post(admin,ruta()+"/zonas",zona("CENTRO")));String zona=zonaId("CENTRO"),antes=estado().toString();int eventos=contar("evento_configuracion");
        for(String origen:List.of(a,b)){var r=post(admin,ruta()+"/simular",envio(origen));status(r,200);var s=JSON.readTree(r.body());assertInstante(s,"llegadaEstimada",origen.equals(a)?"2026-09-28T15:00:00Z":"2026-09-28T16:00:00Z");assertInstante(s,"disponibleDesde","2026-09-28T21:00:00Z");assertThat(s.get("destinoZona").get("zona").asString()).isEqualTo(zona);assertThat(s.get("destinoZona").get("costo").asString()).isEqualTo("12.50");assertThat(s.get("destinoZona").get("cupoConfigurado").asInt()).isEqualTo(7);assertThat(s.get("advertencias").toString()).contains("globales","no representa plazas libres");}
        var normalizado=envio(a);normalizado.put("territorio",territorio("5 600"," san  rafael ","mendoza"));status(post(admin,ruta()+"/simular",normalizado),200);
        for(var t:List.of(territorio("5601","San Rafael","Mendoza"),territorio("5600","Otra","Mendoza"),territorio("5600","San Rafael","Otra"))){var fuera=envio(a);fuera.put("territorio",t);status(post(admin,ruta()+"/simular",fuera),409);}
        var falta=envio(a);falta.remove("territorio");status(post(admin,ruta()+"/simular",falta),400);assertThat(estado().toString()).isEqualTo(antes);assertThat(contar("evento_configuracion")).isEqualTo(eventos);
        app.close();arrancar();status(post(admin,ruta()+"/simular",envio(a)),200);assertThat(estado().toString()).isEqualTo(antes);
        var apagar=editar(zona("CENTRO"));apagar.put("habilitada",false);aceptar(put(admin,ruta()+"/zonas/"+zona,apagar));assertProblemas("COBERTURA_ENVIO_PENDIENTE");status(post(admin,ruta()+"/simular",envio(a)),409);
        guardar("2","1",List.of("ENVIO_DOMICILIO","RETIRO_SUCURSAL"),List.of(horario(a,semana("09:00","18:00")),horario(b,semana("10:00","18:00"))));assertThat(validacion().get("valida").asBoolean()).isTrue();assertThat(validacion().get("avisos").toString()).contains("No se ofrecerá envío a domicilio");var extra=simulacion(a,"RETIRO_SUCURSAL","2026-09-28T12:00:00Z");extra.put("territorio",territorio());status(post(admin,ruta()+"/simular",extra),400);
    }

    @Test void replayConcurrenciaRollbackRevocacionYConservacionHistorica()throws Exception{
        var c=zona("CENTRO");aceptar(post(admin,ruta()+"/zonas",c));String zona=zonaId("CENTRO");long version=borrador.get("version").asLong();aceptar(post(admin,ruta()+"/zonas",c));assertThat(borrador.get("version").asLong()).isEqualTo(version);assertThat(contar("zona_entrega")).isEqualTo(1);var distinto=new HashMap<>(c);distinto.put("nombre","Otro");status(post(admin,ruta()+"/zonas",distinto),409);
        var cambiar=editar(c);cambiar.put("nombre","Nueva zona");var rival=editar(c);rival.put("nombre","Cambio rival");var req1=request(admin,"PUT",ruta()+"/zonas/"+zona,cambiar);var req2=request(admin,"PUT",ruta()+"/zonas/"+zona,rival);var f1=admin.sendAsync(req1,HttpResponse.BodyHandlers.ofString());var f2=admin.sendAsync(req2,HttpResponse.BodyHandlers.ofString());var r1=f1.get();var r2=f2.get();assertThat(List.of(r1.statusCode(),r2.statusCode())).containsExactlyInAnyOrder(200,409);aceptar(r1.statusCode()==200?r1:r2);
        long config=jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?",Long.class,UUID.fromString(id));long actor=jdbc.queryForObject("SELECT id_usuario FROM lamontana.usuario WHERE correo=?",Long.class,EMAIL);
        jdbc.update("INSERT INTO lamontana.autorizacion_configuracion(id_operacion,proposito,id_configuracion_version,version_configuracion,id_actor,version_acceso,correo_destino,hash_solicitud,hash_codigo,fecha_vencimiento) VALUES (?,'CANCELAR_BORRADOR',?,?,?,?,?,?,?,clock_timestamp()+interval '10 minutes')",UUID.randomUUID(),config,borrador.get("version").asLong(),actor,1,EMAIL,"a".repeat(64),"b".repeat(64));
        aceptar(put(admin,ruta()+"/zonas/"+zona,editar(c)));assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.autorizacion_configuracion WHERE fecha_revocacion IS NOT NULL",Integer.class)).isEqualTo(1);
        String antes=estado().toString();int eventos=contar("evento_configuracion");jdbc.execute("CREATE FUNCTION lamontana.fallar_zona_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fallo de prueba'; END $$");jdbc.execute("CREATE TRIGGER fallo_zona BEFORE INSERT ON lamontana.evento_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_zona_test()");
        var cuerpo=JSON.readValue(JSON.writeValueAsString(editar(c)),ar.com.lamontana.configuracion.ZonaEntregaController.EditarZona.class);assertThatThrownBy(()->app.getBean(ar.com.lamontana.configuracion.ZonaEntregaService.class).editar(UUID.fromString(id),UUID.fromString(zona),cuerpo,EMAIL)).isInstanceOf(org.springframework.dao.DataAccessException.class);jdbc.execute("DROP TRIGGER fallo_zona ON lamontana.evento_configuracion");assertThat(estado().toString()).isEqualTo(antes);assertThat(contar("evento_configuracion")).isEqualTo(eventos);
        jdbc.update("UPDATE lamontana.configuracion_version SET estado='CANCELADA',fecha_cancelacion=clock_timestamp(),motivo_cancelacion='Fixture histórico',id_usuario_cancelador=? WHERE id_configuracion_version=?",actor,config);
        assertThat(estado().get("historial").get(0).get("entrega").get("zonas").size()).isEqualTo(1);status(put(admin,ruta()+"/zonas/"+zona,editar(c)),409);
        var nuevo=post(admin,"/api/admin/configuracion/borradores",Map.of("operacion",UUID.randomUUID().toString()));status(nuevo,200);assertThat(JSON.readTree(nuevo.body()).get("entrega").get("zonas").size()).isZero();
    }

    @Test void referenciasPermisosYCsrf()throws Exception{
        var c=zona("CENTRO");status(put(admin,ruta()+"/zonas/"+UUID.randomUUID(),editar(c)),404);
        var sin=HttpRequest.newBuilder(uri(ruta()+"/zonas")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(c))).build();status(admin.send(sin,HttpResponse.BodyHandlers.ofString()),403);
        var externo=cliente();status(post(externo,ruta()+"/zonas",c),401);status(post(externo,"/api/auth/registro",Map.of("nombre","Cliente","apellido","Test","correo","cliente-zonas@example.test","contrasena",CLAVE)),201);login(externo,"cliente-zonas@example.test");status(post(externo,ruta()+"/zonas",c),403);
        for(String rol:List.of("EMPLEADO","ADMIN_ADMIN")){String correo=rol.toLowerCase()+"-zonas@example.test";jdbc.update("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado) SELECT ?,r.id_rol,?,u.hash_contrasena,'Interno','Test','ACTIVO' FROM lamontana.usuario u CROSS JOIN lamontana.rol r WHERE u.correo=? AND r.codigo=?",UUID.randomUUID(),correo,EMAIL,rol);var interno=cliente();login(interno,correo);status(post(interno,ruta()+"/zonas",c),403);status(put(interno,ruta()+"/zonas/"+UUID.randomUUID(),editar(c)),403);}
    }

    private void servicios(List<String> sucursales)throws Exception{var c=comando();c.put("metodoAsignacion","MANUAL");c.put("serviciosPorSucursal",sucursales.stream().map(s->Map.of("sucursal",s,"servicios",List.of(servicio))).toList());aceptar(put(admin,base()+"/recursos",c));}
    private Map<String,Object> dia(int dia,Boolean habilitado,String apertura,String cierre){var d=new HashMap<String,Object>();d.put("dia",dia);d.put("habilitado",habilitado);d.put("apertura",apertura);d.put("cierre",cierre);return d;}
    private List<Map<String,Object>> semana(String apertura,String cierre){var dias=new ArrayList<Map<String,Object>>();for(int d=1;d<=7;d++)dias.add(dia(d,d<=5,d<=5?apertura:null,d<=5?cierre:null));return dias;}
    private List<Map<String,Object>> soloDomingo(String apertura,String cierre){var dias=new ArrayList<Map<String,Object>>();for(int d=1;d<=7;d++)dias.add(dia(d,d==7,d==7?apertura:null,d==7?cierre:null));return dias;}
    // Capacidad explícita del fixture; la instalación no recibe estas franjas.
    private Map<String,Object> horario(String sucursal,List<? extends Map<String,Object>> dias){var franjas=dias.stream().filter(d->Boolean.TRUE.equals(d.get("habilitado"))&&d.get("apertura")!=null&&d.get("cierre")!=null).map(d->Map.of("dia",d.get("dia"),"apertura",d.get("apertura"),"cierre",d.get("cierre"),"capacidadPedidos",20,"habilitada",true)).toList();return Map.of("sucursal",sucursal,"dias",dias,"franjasRetiro",franjas);}
    private Map<String,Object> entrega(String preparacion,String traslado,List<String> modalidades,List<? extends Map<String,Object>> horarios){var c=comando();c.put("preparacionHoras",preparacion);c.put("trasladoHoras",traslado);c.put("modalidades",modalidades);c.put("horariosPorSucursal",horarios);return c;}
    private void guardar(String preparacion,String traslado,List<String> modalidades,List<? extends Map<String,Object>> horarios)throws Exception{aceptar(put(admin,ruta(),entrega(preparacion,traslado,modalidades,horarios)));}
    private Map<String,Object> simulacion(String sucursal,String modalidad,String recibido){return new HashMap<>(Map.of("version",borrador.get("version").asLong(),"sucursal",sucursal,"modalidad",modalidad,"recibidoEn",recibido));}
    private JsonNode simular(String sucursal,String modalidad,String recibido)throws Exception{var r=post(admin,ruta()+"/simular",simulacion(sucursal,modalidad,recibido));status(r,200);return JSON.readTree(r.body());}
    private JsonNode validacion()throws Exception{var r=get(admin,ruta()+"/validacion");status(r,200);return JSON.readTree(r.body());}
    private void assertProblemas(String... esperados)throws Exception{var v=validacion();assertThat(v.get("valida").asBoolean()).isFalse();var codigos=new ArrayList<String>();for(var p:v.get("problemas"))codigos.add(p.get("codigo").asString());assertThat(codigos).contains(esperados);}
    private void assertInstante(JsonNode respuesta,String campo,String esperado){assertThat(Instant.parse(respuesta.get(campo).asString())).isEqualTo(Instant.parse(esperado));}
    private String sucursal(String codigo)throws Exception{var r=post(admin,"/api/admin/sucursales",Map.of("codigo",codigo,"nombre","Sucursal "+codigo,"calle","Calle","numero","123","localidad","Ciudad","provincia","Provincia","codigoPostal","1234","zonaHoraria","America/Argentina/Buenos_Aires"));status(r,201);return JSON.readTree(r.body()).get("codigoPublico").asString();}
    private Map<String,Object> comando(){return new HashMap<>(Map.of("operacion",UUID.randomUUID().toString(),"version",borrador.get("version").asLong()));}
    private String base(){return "/api/admin/configuracion/borradores/"+id;}
    private String ruta(){return base()+"/entrega";}
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
