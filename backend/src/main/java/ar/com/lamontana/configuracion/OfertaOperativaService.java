package ar.com.lamontana.configuracion;

import ar.com.lamontana.catalogo.CatalogoService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Lectura interna: no expone parámetros administrativos ni autoriza accesos HTTP. */
@Service
public class OfertaOperativaService {
    private final ConfiguracionService configuracion;
    private final CatalogoService catalogo;
    private final EntregaConfiguracionService entrega;
    private final JdbcTemplate jdbc;
    public OfertaOperativaService(ConfiguracionService configuracion,CatalogoService catalogo,EntregaConfiguracionService entrega,JdbcTemplate jdbc){this.configuracion=configuracion;this.catalogo=catalogo;this.entrega=entrega;this.jdbc=jdbc;}
    public record Sucursal(UUID codigoPublico,String nombre,String direccion){}
    public record Contexto(ConfiguracionService.Borrador configuracion,CatalogoService.Estado catalogo,List<Sucursal> sucursales,List<RevisionConfiguracionService.Opcion> opciones,List<EntregaConfiguracionService.PuntoDisponible> puntos){}
    public ConfiguracionService.Borrador configuracionActiva(){var activa=configuracion.activa();return activa==null?null:activa.configuracion();}
    public Contexto leer(){
        var activa=configuracion.activa();var comercial=catalogo.leerEstado();
        if(activa==null)return new Contexto(null,comercial,List.of(),List.of(),List.of());
        var sucursales=jdbc.query("SELECT codigo_publico,nombre,calle||' '||numero||', '||localidad||', '||provincia AS direccion FROM lamontana.sucursal WHERE estado='ACTIVA' ORDER BY codigo",(r,n)->new Sucursal(r.getObject(1,UUID.class),r.getString(2),r.getString(3)));
        var ids=new HashSet<UUID>();sucursales.forEach(s->ids.add(s.codigoPublico()));
        var b=activa.configuracion();var opciones=OpcionesImpresion.calcular(b,comercial,ids);
        var utilizables=sucursales.stream().filter(s->opciones.stream().anyMatch(o->o.sucursal().equals(s.codigoPublico()))).toList();
        return new Contexto(b,comercial,utilizables,opciones,entrega.puntosCotizacion(b));
    }
    public EvaluadorCalendario.Simulacion calcularEntrega(Contexto c,EntregaConfiguracionController.SimularEntrega in,int minutos){return entrega.calcular(c.configuracion(),in,minutos);}
}
