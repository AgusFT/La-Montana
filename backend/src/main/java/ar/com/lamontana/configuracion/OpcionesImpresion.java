package ar.com.lamontana.configuracion;

import ar.com.lamontana.catalogo.*;
import static ar.com.lamontana.catalogo.CatalogoController.*;
import java.util.*;

/** La misma intersección de catálogo y capacidades sirve a simulación y cotización. */
public final class OpcionesImpresion {
    private OpcionesImpresion() {}
    public static List<RevisionConfiguracionService.Opcion> calcular(ConfiguracionService.Borrador b,CatalogoService.Estado c,Set<UUID> activas){
        if(c.actual()==null)return List.of();var tipos=new HashMap<UUID,TipoServicio>();c.servicios().forEach(s->tipos.put(s.codigoPublico(),s.tipo()));var resultado=new ArrayList<RevisionConfiguracionService.Opcion>();
        for(var origen:b.recursos().serviciosPorSucursal())if(activas.contains(origen.sucursal()))for(var servicio:c.actual().servicios())if(servicio.habilitado()&&tipos.get(servicio.servicio())==TipoServicio.IMPRESION&&origen.servicios().contains(servicio.servicio()))for(var tarifa:c.actual().tarifas())if(tarifa.habilitada()){
            var compatibles=b.recursos().impresoras().stream().filter(p->compatible(p,origen.sucursal(),tarifa.formato(),tarifa.color(),false)).toList();if(compatibles.isEmpty())continue;
            var adicionales=c.actual().servicios().stream().filter(s->s.habilitado()&&tipos.get(s.servicio())==TipoServicio.TERMINACION&&origen.servicios().contains(s.servicio())&&s.compatibilidades().contains(new Compatibilidad(tarifa.formato(),tarifa.papel()))).map(OfertaServicio::servicio).toList();
            resultado.add(new RevisionConfiguracionService.Opcion(origen.sucursal(),servicio.servicio(),tarifa.formato(),tarifa.papel(),tarifa.color(),compatibles.stream().anyMatch(RecursosRepositorio.Impresora::admiteDobleFaz),adicionales));
        }
        return List.copyOf(resultado);
    }
    public static boolean compatible(RecursosRepositorio.Impresora p,UUID sucursal,UUID formato,ModoColor color,boolean doble){return p.estado().equals("OPERATIVA")&&p.sucursal().equals(sucursal)&&p.formatos().contains(formato)&&(color==ModoColor.BLANCO_NEGRO||p.admiteColor())&&(!doble||p.admiteDobleFaz());}
}
