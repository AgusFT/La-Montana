package ar.com.lamontana.instalador;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

/** Copia bruta detenida. La comprobación del archivo no implementa restauración. */
final class Respaldo {
    static final Set<String> VOLUMENES=Set.of("postgres_data","archivos_data","imagenes_web_data","antivirus_data","mail_data","tls_data","tls_config");
    static Path crear(Instalacion app,Path destino)throws Exception {
        destino=destino.toAbsolutePath().normalize();Files.createDirectories(destino);destino=destino.toRealPath();
        if(destino.startsWith(app.raiz.toRealPath())||destino.toString().startsWith("\\\\")||Set.of("nfs","nfs4","cifs","smbfs").contains(Files.getFileStore(destino).type()))throw new IOException("Elegí una carpeta local fuera del directorio de la app.");
        if(app.equipo.windows){
            String root=destino.getRoot().toString().replace("'","''");
            var tipo=app.equipo.run(List.of("powershell.exe","-NoProfile","-NonInteractive","-Command","([IO.DriveInfo]::new('"+root+"')).DriveType.ToString()"));
            if(!tipo.ok()||!Set.of("Fixed","Removable").contains(tipo.salida().strip()))throw new IOException("El respaldo debe guardarse en un disco local, no en una unidad de red.");
        }
        var manifest=Util.properties(app.raiz.resolve("version.properties"));String helper=manifest.getProperty("image.postgres.id","");if(!helper.matches("sha256:[a-f0-9]{64}"))throw new IOException("Falta el inventario de la versión instalada; no se puede respaldar automáticamente.");
        var list=app.equipo.run(app.equipo.docker("volume","ls","--filter","label=com.docker.compose.project="+app.proyecto(),"--format","{{.Name}}"));if(!list.ok())throw new IOException("No se pudo inventariar los volúmenes.");
        var volumenes=list.salida().lines().filter(s->!s.isBlank()).toList();if(volumenes.isEmpty())throw new IOException("No se detectaron los volúmenes esperados; no se continúa el mantenimiento.");
        long bytes=100L*1024*1024;var images=new LinkedHashSet<String>();for(String name:List.of("postgres","backend","frontend","antivirus","mailpit","proxy")){String id=manifest.getProperty("image."+name+".id","");if(!id.matches("sha256:[a-f0-9]{64}"))throw new IOException("Inventario de imágenes incompleto.");images.add(id);var r=app.equipo.run(app.equipo.docker("image","inspect",id,"--format","{{.Size}}"));if(!r.ok())throw new IOException("Falta una imagen instalada para el respaldo.");bytes=Math.addExact(bytes,Long.parseLong(r.salida().strip()));}
        var encontrados=new HashSet<String>();
        for(String volume:volumenes){if(!volume.matches("[a-zA-Z0-9_.-]+"))throw new IOException("Nombre de volumen inesperado.");var ownership=app.equipo.run(app.equipo.docker("volume","inspect",volume,"--format","{{index .Labels `com.docker.compose.volume`}}|{{index .Labels `com.docker.compose.project`}}"));String[] parts=ownership.salida().strip().split("\\|");if(!ownership.ok()||parts.length!=2||!VOLUMENES.contains(parts[0])||!parts[1].equals(app.proyecto()))throw new IOException("El volumen no pertenece al inventario de esta app.");encontrados.add(parts[0]);
            var size=app.equipo.run(app.equipo.docker("run","--rm","--pull","never","--network","none","--read-only","--mount","type=volume,src="+volume+",dst=/datos,readonly","--entrypoint","du",helper,"-sk","/datos"));if(!size.ok())throw new IOException("No se pudo leer el tamaño de un volumen.");bytes=Math.addExact(bytes,Math.multiplyExact(Long.parseLong(size.salida().strip().split("\\s+")[0]),1024));}
        if(!encontrados.containsAll(Set.of("postgres_data","archivos_data","imagenes_web_data","antivirus_data"))||app.modo().equals("DESARROLLO")&&!encontrados.contains("mail_data"))throw new IOException("Faltan volúmenes obligatorios; no se puede completar el respaldo.");
        try(var tree=Files.walk(app.raiz)){for(Path p:tree.toList()){if(Files.isSymbolicLink(p))throw new IOException("El directorio contiene enlaces simbólicos; requiere inventario manual antes de respaldar.");if(Files.isRegularFile(p))bytes=Math.addExact(bytes,Files.size(p));}}
        if(Files.getFileStore(destino).getUsableSpace()<Math.multiplyExact(bytes,2))throw new IOException("Espacio insuficiente para staging y respaldo; se requieren aproximadamente "+(bytes*2/1024/1024)+" MiB libres.");
        app.estado.setProperty("estado","RESPALDO_EN_CURSO");app.persistir();
        app.ejecutar("--profile","standard","stop","--timeout","60");
        var running=app.equipo.run(app.compose("--profile","standard","ps","--status","running","-q"));if(!running.ok()||!running.salida().isBlank())throw new IOException("Todavía hay servicios en ejecución; no se copia la base en caliente.");
        Path stage=Files.createTempDirectory(destino,".lamontana-respaldo-");Util.privado(stage,true);String stamp=DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(Instant.now());Path archive=destino.resolve(app.proyecto()+"-"+stamp+"-"+Util.secreto().substring(0,8)+".zip"),partial=Path.of(archive+".partial");
        try{
            for(String volume:volumenes){var r=Util.ejecutar(app.equipo.docker("run","--rm","--pull","never","--network","none","--read-only","--mount","type=volume,src="+volume+",dst=/datos,readonly","--mount","type=bind,src="+app.equipo.rutaLinux(stage)+",dst=/copia","--entrypoint","tar",helper,"czf","/copia/"+volume+".tar.gz","-C","/datos","."),null,Duration.ofHours(2),null);if(!r.ok())throw new IOException("No se pudo copiar el volumen "+volume+".");}
            var save=new ArrayList<>(app.equipo.docker("image","save","--output",app.equipo.rutaLinux(stage.resolve("images.tar"))));save.addAll(images);var saved=Util.ejecutar(save,null,Duration.ofHours(2),null);if(!saved.ok())throw new IOException("No se pudieron exportar las imágenes de la instalación.");
            var files=new LinkedHashMap<String,Path>();try(var tree=Files.walk(app.raiz)){for(Path p:tree.filter(Files::isRegularFile).sorted().toList())if(!p.getFileName().toString().equals(".instalador.lock"))files.put("app/"+app.raiz.relativize(p).toString().replace('\\','/'),p);}try(var tree=Files.list(stage)){for(Path p:tree.sorted().toList())files.put("recursos/"+p.getFileName(),p);}
            var checksums=new Properties();for(var entry:files.entrySet())checksums.setProperty(entry.getKey(),Util.sha256(entry.getValue()));Util.guardar(stage.resolve("inventario.properties"),checksums);files.put("inventario.properties",stage.resolve("inventario.properties"));
            Files.createFile(partial);Util.privado(partial,false);
            try(var zip=new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(partial)))){zip.setLevel(1);for(var entry:files.entrySet()){zip.putNextEntry(new ZipEntry(entry.getKey()));Files.copy(entry.getValue(),zip);zip.closeEntry();}}
            verificar(partial,checksums);Files.move(partial,archive,StandardCopyOption.ATOMIC_MOVE);Files.writeString(Path.of(archive+".sha256"),Util.sha256(archive)+"  "+archive.getFileName()+"\n");Util.privado(Path.of(archive+".sha256"),false);return archive;
        }finally{ // Solo staging de esta operación; nunca los datos de la app ni respaldos anteriores.
            try(var tree=Files.walk(stage)){for(Path p:tree.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
    static void verificar(Path archive,Properties expected)throws Exception{var seen=new HashSet<String>();try(var zip=new ZipFile(archive.toFile())){var entries=zip.entries();while(entries.hasMoreElements()){var e=entries.nextElement();if(!seen.add(e.getName()))throw new IOException("Archivo duplicado en el respaldo.");if(e.getName().equals("inventario.properties"))continue;String hash=expected.getProperty(e.getName());if(hash==null)throw new IOException("Recurso fuera del inventario.");var digest=java.security.MessageDigest.getInstance("SHA-256");try(var in=zip.getInputStream(e)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}if(!HexFormat.of().formatHex(digest.digest()).equals(hash))throw new IOException("La copia no pasó la comprobación de integridad.");}}if(seen.size()!=expected.size()+1||!seen.containsAll(expected.stringPropertyNames()))throw new IOException("El respaldo está incompleto.");}
}
