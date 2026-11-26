package ar.com.lamontana;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.lamontana.instalacion.MigracionesDeInstalacion;
import ar.com.lamontana.pagos.ConservacionArchivosScheduler;
import ar.com.lamontana.pagos.PagoController;
import ar.com.lamontana.pagos.PagoService;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class VigenciaDemoIntegrationTest {
    private static final Instant INICIO = Instant.parse("2026-11-02T12:30:00Z");
    private static final Instant FIN = INICIO.plus(Duration.ofHours(360));
    private static final String ARCHIVO = "4d9fdf39-1f90-4d08-b7b6-0c77fc9f0081";
    private static final String MENSAJE_FIN = "Fin de la Demo, para adquirir el producto comunicate con administracion@yg-soluciones.com";
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient navegador = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    private final Reloj reloj = new Reloj();
    @TempDir Path equipo;
    private URI base;

    @Test void respetaElLimiteExactoConSesionAbiertaYConservaElOriginal() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            var jdbc = instalarV50(pg, INICIO);
            try (var app = arrancar(pg)) {
                assertThat(estado().get("diasRestantes").asLong()).isEqualTo(15);
                String csrf = json.readTree(get("/api/auth/csrf").body()).get("token").asString();
                assertThat(elegir(csrf).statusCode()).isEqualTo(200);
                assertThat(get("/api/auth/me").statusCode()).isEqualTo(200);
                String quote = jdbc.queryForObject("SELECT codigo_publico::text FROM lamontana.cotizacion", String.class);
                String original = "/api/cliente/cotizaciones/" + quote + "/archivos/" + ARCHIVO + "/original";
                assertThat(get(original).statusCode()).isEqualTo(200);
                Path pdf = equipo.resolve("archivos/pdf/" + ARCHIVO + "/original.pdf");
                byte[] bytes = Files.readAllBytes(pdf);
                assertThat(bytes).isNotEmpty();

                reloj.ahora = FIN.minusSeconds(1);
                var antes = estado();
                assertThat(antes.get("estado").asString()).isEqualTo("VIGENTE");
                assertThat(antes.get("restanteMillis").asLong()).isEqualTo(1000);
                assertThat(antes.get("diasRestantes").asLong()).isOne();
                reloj.ahora = FIN.minusMillis(1);
                assertThat(estado().get("restanteMillis").asLong()).isOne();
                assertThat(get("/api/cliente/pedidos").statusCode()).isEqualTo(200);
                long eventos = jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_acceso", Long.class);
                // Dejar un original cuya política normal ya autoriza purgar.
                var pagos = app.getBean(PagoService.class);
                var cotizacion = UUID.fromString(quote);
                var cobro = pagos.vista(cotizacion, "administracion@demo.invalid", true).pagos().get(0);
                pagos.devolver(cotizacion, new PagoController.Devolver(UUID.randomUUID(), cobro.codigoPublico(),
                        cobro.version(), MedioPago.EFECTIVO, "300.00", "devolucion-prueba",
                        Instant.now().minusSeconds(1), true, "Devolución para probar la conservación."),
                        "administracion@demo.invalid");
                jdbc.update("""
                        UPDATE lamontana.cobro_pedido SET fecha_limite=statement_timestamp()-interval '100 hours',
                            retener_hasta=statement_timestamp()-interval '100 hours'+retencion_horas*interval '1 hour'
                        """);
                assertThat(jdbc.queryForObject("SELECT lamontana.cobro_puede_purgar(id_pedido) FROM lamontana.pedido",
                        Boolean.class)).isTrue();
                reloj.ahora = FIN;
                var fin = estado();
                assertThat(fin.get("estado").asString()).isEqualTo("VENCIDA");
                assertThat(fin.get("restanteMillis").asLong()).isZero();
                assertThat(fin.get("mensaje").asString()).isEqualTo(MENSAJE_FIN);
                assertThat(get("/api/cliente/pedidos").statusCode()).isEqualTo(423);
                assertThat(get(original).statusCode()).isEqualTo(423);
                assertThat(elegir(csrf).statusCode()).isEqualTo(423);
                assertThat(get("/actuator/health/readiness").statusCode()).isEqualTo(200);
                app.getBean(ConservacionArchivosScheduler.class).ejecutar();
                assertThat(Files.readAllBytes(pdf)).isEqualTo(bytes);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_acceso", Long.class)).isEqualTo(eventos);
                reloj.ahora = INICIO;
                assertThat(estado().get("estado").asString()).isEqualTo("VENCIDA");
            }
        }
    }

    @Test void unRetrocesoBloqueaHastaAlcanzarElUltimoInstanteInclusoTrasReiniciar() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            instalarV50(pg, INICIO);
            var ultimo = INICIO.plus(Duration.ofDays(3));
            reloj.ahora = ultimo;
            try (var app = arrancar(pg)) {
                assertThat(estado().get("diasRestantes").asLong()).isEqualTo(12);
                reloj.ahora = ultimo.minusMillis(1);
                assertThat(estado().get("estado").asString()).isEqualTo("RELOJ_ATRASADO");
                assertThat(get("/api/auth/csrf").statusCode()).isEqualTo(423);
            }
            try (var app = arrancar(pg)) {
                assertThat(estado().get("estado").asString()).isEqualTo("RELOJ_ATRASADO");
                reloj.ahora = ultimo;
                assertThat(estado().get("estado").asString()).isEqualTo("VIGENTE");
                assertThat(estado().get("venceEn").asString()).isEqualTo(FIN.toString());
                assertThat(get("/api/auth/csrf").statusCode()).isEqualTo(200);
            }
        }
    }

    @Test void reinstalarConBaseNuevaConservaElInicioYLaMarcaDanadaOBorradaBloqueaElAcceso() throws Exception {
        Path marca = equipo.resolve("estado/vigencia.txt");
        try (var anterior = EmbeddedPostgres.builder().start()) {
            instalarV50(anterior, INICIO);
            reloj.ahora = INICIO.plus(Duration.ofHours(40));
            try (var app = arrancar(anterior)) {
                assertThat(estado().get("inicio").asString()).isEqualTo(INICIO.toString());
            }
        }
        try (var nueva = EmbeddedPostgres.builder().start()) {
            instalarV50(nueva, INICIO.plus(Duration.ofHours(60)));
            reloj.ahora = INICIO.plus(Duration.ofHours(61));
            String original;
            try (var app = arrancar(nueva)) {
                assertThat(estado().get("inicio").asString()).isEqualTo(INICIO.toString());
                assertThat(estado().get("venceEn").asString()).isEqualTo(FIN.toString());
                original = Files.readString(marca);
                Files.writeString(marca, "incompleta");
                assertThat(estado().get("estado").asString()).isEqualTo("NO_DISPONIBLE");
                assertThat(get("/api/auth/csrf").statusCode()).isEqualTo(503);
                assertThat(Files.readString(marca)).isEqualTo("incompleta");
                Files.delete(marca);
            }
            try (var app = arrancar(nueva)) {
                assertThat(estado().get("estado").asString()).isEqualTo("NO_DISPONIBLE");
                assertThat(marca).doesNotExist();
                Files.writeString(marca, original);
                assertThat(estado().get("estado").asString()).isEqualTo("VIGENTE");
                assertThat(estado().get("inicio").asString()).isEqualTo(INICIO.toString());
            }
        }
    }

    private JdbcTemplate instalarV50(EmbeddedPostgres pg, Instant inicio) {
        var ds = pg.getPostgresDatabase();
        var cfg = Flyway.configure().dataSource(ds).schemas("lamontana").defaultSchema("lamontana")
                .locations("classpath:db/migration", "classpath:db/imprenta").target("50");
        new MigracionesDeInstalacion("DEMO").customize(cfg);
        cfg.load().migrate();
        var jdbc = new JdbcTemplate(ds);
        jdbc.update("UPDATE lamontana.entorno_instalacion SET creada_en=?", Timestamp.from(inicio));
        return jdbc;
    }

    private ConfigurableApplicationContext arrancar(EmbeddedPostgres pg) {
        var app = new SpringApplicationBuilder(LaMontanaApplication.class)
                .initializers(c -> c.getBeanFactory().registerSingleton("relojDePrueba", reloj))
                .run("--server.port=0", "--spring.datasource.url=" + pg.getJdbcUrl("postgres", "postgres"),
                        "--spring.datasource.username=postgres", "--spring.datasource.password=",
                        "--lamontana.demo.vigencia-directorio=" + equipo.resolve("estado"),
                        "--lamontana.archivos.directorio=" + equipo.resolve("archivos"),
                        "--lamontana.archivos.conservacion-inicio-ms=3600000");
        base = URI.create("http://127.0.0.1:" + app.getEnvironment().getRequiredProperty("local.server.port"));
        return app;
    }

    private JsonNode estado() throws Exception {
        var respuesta = get("/api/demo/vigencia");
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().firstValue("Cache-Control")).contains("no-store");
        return json.readTree(respuesta.body());
    }

    private HttpResponse<String> elegir(String csrf) throws Exception {
        return navegador.send(HttpRequest.newBuilder(base.resolve("/api/demo/sesion"))
                .header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf)
                .POST(HttpRequest.BodyPublishers.ofString("{\"rol\":\"CLIENTE\"}")).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> get(String ruta) throws Exception {
        return navegador.send(HttpRequest.newBuilder(base.resolve(ruta)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private static final class Reloj extends Clock {
        volatile Instant ahora = INICIO;
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(ahora, zone); }
        @Override public Instant instant() { return ahora; }
    }
}
