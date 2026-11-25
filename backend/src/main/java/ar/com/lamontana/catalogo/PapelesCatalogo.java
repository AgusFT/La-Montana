//#region ENCABEZADO · PapelesCatalogo.java
/*
 * ========================================================================
 * ARCHIVO: PapelesCatalogo.java
 * ========================================================================
 * FUNCIÓN
 * Define los tamaños y gramajes precargados y administra la selección de papeles y sus variantes
 * personalizadas. Sus escrituras se realizan dentro de la transacción y el bloqueo de
 * CatalogoService.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [paquete] PapelesCatalogo(JdbcTemplate jdbc)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [private, static] List<Predefinido> predefinidos()
 * - [paquete] List<Seleccion> seleccion()
 * - [paquete] void predefinido(String codigo, String actor)
 * - [paquete] void personalizado(PapelPersonalizado p, String actor)
 * - [paquete] void cambiar(SeleccionPapel p, String actor)
 * - [private] long formato(BigDecimal ancho, BigDecimal alto, String nombre)
 * - [private] long crearPapel(String codigo, String nombre, BigDecimal gramaje, String
 *   terminacion)
 * - [private] void evento(String actor, String tipo, long papel)
 * - [private] ResponseStatusException error(HttpStatus status, String message)
 *   Construye un error HTTP controlado.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PapelesCatalogo (clase).
 * - PapelesCatalogo.Seleccion (record).
 * - PapelesCatalogo.Predefinido (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.catalogo;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static ar.com.lamontana.catalogo.CatalogoController.*;

/** Operaciones llamadas dentro de la transacción y bloqueo de CatalogoService. */
public final class PapelesCatalogo {
    private final JdbcTemplate jdbc;
    PapelesCatalogo(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Seleccion(UUID formato,UUID papel,boolean habilitado,String predefinido) {}
    public record Predefinido(String codigo,String nombre,BigDecimal anchoMm,BigDecimal altoMm,BigDecimal gramaje,String terminacion) {}
    // Dimensiones: manual Brother MFC-J3940DW, tamaños de papel para cada operación.
    static final List<Predefinido> PREDEFINIDOS=predefinidos();
    private static List<Predefinido> predefinidos() {
        String[][] tamanos={{"A4","A4","210","297"},{"A3","A3","297","420"},{"A5","A5","148","210"},
            {"A6","A6","105","148"},{"CARTA","Carta","215.9","279.4"},{"FOLIO","Oficio / Folio 8½ × 13","215.9","330.2"},
            {"LEGAL","Legal 8½ × 14","215.9","355.6"}};
        var lista=new ArrayList<Predefinido>();
        for(var t:tamanos)for(int g:new int[]{75,80,90})lista.add(new Predefinido(t[0]+"-COMUN-"+g,t[1]+" · papel común · "+g+" g/m²",new BigDecimal(t[2]),new BigDecimal(t[3]),BigDecimal.valueOf(g),"Sin estucar"));
        return List.copyOf(lista);
    }
    List<Seleccion> seleccion() {
        return jdbc.query("SELECT f.codigo_publico,p.codigo_publico,c.habilitado,c.predefinido FROM lamontana.catalogo_papel c JOIN lamontana.formato f USING(id_formato) JOIN lamontana.papel p USING(id_papel) ORDER BY f.codigo,p.codigo",
            (r,n)->new Seleccion(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getBoolean(3),r.getString(4)));
    }
    void predefinido(String codigo,String actor) {
        var p=PREDEFINIDOS.stream().filter(x->x.codigo().equals(codigo)).findFirst().orElseThrow(()->error(HttpStatus.BAD_REQUEST,"Elegí un papel de la lista precargada."));
        var existentes=jdbc.query("SELECT f.codigo_publico,p.codigo_publico FROM lamontana.catalogo_papel c JOIN lamontana.formato f USING(id_formato) JOIN lamontana.papel p USING(id_papel) WHERE c.predefinido=?",(r,n)->new UUID[]{r.getObject(1,UUID.class),r.getObject(2,UUID.class)},codigo);
        if(!existentes.isEmpty()){cambiar(new SeleccionPapel(existentes.get(0)[0],existentes.get(0)[1],true),actor);return;}
        long f=formato(p.anchoMm(),p.altoMm(),p.nombre().split(" · ")[0]);
        var materiales=jdbc.query("SELECT id_papel FROM lamontana.papel WHERE nombre='Papel común blanco' AND gramaje_g_m2=? AND terminacion_tipo=? ORDER BY id_papel LIMIT 1",(r,n)->r.getLong(1),p.gramaje(),p.terminacion());
        long papel=materiales.isEmpty()?crearPapel("COMUN-"+p.gramaje().toPlainString()+"-"+UUID.randomUUID().toString().substring(0,8),"Papel común blanco",p.gramaje(),p.terminacion()):materiales.get(0);
        jdbc.update("INSERT INTO lamontana.catalogo_papel(id_formato,id_papel,predefinido) VALUES (?,?,?) ON CONFLICT(id_formato,id_papel) DO UPDATE SET habilitado=true,predefinido=excluded.predefinido",f,papel,codigo);
        evento(actor,"PAPEL_HABILITADO",papel);
    }
    void personalizado(PapelPersonalizado p,String actor) {
        String codigo=p.codigo().toUpperCase(Locale.ROOT);
        var existentes=jdbc.query("SELECT id_papel,nombre,gramaje_g_m2,terminacion_tipo FROM lamontana.papel WHERE codigo=?",(r,n)->new Object[]{r.getLong(1),r.getString(2),r.getBigDecimal(3),r.getString(4)},codigo);
        if(!existentes.isEmpty()) {
            var e=existentes.get(0);long id=(Long)e[0];
            boolean mismo=e[1].equals(p.nombre().strip())&&((BigDecimal)e[2]).compareTo(p.gramaje())==0&&e[3].equals(p.terminacion().strip());
            var pares=jdbc.query("SELECT f.codigo_publico,p.codigo_publico FROM lamontana.catalogo_papel c JOIN lamontana.formato f USING(id_formato) JOIN lamontana.papel p USING(id_papel) WHERE c.id_papel=? AND f.ancho_mm=? AND f.alto_mm=?",(r,n)->new UUID[]{r.getObject(1,UUID.class),r.getObject(2,UUID.class)},id,p.anchoMm(),p.altoMm());
            if(!mismo||pares.isEmpty())throw error(HttpStatus.CONFLICT,"Ese código ya identifica otro papel. Elegí uno diferente para estas características.");
            cambiar(new SeleccionPapel(pares.get(0)[0],pares.get(0)[1],true),actor);return;
        }
        long f=formato(p.anchoMm(),p.altoMm(),p.anchoMm().stripTrailingZeros().toPlainString()+" × "+p.altoMm().stripTrailingZeros().toPlainString()+" mm");
        long papel=crearPapel(codigo,p.nombre().strip(),p.gramaje(),p.terminacion().strip());
        jdbc.update("INSERT INTO lamontana.catalogo_papel(id_formato,id_papel) VALUES (?,?)",f,papel);
        evento(actor,"ALTA_PAPEL_PERSONALIZADO",papel);
    }
    void cambiar(SeleccionPapel p,String actor) {
        var valores=jdbc.query("SELECT c.id_papel,c.habilitado FROM lamontana.catalogo_papel c JOIN lamontana.formato f USING(id_formato) JOIN lamontana.papel p USING(id_papel) WHERE f.codigo_publico=? AND p.codigo_publico=?",(r,n)->new Object[]{r.getLong(1),r.getBoolean(2)},p.formato(),p.papel());
        if(valores.isEmpty())throw error(HttpStatus.NOT_FOUND,"El papel con ese tamaño no está en el catálogo base.");
        if(valores.get(0)[1].equals(p.habilitado()))return;
        jdbc.update("UPDATE lamontana.catalogo_papel SET habilitado=? WHERE id_formato=(SELECT id_formato FROM lamontana.formato WHERE codigo_publico=?) AND id_papel=(SELECT id_papel FROM lamontana.papel WHERE codigo_publico=?)",p.habilitado(),p.formato(),p.papel());
        evento(actor,p.habilitado()?"PAPEL_HABILITADO":"PAPEL_DESHABILITADO",(Long)valores.get(0)[0]);
    }
    private long formato(BigDecimal ancho,BigDecimal alto,String nombre) {
        var existentes=jdbc.query("SELECT id_formato FROM lamontana.formato WHERE ancho_mm=? AND alto_mm=? ORDER BY id_formato LIMIT 1",(r,n)->r.getLong(1),ancho,alto);
        if(!existentes.isEmpty())return existentes.get(0);
        UUID id=UUID.randomUUID();
        return jdbc.queryForObject("INSERT INTO lamontana.formato(codigo_publico,codigo,nombre,ancho_mm,alto_mm) VALUES (?,?,?,?,?) RETURNING id_formato",Long.class,id,"F-"+id,nombre,ancho,alto);
    }
    private long crearPapel(String codigo,String nombre,BigDecimal gramaje,String terminacion) {
        return jdbc.queryForObject("INSERT INTO lamontana.papel(codigo_publico,codigo,nombre,gramaje_g_m2,terminacion_tipo) VALUES (?,?,?,?,?) RETURNING id_papel",Long.class,UUID.randomUUID(),codigo,nombre,gramaje,terminacion);
    }
    private void evento(String actor,String tipo,long papel) {
        jdbc.update("INSERT INTO lamontana.evento_catalogo(id_actor,tipo,codigo_objeto) SELECT u.id_usuario,?,p.codigo_publico FROM lamontana.usuario u CROSS JOIN lamontana.papel p WHERE u.correo=? AND p.id_papel=?",tipo,actor,papel);
    }
    private ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
