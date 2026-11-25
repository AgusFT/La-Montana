package ar.com.lamontana.instalacion;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@DependsOnDatabaseInitialization
public class EntornoInstalacion {
    private final JdbcTemplate jdbc;

    public EntornoInstalacion(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void verificar() {
        if (!Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT FROM lamontana.entorno_instalacion WHERE unica AND modo='DEMO')", Boolean.class))) {
            throw new IllegalStateException("La instalación requiere una base identificada como DEMO.");
        }
    }

    @GetMapping("/api/sistema/entorno")
    public Map<String,Object> estado() {
        return Map.of("modo", "DEMO", "simulacionesPermitidas", true, "correoDePrueba", true);
    }
}
