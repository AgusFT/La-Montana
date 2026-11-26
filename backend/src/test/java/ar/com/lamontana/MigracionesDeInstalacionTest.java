package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;

import ar.com.lamontana.instalacion.MigracionesDeInstalacion;
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
        try (var app = new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0",
                "--spring.datasource.url=" + pg.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=")) {
            app.getBean(Flyway.class).validate();
        }
    }

    @Test
    void conservaElHistorialDePuntosAlActualizarLaDemoYRechazaOperacionesNuevas() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            var ds = pg.getPostgresDatabase();
            var v50 = Flyway.configure().dataSource(ds).schemas("lamontana").defaultSchema("lamontana")
                    .locations("classpath:db/migration", "classpath:db/imprenta").target("50");
            new MigracionesDeInstalacion("DEMO").customize(v50);
            v50.load().migrate();
            var jdbc = new JdbcTemplate(ds);
            jdbc.update("""
                    INSERT INTO lamontana.punto_entrega(codigo_publico,codigo,id_usuario_creador)
                    SELECT gen_random_uuid(),'MOVIL',id_usuario FROM lamontana.usuario WHERE es_administrador_propietario
                    """);
            jdbc.update("""
                    INSERT INTO lamontana.disponibilidad_punto_entrega
                    SELECT id_punto_entrega,'HABILITADO',1,id_usuario_creador,now() FROM lamontana.punto_entrega
                    """);
            String cotizar = """
                    INSERT INTO lamontana.cotizacion(codigo_publico,id_usuario_creador,id_sucursal,id_configuracion_version,
                        id_catalogo_revision,estado,generada_en,vigente_hasta,subtotal,costo_entrega,total,oferta)
                    SELECT gen_random_uuid(),c.id_usuario_creador,id_sucursal,id_configuracion_version,id_catalogo_revision,
                        'VIGENTE',now(),now()+interval '1 day',subtotal,costo_entrega,total,
                        oferta || jsonb_build_object('modalidad','RETIRO_PUNTO_ENTREGA','punto',p.codigo_publico)
                    FROM lamontana.cotizacion c CROSS JOIN lamontana.punto_entrega p WHERE estado='CONFIRMADA'
                    """;
            assertThat(jdbc.update(cotizar)).isEqualTo(1);
            String historia = """
                    SELECT to_jsonb(c)::text AS cotizacion,to_jsonb(d)::text AS disponibilidad
                    FROM lamontana.cotizacion c CROSS JOIN lamontana.disponibilidad_punto_entrega d
                    WHERE c.oferta->>'modalidad'='RETIRO_PUNTO_ENTREGA'
                    """;
            var anterior = jdbc.queryForMap(historia);
            arrancar(pg);
            assertThat(jdbc.queryForMap(historia)).isEqualTo(anterior);
            assertThat(jdbc.queryForList("SELECT modalidad FROM lamontana.modalidades_entrega_demo", String.class))
                    .containsExactlyInAnyOrder("RETIRO_SUCURSAL", "ENVIO_DOMICILIO");
            for (String insertar : java.util.List.of(cotizar,
                    "INSERT INTO lamontana.reserva_entrega(modalidad) VALUES ('RETIRO_PUNTO_ENTREGA')",
                    "INSERT INTO lamontana.propuesta_reprogramacion(franja_nueva) VALUES (jsonb_build_object('modalidad','RETIRO_PUNTO_ENTREGA'))")) {
                assertThatThrownBy(() -> jdbc.update(insertar)).rootCause()
                        .isInstanceOfSatisfying(java.sql.SQLException.class,
                                error -> assertThat(error.getSQLState()).isEqualTo("23D16"));
            }
            assertThat(jdbc.queryForMap(historia)).isEqualTo(anterior);
        }
    }

    @Test
    void rechazaProductoV43YV45ConservandoPedidosPagosYSoporte() throws Exception {
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
            assertThatThrownBy(() -> arrancar(remoto)).hasStackTraceContaining("La Demo requiere una base nueva");
            assertThat(negocio.queryForMap(consulta)).isEqualTo(operacion);
            assertThat(negocio.queryForList(HISTORIAL)).isEqualTo(historialRemoto);
            assertThat(negocio.queryForObject("SELECT to_regclass('lamontana.entorno_instalacion') IS NULL", Boolean.class)).isTrue();

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
            assertThatThrownBy(() -> arrancar(v45)).hasStackTraceContaining("La Demo requiere una base nueva");
            assertThat(soporte.queryForMap(consultaSoporte)).isEqualTo(cuentas);
            assertThat(soporte.queryForList(HISTORIAL)).isEqualTo(historialAnterior);
            assertThat(soporte.queryForMap(consulta)).isEqualTo(operacionV45);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 39, 40, 44, 46})
    void rechazaHistorialDelProductoAunqueElEntornoNoEsteMarcado(int version) throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            preparar(pg.getPostgresDatabase(), version);
            var jdbc = new JdbcTemplate(pg.getPostgresDatabase());
            var historial = jdbc.queryForList(HISTORIAL);
            assertThatThrownBy(() -> arrancar(pg)).hasStackTraceContaining("La Demo requiere una base nueva");
            assertThat(jdbc.queryForList(HISTORIAL)).isEqualTo(historial);
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
            assertThatThrownBy(() -> arrancar(pg)).hasStackTraceContaining("La Demo requiere una base nueva");
            assertThat(jdbc.queryForMap(consulta)).isEqualTo(identidad);
        }
    }

    @Test
    void completaLaInicializacionSiSoloSeCreoElHistorialDeDemo() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            var ds = pg.getPostgresDatabase();
            var demo = Flyway.configure().dataSource(ds).schemas("lamontana").defaultSchema("lamontana");
            new MigracionesDeInstalacion("DEMO").customize(demo);
            demo.locations("classpath:db/demo").load().migrate();
            arrancar(pg);
            var jdbc = new JdbcTemplate(ds);
            assertThat(jdbc.queryForObject("SELECT modo FROM lamontana.entorno_instalacion", String.class))
                    .isEqualTo("DEMO");
        }
    }

    @Test
    void rechazaLaMarcaDeEsquemaDelProductoAunqueAunNoHayaMigradoV1() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            var ds = pg.getPostgresDatabase();
            Flyway.configure().dataSource(ds).schemas("lamontana").defaultSchema("lamontana")
                    .locations("classpath:db/demo").load().migrate();
            var jdbc = new JdbcTemplate(ds);
            var antes = jdbc.queryForList(HISTORIAL);
            assertThat(antes).hasSize(1);
            assertThat(antes.get(0)).containsEntry("type", "SCHEMA").containsEntry("installed_by", "postgres");
            assertThatThrownBy(() -> arrancar(pg)).hasStackTraceContaining("La Demo requiere una base nueva");
            assertThat(jdbc.queryForList(HISTORIAL)).isEqualTo(antes);
        }
    }

    @Test
    void rechazaAntesDelPrimerDdlSiElProductoTomaLaBaseEntreLaInspeccionYElBloqueo() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            var ds = pg.getPostgresDatabase();
            var demo = Flyway.configure().dataSource(ds).schemas("lamontana").defaultSchema("lamontana");
            new MigracionesDeInstalacion("DEMO").customize(demo);
            preparar(ds, 43);
            var jdbc = new JdbcTemplate(ds);
            jdbc.execute("""
                    CREATE SEQUENCE public.ddl_detectado;
                    CREATE FUNCTION public.detectar_ddl() RETURNS event_trigger LANGUAGE plpgsql AS $$
                      BEGIN PERFORM nextval('public.ddl_detectado'); END $$;
                    CREATE EVENT TRIGGER detectar_ddl ON ddl_command_start WHEN TAG IN ('CREATE TABLE','ALTER TABLE')
                      EXECUTE FUNCTION public.detectar_ddl();
                    """);
            var antes = jdbc.queryForList(HISTORIAL);

            assertThatThrownBy(() -> demo.load().migrate()).hasStackTraceContaining("La Demo requiere una base");
            // nextval conserva la evidencia aun si Flyway revierte su transacción.
            assertThat(jdbc.queryForObject("SELECT is_called FROM public.ddl_detectado", Boolean.class)).isFalse();
            assertThat(jdbc.queryForList(HISTORIAL)).isEqualTo(antes);
            assertThat(jdbc.queryForObject("SELECT to_regclass('lamontana.entorno_instalacion') IS NULL",
                    Boolean.class)).isTrue();
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
            arrancar(pg);
            var jdbc = new JdbcTemplate(pg.getPostgresDatabase());
            jdbc.update("UPDATE lamontana.flyway_schema_history SET " + alteracion + " WHERE version='41'");
            var antes = jdbc.queryForList(HISTORIAL);
            assertThatThrownBy(() -> arrancar(pg)).hasStackTraceContaining(diagnostico);
            assertThat(jdbc.queryForList(HISTORIAL)).isEqualTo(antes);
            assertThat(jdbc.queryForObject("SELECT modo FROM lamontana.entorno_instalacion", String.class)).isEqualTo("DEMO");
        }
    }
}
