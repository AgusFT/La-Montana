//#region ENCABEZADO · CatalogoMigracionV8IntegrationTest.java
/*
 * ========================================================================
 * ARCHIVO: CatalogoMigracionV8IntegrationTest.java
 * ========================================================================
 * FUNCIÓN
 * Comprueba que migrar un catálogo poblado de V7 a V8 conserva las configuraciones guardadas y los
 * comprobantes idempotentes anteriores.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] void migrarV7PobladaConservaSnapshotsYComprobantesIdempotentesAnteriores() throws
 *   Exception
 *   Caso de prueba.
 * - [private] void insertarRevisionV7(JdbcTemplate jdbc, UUID codigo, String comando, boolean
 *   vigente, Instant creada, long actor) throws Exception
 * - [private] String solicitudAnterior(UUID base, UUID operacion, String precio, String
 *   terminacion)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - CatalogoMigracionV8IntegrationTest (clase).
 * - CatalogoMigracionV8IntegrationTest.Transacciones (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.catalogo.CatalogoController;
import ar.com.lamontana.catalogo.CatalogoService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tools.jackson.databind.json.JsonMapper;

class CatalogoMigracionV8IntegrationTest {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String ACTOR="admin-v7@example.test";
    private static final UUID FORMATO=UUID.fromString("00000000-0000-0000-0000-000000000001"),
            PAPEL=UUID.fromString("00000000-0000-0000-0000-000000000002"),
            IMPRESION=UUID.fromString("00000000-0000-0000-0000-000000000003"),
            TERMINACION=UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test void migrarV7PobladaConservaSnapshotsYComprobantesIdempotentesAnteriores() throws Exception {
        try(var pg=EmbeddedPostgres.builder().start()) {
            DataSource dataSource=pg.getPostgresDatabase();
            var anterior=Flyway.configure().dataSource(dataSource).defaultSchema("lamontana").schemas("lamontana").target("7").load();
            anterior.migrate();
            assertThat(anterior.info().current().getVersion().getVersion()).isEqualTo("7");
            var jdbc=new JdbcTemplate(dataSource);
            long actor=jdbc.queryForObject("""
                    INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado)
                    VALUES (?,(SELECT id_rol FROM lamontana.rol WHERE codigo='ADMIN_ADMIN'),?,'hash-solo-fixture','Admin','Anterior','ACTIVO') RETURNING id_usuario
                    """,Long.class,UUID.randomUUID(),ACTOR);
            jdbc.update("INSERT INTO lamontana.formato(codigo_publico,codigo,nombre,ancho_mm,alto_mm) VALUES (?,'F','Formato anterior',210,297)",FORMATO);
            jdbc.update("INSERT INTO lamontana.papel(codigo_publico,codigo,nombre,gramaje_g_m2,terminacion_tipo) VALUES (?,'P','Papel anterior',80,'Mate')",PAPEL);
            jdbc.update("INSERT INTO lamontana.servicio(codigo_publico,codigo,nombre,tipo) VALUES (?,'IMP','Impresion anterior','IMPRESION'),(?,'TERM','Terminacion anterior','TERMINACION')",IMPRESION,TERMINACION);

            UUID historica=UUID.randomUUID(),vigente=UUID.randomUUID();
            String comandoHistorico=solicitudAnterior(null,UUID.randomUUID(),"12.34","7.89");
            String comandoVigente=solicitudAnterior(historica,UUID.randomUUID(),"19.99","8.90");
            Instant fechaHistorica=Instant.parse("2026-09-01T12:00:00Z"),fechaVigente=Instant.parse("2026-09-02T12:00:00Z");
            insertarRevisionV7(jdbc,historica,comandoHistorico,false,fechaHistorica,actor);
            insertarRevisionV7(jdbc,vigente,comandoVigente,true,fechaVigente,actor);
            var comprobantesAntes=jdbc.queryForList("SELECT codigo_publico,id_operacion,hash_solicitud,id_actor,creada_en FROM lamontana.catalogo_revision ORDER BY id_catalogo_revision");
            var tarifasAntes=jdbc.queryForList("SELECT * FROM lamontana.tarifa_impresion ORDER BY id_tarifa_impresion");
            var serviciosAntes=jdbc.queryForList("SELECT * FROM lamontana.configuracion_servicio ORDER BY id_configuracion_servicio");
            var compatibilidadesAntes=jdbc.queryForList("SELECT * FROM lamontana.compatibilidad_servicio ORDER BY id_configuracion_servicio");

            var actual=Flyway.configure().dataSource(dataSource).defaultSchema("lamontana").schemas("lamontana").target("8").load();
            assertThat(actual.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(actual.info().current().getVersion().getVersion()).isEqualTo("8");
            assertThat(jdbc.queryForList("SELECT codigo_publico,id_operacion,hash_solicitud,id_actor,creada_en FROM lamontana.catalogo_revision ORDER BY id_catalogo_revision")).isEqualTo(comprobantesAntes);
            assertThat(jdbc.queryForList("SELECT * FROM lamontana.tarifa_impresion ORDER BY id_tarifa_impresion")).isEqualTo(tarifasAntes);
            assertThat(jdbc.queryForList("SELECT * FROM lamontana.configuracion_servicio ORDER BY id_configuracion_servicio")).isEqualTo(serviciosAntes);
            assertThat(jdbc.queryForList("SELECT * FROM lamontana.compatibilidad_servicio ORDER BY id_configuracion_servicio")).isEqualTo(compatibilidadesAntes);

            Flyway.configure().dataSource(dataSource).defaultSchema("lamontana").schemas("lamontana").load().migrate();

            // Usa el servicio real con su proxy transaccional; la migración ya ocurrió sobre datos V7.
            try(var context=new AnnotationConfigApplicationContext()) {
                context.registerBean(DataSource.class,()->dataSource);
                context.registerBean(JdbcTemplate.class,()->jdbc);
                context.registerBean(PlatformTransactionManager.class,()->new JdbcTransactionManager(dataSource));
                context.registerBean(CatalogoService.class);
                context.register(Transacciones.class);
                context.refresh();
                var catalogo=context.getBean(CatalogoService.class);
                var estado=catalogo.estado();
                assertThat(estado.actual().codigoPublico()).isEqualTo(vigente);
                assertThat(estado.actual().estado()).isEqualTo(CatalogoService.EstadoRevision.VIGENTE);
                assertThat(estado.actual().activadaEn()).isEqualTo(fechaVigente);
                assertThat(estado.programada()).isNull();
                assertThat(estado.historial()).hasSize(2);
                var previa=catalogo.revision(historica);
                assertThat(previa.estado()).isEqualTo(CatalogoService.EstadoRevision.HISTORICA);
                assertThat(previa.activadaEn()).isEqualTo(fechaHistorica);
                assertThat(previa.programadaPara()).isNull();
                assertThat(previa.tarifas().get(0).precio()).isEqualByComparingTo("12.34");
                assertThat(estado.actual().tarifas().get(0).precio()).isEqualByComparingTo("19.99");
                assertThat(previa.servicios().get(1).precio()).isEqualByComparingTo("7.89");
                assertThat(estado.actual().servicios().get(1).precio()).isEqualByComparingTo("8.90");

                // JSON canónico del DTO V7: no contiene el campo nuevo ni usa la función de huella productiva.
                var retryAnterior=JSON.readValue(comandoHistorico,CatalogoController.NuevaRevision.class);
                var retryVigente=JSON.readValue(comandoVigente,CatalogoController.NuevaRevision.class);
                assertThat(retryAnterior.programadaPara()).isNull();
                assertThat(retryVigente.programadaPara()).isNull();
                assertThat(catalogo.guardar(retryAnterior,ACTOR)).isEqualTo(previa);
                assertThat(catalogo.guardar(retryVigente,ACTOR)).isEqualTo(estado.actual());
                assertThat(catalogo.estado()).isEqualTo(estado);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.catalogo_revision",Integer.class)).isEqualTo(2);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_catalogo",Integer.class)).isEqualTo(2);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.catalogo_cancelacion",Integer.class)).isZero();
            }
        }
    }

    private void insertarRevisionV7(JdbcTemplate jdbc,UUID codigo,String comando,boolean vigente,Instant creada,long actor) throws Exception {
        var in=JSON.readValue(comando,CatalogoController.NuevaRevision.class);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(comando.getBytes(StandardCharsets.UTF_8)));
        long id=jdbc.queryForObject("""
                INSERT INTO lamontana.catalogo_revision(codigo_publico,id_revision_base,id_operacion,hash_solicitud,motivo,id_actor,creada_en,vigente)
                VALUES (?,(SELECT id_catalogo_revision FROM lamontana.catalogo_revision WHERE codigo_publico=?),?,?,?,?,?,?) RETURNING id_catalogo_revision
                """,Long.class,codigo,in.versionBase(),in.operacion(),hash,in.motivo(),actor,Timestamp.from(creada),vigente);
        var tarifa=in.tarifas().get(0);
        jdbc.update("""
                INSERT INTO lamontana.tarifa_impresion(id_catalogo_revision,id_formato,id_papel,modo_color,precio_por_carilla,recargo_doble_faz,habilitada)
                VALUES (?,(SELECT id_formato FROM lamontana.formato WHERE codigo_publico=?),(SELECT id_papel FROM lamontana.papel WHERE codigo_publico=?),?,?,?,?)
                """,id,tarifa.formato(),tarifa.papel(),tarifa.color().name(),tarifa.precio(),tarifa.recargoDobleFaz(),tarifa.habilitada());
        for(var servicio:in.servicios()) {
            long configuracion=jdbc.queryForObject("""
                    INSERT INTO lamontana.configuracion_servicio(id_catalogo_revision,id_servicio,nombre_visible,base_precio,precio_unitario,preparacion_minutos,habilitado)
                    VALUES (?,(SELECT id_servicio FROM lamontana.servicio WHERE codigo_publico=?),?,?,?,?,?) RETURNING id_configuracion_servicio
                    """,Long.class,id,servicio.servicio(),servicio.nombreVisible(),servicio.basePrecio().name(),servicio.precio(),servicio.preparacionMinutos(),servicio.habilitado());
            for(var compatibilidad:servicio.compatibilidades()) jdbc.update("""
                    INSERT INTO lamontana.compatibilidad_servicio(id_configuracion_servicio,id_formato,id_papel)
                    VALUES (?,(SELECT id_formato FROM lamontana.formato WHERE codigo_publico=?),(SELECT id_papel FROM lamontana.papel WHERE codigo_publico=?))
                    """,configuracion,compatibilidad.formato(),compatibilidad.papel());
        }
        jdbc.update("INSERT INTO lamontana.evento_catalogo(id_actor,tipo,codigo_objeto,fecha) VALUES (?,'REVISION_ACTIVADA',?,?)",actor,codigo,Timestamp.from(creada));
    }

    private String solicitudAnterior(UUID base,UUID operacion,String precio,String terminacion) {
        return "{\"versionBase\":"+(base==null?"null":"\""+base+"\"")+",\"operacion\":\""+operacion+"\",\"motivo\":\"Lista anterior\","
                +"\"tarifas\":[{\"formato\":\""+FORMATO+"\",\"papel\":\""+PAPEL+"\",\"color\":\"BLANCO_NEGRO\",\"precio\":"+precio+",\"recargoDobleFaz\":0.55,\"habilitada\":true}],"
                +"\"servicios\":[{\"servicio\":\""+IMPRESION+"\",\"nombreVisible\":\"Impresion anterior\",\"basePrecio\":\"POR_CARILLA\",\"precio\":0.00,\"preparacionMinutos\":5,\"habilitado\":true,\"compatibilidades\":[]},"
                +"{\"servicio\":\""+TERMINACION+"\",\"nombreVisible\":\"Terminacion anterior\",\"basePrecio\":\"FIJO_POR_ITEM\",\"precio\":"+terminacion+",\"preparacionMinutos\":10,\"habilitado\":true,\"compatibilidades\":[{\"formato\":\""+FORMATO+"\",\"papel\":\""+PAPEL+"\"}]}]}";
    }

    @Configuration(proxyBeanMethods=false)
    @EnableTransactionManagement
    static class Transacciones {}
}
