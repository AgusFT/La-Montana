const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),ts=require('typescript'),Module=require('node:module');
const m=new Module('delivery-locations',module);m.paths=module.paths;
m._compile(ts.transpileModule(fs.readFileSync('src/lib/delivery-locations.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,'delivery-locations');
const {deliveryLocations,deliveryModes,deliverySelectionAvailable,deliveryLocationLabel}=m.exports;
const branch=(id,disponible)=>({codigoPublico:id,nombre:id,disponible});
const point=(available,origins)=>({modalidad:'RETIRO_PUNTO_ENTREGA',codigoPublico:'city-bell',nombre:'City Bell',direccion:'Centenario',disponible:available,motivo:available?null:'Momentáneamente fuera de servicio',sucursales:origins});
const options=(destinations,available=true)=>({disponible:available,modalidades:available?['RETIRO_SUCURSAL']:[],destinosEntrega:destinations,sucursales:[],puntos:[]});
test('un punto exclusivo pausado permanece visible y no puede cotizarse',()=>{
 const o=options([point(false,[branch('La Plata',false)])]);const [p]=deliveryLocations(o,'RETIRO_PUNTO_ENTREGA');
 assert.equal(p.nombre,'City Bell');assert.equal(p.disponible,false);assert.match(deliveryLocationLabel(p),/Momentáneamente fuera de servicio/);
 assert.equal(deliverySelectionAvailable(o,'RETIRO_PUNTO_ENTREGA','','city-bell'),false);
});
test('un punto compartido conserva el origen habilitado y se restringe por mostrador',()=>{
 const o=options([point(true,[branch('La Plata',false),branch('Floresta',true)])]);
 assert.equal(deliverySelectionAvailable(o,'RETIRO_PUNTO_ENTREGA','','city-bell'),true);
 assert.equal(deliveryLocations(o,'RETIRO_PUNTO_ENTREGA','La Plata')[0].disponible,false);
 assert.equal(deliveryLocations(o,'RETIRO_PUNTO_ENTREGA','Floresta')[0].disponible,true);
 assert.equal(deliveryLocations(o,'RETIRO_PUNTO_ENTREGA','Medicina').length,0);
});
test('las sucursales pausadas siguen en el selector pero no permiten confirmar',()=>{
 const o=options([{modalidad:'RETIRO_SUCURSAL',codigoPublico:'La Plata',nombre:'La Plata',direccion:'Calle 7',disponible:false,motivo:'Pausada',sucursales:[]}]);
 assert.equal(deliveryLocations(o,'RETIRO_SUCURSAL').length,1);
 assert.equal(deliverySelectionAvailable(o,'RETIRO_SUCURSAL','La Plata',''),false);
});
test('si todos están pausados se puede inspeccionar la modalidad pero no crear pedidos',()=>{
 const o=options([point(false,[branch('La Plata',false)])],false);
 assert.deepEqual(deliveryModes(o),['RETIRO_PUNTO_ENTREGA']);
 assert.equal(deliverySelectionAvailable(o,'RETIRO_PUNTO_ENTREGA','','city-bell'),false);
});
test('reanudar un origen habilita el punto sin modificar su identificador',()=>{
 assert.equal(deliveryLocations(options([point(false,[branch('La Plata',false)])]),'RETIRO_PUNTO_ENTREGA')[0].codigoPublico,'city-bell');
 assert.equal(deliverySelectionAvailable(options([point(true,[branch('La Plata',true)])]),'RETIRO_PUNTO_ENTREGA','','city-bell'),true);
});
test('la pausa propia del punto no se levanta por tener una sucursal activa',()=>{
 assert.equal(deliverySelectionAvailable(options([point(false,[branch('Floresta',true)])]),'RETIRO_PUNTO_ENTREGA','','city-bell'),false);
});
test('un punto ajeno o eliminado no permite reutilizar una selección anterior',()=>{
 assert.equal(deliverySelectionAvailable(options([]),'RETIRO_PUNTO_ENTREGA','','city-bell'),false);
});
test('compatibilidad con respuestas anteriores: los puntos compartidos se muestran una sola vez',()=>{
 const o={disponible:true,modalidades:['RETIRO_PUNTO_ENTREGA'],sucursales:[],puntos:[{codigoPublico:'city-bell',sucursal:'La Plata',prioridad:1,nombre:'City Bell',direccion:'Centenario'},{codigoPublico:'city-bell',sucursal:'Floresta',prioridad:2,nombre:'City Bell',direccion:'Centenario'}]};
 assert.equal(deliveryLocations(o,'RETIRO_PUNTO_ENTREGA').length,1);assert.equal(deliverySelectionAvailable(o,'RETIRO_PUNTO_ENTREGA','','city-bell'),true);
});
test('el contrato de opciones exige estados booleanos para decidir qué destino habilitar',()=>{
 const q=new Module('quote-types',module);q.paths=module.paths;q._compile(ts.transpileModule(fs.readFileSync('src/lib/quote-types.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,'quote-types');
 const o={...options([point(false,[branch('La Plata',false)])]),mensaje:'Disponible',combinaciones:[],formatos:[],papeles:[],servicios:[],medios:[]};
 assert.equal(q.exports.isOptions(o),true);
 assert.equal(q.exports.isOptions({...o,destinosEntrega:[{...o.destinosEntrega[0],disponible:'false'}]}),false);
});
test('la lista operativa muestra fuera de servicio un punto habilitado manualmente sin origen',()=>{
 const p=new Module('point-availability',module);p.paths=module.paths;p.require=()=>({uuidPattern:/^[0-9a-f-]+$/});
 p._compile(ts.transpileModule(fs.readFileSync('src/lib/point-availability-types.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,'point-availability');
 const effective=p.exports.pointReceivesOrders;
 assert.equal(effective({disponibilidad:{estado:'HABILITADO'},disponibleParaPedidos:false}),false);
 assert.equal(effective({disponibilidad:{estado:'HABILITADO'},disponibleParaPedidos:true}),true);
 assert.equal(effective({disponibilidad:{estado:'DESHABILITADO'},disponibleParaPedidos:true}),false);
});
