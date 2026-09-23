package ar.com.lamontana;
import static org.assertj.core.api.Assertions.*;
import static ar.com.lamontana.catalogo.CatalogoController.*;
import static ar.com.lamontana.configuracion.ConfiguracionController.*;
import ar.com.lamontana.catalogo.*;
import ar.com.lamontana.configuracion.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
class EvaluadorPrecioItemTest {
 private final UUID imp=UUID.randomUUID(),f=UUID.randomUUID(),p=UUID.randomUUID();
 private final EvaluadorPrecioItem precios=new EvaluadorPrecioItem();
 private CatalogoService.Estado catalogo(String precio,String recargo,List<OfertaServicio> extras){
  var tipos=new ArrayList<CatalogoService.Servicio>();tipos.add(new CatalogoService.Servicio(imp,"IMP","Impresión",TipoServicio.IMPRESION,null));
  var ofertas=new ArrayList<OfertaServicio>();ofertas.add(new OfertaServicio(imp,"Impresión",BasePrecio.POR_CARILLA,BigDecimal.ZERO,15,true,List.of()));
  for(var e:extras){tipos.add(new CatalogoService.Servicio(e.servicio(),e.nombreVisible(),e.nombreVisible(),TipoServicio.TERMINACION,null));ofertas.add(e);}
  var r=new CatalogoService.Revision(UUID.randomUUID(),1,"Prueba",Instant.EPOCH,"Prueba",List.of(new Tarifa(f,p,ModoColor.BLANCO_NEGRO,new BigDecimal(precio),new BigDecimal(recargo),true)),ofertas,CatalogoService.EstadoRevision.VIGENTE,null,Instant.EPOCH);
  return new CatalogoService.Estado(List.of(),List.of(),tipos,r,List.of(),null,List.of(),List.of());
 }
 private EvaluadorPrecioItem.Item item(int paginas,int copias,boolean doble,List<UUID> extras){return new EvaluadorPrecioItem.Item(imp,f,p,ModoColor.BLANCO_NEGRO,paginas,copias,doble,extras);}
 @Test void basesExactasHojasSeparadasPorCopiaYMinimoMayor(){
  var extras=new ArrayList<OfertaServicio>();for(var base:BasePrecio.values())extras.add(new OfertaServicio(UUID.randomUUID(),base.name(),base,new BigDecimal("0.25"),30+extras.size()*10,true,List.of(new Compatibilidad(f,p))));
  var r=precios.calcular(catalogo("0.10","0.05",extras),item(3,2,true,extras.stream().map(OfertaServicio::servicio).toList()));
  assertThat(r.carillas()).isEqualTo(6);assertThat(r.hojas()).isEqualTo(4);assertThat(r.lineas()).extracting(EvaluadorPrecioItem.Linea::unidades).containsExactly(6L,2L,4L,6L,1L);assertThat(r.subtotal()).isEqualTo("4.15");assertThat(r.minimoPreparacionMinutos()).isEqualTo(60);
  assertThat(precios.calcular(catalogo("0.10","0.05",List.of()),item(3,2,false,List.of())).subtotal()).isEqualTo("0.60");
 }
 @Test void entradasCompatibilidadYLimiteMonetario(){
  var e=new OfertaServicio(UUID.randomUUID(),"Acabado",BasePrecio.FIJO_POR_ITEM,BigDecimal.ONE,0,true,List.of(new Compatibilidad(UUID.randomUUID(),p)));var c=catalogo("1","0",List.of(e));
  for(var i:List.of(item(0,1,false,List.of()),item(1,10001,false,List.of()),item(1,1,false,List.of(e.servicio())),item(1,1,false,List.of(e.servicio(),e.servicio()))))assertThatThrownBy(()->precios.calcular(c,i)).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->precios.calcular(catalogo("999999999999.99","0",List.of()),item(10000,10000,false,List.of()))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("límite técnico");
 }
 @Test void totalCeroNoRequiereAcreditacionParaCargarPdf(){
  var r=precios.calcular(catalogo("0","0",List.of()),item(3,2,true,List.of()));assertThat(r.subtotal()).isEqualTo("0.00");
  var pagos=new Pagos(List.of(MedioPago.TRANSFERENCIA),"Cuenta",60,false,null,null,null,null,null);
  var financiero=new EvaluadorFinanciero().evaluar(Modelo.CONDICIONAL,Criterio.PAGO_PREVIO,pagos,1,new BigDecimal(r.subtotal()),r.carillas());assertThat(financiero.cargaRequiereAcreditacion()).isFalse();assertThat(financiero.pagoPrevioRequerido()).isEqualTo("0.00");assertThat(financiero.mediosAcreditacion()).isEmpty();
 }
}
