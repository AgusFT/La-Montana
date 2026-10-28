package ar.com.lamontana.pruebas;

import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/** Datos deterministas, exclusivos del perfil local "pruebas". */
@Configuration
@Profile("pruebas")
public class DatosPruebaConfiguration {
    private static final String ADMIN = "admin@prueba.local";
    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CONFIG_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
    private static final UUID CATALOGO_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

    @Bean
    ApplicationRunner cargarDatosDePrueba(JdbcTemplate jdbc, PasswordEncoder encoder, TransactionTemplate tx,
            @Value("${lamontana.pruebas.clave:MontanaPrueba2026!}") String clave) {
        return args -> tx.executeWithoutResult(status -> cargar(jdbc, encoder, clave));
    }

    private void cargar(JdbcTemplate jdbc, PasswordEncoder encoder, String clave) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS lamontana.entorno_prueba (unica boolean PRIMARY KEY CHECK(unica), cargado_en timestamptz NOT NULL DEFAULT clock_timestamp())");
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.entorno_prueba WHERE unica)", Boolean.class))) return;

        String hash = encoder.encode(clave);
        usuario(jdbc, ADMIN_ID, "ADMIN_ADMIN", ADMIN, hash, "Alejandro", "Administrador", true);
        usuario(jdbc, uuid("10000000-0000-0000-0000-000000000002"), "EMPLEADO", "produccion@prueba.local", hash, "Paula", "Producción", false);
        usuario(jdbc, uuid("10000000-0000-0000-0000-000000000003"), "EMPLEADO", "caja@prueba.local", hash, "Carlos", "Caja", false);
        usuario(jdbc, uuid("10000000-0000-0000-0000-000000000101"), "CLIENTE", "cliente1@prueba.local", hash, "Lucía", "Fernández", false);
        usuario(jdbc, uuid("10000000-0000-0000-0000-000000000102"), "CLIENTE", "cliente2@prueba.local", hash, "Martín", "Gómez", false);
        usuario(jdbc, uuid("10000000-0000-0000-0000-000000000103"), "CLIENTE", "cliente3@prueba.local", hash, "Sofía", "Rossi", false);
        usuario(jdbc, uuid("10000000-0000-0000-0000-000000000104"), "CLIENTE", "cliente4@prueba.local", hash, "Diego", "Pérez", false);
        jdbc.update("UPDATE lamontana.inicializacion_sistema SET completada_en=clock_timestamp(),id_usuario_propietario=(SELECT id_usuario FROM lamontana.usuario WHERE correo=?) WHERE unica", ADMIN);

        sucursal(jdbc, "20000000-0000-0000-0000-000000000001", "FLORESTA", "Casa Matriz Floresta", "Av. Rivadavia", "8500", "Floresta", "CABA", "1407", true);
        sucursal(jdbc, "20000000-0000-0000-0000-000000000002", "MEDICINA", "Sucursal Medicina", "Paraguay", "2155", "Recoleta", "CABA", "1121", false);
        sucursal(jdbc, "20000000-0000-0000-0000-000000000003", "LAPLATA", "Sucursal La Plata", "Calle 7", "1280", "La Plata", "Buenos Aires", "1900", false);

        jdbc.update("INSERT INTO lamontana.usuario_sucursal(id_usuario,id_sucursal) SELECT u.id_usuario,s.id_sucursal FROM lamontana.usuario u CROSS JOIN lamontana.sucursal s WHERE u.correo='produccion@prueba.local'");
        jdbc.update("INSERT INTO lamontana.usuario_sucursal(id_usuario,id_sucursal) SELECT u.id_usuario,s.id_sucursal FROM lamontana.usuario u CROSS JOIN lamontana.sucursal s WHERE u.correo='caja@prueba.local'");
        jdbc.update("INSERT INTO lamontana.usuario_permiso(id_usuario,id_permiso,id_usuario_otorgante) SELECT u.id_usuario,p.id_permiso,a.id_usuario FROM lamontana.usuario u CROSS JOIN lamontana.permiso p CROSS JOIN lamontana.usuario a WHERE u.correo='produccion@prueba.local' AND a.correo=? AND p.codigo IN ('GESTIONAR_PEDIDOS','GESTIONAR_PRODUCCION','CONTROLAR_CALIDAD','GESTIONAR_ENTREGAS','CERRAR_PEDIDOS')", ADMIN);
        jdbc.update("INSERT INTO lamontana.usuario_permiso(id_usuario,id_permiso,id_usuario_otorgante) SELECT u.id_usuario,p.id_permiso,a.id_usuario FROM lamontana.usuario u CROSS JOIN lamontana.permiso p CROSS JOIN lamontana.usuario a WHERE u.correo='caja@prueba.local' AND a.correo=? AND p.codigo IN ('REGISTRAR_COBRO','ACREDITAR_PAGO','REGISTRAR_DEVOLUCION')", ADMIN);

        catalogo(jdbc);
        configuracion(jdbc);
        puntosEntrega(jdbc);
        jdbc.update("INSERT INTO lamontana.entorno_prueba(unica) VALUES (true)");
    }

    private void usuario(JdbcTemplate jdbc, UUID id, String rol, String correo, String hash, String nombre, String apellido, boolean propietario) {
        jdbc.update("""
            INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado,correo_verificado_en,es_administrador_propietario,debe_cambiar_contrasena)
            SELECT ?,id_rol,?,?,?,?,'ACTIVO',clock_timestamp(),?,false FROM lamontana.rol WHERE codigo=?
            """, id, correo, hash, nombre, apellido, propietario, rol);
    }

    private void sucursal(JdbcTemplate jdbc, String id, String codigo, String nombre, String calle, String numero, String localidad, String provincia, String cp, boolean propuesta) {
        jdbc.update("""
            INSERT INTO lamontana.sucursal(id_usuario_alta,codigo_publico,codigo,nombre,calle,numero,localidad,provincia,codigo_postal,correo,telefono,zona_horaria,estado,propuesta_matriz)
            SELECT id_usuario,?,?,?,?,?,?,?,?,?,?,?,'ACTIVA',? FROM lamontana.usuario WHERE correo=?
            """, uuid(id), codigo, nombre, calle, numero, localidad, provincia, cp, codigo.toLowerCase()+"@prueba.local", "11-5555-"+codigo.length()+"00", "America/Argentina/Buenos_Aires", propuesta, ADMIN);
        long branch = jdbc.queryForObject("SELECT id_sucursal FROM lamontana.sucursal WHERE codigo=?", Long.class, codigo);
        for (int dia=1; dia<=7; dia++) {
            boolean abierto=dia<=6;
            jdbc.update("INSERT INTO lamontana.sucursal_horario_atencion(id_sucursal,dia_semana,habilitado,hora_desde,hora_hasta) VALUES (?,?,?,?,?)",
                    branch,dia,abierto,abierto?Time.valueOf("09:00:00"):null,abierto?Time.valueOf("18:00:00"):null);
        }
    }

    private void catalogo(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO lamontana.formato(codigo_publico,codigo,nombre,ancho_mm,alto_mm) VALUES (?,?,?,?,?)", uuid("30000000-0000-0000-0000-000000000001"),"A4","A4 (210 × 297 mm)",210,297);
        jdbc.update("INSERT INTO lamontana.papel(codigo_publico,codigo,nombre,gramaje_g_m2,terminacion_tipo) VALUES (?,?,?,?,?)", uuid("40000000-0000-0000-0000-000000000001"),"OBRA-80","Obra blanco",80,"MATE");
        jdbc.update("INSERT INTO lamontana.papel(codigo_publico,codigo,nombre,gramaje_g_m2,terminacion_tipo) VALUES (?,?,?,?,?)", uuid("40000000-0000-0000-0000-000000000002"),"ILUS-130","Ilustración",130,"BRILLANTE");
        servicio(jdbc,"50000000-0000-0000-0000-000000000001","IMPRESION","Impresión","IMPRESION","Impresión blanco y negro o color.");
        servicio(jdbc,"50000000-0000-0000-0000-000000000002","ANILLADO","Anillado","TERMINACION","Terminación anillada.");
        servicio(jdbc,"50000000-0000-0000-0000-000000000003","ABROCHADO","Abrochado","TERMINACION","Terminación abrochada.");
        jdbc.update("INSERT INTO lamontana.catalogo_papel(id_formato,id_papel,habilitado) SELECT f.id_formato,p.id_papel,true FROM lamontana.formato f CROSS JOIN lamontana.papel p");
        long catalogo=jdbc.queryForObject("""
            INSERT INTO lamontana.catalogo_revision(codigo_publico,id_operacion,hash_solicitud,motivo,id_actor,vigente,estado,activada_en)
            SELECT ?,?,'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa','Escenario comercial de pruebas',id_usuario,true,'VIGENTE',clock_timestamp()
            FROM lamontana.usuario WHERE correo=? RETURNING id_catalogo_revision
            """,Long.class,CATALOGO_ID,uuid("60000000-0000-0000-0000-000000000002"),ADMIN);
        for (String papel:List.of("OBRA-80","ILUS-130")) {
            tarifa(jdbc,catalogo,papel,"BLANCO_NEGRO",papel.equals("OBRA-80")?45:70);
            tarifa(jdbc,catalogo,papel,"COLOR",papel.equals("OBRA-80")?120:180);
        }
        configServicio(jdbc,catalogo,"IMPRESION","Impresión","POR_CARILLA",0,5);
        long anillado=configServicio(jdbc,catalogo,"ANILLADO","Anillado","FIJO_POR_ITEM",1200,15);
        long abrochado=configServicio(jdbc,catalogo,"ABROCHADO","Abrochado","FIJO_POR_ITEM",250,2);
        jdbc.update("INSERT INTO lamontana.compatibilidad_servicio(id_configuracion_servicio,id_formato,id_papel) SELECT ?,f.id_formato,p.id_papel FROM lamontana.formato f CROSS JOIN lamontana.papel p",anillado);
        jdbc.update("INSERT INTO lamontana.compatibilidad_servicio(id_configuracion_servicio,id_formato,id_papel) SELECT ?,f.id_formato,p.id_papel FROM lamontana.formato f CROSS JOIN lamontana.papel p",abrochado);
    }

    private void servicio(JdbcTemplate jdbc,String id,String codigo,String nombre,String tipo,String descripcion){jdbc.update("INSERT INTO lamontana.servicio(codigo_publico,codigo,nombre,tipo,descripcion,activo) VALUES (?,?,?,?,?,true)",uuid(id),codigo,nombre,tipo,descripcion);}
    private void tarifa(JdbcTemplate jdbc,long catalogo,String papel,String color,int precio){jdbc.update("INSERT INTO lamontana.tarifa_impresion(id_catalogo_revision,id_formato,id_papel,modo_color,precio_por_carilla,recargo_doble_faz,habilitada) SELECT ?,f.id_formato,p.id_papel,?,?,0,true FROM lamontana.formato f JOIN lamontana.papel p ON p.codigo=? WHERE f.codigo='A4'",catalogo,color,precio,papel);}
    private long configServicio(JdbcTemplate jdbc,long catalogo,String servicio,String nombre,String base,int precio,int minutos){return jdbc.queryForObject("INSERT INTO lamontana.configuracion_servicio(id_catalogo_revision,id_servicio,nombre_visible,base_precio,precio_unitario,preparacion_minutos,habilitado) SELECT ?,id_servicio,?,?,?,?,true FROM lamontana.servicio WHERE codigo=? RETURNING id_configuracion_servicio",Long.class,catalogo,nombre,base,precio,minutos,servicio);}

    private void configuracion(JdbcTemplate jdbc) {
        long config=jdbc.queryForObject("""
            INSERT INTO lamontana.configuracion_version(codigo_publico,numero_version,id_usuario_creador,estado,modelo,version)
            SELECT ?,1,id_usuario,'ACTIVA','MANUAL',7 FROM lamontana.usuario WHERE correo=? RETURNING id_configuracion_version
            """,Long.class,CONFIG_ID,ADMIN);
        jdbc.update("INSERT INTO lamontana.configuracion_financiera(id_configuracion_version,vigencia_cotizacion_minutos,instrucciones_transferencia,exigir_sena,politica_pago_archivos) VALUES (?,1440,'Transferencia de prueba. No enviar dinero real.',false,'{\"plazoMinutos\":1440,\"recordatorioMinutos\":720,\"retencionHoras\":48}'::jsonb)",config);
        jdbc.update("INSERT INTO lamontana.configuracion_medio_pago(id_configuracion_version,medio_pago) VALUES (?,'TRANSFERENCIA'),(?,'EFECTIVO')",config,config);
        jdbc.update("INSERT INTO lamontana.configuracion_recursos(id_configuracion_version,metodo_asignacion) VALUES (?,'MANUAL')",config);
        long printer=jdbc.queryForObject("INSERT INTO lamontana.impresora(codigo_publico,id_usuario_creador) SELECT ?,id_usuario FROM lamontana.usuario WHERE correo=? RETURNING id_impresora",Long.class,uuid("80000000-0000-0000-0000-000000000001"),ADMIN);
        jdbc.update("INSERT INTO lamontana.configuracion_impresora(id_configuracion_version,id_impresora,nombre,admite_color,admite_doble_faz,capacidad_maxima_hojas,estado,id_sucursal_ubicacion) SELECT ?,?,'Brother de pruebas',true,true,500,'OPERATIVA',id_sucursal FROM lamontana.sucursal WHERE codigo='FLORESTA'",config,printer);
        jdbc.update("INSERT INTO lamontana.configuracion_impresora_formato(id_configuracion_version,id_impresora,id_formato) SELECT ?,?,id_formato FROM lamontana.formato",config,printer);
        jdbc.update("INSERT INTO lamontana.sucursal_servicio(id_configuracion_version,id_sucursal,id_servicio) SELECT ?,s.id_sucursal,v.id_servicio FROM lamontana.sucursal s CROSS JOIN lamontana.servicio v",config);
        jdbc.update("INSERT INTO lamontana.configuracion_entrega(id_configuracion_version,preparacion_horas,traslado_horas,id_sucursal_matriz,matriz_confirmada) SELECT ?,24,4,id_sucursal,true FROM lamontana.sucursal WHERE codigo='FLORESTA'",config);
        jdbc.update("INSERT INTO lamontana.configuracion_modalidad_entrega(id_configuracion_version,modalidad) VALUES (?,'RETIRO_SUCURSAL'),(?,'RETIRO_PUNTO_ENTREGA')",config,config);
        for (String codigo:List.of("FLORESTA","MEDICINA","LAPLATA")) {
            long branch=jdbc.queryForObject("SELECT id_sucursal FROM lamontana.sucursal WHERE codigo=?",Long.class,codigo);
            jdbc.update("INSERT INTO lamontana.configuracion_horario_sucursal(id_configuracion_version,id_sucursal,zona_horaria) VALUES (?,?,'America/Argentina/Buenos_Aires')",config,branch);
            for(int dia=1;dia<=7;dia++){
                boolean abierto=dia<=6;
                jdbc.update("INSERT INTO lamontana.horario_sucursal(id_configuracion_version,id_sucursal,dia_semana,habilitado,hora_desde,hora_hasta) VALUES (?,?,?,?,?,?)",config,branch,dia,abierto,abierto?Time.valueOf("09:00:00"):null,abierto?Time.valueOf("18:00:00"):null);
                if(abierto)jdbc.update("INSERT INTO lamontana.franja_entrega(id_configuracion_retiro,id_sucursal_retiro,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada) VALUES (?,?,?,?,?,12,true)",config,branch,dia,Time.valueOf("14:00:00"),Time.valueOf("18:00:00"));
            }
        }
        UUID auth=uuid("70000000-0000-0000-0000-000000000002");
        jdbc.update("""
            INSERT INTO lamontana.autorizacion_configuracion(id_operacion,proposito,id_configuracion_version,version_configuracion,id_actor,version_acceso,correo_destino,hash_solicitud,hash_codigo,fecha_vencimiento,fecha_consumo)
            SELECT ?,'ACTIVAR_CONFIGURACION',?,7,id_usuario,version_acceso,correo,'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb','cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc',clock_timestamp()+interval '15 minutes',clock_timestamp() FROM lamontana.usuario WHERE correo=?
            """,auth,config,ADMIN);
        jdbc.update("INSERT INTO lamontana.activacion_configuracion(id_configuracion_version,id_catalogo_revision,id_actor,id_operacion,motivo,fecha_activacion) SELECT ?,r.id_catalogo_revision,u.id_usuario,?,'Activación inicial del entorno de pruebas',clock_timestamp() FROM lamontana.catalogo_revision r CROSS JOIN lamontana.usuario u WHERE r.codigo_publico=? AND u.correo=?",config,auth,CATALOGO_ID,ADMIN);
        jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version,motivo) SELECT ?,id_usuario,'CONFIGURACION_ACTIVADA',7,'Entorno de pruebas' FROM lamontana.usuario WHERE correo=?",config,ADMIN);
    }

    private void puntosEntrega(JdbcTemplate jdbc) {
        punto(jdbc,"90000000-0000-0000-0000-000000000001","PUNTO-CABALLITO","Punto Caballito","Av. Acoyte","550","Caballito","CABA","1405","FLORESTA",600);
        punto(jdbc,"90000000-0000-0000-0000-000000000002","PUNTO-PALERMO","Punto Palermo","Av. Santa Fe","3200","Palermo","CABA","1425","MEDICINA",750);
    }

    private void punto(JdbcTemplate jdbc,String id,String codigo,String nombre,String calle,String numero,String localidad,String provincia,String cp,String sucursal,int costo){
        long point=jdbc.queryForObject("INSERT INTO lamontana.punto_entrega(codigo_publico,codigo,id_usuario_creador) SELECT ?,?,id_usuario FROM lamontana.usuario WHERE correo=? RETURNING id_punto_entrega",Long.class,uuid(id),codigo,ADMIN);
        long config=jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?",Long.class,CONFIG_ID);
        jdbc.update("INSERT INTO lamontana.configuracion_definicion_punto(id_configuracion_version,id_punto_entrega,nombre,calle,numero,localidad,provincia,codigo_postal,referencias,zona_horaria) VALUES (?,?,?,?,?,?,?,?,?,'America/Argentina/Buenos_Aires')",config,point,nombre,calle,numero,localidad,provincia,cp,"Mostrador identificado como entorno de prueba");
        long link=jdbc.queryForObject("INSERT INTO lamontana.configuracion_punto_entrega(id_configuracion_version,id_punto_entrega,id_sucursal,costo,habilitado) SELECT ?,?,id_sucursal,?,true FROM lamontana.sucursal WHERE codigo=? RETURNING id_configuracion_punto_entrega",Long.class,config,point,costo,sucursal);
        for(int dia=1;dia<=6;dia++)jdbc.update("INSERT INTO lamontana.franja_entrega(id_configuracion_punto_entrega,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada) VALUES (?,?,?,?,8,true)",link,dia,Time.valueOf(LocalTime.of(15,0)),Time.valueOf(LocalTime.of(18,0)));
        jdbc.update("INSERT INTO lamontana.disponibilidad_punto_entrega(id_punto_entrega,estado,version,id_actor,fecha_actualizacion) SELECT ?,'HABILITADO',1,id_usuario,clock_timestamp() FROM lamontana.usuario WHERE correo=?",point,ADMIN);
    }

    private static UUID uuid(String value){return UUID.fromString(value);}
}
