const {test}=require('node:test');
const assert=require('node:assert/strict');
const ts=require('typescript'),fs=require('node:fs');
require.extensions['.ts']=(module,filename)=>module._compile(ts.transpileModule(fs.readFileSync(filename,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,filename);
const {deliveryExperience}=require('../src/lib/delivery-experience.ts');
const order=(modalidad,retiroConTraslado=false)=>({reserva:{modalidad},retiroConTraslado,oferta:{sucursal:{nombre:'Casa Matriz Floresta'}}});
test('domicilio en viaje permite entregar sin pedir llegada a un punto de retiro',()=>{
 const home=deliveryExperience(order('ENVIO_DOMICILIO'),'EN_VIAJE');
 assert.equal(home.title,'Entrega a domicilio en curso');assert.equal(home.canReceive,true);assert.equal(home.home,true);assert.doesNotMatch(home.description,/registrá la llegada/i);
 const point=deliveryExperience(order('RETIRO_PUNTO_ENTREGA'),'EN_VIAJE');assert.equal(point.canReceive,false);assert.equal(point.title,'Esperando recepción del punto');assert.match(point.description,/rider.*responsable del punto/);
});
test('ausencia deja la entrega pendiente e impide ingresar código hasta el nuevo intento',()=>{
 const absent=deliveryExperience(order('ENVIO_DOMICILIO'),'CLIENTE_AUSENTE');assert.equal(absent.canReceive,false);assert.equal(absent.absent,true);assert.match(absent.description,/código se conserva/);
 assert.equal(deliveryExperience(order('ENVIO_DOMICILIO'),'EN_VIAJE').canReceive,true);
 for(const state of ['PENDIENTE_PREPARACION','PREPARADO','CLIENTE_AUSENTE','ENTREGADO'])assert.equal(deliveryExperience(order('ENVIO_DOMICILIO'),state).canReceive,false);
});
test('retiro local y traslado conservan sus condiciones independientes',()=>{
 assert.equal(deliveryExperience(order('RETIRO_SUCURSAL'),'LISTO_RETIRO').canReceive,true);
 assert.equal(deliveryExperience(order('RETIRO_SUCURSAL',true),'EN_VIAJE').canReceive,false);
 assert.equal(deliveryExperience(order('RETIRO_SUCURSAL',true),'DISPONIBLE_PUNTO').canReceive,true);
});
