//#region ENCABEZADO · ImagenWebIntegrationTest.java
/*
 * ========================================================================
 * ARCHIVO: ImagenWebIntegrationTest.java
 * ========================================================================
 * FUNCIÓN
 * Comprueba importación, normalización y miniaturas PNG/JPEG/WebP, deduplicación, privacidad,
 * validación de rutas, integridad, concurrencia y publicación de imágenes.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] void iniciar() throws Exception
 *   Preparación antes de cada prueba.
 * - [paquete] void arrancar()
 * - [paquete] void cerrar() throws Exception
 *   Limpieza después de cada prueba.
 * - [paquete] JsonNode estado() throws Exception
 * - [paquete] Map<String, Object> contenido()
 * - [paquete] Map<String, Object> ficha(String nombre, boolean visible)
 * - [paquete] Map<String, Object> guardarDatos(long v, Map<String, Object> c)
 * - [paquete] JsonNode guardar(long v, Map<String, Object> c) throws Exception
 * - [paquete] Map<String, Object> publicarDatos(long v)
 * - [paquete] HttpResponse<String> publicar(Map<String, Object> datos) throws Exception
 * - [paquete] JsonNode publico() throws Exception
 * - [paquete] byte[] crearImagen(String nombre, String formato, int color) throws Exception
 * - [paquete] String hash(byte[] bytes) throws Exception
 *   Calcula o prepara la huella SHA-256 del contenido.
 * - [paquete] Map<String, Object> importacion(String ruta, byte[] bytes) throws Exception
 * - [paquete] JsonNode importar(String ruta, byte[] bytes) throws Exception
 * - [paquete] String consulta(String path, String campo, String value)
 * - [paquete] HttpResponse<byte[]> binario(HttpClient c, String path) throws Exception
 * - [paquete] void pngJpegWebpSeNormalizanConMiniaturasYNoSePublicanAlImportar() throws Exception
 *   Caso de prueba.
 * - [paquete] void originalMovidoDedupeReemplazoYPublicacionAnteriorConservada() throws Exception
 *   Caso de prueba.
 * - [paquete] void rechazaEnlacesTraversalNoImagenLimitesYCambioDuranteSeleccion() throws
 *   Exception
 *   Caso de prueba.
 * - [paquete] void bibliotecaYSistemaDeArchivosSonPrivadosYLaPublicacionFiltraRecursos() throws
 *   Exception
 *   Caso de prueba.
 * - [paquete] void concurrenciaNoDuplicaContenidoYCsrfEsObligatorio() throws Exception
 *   Caso de prueba.
 * - [paquete] HttpClient cliente()
 * - [paquete] URI uri(String p)
 * - [paquete] HttpResponse<String> get(HttpClient c, String p) throws Exception
 * - [paquete] String csrf(HttpClient c) throws Exception
 * - [paquete] HttpRequest solicitud(HttpClient c, String method, String path, Object body) throws
 *   Exception
 * - [paquete] HttpResponse<String> mutar(HttpClient c, String method, String path, Object body)
 *   throws Exception
 * - [paquete] void login(HttpClient c, String correo) throws Exception
 * - [paquete] void status(HttpResponse<String> r, int expected)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ImagenWebIntegrationTest (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import static org.assertj.core.api.Assertions.*;
import ar.com.lamontana.identidad.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ImagenWebIntegrationTest {
    static final JsonMapper JSON=JsonMapper.builder().build();
    static final String EMAIL="sitio@example.test",PASSWORD="SitioPruebas2026!",TOKEN="instalacion-web-test-exclusiva-2026-09";
    Path entrada,biblioteca;
    EmbeddedPostgres pg;ConfigurableApplicationContext app;JdbcTemplate jdbc;Path files;HttpClient admin;int port;
    @BeforeEach void iniciar()throws Exception{
        pg=EmbeddedPostgres.builder().start();files=Files.createTempDirectory("lamontana-web-test-");entrada=files.resolve("entrada");biblioteca=files.resolve("biblioteca");Files.createDirectories(entrada);arrancar();
        status(get(cliente(),"/api/admin/pagina-web"),401);
        app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(TOKEN,"Dueño","Sitio",EMAIL,PASSWORD));admin=cliente();login(admin,EMAIL);
    }
    void arrancar(){app=new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+files,"--lamontana.instalacion.token="+TOKEN,"--lamontana.web.entrada="+entrada,"--lamontana.web.biblioteca="+biblioteca,"--lamontana.web.ruta-host="+entrada);port=Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));jdbc=app.getBean(JdbcTemplate.class);}
    @AfterEach void cerrar()throws Exception{if(app!=null)app.close();if(pg!=null)pg.close();if(files!=null)try(var paths=Files.walk(files)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    JsonNode estado()throws Exception{var r=get(admin,"/api/admin/pagina-web");status(r,200);return JSON.readTree(r.body());}
    Map<String,Object> contenido(){var c=new LinkedHashMap<String,Object>();c.putAll(Map.of("nombre","Imprenta propia","titulo","Trabajos de nuestra imprenta","descripcion","Presentación configurada por su propietario","logoAlt","","portadaAlt","","correo","contacto@example.test","telefono","","direccion","","secciones",List.of("PORTADA","CATALOGO","CONTACTO"),"fichas",List.of()));c.put("logo",null);c.put("portada",null);return c;}
    Map<String,Object> ficha(String nombre,boolean visible){var f=new LinkedHashMap<String,Object>();f.putAll(Map.of("codigo",UUID.randomUUID(),"tipo","PRODUCTO","nombre",nombre,"descripcion","Descripción propia","imagenes",List.of(),"orden",1,"visible",visible));f.put("servicio",null);return f;}
    Map<String,Object> guardarDatos(long v,Map<String,Object> c){return Map.of("operacion",UUID.randomUUID(),"version",v,"contenido",c,"carpeta","entrada-propia");}
    JsonNode guardar(long v,Map<String,Object> c)throws Exception{var r=mutar(admin,"PUT","/api/admin/pagina-web/borrador",guardarDatos(v,c));status(r,200);return JSON.readTree(r.body());}
    Map<String,Object> publicarDatos(long v){return Map.of("operacion",UUID.randomUUID(),"version",v,"confirmado",true);}
    HttpResponse<String> publicar(Map<String,Object> datos)throws Exception{return mutar(admin,"POST","/api/admin/pagina-web/publicar",datos);}
    JsonNode publico()throws Exception{var r=get(cliente(),"/api/publico/pagina-web");status(r,200);return JSON.readTree(r.body());}

    byte[] crearImagen(String nombre,String formato,int color)throws Exception{
        var image=new java.awt.image.BufferedImage(40,30,java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new java.awt.Color(color));g.fillRect(0,0,40,30);g.dispose();var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,formato,output);byte[] bytes=output.toByteArray();Files.write(entrada.resolve(nombre),bytes);return bytes;
    }
    String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
    Map<String,Object> importacion(String ruta,byte[] bytes)throws Exception{return Map.of("operacion",UUID.randomUUID(),"ruta",ruta,"sha256",hash(bytes));}
    JsonNode importar(String ruta,byte[] bytes)throws Exception{var r=mutar(admin,"POST","/api/admin/pagina-web/imagenes/importar",importacion(ruta,bytes));status(r,200);return JSON.readTree(r.body()).get("imagen");}
    String consulta(String path,String campo,String value){return path+"?"+campo+"="+URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8);}
    HttpResponse<byte[]> binario(HttpClient c,String path)throws Exception{return c.send(HttpRequest.newBuilder(uri(path)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());}
    @Test void pngJpegWebpSeNormalizanConMiniaturasYNoSePublicanAlImportar()throws Exception{
        var png=crearImagen("foto.png","png",0xff5577);var jpeg=crearImagen("foto.jpg","jpeg",0x22bb88);
        byte[] webp=getClass().getResourceAsStream("/web/imagen-fixture.webp").readAllBytes();Files.write(entrada.resolve("foto.webp"),webp);
        var source=get(admin,"/api/admin/pagina-web/origen");status(source,200);assertThat(JSON.readTree(source.body()).get("legible").asBoolean()).isTrue();assertThat(JSON.readTree(source.body()).get("archivos").size()).isEqualTo(3);
        for(var entry:Map.of("foto.png",png,"foto.jpg",jpeg,"foto.webp",webp).entrySet()){
            var imagen=importar(entry.getKey(),entry.getValue());String id=imagen.get("codigo").asString();
            var preview=binario(admin,"/api/admin/pagina-web/origen/miniatura?ruta="+entry.getKey()+"&sha256="+hash(entry.getValue()));assertThat(preview.statusCode()).isEqualTo(200);
            var copia=binario(admin,"/api/admin/pagina-web/imagenes/"+id);assertThat(copia.statusCode()).isEqualTo(200);assertThat(copia.headers().firstValue("Content-Type").orElse("")).isEqualTo("image/png");assertThat(copia.headers().firstValue("X-Content-Type-Options").orElse("")).isEqualTo("nosniff");
            var decoded=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(copia.body()));assertThat(decoded.getWidth()).isEqualTo(imagen.get("ancho").asInt());
            assertThat(binario(cliente(),"/api/publico/pagina-web/imagenes/"+id).statusCode()).isEqualTo(404);
        }
        assertThat(publico().get("configurada").asBoolean()).isFalse();assertThat(estado().get("borrador").get("version").asLong()).isEqualTo(1);
    }
    @Test void originalMovidoDedupeReemplazoYPublicacionAnteriorConservada()throws Exception{
        byte[] original=crearImagen("producto.png","png",0x4455ff);var input=importacion("producto.png",original);var first=mutar(admin,"POST","/api/admin/pagina-web/imagenes/importar",input);status(first,200);String id=JSON.readTree(first.body()).get("imagen").get("codigo").asString();
        Files.move(entrada.resolve("producto.png"),entrada.resolve("renombrada.png"));status(mutar(admin,"POST","/api/admin/pagina-web/imagenes/importar",input),200);
        assertThat(importar("renombrada.png",original).get("codigo").asString()).isEqualTo(id);assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.web_imagen",Integer.class)).isEqualTo(1);
        var c=contenido();c.put("logo",id);c.put("logoAlt","Marca de prueba");long v=guardar(1,c).get("borrador").get("version").asLong();status(publicar(publicarDatos(v)),200);
        var anon=cliente();byte[] publicada=binario(anon,"/api/publico/pagina-web/imagenes/"+id).body();Files.delete(entrada.resolve("renombrada.png"));assertThat(binario(anon,"/api/publico/pagina-web/imagenes/"+id).body()).isEqualTo(publicada);
        byte[] reemplazo=crearImagen("producto.png","png",0xff9933);String nueva=importar("producto.png",reemplazo).get("codigo").asString();assertThat(nueva).isNotEqualTo(id);assertThat(binario(anon,"/api/publico/pagina-web/imagenes/"+nueva).statusCode()).isEqualTo(404);
        String sitioPrevio=publico().toString();c.put("logo",nueva);v=guardar(v,c).get("borrador").get("version").asLong();assertThat(publico().toString()).isEqualTo(sitioPrevio);
        Files.writeString(biblioteca.resolve(nueva+".png"),"archivo alterado");status(publicar(publicarDatos(v)),409);assertThat(publico().toString()).isEqualTo(sitioPrevio);assertThat(binario(anon,"/api/publico/pagina-web/imagenes/"+id).body()).isEqualTo(publicada);
        app.close();arrancar();assertThat(publico().toString()).isEqualTo(sitioPrevio);assertThat(binario(admin,"/api/admin/pagina-web/imagenes/"+id).body()).isEqualTo(publicada);
    }
    @Test void rechazaEnlacesTraversalNoImagenLimitesYCambioDuranteSeleccion()throws Exception{
        byte[] png=crearImagen("valida.png","png",0x11ff44);Path exterior=files.resolve("fuera.png");Files.write(exterior,png);Files.createSymbolicLink(entrada.resolve("enlace.png"),exterior);Files.createSymbolicLink(entrada.resolve("carpeta-enlace"),files);
        for(String path:List.of("enlace.png","../fuera.png","carpeta-enlace/fuera.png",exterior.toString()))status(mutar(admin,"POST","/api/admin/pagina-web/imagenes/importar",importacion(path,png)),400);
        var linked=get(admin,consulta("/api/admin/pagina-web/origen","carpeta","carpeta-enlace"));status(linked,200);assertThat(JSON.readTree(linked.body()).get("legible").asBoolean()).isFalse();
        byte[] text="<html>no es una imagen</html>".getBytes();Files.write(entrada.resolve("falsa.jpg"),text);status(mutar(admin,"POST","/api/admin/pagina-web/imagenes/importar",importacion("falsa.jpg",text)),400);
        byte[] grande=new byte[10*1024*1024+1];Files.write(entrada.resolve("grande.png"),grande);status(mutar(admin,"POST","/api/admin/pagina-web/imagenes/importar",importacion("grande.png",grande)),400);
        byte[] dimensiones=png.clone();java.nio.ByteBuffer.wrap(dimensiones).putInt(16,6000).putInt(20,6000);var crc=new java.util.zip.CRC32();crc.update(dimensiones,12,17);java.nio.ByteBuffer.wrap(dimensiones).putInt(29,(int)crc.getValue());Files.write(entrada.resolve("dimensiones.png"),dimensiones);
        var limite=mutar(admin,"POST","/api/admin/pagina-web/imagenes/importar",importacion("dimensiones.png",dimensiones));status(limite,400);assertThat(limite.body()).contains("25 millones");
        var selected=importacion("valida.png",png);crearImagen("valida.png","png",0xeeee33);status(mutar(admin,"POST","/api/admin/pagina-web/imagenes/importar",selected),409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.web_imagen",Integer.class)).isZero();assertThat(estado().get("borrador").get("version").asLong()).isEqualTo(1);
    }
    @Test void bibliotecaYSistemaDeArchivosSonPrivadosYLaPublicacionFiltraRecursos()throws Exception{
        byte[] original=crearImagen("oculta.png","png",0x4455ff);String id=importar("oculta.png",original).get("codigo").asString();var c=contenido();var f=ficha("No publicar",false);f.put("imagenes",List.of(Map.of("codigo",id,"alternativo","Recurso privado")));c.put("fichas",List.of(f));long v=guardar(1,c).get("borrador").get("version").asLong();status(publicar(publicarDatos(v)),200);
        var anon=cliente();for(String path:List.of("/api/admin/pagina-web/imagenes","/api/admin/pagina-web/origen","/api/admin/pagina-web/imagenes/"+id,"/api/admin/pagina-web/imagenes/"+id+"/miniatura"))status(get(anon,path),401);assertThat(binario(anon,"/api/publico/pagina-web/imagenes/"+id).statusCode()).isEqualTo(404);
        for(String rol:List.of("CLIENTE","EMPLEADO","ADMIN_ADMIN")){
            String correo=rol.toLowerCase()+"-media@example.test";jdbc.update("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado) SELECT ?,r.id_rol,?,u.hash_contrasena,'Interno','Test','ACTIVO' FROM lamontana.usuario u CROSS JOIN lamontana.rol r WHERE u.correo=? AND r.codigo=?",UUID.randomUUID(),correo,EMAIL,rol);var user=cliente();login(user,correo);
            status(get(user,"/api/admin/pagina-web/origen"),403);status(get(user,"/api/admin/pagina-web/imagenes"),403);status(get(user,"/api/admin/pagina-web/imagenes/"+id),403);status(mutar(user,"POST","/api/admin/pagina-web/imagenes/importar",importacion("oculta.png",original)),403);
        }
        f.put("visible",true);v=guardar(v,c).get("borrador").get("version").asLong();status(publicar(publicarDatos(v)),200);assertThat(binario(anon,"/api/publico/pagina-web/imagenes/"+id).statusCode()).isEqualTo(200);
        f.put("visible",false);v=guardar(v,c).get("borrador").get("version").asLong();status(publicar(publicarDatos(v)),200);assertThat(binario(anon,"/api/publico/pagina-web/imagenes/"+id).statusCode()).isEqualTo(404);
    }
    @Test void concurrenciaNoDuplicaContenidoYCsrfEsObligatorio()throws Exception{
        byte[] bytes=crearImagen("copia.png","png",0xabcdef);var data=importacion("copia.png",bytes);var request=solicitud(admin,"POST","/api/admin/pagina-web/imagenes/importar",data);var a=admin.sendAsync(request,HttpResponse.BodyHandlers.ofString());var b=admin.sendAsync(request,HttpResponse.BodyHandlers.ofString());status(a.get(),200);status(b.get(),200);assertThat(a.get().body()).isEqualTo(b.get().body());assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.web_imagen",Integer.class)).isEqualTo(1);
        var missing=HttpRequest.newBuilder(uri("/api/admin/pagina-web/imagenes/importar")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(importacion("copia.png",bytes)))).build();status(admin.send(missing,HttpResponse.BodyHandlers.ofString()),403);
    }

    HttpClient cliente(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();}
    URI uri(String p){return URI.create("http://127.0.0.1:"+port+p);}
    HttpResponse<String> get(HttpClient c,String p)throws Exception{return c.send(HttpRequest.newBuilder(uri(p)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    String csrf(HttpClient c)throws Exception{return JSON.readTree(get(c,"/api/auth/csrf").body()).get("token").asString();}
    HttpRequest solicitud(HttpClient c,String method,String path,Object body)throws Exception{return HttpRequest.newBuilder(uri(path)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf(c)).method(method,HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build();}
    HttpResponse<String> mutar(HttpClient c,String method,String path,Object body)throws Exception{return c.send(solicitud(c,method,path,body),HttpResponse.BodyHandlers.ofString());}
    void login(HttpClient c,String correo)throws Exception{var r=HttpRequest.newBuilder(uri("/api/auth/login")).header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",csrf(c)).POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(correo,java.nio.charset.StandardCharsets.UTF_8)+"&password="+PASSWORD)).build();status(c.send(r,HttpResponse.BodyHandlers.ofString()),200);}
    void status(HttpResponse<String> r,int expected){assertThat(r.statusCode()).withFailMessage("Esperado %s recibido %s: %s",expected,r.statusCode(),r.body()).isEqualTo(expected);}
}
