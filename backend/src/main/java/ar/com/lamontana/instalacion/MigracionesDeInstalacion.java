package ar.com.lamontana.instalacion;

import java.util.List;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MigracionesDeInstalacion implements FlywayConfigurationCustomizer {
    private static final String ORIGEN = "LaMontana-Demo";
    private record Base(boolean tieneHistorial, boolean ocupada) {}
    private record Aplicada(String version, String script, boolean correcta, String origen) {}

    public MigracionesDeInstalacion(@Value("${lamontana.instalacion.modo}") String modo) {
        if (!"DEMO".equals(modo)) {
            throw new IllegalStateException("Este backend requiere el modo DEMO.");
        }
    }

    @Override
    public void customize(FluentConfiguration config) {
        // Aplicar el grupo bajo un único bloqueo de Flyway y conservar la procedencia en el historial.
        config.group(true).installedBy(ORIGEN).callbackLocations("classpath:db/demo")
                .validateOnMigrate(true).ignoreMigrationPatterns(new String[0]);
        var jdbc = new JdbcTemplate(config.getDataSource());
        var base = jdbc.queryForObject("""
                SELECT to_regclass('lamontana.flyway_schema_history') IS NOT NULL,
                       EXISTS (SELECT FROM pg_namespace n
                               LEFT JOIN pg_depend d ON d.refclassid = 'pg_namespace'::regclass
                                                    AND d.refobjid = n.oid AND d.deptype = 'n'
                               WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
                                 AND (n.nspname NOT IN ('public', 'lamontana') OR
                                      (d.objid IS NOT NULL AND
                                       (d.classid <> 'pg_class'::regclass OR
                                        d.objid IS DISTINCT FROM to_regclass('lamontana.flyway_schema_history')))))
                """, (rs, fila) -> new Base(rs.getBoolean(1), rs.getBoolean(2)));
        List<Aplicada> aplicadas = base.tieneHistorial() ? jdbc.query("""
                SELECT version, script, success, installed_by FROM lamontana.flyway_schema_history
                WHERE type <> 'SCHEMA' OR installed_by <> ? ORDER BY installed_rank
                """, (rs, fila) -> new Aplicada(rs.getString(1), rs.getString(2), rs.getBoolean(3), rs.getString(4)), ORIGEN)
                : List.of();
        if (aplicadas.isEmpty()) {
            if (base.ocupada()) throw baseAjena();
            return;
        }
        for (var m : aplicadas) {
            if (!ORIGEN.equals(m.origen())) throw baseAjena();
            if (!m.correcta()) throw new IllegalStateException("Migración fallida en el historial: " + m.script());
            if (m.version() != null && (!m.script().startsWith("V" + m.version() + "__")
                    || m.script().contains("/") || m.script().contains("\\")
                    || (config.getClassLoader().getResource("db/migration/" + m.script()) == null
                     && config.getClassLoader().getResource("db/imprenta/" + m.script()) == null))) {
                throw new IllegalStateException("Historial de migraciones incompatible en V" + m.version() + ": " + m.script());
            }
        }
        // Desde la primera versión publicada de la Demo, el marcador es obligatorio en cada arranque.
        if (!Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT to_regclass('lamontana.entorno_instalacion') IS NOT NULL", Boolean.class))
                || !Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT FROM lamontana.entorno_instalacion WHERE unica AND modo = 'DEMO')",
                Boolean.class))) {
            throw baseAjena();
        }
    }

    private IllegalStateException baseAjena() {
        return new IllegalStateException("La Demo requiere una base nueva o inicializada por La Montaña Demo.");
    }
}
