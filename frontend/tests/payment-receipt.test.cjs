const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),ts=require('typescript');
require.extensions['.ts']=(m,p)=>m._compile(ts.transpileModule(fs.readFileSync(p,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,p);
const {receiptSuggestion,normalizeReceiptAmount}=require('../src/lib/payment-receipt.ts');
const view=changes=>({pendiente:'34560.00',anticipoOfertado:'10368.00',coberturaAnticipoOfertado:'0.00',anticipoActual:'10368.00',coberturaAnticipoActual:'0.00',mediosAnticipo:['TRANSFERENCIA'],intentos:[],pedidoFinalizado:false,pedidoCerrado:false,cotizacionSustituida:false,...changes});
test('sugiere la seña faltante y luego el saldo, sin volver a cobrar la seña cubierta',()=>{
 assert.equal(receiptSuggestion(view(),'TRANSFERENCIA').importe,'10368.00');
 assert.equal(receiptSuggestion(view({coberturaAnticipoOfertado:'4000.10',coberturaAnticipoActual:'4000.10'}),'TRANSFERENCIA').importe,'6367.90');
 assert.equal(receiptSuggestion(view({coberturaAnticipoOfertado:'10368.00',coberturaAnticipoActual:'10368.00',pendiente:'24192.00'}),'TRANSFERENCIA').importe,'24192.00');
});
test('pago total previo, condiciones históricas y límite del saldo se conservan',()=>{
 assert.equal(receiptSuggestion(view({anticipoActual:'34560.00'}),'TRANSFERENCIA').importe,'34560.00');
 assert.equal(receiptSuggestion(view({anticipoActual:'0.00'}),'TRANSFERENCIA').importe,'10368.00');
 assert.equal(receiptSuggestion(view({pendiente:'100.01'}),'TRANSFERENCIA').importe,'100.01');
});
test('verificar un aviso propone su importe real declarado, incluso si excede el saldo',()=>{
 const data=view({intentos:[{codigoPublico:'aviso',estado:'PENDIENTE',importe:'40000.00'}]});
 assert.equal(receiptSuggestion(data,'TRANSFERENCIA','aviso').importe,'40000.00');
 assert.equal(receiptSuggestion({...data,intentos:[{...data.intentos[0],estado:'ACREDITADO'}]},'TRANSFERENCIA','aviso').importe,'10368.00');
});
test('un pedido terminado o sin saldo no sugiere otro cobro',()=>{
 for(const key of ['pedidoFinalizado','pedidoCerrado','cotizacionSustituida'])assert.equal(receiptSuggestion(view({[key]:true}),'TRANSFERENCIA').importe,'');
 assert.equal(receiptSuggestion(view({pendiente:'0.00'}),'TRANSFERENCIA').importe,'');
});
test('un medio no admitido para la seña sólo propone el saldo posterior',()=>{
 assert.equal(receiptSuggestion(view(),'EFECTIVO').importe,'24192.00');
 assert.equal(receiptSuggestion(view({pendiente:'10368.00'}),'EFECTIVO').importe,'');
});
test('admite coma o punto decimal sin redondear ni aceptar formatos ambiguos',()=>{
 for(const [input,expected] of [['10368,10','10368.10'],[' 10.1 ','10.10'],['0001','1.00'],['99999999999999999.99','99999999999999999.99']])assert.equal(normalizeReceiptAmount(input),expected);
 for(const input of ['','0','-10','1.234,56','1,234.56','1e3','1.001','1,2,3','Infinity','100000000000000000'])assert.equal(normalizeReceiptAmount(input),'');
});
