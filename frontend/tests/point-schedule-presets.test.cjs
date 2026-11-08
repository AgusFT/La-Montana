const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript');
const React=require('react'),{renderToStaticMarkup}=require('react-dom/server');
const compile=path=>ts.transpileModule(fs.readFileSync(path,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,jsx:ts.JsxEmit.ReactJSX}}).outputText;
const preset={};vm.runInNewContext(compile('src/lib/point-schedule-presets.ts'),{exports:preset,Error});
const {priorityOneBranch,pointWindowsFromBranch}=preset;
const days=[{dia:6,habilitado:true,apertura:'10:00',cierre:'14:00'},{dia:1,habilitado:true,apertura:'09:00',cierre:'18:00'},{dia:7,habilitado:false,apertura:null,cierre:null}];
const branches=[{codigoPublico:'F',nombre:'Floresta',horarioAtencion:days},{codigoPublico:'L',nombre:'La Plata',horarioAtencion:[{dia:2,habilitado:true,apertura:'11:00',cierre:'16:00'}]}];
const sources=[{sucursal:'L',prioridad:2,habilitado:true,franjas:[]},{sucursal:'F',prioridad:1,habilitado:true,franjas:[]}];
test('propone la sucursal de prioridad 1 aunque la ruta editada tenga prioridad 2',()=>{
 assert.equal(priorityOneBranch(sources,branches),branches[0]);
 assert.equal(priorityOneBranch([{...sources[1],habilitado:false},sources[0]],branches),undefined);
 assert.equal(priorityOneBranch(sources.map(s=>({...s,prioridad:1})),branches),undefined);
});
test('copia días abiertos, horarios y un cupo común sin modificar la sucursal ni las rutas',()=>{
 const before=JSON.stringify({branches,sources}),copy=pointWindowsFromBranch(branches[0],12);
 assert.equal(JSON.stringify(copy),JSON.stringify([{dia:1,apertura:'09:00',cierre:'18:00',capacidadPedidos:12,habilitada:true},{dia:6,apertura:'10:00',cierre:'14:00',capacidadPedidos:12,habilitada:true}]));
 copy[0].apertura='09:30';assert.equal(JSON.stringify({branches,sources}),before);
});
test('no inventa horarios ni cupos cuando faltan o son inválidos',()=>{
 for(const capacity of [0,-1,NaN,1.5,1000001])assert.throws(()=>pointWindowsFromBranch(branches[0],capacity),/cupo/);
 for(const hours of [[],[{dia:1,habilitado:true,apertura:null,cierre:'18:00'}],[{dia:1,habilitado:true,apertura:'18:00',cierre:'09:00'}],[days[1],days[1]]])assert.throws(()=>pointWindowsFromBranch({...branches[0],horarioAtencion:hours},10),/horarios|días/);
});
function tools({chosen='',capacity='',existing=[]}={}){
 const state=[chosen,capacity,'',''],changes=[];let index=0,copied;
 const out={};vm.runInNewContext(compile('src/components/point-schedule-tools.tsx'),{exports:out,Error,require:name=>{
  if(name==='react')return {useState:()=>{const i=index++;return [state[i],value=>{changes.push([i,value]);state[i]=value;}];}};
  if(name==='react/jsx-runtime')return require(name);
  if(name==='@/lib/point-schedule-presets')return preset;
  if(name.endsWith('.css'))return {default:{}};
  return {};
 }});
 const element=out.PointScheduleTools({branches,sources,route:{...sources[0],franjas:existing},onChange:value=>{copied=value;}});
 function find(node,type){if(!node||typeof node!=='object')return undefined;if(node.type===type)return node;return React.Children.toArray(node.props?.children).map(child=>find(child,type)).find(Boolean);}
 return {html:renderToStaticMarkup(element),apply:()=>find(element,'button').props.onClick(),state,get copied(){return copied;}};
}
test('editar un punto con franjas existentes sigue mostrando la copia desde prioridad 1',()=>{
 const t=tools({capacity:'15',existing:[{dia:2}]});assert.match(t.html,/Floresta · prioridad 1/);assert.match(t.html,/value="F" selected/);assert.match(t.html,/Copiar y reemplazar franjas/);
 t.apply();assert.equal(t.copied.length,2);assert.equal(t.copied[0].apertura,'09:00');assert.equal(t.copied[0].capacidadPedidos,15);assert.match(t.state[3],/antes de guardar/);
});
test('puede elegir otra sucursal y copiarla en lugar de la prioritaria',()=>{
 const t=tools({chosen:'L',capacity:'7'});t.apply();assert.equal(t.copied.length,1);assert.equal(t.copied[0].dia,2);assert.equal(t.copied[0].apertura,'11:00');assert.equal(t.copied[0].capacidadPedidos,7);
});
test('sin cupo explícito no reemplaza las franjas existentes',()=>{
 const t=tools();t.apply();assert.equal(t.copied,undefined);assert.match(t.state[2],/cupo/);
});
