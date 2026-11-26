package ar.com.lamontana.instalador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/** El MSI ejecuta el retiro de todas las instalaciones de esta edición del usuario. */
final class RetiroWindowsTest {
    static void verificar(Path dir) throws Exception {
        Files.createDirectories(dir);
        try {
            Path registro = dir.resolve("perfil/asistente.properties");
            var motor = new Motor(new Paquete(dir));
            Instalacion.retirarRegistradas(motor, registro);
            assert motor.consultas == 0;

            var una = instalar(dir.resolve("imprenta uno"), motor);
            var dos = instalar(dir.resolve("imprenta dos"), motor);
            var tres = instalar(dir.resolve("imprenta anterior"), motor);
            una.registrarDestino(registro);
            dos.registrarDestino(registro);
            var ultima = new Properties();
            ultima.setProperty("directorio", dos.raiz.toString());
            Util.guardar(registro, ultima);
            var vigencia = Files.writeString(dir.resolve("vigencia"), "fecha original");

            // Un contenedor con otra identidad impide retirar un proyecto ajeno.
            motor.intruso = una;
            try {
                Instalacion.retirarRegistradas(motor, registro);
                throw new AssertionError("Debe rechazar la identidad incorrecta");
            } catch (IOException esperada) {
                assert esperada.getMessage().contains("confirmar el retiro");
                assert motor.retiros.isEmpty();
            }
            motor.intruso = null;
            Instalacion.retirarRegistradas(motor, registro);
            assert motor.retiros.size() == 3 && motor.cierres == 1;
            for (var app : List.of(una, dos, tres)) {
                assert motor.retiros.contains(app.proyecto());
                assert Util.properties(app.raiz.resolve("instalacion.properties")).getProperty("estado").equals("DESINSTALADA");
                assert Files.readString(app.raiz.resolve("trabajo.pdf")).equals("datos");
            }
            assert Files.readString(vigencia).equals("fecha original");
            int arranques = motor.arranques;
            Instalacion.retirarRegistradas(motor, registro);
            assert motor.arranques == arranques && motor.retiros.size() == 3;
        } finally {
            try (var archivos = Files.walk(dir)) {
                for (Path p : archivos.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
    }

    private static Instalacion instalar(Path dir, Motor motor) throws Exception {
        Files.createDirectories(dir);
        var p = new Properties();
        String id = UUID.randomUUID().toString();
        p.setProperty("id", id);
        p.setProperty("proyecto", "lamontana-demo-" + id.replace("-", "").substring(0, 12));
        p.setProperty("modo", "DEMO");
        p.setProperty("estado", "LISTA");
        p.setProperty("version", "0.1.0");
        Util.guardar(dir.resolve("instalacion.properties"), p);
        Files.createFile(dir.resolve(".env"));
        Files.createFile(dir.resolve("compose.yaml"));
        Files.writeString(dir.resolve("trabajo.pdf"), "datos");
        var app = new Instalacion(dir, motor);
        motor.instaladas.put(app.proyecto(), app);
        return app;
    }

    private static final class Motor extends Equipo {
        final Map<String, Instalacion> instaladas = new LinkedHashMap<>();
        final List<String> retiros = new ArrayList<>();
        Instalacion intruso;
        int consultas, arranques, cierres;
        Motor(Paquete paquete) { super(paquete); }
        @Override boolean distroPresente(boolean soloEnEjecucion) {
            consultas++;
            return !soloEnEjecucion || !instaladas.isEmpty();
        }
        @Override void prepararRetiro() { arranques++; }
        @Override void liberarEsperaSiCorresponde() { assert instaladas.isEmpty(); cierres++; }
        @Override Path rutaWindows(String ruta) { return Path.of(ruta); }
        @Override String rutaLinux(Path ruta) { return ruta.toString(); }
        @Override List<String> recursosDemo() {
            var filas = new ArrayList<String>();
            for (var app : instaladas.values()) {
                String id = app == intruso ? "identidad-distinta" : app.estado.getProperty("id");
                filas.add(id + "|" + app.proyecto() + "|" + app.raiz);
            }
            return filas;
        }
        @Override Util.Resultado run(List<String> cmd, Duration timeout) {
            if (cmd.contains("ps")) {
                String filtro = cmd.get(cmd.indexOf("--filter") + 1);
                var app = instaladas.get(filtro.substring("label=com.docker.compose.project=".length()));
                return new Util.Resultado(0, app == null ? "" : app.estado.getProperty("id") + "|" + app.raiz);
            }
            assert cmd.contains("compose") && cmd.contains("down") && !cmd.contains("-v") && !cmd.contains("--volumes");
            String proyecto = cmd.get(cmd.indexOf("--project-name") + 1);
            assert instaladas.remove(proyecto) != null;
            retiros.add(proyecto);
            return new Util.Resultado(0, "");
        }
    }
}
