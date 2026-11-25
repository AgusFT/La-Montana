//#region ENCABEZADO · OpcionesImpresion.java
/*
 * ========================================================================
 * ARCHIVO: OpcionesImpresion.java
 * ========================================================================
 * FUNCIÓN
 * Deriva combinaciones de impresión globales cruzando catálogo, tarifas e impresoras compatibles.
 * Mientras el contrato cliente conserve un destino previo, replica la misma oferta para cada
 * sucursal activa sin convertirla en origen productivo ni limitar capacidades.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [private] OpcionesImpresion()
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public, static] List<RevisionConfiguracionService.Opcion>
 *   calcular(ConfiguracionService.Borrador b, CatalogoService.Estado c, Set<UUID> activas)
 * - [public, static] boolean compatible(RecursosRepositorio.Impresora p, UUID sucursal, UUID
 *   formato, ModoColor color, boolean doble)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - OpcionesImpresion (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import ar.com.lamontana.catalogo.*;
import static ar.com.lamontana.catalogo.CatalogoController.*;
import java.util.*;

/** La misma intersección de catálogo y capacidades sirve a simulación y cotización. */
public final class OpcionesImpresion {
    private OpcionesImpresion() {}
    public static List<RevisionConfiguracionService.Opcion> calcular(ConfiguracionService.Borrador b,CatalogoService.Estado c,Set<UUID> activas){
        if(c.actual()==null)return List.of();var tipos=new HashMap<UUID,TipoServicio>();c.servicios().forEach(s->tipos.put(s.codigoPublico(),s.tipo()));var resultado=new ArrayList<RevisionConfiguracionService.Opcion>();
        for(var servicio:c.actual().servicios())if(servicio.habilitado()&&tipos.get(servicio.servicio())==TipoServicio.IMPRESION)for(var tarifa:c.actual().tarifas())if(tarifa.habilitada()){
            var compatibles=b.recursos().impresoras().stream().filter(p->compatible(p,tarifa.formato(),tarifa.color(),false)).toList();if(compatibles.isEmpty())continue;
            var adicionales=c.actual().servicios().stream().filter(s->s.habilitado()&&tipos.get(s.servicio())==TipoServicio.TERMINACION&&s.compatibilidades().contains(new Compatibilidad(tarifa.formato(),tarifa.papel()))).map(OfertaServicio::servicio).toList();
            for(UUID destino:activas)resultado.add(new RevisionConfiguracionService.Opcion(destino,servicio.servicio(),tarifa.formato(),tarifa.papel(),tarifa.color(),compatibles.stream().anyMatch(RecursosRepositorio.Impresora::admiteDobleFaz),adicionales));
        }
        return List.copyOf(resultado);
    }
    public static boolean compatible(RecursosRepositorio.Impresora p,UUID formato,ModoColor color,boolean doble){return p.estado().equals("OPERATIVA")&&p.formatos().contains(formato)&&(color==ModoColor.BLANCO_NEGRO||p.admiteColor())&&(!doble||p.admiteDobleFaz());}
}
