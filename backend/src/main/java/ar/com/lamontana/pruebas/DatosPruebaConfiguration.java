package ar.com.lamontana.pruebas;

import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.catalogo.CatalogoService;
import ar.com.lamontana.catalogo.EvaluadorPrecioItem;
import ar.com.lamontana.configuracion.EvaluadorFinanciero;
import ar.com.lamontana.configuracion.OfertaOperativaService;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/** Escenario ficticio exclusivo de pruebas. Una única transacción, sin desactivar restricciones. */
@Configuration
@Profile("pruebas")
public class DatosPruebaConfiguration {
    static final String ADMIN="admin@prueba.local";
    static final ZoneId ZONA=ZoneId.of("America/Argentina/Buenos_Aires");

    @Bean
    ApplicationRunner cargarDatosDePrueba(JdbcTemplate jdbc,PasswordEncoder encoder,TransactionTemplate tx,
            CatalogoService catalogo,OfertaOperativaService operativa,EvaluadorPrecioItem precios,
            EvaluadorFinanciero finanzas,ArchivosPrivados archivos,
            @Value("${lamontana.pruebas.clave:MontanaPrueba2026!}") String clave) {
        return args -> tx.executeWithoutResult(status -> {
            for(long lock:new long[]{764003,764001,764002,764004,764099})jdbc.execute("SELECT pg_advisory_xact_lock("+lock+")");
            jdbc.execute("CREATE TABLE IF NOT EXISTS lamontana.entorno_prueba (unica boolean PRIMARY KEY CHECK(unica), cargado_en timestamptz NOT NULL DEFAULT clock_timestamp(), version_escenario integer NOT NULL DEFAULT 2)");
            jdbc.execute("ALTER TABLE lamontana.entorno_prueba ADD COLUMN IF NOT EXISTS version_escenario integer NOT NULL DEFAULT 1");
            var versiones=jdbc.queryForList("SELECT version_escenario FROM lamontana.entorno_prueba WHERE unica",Integer.class);
            if(!versiones.isEmpty()) {
                if(versiones.get(0)<2)throw new IllegalStateException("El escenario de pruebas anterior está conservado. Para cargar seis meses de actividad, ejecutá ./infra/pruebas/reiniciar-datos.sh (borra sólo los datos ficticios del proyecto lamontana-pruebas).");
                return;
            }
            if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.usuario)",Boolean.class)))
                throw new IllegalStateException("El perfil pruebas requiere su base vacía y exclusiva; no se sobreescribe una instalación existente.");
            var hoy=LocalDate.now(ZONA);
            var inicio=hoy.minusMonths(6).atTime(8,0).atZone(ZONA).toInstant();
            identidades(jdbc,encoder.encode(clave),inicio);
            catalogo.todosPredefinidos(ADMIN);
            for(int g:new int[]{130,170,250}) {
                jdbc.update("INSERT INTO lamontana.papel(codigo_publico,codigo,nombre,gramaje_g_m2,terminacion_tipo) VALUES (?,?,'Ilustración',?,'Brillante')",UUID.randomUUID(),"ILUS-"+g,g);
                jdbc.update("INSERT INTO lamontana.catalogo_papel(id_formato,id_papel) SELECT f.id_formato,p.id_papel FROM lamontana.formato f CROSS JOIN lamontana.papel p WHERE p.codigo=? AND f.nombre IN ('A4','A3')","ILUS-"+g);
            }
            for(String[] s:new String[][]{{"IMPRESION","Impresión","IMPRESION"},{"ANILLADO","Anillado","TERMINACION"},{"ABROCHADO","Abrochado","TERMINACION"},{"PLASTIFICADO","Plastificado","TERMINACION"}})
                jdbc.update("INSERT INTO lamontana.servicio(codigo_publico,codigo,nombre,tipo,descripcion,activo) VALUES (?,?,?,?,?,true)",UUID.randomUUID(),s[0],s[1],s[2],s[1]+" de prueba");
            var configuraciones=new ConfiguracionesPrueba(jdbc);
            var pedidos=new PedidosPrueba(jdbc,operativa,precios,finanzas,archivos,encoder);
            long anterior=0;
            for(int etapa=1;etapa<=4;etapa++) {
                var desde=switch(etapa){case 1->hoy.minusMonths(6);case 2->hoy.minusMonths(4);case 3->hoy.minusMonths(2);default->hoy.minusDays(14);};
                var hasta=switch(etapa){case 1->hoy.minusMonths(4);case 2->hoy.minusMonths(2);case 3->hoy.minusDays(14);default->hoy.minusDays(1);};
                anterior=configuraciones.crear(etapa,desde.atTime(8,0).atZone(ZONA).toInstant(),anterior);
                for(var dia=desde.plusDays(1);dia.isBefore(hasta);dia=dia.plusDays(1))
                    if(dia.getDayOfWeek().getValue()<=5&&dia.getDayOfMonth()%2==0)pedidos.historico(dia);
            }
            pedidos.jornada(hoy);
            jdbc.update("INSERT INTO lamontana.entorno_prueba(unica,version_escenario) VALUES (true,2)");
            comprobarRestricciones(jdbc);
        });
    }

    static void comprobarRestricciones(JdbcTemplate jdbc) {
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
        jdbc.execute("SET CONSTRAINTS ALL DEFERRED");
    }

    private void identidades(JdbcTemplate jdbc,String hash,Instant inicio) {
        usuario(jdbc,"ADMIN_ADMIN",ADMIN,hash,"Alejandro","Administrador",true,inicio);
        usuario(jdbc,"EMPLEADO","produccion@prueba.local",hash,"Paula","Producción",false,inicio);
        usuario(jdbc,"EMPLEADO","caja@prueba.local",hash,"Carlos","Caja",false,inicio);
        String[][] clientes={{"Lucía","Fernández"},{"Martín","Gómez"},{"Sofía","Rossi"},{"Diego","Pérez"}};
        for(int i=0;i<clientes.length;i++)usuario(jdbc,"CLIENTE","cliente"+(i+1)+"@prueba.local",hash,clientes[i][0],clientes[i][1],false,inicio);
        jdbc.update("UPDATE lamontana.inicializacion_sistema SET completada_en=?,id_usuario_propietario=(SELECT id_usuario FROM lamontana.usuario WHERE correo=?) WHERE unica",Timestamp.from(inicio),ADMIN);
        String[][] branches={{"FLORESTA","Casa Matriz Floresta","Av. Rivadavia","8500","Floresta","CABA","1407"},{"MEDICINA","Sucursal Medicina","Paraguay","2155","Recoleta","CABA","1121"},{"LAPLATA","Sucursal La Plata","Calle 7","1280","La Plata","Buenos Aires","1900"}};
        for(var b:branches) {
            long id=jdbc.queryForObject("INSERT INTO lamontana.sucursal(id_usuario_alta,codigo_publico,codigo,nombre,calle,numero,localidad,provincia,codigo_postal,correo,telefono,zona_horaria,estado,propuesta_matriz) SELECT id_usuario,?,?,?,?,?,?,?,?,?,?,'America/Argentina/Buenos_Aires','ACTIVA',? FROM lamontana.usuario WHERE correo=? RETURNING id_sucursal",Long.class,UUID.randomUUID(),b[0],b[1],b[2],b[3],b[4],b[5],b[6],b[0].toLowerCase()+"@prueba.local","11-5555-1000",b[0].equals("FLORESTA"),ADMIN);
            for(int d=1;d<=7;d++)jdbc.update("INSERT INTO lamontana.sucursal_horario_atencion(id_sucursal,dia_semana,habilitado,hora_desde,hora_hasta) VALUES (?,?,?,?,?)",id,d,d<=6,d<=6?Time.valueOf("09:00:00"):null,d<=6?Time.valueOf("18:00:00"):null);
        }
        jdbc.update("INSERT INTO lamontana.usuario_sucursal(id_usuario,id_sucursal,fecha_desde) SELECT u.id_usuario,s.id_sucursal,? FROM lamontana.usuario u CROSS JOIN lamontana.sucursal s WHERE u.correo IN ('produccion@prueba.local','caja@prueba.local')",Timestamp.from(inicio));
        jdbc.update("INSERT INTO lamontana.usuario_permiso(id_usuario,id_permiso,id_usuario_otorgante,fecha_otorgamiento) SELECT u.id_usuario,p.id_permiso,a.id_usuario,? FROM lamontana.usuario u CROSS JOIN lamontana.permiso p CROSS JOIN lamontana.usuario a WHERE u.correo='produccion@prueba.local' AND a.correo=? AND p.codigo IN ('GESTIONAR_PEDIDOS','GESTIONAR_PRODUCCION','CONTROLAR_CALIDAD','GESTIONAR_ENTREGAS','CERRAR_PEDIDOS')",Timestamp.from(inicio),ADMIN);
        jdbc.update("INSERT INTO lamontana.usuario_permiso(id_usuario,id_permiso,id_usuario_otorgante,fecha_otorgamiento) SELECT u.id_usuario,p.id_permiso,a.id_usuario,? FROM lamontana.usuario u CROSS JOIN lamontana.permiso p CROSS JOIN lamontana.usuario a WHERE u.correo='caja@prueba.local' AND a.correo=? AND p.codigo IN ('REGISTRAR_COBRO','ACREDITAR_PAGO','REGISTRAR_DEVOLUCION')",Timestamp.from(inicio),ADMIN);
    }

    private void usuario(JdbcTemplate jdbc,String rol,String correo,String hash,String nombre,String apellido,boolean propietario,Instant fecha) {
        jdbc.update("INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado,correo_verificado_en,es_administrador_propietario,debe_cambiar_contrasena,fecha_alta) SELECT ?,id_rol,?,?,?,?,'ACTIVO',?,?,false,? FROM lamontana.rol WHERE codigo=?",UUID.randomUUID(),correo,hash,nombre,apellido,Timestamp.from(fecha),propietario,Timestamp.from(fecha),rol);
    }
}
