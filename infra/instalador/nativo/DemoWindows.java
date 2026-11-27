import java.awt.Desktop;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;

/** Panel de la edición nativa. Sólo administra procesos y archivos de su propia instalación. */
public final class DemoWindows {
    final Path app, data, log, cluster, lockPath;
    final List<Process> children = new ArrayList<>();
    // El servidor HTTP de Next.js atiende HTTP/1.1. Un intento de upgrade h2c cierra esa conexión.
    final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                            .connectTimeout(Duration.ofSeconds(2)).build();
    final JFrame panel = new JFrame("La Montaña Demo · YG Soluciones");
    final JLabel message = new JLabel("Preparando la demo…");
    final JButton open = new JButton("Abrir la demo");
    final boolean browser;
    final Path source;
    Thread actualizacion;
    boolean postgres;
    volatile boolean closing;
    URI address;

    DemoWindows(Path app, Path data, List<String> args) {
        this.app = app;
        this.data = data;
        this.log = data.resolve("registros");
        cluster = data.resolve("postgres");
        lockPath = data.resolve("sesion.lock");
        browser = !args.contains("--sin-navegador");
        int index = args.indexOf("--firmas");
        source = index < 0 ? null : Path.of(args.get(index + 1));
    }
    int detener() throws Exception {
        try (var channel = FileChannel.open(lockPath, CREATE, WRITE)) {
            for (int i = 0; i < 120; i++) {
                try (var lock = channel.tryLock()) {
                    if (lock != null) {
                        recuperarServicios();
                        return 0;
                    }
                }
                Files.writeString(data.resolve("cerrar"), "");
                Thread.sleep(500);
            }
        }
        return 1;
    }
    public static void main(String[] args) throws Exception {
        var options = List.of(args);
        Path app = Path.of(DemoWindows.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        Path data = Path.of(System.getenv("LOCALAPPDATA"), "YG-Soluciones", "LaMontanaDemo", "nativa");
        Files.createDirectories(data);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        var demo = new DemoWindows(app, data, options);
        if (options.contains("--detener")) {
            System.exit(demo.detener());
        }
        try (var file = FileChannel.open(demo.lockPath, CREATE, WRITE); var lock = file.tryLock()) {
            if (lock == null) {
                Files.writeString(data.resolve("mostrar"), "");
                return;
            }
            Files.deleteIfExists(data.resolve("cerrar"));
            Files.deleteIfExists(data.resolve("mostrar"));
            Files.createDirectories(demo.log);
            try (var logs = Files.newDirectoryStream(demo.log, "*.log")) {
                for (Path path : logs) {
                    if (Files.size(path) > 4 * 1024 * 1024) {
                        Files.move(path, path.resolveSibling(path.getFileName() + ".anterior"), REPLACE_EXISTING);
                    }
                }
            }
            SwingUtilities.invokeAndWait(demo::display);
            Exception fallo = null;
            try {
                demo.run();
            } catch (Exception error) {
                fallo = error;
            } finally {
                try {
                    demo.stop();
                } catch (Exception cierre) {
                    if (fallo == null) {
                        fallo = cierre;
                    } else {
                        fallo.addSuppressed(cierre);
                    }
                }
            }
            if (fallo != null) {
                Exception error = fallo;
                try (var out = new PrintWriter(Files.newBufferedWriter(demo.log.resolve("inicio.log"), CREATE, APPEND))) {
                    error.printStackTrace(out);
                }
                if (!demo.closing && !Files.exists(data.resolve("cerrar"))) JOptionPane.showMessageDialog(demo.panel,
                            error.getMessage() + "\nRegistros: " + demo.log, "La Montaña Demo", JOptionPane.ERROR_MESSAGE);
            }
            SwingUtilities.invokeAndWait(demo.panel::dispose);
        }
    }
    void display() {
        panel.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        panel.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closing = true;
            }
        });
        JPanel body = new JPanel(new java.awt.BorderLayout(14, 14));
        body.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        var buttons = new JPanel();
        open.setEnabled(false);
        open.addActionListener(e->browse());
        buttons.add(open);
        JButton close = new JButton("Cerrar la demo");
        close.addActionListener(e->closing = true);
        buttons.add(close);
        body.add(message, java.awt.BorderLayout.CENTER);
        body.add(buttons, java.awt.BorderLayout.SOUTH);
        panel.setContentPane(body);
        panel.setSize(500, 145);
        panel.setLocationRelativeTo(null);
        panel.setVisible(true);
    }
    void status(String text) {
        SwingUtilities.invokeLater(()->message.setText(text));
    }
    void browse() {
        try {
            Desktop.getDesktop().browse(address);
        } catch (Exception e) {
            status("Abrí " + address + " en tu navegador.");
        }
    }
    void check() throws IOException {
        if (closing || Files.exists(data.resolve("cerrar"))) {
            throw new IOException("Se solicitó cerrar la demo.");
        }
        for (Process process : children) {
            if (!process.isAlive()) {
                throw new IOException("Se detuvo " + Path.of(process.info().command().orElse("un servicio")).getFileName() + ". Revisá los registros y volvé a abrir la demo.");
            }
        }
    }
    Process start(String name, List<String> command, Map<String, String> env, Path dir) throws IOException {
        var builder = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(log.resolve(name + ".log").toFile()));
        builder.environment().putAll(env);
        var process = builder.start();
        process.getOutputStream().close();
        return process;
    }
    void command(String name, Path dir, int seconds, String... args) throws Exception {
        command(false, name, dir, seconds, args);
    }
    void command(boolean cancelable, String name, Path dir, int seconds, String... args) throws Exception {
        var p = start(name, List.of(args), Map.of(), dir);
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
            while (!p.waitFor(500, TimeUnit.MILLISECONDS)) {
                if (cancelable) {
                    check();
                }
                if (System.nanoTime() >= deadline) {
                    throw new IOException("Se agotó el tiempo de espera de " + name + ". Consultá " + log.resolve(name + ".log"));
                }
            }
            if (p.exitValue() != 0) {
                throw new IOException("Falló " + name + ". Consultá " + log.resolve(name + ".log"));
            }
        } finally {
            if (p.isAlive()) {
                p.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            }
        }
    }
    void service(String name, Map<String, String> env, Path dir, String... args) throws IOException {
        Process p = start(name, List.of(args), env, dir);
        children.add(p);
        Path pid = data.resolve(name + ".pid");
        Path provisional = Files.createTempFile(data, name + "-", ".tmp");
        try {
            Files.writeString(provisional, p.pid() + "\n" + p.info().startInstant().orElseThrow());
            Files.move(provisional, pid, ATOMIC_MOVE, REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(provisional);
        }
    }
    // Si Windows interrumpió el panel, cotejar PID, fecha y ejecutable antes de recuperar un proceso propio.
    void recover(String name, Path exe) throws Exception {
        Path path = data.resolve(name + ".pid");
        if (!Files.exists(path)) {
            return;
        }
        var lines = Files.readAllLines(path);
        var p = ProcessHandle.of(Long.parseLong(lines.get(0)));
        if (p.isPresent() && p.get().info().startInstant().map(Instant::toString).orElse("").equals(lines.get(1)) &&
                p.get().info().command().map(Path::of).filter(exe::equals).isPresent()) {
            p.get().destroyForcibly();
            p.get().onExit().get(10, TimeUnit.SECONDS);
        }
        Files.delete(path);
    }
    int port(Set<Integer> used, int preferred) throws IOException {
        for (int i = 0; i < 64; i++) {
            try (ServerSocket s = new ServerSocket(i == 0 ? preferred : 0, 1, InetAddress.getByName("127.0.0.1"))) {
                if (used.add(s.getLocalPort())) {
                    return s.getLocalPort();
                }
            } catch (BindException busy) { /* Otra aplicación usa el puerto preferido. */ }
        }
        throw new IOException("No hay un puerto local disponible.");
    }
    void waitReady(int port, String path, int seconds) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < end) {
            check();
            try {
                if (path == null || path.equals("clamd")) {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress("127.0.0.1", port), 500);
                        if (path == null) {
                            return;
                        }
                        s.setSoTimeout(1200);
                        s.getOutputStream().write(new byte[] {'z', 'P', 'I', 'N', 'G', 0});
                        if (Arrays.equals(s.getInputStream().readNBytes(5), new byte[] {'P', 'O', 'N', 'G', 0})) {
                            return;
                        }
                    }
                } else {
                    var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(3)).build();
                    if (http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                        return;
                    }
                }
            } catch (IOException waiting) { /* El proceso sigue iniciándose. */ }
            Thread.sleep(500);
        }
        throw new IOException("Se agotó la espera de inicio del servicio local en el puerto " + port + ". Revisá los registros.");
    }
    void recuperarServicios() throws Exception {
        recover("frontend", app.resolve("node/node.exe"));
        recover("backend", app.resolve("java/bin/java.exe"));
        recover("antivirus", app.resolve("clamav/clamd.exe"));
        recover("correo", app.resolve("correo/mailpit.exe"));
        if (Files.exists(cluster.resolve("postmaster.pid"))) {
            Process probe = start("postgres", List.of(pgCtl(), "status", "-D", cluster.toString()), Map.of(), data);
            if (!probe.waitFor(10, TimeUnit.SECONDS)) {
                probe.destroyForcibly();
                throw new IOException("PostgreSQL no respondió.");
            }
            if (probe.exitValue() == 0) {
                stopPg();
            }
        }
    }
    void run() throws Exception {
        recuperarServicios();
        Path java = app.resolve("java/bin/java.exe"), node = app.resolve("node/node.exe"), clam = app.resolve("clamav/clamd.exe"), mail = app.resolve("correo/mailpit.exe");
        Set<Integer> used = new HashSet<>();
        int web = port(used, 3200), api = port(used, 8280), pg = port(used, 55433), av = port(used, 13310), smtp = port(used, 11025), inbox = port(used, 8225);
        address = URI.create("http://127.0.0.1:" + web + "/acceso");
        status("Preparando La Montaña…");
        Path db = FirmasWindows.preparar(app, data, source,
                                         (name, dir, seconds, args) -> command(true, name, dir, seconds, args));
        check();
        Path config = data.resolve("clamd.conf");
        Files.writeString(config, "DatabaseDirectory \"" + db + "\"\nTCPAddr 127.0.0.1\nTCPSocket " + av + "\nForeground yes\nStreamMaxLength 10M\nMaxFileSize 10M\nMaxScanSize 128M\nMaxScanTime 15000\nMaxThreads 2\nAlertExceedsMax yes\nAlertEncrypted yes\nConcurrentDatabaseReload no\nSelfCheck 60\n");
        service("antivirus", Map.of(), data, clam.toString(), "--config-file=" + config);
        actualizacion = new Thread(() -> {
            try {
                FirmasWindows.actualizar(app, data, this::command);
            } catch (InterruptedException cierre) {
                Thread.currentThread().interrupt();
            } catch (Exception error) {
                FirmasWindows.guardarAviso(data, error);
            }
        }, "actualizar-firmas");
        actualizacion.start();
        service("correo", Map.of("MP_SMTP_BIND_ADDR", "127.0.0.1:" + smtp, "MP_UI_BIND_ADDR", "127.0.0.1:" + inbox, "MP_DATABASE", data.resolve("correo.db").toString()), data, mail.toString());
        status("Preparando la base de datos…");
        Path secret = data.resolve("postgres.key");
        if (!Files.exists(secret) && !Files.exists(cluster)) {
            Files.writeString(secret, UUID.randomUUID().toString() + UUID.randomUUID(), CREATE_NEW);
        }
        String password = Files.readString(secret).strip();
        if (!Files.isDirectory(cluster)) {
            Path temp = Files.createTempDirectory(data, "pg-nuevo-");
            try {
                command("initdb", data, 120, app.resolve("pg/bin/initdb.exe").toString(), "-D", temp.toString(), "-L", app.resolve("pg/share").toString(), "-U", "lamontana_demo", "--pwfile=" + secret, "-A", "scram-sha-256", "--encoding=UTF8", "--locale=C");
                Files.move(temp, cluster, ATOMIC_MOVE);
            } finally {
                FirmasWindows.limpiar(temp);
            }
        }
        command("postgres", data, 90, pgCtl(), "start", "-w", "-t", "75", "-D", cluster.toString(), "-l", log.resolve("servidor-pg.log").toString(), "-o", "-h 127.0.0.1 -p " + pg);
        postgres = true;
        status("Iniciando La Montaña…");
        Path entrada = Files.createDirectories(data.resolve("entrada"));
        var env = new HashMap<String, String>();
        env.put("DB_URL", "jdbc:postgresql://127.0.0.1:" + pg + "/postgres");
        env.put("DB_USER", "lamontana_demo");
        env.put("DB_PASSWORD", password);
        env.put("SERVER_ADDRESS", "127.0.0.1");
        env.put("SERVER_PORT", "" + api);
        env.put("LAMONTANA_MODO", "DEMO");
        env.put("CLAMAV_HOST", "127.0.0.1");
        env.put("CLAMAV_PORT", "" + av);
        env.put("SMTP_HOST", "127.0.0.1");
        env.put("SMTP_PORT", "" + smtp);
        env.put("SMTP_FROM", "noreply@demo.lamontana.local");
        env.put("FILES_DIR", data.resolve("archivos").toString());
        env.put("WEB_SOURCE_DIR", entrada.toString());
        env.put("WEB_SOURCE_HOST", entrada.toString());
        env.put("WEB_LIBRARY_DIR", data.resolve("imagenes-web").toString());
        env.put("PDF_INSPECTOR_JAR", app.resolve("backend.jar").toString());
        env.put("DEMO_VIGENCIA_DIR", Path.of(System.getenv("ProgramData"), "YG-Soluciones", "LaMontanaDemo", "vigencia").toString());
        service("backend", env, data, java.toString(), "-Xmx512m", "-Dfile.encoding=UTF-8", "-jar", app.resolve("backend.jar").toString());
        waitReady(api, "/actuator/health/readiness", 240);
        waitReady(av, "clamd", 240);
        waitReady(smtp, null, 15);
        service("frontend", Map.of("PORT", "" + web, "HOSTNAME", "127.0.0.1", "BACKEND_INTERNAL_URL", "http://127.0.0.1:" + api, "NEXT_TELEMETRY_DISABLED", "1"), app.resolve("web"), node.toString(), "server.js");
        waitReady(web, "/health", 90);
        status("Demo lista en " + address);
        SwingUtilities.invokeLater(()->open.setEnabled(true));
        if (browser) {
            browse();
        }
        while (!closing && !Files.exists(data.resolve("cerrar"))) {
            check();
            if (Files.deleteIfExists(data.resolve("mostrar"))) {
                panel.toFront();
                browse();
            }
            Thread.sleep(500);
        }
    }
    String pgCtl() {
        return app.resolve("pg/bin/pg_ctl.exe").toString();
    }
    void stopPg() throws Exception {
        command("postgres", data, 40, pgCtl(), "stop", "-w", "-t", "30", "-m", "fast", "-D", cluster.toString());
    }
    void stop() throws Exception {
        status("Cerrando la demo…");
        if (actualizacion != null) {
            actualizacion.interrupt();
            actualizacion.join();
        }
        Collections.reverse(children);
        try {
            for (var p : children) {
                if (p.isAlive()) {
                    p.destroy();
                    if (!p.waitFor(5, TimeUnit.SECONDS)) {
                        p.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
                    }
                }
            }
        } finally {
            if (postgres) {
                stopPg();
            }
        }
    }
}
