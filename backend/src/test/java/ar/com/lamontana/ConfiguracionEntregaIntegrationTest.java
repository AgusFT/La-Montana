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

class ConfiguracionEntregaIntegrationTest {
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

    @Test void vacioParcialValidacionMultisucursalModalidadesYErroresSinDatosPrecargados()throws Exception {
        var inicial=borrador.get("entrega");assertThat(inicial.get("preparacionHoras").isNull()).isTrue();assertThat(inicial.get("trasladoHoras").isNull()).isTrue();assertThat(inicial.get("modalidades").size()).isZero();assertThat(inicial.get("horariosPorSucursal").size()).isZero();assertThat(contar("horario_sucursal")).isZero();
        assertProblemas("PREPARACION_PENDIENTE","SIN_MODALIDAD","HORARIO_PENDIENTE");
        var vacios=new ArrayList<Map<String,Object>>();for(int dia=1;dia<=7;dia++)vacios.add(dia(dia,null,null,null));
        aceptar(put(admin,ruta(),entrega(null,null,List.of(),List.of(horario(a,vacios)))));assertProblemas("HORARIO_INCOMPLETO","SIN_DIA_OPERATIVO");
        assertThat(borrador.get("entrega").get("horariosPorSucursal").get(0).get("dias").get(0).get("habilitado").isNull()).isTrue();
        var parcial=semana("09:00","18:00");parcial.set(0,dia(1,true,"09:00",null));guardar("4",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,parcial)));assertProblemas("HORARIO_INCOMPLETO");
        status(post(admin,ruta()+"/simular",simulacion(a,"RETIRO_SUCURSAL","2026-09-21T05:00:00Z")),409);
        guardar("4",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,semana("09:00","18:00"))));assertThat(validacion().get("valida").asBoolean()).isTrue();
        servicios(List.of(a,b));assertProblemas("HORARIO_PENDIENTE");
        guardar("4","0",List.of("RETIRO_SUCURSAL"),List.of(horario(a,semana("09:00","18:00")),horario(b,semana("10:00","17:00"))));assertThat(validacion().get("valida").asBoolean()).isTrue();
        guardar("4","2",List.of("RETIRO_SUCURSAL","RETIRO_PUNTO_ENTREGA","ENVIO_DOMICILIO"),List.of(horario(a,semana("09:00","18:00")),horario(b,semana("10:00","17:00"))));assertProblemas("PUNTOS_PENDIENTES","COBERTURA_ENVIO_PENDIENTE");
        assertThat(validacion().get("valida").asBoolean()).isFalse();status(post(admin,ruta()+"/simular",simulacion(a,"RETIRO_PUNTO_ENTREGA","2026-09-21T05:00:00Z")),409);
        String antes=borrador.toString();
        for(String invalido:List.of("0","-1","0.001","10000.01","10001","1e2","NaN"))status(put(admin,ruta(),entrega(invalido,"0",List.of("RETIRO_SUCURSAL"),List.of())),400);
        for(var dias:List.of(List.of(dia(1,true,"09:00","09:00")),List.of(dia(1,true,"18:00","09:00")),List.of(dia(1,false,"09:00",null)),List.of(dia(1,null,"09:00",null)),List.of(dia(0,true,"09:00","18:00")),List.of(dia(1,true,"9:00","18:00")),List.of(dia(1,true,"09:00:00","18:00")),List.of(dia(1,true,"09:00","24:00")),List.of(dia(1,false,null,null),dia(1,false,null,null))))
            status(put(admin,ruta(),entrega("4",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,dias)))),400);
        var fraccion=dia(1,true,"09:00","18:00");fraccion.put("dia",1.5);status(put(admin,ruta(),entrega("4",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,List.of(fraccion))))),400);
        status(put(admin,ruta(),entrega("4",null,List.of("RETIRO_SUCURSAL","RETIRO_SUCURSAL"),List.of())),400);
        status(put(admin,ruta(),entrega("4",null,List.of(),List.of(horario(a,List.of()),horario(a,List.of())))),400);
        status(put(admin,ruta(),entrega("4",null,List.of(),List.of(horario(UUID.randomUUID().toString(),List.of())))),400);
        assertThat(estado().get("borrador").toString()).isEqualTo(antes);
        jdbc.update("UPDATE lamontana.sucursal SET estado='DESACTIVADA',fecha_desactivacion=clock_timestamp() WHERE codigo_publico=?",UUID.fromString(a));
        status(post(admin,ruta()+"/simular",simulacion(a,"RETIRO_SUCURSAL","2026-09-21T05:00:00Z")),409);
        servicios(List.of());assertProblemas("SIN_SUCURSAL_OPERATIVA");assertThat(borrador.get("entrega").get("horariosPorSucursal").size()).isEqualTo(2);
    }

    @Test void simulacionCruzaCierresFinDeSemanaRespetaSegundosExactosYSnapshotDeZona()throws Exception {
        guardar("4","2",List.of("RETIRO_SUCURSAL","ENVIO_DOMICILIO"),List.of(horario(a,semana("09:00","18:00"))));
        var s=simular(a,"RETIRO_SUCURSAL","2026-09-21T05:00:00Z");assertInstante(s,"inicioPreparacion","2026-09-21T12:00:00Z");assertInstante(s,"finPreparacion","2026-09-21T16:00:00Z");assertThat(s.get("enCola").asBoolean()).isTrue();
        s=simular(a,"RETIRO_SUCURSAL","2026-09-21T20:00:00Z");assertInstante(s,"finPreparacion","2026-09-22T15:00:00Z");assertThat(s.get("enCola").asBoolean()).isFalse();
        guardar("1","2",List.of("RETIRO_SUCURSAL","ENVIO_DOMICILIO"),List.of(horario(a,semana("09:00","18:00"))));
        s=simular(a,"RETIRO_SUCURSAL","2026-09-21T20:00:00Z");assertInstante(s,"finPreparacion","2026-09-21T21:00:00Z");assertInstante(s,"disponibleDesde","2026-09-22T12:00:00Z");
        s=simular(a,"ENVIO_DOMICILIO","2026-09-21T20:00:00Z");assertInstante(s,"llegadaEstimada","2026-09-22T14:00:00Z");assertThat(s.get("disponibleDesde").isNull()).isTrue();assertThat(s.get("advertencias").toString()).contains("cobertura","ventanas operativas");
        guardar("2",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,semana("09:00","18:00"))));
        s=simular(a,"RETIRO_SUCURSAL","2026-09-25T20:00:00Z");assertInstante(s,"finPreparacion","2026-09-28T13:00:00Z");
        guardar("0.01",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,semana("09:00","18:00"))));s=simular(a,"RETIRO_SUCURSAL","2026-09-21T12:00:00Z");assertInstante(s,"finPreparacion","2026-09-21T12:00:36Z");
        var configuracion=borrador.get("entrega");jdbc.update("UPDATE lamontana.sucursal SET zona_horaria='UTC' WHERE codigo_publico=?",UUID.fromString(a));
        guardar("0.01",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,semana("09:00","18:00"))));assertThat(borrador.get("entrega")).isEqualTo(configuracion);
        s=simular(a,"RETIRO_SUCURSAL","2026-09-21T05:00:00Z");assertThat(s.get("zonaHoraria").asString()).isEqualTo("America/Argentina/Buenos_Aires");assertInstante(s,"inicioPreparacion","2026-09-21T12:00:00Z");
        int eventos=contar("evento_configuracion"),comprobantes=contar("comprobante_configuracion");String antes=estado().toString();validacion();simular(a,"RETIRO_SUCURSAL","2026-09-21T05:00:00Z");assertThat(estado().toString()).isEqualTo(antes);assertThat(contar("evento_configuracion")).isEqualTo(eventos);assertThat(contar("comprobante_configuracion")).isEqualTo(comprobantes);
        var vieja=simulacion(a,"RETIRO_SUCURSAL","2026-09-21T05:00:00Z");vieja.put("version",borrador.get("version").asLong()-1);status(post(admin,ruta()+"/simular",vieja),409);
        status(post(admin,ruta()+"/simular",simulacion(a,"ENVIO_DOMICILIO","2026-09-21T05:00:00Z")),409);
        guardar("10000",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,soloDomingo("09:00","09:01"))));var limite=post(admin,ruta()+"/simular",simulacion(a,"RETIRO_SUCURSAL","2026-09-21T05:00:00Z"));status(limite,400);assertThat(limite.body()).contains("cinco años");
    }

    @Test void cambiosDstCuentanDuracionRealYOmiteVentanasInvertidasPorSaltoHorario()throws Exception {
        jdbc.update("UPDATE lamontana.sucursal SET zona_horaria='America/New_York' WHERE codigo_publico=?",UUID.fromString(a));
        guardar("2",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,soloDomingo("01:00","04:00"))));
        var s=simular(a,"RETIRO_SUCURSAL","2026-03-08T06:00:00Z");assertInstante(s,"finPreparacion","2026-03-08T08:00:00Z");assertInstante(s,"disponibleDesde","2026-03-15T05:00:00Z");
        guardar("3",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,soloDomingo("01:00","04:00"))));
        s=simular(a,"RETIRO_SUCURSAL","2026-11-01T05:00:00Z");assertInstante(s,"finPreparacion","2026-11-01T08:00:00Z");assertInstante(s,"disponibleDesde","2026-11-01T08:00:00Z");
        guardar("2",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,soloDomingo("01:00","02:00"))));
        s=simular(a,"RETIRO_SUCURSAL","2026-11-01T05:00:00Z");assertInstante(s,"finPreparacion","2026-11-01T07:00:00Z");assertInstante(s,"disponibleDesde","2026-11-08T06:00:00Z");
        guardar("0.01",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,soloDomingo("02:30","03:00"))));
        s=simular(a,"RETIRO_SUCURSAL","2026-03-08T05:00:00Z");assertInstante(s,"inicioPreparacion","2026-03-15T06:30:00Z");assertInstante(s,"finPreparacion","2026-03-15T06:30:36Z");
    }

    @Test void idempotenciaConcurrenciaPermisosCsrfRollbackRevocaAutorizacionYPersiste()throws Exception {
        var inicial=entrega("4",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,semana("09:00","18:00"))));var req=request(admin,"PUT",ruta(),inicial);
        var x=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());var y=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());var rx=x.get(15,TimeUnit.SECONDS);var ry=y.get(15,TimeUnit.SECONDS);status(rx,200);status(ry,200);assertThat(rx.body()).isEqualTo(ry.body());borrador=JSON.readTree(rx.body());
        var cambio=entrega("5",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,semana("09:00","18:00"))));var otro=entrega("6",null,List.of("RETIRO_SUCURSAL"),List.of(horario(a,semana("09:00","18:00"))));
        var requestA=request(admin,"PUT",ruta(),cambio);var requestB=request(admin,"PUT",ruta(),otro);x=admin.sendAsync(requestA,HttpResponse.BodyHandlers.ofString());y=admin.sendAsync(requestB,HttpResponse.BodyHandlers.ofString());
        assertThat(List.of(x.get(15,TimeUnit.SECONDS).statusCode(),y.get(15,TimeUnit.SECONDS).statusCode())).containsExactlyInAnyOrder(200,409);borrador=estado().get("borrador");
        assertThat(JSON.readTree(put(admin,ruta(),inicial).body())).isEqualTo(borrador);var diferente=new HashMap<>(inicial);diferente.put("preparacionHoras","9");status(put(admin,ruta(),diferente),409);
        status(put(admin,"/api/admin/configuracion/borradores/"+UUID.randomUUID()+"/entrega",inicial),409);
        var noCsrf=HttpRequest.newBuilder(uri(ruta())).header("Content-Type","application/json").PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(cambio))).build();status(admin.send(noCsrf,HttpResponse.BodyHandlers.ofString()),403);
        var cliente=cliente();status(get(cliente,ruta()+"/validacion"),401);status(post(cliente,"/api/auth/registro",Map.of("nombre","Cliente","apellido","Test","correo","cliente-entrega@example.test","contrasena",CLAVE)),201);login(cliente,"cliente-entrega@example.test");
        status(get(cliente,ruta()+"/validacion"),403);status(put(cliente,ruta(),inicial),403);status(post(cliente,ruta()+"/simular",simulacion(a,"RETIRO_SUCURSAL","2026-09-21T05:00:00Z")),403);
        UUID autorizacion=UUID.randomUUID();jdbc.update("""
                INSERT INTO lamontana.autorizacion_configuracion(id_operacion,proposito,id_configuracion_version,version_configuracion,id_actor,version_acceso,correo_destino,hash_solicitud,hash_codigo,fecha_vencimiento)
                SELECT ?,'CANCELAR_BORRADOR',c.id_configuracion_version,c.version,u.id_usuario,u.version_acceso,u.correo,?, ?,clock_timestamp()+interval '15 minutes'
                FROM lamontana.configuracion_version c JOIN lamontana.usuario u ON u.id_usuario=c.id_usuario_creador WHERE c.codigo_publico=?
                """,autorizacion,"a".repeat(64),"b".repeat(64),UUID.fromString(id));
        var siguiente=entrega("7","2",List.of("ENVIO_DOMICILIO"),List.of(horario(b,semana("10:00","16:00"))));var tipada=JSON.readValue(JSON.writeValueAsString(siguiente),EntregaConfiguracionController.GuardarEntrega.class);int comprobantes=contar("comprobante_configuracion");
        jdbc.execute("CREATE FUNCTION lamontana.fallar_entrega_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.tipo='ENTREGA_CONFIGURADA' THEN RAISE EXCEPTION 'fallo transaccional de prueba'; END IF; RETURN NEW; END $$");jdbc.execute("CREATE TRIGGER fallo_entrega_test BEFORE INSERT ON lamontana.evento_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_entrega_test()");
        assertThatThrownBy(()->app.getBean(EntregaConfiguracionService.class).guardar(UUID.fromString(id),tipada,EMAIL)).isInstanceOf(DataAccessException.class);
        assertThat(estado().get("borrador")).isEqualTo(borrador);assertThat(contar("horario_sucursal")).isEqualTo(7);assertThat(contar("comprobante_configuracion")).isEqualTo(comprobantes);assertThat(jdbc.queryForObject("SELECT fecha_revocacion IS NULL FROM lamontana.autorizacion_configuracion WHERE id_operacion=?",Boolean.class,autorizacion)).isTrue();
        jdbc.execute("DROP TRIGGER fallo_entrega_test ON lamontana.evento_configuracion");jdbc.execute("DROP FUNCTION lamontana.fallar_entrega_test()");aceptar(put(admin,ruta(),siguiente));assertThat(jdbc.queryForObject("SELECT fecha_revocacion IS NOT NULL FROM lamontana.autorizacion_configuracion WHERE id_operacion=?",Boolean.class,autorizacion)).isTrue();
        var entrega=borrador.get("entrega");var modelo=comando();modelo.put("modelo","CONDICIONAL");modelo.put("criterio","SENA");aceptar(put(admin,base()+"/modelo",modelo));assertThat(borrador.get("entrega")).isEqualTo(entrega);
        String antes=estado().toString();app.close();arrancar();assertThat(estado().toString()).isEqualTo(antes);assertThat(JSON.readTree(put(admin,ruta(),inicial).body())).isEqualTo(borrador);
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
