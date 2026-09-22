package ar.com.lamontana;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

class IdentidadIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TOKEN = "token-exclusivo-de-pruebas-no-utilizar-en-demo";
    private static final String PASSWORD = "ClaveSoloParaPruebas123";
    private int port;

    @Test
    void propietarioUnicoSesionPersistenteCsrfYRevocacion() throws Exception {
        Path files = Files.createTempDirectory("lamontana-identidad-");
        try (EmbeddedPostgres pg = EmbeddedPostgres.builder().start()) {
            var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            var client = HttpClient.newBuilder().cookieHandler(cookies).build();
            String oldCookie;
            try (var app = iniciar(pg, files)) {
                assertThat(get(client, "/api/setup/estado").body()).contains("\"requierePropietario\":true");
                assertThat(get(client, "/api/auth/me").statusCode()).isEqualTo(401);
                assertThat(post(client, "/api/setup/propietario", alta(TOKEN), "application/json", null).statusCode()).isEqualTo(403);
                String csrf = csrf(client);
                assertThat(post(client, "/api/auth/registro", registro(), "application/json", csrf).statusCode()).isEqualTo(409);
                var csrfCookie = cookies.getCookieStore().getCookies().get(0);
                assertThat(csrfCookie.isHttpOnly()).isTrue();
                assertThat(post(client, "/api/setup/propietario", alta("incorrecto"), "application/json", csrf).statusCode()).isEqualTo(403);
                assertThat(post(client, "/api/setup/propietario", "{}", "application/json", csrf).statusCode()).isEqualTo(400);

                var request = requestPost("/api/setup/propietario", alta(TOKEN), "application/json", csrf);
                var first = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                var second = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                assertThat(List.of(first.get().statusCode(), second.get().statusCode())).containsExactlyInAnyOrder(201, 409);

                var jdbc = app.getBean(JdbcTemplate.class);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario", Integer.class)).isEqualTo(1);
                assertThat(jdbc.queryForObject("SELECT hash_contrasena FROM lamontana.usuario", String.class))
                        .startsWith("{pbkdf2@SpringSecurity_v5_8}").doesNotContain(PASSWORD);
                assertThat(post(client, "/api/auth/login", "username=admin%40example.test&password=incorrecta", "application/x-www-form-urlencoded", csrf).statusCode()).isEqualTo(401);
                String beforeLogin = cookie(cookies);
                assertThat(post(client, "/api/auth/login", "username=ADMIN%40EXAMPLE.TEST&password=" + PASSWORD, "application/x-www-form-urlencoded", csrf).statusCode()).isEqualTo(200);
                assertThat(cookie(cookies)).isNotEqualTo(beforeLogin);
                assertThat(get(client, "/api/auth/me").body()).contains("ADMIN_ADMIN", "admin@example.test").doesNotContain("hash_contrasena", PASSWORD);
                assertThat(get(client, "/api/admin/estado").statusCode()).isEqualTo(200);
                assertThat(get(client, "/api/admin/sucursales").body()).isEqualTo("[]");
                String adminCsrf = csrf(client);
                assertThat(post(client, "/api/admin/sucursales", sucursal("CENTRO"), "application/json", null).statusCode()).isEqualTo(403);
                assertThat(post(client, "/api/admin/sucursales", sucursal("CENTRO"), "application/json", adminCsrf).statusCode()).isEqualTo(201);
                assertThat(post(client, "/api/admin/sucursales", sucursal("NORTE"), "application/json", adminCsrf).statusCode()).isEqualTo(201);
                assertThat(post(client, "/api/admin/sucursales", sucursal("centro"), "application/json", adminCsrf).statusCode()).isEqualTo(409);
                assertThat(post(client, "/api/admin/sucursales", sucursal("SUR").replace("America/Argentina/Buenos_Aires", "zona-inexistente"), "application/json", adminCsrf).statusCode()).isEqualTo(400);
                assertThat(JSON.readTree(get(client, "/api/admin/sucursales").body()).size()).isEqualTo(2);
                assertThat(get(client, "/api/cliente/estado").statusCode()).isEqualTo(403);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.sesion_http WHERE principal_name='admin@example.test'", Integer.class)).isEqualTo(1);
                oldCookie = cookie(cookies);

                var cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
                String clienteCsrf = csrf(cliente);
                assertThat(post(cliente, "/api/auth/registro", registro(), "application/json", null).statusCode()).isEqualTo(403);
                var registroRequest = requestPost("/api/auth/registro", registro(), "application/json", clienteCsrf);
                var registroA = cliente.sendAsync(registroRequest, HttpResponse.BodyHandlers.ofString());
                var registroB = cliente.sendAsync(registroRequest, HttpResponse.BodyHandlers.ofString());
                assertThat(List.of(registroA.get().statusCode(), registroB.get().statusCode())).containsExactlyInAnyOrder(201, 409);
                assertThat(get(cliente, "/api/auth/me").statusCode()).isEqualTo(401);
                assertThat(post(cliente, "/api/auth/login", "username=CLIENTE%40EXAMPLE.TEST&password=" + PASSWORD, "application/x-www-form-urlencoded", clienteCsrf).statusCode()).isEqualTo(200);
                assertThat(get(cliente, "/api/auth/me").body()).contains("\"rol\":\"CLIENTE\"", "cliente@example.test").doesNotContain("admin@example.test");
                assertThat(get(cliente, "/api/admin/estado").statusCode()).isEqualTo(403);
                assertThat(get(cliente, "/api/admin/sucursales").statusCode()).isEqualTo(403);
                assertThat(post(cliente, "/api/admin/sucursales", sucursal("OTRA"), "application/json", csrf(cliente)).statusCode()).isEqualTo(403);
                assertThat(get(cliente, "/api/cliente/estado").statusCode()).isEqualTo(200);
                assertThat(jdbc.queryForObject("SELECT es_administrador_propietario FROM lamontana.usuario WHERE correo='cliente@example.test'", Boolean.class)).isFalse();
                assertThat(jdbc.queryForObject("SELECT correo_verificado_en IS NULL FROM lamontana.usuario WHERE correo='cliente@example.test'", Boolean.class)).isTrue();
            }

            // La segunda aplicación usa la misma base y la cookie anterior: no una sesión en memoria.
            try (var app = iniciar(pg, files)) {
                assertThat(get(client, "/api/auth/me").statusCode()).isEqualTo(200);
                assertThat(JSON.readTree(get(client, "/api/admin/sucursales").body()).size()).isEqualTo(2);
                assertThat(get(client, "/api/setup/estado").body()).contains("\"requierePropietario\":false");
                assertThat(post(client, "/api/auth/logout", "", "application/x-www-form-urlencoded", null).statusCode()).isEqualTo(403);
                assertThat(post(client, "/api/auth/logout", "", "application/x-www-form-urlencoded", csrf(client)).statusCode()).isEqualTo(204);
                assertThat(get(client, "/api/auth/me").statusCode()).isEqualTo(401);
                var replay = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/api/auth/me")).header("Cookie", oldCookie).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertThat(replay.statusCode()).isEqualTo(401);
                assertThat(post(client, "/api/setup/propietario", alta(TOKEN), "application/json", csrf(client)).statusCode()).isEqualTo(409);
                assertThat(app.getBean(JdbcTemplate.class).queryForObject("SELECT count(*) FROM lamontana.evento_acceso WHERE tipo='ALTA_PROPIETARIO'", Integer.class)).isEqualTo(1);
                String retryToken = csrf(client);
                HttpResponse<String> throttled = null;
                for (int i = 0; i < 31; i++) {
                    throttled = post(client, "/api/setup/propietario", alta(TOKEN), "application/json", retryToken);
                }
                assertThat(throttled.statusCode()).isEqualTo(429);
                assertThat(throttled.headers().firstValue("Retry-After")).contains("60");
            }
        } finally { Files.deleteIfExists(files); }
    }

    private ConfigurableApplicationContext iniciar(EmbeddedPostgres pg, Path files) {
        var app = new SpringApplicationBuilder(LaMontanaApplication.class).run(
                "--server.port=0", "--spring.datasource.url=" + pg.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=",
                "--lamontana.archivos.directorio=" + files, "--lamontana.instalacion.token=" + TOKEN);
        port = Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));
        return app;
    }

    private String alta(String token) {
        return JSON.writeValueAsString(Map.of("token", token, "nombre", "Admin", "apellido", "Prueba", "correo", "admin@example.test", "contrasena", PASSWORD));
    }
    private String registro() {
        return JSON.writeValueAsString(Map.of("nombre", "Cliente", "apellido", "Prueba", "correo", "cliente@example.test", "contrasena", PASSWORD));
    }
    private String sucursal(String codigo) {
        return JSON.writeValueAsString(Map.of("codigo", codigo, "nombre", "Sucursal " + codigo, "calle", "Prueba", "numero", "123", "localidad", "Ciudad", "provincia", "Provincia", "codigoPostal", "1234", "zonaHoraria", "America/Argentina/Buenos_Aires"));
    }
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private String csrf(HttpClient client) throws Exception {
        var response = get(client, "/api/auth/csrf");
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body()).get("token").asString();
    }
    private HttpRequest requestPost(String path, String body, String contentType, String csrf) {
        var request = HttpRequest.newBuilder(uri(path)).header("Content-Type", contentType);
        if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        return request.POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }
    private HttpResponse<String> post(HttpClient client, String path, String body, String contentType, String csrf) throws Exception {
        return client.send(requestPost(path, body, contentType, csrf), HttpResponse.BodyHandlers.ofString());
    }
    private String cookie(CookieManager manager) {
        var cookie = manager.getCookieStore().getCookies().stream().filter(c -> "SESSION".equals(c.getName())).findFirst().orElseThrow();
        return cookie.getName() + "=" + cookie.getValue();
    }
}
