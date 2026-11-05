package ar.com.lamontana.pruebas;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import static ar.com.lamontana.pruebas.DatosPruebaConfiguration.*;

/** Cuatro fotografías comerciales/operativas sucesivas de la misma imprenta ficticia. */
final class ConfiguracionesPrueba {
    private final JdbcTemplate jdbc;
    ConfiguracionesPrueba(JdbcTemplate jdbc){this.jdbc=jdbc;}

    long crear(int n,Instant fecha,long anterior) {
        var t=Timestamp.from(fecha);
        if(anterior>0) {
            jdbc.update("UPDATE lamontana.configuracion_version SET estado='HISTORICA' WHERE id_configuracion_version=?",anterior);
            jdbc.update("UPDATE lamontana.activacion_configuracion SET fin_vigencia=? WHERE id_configuracion_version=?",t,anterior);
            evento(anterior,"CONFIGURACION_HISTORICA","Reemplazada por la versión "+n,t);
            jdbc.update("UPDATE lamontana.catalogo_revision SET vigente=false,estado='HISTORICA' WHERE vigente");
        }
        long comercial=catalogo(n,t);
        String modelo=n==2||n==3?"CONDICIONAL":"MANUAL";
        String criterio=n==2?"MONTO_TOTAL":n==3?"SENA":null;
        long id=jdbc.queryForObject("INSERT INTO lamontana.configuracion_version(codigo_publico,numero_version,id_usuario_creador,estado,modelo,criterio,version,fecha_creacion,fecha_actualizacion) SELECT ?,?,id_usuario,'ACTIVA',?,?,7,?,? FROM lamontana.usuario WHERE correo=? RETURNING id_configuracion_version",Long.class,UUID.randomUUID(),n,modelo,criterio,t,t,ADMIN);
        String condicion=n==2?"SUPERAR_UMBRAL_APROBACION":n==3?"SIEMPRE":"DESDE_CARILLAS";
        jdbc.update("INSERT INTO lamontana.configuracion_financiera(id_configuracion_version,vigencia_cotizacion_minutos,instrucciones_transferencia,exigir_sena,condicion_sena,umbral_sena,tipo_sena,valor_sena,umbral_aprobacion,politica_pago_archivos) VALUES (?,?,?,true,?,?,'PORCENTAJE',?,?,?::jsonb)",id,n==1?720:1440,"Transferencia SIMULADA V"+n+" · alias montana.pruebas · No enviar dinero real.",condicion,n==1||n==4?201:null,n==3?40:30,n==2?10000:null,"{\"plazoMinutos\":1440,\"recordatorioMinutos\":720,\"retencionHoras\":"+(n==1?24:n==2?48:72)+"}");
        jdbc.update("INSERT INTO lamontana.configuracion_medio_pago(id_configuracion_version,medio_pago) VALUES (?,'TRANSFERENCIA'),(?,'EFECTIVO')",id,id);
        jdbc.update("INSERT INTO lamontana.configuracion_recursos(id_configuracion_version,metodo_asignacion) VALUES (?,'MANUAL')",id);
        impresora(id,"FLORESTA-BROTHER","FLORESTA","Brother MFC-J3940DW",true,2000,t);
        impresora(id,"FLORESTA-XEROX-C405","FLORESTA","Xerox VersaLink C405",true,2500,t);
        impresora(id,"FLORESTA-HP-M404","FLORESTA","HP LaserJet Pro M404dn",false,3000,t);
        impresora(id,"MEDICINA-EPSON-L8180","MEDICINA","Epson EcoTank L8180",true,1500,t);
        impresora(id,"MEDICINA-BROTHER-L6400","MEDICINA","Brother HL-L6400DW",false,4000,t);
        impresora(id,"LAPLATA-CANON-GX7010","LAPLATA","Canon MAXIFY GX7010",true,2200,t);
        impresora(id,"LAPLATA-BROTHER-L6900","LAPLATA","Brother MFC-L6900DW",false,4500,t);
        impresora(id,"LAPLATA-EPSON-C5890","LAPLATA","Epson WorkForce Pro WF-C5890",true,2400,t);
        jdbc.update("INSERT INTO lamontana.sucursal_servicio(id_configuracion_version,id_sucursal,id_servicio) SELECT ?,b.id_sucursal,s.id_servicio FROM lamontana.sucursal b CROSS JOIN lamontana.servicio s",id);
        jdbc.update("INSERT INTO lamontana.configuracion_entrega(id_configuracion_version,preparacion_horas,traslado_horas,id_sucursal_matriz,matriz_confirmada) SELECT ?,?,?,id_sucursal,true FROM lamontana.sucursal WHERE codigo='FLORESTA'",id,n==1?8:n==2?6:n==3?4:2,n==1?4:2);
        for(String modo:new String[]{"RETIRO_SUCURSAL","RETIRO_PUNTO_ENTREGA","ENVIO_DOMICILIO"})jdbc.update("INSERT INTO lamontana.configuracion_modalidad_entrega VALUES (?,?)",id,modo);
        for(long sucursal:jdbc.queryForList("SELECT id_sucursal FROM lamontana.sucursal ORDER BY id_sucursal",Long.class)) {
            jdbc.update("INSERT INTO lamontana.configuracion_horario_sucursal VALUES (?,?,'America/Argentina/Buenos_Aires')",id,sucursal);
            for(int dia=1;dia<=7;dia++) {
                boolean abierto=dia<=6;
                jdbc.update("INSERT INTO lamontana.horario_sucursal VALUES (?,?,?,?,?,?)",id,sucursal,dia,abierto,abierto?Time.valueOf("09:00:00"):null,abierto?Time.valueOf(n==1?"17:00:00":"18:00:00"):null);
                if(abierto)jdbc.update("INSERT INTO lamontana.franja_entrega(id_configuracion_retiro,id_sucursal_retiro,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada) VALUES (?,?,?,'14:00','18:00',?,true)",id,sucursal,dia,n==1?12:24);
            }
        }
        punto(id,"CABALLITO","Punto Caballito","Av. Acoyte","550","Caballito","1405",500+n*100,t,"FLORESTA");
        punto(id,"PALERMO","Punto Palermo","Av. Santa Fe","3200","Palermo","1425",600+n*100,t,"MEDICINA","FLORESTA");
        punto(id,"LAPLATA-CENTRO","Punto La Plata Centro","Calle 12","845","La Plata","1900",650+n*100,t,"LAPLATA","FLORESTA");
        punto(id,"TOLOSA","Punto Tolosa","Avenida 7","215","Tolosa","1901",700+n*100,t,"LAPLATA","FLORESTA");
        punto(id,"CITY-BELL","Punto City Bell","Camino Centenario","1320","City Bell","1896",800+n*100,t,"LAPLATA","FLORESTA");
        zona(id,n,t);
        UUID op=UUID.randomUUID();
        jdbc.update("INSERT INTO lamontana.autorizacion_configuracion(id_operacion,proposito,id_configuracion_version,version_configuracion,id_actor,version_acceso,correo_destino,hash_solicitud,hash_codigo,fecha_emision,fecha_vencimiento,fecha_consumo) SELECT ?,'ACTIVAR_CONFIGURACION',?,7,id_usuario,version_acceso,correo,repeat('b',64),?,?,?,? FROM lamontana.usuario WHERE correo=?",op,id,String.format("%064d",n),t,Timestamp.from(fecha.plusSeconds(900)),t,ADMIN);
        String motivo=switch(n){case 1->"Apertura de la imprenta · operación manual";case 2->"Mayor volumen · aprobación por monto y nuevas tarifas";case 3->"Seña del 40 % · preparación más rápida";default->"Operación actual · revisión humana y seña del 30 % desde 201 carillas";};
        jdbc.update("INSERT INTO lamontana.activacion_configuracion(id_configuracion_version,id_predecesora,id_catalogo_revision,id_actor,id_operacion,motivo,fecha_activacion) SELECT ?,?,?,id_usuario,?,?,? FROM lamontana.usuario WHERE correo=?",id,anterior==0?null:anterior,comercial,op,motivo,t,ADMIN);
        for(String tipo:new String[]{"BORRADOR_CREADO","MODELO_SELECCIONADO","PAGOS_CONFIGURADOS","RECURSOS_CONFIGURADOS","ENTREGA_CONFIGURADA","CONFIGURACION_ACTIVADA"})evento(id,tipo,motivo,t);
        comprobarRestricciones(jdbc);
        return id;
    }

    private long catalogo(int n,Timestamp t) {
        Long anterior=jdbc.queryForObject("SELECT max(id_catalogo_revision) FROM lamontana.catalogo_revision",Long.class);
        long id=jdbc.queryForObject("INSERT INTO lamontana.catalogo_revision(codigo_publico,id_revision_base,id_operacion,hash_solicitud,motivo,id_actor,vigente,estado,creada_en,activada_en) SELECT ?,?,?,repeat('a',64),?,id_usuario,true,'VIGENTE',?,? FROM lamontana.usuario WHERE correo=? RETURNING id_catalogo_revision",Long.class,UUID.randomUUID(),anterior,UUID.randomUUID(),"Lista de precios V"+n+" · simulación histórica",t,t,ADMIN);
        var pares=jdbc.queryForList("SELECT cp.id_formato,cp.id_papel,f.nombre,p.gramaje_g_m2,p.terminacion_tipo FROM lamontana.catalogo_papel cp JOIN lamontana.formato f USING(id_formato) JOIN lamontana.papel p USING(id_papel) WHERE cp.habilitado ORDER BY cp.id_formato,cp.id_papel");
        for(var par:pares)for(String color:new String[]{"BLANCO_NEGRO","COLOR"}) {
            int precio=(color.equals("COLOR")?90:30)+n*10+((Number)par.get("gramaje_g_m2")).intValue()/10;
            if(par.get("nombre").equals("A3"))precio*=2;
            // Cada grupo tiene una regla uniforme. Nombre, precio simple y doble faz visibles en UI.
            jdbc.update("INSERT INTO lamontana.tarifa_impresion(id_catalogo_revision,id_formato,id_papel,modo_color,precio_por_carilla,recargo_doble_faz,habilitada,grupo,nombre,modo_doble_faz,valor_doble_faz) VALUES (?,?,?,?,?,0,true,?,?,'ADICIONAL',?)",id,par.get("id_formato"),par.get("id_papel"),color,precio,UUID.randomUUID(),par.get("nombre")+" · "+par.get("gramaje_g_m2")+" g · "+(color.equals("COLOR")?"Color":"BN"),Math.max(10,precio/2));
        }
        for(String servicio:new String[]{"IMPRESION","ANILLADO","ABROCHADO","PLASTIFICADO"}) {
            int precio=switch(servicio){case "IMPRESION"->0;case "ANILLADO"->800+n*100;case "ABROCHADO"->150+n*25;default->500+n*100;};
            long cfg=jdbc.queryForObject("INSERT INTO lamontana.configuracion_servicio(id_catalogo_revision,id_servicio,nombre_visible,base_precio,precio_unitario,preparacion_minutos,habilitado) SELECT ?,id_servicio,nombre,?,?,?,true FROM lamontana.servicio WHERE codigo=? RETURNING id_configuracion_servicio",Long.class,id,servicio.equals("IMPRESION")?"POR_CARILLA":"FIJO_POR_ITEM",precio,servicio.equals("IMPRESION")?5:15,servicio);
            if(!servicio.equals("IMPRESION"))jdbc.update("INSERT INTO lamontana.compatibilidad_servicio SELECT ?,id_formato,id_papel FROM lamontana.catalogo_papel WHERE habilitado",cfg);
        }
        return id;
    }

    private void impresora(long config,String codigo,String sucursal,String nombre,boolean color,int capacidad,Timestamp fecha) {
        UUID publico=UUID.nameUUIDFromBytes(("impresora-prueba-"+codigo).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        jdbc.update("INSERT INTO lamontana.impresora(codigo_publico,id_usuario_creador,fecha_alta) SELECT ?,id_usuario,? FROM lamontana.usuario WHERE correo=? ON CONFLICT(codigo_publico) DO NOTHING",publico,fecha,ADMIN);
        long id=jdbc.queryForObject("SELECT id_impresora FROM lamontana.impresora WHERE codigo_publico=?",Long.class,publico);
        jdbc.update("INSERT INTO lamontana.configuracion_impresora(id_configuracion_version,id_impresora,nombre,admite_color,admite_doble_faz,capacidad_maxima_hojas,estado,id_sucursal_ubicacion) SELECT ?,?,?,?,true,?,'OPERATIVA',id_sucursal FROM lamontana.sucursal WHERE codigo=?",config,id,nombre,color,capacidad,sucursal);
        jdbc.update("INSERT INTO lamontana.configuracion_impresora_formato SELECT ?,?,id_formato FROM lamontana.formato",config,id);
    }

    private void punto(long config,String codigo,String nombre,String calle,String numero,String localidad,String cp,int costo,Timestamp fecha,String... origenes) {
        jdbc.update("INSERT INTO lamontana.punto_entrega(codigo_publico,codigo,id_usuario_creador,fecha_creacion) SELECT ?,?,id_usuario,? FROM lamontana.usuario WHERE correo=? ON CONFLICT(codigo) DO NOTHING",UUID.randomUUID(),"PUNTO-"+codigo,fecha,ADMIN);
        long id=jdbc.queryForObject("SELECT id_punto_entrega FROM lamontana.punto_entrega WHERE codigo=?",Long.class,"PUNTO-"+codigo);
        String provincia=localidad.equals("La Plata")||localidad.equals("Tolosa")||localidad.equals("City Bell")?"Buenos Aires":"CABA";
        jdbc.update("INSERT INTO lamontana.configuracion_definicion_punto VALUES (?,?,?,?,?,?,?,?,?,'America/Argentina/Buenos_Aires')",config,id,nombre,calle,numero,localidad,provincia,cp,"Mostrador de pruebas · lunes a sábado 15 a 18 h");
        for(int prioridad=1;prioridad<=origenes.length;prioridad++){
            long vinculo=jdbc.queryForObject("INSERT INTO lamontana.configuracion_punto_entrega(id_configuracion_version,id_punto_entrega,id_sucursal,prioridad,costo,habilitado) SELECT ?,?,id_sucursal,?,?,true FROM lamontana.sucursal WHERE codigo=? RETURNING id_configuracion_punto_entrega",Long.class,config,id,prioridad,costo+(prioridad-1)*200,origenes[prioridad-1]);
            for(int dia=1;dia<=6;dia++)jdbc.update("INSERT INTO lamontana.franja_entrega(id_configuracion_punto_entrega,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada) VALUES (?,?,'15:00','18:00',16,true)",vinculo,dia);
        }
        jdbc.update("INSERT INTO lamontana.disponibilidad_punto_entrega(id_punto_entrega,estado,version,id_actor,fecha_actualizacion) SELECT ?,'HABILITADO',1,id_usuario,? FROM lamontana.usuario WHERE correo=? ON CONFLICT(id_punto_entrega) DO NOTHING",id,fecha,ADMIN);
    }

    void ampliarActiva(Instant fecha){
        long config=jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE estado='ACTIVA'",Long.class);var t=Timestamp.from(fecha);
        impresora(config,"FLORESTA-XEROX-C405","FLORESTA","Xerox VersaLink C405",true,2500,t);
        impresora(config,"FLORESTA-HP-M404","FLORESTA","HP LaserJet Pro M404dn",false,3000,t);
        impresora(config,"MEDICINA-EPSON-L8180","MEDICINA","Epson EcoTank L8180",true,1500,t);
        impresora(config,"MEDICINA-BROTHER-L6400","MEDICINA","Brother HL-L6400DW",false,4000,t);
        impresora(config,"LAPLATA-CANON-GX7010","LAPLATA","Canon MAXIFY GX7010",true,2200,t);
        impresora(config,"LAPLATA-BROTHER-L6900","LAPLATA","Brother MFC-L6900DW",false,4500,t);
        impresora(config,"LAPLATA-EPSON-C5890","LAPLATA","Epson WorkForce Pro WF-C5890",true,2400,t);
        jdbc.update("UPDATE lamontana.configuracion_punto_entrega c SET prioridad=CASE p.codigo WHEN 'PUNTO-PALERMO' THEN 2 ELSE 1 END FROM lamontana.punto_entrega p WHERE p.id_punto_entrega=c.id_punto_entrega AND c.id_configuracion_version=?",config);
        long palermo=jdbc.queryForObject("SELECT id_punto_entrega FROM lamontana.punto_entrega WHERE codigo='PUNTO-PALERMO'",Long.class);
        long vinculo=jdbc.queryForObject("INSERT INTO lamontana.configuracion_punto_entrega(id_configuracion_version,id_punto_entrega,id_sucursal,prioridad,costo,habilitado) SELECT ?,?,id_sucursal,1,700,true FROM lamontana.sucursal WHERE codigo='MEDICINA' RETURNING id_configuracion_punto_entrega",Long.class,config,palermo);
        for(int dia=1;dia<=6;dia++)jdbc.update("INSERT INTO lamontana.franja_entrega(id_configuracion_punto_entrega,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada) VALUES (?,?,'15:00','18:00',16,true)",vinculo,dia);
        punto(config,"LAPLATA-CENTRO","Punto La Plata Centro","Calle 12","845","La Plata","1900",1050,t,"LAPLATA","FLORESTA");
        punto(config,"TOLOSA","Punto Tolosa","Avenida 7","215","Tolosa","1901",1100,t,"LAPLATA","FLORESTA");
        punto(config,"CITY-BELL","Punto City Bell","Camino Centenario","1320","City Bell","1896",1200,t,"LAPLATA","FLORESTA");
    }

    private void zona(long config,int n,Timestamp fecha) {
        jdbc.update("INSERT INTO lamontana.zona_entrega(codigo_publico,codigo,id_usuario_creador,fecha_creacion) SELECT ?,'CABA-OESTE',id_usuario,? FROM lamontana.usuario WHERE correo=? ON CONFLICT(codigo) DO NOTHING",UUID.randomUUID(),fecha,ADMIN);
        long id=jdbc.queryForObject("INSERT INTO lamontana.configuracion_zona_entrega(id_configuracion_version,id_zona_entrega,nombre,descripcion,zona_horaria,costo,habilitada) SELECT ?,id_zona_entrega,'CABA Oeste','Envío simulado a Floresta y Caballito','America/Argentina/Buenos_Aires',?,true FROM lamontana.zona_entrega WHERE codigo='CABA-OESTE' RETURNING id_configuracion_zona_entrega",Long.class,config,1000+n*200);
        jdbc.update("INSERT INTO lamontana.zona_entrega_codigo_postal VALUES (?,?,true,'1407','FLORESTA','CABA'),(?,?,true,'1405','CABALLITO','CABA')",id,config,id,config);
        for(int dia=1;dia<=6;dia++)jdbc.update("INSERT INTO lamontana.franja_entrega(id_configuracion_zona_entrega,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada) VALUES (?,?,'15:00','18:00',16,true)",id,dia);
    }

    private void evento(long id,String tipo,String motivo,Timestamp fecha) {
        jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version,motivo,fecha) SELECT ?,id_usuario,?,7,?,? FROM lamontana.usuario WHERE correo=?",id,tipo,motivo,fecha,ADMIN);
    }
}
