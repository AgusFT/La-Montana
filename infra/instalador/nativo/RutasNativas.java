import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * PostgreSQL para Windows usa la página ANSI en algunas rutas que también envía como SQL UTF-8
 * (initdb inserta la ruta absoluta de sql_features.txt). SUBST da a sus procesos una vista ASCII
 * de las mismas carpetas del usuario para PostgreSQL y los componentes de ClamAV. Windows conserva los datos y ACL en su ubicación original.
 * Solo se usan desde el panel que tiene el bloqueo exclusivo de esta demo.
 */
final class RutasNativas implements AutoCloseable {
    final Path programa;
    final Path datos;
    private final List<Unidad> propias;
    private record Unidad(Path raiz, Path destino, Path marca) {}

    private RutasNativas(Path programa, Path datos, List<Unidad> propias) {
        this.programa = programa;
        this.datos = datos;
        this.propias = propias;
    }

    static RutasNativas abrir(Path programa, Path datos) throws Exception {
        var propias = new ArrayList<Unidad>();
        try {
            Path pg = compatible(programa, datos.resolve("unidad-postgres-programa.txt"), propias);
            Path db = compatible(datos, datos.resolve("unidad-postgres-datos.txt"), propias);
            return new RutasNativas(pg, db, propias);
        } catch (Exception error) {
            for (Unidad unidad : propias) {
                try { quitar(unidad); } catch (Exception cierre) { error.addSuppressed(cierre); }
            }
            throw error;
        }
    }

    static boolean necesitaUnidad(Path carpeta) {
        return !StandardCharsets.US_ASCII.newEncoder().canEncode(carpeta.toAbsolutePath().toString());
    }

    static boolean tieneMarcas(Path datos) {
        return Files.exists(datos.resolve("unidad-postgres-programa.txt")) || Files.exists(datos.resolve("unidad-postgres-datos.txt"));
    }

    private static Path compatible(Path destino, Path marca, List<Unidad> propias) throws Exception {
        destino = destino.toAbsolutePath().normalize();
        if (!necesitaUnidad(destino)) return destino;
        if (Files.isRegularFile(marca)) {
            String anterior = Files.readString(marca, StandardCharsets.US_ASCII).strip();
            if (anterior.matches("[D-Z]:")) {
                var unidad = new Unidad(Path.of(anterior + "\\"), destino, marca);
                if (mismoDestino(unidad)) {
                    propias.add(unidad);
                    return unidad.raiz();
                }
            }
        }
        List<File> ocupadas = Arrays.asList(File.listRoots());
        for (char letra = 'Z'; letra >= 'D'; letra--) {
            String nombre = letra + ":";
            Path raiz = Path.of(nombre + "\\");
            if (ocupadas.contains(raiz.toFile())) continue;
            Path provisional = Files.createTempFile(marca.getParent(),"unidad-pg-",".tmp");
            try {
                Files.writeString(provisional, nombre, StandardCharsets.US_ASCII);
                Files.move(provisional, marca, ATOMIC_MOVE, REPLACE_EXISTING);
            } finally { Files.deleteIfExists(provisional); }
            if (subst(nombre, destino.toString()) != 0) continue;
            var unidad = new Unidad(raiz, destino, marca);
            if (!mismoDestino(unidad))
                throw new IOException("Windows no pudo comprobar la unidad temporal " + nombre + " de la demo.");
            propias.add(unidad);
            return raiz;
        }
        Files.deleteIfExists(marca);
        throw new IOException("La demo necesita una letra de unidad libre para usar esta carpeta. Liberá una letra y volvé a abrir la demo.");
    }

    private static boolean mismoDestino(Unidad unidad) throws IOException {
        return Files.isDirectory(unidad.raiz()) && Files.isDirectory(unidad.destino()) &&
                Files.isSameFile(unidad.raiz(), unidad.destino());
    }

    private static int subst(String unidad, String argumento) throws Exception {
        Path exe = Path.of(System.getenv("SystemRoot"), "System32", "subst.exe");
        Process p = new ProcessBuilder(exe.toString(), unidad, argumento)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!p.waitFor(8, TimeUnit.SECONDS))
                throw new IOException("Windows no respondió al preparar la unidad temporal para la demo.");
            return p.exitValue();
        } finally { if (p.isAlive()) p.destroyForcibly(); }
    }

    private static void quitar(Unidad unidad) throws Exception {
        // Una letra puede haber sido reasignada mientras el panel estaba abierto. No tocarla entonces.
        if (mismoDestino(unidad) && subst(unidad.raiz().toString().substring(0,2), "/D") != 0)
            throw new IOException("No se pudo retirar la unidad temporal " + unidad.raiz() + ". Volvé a abrir y cerrar la demo.");
        Files.deleteIfExists(unidad.marca());
    }

    @Override public void close() throws Exception {
        Exception fallo = null;
        for (int i = propias.size() - 1; i >= 0; i--) {
            try { quitar(propias.get(i)); }
            catch (Exception error) { if (fallo == null) fallo = error; else fallo.addSuppressed(error); }
        }
        propias.clear();
        if (fallo != null) throw fallo;
    }
}
