package ar.com.lamontana.instalacion;

import ar.com.lamontana.archivos.ArchivosPrivados;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Instala el original del pedido ficticio en el mismo almacén privado que usa producción. */
@Component
public class MuestraImpresionDemo implements ApplicationRunner {
    private static final UUID ARCHIVO = UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0081");
    private record Original(String hash, long bytes) {}
    private final JdbcTemplate jdbc;
    private final ArchivosPrivados archivos;

    public MuestraImpresionDemo(JdbcTemplate jdbc, ArchivosPrivados archivos) {
        this.jdbc = jdbc;
        this.archivos = archivos;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        // Serializa arranques y la eliminación por política de conservación.
        jdbc.execute("SELECT pg_advisory_xact_lock(764003)");
        var original = jdbc.query("""
                SELECT a.sha256, a.cantidad_bytes FROM lamontana.archivo_almacenado a
                WHERE a.codigo_publico=? AND a.estado='VALIDO' AND NOT EXISTS (
                    SELECT FROM lamontana.eliminacion_archivo e WHERE e.id_archivo_almacenado=a.id_archivo_almacenado)
                """, (rs, fila) -> new Original(rs.getString(1), rs.getLong(2)), ARCHIVO).stream().findFirst();
        if (original.isEmpty()) return;
        var datos = original.get();
        var carpeta = archivos.carpeta(ARCHIVO);
        if (Files.exists(carpeta.resolve("original.pdf"), LinkOption.NOFOLLOW_LINKS)) {
            archivos.original(ARCHIVO, datos.hash(), datos.bytes());
            return;
        }
        // Una interrupción antes del movimiento atómico puede dejar sólo el parcial de esta muestra.
        Files.deleteIfExists(carpeta.resolve("original.part"));
        try (var input = new ClassPathResource("demo/afiche-senderos.pdf").getInputStream()) {
            var recibido = archivos.recibir(ARCHIVO, input);
            if (!datos.hash().equals(recibido.sha256()) || datos.bytes() != recibido.bytes()) {
                throw new IOException("El afiche de muestra difiere del original registrado en la Demo.");
            }
            archivos.publicar(ARCHIVO);
        }
    }
}
