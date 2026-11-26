package ar.com.lamontana.instalador;
import java.net.*;import java.net.http.*;import java.nio.file.*;import java.util.*;
public class InstaladorTest {
    public static void main(String[] args) throws Exception {
        assert Util.decodificar("Ubuntu: ejecución correcta".getBytes(java.nio.charset.StandardCharsets.UTF_16LE)).equals("Ubuntu: ejecución correcta");
        assert Util.decodificar("La Montaña".getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals("La Montaña");
        assert Paquete.comparar("1.10.0","1.9.0")>0;
        falla(()->Paquete.validarOperacion("actualizar","2.0.0","1.0.0"));
        falla(()->Paquete.validarOperacion("reparar","1.0.0","1.1.0"));
        assert Equipo.windowsArgument("C:\\Program Files\\La Montana").equals("\"C:\\Program Files\\La Montana\"");
        falla(()->Equipo.windowsArgument("x\ny"));
        assert Util.form("nombre=La+Monta%C3%B1a").get("nombre").equals("La Montaña");
        falla(()->Util.form("x=1&x=2"));
        assert Instalacion.puerto("3200")==3200;
        falla(()->Instalacion.puerto("80"));
        assert Equipo.DISTRO.equals("LaMontanaDemo-Ubuntu");
        Path dir=Files.createTempDirectory("instalador-test-");
        RetiroWindowsTest.verificar(dir.resolve("retiro"));
        Path original=dir.resolve("release.properties"),copia=dir.resolve("version.properties");
        Files.writeString(original,"version=1\n");
        if(System.getProperty("os.name").startsWith("Windows"))Files.setAttribute(original,"dos:readonly",true);
        Util.copiarManifiesto(original,copia);
        if(System.getProperty("os.name").startsWith("Windows"))Files.setAttribute(copia,"dos:readonly",true);
        Util.copiarManifiesto(original,copia);
        assert Files.readString(copia).equals("version=1\n");
        if(System.getProperty("os.name").startsWith("Windows"))Files.setAttribute(original,"dos:readonly",false);
        Files.delete(original);Files.delete(copia);
        if(System.getProperty("os.name").startsWith("Windows")){
            Util.privado(dir,true);
            var acl=Files.getFileAttributeView(dir,java.nio.file.attribute.AclFileAttributeView.class);
            assert acl.getOwner().equals(Util.usuarioWindows());
            assert acl.getAcl().size()==1&&acl.getAcl().get(0).principal().equals(Util.usuarioWindows());
        }
        var app=new Instalacion(dir,new Equipo(new Paquete(dir)));
        try{Respaldo.crear(app,dir.resolve("respaldo-interno"));throw new AssertionError("No debe copiar dentro de la app");}
        catch(java.io.IOException esperada){assert esperada.getMessage().contains("fuera del directorio");}
        Files.delete(dir.resolve("respaldo-interno"));
        verificarRespaldos(dir);
        var segunda=new Instalacion(dir,new Equipo(new Paquete(dir)));
        String id=UUID.randomUUID().toString();
        Files.writeString(dir.resolve("instalacion.properties"),"id="+id+"\nmodo=STANDARD\nversion=0.1.0\nproyecto=lamontana-"+id.replace("-","").substring(0,12));
        String antes=Util.sha256(dir.resolve("instalacion.properties"));
        try{new Instalacion(dir,new Equipo(new Paquete(dir)));throw new AssertionError("Debe rechazar la instalación Standard antes de acceder a Docker.");}
        catch(IllegalArgumentException esperada){assert esperada.getMessage().contains("Demo");}
        assert Util.sha256(dir.resolve("instalacion.properties")).equals(antes);
        Files.delete(dir.resolve("instalacion.properties"));
        try{app.instalar(Map.of("modo","STANDARD"));throw new AssertionError("Debe rechazar Standard.");}catch(IllegalArgumentException esperada){}
        try{app.instalar(Map.of("lan","si"));throw new AssertionError("La instalación debe ser sólo local.");}catch(IllegalArgumentException esperada){}
        Path preexistente=Files.writeString(dir.resolve("dato-preexistente"),"conservar");
        try{app.instalar(Map.of());throw new AssertionError("Debe revisar el destino antes de escribir el lock");}
        catch(java.io.IOException esperada){assert esperada.getMessage().contains("carpeta vacía");}
        assert !Files.exists(dir.resolve(".instalador.lock")) && Files.readString(preexistente).equals("conservar");
        Files.delete(preexistente);
        try{app.paquete.verificar();throw new AssertionError("Debe rechazar el paquete sin identidad Demo.");}
        catch(java.io.IOException esperada){assert esperada.getMessage().contains("La Montaña Demo");}
        app.modificar(()->{
            try{segunda.modificar(()->{throw new AssertionError("No debe entrar en una instalación bloqueada");});throw new AssertionError("Debió bloquear al segundo asistente");}
            catch(java.io.IOException esperada){assert esperada.getMessage().contains("Otra ejecución");}
        });
        Files.delete(dir.resolve(".instalador.lock"));
        Files.writeString(original,"app.version=0.2.1\ninstaller.version=0.2.3\n");
        try(var s=new Asistente(new Paquete(dir))){s.start();var c=HttpClient.newHttpClient();String base=s.origen();
            assert c.send(HttpRequest.newBuilder(URI.create(base+"/")).build(),HttpResponse.BodyHandlers.ofString()).statusCode()==200;
            assert c.send(HttpRequest.newBuilder(URI.create(base+"/api/estado")).build(),HttpResponse.BodyHandlers.ofString()).statusCode()==401;
            assert c.send(HttpRequest.newBuilder(URI.create(base+"/api/estado")).header("Authorization","Bearer "+s.token()).header("Origin","https://ajeno.invalid").build(),HttpResponse.BodyHandlers.ofString()).statusCode()==403;
            var metadata=c.send(HttpRequest.newBuilder(URI.create(base+"/api/estado")).header("Authorization","Bearer "+s.token()).build(),HttpResponse.BodyHandlers.ofString());
            assert metadata.statusCode()==200&&metadata.body().contains("\"version\":\"0.2.1\"")&&metadata.body().contains("\"versionInstalador\":\"0.2.3\"");
            var logo=c.send(HttpRequest.newBuilder(URI.create(base+"/yg-soluciones.svg")).build(),HttpResponse.BodyHandlers.ofString());
            assert logo.statusCode()==200&&logo.headers().firstValue("Content-Type").orElse("").startsWith("image/svg+xml");
            assert c.send(HttpRequest.newBuilder(URI.create(base+"/release.properties")).build(),HttpResponse.BodyHandlers.ofString()).statusCode()==404;
            assert c.send(HttpRequest.newBuilder(URI.create(base+"/api/cerrar")).header("Authorization","Bearer "+s.token()).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString()).statusCode()==403;
        }finally{Files.delete(original);Files.delete(dir);}
        System.out.println("OK: versiones, formularios y límites de acceso HTTP.");
    }
    static void verificarRespaldos(Path dir)throws Exception{
        Path recurso=dir.resolve("dato"),archivo=dir.resolve("copia.zip");Files.writeString(recurso,"dato original");
        var inventario=new Properties();inventario.setProperty("app/dato",Util.sha256(recurso));
        for(String contenido:List.of("dato original","dato alterado","")){
            try(var zip=new java.util.zip.ZipOutputStream(Files.newOutputStream(archivo))){
                zip.putNextEntry(new java.util.zip.ZipEntry("inventario.properties"));inventario.store(zip,"");zip.closeEntry();
                if(!contenido.isEmpty()){zip.putNextEntry(new java.util.zip.ZipEntry("app/dato"));zip.write(contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}
            }
            if(contenido.equals("dato original"))Respaldo.verificar(archivo,inventario);
            else try{Respaldo.verificar(archivo,inventario);throw new AssertionError("Debe rechazar respaldo alterado o incompleto");}catch(java.io.IOException esperada){}
        }
        Files.delete(recurso);Files.delete(archivo);
    }
    static void falla(Runnable f){try{f.run();throw new AssertionError("Debió rechazar la operación");}catch(IllegalArgumentException esperado){}}
}
