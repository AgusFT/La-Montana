package ar.com.lamontana.archivos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlmacenamientoConfiguration {
    @Bean
    Path directorioArchivos(@Value("${lamontana.archivos.directorio}") String directorio) throws IOException {
        Path path = Files.createDirectories(Path.of(directorio).toAbsolutePath().normalize());
        Path probe = Files.createTempFile(path, ".comprobacion-", ".tmp");
        Files.delete(probe);
        return path;
    }
}
