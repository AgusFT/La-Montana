const {test}=require('node:test'),assert=require('node:assert/strict'),ts=require('typescript'),fs=require('node:fs');
require.extensions['.ts']=(module,filename)=>module._compile(ts.transpileModule(fs.readFileSync(filename,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,filename);
const {operationalStatus,productionSituation,recentMovementLabel}=require('../src/lib/order-operational-status.ts');
const production=(...jobs)=>({items:jobs.map(trabajos=>({trabajos}))});
test('una reimpresión pendiente tiene prioridad aunque otro archivo siga imprimiéndose',()=>{
 const p=production([{estado:'COMPLETADO',calidad:{resultado:'REIMPRESION_REQUERIDA',fecha:'2026-10-30T12:00:00Z'}}],[{estado:'IMPRIMIENDO',inicio:'2026-10-30T13:00:00Z'}]);
 const state=productionSituation(p);assert.equal(state.situacionOperativa,'REIMPRESION_REQUERIDA');assert.equal(state.situacionDesde,'2026-10-30T12:00:00Z');assert.equal(operationalStatus({...state,estado:'EN_PRODUCCION'}).tone,'danger');
});
test('el nuevo intento reemplaza al fallido sin ocultar la necesidad de inspección',()=>{
 const p=production([{estado:'COMPLETADO',calidad:{resultado:'REIMPRESION_REQUERIDA',fecha:'2026-10-30T12:00:00Z'}},{estado:'IMPRIMIENDO',inicio:'2026-10-30T14:00:00Z',calidad:null}]);
 const state=productionSituation(p);assert.equal(state.situacionOperativa,'CALIDAD_PENDIENTE');assert.equal(state.situacionDesde,'2026-10-30T14:00:00Z');assert.equal(operationalStatus({...state,estado:'EN_PRODUCCION'}).tone,'warning');
});
test('el último control aprobado no convierte una reimpresión actual en un pedido listo',()=>{
 const state=productionSituation(production([{estado:'COMPLETADO',calidad:{resultado:'APROBADO'}},{estado:'ERROR',fin:'2026-10-30T15:00:00Z'}]));
 assert.equal(state.situacionOperativa,'INCIDENCIA_PRODUCCION');assert.equal(recentMovementLabel('CONTROLAR_CALIDAD_REIMPRESION_REQUERIDA'),'Calidad: requiere reimpresión');
});
