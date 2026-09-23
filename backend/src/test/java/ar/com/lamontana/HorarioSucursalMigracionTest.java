//#region ENCABEZADO · HorarioSucursalMigracionTest.java
/*
 * ========================================================================
 * ARCHIVO: HorarioSucursalMigracionTest.java
 * ========================================================================
 * FUNCIÓN
 * Comprueba que la migración de horarios habituales conserva las versiones existentes y copia
 * únicamente calendarios activos completos.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] void migrarConservaVersionesYCopiaSoloElHorarioActivoCompleto() throws Exception
 *   Caso de prueba.
 * - [private] long sucursal(JdbcTemplate jdbc, long actor, String codigo)
 * - [private] long config(JdbcTemplate jdbc, long actor, int numero, String estado)
 * - [private] void calendario(JdbcTemplate jdbc, long config, long branch, int cantidad, String
 *   apertura, String cierre)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - HorarioSucursalMigracionTest (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class HorarioSucursalMigracionTest {
    @Test void migrarConservaVersionesYCopiaSoloElHorarioActivoCompleto() throws Exception {
        try (var pg=EmbeddedPostgres.builder().start()) {
            var db=pg.getPostgresDatabase();
            Flyway.configure().dataSource(db).defaultSchema("lamontana").schemas("lamontana").target("35").load().migrate();
            var jdbc=new JdbcTemplate(db);
            long actor=jdbc.queryForObject("""
                INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado)
                VALUES (?,(SELECT id_rol FROM lamontana.rol WHERE codigo='ADMIN_ADMIN'),'migracion@example.test','hash-fixture','Admin','Test','ACTIVO') RETURNING id_usuario
                """,Long.class,UUID.randomUUID());
            long a=sucursal(jdbc,actor,"A"),b=sucursal(jdbc,actor,"B"),c=sucursal(jdbc,actor,"C");
            long activa=config(jdbc,actor,1,"ACTIVA"),borrador=config(jdbc,actor,2,"EN_PREPARACION");
            calendario(jdbc,activa,a,7,"09:00","18:00");
            calendario(jdbc,borrador,a,7,"10:00","17:00");
            calendario(jdbc,borrador,b,7,"10:00","17:00");
            calendario(jdbc,activa,c,3,"09:00","18:00");
            var versiones=jdbc.queryForList("SELECT * FROM lamontana.configuracion_version ORDER BY id_configuracion_version");
            var horarios=jdbc.queryForList("SELECT * FROM lamontana.horario_sucursal ORDER BY id_configuracion_version,id_sucursal,dia_semana");
            var sucursales=jdbc.queryForList("SELECT * FROM lamontana.sucursal ORDER BY id_sucursal");
            Flyway.configure().dataSource(db).defaultSchema("lamontana").schemas("lamontana").load().migrate();
            assertThat(jdbc.queryForList("SELECT * FROM lamontana.configuracion_version ORDER BY id_configuracion_version")).isEqualTo(versiones);
            assertThat(jdbc.queryForList("SELECT * FROM lamontana.horario_sucursal ORDER BY id_configuracion_version,id_sucursal,dia_semana")).isEqualTo(horarios);
            assertThat(jdbc.queryForList("SELECT * FROM lamontana.sucursal ORDER BY id_sucursal")).isEqualTo(sucursales);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.sucursal_horario_atencion",Integer.class)).isEqualTo(7);
            assertThat(jdbc.queryForObject("SELECT to_char(hora_desde,'HH24:MI') FROM lamontana.sucursal_horario_atencion WHERE id_sucursal=? AND dia_semana=1",String.class,a)).isEqualTo("09:00");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.sucursal_horario_atencion WHERE id_sucursal IN (?,?)",Integer.class,b,c)).isZero();
        }
    }
    private long sucursal(JdbcTemplate jdbc,long actor,String codigo) {
        return jdbc.queryForObject("""
            INSERT INTO lamontana.sucursal(codigo_publico,codigo,nombre,calle,numero,localidad,provincia,codigo_postal,zona_horaria,estado,id_usuario_alta)
            VALUES (?,?,?,'Calle','1','CABA','CABA','1000','America/Argentina/Buenos_Aires','ACTIVA',?) RETURNING id_sucursal
            """,Long.class,UUID.randomUUID(),codigo,codigo,actor);
    }
    private long config(JdbcTemplate jdbc,long actor,int numero,String estado) {
        return jdbc.queryForObject("INSERT INTO lamontana.configuracion_version(codigo_publico,numero_version,id_usuario_creador,estado) VALUES (?,?,?,?) RETURNING id_configuracion_version",Long.class,UUID.randomUUID(),numero,actor,estado);
    }
    private void calendario(JdbcTemplate jdbc,long config,long branch,int cantidad,String apertura,String cierre) {
        jdbc.update("INSERT INTO lamontana.configuracion_horario_sucursal(id_configuracion_version,id_sucursal,zona_horaria) VALUES (?,?,'America/Argentina/Buenos_Aires')",config,branch);
        for(int dia=1;dia<=cantidad;dia++)jdbc.update("INSERT INTO lamontana.horario_sucursal(id_configuracion_version,id_sucursal,dia_semana,habilitado,hora_desde,hora_hasta) VALUES (?,?,?,true,?::time,?::time)",config,branch,dia,apertura,cierre);
    }
}
