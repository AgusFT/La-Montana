package ar.com.lamontana.web;

import static ar.com.lamontana.web.PaginaWebController.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PaginaWebService {
    private final JdbcTemplate jdbc;
    private final JsonMapper json=JsonMapper.builder().build();
    public PaginaWebService(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Borrador(long version,Contenido contenido,String carpeta,Instant actualizadaEn){}
    public record Publicacion(UUID codigo,long numero,long versionBorrador,String autor,Instant publicadaEn){}
    public record Estado(Borrador borrador,Publicacion publicada){}
    public record Revision(long version,boolean publicable,List<String> pendientes,int fichasVisibles,int imagenes){}
    /** Deliberately excludes local folder, actor identity, draft and hidden cards. */
    public record Publico(boolean configurada,Long version,Instant publicadaEn,Contenido contenido){}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Estado estado(String correo){propietario(correo);return leerEstado();}
    private Estado leerEstado(){return new Estado(borrador(),publicacionActual());}
    Borrador borrador(){return jdbc.queryForObject("SELECT version,contenido::text,carpeta,actualizada_en FROM lamontana.web_borrador WHERE unica",(r,n)->new Borrador(r.getLong(1),json.readValue(r.getString(2),Contenido.class),r.getString(3),r.getTimestamp(4).toInstant()));}
    private Publicacion publicacionActual(){var rows=jdbc.query("SELECT p.* FROM lamontana.web_publicacion p JOIN lamontana.web_borrador b ON p.codigo=b.publicacion WHERE b.unica",(r,n)->new Publicacion(r.getObject("codigo",UUID.class),r.getLong("numero"),r.getLong("version_borrador"),r.getString("autor"),r.getTimestamp("publicada_en").toInstant()));return rows.isEmpty()?null:rows.get(0);}
    @Transactional
    public Estado guardar(Guardar input,String correo){
        long actor=propietario(correo);bloquear();String huella=huella(input);
        var replay=reintento(input.operacion(),"GUARDAR",actor,huella,Estado.class);if(replay!=null)return leerEstado();
        var actual=borrador();version(actual,input.version());validarEstructura(input.contenido());validarCarpeta(input.carpeta());
        jdbc.update("UPDATE lamontana.web_borrador SET version=version+1,contenido=?::jsonb,carpeta=?,id_actor=?,actualizada_en=clock_timestamp() WHERE unica",json.writeValueAsString(input.contenido()),input.carpeta(),actor);
        var result=leerEstado();registrar(input.operacion(),"GUARDAR",actor,huella,result);return result;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Revision revision(String correo){propietario(correo);return revisar(borrador());}
    private Revision revisar(Borrador b){
        var c=b.contenido();var problemas=new ArrayList<String>();
        if(c.nombre().isBlank())problemas.add("Completá el nombre público de la imprenta en Identidad.");
        if(c.titulo().isBlank())problemas.add("Completá el título de portada en Identidad.");
        if(c.logo()!=null&&c.logoAlt().isBlank())problemas.add("Describí el logotipo con un texto alternativo.");
        if(c.portada()!=null&&c.portadaAlt().isBlank())problemas.add("Describí la imagen de portada con un texto alternativo.");
        for(var f:c.fichas())if(f.visible()){
            if(f.nombre().isBlank())problemas.add("Una ficha visible necesita nombre. Completalo en Productos y servicios.");
            if(f.descripcion().isBlank())problemas.add("Completá la descripción de la ficha "+(f.nombre().isBlank()?"sin nombre":f.nombre())+".");
            if(f.imagenes().stream().anyMatch(i->i.alternativo().isBlank()))problemas.add("Completá los textos alternativos de las imágenes de "+f.nombre()+".");
        }
        return new Revision(b.version(),problemas.isEmpty(),List.copyOf(problemas),(int)c.fichas().stream().filter(Ficha::visible).count(),imagenes(visibles(c)).size());
    }
    @Transactional
    public Publicacion publicar(Publicar input,String correo){
        long actor=propietario(correo);bloquear();String huella=huella(input);
        var replay=reintento(input.operacion(),"PUBLICAR",actor,huella,Publicacion.class);if(replay!=null)return replay;
        if(!input.confirmado())throw error(HttpStatus.BAD_REQUEST,"Confirmá expresamente que revisaste el contenido público.");
        var b=borrador();version(b,input.version());validarEstructura(b.contenido());var revision=revisar(b);
        if(!revision.publicable())throw error(HttpStatus.CONFLICT,String.join(" ",revision.pendientes()));
        var anterior=publicacionActual();if(anterior!=null&&anterior.versionBorrador()==b.version()){
            registrar(input.operacion(),"PUBLICAR",actor,huella,anterior);return anterior;
        }
        UUID codigo=UUID.randomUUID();long numero=jdbc.queryForObject("SELECT coalesce(max(numero),0)+1 FROM lamontana.web_publicacion",Long.class);
        String autor=jdbc.queryForObject("SELECT nombre||' '||apellido FROM lamontana.usuario WHERE id_usuario=?",String.class,actor);
        Contenido contenido=visibles(b.contenido());
        jdbc.update("INSERT INTO lamontana.web_publicacion(codigo,numero,version_borrador,contenido,id_actor,autor) VALUES(?,?,?,?::jsonb,?,?)",codigo,numero,b.version(),json.writeValueAsString(contenido),actor,autor);
        for(UUID imagen:imagenes(contenido))jdbc.update("INSERT INTO lamontana.web_publicacion_imagen(publicacion,imagen) VALUES(?,?)",codigo,imagen);
        jdbc.update("UPDATE lamontana.web_borrador SET publicacion=? WHERE unica",codigo);
        var result=publicacionActual();registrar(input.operacion(),"PUBLICAR",actor,huella,result);return result;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Publico publico(){var rows=jdbc.query("SELECT p.numero,p.publicada_en,p.contenido::text FROM lamontana.web_publicacion p JOIN lamontana.web_borrador b ON p.codigo=b.publicacion WHERE b.unica",(r,n)->new Publico(true,r.getLong(1),r.getTimestamp(2).toInstant(),json.readValue(r.getString(3),Contenido.class)));return rows.isEmpty()?new Publico(false,null,null,null):rows.get(0);}
    public long propietario(String correo){var rows=jdbc.query("SELECT u.id_usuario FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.correo=? AND u.estado='ACTIVO' AND u.es_administrador_propietario AND r.codigo='ADMIN_ADMIN' AND r.activo",(r,n)->r.getLong(1),correo);if(rows.isEmpty())throw error(HttpStatus.FORBIDDEN,"Sólo el propietario activo puede configurar la página web.");return rows.get(0);}
    void bloquear(){jdbc.execute("SELECT pg_advisory_xact_lock(764020)");}
    void version(Borrador b,long version){if(b.version()!=version)throw error(HttpStatus.CONFLICT,"El borrador web cambió en otra pestaña. Consultá la versión guardada antes de volver a guardar o publicar; tus datos locales se conservan.");}
    static void validarCarpeta(String carpeta){if(carpeta==null||carpeta.startsWith("/")||carpeta.contains("\\")||carpeta.contains("\u0000")||Arrays.stream(carpeta.split("/",-1)).anyMatch(s->s.equals("..")||s.equals(".")))throw error(HttpStatus.BAD_REQUEST,"Elegí una subcarpeta dentro de la carpeta de imágenes autorizada.");}
    private void validarEstructura(Contenido c){
        if(new HashSet<>(c.secciones()).size()!=c.secciones().size()||!c.secciones().contains(Seccion.PORTADA))throw error(HttpStatus.BAD_REQUEST,"Las secciones no se repiten y deben incluir Portada.");
        var ids=new HashSet<UUID>();for(var f:c.fichas()){
            if(!ids.add(f.codigo()))throw error(HttpStatus.BAD_REQUEST,"Cada ficha debe tener su identificador propio.");
            if(f.servicio()!=null&&(f.tipo()!=TipoFicha.SERVICIO||!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.servicio WHERE codigo_publico=?)",Boolean.class,f.servicio()))))throw error(HttpStatus.BAD_REQUEST,"La referencia de servicio debe corresponder a un servicio operativo existente.");
            if(f.imagenes().stream().map(Imagen::codigo).distinct().count()!=f.imagenes().size())throw error(HttpStatus.BAD_REQUEST,"Una ficha no puede repetir la misma imagen.");
        }
        for(UUID imagen:imagenes(c))if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.web_imagen WHERE codigo=?)",Boolean.class,imagen)))throw error(HttpStatus.BAD_REQUEST,"Una imagen ya no está en la biblioteca. Importala antes de asignarla.");
    }
    static Contenido visibles(Contenido c){return new Contenido(c.nombre(),c.titulo(),c.descripcion(),c.logo(),c.logoAlt(),c.portada(),c.portadaAlt(),c.secciones().contains(Seccion.CONTACTO)?c.correo():"",c.secciones().contains(Seccion.CONTACTO)?c.telefono():"",c.secciones().contains(Seccion.CONTACTO)?c.direccion():"",c.secciones(),c.fichas().stream().filter(f->f.visible()&&c.secciones().contains(Seccion.CATALOGO)).sorted(Comparator.comparingInt(Ficha::orden).thenComparing(f->f.codigo().toString())).toList());}
    static Set<UUID> imagenes(Contenido c){var ids=new HashSet<UUID>();if(c.logo()!=null)ids.add(c.logo());if(c.portada()!=null)ids.add(c.portada());c.fichas().forEach(f->f.imagenes().forEach(i->ids.add(i.codigo())));return ids;}
    String huella(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(value).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    <T>T reintento(UUID op,String tipo,long actor,String huella,Class<T> clase){var rows=jdbc.query("SELECT tipo,id_actor,huella,respuesta::text FROM lamontana.web_operacion WHERE codigo=?",(r,n)->{if(!tipo.equals(r.getString(1))||actor!=r.getLong(2)||!huella.equals(r.getString(3)))throw error(HttpStatus.CONFLICT,"Esa operación ya se utilizó con otros datos.");return json.readValue(r.getString(4),clase);},op);return rows.isEmpty()?null:rows.get(0);}
    void registrar(UUID op,String tipo,long actor,String huella,Object respuesta){jdbc.update("INSERT INTO lamontana.web_operacion(codigo,tipo,id_actor,huella,respuesta) VALUES(?,?,?,?,?::jsonb)",op,tipo,actor,huella,json.writeValueAsString(respuesta));}
    static ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
