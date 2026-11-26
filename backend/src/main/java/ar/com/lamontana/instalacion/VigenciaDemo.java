package ar.com.lamontana.instalacion;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

@Component
@DependsOnDatabaseInitialization
public class VigenciaDemo {
    private static final Logger log = LoggerFactory.getLogger(VigenciaDemo.class);
    private static final Duration PLAZO = Duration.ofHours(360);
    private static final long DIA_MS = Duration.ofDays(1).toMillis();
    private static final Object BLOQUEO_JVM = new Object();
    static final String FIN = "Fin de la Demo, para adquirir el producto comunicate con administracion@yg-soluciones.com";

    public enum Situacion { VIGENTE, RELOJ_ATRASADO, VENCIDA, NO_DISPONIBLE }
    public record Estado(Situacion estado, Instant inicio, Instant venceEn, long restanteMillis,
                         long diasRestantes, String mensaje) {
        public String getCodigo() { return estado.name(); }
    }
    private record Marca(Instant inicio, Instant ultimo) {}
    private record Ancla(Instant creacionBase, Instant inicioRegistrado) {}

    private final Clock reloj;
    private final Path directorio;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate independiente;
    private Ancla ancla;
    private boolean errorNotificado;

    public VigenciaDemo(JdbcTemplate jdbc, PlatformTransactionManager transacciones, ObjectProvider<Clock> reloj,
                        @Value("${lamontana.demo.vigencia-directorio}") Path directorio) {
        this.jdbc = jdbc;
        this.reloj = reloj.getIfAvailable(Clock::systemUTC);
        this.directorio = directorio.toAbsolutePath().normalize();
        this.independiente = new TransactionTemplate(transacciones);
        this.independiente.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean operativa() { return evaluar().estado() == Situacion.VIGENTE; }

    public Estado evaluar() {
        // El bloqueo de archivo coordina dos procesos. El monitor evita bloqueos solapados dentro de una JVM.
        synchronized (BLOQUEO_JVM) {
            try {
                Files.createDirectories(directorio);
                try (var canal = FileChannel.open(directorio.resolve("vigencia.lock"), CREATE, WRITE);
                     var bloqueo = canal.lock()) {
                    Estado estado = evaluarConBloqueo();
                    errorNotificado = false;
                    return estado;
                }
            } catch (IOException | java.time.DateTimeException | DataAccessException | ArithmeticException e) {
                if (!errorNotificado) log.error("No se pudo verificar la vigencia local de la Demo.", e);
                errorNotificado = true;
                return new Estado(Situacion.NO_DISPONIBLE, null, null, 0, 0,
                        "No se pudo verificar la vigencia de la Demo en este equipo. Revisá su almacenamiento local.");
            }
        }
    }

    private Estado evaluarConBloqueo() throws IOException {
        if (ancla == null) {
            ancla = jdbc.queryForObject("""
                    SELECT creada_en, vigencia_inicio FROM lamontana.entorno_instalacion WHERE unica AND modo='DEMO'
                    """, (rs, fila) -> {
                var registrado = rs.getObject("vigencia_inicio", OffsetDateTime.class);
                return new Ancla(rs.getObject("creada_en", OffsetDateTime.class).toInstant(),
                        registrado == null ? null : registrado.toInstant());
            });
        }
        Path archivo = directorio.resolve("vigencia.txt");
        Marca marca;
        try {
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.US_ASCII);
            if (lineas.size() != 2) throw new IOException("Marca de vigencia incompleta.");
            marca = new Marca(Instant.parse(lineas.get(0)), Instant.parse(lineas.get(1)));
            if (marca.ultimo().isBefore(marca.inicio())) throw new IOException("Marca de vigencia inconsistente.");
        } catch (NoSuchFileException ausente) {
            if (ancla.inicioRegistrado() != null) throw new IOException("Falta la marca de vigencia registrada.", ausente);
            marca = null;
        }

        Instant inicio = ancla.inicioRegistrado() == null ? ancla.creacionBase() : ancla.inicioRegistrado();
        if (marca != null && marca.inicio().isBefore(inicio)) inicio = marca.inicio();
        Instant ultimo = marca == null ? inicio : marca.ultimo();
        Instant ahora = reloj.instant();
        Instant fin = inicio.plus(PLAZO);
        // Una vez observado el vencimiento queda registrado, incluso si después se atrasa el reloj.
        Instant observado = ahora.isAfter(ultimo) ? ahora : ultimo;
        if (observado.isAfter(fin)) observado = fin;
        var nueva = new Marca(inicio, observado);
        if (!nueva.equals(marca)) guardar(archivo, nueva);
        if (!inicio.equals(ancla.inicioRegistrado())) {
            Instant registrar = inicio;
            independiente.executeWithoutResult(tx -> jdbc.update("""
                    UPDATE lamontana.entorno_instalacion SET vigencia_inicio=?
                    WHERE unica AND modo='DEMO'
                    """, java.sql.Timestamp.from(registrar)));
            ancla = new Ancla(ancla.creacionBase(), inicio);
        }
        if (!observado.isBefore(fin)) return new Estado(Situacion.VENCIDA, inicio, fin, 0, 0, FIN);
        Duration pendiente = Duration.between(observado, fin);
        long ms = (pendiente.toNanos() + 999_999L) / 1_000_000L;
        long dias = (ms + DIA_MS - 1) / DIA_MS;
        if (ahora.isBefore(ultimo)) {
            return new Estado(Situacion.RELOJ_ATRASADO, inicio, fin, ms, dias,
                    "El reloj del equipo está atrasado. Corregí la fecha y la hora para volver a comprobar la vigencia de la Demo.");
        }
        return new Estado(Situacion.VIGENTE, inicio, fin, ms, dias, "Demo de La Montaña");
    }

    private void guardar(Path archivo, Marca marca) throws IOException {
        Path temporal = Files.createTempFile(directorio, ".vigencia-", ".tmp");
        try {
            var datos = ByteBuffer.wrap((marca.inicio() + "\n" + marca.ultimo() + "\n").getBytes(StandardCharsets.US_ASCII));
            try (var canal = FileChannel.open(temporal, WRITE)) {
                while (datos.hasRemaining()) canal.write(datos);
                canal.force(true);
            }
            Files.move(temporal, archivo, ATOMIC_MOVE, REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }
}
