package ar.com.lamontana.configuracion;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/** Invocado dentro de una transacción que ya posee los locks operativos. */
@Component
public class PublicacionConfiguracion {
 private final JdbcTemplate jdbc;
 public PublicacionConfiguracion(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public void publicar(long id,Long actorId,UUID operacion,UUID comercial,String motivo){var ahora=jdbc.queryForObject("SELECT clock_timestamp()",Timestamp.class);var anteriores=jdbc.query("SELECT id_configuracion_version,version FROM lamontana.configuracion_version WHERE estado='ACTIVA'",(r,n)->new long[]{r.getLong(1),r.getLong(2)});Long anterior=anteriores.isEmpty()?null:anteriores.get(0)[0];
  // Desde aquí no hay rechazos de negocio: cualquier error de almacenamiento revierte toda la transición.
  if(anterior!=null){jdbc.update("UPDATE lamontana.configuracion_version SET estado='HISTORICA',fecha_actualizacion=? WHERE id_configuracion_version=?",ahora,anterior);jdbc.update("UPDATE lamontana.activacion_configuracion SET fin_vigencia=? WHERE id_configuracion_version=?",ahora,anterior);jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version,motivo) VALUES (?,?,'CONFIGURACION_HISTORICA',?,'Reemplazada por nueva configuración activa')",anterior,actorId,anteriores.get(0)[1]);}
  jdbc.update("UPDATE lamontana.configuracion_version SET estado='ACTIVA',version=version+1,fecha_actualizacion=? WHERE id_configuracion_version=?",ahora,id);
  jdbc.update("INSERT INTO lamontana.activacion_configuracion(id_configuracion_version,id_predecesora,id_catalogo_revision,id_actor,id_operacion,motivo,fecha_activacion) VALUES (?,?,(SELECT id_catalogo_revision FROM lamontana.catalogo_revision WHERE codigo_publico=?),?,?,?,?)",id,anterior,comercial,actorId,operacion,motivo,ahora);
 }
}
