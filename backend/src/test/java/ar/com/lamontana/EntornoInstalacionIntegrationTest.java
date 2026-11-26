package ar.com.lamontana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.com.lamontana.instalacion.EntornoInstalacion;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

class EntornoInstalacionIntegrationTest {
    @Test
    void inicializaComoDemoPorDefectoYConservaSuIdentidadAlReiniciar() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            var jdbc = new JdbcTemplate(pg.getPostgresDatabase());
            String identidad = "SELECT * FROM lamontana.entorno_instalacion";
            try (var app = iniciar(pg)) {
                assertThat(app.getBean(EntornoInstalacion.class).estado())
                        .containsEntry("modo", "DEMO").containsEntry("simulacionesPermitidas", true)
                        .containsEntry("correoDePrueba", true);
                assertThat(jdbc.queryForList("""
                        SELECT r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                        WHERE u.estado='ACTIVO'
                        """, String.class)).containsExactlyInAnyOrder("ADMIN_ADMIN", "EMPLEADO", "CLIENTE");
            }
            var antes = jdbc.queryForMap(identidad);
            try (var app = iniciar(pg, "--lamontana.instalacion.modo=DEMO")) {
                assertThat(jdbc.queryForMap(identidad)).isEqualTo(antes);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"DESARROLLO", "STANDARD"})
    void rechazaModoDeProductoAntesDeCrearElEsquema(String modo) throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            assertThatThrownBy(() -> iniciar(pg, "--lamontana.instalacion.modo=" + modo))
                    .hasStackTraceContaining("Este backend requiere el modo DEMO");
            var jdbc = new JdbcTemplate(pg.getPostgresDatabase());
            assertThat(jdbc.queryForObject("SELECT to_regnamespace('lamontana') IS NULL", Boolean.class)).isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "lamontana"})
    void conservaBaseAjenaSinHistorial(String esquema) throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            var jdbc = new JdbcTemplate(pg.getPostgresDatabase());
            jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + esquema);
            String tabla = esquema + ".datos_previos";
            jdbc.execute("CREATE TABLE " + tabla + " (id integer)");
            jdbc.update("INSERT INTO " + tabla + " VALUES (42)");
            assertThatThrownBy(() -> iniciar(pg)).hasStackTraceContaining("La Demo requiere una base nueva");
            assertThat(jdbc.queryForObject("SELECT id FROM " + tabla, Integer.class)).isEqualTo(42);
            assertThat(jdbc.queryForObject("SELECT to_regclass('lamontana.flyway_schema_history') IS NULL",
                    Boolean.class)).isTrue();
        }
    }

    @Test
    void rechazaUnaDemoQuePerdioSuIdentidadSinRecrearla() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            try (var app = iniciar(pg)) {
                app.getBean(JdbcTemplate.class).update("DELETE FROM lamontana.entorno_instalacion");
            }
            assertThatThrownBy(() -> iniciar(pg)).hasStackTraceContaining("La Demo requiere una base nueva");
            var jdbc = new JdbcTemplate(pg.getPostgresDatabase());
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.entorno_instalacion",
                    Integer.class)).isZero();
        }
    }

    private ConfigurableApplicationContext iniciar(EmbeddedPostgres pg, String... opciones) {
        var args = new ArrayList<>(List.of("--server.port=0", "--spring.datasource.username=postgres",
                "--spring.datasource.password=", "--spring.datasource.url=" + pg.getJdbcUrl("postgres", "postgres")));
        args.addAll(List.of(opciones));
        return new SpringApplicationBuilder(LaMontanaApplication.class).run(args.toArray(String[]::new));
    }
}
