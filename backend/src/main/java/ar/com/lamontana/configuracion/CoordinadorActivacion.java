package ar.com.lamontana.configuracion;

import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Excluye ejecutores durante las transacciones separadas de inicio y publicación. */
@Component
public class CoordinadorActivacion {
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    public CoordinadorActivacion(DataSource dataSource,JdbcTemplate jdbc){this.dataSource=dataSource;this.jdbc=jdbc;}
    <T> T exclusivo(boolean esperar,Supplier<T> tarea){
        try(var conexion=dataSource.getConnection()){
            boolean tomado;
            try(var q=conexion.createStatement()){
                q.setQueryTimeout(30);
                if(esperar){q.execute("SELECT pg_advisory_lock(764004)");tomado=true;}
                else try(var r=q.executeQuery("SELECT pg_try_advisory_lock(764004)")){r.next();tomado=r.getBoolean(1);}
            }
            if(!tomado)return null;
            try{return tarea.get();}
            finally{try(var q=conexion.createStatement()){q.execute("SELECT pg_advisory_unlock(764004)");}}
        }catch(SQLException ex){throw new IllegalStateException("No se pudo coordinar la ejecución de configuración.",ex);}
    }
    // Requiere el lock exclusivo de sesión y el lock transaccional de configuración.
    int recuperar(){return jdbc.update("UPDATE lamontana.intento_activacion_configuracion SET estado='INTERRUMPIDO',fecha_fin=clock_timestamp(),codigo_resultado='EJECUCION_INTERRUMPIDA',detalle_sanitizado=CASE WHEN origen='MANUAL' THEN 'Se recuperó un intento sin publicación confirmada. No se repetirá este comando manual.' ELSE 'Se recuperó un intento sin publicación confirmada; se revisará el vencimiento pendiente.' END WHERE estado='INICIADO'");}
}
