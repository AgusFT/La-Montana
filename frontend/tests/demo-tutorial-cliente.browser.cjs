// Requiere una demo local de ensayo con el pedido de muestra, ClamAV y PostgreSQL.
const {test} = require("node:test");
const assert = require("node:assert/strict");
const path = require("node:path");
const {expectTutorialAnchor} = require("./helpers/tutorial-anchor.cjs");
const {restartDialog, tutorialChoice} = require("./helpers/tutorial-restart.cjs");
const {openGuideIndex, orderReview, reviewFields} = require("./helpers/tutorial-review.cjs");
const url = process.env.DEMO_CLIENTE_E2E_URL;

test("el cliente conserva su preparación o confirma un único pedido y puede seguirlo", {skip:!url}, async t => {
  const endpoint = new URL(url);
  const fourPages=process.env.DEMO_CLIENTE_PDF_CUATRO_PAGINAS;
  const fixture=fourPages||path.resolve(__dirname,"fixtures/afiche-dos-paginas.pdf");
  assert.ok(endpoint.protocol === "http:" && ["127.0.0.1","localhost","[::1]"].includes(endpoint.hostname) && endpoint.port,
    "Esta prueba requiere una instancia local descartable.");
  const {chromium, expect} = require("@playwright/test");
  const browser = await chromium.launch({executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || undefined});
  t.after(()=>browser.close());
  const viewport=process.env.DEMO_E2E_MOVIL ? {width:390,height:844} :
    process.env.DEMO_CLIENTE_VENTANA==="compacta" ? {width:1125,height:940} : {width:1440,height:1000};
  const page = await browser.newPage({viewport,reducedMotion:"reduce"});
  const errors = [];
  page.on("pageerror",error=>errors.push(error.message));
  page.on("dialog",async dialog => {
    if(dialog.type()==="beforeunload") await dialog.accept();
    else { errors.push(dialog.message()); await dialog.dismiss(); }
  });
  page.setDefaultTimeout(17000);
  const root = endpoint.origin;
  const admin=await browser.newContext();
  async function mutate(request,method,path,data) {
    const csrf=await (await request.get(root+"/api/auth/csrf")).json();
    const response=await request.fetch(root+path,{method,data,headers:{[csrf.headerName]:csrf.token}});
    assert.ok(response.ok(),path+" HTTP "+response.status());
    return response.json();
  }
  await mutate(admin.request,"POST","/api/demo/sesion",{rol:"ADMINISTRADOR"});
  async function usedQuota(){
    const response=await admin.request.get(root+"/api/demo/cupos");
    assert.ok(response.ok());
    return (await response.json()).cupos.find(c=>c.tipo==="PEDIDOS").usados;
  }
  const card = page.locator(".tutorial-callout:visible");
  const panel = page.locator("#tutorial-panel");
  const choice = tutorialChoice(page,"Tutorial del lado cliente");
  const read = async route => {
    const response = await page.request.get(root + route);
    assert.ok(response.ok(), route + " HTTP " + response.status());
    return response.json();
  };
  const progress = async()=>(await read("/api/demo/tutorial/recorridos")).cliente;
  const orderCount = async()=>(await read("/api/cliente/pedidos")).total;
  const heading = name=>expect(card.getByRole("heading",{name,exact:true})).toBeVisible({timeout:20000});
  async function next(name) {
    await heading(name);
    await card.getByRole("button",{name:"Continuar",exact:true}).click();
  }
  async function start(){
    await page.getByRole("button",{name:"Empezar tutorial",exact:true}).click();
    await choice.getByRole("button",{name:"Iniciar",exact:true}).click();
  }
  async function clickControl(control, options) {
    await expectTutorialAnchor(page,control,options);
    await control.click();
  }
  async function selectControl(control,option) {
    await clickControl(control);
    await control.press("Escape");
    await control.selectOption(option);
  }
  async function reviewPdf() {
    await clickControl(page.getByRole("button",{name:/Revisar archivos/}));
    const dialog=page.locator(".workspace-review-dialog[open]");
    await clickControl(dialog.getByRole("button",{name:/Ver páginas/}).first());
    const pdf=dialog.getByRole("region",{name:"Vista previa del PDF"});
    await expect(pdf).toContainText("1 de "+(fourPages?4:2)+" páginas abiertas",{timeout:45000});
    await restartDialog(page,dialog.locator(".tutorial-callout"),"cliente","escape");
    await expect(pdf).toBeVisible();
    const nextPage=pdf.getByRole("button",{name:"Página siguiente"});
    if(fourPages){
      // La persona puede saltar páginas usando el número del visor. La guía debe recuperar las pendientes.
      await expectTutorialAnchor(page,nextPage);
      await pdf.getByRole("spinbutton",{name:"Página",exact:true}).fill("4");
      const previous=pdf.getByRole("button",{name:"Página anterior"});
      await expect(pdf).toContainText("2 de 4 páginas abiertas",{timeout:45000});
      await clickControl(previous);
      await expect(pdf).toContainText("3 de 4 páginas abiertas",{timeout:45000});
      await clickControl(previous);
    }else{
      await clickControl(nextPage);
    }
    const ack=pdf.getByRole("checkbox");
    await expect(ack).toBeEnabled({timeout:45000});
    await clickControl(ack);
    await expect(ack).toBeChecked();
    await clickControl(pdf.getByRole("button",{name:"Aceptar vista previa"}));
    await expect(dialog.locator('[data-tutorial-pdf="volver"]')).toBeEnabled();
    await clickControl(dialog.getByRole("button",{name:/Volver al resumen/}));
  }
  async function expectOrderSections(){
    for(const section of ["Reconocer el estado y la reserva","Consultar los PDF","Leer los pagos","Consultar correcciones","Revisar la fecha","Leer la entrega"]){
      await heading(section);
      await expect(page.locator(".tutorial-highlight:visible")).toHaveCount(1);
      if(section==="Consultar los PDF"){
        await clickControl(page.getByRole("button",{name:"Ver páginas del pedido"}));
        const pdf=page.getByRole("region",{name:"Vista previa del PDF"});
        await clickControl(pdf.getByRole("button",{name:"Página siguiente"}));
      }
      await next(section);
    }
  }

  await page.goto(root+"/acceso");
  await page.getByRole("button",{name:"Cliente",exact:true}).click();
  await page.waitForURL(/\/cliente$/);
  assert.equal((await progress()).lesson,"","El ensayo debe iniciar con el tutorial del cliente nuevo.");
  const before=await orderCount();
  const slots=await usedQuota();
  assert.ok(before>0,"Se necesita un pedido precargado para consultar.");
  await start();
  await next("Leer el pedido actual");
  await next("Interpretar el resumen");
  await next("Distinguir una cotización de un pedido");
  await heading("Ubicar el comienzo del trabajo");
  await clickControl(page.getByRole("link",{name:"Preparar un pedido",exact:true}));
  await heading("Cargar o retomar el archivo");
  const picker=page.getByRole("button",{name:"Seleccionar PDF",exact:true});
  await expectTutorialAnchor(page,picker);
  const [chooser]=await Promise.all([page.waitForEvent("filechooser"),picker.click()]);
  await chooser.setFiles([]);
  await expect(card.getByRole("button",{name:"Continuar",exact:true})).toBeDisabled();
  assert.equal((await progress()).draft.preparacion,undefined);
  await card.getByRole("button",{name:"Pausar",exact:true}).click();
  const file=page.locator("#client-upload input[type=file]");
  await expect(file).toBeEnabled();
  await file.setInputFiles(fixture);
  await expect(page.getByRole("button",{name:/Configurar PDF 1/})).toBeEnabled({timeout:80000});
  // Simula una instalación con un recorrido completo anterior y una preparación en curso.
  const api="/api/demo/tutorial";
  assert.equal((await read(api)).preparacion,null,"Usar una demo de ensayo sin preparación previa vinculada al flujo completo.");
  await mutate(admin.request,"PUT",api,{accion:"iniciar"});
  await mutate(admin.request,"PUT",api,{accion:"tarifa"});
  const existing=await mutate(page.request,"POST","/api/cliente/preparaciones");
  await mutate(page.request,"PUT",api,{accion:"vincular",preparacion:existing.codigoPublico});
  const otherBefore=await mutate(page.request,"PUT",api,{accion:"borrador",paso:"archivo",campos:{archivoActivo:existing.archivos[0].codigoPublico}});
  await page.goto(root+"/cliente");
  await start();
  await heading("Cargar o retomar el archivo");
  await expect(card.getByRole("button",{name:"Continuar",exact:true})).toBeDisabled();
  await expect(page.getByRole("region",{name:"Preparación en curso"})).toContainText(path.basename(fixture));
  assert.equal((await progress()).draft.preparacion,undefined);
  await clickControl(page.getByRole("button",{name:"Usar esta preparación en el tutorial"}));
  await expectTutorialAnchor(page,page.getByRole("button",{name:/Configurar PDF 1/}));
  const linked=await progress();
  await restartDialog(page,card,"cliente","reiniciar");
  await heading("Leer el pedido actual");
  assert.equal((await progress()).intento,linked.intento+1);
  assert.deepEqual((await progress()).draft,{});
  assert.deepEqual((await read(api)).borradores,otherBefore.borradores);
  assert.equal((await read(api)).preparacion,existing.codigoPublico);
  await next("Leer el pedido actual");
  await next("Interpretar el resumen");
  await next("Distinguir una cotización de un pedido");
  await clickControl(page.getByRole("link",{name:"Preparar un pedido",exact:true}));
  await clickControl(page.getByRole("button",{name:"Usar esta preparación en el tutorial"}));
  await next("Cargar o retomar el archivo");
  assert.equal((await progress()).draft.preparacion,otherBefore.preparacion);
  const print=page.locator("#client-work-options");
  for(const [field,label] of [["Servicio de impresión","Elegir el servicio"],["Tamaño de hoja","Elegir el tamaño"],["Papel y gramaje","Elegir el papel"],["Impresión","Elegir la impresión"]]){
    await heading(label);
    await selectControl(print.getByRole("combobox",{name:field,exact:true}),{index:1});
    await next(label);
  }
  await heading("Ajustar las copias y las caras");
  const copies=print.getByLabel("Copias",{exact:true}), sides=print.getByRole("combobox",{name:"Caras"});
  await clickControl(copies);
  await copies.fill("3");
  await expectTutorialAnchor(page,copies);
  await copies.press("Tab");
  const sidesIndex=(await sides.locator("option").count())>1 ? 1 : 0;
  await selectControl(sides,{index:sidesIndex});
  const extras=print.locator(".workspace-additions-list input[type=checkbox]");
  if(sidesIndex>0 && await extras.count()) await clickControl(extras.first(),{group:true});
  await card.getByRole("button",{name:"Pausar",exact:true}).click();
  await page.reload();
  await expect(card).toHaveCount(0);
  await start();
  await heading("Ajustar las copias y las caras");
  await expect(print.getByLabel("Copias",{exact:true})).toHaveValue("3");
  await next("Ajustar las copias y las caras");
  await heading("Calcular el presupuesto");
  await clickControl(page.locator("#client-budget-action button"));
  await next("Leer el subtotal");
  await heading("Elegir cómo recibirlo");
  const shipping=process.env.DEMO_CLIENTE_ENTREGA==="domicilio";
  const mode=page.getByRole("combobox",{name:"Modalidad",exact:true});
  await selectControl(mode,shipping?"ENVIO_DOMICILIO":"RETIRO_SUCURSAL");
  if(shipping){
    for(const [name,value] of Object.entries({calle:"Calle de ensayo",numero:"109",codigoPostal:"1407",localidad:"CAPITAL FEDERAL",provincia:"CABA",referencias:"Piso 2"})){
      const field=page.locator('#client-delivery input[name="'+name+'"]');
      await clickControl(field);
      await field.fill(value);
      await expectTutorialAnchor(page,field);
      await field.press("Tab");
    }
  }else{
    await selectControl(page.getByLabel("Destino de retiro",{exact:true}),{index:1});
  }
  await next("Elegir cómo recibirlo");
  await heading("Elegir el medio de pago");
  await selectControl(page.getByLabel("Medio de pago",{exact:true}),"EFECTIVO");
  await next("Elegir el medio de pago");
  await heading("Revisar todas las páginas");
  assert.ok(await page.locator("#client-create-order input[type=checkbox]").isDisabled());
  await reviewPdf();
  await next("Revisar todas las páginas");
  await heading("Elegir cómo terminar la práctica");
  const prepared=(await progress()).draft.preparacion;
  await expectTutorialAnchor(page,page.getByRole("button",{name:"Revisar para crear este pedido"}),{group:true});
  await clickControl(page.getByRole("button",{name:"Conservar la preparación y ver el seguimiento"}),{group:true});
  await heading("Consultar la lista");
  assert.equal(await orderCount(),before);
  assert.equal(await usedQuota(),slots);
  const preserved=await read("/api/cliente/preparaciones/"+prepared+"/resumen");
  assert.ok(preserved.preparacion.presupuesto);
  assert.equal(preserved.preparacion.pedido,null);
  await clickControl(page.getByRole("combobox",{name:"Filtrar por estado"}));
  await page.keyboard.press("Escape");
  await next("Consultar la lista");
  await clickControl(page.locator("[data-tutorial-consultar-pedido]").first());
  await heading("Reconocer el estado y la reserva");

  // Volver al punto de decisión usa exactamente el trabajo que acabamos de conservar.
  await openGuideIndex(page);
  await panel.getByRole("button",{name:/Decidir si crear el pedido/}).click();
  await heading("Elegir cómo terminar la práctica");
  const form=page.locator("#client-create-order");
  await clickControl(form.getByRole("button",{name:"Revisar para crear este pedido"}),{group:true});
  await reviewFields(page,orderReview(shipping),{capture:"pedido",
    async inspect(target,selector) {
      if(selector.includes("budget-items")) await expect(target).toContainText(path.basename(fixture));
      if(selector.includes("Medio de pago")) await expect(target).toHaveValue("EFECTIVO");
      if(selector.endsWith(".workspace-contact")) {
        for(const [name,value] of Object.entries({receptor:"Cliente de ensayo",telefono:"1100000000"})){
          const field=target.locator('input[name="'+name+'"]');
          await expectTutorialAnchor(page,field,{group:true});
          await field.fill(value);
        }
      }
    }
  });
  assert.equal(await orderCount(),before);
  assert.equal(await usedQuota(),slots);
  await expect(form.getByRole("checkbox")).toBeEnabled();
  await clickControl(form.getByRole("checkbox"));
  await expect(form.getByRole("checkbox")).toBeChecked();
  await clickControl(form.getByRole("button",{name:/Confirmar \/ Crear pedido/}));
  await expectOrderSections();
  const created=(await progress()).draft.pedido;
  assert.ok(created);
  assert.equal((await read("/api/cliente/preparaciones/"+prepared+"/resumen")).preparacion.pedido,created);
  assert.equal(await orderCount(),before+1);
  assert.equal(await usedQuota(),slots+1);
  await next("Ubicar el manual");
  await heading("Reconocer los límites de la demo");
  await card.getByRole("button",{name:"Terminar tutorial"}).click();
  await heading("Tutorial completado");
  assert.equal((await progress()).complete,true);
  const otherAfter=await read(api);
  assert.equal(otherAfter.preparacion,otherBefore.preparacion);
  assert.deepEqual(otherAfter.borradores,otherBefore.borradores);
  const completed=await progress();
  await page.getByRole("button",{name:"Ir al tutorial",exact:true}).click();
  await restartDialog(page,choice,"cliente","reiniciar",{
    triggerName:"Reiniciar datos",lostResponse:"**/api/demo/tutorial/recorridos",
  });
  await heading("Leer el pedido actual");
  await page.reload();
  await heading("Leer el pedido actual");
  assert.deepEqual(await progress(),{
    lesson:"inicio",task:0,reached:0,complete:false,draft:{},intento:completed.intento+1,
  });
  assert.equal((await read("/api/cliente/preparaciones/"+prepared+"/resumen")).preparacion.pedido,created);
  assert.equal(await usedQuota(),slots+1);
  assert.equal(await orderCount(),before+1);
  assert.equal((await read(api)).preparacion,otherBefore.preparacion);
  assert.deepEqual(errors,[]);
});
