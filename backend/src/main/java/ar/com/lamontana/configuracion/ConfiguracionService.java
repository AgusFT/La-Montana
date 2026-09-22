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
    private final JsonMapper json=JsonMapper.builder().build();
    public ConfiguracionService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public record Borrador(UUID codigoPublico,long numero,long version,String estado,Modelo modelo,Criterio criterio,
                           Instant creadaEn,Instant actualizadaEn,String actor,Instant canceladaEn,String motivoCancelacion,String cancelador,Pagos pagos) {}
    public record Estado(Borrador borrador,List<Borrador> historial) {}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Estado estado(String correo) {
        propietario(correo);
        var borradores=jdbc.query(BORRADOR_SQL+" WHERE c.estado='EN_PREPARACION'",this::mapear);
        return new Estado(borradores.isEmpty()?null:borradores.get(0),
                jdbc.query(BORRADOR_SQL+" WHERE c.estado='CANCELADA' ORDER BY c.fecha_cancelacion DESC,c.numero_version DESC LIMIT 50",this::mapear));
    }

    @Transactional
    public Borrador crear(CrearBorrador input,String correo) {
        bloquear();long actor=propietario(correo);String huella=huella(input);
        Borrador repetido=reintento(input.operacion(),CREAR,null,actor,huella);
        if(repetido!=null) return repetido;
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE estado='EN_PREPARACION')",Boolean.class)))
            throw error(HttpStatus.CONFLICT,"Ya hay una configuración en preparación. Retomá el borrador existente.");
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
            if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.autorizacion_configuracion WHERE id_operacion=?)",Boolean.class,operacion)))
                throw error(HttpStatus.CONFLICT,"Esa operación ya se usó para una autorización de cancelación.");
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
        jdbc.update("INSERT INTO lamontana.comprobante_configuracion(id_operacion,tipo,id_configuracion_version,id_actor,hash_solicitud) VALUES (?,?,?,?,?)",operacion,tipo,id,actor,huella);
        jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version) VALUES (?,?,?,?)",id,actor,evento,version);
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
                rs.getTimestamp("fecha_cancelacion")==null?null:rs.getTimestamp("fecha_cancelacion").toInstant(),rs.getString("motivo_cancelacion"),rs.getString("cancelador"),pagos(rs.getLong("id_configuracion_version")));
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
