//#region ENCABEZADO · ImagenWebService.java
/*
 * ========================================================================
 * ARCHIVO: ImagenWebService.java
 * ========================================================================
 * FUNCIÓN
 * Gestiona la biblioteca de imágenes y su importación idempotente, verificando contenido y
 * permisos. Coordina archivos con la transacción y limita el acceso público a imágenes publicadas.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] ImagenWebService(JdbcTemplate jdbc, PaginaWebService sitio, AlmacenImagenWeb almacen)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [private] Imagen mapear(java.sql.ResultSet r, int fila) throws java.sql.SQLException
 * - [public] List<Imagen> biblioteca(String correo)
 * - [public] AlmacenImagenWeb.Origen origen(String carpeta, String correo)
 * - [public] byte[] miniaturaEntrada(String ruta, String sha256, String correo)
 * - [public] Importacion importar(ImagenWebController.Importar input, String correo)
 * - [public] <anónima> :: void afterCompletion(int status)
 *   Elimina el archivo recién creado si la transacción no se confirma.
 * - [public] Path archivo(UUID codigo, boolean miniatura, boolean publico, String correo)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ImagenWebService (clase).
 * - ImagenWebService.Imagen (record).
 * - ImagenWebService.Importacion (record).
 * - ImagenWebService.<anónima> (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.web;

import static ar.com.lamontana.web.PaginaWebService.error;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;

@Service
public class ImagenWebService {
    private final JdbcTemplate jdbc;private final PaginaWebService sitio;private final AlmacenImagenWeb almacen;
    public ImagenWebService(JdbcTemplate jdbc,PaginaWebService sitio,AlmacenImagenWeb almacen){this.jdbc=jdbc;this.sitio=sitio;this.almacen=almacen;}
    public record Imagen(UUID codigo,String nombre,String sha256,String tipoOrigen,long bytesOrigen,int ancho,int alto,Instant creadaEn){}
    public record Importacion(Imagen imagen,boolean existente){}
    private Imagen mapear(java.sql.ResultSet r,int fila)throws java.sql.SQLException{return new Imagen(r.getObject("codigo",UUID.class),r.getString("nombre"),r.getString("sha256"),r.getString("tipo_origen"),r.getLong("bytes_origen"),r.getInt("ancho"),r.getInt("alto"),r.getTimestamp("creada_en").toInstant());}
    @Transactional(readOnly=true)
    public List<Imagen> biblioteca(String correo){sitio.propietario(correo);return jdbc.query("SELECT * FROM lamontana.web_imagen ORDER BY creada_en DESC,codigo",this::mapear);}
    public AlmacenImagenWeb.Origen origen(String carpeta,String correo){sitio.propietario(correo);return almacen.listar(carpeta);}
    public byte[] miniaturaEntrada(String ruta,String sha256,String correo){sitio.propietario(correo);var bytes=almacen.leer(ruta);if(!AlmacenImagenWeb.sha256(bytes).equals(sha256))throw error(HttpStatus.CONFLICT,"El archivo cambió. Actualizá la carpeta antes de elegirlo.");return almacen.miniatura(bytes);}
    @Transactional
    public Importacion importar(ImagenWebController.Importar input,String correo){
        long actor=sitio.propietario(correo);sitio.bloquear();String huella=sitio.huella(input);
        var replay=sitio.reintento(input.operacion(),"IMPORTAR",actor,huella,Importacion.class);if(replay!=null)return replay;
        byte[] bytes=almacen.leer(input.ruta());String hash=AlmacenImagenWeb.sha256(bytes);if(!hash.equals(input.sha256()))throw error(HttpStatus.CONFLICT,"El archivo cambió desde la consulta. Actualizá la carpeta para importar la imagen actual.");
        var existentes=jdbc.query("SELECT * FROM lamontana.web_imagen WHERE sha256=?",this::mapear,hash);
        if(!existentes.isEmpty()){var result=new Importacion(existentes.get(0),true);sitio.registrar(input.operacion(),"IMPORTAR",actor,huella,result);return result;}
        var imagen=almacen.normalizar(bytes);UUID codigo=UUID.randomUUID();almacen.guardar(codigo,imagen);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)almacen.borrarNuevo(codigo);}});
        String nombre=Path.of(input.ruta()).getFileName().toString();
        jdbc.update("INSERT INTO lamontana.web_imagen(codigo,sha256,nombre,tipo_origen,bytes_origen,ancho,alto,id_actor,sha256_publico,sha256_miniatura) VALUES(?,?,?,?,?,?,?,?,?,?)",codigo,hash,nombre,imagen.tipo(),bytes.length,imagen.ancho(),imagen.alto(),actor,AlmacenImagenWeb.sha256(imagen.completa()),AlmacenImagenWeb.sha256(imagen.miniatura()));
        var result=new Importacion(jdbc.queryForObject("SELECT * FROM lamontana.web_imagen WHERE codigo=?",this::mapear,codigo),false);sitio.registrar(input.operacion(),"IMPORTAR",actor,huella,result);return result;
    }
    public Path archivo(UUID codigo,boolean miniatura,boolean publico,String correo){
        if(!publico)sitio.propietario(correo);
        boolean disponible=Boolean.TRUE.equals(jdbc.queryForObject(publico?"SELECT EXISTS(SELECT 1 FROM lamontana.web_publicacion_imagen i JOIN lamontana.web_borrador b ON b.publicacion=i.publicacion WHERE b.unica AND i.imagen=?)":"SELECT EXISTS(SELECT 1 FROM lamontana.web_imagen WHERE codigo=?)",Boolean.class,codigo));
        Path file=almacen.archivo(codigo,miniatura);if(!disponible||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw error(HttpStatus.NOT_FOUND,"La imagen no está disponible en esta página.");return file;
    }
}
