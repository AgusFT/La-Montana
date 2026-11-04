package ar.com.lamontana.cotizaciones;

import java.util.*;
import ar.com.lamontana.organizacion.OrganizacionService;
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
    private final OrganizacionService organizacion;
    public PreparacionClienteStore(JdbcTemplate jdbc,OrganizacionService organizacion){this.jdbc=jdbc;this.organizacion=organizacion;}
    public JsonMapper mapper(){return json;}
    public record Cliente(String nombre,String correo,boolean invitado){}
    public record Datos(long id,UUID codigoPublico,long usuario,Long operador,Cliente cliente,UUID sucursalMostrador,long version,String estado,String items,String presupuesto,String preferencias,UUID cotizacion,UUID pedido){
        public long actor(){return operador==null?usuario:operador;}
        public Long preparacionInterna(){return operador==null?null:id;}
    }
    public void bloquear(){for(long id:new long[]{764003,764001,764002,764004})jdbc.execute("SELECT pg_advisory_xact_lock("+id+")");}
    private long cliente(String correo){return jdbc.query("SELECT u.id_usuario FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.codigo='CLIENTE' AND r.activo",(r,n)->r.getLong(1),correo).stream().findFirst().orElseThrow(()->error(HttpStatus.FORBIDDEN,"No encontramos una cuenta de cliente activa con ese correo."));}
    public Datos datos(UUID codigo,String correo,boolean editable){
        var d=jdbc.query("""
            SELECT p.*,c.codigo_publico AS cotizacion,ped.codigo_publico AS pedido,
                   coalesce(p.nombre_invitado,cliente.nombre||' '||cliente.apellido) AS nombre_cliente,
                   CASE WHEN p.nombre_invitado IS NULL THEN cliente.correo END AS correo_cliente
            FROM lamontana.preparacion_cliente p
            JOIN lamontana.usuario cliente ON cliente.id_usuario=p.id_usuario
            JOIN lamontana.rol rol_cliente ON rol_cliente.id_rol=cliente.id_rol
            JOIN lamontana.usuario actor ON actor.id_usuario=coalesce(p.id_operador,p.id_usuario)
            JOIN lamontana.rol rol_actor ON rol_actor.id_rol=actor.id_rol
            LEFT JOIN lamontana.cotizacion c ON c.id_cotizacion=p.id_ultima_cotizacion
            LEFT JOIN lamontana.pedido ped ON ped.id_pedido=p.id_pedido
            WHERE p.codigo_publico=? AND lower(actor.correo)=lower(?)
              AND ((p.nombre_invitado IS NULL AND cliente.estado='ACTIVO' AND rol_cliente.activo AND rol_cliente.codigo='CLIENTE')
                OR (p.nombre_invitado IS NOT NULL AND rol_cliente.codigo='INVITADO_MOSTRADOR'))
              AND actor.estado='ACTIVO' AND rol_actor.activo
              AND (p.id_operador IS NULL OR rol_actor.codigo IN ('ADMIN_ADMIN','EMPLEADO'))
            """,(r,n)->new Datos(r.getLong("id_preparacion"),codigo,r.getLong("id_usuario"),r.getObject("id_operador",Long.class),
                new Cliente(r.getString("nombre_cliente"),r.getString("correo_cliente"),r.getString("nombre_invitado")!=null),
                r.getObject("sucursal_mostrador",UUID.class),r.getLong("version"),r.getString("estado"),
                r.getString("items"),r.getString("presupuesto"),r.getString("preferencias"),
                r.getObject("cotizacion",UUID.class),r.getObject("pedido",UUID.class)),codigo,correo).stream().findFirst()
            .orElseThrow(()->error(HttpStatus.NOT_FOUND,"El trabajo preparado no está disponible en tu cuenta."));
        if(d.operador()!=null)organizacion.exigirPermiso(correo,"CREAR_PEDIDOS");
        if(d.sucursalMostrador()!=null)organizacion.sucursalAutorizada(correo,d.sucursalMostrador());
        if(editable&&!d.estado().equals("ABIERTA"))throw conflicto("Este trabajo ya tiene un pedido; consultá su seguimiento.");
        return d;
    }
    @Transactional public Datos abrir(String correo){return abrir(correo,correo,null);}
    @Transactional public Datos abrirInterna(String correo,String correoCliente){
        bloquear();
        return abrir(correo,correoCliente.strip(),operador(correo));
    }
    @Transactional public Datos abrirInvitado(String correo,UUID operacion,String nombre,UUID sucursal){
        bloquear();
        long operador=operador(correo);
        organizacion.sucursalAutorizada(correo,sucursal);
        String limpio=nombre.strip();
        if(limpio.isBlank()||limpio.codePoints().anyMatch(Character::isISOControl))
            throw error(HttpStatus.BAD_REQUEST,"Ingresá un nombre para identificar al invitado.");
        var prev=jdbc.query("SELECT id_operador,nombre_invitado,sucursal_mostrador FROM lamontana.preparacion_cliente WHERE codigo_publico=?",
            (r,n)->new Object[]{r.getObject(1,Long.class),r.getString(2),r.getObject(3,UUID.class)},operacion);
        if(prev.isEmpty()){
            // El titular técnico sólo existe a partir del primer pedido invitado y nunca inicia sesión.
            jdbc.update("""
                INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado)
                SELECT '647e6a54-0415-47ef-a4a1-8254268721ef',id_rol,?,'!','Invitado','Mostrador','DESACTIVADO'
                FROM lamontana.rol WHERE codigo='INVITADO_MOSTRADOR' AND NOT activo
                ON CONFLICT (codigo_publico) DO NOTHING
                """,UUID.randomUUID()+"@lamontana.invalid");
            jdbc.update("""
                INSERT INTO lamontana.preparacion_cliente(codigo_publico,id_usuario,id_operador,nombre_invitado,sucursal_mostrador)
                SELECT ?,u.id_usuario,?,?,? FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                WHERE u.codigo_publico='647e6a54-0415-47ef-a4a1-8254268721ef' AND r.codigo='INVITADO_MOSTRADOR' AND NOT r.activo AND u.estado='DESACTIVADO'
                """,operacion,operador,limpio,sucursal);
        }else if(!Arrays.equals(prev.get(0),new Object[]{operador,limpio,sucursal})){
            throw conflicto("La operación de alta ya se utilizó con otros datos.");
        }
        return datos(operacion,correo,false);
    }
    private long operador(String correo){
        var operadores=jdbc.query("""
            SELECT u.id_usuario FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
            WHERE u.correo=? AND u.estado='ACTIVO' AND r.activo AND r.codigo IN ('ADMIN_ADMIN','EMPLEADO')
            """,(r,n)->r.getLong(1),correo);
        if(operadores.isEmpty())throw error(HttpStatus.FORBIDDEN,"Se requiere una cuenta interna activa.");
        organizacion.exigirPermiso(correo,"CREAR_PEDIDOS");
        return operadores.get(0);
    }
    private Datos abrir(String actor,String correoCliente,Long operador){
        bloquear();
        long usuario=cliente(correoCliente);
        var ids=jdbc.query("SELECT codigo_publico FROM lamontana.preparacion_cliente WHERE id_usuario=? AND id_operador IS NOT DISTINCT FROM ?::bigint AND estado='ABIERTA' AND nombre_invitado IS NULL",
            (r,n)->r.getObject(1,UUID.class),usuario,operador);
        UUID id=ids.isEmpty()?UUID.randomUUID():ids.get(0);
        if(ids.isEmpty())jdbc.update("INSERT INTO lamontana.preparacion_cliente(codigo_publico,id_usuario,id_operador) VALUES (?,?,?)",id,usuario,operador);
        return datos(id,actor,true);
    }
    public void version(Datos d,long version){if(d.version()!=version)throw conflicto("El trabajo cambió en otra pestaña. Actualizá antes de continuar.");}
    public void cambio(long id,boolean invalida){jdbc.update("UPDATE lamontana.preparacion_cliente SET version=version+1,actualizada_en=clock_timestamp(),presupuesto=CASE WHEN ? THEN NULL ELSE presupuesto END,preferencias=CASE WHEN ? THEN NULL ELSE preferencias END WHERE id_preparacion=?",invalida,invalida,id);}
    public String huella(Object value){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(value).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public String previo(Datos d,UUID operacion,String tipo,Object input){var rows=jdbc.query("SELECT id_preparacion,id_actor,tipo,huella,resultado::text FROM lamontana.evento_preparacion WHERE id_operacion=?",(r,n)->new Object[]{r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5)},operacion);if(rows.isEmpty())return null;var p=rows.get(0);if((long)p[0]!=d.id()||(long)p[1]!=d.actor()||!tipo.equals(p[2])||!huella(input).equals(p[3]))throw conflicto("La operación ya se utilizó con otros datos.");return (String)p[4];}
    public void evento(Datos d,UUID operacion,String tipo,Object input,Object resultado){jdbc.update("INSERT INTO lamontana.evento_preparacion(id_preparacion,id_actor,id_operacion,tipo,huella,resultado) VALUES (?,?,?,?,?,?::jsonb)",d.id(),d.actor(),operacion,tipo,huella(input),json.writeValueAsString(resultado));}
    public static ResponseStatusException conflicto(String mensaje){return error(HttpStatus.CONFLICT,mensaje);}
    public static ResponseStatusException error(HttpStatus status,String mensaje){return new ResponseStatusException(status,mensaje);}
}
