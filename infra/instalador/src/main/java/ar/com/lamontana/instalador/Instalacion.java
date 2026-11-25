package ar.com.lamontana.instalador;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Una instalación es un directorio propio y un proyecto Compose identificado por UUID. */
final class Instalacion {
    final Path raiz;final Equipo equipo;final Paquete paquete;Properties estado;
    private String codigoPublico,codigoCorreo,rutaPrueba;private Instant vencePublico,venceCorreo;
    private final java.util.concurrent.ScheduledExecutorService expiraciones=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"caducidad-publicacion");t.setDaemon(true);return t;});
    Instalacion(Path raiz,Equipo equipo) throws IOException {this.raiz=raiz.toAbsolutePath().normalize();this.equipo=equipo;this.paquete=equipo.paquete;this.estado=Util.properties(this.raiz.resolve("instalacion.properties"));if(estado.containsKey("publico.prueba")){try{modificar(this::cerrarPruebaPublica);}catch(Exception e){throw new IOException("La prueba pública anterior quedó interrumpida. Revisá Docker para poder retirar su dirección temporal.");}}}
    synchronized void modificar(Asistente.Tarea accion) throws Exception {
        if(!Files.isDirectory(raiz))throw new IOException("Primero seleccioná una instalación existente.");
        try(var canal=FileChannel.open(raiz.resolve(".instalador.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var bloqueo=canal.tryLock()){
            if(bloqueo==null)throw new IOException("Otra ejecución está modificando esta instalación.");
            estado=Util.properties(raiz.resolve("instalacion.properties"));accion.ejecutar();
        }catch(OverlappingFileLockException e){throw new IOException("Otra ejecución está modificando esta instalación.");}
    }
    void persistir() throws IOException{Util.guardar(raiz.resolve("instalacion.properties"),estado);}
    boolean existe(){return estado.containsKey("id");}
    String modo(){return estado.getProperty("modo","");}
    String proyecto(){String p=estado.getProperty("proyecto","");if(!p.matches("lamontana-[a-f0-9]{12}"))throw new IllegalArgumentException("Identidad de instalación no válida.");return p;}
    List<String> compose(String... args) throws Exception {var cmd=new ArrayList<>(equipo.docker("compose","--project-name",proyecto(),"--project-directory",equipo.rutaLinux(raiz),"--env-file",equipo.rutaLinux(raiz.resolve(".env")),"-f",equipo.rutaLinux(raiz.resolve("compose.yaml"))));if(modo().equals("DESARROLLO"))cmd.addAll(List.of("--profile","desarrollo"));cmd.addAll(List.of(args));return cmd;}
    void ejecutar(String...args) throws Exception {var r=Util.ejecutar(compose(args),null,Duration.ofMinutes(15),null);if(!r.ok())throw new IOException("Compose no completó "+args[0]+" (código "+r.codigo()+"). Revisá dependencias, puertos, espacio y salud de los servicios.");}
    Map<String,Object> resumen() throws Exception {
        var m=new LinkedHashMap<String,Object>();m.put("existe",existe());m.put("directorio",raiz.toString());
        if(existe()){m.put("version",estado.getProperty("version"));m.put("modo",modo());m.put("estado",estado.getProperty("estado","PENDIENTE"));m.put("url",modo().equals("STANDARD")?(estado.getProperty("dominio","").isEmpty()?"":"https://"+estado.getProperty("dominio")):"http://"+estado.getProperty("bind","127.0.0.1")+":"+estado.getProperty("puerto"));m.put("servicios",equipo.run(compose("ps","--all","--format","table")).salida());m.put("disponible",saludable(modo().equals("STANDARD")));m.put("publicoConfirmadoEn",estado.getProperty("publico.confirmado",""));m.put("smtpConfirmadoEn",estado.getProperty("smtp.confirmado",""));}
        if(existe()){
            m.put("puerto",estado.getProperty("puerto",""));m.put("bind",estado.getProperty("bind",""));
            var correo=Util.properties(raiz.resolve("secretos.properties"));
            m.put("publicacionConfigurada",modo().equals("STANDARD")&&!estado.getProperty("dominio","").isBlank()&&!correo.getProperty("SMTP_HOST","").isBlank()&&!correo.getProperty("SMTP_PASSWORD","").isBlank());
        }
        if(modo().equals("DESARROLLO"))m.put("urlMailpit","http://127.0.0.1:"+estado.getProperty("mailpit"));
        m.put("pruebaPublica",rutaPrueba==null?"":rutaPrueba);m.put("arranqueConfigurado",estado.getProperty("arranque.configurado","false"));m.put("arranqueVerificado",estado.getProperty("arranque.verificado","false"));return m;
    }
    void instalar(Map<String,String> datos) throws Exception {
        equipo.validarMotor();paquete.verificar();if(!Set.of("DESARROLLO","STANDARD").contains(Util.requerido(datos,"modo")))throw new IllegalArgumentException("Modo inválido.");
        Files.createDirectories(raiz);Util.privado(raiz,true);
        if(Files.getFileStore(raiz).getUsableSpace()<Files.size(paquete.archivo("images.tar.gz"))*4+512L*1024*1024)throw new IOException("Espacio insuficiente en el destino para preparar la app. Liberá espacio y verificá el almacenamiento de Docker.");
        try(var channel=FileChannel.open(raiz.resolve(".instalador.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.tryLock()){
            if(lock==null)throw new IOException("Otra ejecución está modificando esta instalación.");estado=Util.properties(raiz.resolve("instalacion.properties"));
            if(existe()){
                if(!modo().equals(datos.get("modo"))||!estado.getProperty("version").equals(paquete.version()))throw new IOException("El destino pertenece a otra versión o modo. Usá mantenimiento o una carpeta nueva para Standard.");
                if(!Set.of("PREPARADA","CUENTAS_PENDIENTES").contains(estado.getProperty("estado","")))throw new IOException("La app ya está instalada. Elegí Reparar o Actualizar.");
            }else{
                try(var files=Files.list(raiz)){if(files.anyMatch(p->!p.getFileName().toString().equals(".instalador.lock")))throw new IOException("La carpeta contiene archivos previos. Seleccioná una carpeta nueva; no se sobrescribe su contenido.");}
                String bind=datos.getOrDefault("lan","").equals("si")?validarIPLocal(Util.requerido(datos,"ipLan")):"127.0.0.1";
                if(equipo.windows&&!bind.equals("127.0.0.1")){var red=equipo.run(List.of("wsl.exe","-d",Equipo.DISTRO,"-u","root","--exec","wslinfo","--networking-mode"));if(!red.ok()||!Set.of("nat","virtioproxy").contains(red.salida().strip()))throw new IOException("El acceso LAN requiere la red NAT de WSL; no se cambia la configuración global automáticamente.");}
                if(datos.get("modo").equals("STANDARD"))bind="127.0.0.1";
                int port=puerto(datos.getOrDefault("puerto","3000"));if(port<1024)throw new IllegalArgumentException("La web local necesita un puerto entre 1024 y 65535.");libre(bind,port);int control=disponible(),mail=disponible();while(control==port)control=disponible();while(mail==control||mail==port)mail=disponible();
                estado.setProperty("id",UUID.randomUUID().toString());estado.setProperty("proyecto","lamontana-"+estado.getProperty("id").replace("-","").substring(0,12));estado.setProperty("modo",datos.get("modo"));estado.setProperty("version",paquete.version());estado.setProperty("bind",bind);estado.setProperty("puerto",Integer.toString(port));estado.setProperty("control",Integer.toString(control));estado.setProperty("mailpit",Integer.toString(mail));estado.setProperty("estado","PREPARADA");
                var secrets=new Properties();secrets.setProperty("DB_PASSWORD",Util.secreto());secrets.setProperty("SETUP_TOKEN",Util.secreto());
                boolean standard=modo().equals("STANDARD");
                secrets.setProperty("SMTP_HOST",standard?datos.getOrDefault("smtpHost","").strip():"mailpit");secrets.setProperty("SMTP_PORT",standard?Integer.toString(puerto(datos.getOrDefault("smtpPuerto","587"))):"1025");secrets.setProperty("SMTP_FROM",standard?datos.getOrDefault("smtpFrom","").strip():"noreply@lamontana.local");secrets.setProperty("SMTP_USER",standard?datos.getOrDefault("smtpUser",""):"");secrets.setProperty("SMTP_PASSWORD",standard?datos.getOrDefault("smtpPassword",""):"");secrets.setProperty("SMTP_AUTH",Boolean.toString(standard));secrets.setProperty("SMTP_STARTTLS",Boolean.toString(standard&&!datos.getOrDefault("smtpTls","starttls").equals("ssl")));secrets.setProperty("SMTP_SSL",Boolean.toString(standard&&datos.getOrDefault("smtpTls","").equals("ssl")));
                if(standard)estado.setProperty("dominio",dominio(datos.getOrDefault("dominio","")));
                Util.guardar(raiz.resolve("secretos.properties"),secrets);Files.createDirectories(raiz.resolve("entrada"));Files.createDirectories(raiz.resolve("proxy"));persistir();
            }
            cargarImagenes();escribirConfiguracion();ejecutar("up","-d","--wait","--wait-timeout","600","--pull","never","postgres","antivirus","backend","frontend");if(modo().equals("DESARROLLO"))ejecutar("up","-d","--wait","--wait-timeout","120","--pull","never","mailpit");estado.setProperty("estado","CUENTAS_PENDIENTES");persistir();
            // Se detecta el resultado real del alta; nunca se cambian contraseñas al reabrir el asistente.
            String actual=api("GET","/api/instalador/estado",null);
            if(!actual.contains("\"cuentasCompletas\":true")){
                api("POST","/api/instalador/cuentas",cuentas(datos));
            }
            estado.setProperty("cuentas.completas","true");estado.setProperty("estado",modo().equals("DESARROLLO")?"LISTA":"PUBLICACION_PENDIENTE");persistir();
        }
    }
    static Map<String,Object> cuentas(Map<String,String> datos){
        String crear=datos.getOrDefault("crearSoporte","");
        if(!Set.of("","si").contains(crear))throw new IllegalArgumentException("Elección de soporte inválida.");
        var cuentas=new LinkedHashMap<String,Object>();cuentas.put("propietario",cuenta(datos,"propietario"));
        if(crear.equals("si"))cuentas.put("soporte",cuenta(datos,"soporte"));
        return cuentas;
    }
    static Map<String,String> cuenta(Map<String,String> d,String prefix){var c=new LinkedHashMap<String,String>();for(String f:List.of("nombre","apellido","correo","contrasena"))c.put(f,f.equals("contrasena")?contrasena(d,prefix+"."+f):Util.requerido(d,prefix+"."+f));return c;}
    static String contrasena(Map<String,String> d,String key){String value=d.getOrDefault(key,"");if(value.isBlank()||value.length()>4096||value.indexOf(0)>=0)throw new IllegalArgumentException("Completá la contraseña.");return value;}
    void cargarImagenes() throws Exception {var result=Util.ejecutar(equipo.docker("load","--input",equipo.rutaLinux(paquete.archivo("images.tar.gz"))),null,Duration.ofMinutes(20),null);if(!result.ok())throw new IOException("No se pudieron cargar las imágenes incluidas.");
        for(String service:List.of("postgres","backend","frontend","antivirus","mailpit","proxy")){String name=paquete.manifiesto.getProperty("image."+service,"");String expected=paquete.manifiesto.getProperty("image."+service+".id","");var r=equipo.run(equipo.docker("image","inspect",name,"--format","{{.Id}}"));if(!expected.matches("sha256:[a-f0-9]{64}")||!r.ok()||!r.salida().strip().equals(expected))throw new IOException("La imagen de "+service+" no corresponde al paquete.");}}
    void mantener(String operacion,Path destino) throws Exception {
        if(!Set.of("actualizar","reparar").contains(operacion)||!existe())throw new IllegalArgumentException("Operación de mantenimiento inválida.");
        validarAltaCompleta();
        equipo.validarMotor();paquete.verificar();Paquete.validarOperacion(operacion,estado.getProperty("version"),paquete.version());
        try(var channel=FileChannel.open(raiz.resolve(".instalador.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.tryLock()){
            if(lock==null)throw new IOException("Otra ejecución está trabajando en esta app.");
            estado=Util.properties(raiz.resolve("instalacion.properties"));Paquete.validarOperacion(operacion,estado.getProperty("version"),paquete.version());
            validarAltaCompleta();
            // La copia usa el inventario anterior, antes de cargar o modificar la versión nueva.
            Path copia;try{copia=Respaldo.crear(this,destino);}catch(Exception e){estado.setProperty("estado","RESPALDO_FALLIDO");persistir();throw e;}estado.setProperty("ultimo.respaldo",copia.toString());estado.setProperty("estado","MANTENIMIENTO_EN_CURSO");persistir();
            try{cargarImagenes();escribirConfiguracion();ejecutar("up","-d","--wait","--wait-timeout","600","--pull","never","postgres","antivirus","backend","frontend");if(modo().equals("DESARROLLO"))ejecutar("up","-d","--wait","--wait-timeout","120","--pull","never","mailpit");
                estado.setProperty("version",paquete.version());estado.setProperty("estado",modo().equals("DESARROLLO")?"LISTA":"PUBLICACION_PENDIENTE");estado.remove("publico.confirmado");persistir();
            }catch(Exception e){estado.setProperty("estado","MANTENIMIENTO_FALLIDO");persistir();throw e;}
        }
    }
    void validarAltaCompleta() throws IOException {
        if(!Boolean.parseBoolean(estado.getProperty("cuentas.completas","false")))throw new IOException("La instalación inicial quedó incompleta. Retomá la creación de cuentas antes de Reparar o Actualizar.");
    }
    void escribirConfiguracion() throws Exception {
        Util.copiarManifiesto(paquete.archivo("release.properties"),raiz.resolve("version.properties"));
        try(var in=getClass().getResourceAsStream("/despliegue/compose.yaml")){Files.copy(Objects.requireNonNull(in),raiz.resolve("compose.yaml"),StandardCopyOption.REPLACE_EXISTING);}
        escribirVariables();
    }
    void escribirVariables() throws Exception {
        var versionInstalada=Util.properties(raiz.resolve("version.properties"));
        var vars=Util.properties(raiz.resolve("secretos.properties"));vars.setProperty("INSTALLATION_ID",estado.getProperty("id"));vars.setProperty("LAMONTANA_MODO",modo());vars.setProperty("SESSION_COOKIE_SECURE",Boolean.toString(modo().equals("STANDARD")));vars.setProperty("APP_BIND",equipo.windows&&!estado.getProperty("bind").equals("127.0.0.1")?"0.0.0.0":estado.getProperty("bind"));vars.setProperty("APP_PORT",estado.getProperty("puerto"));vars.setProperty("CONTROL_PORT",estado.getProperty("control"));vars.setProperty("MAILPIT_PORT",estado.getProperty("mailpit"));vars.setProperty("WEB_SOURCE_DIR",equipo.rutaLinux(raiz.resolve("entrada")));
        for(String s:List.of("postgres","backend","frontend","antivirus","mailpit","proxy"))vars.setProperty("IMAGE_"+s.toUpperCase(Locale.ROOT),versionInstalada.getProperty("image."+s+".id"));
        var env=new StringBuilder();for(String key:new TreeSet<>(vars.stringPropertyNames()))env.append(key).append('=').append(dotenv(vars.getProperty(key))).append('\n');Path p=raiz.resolve(".env");Files.writeString(p,env);Util.privado(p,false);
    }
    static String dotenv(String value){if(value.indexOf('\n')>=0||value.indexOf('\r')>=0||value.indexOf('\0')>=0)throw new IllegalArgumentException("La configuración no admite saltos de línea.");return Util.json(value).replace("$","$$");}
    String api(String metodo,String path,Object body) throws Exception {var secret=Util.properties(raiz.resolve("secretos.properties")).getProperty("SETUP_TOKEN");var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+estado.getProperty("control")+path)).timeout(Duration.ofSeconds(30)).header("X-LaMontana-Instalacion",secret);if(body!=null)req.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(Util.json(body)));else req.GET();var r=HttpClient.newHttpClient().send(req.build(),HttpResponse.BodyHandlers.ofString());if(r.statusCode()/100!=2)throw new IOException("La app rechazó la operación de instalación (HTTP "+r.statusCode()+"). Verificá cuentas, correos, contraseñas y configuración SMTP.");return r.body();}
    static int puerto(String text){try{int p=Integer.parseInt(text);if(p<1||p>65535)throw new NumberFormatException();return p;}catch(NumberFormatException e){throw new IllegalArgumentException("Puerto inválido (1 a 65535).");}}
    static int disponible() throws IOException {try(var socket=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){return socket.getLocalPort();}}
    static void libre(String bind,int port)throws IOException{try(var s=new ServerSocket()){s.bind(new InetSocketAddress(bind,port));}catch(IOException e){throw new IOException("El puerto "+port+" está ocupado; elegí otro.");}}
    static String validarIPLocal(String ip)throws Exception{var address=InetAddress.getByName(ip);if(!address.getHostAddress().equals(ip)||address.isAnyLocalAddress()||address.isMulticastAddress()||NetworkInterface.getByInetAddress(address)==null)throw new IllegalArgumentException("Elegí una dirección IP de la red local de esta PC.");return ip;}
    static String dominio(String value){String d=value.strip().toLowerCase(Locale.ROOT);if(d.isEmpty())return "";if(d.length()>253||!d.matches("(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}"))throw new IllegalArgumentException("Ingresá un dominio sin protocolo, puerto ni ruta.");return d;}
    synchronized void iniciarPruebaPublica()throws Exception{if(!modo().equals("STANDARD"))throw new IllegalArgumentException("Esta prueba corresponde a Standard.");if(!api("GET","/api/instalador/estado",null).contains("\"cuentasCompletas\":true"))throw new IOException("Completá las cuentas antes de publicar la app.");String domain=dominio(estado.getProperty("dominio",""));if(domain.isBlank())throw new IOException("Falta configurar el dominio.");InetAddress.getAllByName(domain);codigoPublico=Util.secreto().substring(0,12).toUpperCase(Locale.ROOT);String nonce=Util.secreto();vencePublico=Instant.now().plusSeconds(600);rutaPrueba="https://"+domain+"/__verificacion/"+nonce;estado.setProperty("publico.prueba",vencePublico.toString());persistir();escribirProxy("/__verificacion/"+nonce,codigoPublico);String vigente=codigoPublico;expiraciones.schedule(()->{synchronized(this){if(Objects.equals(vigente,codigoPublico)){try{modificar(this::cerrarPruebaPublica);}catch(Exception ignored){/* La siguiente apertura reintenta retirar la ruta. */}}}},600,java.util.concurrent.TimeUnit.SECONDS);ejecutar("--profile","standard","up","-d","--pull","never","proxy");ejecutar("exec","-T","proxy","caddy","reload","--config","/etc/caddy/Caddyfile");estado.setProperty("estado","VERIFICACION_PUBLICA_PENDIENTE");persistir();}
    void escribirProxy(String ruta,String codigo)throws IOException {String domain=dominio(estado.getProperty("dominio",""));String config=domain+" {\n  @privado path /api/instalador/* /api/setup/*\n  respond @privado 404\n"+(ruta==null?"":"  respond "+ruta+" \""+codigo+"\" 200\n")+"  reverse_proxy frontend:3000\n}\n";Files.writeString(raiz.resolve("proxy/Caddyfile"),config);}
    synchronized void confirmarPublico(String codigo)throws Exception{if(codigoPublico==null||!Instant.now().isBefore(vencePublico)||!codigoPublico.equals(codigo.strip().toUpperCase(Locale.ROOT)))throw new IllegalArgumentException("El código público no coincide o venció. Repetí la prueba con Wi-Fi apagado.");
        var r=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build().send(HttpRequest.newBuilder(URI.create(rutaPrueba)).timeout(Duration.ofSeconds(20)).build(),HttpResponse.BodyHandlers.ofString());if(r.statusCode()!=200||!r.body().strip().equals(codigoPublico))throw new IOException("El código fue confirmado, pero esta PC todavía no pudo validar HTTPS y la respuesta del dominio. Revisá DNS y acceso al dominio desde la red local.");
        cerrarPruebaPublica();estado.setProperty("publico.confirmado",Instant.now().toString());evaluarListo();}
    void probarCorreo(String destino)throws Exception{if(!modo().equals("STANDARD"))throw new IllegalArgumentException("Desarrollo utiliza Mailpit.");codigoCorreo=Util.secreto().substring(0,12).toUpperCase(Locale.ROOT);venceCorreo=Instant.now().plusSeconds(600);estado.remove("smtp.confirmado");api("POST","/api/instalador/correo",Map.of("destino",destino,"codigo",codigoCorreo));persistir();}
    void confirmarCorreo(String codigo)throws Exception{if(codigoCorreo==null||!Instant.now().isBefore(venceCorreo)||!codigoCorreo.equals(codigo.strip().toUpperCase(Locale.ROOT)))throw new IllegalArgumentException("El código de correo no coincide o venció.");codigoCorreo=null;estado.setProperty("smtp.confirmado",Instant.now().toString());evaluarListo();}
    void evaluarListo()throws Exception{boolean servicios=saludable(true);estado.setProperty("estado",servicios&&estado.containsKey("smtp.confirmado")&&estado.containsKey("publico.confirmado")&&Boolean.parseBoolean(estado.getProperty("arranque.verificado","false"))&&Boolean.parseBoolean(estado.getProperty("cuentas.completas","false"))?"LISTA":"PUBLICACION_PENDIENTE");persistir();}
    synchronized void cerrarPruebaPublica() throws Exception {
        if(!estado.containsKey("publico.prueba"))return;
        escribirProxy(null,null);ejecutar("exec","-T","proxy","caddy","reload","--config","/etc/caddy/Caddyfile");
        codigoPublico=null;rutaPrueba=null;estado.remove("publico.prueba");persistir();
    }
    void cerrar(){expiraciones.shutdownNow();try{modificar(this::cerrarPruebaPublica);}catch(Exception ignored){/* Se detecta al reabrir. */}}
    boolean saludable(boolean publico) throws Exception {
        var ids=equipo.run(compose("--profile","standard","ps","-q"));if(!ids.ok())return false;
        var vistos=new HashSet<String>();
        for(String id:ids.salida().lines().filter(v->v.matches("[a-f0-9]{12,64}")).toList()){
            var r=equipo.run(equipo.docker("inspect","--format","{{index .Config.Labels `com.docker.compose.service`}}|{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-control{{end}}",id));
            String[] row=r.salida().strip().split("\\|");
            if(r.ok()&&row.length==3&&row[1].equals("running")&&(row[2].equals("healthy")||row[0].equals("proxy")&&row[2].equals("sin-control")))vistos.add(row[0]);
        }
        return vistos.containsAll(List.of("postgres","backend","frontend","antivirus"))&&(!publico||vistos.contains("proxy"))&&(!modo().equals("DESARROLLO")||vistos.contains("mailpit"));
    }
    void configurarPublicacion(Map<String,String> datos) throws Exception {
        if(!modo().equals("STANDARD")||!Boolean.parseBoolean(estado.getProperty("cuentas.completas","false")))throw new IOException("Primero completá una instalación Standard y sus cuentas.");
        String domain=dominio(Util.requerido(datos,"dominio"));String host=Util.requerido(datos,"smtpHost");
        if(!host.matches("[a-zA-Z0-9.-]+"))throw new IllegalArgumentException("Servidor SMTP inválido.");
        String from=Util.requerido(datos,"smtpFrom"),user=Util.requerido(datos,"smtpUser");
        if(!from.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new IllegalArgumentException("Correo remitente inválido.");
        String tls=Util.requerido(datos,"smtpTls");if(!Set.of("starttls","ssl").contains(tls))throw new IllegalArgumentException("El correo Standard requiere TLS.");
        var secret=Util.properties(raiz.resolve("secretos.properties"));String password=datos.getOrDefault("smtpPassword","");
        if(!password.isEmpty())secret.setProperty("SMTP_PASSWORD",password);
        if(secret.getProperty("SMTP_PASSWORD","").isEmpty())throw new IllegalArgumentException("Completá la contraseña SMTP.");
        secret.setProperty("SMTP_HOST",host);secret.setProperty("SMTP_PORT",Integer.toString(puerto(Util.requerido(datos,"smtpPuerto"))));secret.setProperty("SMTP_FROM",from);secret.setProperty("SMTP_USER",user);secret.setProperty("SMTP_AUTH","true");secret.setProperty("SMTP_STARTTLS",Boolean.toString(tls.equals("starttls")));secret.setProperty("SMTP_SSL",Boolean.toString(tls.equals("ssl")));
        // Retirar la publicación antes de cambiar dominio o credenciales. No toca imágenes ni datos.
        ejecutar("--profile","standard","stop","proxy");estado.setProperty("dominio",domain);estado.remove("publico.confirmado");estado.remove("smtp.confirmado");estado.setProperty("estado","PUBLICACION_PENDIENTE");
        Util.guardar(raiz.resolve("secretos.properties"),secret);persistir();escribirVariables();escribirProxy(null,null);
        ejecutar("up","-d","--wait","--wait-timeout","600","--pull","never","postgres","antivirus","backend","frontend");
    }

    String bootId() throws Exception {
        if(!equipo.windows)return Files.readString(Path.of("/proc/sys/kernel/random/boot_id")).strip();
        var r=equipo.run(List.of("powershell.exe","-NoProfile","-NonInteractive","-Command","(Get-CimInstance Win32_OperatingSystem).LastBootUpTime.ToUniversalTime().Ticks"));
        if(!r.ok()||!r.salida().strip().matches("[0-9]+"))throw new IOException("No se pudo identificar el inicio de Windows.");return r.salida().strip();
    }
    void sistema(String accion) throws Exception {
        if(!Set.of("arranque","red").contains(accion)||!existe())throw new IllegalArgumentException("Operación inválida.");
        if(accion.equals("arranque")&&(!modo().equals("STANDARD")||!Boolean.parseBoolean(estado.getProperty("cuentas.completas","false"))||estado.getProperty("dominio","").isBlank()))throw new IOException("Configurá Standard, sus cuentas y dominio antes de registrar el arranque.");
        String bind=modo().equals("STANDARD")?"0.0.0.0":estado.getProperty("bind");
        if(accion.equals("red")&&bind.equals("127.0.0.1"))throw new IOException("La instalación está limitada a esta PC. No necesita abrir el firewall.");
        Path script=equipo.script(equipo.windows?"servicio.ps1":"servicio.sh");
        var args=new ArrayList<>(List.of(accion,raiz.toString(),proyecto(),bind,estado.getProperty("puerto")));
        if(equipo.windows){var user=equipo.run(List.of("whoami.exe"));if(!user.ok())throw new IOException("No se pudo identificar la cuenta de Windows.");args.add(user.salida().strip());args.add(equipo.script("trabajador.ps1").toString());}
        equipo.elevar(script,args);
        if(accion.equals("arranque")){estado.setProperty("arranque.configurado","true");estado.setProperty("arranque.boot.configuracion",bootId());estado.remove("arranque.verificado");}
        persistir();
    }
    void verificarArranque() throws Exception {
        if(!modo().equals("STANDARD")||!Boolean.parseBoolean(estado.getProperty("arranque.configurado","false")))throw new IOException("Primero registrá el arranque automático Standard.");
        String actual=bootId();var marker=Util.properties(raiz.resolve("arranque.properties"));
        if(actual.equals(estado.getProperty("arranque.boot.configuracion"))||!actual.equals(marker.getProperty("boot.id"))||!saludable(true))throw new IOException("Falta comprobar un reinicio posterior a la configuración con los servicios saludables. Standard sigue pendiente.");
        estado.setProperty("arranque.verificado","true");evaluarListo();
    }

}
