package ar.com.lamontana.instalador;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class FirmasAntivirusTest {
    private static final class EquipoDePrueba extends Equipo {
        int codigo;
        String via;
        List<String> comando;
        EquipoDePrueba(Paquete paquete) { super(paquete); }
        @Override String rutaLinux(Path p) { return p.toString(); }
        @Override Util.Resultado run(List<String> cmd, Duration timeout) {
            comando = cmd;
            via = cmd.get(cmd.size()-1);
            if (codigo == 0) {
                String montaje = cmd.get(cmd.indexOf("--volume") + 1);
                Path tmp = Path.of(montaje.substring(0, montaje.length()-":/intercambio".length()));
                try {
                    Files.copy(tmp.resolve("candidato.cvd"),tmp.resolve("validado.cvd"));
                } catch (IOException e) { throw new AssertionError(e); }
            }
            return new Util.Resultado(codigo, "");
        }
    }
    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("Demo prueba firmas ");
        try {
            Files.writeString(tmp.resolve("release.properties"),"platform=windows-x64\nantivirus.bytecode=externo\n");
            var equipo = new EquipoDePrueba(new Paquete(tmp));
            var app = new Instalacion(tmp,equipo);
            app.estado.setProperty("id","d41d8cd9-8f00-4204-9800-00e998ecf842");
            app.estado.setProperty("proyecto","lamontana-demo-d41d8cd98f00");
            app.estado.setProperty("modo","DEMO");
            app.estado.setProperty("version","0.1.2");
            assert FirmasAntivirus.externas(equipo.paquete.manifiesto);
            assert !FirmasAntivirus.externas(new Properties());
            assert FirmasAntivirus.via(Map.of()).equals("internet");
            for (var error : List.of(Map.of("viaFirmas","otro"),Map.of("viaFirmas","archivo","archivoFirmas","relativa.cvd"),
                    Map.of("viaFirmas","internet","archivoFirmas","inesperado"))) {
                try { FirmasAntivirus.via(error); throw new AssertionError(error); } catch (IllegalArgumentException esperado) {}
            }
            Path usb = tmp.resolve("original del disco USB.cvd");
            Files.writeString(usb,"entrada de la prueba del intercambio; la firma se comprueba en el ensayo Docker");
            FirmasAntivirus.preparar(app,"archivo",usb.toString());
            assert FirmasAntivirus.guardadas(app);
            assert !equipo.comando.contains(usb.toString());
            Files.delete(usb);
            FirmasAntivirus.preparar(app,"internet",null);
            assert equipo.via.equals("archivo");
            Path privado = tmp.resolve("firmas/bytecode.cvd");
            Files.writeString(privado,"alterado");
            assert !FirmasAntivirus.guardadas(app);
            equipo.codigo = 20;
            try { FirmasAntivirus.preparar(app,"internet",null); throw new AssertionError(); }
            catch (IOException e) { assert e.getMessage().contains("FreshClam"); }
            assert Files.readString(privado).equals("alterado");
            try(var lista=Files.list(privado.getParent())) { assert lista.toList().equals(List.of(privado)); }
            System.out.println("OK: origen explícito, ruta USB, copia privada, reintento offline y fallo sin modificar la instalación.");
        } finally {
            try(var archivos=Files.walk(tmp)) {
                for(Path archivo:archivos.sorted(Comparator.reverseOrder()).toList()) Files.delete(archivo);
            }
        }
    }
}
