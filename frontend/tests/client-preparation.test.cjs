const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),ts=require('typescript');
const Module=require('node:module');
const source=fs.readFileSync('src/app/api/[...path]/route.ts','utf8');
const m=new Module('client-proxy',module);m.filename='client-proxy';m.paths=module.paths;
m.require=id=>id==='@/lib/organization-types'?{uuidPattern:/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i}:require(id);
m._compile(ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,'client-proxy');
const id='11111111-1111-4111-8111-111111111111',file='22222222-2222-4222-8222-222222222222';
const path=['cliente','preparaciones',id];
const proxy=(method,p,body,headers={})=>m.exports[method](new Request('http://app.test/api/'+p.join('/'),{method,headers,body}),{params:Promise.resolve({path:p})});
test('preparación: lista explícita de rutas y métodos, sin ampliar superficies vecinas',async()=>{
 process.env.BACKEND_INTERNAL_URL='http://backend.test';const old=global.fetch,calls=[];
 global.fetch=async(url,init)=>{calls.push({url,init});return Response.json({ok:true});};
 try{for(const p of [['cliente','preparaciones'],[...path,'presupuesto'],[...path,'cotizar'],[...path,'confirmar'],[...path,'archivos',file,'quitar'],[...path,'archivos',file,'aceptar']])assert.equal((await proxy('POST',p,'{}',{'Content-Type':'application/json'})).status,200);
 for(const p of [path,[...path,'resumen'],[...path,'archivos'],[...path,'archivos',file,'carga']])assert.equal((await proxy('GET',p)).status,200);
 assert.equal((await proxy('POST',[...path,'resumen'],'{}')).status,405);
 for(const p of [['cliente','preparaciones','no-uuid'],[...path,'activar'],[...path,'archivos',file,'reemplazar'],[...path,'archivos',file,'paginas','0'],[...path,'resumen','extra']])assert.equal((await proxy('GET',p)).status,404);
 assert(calls.every(c=>c.init.redirect==='manual'&&c.init.cache==='no-store'));
 }finally{global.fetch=old;}
});
test('la carga preparada conserva PDF binario, propietario/cookie y CSRF; consulta permiso antes de recibir bytes',async()=>{
 process.env.BACKEND_INTERNAL_URL='http://backend.test';const old=global.fetch,calls=[];
 global.fetch=async(url,init)=>{calls.push({url,init});return Response.json(url.endsWith('/carga')?{habilitada:true}:{estado:'VALIDO'});};
 try{const result=await proxy('PUT',[...path,'archivos',file,'contenido'],new Uint8Array([37,80,68,70,45]),{'Content-Type':'application/pdf','Cookie':'SESSION=privada','X-CSRF-TOKEN':'token'});assert.equal(result.status,200);assert.equal(calls.length,2);assert(calls[0].url.endsWith('/carga'));assert(calls[1].init.body instanceof Uint8Array);assert.equal(calls[1].init.headers.get('Cookie'),'SESSION=privada');assert.equal(calls[1].init.headers.get('X-CSRF-TOKEN'),'token');
 assert.equal((await proxy('PUT',[...path,'archivos',file,'contenido'],'texto',{'Content-Type':'text/plain'})).status,415);
 assert.equal((await proxy('PUT',[...path,'archivos',file,'contenido'],'x',{'Content-Type':'application/pdf','Content-Length':'10485761'})).status,413);
 }finally{global.fetch=old;}
});
test('descarga y vista previa preparadas no convierten bytes privados a JSON',async()=>{
 process.env.BACKEND_INTERNAL_URL='http://backend.test';const old=global.fetch;
 global.fetch=async()=>new Response(new Uint8Array([37,80,68,70,45]),{headers:{'Content-Type':'application/pdf','Content-Disposition':'attachment; filename=privado.pdf'}});
 try{const result=await proxy('GET',[...path,'archivos',file,'original']);assert.equal(result.status,200);assert.equal(result.headers.get('Content-Type'),'application/pdf');assert.equal(result.headers.get('Cache-Control'),'no-store');assert.equal(result.headers.get('X-Content-Type-Options'),'nosniff');assert.deepEqual([...new Uint8Array(await result.arrayBuffer())],[37,80,68,70,45]);}finally{global.fetch=old;}
});
