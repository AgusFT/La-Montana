//#region ENCABEZADO · BaseTecnicaIntegrationTest.java
/*
 * ========================================================================
 * ARCHIVO: BaseTecnicaIntegrationTest.java
 * ========================================================================
 * FUNCIÓN
 * Comprueba el arranque y las migraciones en PostgreSQL real, la ausencia de datos de negocio
 * precargados y los estados públicos de salud e instalación.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete, static] void propiedades(DynamicPropertyRegistry properties)
 *   Registra las propiedades del entorno de prueba.
 * - [paquete] void migraPostgresRealSinPrecargarElNegocio()
 *   Caso de prueba.
 * - [paquete] void estadoYSaludPublicosNoHabilitanOperacion() throws Exception
 *   Caso de prueba.
 * - [private] HttpResponse<String> get(String path) throws Exception
 * - [paquete, static] void limpiar() throws IOException
 *   Cierre de los recursos compartidos de la prueba.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - BaseTecnicaIntegrationTest (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BaseTecnicaIntegrationTest {
    static final EmbeddedPostgres POSTGRES;
    static final Path FILES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
            FILES = Files.createTempDirectory("lamontana-test-");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
        properties.add("lamontana.archivos.directorio", FILES::toString);
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migraPostgresRealSinPrecargarElNegocio() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.flyway_schema_history WHERE version='1' AND success", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT completada_en IS NULL FROM lamontana.inicializacion_sistema", Boolean.class)).isTrue();
        assertThat(Files.isWritable(FILES)).isTrue();
    }

    @Test
    void estadoYSaludPublicosNoHabilitanOperacion() throws Exception {
        var response = get("/api/sistema/estado");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"etapa\":\"IDENTIDAD_INICIAL\"", "\"configuracionDisponible\":false", "\"operacionDisponible\":false");
        var readiness = get("/actuator/health/readiness");
        assertThat(readiness.statusCode()).isEqualTo(200);
        assertThat(readiness.body()).contains("\"status\":\"UP\"").doesNotContain("jdbc", "password", "components");
        assertThat(get("/api/pedidos").statusCode()).isEqualTo(401);
        assertThat(get("/actuator/env").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @AfterAll
    static void limpiar() throws IOException {
        POSTGRES.close();
        Files.deleteIfExists(FILES);
    }
}
