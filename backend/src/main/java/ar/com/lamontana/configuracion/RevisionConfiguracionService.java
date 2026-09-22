package ar.com.lamontana.configuracion;

import ar.com.lamontana.catalogo.*;
import static ar.com.lamontana.catalogo.CatalogoController.*;
import static ar.com.lamontana.configuracion.RevisionConfiguracionController.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RevisionConfiguracionService {
    private final JdbcTemplate jdbc;private final ConfiguracionService configuracion;private final CatalogoService catalogo;
    private final EntregaConfiguracionService entrega;private final PagosConfiguracionService pagos;private final EvaluadorFinanciero finanzas;private final EvaluadorPrecioItem precios;
    public RevisionConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion,CatalogoService catalogo,EntregaConfiguracionService entrega,PagosConfiguracionService pagos,EvaluadorFinanciero finanzas,EvaluadorPrecioItem precios){this.jdbc=jdbc;this.configuracion=configuracion;this.catalogo=catalogo;this.entrega=entrega;this.pagos=pagos;this.finanzas=finanzas;this.precios=precios;}
    public record Hallazgo(String nivel,String codigo,String area,int fase,String mensaje,UUID sucursal){}
    public record Sucursal(UUID codigoPublico,String nombre,boolean activa){}
    public record Opcion(UUID sucursal,UUID servicio,UUID formato,UUID papel,ModoColor color,boolean dobleFaz,List<UUID> terminaciones){}
    public record Revision(ConfiguracionService.Borrador borrador,CatalogoService.Estado catalogo,List<Sucursal> sucursales,boolean fase5Valida,List<Hallazgo> hallazgos,long bloqueos,long advertencias,List<Opcion> opciones,List<EntregaConfiguracionService.PuntoDisponible> puntosDisponibles,String huella){}
    private record Referencia(ConfiguracionService.Borrador borrador,CatalogoService.Estado catalogo,List<Sucursal> sucursales,List<Hallazgo> hallazgos,List<EntregaConfiguracionService.PuntoDisponible> puntos){}
    public record Impresora(UUID codigoPublico,String nombre){}
    public record Simulacion(long version,UUID revisionComercial,EvaluadorPrecioItem.Precio precio,String costoEntrega,String total,
        EvaluadorFinanciero.Simulacion finanzas,EvaluadorCalendario.Simulacion entrega,List<Impresora> impresoras,List<String> recorrido,List<String> observaciones){}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Revision revisar(UUID codigo,String correo){
        return evaluar(codigo,correo,false);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Revision revisarProgramada(UUID codigo,String correo){return evaluar(codigo,correo,true);}
    Revision evaluar(UUID codigo,String correo,boolean programada){return evaluar(codigo,correo,programada,false);}
    Revision evaluarHistorica(UUID codigo,String correo){return evaluar(codigo,correo,false,true);}
    private Revision evaluar(UUID codigo,String correo,boolean programada,boolean historica){
        configuracion.propietario(correo);var b=configuracion.cargar(codigo);var comercial=catalogo.leerEstado();var h=new ArrayList<Hallazgo>();
        var sucursales=jdbc.query("SELECT codigo_publico,nombre,estado='ACTIVA' FROM lamontana.sucursal ORDER BY codigo",(r,n)->new Sucursal(r.getObject(1,UUID.class),r.getString(2),r.getBoolean(3)));
        if(!b.estado().equals(historica?"HISTORICA":programada?"PROGRAMADA":"EN_PREPARACION"))bloqueo(h,"BORRADOR_NO_EDITABLE","modelo",1,"Sólo una configuración en preparación puede revisarse para activar.",null);
        if(b.copia()!=null&&!programada&&!historica)for(int fase=2;fase<=5;fase++)if(!b.copia().fasesConfirmadas().contains(fase))bloqueo(h,"RECONFIRMAR_FASE_"+fase,switch(fase){case 2->"modelo";case 3->"pagos";case 4->"recursos";default->"horarios";},fase,"La copia requiere revisar y guardar nuevamente la fase "+fase+".",null);
        if(b.modelo()==null)bloqueo(h,"MODELO_PENDIENTE","modelo",2,"Elegí el modelo operativo y su condición para definir el recorrido de aprobación.",null);
        if(b.pagos()==null)bloqueo(h,"PAGOS_PENDIENTES","pagos",3,"Completá medios, vigencia y parámetros financieros antes de cotizar.",null);
        else if(b.modelo()!=null)try{pagos.comprobar(b);}catch(ResponseStatusException ex){bloqueo(h,"PAGOS_INCOMPATIBLES","pagos",3,ex.getReason(),null);}
        if(b.recursos().metodoAsignacion()==null)bloqueo(h,"ASIGNACION_PENDIENTE","recursos",4,"Confirmá la asignación manual de trabajos.",null);
        var activas=new HashSet<UUID>();sucursales.stream().filter(Sucursal::activa).forEach(s->activas.add(s.codigoPublico()));
        var impresoras=b.recursos().impresoras().stream().filter(p->p.estado().equals("OPERATIVA")&&activas.contains(p.sucursal())).toList();
        if(impresoras.isEmpty())bloqueo(h,"SIN_IMPRESORA_OPERATIVA","recursos",4,"Declarar una impresora Operativa en una sucursal activa es obligatorio. No necesita conexión CUPS.",null);
        var entregaActual=historica?entrega.evaluarHistorica(codigo,correo):entrega.evaluar(codigo,correo,programada);
        for(var p:entregaActual.problemas())bloqueo(h,p.codigo(),p.codigo().startsWith("HORARIO")||p.codigo().contains("OPERATIVO")||p.codigo().contains("PREPARACION")||p.codigo().contains("TRASLADO")?"horarios":"entrega",5,p.mensaje(),p.sucursal());
        for(String aviso:entregaActual.avisos())h.add(new Hallazgo("ADVERTENCIA","MODALIDAD_SIN_DESTINO","entrega",5,aviso,null));
        var opciones=opciones(b,comercial,activas);var revision=comercial.actual();
        if(revision==null)bloqueo(h,"CATALOGO_PENDIENTE","catalogo",0,"Activá una revisión comercial válida. Una revisión programada para el futuro todavía no sirve para cotizar.",null);
        var tipos=new HashMap<UUID,TipoServicio>();comercial.servicios().forEach(s->tipos.put(s.codigoPublico(),s.tipo()));
        for(var origen:b.recursos().serviciosPorSucursal()){
            String nombre=sucursales.stream().filter(s->s.codigoPublico().equals(origen.sucursal())).map(Sucursal::nombre).findFirst().orElse("Sucursal");
            if(!activas.contains(origen.sucursal())){h.add(new Hallazgo("ADVERTENCIA","SUCURSAL_DESACTIVADA","recursos",4,nombre+" está desactivada; sus capacidades no se ofrecerán.",origen.sucursal()));continue;}
            if(origen.servicios().stream().noneMatch(id->tipos.get(id)==TipoServicio.IMPRESION))bloqueo(h,"IMPRESION_SUCURSAL_PENDIENTE","recursos",4,nombre+" necesita habilitar un servicio de impresión para recibir pedidos PDF.",origen.sucursal());
            if(revision==null)continue;
            var locales=opciones.stream().filter(o->o.sucursal().equals(origen.sucursal())).toList();
            for(UUID servicio:origen.servicios()){
                var oferta=revision.servicios().stream().filter(s->s.servicio().equals(servicio)&&s.habilitado()).findFirst();String servicioNombre=comercial.servicios().stream().filter(s->s.codigoPublico().equals(servicio)).map(CatalogoService.Servicio::nombre).findFirst().orElse("Servicio");
                if(oferta.isEmpty())bloqueo(h,"SERVICIO_SIN_PRECIO_VIGENTE","catalogo",0,nombre+": "+servicioNombre+" está habilitado localmente pero no tiene oferta/precio vigente. Completá el catálogo o quitá el servicio de esa sucursal.",origen.sucursal());
                else if(locales.stream().noneMatch(o->o.servicio().equals(servicio)||o.terminaciones().contains(servicio)))bloqueo(h,"SERVICIO_SIN_CAPACIDAD","recursos",4,nombre+": "+servicioNombre+" no tiene una combinación tarifada compatible con una impresora Operativa. Revisá formatos, color y servicios.",origen.sucursal());
            }
        }
        if(revision!=null){
            long sinOrigen=revision.tarifas().stream().filter(Tarifa::habilitada).filter(t->opciones.stream().noneMatch(o->o.formato().equals(t.formato())&&o.papel().equals(t.papel())&&o.color()==t.color())).count();
            if(sinOrigen>0)h.add(new Hallazgo("ADVERTENCIA","TARIFAS_SIN_ORIGEN","catalogo",0,sinOrigen+" tarifas habilitadas no tienen un origen compatible; esas combinaciones no se ofrecerán.",null));
        }
        if(comercial.programada()!=null)h.add(new Hallazgo("ADVERTENCIA","CATALOGO_PROGRAMADO","catalogo",0,"Existe una revisión comercial programada. Este resumen usa la vigente; los precios se volverán a comprobar al activar y al cotizar.",null));
        h.add(new Hallazgo("INFORMACION","REFERENCIAS_INDEPENDIENTES","catalogo",0,"La revisión comercial es independiente del motor. Las cotizaciones y pedidos conservarán sus precios capturados; esta revisión no crea ni modifica pedidos.",null));
        h.add(new Hallazgo("INFORMACION","OPERACION_MANUAL","recursos",4,"La producción requiere asignación y registro manual. CUPS, pasarelas de pago y oferta de pedidos siguen En construcción.",null));
        return new Revision(b,comercial,List.copyOf(sucursales),entregaActual.valida(),List.copyOf(h),h.stream().filter(x->x.nivel().equals("BLOQUEO")).count(),h.stream().filter(x->x.nivel().equals("ADVERTENCIA")).count(),opciones,entregaActual.puntosDisponibles(),configuracion.huella(new Referencia(b,comercial,sucursales,h,entregaActual.puntosDisponibles())));
    }
    private List<Opcion> opciones(ConfiguracionService.Borrador b,CatalogoService.Estado c,Set<UUID> activas){
        if(c.actual()==null)return List.of();var tipos=new HashMap<UUID,TipoServicio>();c.servicios().forEach(s->tipos.put(s.codigoPublico(),s.tipo()));var resultado=new ArrayList<Opcion>();
        for(var origen:b.recursos().serviciosPorSucursal())if(activas.contains(origen.sucursal()))for(var servicio:c.actual().servicios())if(servicio.habilitado()&&tipos.get(servicio.servicio())==TipoServicio.IMPRESION&&origen.servicios().contains(servicio.servicio()))for(var tarifa:c.actual().tarifas())if(tarifa.habilitada()){
            var compatibles=b.recursos().impresoras().stream().filter(p->compatible(p,origen.sucursal(),tarifa.formato(),tarifa.color(),false)).toList();if(compatibles.isEmpty())continue;
            var adicionales=c.actual().servicios().stream().filter(s->s.habilitado()&&tipos.get(s.servicio())==TipoServicio.TERMINACION&&origen.servicios().contains(s.servicio())&&s.compatibilidades().contains(new Compatibilidad(tarifa.formato(),tarifa.papel()))).map(OfertaServicio::servicio).toList();
            resultado.add(new Opcion(origen.sucursal(),servicio.servicio(),tarifa.formato(),tarifa.papel(),tarifa.color(),compatibles.stream().anyMatch(RecursosRepositorio.Impresora::admiteDobleFaz),adicionales));
        }
        return List.copyOf(resultado);
    }
    private boolean compatible(RecursosRepositorio.Impresora p,UUID sucursal,UUID formato,ModoColor color,boolean doble){return p.estado().equals("OPERATIVA")&&p.sucursal().equals(sucursal)&&p.formatos().contains(formato)&&(color==ModoColor.BLANCO_NEGRO||p.admiteColor())&&(!doble||p.admiteDobleFaz());}
    private void bloqueo(List<Hallazgo> h,String codigo,String area,int fase,String mensaje,UUID sucursal){h.add(new Hallazgo("BLOQUEO",codigo,area,fase,mensaje,sucursal));}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Simulacion simular(UUID codigo,Ejemplo input,String correo){
        var r=revisar(codigo,correo);var b=r.borrador();
        if(b.version()!=input.version())throw conflicto("El borrador cambió. Actualizá la revisión antes de simular.");
        if(r.catalogo().actual()==null||!r.catalogo().actual().codigoPublico().equals(input.revisionComercial()))throw conflicto("La referencia comercial cambió. Actualizá la revisión y volvé a elegir el ejemplo.");
        if(r.bloqueos()>0)throw conflicto("Corregí los bloqueos de la revisión integral antes de simular el recorrido completo.");
        var item=input.item();var opcion=r.opciones().stream().filter(o->o.sucursal().equals(input.sucursal())&&o.servicio().equals(item.servicio())&&o.formato().equals(item.formato())&&o.papel().equals(item.papel())&&o.color()==item.color()&&(!item.dobleFaz()||o.dobleFaz())&&o.terminaciones().containsAll(item.terminaciones())).findFirst();
        if(opcion.isEmpty())throw conflicto("El ejemplo no tiene servicio, tarifa y capacidad compatibles en esa sucursal.");
        var precio=precios.calcular(r.catalogo(),item);
        var temporal=entrega.simular(codigo,new EntregaConfiguracionController.SimularEntrega(input.version(),input.sucursal(),input.modalidad(),input.recibidoEn(),input.punto(),input.territorio()),correo,precio.minimoPreparacionMinutos());
        String costo=temporal.destinoPunto()!=null?temporal.destinoPunto().costo():temporal.destinoZona()!=null?temporal.destinoZona().costo():"0.00";
        BigDecimal total=new BigDecimal(precio.subtotal()).add(new BigDecimal(costo));precios.limite(total);
        var economica=finanzas.evaluar(b.modelo(),b.criterio(),b.pagos(),b.version(),total,precio.carillas());
        var impresoras=b.recursos().impresoras().stream().filter(p->compatible(p,input.sucursal(),item.formato(),item.color(),item.dobleFaz())).map(p->new Impresora(p.codigoPublico(),p.nombre())).toList();
        var recorrido=new ArrayList<String>();recorrido.add("Cotización de ejemplo con la revisión comercial "+r.catalogo().actual().numero()+": ARS "+total.toPlainString()+". Vigencia configurada: "+b.pagos().vigenciaCotizacionMinutos()+" minutos.");
        recorrido.addAll(economica.instrucciones());recorrido.add(economica.cargaRequiereAcreditacion()?"Archivo: carga del PDF bloqueada hasta acreditar el importe requerido; después se inspecciona su contenido real.":"Archivo: carga e inspección real del PDF; las discrepancias y correcciones se resuelven antes de producir.");
        if(economica.revisionHumana())recorrido.add("Revisión humana: aprobar o pedir correcciones. Si la seña se exige después de aprobar, producción espera su acreditación.");
        recorrido.add("Reloj del ejemplo: supone ya resueltos los requisitos anteriores; preparación comienza según la apertura del origen. El mínimo de los servicios seleccionados también se respeta.");
        recorrido.add("Asignación manual a una impresora compatible; registrar producción, control de calidad y eventual reimpresión. Ningún trabajo se envía a CUPS.");
        recorrido.add(switch(input.modalidad()){case RETIRO_SUCURSAL->"Calidad aprobada → Listo para entregar en local; no pasa por En viaje.";case RETIRO_PUNTO_ENTREGA->"Calidad aprobada → En viaje al punto → llegada/listo para retirar → entrega efectiva al cliente.";case ENVIO_DOMICILIO->"Calidad aprobada → En viaje al domicilio → entrega efectiva al cliente.";});
        recorrido.add("Antes de entregar se verifican el saldo y el código. La entrega efectiva y el cierre financiero se registran por separado.");
        var notas=new ArrayList<>(temporal.advertencias());notas.add("El ejemplo usa datos declarados de un PDF homogéneo; no valida ni carga un archivo, ni crea cotización, pedido, reserva o pago.");notas.add("Los mínimos de preparación de servicios no se suman: se respeta el mayor y el tiempo global. Los avances reales pueden adelantar o demorar el recorrido.");
        return new Simulacion(b.version(),r.catalogo().actual().codigoPublico(),precio,costo,total.toPlainString(),economica,temporal,impresoras,List.copyOf(recorrido),List.copyOf(notas));
    }
    private ResponseStatusException conflicto(String mensaje){return new ResponseStatusException(HttpStatus.CONFLICT,mensaje);}
}
