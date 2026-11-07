const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript');
const React=require('react'),{renderToStaticMarkup}=require('react-dom/server');
const exportsForTest={};
const source=ts.transpileModule(fs.readFileSync('src/components/order-receipt-action.tsx','utf8'),{
 compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,jsx:ts.JsxEmit.ReactJSX},
}).outputText;
vm.runInNewContext(source,{exports:exportsForTest,require:name=>{
 if(name==='react'||name==='react/jsx-runtime'||name==='react-dom')return require(name);
 if(name==='@/lib/quote-types')return {money:value=>value};
 return {};
}});
const render=saldo=>renderToStaticMarkup(React.createElement(exportsForTest.OrderReceiptAction,{
 base:'/api/operacion/cotizaciones/ejemplo/pagos',medio:'TRANSFERENCIA',saldo,onSaved:()=>{},
}));

test('al reabrir un pedido sin deuda no ofrece volver a registrar el cobro',()=>{
 for(const saldo of ['0','0.00'])assert.equal(render(saldo),'');
});
test('un saldo positivo conserva la acción para registrar el dinero recibido',()=>{
 const html=render('1640.00');
 assert.match(html,/Registrar cobro/);
 assert.match(html,/1640.00/);
 assert.match(html,/<button/);
});
