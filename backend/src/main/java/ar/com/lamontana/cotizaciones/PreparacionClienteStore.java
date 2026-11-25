package ar.com.lamontana.cotizaciones;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** Un espacio privado por cliente. No es una configuración administrativa ni un pedido. */
@Service
public class PreparacionClienteStore {
    final JdbcTemplate jdbc;final JsonMapper json=JsonMapper.builder().build();
    public PreparacionClienteStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public JsonMapper mapper(){return json;}
    public record Datos(long id,UUID codigoPublico,long usuario,long version,String estado,String items,String presupuesto,String preferencias,UUID cotizacion,UUID pedido){}
    public void bloquear(){for(long id:new long[]{764003,764001,764002,764004})jdbc.execute("SELECT pg_advisory_xact_lock("+id+")");}
    public long cliente(String correo){return jdbc.query("SELECT u.id_usuario FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.codigo='CLIENTE' AND r.activo",(r,n)->r.getLong(1),correo).stream().findFirst().orElseThrow(()->error(HttpStatus.FORBIDDEN,"Se requiere una cuenta activa de cliente."));}
    public Datos datos(UUID codigo,String correo,boolean editable){
        long usuario=cliente(correo);
        var d=jdbc.query("SELECT p.*,c.codigo_publico AS cotizacion,ped.codigo_publico AS pedido FROM lamontana.preparacion_cliente p LEFT JOIN lamontana.cotizacion c ON c.id_cotizacion=p.id_ultima_cotizacion LEFT JOIN lamontana.pedido ped ON ped.id_pedido=p.id_pedido WHERE p.codigo_publico=? AND p.id_usuario=?",(r,n)->new Datos(r.getLong("id_preparacion"),codigo,usuario,r.getLong("version"),r.getString("estado"),r.getString("items"),r.getString("presupuesto"),r.getString("preferencias"),r.getObject("cotizacion",UUID.class),r.getObject("pedido",UUID.class)),codigo,usuario).stream().findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"El trabajo preparado no está disponible en tu cuenta."));
        if(editable&&!d.estado().equals("ABIERTA"))throw conflicto("Este trabajo ya tiene un pedido; consultá su seguimiento.");return d;
    }
    @Transactional public Datos abrir(String correo){bloquear();long usuario=cliente(correo);var ids=jdbc.query("SELECT codigo_publico FROM lamontana.preparacion_cliente WHERE id_usuario=? AND estado='ABIERTA'",(r,n)->r.getObject(1,UUID.class),usuario);UUID id=ids.isEmpty()?UUID.randomUUID():ids.get(0);if(ids.isEmpty())jdbc.update("INSERT INTO lamontana.preparacion_cliente(codigo_publico,id_usuario) VALUES (?,?)",id,usuario);return datos(id,correo,true);}
    public void version(Datos d,long version){if(d.version()!=version)throw conflicto("El trabajo cambió en otra pestaña. Actualizá antes de continuar.");}
    public void cambio(long id,boolean invalida){jdbc.update("UPDATE lamontana.preparacion_cliente SET version=version+1,actualizada_en=clock_timestamp(),presupuesto=CASE WHEN ? THEN NULL ELSE presupuesto END,preferencias=CASE WHEN ? THEN NULL ELSE preferencias END WHERE id_preparacion=?",invalida,invalida,id);}
    public String huella(Object value){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(value).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public String previo(Datos d,UUID operacion,String tipo,Object input){var rows=jdbc.query("SELECT id_preparacion,id_actor,tipo,huella,resultado::text FROM lamontana.evento_preparacion WHERE id_operacion=?",(r,n)->new Object[]{r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5)},operacion);if(rows.isEmpty())return null;var p=rows.get(0);if((long)p[0]!=d.id()||(long)p[1]!=d.usuario()||!tipo.equals(p[2])||!huella(input).equals(p[3]))throw conflicto("La operación ya se utilizó con otros datos.");return (String)p[4];}
    public void evento(Datos d,UUID operacion,String tipo,Object input,Object resultado){jdbc.update("INSERT INTO lamontana.evento_preparacion(id_preparacion,id_actor,id_operacion,tipo,huella,resultado) VALUES (?,?,?,?,?,?::jsonb)",d.id(),d.usuario(),operacion,tipo,huella(input),json.writeValueAsString(resultado));}
    public static ResponseStatusException conflicto(String mensaje){return error(HttpStatus.CONFLICT,mensaje);}
    public static ResponseStatusException error(HttpStatus status,String mensaje){return new ResponseStatusException(status,mensaje);}
}
