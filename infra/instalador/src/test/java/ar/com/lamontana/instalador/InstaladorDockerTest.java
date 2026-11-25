package ar.com.lamontana.instalador;

import java.nio.file.*;
import java.util.*;

/** Prueba optativa contra Compose real; requiere la imagen incluida, nunca descarga. */
public class InstaladorDockerTest {
    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("lamontana-env-test-");
        var equipo=new Equipo(new Paquete(dir));
        String volume="lamontana-prueba-"+UUID.randomUUID();
        try {
            Files.writeString(dir.resolve("compose.yaml"),"services:\n  comprobacion:\n    image: postgres:18.6-alpine\n    network_mode: none\n    environment:\n      PRUEBA: ${PRUEBA}\n");
            for(String value:List.of("barra\\texto'quote$HOME espacio","barra final\\","  ñ # ${NO_INTERPOLAR} \\\"'  ")){
                Files.writeString(dir.resolve(".env"),"PRUEBA="+Instalacion.dotenv(value)+"\n");
                var result=Util.ejecutar(equipo.docker("compose","--progress","quiet","--project-name","lamontana-prueba-env","--project-directory",equipo.rutaLinux(dir),"run","--rm","--no-deps","--pull","never","--entrypoint","printenv","comprobacion","PRUEBA"));
                if(!result.ok()||!result.salida().equals(value+"\n"))throw new AssertionError("Compose alteró una credencial sintética.");
            }
            var creado=equipo.run(equipo.docker("volume","create","--label","com.docker.compose.project=lamontana-prueba","--label","com.docker.compose.volume=postgres_data",volume));
            if(!creado.ok())throw new AssertionError("No se pudo crear el volumen vacío de prueba.");
            var etiqueta=equipo.run(equipo.docker("volume","inspect",volume,"--format","{{index .Labels `com.docker.compose.volume`}}|{{index .Labels `com.docker.compose.project`}}"));
            if(!etiqueta.ok()||!etiqueta.salida().strip().equals("postgres_data|lamontana-prueba"))throw new AssertionError("Se alteraron las comillas de las etiquetas Docker.");
            System.out.println("OK: Compose conserva credenciales especiales y Docker interpreta las etiquetas en Linux/WSL.");
        }finally{equipo.run(equipo.docker("volume","rm",volume));try(var files=Files.walk(dir)){for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
    }
}
