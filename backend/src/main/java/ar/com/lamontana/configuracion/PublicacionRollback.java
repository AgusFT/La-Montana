package ar.com.lamontana.configuracion;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Toda la copia, cancelación y publicación participa en una sola transacción del llamador. */
@Component
public class PublicacionRollback {
    private final JdbcTemplate jdbc;
    private final CopiaParametrosConfiguracion parametros;
    private final PublicacionConfiguracion publicacion;
    private final JsonMapper json=JsonMapper.builder().build();
    public PublicacionRollback(JdbcTemplate jdbc,CopiaParametrosConfiguracion parametros,PublicacionConfiguracion publicacion){this.jdbc=jdbc;this.parametros=parametros;this.publicacion=publicacion;}
    public long publicar(long origen,long actor,UUID operacion,UUID intento,RollbackConfiguracionService.Revision revision,String motivo){
        var source=revision.objetivo().configuracion();
        if(revision.programada()!=null){
            UUID programada=revision.programada().configuracion().codigoPublico();
            String cancelacion="Cancelada al recuperar V"+source.numero()+" mediante reversión manual.";
            long pendiente=jdbc.queryForObject("UPDATE lamontana.configuracion_version SET estado='CANCELADA',version=version+1,fecha_actualizacion=clock_timestamp(),fecha_cancelacion=clock_timestamp(),id_usuario_cancelador=?,motivo_cancelacion=? WHERE codigo_publico=? AND estado='PROGRAMADA' RETURNING id_configuracion_version",Long.class,actor,cancelacion,programada);
            jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version,codigo_recurso,motivo) SELECT id_configuracion_version,?,'PROGRAMACION_CANCELADA',version,?,? FROM lamontana.configuracion_version WHERE id_configuracion_version=?",actor,intento,cancelacion,pendiente);
            jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",pendiente);
        }
        long nueva=jdbc.queryForObject("INSERT INTO lamontana.configuracion_version(codigo_publico,numero_version,id_usuario_creador,estado,modelo,criterio,version) VALUES (?,(SELECT max(numero_version)+1 FROM lamontana.configuracion_version),?,'EN_PREPARACION',?,?,2) RETURNING id_configuracion_version",Long.class,UUID.randomUUID(),actor,source.modelo().name(),source.criterio()==null?null:source.criterio().name());
        parametros.copiar(origen,nueva);
        jdbc.update("INSERT INTO lamontana.origen_configuracion(id_configuracion_version,id_version_origen,tipo,barrido_inicial) VALUES (?,?,'ROLLBACK',?::jsonb)",nueva,origen,json.writeValueAsString(revision.condiciones().hallazgos()));
        publicacion.publicar(nueva,actor,operacion,revision.condiciones().catalogo().actual().codigoPublico(),motivo);
        jdbc.update("INSERT INTO lamontana.evento_configuracion(id_configuracion_version,id_actor,tipo,version,codigo_recurso,motivo) SELECT id_configuracion_version,?,'ROLLBACK_PUBLICADO',version,?,? FROM lamontana.configuracion_version WHERE id_configuracion_version=?",actor,intento,motivo,nueva);
        return nueva;
    }
}
