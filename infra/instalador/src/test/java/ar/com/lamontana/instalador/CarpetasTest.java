package ar.com.lamontana.instalador;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;

/** Ejecución con -ea; solo usa un árbol temporal y el servidor local de prueba. */
public final class CarpetasTest {
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("carpetas-prueba-");
        try{
            String id=UUID.randomUUID().toString();
            Path app=instalacion(root.resolve("Montaña con espacios"),id);
            Path copia=instalacion(root.resolve("copia"),id);
            Path parcial=instalacion(root.resolve("incompleta"),UUID.randomUUID().toString());Files.delete(parcial.resolve("compose.yaml"));
            Path corrupta=Files.createDirectories(root.resolve("corrupta"));Files.writeString(corrupta.resolve("instalacion.properties"),"id=no-valida\n");
            Path enorme=Files.createDirectories(root.resolve("enorme"));Files.writeString(enorme.resolve("instalacion.properties"),"x".repeat(65537));
            Path antigua=Files.createDirectories(root.resolve("antigua/infra"));Files.writeString(antigua.resolve("iniciar-local.sh"),"");Files.writeString(antigua.getParent().resolve("compose.yaml"),"");
            Path fuera=Files.createDirectories(root.resolve("afuera"));
            Path oculta=instalacion(root.resolve("node_modules/no-recorrer"),UUID.randomUUID().toString());
            boolean unix=FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
            if(unix){Files.createSymbolicLink(root.resolve("enlace"),app);Files.createSymbolicLink(fuera.resolve("ciclo"),root);}
            else if(System.getProperty("os.name").startsWith("Windows")){
                var junction=new ProcessBuilder("cmd.exe","/c","mklink","/J",root.resolve("enlace").toString(),app.toString()).redirectErrorStream(true).start();
                String salida=new String(junction.getInputStream().readAllBytes());assert junction.waitFor()==0:salida;
                assert Carpetas.enlace(root.resolve("enlace")):"No detectó la unión de Windows";
            }
            var equipo=new Equipo(new Paquete(root));
            Map<Path,String> antes=inventario(root);
            try(var c=new Carpetas(equipo)){
                c.buscar(List.of(root),Carpetas.AMPLIADA,false);esperar(c);
                var s=c.estado();assert s.get("estado").equals("COMPLETADO"):s;
                var resultados=resultados(s);assert resultados.size()==6:s;
                assert resultados.stream().filter(r->r.get("tipo").equals("IDENTIFICADA")).count()==2:s;
                assert resultados.stream().anyMatch(r->r.get("tipo").equals("INCOMPLETA")):s;
                assert resultados.stream().anyMatch(r->r.get("tipo").equals("SCRIPT_ANTERIOR")&&r.get("elegible").equals(false)):s;
                assert resultados.stream().filter(r->r.get("tipo").equals("IDENTIFICADA")).allMatch(r->r.get("detalle").toString().contains("misma identidad")):s;
                assert resultados.stream().noneMatch(r->r.get("directorio").toString().contains("no-recorrer")):s;
                assert antes.equals(inventario(root)):"Buscar modificó el árbol";
                c.buscar(List.of(enorme,root),Carpetas.AMPLIADA,false);esperar(c);
                assert resultados(c.estado()).size()==6:"Repitió una carpeta inconsistente al revisar otra raíz";
                c.buscar(List.of(root),new Carpetas.Limites(6,2,1000,100),false);esperar(c);assert c.estado().get("parcial").equals(true):c.estado();
                c.buscar(List.of(root),new Carpetas.Limites(6,20000,10000,1),false);esperar(c);assert resultados(c.estado()).size()==1:c.estado();
                c.buscar(List.of(fuera),Carpetas.AMPLIADA,false);esperar(c);assert resultados(c.estado()).isEmpty():c.estado();
                c.buscar(List.of(root),Carpetas.AMPLIADA,false);c.cancelar();esperar(c);assert c.estado().get("estado").equals("CANCELADO"):c.estado();
                // Volver a buscar tras cancelar no conserva señales ni resultados anteriores.
                c.buscar(List.of(app),Carpetas.AMPLIADA,false);esperar(c);assert resultados(c.estado()).size()==1:c.estado();
                if(unix){
                    Path inaccesible=instalacion(root.resolve("sin-permiso"),UUID.randomUUID().toString());
                    Files.setPosixFilePermissions(inaccesible,Set.of());
                    try{c.buscar(List.of(inaccesible,app),Carpetas.AMPLIADA,false);esperar(c);assert c.estado().get("parcial").equals(true);assert resultados(c.estado()).stream().anyMatch(r->r.get("directorio").equals(app.toString())):c.estado();}
                    finally{Files.setPosixFilePermissions(inaccesible,java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));}
                }
            }
            comprobarHttp(root,app);
            try(var c=new Carpetas(equipo)){
                c.buscar(List.of(root),Carpetas.AMPLIADA,false);c.close();esperar(c);
                assert !c.ocupado():"El cierre dejó una tarea pendiente";
                try{c.buscar(List.of(root),Carpetas.AMPLIADA,false);throw new AssertionError("Reabrió un selector cerrado");}catch(IllegalArgumentException esperada){}
            }
            assert !Files.exists(root.resolve("LaMontana")):"La recomendación no debe crear carpetas";
            try{Carpetas.ruta("relativa");throw new AssertionError();}catch(IllegalArgumentException esperada){}
            try{Carpetas.ruta("x\nruta");throw new AssertionError();}catch(IllegalArgumentException esperada){}
            System.out.println("OK: descubrimiento de lectura, clasificación, copias, límites, exclusiones, cancelación, permisos y HTTP autenticado.");
        }finally{try(var paths=Files.walk(root)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
    static Path instalacion(Path p,String id)throws Exception{
        Files.createDirectories(p);
        Files.writeString(p.resolve("instalacion.properties"),"id="+id+"\nproyecto=lamontana-"+id.replace("-","").substring(0,12)+"\nversion=0.2.1\nmodo=DESARROLLO\nestado=LISTA\npublico.prueba=interrumpida\n");
        Files.writeString(p.resolve("compose.yaml"),"# No debe ejecutarse\n");
        Files.writeString(p.resolve("secretos.properties"),"NO_LEER_NI_MOSTRAR=prueba\n");
        return p;
    }
    static Map<Path,String> inventario(Path root)throws Exception{
        var r=new HashMap<Path,String>();try(var paths=Files.walk(root)){for(Path p:paths.filter(x->Files.isRegularFile(x,LinkOption.NOFOLLOW_LINKS)).toList())r.put(p,Util.sha256(p));}return r;
    }
    @SuppressWarnings("unchecked") static List<Map<String,Object>> resultados(Map<String,Object> s){return (List<Map<String,Object>>)s.get("resultados");}
    static void esperar(Carpetas c)throws Exception{for(int n=0;n<500&&c.ocupado();n++)Thread.sleep(10);assert !c.ocupado():"No terminó la búsqueda";}
    static void comprobarHttp(Path root,Path app)throws Exception{
        Files.writeString(root.resolve("release.properties"),"app.version=0.2.1\ninstaller.version=0.2.6\n");
        try(var s=new Asistente(new Paquete(root))){
            s.start();var client=HttpClient.newHttpClient();String base=s.origen();
            for(String path:List.of("/api/carpetas/estado","/api/estado")){
                assert client.send(HttpRequest.newBuilder(URI.create(base+path)).build(),HttpResponse.BodyHandlers.ofString()).statusCode()==401;
            }
            String body="raiz="+java.net.URLEncoder.encode(app.toString(),java.nio.charset.StandardCharsets.UTF_8);
            var sinOrigen=HttpRequest.newBuilder(URI.create(base+"/api/carpetas/buscar")).header("Authorization","Bearer "+s.token()).header("Content-Type","application/x-www-form-urlencoded;charset=UTF-8").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            assert client.send(sinOrigen,HttpResponse.BodyHandlers.ofString()).statusCode()==403;
            var conOrigen=HttpRequest.newBuilder(sinOrigen.uri()).headers("Authorization","Bearer "+s.token(),"Origin",base,"Content-Type","application/x-www-form-urlencoded;charset=UTF-8").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            assert client.send(conOrigen,HttpResponse.BodyHandlers.ofString()).statusCode()==202;
            String estado="";
            for(int i=0;i<100;i++){estado=client.send(HttpRequest.newBuilder(URI.create(base+"/api/carpetas/estado")).header("Authorization","Bearer "+s.token()).build(),HttpResponse.BodyHandlers.ofString()).body();if(estado.contains("COMPLETADO"))break;Thread.sleep(10);}
            assert estado.contains("IDENTIFICADA")&&!estado.contains("NO_LEER_NI_MOSTRAR"):estado;
            assert !Files.exists(app.resolve(".instalador.lock")):"La búsqueda construyó una instalación o escribió un bloqueo";
            assert Files.readString(app.resolve("instalacion.properties")).contains("publico.prueba=interrumpida");
            var meta=client.send(HttpRequest.newBuilder(URI.create(base+"/api/estado")).header("Authorization","Bearer "+s.token()).build(),HttpResponse.BodyHandlers.ofString()).body();
            assert meta.contains("directorioRecomendado")&&meta.contains("LaMontana"):meta;
            var liberar=new java.util.concurrent.CountDownLatch(1);
            s.iniciarTrabajo(()->liberar.await());
            try{assert client.send(conOrigen,HttpResponse.BodyHandlers.ofString()).statusCode()==400:"Permitió buscar durante una operación";}
            finally{liberar.countDown();}
        }
    }
}
