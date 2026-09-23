//#region ENCABEZADO · OrganizacionIntegrationTest.java
/*
 * ========================================================================
 * ARCHIVO: OrganizacionIntegrationTest.java
 * ========================================================================
 * FUNCIÓN
 * Comprueba empleados, permisos, asignación y aislamiento por sucursal, bajas y edición
 * concurrente de la organización.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] void empleadosPermisosAislamientoBajasYEdicionConcurrente() throws Exception
 *   Caso de prueba.
 * - [private] HttpClient cliente()
 * - [private] Map<String, Object> sucursal(String codigo)
 * - [private] Map<String, Object> alta(String nombre, List<String> sucursales, List<String>
 *   permisos)
 * - [private] Map<String, Object> editarEmpleado(HttpClient admin, String id) throws Exception
 * - [private] Map<String, Object> editarSucursal(HttpClient admin, String id) throws Exception
 * - [private] Map<String, Object> editable(HttpClient admin, String path, String id) throws
 *   Exception
 * - [private] void loginEmpleado(HttpClient client, String correo) throws Exception
 * - [private] void login(HttpClient client, String correo, int expected) throws Exception
 * - [private] void login(HttpClient client, String correo, int expected, String clave) throws
 *   Exception
 * - [private] URI uri(String path)
 * - [private] HttpResponse<String> get(HttpClient client, String path) throws Exception
 * - [private] String csrf(HttpClient client) throws Exception
 * - [private] HttpRequest request(HttpClient client, String method, String path, Object body)
 *   throws Exception
 * - [private] HttpResponse<String> enviar(HttpClient client, String method, String path, Object
 *   body) throws Exception
 * - [private] void assertStatus(HttpResponse<String> r, int status)
 * - [private] String creado(HttpResponse<String> r)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - OrganizacionIntegrationTest (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import ar.com.lamontana.organizacion.OrganizacionService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

class OrganizacionIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String CLAVE = "PruebasOrganizacion123!";
    private static final String TOKEN = "token-de-organizacion-exclusivo-de-pruebas";
    private int port;

    @Test
    void empleadosPermisosAislamientoBajasYEdicionConcurrente() throws Exception {
        var files = Files.createTempDirectory("lamontana-organizacion-");
        try (var pg = EmbeddedPostgres.builder().start();
             var app = new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0",
                     "--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"), "--spring.datasource.username=postgres",
                     "--spring.datasource.password=", "--lamontana.archivos.directorio="+files, "--lamontana.instalacion.token="+TOKEN)) {
            port = Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));
            var admin = cliente();
            assertStatus(enviar(admin,"POST","/api/setup/propietario",Map.of("nombre","Admin","apellido","Test","correo","admin@example.test","contrasena",CLAVE,"token",TOKEN)),201);
            login(admin,"admin@example.test",200);
            String a = creado(enviar(admin,"POST","/api/admin/sucursales",sucursal("A")));
            String b = creado(enviar(admin,"POST","/api/admin/sucursales",sucursal("B")));
            assertStatus(enviar(admin,"POST","/api/admin/empleados",alta("alice",List.of(),List.of())),400);
            assertStatus(enviar(admin,"POST","/api/admin/empleados",alta("alice",List.of(UUID.randomUUID().toString()),List.of())),400);
            assertStatus(enviar(admin,"POST","/api/admin/empleados",alta("alice",List.of(a),List.of("ADMIN_ADMIN"))),400);
            assertStatus(enviar(admin,"POST","/api/admin/empleados",alta("alice",List.of(a,a),List.of())),400);
            String aliceId = creado(enviar(admin,"POST","/api/admin/empleados",alta("alice",List.of(a),List.of("ACREDITAR_PAGO"))));
            String bobId = creado(enviar(admin,"POST","/api/admin/empleados",alta("bob",List.of(b),List.of())));
            assertStatus(enviar(admin,"POST","/api/admin/empleados",alta("ALICE",List.of(b),List.of())),409);
            var alice = cliente(); var bob = cliente();
            loginEmpleado(alice,"ALICE@example.test"); loginEmpleado(bob,"bob@example.test");
            assertThat(get(alice,"/api/auth/me").body()).contains("EMPLEADO").doesNotContain(CLAVE,"hash_contrasena");
            assertThat(get(alice,"/api/operacion/contexto").body()).contains(a,"ACREDITAR_PAGO").doesNotContain(b);
            assertThat(get(bob,"/api/operacion/contexto").body()).contains(b).doesNotContain(a,"ACREDITAR_PAGO");
            assertStatus(get(alice,"/api/operacion/sucursales/"+a),200);
            assertStatus(get(alice,"/api/operacion/sucursales/"+b),403);
            assertStatus(get(alice,"/api/admin/empleados"),403);
            assertStatus(get(alice,"/api/admin/sucursales"),403);
            assertStatus(enviar(alice,"POST","/api/admin/empleados",alta("no",List.of(a),List.of())),403);
            assertStatus(enviar(alice,"PUT","/api/admin/empleados/"+aliceId,editarEmpleado(admin,aliceId)),403);
            // Nueva alta: horario explícito, provincia argentina y zona resuelta en servidor.
            assertStatus(get(alice,"/api/admin/sucursales/ubicaciones"),403);
            var ubicaciones=get(admin,"/api/admin/sucursales/ubicaciones");assertStatus(ubicaciones,200);
            assertThat(JSON.readTree(ubicaciones.body()).size()).isEqualTo(24);
            assertThat(ubicaciones.body()).contains("GMT-03:00","Ciudad Autónoma de Buenos Aires");
            var nueva=sucursal("HORARIO");nueva.put("provincia","CABA");nueva.put("localidad","Ciudad Autónoma de Buenos Aires");
            nueva.put("zonaHoraria","Asia/Tokyo"); // No puede alterar la detección automática.
            var semana=new ArrayList<Map<String,Object>>();
            for(int dia=1;dia<=7;dia++){var fila=new HashMap<String,Object>();fila.put("dia",dia);fila.put("habilitado",dia<=5);fila.put("apertura",dia<=5?"09:00":null);fila.put("cierre",dia<=5?"18:00":null);semana.add(fila);}
            nueva.put("horarioAtencion",semana);
            String horarioId=creado(enviar(admin,"POST","/api/admin/sucursales",nueva));
            var guardada=editarSucursal(admin,horarioId);
            assertThat(guardada.get("zonaHoraria")).isEqualTo("America/Argentina/Buenos_Aires");
            assertThat(guardada.get("horarioAtencion")).isEqualTo(semana);
            var invalida=new HashMap<>(nueva);invalida.put("codigo","INVALIDA");invalida.put("provincia","Provincia desconocida");
            assertStatus(enviar(admin,"POST","/api/admin/sucursales",invalida),400);
            invalida.put("provincia","CABA");invalida.put("horarioAtencion",List.of());
            assertStatus(enviar(admin,"POST","/api/admin/sucursales",invalida),400);
            var incompleta=new ArrayList<>(semana);incompleta.set(0,Map.of("dia",1,"habilitado",true,"apertura","18:00","cierre","09:00"));
            guardada.put("horarioAtencion",incompleta);
            assertStatus(enviar(admin,"PUT","/api/admin/sucursales/"+horarioId,guardada),400);
            assertThat(editarSucursal(admin,horarioId).get("horarioAtencion")).isEqualTo(semana);
            guardada=editarSucursal(admin,horarioId);guardada.remove("zonaHoraria");guardada.put("nombre","Horario actualizado");
            assertStatus(enviar(admin,"PUT","/api/admin/sucursales/"+horarioId,guardada),200);
            assertStatus(enviar(admin,"PUT","/api/admin/sucursales/"+horarioId,guardada),409);
            assertThat(editarSucursal(admin,horarioId).get("horarioAtencion")).isEqualTo(semana);
            assertStatus(enviar(alice,"POST","/api/admin/sucursales",nueva),403);

            var org = app.getBean(OrganizacionService.class);
            org.exigirPermiso("alice@example.test","ACREDITAR_PAGO");
            assertThatThrownBy(()->org.exigirPermiso("bob@example.test","ACREDITAR_PAGO")).isInstanceOf(ResponseStatusException.class);

            var bajaA = editarSucursal(admin,a); bajaA.put("estado","DESACTIVADA");
            assertStatus(enviar(admin,"PUT","/api/admin/sucursales/"+a,bajaA),409);
            var editAlice = editarEmpleado(admin,aliceId);
            var sinAsignacion = new HashMap<>(editAlice); sinAsignacion.put("sucursales",List.of());
            assertStatus(enviar(admin,"PUT","/api/admin/empleados/"+aliceId,sinAsignacion),400);
            editAlice.put("sucursales",List.of(b)); editAlice.put("permisos",List.of());
            assertStatus(enviar(admin,"PUT","/api/admin/empleados/"+aliceId,editAlice),200);
            // Se usa la misma cookie: asignaciones y permisos se consultan con su vigencia actual.
            assertStatus(get(alice,"/api/operacion/sucursales/"+a),403);
            assertStatus(get(alice,"/api/operacion/sucursales/"+b),200);
            assertThat(get(alice,"/api/operacion/contexto").body()).doesNotContain("ACREDITAR_PAGO");
            assertThatThrownBy(()->org.exigirPermiso("alice@example.test","ACREDITAR_PAGO")).isInstanceOf(ResponseStatusException.class);
            assertStatus(enviar(admin,"PUT","/api/admin/empleados/"+aliceId,editAlice),409);
            var jdbc = app.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario_permiso WHERE fecha_revocacion IS NOT NULL",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario_sucursal WHERE NOT activa",Integer.class)).isEqualTo(1);

            // Dos ediciones del mismo formulario: sólo una puede guardar la versión leída.
            var editA = editarSucursal(admin,a); editA.put("nombre","Sucursal editada");
            var req = request(admin,"PUT","/api/admin/sucursales/"+a,editA);
            var r1 = admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());
            var r2 = admin.sendAsync(req,HttpResponse.BodyHandlers.ofString());
            assertThat(List.of(r1.get().statusCode(),r2.get().statusCode())).containsExactlyInAnyOrder(200,409);

            var editBob = editarEmpleado(admin,bobId); editBob.put("estado","DESACTIVADO"); editBob.put("sucursales",List.of());
            assertStatus(enviar(admin,"PUT","/api/admin/empleados/"+bobId,editBob),200);
            assertStatus(get(bob,"/api/auth/me"),401); login(cliente(),"bob@example.test",401);
            var reactivar = editarEmpleado(admin,bobId); reactivar.put("estado","ACTIVO");
            assertStatus(enviar(admin,"PUT","/api/admin/empleados/"+bobId,reactivar),400);
            editAlice = editarEmpleado(admin,aliceId); editAlice.put("sucursales",List.of(a,b));
            assertStatus(enviar(admin,"PUT","/api/admin/empleados/"+aliceId,editAlice),200);
            // Bajas simultáneas de las dos sucursales: una queda activa para la empleada.
            bajaA = editarSucursal(admin,a); bajaA.put("estado","DESACTIVADA");
            var bajaB = editarSucursal(admin,b); bajaB.put("estado","DESACTIVADA");
            var requestA=request(admin,"PUT","/api/admin/sucursales/"+a,bajaA);
            var requestB=request(admin,"PUT","/api/admin/sucursales/"+b,bajaB);
            r1=admin.sendAsync(requestA,HttpResponse.BodyHandlers.ofString());
            r2=admin.sendAsync(requestB,HttpResponse.BodyHandlers.ofString());
            assertThat(List.of(r1.get().statusCode(),r2.get().statusCode())).containsExactlyInAnyOrder(200,409);
            assertThat(JSON.readTree(get(alice,"/api/operacion/contexto").body()).get("sucursales").size()).isEqualTo(1);
            // La baja de sucursal no borra asignaciones históricas; se pueden conservar al editar si hay otra activa.
            editAlice = editarEmpleado(admin,aliceId); editAlice.put("nombre","Alice actualizada");
            assertStatus(enviar(admin,"PUT","/api/admin/empleados/"+aliceId,editAlice),200);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_organizacion",Integer.class)).isGreaterThanOrEqualTo(8);
            assertThat(get(admin,"/api/admin/empleados").body()).doesNotContain(CLAVE,"hash_contrasena");

            var publico=cliente();
            assertStatus(enviar(publico,"POST","/api/auth/registro",Map.of("nombre","Cliente","apellido","Test","correo","cliente@example.test","contrasena",CLAVE)),201);
            login(publico,"cliente@example.test",200);
            assertStatus(get(publico,"/api/operacion/contexto"),403);
            assertStatus(enviar(publico,"PUT","/api/admin/sucursales/"+a,bajaA),403);
            assertStatus(enviar(admin,"PUT","/api/admin/empleados/"+JSON.readTree(get(publico,"/api/auth/me").body()).get("codigoPublico").asString(),editarEmpleado(admin,aliceId)),404);
            var noCsrf=HttpRequest.newBuilder(uri("/api/admin/empleados/"+aliceId)).header("Content-Type","application/json").PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(editarEmpleado(admin,aliceId)))).build();
            assertStatus(admin.send(noCsrf,HttpResponse.BodyHandlers.ofString()),403);
        } finally { Files.deleteIfExists(files); }
    }

    private HttpClient cliente() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build(); }
    private Map<String,Object> sucursal(String codigo) {
        return new HashMap<>(Map.of("codigo",codigo,"nombre","Sucursal "+codigo,"calle","Calle","numero","123","localidad","Ciudad","provincia","Provincia","codigoPostal","1234","zonaHoraria","America/Argentina/Buenos_Aires"));
    }
    private Map<String,Object> alta(String nombre,List<String> sucursales,List<String> permisos) {
        return new HashMap<>(Map.of("nombre",nombre,"apellido","Prueba","correo",nombre+"@example.test","contrasena",CLAVE,"sucursales",sucursales,"permisos",permisos));
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> editarEmpleado(HttpClient admin,String id) throws Exception { return editable(admin,"/api/admin/empleados",id); }
    private Map<String,Object> editarSucursal(HttpClient admin,String id) throws Exception { return editable(admin,"/api/admin/sucursales",id); }
    @SuppressWarnings("unchecked")
    private Map<String,Object> editable(HttpClient admin,String path,String id) throws Exception {
        var response=get(admin,path); assertStatus(response,200);
        for (var node:JSON.readTree(response.body())) if(node.get("codigoPublico").asString().equals(id)) {
            Map<String,Object> data=new HashMap<>(JSON.readValue(node.toString(),Map.class)); data.remove("codigoPublico"); return data;
        }
        throw new AssertionError("Objeto no encontrado");
    }
    private void loginEmpleado(HttpClient client,String correo) throws Exception {
        login(client,correo,200);
        assertStatus(get(client,"/api/operacion/contexto"),403);
        assertStatus(enviar(client,"POST","/api/auth/contrasena",Map.of("contrasenaActual",CLAVE,"nuevaContrasena",CLAVE+"Nueva")),200);
        login(client,correo,200,CLAVE+"Nueva");
    }
    private void login(HttpClient client,String correo,int expected) throws Exception { login(client,correo,expected,CLAVE); }
    private void login(HttpClient client,String correo,int expected,String clave) throws Exception {
        String csrf=csrf(client);
        var req=HttpRequest.newBuilder(uri("/api/auth/login")).timeout(Duration.ofSeconds(15)).header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",csrf)
                .POST(HttpRequest.BodyPublishers.ofString("username="+java.net.URLEncoder.encode(correo,java.nio.charset.StandardCharsets.UTF_8)+"&password="+java.net.URLEncoder.encode(clave,java.nio.charset.StandardCharsets.UTF_8))).build();
        assertStatus(client.send(req,HttpResponse.BodyHandlers.ofString()),expected);
    }
    private URI uri(String path) { return URI.create("http://127.0.0.1:"+port+path); }
    private HttpResponse<String> get(HttpClient client,String path) throws Exception { return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).GET().build(),HttpResponse.BodyHandlers.ofString()); }
    private String csrf(HttpClient client) throws Exception { var r=get(client,"/api/auth/csrf"); assertStatus(r,200); return JSON.readTree(r.body()).get("token").asString(); }
    private HttpRequest request(HttpClient client,String method,String path,Object body) throws Exception {
        return HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf(client)).method(method,HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build();
    }
    private HttpResponse<String> enviar(HttpClient client,String method,String path,Object body) throws Exception { return client.send(request(client,method,path,body),HttpResponse.BodyHandlers.ofString()); }
    private void assertStatus(HttpResponse<String> r,int status) { assertThat(r.statusCode()).withFailMessage("HTTP %s esperado %s: %s",r.statusCode(),status,r.body()).isEqualTo(status); }
    private String creado(HttpResponse<String> r) { assertStatus(r,201); return JSON.readTree(r.body()).get("codigoPublico").asString(); }
}
