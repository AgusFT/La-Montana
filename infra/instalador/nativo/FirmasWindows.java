import java.io.*;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardCopyOption.*;

final class FirmasWindows {
    interface Ejecutar {
        void run(String nombre, Path directorio, int segundos, String... argumentos) throws Exception;
    }
    static Path preparar(Path app, Path datos, Path origen, Ejecutar ejecutar) throws Exception {
        Path db = Files.createDirectories(datos.resolve("firmas"));
        for (String nombre : List.of("daily.cld", "main.cvd")) {
            Path copia = db.resolve(nombre), original = app.resolve("firmas").resolve(nombre);
            if (!Files.isRegularFile(copia) || Files.mismatch(original, copia) != -1) {
                Path temporal = Files.createTempFile(db, "base-", ".tmp");
                try {
                    Files.copy(original, temporal, REPLACE_EXISTING);
                    Files.move(temporal, copia, ATOMIC_MOVE, REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(temporal);
                }
            }
        }
        Path guardada = db.resolve("bytecode.cvd");
        if (origen != null) {
            instalarBytecode(app, datos, origen, guardada, ejecutar);
        } else if (Files.exists(guardada)) {
            try {
                instalarBytecode(app, datos, guardada, guardada, ejecutar);
            } catch (IOException error) {
                // Una copia opcional dañada no debe impedir el análisis con las bases del instalador.
                guardarAviso(datos, error);
                return app.resolve("firmas");
            }
        }
        return db;
    }
    static void actualizar(Path app, Path datos, Ejecutar ejecutar) throws Exception {
        Path candidato = descargar(app, datos, ejecutar);
        instalarBytecode(app, datos, candidato, datos.resolve("firmas/bytecode.cvd"), ejecutar);
    }
    static void guardarAviso(Path datos, Exception error) {
        try {
            Files.writeString(datos.resolve("registros/actualizacion-firmas.log"),
                java.time.Instant.now() + " · Se conservaron las firmas locales. " + error.getMessage() + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // El registro de una actualización opcional no interviene en el análisis.
        }
    }
    private static void instalarBytecode(Path app, Path datos, Path origen, Path guardada, Ejecutar ejecutar) throws Exception {
        if (!origen.isAbsolute() || origen.toString().startsWith("\\\\") || !Files.isRegularFile(origen, LinkOption.NOFOLLOW_LINKS)
                || Files.size(origen) > 16 * 1024 * 1024) {
            throw new IOException("Base de bytecode inválida.");
        }
        Path temp = Files.createTempDirectory(datos, "validar-cvd-");
        try {
            Path candidato = temp.resolve("bytecode.cvd");
            try (var contenido = Files.newInputStream(origen)) {
                Files.copy(contenido, candidato);
            }
            String sigtool = app.resolve("clamav/sigtool.exe").toString();
            ejecutar.run("verificar-firmas", temp, 120, sigtool, "--info=bytecode.cvd");
            ejecutar.run("verificar-firmas", temp, 120, sigtool, "--unpack=bytecode.cvd");
            Path info = temp.resolve("bytecode.info");
            if (!Files.isRegularFile(info)) {
                throw new IOException("El archivo firmado no es la base bytecode de ClamAV.");
            }
            String cabecera;
            try(var r = Files.newBufferedReader(info)) {
                cabecera = r.readLine();
            }
            if (cabecera == null || !cabecera.matches("ClamAV-VDB:[^:]+:[0-9]+:[0-9]+:[0-9]+:.*")) {
                throw new IOException("La base bytecode no tiene una cabecera válida.");
            }
            var ayuda = new ProcessBuilder(sigtool, "--help").redirectErrorStream(true).start();
            String texto = new String(ayuda.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.US_ASCII);
            var motor = java.util.regex.Pattern.compile("--flevel=FLEVEL[^\\r\\n]*\\s+Default:\\s*([0-9]+)").matcher(texto);
            if (ayuda.waitFor() != 0 || !motor.find() || Integer.parseInt(cabecera.split(":")[4]) > Integer.parseInt(motor.group(1))) {
                throw new IOException("El motor de ClamAV no admite esta base bytecode.");
            }
            if (!origen.equals(guardada) && (!Files.isRegularFile(guardada) || Files.mismatch(candidato, guardada) != -1)) {
                if (Files.exists(guardada, LinkOption.NOFOLLOW_LINKS)) {
                    Files.setAttribute(guardada, "dos:readonly", false, LinkOption.NOFOLLOW_LINKS);
                }
                Files.move(candidato, guardada, ATOMIC_MOVE, REPLACE_EXISTING);
            }
        } finally {
            limpiar(temp);
        }
    }
    private static Path descargar(Path app, Path datos, Ejecutar ejecutar) throws Exception {
        // FreshClam conserva su identificación y los plazos de reintento del proveedor.
        Path descarga = Files.createDirectories(datos.resolve("freshclam"));
        // FreshClam lee las raíces instaladas. Schannel puede actualizar esa confianza
        // desde Windows Update la primera vez que Windows visita el proveedor.
        Path curl = Path.of(System.getenv("SystemRoot"), "System32", "curl.exe");
        try {
            ejecutar.run("conexion-firmas", descarga, 75, curl.toString(), "--head", "--silent",
                         "--show-error", "--max-time", "60", "--output", "NUL", "https://database.clamav.net/");
        } catch (IOException ignored) {
            // FreshClam también funciona con su propia configuración de red.
        }
        Path config = descarga.resolve("freshclam.conf");
        Path registro = datos.resolve("registros/freshclam.log");
        Files.writeString(config, "DatabaseMirror database.clamav.net\nDatabaseDirectory \"" + descarga
                          + "\"\nUpdateLogFile \"" + registro + "\"\nLogTime yes\nMaxAttempts 1\nConnectTimeout 15\nReceiveTimeout 60\n");
        ejecutar.run("descarga-firmas", descarga, 360, app.resolve("clamav/freshclam.exe").toString(),
                     "--stdout", "--config-file=" + config, "--update-db=bytecode");
        return descarga.resolve("bytecode.cvd");
    }
    static void limpiar(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try(var walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
