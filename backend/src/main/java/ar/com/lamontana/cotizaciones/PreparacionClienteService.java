package ar.com.lamontana.cotizaciones;

import static ar.com.lamontana.cotizaciones.PreparacionClienteStore.*;
import ar.com.lamontana.archivos.*;
import ar.com.lamontana.catalogo.EvaluadorPrecioItem;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad;
import ar.com.lamontana.pedidos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reutiliza cotización, aceptación y confirmación. El navegador nunca fija importes ni origen productivo. */
@Service
public class PreparacionClienteService {
    private final PreparacionClienteStore store;private final ArchivoPreparadoService archivos;private final CotizacionService cotizaciones;private final OfertaOperativaService operativa;private final PedidoService pedidos;private final JdbcTemplate jdbc;
    public PreparacionClienteService(PreparacionClienteStore store,ArchivoPreparadoService archivos,CotizacionService cotizaciones,OfertaOperativaService operativa,PedidoService pedidos,JdbcTemplate jdbc){this.store=store;this.archivos=archivos;this.cotizaciones=cotizaciones;this.operativa=operativa;this.pedidos=pedidos;this.jdbc=jdbc;}
    public record Entrada(@NotNull UUID archivo,@NotNull @Valid EvaluadorPrecioItem.Item trabajo){}
    public record Presupuestar(@NotNull UUID operacion,@Positive long version,@NotNull UUID configuracion,@NotNull UUID revisionComercial,@NotNull @Size(min=1,max=20) List<@NotNull @Valid Entrada> items){}
    public record Preferencias(@NotNull Modalidad modalidad,UUID destinoRetiro,UUID punto,@Valid CotizacionController.Direccion direccion,@NotNull MedioPago medioPago){}
    public record Cotizar(@NotNull UUID operacion,@Positive long version,@NotNull @Valid Preferencias preferencias){}
    public record Confirmar(@NotNull UUID operacion,@Positive long version,@NotNull UUID cotizacion,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String huella,@AssertTrue boolean condicionesAceptadas,@Valid PedidoController.Contacto contacto){}
    public record Vista(UUID codigoPublico,long version,String estado,List<ArchivoService.Archivo> archivos,List<Entrada> items,CotizacionService.Presupuesto presupuesto,Preferencias preferencias,UUID cotizacion,UUID pedido){}
    public record Checkout(Vista preparacion,CotizacionService.Detalle cotizacion,PedidoService.Revision revision,String huella){}
    @Transactional public Vista abrir(String correo){return vista(store.abrir(correo),correo);}
    @Transactional public Vista consultar(UUID codigo,String correo){return vista(store.datos(codigo,correo,false),correo);}
    private List<Entrada> entradas(String value){return Arrays.asList(store.mapper().readValue(value,Entrada[].class));}
    private Vista vista(PreparacionClienteStore.Datos d,String correo){return new Vista(d.codigoPublico(),d.version(),d.estado(),archivos.lista(d.codigoPublico(),correo),entradas(d.items()),d.presupuesto()==null?null:store.mapper().readValue(d.presupuesto(),CotizacionService.Presupuesto.class),d.preferencias()==null?null:store.mapper().readValue(d.preferencias(),Preferencias.class),d.cotizacion(),d.pedido());}
    private void editable(PreparacionClienteStore.Datos d){if(!d.estado().equals("ABIERTA"))throw conflicto("El trabajo ya tiene un pedido. Consultá su seguimiento.");}
    private List<CotizacionController.Item> documentos(PreparacionClienteStore.Datos d,List<Entrada> entradas,String correo){
        if(archivos.lista(d.codigoPublico(),correo).size()!=entradas.size())throw conflicto("Incluí todos los PDF del trabajo o quitá los que no quieras imprimir.");
        var ids=new HashSet<UUID>();var out=new ArrayList<CotizacionController.Item>();
        for(var in:entradas){if(!ids.add(in.archivo()))throw error(HttpStatus.BAD_REQUEST,"No repitas el mismo PDF.");var f=archivos.archivo(d.codigoPublico(),in.archivo(),correo);if(!f.activo()||!f.estado().equals("VALIDO")||f.paginas()!=in.trabajo().paginas())throw conflicto("El PDF debe estar inspeccionado y coincidir con las páginas reales.");out.add(new CotizacionController.Item(new CotizacionController.Documento(f.nombre(),f.bytes(),f.sha256()),in.trabajo()));}return List.copyOf(out);
    }
    @Transactional public Vista presupuestar(UUID codigo,Presupuestar in,String correo){
        store.bloquear();var d=store.datos(codigo,correo,false);if(store.previo(d,in.operacion(),"PRESUPUESTAR",in)!=null)return vista(d,correo);editable(d);store.version(d,in.version());
        var budget=cotizaciones.presupuestar(in.configuracion(),in.revisionComercial(),documentos(d,in.items(),correo),correo);
        jdbc.update("UPDATE lamontana.preparacion_cliente SET items=?::jsonb,presupuesto=?::jsonb,preferencias=NULL,version=version+1,actualizada_en=clock_timestamp() WHERE id_preparacion=?",store.mapper().writeValueAsString(in.items()),store.mapper().writeValueAsString(budget),d.id());store.evento(d,in.operacion(),"PRESUPUESTAR",in,budget);return consultar(codigo,correo);
    }
    @Transactional public Checkout checkout(UUID codigo,String correo){var d=store.datos(codigo,correo,false);return checkout(d,correo,d.cotizacion());}
    private Checkout checkout(PreparacionClienteStore.Datos d,String correo,UUID quote){
        var q=quote==null?null:cotizaciones.detalle(quote,correo);PedidoService.Revision r=null;
        if(q!=null&&q.estado().equals("VIGENTE")&&q.reemplazadaPor()==null&&Instant.now().isBefore(q.vigenteHasta()))r=pedidos.revisar(q.codigoPublico(),correo);
        return new Checkout(vista(d,correo),q,r,r==null?null:firma(q,r));
    }
    private String firma(CotizacionService.Detalle q,PedidoService.Revision r){if(r.condiciones()==null||r.franja()==null)return null;var f=r.franja();return store.huella(Arrays.asList(q.codigoPublico(),q.oferta(),r.configuracion(),r.condiciones(),Arrays.asList(f.modalidad(),f.destino(),f.origen(),f.zonaHoraria(),f.fecha(),f.apertura(),f.cierre(),f.desde(),f.hasta(),f.cupoConfigurado(),f.costo()),r.archivos()));}
    @Transactional public Checkout cotizar(UUID codigo,Cotizar in,String correo){
        store.bloquear();var d=store.datos(codigo,correo,false);String previo=store.previo(d,in.operacion(),"COTIZAR",in);if(previo!=null){UUID previa=UUID.fromString(store.mapper().readTree(previo).get("cotizacion").asString());if(!Objects.equals(previa,d.cotizacion())||d.presupuesto()==null||d.preferencias()==null)throw conflicto("Esa cotización fue sustituida o el trabajo cambió. Recuperá el resumen actual.");return checkout(d,correo,previa);}editable(d);store.version(d,in.version());
        if(d.presupuesto()==null)throw conflicto("Revisá primero el presupuesto del trabajo.");
        var budget=store.mapper().readValue(d.presupuesto(),CotizacionService.Presupuesto.class);var pref=in.preferencias();var contexto=operativa.leer();UUID matriz=cotizaciones.matriz(contexto);
        if(pref.modalidad()==Modalidad.RETIRO_SUCURSAL&&pref.destinoRetiro()==null||pref.modalidad()!=Modalidad.RETIRO_SUCURSAL&&pref.destinoRetiro()!=null)throw error(HttpStatus.BAD_REQUEST,"Elegí un destino de retiro sólo para esa modalidad.");
        if(d.preferencias()!=null&&store.mapper().readValue(d.preferencias(),Preferencias.class).equals(pref)&&d.cotizacion()!=null){var q=cotizaciones.detalle(d.cotizacion(),correo);if(q.estado().equals("VIGENTE")&&q.reemplazadaPor()==null&&Instant.now().isBefore(q.vigenteHasta())){store.evento(d,in.operacion(),"COTIZAR",in,Map.of("cotizacion",q.codigoPublico()));return checkout(d,correo,q.codigoPublico());}}
        var entrada=entradas(d.items());var items=documentos(d,entrada,correo);
        var crear=new CotizacionController.Crear(derivada(in.operacion(),"cotizacion"),budget.configuracion(),budget.revisionComercial(),matriz,items,pref.modalidad(),pref.punto(),pref.direccion(),pref.medioPago(),d.cotizacion(),pref.destinoRetiro(),true);
        var q=cotizaciones.crear(crear,correo);
        for(int n=0;n<entrada.size();n++){
            UUID file=entrada.get(n).archivo(),item=q.oferta().items().get(n).codigoPublico();
            jdbc.update("INSERT INTO lamontana.archivo_trabajo(id_archivo_almacenado,id_cotizacion_item,activo) SELECT a.id_archivo_almacenado,i.id_cotizacion_item,true FROM lamontana.archivo_almacenado a,lamontana.cotizacion_item i WHERE a.codigo_publico=? AND i.codigo_publico=?",file,item);
        }
        jdbc.update("UPDATE lamontana.preparacion_cliente SET preferencias=?::jsonb,id_ultima_cotizacion=(SELECT id_cotizacion FROM lamontana.cotizacion WHERE codigo_publico=?),version=version+1,actualizada_en=clock_timestamp() WHERE id_preparacion=?",store.mapper().writeValueAsString(pref),q.codigoPublico(),d.id());store.evento(d,in.operacion(),"COTIZAR",in,Map.of("cotizacion",q.codigoPublico()));return checkout(store.datos(codigo,correo,true),correo,q.codigoPublico());
    }
    @Transactional public PedidoService.Detalle confirmar(UUID codigo,Confirmar in,String correo){
        store.bloquear();var d=store.datos(codigo,correo,false);var anterior=store.previo(d,in.operacion(),"CONFIRMAR",in);if(anterior!=null)return pedidos.detalle(UUID.fromString(store.mapper().readTree(anterior).get("pedido").asString()),correo,false);
        editable(d);store.version(d,in.version());if(!in.condicionesAceptadas()||!Objects.equals(d.cotizacion(),in.cotizacion())||d.preferencias()==null||d.presupuesto()==null)throw conflicto("Revisá el resumen final vigente antes de confirmar.");
        var actual=checkout(d,correo,in.cotizacion());if(!Objects.equals(in.huella(),actual.huella()))throw conflicto("Cambiaron la entrega, los documentos o las condiciones. Revisá y aceptá el resumen actualizado.");
        var q=actual.cotizacion();if(q.aceptadaEn()==null)q=cotizaciones.decidir(q.codigoPublico(),new CotizacionController.Decision(derivada(in.operacion(),"aceptacion"),q.version(),null),true,correo);
        var r=pedidos.revisar(q.codigoPublico(),correo);if(!r.habilitada())throw conflicto(String.join(" ",r.bloqueos()));if(!Objects.equals(in.huella(),firma(q,r)))throw conflicto("Las condiciones cambiaron durante la confirmación. Actualizá el resumen.");
        var pedido=pedidos.confirmar(q.codigoPublico(),new PedidoController.Confirmar(derivada(in.operacion(),"pedido"),r.version(),r.huella(),true,in.contacto()),correo);
        jdbc.update("UPDATE lamontana.preparacion_cliente SET estado='CONFIRMADA',id_pedido=?,version=version+1,actualizada_en=clock_timestamp() WHERE id_preparacion=?",pedido.numero(),d.id());store.evento(d,in.operacion(),"CONFIRMAR",in,Map.of("pedido",pedido.codigoPublico()));return pedido;
    }
    private UUID derivada(UUID operacion,String paso){return UUID.nameUUIDFromBytes((operacion+":"+paso).getBytes(StandardCharsets.UTF_8));}
}
