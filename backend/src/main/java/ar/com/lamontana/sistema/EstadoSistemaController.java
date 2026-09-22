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
        jdbc.queryForObject("SELECT 1", Integer.class);
        return new EstadoSistema("La Montaña", "0.1.0", "BASE_TECNICA", false, false);
    }

    public record EstadoSistema(String producto, String version, String etapa,
                                boolean configuracionDisponible, boolean operacionDisponible) {}
}
