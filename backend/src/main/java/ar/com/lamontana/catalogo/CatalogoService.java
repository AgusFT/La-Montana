//#region ENCABEZADO · CatalogoService.java
/*
 * ========================================================================
 * ARCHIVO: CatalogoService.java
 * ========================================================================
 * FUNCIÓN
 * Guarda, consulta y programa configuraciones comerciales de forma atómica e idempotente. Valida
 * variantes, reglas comunes del grupo, servicios y precios sin modificar el historial.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] CatalogoService(JdbcTemplate jdbc)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * - [public] Estado estado()
 * - [public] Estado leerEstado()
 * - [private] List<Formato> formatos()
 * - [private] List<Papel> papeles()
 * - [private] List<Servicio> servicios()
 * - [public] void predefinido(String codigo, String actor)
 * - [public] void todosPredefinidos(String actor)
 * - [public] void personalizado(PapelPersonalizado in, String actor)
 * - [public] void seleccion(SeleccionPapel in, String actor)
 * - [public] void formato(AltaFormato in, String actor)
 * - [public] void papel(AltaPapel in, String actor)
 * - [public] void servicio(AltaServicio in, String actor)
 * - [private] void creado(int n, String tipo)
 * - [private] String codigo(String value)
 * - [private] void evento(String actor, String tipo, UUID objeto)
 * - [public] Revision guardar(NuevaRevision in, String actor)
 * - [public] Revision cancelar(UUID codigo, CancelarProgramacion in, String actor)
 * - [public] void reconciliarProgramaciones()
 * - [private] void bloquear()
 * - [private] Instant ahora()
 * - [private] Timestamp timestamp(Instant instante)
 * - [private] void publicar(long id)
 * - [private] void aplicarVencida()
 * - [private] String huellaRevision(NuevaRevision in)
 * - [private] void validar(NuevaRevision in)
 * - [private] boolean combinacionExiste(Set<UUID> formatos, Set<UUID> papeles, UUID f, UUID p)
 * - [private] void existe(boolean condition, String message)
 * - [public] Revision revision(UUID codigo)
 * - [private] Resumen resumen(ResultSet rs, int row) throws SQLException
 * - [private] Revision cargarRevision(UUID codigo)
 * - [private] ResponseStatusException error(HttpStatus status, String message)
 * - [private] String hash(String text)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - CatalogoService (class).
 * - CatalogoService.Formato (record).
 * - CatalogoService.Papel (record).
 * - CatalogoService.Servicio (record).
 * - CatalogoService.EstadoRevision (enum).
 * - CatalogoService.Resumen (record).
 * - CatalogoService.Revision (record).
 * - CatalogoService.Estado (record).
 * - CatalogoService.Pendiente (record).
 * - CatalogoService.RevisionInmediata (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.catalogo;

import static ar.com.lamontana.catalogo.CatalogoController.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
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
    public enum EstadoRevision { VIGENTE, HISTORICA, PROGRAMADA, CANCELADA }
    public record Resumen(UUID codigoPublico,long numero,String motivo,Instant creadaEn,String actor,
                          EstadoRevision estado,Instant programadaPara,Instant activadaEn) {}
    public record Revision(UUID codigoPublico,long numero,String motivo,Instant creadaEn,String actor,List<Tarifa> tarifas,List<OfertaServicio> servicios,
                           EstadoRevision estado,Instant programadaPara,Instant activadaEn) {}
    public record Estado(List<Formato> formatos,List<Papel> papeles,List<Servicio> servicios,Revision actual,List<Resumen> historial,Revision programada,List<PapelesCatalogo.Seleccion> papelesHabilitados,List<PapelesCatalogo.Predefinido> papelesPredefinidos) {}
    private static final String RESUMEN_SQL="SELECT r.codigo_publico,r.id_catalogo_revision,r.motivo,r.creada_en,u.nombre||' '||u.apellido AS actor,r.estado,r.programada_para,r.activada_en FROM lamontana.catalogo_revision r JOIN lamontana.usuario u ON u.id_usuario=r.id_actor";

    @Transactional
    public Estado estado() {
        bloquear(); aplicarVencida();return leerEstado();
    }
    @Transactional(readOnly=true)
    public Estado leerEstado() {
        var actuales=jdbc.query("SELECT codigo_publico FROM lamontana.catalogo_revision WHERE vigente",(rs,row)->rs.getObject(1,UUID.class));
        var programadas=jdbc.query("SELECT codigo_publico FROM lamontana.catalogo_revision WHERE estado='PROGRAMADA'",(rs,row)->rs.getObject(1,UUID.class));
        return new Estado(formatos(),papeles(),servicios(),actuales.isEmpty()?null:cargarRevision(actuales.get(0)),
                jdbc.query(RESUMEN_SQL+" ORDER BY r.id_catalogo_revision DESC LIMIT 50",this::resumen),
                programadas.isEmpty()?null:cargarRevision(programadas.get(0)),new PapelesCatalogo(jdbc).seleccion(),PapelesCatalogo.PREDEFINIDOS);
    }
    private List<Formato> formatos() { return jdbc.query("SELECT codigo_publico,codigo,nombre,ancho_mm,alto_mm FROM lamontana.formato ORDER BY codigo",(r,n)->new Formato(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getBigDecimal(4),r.getBigDecimal(5))); }
    private List<Papel> papeles() { return jdbc.query("SELECT codigo_publico,codigo,nombre,gramaje_g_m2,terminacion_tipo FROM lamontana.papel ORDER BY codigo",(r,n)->new Papel(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getBigDecimal(4),r.getString(5))); }
    private List<Servicio> servicios() { return jdbc.query("SELECT codigo_publico,codigo,nombre,tipo,descripcion FROM lamontana.servicio ORDER BY codigo",(r,n)->new Servicio(r.getObject(1,UUID.class),r.getString(2),r.getString(3),TipoServicio.valueOf(r.getString(4)),r.getString(5))); }

    @Transactional public void predefinido(String codigo,String actor) { bloquear();new PapelesCatalogo(jdbc).predefinido(codigo,actor); }
    @Transactional public void todosPredefinidos(String actor) {
        bloquear();var papeles=new PapelesCatalogo(jdbc);
        for(var predefinido:PapelesCatalogo.PREDEFINIDOS)papeles.predefinido(predefinido.codigo(),actor);
    }
    @Transactional public void personalizado(PapelPersonalizado in,String actor) { bloquear();new PapelesCatalogo(jdbc).personalizado(in,actor); }
    @Transactional public void seleccion(SeleccionPapel in,String actor) { bloquear();new PapelesCatalogo(jdbc).cambiar(in,actor); }

    @Transactional public void formato(AltaFormato in,String actor) {
        bloquear();UUID id=UUID.randomUUID();
        int n=jdbc.update("INSERT INTO lamontana.formato(codigo_publico,codigo,nombre,ancho_mm,alto_mm) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",id,codigo(in.codigo()),in.nombre().strip(),in.anchoMm(),in.altoMm());
        creado(n,"formato");
        jdbc.update("INSERT INTO lamontana.catalogo_papel(id_formato,id_papel) SELECT id_formato,id_papel FROM lamontana.formato CROSS JOIN lamontana.papel WHERE formato.codigo_publico=?",id);
        evento(actor,"ALTA_FORMATO",id);
    }
    @Transactional public void papel(AltaPapel in,String actor) {
        bloquear();UUID id=UUID.randomUUID();
        int n=jdbc.update("INSERT INTO lamontana.papel(codigo_publico,codigo,nombre,gramaje_g_m2,terminacion_tipo) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",id,codigo(in.codigo()),in.nombre().strip(),in.gramaje(),in.terminacion().strip());
        creado(n,"papel");
        jdbc.update("INSERT INTO lamontana.catalogo_papel(id_formato,id_papel) SELECT id_formato,id_papel FROM lamontana.formato CROSS JOIN lamontana.papel WHERE papel.codigo_publico=?",id);
        evento(actor,"ALTA_PAPEL",id);
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

    // Una reconciliación vencida no se revierte por un comando posterior inválido o desactualizado.
    // Todos los rechazos de negocio ocurren antes de escribir la nueva revisión.
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public Revision guardar(NuevaRevision in,String actor) {
        // Una sola revisión efectiva y confirmaciones idempotentes, incluyendo solicitudes simultáneas.
        bloquear(); aplicarVencida();
        String fingerprint=huellaRevision(in);
        var existentes=jdbc.query("SELECT r.codigo_publico,r.hash_solicitud,u.correo FROM lamontana.catalogo_revision r JOIN lamontana.usuario u ON u.id_usuario=r.id_actor WHERE r.id_operacion=?",
                (rs,row)->new String[]{rs.getString(1),rs.getString(2),rs.getString(3)},in.operacion());
        if(!existentes.isEmpty()) {
            var e=existentes.get(0);
            if(!e[1].equals(fingerprint)||!e[2].equals(actor)) throw error(HttpStatus.CONFLICT,"Esa confirmación ya se usó con otros datos. Volvé a revisar el catálogo.");
            return cargarRevision(UUID.fromString(e[0]));
        }
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.catalogo_cancelacion WHERE id_operacion=?)",Boolean.class,in.operacion())))
            throw error(HttpStatus.CONFLICT,"Esa confirmación ya se usó para otra operación.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.catalogo_revision WHERE estado='PROGRAMADA')",Boolean.class)))
            throw error(HttpStatus.CONFLICT,"Hay una revisión comercial programada. Cancelala o esperá su activación antes de guardar otra.");
        if(in.programadaPara()!=null && !in.programadaPara().isAfter(ahora()))
            throw error(HttpStatus.BAD_REQUEST,"La fecha y hora programadas deben ser futuras.");
        var actual=jdbc.query("SELECT codigo_publico FROM lamontana.catalogo_revision WHERE vigente",(rs,row)->rs.getObject(1,UUID.class));
        UUID base=actual.isEmpty()?null:actual.get(0);
        if(!Objects.equals(base,in.versionBase())) throw error(HttpStatus.CONFLICT,"El catálogo cambió desde que lo abriste. Consultá la revisión vigente y revisá tus cambios antes de guardar.");
        validar(in);
        UUID codigo=UUID.randomUUID();
        Long id=jdbc.queryForObject("""
                INSERT INTO lamontana.catalogo_revision(codigo_publico,id_revision_base,id_operacion,hash_solicitud,motivo,id_actor,estado,programada_para,activada_en)
                VALUES (?,(SELECT id_catalogo_revision FROM lamontana.catalogo_revision WHERE codigo_publico=?),?,?,?,(SELECT id_usuario FROM lamontana.usuario WHERE correo=?),?,?,?) RETURNING id_catalogo_revision
                """,Long.class,codigo,base,in.operacion(),fingerprint,in.motivo().strip(),actor,
                in.programadaPara()==null?"HISTORICA":"PROGRAMADA",timestamp(in.programadaPara()),in.programadaPara()==null?timestamp(ahora()):null);
        for(Tarifa t:in.tarifas()) jdbc.update("""
                INSERT INTO lamontana.tarifa_impresion(id_catalogo_revision,id_formato,id_papel,modo_color,precio_por_carilla,recargo_doble_faz,habilitada,grupo,nombre,modo_doble_faz,valor_doble_faz)
                VALUES (?,(SELECT id_formato FROM lamontana.formato WHERE codigo_publico=?),(SELECT id_papel FROM lamontana.papel WHERE codigo_publico=?),?,?,?,?,?,?,?,?)
                """,id,t.formato(),t.papel(),t.color().name(),t.precio(),t.recargoDobleFaz(),t.habilitada(),t.grupo(),t.nombre()==null?null:t.nombre().strip(),t.modoDobleFaz()==null?null:t.modoDobleFaz().name(),t.valorDobleFaz());
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
        if(in.programadaPara()==null) {
            publicar(id);
            evento(actor,"REVISION_ACTIVADA",codigo);
        } else evento(actor,"REVISION_PROGRAMADA",codigo);
        return cargarRevision(codigo);
    }

    @Transactional(noRollbackFor=ResponseStatusException.class)
    public Revision cancelar(UUID codigo,CancelarProgramacion in,String actor) {
        bloquear(); aplicarVencida();
        String fingerprint=hash(codigo+"\n"+json.writeValueAsString(in));
        var existentes=jdbc.query("SELECT r.codigo_publico,c.hash_solicitud,u.correo FROM lamontana.catalogo_cancelacion c JOIN lamontana.catalogo_revision r USING(id_catalogo_revision) JOIN lamontana.usuario u ON u.id_usuario=c.id_actor WHERE c.id_operacion=?",
                (rs,row)->new String[]{rs.getString(1),rs.getString(2),rs.getString(3)},in.operacion());
        if(!existentes.isEmpty()) {
            var e=existentes.get(0);
            if(!e[0].equals(codigo.toString())||!e[1].equals(fingerprint)||!e[2].equals(actor))
                throw error(HttpStatus.CONFLICT,"Esa confirmación ya se usó con otros datos.");
            return cargarRevision(codigo);
        }
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.catalogo_revision WHERE id_operacion=?)",Boolean.class,in.operacion())))
            throw error(HttpStatus.CONFLICT,"Esa confirmación ya se usó para otra operación.");
        Revision revision=cargarRevision(codigo);
        if(revision.estado()!=EstadoRevision.PROGRAMADA)
            throw error(HttpStatus.CONFLICT,"Sólo puede cancelarse una revisión todavía programada; la revisión vigente permanece sin cambios.");
        jdbc.update("INSERT INTO lamontana.catalogo_cancelacion(id_operacion,id_catalogo_revision,hash_solicitud,id_actor,motivo) VALUES (?,?,?,(SELECT id_usuario FROM lamontana.usuario WHERE correo=?),?)",
                in.operacion(),revision.numero(),fingerprint,actor,in.motivo().strip());
        jdbc.update("UPDATE lamontana.catalogo_revision SET estado='CANCELADA',cancelada_en=clock_timestamp() WHERE id_catalogo_revision=?",revision.numero());
        evento(actor,"PROGRAMACION_CANCELADA",codigo);
        return cargarRevision(codigo);
    }

    @Transactional
    public void reconciliarProgramaciones() { bloquear(); aplicarVencida(); }

    private void bloquear() { jdbc.execute("SELECT pg_advisory_xact_lock(764002)"); }
    private Instant ahora() { return jdbc.queryForObject("SELECT clock_timestamp()",Timestamp.class).toInstant(); }
    private Timestamp timestamp(Instant instante) { return instante==null?null:Timestamp.from(instante); }
    private void publicar(long id) {
        jdbc.update("UPDATE lamontana.catalogo_revision SET vigente=false,estado='HISTORICA' WHERE vigente");
        jdbc.update("UPDATE lamontana.catalogo_revision SET vigente=true,estado='VIGENTE',activada_en=clock_timestamp() WHERE id_catalogo_revision=?",id);
    }
    private void aplicarVencida() {
        var pendientes=jdbc.query("SELECT id_catalogo_revision,codigo_publico,id_actor FROM lamontana.catalogo_revision WHERE estado='PROGRAMADA' AND programada_para<=clock_timestamp()",
                (rs,row)->new Pendiente(rs.getLong(1),rs.getObject(2,UUID.class),rs.getLong(3)));
        if(pendientes.isEmpty()) return;
        Pendiente pendiente=pendientes.get(0);
        publicar(pendiente.id());
        jdbc.update("INSERT INTO lamontana.evento_catalogo(id_actor,tipo,codigo_objeto) VALUES (?,'PROGRAMACION_APLICADA',?)",pendiente.actor(),pendiente.codigo());
    }
    private record Pendiente(long id,UUID codigo,long actor) {}
    // Conserva las huellas de comandos inmediatos almacenadas por V7 antes de añadir programadaPara.
    private record RevisionInmediata(UUID versionBase,UUID operacion,String motivo,List<Tarifa> tarifas,List<OfertaServicio> servicios) {}
    private String huellaRevision(NuevaRevision in) {
        String original=json.writeValueAsString(new RevisionInmediata(in.versionBase(),in.operacion(),in.motivo(),in.tarifas(),in.servicios()));
        return hash(original+(in.programadaPara()==null?"":"\nPROGRAMADA:"+in.programadaPara()));
    }

    private void validar(NuevaRevision in) {
        Set<UUID> formatos=new HashSet<>(),papeles=new HashSet<>();Map<UUID,TipoServicio> servicios=new HashMap<>();
        formatos().forEach(f->formatos.add(f.codigoPublico()));papeles().forEach(p->papeles.add(p.codigoPublico()));servicios().forEach(s->servicios.put(s.codigoPublico(),s.tipo()));
        var seleccionados=new HashSet<Compatibilidad>();
        new PapelesCatalogo(jdbc).seleccion().stream().filter(PapelesCatalogo.Seleccion::habilitado).forEach(p->seleccionados.add(new Compatibilidad(p.formato(),p.papel())));
        var combinaciones=new HashSet<String>();var disponibles=new HashSet<Compatibilidad>();
        var grupos=new HashMap<UUID,List<Object>>();
        for(Tarifa t:in.tarifas()) {
            PreciosTarifa.validar(t);
            if(t.grupo()!=null) {
                var firma=PreciosTarifa.firma(t);
                var anterior=grupos.putIfAbsent(t.grupo(),firma);
                existe(anterior==null||anterior.equals(firma),"Las variantes de una tarifa deben compartir nombre, color, precios y estado.");
            }
            existe(combinacionExiste(formatos,papeles,t.formato(),t.papel()),"La tarifa debe referir a un formato y papel existentes.");
            existe(combinaciones.add(t.formato()+"/"+t.papel()+"/"+t.color()),"Hay tarifas duplicadas para la misma combinación.");
            if(t.habilitada()) {
                existe(seleccionados.contains(new Compatibilidad(t.formato(),t.papel())),"Una tarifa habilitada usa un papel retirado del catálogo base. Volvé a habilitarlo o desmarcá esa tarifa antes de guardar.");
                disponibles.add(new Compatibilidad(t.formato(),t.papel()));
            }
        }
        existe(!disponibles.isEmpty(),"Configurá al menos una tarifa de impresión habilitada.");
        var usados=new HashSet<UUID>();int impresiones=0;
        for(OfertaServicio s:in.servicios()) {
            TipoServicio tipo=servicios.get(s.servicio());
            existe(tipo!=null,"El servicio no existe en el catálogo.");
            existe(usados.add(s.servicio()),"Hay servicios duplicados en esta revisión.");
            if(tipo==TipoServicio.IMPRESION) {
                existe(s.basePrecio()==BasePrecio.POR_CARILLA && s.precio().signum()==0,"El precio de impresión se toma de las tarifas; no admite un segundo cargo de servicio.");
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

    @Transactional(noRollbackFor=ResponseStatusException.class)
    public Revision revision(UUID codigo) {
        bloquear(); aplicarVencida();
        return cargarRevision(codigo);
    }
    private Resumen resumen(ResultSet rs,int row) throws SQLException {
        Timestamp programada=rs.getTimestamp("programada_para"),activada=rs.getTimestamp("activada_en");
        return new Resumen(rs.getObject("codigo_publico",UUID.class),rs.getLong("id_catalogo_revision"),rs.getString("motivo"),rs.getTimestamp("creada_en").toInstant(),rs.getString("actor"),
                EstadoRevision.valueOf(rs.getString("estado")),programada==null?null:programada.toInstant(),activada==null?null:activada.toInstant());
    }
    private Revision cargarRevision(UUID codigo) {
        var result=jdbc.query(RESUMEN_SQL+" WHERE r.codigo_publico=?",this::resumen,codigo);
        if(result.isEmpty()) throw error(HttpStatus.NOT_FOUND,"La revisión comercial no existe.");
        Resumen r=result.get(0);long id=r.numero();
        var tarifas=jdbc.query("""
                SELECT f.codigo_publico,p.codigo_publico,t.modo_color,t.precio_por_carilla,t.recargo_doble_faz,t.habilitada,t.grupo,t.nombre,t.modo_doble_faz,t.valor_doble_faz
                FROM lamontana.tarifa_impresion t JOIN lamontana.formato f USING(id_formato) JOIN lamontana.papel p USING(id_papel)
                WHERE t.id_catalogo_revision=? ORDER BY f.codigo,p.codigo,t.modo_color
                """,(rs,row)->new Tarifa(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),ModoColor.valueOf(rs.getString(3)),rs.getBigDecimal(4),rs.getBigDecimal(5),rs.getBoolean(6),rs.getObject(7,UUID.class),rs.getString(8),rs.getString(9)==null?null:ModoDobleFaz.valueOf(rs.getString(9)),rs.getBigDecimal(10)),id);
        var ofertas=jdbc.query("""
                SELECT c.id_configuracion_servicio,s.codigo_publico,c.nombre_visible,c.base_precio,c.precio_unitario,c.preparacion_minutos,c.habilitado
                FROM lamontana.configuracion_servicio c JOIN lamontana.servicio s USING(id_servicio)
                WHERE c.id_catalogo_revision=? ORDER BY s.codigo
                """,(rs,row)->new OfertaServicio(rs.getObject(2,UUID.class),rs.getString(3),BasePrecio.valueOf(rs.getString(4)),rs.getBigDecimal(5),rs.getInt(6),rs.getBoolean(7),
                        jdbc.query("SELECT f.codigo_publico,p.codigo_publico FROM lamontana.compatibilidad_servicio x JOIN lamontana.formato f USING(id_formato) JOIN lamontana.papel p USING(id_papel) WHERE x.id_configuracion_servicio=? ORDER BY f.codigo,p.codigo",
                                (c,n)->new Compatibilidad(c.getObject(1,UUID.class),c.getObject(2,UUID.class)),rs.getLong(1))),id);
        return new Revision(codigo,id,r.motivo(),r.creadaEn(),r.actor(),tarifas,ofertas,r.estado(),r.programadaPara(),r.activadaEn());
    }
    private ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
    private String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
