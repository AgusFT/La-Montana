//#region ENCABEZADO · tests/order-tracking.test.cjs
/* FUNCIÓN: Regresiones del recorrido público según estados reales, modalidad y condiciones guardadas.
 * FUNCIONES: cargador TypeScript aislado, pedido, actual. Ejecutar con pnpm test:tracking.
 */
//#endregion
const {test}=require('node:test');
const assert=require('node:assert/strict');
const ts=require('typescript');
const fs=require('node:fs');
require.extensions['.ts']=(module,filename)=>module._compile(ts.transpileModule(fs.readFileSync(filename,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2017}}).outputText,filename);
const {orderTracking,orderTrackingLabel,orderTrackingSummary}=require('../src/lib/order-tracking.ts');
function pedido(changes={}){return {estado:'PENDIENTE_REVISION',estadoLogistico:'NO_DISPONIBLE',confirmadaEn:'2026-10-20T12:00:00Z',aprobadaEn:null,finalizadaEn:null,condiciones:{revisionHumana:true},reserva:{modalidad:'RETIRO_SUCURSAL'},historial:[{estado:'PENDIENTE_REVISION',fecha:'2026-10-20T12:00:00Z'}],...changes};}
function actual(value){return orderTracking(value).steps.filter(s=>s.state==='current');}
test('manual: revisión actual y producción pendiente; sin viaje en retiro local',()=>{
 const r=orderTracking(pedido());assert.equal(r.unavailable,false);assert.equal(actual(pedido())[0].id,'review');
 assert.equal(r.steps.find(s=>s.id==='production').state,'pending');assert(!r.steps.some(s=>['travel','point','prepared'].includes(s.id)));
});
test('aprobación condicional directa no finge una revisión humana',()=>{
 const p=pedido({estado:'APROBADO',condiciones:{revisionHumana:false},historial:[{estado:'APROBADO',fecha:'2026-10-20T12:00:00Z'}],aprobadaEn:'2026-10-20T12:00:00Z'});
 assert.equal(actual(p)[0].id,'approved');assert(!orderTracking(p).steps.some(s=>s.id==='review'));
});
test('una revisión registrada se conserva aunque cambien condiciones posteriores',()=>{
 const r=orderTracking(pedido({estado:'APROBADO',condiciones:{revisionHumana:false}}));assert(r.steps.some(s=>s.id==='review'&&s.state==='complete'));
});
test('producción y calidad se agrupan sin inventar una aprobación de calidad',()=>{
 const r=orderTracking(pedido({estado:'EN_PRODUCCION'}));assert.equal(r.steps.find(s=>s.state==='current').id,'production');assert.equal(r.steps.find(s=>s.id==='ready').state,'pending');
});
test('retiro local listo no muestra logística de reparto',()=>{
 const p=pedido({estado:'LISTO_PARA_ENTREGA',estadoLogistico:'LISTO_RETIRO'});assert.equal(actual(p)[0].id,'ready');assert.match(orderTrackingLabel(p),/Disponible para retirar/);assert(!orderTracking(p).steps.some(s=>s.id==='travel'));
});
test('el resumen público agrupa el recorrido real en cinco etapas y conserva el detalle',()=>{
 const local=pedido({estado:'LISTO_PARA_ENTREGA',estadoLogistico:'LISTO_RETIRO'}),tracking=orderTracking(local),summary=orderTrackingSummary(local,tracking.steps);
 assert.equal(summary.length,5);assert.equal(summary.find(s=>s.state==='current').id,'delivery');assert.equal(summary[3].label,'Retiro en sucursal');
 assert.ok(tracking.steps.length>summary.length);assert.ok(tracking.steps.some(s=>s.id==='review'));
});
for(const [logistic,id] of [['PENDIENTE_PREPARACION','preparing'],['PREPARADO','prepared'],['EN_VIAJE','travel'],['DISPONIBLE_PUNTO','point']])test(`punto de entrega: ${logistic} usa el estado logístico real`,()=>{
 const p=pedido({estado:'LISTO_PARA_ENTREGA',estadoLogistico:logistic,reserva:{modalidad:'RETIRO_PUNTO_ENTREGA'}});assert.equal(actual(p)[0].id,id);assert.equal(actual(p)[0].date,null);assert.equal(orderTracking(p).steps.find(s=>s.id==='delivered').state,'pending');
});
test('domicilio incluye viaje y excluye disponibilidad en punto',()=>{
 const p=pedido({estado:'LISTO_PARA_ENTREGA',estadoLogistico:'EN_VIAJE',reserva:{modalidad:'ENVIO_DOMICILIO'}});assert.equal(actual(p)[0].id,'travel');assert(!orderTracking(p).steps.some(s=>s.id==='point'));assert.equal(orderTrackingLabel(p),'En viaje');
});
test('corrección después de aprobación vuelve a pedir intervención sin mostrar aprobación completada',()=>{
 const p=pedido({estado:'CORRECCION_SOLICITADA',aprobadaEn:'2026-10-20T12:01:00Z',historial:[{estado:'APROBADO',fecha:'2026-10-20T12:01:00Z'},{estado:'CORRECCION_SOLICITADA',fecha:'2026-10-20T12:02:00Z'}]});const r=orderTracking(p);assert.equal(actual(p)[0].label,'Corrección solicitada');assert.equal(r.steps.find(s=>s.id==='approved').state,'pending');assert.equal(r.steps.find(s=>s.id==='approved').date,null);
});
for(const state of ['CANCELADO','RECHAZADO'])test(`${state}: conserva hechos y detiene las etapas futuras`,()=>{
 const r=orderTracking(pedido({estado:state,finalizadaEn:'2026-10-20T12:03:00Z',historial:[{estado:'PENDIENTE_REVISION',fecha:'2026-10-20T12:00:00Z'},{estado:state,fecha:'2026-10-20T12:03:00Z'}]}));assert.equal(r.steps.at(-1).state,'stopped');assert(!r.steps.some(s=>['pending','complete'].includes(s.state)));assert(!r.steps.some(s=>['production','closed','delivered'].includes(s.id)));
});
test('entregado y cerrado son etapas distintas',()=>{
 assert.equal(actual(pedido({estado:'ENTREGADO',estadoLogistico:'ENTREGADO'}))[0].id,'delivered');const r=orderTracking(pedido({estado:'CERRADO',estadoLogistico:'ENTREGADO',finalizadaEn:'2026-10-20T14:00:00Z'}));assert.equal(r.steps.find(s=>s.id==='delivered').state,'complete');assert.equal(r.steps.at(-1).state,'current');
});
for(const changes of [{estado:'NUEVO_ESTADO'},{estado:'LISTO_PARA_ENTREGA',estadoLogistico:'NO_DISPONIBLE'},{estado:'LISTO_PARA_ENTREGA',estadoLogistico:'DISPONIBLE_PUNTO',reserva:{modalidad:'ENVIO_DOMICILIO'}}])test('un estado desconocido o incompatible no produce avance ficticio',()=>{const r=orderTracking(pedido(changes));assert(r.unavailable);assert.equal(r.steps.length,0);});
test('las etiquetas reconocen la logística real de otra sucursal sin inferirla del horario',()=>{
 assert.equal(orderTrackingLabel(pedido({estado:'LISTO_PARA_ENTREGA',estadoLogistico:'EN_VIAJE'})),'En viaje');
 assert.equal(orderTrackingLabel(pedido({estado:'LISTO_PARA_ENTREGA',estadoLogistico:'DISPONIBLE_PUNTO',reserva:{modalidad:'ENVIO_DOMICILIO'}})),'Listo para despachar');
});

test('manda el recorrido confirmado, no la matriz de la cotización anterior',()=>{
 const external=pedido({estado:'LISTO_PARA_ENTREGA',estadoLogistico:'EN_VIAJE',retiroConTraslado:true,oferta:{entrega:{llegadaEstimada:null}}});
 assert.equal(actual(external)[0].id,'travel');
 const local=pedido({estado:'LISTO_PARA_ENTREGA',estadoLogistico:'LISTO_RETIRO',retiroConTraslado:false,oferta:{entrega:{llegadaEstimada:'2099-01-01T12:00:00Z'}}});
 assert.equal(actual(local)[0].id,'ready');assert(!orderTracking(local).steps.some(s=>s.id==='travel'));
});

test('la llegada real anticipada tiene fecha propia, sin modificar la de calidad',()=>{
 const p=pedido({estado:'LISTO_PARA_ENTREGA',estadoLogistico:'DISPONIBLE_PUNTO',oferta:{entrega:{llegadaEstimada:'2099-01-01T12:00:00Z'}},historial:[
  {estado:'LISTO_PARA_ENTREGA',fecha:'2026-10-27T10:00:00Z',accion:'CONTROLAR_CALIDAD'},
  {estado:'LISTO_PARA_ENTREGA',fecha:'2026-10-27T10:05:00Z',accion:'PREPARAR_ENVIO'},
  {estado:'LISTO_PARA_ENTREGA',fecha:'2026-10-27T10:10:00Z',accion:'SALIR_REPARTO'},
  {estado:'LISTO_PARA_ENTREGA',fecha:'2026-10-27T10:30:00Z',accion:'LLEGAR_PUNTO'}
 ]});
 const result=orderTracking(p);
 assert.equal(result.steps.find(s=>s.id==='preparing').date,'2026-10-27T10:00:00Z');
 assert.equal(actual(p)[0].date,'2026-10-27T10:30:00Z');
 assert.equal(result.steps.find(s=>s.id==='delivered').state,'pending');
});

for(const [state,id] of [['PENDIENTE_PREPARACION','preparing'],['PREPARADO','prepared'],['EN_VIAJE','travel'],['DISPONIBLE_PUNTO','point']])test(`otra sucursal: ${state} requiere recorrido guardado`,()=>{
 const p=pedido({estado:'LISTO_PARA_ENTREGA',estadoLogistico:state,oferta:{entrega:{llegadaEstimada:'2099-01-01T12:00:00Z'}}});
 assert.equal(actual(p)[0].id,id);assert(!orderTracking(p).steps.some(s=>s.id==='ready'));
 assert.equal(orderTracking(p).steps.find(s=>s.id==='delivered').state,'pending');
 if(id==='point')assert.match(orderTracking(p).notice,/franja de retiro/);
});
test('una estimación vencida no avanza el timeline sin movimiento real',()=>{
 const p=pedido({estado:'LISTO_PARA_ENTREGA',estadoLogistico:'PENDIENTE_PREPARACION',oferta:{entrega:{llegadaEstimada:'2000-01-01T12:00:00Z'}}});
 assert.equal(actual(p)[0].id,'preparing');
});

test('esperar anticipo no inventa una revisión humana en modo condicional',()=>{
 const p=pedido({condiciones:{revisionHumana:false},cobro:{estado:'PENDIENTE_PAGO'}});
 const r=orderTracking(p);assert.equal(actual(p)[0].id,'payment');assert(!r.steps.some(s=>s.id==='review'));
 assert.match(orderTrackingLabel(p),/condición económica/);
});
test('informar un pago mantiene pendiente la aprobación y el inicio de producción',()=>{
 const p=pedido({condiciones:{revisionHumana:false},cobro:{estado:'PAGO_INFORMADO_FUERA_TERMINO'}});
 assert.equal(actual(p)[0].id,'payment');assert.match(orderTrackingLabel(p),/pendiente de verificación/);
 assert.equal(orderTracking(p).steps.find(s=>s.id==='approved').state,'pending');
});
test('manual sin anticipo espera revisión humana, no un cobro inexistente',()=>{
 const p=pedido({cobro:{estado:'SIN_ANTICIPO'}});assert.equal(actual(p)[0].id,'review');
 assert(!orderTrackingLabel(p).includes('condición económica'));
});
