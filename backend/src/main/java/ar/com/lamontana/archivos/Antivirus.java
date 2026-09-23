//#region ENCABEZADO · Antivirus.java
/*
 * ========================================================================
 * ARCHIVO: Antivirus.java
 * ========================================================================
 * FUNCIÓN
 * Envía archivos al servicio ClamAV por el protocolo INSTREAM, verifica su respuesta y distingue
 * detecciones de malware de fallos o análisis incompletos.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] Antivirus(String host, int port)
 * - [paquete] Deteccion :: Deteccion(String version)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] String analizar(Path file) throws IOException
 * - [private] String version() throws IOException
 * - [private] String reply(InputStream in) throws IOException
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Antivirus (clase).
 * - Antivirus.Deteccion (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.archivos;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Antivirus {
    private final String host;private final int port;
    private static final ScheduledExecutorService DEADLINES=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"clamav-timeouts");t.setDaemon(true);return t;});
    public Antivirus(@Value("${lamontana.archivos.antivirus.host:127.0.0.1}")String host,@Value("${lamontana.archivos.antivirus.port:3310}")int port){this.host=host;this.port=port;}
    public String analizar(Path file)throws IOException{
        String version=version();
        try(Socket socket=new Socket()){
            socket.connect(new InetSocketAddress(host,port),2000);socket.setSoTimeout(15000);
            var deadline=DEADLINES.schedule(()->{try{socket.close();}catch(IOException ignored){}},20,TimeUnit.SECONDS);
            try(var out=new DataOutputStream(socket.getOutputStream());var in=Files.newInputStream(file)){
                out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));byte[] b=new byte[8192];int n;
                while((n=in.read(b))!=-1){out.writeInt(n);out.write(b,0,n);}out.writeInt(0);out.flush();
                String result=reply(socket.getInputStream());
                if(result.endsWith(" FOUND"))throw new Deteccion(version);
                if(!result.equals("stream: OK"))throw new IOException("Resultado antivirus incompleto");
                return version;
            }finally{deadline.cancel(false);}
        }catch(InspectorPdf.Rechazado e){throw e;}catch(IOException e){throw new InspectorPdf.NoDisponible("El análisis de seguridad no está disponible. El archivo no fue habilitado; volvé a intentar más tarde.");}
    }
    private String version()throws IOException{
        try(Socket s=new Socket()) {s.connect(new InetSocketAddress(host,port),2000);s.setSoTimeout(2000);s.getOutputStream().write("zVERSION\0".getBytes(StandardCharsets.US_ASCII));String v=reply(s.getInputStream());if(!v.startsWith("ClamAV ")||v.length()>180)throw new IOException();return v;}
        catch(IOException e){throw new InspectorPdf.NoDisponible("El análisis de seguridad no está disponible. El archivo no fue habilitado; volvé a intentar más tarde.");}
    }
    private String reply(InputStream in)throws IOException{var b=new ByteArrayOutputStream();for(int n;(n=in.read())!=-1;){if(n==0)return b.toString(StandardCharsets.UTF_8);if(b.size()>=1024)throw new IOException();b.write(n);}throw new EOFException();}
    public static class Deteccion extends InspectorPdf.Rechazado {
        public final String version;
        Deteccion(String version){super("SEGURIDAD_RECHAZADA","El análisis de seguridad rechazó el archivo. No se habilitó su contenido.");this.version=version;}
    }
}
