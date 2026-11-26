package ar.com.lamontana.instalador;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Operaciones de plataforma acotadas; no recibe comandos del navegador. */
class Equipo {
    static final String DISTRO="LaMontanaDemo-Ubuntu";
    final boolean windows=System.getProperty("os.name").startsWith("Windows");
    final Paquete paquete;
    private Process sesionWsl;
    Equipo(Paquete paquete){this.paquete=paquete;}
    List<String> docker(String...args){var cmd=new ArrayList<String>();if(windows)cmd.addAll(List.of("wsl.exe","--distribution",DISTRO,"--user","root","--exec"));cmd.add("docker");cmd.addAll(List.of(args));return cmd;}
    Util.Resultado run(List<String> cmd) {
        return run(cmd,Duration.ofSeconds(windows&&cmd.get(0).equals("wsl.exe")?120:30));
    }
    Util.Resultado run(List<String> cmd, Duration timeout){
        try {return Util.ejecutar(cmd,null,timeout,null);}
        catch(Exception e){return new Util.Resultado(127,"No se pudo ejecutar "+cmd.get(0)+". Verificá su instalación y permisos.");}
    }
    synchronized void mantenerWsl() throws IOException {
        if(sesionWsl!=null&&sesionWsl.isAlive())return;
        // El cliente de systemd-run queda esperando en WSL mientras la Demo está instalada.
        // El nombre de unidad identifica exclusivamente esta espera y permite terminarla desde otro proceso.
        sesionWsl=new ProcessBuilder("wsl.exe","-d",DISTRO,"-u","root","--exec","systemd-run","--wait","--collect","--quiet",
                "--unit=lamontana-demo-espera","/usr/bin/sleep","infinity")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        sesionWsl.getOutputStream().close();
    }
    boolean distroPresente(boolean soloEnEjecucion) throws IOException {
        if(!windows)return true;
        var cmd=new ArrayList<>(List.of("wsl.exe","--list","--quiet"));
        if(soloEnEjecucion)cmd.add("--running");
        var r=run(cmd);
        if(!r.ok())throw new IOException("No se pudieron consultar las distribuciones de WSL2.");
        return r.salida().replace("\0","").lines().map(String::strip).anyMatch(DISTRO::equalsIgnoreCase);
    }
    List<String> recursosDemo() throws IOException {
        var r=run(docker("ps","-a","--filter","label=ar.com.lamontana.instalacion",
                "--format","{{.Label `ar.com.lamontana.instalacion`}}|{{.Label `com.docker.compose.project`}}|{{.Label `com.docker.compose.project.working_dir`}}"));
        if(!r.ok())throw new IOException("No se pudieron identificar los servicios de la Demo en Docker.");
        return r.salida().lines().filter(line->line.contains("|lamontana-demo-")).toList();
    }
    void liberarEsperaSiCorresponde() throws IOException {
        if(!windows||!recursosDemo().isEmpty())return;
        var cmd=List.of("wsl.exe","-d",DISTRO,"-u","root","--exec","sh","-c",
                "if systemctl is-active --quiet lamontana-demo-espera.service; then systemctl stop lamontana-demo-espera.service; fi");
        if(!run(cmd).ok())throw new IOException("No se pudo cerrar la espera de La Montaña Demo en WSL2.");
    }
    void prepararRetiro() throws IOException {
        if(windows&&!run(List.of("wsl.exe","-d",DISTRO,"-u","root","--exec","systemctl","start","docker")).ok())
            throw new IOException("No se pudo iniciar Docker en la distribución de la Demo para retirar sus servicios.");
    }
    Path rutaWindows(String linux) throws IOException {
        if(!windows)return Carpetas.ruta(linux);
        var r=run(List.of("wsl.exe","-d",DISTRO,"-u","root","--exec","wslpath","-w",linux));
        if(!r.ok())throw new IOException("No se pudo localizar la carpeta Windows de una instalación de la Demo.");
        return Carpetas.ruta(r.salida().strip());
    }
    Path directorioVigencia() throws IOException {
        if(windows) {
            var p=run(List.of("powershell.exe","-NoProfile","-NonInteractive","-Command","[Environment]::GetFolderPath('CommonApplicationData')"));
            if(!p.ok()||p.salida().isBlank()) throw new IOException("Windows no informó la carpeta de datos del equipo.");
            return Carpetas.ruta(p.salida().strip()).resolve("YG-Soluciones/LaMontanaDemo/vigencia");
        }
        return Path.of(System.getProperty("user.home"),".local","state","YG-Soluciones","LaMontanaDemo","vigencia");
    }
    boolean discoLocal(Path archivo) throws IOException {
        Path real = archivo.toRealPath();
        if (Carpetas.red(Files.getFileStore(real))) return false;
        if (!windows) return true;
        String unidad = real.getRoot().toString().replace("'", "''");
        var tipo = run(List.of("powershell.exe","-NoProfile","-NonInteractive","-Command",
                "([IO.DriveInfo]::new('" + unidad + "')).DriveType.ToString()"));
        return tipo.ok() && Set.of("Fixed","Removable","CDRom").contains(tipo.salida().strip());
    }

    String rutaLinux(Path path) throws Exception {if(!windows)return path.toAbsolutePath().toString();var r=run(List.of("wsl.exe","-d",DISTRO,"-u","root","--exec","wslpath","-u",path.toAbsolutePath().toString()));if(!r.ok())throw new IOException("No se pudo traducir la ruta para WSL2.");return r.salida().strip();}
    List<Map<String,String>> diagnostico(){
        var rows=new ArrayList<Map<String,String>>();
        boolean supported;boolean dockerPosible=true;
        if(windows){supported=sistemaCompatible();var wsl=run(List.of("wsl.exe","--list","--verbose"));String listado=wsl.salida().replace("\0","");boolean wsl2=wsl.ok()&&listado.lines().anyMatch(line->line.contains(DISTRO)&&line.strip().endsWith("2"));dockerPosible=wsl2;rows.add(row("wsl",wsl2?"WSL2_DISPONIBLE":"PENDIENTE",listado));
            var virtual=run(List.of("powershell.exe","-NoProfile","-NonInteractive","-Command","Get-CimInstance Win32_Processor | Select-Object VirtualizationFirmwareEnabled,SecondLevelAddressTranslationExtensions | Format-List"));rows.add(row("virtualizacion","DIAGNOSTICO",virtual.salida()));
            var otro=run(List.of("docker.exe","info","--format","{{.OperatingSystem}}"));if(otro.ok())rows.add(row("dockerWindows","EXISTENTE",otro.salida()+". Se conserva; La Montaña utiliza Docker Engine dentro de "+DISTRO+"."));}
        else supported=sistemaCompatible();
        rows.add(row("sistema",supported?"COMPATIBLE":"NO_VALIDADO",System.getProperty("os.name")+" / "+System.getProperty("os.arch")));
        var pendienteWsl=new Util.Resultado(127,"Primero completá WSL2 y la distribución LaMontanaDemo-Ubuntu; después verificá Docker.");
        var cli=dockerPosible?run(docker("--version")):pendienteWsl;var daemon=dockerPosible?run(docker("info","--format","{{.ServerVersion}}")):pendienteWsl;
        String state=daemon.ok()?(versionMinima(daemon.salida().strip(),24,0)?"DISPONIBLE":"INCOMPATIBLE"):!cli.ok()?"NO_ACCESIBLE":daemon.salida().toLowerCase(Locale.ROOT).contains("permission denied")?"SIN_PERMISOS":"DETENIDO_O_INACCESIBLE";
        rows.add(row("docker",state,daemon.ok()?"Docker Engine "+daemon.salida().strip():daemon.salida().strip()));
        var compose=dockerPosible?run(docker("compose","version","--short")):pendienteWsl;rows.add(row("compose",compose.ok()?(versionMinima(compose.salida().strip(),2,20)?"DISPONIBLE":"INCOMPATIBLE"):"PENDIENTE",compose.salida().strip()));
        if(daemon.ok()){
            var recursos=run(docker("info","--format","CPU={{.NCPU}}; RAM Docker={{.MemTotal}} bytes; plataforma={{.OperatingSystem}}"));rows.add(row("recursos","DIAGNOSTICO",recursos.salida()));
            var anterior=run(docker("ps","-a","--filter","label=com.docker.compose.project=lamontana-demo","--format","{{.Names}} | {{.Label `com.docker.compose.project.working_dir`}}"));
            if(anterior.ok()&&!anterior.salida().isBlank())rows.add(row("instalacionAnterior","REQUIERE_IDENTIFICACION","Se detectó una instalación del script anterior. No se adopta ni sobrescribe automáticamente: "+anterior.salida()));
        }
        try {
            paquete.verificar();
            boolean completo = Boolean.parseBoolean(paquete.manifiesto.getProperty("offline.complete","false"));
            boolean externas = FirmasAntivirus.externas(paquete.manifiesto);
            String detalle = externas ? "En la primera instalación se necesita obtener la base oficial de ClamAV por Internet o desde un disco o USB."
                    : completo ? "Dependencias incluidas." : "Paquete para pruebas con dependencias previamente instaladas.";
            rows.add(row("paquete", completo || externas ? "VERIFICADO" : "SOLO_APP",
                    "Versión " + paquete.version() + "; integridad comprobada. " + detalle));
        } catch (Exception e) { rows.add(row("paquete","INCOMPLETO",e.getMessage())); }
        return rows;
    }
    boolean sistemaCompatible(){
        if(!Set.of("amd64","x86_64").contains(System.getProperty("os.arch")))return false;
        if(windows){var r=run(List.of("powershell.exe","-NoProfile","-NonInteractive","-Command","[Environment]::OSVersion.Version.Build"));return r.ok()&&r.salida().strip().matches("[0-9]{5,6}")&&Integer.parseInt(r.salida().strip())>=22000;}
        var os=new Properties();try{for(String line:Files.readAllLines(Path.of("/etc/os-release"))){String[] p=line.split("=",2);if(p.length==2)os.setProperty(p[0],p[1].replace("\"",""));}}catch(IOException e){return false;}
        return os.getProperty("ID","").equals("ubuntu")&&os.getProperty("VERSION_ID","").equals("26.04");
    }
    void validarMotor()throws IOException{
        if(!sistemaCompatible())throw new IOException("Esta versión requiere Ubuntu 26.04 o Windows 11 x64. No se modificó la app.");
        var docker=run(docker("info","--format","{{.ServerVersion}}"));var compose=run(docker("compose","version","--short"));
        if(!docker.ok()||!versionMinima(docker.salida().strip(),24,0)||!compose.ok()||!versionMinima(compose.salida().strip(),2,20))throw new IOException("Completá la verificación: se requiere Docker Engine accesible >= 24 y Compose >= 2.20 antes de instalar o mantener la app.");
    }
    static boolean versionMinima(String text,int major,int minor){var matcher=java.util.regex.Pattern.compile("^v?(\\d+)\\.(\\d+)(?:[. -].*)?$").matcher(text);if(!matcher.matches())return false;try{int a=Integer.parseInt(matcher.group(1)),b=Integer.parseInt(matcher.group(2));return a>major||a==major&&b>=minor;}catch(NumberFormatException e){return false;}}
    static Map<String,String> row(String id,String estado,String detalle){return Map.of("id",id,"estado",estado,"detalle",detalle);}
    void dependencia(String accion,String via) throws Exception {
        if(!Set.of("docker","iniciar","wsl","ubuntu","navegador").contains(accion)||!Set.of("incluida","internet").contains(via))throw new IllegalArgumentException("Acción de dependencia no válida.");
        if(!windows&&Set.of("wsl","ubuntu").contains(accion))throw new IllegalArgumentException("Esta operación corresponde a Windows.");
        Path payload=null;
        if(accion.equals("docker")&&via.equals("internet")){payload=descargarPaquetes();}
        else if(!accion.equals("iniciar")){
            String key="dep."+(windows?"windows.":"ubuntu.")+accion;
            String relative=paquete.manifiesto.getProperty(key+".file","");String hash=paquete.manifiesto.getProperty(key+".sha256","");
            if(relative.isBlank()||!hash.matches("[a-f0-9]{64}"))throw new IOException("Este paquete no contiene la dependencia solicitada. Usá la instalación manual y verificá de nuevo.");
            if(via.equals("incluida"))payload=paquete.archivo(relative);
            else{
                URI uri=URI.create(paquete.manifiesto.getProperty(key+".url",""));if(!"https".equals(uri.getScheme()))throw new IOException("No hay una descarga oficial HTTPS definida para esta dependencia.");
                payload=Files.createTempFile("lamontana-dependencia-",relative.substring(relative.lastIndexOf('.')));Util.privado(payload,false);
                var c=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();var response=c.send(HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(30)).build(),HttpResponse.BodyHandlers.ofFile(payload));if(response.statusCode()!=200)throw new IOException("Falló la descarga oficial (HTTP "+response.statusCode()+").");
            }
            if(!Util.sha256(payload).equals(hash))throw new IOException("La dependencia no coincide con su SHA-256; no se ejecutó.");
        }
        Path script=script(windows?"dependencias.ps1":"dependencias.sh");
        if(windows&&accion.equals("docker")){
            Path auxiliar=script.getParent().resolve("dependencias.sh");
            try(var in=getClass().getResourceAsStream("/sistema/dependencias.sh")){Files.copy(Objects.requireNonNull(in),auxiliar);}
            Util.privado(auxiliar,false);
        }
        var args=new ArrayList<String>();args.add(accion);args.add(payload==null?"":payload.toAbsolutePath().toString());
        if(windows&&Set.of("ubuntu","docker","iniciar").contains(accion)){
            var command=new ArrayList<>(List.of("powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",script.toString()));command.addAll(args);
            var result=Util.ejecutar(command,null,Duration.ofMinutes(40),null);
            if(!result.ok()){String detalle=result.salida().strip();throw new IOException("No se completó la preparación de Ubuntu/Docker (código "+result.codigo()+"). Volvé a verificar dependencias.\n"+detalle.substring(Math.max(0,detalle.length()-3000)));}
        }else elevar(script,args);
    }
    Path descargarPaquetes() throws Exception {
        paquete.verificar();
        var lista=Util.properties(paquete.archivo("docker-descargas.properties"));
        int count=Integer.parseInt(lista.getProperty("count","0"));
        if(count<1||count>1000)throw new IOException("Inventario de dependencias inválido.");
        Path dir=Files.createTempDirectory("lamontana-paquetes-");Util.privado(dir,true);
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();
        for(int i=0;i<count;i++){
            String name=lista.getProperty("file."+i,"");String hash=lista.getProperty("sha256."+i,"");
            if(!name.matches("[a-zA-Z0-9_.+%:~-]+\\.deb")||!hash.matches("[a-f0-9]{64}"))throw new IOException("Inventario de paquetes inválido.");
            URI uri=URI.create(lista.getProperty("url."+i,""));
            if(!"https".equals(uri.getScheme())||!Set.of("archive.ubuntu.com","security.ubuntu.com","download.docker.com").contains(uri.getHost()))throw new IOException("El paquete no tiene un origen oficial permitido.");
            Path file=dir.resolve(name);
            var r=client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(10)).build(),HttpResponse.BodyHandlers.ofFile(file));
            Util.privado(file,false);
            if(r.statusCode()!=200||!Util.sha256(file).equals(hash))throw new IOException("Falló la descarga o integridad de "+name+". No se instaló.");
        }
        return dir;
    }
    Path script(String name) throws IOException {Path dir=Files.createTempDirectory("lamontana-sistema-");Util.privado(dir,true);Path p=dir.resolve(name);try(var in=getClass().getResourceAsStream("/sistema/"+name)){try(var out=Files.newOutputStream(p)){if(name.endsWith(".ps1"))out.write(new byte[]{(byte)0xef,(byte)0xbb,(byte)0xbf});Objects.requireNonNull(in).transferTo(out);}}Util.privado(p,false);return p;}
    void elevar(Path script,List<String> args) throws Exception {
        List<String> cmd;
        if(windows){
            // Solo literales PowerShell; comillas simples duplicadas y límites de argv preservados.
            var words=new ArrayList<String>();words.addAll(List.of("-NoProfile","-ExecutionPolicy","Bypass","-File",script.toString()));words.addAll(args);
            String argumentLine=String.join(" ",words.stream().map(Equipo::windowsArgument).toList());
            String command="$p=Start-Process -FilePath powershell.exe -Verb RunAs -Wait -PassThru -ArgumentList '"+argumentLine.replace("'","''")+"'; exit $p.ExitCode";
            cmd=List.of("powershell.exe","-NoProfile","-NonInteractive","-Command",command);
        }else{var list=new ArrayList<String>(List.of("pkexec","/bin/bash",script.toString()));list.addAll(args);cmd=list;}
        var result=Util.ejecutar(cmd,null,Duration.ofMinutes(40),null);
        if(result.codigo()==3010)throw new IOException("La dependencia requiere reiniciar Windows. Reiniciá y abrí el instalador desde el comienzo.");
        if(!result.ok())throw new IOException("La operación del sistema no terminó (código "+result.codigo()+"). Revisá el diálogo de permisos y volvé a verificar dependencias.");
    }
    static String windowsArgument(String s){if(s.indexOf('\n')>=0||s.indexOf('\r')>=0||s.indexOf('\0')>=0)throw new IllegalArgumentException("Argumento no válido.");return "\""+s.replaceAll("(\\\\*)\"","$1$1\\\\\"").replaceAll("(\\\\+)$","$1$1")+"\"";}
}
