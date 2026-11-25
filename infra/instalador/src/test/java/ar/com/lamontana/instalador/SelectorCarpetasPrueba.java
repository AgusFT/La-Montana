package ar.com.lamontana.instalador;
import java.nio.file.Path;
/** Ensayo gráfico opcional: elegir/cancelar en el diálogo real y leer su resultado. */
public final class SelectorCarpetasPrueba {
    public static void main(String[] args)throws Exception{
        try(var c=new Carpetas(new Equipo(new Paquete(Path.of(args[0]))))){
            Runtime.getRuntime().addShutdownHook(new Thread(c::close));
            c.examinar(args[0]);
            for(int n=0;n<1800&&c.ocupado();n++)Thread.sleep(200);
            if(c.ocupado())throw new AssertionError("El diálogo no terminó");
            System.out.println(Util.json(c.estado()));
            if(c.estado().get("estado").equals("ERROR"))throw new AssertionError(c.estado());
        }
    }
}
