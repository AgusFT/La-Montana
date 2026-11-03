const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs');
const shell=fs.readFileSync('src/components/admin-shell.tsx','utf8');
const page=fs.readFileSync('src/app/administracion/caja/page.tsx','utf8');

test('el menú administrativo muestra un acceso propio a Caja',()=>{
 assert.match(shell,/href="\/administracion\/caja"/);
 assert.match(shell,/>Caja<\/GuidedNavLink>/);
 assert.match(shell,/active\?:[^\n]+"caja"/);
});

test('la caja administrativa exige administrador y permite recorrer todas las sucursales',()=>{
 assert.match(page,/rol!=="ADMIN_ADMIN"/);
 assert.match(page,/getOperationContext\(\)/);
 assert.match(page,/branches\.map/);
 assert.match(page,/<PaymentQueue branch=\{selected\.codigoPublico\}/);
 assert.match(page,/Cobros, transferencias, aplicaciones y devoluciones/);
});
