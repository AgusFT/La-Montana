//#region ENCABEZADO · AlmacenImagenWeb.java
/*
 * ========================================================================
 * ARCHIVO: AlmacenImagenWeb.java
 * ========================================================================
 * FUNCIÓN
 * Lee imágenes desde una carpeta local permitida, valida rutas y contenido, genera copias
 * normalizadas y miniaturas y verifica sus huellas en la biblioteca privada.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] AlmacenImagenWeb(String entrada, String biblioteca, String rutaHost)
 * - [paquete] Carpeta :: Carpeta(SecureDirectoryStream<Path> directorio,
 *   List<DirectoryStream<Path>> abiertos)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] Origen listar(String carpeta)
 * - [public] Carpeta :: void close() throws IOException
 * - [private] Carpeta abrirCarpeta(String relativa) throws IOException
 * - [paquete] byte[] leer(String relativa)
 * - [private, static] String tipo(byte[] b)
 * - [paquete, synchronized] Normalizada normalizar(byte[] bytes)
 * - [paquete, synchronized] byte[] miniatura(byte[] bytes)
 * - [private] Normalizada decodificar(byte[] bytes, boolean soloMiniatura)
 * - [private, static] byte[] png(BufferedImage image) throws IOException
 * - [paquete] Path archivo(UUID id, boolean miniatura)
 * - [paquete] void guardar(UUID id, Normalizada image)
 * - [private, static] void escribir(Path destino, byte[] bytes) throws IOException
 * - [paquete] void borrarNuevo(UUID id)
 * - [paquete] void comprobar(UUID id, String completa, String miniatura)
 * - [private, static] String huellaArchivo(Path path) throws IOException
 * - [paquete, static] String sha256(byte[] bytes)
 *   Calcula o prepara la huella SHA-256 del contenido.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - AlmacenImagenWeb (clase).
 * - AlmacenImagenWeb.ArchivoEntrada (record).
 * - AlmacenImagenWeb.Origen (record).
 * - AlmacenImagenWeb.Normalizada (record).
 * - AlmacenImagenWeb.Carpeta (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.web;

import static ar.com.lamontana.web.PaginaWebService.error;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.channels.Channels;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AlmacenImagenWeb {
    static final int MAX_BYTES=10*1024*1024;
    static final long MAX_PIXELES=25_000_000;
    final Path entrada,biblioteca;
    final String rutaHost;
    public AlmacenImagenWeb(@Value("${lamontana.web.entrada:./datos/imagenes-web/entrada}") String entrada,
            @Value("${lamontana.web.biblioteca:./var/imagenes-web}") String biblioteca,
            @Value("${lamontana.web.ruta-host:}") String rutaHost){
        this.entrada=Path.of(entrada).toAbsolutePath().normalize();this.biblioteca=Path.of(biblioteca).toAbsolutePath().normalize();
        this.rutaHost=rutaHost.isBlank()?this.entrada.toString():rutaHost;
    }
    record ArchivoEntrada(String ruta,String nombre,long bytes,String sha256,String estado,String mensaje){}
    record Origen(String rutaHost,String carpeta,boolean legible,String mensaje,List<String> subcarpetas,List<ArchivoEntrada> archivos,boolean limitada){}
    record Normalizada(String tipo,int ancho,int alto,byte[] completa,byte[] miniatura){}
    Origen listar(String carpeta){
        PaginaWebService.validarCarpeta(carpeta);
        try(var abierta=abrirCarpeta(carpeta)){
            var nombres=new ArrayList<String>();boolean limitada=false;
            for(Path p:abierta.directorio){if(nombres.size()>=100){limitada=true;break;}nombres.add(p.getFileName().toString());}Collections.sort(nombres);
            var dirs=new ArrayList<String>();var archivos=new ArrayList<ArchivoEntrada>();
            for(String nombre:nombres){String relativa=carpeta.isEmpty()?nombre:carpeta+"/"+nombre;
                try{
                    var attrs=abierta.directorio.getFileAttributeView(Path.of(nombre),BasicFileAttributeView.class,LinkOption.NOFOLLOW_LINKS).readAttributes();
                    if(attrs.isDirectory()){dirs.add(relativa);continue;}
                    if(!attrs.isRegularFile()){archivos.add(new ArchivoEntrada(relativa,nombre,0,null,"INVALIDA","No se admiten enlaces simbólicos ni archivos especiales."));continue;}
                    byte[] bytes=leer(relativa);String tipo=tipo(bytes);
                    archivos.add(new ArchivoEntrada(relativa,nombre,bytes.length,sha256(bytes),tipo==null?"INVALIDA":"CANDIDATA",tipo==null?"El contenido no es JPEG, PNG ni WebP.":"La imagen se comprueba y normaliza al importar."));
                }catch(Exception ex){archivos.add(new ArchivoEntrada(relativa,nombre,0,null,"INVALIDA","No se pudo leer o supera el límite de 10 MiB. Revisá el archivo y sus permisos de lectura."));}
            }
            return new Origen(rutaHost,carpeta,true,"Carpeta disponible para lectura. Copiá imágenes aquí y actualizá la lista.",List.copyOf(dirs),List.copyOf(archivos),limitada);
        }catch(IOException ex){return new Origen(rutaHost,carpeta,false,"No pudimos leer esta carpeta. Comprobá que exista dentro de la entrada autorizada y que el servidor pueda leerla.",List.of(),List.of(),false);}
    }
    private static final class Carpeta implements AutoCloseable {
        final SecureDirectoryStream<Path> directorio;final List<DirectoryStream<Path>> abiertos;
        Carpeta(SecureDirectoryStream<Path> directorio,List<DirectoryStream<Path>> abiertos){this.directorio=directorio;this.abiertos=abiertos;}
        public void close()throws IOException{IOException error=null;for(int i=abiertos.size()-1;i>=0;i--)try{abiertos.get(i).close();}catch(IOException e){error=e;}if(error!=null)throw error;}
    }
    private Carpeta abrirCarpeta(String relativa)throws IOException{
        PaginaWebService.validarCarpeta(relativa);
        if(!Files.isDirectory(entrada,LinkOption.NOFOLLOW_LINKS))throw new IOException("Entrada no disponible");
        var abiertos=new ArrayList<DirectoryStream<Path>>();
        try{
            var root=Files.newDirectoryStream(entrada);abiertos.add(root);
            if(!(root instanceof SecureDirectoryStream<Path> actual))throw new IOException("Se requiere lectura segura de directorios");
            for(String parte:relativa.split("/"))if(!parte.isEmpty()){actual=actual.newDirectoryStream(Path.of(parte),LinkOption.NOFOLLOW_LINKS);abiertos.add(actual);}
            return new Carpeta(actual,abiertos);
        }catch(IOException|RuntimeException ex){for(var a:abiertos)try{a.close();}catch(IOException ignored){}throw ex;}
    }
    byte[] leer(String relativa){
        PaginaWebService.validarCarpeta(relativa);
        if(relativa.isBlank()||relativa.endsWith("/"))throw error(HttpStatus.BAD_REQUEST,"Elegí un archivo de imagen dentro de la carpeta autorizada.");
        int split=relativa.lastIndexOf('/');String carpeta=split<0?"":relativa.substring(0,split),nombre=relativa.substring(split+1);
        try(var abierta=abrirCarpeta(carpeta)){
            var path=Path.of(nombre);var attrs=abierta.directorio.getFileAttributeView(path,BasicFileAttributeView.class,LinkOption.NOFOLLOW_LINKS).readAttributes();
            if(!attrs.isRegularFile()||attrs.size()<1||attrs.size()>MAX_BYTES)throw error(HttpStatus.BAD_REQUEST,"La entrada debe ser una imagen regular de hasta 10 MiB; no se admiten enlaces simbólicos.");
            try(var channel=abierta.directorio.newByteChannel(path,Set.of(StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS));var stream=Channels.newInputStream(channel)){
                byte[] bytes=stream.readNBytes(MAX_BYTES+1);if(bytes.length<1||bytes.length>MAX_BYTES)throw error(HttpStatus.BAD_REQUEST,"La imagen supera el límite de 10 MiB.");return bytes;
            }
        }catch(IOException ex){throw error(HttpStatus.BAD_REQUEST,"No pudimos leer la imagen. Actualizá la carpeta y comprobá que el archivo siga disponible y tenga permiso de lectura.");}
    }
    private static String tipo(byte[] b){
        if(b.length>=8&&b[0]==(byte)137&&b[1]==80&&b[2]==78&&b[3]==71&&b[4]==13&&b[5]==10&&b[6]==26&&b[7]==10)return "image/png";
        if(b.length>=3&&b[0]==(byte)255&&b[1]==(byte)216&&b[2]==(byte)255)return "image/jpeg";
        if(b.length>=12&&b[0]==82&&b[1]==73&&b[2]==70&&b[3]==70&&b[8]==87&&b[9]==69&&b[10]==66&&b[11]==80)return "image/webp";
        return null;
    }
    /** One bounded decode at a time; dimensions are checked before allocating the pixel buffer. */
    synchronized Normalizada normalizar(byte[] bytes){return decodificar(bytes,false);}
    synchronized byte[] miniatura(byte[] bytes){return decodificar(bytes,true).miniatura();}
    private Normalizada decodificar(byte[] bytes,boolean soloMiniatura){
        String tipo=tipo(bytes);if(tipo==null)throw error(HttpStatus.BAD_REQUEST,"El contenido no es una imagen JPEG, PNG o WebP válida.");
        try(var stream=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new IOException("Formato no reconocido");var reader=readers.next();
            try{
                reader.setInput(stream,true,true);int ancho=reader.getWidth(0),alto=reader.getHeight(0);
                if(ancho<1||alto<1||(long)ancho*alto>MAX_PIXELES)throw error(HttpStatus.BAD_REQUEST,"La imagen supera el máximo de 25 millones de píxeles.");
                var parametros=reader.getDefaultReadParam();int salto=soloMiniatura?Math.max(1,Math.max(ancho,alto)/480):1;parametros.setSourceSubsampling(salto,salto,0,0);
                BufferedImage original=reader.read(0,parametros);if(original==null||original.getWidth()!=(ancho+salto-1)/salto||original.getHeight()!=(alto+salto-1)/salto)throw new IOException("Dimensiones inconsistentes");
                int anchoDecodificado=original.getWidth(),altoDecodificado=original.getHeight();
                var normal=new BufferedImage(anchoDecodificado,altoDecodificado,BufferedImage.TYPE_INT_ARGB);var g=normal.createGraphics();try{g.drawImage(original,0,0,null);}finally{g.dispose();original.flush();}
                double escala=Math.min(1,480.0/Math.max(anchoDecodificado,altoDecodificado));var thumb=new BufferedImage(Math.max(1,(int)(anchoDecodificado*escala)),Math.max(1,(int)(altoDecodificado*escala)),BufferedImage.TYPE_INT_ARGB);
                var t=thumb.createGraphics();try{t.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);t.drawImage(normal,0,0,thumb.getWidth(),thumb.getHeight(),null);}finally{t.dispose();}
                try{return new Normalizada(tipo,ancho,alto,soloMiniatura?new byte[0]:png(normal),png(thumb));}finally{normal.flush();thumb.flush();}
            }finally{reader.dispose();}
        }catch(IOException|IllegalArgumentException ex){throw error(HttpStatus.BAD_REQUEST,"La imagen está dañada o no se pudo decodificar. Exportala nuevamente como JPEG, PNG o WebP.");}
    }
    private static byte[] png(BufferedImage image)throws IOException{var out=new ByteArrayOutputStream();if(!ImageIO.write(image,"png",out))throw new IOException("No hay codificador PNG");return out.toByteArray();}
    Path archivo(UUID id,boolean miniatura){return biblioteca.resolve(id+(miniatura?"-miniatura.png":".png"));}
    void guardar(UUID id,Normalizada image){try{Files.createDirectories(biblioteca);escribir(archivo(id,false),image.completa());escribir(archivo(id,true),image.miniatura());}catch(IOException ex){borrarNuevo(id);throw error(HttpStatus.SERVICE_UNAVAILABLE,"No pudimos guardar las copias importadas. Revisá el almacenamiento de imágenes y volvé a intentar.");}}
    private static void escribir(Path destino,byte[] bytes)throws IOException{Path temporal=Files.createTempFile(destino.getParent(),".importar-",".tmp");try{Files.write(temporal,bytes);Files.move(temporal,destino,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(temporal);}}
    void borrarNuevo(UUID id){try{Files.deleteIfExists(archivo(id,false));Files.deleteIfExists(archivo(id,true));}catch(IOException ignored){/* No referenced asset is removed; a failed cleanup leaves an inaccessible orphan. */}}
    void comprobar(UUID id,String completa,String miniatura){try{if(!huellaArchivo(archivo(id,false)).equals(completa)||!huellaArchivo(archivo(id,true)).equals(miniatura))throw new IOException("Huella distinta");}catch(IOException ex){throw error(HttpStatus.CONFLICT,"Una copia de imagen falta o está dañada. Recuperá su almacenamiento antes de publicar; la publicación anterior se conserva.");}}
    private static String huellaArchivo(Path path)throws IOException{
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.size(path)>110_000_000)throw new IOException("Copia inválida");
        try(var stream=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)){var digest=MessageDigest.getInstance("SHA-256");byte[] chunk=new byte[65536];long total=0;int n;while((n=stream.read(chunk))>=0){total+=n;if(total>110_000_000)throw new IOException("Copia demasiado grande");digest.update(chunk,0,n);}return HexFormat.of().formatHex(digest.digest());}catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
    static String sha256(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
}
