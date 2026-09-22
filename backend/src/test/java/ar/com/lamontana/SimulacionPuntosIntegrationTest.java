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

class SimulacionPuntosIntegrationTest {
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

    private Map<String,Object> franja(int dia,String inicio,String fin,int cupo,boolean habilitada){return Map.of("dia",dia,"apertura",inicio,"cierre",fin,"capacidadPedidos",cupo,"habilitada",habilitada);}
    private Map<String,Object> relacion(String origen,String costo,boolean habilitado,List<Map<String,Object>> franjas){return Map.of("sucursal",origen,"costo",costo,"habilitado",habilitado,"franjas",franjas);}
    private String punto(String codigo,String zona,List<Map<String,Object>> relaciones)throws Exception{var c=comando();c.putAll(Map.of("codigo",codigo,"nombre","Punto "+codigo,"calle","Calle","numero","123","localidad","Ciudad","provincia","Provincia","codigoPostal","1234","zonaHoraria",zona,"sucursales",relaciones));aceptar(post(admin,ruta()+"/puntos",c));return borrador.get("entrega").get("puntos").get(0).get("codigoPublico").asString();}
    private void disponibilidad(String punto,int version,String estado)throws Exception{status(put(admin,"/api/admin/puntos-entrega/disponibilidad/"+punto,Map.of("operacion",UUID.randomUUID().toString(),"version",version,"estado",estado)),200);}
    private Map<String,Object> ejemplo(String punto,String origen,String recibido){var c=simulacion(origen,"RETIRO_PUNTO_ENTREGA",recibido);c.put("punto",punto);return c;}
    private JsonNode calcular(String punto,String origen,String recibido)throws Exception{var r=post(admin,ruta()+"/simular",ejemplo(punto,origen,recibido));status(r,200);return JSON.readTree(r.body());}

    @Test void elegibilidadActualCompartidaConValidacionYRetiroAlternativo()throws Exception{
        guardar("2","1",List.of("RETIRO_PUNTO_ENTREGA"),List.of(horario(a,semana("09:00","18:00"))));
        String punto=punto("P","America/New_York",List.of(relacion(a,"12.50",true,List.of(franja(1,"17:00","19:00",2,true))),relacion(b,"0",true,List.of(franja(1,"17:00","19:00",4,true)))));
        assertProblemas("DISPONIBILIDAD_PUNTO_PENDIENTE");assertThat(validacion().get("puntosDisponibles").size()).isZero();status(post(admin,ruta()+"/simular",ejemplo(punto,a,"2026-09-28T12:00:00Z")),409);
        disponibilidad(punto,0,"HABILITADO");var v=validacion();assertThat(v.get("valida").asBoolean()).isTrue();assertThat(v.get("puntosDisponibles").size()).isEqualTo(1);assertThat(v.get("puntosDisponibles").get(0).get("sucursal").asString()).isEqualTo(a);
        var r=calcular(punto,a,"2026-09-28T12:00:00Z");assertInstante(r,"finPreparacion","2026-09-28T14:00:00Z");assertInstante(r,"llegadaEstimada","2026-09-28T15:00:00Z");assertInstante(r,"disponibleDesde","2026-09-28T21:00:00Z");var destino=r.get("destinoPunto");assertThat(destino.get("zonaHoraria").asString()).isEqualTo("America/New_York");assertThat(destino.get("costo").asString()).isEqualTo("12.50");assertThat(destino.get("cupoConfigurado").asInt()).isEqualTo(2);assertInstante(destino,"franjaHasta","2026-09-28T23:00:00Z");assertThat(destino.get("versionDisponibilidad").asLong()).isEqualTo(1);
        String antes=estado().toString();disponibilidad(punto,1,"DESHABILITADO");assertThat(estado().toString()).isEqualTo(antes);assertProblemas("DISPONIBILIDAD_PUNTO_PENDIENTE");status(post(admin,ruta()+"/simular",ejemplo(punto,a,"2026-09-28T12:00:00Z")),409);
        guardar("2","1",List.of("RETIRO_SUCURSAL","RETIRO_PUNTO_ENTREGA"),List.of(horario(a,semana("09:00","18:00"))));assertThat(validacion().get("valida").asBoolean()).isTrue();assertThat(validacion().get("avisos").size()).isEqualTo(1);assertThat(simular(a,"RETIRO_SUCURSAL","2026-09-28T12:00:00Z").get("destinoPunto").isNull()).isTrue();
        disponibilidad(punto,2,"HABILITADO");assertThat(calcular(punto,a,"2026-09-28T12:00:00Z").get("destinoPunto").get("versionDisponibilidad").asLong()).isEqualTo(3);assertThat(validacion().get("avisos").size()).isZero();
        status(post(admin,ruta()+"/simular",ejemplo(punto,b,"2026-09-28T12:00:00Z")),409);servicios(List.of(a,b));status(post(admin,ruta()+"/simular",ejemplo(punto,b,"2026-09-28T12:00:00Z")),409);
        guardar("2","1",List.of("RETIRO_PUNTO_ENTREGA"),List.of(horario(a,semana("09:00","18:00")),horario(b,semana("09:00","18:00"))));assertThat(validacion().get("puntosDisponibles").size()).isEqualTo(2);assertThat(calcular(punto,b,"2026-09-28T12:00:00Z").get("destinoPunto").get("costo").asString()).isEqualTo("0.00");
        jdbc.update("UPDATE lamontana.sucursal SET estado='DESACTIVADA',fecha_desactivacion=clock_timestamp() WHERE codigo_publico=?",UUID.fromString(b));assertThat(validacion().get("puntosDisponibles").size()).isEqualTo(1);status(post(admin,ruta()+"/simular",ejemplo(punto,b,"2026-09-28T12:00:00Z")),409);
    }

    @Test void cupoCeroRelacionDeshabilitadaCierresFinDeSemanaYPersistenciaSinEscrituras()throws Exception{
        guardar("2","1",List.of("RETIRO_PUNTO_ENTREGA"),List.of(horario(a,semana("09:00","18:00"))));
        String punto=punto("P","America/Argentina/Buenos_Aires",List.of(relacion(a,"0",true,List.of(franja(1,"09:00","12:00",0,true),franja(1,"12:00","14:00",8,false),franja(1,"17:00","19:00",2,true))),relacion(b,"99",false,List.of(franja(1,"17:00","19:00",2,true)))));disponibilidad(punto,0,"HABILITADO");
        var r=calcular(punto,a,"2026-09-25T20:00:00Z");assertInstante(r,"finPreparacion","2026-09-28T13:00:00Z");assertInstante(r,"llegadaEstimada","2026-09-28T14:00:00Z");assertInstante(r,"disponibleDesde","2026-09-28T20:00:00Z");
        assertThat(r.get("advertencias").toString()).contains("no representa plazas libres","ni crea una reserva");int eventos=contar("evento_configuracion"),operaciones=contar("evento_disponibilidad_punto");String antes=estado().toString();calcular(punto,a,"2026-09-25T20:00:00Z");validacion();assertThat(estado().toString()).isEqualTo(antes);assertThat(contar("evento_configuracion")).isEqualTo(eventos);assertThat(contar("evento_disponibilidad_punto")).isEqualTo(operaciones);
        app.close();arrancar();assertThat(calcular(punto,a,"2026-09-25T20:00:00Z")).isEqualTo(r);
        jdbc.update("UPDATE lamontana.franja_entrega SET capacidad_pedidos=0 WHERE habilitada");assertProblemas("PUNTOS_PENDIENTES");assertThat(validacion().get("puntosDisponibles").size()).isZero();status(post(admin,ruta()+"/simular",ejemplo(punto,a,"2026-09-25T20:00:00Z")),409);
    }

    @Test void referenciasVersionModalidadPermisosYCsrfSeValidanEnServidor()throws Exception{
        guardar("2","1",List.of("RETIRO_SUCURSAL","RETIRO_PUNTO_ENTREGA"),List.of(horario(a,semana("09:00","18:00"))));String punto=punto("P","UTC",List.of(relacion(a,"1",true,List.of(franja(1,"17:00","19:00",2,true)))));disponibilidad(punto,0,"HABILITADO");var c=ejemplo(punto,a,"2026-09-28T12:00:00Z");
        status(post(admin,ruta()+"/simular",simulacion(a,"RETIRO_PUNTO_ENTREGA","2026-09-28T12:00:00Z")),400);var extra=simulacion(a,"RETIRO_SUCURSAL","2026-09-28T12:00:00Z");extra.put("punto",punto);status(post(admin,ruta()+"/simular",extra),400);status(post(admin,ruta()+"/simular",ejemplo(UUID.randomUUID().toString(),a,"2026-09-28T12:00:00Z")),400);
        var viejo=new HashMap<>(c);viejo.put("version",borrador.get("version").asLong()-1);status(post(admin,ruta()+"/simular",viejo),409);
        var noCsrf=HttpRequest.newBuilder(uri(ruta()+"/simular")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(c))).build();status(admin.send(noCsrf,HttpResponse.BodyHandlers.ofString()),403);
        var externo=cliente();status(post(externo,ruta()+"/simular",c),401);status(post(externo,"/api/auth/registro",Map.of("nombre","Cliente","apellido","Test","correo","cliente-puntos@example.test","contrasena",CLAVE)),201);login(externo,"cliente-puntos@example.test");status(get(externo,ruta()+"/validacion"),403);status(post(externo,ruta()+"/simular",c),403);
        for(String rol:List.of("EMPLEADO","ADMIN_ADMIN")){String correo=rol.toLowerCase()+"-puntos@example.test";jdbc.update("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado) SELECT ?,r.id_rol,?,u.hash_contrasena,'Interno','Test','ACTIVO' FROM lamontana.usuario u CROSS JOIN lamontana.rol r WHERE u.correo=? AND r.codigo=?",UUID.randomUUID(),correo,EMAIL,rol);var interno=cliente();login(interno,correo);status(get(interno,ruta()+"/validacion"),403);status(post(interno,ruta()+"/simular",c),403);}
        guardar("2","1",List.of("RETIRO_SUCURSAL"),List.of(horario(a,semana("09:00","18:00"))));assertThat(validacion().get("puntosDisponibles").size()).isZero();status(post(admin,ruta()+"/simular",ejemplo(punto,a,"2026-09-28T12:00:00Z")),409);
    }

    private void servicios(List<String> sucursales)throws Exception{var c=comando();c.put("metodoAsignacion","MANUAL");c.put("serviciosPorSucursal",sucursales.stream().map(s->Map.of("sucursal",s,"servicios",List.of(servicio))).toList());aceptar(put(admin,base()+"/recursos",c));}
    private Map<String,Object> dia(int dia,Boolean habilitado,String apertura,String cierre){var d=new HashMap<String,Object>();d.put("dia",dia);d.put("habilitado",habilitado);d.put("apertura",apertura);d.put("cierre",cierre);return d;}
    private List<Map<String,Object>> semana(String apertura,String cierre){var dias=new ArrayList<Map<String,Object>>();for(int d=1;d<=7;d++)dias.add(dia(d,d<=5,d<=5?apertura:null,d<=5?cierre:null));return dias;}
    private List<Map<String,Object>> soloDomingo(String apertura,String cierre){var dias=new ArrayList<Map<String,Object>>();for(int d=1;d<=7;d++)dias.add(dia(d,d==7,d==7?apertura:null,d==7?cierre:null));return dias;}
    private Map<String,Object> horario(String sucursal,List<? extends Map<String,Object>> dias){return Map.of("sucursal",sucursal,"dias",dias);}
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
