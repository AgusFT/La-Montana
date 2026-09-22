package ar.com.lamontana.configuracion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/** Copia solamente definiciones versionadas; requiere transacción y exclusión del configurador. */
@Component
public class CopiaParametrosConfiguracion {
 private final JdbcTemplate jdbc;
 public CopiaParametrosConfiguracion(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public void copiar(long origen,long destino){
  copiar("configuracion_financiera","vigencia_cotizacion_minutos,instrucciones_transferencia,exigir_sena,condicion_sena,umbral_sena,tipo_sena,valor_sena,umbral_aprobacion",origen,destino);
  copiar("configuracion_medio_pago","medio_pago",origen,destino);
  copiar("configuracion_recursos","metodo_asignacion",origen,destino);
  copiar("configuracion_impresora","id_impresora,nombre,admite_color,admite_doble_faz,capacidad_maxima_hojas,estado,retirada_en,motivo_retiro",origen,destino);
  copiar("configuracion_impresora_formato","id_impresora,id_formato",origen,destino);
  copiar("sucursal_servicio","id_sucursal,id_servicio",origen,destino);
  copiar("configuracion_entrega","preparacion_horas,traslado_horas",origen,destino);
  copiar("configuracion_modalidad_entrega","modalidad",origen,destino);
  copiar("configuracion_horario_sucursal","id_sucursal,zona_horaria",origen,destino);
  copiar("horario_sucursal","id_sucursal,dia_semana,habilitado,hora_desde,hora_hasta",origen,destino);
  jdbc.update("INSERT INTO lamontana.franja_entrega(id_configuracion_retiro,id_sucursal_retiro,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada) SELECT ?,id_sucursal_retiro,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada FROM lamontana.franja_entrega WHERE id_configuracion_retiro=?",destino,origen);
  copiar("configuracion_definicion_punto","id_punto_entrega,nombre,calle,numero,localidad,provincia,codigo_postal,referencias,zona_horaria",origen,destino);
  copiar("configuracion_punto_entrega","id_punto_entrega,id_sucursal,costo,habilitado",origen,destino);
  jdbc.update("""
   INSERT INTO lamontana.franja_entrega(id_configuracion_punto_entrega,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada)
   SELECT d.id_configuracion_punto_entrega,f.dia_semana,f.hora_desde,f.hora_hasta,f.capacidad_pedidos,f.habilitada
   FROM lamontana.franja_entrega f JOIN lamontana.configuracion_punto_entrega o USING(id_configuracion_punto_entrega)
   JOIN lamontana.configuracion_punto_entrega d ON d.id_configuracion_version=? AND d.id_punto_entrega=o.id_punto_entrega AND d.id_sucursal=o.id_sucursal
   WHERE o.id_configuracion_version=?
   """,destino,origen);
  copiar("configuracion_zona_entrega","id_zona_entrega,nombre,descripcion,zona_horaria,costo,habilitada",origen,destino);
  jdbc.update("""
   INSERT INTO lamontana.zona_entrega_codigo_postal(id_configuracion_zona_entrega,id_configuracion_version,habilitada,codigo_postal,localidad,provincia)
   SELECT d.id_configuracion_zona_entrega,d.id_configuracion_version,d.habilitada,t.codigo_postal,t.localidad,t.provincia
   FROM lamontana.zona_entrega_codigo_postal t JOIN lamontana.configuracion_zona_entrega o USING(id_configuracion_zona_entrega)
   JOIN lamontana.configuracion_zona_entrega d ON d.id_configuracion_version=? AND d.id_zona_entrega=o.id_zona_entrega WHERE o.id_configuracion_version=?
   """,destino,origen);
  jdbc.update("""
   INSERT INTO lamontana.franja_entrega(id_configuracion_zona_entrega,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada)
   SELECT d.id_configuracion_zona_entrega,f.dia_semana,f.hora_desde,f.hora_hasta,f.capacidad_pedidos,f.habilitada
   FROM lamontana.franja_entrega f JOIN lamontana.configuracion_zona_entrega o USING(id_configuracion_zona_entrega)
   JOIN lamontana.configuracion_zona_entrega d ON d.id_configuracion_version=? AND d.id_zona_entrega=o.id_zona_entrega WHERE o.id_configuracion_version=?
   """,destino,origen);
 }
 private void copiar(String tabla,String columnas,long origen,long destino){jdbc.update("INSERT INTO lamontana."+tabla+"(id_configuracion_version,"+columnas+") SELECT ?,"+columnas+" FROM lamontana."+tabla+" WHERE id_configuracion_version=?",destino,origen);}
}
