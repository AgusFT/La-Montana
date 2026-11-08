//#region ENCABEZADO · OfertaOperativaService.java
/*
 * ========================================================================
 * ARCHIVO: OfertaOperativaService.java
 * ========================================================================
 * FUNCIÓN
 * Reúne la configuración operativa activa, el catálogo comercial y las sucursales utilizables para
 * ofrecer opciones reales de impresión y calcular la entrega.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] OfertaOperativaService(ConfiguracionService configuracion, CatalogoService catalogo,
 *   EntregaConfiguracionService entrega, JdbcTemplate jdbc)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] ConfiguracionService.Borrador configuracionActiva()
 * - [public] Contexto leer()
 * - [public] EvaluadorCalendario.Simulacion calcularEntrega(Contexto c,
 *   EntregaConfiguracionController.SimularEntrega in, int minutos)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - OfertaOperativaService (clase).
 * - OfertaOperativaService.Sucursal (record).
 * - OfertaOperativaService.Contexto (record).
 * ========================================================================
 */
//#endregion

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
    public ConfiguracionService.Borrador configuracionActiva(){var activa=configuracion.activa();return activa==null?null:new RedPuntosRepositorio(jdbc).aplicar(activa.configuracion());}
    public Contexto leer(){
        var activa=configuracion.activa();var comercial=catalogo.leerEstado();
        if(activa==null)return new Contexto(null,comercial,List.of(),List.of(),List.of());
        var sucursales=sucursalesDisponibles();
        var ids=new HashSet<UUID>();sucursales.forEach(s->ids.add(s.codigoPublico()));
        var b=new RedPuntosRepositorio(jdbc).aplicar(activa.configuracion());var opciones=OpcionesImpresion.calcular(b,comercial,ids);
        var utilizables=sucursales.stream().filter(s->opciones.stream().anyMatch(o->o.sucursal().equals(s.codigoPublico()))).toList();
        var disponibles=utilizables.stream().map(Sucursal::codigoPublico).collect(java.util.stream.Collectors.toSet());
        return new Contexto(b,comercial,utilizables,opciones,entrega.puntosCotizacion(b).stream().filter(p->disponibles.contains(p.sucursal())).toList());
    }
    public List<DisponibilidadDestinos.Destino> destinos(Contexto contexto,UUID mostrador){
        return new DisponibilidadDestinos(jdbc).catalogo(contexto.configuracion(),contexto.sucursales().stream().map(Sucursal::codigoPublico).collect(java.util.stream.Collectors.toSet()),mostrador);
    }
    public EvaluadorCalendario.Simulacion calcularEntrega(Contexto c,EntregaConfiguracionController.SimularEntrega in,int minutos){return calcularEntrega(c,in,minutos,false);}
    List<Sucursal> sucursalesDisponibles(){
        return jdbc.query("SELECT s.codigo_publico,s.nombre,s.calle||' '||s.numero||', '||s.localidad||', '||s.provincia AS direccion FROM lamontana.sucursal s WHERE s.estado='ACTIVA' AND NOT EXISTS(SELECT 1 FROM lamontana.disponibilidad_sucursal d WHERE d.id_sucursal=s.id_sucursal AND d.estado='PAUSADA') ORDER BY s.codigo",(r,n)->new Sucursal(r.getObject(1,UUID.class),r.getString(2),r.getString(3)));
    }
    public EvaluadorCalendario.Simulacion calcularEntrega(Contexto c,EntregaConfiguracionController.SimularEntrega in,int minutos,boolean mostrador){
        return entrega.calcular(c.configuracion(),in,minutos,mostrador);
    }
}

