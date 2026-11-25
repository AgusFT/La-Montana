package ar.com.lamontana.instalador;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

final class Util {
    private Util() {}
    static String secreto() { byte[] b=new byte[32];new SecureRandom().nextBytes(b);return HexFormat.of().formatHex(b); }
    static String sha256(Path file) throws IOException {
        try(var in=Files.newInputStream(file)) { var d=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);return HexFormat.of().formatHex(d.digest()); }
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    static Map<String,String> form(String body) {
        var values=new LinkedHashMap<String,String>();
        for(String item:body.split("&")){if(item.isEmpty())continue;String[] pair=item.split("=",2);String k=URLDecoder.decode(pair[0],StandardCharsets.UTF_8);String v=URLDecoder.decode(pair.length==2?pair[1]:"",StandardCharsets.UTF_8);if(values.put(k,v)!=null)throw new IllegalArgumentException("Campo repetido.");}
        return values;
    }
    static String json(Object value) {
        if(value==null)return "null";
        if(value instanceof Boolean||value instanceof Number)return value.toString();
        if(value instanceof Map<?,?> m){var parts=new ArrayList<String>();m.forEach((k,v)->parts.add(json(k.toString())+":"+json(v)));return "{"+String.join(",",parts)+"}";}
        if(value instanceof Collection<?> c)return "["+String.join(",",c.stream().map(Util::json).toList())+"]";
        var b=new StringBuilder("\"");for(char c:value.toString().toCharArray())switch(c){case '"'->b.append("\\\"");case '\\'->b.append("\\\\");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();
    }
    static String requerido(Map<String,String> v,String key){String s=v.getOrDefault(key,"").strip();if(s.isEmpty()||s.length()>4096||s.indexOf('\0')>=0)throw new IllegalArgumentException("Completá el campo "+key+".");return s;}
    static void privado(Path path,boolean directory) throws IOException {if(Files.getFileStore(path).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(path,PosixFilePermissions.fromString(directory?"rwx------":"rw-------"));
        else {var view=Files.getFileAttributeView(path,java.nio.file.attribute.AclFileAttributeView.class);if(view==null)throw new IOException("El destino no permite restringir el acceso a credenciales.");var usuario=usuarioWindows();view.setOwner(usuario);var rule=java.nio.file.attribute.AclEntry.newBuilder().setType(java.nio.file.attribute.AclEntryType.ALLOW).setPrincipal(usuario).setPermissions(java.util.EnumSet.allOf(java.nio.file.attribute.AclEntryPermission.class));if(directory)rule.setFlags(java.nio.file.attribute.AclEntryFlag.DIRECTORY_INHERIT,java.nio.file.attribute.AclEntryFlag.FILE_INHERIT);view.setAcl(java.util.List.of(rule.build()));}}
    static java.nio.file.attribute.UserPrincipal usuarioWindows() throws IOException {
        // Una ejecución elevada puede crear archivos cuyo dueño inicial es Administradores.
        // Los secretos deben seguir perteneciendo a la cuenta que podrá reabrir el asistente sin elevarlo.
        String dominio=System.getenv("USERDOMAIN");
        String cuenta=(dominio==null||dominio.isBlank()?"":dominio+"\\")+System.getProperty("user.name");
        return FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(cuenta);
    }
    static Properties properties(Path path) throws IOException {var p=new Properties();if(Files.isRegularFile(path))try(var in=Files.newBufferedReader(path)){p.load(in);}return p;}
    static void copiarManifiesto(Path origen,Path destino) throws IOException {
        boolean windows=System.getProperty("os.name").startsWith("Windows");
        // Un paquete copiado de CD/ISO puede conservar ReadOnly; el manifiesto instalado es reemplazable.
        if(windows&&Files.exists(destino))Files.setAttribute(destino,"dos:readonly",false);
        Files.copy(origen,destino,StandardCopyOption.REPLACE_EXISTING);
        if(windows)Files.setAttribute(destino,"dos:readonly",false);
    }
    static void guardar(Path path,Properties p) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());Path tmp=Files.createTempFile(path.toAbsolutePath().getParent(),".escritura-",".tmp");privado(tmp,false);
        try {try(var out=Files.newBufferedWriter(tmp)){p.store(out,"La Montana");}try(var channel=java.nio.channels.FileChannel.open(tmp,StandardOpenOption.WRITE)){channel.force(true);}Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(tmp);}
    }
    // wsl.exe escribe diagnósticos UTF-16LE al redirigir; los procesos Linux usan UTF-8.
    static String decodificar(byte[] bytes){
        int ceros=0;for(int i=1;i<bytes.length;i+=2)if(bytes[i]==0)ceros++;
        boolean utf16=bytes.length>=2&&(bytes[0]==(byte)0xff&&bytes[1]==(byte)0xfe||ceros>bytes.length/8);
        return new String(bytes,utf16?StandardCharsets.UTF_16LE:StandardCharsets.UTF_8).replace("\ufeff","");
    }
    record Resultado(int codigo,String salida) {boolean ok(){return codigo==0;}}
    // No pasa por shell: cada argumento conserva sus límites. La salida tiene un límite independiente del proceso.
    static Resultado ejecutar(List<String> cmd,Path cwd,Duration timeout,byte[] entrada) throws IOException,InterruptedException {
        var builder=new ProcessBuilder(cmd).redirectErrorStream(true);
        if(cmd.contains("compose")){builder.environment().keySet().removeIf(k->k.startsWith("COMPOSE_")||k.startsWith("IMAGE_")||k.startsWith("SMTP_")||Set.of("DB_PASSWORD","SETUP_TOKEN","INSTALLATION_ID","LAMONTANA_MODO","SESSION_COOKIE_SECURE","APP_BIND","APP_PORT","CONTROL_PORT","MAILPIT_PORT","WEB_SOURCE_DIR").contains(k));}
        if(cwd!=null)builder.directory(cwd.toFile());var process=builder.start();var out=new ByteArrayOutputStream();
        Thread reader=new Thread(()->{try(var in=process.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)synchronized(out){int available=1024*1024-out.size();if(available>0)out.write(b,0,Math.min(n,available));}}catch(IOException ignored){}},"salida-proceso");reader.setDaemon(true);reader.start();
        try {try(var in=process.getOutputStream()){if(entrada!=null)in.write(entrada);}if(!process.waitFor(timeout.toMillis(),TimeUnit.MILLISECONDS)){process.descendants().forEach(ProcessHandle::destroyForcibly);process.destroyForcibly();throw new IOException("El proceso superó el tiempo permitido: "+Path.of(cmd.get(0)).getFileName());}reader.join(3000);synchronized(out){return new Resultado(process.exitValue(),decodificar(out.toByteArray()));}}
        finally{if(process.isAlive())process.destroyForcibly();}
    }
    static Resultado ejecutar(List<String> cmd) throws IOException,InterruptedException{return ejecutar(cmd,null,Duration.ofSeconds(30),null);}
}
