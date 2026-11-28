const {test} = require("node:test");
const assert = require("node:assert/strict");
const {expectTutorialAnchor} = require("./helpers/tutorial-anchor.cjs");
const {restartDialog} = require("./helpers/tutorial-restart.cjs");
const endpoint = process.env.DEMO_ADMIN_E2E_URL;

// Este recorrido publica una tarifa al final. Usar una demo local descartable.
test("el administrador sigue cada control, retoma el borrador y decide si publica", {skip:!endpoint,timeout:360000}, async t => {
  const url = new URL(endpoint);
  assert.ok(url.protocol==="http:" && ["127.0.0.1","localhost","[::1]"].includes(url.hostname) && url.port);
  const {chromium,expect} = require("@playwright/test");
  const browser = await chromium.launch({executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE||undefined});
  t.after(()=>browser.close());
  const desktop={width:1125,height:840}, mobile={width:390,height:844};
  const viewport=process.env.DEMO_ADMIN_E2E_MOBILE==="1"?mobile:desktop;
  const page = await browser.newPage({viewport,reducedMotion:"reduce"});
  page.setDefaultTimeout(12000);
  const errors=[]; page.on("pageerror",error=>errors.push(error.message));
  let publish=false;
  page.on("dialog",async dialog=>{
    if(dialog.type()==="confirm" && dialog.message().includes("¿Confirmás publicar")){
      if(publish)await dialog.accept(); else await dialog.dismiss();
    }else if(dialog.type()==="beforeunload"||dialog.message().includes("cambios sin guardar"))await dialog.accept(); else await dialog.dismiss();
  });
  const root=url.origin;
  async function read(path){const response=await page.request.get(root+path);assert.equal(response.status(),200,path);return response.json();}
  const card=page.locator(".tutorial-callout:visible");
  const field=selector=>page.locator(selector).first();
  async function treeButton(button){
    const index=page.locator(".tutorial-index-toggle");
    await index.waitFor({state:"attached"});
    if(await index.isVisible()&&!await button.isVisible())await index.click();
    await button.click();
  }
  async function choose(){await page.getByRole("button",{name:"Empezar tutorial",exact:true}).click();await page.getByRole("button",{name:/Tutorial del lado administrador/}).click();}
  async function heading(text){await expect(card.getByRole("heading",{name:text,exact:true})).toBeVisible();}
  async function anchor(selector){
    const el=field(selector);
    try { await expectTutorialAnchor(page,el); }
    catch(e){
      if(process.env.DEMO_ADMIN_E2E_CAPTURES)await picture("fallo");
      const detail=await el.evaluate(el=>({
        rect:el.getBoundingClientRect().toJSON(),viewport:[innerWidth,innerHeight],
        guide:document.querySelector(".tutorial-callout")?.innerText, described:document.querySelector('[aria-describedby~=tutorial-indicacion]')?.outerHTML.slice(0,170),
        halo:document.querySelector(".tutorial-highlight")?.getBoundingClientRect().toJSON(),arrow:document.querySelector(".tutorial-arrow")?.outerHTML.slice(0,250),
        phase:document.querySelector(".configuration-layout")?.getAttribute("data-fase"),
      }));
      console.error(selector,detail); throw e;
    }
    return el;
  }
  async function tap(selector){await (await anchor(selector)).click();}
  async function next(title,selector){await heading(title);await anchor(selector);await card.getByRole("button",{name:/^(Continuar|Terminar tutorial)$/}).click();}
  async function selectFirst(selector){const control=await anchor(selector);const option=control.locator('option:not([value=""])').first();await control.selectOption(await option.getAttribute("value"));}
  async function phaseTask(title,number) {
    await heading(title);
    const lay=field(".configuration-layout");
    if(await lay.getAttribute("data-fase")!==String(number))await tap(`#config-tour-phases nav button:nth-child(${number})`);
    await expect(lay).toHaveAttribute("data-fase",String(number));
  }
  async function picture(label){
    if(process.env.DEMO_ADMIN_E2E_CAPTURES)await page.screenshot({path:require("node:path").join(process.env.DEMO_ADMIN_E2E_CAPTURES,label+".png")});
  }
  await page.goto(root+"/acceso");await page.getByRole("button",{name:"Administrador",exact:true}).click();
  await page.waitForURL(/\/administracion$/);
  const catalog=()=>read("/api/admin/catalogo"), config=()=>read("/api/admin/configuracion");
  const initial=(await catalog()).actual, operational=(await config()).activa.configuracion.codigoPublico;
  await page.goto(root+"/administracion/configuracion");
  if(!(await config()).borrador)await page.getByRole("button",{name:"Crear borrador",exact:true}).click();
  await expect.poll(async()=>(await config()).borrador?.codigoPublico).toBeTruthy();
  const prior=(await config()).borrador.codigoPublico;
  await page.goto(root+"/administracion"); await choose();
  await treeButton(page.locator(".tutorial-branch",{hasText:"Interpretar el tablero"}));
  await next("Leer un indicador",'.dashboard-metric strong[aria-label^="Pendientes de revisión"]');
  await heading("Comparar las bandejas");
  const rolesPath="/api/demo/tutorial/recorridos";
  const active=await read(rolesPath);
  await restartDialog(page,card,"administrador","cancelar");
  assert.deepEqual(await read(rolesPath),active);
  await restartDialog(page,card,"administrador","reiniciar");
  await heading("Leer un indicador");
  await page.reload();
  await heading("Leer un indicador");
  let progress=await read(rolesPath);
  assert.equal(progress.administrador.intento,active.administrador.intento+1);
  assert.deepEqual(progress.cliente,active.cliente);
  await next("Leer un indicador",'.dashboard-metric strong[aria-label^="Pendientes de revisión"]');
  await next("Comparar las bandejas",'.dashboard-queue[aria-label="Pendientes de revisión"] header h2');
  await heading("Elegir el servicio");await expect(field("#tutorial-tarifa-servicio")).toBeVisible();
  if(await field("#tutorial-tarifa-restaurar").isVisible())await tap("#tutorial-tarifa-restaurar");
  await selectFirst("#tutorial-tarifa-servicio");await next("Elegir el servicio","#tutorial-tarifa-servicio");
  await selectFirst("#tutorial-tarifa-papel");await next("Elegir papel y color","#tutorial-tarifa-papel");
  const input=await anchor("#tutorial-tarifa-precio input"), price=String(Math.ceil(Number((await input.inputValue()).replace(",",".")))+3);
  await input.fill(price);
  await card.getByRole("button",{name:"Pausar",exact:true}).click();await choose();
  await expect(input).toHaveValue(price);await next("Cambiar el precio por hoja","#tutorial-tarifa-precio input");
  await tap("#tutorial-tarifa-aplicar");await next("Aplicar al borrador","#tutorial-tarifa-aplicar");
  await expect(field("#tutorial-tarifa-resultado")).toContainText("tres hojas");
  assert.equal((await catalog()).actual.codigoPublico,initial.codigoPublico);await next("Comparar y decidir","#tutorial-tarifa-publicar");
  await next("Consultar papeles","#catalog-papers-tab");await tap("#catalog-services-tab");await next("Consultar servicios","#catalog-services-tab");
  await tap("#historial-comercial tbody tr:first-child button");await next("Comparar versiones",".catalog-history-detail h3");
  await phaseTask("Reconocer la configuración vigente",1);await next("Reconocer la configuración vigente","#config-tour-current h3");
  await tap("#config-tour-resume button");
  await phaseTask("Revisar el modelo de aprobación",2);
  assert.equal((await config()).borrador.codigoPublico,prior);
  // Con datos precargados se puede conservar directamente la selección. Probar
  // también el estado que aparece cuando se elige otro modo y falta su condición.
  const manual=field('input[name=modelo][value=MANUAL]'), conditional=field('input[name=modelo][value=CONDICIONAL]');
  const oldManual=await manual.isChecked(),oldConditional=await conditional.isChecked();
  const oldCriterion=await field("#tutorial-criterios input:checked").getAttribute("value",{timeout:300}).catch(()=>null);
  if(!oldManual&&!oldConditional){
    for(const [size,label] of [[desktop,"escritorio"],[mobile,"movil"]]){
      await page.setViewportSize(size);
      await expectTutorialAnchor(page,manual,{group:true});
      await expectTutorialAnchor(page,conditional,{group:true});
      await picture("modelos-disponibles-"+label);
    }
  }else{
    const modelAction=await field('[data-modelo-guardado="true"]').count()?"#tutorial-modelo-continuar":"#tutorial-modelo-guardar";
    await page.setViewportSize(desktop);await anchor(modelAction);await picture("modelo-escritorio");
    await page.setViewportSize(mobile);await anchor(modelAction);await picture("modelo-movil");
    if(oldConditional)await manual.check();
  }
  await conditional.check();
  const criterion=page.locator("#tutorial-criterios input").nth(1);
  for(const [size,label] of [[desktop,"escritorio"],[mobile,"movil"]]){
    await page.setViewportSize(size);
    await expectTutorialAnchor(page,criterion,{group:true});await picture("criterios-"+label);
  }
  await criterion.check();
  async function saveSurface(){
    const save=await anchor("#tutorial-modelo-guardar");
    await expect(page.locator(".guided-tour-help:visible")).toHaveCount(0);
    await expect.poll(()=>save.evaluate(el=>{
      const r=el.getBoundingClientRect();
      return [r.left+8,r.left+r.width/2,r.right-8].every(x=>
        [r.top+7,r.top+r.height/2,r.bottom-7].every(y=>el.contains(document.elementFromPoint(x,y))));
    })).toBe(true);
  }
  await saveSurface();await picture("guardar-seleccion-movil");
  await page.setViewportSize(desktop);await saveSurface();await picture("guardar-seleccion-escritorio");
  await card.getByRole("button",{name:"Pausar",exact:true}).click();
  await expect(page.locator(".guided-tour-help:visible")).toHaveCount(1);
  await choose();await heading("Revisar el modelo de aprobación");await saveSurface();

  if(oldConditional&&oldCriterion)await field(`#tutorial-criterios input[value="${oldCriterion}"]`).check();
  else await manual.check();
  await page.setViewportSize(viewport);
  if(await field('[data-modelo-guardado="false"]').count())await tap("#tutorial-modelo-guardar");
  await anchor("#tutorial-modelo-continuar");await picture("modelo-guardado");
  await tap("#tutorial-modelo-continuar");await phaseTask("Revisar pagos y seña",3);
  if(await field('[data-pagos-guardados="false"]').count()){
    if(!await field("#tutorial-medios-pago input:checked").count()){
      const transfer=field("#tutorial-medios-pago input");
      await expectTutorialAnchor(page,transfer,{group:true});await transfer.check();
    }
    if(await field("#payment-instructions:invalid").count())await (await anchor("#payment-instructions")).fill("Transferencia ficticia para el ejemplo.");
    if(await field("#quote-duration:invalid").count())await (await anchor("#quote-duration")).fill("60");
    if(await field("#require-deposit:invalid").count())await (await anchor("#require-deposit")).selectOption("NO");
    const window="#config-tour-archives-policy input[type=number]:invalid";
    if(await field(window).count())await (await anchor(window)).fill("1");
    await tap("#tutorial-pagos-guardar");
  }
  await tap("#tutorial-pagos-continuar");await phaseTask("Comprobar recursos",4);
  if(await field('[data-asignacion-guardada="false"]').count())await tap("#tutorial-recursos-guardar");
  await tap("#tutorial-recursos-continuar");await phaseTask("Comprobar horarios y entrega",5);
  const matrix="#config-tour-matrix input[type=checkbox]";if(!(await field(matrix).isChecked()))await tap(matrix);
  if(!await field("#tutorial-entrega-continuar").isEnabled())await tap("#guardar-horarios");
  await tap("#tutorial-entrega-continuar");await phaseTask("Abrir una simulación",6);
  await tap(".optional-simulation summary");await heading("Simular y leer el resultado");
  const sim=field(".review-simulator");
  async function fillMissing(selector,value) {
    const input=field(selector);
    if(await input.evaluate(control=>!control.validity.valid))await (await anchor(selector)).fill(value);
  }
  if(!await field("#review-combination").inputValue())await selectFirst("#review-combination");
  await fillMissing("#tutorial-simulacion-paginas","3");await fillMissing("#tutorial-simulacion-copias","2");
  if(!await field("#review-sides").inputValue())await (await anchor("#review-sides")).selectOption("NO");
  if(!await field("#review-mode").inputValue())await (await anchor("#review-mode")).selectOption("RETIRO_SUCURSAL");
  await fillMissing("#tutorial-simulacion-recepcion",new Date().toISOString().slice(0,16));
  await anchor("#tutorial-simulacion-calcular");
  await card.getByRole("button",{name:"Pausar",exact:true}).click();await expect(page.getByRole("button",{name:"Empezar tutorial",exact:true})).toBeVisible();
  await page.goto(root+"/administracion");await choose();await heading("Simular y leer el resultado");await expect(field(".configuration-layout")).toHaveAttribute("data-fase","6");
  if(!await field(".optional-simulation").evaluate(el=>el.open))await tap(".optional-simulation summary");
  await expect(sim.getByLabel("Páginas del PDF")).toHaveValue("3");await expect(sim.getByLabel("Copias del ejemplo")).toHaveValue("2");
  await tap("#tutorial-simulacion-calcular");await next("Simular y leer el resultado",".review-result h3");
  await next("Decidir sobre la activación","#config-tour-review-actions button:last-child");
  await tap("#config-tour-phases nav button:nth-child(8)");await next("Abrir el historial de versiones",".history-table tbody tr:first-child td:first-child");
  await heading("Reconocer la identidad pública");
  const site='.website-configuration', fase=()=>field(site).getAttribute("data-fase-web");
  if(await fase()!=="1")await tap(site+" nav button:first-child");
  const nombre='input[aria-label="Nombre público de la imprenta"]',titulo='input[aria-label="Título de portada"]';
  await next("Reconocer la identidad pública",nombre);
  for(const [name,number,dest] of [
    ["Ubicar las imágenes de identidad",3,'select[aria-label="Logotipo"]'],
    ["Preparar una tarjeta del catálogo",4,'#tutorial-web-agregar-ficha'],
    ["Ubicar la vista previa",5,'.website-configuration .resource-toolbar h3'],
    ["Distinguir borrador y publicación local",6,'.web-publish-confirm'],
  ]){
    await heading(name);
    const nav=site+` nav button:nth-child(${number})`;
    while(Number(await fase())!==number){
      if(await field(nav).isEnabled())await tap(nav);
      else{
        if((await fase())==="1"){
          for(const [selector,value] of [[nombre,"Imprenta de ejemplo"],[titulo,"Impresiones de muestra"]]){
            if(!await field(selector).inputValue())await (await anchor(selector)).fill(value);
          }
        }
        const old=await fase();
        await tap("#tutorial-web-continuar");
        await expect(field(site)).not.toHaveAttribute("data-fase-web",old);
      }
    }
    await next(name,dest);
  }
  for(const [title,selector] of [
    ["Revisar dirección y horarios",".branch-list .branch-card h3"],
    ["Filtrar la bandeja",'select[aria-label="Filtrar por estado"]'],
  ])await next(title,selector);
  const total=".client-table tbody tr:first-child td:nth-child(6)";
  await next("Interpretar el total y el saldo",await field(total).isVisible()?total:'select[aria-label="Filtrar por estado"]');
  const quota='.demo-quota-cards button[data-recurso="PEDIDOS"]';
  await tap(quota);await next("Consultar el saldo de recursos",quota);
  for(const [title,selector] of [
    ["Reconocer las funciones bloqueadas",".demo-locked-title"],
    ["Ubicar la asistencia",'.admin-sidebar a[href="/administracion/soporte"]'],
    ["Encontrar el manual",'.admin-sidebar a[href="/manual-de-usuario"]'],
    ["Identificar la seguridad de la cuenta",'.admin-sidebar a[href="/cuenta/seguridad"]'],
  ])await next(title,selector);
  await heading("Tutorial completado");assert.equal((await config()).activa.configuracion.codigoPublico,operational);
  assert.equal((await config()).borrador.codigoPublico,prior);assert.equal((await catalog()).actual.codigoPublico,initial.codigoPublico);
  await treeButton(page.getByRole("button",{name:/Preparar una tarifa/}));
  await treeButton(page.getByRole("button",{name:"Comparar y decidir"}));
  await tap("#tutorial-tarifa-publicar");
  await (await anchor("#catalog-reason")).fill("Verificación de la publicación opcional en base descartable");
  const save="#tariff-tour-publish button";await tap(save);
  assert.equal((await catalog()).actual.codigoPublico,initial.codigoPublico);publish=true;await tap(save);
  await anchor('[data-precio-publicado] .save-notice:not(.is-pending) strong');
  await expect.poll(async()=>(await catalog()).actual.codigoPublico).not.toBe(initial.codigoPublico);
  assert.ok((await catalog()).actual.tarifas.some(row=>Number(row.precio)===Number(price)));
  const published=(await catalog()).actual.codigoPublico;
  const completed=await read(rolesPath);
  assert.equal(completed.administrador.complete,true);
  await restartDialog(page,card,"administrador","reiniciar");
  await heading("Leer un indicador");
  progress=await read(rolesPath);
  assert.deepEqual(progress.administrador,{
    lesson:"tablero",task:0,reached:0,complete:false,draft:{},intento:completed.administrador.intento+1,
  });
  assert.deepEqual(progress.cliente,completed.cliente);
  assert.equal((await catalog()).actual.codigoPublico,published);
  assert.equal((await config()).borrador.codigoPublico,prior);
  assert.deepEqual(errors,[]);
});
