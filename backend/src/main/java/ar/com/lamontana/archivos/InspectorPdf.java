package ar.com.lamontana.archivos;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InspectorPdf {
    private final String jar;
    private final Semaphore workers=new Semaphore(2);
    public InspectorPdf(@Value("${lamontana.archivos.inspector-jar:}")String jar){this.jar=jar;}
    public int inspeccionar(Path input,Path result)throws IOException{
        ejecutar("inspect",input,result,0,Duration.ofSeconds(45));
        return Integer.parseInt(Files.readString(result));
    }
    public void preview(Path input,Path output,int page)throws IOException{ejecutar("preview",input,output,page,Duration.ofSeconds(20));}
    private void ejecutar(String operation,Path input,Path output,int page,Duration limit)throws IOException{
        if(!workers.tryAcquire())throw new NoDisponible("El análisis de archivos está ocupado. Volvé a intentar.");
        Process process=null;
        try {
            var command=new ArrayList<String>();command.add(Path.of(System.getProperty("java.home"),"bin","java").toString());
            command.addAll(List.of("-Xmx256m","-XX:MaxMetaspaceSize=128m","-Djava.awt.headless=true","-Djava.io.tmpdir="+output.getParent(),"-Duser.home="+output.getParent()));
            if(jar.isBlank())command.addAll(List.of("-cp",System.getProperty("java.class.path"),PdfInspectorMain.class.getName()));
            else command.addAll(List.of("-Dloader.main="+PdfInspectorMain.class.getName(),"-cp",Path.of(jar).toAbsolutePath().toString(),"org.springframework.boot.loader.launch.PropertiesLauncher"));
            command.addAll(List.of(operation,input.toAbsolutePath().toString(),output.toAbsolutePath().toString(),Integer.toString(page)));
            var builder=new ProcessBuilder(command).directory(output.getParent().toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
            builder.environment().clear();builder.environment().put("LANG","C.UTF-8");
            process=builder.start();
            if(!process.waitFor(limit.toMillis(),TimeUnit.MILLISECONDS))throw new Rechazado("LIMITE_ANALISIS","El PDF excede el tiempo de análisis permitido. Simplificá el documento e intentá nuevamente.");
            int code=process.exitValue();
            if(code!=0)throw switch(code){
                case 23->new Rechazado("CONTENIDO_ACTIVO","El PDF contiene acciones o archivos incrustados no admitidos. Exportá una versión estática.");
                case 24->new Rechazado("CIFRADO","El PDF está cifrado o protegido por contraseña. Exportá una copia sin protección.");
                case 25->new Rechazado("LIMITES_PDF","El PDF excede los límites de páginas, dimensiones o complejidad admitidos.");
                default->new Rechazado("PDF_INVALIDO","No se pudieron interpretar y renderizar todas las páginas del PDF.");};
            if(!Files.isRegularFile(output)||Files.size(output)>8L*1024*1024)throw new IOException("Resultado de inspección inválido");
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new NoDisponible("El análisis se interrumpió. Volvé a intentar.");}
        finally {if(process!=null&&process.isAlive()){process.destroyForcibly();try{process.waitFor(1,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}workers.release();}
    }
    public static class Rechazado extends IOException {public final String codigo;public Rechazado(String codigo,String message){super(message);this.codigo=codigo;}}
    public static class NoDisponible extends IOException {public NoDisponible(String message){super(message);}}
}
