const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),ts=require('typescript'),Module=require('node:module');
const source=fs.readFileSync('src/app/api/[...path]/route.ts','utf8');const m=new Module('admin-global-proxy',module);m.paths=module.paths;m.require=id=>id==='@/lib/organization-types'?{uuidPattern:/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i}:require(id);m._compile(ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,'admin-global-proxy');
test('la consulta administrativa global conserva filtro, paginación y sesión sin enviar una sucursal',async()=>{
 const old=global.fetch;process.env.BACKEND_INTERNAL_URL='http://backend.test';const calls=[];global.fetch=async(url,init)=>{calls.push({url,init});return Response.json({elementos:[],total:0,pagina:2});};
 try{const r=await m.exports.GET(new Request('http://app.test/api/admin/pedidos?estado=APROBADO&pagina=2',{headers:{Cookie:'SESSION=admin'}}),{params:Promise.resolve({path:['admin','pedidos']})});assert.equal(r.status,200);assert.equal(calls[0].url,'http://backend.test/api/admin/pedidos?estado=APROBADO&pagina=2');assert.equal(calls[0].init.headers.get('Cookie'),'SESSION=admin');assert.equal(calls[0].init.cache,'no-store');assert.equal(r.headers.get('Cache-Control'),'no-store');}finally{global.fetch=old;}
});
test('la ruta global no habilita escrituras ni rutas administrativas vecinas',async()=>{
 for(const method of ['POST','PUT']){const r=await m.exports[method](new Request('http://app.test/api/admin/pedidos',{method,body:'{}'}),{params:Promise.resolve({path:['admin','pedidos']})});assert.equal(r.status,405);}
 for(const path of [['admin','pedidos','otro'],['admin','pedidos-globales']]){const r=await m.exports.GET(new Request('http://app.test/api/'+path.join('/')),{params:Promise.resolve({path})});assert.equal(r.status,404);}
});
test('la bandeja conserva el orden temporal y el grupo sólo habilita POST a su ruta exacta',async()=>{
 const old=global.fetch;process.env.BACKEND_INTERNAL_URL='http://backend.test';const calls=[];
 global.fetch=async(url,init)=>{calls.push({url,init});return Response.json({cantidad:2});};
 try{
  await m.exports.GET(new Request('http://app.test/api/admin/pedidos?estado=LISTO_PARA_ENTREGA&orden=MOVIMIENTO_ASC'),{params:Promise.resolve({path:['admin','pedidos']})});
  assert.match(calls[0].url,/orden=MOVIMIENTO_ASC/);
  const request=new Request('http://app.test/api/operacion/entregas/lotes',{method:'POST',body:'{"pedidos":[]}',headers:{Cookie:'SESSION=operador','X-CSRF-TOKEN':'csrf','Content-Type':'application/json'}});
  const out=await m.exports.POST(request,{params:Promise.resolve({path:['operacion','entregas','lotes']})});assert.equal(out.status,200);assert.equal(calls[1].init.headers.get('X-CSRF-TOKEN'),'csrf');
  const invalid=await m.exports.GET(new Request('http://app.test/api/operacion/entregas/lotes'),{params:Promise.resolve({path:['operacion','entregas','lotes']})});assert.equal(invalid.status,405);
 }finally{global.fetch=old;}
});
