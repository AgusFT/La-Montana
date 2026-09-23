//#region ENCABEZADO · RevisionConfiguracionIntegrationTest.java
/*
 * ========================================================================
 * ARCHIVO: RevisionConfiguracionIntegrationTest.java
 * ========================================================================
 * FUNCIÓN
 * Comprueba hallazgos y simulación integral del configurador, interacción con el catálogo,
 * capacidades, precios, tiempos y mapa de preparación, además de permisos y CSRF.
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
 * - [private] JsonNode revision() throws Exception
 * - [private] Set<String> codigos(JsonNode r)
 * - [private] void preparar() throws Exception
 * - [private] Map<String, Object> territorio()
 * - [private] Map<String, Object> oferta(String id, String base, String precio, int minutos,
 *   boolean habilitada)
 * - [private] void publicar(Instant fecha, String precio, boolean term, boolean color) throws
 *   Exception
 * - [private] Map<String, Object> ejemplo(String modalidad)
 * - [private] JsonNode simular(Map<String, Object> e) throws Exception
 * - [paquete] void bloqueosVinculadosPreciosCapacidadesSucursalYAdvertencias() throws Exception
 *   Caso de prueba.
 * - [paquete] void precioDobleFazD1CostoDeEntregaTiemposYLecturasSinEscrituras() throws Exception
 *   Caso de prueba.
 * - [paquete] void revisionComercialIndependienteProgramadaYReconciliadaAunqueSimulacionFalle()
 *   throws Exception
 *   Caso de prueba.
 * - [paquete] void soloPropietarioYCsrfConErroresControlados() throws Exception
 *   Caso de prueba.
 * - [paquete] void mapaPreparacionReutilizaValidacionesYNoActivaBorrador() throws Exception
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
 * - RevisionConfiguracionIntegrationTest (clase).
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

class RevisionConfiguracionIntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String EMAIL="entrega@example.test",CLAVE="ConfiguracionEntrega123!",TOKEN="token-local-entrega-configuracion-exclusivo-test";
    private EmbeddedPostgres pg;
    private ConfigurableApplicationContext app;
    private JdbcTemplate jdbc;
    private Path archivos;
    private HttpClient admin;
    private int port;
    private String id,a,b,servicio,formato,papel,terminacion,comercial;
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


    private JsonNode revision()throws Exception{var r=get(admin,base()+"/revision");status(r,200);return JSON.readTree(r.body());}
    private Set<String> codigos(JsonNode r){var c=new HashSet<String>();for(var h:r.get("hallazgos"))c.add(h.get("codigo").asString());return c;}
    private void preparar()throws Exception{
        status(post(admin,"/api/admin/catalogo/formatos",Map.of("codigo","F","nombre","Formato","anchoMm","210","altoMm","297")),201);
        status(post(admin,"/api/admin/catalogo/papeles",Map.of("codigo","P","nombre","Papel","gramaje","80","terminacion","Mate")),201);
        status(post(admin,"/api/admin/catalogo/servicios",Map.of("codigo","TERM","nombre","Acabado","tipo","TERMINACION")),201);
        formato=jdbc.queryForObject("SELECT codigo_publico::text FROM lamontana.formato",String.class);
        papel=jdbc.queryForObject("SELECT codigo_publico::text FROM lamontana.papel",String.class);
        terminacion=jdbc.queryForObject("SELECT codigo_publico::text FROM lamontana.servicio WHERE codigo='TERM'",String.class);
        var m=comando();m.putAll(Map.of("modelo","CONDICIONAL","criterio","MONTO_TOTAL"));aceptar(put(admin,base()+"/modelo",m));
        var p=comando();p.put("pagos",Map.of("medios",List.of("TRANSFERENCIA","EFECTIVO"),"instruccionesTransferencia","Cuenta de prueba","vigenciaCotizacionMinutos",90,"exigirSena",true,"condicionSena","SUPERAR_UMBRAL_APROBACION","tipoSena","PORCENTAJE","valorSena","25","umbralAprobacion","30"));aceptar(put(admin,base()+"/pagos",p));
        var r=comando();r.put("metodoAsignacion","MANUAL");r.put("serviciosPorSucursal",List.of(Map.of("sucursal",a,"servicios",List.of(servicio,terminacion)),Map.of("sucursal",b,"servicios",List.of(servicio))));aceptar(put(admin,base()+"/recursos",r));
        for(String origen:List.of(a,b)){var i=comando();i.putAll(Map.of("sucursal",origen,"nombre","Impresora "+origen,"formatos",List.of(formato),"admiteColor",false,"admiteDobleFaz",true,"capacidadHojas",2,"estado","OPERATIVA"));aceptar(post(admin,base()+"/impresoras",i));}
        guardar("1","1",List.of("RETIRO_SUCURSAL","ENVIO_DOMICILIO"),List.of(horario(a,semana("09:00","18:00")),horario(b,semana("09:00","18:00"))));
        var z=comando();z.putAll(Map.of("codigo","CENTRO","nombre","Centro","zonaHoraria","America/Argentina/Buenos_Aires","costo","12.50","habilitada",true,"territorios",List.of(territorio()),"franjas",List.of(Map.of("dia",1,"apertura","09:00","cierre","18:00","capacidadPedidos",2,"habilitada",true))));aceptar(post(admin,ruta()+"/zonas",z));
        publicar(null,"2.00",true,false);
    }
    private Map<String,Object> territorio(){return Map.of("codigoPostal","5600","localidad","San Rafael","provincia","Mendoza");}
    private Map<String,Object> oferta(String id,String base,String precio,int minutos,boolean habilitada){return Map.of("servicio",id,"nombreVisible",id.equals(servicio)?"Impresión":"Acabado","basePrecio",base,"precio",precio,"preparacionMinutos",minutos,"habilitado",habilitada,"compatibilidades",id.equals(servicio)?List.of():List.of(Map.of("formato",formato,"papel",papel)));}
    private void publicar(Instant fecha,String precio,boolean term,boolean color)throws Exception{
        var c=new HashMap<String,Object>();c.put("operacion",UUID.randomUUID().toString());c.put("versionBase",comercial);c.put("motivo","Revisión de prueba");c.put("programadaPara",fecha==null?null:fecha.toString());
        var tarifas=new ArrayList<Map<String,Object>>();tarifas.add(Map.of("formato",formato,"papel",papel,"color","BLANCO_NEGRO","precio",precio,"recargoDobleFaz","0.50","habilitada",true));if(color)tarifas.add(Map.of("formato",formato,"papel",papel,"color","COLOR","precio","5","recargoDobleFaz","0","habilitada",true));c.put("tarifas",tarifas);c.put("servicios",List.of(oferta(servicio,"POR_CARILLA","0",30,true),oferta(terminacion,"POR_HOJA","1.25",120,term)));
        var r=post(admin,"/api/admin/catalogo/revisiones",c);status(r,200);if(fecha==null)comercial=JSON.readTree(r.body()).get("codigoPublico").asString();
    }
    private Map<String,Object> ejemplo(String modalidad){var c=new HashMap<String,Object>();c.putAll(Map.of("version",borrador.get("version").asLong(),"revisionComercial",comercial,"sucursal",a,"item",Map.of("servicio",servicio,"formato",formato,"papel",papel,"color","BLANCO_NEGRO","paginas",3,"copias",2,"dobleFaz",true,"terminaciones",List.of(terminacion)),"modalidad",modalidad,"recibidoEn","2026-09-28T12:00:00Z"));if(modalidad.equals("ENVIO_DOMICILIO"))c.put("territorio",territorio());return c;}
    private JsonNode simular(Map<String,Object> e)throws Exception{var r=post(admin,base()+"/revision/simular",e);status(r,200);return JSON.readTree(r.body());}
    @Test void bloqueosVinculadosPreciosCapacidadesSucursalYAdvertencias()throws Exception{
        var incompleta=revision();assertThat(incompleta.get("bloqueos").asInt()).isGreaterThan(0);assertThat(codigos(incompleta)).contains("PAGOS_PENDIENTES","SIN_IMPRESORA_OPERATIVA","CATALOGO_PENDIENTE");assertThat(incompleta.get("fase5Valida").asBoolean()).isFalse();
        preparar();var lista=revision();assertThat(lista.get("bloqueos").asInt()).isZero();assertThat(lista.get("opciones").size()).isEqualTo(2);assertThat(lista.get("fase5Valida").asBoolean()).isTrue();
        publicar(null,"2",false,false);assertThat(codigos(revision())).contains("SERVICIO_SIN_PRECIO_VIGENTE");status(post(admin,base()+"/revision/simular",ejemplo("RETIRO_SUCURSAL")),409);
        publicar(null,"2",true,true);assertThat(revision().get("bloqueos").asInt()).isZero();assertThat(codigos(revision())).contains("TARIFAS_SIN_ORIGEN");
        JsonNode printer=borrador.get("recursos").get("impresoras").get(1);var off=comando();off.putAll(Map.of("estado","DESHABILITADA","motivo","Mantenimiento de prueba"));aceptar(post(admin,base()+"/impresoras/"+printer.get("codigoPublico").asString()+"/estado",off));assertThat(codigos(revision())).contains("SERVICIO_SIN_CAPACIDAD");
    }
    @Test void precioDobleFazD1CostoDeEntregaTiemposYLecturasSinEscrituras()throws Exception{
        preparar();String antes=estado().toString();int eventos=contar("evento_configuracion");
        var local=simular(ejemplo("RETIRO_SUCURSAL"));assertThat(local.get("precio").get("carillas").asInt()).isEqualTo(6);assertThat(local.get("precio").get("hojas").asInt()).isEqualTo(4);assertThat(local.get("precio").get("subtotal").asString()).isEqualTo("20.00");assertThat(local.get("finanzas").get("revisionHumana").asBoolean()).isFalse();assertThat(local.get("recorrido").toString()).contains("Listo para entregar en local; no pasa por En viaje");assertInstante(local.get("entrega"),"finPreparacion","2026-09-28T14:00:00Z");
        var domicilio=simular(ejemplo("ENVIO_DOMICILIO"));assertThat(domicilio.get("total").asString()).isEqualTo("32.50");assertThat(domicilio.get("finanzas").get("senaRequerida").asString()).isEqualTo("8.13");assertThat(domicilio.get("finanzas").get("revisionHumana").asBoolean()).isTrue();assertThat(domicilio.get("finanzas").get("cargaRequiereAcreditacion").asBoolean()).isFalse();assertThat(domicilio.get("finanzas").get("momento").asString()).isEqualTo("DESPUES_APROBACION_ANTES_PRODUCCION");assertInstante(domicilio.get("entrega"),"disponibleDesde","2026-09-28T15:00:00Z");
        assertThat(estado().toString()).isEqualTo(antes);assertThat(contar("evento_configuracion")).isEqualTo(eventos);
        app.close();arrancar();assertThat(simular(ejemplo("ENVIO_DOMICILIO"))).isEqualTo(domicilio);
        var malo=ejemplo("RETIRO_SUCURSAL");malo.put("sucursal",b);status(post(admin,base()+"/revision/simular",malo),409);
        var item=new HashMap<String,Object>(JSON.readValue(JSON.writeValueAsString(ejemplo("RETIRO_SUCURSAL").get("item")),Map.class));item.put("terminaciones",List.of(terminacion,terminacion));malo=ejemplo("RETIRO_SUCURSAL");malo.put("item",item);status(post(admin,base()+"/revision/simular",malo),400);item.put("paginas",0);status(post(admin,base()+"/revision/simular",malo),400);
    }
    @Test void revisionComercialIndependienteProgramadaYReconciliadaAunqueSimulacionFalle()throws Exception{
        preparar();var previo=ejemplo("RETIRO_SUCURSAL");String antes=estado().toString();publicar(Instant.now().plusSeconds(3600),"3",true,false);assertThat(revision().get("catalogo").get("actual").get("codigoPublico").asString()).isEqualTo(comercial);assertThat(codigos(revision())).contains("CATALOGO_PROGRAMADO");assertThat(simular(previo).get("total").asString()).isEqualTo("20.00");
        jdbc.update("UPDATE lamontana.catalogo_revision SET creada_en=clock_timestamp()-interval '2 hours',programada_para=clock_timestamp()-interval '1 hour' WHERE estado='PROGRAMADA'");status(post(admin,base()+"/revision/simular",previo),409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.catalogo_revision WHERE estado='PROGRAMADA'",Integer.class)).isZero();var nuevo=revision().get("catalogo").get("actual");assertThat(nuevo.get("codigoPublico").asString()).isNotEqualTo(comercial);comercial=nuevo.get("codigoPublico").asString();assertThat(simular(ejemplo("RETIRO_SUCURSAL")).get("total").asString()).isEqualTo("26.00");assertThat(estado().toString()).isEqualTo(antes);
        var v=ejemplo("RETIRO_SUCURSAL");v.put("version",1);status(post(admin,base()+"/revision/simular",v),409);
        jdbc.update("UPDATE lamontana.catalogo_revision SET vigente=false,estado='HISTORICA' WHERE vigente");comercial=null;publicar(Instant.now().plusSeconds(3600),"4",true,false);assertThat(codigos(revision())).contains("CATALOGO_PENDIENTE");
    }
    @Test void soloPropietarioYCsrfConErroresControlados()throws Exception{
        preparar();var e=ejemplo("RETIRO_SUCURSAL");String review=base()+"/revision";status(get(admin,"/api/admin/configuracion/borradores/"+UUID.randomUUID()+"/revision"),404);
        var sin=HttpRequest.newBuilder(uri(review+"/simular")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(e))).build();status(admin.send(sin,HttpResponse.BodyHandlers.ofString()),403);
        var anon=cliente();status(get(anon,review),401);status(post(anon,review+"/simular",e),401);
        for(String rol:List.of("CLIENTE","EMPLEADO","ADMIN_ADMIN")){String correo=rol.toLowerCase()+"-review@example.test";jdbc.update("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado) SELECT ?,r.id_rol,?,u.hash_contrasena,'Interno','Test','ACTIVO' FROM lamontana.usuario u CROSS JOIN lamontana.rol r WHERE u.correo=? AND r.codigo=?",UUID.randomUUID(),correo,EMAIL,rol);var c=cliente();login(c,correo);status(get(c,review),403);status(post(c,review+"/simular",e),403);}
    }

    @Test void mapaPreparacionReutilizaValidacionesYNoActivaBorrador()throws Exception{
        var incompleta=get(admin,"/api/admin/preparacion");status(incompleta,200);
        var antes=JSON.readTree(incompleta.body());assertThat(antes.get("activa").asBoolean()).isFalse();
        assertThat(antes.get("pasos").get(3).get("completo").asBoolean()).isFalse();
        preparar();String configuracionAntes=estado().toString();int eventos=contar("evento_configuracion");
        var completa=JSON.readTree(get(admin,"/api/admin/preparacion").body());
        for(int n=0;n<6;n++)assertThat(completa.get("pasos").get(n).get("completo").asBoolean()).isTrue();
        assertThat(completa.get("pasos").get(3).get("estado").asString()).isEqualTo("Configurada en borrador");
        assertThat(completa.get("pasos").get(6).get("completo").asBoolean()).isFalse();
        assertThat(estado().toString()).isEqualTo(configuracionAntes);assertThat(contar("evento_configuracion")).isEqualTo(eventos);
        publicar(null,"2",false,false);
        var incompatible=JSON.readTree(get(admin,"/api/admin/preparacion").body());
        assertThat(incompatible.get("pasos").get(2).get("completo").asBoolean()).isFalse();
        assertThat(incompatible.get("pasos").get(2).get("detalle").asString()).contains("precio vigente");
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
