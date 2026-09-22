package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.ConfiguracionController.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ConfiguracionService {
    private static final String CREAR="CREAR_BORRADOR",SELECCIONAR="GUARDAR_MODELO";
    private static final String BORRADOR_SQL="""
            SELECT c.id_configuracion_version,c.codigo_publico,c.numero_version,c.version,c.estado,c.modelo,c.criterio,c.fecha_creacion,c.fecha_actualizacion,
                   u.nombre||' '||u.apellido AS actor,c.fecha_cancelacion,c.motivo_cancelacion,
                   cancelador.nombre||' '||cancelador.apellido AS cancelador
            FROM lamontana.configuracion_version c JOIN lamontana.usuario u ON u.id_usuario=c.id_usuario_creador
            LEFT JOIN lamontana.usuario cancelador ON cancelador.id_usuario=c.id_usuario_cancelador
            """;
    private final JdbcTemplate jdbc;
    private final RecursosRepositorio recursos;
    private final EntregaRepositorio entrega;
    private final JsonMapper json=JsonMapper.builder().build();
    public ConfiguracionService(JdbcTemplate jdbc,RecursosRepositorio recursos,EntregaRepositorio entrega) { this.jdbc=jdbc;this.recursos=recursos;this.entrega=entrega; }

    public record Borrador(UUID codigoPublico,long numero,long version,String estado,Modelo modelo,Criterio criterio,
                           Instant creadaEn,Instant actualizadaEn,String actor,Instant canceladaEn,String motivoCancelacion,String cancelador,Pagos pagos,RecursosRepositorio.Recursos recursos,EntregaRepositorio.Entrega entrega,Copia copia) {}
    public record Copia(UUID origen,long numeroOrigen,Instant creadaEn,String tipo,List<Integer> fasesConfirmadas,List<RevisionConfiguracionService.Hallazgo> barridoInicial) {}
    public record Version(Borrador configuracion,Instant activadaEn,Instant finVigencia,String activador,String motivo,UUID predecesora,UUID revisionComercial) {}
    public record Intento(UUID codigo,UUID configuracion,long numero,String origen,String estado,Instant iniciadoEn,Instant terminadoEn,Instant atrasoDetectadoEn,String resultado,String detalle,String operacion,UUID resultante,Long numeroResultante) {}
    public record Programacion(Borrador configuracion,Instant confirmadaEn,Instant previstaEn,String zonaHoraria,String programador,String motivo,Instant proximoIntentoEn,List<Intento> intentos) {}
    public record Estado(Borrador borrador,List<Borrador> historial,Version activa,Programacion programada,List<Intento> intentos) {}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Estado estado(String correo) {
        propietario(correo);
        var borradores=jdbc.query(BORRADOR_SQL+" WHERE c.estado='EN_PREPARACION'",this::mapear);
        return new Estado(borradores.isEmpty()?null:borradores.get(0),
                jdbc.query(BORRADOR_SQL+" WHERE c.estado='CANCELADA' ORDER BY c.fecha_cancelacion DESC,c.numero_version DESC LIMIT 50",this::mapear),activa(),programada(),intentos(null));
    }

    private static final String INTENTO_SQL="SELECT i.*,c.codigo_publico,c.numero_version,v.codigo_publico AS resultante,v.numero_version AS numero_resultante FROM lamontana.intento_activacion_configuracion i JOIN lamontana.configuracion_version c ON c.id_configuracion_version=i.id_version_objetivo LEFT JOIN lamontana.configuracion_version v ON v.id_configuracion_version=i.id_version_resultante ";
    List<Intento> intentos(UUID codigo){return jdbc.query(INTENTO_SQL+"WHERE (?::uuid IS NULL OR c.codigo_publico=?) ORDER BY i.fecha_inicio DESC LIMIT 50",this::mapearIntento,codigo,codigo);}
    Intento intentoPorOperacion(UUID operacion){var rows=jdbc.query(INTENTO_SQL+"WHERE i.id_operacion=?",this::mapearIntento,operacion);return rows.isEmpty()?null:rows.get(0);}
    private Intento mapearIntento(ResultSet r,int fila)throws SQLException{return new Intento(r.getObject("id_intento",UUID.class),r.getObject("codigo_publico",UUID.class),r.getLong("numero_version"),r.getString("origen"),r.getString("estado"),r.getTimestamp("fecha_inicio").toInstant(),r.getTimestamp("fecha_fin")==null?null:r.getTimestamp("fecha_fin").toInstant(),r.getTimestamp("fecha_atraso_detectado")==null?null:r.getTimestamp("fecha_atraso_detectado").toInstant(),r.getString("codigo_resultado"),r.getString("detalle_sanitizado"),r.getString("operacion"),r.getObject("resultante",UUID.class),r.getObject("numero_resultante",Long.class));}
    Programacion programada(){var ids=jdbc.query("SELECT codigo_publico FROM lamontana.configuracion_version WHERE estado='PROGRAMADA'",(r,n)->r.getObject(1,UUID.class));return ids.isEmpty()?null:programacion(ids.get(0));}
    Programacion programacion(UUID codigo){
        var b=cargar(codigo);var intentos=intentos(codigo);
        var rows=jdbc.query("""
            SELECT p.*,u.nombre||' '||u.apellido AS actor FROM lamontana.programacion_configuracion p
            JOIN lamontana.usuario u ON u.id_usuario=p.id_actor JOIN lamontana.configuracion_version c USING(id_configuracion_version) WHERE c.codigo_publico=?
            """,(r,n)->{
                Instant confirmada=r.getTimestamp("fecha_confirmacion").toInstant(),prevista=r.getTimestamp("fecha_programada").toInstant(),proxima=prevista;
                // Un fallo inmediato anterior no inicia el ciclo de una programación creada después.
                var actuales=intentos.stream().filter(i->!i.iniciadoEn().isBefore(confirmada)).toList();
                if(!b.estado().equals("PROGRAMADA")||!actuales.isEmpty()&&actuales.get(0).estado().equals("INICIADO"))proxima=null;
                else if(!actuales.isEmpty()&&actuales.get(0).estado().equals("FALLIDO"))proxima=actuales.get(0).terminadoEn().plusSeconds(600);
                return new Programacion(b,r.getTimestamp("fecha_confirmacion").toInstant(),prevista,r.getString("zona_horaria"),r.getString("actor"),r.getString("motivo"),proxima,intentos);
            },codigo);
        if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"No hay una programación para esta configuración.");return rows.get(0);
    }

    Version activa(){var ids=jdbc.query("SELECT codigo_publico FROM lamontana.configuracion_version WHERE estado='ACTIVA'",(r,n)->r.getObject(1,UUID.class));return ids.isEmpty()?null:versionActivada(ids.get(0));}
    Version versionActivada(UUID codigo){
        var b=cargar(codigo);var rows=jdbc.query("""
            SELECT a.fecha_activacion,a.fin_vigencia,coalesce(u.nombre||' '||u.apellido,'Sistema automático') AS actor,a.motivo,p.codigo_publico AS predecesora,r.codigo_publico AS revision
            FROM lamontana.activacion_configuracion a JOIN lamontana.configuracion_version c USING(id_configuracion_version)
            LEFT JOIN lamontana.usuario u ON u.id_usuario=a.id_actor JOIN lamontana.catalogo_revision r USING(id_catalogo_revision)
            LEFT JOIN lamontana.configuracion_version p ON p.id_configuracion_version=a.id_predecesora WHERE c.codigo_publico=?
            """,(r,n)->new Version(b,r.getTimestamp("fecha_activacion").toInstant(),r.getTimestamp("fin_vigencia")==null?null:r.getTimestamp("fin_vigencia").toInstant(),r.getString("actor"),r.getString("motivo"),r.getObject("predecesora",UUID.class),r.getObject("revision",UUID.class)),codigo);
        if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"La configuración no tiene una activación registrada.");return rows.get(0);
    }

    @Transactional
    public Borrador crear(CrearBorrador input,String correo) {
        bloquear();long actor=propietario(correo);String huella=huella(input);
        Borrador repetido=reintento(input.operacion(),CREAR,null,actor,huella);
        if(repetido!=null) return repetido;
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE estado IN ('EN_PREPARACION','PROGRAMADA'))",Boolean.class)))
            throw error(HttpStatus.CONFLICT,"Ya hay una configuración pendiente. Retomá el borrador o consultá la programación existente.");
        UUID codigo=UUID.randomUUID();
        long id=jdbc.queryForObject("""
                INSERT INTO lamontana.configuracion_version(codigo_publico,numero_version,id_usuario_creador,estado)
                VALUES (?,(SELECT coalesce(max(numero_version),0)+1 FROM lamontana.configuracion_version),?,'EN_PREPARACION')
                RETURNING id_configuracion_version
                """,Long.class,codigo,actor);
        registrar(input.operacion(),CREAR,id,actor,huella,"BORRADOR_CREADO",1);
        return cargar(codigo);
    }

    @Transactional
    public Borrador seleccionar(UUID codigo,SeleccionarModelo input,String correo) {
        bloquear();long actor=propietario(correo);String huella=huella(input);
        Borrador repetido=reintento(input.operacion(),SELECCIONAR,codigo,actor,huella);
        if(repetido!=null) return repetido;
        Borrador actual=cargar(codigo);
        if(!actual.estado().equals("EN_PREPARACION"))
            throw error(HttpStatus.CONFLICT,"Sólo se puede editar una configuración en preparación.");
        if(input.version()==null||actual.version()!=input.version())
            throw error(HttpStatus.CONFLICT,"El borrador cambió desde que lo abriste. Consultá su versión actual antes de guardar.");
        if(input.modelo()==null||(input.modelo()==Modelo.MANUAL&&input.criterio()!=null)
                ||(input.modelo()==Modelo.CONDICIONAL&&input.criterio()==null))
            throw error(HttpStatus.BAD_REQUEST,"El modelo manual no lleva criterio; el condicional exige un criterio de revisión.");
        long id=jdbc.queryForObject("""
                UPDATE lamontana.configuracion_version SET modelo=?,criterio=?,version=version+1,fecha_actualizacion=clock_timestamp()
                WHERE codigo_publico=? RETURNING id_configuracion_version
                """,Long.class,input.modelo().name(),input.criterio()==null?null:input.criterio().name(),codigo);
        if(actual.modelo()!=input.modelo()||actual.criterio()!=input.criterio())
            jdbc.update("DELETE FROM lamontana.configuracion_financiera WHERE id_configuracion_version=?",id);
        registrar(input.operacion(),SELECCIONAR,id,actor,huella,"MODELO_SELECCIONADO",actual.version()+1);
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",id);
        return cargar(codigo);
    }

    void sinIntentoEnCurso(){if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.intento_activacion_configuracion WHERE estado='INICIADO')",Boolean.class)))throw error(HttpStatus.CONFLICT,"Hay una activación en curso. Consultá su resultado antes de editar o autorizar otra acción.");}
    void bloquear() { jdbc.execute("SELECT pg_advisory_xact_lock(764003)"); }
    long propietario(String correo) {
        var actores=jdbc.query("""
                SELECT u.id_usuario FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                WHERE u.correo=? AND u.estado='ACTIVO' AND u.es_administrador_propietario AND r.codigo='ADMIN_ADMIN' AND r.activo
                """,(rs,row)->rs.getLong(1),correo);
        if(actores.isEmpty()) throw error(HttpStatus.FORBIDDEN,"Sólo el propietario activo puede preparar la configuración operativa.");
        return actores.get(0);
    }
    // Los comprobantes impiden reaplicar una operación; el replay devuelve la versión actual del mismo borrador.
    Borrador reintento(UUID operacion,String tipo,UUID destino,long actor,String huella) {
        var encontrados=jdbc.query("""
                SELECT p.tipo,c.codigo_publico,p.id_actor,p.hash_solicitud
                FROM lamontana.comprobante_configuracion p JOIN lamontana.configuracion_version c USING(id_configuracion_version)
                WHERE p.id_operacion=?
                """,(rs,row)->new Comprobante(rs.getString(1),rs.getObject(2,UUID.class),rs.getLong(3),rs.getString(4)),operacion);
        if(encontrados.isEmpty()) {
            sinIntentoEnCurso();
            if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.autorizacion_configuracion WHERE id_operacion=?)",Boolean.class,operacion)))
                throw error(HttpStatus.CONFLICT,"Esa operación ya se usó para una autorización de configuración.");
            return null;
        }
        var comprobante=encontrados.get(0);
        if(!comprobante.tipo().equals(tipo)||(destino!=null&&!comprobante.destino().equals(destino))
                ||comprobante.actor()!=actor||!comprobante.huella().equals(huella))
            throw error(HttpStatus.CONFLICT,"Esa operación ya se usó con otro contenido, destino o actor.");
        return cargar(comprobante.destino());
    }
    private record Comprobante(String tipo,UUID destino,long actor,String huella) {}
    void registrar(UUID operacion,String tipo,long id,long actor,String huella,String evento,long version) {
        registrar(operacion,tipo,id,actor,huella,evento,version,null,null);
    }
    void registrar(UUID operacion,String tipo,long id,long actor,String huella,String evento,long version,UUID recurso,String motivo) {
        jdbc.update("INSERT INTO lamontana.comprobante_configuracion(id_operacion,tipo,id_configuracion_version,id_actor,hash_solicitud) VALUES (?,?,?,?,?)",operacion,tipo,id,actor,huella);
        jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version,codigo_recurso,motivo) VALUES (?,?,?,?,?,?)",id,actor,evento,version,recurso,motivo);
        reconfirmarGuardado(id,evento,actor,version);
    }
    Borrador cargar(UUID codigo) {
        var result=jdbc.query(BORRADOR_SQL+" WHERE c.codigo_publico=?",this::mapear,codigo);
        if(result.isEmpty()) throw error(HttpStatus.NOT_FOUND,"El borrador de configuración no existe.");
        return result.get(0);
    }
    private Borrador mapear(ResultSet rs,int fila) throws SQLException {
        String modelo=rs.getString("modelo"),criterio=rs.getString("criterio");
        return new Borrador(rs.getObject("codigo_publico",UUID.class),rs.getLong("numero_version"),rs.getLong("version"),rs.getString("estado"),
                modelo==null?null:Modelo.valueOf(modelo),criterio==null?null:Criterio.valueOf(criterio),
                rs.getTimestamp("fecha_creacion").toInstant(),rs.getTimestamp("fecha_actualizacion").toInstant(),rs.getString("actor"),
                rs.getTimestamp("fecha_cancelacion")==null?null:rs.getTimestamp("fecha_cancelacion").toInstant(),rs.getString("motivo_cancelacion"),rs.getString("cancelador"),pagos(rs.getLong("id_configuracion_version")),recursos.leer(rs.getLong("id_configuracion_version")),entrega.leer(rs.getLong("id_configuracion_version")),copia(rs.getLong("id_configuracion_version")));
    }
    Copia copia(long id){var rows=jdbc.query("SELECT v.codigo_publico,v.numero_version,o.creada_en,o.barrido_inicial::text,o.tipo FROM lamontana.origen_configuracion o JOIN lamontana.configuracion_version v ON v.id_configuracion_version=o.id_version_origen WHERE o.id_configuracion_version=?",(r,n)->new Copia(r.getObject(1,UUID.class),r.getLong(2),r.getTimestamp(3).toInstant(),r.getString(5),jdbc.query("SELECT fase FROM lamontana.reconfirmacion_configuracion WHERE id_configuracion_version=? ORDER BY fase",(f,k)->f.getInt(1),id),java.util.Arrays.asList(json.readValue(r.getString(4),RevisionConfiguracionService.Hallazgo[].class))),id);return rows.isEmpty()?null:rows.get(0);}
    private void reconfirmarGuardado(long id,String evento,long actor,long version){
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.origen_configuracion WHERE id_configuracion_version=?)",Boolean.class,id)))return;
        int fase=switch(evento){case "MODELO_SELECCIONADO"->2;case "PAGOS_CONFIGURADOS"->3;case "RECURSOS_CONFIGURADOS","IMPRESORA_CREADA","IMPRESORA_EDITADA","IMPRESORA_ESTADO_CAMBIADO","IMPRESORA_RETIRADA"->4;case "ENTREGA_CONFIGURADA","PUNTO_CREADO","PUNTO_EDITADO","ZONA_CREADA","ZONA_EDITADA"->5;default->0;};
        if(fase==0)return;
        jdbc.update("DELETE FROM lamontana.reconfirmacion_configuracion WHERE id_configuracion_version=? AND fase>=?",id,fase);
        if(java.util.Set.of("MODELO_SELECCIONADO","PAGOS_CONFIGURADOS","RECURSOS_CONFIGURADOS","ENTREGA_CONFIGURADA").contains(evento))jdbc.update("INSERT INTO lamontana.reconfirmacion_configuracion(id_configuracion_version,fase,id_actor,version) VALUES (?,?,?,?)",id,fase,actor,version);
    }
    void exigirRevisionCopia(Borrador b,String huella,boolean programada){
        if(b.copia()==null||!b.copia().tipo().equals("USO_COMO_BASE"))return;
        if(!b.copia().fasesConfirmadas().containsAll(List.of(2,3,4,5,6)))throw error(HttpStatus.CONFLICT,"La copia requiere revisar y confirmar nuevamente las fases 2 a 6.");
        if(!programada&&!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.reconfirmacion_configuracion f JOIN lamontana.configuracion_version c USING(id_configuracion_version) WHERE c.codigo_publico=? AND f.fase=6 AND f.version=c.version AND f.huella_revision=?)",Boolean.class,b.codigoPublico(),huella)))throw error(HttpStatus.CONFLICT,"La revisión confirmada de la copia cambió. Volvé a fase 6 y confirmá las condiciones actuales.");
    }
    private Pagos pagos(long id) {
        var result=jdbc.query("SELECT * FROM lamontana.configuracion_financiera WHERE id_configuracion_version=?",(rs,row)->{
            String condicion=rs.getString("condicion_sena"),tipo=rs.getString("tipo_sena");
            BigDecimal umbral=rs.getBigDecimal("umbral_sena"),valor=rs.getBigDecimal("valor_sena");
            var medios=jdbc.query("SELECT medio_pago FROM lamontana.configuracion_medio_pago WHERE id_configuracion_version=? ORDER BY medio_pago",(m,n)->MedioPago.valueOf(m.getString(1)),id);
            return new Pagos(medios,rs.getString("instrucciones_transferencia"),rs.getInt("vigencia_cotizacion_minutos"),rs.getBoolean("exigir_sena"),
                    condicion==null?null:CondicionSena.valueOf(condicion),umbral==null?null:condicion.equals("DESDE_CARILLAS")?umbral.toBigIntegerExact().toString():dinero(umbral),
                    tipo==null?null:TipoSena.valueOf(tipo),valor==null?null:tipo.equals("PORCENTAJE")?valor.stripTrailingZeros().toPlainString():dinero(valor),dinero(rs.getBigDecimal("umbral_aprobacion")));
        },id);
        return result.isEmpty()?null:result.get(0);
    }
    private String dinero(BigDecimal valor) { return valor==null?null:valor.setScale(2).toPlainString(); }
    String huella(Object input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(input).getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private ResponseStatusException error(HttpStatus status,String mensaje) { return new ResponseStatusException(status,mensaje); }
}
