const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),ts=require('typescript');
require.extensions['.ts']=(m,p)=>m._compile(ts.transpileModule(fs.readFileSync(p,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,p);
const {operationTask,operationStage}=require('../src/lib/operation-task.ts');
const order=(estado='PENDIENTE_REVISION',changes={})=>({version:4,estado,cobro:{estado:'SIN_ANTICIPO'},...changes});
const facts=(changes={})=>({review:null,production:null,delivery:null,...changes});
const review=acciones=>({version:4,acciones});
const production=changes=>({version:4,puedeProducir:true,puedeControlarCalidad:true,cobertura:[],bloqueos:[],items:[],...changes});
const delivery=changes=>({version:4,puedeGestionar:true,puedeCerrar:false,saldo:'0.00',...changes});
const action=(accion,habilitada=true,motivo=null)=>({accion,habilitada,motivo});
test('un empleado de producción espera la revisión sin recibir un botón de aprobación',()=>{
 const task=operationTask(order(),['GESTIONAR_PRODUCCION'],facts({review:review([action('REVISAR')])}));
 assert.equal(task.panel,'pedido-resumen');assert.equal(task.waiting,true);assert.match(task.title,/Esperando revisión/);
});
test('el responsable ve el bloqueo económico antes de poder aprobar, aunque pueda registrar revisión',()=>{
 const task=operationTask(order(undefined,{cobro:{estado:'PENDIENTE'}}),['GESTIONAR_PEDIDOS','ACREDITAR_PAGO'],facts({review:review([action('REVISAR'),action('APROBAR',false)])}));
 assert.equal(task.panel,'pedido-revision');assert.equal(task.title,'Falta resolver el pago requerido');assert.equal(task.waiting,true);
});
test('aprobar se propone solamente con la autorización actual del servidor',()=>{
 const permissions=['GESTIONAR_PEDIDOS'];
 assert.equal(operationTask(order(),permissions,facts({review:review([action('REVISAR'),action('APROBAR')])})).title,'Revisar y aprobar el pedido');
 const blocked=operationTask(order(),permissions,facts({review:review([action('APROBAR',false,'Falta revisión humana.')])}));
 assert.equal(blocked.waiting,true);assert.equal(blocked.description,'Falta revisión humana.');
});
test('caja ve el anticipo y otro perfil ve quién debe resolverlo',()=>{
 const pending=order(undefined,{cobro:{estado:'PENDIENTE'}});
 assert.equal(operationTask(pending,['REGISTRAR_COBRO'],facts()).panel,'pedido-pagos');
 assert.equal(operationTask(pending,['GESTIONAR_PRODUCCION'],facts()).waiting,true);
});
test('una versión vieja no habilita revisar, producir ni cerrar',()=>{
 const stale=3;
 assert.notEqual(operationTask(order(),['GESTIONAR_PEDIDOS'],facts({review:{...review([action('APROBAR')]),version:stale}})).title,'Revisar y aprobar el pedido');
 const job=production({version:stale,items:[{puedeIniciar:true,trabajos:[]}]});
 assert.notEqual(operationTask(order('APROBADO'),['GESTIONAR_PRODUCCION'],facts({production:job})).title,'Preparar e iniciar la impresión');
 assert.equal(operationTask(order('ENTREGADO'),['CERRAR_PEDIDOS'],facts({delivery:delivery({version:stale,puedeCerrar:true})})).waiting,true);
});
test('producción sólo propone iniciar un ítem realmente habilitado',()=>{
 const permissions=['GESTIONAR_PRODUCCION'];
 const task=operationTask(order('APROBADO'),permissions,facts({production:production({items:[{puedeIniciar:true,trabajos:[]}]})}));
 assert.equal(task.title,'Preparar e iniciar la impresión');assert.equal(task.waiting,false);
 const blocked=operationTask(order('APROBADO'),permissions,facts({production:production({bloqueos:['Falta anticipo.'],items:[{puedeIniciar:false,trabajos:[]}]})}));
 assert.equal(blocked.waiting,true);assert.equal(blocked.description,'Falta anticipo.');
});
test('control de calidad tiene prioridad sólo para el inspector autorizado',()=>{
 const data=production({items:[{puedeIniciar:false,trabajos:[{estado:'COMPLETADO',calidad:null}]}]});
 assert.equal(operationTask(order('EN_PRODUCCION'),['CONTROLAR_CALIDAD'],facts({production:data})).title,'Controlar la calidad del trabajo');
 assert.notEqual(operationTask(order('EN_PRODUCCION'),['GESTIONAR_PRODUCCION'],facts({production:data})).title,'Controlar la calidad del trabajo');
 assert.notEqual(operationTask(order('EN_PRODUCCION'),['CONTROLAR_CALIDAD'],facts({production:{...data,puedeControlarCalidad:false}})).title,'Controlar la calidad del trabajo');
});
test('un trabajo activo propone registrar su finalización y una incidencia habilita reimpresión',()=>{
 const task=jobs=>operationTask(order('EN_PRODUCCION'),['GESTIONAR_PRODUCCION'],facts({production:production({items:[{puedeIniciar:jobs[0].estado!=='IMPRIMIENDO',trabajos:jobs}]})}));
 assert.equal(task([{estado:'IMPRIMIENDO'}]).title,'Impresión en curso');
 assert.equal(task([{estado:'COMPLETADO',calidad:{resultado:'REIMPRESION_REQUERIDA'}}]).title,'Preparar e iniciar la impresión');
});
test('el recorrido avanza a impresión y calidad al iniciar y conserva las etapas anteriores',()=>{
 assert.equal(operationStage(order(),null),0);
 assert.equal(operationStage(order('APROBADO'),null),1);
 assert.equal(operationStage(order('EN_PRODUCCION'),null),2);
 const jobs=trabajos=>production({items:[{trabajos}]});
 assert.equal(operationStage(order('EN_PRODUCCION'),jobs([{estado:'IMPRIMIENDO'}])),2);
 assert.equal(operationStage(order('EN_PRODUCCION'),jobs([{estado:'COMPLETADO',calidad:null}])),2);
 assert.equal(operationStage(order('LISTO_PARA_ENTREGA'),null),3);
 assert.equal(operationStage(order('ENTREGADO'),null),4);
 assert.equal(operationStage(order('CERRADO'),null),4);
});
test('varios archivos no ocultan la impresión en curso; una reimpresión vuelve a preparación',()=>{
 const data=production({items:[{trabajos:[{estado:'IMPRIMIENDO'}]},{trabajos:[]}]});
 assert.equal(operationStage(order('EN_PRODUCCION'),data),2);
 assert.equal(operationStage(order('EN_PRODUCCION'),production({items:[{trabajos:[{estado:'COMPLETADO',calidad:{resultado:'REIMPRESION_REQUERIDA'}}]}]})),1);
});
test('el operador con ambos permisos recibe una sola tarea para terminar e inspeccionar',()=>{
 const data=production({items:[{puedeIniciar:false,trabajos:[{estado:'IMPRIMIENDO'}]}]});
 const task=operationTask(order('EN_PRODUCCION'),['GESTIONAR_PRODUCCION','CONTROLAR_CALIDAD'],facts({production:data}));
 assert.equal(task.title,'Impresión y control de calidad');assert.match(task.description,/una sola ventana/);
 assert.notEqual(operationTask(order('EN_PRODUCCION'),['GESTIONAR_PRODUCCION'],facts({production:data})).title,task.title);
});
test('la cobertura pendiente orienta a caja y no habilita una impresión',()=>{
 const task=operationTask(order('APROBADO'),['ACREDITAR_PAGO'],facts({production:production({cobertura:[{pendiente:'300.00'}]})}));
 assert.equal(task.panel,'pedido-pagos');
});
test('entrega distingue operador, caja y empleado sin permisos',()=>{
 const data=facts({delivery:delivery({saldo:'500.00'})});
 assert.equal(operationTask(order('LISTO_PARA_ENTREGA'),['GESTIONAR_ENTREGAS'],data).panel,'pedido-entrega');
 assert.equal(operationTask(order('LISTO_PARA_ENTREGA'),['REGISTRAR_COBRO'],data).panel,'pedido-pagos');
 assert.equal(operationTask(order('LISTO_PARA_ENTREGA'),['CONTROLAR_CALIDAD'],data).waiting,true);
});
test('cierre conserva la espera mientras el servidor no lo habilita',()=>{
 assert.equal(operationTask(order('ENTREGADO'),['CERRAR_PEDIDOS'],facts({delivery:delivery({puedeCerrar:false})})).waiting,true);
 assert.equal(operationTask(order('ENTREGADO'),['CERRAR_PEDIDOS'],facts({delivery:delivery({puedeCerrar:true})})).waiting,false);
});
test('corrección espera al cliente y los estados finales no proponen operaciones nuevas',()=>{
 assert.equal(operationTask(order('CORRECCION_SOLICITADA'),['GESTIONAR_PEDIDOS'],facts()).panel,'pedido-correcciones');
 for(const state of ['CERRADO','CANCELADO','RECHAZADO','DESCONOCIDO']){const task=operationTask(order(state),['GESTIONAR_PEDIDOS','GESTIONAR_PRODUCCION'],facts());assert.equal(task.panel,'pedido-resumen');assert.equal(task.waiting,true);}
});
