//#region ENCABEZADO · src/lib/file-types.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/file-types.ts
 * ========================================================================
 * FUNCIÓN
 * Define y valida los datos y vistas de PDF privados que consume la interfaz de archivos.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] isFile(v: unknown): v is PrivateFile
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isFileView(v: unknown): v is FileView
 *   Validador de datos recibidos en tiempo de ejecución.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PrivateFile (tipo).
 * - FileView (tipo).
 * ========================================================================
 */
//#endregion

export type PrivateFile={codigoPublico:string;item:string;nombre:string;estado:string;activo:boolean;creadoEn:string;cargarHasta:string;bytes:number|null;sha256:string|null;paginas:number|null;codigoResultado:string|null;mensaje:string|null;aceptadaEn:string|null};
export type FileView={version:number;correccion:string|null;cotizacion:string;sucursal:string;carga:{habilitada:boolean;motivo:string};items:{codigoPublico:string;nombre:string;admiteCarga:boolean;archivos:PrivateFile[]}[]};
export function isFile(v:unknown):v is PrivateFile{return typeof v==="object"&&v!==null&&"codigoPublico" in v&&typeof v.codigoPublico==="string"&&"estado" in v&&typeof v.estado==="string"&&"item" in v&&typeof v.item==="string";}
export function isFileView(v:unknown):v is FileView{return typeof v==="object"&&v!==null&&"cotizacion" in v&&typeof v.cotizacion==="string"&&"items" in v&&Array.isArray(v.items)&&"carga" in v&&typeof v.carga==="object"&&v.carga!==null&&v.items.every(i=>i&&Array.isArray(i.archivos)&&i.archivos.every(isFile));}
