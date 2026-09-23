//#region ENCABEZADO · src/lib/catalog-types.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/catalog-types.ts
 * ========================================================================
 * FUNCIÓN
 * Define estructuras del catálogo, tarifas y configuraciones comerciales, con validadores de
 * respuestas en tiempo de ejecución y formatos de fecha e importe.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - record(value: unknown): value is Record<string, unknown>
 * - strings(value: Record<string, unknown>, keys: string[]): boolean
 * - number(value: unknown): value is number
 * - compatibility(value: unknown): value is Compatibility
 * - rate(value: unknown): value is PrintRate
 * - offer(value: unknown): value is ServiceOffer
 * - summary(value: unknown): value is RevisionSummary
 * - [export] isCatalogRevision(value: unknown): value is CatalogRevision
 *   Validador de datos recibidos en tiempo de ejecución.
 * - selection(value: unknown): value is PaperSelection
 * - preset(value: unknown): value is PaperPreset
 * - [export] isCatalogState(value: unknown): value is CatalogState
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] catalogDate(value: string): string
 * - [export] catalogMoney(value: number): string
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Format (tipo).
 * - Paper (tipo).
 * - CatalogService (tipo).
 * - ColorMode (tipo).
 * - PriceBase (tipo).
 * - Compatibility (tipo).
 * - PrintRate (tipo).
 * - ServiceOffer (tipo).
 * - CommercialStatus (tipo).
 * - RevisionSummary (tipo).
 * - CatalogRevision (tipo).
 * - PaperSelection (tipo).
 * - PaperPreset (tipo).
 * - CatalogState (tipo).
 * - NewRevision (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - colorLabels [const, exportado].
 * - priceLabels [const, exportado].
 * ========================================================================
 */
//#endregion

export type Format = { codigoPublico: string; codigo: string; nombre: string; anchoMm: number; altoMm: number };
export type Paper = { codigoPublico: string; codigo: string; nombre: string; gramaje: number; terminacion: string };
export type CatalogService = { codigoPublico: string; codigo: string; nombre: string; tipo: "IMPRESION" | "TERMINACION"; descripcion: string | null };
export const colorLabels = { BLANCO_NEGRO: "Blanco y negro", COLOR: "Color" } as const;
export const priceLabels = { POR_COPIA: "Por copia", POR_HOJA: "Por hoja", POR_CARILLA: "Por carilla", FIJO_POR_ITEM: "Fijo por ítem" } as const;
export type ColorMode = keyof typeof colorLabels;
export type PriceBase = keyof typeof priceLabels;
export type Compatibility = { formato: string; papel: string };
export type PrintRate = Compatibility & { color: ColorMode; precio: number; recargoDobleFaz: number; habilitada: boolean };
export type ServiceOffer = { servicio: string; nombreVisible: string; basePrecio: PriceBase; precio: number; preparacionMinutos: number; habilitado: boolean; compatibilidades: Compatibility[] };
export type CommercialStatus = "VIGENTE" | "HISTORICA" | "PROGRAMADA" | "CANCELADA";
export type RevisionSummary = { codigoPublico: string; numero: number; motivo: string; creadaEn: string; actor: string; estado: CommercialStatus; programadaPara: string | null; activadaEn: string | null };
export type CatalogRevision = RevisionSummary & { tarifas: PrintRate[]; servicios: ServiceOffer[] };
export type PaperSelection = Compatibility & {habilitado:boolean;predefinido:string|null};
export type PaperPreset = {codigo:string;nombre:string;anchoMm:number;altoMm:number;gramaje:number;terminacion:string};
export type CatalogState = {
  papelesHabilitados: PaperSelection[]; papelesPredefinidos: PaperPreset[]; formatos: Format[]; papeles: Paper[]; servicios: CatalogService[]; actual: CatalogRevision | null; programada: CatalogRevision | null; historial: RevisionSummary[] };
export type NewRevision = { versionBase: string | null; operacion: string; motivo: string; programadaPara: string | null; tarifas: PrintRate[]; servicios: ServiceOffer[] };
function record(value: unknown): value is Record<string, unknown> { return !!value && typeof value === "object"; }
function strings(value: Record<string, unknown>, keys: string[]): boolean { return keys.every(key => typeof value[key] === "string"); }
function number(value: unknown): value is number { return typeof value === "number" && Number.isFinite(value); }
function compatibility(value: unknown): value is Compatibility { return record(value) && strings(value, ["formato", "papel"]); }
function rate(value: unknown): value is PrintRate { return record(value) && strings(value,["formato","papel"]) && typeof value.color === "string" && Object.hasOwn(colorLabels,value.color) && number(value.precio) && number(value.recargoDobleFaz) && typeof value.habilitada === "boolean"; }
function offer(value: unknown): value is ServiceOffer { return record(value) && strings(value,["servicio","nombreVisible"]) && typeof value.basePrecio === "string" && Object.hasOwn(priceLabels,value.basePrecio) && number(value.precio) && number(value.preparacionMinutos) && typeof value.habilitado === "boolean" && Array.isArray(value.compatibilidades) && value.compatibilidades.every(compatibility); }
function summary(value: unknown): value is RevisionSummary { return record(value) && strings(value,["codigoPublico","motivo","creadaEn","actor"]) && number(value.numero) && ["VIGENTE","HISTORICA","PROGRAMADA","CANCELADA"].includes(String(value.estado)) && (value.programadaPara===null||typeof value.programadaPara==="string") && (value.activadaEn===null||typeof value.activadaEn==="string"); }
export function isCatalogRevision(value: unknown): value is CatalogRevision { return record(value) && Array.isArray(value.tarifas) && value.tarifas.every(rate) && Array.isArray(value.servicios) && value.servicios.every(offer) && summary(value); }
function selection(value:unknown):value is PaperSelection {return record(value)&&strings(value,["formato","papel"])&&typeof value.habilitado==="boolean"&&(value.predefinido===null||typeof value.predefinido==="string");}
function preset(value:unknown):value is PaperPreset {return record(value)&&strings(value,["codigo","nombre","terminacion"])&&number(value.anchoMm)&&number(value.altoMm)&&number(value.gramaje);}
export function isCatalogState(value: unknown): value is CatalogState {
  return record(value) && Array.isArray(value.papelesHabilitados) && value.papelesHabilitados.every(selection) && Array.isArray(value.papelesPredefinidos) && value.papelesPredefinidos.every(preset) && Array.isArray(value.formatos) && value.formatos.every(f=>record(f)&&strings(f,["codigoPublico","codigo","nombre"])&&number(f.anchoMm)&&number(f.altoMm)) &&
    Array.isArray(value.papeles) && value.papeles.every(p=>record(p)&&strings(p,["codigoPublico","codigo","nombre","terminacion"])&&number(p.gramaje)) &&
    Array.isArray(value.servicios) && value.servicios.every(s=>record(s)&&strings(s,["codigoPublico","codigo","nombre"])&&(s.tipo==="IMPRESION"||s.tipo==="TERMINACION")&&(s.descripcion===null||typeof s.descripcion==="string")) &&
    (value.actual===null||isCatalogRevision(value.actual)) && (value.programada===null||isCatalogRevision(value.programada)) && Array.isArray(value.historial) && value.historial.every(summary);
}
export function catalogDate(value: string): string { const date=new Date(value); return Number.isNaN(date.valueOf()) ? value : new Intl.DateTimeFormat("es-AR",{dateStyle:"short",timeStyle:"short",timeZone:"UTC"}).format(date)+" UTC"; }
export function catalogMoney(value: number): string { return new Intl.NumberFormat("es-AR",{minimumFractionDigits:2,maximumFractionDigits:2}).format(value); }
