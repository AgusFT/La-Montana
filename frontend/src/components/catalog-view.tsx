//#region ENCABEZADO · src/components/catalog-view.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/catalog-view.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta configuraciones comerciales en modo de consulta, con tarifas agrupadas, sus reglas y
 * precios finales o históricos, variantes, servicios y vigencia.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * - [export] RevisionView({ revision, catalog }: { revision:CatalogRevision; catalog:CatalogState
 *   })
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos propios.
 * ========================================================================
 */
//#endregion

import { catalogDate, colorLabels, priceLabels, type CatalogState, type CatalogRevision } from "@/lib/catalog-types";
import {ars} from "@/lib/catalog-pricing";
import {seedRates,duplexPrice} from "@/lib/catalog-rate-draft";
import {presetPaperGroups,registeredPaperGroups} from "@/lib/catalog-paper-groups";
import {paperLabel} from "@/components/catalog-paper-select";
export function RevisionView({ revision, catalog }: { revision:CatalogRevision; catalog:CatalogState }) {
  return <div className="catalog-revision-view"><p><strong>Configuración {revision.numero}</strong> · {catalogDate(revision.creadaEn)} · {revision.actor}</p><p>{revision.motivo}</p><p>Estado: {revision.estado.toLowerCase()}{revision.programadaPara&&<> · Programada para {catalogDate(revision.programadaPara)}</>}{revision.activadaEn&&<> · Activada el {catalogDate(revision.activadaEn)}</>}</p><p>Moneda: ARS · pesos argentinos. Importes fijos.</p>
    <h3>Tarifas de impresión</h3><div className="pricing-read-list">{seedRates(revision).map(rate=>{
      const simple=Number(rate.precio.replace(",",".")),legacy=rate.recargoAnterior!==undefined;
      const final=legacy?simple+(rate.recargoAnterior??0):duplexPrice(rate.precio,rate.modoDobleFaz,rate.valorDobleFaz);
      const groups=registeredPaperGroups({...catalog,papelesHabilitados:catalog.papelesHabilitados.filter(p=>rate.papeles.some(r=>r.formato===p.formato&&r.papel===p.papel))},presetPaperGroups(catalog.papelesPredefinidos));
      return <article className="pricing-read-card" key={rate.id}><strong>{rate.nombre}</strong><dl><div><dt>Color</dt><dd>{rate.color?colorLabels[rate.color]:"—"}</dd></div><div><dt>Simple faz · {legacy?"por cara":"por hoja"}</dt><dd>{ars(simple)}</dd></div><div><dt>Doble faz · {legacy?"por cara (tarifa anterior)":"por hoja con dos caras"}</dt><dd>{final===null?"—":ars(final)}</dd></div><div><dt>Estado</dt><dd>{rate.habilitada?"Habilitada":"Deshabilitada"}</dd></div></dl>
      {!legacy&&<p>{rate.modoDobleFaz==="FIJO"?"Precio doble faz independiente de simple faz.":rate.modoDobleFaz==="ADICIONAL"?`Doble faz = simple faz + ${ars(Number(rate.valorDobleFaz.replace(",",".")))}.`:`Doble faz = simple faz + ${rate.valorDobleFaz} %.`} La última hoja impar se cobra a simple faz.</p>}
      <details><summary>{rate.papeles.length} variantes incluidas · ver hojas</summary>{groups.map(group=><div key={group.key}><strong>{group.nombre} · {group.anchoMm} × {group.altoMm} mm</strong><ul>{group.variantes.map(p=><li key={`${p.formato}/${p.papel}`}>{paperLabel(catalog,p)}</li>)}</ul></div>)}</details></article>;
    })}</div>
    <h3>Servicios</h3><div className="pricing-read-list">{revision.servicios.map(offer=>{const printing=catalog.servicios.find(s=>s.codigoPublico===offer.servicio)?.tipo==="IMPRESION";return <article className="pricing-read-card" key={offer.servicio}><strong>{offer.nombreVisible}</strong><dl><div><dt>Forma de cobro</dt><dd>{priceLabels[offer.basePrecio]}</dd></div><div><dt>Precio</dt><dd>{printing?"Según tarifas de impresión":ars(offer.precio)}</dd></div><div><dt>Preparación</dt><dd>{offer.preparacionMinutos} min</dd></div><div><dt>Estado</dt><dd>{offer.habilitado?"Habilitado":"Deshabilitado"}</dd></div></dl>{offer.compatibilidades.length>0&&<div><strong>Papeles compatibles</strong><ul>{offer.compatibilidades.map((pair,i)=><li key={i}>{paperLabel(catalog,pair)}</li>)}</ul></div>}</article>;})}</div>
  </div>;
}
