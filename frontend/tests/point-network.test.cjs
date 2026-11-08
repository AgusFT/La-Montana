const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),ts=require('typescript'),Module=require('node:module');
function load(path,stubs={}){const m=new Module(path,module);m.paths=module.paths;m.require=id=>Object.hasOwn(stubs,id)?stubs[id]:require(id);m._compile(ts.transpileModule(fs.readFileSync(path,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,jsx:ts.JsxEmit.ReactJSX}}).outputText,path);return m.exports;}
const {printerInventory}=load('src/lib/printer-inventory.ts');
const printer=(id,branch,state='OPERATIVA')=>({codigoPublico:id,sucursal:branch,nombre:id,estado:state});
const branches=[{codigoPublico:'F',nombre:'Floresta'},{codigoPublico:'L',nombre:'La Plata'},{codigoPublico:'M',nombre:'Medicina'}];
test('el inventario incluye todas las sucursales y distingue equipos vigentes faltantes',()=>{
 const active=[printer('F1','F'),printer('F2','F'),printer('F3','F'),printer('F4','F'),printer('L1','L'),printer('L2','L'),printer('M1','M')];
 const data=printerInventory([active[0],active[1]],active,branches);
 assert.equal(data.rows.length,7);assert.equal(data.missing.length,5);assert.deepEqual(data.groups.map(g=>[g.name,g.active,g.draft,g.missing]),[['Floresta',4,2,2],['La Plata',2,0,2],['Medicina',1,0,1]]);
});
test('los cambios del borrador y los retiros explícitos no se pisan al comparar inventario',()=>{
 const edited={...printer('F1','F','DESHABILITADA'),nombre:'Editada'},retired=printer('L1','L','RETIRADA');const data=printerInventory([edited,retired],[printer('F1','F'),printer('L1','L'),printer('M1','M')],branches);
 assert.equal(data.rows[0].printer,edited);assert.deepEqual(data.missing.map(p=>p.codigoPublico),['M1']);assert.equal(data.rows.filter(p=>p.printer.codigoPublico==='L1').length,1);
});
test('una sucursal sin equipos sigue apareciendo y no se inventan impresoras',()=>{
 const data=printerInventory([],[],branches);assert.equal(data.rows.length,0);assert.equal(data.groups.length,3);assert(data.groups.every(g=>g.active===0&&g.draft===0));
});
const {sourcesForBranch,isPointNetwork}=load('src/lib/point-network.ts',{'./organization-types':{isBranch:()=>true}});
test('las asignaciones de puntos se leen de los datos guardados y permiten compartir',()=>{
 const p={definicion:{codigoPublico:'P',sucursales:[{sucursal:'L',habilitado:true},{sucursal:'F',habilitado:true},{sucursal:'M',habilitado:false}]}};const n={puntos:[p]};assert.equal(sourcesForBranch(n,'L').length,1);assert.equal(sourcesForBranch(n,'F').length,1);assert.equal(sourcesForBranch(n,'M').length,0);
});
test('la red rechaza respuestas incompletas o versiones no confirmadas',()=>{
 const point={version:1,revision:'revision',actor:'Admin',fecha:'2026-11-08T00:00:00Z',definicion:{codigoPublico:'punto',codigo:'P',nombre:'Punto',calle:'Calle',numero:'1',localidad:'Ciudad',provincia:'Provincia',codigoPostal:'1234',zonaHoraria:'America/Argentina/Buenos_Aires',referencias:null,sucursales:[{sucursal:'L',prioridad:1,habilitado:true,costo:'0.00',franjas:[{dia:1,apertura:'14:00',cierre:'18:00',capacidadPedidos:10,habilitada:true}]}]}};
 const panel={puntos:[point],sucursales:[],configuracionActiva:true};assert.equal(isPointNetwork(panel),true);
 const invalid=structuredClone(panel);invalid.puntos[0].definicion.sucursales[0].franjas[0].dia=8;assert.equal(isPointNetwork(invalid),false);
 assert.equal(isPointNetwork({puntos:[{version:-1}],sucursales:[],configuracionActiva:true}),false);
});
test('el proxy conserva operaciones y CSRF de la red sin abrir rutas vecinas',async()=>{
 const proxy=load('src/app/api/[...path]/route.ts',{'@/lib/organization-types':{uuidPattern:/^[0-9a-f]{8}-[0-9a-f-]{27}$/}}),old=global.fetch,calls=[];process.env.BACKEND_INTERNAL_URL='http://backend.test';global.fetch=async(url,init)=>{calls.push({url,init});return Response.json({ok:true});};
 const id='11111111-1111-4111-8111-111111111111';async function req(method,path){return proxy[method](new Request('http://app.test/api/'+path.join('/'),{method,headers:{Cookie:'SESSION=prueba','X-CSRF-TOKEN':'token','Content-Type':'application/json'},...(method!=='GET'?{body:'{"operacion":"misma-operacion","version":0}'}:{})}),{params:Promise.resolve({path})});}
 try{
  assert.equal((await req('GET',['admin','red-puntos'])).status,200);assert.equal((await req('POST',['admin','red-puntos'])).status,200);assert.equal((await req('PUT',['admin','red-puntos',id])).status,200);
  assert.equal(calls[2].init.headers.get('X-CSRF-TOKEN'),'token');assert.equal(calls[2].init.headers.get('Cookie'),'SESSION=prueba');assert.match(calls[2].init.body,/misma-operacion/);
  assert.equal((await req('GET',['admin','red-puntos',id])).status,405);assert.equal((await req('PUT',['admin','red-puntos',id,'otra'])).status,404);assert.equal(calls.length,3);
 }finally{global.fetch=old;}
});
