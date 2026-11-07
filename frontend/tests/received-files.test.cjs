const {test}=require('node:test'),assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript');
const React=require('react'),{renderToStaticMarkup}=require('react-dom/server');
const exportsForTest={};
const source=ts.transpileModule(fs.readFileSync('src/components/received-files.tsx','utf8'),{
 compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,jsx:ts.JsxEmit.ReactJSX},
}).outputText;
vm.runInNewContext(source,{exports:exportsForTest,require:name=>name.endsWith('.css')?{}:require(name)});
const row={cotizacion:'quote-160',numero:160,cliente:'Raúl · Invitado',estado:'CONFIRMADA',archivos:1,validos:1,aceptados:0};
const render=quote=>renderToStaticMarkup(React.createElement(exportsForTest.ReceivedFilesTable,{quotes:[quote]}));
test('una cotización confirmada de invitado abre su pedido para continuar la gestión',()=>{
 const html=render({...row,pedido:'order-156'});
 assert.match(html,/href="\/operacion\/pedidos\/order-156"/);assert.match(html,/>Ver pedido<\/a>/);
 assert.doesNotMatch(html,/quote-160\/archivos/);assert.match(html,/Raúl · Invitado/);
});
test('una cotización sin pedido conserva la consulta de sus archivos',()=>{
 for(const pedido of [null,undefined]){
  const html=render({...row,estado:'VIGENTE',pedido});
  assert.match(html,/href="\/operacion\/cotizaciones\/quote-160\/archivos"/);assert.match(html,/>Ver archivos<\/a>/);
  assert.doesNotMatch(html,/>Ver pedido<\/a>/);
 }
});
