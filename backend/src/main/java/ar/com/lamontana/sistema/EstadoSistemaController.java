package ar.com.lamontana.sistema;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EstadoSistemaController {
    private final JdbcTemplate jdbc;

    public EstadoSistemaController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/sistema/estado")
    public EstadoSistema estado() {
        // La disponibilidad implica una conexión real; no se devuelve éxito si falla PostgreSQL.
        boolean instalada=Boolean.TRUE.equals(jdbc.queryForObject("SELECT completada_en IS NOT NULL FROM lamontana.inicializacion_sistema WHERE unica",Boolean.class));
        boolean activa=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE estado='ACTIVA')",Boolean.class));
        return new EstadoSistema("La Montaña", "0.1.0", !instalada?"IDENTIDAD_INICIAL":activa?"OPERATIVA":"CONFIGURACION_PENDIENTE", instalada, instalada, activa);
    }

    public record EstadoSistema(String producto, String version, String etapa, boolean accesoDisponible,
                                boolean configuracionDisponible, boolean operacionDisponible) {}
}
