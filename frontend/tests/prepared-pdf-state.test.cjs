const {test}=require('node:test'),assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript');
const React=require('react'),{renderToStaticMarkup}=require('react-dom/server');
const compile=path=>ts.transpileModule(fs.readFileSync(path,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,jsx:ts.JsxEmit.ReactJSX}}).outputText;
const print={};vm.runInNewContext(compile('src/lib/print-options.ts'),{exports:print});
const source=compile('src/components/quote-form.tsx');
function render(state,configured=false){
 const file={codigoPublico:'pdf',nombre:'trabajo.pdf',estado:state,paginas:state==='VALIDO'?10:null,bytes:1000,mensaje:'Motivo específico del análisis.'};
 const options={disponible:true,combinaciones:[{sucursal:'matriz',servicio:'impresion',formato:'A4',papel:'80g',color:'COLOR',dobleFaz:true,terminaciones:['anillado']}],formatos:[{codigoPublico:'A4',nombre:'A4'}],papeles:[{codigoPublico:'80g',nombre:'Obra 80 g'}],servicios:[{codigoPublico:'anillado',nombre:'Anillado'}]};
 const row={archivo:'pdf',choice:configured?'impresion:A4:80g:COLOR':'',copies:'1',double:false,ends:[]};
 const states=[options,{codigoPublico:'preparacion',archivos:[file]},[row],'pdf'];let i=0;
 const out={};vm.runInNewContext(source,{exports:out,require:name=>{
  if(name==='react')return {...React,useState:initial=>[i<states.length?states[i++]:typeof initial==='function'?initial():initial,()=>{}],useRef:value=>({current:value}),useEffect:()=>{}};
  if(name==='react/jsx-runtime')return require(name);
  if(name==='next/navigation')return {useRouter:()=>({})};
  if(name==='@/lib/print-options')return print;
  if(name==='./navigation-boundary')return {useNavigationGuard:()=>{}};
  if(name.startsWith('./'))return new Proxy({},{get:()=>()=>null});
  return {};
 }});
 return renderToStaticMarkup(React.createElement(out.QuoteForm,{}));
}
const budget=html=>html.match(/<button[^>]*>Revisar presupuesto →<\/button>/)[0];
test('un PDF rechazado explica el bloqueo y ofrece quitarlo aunque tenga opciones guardadas',()=>{
 const html=render('RECHAZADO',true);
 assert.match(html,/PDF no admitido/);assert.match(html,/role="alert"/);
 assert.match(html,/Motivo específico del análisis/);assert.match(html,/Quitar este PDF/);
 assert.match(html,/<fieldset disabled="" class="workspace-print-fields"/);
 assert.match(budget(html),/disabled=""/);assert.doesNotMatch(html,/type="checkbox"/);
});
test('un análisis fallido permite recuperar el flujo y no habilita presupuesto',()=>{
 const html=render('FALLIDO');assert.match(html,/No pudimos analizar este PDF/);assert.match(html,/Quitar este PDF/);assert.match(budget(html),/disabled=""/);
});
test('un PDF en análisis todavía no permite configurar ni presupuestar',()=>{
 const html=render('VALIDANDO');assert.doesNotMatch(html,/role="alert"/);assert.match(html,/Analizando seguridad y páginas/);assert.match(budget(html),/disabled=""/);
});
test('un PDF válido habilita tamaño y copias y requiere completar las opciones',()=>{
 const html=render('VALIDO');assert.doesNotMatch(html,/role="alert"/);
 assert.match(html,/<fieldset class="workspace-print-fields"/);assert.match(html,/<option value="A4">A4/);assert.match(budget(html),/disabled=""/);
});
test('un PDF válido y configurado muestra adicionales y permite revisar el presupuesto',()=>{
 const html=render('VALIDO',true);assert.match(html,/1 de 1 archivos configurados/);
 assert.match(html,/type="checkbox"/);assert.match(html,/Anillado/);assert.doesNotMatch(budget(html),/disabled/);
});
