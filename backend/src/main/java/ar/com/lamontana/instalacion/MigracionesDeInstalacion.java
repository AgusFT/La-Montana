package ar.com.lamontana.instalacion;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Verifica los nombres registrados además de las sumas que comprueba Flyway. */
@Component
public class MigracionesDeInstalacion implements FlywayConfigurationCustomizer {
    private record Aplicada(String version, String script, boolean correcta) {}

    @Override
    public void customize(FluentConfiguration config) {
        // Validar también versiones futuras/desconocidas antes de aplicar SQL pendiente.
        config.validateOnMigrate(true).ignoreMigrationPatterns(new String[0]);
        var jdbc = new JdbcTemplate(config.getDataSource());
        boolean tieneHistorial = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT to_regclass('lamontana.flyway_schema_history') IS NOT NULL", Boolean.class));
        if (!tieneHistorial) return;

        var aplicadas = jdbc.query("""
                SELECT version, script, success FROM lamontana.flyway_schema_history
                WHERE type <> 'SCHEMA' ORDER BY installed_rank
                """, (rs, fila) -> new Aplicada(rs.getString(1), rs.getString(2), rs.getBoolean(3)));
        for (var m : aplicadas) {
            if (!m.correcta()) throw new IllegalStateException("Migración fallida en el historial: " + m.script());
            if (m.version() != null && (!m.script().startsWith("V" + m.version() + "__")
                    || m.script().contains("/") || m.script().contains("\\")
                    || config.getClassLoader().getResource("db/migration/" + m.script()) == null)) {
                throw new IllegalStateException("Historial de migraciones incompatible en V" + m.version() + ": " + m.script());
            }
        }
    }
}
