package ar.com.lamontana.archivos;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ArchivosPrivados {
    public static final long MAX_BYTES=10L*1024*1024;
    private final Path root;
    public ArchivosPrivados(Path directorioArchivos)throws IOException{
        root=directorioArchivos.resolve("pdf");
    }
    public record Recibido(long bytes,String sha256,Path path){}
    public Path carpeta(UUID id)throws IOException{
        if(!Files.exists(root,LinkOption.NOFOLLOW_LINKS))Files.createDirectories(root,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        verificarDirectorio(root);Path dir=root.resolve(id.toString());
        if(!Files.exists(dir,LinkOption.NOFOLLOW_LINKS))Files.createDirectory(dir,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        verificarDirectorio(dir);return dir;
    }
    public Recibido recibir(UUID id,InputStream input)throws IOException{
        Path file=carpeta(id).resolve("original.part");MessageDigest digest=sha();long length=0;
        try(var out=Files.newOutputStream(file,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
            byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){length+=n;if(length>MAX_BYTES)throw new InspectorPdf.Rechazado("ARCHIVO_DEMASIADO_GRANDE","El PDF supera el máximo de 10 MiB.");out.write(buffer,0,n);digest.update(buffer,0,n);}
        }
        byte[] prefix;try(var in=Files.newInputStream(file)){prefix=in.readNBytes(5);}
        if(length==0||!Arrays.equals(prefix,new byte[]{37,80,68,70,45}))throw new InspectorPdf.Rechazado("TIPO_INVALIDO","El contenido recibido no comienza con una cabecera PDF válida.");
        return new Recibido(length,HexFormat.of().formatHex(digest.digest()),file);
    }
    public void publicar(UUID id)throws IOException{Files.move(carpeta(id).resolve("original.part"),carpeta(id).resolve("original.pdf"),StandardCopyOption.ATOMIC_MOVE);}
    public Path original(UUID id,String hash,long bytes)throws IOException{
        Path file=carpeta(id).resolve("original.pdf");
        if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)!=bytes)throw new IOException("Archivo no disponible");
        MessageDigest digest=sha();try(var in=Files.newInputStream(file)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}
        if(!hash.equals(HexFormat.of().formatHex(digest.digest())))throw new IOException("Integridad de archivo inválida");return file;
    }
    public void descartar(UUID id)throws IOException{
        Path dir=root.resolve(id.toString());if(!Files.exists(dir,LinkOption.NOFOLLOW_LINKS))return;verificarDirectorio(dir);
        try(var files=Files.list(dir)){for(Path f:files.toList())if(Files.isRegularFile(f,LinkOption.NOFOLLOW_LINKS))Files.delete(f);}
        Files.delete(dir);
    }
    private static void verificarDirectorio(Path p)throws IOException{if(!Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS))throw new IOException("Directorio privado inválido");}
    private static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
