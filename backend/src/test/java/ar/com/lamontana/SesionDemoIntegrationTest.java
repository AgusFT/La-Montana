package ar.com.lamontana;

import static org.assertj.core.api.Assertions.assertThat;

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
