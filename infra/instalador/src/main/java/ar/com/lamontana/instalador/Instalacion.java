package ar.com.lamontana.instalador;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Instalación local de la imprenta de muestra y sus recursos de Docker. */
final class Instalacion {
    static final List<String> SERVICIOS = List.of("postgres", "backend", "frontend", "antivirus", "mailpit");
    final Path raiz;
    final Equipo equipo;
    final Paquete paquete;
    Properties estado;

    Instalacion(Path raiz, Equipo equipo) throws IOException {
        this.raiz = raiz.toAbsolutePath().normalize();
        this.equipo = equipo;
        this.paquete = equipo.paquete;
        leerEstado();
    }

    private void leerEstado() throws IOException {
        Path archivo = raiz.resolve("instalacion.properties");
        estado = Files.exists(archivo, LinkOption.NOFOLLOW_LINKS) ? Carpetas.leerFicha(archivo) : new Properties();
        if (Files.exists(archivo, LinkOption.NOFOLLOW_LINKS) && !identidadValida(estado))
            throw new IllegalArgumentException("Esta carpeta no contiene una instalación identificada de La Montaña Demo.");
    }

    static boolean identidadValida(Properties p) {
        String id = p.getProperty("id", "");
        return id.matches("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")
                && p.getProperty("proyecto", "").equals("lamontana-demo-" + id.replace("-", "").substring(0,12))
                && "DEMO".equals(p.getProperty("modo")) && p.getProperty("version", "").matches("[0-9]+\\.[0-9]+\\.[0-9]+");
    }

    boolean existe() {return estado.containsKey("id");}
    String proyecto() {
        if (!identidadValida(estado)) throw new IllegalArgumentException("La carpeta no corresponde a una Demo identificada.");
        return estado.getProperty("proyecto");
    }

    synchronized void modificar(Asistente.Tarea tarea) throws Exception {
        try (var canal = FileChannel.open(raiz.resolve(".instalador.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var bloqueo = canal.tryLock()) {
            if (bloqueo == null) throw new IOException("Otra ejecución está modificando esta instalación.");
            leerEstado();
            tarea.ejecutar();
        } catch (OverlappingFileLockException e) {throw new IOException("Otra ejecución está modificando esta instalación.");}
    }

    void persistir() throws IOException {Util.guardar(raiz.resolve("instalacion.properties"), estado);}
    String url() {return "http://127.0.0.1:" + puerto(estado.getProperty("puerto")) + "/acceso";}

    List<String> compose(String... args) throws Exception {
        var cmd = new ArrayList<>(equipo.docker("compose", "--project-name", proyecto(),
                "--project-directory", equipo.rutaLinux(raiz), "--env-file", equipo.rutaLinux(raiz.resolve(".env")),
                "-f", equipo.rutaLinux(raiz.resolve("compose.yaml"))));
        cmd.addAll(List.of(args));
        return cmd;
    }
    void ejecutar(String... args) throws Exception {
        var r = Util.ejecutar(compose(args), null, Duration.ofMinutes(20), null);
        if (!r.ok()) throw new IOException("Compose no completó " + args[0] + " (código " + r.codigo() + "). Revisá Docker, los puertos y el espacio disponible.");
    }

    Map<String,Object> resumen() throws Exception {
        var m = new LinkedHashMap<String,Object>();
        m.put("existe", existe());
        m.put("directorio", raiz.toString());
        if (existe()) {
            m.put("version", estado.getProperty("version"));
            m.put("modo", "DEMO");
            m.put("puerto", estado.getProperty("puerto"));
            m.put("estado", estado.getProperty("estado"));
            m.put("url", url());
            boolean saludable = false;
            if (Files.isRegularFile(raiz.resolve("compose.yaml")) && Files.isRegularFile(raiz.resolve(".env"))) {
                var r = equipo.run(compose("ps", "--all", "--format", "table"));
                m.put("servicios", r.salida());
                var services = equipo.run(compose("ps", "--status", "running", "--format", "{{.Service}}|{{.Health}}"));
                saludable = services.ok() && services.salida().lines().collect(java.util.stream.Collectors.toSet())
                        .containsAll(SERVICIOS.stream().map(s->s+"|healthy").toList());
            }
            m.put("disponible", saludable);
        }
        return m;
    }

    private void verificarCarpetaNueva() throws IOException {
        try (var archivos = Files.list(raiz)) {
            if (archivos.anyMatch(p -> !p.getFileName().toString().equals(".instalador.lock")))
                throw new IOException("Elegí una carpeta vacía para instalar la Demo.");
        }
    }

    void instalar(Map<String,String> datos) throws Exception {
        if (!"DEMO".equals(datos.getOrDefault("modo","DEMO")) || !Set.of("modo","puerto").containsAll(datos.keySet()))
            throw new IllegalArgumentException("La Demo se instala en modo local con las cuentas de muestra.");
        int port = puerto(datos.getOrDefault("puerto","3200"));
        leerEstado();
        if (!existe() && Files.exists(raiz)) verificarCarpetaNueva();
        equipo.validarMotor();
        paquete.verificar();
        Files.createDirectories(raiz);
        if (Files.getFileStore(raiz).getUsableSpace() < Files.size(paquete.archivo("images.tar.gz")) * 4 + 512L*1024*1024)
            throw new IOException("Falta espacio en la carpeta de instalación para preparar la Demo.");
        modificar(() -> {
            if (existe()) {
                if (!paquete.version().equals(estado.getProperty("version")))
                    throw new IOException("Para cambiar la versión de esta instalación elegí Actualizar.");
                if (!Set.of("PREPARADA","DESINSTALADA").contains(estado.getProperty("estado","")))
                    throw new IOException("La Demo ya está instalada. Elegí Abrir, Reparar o Actualizar.");
            } else {
                verificarCarpetaNueva();
                libre(port);
                Util.privado(raiz,true);
                String id = UUID.randomUUID().toString();
                estado.setProperty("id", id);
                estado.setProperty("proyecto","lamontana-demo-"+id.replace("-","").substring(0,12));
                estado.setProperty("modo","DEMO");
                estado.setProperty("version",paquete.version());
                estado.setProperty("puerto",Integer.toString(port));
                int control = disponible();
                while (control == port) control = disponible();
                int mail = disponible();
                while (mail == port || mail == control) mail = disponible();
                estado.setProperty("control",Integer.toString(control));
                estado.setProperty("mailpit",Integer.toString(mail));
                estado.setProperty("estado","PREPARADA");
                persistir();
            }
            verificarUbicacion();
            var secretos = Util.properties(raiz.resolve("secretos.properties"));
            if (secretos.getProperty("DB_PASSWORD","").isBlank()) {
                if (Files.exists(raiz.resolve(".env"))) throw new IOException("Falta la credencial de la instalación.");
                secretos.setProperty("DB_PASSWORD",Util.secreto());
                Util.guardar(raiz.resolve("secretos.properties"),secretos);
            }
            Files.createDirectories(raiz.resolve("entrada"));
            desplegar();
        });
    }

    private void verificarUbicacion() throws Exception {
        var r = equipo.run(equipo.docker("ps", "-a", "--filter", "label=com.docker.compose.project="+proyecto(),
                "--format","{{.Label `com.docker.compose.project.working_dir`}}"));
        if (!r.ok()) throw new IOException("No se pudo comprobar la identidad del proyecto Docker.");
        String esperada = equipo.rutaLinux(raiz.toRealPath());
        if (r.salida().lines().anyMatch(ruta -> !(equipo.windows ? ruta.equalsIgnoreCase(esperada) : ruta.equals(esperada))))
            throw new IOException("El proyecto de esta Demo ya pertenece a otra carpeta. Elegí su instalación original.");
    }

    void mantener(String operacion, Path destino) throws Exception {
        if (!Set.of("actualizar","reparar").contains(operacion) || !existe()) throw new IllegalArgumentException("Operación de mantenimiento inválida.");
        equipo.validarMotor();
        paquete.verificar();
        modificar(() -> {
            if (Set.of("PREPARADA","DESINSTALADA").contains(estado.getProperty("estado","")))
                throw new IOException("Primero completá la instalación de la Demo.");
            Paquete.validarOperacion(operacion,estado.getProperty("version"),paquete.version());
            verificarUbicacion();
            Path copia;
            try {copia = Respaldo.crear(this,destino);} catch (Exception e) {
                estado.setProperty("estado","RESPALDO_FALLIDO");persistir();throw e;
            }
            estado.setProperty("ultimo.respaldo",copia.toString());
            estado.setProperty("estado","MANTENIMIENTO_EN_CURSO");persistir();
            try {desplegar();} catch (Exception e) {
                estado.setProperty("estado","MANTENIMIENTO_FALLIDO");persistir();throw e;
            }
        });
    }

    void desinstalar() throws Exception {
        modificar(() -> {
            if (!existe()) throw new IllegalArgumentException("Seleccioná la carpeta de una Demo instalada.");
            verificarUbicacion();
            if (Files.isRegularFile(raiz.resolve("compose.yaml"))) ejecutar("down","--timeout","60");
            estado.setProperty("estado","DESINSTALADA");persistir();
            recordar();
        });
    }

    void abrir() throws Exception {
        modificar(() -> {
            if (!"LISTA".equals(estado.getProperty("estado"))) throw new IOException("La instalación necesita atención. Abrí el asistente de mantenimiento de la Demo.");
            equipo.validarMotor();
            verificarUbicacion();
            iniciarServicios();
        });
    }

    private void iniciarServicios() throws Exception {ejecutar("up","-d","--wait","--wait-timeout","600","--pull","never");}

    private void desplegar() throws Exception {
        var carga=Util.ejecutar(equipo.docker("load","--input",equipo.rutaLinux(paquete.archivo("images.tar.gz"))),null,Duration.ofMinutes(20),null);
        if (!carga.ok()) throw new IOException("No se pudieron cargar las imágenes incluidas.");
        for (String s: SERVICIOS) {
            var r=equipo.run(equipo.docker("image","inspect",paquete.manifiesto.getProperty("image."+s),"--format","{{.Id}}"));
            if (!r.ok() || !r.salida().strip().equals(paquete.manifiesto.getProperty("image."+s+".id"))) throw new IOException("La imagen de "+s+" no corresponde al paquete.");
        }
        Util.copiarManifiesto(paquete.archivo("release.properties"),raiz.resolve("version.properties"));
        try (var in=getClass().getResourceAsStream("/despliegue/compose.yaml")) {
            Files.copy(Objects.requireNonNull(in),raiz.resolve("compose.yaml"),StandardCopyOption.REPLACE_EXISTING);
        }
        var vars=Util.properties(raiz.resolve("secretos.properties"));
        vars.setProperty("INSTALLATION_ID",estado.getProperty("id"));
        vars.setProperty("APP_PORT",estado.getProperty("puerto"));
        vars.setProperty("CONTROL_PORT",estado.getProperty("control"));
        vars.setProperty("MAILPIT_PORT",estado.getProperty("mailpit"));
        vars.setProperty("WEB_SOURCE_DIR",equipo.rutaLinux(raiz.resolve("entrada")));
        Path vigencia = equipo.directorioVigencia();
        Files.createDirectories(vigencia);
        String rutaVigencia = equipo.rutaLinux(vigencia);
        if (!equipo.windows) {
            var permisos = equipo.run(equipo.docker("run","--rm","--pull","never","--network","none","--read-only",
                    "--user","0:0","--mount","type=bind,source="+rutaVigencia+",target=/vigencia",
                    "--entrypoint","chown",paquete.manifiesto.getProperty("image.backend.id"),"10001:10001","/vigencia"));
            if (!permisos.ok()) throw new IOException("El backend no puede guardar la vigencia en este equipo.");
        }
        vars.setProperty("DEMO_VIGENCIA_HOST",rutaVigencia);
        for(String service:SERVICIOS) vars.setProperty("IMAGE_"+service.toUpperCase(Locale.ROOT),paquete.manifiesto.getProperty("image."+service+".id"));
        var env=new StringBuilder();
        for(String key:new TreeSet<>(vars.stringPropertyNames())) env.append(key).append('=').append(dotenv(vars.getProperty(key))).append('\n');
        Path p=raiz.resolve(".env");Files.writeString(p,env);Util.privado(p,false);
        iniciarServicios();
        estado.setProperty("version",paquete.version());estado.setProperty("estado","LISTA");persistir();
        recordar();
    }

    static Path registro() {
        String local=System.getenv("LOCALAPPDATA");
        Path dir=System.getProperty("os.name").startsWith("Windows")&&local!=null ? Path.of(local) : Path.of(System.getProperty("user.home"),".local","state");
        return dir.resolve("YG-Soluciones/LaMontanaDemo/asistente.properties");
    }
    void recordar() throws IOException {
        Path p=registro();Files.createDirectories(p.getParent());
        var valor=new Properties();valor.setProperty("directorio",raiz.toString());Util.guardar(p,valor);
    }
    static Path ultima() throws IOException {
        String dir=Util.properties(registro()).getProperty("directorio","");
        if (dir.isEmpty()) return null;
        Path p=Carpetas.ruta(dir);
        return Files.isRegularFile(p.resolve("instalacion.properties"), LinkOption.NOFOLLOW_LINKS) ? p : null;
    }
    static String dotenv(String value){if(value.indexOf('\n')>=0||value.indexOf('\r')>=0||value.indexOf('\0')>=0)throw new IllegalArgumentException("La configuración no admite saltos de línea.");return Util.json(value).replace("$","$$");}
    static int puerto(String text) {
        try {int p=Integer.parseInt(text);if(p<1024||p>65535)throw new NumberFormatException();return p;}
        catch(NumberFormatException e){throw new IllegalArgumentException("Elegí un puerto local entre 1024 y 65535.");}
    }
    static int disponible() throws IOException {try(var socket=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){return socket.getLocalPort();}}
    static void libre(int puerto) throws IOException {try(var s=new ServerSocket()){s.bind(new InetSocketAddress("127.0.0.1",puerto));}catch(IOException e){throw new IOException("El puerto "+puerto+" está ocupado; elegí otro.");}}
}
