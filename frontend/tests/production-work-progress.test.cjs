const {test}=require('node:test'),assert=require('node:assert/strict'),ts=require('typescript'),fs=require('node:fs');
require.extensions['.ts']=(m,p)=>m._compile(ts.transpileModule(fs.readFileSync(p,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,p);
const {productionWorkProgress}=require('../src/lib/production-work-progress.ts');
const order={version:4,oferta:{items:[{codigoPublico:'one',documento:{nombre:'Material.pdf'}},{codigoPublico:'two',documento:{nombre:'Carpeta.pdf'}}]},archivos:[{item:'one',archivo:'pdf-one'},{item:'two',archivo:'pdf-two'}]};
const approved=file=>({estado:'COMPLETADO',archivo:file,calidad:{resultado:'APROBADO',fecha:'2026-11-02T15:00:00Z'}});
const snapshot=(first,second,version=4)=>({version,items:[{codigoPublico:'one',trabajos:first},{codigoPublico:'two',trabajos:second}]});
test('dos PDF mantienen contadores separados; aprobar uno no completa el pedido',()=>{
 const progress=productionWorkProgress(order,snapshot([approved('pdf-one')],[]));assert.equal(progress.approved,1);assert.equal(progress.pending,1);assert.equal(progress.complete,false);assert.equal(progress.rows[1].status.code,'prepare');
});
test('una reimpresión posterior invalida visualmente la aprobación del intento antiguo',()=>{
 const progress=productionWorkProgress(order,snapshot([approved('pdf-one'),{estado:'IMPRIMIENDO',archivo:'pdf-one',inicio:'2026-11-02T16:00:00Z',calidad:null}],[approved('pdf-two')]));
 assert.equal(progress.approved,1);assert.equal(progress.complete,false);assert.equal(progress.rows[0].status.code,'printing');
});
test('calidad rechazada y archivos reemplazados siguen pendientes',()=>{
 const failed={...approved('pdf-one'),calidad:{resultado:'REIMPRESION_REQUERIDA'}};const progress=productionWorkProgress(order,snapshot([failed],[approved('old-pdf-two')]));
 assert.equal(progress.approved,0);assert.equal(progress.rows[0].status.code,'reprint');assert.equal(progress.rows[1].status.code,'prepare');assert.equal(progress.complete,false);
});
test('una versión vieja no muestra trabajos aprobados como si fueran actuales',()=>{
 const progress=productionWorkProgress(order,snapshot([approved('pdf-one')],[approved('pdf-two')],3));assert.equal(progress.known,false);assert.equal(progress.complete,false);assert.equal(progress.approved,0);assert.ok(progress.rows.every(r=>r.status.code==='loading'));
});
test('el avance se completa únicamente cuando ambos PDF vigentes aprobaron calidad',()=>{
 const progress=productionWorkProgress(order,snapshot([approved('pdf-one')],[approved('pdf-two')]));assert.equal(progress.complete,true);assert.equal(progress.approved,2);assert.equal(progress.pending,0);
});
