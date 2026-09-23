//#region ENCABEZADO · scripts/prepare-pdf.mjs
/*
 * ========================================================================
 * ARCHIVO: scripts/prepare-pdf.mjs
 * ========================================================================
 * FUNCIÓN
 * Copia el worker de PDF.js y su licencia desde la dependencia instalada a una ruta pública
 * versionada, para que la lectura local de PDF funcione sin recurrir a un CDN.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - No declara funciones o métodos propios.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - require [const].
 * - root [const].
 * - {version} [const].
 * - target [const].
 * ========================================================================
 */
//#endregion

import {mkdir,copyFile} from 'node:fs/promises';
import {createRequire} from 'node:module';
import {dirname,resolve} from 'node:path';
const require=createRequire(import.meta.url);
const root=dirname(require.resolve('pdfjs-dist/package.json'));
const {version}=require('pdfjs-dist/package.json');
const target=resolve(import.meta.dirname,'../public/pdfjs',version);
await mkdir(target,{recursive:true});
await copyFile(resolve(root,'build/pdf.worker.min.mjs'),resolve(target,'pdf.worker.min.mjs'));
await copyFile(resolve(root,'LICENSE'),resolve(target,'LICENSE'));
