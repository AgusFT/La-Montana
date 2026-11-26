package ar.com.lamontana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.com.lamontana.archivos.ArchivoService;
import ar.com.lamontana.cotizaciones.CotizacionController;
import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.instalacion.MigracionesDeInstalacion;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pedidos.PedidoService;
import ar.com.lamontana.produccion.ProduccionController;
import ar.com.lamontana.produccion.ProduccionService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

class ImprentaDemoIntegrationTest {
    private static final UUID PEDIDO = UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0078");
    private static final UUID IMPRESORA = UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0077");
    private static final UUID ADMIN = UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0071");
    private static final UUID EMPLEADO = UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0072");
    private static final UUID CLIENTE = UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0073");
    private static final String INVENTARIO = """
            SELECT (SELECT count(*) FROM lamontana.pedido) AS pedidos,
                   (SELECT count(*) FROM lamontana.usuario JOIN lamontana.rol USING(id_rol)
                    WHERE rol.codigo='CLIENTE') AS clientes,
                   (SELECT count(*) FROM lamontana.pago) + (SELECT count(*) FROM lamontana.reembolso) AS caja,
                   (SELECT count(*) FROM lamontana.impresora) AS impresoras
            """;

    @TempDir Path almacenamiento;

    @Test
    void arrancaConOfertaOperativaYPermiteImprimirLaMuestraSinDuplicarlaAlReiniciar() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            var jdbc = new JdbcTemplate(pg.getPostgresDatabase());

            try (var app = iniciar(pg)) {
                assertThat(jdbc.queryForMap(INVENTARIO)).containsEntry("pedidos", 1L).containsEntry("clientes", 1L)
                        .containsEntry("caja", 1L).containsEntry("impresoras", 1L);
                assertThat(jdbc.queryForObject("""
                        SELECT codigo_publico FROM lamontana.inicializacion_sistema
                        JOIN lamontana.usuario ON id_usuario=id_usuario_propietario
                        WHERE es_administrador_propietario
                        """, UUID.class)).isEqualTo(ADMIN);

                String cliente = correo(jdbc, CLIENTE);
                String empleado = correo(jdbc, EMPLEADO);
                var pedidos = app.getBean(PedidoService.class);
                var detalle = pedidos.detalle(PEDIDO, cliente, false);
                assertThat(detalle.estado()).isEqualTo("APROBADO");
                assertThat(detalle.archivos()).hasSize(1);
                var pdf = detalle.archivos().get(0);
                var original = app.getBean(ArchivoService.class).contenido(detalle.cotizacion(), pdf.archivo(), null, cliente, false);
                try (var muestra = new ClassPathResource("demo/afiche-senderos.pdf").getInputStream()) {
                    assertThat(original.bytes()).isEqualTo(muestra.readAllBytes());
                }
                var sucursal = detalle.oferta().sucursal().codigoPublico();
                var organizacion = app.getBean(OrganizacionService.class).contexto(empleado);
                assertThat(organizacion.sucursales()).singleElement().satisfies(s -> assertThat(s.codigoPublico()).isEqualTo(sucursal));
                assertThat(organizacion.permisos()).contains("GESTIONAR_PRODUCCION", "CONTROLAR_CALIDAD")
                        .doesNotContain("REGISTRAR_DEVOLUCION");

                var cotizaciones = app.getBean(CotizacionService.class);
                var opciones = cotizaciones.opciones(cliente);
                assertThat(opciones.disponible()).isTrue();
                assertThat(opciones.destinosRetiro()).containsExactly(opciones.casaMatriz());
                var item = detalle.oferta().items().get(0);
                var cotizacion = cotizaciones.crear(new CotizacionController.Crear(UUID.randomUUID(),
                        opciones.configuracion(), opciones.revisionComercial(), sucursal,
                        List.of(new CotizacionController.Item(item.documento(), item.trabajo())),
                        detalle.oferta().modalidad(), null, null, detalle.oferta().medioPago(), null, sucursal, true), cliente);
                assertThat(cotizacion.oferta().total()).isEqualTo(detalle.oferta().total());
                assertThat(cotizacion.oferta().entrega().destinoSucursal().sucursal()).isEqualTo(sucursal);

                var produccion = app.getBean(ProduccionService.class);
                var disponible = produccion.consultar(PEDIDO, empleado);
                assertThat(disponible.bloqueos()).isEmpty();
                assertThat(disponible.items()).singleElement().satisfies(trabajo -> {
                    assertThat(trabajo.puedeIniciar()).isTrue();
                    assertThat(trabajo.impresoras()).singleElement().satisfies(i ->
                            assertThat(i.codigoPublico()).isEqualTo(IMPRESORA));
                });
                var iniciado = produccion.iniciar(PEDIDO, new ProduccionController.Inicio(UUID.randomUUID(),
                        disponible.version(), item.codigoPublico(), IMPRESORA, "Impresión de muestra", true), empleado);
                var trabajo = iniciado.items().get(0).trabajos().get(0);
                assertThat(trabajo.estado()).isEqualTo("IMPRIMIENDO");
                var terminado = produccion.cambiar(PEDIDO, trabajo.codigoPublico(), new ProduccionController.Cambio(
                        UUID.randomUUID(), iniciado.version(), ProduccionController.Accion.COMPLETAR, "Impresión terminada", true),
                        empleado);
                assertThat(terminado.items().get(0).trabajos().get(0).estado()).isEqualTo("COMPLETADO");
                var checklist = new ProduccionController.Checklist(true, true, true, true, true);
                var aprobado = produccion.calidad(PEDIDO, trabajo.codigoPublico(), new ProduccionController.Inspeccion(
                        UUID.randomUUID(), terminado.version(), ProduccionController.Resultado.APROBADO, checklist,
                        "Afiche de muestra revisado", true), empleado);
                assertThat(aprobado.estado()).isEqualTo("LISTO_PARA_ENTREGA");
            }

            var inventarioAntes = jdbc.queryForMap(INVENTARIO);
            try (var app = iniciar(pg)) {
                assertThat(jdbc.queryForMap(INVENTARIO)).isEqualTo(inventarioAntes);
                assertThat(app.getBean(PedidoService.class).detalle(PEDIDO, correo(jdbc, CLIENTE), false).estado())
                        .isEqualTo("LISTO_PARA_ENTREGA");
                assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.trabajo_impresion", Long.class)).isEqualTo(1L);
                assertThat(jdbc.queryForObject("""
                        SELECT count(*) FROM lamontana.flyway_schema_history WHERE version='48' AND success
                        """, Long.class)).isEqualTo(1L);
            }
        }
    }

    @Test
    void actualizaV47ConservandoLaIdentidadElPropietarioYLasVersionesVigentes() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            var ds = pg.getPostgresDatabase();
            flyway(ds, "47").migrate();
            var jdbc = new JdbcTemplate(ds);
            jdbc.execute("""
                    INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado,
                                                  es_administrador_propietario)
                    SELECT gen_random_uuid(),id_rol,'administracion@demo.invalid','hash-previo','Cuenta','Preexistente',
                           'ACTIVO',true FROM lamontana.rol WHERE codigo='ADMIN_ADMIN';
                    UPDATE lamontana.inicializacion_sistema
                    SET completada_en=now(),id_usuario_propietario=(SELECT id_usuario FROM lamontana.usuario)
                    WHERE unica;
                    INSERT INTO lamontana.catalogo_revision(codigo_publico,id_operacion,hash_solicitud,motivo,id_actor,
                                                            estado,vigente,activada_en)
                    SELECT gen_random_uuid(),gen_random_uuid(),repeat('a',64),'Catálogo anterior',id_usuario,
                           'VIGENTE',true,now() FROM lamontana.usuario;
                    INSERT INTO lamontana.configuracion_version(codigo_publico,numero_version,id_usuario_creador,
                                                                 estado,modelo,version)
                    SELECT gen_random_uuid(),1,id_usuario,'ACTIVA','MANUAL',2 FROM lamontana.usuario;
                    """);
            String existentes = """
                    SELECT e.unica, e.id_instalacion, e.modo, e.creada_en,
                           to_jsonb(i)::text AS inicializacion,to_jsonb(u)::text AS propietario,
                           to_jsonb(c)::text AS catalogo,to_jsonb(v)::text AS configuracion
                    FROM lamontana.entorno_instalacion e CROSS JOIN lamontana.inicializacion_sistema i
                    JOIN lamontana.usuario u ON id_usuario=id_usuario_propietario
                    JOIN lamontana.catalogo_revision c ON c.id_actor=u.id_usuario AND c.vigente
                    JOIN lamontana.configuracion_version v ON v.id_usuario_creador=u.id_usuario AND v.estado='ACTIVA'
                    """;
            var antes = jdbc.queryForMap(existentes);
            flyway(ds, "latest").migrate();
            assertThat(jdbc.queryForMap(existentes)).isEqualTo(antes);
            assertThat(jdbc.queryForObject("SELECT vigencia_inicio IS NULL FROM lamontana.entorno_instalacion WHERE unica",
                    Boolean.class)).as("El primer arranque registra el plazo del equipo después de migrar").isTrue();
            assertThat(jdbc.queryForObject("SELECT es_administrador_propietario FROM lamontana.usuario WHERE codigo_publico=?",
                    Boolean.class, ADMIN)).isFalse();
            assertThat(correo(jdbc, ADMIN)).endsWith("@demo.invalid").isNotEqualTo("administracion@demo.invalid");
            assertThat(jdbc.queryForMap("""
                    SELECT p.estado, v.estado AS configuracion, c.estado AS catalogo
                    FROM lamontana.pedido p JOIN lamontana.configuracion_version v USING(id_configuracion_version)
                    JOIN lamontana.cotizacion o ON o.id_cotizacion=p.id_cotizacion
                    JOIN lamontana.catalogo_revision c ON c.id_catalogo_revision=o.id_catalogo_revision
                    WHERE p.codigo_publico=?
                    """, PEDIDO)).containsEntry("estado", "APROBADO").containsEntry("configuracion", "HISTORICA")
                    .containsEntry("catalogo", "HISTORICA");
        }
    }

    @Test
    void revierteTodaLaPrecargaSiFallaUnDatoYAdmiteReintentar() throws Exception {
        try (var pg = EmbeddedPostgres.builder().start()) {
            var ds = pg.getPostgresDatabase();
            flyway(ds, "47").migrate();
            var jdbc = new JdbcTemplate(ds);
            var antes = jdbc.queryForMap(INVENTARIO);
            jdbc.execute("ALTER TABLE lamontana.pago ADD CONSTRAINT prueba_pago_rechazado CHECK(importe < 100)");
            assertThatThrownBy(() -> flyway(ds, "latest").migrate()).hasStackTraceContaining("prueba_pago_rechazado");
            assertThat(jdbc.queryForMap(INVENTARIO)).isEqualTo(antes);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario", Long.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_version", Long.class)).isZero();
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM lamontana.flyway_schema_history WHERE version='48'
                    """, Long.class)).isZero();

            jdbc.execute("ALTER TABLE lamontana.pago DROP CONSTRAINT prueba_pago_rechazado");
            flyway(ds, "latest").migrate();
            assertThat(jdbc.queryForMap(INVENTARIO)).containsEntry("pedidos", 1L).containsEntry("clientes", 1L)
                    .containsEntry("caja", 1L).containsEntry("impresoras", 1L);
        }
    }

    private ConfigurableApplicationContext iniciar(EmbeddedPostgres pg) {
        return new SpringApplicationBuilder(LaMontanaApplication.class).run(
                "--server.port=0", "--spring.datasource.username=postgres", "--spring.datasource.password=",
                "--spring.datasource.url=" + pg.getJdbcUrl("postgres", "postgres"),
                "--lamontana.archivos.directorio=" + almacenamiento.resolve("archivos"),
                "--lamontana.web.entrada=" + almacenamiento.resolve("entrada"),
                "--lamontana.web.biblioteca=" + almacenamiento.resolve("biblioteca"));
    }

    private static Flyway flyway(DataSource ds, String target) {
        var cfg = Flyway.configure().dataSource(ds).schemas("lamontana").defaultSchema("lamontana")
                .locations("classpath:db/migration", "classpath:db/imprenta").target(target);
        new MigracionesDeInstalacion("DEMO").customize(cfg);
        return cfg.load();
    }

    private static String correo(JdbcTemplate jdbc, UUID cuenta) {
        return jdbc.queryForObject("SELECT correo FROM lamontana.usuario WHERE codigo_publico=?", String.class, cuenta);
    }
}
