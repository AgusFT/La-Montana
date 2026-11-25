// Ejecutar con Node y Playwright disponibles en el entorno de pruebas; no son dependencias del instalador.
const {chromium}=require('playwright');
const {createServer}=require('node:http');
const fs=require('node:fs');const path=require('node:path');const assert=require('node:assert/strict');
const web=path.resolve(__dirname,'../../main/resources/web');
let instance={existe:false},calls=[],badDocker=false,failInstall=false,slow=false,job=0,failCheck=false,selectorCancel=false,slowSearch=false;
let folderState={estado:'LIBRE',tipo:''};const chosen='/tmp/La Montaña elegida';
const server=createServer(async(req,res)=>{
 const url=new URL(req.url,'http://localhost');let chunks=[];for await(const c of req)chunks.push(c);const body=Object.fromEntries(new URLSearchParams(Buffer.concat(chunks).toString()));
 if(url.pathname.startsWith('/api/')){
  calls.push([url.pathname,body]);let data={};
  if(url.pathname==='/api/estado')data={version:'0.2.2',versionInstalador:'0.2.7',sistema:'Linux',directorioRecomendado:'/home/prueba/LaMontana'};
  if(url.pathname==='/api/dependencias'&&failCheck){res.writeHead(500,{'Content-Type':'application/json'});res.end(JSON.stringify({error:'No se pudo verificar el equipo.'}));return;}
  if(url.pathname==='/api/dependencias')data={dependencias:[['sistema','COMPATIBLE'],['docker',badDocker?'DETENIDO_O_INACCESIBLE':'DISPONIBLE'],['compose','DISPONIBLE'],['paquete','VERIFICADO']].map(([id,estado])=>({id,estado,detalle:'Comprobación de laboratorio.'}))};
  if(url.pathname==='/api/carpetas/examinar')folderState={estado:selectorCancel?'CANCELADO':'COMPLETADO',tipo:'SELECTOR',directorio:selectorCancel?'':chosen,mensaje:selectorCancel?'Selección cancelada.':'Carpeta elegida.'};
  if(url.pathname==='/api/carpetas/buscar')folderState={estado:slowSearch?'EJECUTANDO':'COMPLETADO',tipo:'BUSQUEDA',mensaje:'Búsqueda terminada.',entradas:12,revisadas:[body.raiz||'/home/prueba'],omisiones:{},parcial:false,resultados:[{directorio:chosen,tipo:'IDENTIFICADA',elegible:true,version:'0.2.2',modo:'DESARROLLO',detalle:'Estado guardado: LISTA.'},{directorio:'/tmp/script-anterior',tipo:'SCRIPT_ANTERIOR',elegible:false,detalle:'No se adopta automáticamente.'}]};
  if(url.pathname==='/api/carpetas/cancelar')folderState={...folderState,estado:'CANCELADO',parcial:true,mensaje:'Búsqueda cancelada.'};
  if(url.pathname==='/api/carpetas/estado')data=folderState;
  if(url.pathname==='/api/seleccionar')data=instance;
  if(url.pathname==='/api/instalacion')data=instance;
  if(url.pathname==='/api/instalar'){
   assert.equal(body.modo,'DESARROLLO');assert.equal(body['propietario.correo'],'propietario@example.invalid');if(body.crearSoporte==='si')assert.equal(body['soporte.contrasena'],'clave-prueba-segura');else assert.equal(Object.keys(body).some(k=>k.startsWith('soporte.')),false);
   job=0;if(!failInstall)instance={existe:true,modo:'DESARROLLO',version:'0.2.2',estado:'LISTA',puerto:'3000',bind:'127.0.0.1',disponible:true,servicios:'cinco servicios saludables',url:'http://127.0.0.1:3000',urlMailpit:'http://127.0.0.1:3025'};
  }
  if(url.pathname==='/api/trabajo')data={estado:slow&&job++===0?'EJECUTANDO':failInstall?'ERROR':'COMPLETADO',mensaje:failInstall?'Fallo controlado de laboratorio.':'Operación de laboratorio.'};
  if(url.pathname==='/api/publico/confirmar')instance.publicoConfirmadoEn='prueba';
  if(url.pathname==='/api/correo/confirmar')instance.smtpConfirmadoEn='prueba';
  if(url.pathname==='/api/cerrar')data={mensaje:'Asistente cerrado.'};
  res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify(data));return;
 }
 const name=url.pathname==='/'?'index.html':url.pathname.slice(1);if(!['index.html','app.js','style.css','yg-soluciones.svg'].includes(name)){res.writeHead(404).end();return;}
 res.setHeader('Content-Type',name.endsWith('.js')?'text/javascript':name.endsWith('.css')?'text/css':name.endsWith('.svg')?'image/svg+xml':'text/html');res.end(fs.readFileSync(path.join(web,name)));
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const browser=await chromium.launch({executablePath:process.env.CHROME_PATH||'/usr/bin/google-chrome',headless:true,args:['--disable-dev-shm-usage']});
 const page=await browser.newPage({viewport:{width:1366,height:1000}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
 const url=`http://127.0.0.1:${server.address().port}/#prueba-local`;
 const active=async id=>{await page.locator(`[data-paso="${id}"]`).waitFor({state:'visible'});assert.equal(await page.locator('[data-paso]:visible').count(),1);};
 const next=()=>page.getByRole('button',{name:'Siguiente →',exact:true}).click();
 const open=async()=>{await page.goto("about:blank");await page.goto(url);await page.getByText('Instalador 0.2.7',{exact:false}).waitFor();};
 const folder=async()=>{await page.locator('#directorio').fill('/tmp/lamontana-prueba-ui');await page.locator('#seleccionar').click();await page.getByText('Carpeta examinada.',{exact:false}).waitFor();};
 const account=async id=>{for(const [field,value] of Object.entries({nombre:'Prueba',apellido:'Local',correo:`${id}@example.invalid`,contrasena:'clave-prueba-segura'}))await page.locator(`[name="${id}.${field}"]`).fill(value);};
 await open();await active('equipo');await next();await active('equipo');assert.match(await page.locator('#error-paso').innerText(),/Verificá/);
 const help=page.getByRole('button',{name:'Más información: Dependencias',exact:true});await help.hover();await page.locator('#ayuda-ampliada').waitFor({state:'visible'});await page.keyboard.press('Escape');assert.equal(await page.locator('#ayuda-ampliada').isVisible(),false);await help.focus();await page.locator('#ayuda-ampliada').waitFor({state:'visible'});await page.keyboard.press('Escape');
 badDocker=true;await page.locator('#verificar').click();await page.getByText('Hay componentes pendientes.',{exact:false}).first().waitFor();await next();await active('equipo');badDocker=false;
 await page.locator('#verificar').click();await page.getByText('Equipo verificado.',{exact:false}).waitFor();
 assert.equal(await page.locator('.resolver.verificada').count(),1);await page.locator('.resolver summary').click();
 assert.equal(await page.locator('#dependencias-aviso').isVisible(),true);assert.equal(await page.locator('#resolver').isEnabled(),true);
 failCheck=true;await page.locator('#verificar').click();await page.getByText('No se pudo verificar el equipo.',{exact:true}).first().waitFor();
 assert.equal(await page.locator('.resolver.verificada').count(),0);assert.equal(await page.locator('#dependencias-aviso').isVisible(),false);
 failCheck=false;await page.locator('#verificar').click();await page.getByText('Equipo verificado.',{exact:false}).waitFor();
 await next();await active('destino');assert.equal(await page.locator('#directorio').inputValue(),'/home/prueba/LaMontana');
 const antes=calls.filter(([p])=>p==='/api/seleccionar').length;
 await page.locator('#examinar-carpeta').click();await page.getByText('Carpeta elegida. Pulsá',{exact:false}).waitFor();assert.equal(await page.locator('#directorio').inputValue(),chosen);
 assert.equal(calls.filter(([p])=>p==='/api/seleccionar').length,antes);await next();assert.match(await page.locator('#error-paso').innerText(),/examiná/);
 selectorCancel=true;await page.locator('#examinar-carpeta').click();await page.getByText('Selección cancelada.',{exact:true}).waitFor();assert.equal(await page.locator('#directorio').inputValue(),chosen);selectorCancel=false;
 await folder();await next();await active('opciones');await next();await active('propietario');await next();await active('propietario');await account('propietario');await next();await active('soporte');assert.equal(await page.locator('#crear-soporte').isChecked(),false);
 await next();await active('revision');assert.match(await page.locator('#revision-datos').innerText(),/No se creará/);
 await page.locator('#anterior').click();await page.locator('#crear-soporte').check();await next();await active('soporte');await account('soporte');
 await page.locator('#crear-soporte').uncheck();assert.equal(await page.locator('[name="soporte.contrasena"]').inputValue(),'');assert.equal(await page.locator('[name="soporte.correo"]').isDisabled(),true);
 await page.locator('#crear-soporte').check();await account('soporte');await next();await active('revision');
 assert.equal((await page.locator('#revision-datos').innerText()).includes('clave-prueba-segura'),false);
 await page.getByRole('button',{name:/3\. Modo y acceso/}).click();await active('opciones');await page.locator('#puerto').fill('3001');assert.equal(await page.getByRole('button',{name:/6\. Revisar y ejecutar/}).isEnabled(),false);
 await next();await next();await next();await active('revision');slow=true;await page.locator('#ejecutar').click();assert.equal(await page.locator('#anterior').isEnabled(),false);await page.getByText('Operación completada. Podés continuar',{exact:false}).waitFor();slow=false;
 assert.equal(calls.filter(([p])=>p==='/api/instalar').length,1);assert.equal(await page.locator('[name="propietario.contrasena"]').inputValue(),'');await next();await active('resultado');
 await page.locator('#anterior').click();await active('revision');assert.equal(await page.locator('#ejecutar').isEnabled(),false);assert.equal(calls.filter(([p])=>p==='/api/instalar').length,1);
 // Reapertura desde la entrada autenticada: nueva detección, sin persistir claves ni operaciones.
 await open();await active('equipo');assert.equal(await page.evaluate(()=>localStorage.length+sessionStorage.length),0);
 await page.locator('#verificar').click();await page.getByText('Equipo verificado.',{exact:false}).waitFor();await next();await folder();await next();await active('operacion');await page.locator('#operacion-elegida').selectOption('actualizar');await next();await active('operacion');assert.match(await page.locator('#error-paso').innerText(),/posterior/);
 await page.locator('#operacion-elegida').selectOption('reparar');await next();await active('respaldo');await next();await active('respaldo');await page.locator('[name=respaldo]').fill('/tmp/copias-ui');await next();await active('revision');await page.locator('#ejecutar').click();await page.getByText('Operación completada. Podés continuar',{exact:false}).waitFor();assert.equal(calls.filter(([p])=>p==='/api/mantener').length,1);
 // Volver a un destino nuevo rehabilita formularios y descarta validaciones previas.
 await page.getByRole('button',{name:/2\. Elegir instalación/}).click();await page.locator('#directorio').fill('/tmp/otra-instalacion-ui');instance={existe:false};await page.locator('#seleccionar').click();await page.getByText('Carpeta examinada.',{exact:false}).waitFor();await next();await active('opciones');assert.equal(await page.locator('#modo').isEnabled(),true);
 // Interrupción: conservar diagnóstico, pedir claves nuevamente y no repetir automáticamente.
 await next();await account('propietario');await next();await page.locator('#crear-soporte').uncheck();await next();failInstall=true;await page.locator('#ejecutar').click();await page.getByText('Fallo controlado de laboratorio.',{exact:true}).first().waitFor();await active('propietario');assert.equal(await page.locator('[name="propietario.contrasena"]').inputValue(),'');failInstall=false;
 await account('propietario');await next();await next();await active('revision');await page.locator('#ejecutar').click();await page.getByText('Operación completada. Podés continuar',{exact:false}).waitFor();
 assert.equal(calls.filter(([p])=>p==='/api/instalar').at(-1)[1].crearSoporte,undefined);
 // Descubrimiento y ampliación: elegir invalida el destino, sin examinar ni instalar automáticamente.
 instance={existe:false};await open();await page.locator('#verificar').click();await page.getByText('Equipo verificado.',{exact:false}).waitFor();await next();await folder();
 await page.locator('#buscar-instalaciones').click();await page.getByRole('button',{name:'Usar esta carpeta'}).waitFor();
 assert.equal(await page.getByRole('button',{name:'Usar esta carpeta'}).count(),1);
 const examenes=calls.filter(([p])=>p==='/api/seleccionar').length;
 await page.getByRole('button',{name:'Usar esta carpeta'}).click();assert.equal(await page.locator('#directorio').inputValue(),chosen);
 assert.equal(calls.filter(([p])=>p==='/api/seleccionar').length,examenes);
 await next();await active('destino');
 await Promise.all([page.waitForResponse(r=>r.url().endsWith('/api/carpetas/buscar')),page.locator('#ampliar-busqueda').click()]);await page.getByRole('button',{name:'Usar esta carpeta'}).waitFor();
 assert.equal(calls.filter(([p])=>p==='/api/carpetas/buscar').at(-1)[1].raiz,chosen);
 slowSearch=true;await page.locator('#buscar-instalaciones').click();await page.locator('#cancelar-carpetas').waitFor({state:'visible'});assert.equal(await page.locator('#cancelar-carpetas').isEnabled(),true);
 await page.locator('#cancelar-carpetas').click();await page.locator('#cancelar-carpetas').waitFor({state:'hidden'});assert.match(await page.locator('#busqueda-estado').innerText(),/parcial/);slowSearch=false;
 await page.screenshot({path:process.env.FOLDER_SCREENSHOT||'/tmp/lamontana-v4-carpetas.png',fullPage:true});
 // Standard: solo navegación contra esta API simulada, sin dominio/SMTP real.
 instance={existe:true,modo:'STANDARD',version:'0.2.2',estado:'PUBLICACION_PENDIENTE',bind:'127.0.0.1',disponible:true,publicacionConfigurada:true,arranqueVerificado:'true'};
 await open();await page.locator('#verificar').click();await page.getByText('Equipo verificado.',{exact:false}).waitFor();await next();await folder();await next();await page.locator('#operacion-elegida').selectOption('acceso');await next();await active('red');await page.locator('[data-sistema=red]').click();await page.getByText('Operación de sistema completada.',{exact:false}).waitFor();await next();await active('standard-config');await next();await active('standard-arranque');await next();await active('standard-publico');await next();await active('standard-publico');await page.locator('#confirmar-publico input').fill('codigo-simulado');await page.locator('#confirmar-publico button').click();await page.getByText('Operación de laboratorio.',{exact:true}).waitFor();await next();await active('standard-correo');await page.locator('#confirmar-correo input').fill('codigo-simulado');await page.locator('#confirmar-correo button').click();await page.getByText('Operación de laboratorio.',{exact:true}).waitFor();await next();await active('resultado');
 instance={existe:false};await open();await active('equipo');await page.screenshot({path:process.env.UI_SCREENSHOT||'/tmp/lamontana-v3-escritorio.png',fullPage:true});
 await page.setViewportSize({width:683,height:500});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);await page.screenshot({path:'/tmp/lamontana-v3-zoom200.png',fullPage:true});
 await page.setViewportSize({width:390,height:844});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);await help.click();await page.locator('#ayuda-ampliada').waitFor({state:'visible'});const box=await page.locator('#ayuda-ampliada').boundingBox();assert.ok(box.x>=0&&box.x+box.width<=390);await page.screenshot({path:'/tmp/lamontana-v3-movil.png',fullPage:true});
 assert.deepEqual(errors,[]);await browser.close();server.close();console.log('OK: soporte opcional, validación condicional, envío sin soporte, navegación, dependencias verdes con controles, carpeta recomendada, selector/cancelación, búsqueda/ampliación, mantenimiento, errores, secretos, ayudas y diseño adaptable. Datos simulados; no se ejecutó Standard.');
})().catch(e=>{console.error(e);server.close();process.exit(1);});
