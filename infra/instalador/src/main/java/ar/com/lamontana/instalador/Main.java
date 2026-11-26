package ar.com.lamontana.instalador;

import java.net.URI;
import java.nio.file.*;
import java.util.*;
import javax.swing.JOptionPane;

public final class Main {
    public static void main(String[] args) throws Exception {
        Path location=Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
        Path bundle=location.getParent().resolve("paquete");boolean abrir=true, mantenimiento=false;
        for(int i=0;i<args.length;i++){
            if(args[i].equals("--paquete")&&i+1<args.length)bundle=Path.of(args[++i]);
            else if(args[i].equals("--sin-navegador"))abrir=false;
            else if(args[i].equals("--mantenimiento"))mantenimiento=true;
            else throw new IllegalArgumentException("Opción desconocida o incompleta.");
        }
        try {
            var paquete=new Paquete(bundle);
            if(abrir&&navegador()==null){
                // Único diálogo de arranque: permite instalar el navegador antes de tener una interfaz web.
                Object[] opciones={"Instalar Firefox incluido","Descargar Firefox oficial","Ya lo instalé; verificar","Cancelar"};
                while(navegador()==null){
                    int opcion=JOptionPane.showOptionDialog(null,"El asistente utiliza el navegador de esta PC. Elegí cómo preparar el navegador.","La Montaña Demo",JOptionPane.DEFAULT_OPTION,JOptionPane.QUESTION_MESSAGE,null,opciones,opciones[0]);
                    if(opcion<0||opcion==3)return;
                    if(opcion<2)new Equipo(paquete).dependencia("navegador",opcion==0?"incluida":"internet");
                }
            }
            if(abrir&&!mantenimiento){
                Path ultima=Instalacion.ultima();
                if(ultima!=null){
                    var app=new Instalacion(ultima,new Equipo(paquete));
                    if("LISTA".equals(app.estado.getProperty("estado"))){
                        app.abrir();
                        new ProcessBuilder(navegador().toString(),app.url()).start();
                        return;
                    }
                }
            }
            var server=new Asistente(paquete);Runtime.getRuntime().addShutdownHook(new Thread(server::close));server.start();
            // El fragmento nunca viaja al servidor ni a sus registros HTTP.
            URI uri=URI.create(server.origen()+"/#"+server.token());
            if(abrir)new ProcessBuilder(navegador().toString(),uri.toString()).start();
            else System.out.println(uri);
        }catch(Exception e){
            if(!abrir)throw e;
            JOptionPane.showMessageDialog(null,Objects.toString(e.getMessage(),"No se pudo abrir el asistente."),"La Montaña Demo",JOptionPane.ERROR_MESSAGE);
        }
    }
    static Path navegador(){
        var candidatos=new ArrayList<Path>();
        if(System.getProperty("os.name").startsWith("Windows")){
            for(String key:List.of("ProgramFiles","ProgramFiles(x86)","LOCALAPPDATA")){
                String dir=System.getenv(key);if(dir==null)continue;
                candidatos.add(Path.of(dir,"Mozilla Firefox","firefox.exe"));
                candidatos.add(Path.of(dir,"Google","Chrome","Application","chrome.exe"));
                candidatos.add(Path.of(dir,"Microsoft","Edge","Application","msedge.exe"));
            }
        }else for(String path:List.of("/usr/bin/firefox","/snap/bin/firefox","/usr/bin/google-chrome","/usr/bin/google-chrome-stable","/opt/lamontana-demo-firefox/firefox"))candidatos.add(Path.of(path));
        return candidatos.stream().filter(Files::isExecutable).findFirst().orElse(null);
    }
}
