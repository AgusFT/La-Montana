//#region ENCABEZADO · ClamavDePrueba.java
/*
 * ========================================================================
 * ARCHIVO: ClamavDePrueba.java
 * ========================================================================
 * FUNCIÓN
 * Implementa un servidor ClamAV de prueba para las verificaciones de carga de archivos. Permite
 * ejecutar los tests sin depender de un antivirus externo real.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [paquete] ClamavDePrueba() throws IOException
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] int port()
 * - [public] void close() throws IOException
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ClamavDePrueba (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Protocol fixture only. Real ClamAV and official signatures are exercised in the local browser smoke. */
final class ClamavDePrueba implements AutoCloseable {
    private final ServerSocket server=new ServerSocket(0,16,InetAddress.getLoopbackAddress());
    private final ExecutorService thread=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"clamav-protocol-test");t.setDaemon(true);return t;});
    volatile String response="stream: OK";
    volatile byte[] ultimo;
    ClamavDePrueba()throws IOException{thread.submit(()->{while(!server.isClosed())try(Socket s=server.accept()){
        s.setSoTimeout(5000);var in=new DataInputStream(s.getInputStream());StringBuilder command=new StringBuilder();int b;while((b=in.read())>0)command.append((char)b);
        String result;if(command.toString().equals("zVERSION"))result="ClamAV test-protocol/123/test";
        else if(command.toString().equals("zINSTREAM")){var bytes=new ByteArrayOutputStream();for(int n;(n=in.readInt())!=0;){if(n<0||n>8192)throw new IOException();bytes.write(in.readNBytes(n));}ultimo=bytes.toByteArray();result=response;}
        else result="UNKNOWN COMMAND";
        s.getOutputStream().write((result+"\0").getBytes(StandardCharsets.US_ASCII));
    }catch(IOException e){if(!server.isClosed())throw new UncheckedIOException(e);}});}
    int port(){return server.getLocalPort();}
    public void close()throws IOException{server.close();thread.shutdownNow();}
}
