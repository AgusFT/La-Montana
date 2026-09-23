//#region ENCABEZADO · EvaluadorPrecioItemTest.java
/*
 * ========================================================================
 * ARCHIVO: EvaluadorPrecioItemTest.java
 * ========================================================================
 * FUNCIÓN
 * Verifica cálculos anteriores por carilla y nuevos por hoja, valores independientes, adicionales,
 * porcentajes, redondeo, páginas impares, copias y terminaciones.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - No declara explícitamente.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * - [private] CatalogoService.Estado catalogo(String precio, String recargo, List<OfertaServicio>
 *   extras)
 * - [private] EvaluadorPrecioItem.Item item(int paginas, int copias, boolean doble, List<UUID>
 *   extras)
 * - [] void basesExactasHojasSeparadasPorCopiaYMinimoMayor()
 * - [] void entradasCompatibilidadYLimiteMonetario()
 * - [] void totalCeroNoRequiereAcreditacionParaCargarPdf()
 * - [private] CatalogoService.Estado porHoja(String simple, ModoDobleFaz modo, String valor,
 *   List<OfertaServicio> extras)
 * - [] void precioFinalIndependienteParesImparesYCopias()
 * - [] void relativosRedondeanPorHojaYSimpleFazNoCambia()
 * - [] void terminacionesConservanCantidadesConTarifaPorHoja()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - EvaluadorPrecioItemTest (class).
 * ========================================================================
 */
//#endregion

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
 private CatalogoService.Estado porHoja(String simple,ModoDobleFaz modo,String valor,List<OfertaServicio> extras){
  var c=catalogo(simple,"0",extras);var r=c.actual();
  var t=new Tarifa(f,p,ModoColor.BLANCO_NEGRO,new BigDecimal(simple),BigDecimal.ZERO,true,UUID.randomUUID(),"Tarifa por hoja",modo,new BigDecimal(valor));
  var nueva=new CatalogoService.Revision(r.codigoPublico(),r.numero(),r.motivo(),r.creadaEn(),r.actor(),List.of(t),r.servicios(),r.estado(),r.programadaPara(),r.activadaEn());
  return new CatalogoService.Estado(c.formatos(),c.papeles(),c.servicios(),nueva,c.historial(),c.programada(),c.papelesHabilitados(),c.papelesPredefinidos());
 }
 @Test void precioFinalIndependienteParesImparesYCopias(){
  var c=porHoja("30",ModoDobleFaz.FIJO,"40",List.of());
  assertThat(precios.calcular(c,item(2,1,true,List.of())).subtotal()).isEqualTo("40.00");
  var impar=precios.calcular(c,item(3,2,true,List.of()));
  assertThat(impar.subtotal()).isEqualTo("140.00");assertThat(impar.hojas()).isEqualTo(4);
  assertThat(impar.lineas()).extracting(EvaluadorPrecioItem.Linea::unidades).containsExactly(2L);
  assertThat(impar.lineas()).extracting(EvaluadorPrecioItem.Linea::base).containsOnly("POR_COPIA");
  assertThat(impar.lineas().get(0).detalle()).contains("1 hojas doble faz a ARS 40,00 + 1 hojas simple faz a ARS 30,00");
  assertThat(precios.calcular(c,item(1,3,true,List.of())).subtotal()).isEqualTo("90.00");
  assertThat(precios.calcular(c,item(3,2,false,List.of())).subtotal()).isEqualTo("180.00");
  assertThat(precios.calcular(porHoja("100",ModoDobleFaz.FIJO,"40",List.of()),item(2,1,true,List.of())).subtotal()).isEqualTo("40.00");
 }
 @Test void relativosRedondeanPorHojaYSimpleFazNoCambia(){
  assertThat(precios.calcular(porHoja("30",ModoDobleFaz.ADICIONAL,"10",List.of()),item(3,2,true,List.of())).subtotal()).isEqualTo("140.00");
  assertThat(precios.calcular(porHoja("30",ModoDobleFaz.PORCENTAJE,"10",List.of()),item(2,1,true,List.of())).subtotal()).isEqualTo("33.00");
  assertThat(precios.calcular(porHoja("0.05",ModoDobleFaz.PORCENTAJE,"10",List.of()),item(3,2,true,List.of())).subtotal()).isEqualTo("0.22");
  assertThat(precios.calcular(porHoja("0",ModoDobleFaz.FIJO,"0",List.of()),item(3,2,true,List.of())).subtotal()).isEqualTo("0.00");
 }
 @Test void terminacionesConservanCantidadesConTarifaPorHoja(){
  var extras=new ArrayList<OfertaServicio>();for(var base:BasePrecio.values())extras.add(new OfertaServicio(UUID.randomUUID(),base.name(),base,BigDecimal.ONE,10,true,List.of(new Compatibilidad(f,p))));
  var r=precios.calcular(porHoja("30",ModoDobleFaz.FIJO,"40",extras),item(3,2,true,extras.stream().map(OfertaServicio::servicio).toList()));
  assertThat(r.subtotal()).isEqualTo("153.00");
  assertThat(r.lineas()).extracting(EvaluadorPrecioItem.Linea::unidades).containsExactly(2L,2L,4L,6L,1L);
 }

}
