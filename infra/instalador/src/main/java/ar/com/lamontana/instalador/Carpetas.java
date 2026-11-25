package ar.com.lamontana.instalador;

import java.awt.GraphicsEnvironment;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

/** Descubrimiento local de lectura. No construye Instalacion ni lee sus secretos. */
final class Carpetas implements AutoCloseable {
    record Limites(int profundidad,int entradas,long milisegundos,int resultados) {}
    static final Limites RAPIDA=new Limites(6,20_000,15_000,100), AMPLIADA=new Limites(Integer.MAX_VALUE,100_000,60_000,100);
    private final Equipo equipo;
    private final ExecutorService tarea=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"carpetas-locales");t.setDaemon(true);return t;});
    private volatile boolean ocupado,cancelado,cerrado;
    private volatile Process proceso;
    private volatile JFileChooser selector;
    private volatile Map<String,Object> estado=Map.of("estado","LIBRE","tipo","");
    Carpetas(Equipo equipo){this.equipo=equipo;}
    static Path recomendada(){return Path.of(System.getProperty("user.home"),"LaMontana").toAbsolutePath().normalize();}
    static Path ruta(String texto){
        if(texto==null||texto.isBlank()||texto.length()>4096||texto.chars().anyMatch(c->c<32))throw new IllegalArgumentException("Ingresá una ruta completa válida.");
        try{Path p=Path.of(texto);if(!p.isAbsolute()||texto.startsWith("\\\\"))throw new IllegalArgumentException("Elegí una carpeta de un disco local con su ruta completa.");return p.normalize();}
        catch(InvalidPathException e){throw new IllegalArgumentException("La ruta contiene caracteres no válidos.");}
    }
    boolean ocupado(){return ocupado;}
    Map<String,Object> estado(){return estado;}
    private synchronized void iniciar(String tipo,Runnable accion){
        if(cerrado||ocupado)throw new IllegalArgumentException("Esperá a que termine o cancelá la selección/búsqueda actual.");
        cancelado=false;ocupado=true;estado=Map.of("estado","EJECUTANDO","tipo",tipo,"mensaje",tipo.equals("SELECTOR")?"Elegí una carpeta en la ventana del sistema.":"Buscando instalaciones…");
        tarea.submit(()->{try{accion.run();}catch(Exception|LinkageError|java.awt.AWTError e){estado=Map.of("estado",cancelado?"CANCELADO":"ERROR","tipo",tipo,"mensaje",cancelado?"Operación cancelada.":Objects.toString(e.getMessage(),"No se pudo completar la operación."));}finally{ocupado=false;}});
    }
    synchronized void cancelar(){
        if(!ocupado)return;cancelado=true;
        Process p=proceso;if(p!=null)p.destroy();
        JFileChooser c=selector;if(c!=null)SwingUtilities.invokeLater(c::cancelSelection);
        // El recorrido comprueba esta señal entre entradas; no se cancela el Future
        // antes de que publique los resultados parciales y libere sus recursos.
    }
    @Override public synchronized void close(){cerrado=true;cancelar();tarea.shutdown();}
    void examinar(String inicial){
        Path p=inicial==null||inicial.isBlank()?recomendada():ruta(inicial);
        iniciar("SELECTOR",()->{try{
            String elegida=elegir(p);
            estado=Map.of("estado",cancelado||elegida.isEmpty()?"CANCELADO":"COMPLETADO","tipo","SELECTOR","directorio",cancelado?"":elegida,"mensaje",cancelado||elegida.isEmpty()?"Selección cancelada; se conserva la carpeta anterior.":"Carpeta elegida. Comprobala antes de continuar.");
        }catch(Exception e){throw new IllegalArgumentException("No se pudo abrir el selector de carpetas. Podés escribir la ruta completa. "+Objects.toString(e.getMessage(),""));}});
    }
    private String elegir(Path inicial)throws Exception{
        while(inicial!=null&&!Files.isDirectory(inicial))inicial=inicial.getParent();
        if(inicial==null)inicial=Path.of(System.getProperty("user.home"));
        if(cancelado)return "";
        if(equipo.windows){
            String base64=Base64.getEncoder().encodeToString(inicial.toString().getBytes(StandardCharsets.UTF_8));
            String ps="[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false); Add-Type -AssemblyName System.Windows.Forms; "+
                "$d=[System.Windows.Forms.FolderBrowserDialog]::new(); $d.Description='Elegir carpeta de La Montaña'; "+
                "$d.SelectedPath=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('"+base64+"')); "+
                "try {if($d.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK){[Console]::Write($d.SelectedPath)}else{exit 1}} finally {$d.Dispose()}";
            try{return selectorNativo(List.of("powershell.exe","-NoProfile","-STA","-EncodedCommand",Base64.getEncoder().encodeToString(ps.getBytes(StandardCharsets.UTF_16LE))));}
            catch(IOException e){if(cancelado)return "";}
        }else if(Files.isExecutable(Path.of("/usr/bin/zenity"))&&(System.getenv("DISPLAY")!=null||System.getenv("WAYLAND_DISPLAY")!=null)){
            try{return selectorNativo(List.of("/usr/bin/zenity","--file-selection","--directory","--title=Elegir carpeta de La Montaña","--filename="+inicial+File.separator));}
            catch(IOException e){if(cancelado)return "";}
        }
        if(GraphicsEnvironment.isHeadless())throw new IOException("No hay una sesión gráfica disponible.");
        final Path desde=inicial;final String[] elegida={""};
        SwingUtilities.invokeAndWait(()->{
            if(cancelado)return;
            try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}
            var c=new JFileChooser(desde.toFile());selector=c;c.setDialogTitle("Elegir carpeta de La Montaña");c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);c.setAcceptAllFileFilterUsed(false);
            try{if(!cancelado&&c.showDialog(null,"Elegir carpeta")==JFileChooser.APPROVE_OPTION)elegida[0]=c.getSelectedFile().toPath().toAbsolutePath().normalize().toString();}
            finally{selector=null;}
        });
        return elegida[0];
    }
    private String selectorNativo(List<String> comando)throws Exception{
        Process p=new ProcessBuilder(comando).redirectError(ProcessBuilder.Redirect.DISCARD).start();proceso=p;p.getOutputStream().close();
        var salida=new ByteArrayOutputStream();
        Thread lector=new Thread(()->{try(var in=p.getInputStream()){byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1)synchronized(salida){if(salida.size()+n<=16384)salida.write(b,0,n);}}catch(IOException ignored){}},"ruta-elegida");lector.setDaemon(true);lector.start();
        try{
            if(cancelado)p.destroy();
            if(!p.waitFor(5,TimeUnit.MINUTES)){p.destroy();throw new IOException("Se agotó el tiempo del selector.");}
            lector.join(1000);
            if(cancelado||p.exitValue()==1)return "";
            if(p.exitValue()!=0)throw new IOException("El selector del sistema no está disponible.");
            String valor; synchronized(salida){valor=salida.toString(StandardCharsets.UTF_8).strip();}
            Path dir=ruta(valor);if(!Files.isDirectory(dir))throw new IOException("La carpeta elegida ya no está disponible.");return dir.toString();
        }finally{if(p.isAlive())p.destroyForcibly();proceso=null;}
    }
    void buscar(String raiz){
        if(raiz!=null&&!raiz.isBlank()){Path p=ruta(raiz);if(!Files.isDirectory(p))throw new IllegalArgumentException("Elegí una carpeta existente para ampliar la búsqueda.");buscar(List.of(p),AMPLIADA,false);}
        else{
            var raices=new LinkedHashSet<Path>();raices.add(recomendada());raices.add(Path.of(System.getProperty("user.home")));
            if(equipo.windows){for(String key:List.of("LOCALAPPDATA","ProgramData")){String v=System.getenv(key);if(v!=null&&!v.isBlank())raices.add(Path.of(v,"LaMontana"));}}
            else raices.addAll(List.of(Path.of("/opt"),Path.of("/srv")));
            buscar(new ArrayList<>(raices),RAPIDA,true);
        }
    }
    void buscar(List<Path> raices,Limites limites,boolean docker){iniciar("BUSQUEDA",()->new Exploracion(limites).ejecutar(raices,docker));}
    private final class Exploracion {
        final Limites limites;final long vence;int entradas;long ultimo;
        final List<Map<String,Object>> resultados=new ArrayList<>();
        final List<String> revisadas=new ArrayList<>();
        final Map<String,Integer> omisiones=new TreeMap<>();
        final Set<Path> vistas=new HashSet<>(),halladas=new HashSet<>();
        final Set<Path> montajes=montajes();
        boolean limite;
        Exploracion(Limites l){limites=l;vence=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(l.milisegundos());}
        boolean parar(){if(cancelado||Thread.currentThread().isInterrupted())return true;if(System.nanoTime()>=vence||entradas>=limites.entradas()||resultados.size()>=limites.resultados()){limite=true;return true;}return false;}
        void omitir(String razon){omisiones.merge(razon,1,Integer::sum);}
        void publicar(String status){
            long ahora=System.nanoTime();if(status.equals("EJECUTANDO")&&ahora-ultimo<100_000_000)return;ultimo=ahora;
            var copia=new ArrayList<Map<String,Object>>();for(var r:resultados)copia.add(Map.copyOf(r));
            String mensaje=status.equals("EJECUTANDO")?"Buscando instalaciones…":status.equals("CANCELADO")?"Búsqueda cancelada. Se conservan los resultados encontrados.":resultados.isEmpty()?"No se encontraron instalaciones en las ubicaciones revisadas.":"Búsqueda terminada. Elegí una carpeta y comprobala.";
            estado=Map.of("estado",status,"tipo","BUSQUEDA","mensaje",mensaje,"resultados",copia,"revisadas",List.copyOf(revisadas),"entradas",entradas,"omisiones",Map.copyOf(omisiones),"parcial",limite||cancelado||!omisiones.isEmpty());
        }
        void ejecutar(List<Path> raices,boolean docker){
            try{
                if(docker)referenciasDocker();
                for(Path raiz:raices){
                    if(parar())break;
                    try{
                    if(!Files.exists(raiz,LinkOption.NOFOLLOW_LINKS))continue;
                    raiz=raiz.toAbsolutePath().normalize();revisadas.add(raiz.toString());
                    if(enlace(raiz)||excluida(raiz)){omitir("Enlaces o ubicaciones excluidas");continue;}
                    final Path inicio=raiz;final FileStore disco=Files.getFileStore(inicio);
                    if(red(disco)){omitir("Ubicaciones de red");continue;}
                    Files.walkFileTree(inicio,EnumSet.noneOf(FileVisitOption.class),limites.profundidad(),new SimpleFileVisitor<Path>(){
                        @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs)throws IOException{
                            if(parar())return FileVisitResult.TERMINATE;entradas++;publicar("EJECUTANDO");
                            if(enlace(dir)||excluida(dir)){omitir("Enlaces o ubicaciones excluidas");return FileVisitResult.SKIP_SUBTREE;}
                            if(!dir.equals(inicio)&&(montajes.contains(dir)||!Files.getFileStore(dir).equals(disco))){omitir("Otros montajes");return FileVisitResult.SKIP_SUBTREE;}
                            if(!vistas.add(dir.toRealPath()))return FileVisitResult.SKIP_SUBTREE;
                            return identificar(dir,false,null)?FileVisitResult.SKIP_SUBTREE:FileVisitResult.CONTINUE;
                        }
                        @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attrs)throws IOException{
                            if(parar())return FileVisitResult.TERMINATE;entradas++;
                            if(attrs.isDirectory()){
                                if(montajes.contains(file)||!Files.getFileStore(file).equals(disco))omitir("Otros montajes");
                                else{identificar(file,false,null);omitir("Límite de profundidad");}
                            }else if(attrs.isSymbolicLink()||attrs.isOther())omitir("Enlaces o ubicaciones excluidas");
                            publicar("EJECUTANDO");return FileVisitResult.CONTINUE;
                        }
                        @Override public FileVisitResult visitFileFailed(Path file,IOException error){omitir("Carpetas o archivos sin acceso");return parar()?FileVisitResult.TERMINATE:FileVisitResult.CONTINUE;}
                        @Override public FileVisitResult postVisitDirectory(Path dir,IOException error){if(error!=null)omitir("Carpetas o archivos sin acceso");return parar()?FileVisitResult.TERMINATE:FileVisitResult.CONTINUE;}
                    });
                    }catch(IOException|SecurityException e){omitir("Ubicaciones que no pudieron completarse");}
                }
            }catch(Exception e){omitir("Ubicaciones que no pudieron completarse");}
            if(limite)omitir("Límite de tiempo, entradas o resultados");
            Map<String,Integer> ids=new HashMap<>();for(var r:resultados){String id=(String)r.get("id");if(id!=null&&!id.isEmpty())ids.merge(id,1,Integer::sum);}
            for(var r:resultados)if(ids.getOrDefault(r.get("id"),0)>1)r.put("detalle",r.get("detalle")+" Hay otra carpeta con la misma identidad; puede ser una copia.");
            resultados.sort(Comparator.comparing(r->r.get("directorio").toString()));
            publicar(cancelado?"CANCELADO":"COMPLETADO");
        }
        boolean identificar(Path dir,boolean antiguo,String proyectoDocker){
            if(parar())return false;
            Path real=dir.toAbsolutePath().normalize();
            try{
                if(enlace(dir)||excluida(dir))return false;
                real=dir.toRealPath();if(halladas.contains(real))return true;
                Path ficha=dir.resolve("instalacion.properties");
                if(Files.exists(ficha,LinkOption.NOFOLLOW_LINKS)){
                    Properties p=leerFicha(ficha);String id=p.getProperty("id",""),proyecto=p.getProperty("proyecto",""),version=p.getProperty("version",""),modo=p.getProperty("modo","");
                    boolean valida=id.matches("[a-fA-F0-9]{8}-(?:[a-fA-F0-9]{4}-){3}[a-fA-F0-9]{12}")&&proyecto.equals("lamontana-"+id.replace("-","").substring(0,12))&&version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")&&Set.of("DESARROLLO","STANDARD").contains(modo);
                    if(proyectoDocker!=null&&!proyectoDocker.equals(proyecto)){omitir("Referencias Docker sin correspondencia confirmada");return false;}
                    String tipo=valida?(Files.isRegularFile(dir.resolve("compose.yaml"),LinkOption.NOFOLLOW_LINKS)?"IDENTIFICADA":"INCOMPLETA"):"INCONSISTENTE";
                    var r=resultado(dir.toString(),tipo,valida,"Estado guardado: "+breve(p.getProperty("estado","PENDIENTE"))+". La salud se comprueba al examinar la carpeta.");
                    r.put("id",valida?id:"");r.put("version",valida?version:"");r.put("modo",valida?modo:"");resultados.add(r);halladas.add(real);return true;
                }
                if(antiguo||Files.isRegularFile(dir.resolve("compose.yaml"),LinkOption.NOFOLLOW_LINKS)&&Files.isRegularFile(dir.resolve("infra/iniciar-local.sh"),LinkOption.NOFOLLOW_LINKS)){
                    resultados.add(resultado(dir.toString(),"SCRIPT_ANTERIOR",false,antiguo?"Referencia del script anterior registrada por Docker. No se adopta automáticamente.":"Carpeta del script anterior; su ejecución no está confirmada. No se adopta automáticamente."));halladas.add(real);return true;
                }
            }catch(Exception e){if(resultados.size()<limites.resultados()&&halladas.add(real)){var r=resultado(dir.toString(),"INCONSISTENTE",false,"No se pudieron leer metadatos válidos y acotados de esta carpeta.");resultados.add(r);}omitir("Metadatos no legibles");return true;}
            return false;
        }
        void referenciasDocker(){
            try{
                if(equipo.windows){var w=consulta(List.of("wsl.exe","--list","--running","--quiet"));if(!w.ok()||w.salida().replace("\0","").lines().noneMatch(l->l.strip().equals(Equipo.DISTRO))){omitir("Docker no consultado: distribución detenida");return;}}
                var r=consulta(equipo.docker("ps","-a","--filter","label=com.docker.compose.project","--format","{{.Label \"com.docker.compose.project\"}}\t{{.Label \"com.docker.compose.project.working_dir\"}}"));
                if(!r.ok()){omitir("Docker no disponible para consultar referencias");return;}
                Set<String> referencias=new HashSet<>();
                for(String line:r.salida().lines().toList()){
                    if(parar())break;String[] partes=line.split("\t",2);if(partes.length!=2||!partes[0].matches("lamontana(?:-[a-f0-9]{12})?")||!referencias.add(line))continue;
                    entradas++;String texto=partes[1];Path dir=null;
                    try{
                        if(equipo.windows&&texto.matches("/mnt/[a-z]/.*"))texto=Character.toUpperCase(texto.charAt(5))+":\\"+texto.substring(7).replace('/','\\');
                        dir=ruta(texto);
                    }catch(IllegalArgumentException ignored){}
                    if(dir!=null&&Files.isDirectory(dir)&&!enlace(dir)){if(identificar(dir,partes[0].equals("lamontana"),partes[0].equals("lamontana")?null:partes[0]))continue;}
                    resultados.add(resultado(breve(partes[1]),"REFERENCIA",false,"Docker registra esta ruta, pero no se confirmó una carpeta correspondiente en este sistema."));
                }
            }catch(Exception e){if(!cancelado)omitir("No se pudieron consultar referencias Docker");}
        }
        Util.Resultado consulta(List<String> cmd)throws Exception{
            long restante=Math.max(1,TimeUnit.NANOSECONDS.toMillis(vence-System.nanoTime()));
            return Util.ejecutar(cmd,null,Duration.ofMillis(Math.min(3000,restante)),null);
        }
    }
    static Map<String,Object> resultado(String dir,String tipo,boolean elegible,String detalle){var r=new LinkedHashMap<String,Object>();r.put("directorio",dir);r.put("tipo",tipo);r.put("elegible",elegible);r.put("detalle",detalle);return r;}
    static String breve(String s){return s.substring(0,Math.min(s.length(),512)).replaceAll("[\\p{Cntrl}]"," ");}
    static Properties leerFicha(Path file)throws IOException{
        if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IOException("Metadatos no regulares.");
        try(var c=Files.newByteChannel(file,Set.of(StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS))){
            ByteBuffer b=ByteBuffer.allocate(65537);while(b.hasRemaining()&&c.read(b)>=0){}
            if(b.position()>65536)throw new IOException("Metadatos demasiado grandes.");
            var p=new Properties();p.load(new StringReader(StandardCharsets.UTF_8.decode((ByteBuffer)b.flip()).toString()));return p;
        }
    }
    static boolean enlace(Path p)throws IOException{
        var a=Files.readAttributes(p,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
        return a.isSymbolicLink()||a.isOther()||!p.toRealPath(LinkOption.NOFOLLOW_LINKS).equals(p.toRealPath());
    }
    static boolean excluida(Path p){
        String name=p.getFileName()==null?"":p.getFileName().toString().toLowerCase(Locale.ROOT);
        return Set.of(".git",".svn",".hg","node_modules",".cache","cache","caches","__pycache__").contains(name)||
            List.of("/proc","/sys","/dev","/run","/var/lib/docker","/var/lib/containerd").stream().anyMatch(s->p.startsWith(Path.of(s)))||
            p.toString().replace('\\','/').matches("(?i).*/(?:\\.docker/desktop|appdata/local/docker)(?:/.*)?");
    }
    static boolean red(FileStore disco){return disco.type().matches("(?i).*(nfs|cifs|smb|sshfs).*");}
    static Set<Path> montajes(){
        var r=new HashSet<Path>();if(!System.getProperty("os.name").startsWith("Windows"))try{
            for(String line:Files.readAllLines(Path.of("/proc/self/mountinfo"))){String[] p=line.split(" ");if(p.length>4)r.add(Path.of(p[4].replace("\\040"," ").replace("\\011","\t").replace("\\134","\\")));}
        }catch(IOException ignored){}return r;
    }
}
