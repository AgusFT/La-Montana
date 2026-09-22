package ar.com.lamontana.catalogo;

import static ar.com.lamontana.catalogo.CatalogoController.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CatalogoService {
    private final JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();
    public CatalogoService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Formato(UUID codigoPublico,String codigo,String nombre,BigDecimal anchoMm,BigDecimal altoMm) {}
    public record Papel(UUID codigoPublico,String codigo,String nombre,BigDecimal gramaje,String terminacion) {}
    public record Servicio(UUID codigoPublico,String codigo,String nombre,TipoServicio tipo,String descripcion) {}
    public record Resumen(UUID codigoPublico,long numero,String motivo,Instant creadaEn,String actor) {}
    public record Revision(UUID codigoPublico,long numero,String motivo,Instant creadaEn,String actor,List<Tarifa> tarifas,List<OfertaServicio> servicios) {}
    public record Estado(List<Formato> formatos,List<Papel> papeles,List<Servicio> servicios,Revision actual,List<Resumen> historial) {}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Estado estado() {
        var actuales=jdbc.query("SELECT codigo_publico FROM lamontana.catalogo_revision WHERE vigente",(rs,row)->rs.getObject(1,UUID.class));
        return new Estado(formatos(),papeles(),servicios(),actuales.isEmpty()?null:revision(actuales.get(0)),
                jdbc.query("SELECT r.codigo_publico,r.id_catalogo_revision,r.motivo,r.creada_en,u.nombre||' '||u.apellido FROM lamontana.catalogo_revision r JOIN lamontana.usuario u ON u.id_usuario=r.id_actor ORDER BY r.id_catalogo_revision DESC LIMIT 50",
                        (rs,row)->new Resumen(rs.getObject(1,UUID.class),rs.getLong(2),rs.getString(3),rs.getTimestamp(4).toInstant(),rs.getString(5))));
    }
    private List<Formato> formatos() { return jdbc.query("SELECT codigo_publico,codigo,nombre,ancho_mm,alto_mm FROM lamontana.formato ORDER BY codigo",(r,n)->new Formato(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getBigDecimal(4),r.getBigDecimal(5))); }
    private List<Papel> papeles() { return jdbc.query("SELECT codigo_publico,codigo,nombre,gramaje_g_m2,terminacion_tipo FROM lamontana.papel ORDER BY codigo",(r,n)->new Papel(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getBigDecimal(4),r.getString(5))); }
    private List<Servicio> servicios() { return jdbc.query("SELECT codigo_publico,codigo,nombre,tipo,descripcion FROM lamontana.servicio ORDER BY codigo",(r,n)->new Servicio(r.getObject(1,UUID.class),r.getString(2),r.getString(3),TipoServicio.valueOf(r.getString(4)),r.getString(5))); }

    @Transactional public void formato(AltaFormato in,String actor) {
        UUID id=UUID.randomUUID();
        int n=jdbc.update("INSERT INTO lamontana.formato(codigo_publico,codigo,nombre,ancho_mm,alto_mm) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",id,codigo(in.codigo()),in.nombre().strip(),in.anchoMm(),in.altoMm());
        creado(n,"formato");evento(actor,"ALTA_FORMATO",id);
    }
    @Transactional public void papel(AltaPapel in,String actor) {
        UUID id=UUID.randomUUID();
        int n=jdbc.update("INSERT INTO lamontana.papel(codigo_publico,codigo,nombre,gramaje_g_m2,terminacion_tipo) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",id,codigo(in.codigo()),in.nombre().strip(),in.gramaje(),in.terminacion().strip());
        creado(n,"papel");evento(actor,"ALTA_PAPEL",id);
    }
    @Transactional public void servicio(AltaServicio in,String actor) {
        UUID id=UUID.randomUUID();
        int n=jdbc.update("INSERT INTO lamontana.servicio(codigo_publico,codigo,nombre,tipo,descripcion) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",id,codigo(in.codigo()),in.nombre().strip(),in.tipo().name(),in.descripcion()==null?null:in.descripcion().strip());
        creado(n,"servicio");evento(actor,"ALTA_SERVICIO",id);
    }
    private void creado(int n,String tipo) { if(n!=1) throw error(HttpStatus.CONFLICT,"Ya existe un "+tipo+" con ese código."); }
    private String codigo(String value) { return value.strip().toUpperCase(Locale.ROOT); }
    private void evento(String actor,String tipo,UUID objeto) {
        jdbc.update("INSERT INTO lamontana.evento_catalogo(id_actor,tipo,codigo_objeto) VALUES ((SELECT id_usuario FROM lamontana.usuario WHERE correo=?),?,?)",actor,tipo,objeto);
    }

    @Transactional
    public Revision guardar(NuevaRevision in,String actor) {
        // Una sola revisión efectiva y confirmaciones idempotentes, incluyendo solicitudes simultáneas.
        jdbc.execute("SELECT pg_advisory_xact_lock(764002)");
        String fingerprint=hash(json.writeValueAsString(in));
        var existentes=jdbc.query("SELECT r.codigo_publico,r.hash_solicitud,u.correo FROM lamontana.catalogo_revision r JOIN lamontana.usuario u ON u.id_usuario=r.id_actor WHERE r.id_operacion=?",
                (rs,row)->new String[]{rs.getString(1),rs.getString(2),rs.getString(3)},in.operacion());
        if(!existentes.isEmpty()) {
            var e=existentes.get(0);
            if(!e[1].equals(fingerprint)||!e[2].equals(actor)) throw error(HttpStatus.CONFLICT,"Esa confirmación ya se usó con otros datos. Volvé a revisar el catálogo.");
            return revision(UUID.fromString(e[0]));
        }
        var actual=jdbc.query("SELECT codigo_publico FROM lamontana.catalogo_revision WHERE vigente",(rs,row)->rs.getObject(1,UUID.class));
        UUID base=actual.isEmpty()?null:actual.get(0);
        if(!Objects.equals(base,in.versionBase())) throw error(HttpStatus.CONFLICT,"El catálogo cambió desde que lo abriste. Consultá la revisión vigente y revisá tus cambios antes de guardar.");
        validar(in);
        UUID codigo=UUID.randomUUID();
        Long id=jdbc.queryForObject("""
                INSERT INTO lamontana.catalogo_revision(codigo_publico,id_revision_base,id_operacion,hash_solicitud,motivo,id_actor)
                VALUES (?,(SELECT id_catalogo_revision FROM lamontana.catalogo_revision WHERE codigo_publico=?),?,?,?,(SELECT id_usuario FROM lamontana.usuario WHERE correo=?)) RETURNING id_catalogo_revision
                """,Long.class,codigo,base,in.operacion(),fingerprint,in.motivo().strip(),actor);
        for(Tarifa t:in.tarifas()) jdbc.update("""
                INSERT INTO lamontana.tarifa_impresion(id_catalogo_revision,id_formato,id_papel,modo_color,precio_por_carilla,recargo_doble_faz,habilitada)
                VALUES (?,(SELECT id_formato FROM lamontana.formato WHERE codigo_publico=?),(SELECT id_papel FROM lamontana.papel WHERE codigo_publico=?),?,?,?,?)
                """,id,t.formato(),t.papel(),t.color().name(),t.precio(),t.recargoDobleFaz(),t.habilitada());
        for(OfertaServicio s:in.servicios()) {
            Long configuracion=jdbc.queryForObject("""
                    INSERT INTO lamontana.configuracion_servicio(id_catalogo_revision,id_servicio,nombre_visible,base_precio,precio_unitario,preparacion_minutos,habilitado)
                    VALUES (?,(SELECT id_servicio FROM lamontana.servicio WHERE codigo_publico=?),?,?,?,?,?) RETURNING id_configuracion_servicio
                    """,Long.class,id,s.servicio(),s.nombreVisible().strip(),s.basePrecio().name(),s.precio(),s.preparacionMinutos(),s.habilitado());
            for(Compatibilidad c:s.compatibilidades()) jdbc.update("""
                    INSERT INTO lamontana.compatibilidad_servicio(id_configuracion_servicio,id_formato,id_papel)
                    VALUES (?,(SELECT id_formato FROM lamontana.formato WHERE codigo_publico=?),(SELECT id_papel FROM lamontana.papel WHERE codigo_publico=?))
                    """,configuracion,c.formato(),c.papel());
        }
        jdbc.update("UPDATE lamontana.catalogo_revision SET vigente=false WHERE vigente");
        jdbc.update("UPDATE lamontana.catalogo_revision SET vigente=true WHERE id_catalogo_revision=?",id);
        evento(actor,"REVISION_ACTIVADA",codigo);
        return revision(codigo);
    }

    private void validar(NuevaRevision in) {
        Set<UUID> formatos=new HashSet<>(),papeles=new HashSet<>();Map<UUID,TipoServicio> servicios=new HashMap<>();
        formatos().forEach(f->formatos.add(f.codigoPublico()));papeles().forEach(p->papeles.add(p.codigoPublico()));servicios().forEach(s->servicios.put(s.codigoPublico(),s.tipo()));
        var combinaciones=new HashSet<String>();var disponibles=new HashSet<Compatibilidad>();
        for(Tarifa t:in.tarifas()) {
            existe(combinacionExiste(formatos,papeles,t.formato(),t.papel()),"La tarifa debe referir a un formato y papel existentes.");
            existe(combinaciones.add(t.formato()+"/"+t.papel()+"/"+t.color()),"Hay tarifas duplicadas para la misma combinación.");
            if(t.habilitada()) disponibles.add(new Compatibilidad(t.formato(),t.papel()));
        }
        existe(!disponibles.isEmpty(),"Configurá al menos una tarifa de impresión habilitada.");
        var usados=new HashSet<UUID>();int impresiones=0;
        for(OfertaServicio s:in.servicios()) {
            TipoServicio tipo=servicios.get(s.servicio());
            existe(tipo!=null,"El servicio no existe en el catálogo.");
            existe(usados.add(s.servicio()),"Hay servicios duplicados en esta revisión.");
            if(tipo==TipoServicio.IMPRESION) {
                existe(s.basePrecio()==BasePrecio.POR_CARILLA && s.precio().signum()==0,"El precio de impresión se toma de las tarifas por carilla; no admite un segundo cargo de servicio.");
                existe(s.compatibilidades().isEmpty(),"Las combinaciones de impresión se configuran en las tarifas.");
                if(s.habilitado()) impresiones++;
            } else {
                var pares=new HashSet<Compatibilidad>();
                for(Compatibilidad c:s.compatibilidades()) {
                    existe(combinacionExiste(formatos,papeles,c.formato(),c.papel()),"La compatibilidad debe referir a un formato y papel existentes.");
                    existe(pares.add(c),"Hay compatibilidades duplicadas.");
                    if(s.habilitado()) existe(disponibles.contains(c),"Una terminación habilitada necesita una tarifa de impresión habilitada para cada compatibilidad.");
                }
                if(s.habilitado()) existe(!pares.isEmpty(),"Elegí las combinaciones admitidas por cada terminación habilitada.");
            }
        }
        existe(impresiones>=1,"Habilitá al menos un servicio de impresión para este catálogo.");
    }
    private boolean combinacionExiste(Set<UUID> formatos,Set<UUID> papeles,UUID f,UUID p) { return formatos.contains(f)&&papeles.contains(p); }
    private void existe(boolean condition,String message) { if(!condition) throw error(HttpStatus.BAD_REQUEST,message); }

    @Transactional(readOnly=true)
    public Revision revision(UUID codigo) {
        var result=jdbc.query("SELECT r.id_catalogo_revision,r.motivo,r.creada_en,u.nombre||' '||u.apellido FROM lamontana.catalogo_revision r JOIN lamontana.usuario u ON u.id_usuario=r.id_actor WHERE r.codigo_publico=?",
                (rs,row)->new Resumen(codigo,rs.getLong(1),rs.getString(2),rs.getTimestamp(3).toInstant(),rs.getString(4)),codigo);
        if(result.isEmpty()) throw error(HttpStatus.NOT_FOUND,"La revisión comercial no existe.");
        Resumen r=result.get(0);long id=r.numero();
        var tarifas=jdbc.query("""
                SELECT f.codigo_publico,p.codigo_publico,t.modo_color,t.precio_por_carilla,t.recargo_doble_faz,t.habilitada
                FROM lamontana.tarifa_impresion t JOIN lamontana.formato f USING(id_formato) JOIN lamontana.papel p USING(id_papel)
                WHERE t.id_catalogo_revision=? ORDER BY f.codigo,p.codigo,t.modo_color
                """,(rs,row)->new Tarifa(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),ModoColor.valueOf(rs.getString(3)),rs.getBigDecimal(4),rs.getBigDecimal(5),rs.getBoolean(6)),id);
        var ofertas=jdbc.query("""
                SELECT c.id_configuracion_servicio,s.codigo_publico,c.nombre_visible,c.base_precio,c.precio_unitario,c.preparacion_minutos,c.habilitado
                FROM lamontana.configuracion_servicio c JOIN lamontana.servicio s USING(id_servicio)
                WHERE c.id_catalogo_revision=? ORDER BY s.codigo
                """,(rs,row)->new OfertaServicio(rs.getObject(2,UUID.class),rs.getString(3),BasePrecio.valueOf(rs.getString(4)),rs.getBigDecimal(5),rs.getInt(6),rs.getBoolean(7),
                        jdbc.query("SELECT f.codigo_publico,p.codigo_publico FROM lamontana.compatibilidad_servicio x JOIN lamontana.formato f USING(id_formato) JOIN lamontana.papel p USING(id_papel) WHERE x.id_configuracion_servicio=? ORDER BY f.codigo,p.codigo",
                                (c,n)->new Compatibilidad(c.getObject(1,UUID.class),c.getObject(2,UUID.class)),rs.getLong(1))),id);
        return new Revision(codigo,id,r.motivo(),r.creadaEn(),r.actor(),tarifas,ofertas);
    }
    private ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
    private String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
