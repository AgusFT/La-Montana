package ar.com.lamontana.archivos;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Captura el original al enviar el pedido; nunca inventa una lectura/aceptación de su vista previa. */
public final class RegistroEnvioPdf {
    private RegistroEnvioPdf(){}
    public static void registrar(JdbcTemplate jdbc,UUID archivo,long usuario,UUID operacion,String sha256) {
        UUID id=UUID.nameUUIDFromBytes((operacion+":envio-pdf:"+archivo).getBytes(StandardCharsets.UTF_8));
        jdbc.update("""
            INSERT INTO lamontana.aceptacion_vista_previa(id_archivo_almacenado,id_usuario,id_operacion,sha256,tipo_registro)
            SELECT id_archivo_almacenado,?,?,?,'ENVIO_PEDIDO' FROM lamontana.archivo_almacenado
            WHERE codigo_publico=? AND id_usuario_cargador=? AND sha256=? AND estado='VALIDO' AND finalidad='TRABAJO'
            ON CONFLICT(id_archivo_almacenado) DO NOTHING
            """,usuario,id,sha256,archivo,usuario,sha256);
    }
}
