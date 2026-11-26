package ar.com.lamontana.instalador;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Provisión del CVD oficial del cliente dentro de su propia instalación. */
final class FirmasAntivirus {
    private static final String SHA = "antivirus.bytecode.sha256";

    static boolean externas(Properties manifiesto) {
        return "windows-x64".equals(manifiesto.getProperty("platform"))
                && "externo".equals(manifiesto.getProperty("antivirus.bytecode"));
    }

    static String via(Map<String, String> datos) {
        String via = datos.getOrDefault("viaFirmas", "internet");
        String archivo = datos.getOrDefault("archivoFirmas", "");
        if (!Set.of("archivo","internet").contains(via) || (!archivo.isEmpty() && !via.equals("archivo")))
            throw new IllegalArgumentException("Elegí descargar la base oficial o indicar un archivo local.");
        if (via.equals("archivo") && (archivo.isBlank() || !Path.of(archivo).isAbsolute() || archivo.startsWith("\\\\") || archivo.startsWith("//")))
            throw new IllegalArgumentException("Indicá la ruta absoluta de un archivo en el equipo o en un USB.");
        return via;
    }

    private static Path copia(Instalacion app) {
        return app.raiz.resolve("firmas").resolve("bytecode.cvd");
    }

    static boolean guardadas(Instalacion app) throws IOException {
        Path copia = copia(app);
        String esperada = app.estado.getProperty(SHA, "");
        return esperada.matches("[0-9a-f]{64}") && !Files.isSymbolicLink(copia.getParent())
                && Files.isRegularFile(copia, LinkOption.NOFOLLOW_LINKS) && esperada.equals(Util.sha256(copia));
    }

    static void preparar(Instalacion app, String via, String archivo) throws Exception {
        Path copia = copia(app);
        Path dir = copia.getParent();
        Files.createDirectories(dir);
        if (Carpetas.enlace(dir)) throw new IOException("La carpeta privada de firmas no puede ser un enlace.");
        Util.privado(dir, true);
        Path temporal = Files.createTempDirectory(dir, ".preparar-");
        String contenedor = app.proyecto() + "-firmas-" + UUID.randomUUID();
        Path candidato = temporal.resolve("candidato.cvd");
        Path validado = temporal.resolve("validado.cvd");
        try {
            Util.privado(temporal, true);
            Path origen = null;
            if (via.equals("archivo")) {
                origen = Path.of(archivo);
                if (!Files.isRegularFile(origen) || !app.equipo.discoLocal(origen))
                    throw new IOException("No se pudo leer la base oficial del disco o USB. Elegí el archivo otra vez.");
            } else if (guardadas(app)) origen = copia;
            if (origen != null) Files.copy(origen, candidato);
            // Se monta únicamente el intercambio privado; Docker nunca recibe la ruta del usuario.
            String montaje = app.equipo.rutaLinux(temporal) + ":/intercambio";
            var cmd = app.compose("run","--rm","--no-deps","-T","--pull","never","--name",contenedor,
                    "--volume",montaje,"antivirus","preparar-firmas",origen == null ? via : "archivo");
            var res = app.equipo.run(cmd, Duration.ofMinutes(10));
            if (!res.ok()) {
                if (res.codigo() == 127) app.equipo.run(app.equipo.docker("rm","--force",contenedor));
                if (res.codigo() == 20) throw new IOException("FreshClam no pudo descargar la base oficial. Revisá Internet o elegí una copia de disco o USB.");
                if (res.codigo() == 21) throw new IOException("Falta una base bytecode de ClamAV firmada y compatible. Elegí descargarla o seleccionar otra copia oficial.");
                throw new IOException("No se pudo preparar la base en el antivirus de la Demo. Verificá el espacio y el acceso y reintentá.");
            }
            if (!Files.isRegularFile(validado, LinkOption.NOFOLLOW_LINKS)) throw new IOException("El antivirus no devolvió la base verificada.");
            Util.privado(validado, false);
            String suma = Util.sha256(validado);
            if (origen != null && !suma.equals(Util.sha256(candidato))) throw new IOException("La copia cambió durante su verificación. Reintentá con el archivo oficial.");
            Files.move(validado, copia, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            app.estado.setProperty(SHA, suma);
            app.persistir();
        } finally {
            Files.deleteIfExists(validado);
            Files.deleteIfExists(candidato);
            Files.deleteIfExists(temporal);
        }
    }
}
