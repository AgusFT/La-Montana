package ar.com.lamontana.configuracion;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Lee la red actual y mantiene disponibles los datos anteriores hasta su primera edición. */
public final class RedPuntosRepositorio {
    private final JdbcTemplate jdbc;
    private final JsonMapper json=JsonMapper.builder().build();
    public RedPuntosRepositorio(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public List<PuntosRepositorio.Punto> leer(long configuracion){
        var puntos=new LinkedHashMap<UUID,PuntosRepositorio.Punto>();
        if(configuracion>0)new PuntosRepositorio(jdbc).leer(configuracion).forEach(p->puntos.put(p.codigoPublico(),p));
        registrados().forEach(p->puntos.put(p.codigoPublico(),p));
        return puntos.values().stream().sorted(Comparator.comparing(PuntosRepositorio.Punto::codigo)).toList();
    }
    public List<PuntosRepositorio.Punto> registrados(){return jdbc.query("SELECT v.definicion FROM lamontana.red_punto_actual a JOIN lamontana.version_red_punto v USING(id_version_red_punto)",(r,n)->json.readValue(r.getString(1),PuntosRepositorio.Punto.class));}
    public long configuracionActual(){return jdbc.query("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE estado='ACTIVA'",(r,n)->r.getLong(1)).stream().findFirst().orElse(0L);}
    public UUID revision(UUID punto){return jdbc.query("SELECT v.codigo_publico FROM lamontana.red_punto_actual a JOIN lamontana.version_red_punto v USING(id_version_red_punto) JOIN lamontana.punto_entrega p ON p.id_punto_entrega=a.id_punto_entrega WHERE p.codigo_publico=?",(r,n)->r.getObject(1,UUID.class),punto).stream().findFirst().orElse(null);}
    public Long idRevision(UUID punto){return jdbc.query("SELECT a.id_version_red_punto FROM lamontana.red_punto_actual a JOIN lamontana.punto_entrega p USING(id_punto_entrega) WHERE p.codigo_publico=?",(r,n)->r.getLong(1),punto).stream().findFirst().orElse(null);}
    public PuntosRepositorio.Punto gestionado(UUID codigo){return jdbc.query("SELECT v.definicion FROM lamontana.red_punto_actual a JOIN lamontana.version_red_punto v USING(id_version_red_punto) JOIN lamontana.punto_entrega p ON p.id_punto_entrega=a.id_punto_entrega WHERE p.codigo_publico=?",(r,n)->json.readValue(r.getString(1),PuntosRepositorio.Punto.class),codigo).stream().findFirst().orElse(null);}
    public PuntosRepositorio.Punto punto(UUID codigo){var actual=gestionado(codigo);return actual!=null?actual:leer(configuracionActual()).stream().filter(p->p.codigoPublico().equals(codigo)).findFirst().orElse(null);}
    public ConfiguracionService.Borrador aplicar(ConfiguracionService.Borrador b){
        if(b==null)return null;
        var e=b.entrega();var id=jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?",Long.class,b.codigoPublico());
        var puntos=leer(id);var modalidades=new ArrayList<>(e.modalidades());
        if(!modalidades.contains(EntregaConfiguracionController.Modalidad.RETIRO_PUNTO_ENTREGA)&&Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.red_punto_actual)",Boolean.class)))modalidades.add(EntregaConfiguracionController.Modalidad.RETIRO_PUNTO_ENTREGA);
        var actual=new EntregaRepositorio.Entrega(e.preparacionHoras(),e.trasladoHoras(),List.copyOf(modalidades),e.horariosPorSucursal(),puntos,e.zonas(),e.casaMatriz(),e.matrizConfirmada());
        return new ConfiguracionService.Borrador(b.codigoPublico(),b.numero(),b.version(),b.estado(),b.modelo(),b.criterio(),b.creadaEn(),b.actualizadaEn(),b.actor(),b.canceladaEn(),b.motivoCancelacion(),b.cancelador(),b.pagos(),b.recursos(),actual,b.copia());
    }
}
