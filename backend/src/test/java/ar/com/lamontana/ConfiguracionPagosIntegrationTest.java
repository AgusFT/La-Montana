package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.configuracion.ConfiguracionController;
import ar.com.lamontana.configuracion.PagosConfiguracionService;
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

class ConfiguracionPagosIntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String EMAIL="pagos@example.test",CLAVE="ConfiguracionPagos123!",TOKEN="token-local-pagos-configuracion-exclusivo-test";
    private EmbeddedPostgres pg;
    private ConfigurableApplicationContext app;
    private JdbcTemplate jdbc;
    private Path archivos;
    private HttpClient admin;
    private int port;
    private String id;
    private JsonNode borrador;

    @BeforeEach void iniciar() throws Exception {
        archivos=Files.createTempDirectory("lamontana-pagos-config-");pg=EmbeddedPostgres.builder().start();arrancar();
        app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Admin","Pagos",EMAIL,CLAVE));
        admin=cliente();login(admin,EMAIL);
        var creado=post(admin,"/api/admin/configuracion/borradores",Map.of("operacion",UUID.randomUUID().toString()));status(creado,200);
        borrador=JSON.readTree(creado.body());id=borrador.get("codigoPublico").asString();
    }
    @AfterEach void cerrar() throws Exception {if(app!=null)app.close();if(pg!=null)pg.close();if(archivos!=null)Files.deleteIfExists(archivos);}
    private void arrancar() {
        app=new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),
                "--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+archivos,"--lamontana.instalacion.token="+TOKEN);
        port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));jdbc=app.getBean(JdbcTemplate.class);
    }

    @Test void matrizGuiadaSimulaManualPagoPrevioSenaYLimitesDeMontoRespetandoD1() throws Exception {
        assertThat(borrador.get("pagos").isNull()).isTrue();assertThat(contar("configuracion_financiera")).isZero();
        status(put(admin,ruta(),comando(pagos(false,null,null,null,null,null,false))),409);
        modelo("MANUAL",null);guardar(pagos(false,null,null,null,null,null,false));
        var s=simular("100.00",199);assertSim(s,true,false,"0.00","0.00","100.00","ANTES_ENTREGA");
        assertThat(s.get("mediosAcreditacion").size()).isZero();
        guardar(pagos(true,"SIEMPRE",null,"PORCENTAJE","30",null,false));
        s=simular("100.00",1);assertSim(s,true,false,"0.00","30.00","70.00","ANTES_PRODUCCION");
        assertThat(s.get("mediosAcreditacion").get(0).asString()).isEqualTo("EFECTIVO");
        guardar(pagos(true,"DESDE_CARILLAS","200","PORCENTAJE","12.3456",null,true));
        assertThat(simular("100.00",199).get("senaRequerida").asString()).isEqualTo("0.00");
        assertThat(simular("100.00",200).get("senaRequerida").asString()).isEqualTo("12.35");
        assertThat(borrador.get("pagos").get("valorSena").asString()).isEqualTo("12.3456");
        guardar(pagos(true,"DESDE_MONTO","100.00","FIJA","400.00",null,true));
        assertThat(simular("99.99",1).get("senaRequerida").asString()).isEqualTo("0.00");
        assertSim(simular("100.00",1),true,false,"0.00","100.00","0.00","ANTES_PRODUCCION");

        modelo("CONDICIONAL","PAGO_PREVIO");assertThat(borrador.get("pagos").isNull()).isTrue();
        var prepago=pagos(false,null,null,null,null,null,true);status(put(admin,ruta(),comando(prepago)),400);
        prepago.put("medios",List.of("TRANSFERENCIA"));guardar(prepago);
        assertSim(simular("17.55",1),false,true,"17.55","0.00","0.00","ANTES_CARGA");
        modelo("CONDICIONAL","SENA");guardar(pagos(true,"SIEMPRE",null,"FIJA","10.00",null,true));
        s=simular("5.00",1);assertSim(s,false,true,"0.00","5.00","0.00","ANTES_CARGA");
        assertThat(s.get("mediosGenerales").size()).isEqualTo(2);assertThat(s.get("mediosAcreditacion").size()).isEqualTo(1);
        assertThat(s.get("mediosAcreditacion").get(0).asString()).isEqualTo("TRANSFERENCIA");
        guardar(pagos(true,"SIEMPRE",null,"PORCENTAJE","0.0001",null,true));
        s=simular("0.01",1);assertSim(s,false,false,"0.00","0.00","0.01","ANTES_CARGA");
        assertThat(s.get("mediosAcreditacion").size()).isZero();assertThat(s.get("instrucciones").toString()).contains("redondea a 0.00","no hay importe exigible");
        modelo("CONDICIONAL","MONTO_TOTAL");guardar(pagos(true,"SUPERAR_UMBRAL_APROBACION",null,"PORCENTAJE","25","1000.00",true));
        assertSim(simular("1000.00",500),false,false,"0.00","0.00","1000.00","ANTES_ENTREGA");
        s=simular("1000.01",1);assertSim(s,true,false,"0.00","250.00","750.01","DESPUES_APROBACION_ANTES_PRODUCCION");
        assertThat(s.get("instrucciones").toString()).contains("revisar el PDF","Después de aprobar");
        assertThat(borrador.get("estado").asString()).isEqualTo("EN_PREPARACION");assertThat(contar("catalogo_revision")).isZero();
        int eventos=contar("evento_configuracion"),comprobantes=contar("comprobante_configuracion");String antes=estado().toString();
        simular("100.00",1);simular("1100.00",200);
        assertThat(estado().toString()).isEqualTo(antes);assertThat(contar("evento_configuracion")).isEqualTo(eventos);assertThat(contar("comprobante_configuracion")).isEqualTo(comprobantes);
    }

    @Test void validacionesDeCompletitudPrecisionModeloYSimulacionRechazanSinGuardarParcial() throws Exception {
        modelo("MANUAL",null);String antes=estado().toString();
        var base=pagos(false,null,null,null,null,null,false);
        for(var cambio:List.<Map<String,Object>>of(
                Map.of("medios",List.of()),Map.of("medios",List.of("EFECTIVO","EFECTIVO")),Map.of("medios",List.of("TRANSFERENCIA")),
                Map.of("instruccionesTransferencia","Datos sin medio habilitado"),Map.of("vigenciaCotizacionMinutos",0),
                Map.of("vigenciaCotizacionMinutos",1.5),Map.of("valorSena","1"),Map.of("umbralAprobacion","100"))) {
            var invalido=new HashMap<>(base);invalido.putAll(cambio);status(put(admin,ruta(),comando(invalido)),400);
        }
        for(var p:List.of(pagos(true,"SIEMPRE",null,"FIJA","1.001",null,false),pagos(true,"SIEMPRE",null,"PORCENTAJE","100.0001",null,false),
                pagos(true,"SIEMPRE",null,"PORCENTAJE","0",null,false),pagos(true,"SIEMPRE",null,"PORCENTAJE","10.12345",null,false),
                pagos(true,"SIEMPRE",null,"FIJA","1e2",null,false),pagos(true,"DESDE_CARILLAS","200.0","FIJA","10",null,false),
                pagos(true,"DESDE_MONTO","0","FIJA","10",null,false),pagos(true,"SUPERAR_UMBRAL_APROBACION",null,"FIJA","10",null,false),
                pagos(true,"SIEMPRE",null,null,null,null,false)))status(put(admin,ruta(),comando(p)),400);
        assertThat(estado().toString()).isEqualTo(antes);assertThat(contar("configuracion_financiera")).isZero();assertThat(contar("comprobante_configuracion")).isEqualTo(2);
        status(post(admin,ruta()+"/simular",Map.of("version",version(),"total","100.00","carillas",1)),409);
        guardar(base);
        for(String total:List.of("0","-1","1.001","1e2","NaN"))status(post(admin,ruta()+"/simular",Map.of("version",version(),"total",total,"carillas",1)),400);
        status(post(admin,ruta()+"/simular",Map.of("version",version(),"total","10.00","carillas",0)),400);
        status(post(admin,ruta()+"/simular",Map.of("version",version(),"total","10.00","carillas",1.5)),400);
        status(post(admin,ruta()+"/simular",Map.of("version",version()-1,"total","10.00","carillas",1)),409);
        modelo("CONDICIONAL","SENA");
        status(put(admin,ruta(),comando(pagos(false,null,null,null,null,null,true))),400);
        status(put(admin,ruta(),comando(pagos(true,"DESDE_CARILLAS","200","FIJA","10",null,true))),400);
        status(put(admin,ruta(),comando(pagos(true,"SIEMPRE",null,"FIJA","10",null,false))),400);
        modelo("CONDICIONAL","MONTO_TOTAL");
        status(put(admin,ruta(),comando(pagos(true,"SUPERAR_UMBRAL_APROBACION",null,"FIJA","10",null,true))),400);
        status(put(admin,ruta(),comando(pagos(true,"SUPERAR_UMBRAL_APROBACION","100","FIJA","10","100",true))),400);
        modelo("CONDICIONAL","PAGO_PREVIO");var p=pagos(true,"SIEMPRE",null,"FIJA","10",null,true);p.put("medios",List.of("TRANSFERENCIA"));status(put(admin,ruta(),comando(p)),400);
    }

    @Test void idempotenciaConcurrenciaRollbackRevocacionPermisosYPersistencia() throws Exception {
        modelo("MANUAL",null);var p=pagos(true,"SIEMPRE",null,"PORCENTAJE","20",null,true);var primero=comando(p);
        var req=request(admin,"PUT",ruta(),primero);var a=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());var b=admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());
        var ra=a.get(15,TimeUnit.SECONDS);var rb=b.get(15,TimeUnit.SECONDS);status(ra,200);status(rb,200);assertThat(ra.body()).isEqualTo(rb.body());borrador=JSON.readTree(ra.body());
        assertThat(contar("configuracion_financiera")).isEqualTo(1);assertThat(contar("configuracion_medio_pago")).isEqualTo(2);
        var otroP=new HashMap<>(p);otroP.put("valorSena","40");var comandoA=comando(p);var comandoB=comando(otroP);
        var reqA=request(admin,"PUT",ruta(),comandoA);var reqB=request(admin,"PUT",ruta(),comandoB);
        a=admin.sendAsync(reqA,HttpResponse.BodyHandlers.ofString());b=admin.sendAsync(reqB,HttpResponse.BodyHandlers.ofString());
        assertThat(List.of(a.get(15,TimeUnit.SECONDS).statusCode(),b.get(15,TimeUnit.SECONDS).statusCode())).containsExactlyInAnyOrder(200,409);borrador=estado().get("borrador");
        assertThat(version()).isEqualTo(4);assertThat(JSON.readTree(put(admin,ruta(),primero).body())).isEqualTo(borrador);
        var diferente=new HashMap<>(primero);diferente.put("pagos",otroP);status(put(admin,ruta(),diferente),409);
        status(put(admin,"/api/admin/configuracion/borradores/"+UUID.randomUUID()+"/pagos",primero),409);
        var noCsrf=HttpRequest.newBuilder(uri(ruta())).header("Content-Type","application/json").PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(comando(p)))).build();
        status(admin.send(noCsrf,HttpResponse.BodyHandlers.ofString()),403);
        var publico=cliente();status(post(publico,ruta()+"/simular",Map.of("version",version(),"total","10.00","carillas",1)),401);
        status(post(publico,"/api/auth/registro",Map.of("nombre","Cliente","apellido","Pagos","correo","cliente-pagos@example.test","contrasena",CLAVE)),201);login(publico,"cliente-pagos@example.test");
        status(put(publico,ruta(),comando(p)),403);status(post(publico,ruta()+"/simular",Map.of("version",version(),"total","10.00","carillas",1)),403);
        // Fixture de una autorización pendiente: guardar pagos debe invalidar la versión revisada.
        UUID autorizacion=UUID.randomUUID();jdbc.update("""
                INSERT INTO lamontana.autorizacion_configuracion(id_operacion,proposito,id_configuracion_version,version_configuracion,id_actor,version_acceso,correo_destino,hash_solicitud,hash_codigo,fecha_vencimiento)
                SELECT ?,'CANCELAR_BORRADOR',c.id_configuracion_version,c.version,u.id_usuario,u.version_acceso,u.correo,?, ?,clock_timestamp()+interval '15 minutes'
                FROM lamontana.configuracion_version c JOIN lamontana.usuario u ON u.id_usuario=c.id_usuario_creador WHERE c.codigo_publico=?
                """,autorizacion,"a".repeat(64),"b".repeat(64),UUID.fromString(id));
        var siguiente=comando(otroP);var tipado=JSON.readValue(JSON.writeValueAsString(siguiente),ConfiguracionController.GuardarPagos.class);
        jdbc.execute("CREATE FUNCTION lamontana.fallar_pagos_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.tipo='PAGOS_CONFIGURADOS' THEN RAISE EXCEPTION 'fallo transaccional de prueba'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER fallo_pagos_test BEFORE INSERT ON lamontana.evento_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.fallar_pagos_test()");
        assertThatThrownBy(()->app.getBean(PagosConfiguracionService.class).guardar(UUID.fromString(id),tipado,EMAIL)).isInstanceOf(DataAccessException.class);
        assertThat(estado().get("borrador")).isEqualTo(borrador);assertThat(contar("comprobante_configuracion")).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT fecha_revocacion IS NULL FROM lamontana.autorizacion_configuracion WHERE id_operacion=?",Boolean.class,autorizacion)).isTrue();
        jdbc.execute("DROP TRIGGER fallo_pagos_test ON lamontana.evento_configuracion");jdbc.execute("DROP FUNCTION lamontana.fallar_pagos_test()");
        var guardado=put(admin,ruta(),siguiente);status(guardado,200);borrador=JSON.readTree(guardado.body());
        assertThat(jdbc.queryForObject("SELECT fecha_revocacion IS NOT NULL FROM lamontana.autorizacion_configuracion WHERE id_operacion=?",Boolean.class,autorizacion)).isTrue();
        var pagosAntes=borrador.get("pagos");modelo("MANUAL",null);assertThat(borrador.get("pagos")).isEqualTo(pagosAntes);
        String antes=estado().toString();app.close();arrancar();assertThat(estado().toString()).isEqualTo(antes);
        modelo("CONDICIONAL","SENA");assertThat(borrador.get("pagos").isNull()).isTrue();assertThat(contar("configuracion_financiera")).isZero();assertThat(contar("configuracion_medio_pago")).isZero();
        assertThat(JSON.readTree(put(admin,ruta(),primero).body())).isEqualTo(borrador);assertThat(contar("configuracion_financiera")).isZero();
    }

    private Map<String,Object> pagos(boolean sena,String condicion,String umbral,String tipo,String valor,String aprobacion,boolean transferencia){
        var p=new HashMap<String,Object>();p.put("medios",transferencia?List.of("EFECTIVO","TRANSFERENCIA"):List.of("EFECTIVO"));p.put("instruccionesTransferencia",transferencia?"Transferir a la cuenta indicada por la imprenta y presentar comprobante.":null);
        p.put("vigenciaCotizacionMinutos",30);p.put("exigirSena",sena);p.put("condicionSena",condicion);p.put("umbralSena",umbral);p.put("tipoSena",tipo);p.put("valorSena",valor);p.put("umbralAprobacion",aprobacion);return p;
    }
    private void modelo(String modelo,String criterio)throws Exception{var datos=new HashMap<String,Object>();datos.put("operacion",UUID.randomUUID().toString());datos.put("version",version());datos.put("modelo",modelo);datos.put("criterio",criterio);var r=put(admin,"/api/admin/configuracion/borradores/"+id+"/modelo",datos);status(r,200);borrador=JSON.readTree(r.body());}
    private Map<String,Object> comando(Map<String,Object> pagos){return new HashMap<>(Map.of("operacion",UUID.randomUUID().toString(),"version",version(),"pagos",pagos));}
    private void guardar(Map<String,Object> pagos)throws Exception{var r=put(admin,ruta(),comando(pagos));status(r,200);borrador=JSON.readTree(r.body());}
    private JsonNode simular(String total,int carillas)throws Exception{var r=post(admin,ruta()+"/simular",Map.of("version",version(),"total",total,"carillas",carillas));status(r,200);return JSON.readTree(r.body());}
    private void assertSim(JsonNode s,boolean humana,boolean carga,String previo,String sena,String saldo,String momento){assertThat(s.get("revisionHumana").asBoolean()).isEqualTo(humana);assertThat(s.get("cargaRequiereAcreditacion").asBoolean()).isEqualTo(carga);assertThat(s.get("pagoPrevioRequerido").asString()).isEqualTo(previo);assertThat(s.get("senaRequerida").asString()).isEqualTo(sena);assertThat(s.get("saldo").asString()).isEqualTo(saldo);assertThat(s.get("momento").asString()).isEqualTo(momento);}
    private long version(){return borrador.get("version").asLong();}
    private String ruta(){return "/api/admin/configuracion/borradores/"+id+"/pagos";}
    private int contar(String tabla){return jdbc.queryForObject("SELECT count(*) FROM lamontana."+tabla,Integer.class);}
    private JsonNode estado()throws Exception{var r=get(admin,"/api/admin/configuracion");status(r,200);return JSON.readTree(r.body());}
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
