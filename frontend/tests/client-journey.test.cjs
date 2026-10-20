//#region ENCABEZADO · tests/client-journey.test.cjs
/* FUNCIÓN: Comprueba que los tres bloques respeten aceptación, acceso del backend, pagos y archivos. */
//#endregion
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),ts=require('typescript');
require.extensions['.ts']=(m,p)=>m._compile(ts.transpileModule(fs.readFileSync(p,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2017}}).outputText,p);
const {quoteStage,positiveAmount}=require('../src/lib/client-journey.ts');
const {rememberPreparedFiles,preparedFile,clearPreparedFiles}=require('../src/lib/prepared-files.ts');
const now=Date.parse('2026-10-20T12:00:00Z');
function quote(changes={}){return {codigoPublico:'q',estado:'VIGENTE',aceptadaEn:null,vigenteHasta:'2026-10-21T12:00:00Z',reemplazadaPor:null,correccion:null,oferta:{items:[{codigoPublico:'a',documento:{sha256:'hash-a'}}]},...changes};}
function files(changes={}){return {cotizacion:'q',carga:{habilitada:true,codigo:'HABILITADA'},items:[{codigoPublico:'a',archivos:[]}],...changes};}
const accepted=()=>quote({aceptadaEn:'2026-10-20T11:00:00Z'});
const ready={activo:true,estado:'VALIDO',aceptadaEn:'2026-10-20T11:30:00Z'};
test('el resumen precede a pagos, archivo y creación aunque el acceso diga otra cosa',()=>assert.equal(quoteStage(quote(),files({carga:{habilitada:false,codigo:'PAGO_PENDIENTE'}}),now),'summary'));
test('manual con acceso habilitado pasa al archivo sin mostrar un paso de pago',()=>assert.equal(quoteStage(accepted(),files(),now),'files'));
test('el requisito de anticipo viene de un código del servidor, no del texto ni de un importe supuesto',()=>{assert.equal(quoteStage(accepted(),files({carga:{habilitada:false,codigo:'PAGO_PENDIENTE',motivo:'Aviso'}}),now),'payment');assert.equal(quoteStage(accepted(),files({carga:{habilitada:false,motivo:'Falta pagar'}}),now),'blocked');});
test('una sucursal no disponible no se presenta como pago pendiente',()=>assert.equal(quoteStage(accepted(),files({carga:{habilitada:false,codigo:'BLOQUEADA'}}),now),'blocked'));
test('un archivo válido necesita aceptación explícita de su vista previa',()=>assert.equal(quoteStage(accepted(),files({items:[{codigoPublico:'a',archivos:[{...ready,aceptadaEn:null}]}]}),now),'files'));
test('sólo la versión activa válida y aceptada permite llegar a la confirmación',()=>{assert.equal(quoteStage(accepted(),files({items:[{codigoPublico:'a',archivos:[ready]}]}),now),'confirm');for(const change of [{activo:false},{estado:'VALIDANDO'},{estado:'REQUIERE_COTIZACION'},{estado:'RECHAZADO'}])assert.equal(quoteStage(accepted(),files({items:[{codigoPublico:'a',archivos:[{...ready,...change}]}]}),now),'files');});
test('todos los archivos del resumen requieren aceptación; no basta un subconjunto ni una respuesta de otra cotización',()=>{const q=accepted();q.oferta.items.push({codigoPublico:'b',documento:{sha256:'hash-b'}});assert.equal(quoteStage(q,files({items:[{codigoPublico:'a',archivos:[ready]}]}),now),'files');assert.equal(quoteStage(accepted(),files({cotizacion:'otro'}),now),'blocked');});
test('un cambio de reglas que vuelve a bloquear archivos impide pasar a confirmar aunque estén aceptados',()=>assert.equal(quoteStage(accepted(),files({carga:{habilitada:false,codigo:'PAGO_PENDIENTE'},items:[{codigoPublico:'a',archivos:[ready]}]}),now),'payment'));
test('vencimiento exacto, fecha inválida, cancelación y sustitución bloquean el avance',()=>{for(const change of [{vigenteHasta:new Date(now).toISOString()},{vigenteHasta:'fecha inválida'},{estado:'CANCELADA'},{reemplazadaPor:'nueva'}])assert.equal(quoteStage(quote({...accepted(),...change}),files(),now),'blocked');});
test('la consulta de un pedido creado no vuelve a permitir crearlo',()=>assert.equal(quoteStage(quote({estado:'CONFIRMADA',vigenteHasta:'2026-10-01T00:00:00Z'}),null,now),'confirmed'));
test('las correcciones resueltas se bloquean y las pendientes mantienen los requisitos del mismo pedido',()=>{assert.equal(quoteStage(quote({...accepted(),correccion:{pendiente:false}}),files(),now),'blocked');assert.equal(quoteStage(quote({...accepted(),correccion:{pendiente:true}}),files(),now),'files');});
test('los importes a mostrar se detectan sin pérdida de precisión decimal',()=>{assert(!positiveAmount('0.00'));assert(positiveAmount('0.01'));assert(positiveAmount('99999999999999999.99'));assert(!positiveAmount('NaN'));});
test('el PDF se conserva por cotización, ítem y huella; otra cuenta o un resumen nuevo no lo heredan',()=>{const f=new File(['pdf'],'a.pdf');rememberPreparedFiles(quote(),[{sha256:'hash-a',file:f}]);assert.equal(preparedFile('q','a','hash-a'),f);assert.equal(preparedFile('otra','a'),undefined);assert.equal(preparedFile('q','a','hash-b'),undefined);rememberPreparedFiles(quote({codigoPublico:'nueva'}),[]);assert.equal(preparedFile('q','a'),undefined);clearPreparedFiles();assert.equal(preparedFile('nueva','a'),undefined);});
