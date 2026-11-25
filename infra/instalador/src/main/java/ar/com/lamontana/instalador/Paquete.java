package ar.com.lamontana.instalador;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Manifiesto local del release. No ejecuta comandos declarados en archivos descargados. */
final class Paquete {
    final Path raiz;
    final Properties manifiesto;
    Paquete(Path raiz) throws IOException {this.raiz=raiz.toAbsolutePath().normalize();this.manifiesto=Util.properties(this.raiz.resolve("release.properties"));}
    String version(){return manifiesto.getProperty("app.version","sin-paquete");}
    Path archivo(String relative) throws IOException {Path p=raiz.resolve(relative).normalize();if(!p.startsWith(raiz)||!Files.isRegularFile(p)||!p.toRealPath().startsWith(raiz.toRealPath()))throw new IOException("Recurso del paquete ausente o fuera de su carpeta: "+relative);return p;}
    void verificar() throws IOException {
        versionValida(version());String[] files=manifiesto.getProperty("files","").split(",");if(files.length==0||files[0].isBlank())throw new IOException("El instalador no contiene un paquete offline completo.");
        for(String file:files){String expected=manifiesto.getProperty("sha256."+file,"");if(!expected.matches("[a-f0-9]{64}")||!expected.equals(Util.sha256(archivo(file))))throw new IOException("Recurso ausente o alterado: "+file);}
    }
    static void versionValida(String s){if(!s.matches("[0-9]+\\.[0-9]+\\.[0-9]+"))throw new IllegalArgumentException("La versión debe tener formato mayor.menor.parche.");for(String part:s.split("\\."))Integer.parseInt(part);}
    static int comparar(String a,String b){versionValida(a);versionValida(b);var aa=a.split("\\.");var bb=b.split("\\.");for(int i=0;i<3;i++){int c=Integer.compare(Integer.parseInt(aa[i]),Integer.parseInt(bb[i]));if(c!=0)return c;}return 0;}
    static void validarOperacion(String op,String instalada,String nueva){int c=comparar(nueva,instalada);if(c<0)throw new IllegalArgumentException("Este paquete es anterior a la app instalada. No se permite rebajar la versión.");if(op.equals("reparar")&&c!=0)throw new IllegalArgumentException("Reparar requiere un instalador de la misma versión.");if(op.equals("actualizar")&&c<=0)throw new IllegalArgumentException("Actualizar requiere una versión posterior; para la misma versión elegí Reparar.");}
}
