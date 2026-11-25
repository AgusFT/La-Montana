//#region ENCABEZADO · EvaluadorPrecioItem.java
/*
 * ========================================================================
 * ARCHIVO: EvaluadorPrecioItem.java
 * ========================================================================
 * FUNCIÓN
 * Calcula el precio de impresión y terminaciones. Las tarifas por hoja distinguen hojas con dos
 * caras y la última cara impar de cada copia; las tarifas históricas conservan el cálculo por
 * carilla.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] EvaluadorPrecioItem.Linea :: Linea(UUID servicio, String nombre, String base, long
 *   unidades, String precioUnitario, String importe)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * - [public] Precio calcular(CatalogoService.Estado catalogo, Item item)
 * - [private] OfertaServicio oferta(CatalogoService.Revision r, UUID servicio)
 * - [public] void limite(BigDecimal total)
 * - [private] String dinero(BigDecimal v)
 * - [private] void exigir(boolean condicion, String mensaje)
 * - [private] ResponseStatusException error(String mensaje)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - EvaluadorPrecioItem (class).
 * - EvaluadorPrecioItem.Item (record).
 * - EvaluadorPrecioItem.Linea (record).
 * - EvaluadorPrecioItem.Precio (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.catalogo;

import static ar.com.lamontana.catalogo.CatalogoController.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Precio puro y reutilizable de un PDF homogéneo. No persiste una cotización. */
@Component
public class EvaluadorPrecioItem {
    public record Item(@NotNull UUID servicio,@NotNull UUID formato,@NotNull UUID papel,@NotNull ModoColor color,
        @NotNull @Min(1) @Max(10000) Integer paginas,@NotNull @Min(1) @Max(10000) Integer copias,@NotNull Boolean dobleFaz,
        @NotNull @Size(max=100) List<@NotNull UUID> terminaciones){}
    public record Linea(UUID servicio,String nombre,String base,long unidades,String precioUnitario,String importe,
        @JsonInclude(JsonInclude.Include.NON_NULL) String detalle){
        public Linea(UUID servicio,String nombre,String base,long unidades,String precioUnitario,String importe){this(servicio,nombre,base,unidades,precioUnitario,importe,null);}
    }
    public record Precio(int paginas,int copias,int carillas,int hojas,List<Linea> lineas,String subtotal,int minimoPreparacionMinutos){}
    public Precio calcular(CatalogoService.Estado catalogo,Item item){
        exigir(catalogo.actual()!=null,"No existe una revisión comercial vigente.");
        exigir(item!=null&&item.paginas()!=null&&item.paginas()>=1&&item.paginas()<=10000&&item.copias()!=null&&item.copias()>=1&&item.copias()<=10000&&item.dobleFaz()!=null,"Indicá páginas, copias y caras válidas para el ejemplo.");
        exigir(item.terminaciones()!=null&&item.terminaciones().size()<=100&&item.terminaciones().stream().allMatch(Objects::nonNull)&&new HashSet<>(item.terminaciones()).size()==item.terminaciones().size(),"Las terminaciones no pueden repetirse ni quedar nulas.");
        var revision=catalogo.actual();var tipos=new HashMap<UUID,TipoServicio>();catalogo.servicios().forEach(s->tipos.put(s.codigoPublico(),s.tipo()));
        var impresion=oferta(revision,item.servicio());exigir(tipos.get(item.servicio())==TipoServicio.IMPRESION,"Elegí un servicio de impresión habilitado.");
        var tarifa=revision.tarifas().stream().filter(t->t.habilitada()&&t.formato().equals(item.formato())&&t.papel().equals(item.papel())&&t.color()==item.color()).findFirst().orElseThrow(()->error("Falta una tarifa habilitada para formato, papel y color."));
        int carillas=Math.multiplyExact(item.paginas(),item.copias()),hojas=Math.multiplyExact(item.dobleFaz()?(item.paginas()+1)/2:item.paginas(),item.copias());
        var lineas=new ArrayList<Linea>();BigDecimal subtotal=BigDecimal.ZERO;int minutos=impresion.preparacionMinutos();
        if(tarifa.modoDobleFaz()==null) {
            // Las revisiones previas conservan exactamente su precio por carilla.
            BigDecimal unitario=tarifa.precio().add(item.dobleFaz()?tarifa.recargoDobleFaz():BigDecimal.ZERO);
            subtotal=unitario.multiply(BigDecimal.valueOf(carillas));
            lineas.add(new Linea(item.servicio(),impresion.nombreVisible(),"POR_CARILLA",carillas,dinero(unitario),dinero(subtotal)));
        } else {
            // Una impresión por ítem: agregar por copia evita duplicar su identidad
            // en cotizacion_item_servicio y conserva cantidades de terminaciones.
            int dobles=item.dobleFaz()?item.paginas()/2:0;
            int simples=item.dobleFaz()?item.paginas()%2:item.paginas();
            BigDecimal precioDoble=PreciosTarifa.dobleFaz(tarifa);
            BigDecimal porCopia=precioDoble.multiply(BigDecimal.valueOf(dobles)).add(tarifa.precio().multiply(BigDecimal.valueOf(simples)));
            subtotal=porCopia.multiply(BigDecimal.valueOf(item.copias()));
            String detalle="Por copia: "+(dobles>0?dobles+" hojas doble faz a ARS "+dinero(precioDoble).replace('.',','):"")
                    +(dobles>0&&simples>0?" + ":"")+(simples>0?simples+" hojas simple faz a ARS "+dinero(tarifa.precio()).replace('.',','):"")+".";
            lineas.add(new Linea(item.servicio(),impresion.nombreVisible(),"POR_COPIA",item.copias(),dinero(porCopia),dinero(subtotal),detalle));
        }
        for(UUID id:item.terminaciones()){
            var s=oferta(revision,id);exigir(tipos.get(id)==TipoServicio.TERMINACION,"El adicional debe ser un servicio de terminación.");
            exigir(s.compatibilidades().contains(new Compatibilidad(item.formato(),item.papel())),"La terminación no admite ese formato y papel.");
            long unidades=switch(s.basePrecio()){case POR_COPIA->item.copias();case POR_HOJA->hojas;case POR_CARILLA->carillas;case FIJO_POR_ITEM->1;};
            BigDecimal importe=s.precio().multiply(BigDecimal.valueOf(unidades));subtotal=subtotal.add(importe);minutos=Math.max(minutos,s.preparacionMinutos());
            lineas.add(new Linea(id,s.nombreVisible(),s.basePrecio().name(),unidades,dinero(s.precio()),dinero(importe)));
        }
        limite(subtotal);return new Precio(item.paginas(),item.copias(),carillas,hojas,List.copyOf(lineas),dinero(subtotal),minutos);
    }
    private OfertaServicio oferta(CatalogoService.Revision r,UUID servicio){return r.servicios().stream().filter(s->s.habilitado()&&s.servicio().equals(servicio)).findFirst().orElseThrow(()->error("El servicio no tiene una oferta habilitada en la revisión vigente."));}
    public void limite(BigDecimal total){exigir(total.signum()>=0&&total.compareTo(new BigDecimal("99999999999999999.99"))<=0,"El total supera el límite técnico monetario; reducí el tamaño del ejemplo.");}
    private String dinero(BigDecimal v){return v.setScale(2,RoundingMode.HALF_UP).toPlainString();}
    private void exigir(boolean condicion,String mensaje){if(!condicion)throw error(mensaje);}
    private ResponseStatusException error(String mensaje){return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
}
