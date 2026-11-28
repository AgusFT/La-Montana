package ar.com.lamontana;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.lamontana.configuracion.CopiaConfiguracionController;
import ar.com.lamontana.configuracion.CopiaConfiguracionService;
import ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad;
import ar.com.lamontana.cotizaciones.CotizacionController;
import ar.com.lamontana.pedidos.PedidoService;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

class SesionDemoIntegrationTest {
    private static final String SELECTOR = "/api/demo/sesion";
    private static final String ME = "/api/auth/me";
    private static final String ADMIN = "4d9fdf39-1f90-4d08-b7b6-0c77fc9f0071";
    private static final String EMPLEADO = "4d9fdf39-1f90-4d08-b7b6-0c77fc9f0072";
    private static final String CLIENTE = "4d9fdf39-1f90-4d08-b7b6-0c77fc9f0073";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static EmbeddedPostgres postgres;
    private static ConfigurableApplicationContext app;
    private static String baseUrl;

    @BeforeAll static void iniciar(@TempDir Path archivos) throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        app = new SpringApplicationBuilder(LaMontanaApplication.class).run(
                "--server.port=0", "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=",
                "--spring.flyway.target=latest", "--lamontana.archivos.directorio=" + archivos);
        baseUrl = "http://127.0.0.1:" + app.getEnvironment().getRequiredProperty("local.server.port");
    }

    @AfterAll static void cerrar() throws Exception {
        try {
            if (app != null) app.close();
        } finally {
            if (postgres != null) postgres.close();
        }
    }

    @BeforeEach void limpiarSesionesYAvancesDeEnsayo() {
        var db=app.getBean(JdbcTemplate.class);
        db.update("DELETE FROM lamontana.sesion_http");
        db.update("DELETE FROM lamontana.tutorial_borrador");
        db.update("UPDATE lamontana.tutorial_demo SET intento=DEFAULT,estado=DEFAULT,paso=DEFAULT,inicio=NULL,preparacion=NULL,tarifa_revisada=DEFAULT");
        db.update("UPDATE lamontana.tutorial_recorrido SET intento=DEFAULT,leccion=DEFAULT,tarea=DEFAULT,alcanzada=DEFAULT,completo=DEFAULT,borrador=DEFAULT");
    }

    @Test void seleccionaIdentidadesFijasRotaSesionYCambiaPermisosHastaCerrar() throws Exception {
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var navegador = HttpClient.newBuilder().cookieHandler(cookies).build();
        var correo = "visitante@example.test";
        var clave = "ClaveVisitanteSoloPrueba123!";
        String registro = """
                {"nombre":"Visitante","apellido":"Prueba","correo":"%s","contrasena":"%s"}
                """.formatted(correo, clave);
        comprobar(post(navegador, "/api/auth/registro", registro, csrf(navegador)), 201);
        comprobar(enviar(navegador, request("/api/auth/login")
                .header("Content-Type", "application/x-www-form-urlencoded").header("X-CSRF-TOKEN", csrf(navegador))
                .POST(HttpRequest.BodyPublishers.ofString("username=visitante%40example.test&password=" + clave))), 200);
        assertThat(comprobar(get(navegador, ME), 200)).contains(correo);

        List<Caso> roles = List.of(
                new Caso("ADMINISTRADOR", "ADMIN_ADMIN", ADMIN, 200, 200, 403),
                new Caso("EMPLEADO", "EMPLEADO", EMPLEADO, 403, 200, 403),
                new Caso("CLIENTE", "CLIENTE", CLIENTE, 403, 403, 200));
        for (Caso caso : roles) {
            String cookieAnterior = cookie(cookies);
            String tokenAnterior = csrf(navegador);
            assertThat(comprobar(elegir(navegador, caso.seleccion(), tokenAnterior), 200)).contains("Sesión iniciada.");
            assertThat(cookie(cookies)).isNotEqualTo(cookieAnterior);
            comprobar(reproducirCookie(cookieAnterior), 401);
            comprobar(elegir(navegador, caso.seleccion(), tokenAnterior), 403);

            var perfil = JSON.readTree(comprobar(get(navegador, ME), 200));
            assertThat(perfil.get("rol").asString()).isEqualTo(caso.rol());
            assertThat(perfil.get("codigoPublico").asString()).isEqualTo(caso.codigo());
            assertThat(perfil.get("correo").asString()).isNotEqualTo(correo);
            assertThat(app.getBean(JdbcTemplate.class).queryForObject(
                    "SELECT count(*) FROM lamontana.sesion_http WHERE principal_name=?",
                    Integer.class, perfil.get("correo").asString())).isEqualTo(1);

            comprobar(get(navegador, "/api/admin/estado"), caso.estadoAdmin());
            comprobar(get(navegador, "/api/operacion/contexto"), caso.estadoOperacion());
            comprobar(get(navegador, "/api/cliente/estado"), caso.estadoCliente());
        }

        String cookieCliente = cookie(cookies);
        comprobar(post(navegador, "/api/auth/logout", "", null), 403);
        comprobar(post(navegador, "/api/auth/logout", "", csrf(navegador)), 204);
        comprobar(get(navegador, ME), 401);
        comprobar(reproducirCookie(cookieCliente), 401);
    }

    @Test void rechazaCsrfInvalidoRolesFueraDelSelectorYAccesosInactivos() throws Exception {
        var navegador = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        comprobar(elegir(navegador, "ADMINISTRADOR", null), 403);
        comprobar(elegir(navegador, "ADMINISTRADOR", "token-invalido"), 403);
        String token = csrf(navegador);
        comprobar(get(navegador, SELECTOR), 401);
        comprobar(elegir(navegador, "ADMIN_ADMIN", token), 400);
        comprobar(elegir(navegador, ADMIN, token), 400);
        comprobar(post(navegador, SELECTOR, "{}", token), 400);
        comprobar(get(navegador, ME), 401);

        var jdbc = app.getBean(JdbcTemplate.class);
        int desactivado = jdbc.update("UPDATE lamontana.usuario SET estado='DESACTIVADO' WHERE codigo_publico=?::uuid", CLIENTE);
        assertThat(desactivado).isEqualTo(1);
        try {
            comprobar(elegir(navegador, "CLIENTE", token), 403);
            comprobar(get(navegador, ME), 401);
        } finally {
            jdbc.update("UPDATE lamontana.usuario SET estado='ACTIVO' WHERE codigo_publico=?::uuid", CLIENTE);
        }

        jdbc.update("UPDATE lamontana.rol SET activo=false WHERE codigo='CLIENTE'");
        try {
            comprobar(elegir(navegador, "CLIENTE", token), 403);
        } finally {
            jdbc.update("UPDATE lamontana.rol SET activo=true WHERE codigo='CLIENTE'");
        }
    }

    @Test void elServidorConservaLasRutasDeTrabajoYExcluyeLasDeLosModulosDePresentacion() throws Exception {
        var mappings = app.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class)
                .getHandlerMethods().keySet().stream().flatMap(m -> m.getPatternValues().stream()).toList();
        assertThat(mappings).contains("/api/operacion/contexto", "/api/operacion/sucursales/{codigo}",
                "/api/demo/clientes", "/api/demo/sesion", "/api/admin/configuracion/borradores/{borrador}/entrega");
        assertThat(mappings).noneMatch(ruta -> ruta.startsWith("/fixture/") || ruta.contains("/empleados")
                || ruta.contains("/usuarios") || ruta.contains("/permisos") || ruta.contains("/puntos-entrega")
                || ruta.contains("/entrega/puntos") || ruta.contains("/riders"));

        var navegador = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        comprobar(elegir(navegador, "ADMINISTRADOR", csrf(navegador)), 200);
        var token = csrf(navegador);
        String id = UUID.randomUUID().toString();
        String puntos = "/api/admin/configuracion/borradores/" + id + "/entrega/puntos";
        var excluidas = Map.of(
                "GET", List.of("/api/admin/empleados", "/api/admin/puntos-entrega/disponibilidad", "/api/admin/riders"),
                "POST", List.of("/api/admin/empleados", puntos),
                "PUT", List.of("/api/admin/empleados/" + id, "/api/admin/puntos-entrega/disponibilidad/" + id,
                        puntos + "/" + id));
        for (var verbo : excluidas.entrySet()) {
            for (String ruta : verbo.getValue()) {
                var intento = request(ruta).header("X-CSRF-TOKEN", token).method(verbo.getKey(), HttpRequest.BodyPublishers.noBody());
                comprobar(enviar(navegador, intento), 403);
            }
        }
    }

    @Test void losPuntosMovilesNoSeOfrecenNiSeActivanDesdeLosFormulariosCompartidos() throws Exception {
        var navegador = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        comprobar(elegir(navegador, "CLIENTE", csrf(navegador)), 200);
        var opciones = JSON.readTree(comprobar(get(navegador, "/api/cliente/cotizaciones/opciones"), 200));
        assertThat(opciones.get("modalidades").valueStream().map(n -> n.asString()))
                .contains("RETIRO_SUCURSAL").doesNotContain("RETIRO_PUNTO_ENTREGA");
        assertThat(opciones.get("puntos").size()).isZero();

        var muestra = app.getBean(PedidoService.class).detalle(
                UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0078"), "cliente@demo.invalid", false).oferta();
        var item = muestra.items().get(0);
        var solicitud = new CotizacionController.Crear(UUID.randomUUID(), muestra.configuracion(), muestra.revisionComercial(),
                muestra.sucursal().codigoPublico(), List.of(new CotizacionController.Item(item.documento(), item.trabajo())),
                Modalidad.RETIRO_PUNTO_ENTREGA, UUID.randomUUID(), null, muestra.medioPago(), null);
        assertThat(comprobar(post(navegador, "/api/cliente/cotizaciones", JSON.writeValueAsString(solicitud), csrf(navegador)), 409))
                .contains("puntos de entrega móviles");

        comprobar(elegir(navegador, "ADMINISTRADOR", csrf(navegador)), 200);
        var borrador = app.getBean(CopiaConfiguracionService.class).copiar(muestra.configuracion(),
                new CopiaConfiguracionController.Crear(UUID.randomUUID()), "administracion@demo.invalid");
        var cuerpo = Map.of("operacion",UUID.randomUUID(), "version",borrador.version(), "preparacionHoras","1",
                "trasladoHoras","1", "modalidades",List.of("RETIRO_PUNTO_ENTREGA"), "horariosPorSucursal",List.of(),
                "matrizConfirmada",false);
        var guardar = request("/api/admin/configuracion/borradores/" + borrador.codigoPublico() + "/entrega")
                .header("Content-Type", "application/json").header("X-CSRF-TOKEN", csrf(navegador))
                .PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(cuerpo)));
        assertThat(comprobar(enviar(navegador, guardar),409)).contains("puntos de entrega móviles");
    }

    @Test void tutorialComparteElPedidoYProtegeLosBorradoresPorRol() throws Exception {
        var navegador = HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String ruta = "/api/demo/tutorial";
        assertThat(get(navegador,ruta).statusCode()).isIn(401,403);
        comprobar(elegir(navegador,"CLIENTE",csrf(navegador)),200);
        var preparacion=JSON.readTree(comprobar(post(navegador,"/api/cliente/preparaciones","{}",csrf(navegador)),200)).get("codigoPublico").asString();
        var db=app.getBean(JdbcTemplate.class);
        var muestra=db.queryForObject("SELECT codigo_publico FROM lamontana.preparacion_cliente WHERE estado='CONFIRMADA' ORDER BY id_preparacion LIMIT 1",UUID.class);
        UUID pdfPrevio = copiarPdfValidado(db, muestra, UUID.fromString(preparacion));

        comprobar(elegir(navegador,"ADMINISTRADOR",csrf(navegador)),200);
        assertThat(comprobar(get(navegador,ruta),200)).contains("NUEVO");
        comprobar(cambiarTutorial(navegador, Map.of("accion","iniciar")),200);
        assertThat(JSON.readTree(comprobar(cambiarTutorial(navegador, Map.of("accion","ir","paso","calidad")),200))
                .get("paso").asString()).isEqualTo("tarifa");
        assertThat(comprobar(cambiarTutorial(navegador,Map.of("accion","tarifa")),200)).contains("\"completados\":[\"tarifa\"]");
        comprobar(elegir(navegador,"CLIENTE",csrf(navegador)),200);
        comprobar(cambiarTutorial(navegador, Map.of("accion","vincular","preparacion",muestra)),409);
        assertThat(comprobar(cambiarTutorial(navegador, Map.of("accion","vincular","preparacion",preparacion)),200))
                .contains("\"completados\":[\"tarifa\",\"archivo\"]");
        String quitar = "/api/cliente/preparaciones/" + preparacion + "/archivos/" + pdfPrevio + "/quitar";
        comprobar(post(navegador, quitar, JSON.writeValueAsString(Map.of("operacion", UUID.randomUUID())), csrf(navegador)),200);
        assertThat(comprobar(get(navegador, ruta),200)).contains("\"completados\":[\"tarifa\"]");
        comprobar(cambiarTutorial(navegador, Map.of("accion","borrador","paso","presupuesto","campos",Map.of("calle","Senderos","numero","23"))),200);
        comprobar(cambiarTutorial(navegador, Map.of("accion","borrador","paso","presupuesto","campos",Map.of("numero",""))),200);
        comprobar(cambiarTutorial(navegador, Map.of("accion","borrador","paso","confirmacion","campos",Map.of("contrasena","privado"))),400);
        comprobar(cambiarTutorial(navegador, Map.of("accion","borrador","paso","archivo","campos",Map.of("opciones","[{\"clave\":\"dato ajeno\"}]"))),400);
        comprobar(cambiarTutorial(navegador, Map.of("accion","borrador","paso","cobro","campos",Map.of("importe","2"))),403);
        comprobar(cambiarTutorial(navegador, Map.of("accion","pausar")),200);
        var otra=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        comprobar(elegir(otra,"EMPLEADO",csrf(otra)),200);
        String lectura=comprobar(get(otra,ruta),200);
        assertThat(lectura).contains("PAUSADO",preparacion).doesNotContain("Senderos");
        comprobar(cambiarTutorial(otra, Map.of("accion","retomar")),200);
        comprobar(elegir(otra,"CLIENTE",csrf(otra)),200);
        var finalCliente=JSON.readTree(comprobar(get(otra,ruta),200));
        assertThat(finalCliente.get("borradores").get("presupuesto").get("calle").asString()).isEqualTo("Senderos");
        assertThat(finalCliente.get("borradores").get("presupuesto").has("numero")).isFalse();
        assertThat(finalCliente.get("pedido").isNull()).isTrue();
        comprobar(post(navegador,"/api/auth/logout","",csrf(navegador)),204);
        comprobar(post(otra,"/api/auth/logout","",csrf(otra)),204);
    }

    // Reproduce un PDF ya analizado con los metadatos y tiempos de la muestra original.
    private UUID copiarPdfValidado(JdbcTemplate db, UUID origen, UUID destino) {
        UUID archivo = UUID.randomUUID();
        db.update("""
                WITH copia AS (
                    INSERT INTO lamontana.archivo_almacenado(codigo_publico,id_usuario_cargador,id_operacion,nombre_original,
                        estado,fecha_creacion,cargar_hasta,fecha_inicio,fecha_fin,cantidad_bytes,sha256,cantidad_paginas)
                    SELECT ?,d.id_usuario,gen_random_uuid(),a.nombre_original,a.estado,a.fecha_creacion,a.cargar_hasta,
                           a.fecha_inicio,a.fecha_fin,a.cantidad_bytes,a.sha256,a.cantidad_paginas
                    FROM lamontana.archivo_almacenado a JOIN lamontana.archivo_preparado ap USING(id_archivo_almacenado)
                    JOIN lamontana.preparacion_cliente o USING(id_preparacion)
                    JOIN lamontana.preparacion_cliente d ON d.codigo_publico=?
                    WHERE o.codigo_publico=? AND ap.retirado_en IS NULL AND a.estado='VALIDO'
                    ORDER BY a.id_archivo_almacenado LIMIT 1 RETURNING id_archivo_almacenado
                )
                INSERT INTO lamontana.archivo_preparado(id_archivo_almacenado,id_preparacion)
                SELECT c.id_archivo_almacenado,d.id_preparacion FROM copia c
                JOIN lamontana.preparacion_cliente d ON d.codigo_publico=?
                """, archivo, destino, origen, destino);
        return archivo;
    }

    @Test
    void cadaRecorridoConservaSuAvanceYProtegeLosBorradoresDelOtroRol() throws Exception {
        var n=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String ruta="/api/demo/tutorial/recorridos";
        assertThat(get(n,ruta).statusCode()).isIn(401,403);
        comprobar(elegir(n,"ADMINISTRADOR",csrf(n)),200);
        comprobar(cambiarRecorrido(n,Map.of("recorrido","administrador","lesson","tarifas","task",1,"reached",2,
                "draft",Map.of("tarifa","{\"precio\":25}"))),200);
        comprobar(cambiarRecorrido(n,Map.of("recorrido","administrador","draft",Map.of("fase","{\"numero\":2}"))),200);
        comprobar(cambiarRecorrido(n,Map.of("recorrido","administrador","draft",Map.of("contrasena","x"))),400);
        comprobar(cambiarRecorrido(n,Map.of("recorrido","cliente","lesson","inicio","task",0)),403);
        comprobar(elegir(n,"CLIENTE",csrf(n)),200);
        var cliente=JSON.readTree(comprobar(cambiarRecorrido(n,Map.of("recorrido","cliente","lesson","archivo","task",1,"reached",3,
                "draft",Map.of("calle","Ejemplo"))),200));
        assertThat(cliente.get("administrador").get("draft").size()).isZero();
        assertThat(cliente.get("cliente").get("draft").get("calle").asString()).isEqualTo("Ejemplo");
        comprobar(elegir(n,"EMPLEADO",csrf(n)),200);
        comprobar(cambiarRecorrido(n,Map.of("recorrido","administrador","complete",true)),403);
        assertThat(JSON.readTree(comprobar(get(n,ruta),200)).get("cliente").get("draft").size()).isZero();
        comprobar(elegir(n,"ADMINISTRADOR",csrf(n)),200);
        var admin=JSON.readTree(comprobar(cambiarRecorrido(n,Map.of("recorrido","administrador","lesson","tablero","task",0,"reached",0)),200))
                .get("administrador");
        assertThat(admin.get("reached").asInt()).isEqualTo(2);
        assertThat(admin.get("draft").size()).isEqualTo(2);
        assertThat(admin.get("draft").get("tarifa").asString()).contains("25");
    }

    private HttpResponse<String> cambiarRecorrido(HttpClient n,Map<String,?> valores) throws Exception {
        return enviar(n,request("/api/demo/tutorial/recorridos").header("Content-Type","application/json")
                .header("X-CSRF-TOKEN",csrf(n)).PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(valores))));
    }

    @Test void reiniciarCadaTutorialProtegeLosOtrosAvancesYLaPreparacionCompartida() throws Exception {
        var db=app.getBean(JdbcTemplate.class);
        var n=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String flujo="/api/demo/tutorial", roles=flujo+"/recorridos";
        var reiniciarAdmin=Map.of("recorrido","administrador","accion","reiniciar","intento",0);
        var reiniciarCliente=Map.of("recorrido","cliente","accion","reiniciar","intento",0);
        var reiniciarFlujo=Map.of("accion","reiniciar","intento",0);

        comprobar(elegir(n,"ADMINISTRADOR",csrf(n)),200);
        comprobar(cambiarTutorial(n,Map.of("accion","iniciar")),200);
        comprobar(cambiarTutorial(n,Map.of("accion","tarifa")),200);
        var tarifa=Map.of("recorrido","administrador","lesson","tarifas","task",2,"reached",4,
                "complete",true,"draft",Map.of("tarifa","{\"precio\":25}"));
        comprobar(cambiarRecorrido(n,tarifa),200);
        comprobar(elegir(n,"CLIENTE",csrf(n)),200);
        UUID prep=UUID.fromString(JSON.readTree(comprobar(post(n,"/api/cliente/preparaciones","{}",csrf(n)),200)).get("codigoPublico").asString());
        UUID muestra=db.queryForObject("SELECT codigo_publico FROM lamontana.preparacion_cliente WHERE estado='CONFIRMADA' ORDER BY id_preparacion LIMIT 1",UUID.class);
        UUID archivo=copiarPdfValidado(db,muestra,prep);
        comprobar(cambiarTutorial(n,Map.of("accion","vincular","preparacion",prep)),200);
        comprobar(cambiarTutorial(n,Map.of("accion","borrador","paso","presupuesto","campos",Map.of("numero","31"))),200);
        var cliente=Map.of("recorrido","cliente","lesson","archivo","task",1,"reached",3,"complete",true,"draft",Map.of("preparacion",prep.toString()));
        var anterior=JSON.readTree(comprobar(cambiarRecorrido(n,cliente),200)).get("cliente");
        comprobar(cambiarRecorrido(n,reiniciarAdmin),403);
        comprobar(elegir(n,"EMPLEADO",csrf(n)),200);
        comprobar(cambiarTutorial(n,Map.of("accion","borrador","paso","cobro","campos",Map.of("importe","70"))),200);
        comprobar(cambiarRecorrido(n,reiniciarCliente),403);

        comprobar(elegir(n,"ADMINISTRADOR",csrf(n)),200);
        var sinCsrf=request(roles).header("Content-Type","application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(reiniciarAdmin)));
        comprobar(enviar(n,sinCsrf),403);
        assertThat(JSON.readTree(comprobar(get(n,roles),200)).get("administrador").get("complete").asBoolean()).isTrue();
        comprobar(cambiarRecorrido(n,Map.of("recorrido","administrador","accion","reiniciar")),400);
        var admin=JSON.readTree(comprobar(cambiarRecorrido(n,reiniciarAdmin),200)).get("administrador");
        comprobarReinicio(admin,"tablero");
        comprobar(cambiarRecorrido(n,tarifa),409);
        comprobar(cambiarRecorrido(n,Map.of("recorrido","administrador","intento",1,"lesson","catalogo","task",0)),200);
        var repetido=JSON.readTree(comprobar(cambiarRecorrido(n,reiniciarAdmin),200)).get("administrador");
        assertThat(repetido.get("lesson").asString()).isEqualTo("catalogo");
        assertThat(repetido.get("intento").asLong()).isEqualTo(1);

        comprobar(elegir(n,"CLIENTE",csrf(n)),200);
        assertThat(JSON.readTree(comprobar(get(n,roles),200)).get("cliente")).isEqualTo(anterior);
        assertThat(JSON.readTree(comprobar(get(n,flujo),200)).get("borradores").get("presupuesto").get("numero").asString()).isEqualTo("31");
        comprobarReinicio(JSON.readTree(comprobar(cambiarRecorrido(n,reiniciarCliente),200)).get("cliente"),"inicio");
        // La práctica de cliente vuelve a adoptar la preparación también usada por el flujo completo.
        comprobar(cambiarRecorrido(n,Map.of("recorrido","cliente","intento",1,"draft",Map.of("preparacion",prep.toString()))),200);

        var negocio=estadoNegocio(db);
        comprobar(enviar(n,request(flujo).header("Content-Type","application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(reiniciarFlujo)))),403);
        var nuevo=JSON.readTree(comprobar(cambiarTutorial(n,reiniciarFlujo),200));
        assertThat(nuevo.get("intento").asLong()).isEqualTo(1);
        assertThat(nuevo.get("estado").asString()).isEqualTo("ACTIVO");
        assertThat(nuevo.get("paso").asString()).isEqualTo("tarifa");
        assertThat(nuevo.get("preparacion").isNull()).isTrue();
        assertThat(nuevo.get("completados").size()).isZero();
        assertThat(nuevo.get("borradores").size()).isZero();
        comprobar(cambiarTutorial(n,Map.of("accion","borrador","paso","presupuesto","campos",Map.of("numero","anterior"))),409);
        var despues=JSON.readTree(comprobar(get(n,roles),200));
        assertThat(despues.get("cliente").get("draft").get("preparacion").asString()).isEqualTo(prep.toString());
        assertThat(despues.get("administrador").get("lesson").asString()).isEqualTo("catalogo");
        comprobar(elegir(n,"EMPLEADO",csrf(n)),200);
        assertThat(JSON.readTree(comprobar(get(n,flujo),200)).get("borradores").size()).isZero();
        comprobar(elegir(n,"ADMINISTRADOR",csrf(n)),200);
        comprobar(cambiarTutorial(n,Map.of("accion","tarifa","intento",1)),200);
        var repetidoFlujo=JSON.readTree(comprobar(cambiarTutorial(n,reiniciarFlujo),200));
        assertThat(repetidoFlujo.get("intento").asLong()).isEqualTo(1);
        assertThat(repetidoFlujo.get("completados").get(0).asString()).isEqualTo("tarifa");
        comprobar(elegir(n,"CLIENTE",csrf(n)),200);
        assertThat(comprobar(cambiarTutorial(n,Map.of("accion","vincular","preparacion",prep,"intento",1)),200)).contains("\"archivo\"");
        assertThat(db.queryForObject("SELECT estado FROM lamontana.archivo_almacenado WHERE codigo_publico=?",String.class,archivo)).isEqualTo("VALIDO");
        assertThat(estadoNegocio(db)).isEqualTo(negocio);
        comprobar(post(n,"/api/cliente/preparaciones/"+prep+"/archivos/"+archivo+"/quitar",
                JSON.writeValueAsString(Map.of("operacion",UUID.randomUUID())),csrf(n)),200);
    }

    private void comprobarReinicio(tools.jackson.databind.JsonNode avance,String leccion) {
        assertThat(avance.get("intento").asLong()).isEqualTo(1L);
        assertThat(avance.get("lesson").asString()).isEqualTo(leccion);
        assertThat(avance.get("task").asInt()).isZero();
        assertThat(avance.get("reached").asInt()).isZero();
        assertThat(avance.get("complete").asBoolean()).isFalse();
        assertThat(avance.get("draft").size()).isZero();
    }

    private Map<String,Object> estadoNegocio(JdbcTemplate db) {
        return db.queryForMap("""
                SELECT (SELECT count(*) FROM lamontana.pedido) AS pedidos,
                    (SELECT count(*) FROM lamontana.pago) AS pagos,
                    (SELECT count(*) FROM lamontana.tarifa_impresion) AS tarifas,
                    (SELECT count(*) FROM lamontana.demo_cupo_registro WHERE baja_en IS NULL) AS cupos
                """);
    }

    @Test void dosReiniciosConcurrentesYUnGuardadoAnteriorDejanUnSoloIntentoNuevo() throws Exception {
        var n=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        comprobar(elegir(n,"CLIENTE",csrf(n)),200);
        String token=csrf(n);
        var ruta="/api/demo/tutorial/recorridos";
        var reset=request(ruta).header("Content-Type","application/json").header("X-CSRF-TOKEN",token)
                .PUT(HttpRequest.BodyPublishers.ofString("{\"recorrido\":\"cliente\",\"accion\":\"reiniciar\",\"intento\":0}")).build();
        var guardado=request(ruta).header("Content-Type","application/json").header("X-CSRF-TOKEN",token)
                .PUT(HttpRequest.BodyPublishers.ofString("{\"recorrido\":\"cliente\",\"intento\":0,\"draft\":{\"calle\":\"Primer intento\"}}")).build();
        var respuesta=HttpResponse.BodyHandlers.ofString();
        var anterior=n.sendAsync(guardado,respuesta);
        var primero=n.sendAsync(reset,respuesta);
        var segundo=n.sendAsync(reset,respuesta);
        assertThat(anterior.join().statusCode()).isIn(200,409);
        comprobarReinicio(JSON.readTree(comprobar(primero.join(),200)).get("cliente"),"inicio");
        comprobarReinicio(JSON.readTree(comprobar(segundo.join(),200)).get("cliente"),"inicio");
        comprobarReinicio(JSON.readTree(comprobar(get(n,ruta),200)).get("cliente"),"inicio");
        comprobar(n.send(guardado,respuesta),409);
    }

    private HttpResponse<String> cambiarTutorial(HttpClient navegador,Map<String,?> cambio) throws Exception {
        return enviar(navegador,request("/api/demo/tutorial").header("Content-Type","application/json")
                .header("X-CSRF-TOKEN",csrf(navegador)).PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(cambio))));
    }

    private String csrf(HttpClient navegador) throws Exception {
        return JSON.readTree(comprobar(get(navegador, "/api/auth/csrf"), 200)).get("token").asString();
    }

    private HttpResponse<String> elegir(HttpClient navegador, String rol, String csrf) throws Exception {
        return post(navegador, SELECTOR, "{\"rol\":\"" + rol + "\"}", csrf);
    }

    private HttpResponse<String> post(HttpClient navegador, String ruta, String cuerpo, String csrf) throws Exception {
        var request = request(ruta).header("Content-Type", "application/json");
        if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        return enviar(navegador, request.POST(HttpRequest.BodyPublishers.ofString(cuerpo)));
    }

    private HttpResponse<String> get(HttpClient navegador, String ruta) throws Exception {
        return enviar(navegador, request(ruta).GET());
    }

    private HttpResponse<String> reproducirCookie(String cookie) throws Exception {
        return enviar(HttpClient.newHttpClient(), request(ME).header("Cookie", cookie).GET());
    }

    private HttpRequest.Builder request(String ruta) {
        return HttpRequest.newBuilder(URI.create(baseUrl + ruta)).timeout(Duration.ofSeconds(15));
    }

    private HttpResponse<String> enviar(HttpClient navegador, HttpRequest.Builder request) throws Exception {
        return navegador.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String comprobar(HttpResponse<String> respuesta, int estado) {
        assertThat(respuesta.statusCode()).as("%s: %s", respuesta.request().uri(), respuesta.body()).isEqualTo(estado);
        return respuesta.body();
    }

    private String cookie(CookieManager cookies) {
        var cookie = cookies.getCookieStore().getCookies().stream()
                .filter(c -> "LAMONTANA_DEMO_SESSION".equals(c.getName())).findFirst().orElseThrow();
        return cookie.getName() + "=" + cookie.getValue();
    }

    private record Caso(String seleccion, String rol, String codigo, int estadoAdmin, int estadoOperacion, int estadoCliente) {}
}
