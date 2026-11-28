import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Ejecutar con Java 17. Las rutas de prueba están separadas de la instalación y de los datos. */
public final class RutasNativasTest {
    public static void main(String[] args) throws Exception {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path raiz = Path.of(windows ? "C:/" : "/");
        if (RutasNativas.necesitaUnidad(raiz.resolve("Carpeta ASCII")) ||
                !RutasNativas.necesitaUnidad(raiz.resolve("Taller Ñ/Perfil de Sofía 文"))) {
            throw new AssertionError("Selección incorrecta de las rutas que necesitan alias.");
        }
        if (!windows) return;
        Path ensayo = Files.createTempDirectory("rutas-pg-");
        try {
            Path programa = Files.createDirectory(ensayo.resolve("Taller Ñ"));
            Path datos = Files.createDirectory(ensayo.resolve("Perfil de Sofía 文"));
            Path ajena = Files.createDirectory(ensayo.resolve("Terceros"));
            Path testigo = Files.writeString(ajena.resolve("conservar.txt"), "original");
            try (var primera = RutasNativas.abrir(programa, datos)) {
                if (!Files.isSameFile(primera.programa, programa) || !Files.isSameFile(primera.datos, datos))
                    throw new AssertionError("El alias debe apuntar a la carpeta física original.");
                try (var recuperada = RutasNativas.abrir(programa, datos)) {
                    if (!recuperada.programa.equals(primera.programa) || !recuperada.datos.equals(primera.datos))
                        throw new AssertionError("El panel debe recuperar las letras tras una interrupción.");
                }
                if (Files.exists(primera.programa) || Files.exists(primera.datos) || RutasNativas.tieneMarcas(datos))
                    throw new AssertionError("La recuperación debe retirar las dos letras y las marcas.");
            }
            try (var nueva = RutasNativas.abrir(programa, datos)) {
                Path letra = nueva.programa;
                subst(letra, "/D");
                subst(letra, ajena.toString());
                try {
                    nueva.close();
                    if (!Files.isSameFile(letra, ajena))
                        throw new AssertionError("El panel retiró una letra reasignada por el usuario.");
                } finally {
                    if (Files.isDirectory(letra) && Files.isSameFile(letra, ajena)) subst(letra, "/D");
                }
            }
            if (!Files.readString(testigo).equals("original")) throw new AssertionError("Se modificó un archivo ajeno.");
            System.out.println("Verificados los alias Unicode, su recuperación y el respeto a letras reasignadas.");
        } finally {
            try (var arbol = Files.walk(ensayo)) {
                for (Path ruta : arbol.sorted(Comparator.reverseOrder()).toList()) Files.delete(ruta);
            }
        }
    }
    private static void subst(Path raiz, String argumento) throws Exception {
        Path exe = Path.of(System.getenv("SystemRoot"), "System32", "subst.exe");
        if (new ProcessBuilder(exe.toString(), raiz.toString().substring(0, 2), argumento).inheritIO().start().waitFor() != 0)
            throw new AssertionError("No se pudo preparar el escenario de la letra reasignada.");
    }
}
