package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

class MigracionesDeInstalacionTest {
    private static final String HISTORIAL = "SELECT * FROM lamontana.flyway_schema_history ORDER BY installed_rank";

    private void preparar(DataSource ds, int version) {
        Flyway.configure().dataSource(ds).schemas("lamontana").defaultSchema("lamontana")
                .target(String.valueOf(version)).load().migrate();
    }

    private void arrancar(EmbeddedPostgres pg) {
        arrancar(pg, "DESARROLLO");
    }

    private void arrancar(EmbeddedPostgres pg, String modo) {
        try (var app = new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0",
                "--spring.datasource.url=" + pg.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=", "--lamontana.instalacion.modo=" + modo)) {
            app.getBean(Flyway.class).validate();
        }
    }

    @Test
    void actualizaDesdeV43YV45ConservandoPedidosPagosYSoporte() throws Exception {
        try (var remoto = EmbeddedPostgres.builder().start(); var v45 = EmbeddedPostgres.builder().start()) {
            preparar(remoto.getPostgresDatabase(), 43);
            var negocio = new JdbcTemplate(remoto.getPostgresDatabase());
            negocio.execute(new ClassPathResource("migraciones/pedido-con-pago.sql").getContentAsString(StandardCharsets.UTF_8));
            String consulta = """
                    SELECT to_jsonb(i) AS impresora, to_jsonb(p) AS pedido, to_jsonb(g) AS pago,
                           to_jsonb(c) AS cotizacion, to_jsonb(r) AS reserva,
                           to_jsonb(h) AS historial, to_jsonb(f) AS evento_financiero
                    FROM lamontana.pedido p JOIN lamontana.cotizacion c USING(id_cotizacion)
                    JOIN lamontana.pago g USING(id_cotizacion) JOIN lamontana.reserva_entrega r USING(id_pedido)
                    JOIN lamontana.historial_estado_pedido h USING(id_pedido)
                    JOIN lamontana.evento_financiero f ON f.id_evento=g.id_evento
                    JOIN lamontana.impresora i ON i.id_sucursal=p.id_sucursal
                    """;
            var operacion = negocio.queryForMap(consulta);
            var historialRemoto = negocio.queryForList(HISTORIAL);
            arrancar(remoto);
            assertThat(negocio.queryForMap(consulta)).isEqualTo(operacion);
            assertThat(negocio.queryForList(HISTORIAL).subList(0, historialRemoto.size())).isEqualTo(historialRemoto);
            assertThat(negocio.queryForObject("SELECT modo FROM lamontana.entorno_instalacion", String.class)).isEqualTo("DESARROLLO");
            assertThat(negocio.queryForObject("SELECT count(*) FROM lamontana.usuario WHERE es_soporte", Integer.class)).isZero();

            preparar(v45.getPostgresDatabase(), 45);
            var soporte = new JdbcTemplate(v45.getPostgresDatabase());
            soporte.execute(new ClassPathResource("migraciones/pedido-con-pago.sql").getContentAsString(StandardCharsets.UTF_8));
            var operacionV45 = soporte.queryForMap(consulta);
            soporte.execute("""
                    INSERT INTO lamontana.entorno_instalacion VALUES (true,gen_random_uuid(),'DESARROLLO',now());
                    INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado,es_soporte,es_administrador_propietario)
                      SELECT gen_random_uuid(),id_rol,correo,'hash-prueba','Cuenta','Test','ACTIVO',es_soporte,NOT es_soporte
                      FROM lamontana.rol CROSS JOIN (VALUES ('soporte@example.test',true),('propietario@example.test',false)) x(correo,es_soporte)
                      WHERE codigo='ADMIN_ADMIN';
                    INSERT INTO lamontana.autorizacion_soporte(id_usuario_soporte,id_usuario_habilitante,motivo,estado,fecha_inicio,fecha_fin)
                      SELECT s.id_usuario,a.id_usuario,'Actualización','VIGENTE',now(),now()+interval '1 hour'
                      FROM lamontana.usuario s, lamontana.usuario a WHERE s.es_soporte AND a.es_administrador_propietario;
                    INSERT INTO lamontana.evento_soporte(id_usuario,tipo,recurso,resultado)
                      SELECT id_usuario,'ACCESO','administracion',200 FROM lamontana.usuario WHERE es_soporte;
                    """);
            String consultaSoporte = """
                    SELECT to_jsonb(e) AS entorno, to_jsonb(s) AS cuenta, to_jsonb(p) AS propietario,
                           to_jsonb(a) AS autorizacion, to_jsonb(h) AS evento, lamontana.soporte_habilitado(s.id_usuario) AS habilitado
                    FROM lamontana.entorno_instalacion e CROSS JOIN lamontana.autorizacion_soporte a
                    JOIN lamontana.usuario s ON s.id_usuario=a.id_usuario_soporte
                    JOIN lamontana.usuario p ON p.id_usuario=a.id_usuario_habilitante
                    JOIN lamontana.evento_soporte h ON h.id_usuario=s.id_usuario
                    """;
            var cuentas = soporte.queryForMap(consultaSoporte);
            var historialAnterior = soporte.queryForList(HISTORIAL);
            arrancar(v45);
            assertThat(soporte.queryForMap(consultaSoporte)).isEqualTo(cuentas);
            assertThat(soporte.queryForList(HISTORIAL).subList(0, historialAnterior.size())).isEqualTo(historialAnterior);
            assertThat(soporte.queryForMap(consulta)).isEqualTo(operacionV45);
            for (var pg : java.util.List.of(remoto, v45)) {
                var db = new JdbcTemplate(pg.getPostgresDatabase());
                var completo = db.queryForList(HISTORIAL);
                arrancar(pg);
                assertThat(db.queryForList(HISTORIAL)).isEqualTo(completo);
                assertThat(db.queryForMap(consulta)).isEqualTo(pg == remoto ? operacion : operacionV45);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {39, 40, 43, 44})
    void completaPrefijosValidosSinCrearCuentaDeSoporte(int version) throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            preparar(pg.getPostgresDatabase(), version);
            arrancar(pg);
            var jdbc = new JdbcTemplate(pg.getPostgresDatabase());
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.flyway_schema_history WHERE version='46' AND success", Integer.class)).isOne();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario WHERE es_soporte", Integer.class)).isZero();
        }
    }

    @Test
    void conservaElModoStandardYLaIdentidadDesdeV45() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            preparar(pg.getPostgresDatabase(), 45);
            var jdbc = new JdbcTemplate(pg.getPostgresDatabase());
            jdbc.update("INSERT INTO lamontana.entorno_instalacion VALUES (true,gen_random_uuid(),'STANDARD',now())");
            String consulta = "SELECT * FROM lamontana.entorno_instalacion";
            var identidad = jdbc.queryForMap(consulta);
            arrancar(pg, "STANDARD");
            assertThat(jdbc.queryForMap(consulta)).isEqualTo(identidad);
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            checksum=0                            | checksum mismatch
            script='V41__soporte_temporal.sql' | Historial de migraciones incompatible
            success=false                         | Migración fallida
            version='99'                          | Historial de migraciones incompatible
            """)
    void rechazaHistorialIncompatibleAntesDeMigrar(String alteracion, String diagnostico) throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            preparar(pg.getPostgresDatabase(), 41);
            var jdbc = new JdbcTemplate(pg.getPostgresDatabase());
            jdbc.update("UPDATE lamontana.flyway_schema_history SET " + alteracion + " WHERE version='41'");
            var antes = jdbc.queryForList(HISTORIAL);
            assertThatThrownBy(() -> arrancar(pg)).hasStackTraceContaining(diagnostico);
            assertThat(jdbc.queryForList(HISTORIAL)).isEqualTo(antes);
            assertThat(jdbc.queryForObject("SELECT to_regclass('lamontana.preparacion_cliente') IS NULL", Boolean.class)).isTrue();
        }
    }
}
