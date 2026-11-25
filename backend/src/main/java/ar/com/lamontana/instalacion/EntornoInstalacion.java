package ar.com.lamontana.instalacion;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntornoInstalacion {
    private final JdbcTemplate jdbc;
    private final String modo;
    public EntornoInstalacion(JdbcTemplate jdbc,@Value("${lamontana.instalacion.modo:DESARROLLO}") String modo){this.jdbc=jdbc;this.modo=modo;}
    @PostConstruct
    void verificar(){
        if(!modo.equals("DESARROLLO")&&!modo.equals("STANDARD"))throw new IllegalStateException("Modo de instalación desconocido.");
        jdbc.update("INSERT INTO lamontana.entorno_instalacion(unica,id_instalacion,modo) VALUES (true,?,?) ON CONFLICT (unica) DO NOTHING",UUID.randomUUID(),modo);
        String guardado=jdbc.queryForObject("SELECT modo FROM lamontana.entorno_instalacion WHERE unica",String.class);
        if(!modo.equals(guardado))throw new IllegalStateException("El modo no coincide con la base. Standard requiere una base nueva; no se convierten datos de Desarrollo.");
    }
    public boolean desarrollo(){return modo.equals("DESARROLLO");}
    @GetMapping("/api/sistema/entorno")
    public Map<String,Object> estado(){return Map.of("modo",modo,"simulacionesPermitidas",desarrollo(),"correoDePrueba",desarrollo());}
}
