const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),ts=require('typescript');
require.extensions['.ts']=(m,p)=>m._compile(ts.transpileModule(fs.readFileSync(p,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,p);
const {ClientTransferCommand,transferFileError}=require('../src/lib/client-transfer.ts');
const {receiptSuggestion}=require('../src/lib/payment-receipt.ts');
const view={cotizacion:'quote',total:'42100.00',aplicado:'0.00',pendiente:'42100.00',anticipoOfertado:'12630.00',coberturaAnticipoOfertado:'0.00',anticipoActual:'12630.00',coberturaAnticipoActual:'0.00',mediosAnticipo:['TRANSFERENCIA'],intentos:[],pagos:[],relacionadas:[],historial:[],permisos:[]};
const file=new File(['%PDF-1.4 receipt'],'comprobante.pdf',{type:'application/pdf'});
const json=v=>Promise.resolve(new Response(JSON.stringify(v)));
function reported(c){return {...view,intentos:[{codigoPublico:'attempt',referencia:c.reference,importe:JSON.parse(c.body).importe,estado:'PENDIENTE'}]};}
test('sugiere seña, total previo o saldo sin tratar otro aviso pendiente como dinero acreditado',()=>{
 assert.equal(receiptSuggestion({...view,intentos:[{codigoPublico:'old',estado:'PENDIENTE',importe:'10.00'}]},'TRANSFERENCIA').importe,'12630.00');
 assert.equal(receiptSuggestion({...view,anticipoActual:'42100.00'},'TRANSFERENCIA').importe,'42100.00');
 assert.equal(receiptSuggestion({...view,coberturaAnticipoOfertado:'12630.00',coberturaAnticipoActual:'12630.00',pendiente:'29470.00'},'TRANSFERENCIA').importe,'29470.00');
});
test('informa sin comprobante ni texto obligatorio, conserva saldo y no llama a recibir/aplicar',async()=>{
 const c=new ClientTransferCommand('quote','12630.00',null),calls=[];
 const result=await c.execute((path,body)=>{calls.push(path);assert.equal(JSON.parse(body).importe,'12630.00');return json(reported(c));});
 assert.deepEqual(calls,['/api/cliente/cotizaciones/quote/pagos/informar']);
 assert.equal(result.view.aplicado,'0.00');assert.equal(result.view.pendiente,'42100.00');assert.equal(result.evidence,null);
 await c.execute(()=>{throw Error('must not repeat report');});
});
test('una respuesta perdida al informar reenvía la misma operación y referencia',async()=>{
 const c=new ClientTransferCommand('quote','12630.00',null),bodies=[];
 await assert.rejects(c.execute((_,body)=>{bodies.push(body);return Promise.reject(Error('lost response'));}));
 await c.execute((_,body)=>{bodies.push(body);return json(reported(c));});
 assert.equal(bodies[0],bodies[1]);assert.ok(JSON.parse(bodies[0]).operacion);
});
test('el comprobante se vincula exactamente al nuevo aviso, después de informar',async()=>{
 const c=new ClientTransferCommand('quote','42100.00',file),paths=[];
 const result=await c.execute((path,body,type,method)=>{
  paths.push(path);
  if(path.endsWith('/informar'))return json({...reported(c),intentos:[{codigoPublico:'unrelated',referencia:'other'},...reported(c).intentos]});
  if(path.endsWith('/comprobantes')){const b=JSON.parse(body);assert.equal(b.intento,'attempt');assert.equal(b.nombre,file.name);return json({codigoPublico:'evidence',estado:'PENDIENTE'});}
  assert.equal(method,'PUT');assert.equal(type,'application/pdf');assert.equal(body,file);return json({codigoPublico:'evidence',estado:'VALIDO'});
 });
 assert.equal(paths.length,3);assert.equal(result.evidence.estado,'VALIDO');assert.equal(result.view.aplicado,'0.00');
});
test('recuperar la creación del comprobante no repite el aviso y conserva su operación',async()=>{
 const c=new ClientTransferCommand('quote','12630.00',file),bodies=[];
 await assert.rejects(c.execute((path,body)=>{
  if(path.endsWith('/informar'))return json(reported(c));bodies.push(body);throw Error('metadata response lost');
 }));
 assert.ok(c.view);
 await c.execute((path,body)=>{
  assert.ok(!path.endsWith('/informar'));
  if(path.endsWith('/comprobantes')){bodies.push(body);return json({codigoPublico:'evidence',estado:'PENDIENTE'});}
  return json({codigoPublico:'evidence',estado:'VALIDO'});
 });
 assert.equal(bodies[0],bodies[1]);
});
test('recuperar los bytes reutiliza el mismo comprobante sin crear otro aviso ni archivo',async()=>{
 const c=new ClientTransferCommand('quote','12630.00',file);
 await assert.rejects(c.execute(path=>{
  if(path.endsWith('/informar'))return json(reported(c));
  if(path.endsWith('/comprobantes'))return json({codigoPublico:'evidence',estado:'PENDIENTE'});
  throw Error('upload response lost');
 }));
 const result=await c.execute(path=>{assert.equal(path,'/api/cliente/cotizaciones/quote/comprobantes/evidence/contenido');return json({codigoPublico:'evidence',estado:'VALIDANDO'});});
 assert.equal(result.evidence.estado,'VALIDANDO');
});
test('un PDF rechazado no borra el aviso; una respuesta ajena no se acepta como confirmación',async()=>{
 const c=new ClientTransferCommand('quote','12630.00',file);
 const result=await c.execute(path=>json(path.endsWith('/informar')?reported(c):{codigoPublico:'evidence',estado:path.endsWith('/contenido')?'RECHAZADO':'PENDIENTE'}));
 assert.ok(result.view);assert.equal(result.evidence.estado,'RECHAZADO');
 const wrong=new ClientTransferCommand('quote','10.00',null);
 await assert.rejects(wrong.execute(()=>json({...reported(wrong),cotizacion:'other'})));assert.equal(wrong.view,null);
 await assert.rejects(wrong.execute(()=>json(view)));assert.equal(wrong.view,null);
});
test('comprobante opcional: limita PDF vacío, tamaño, extensión y nombre antes de informar',()=>{
 assert.equal(transferFileError(null),'');assert.equal(transferFileError(file),'');
 for(const bad of [{name:'receipt.jpg',size:100},{name:'empty.pdf',size:0},{name:'large.pdf',size:10485761},{name:'a'.repeat(256)+'.pdf',size:20}])assert.ok(transferFileError(bad));
 assert.equal(transferFileError({name:'receipt.PDF',size:10485760}),'');
});
