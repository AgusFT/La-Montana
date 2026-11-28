// Requiere una demo local de ensayo con el pedido de muestra, ClamAV y PostgreSQL.
const {test} = require("node:test");
const assert = require("node:assert/strict");
const path = require("node:path");
const url = process.env.DEMO_CLIENTE_E2E_URL;

test("el cliente conserva su preparación o confirma un único pedido y puede seguirlo", {skip:!url}, async t => {
  const endpoint = new URL(url);
  assert.ok(endpoint.protocol === "http:" && ["127.0.0.1","localhost","[::1]"].includes(endpoint.hostname) && endpoint.port,
    "Esta prueba requiere una instancia local descartable.");
  const {chromium, expect} = require("@playwright/test");
  const browser = await chromium.launch({executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || undefined});
  t.after(()=>browser.close());
  const page = await browser.newPage({viewport:{width:1440,height:1000},reducedMotion:"reduce"});
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
    await panel.getByRole("button",{name:/Tutorial del lado cliente/}).click();
  }
  async function reviewPdf() {
    await page.getByRole("button",{name:/Revisar archivos/}).click();
    const dialog=page.locator(".workspace-review-dialog[open]");
    await dialog.getByRole("button",{name:/Ver páginas/}).first().click();
    const pdf=dialog.getByRole("region",{name:"Vista previa del PDF"});
    await expect(pdf).toContainText("1 de 2 páginas abiertas",{timeout:45000});
    await pdf.getByRole("button",{name:"Página siguiente"}).click();
    const ack=pdf.getByRole("checkbox");
    await expect(ack).toBeEnabled({timeout:45000});
    await ack.check();
    await pdf.getByRole("button",{name:"Aceptar vista previa"}).click();
    await expect(dialog.locator('[data-tutorial-pdf="volver"]')).toBeEnabled();
    await dialog.getByRole("button",{name:/Volver al resumen/}).click();
  }
  async function expectOrderSections(){
    for(const section of ["Reconocer el estado y la reserva","Consultar los PDF","Leer los pagos","Consultar correcciones","Revisar la fecha","Leer la entrega"]){
      await heading(section);
      await expect(page.locator(".tutorial-highlight:visible")).toHaveCount(1);
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
  await page.goto(root+"/cliente/cotizaciones/nueva");
  const file=page.locator("#client-upload input[type=file]");
  await expect(file).toBeEnabled();
  await file.setInputFiles(path.resolve(__dirname,"fixtures/afiche-dos-paginas.pdf"));
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
  await next("Leer el pedido actual");
  await next("Interpretar el resumen");
  await next("Distinguir una cotización de un pedido");
  await next("Ubicar el comienzo del trabajo");
  await heading("Cargar o retomar el archivo");
  await expect(card.getByRole("button",{name:"Continuar",exact:true})).toBeDisabled();
  await expect(page.getByRole("region",{name:"Preparación en curso"})).toContainText("afiche-dos-paginas.pdf");
  assert.equal((await progress()).draft.preparacion,undefined);
  await page.getByRole("button",{name:"Usar esta preparación en el tutorial"}).click();
  await next("Cargar o retomar el archivo");
  assert.equal((await progress()).draft.preparacion,otherBefore.preparacion);
  const print=page.locator("#client-work-options");
  for(const [field,label] of [["Servicio de impresión","Elegir el servicio"],["Tamaño de hoja","Elegir el tamaño"],["Papel y gramaje","Elegir el papel"],["Impresión","Elegir la impresión"]]){
    await heading(label);
    await print.getByRole("combobox",{name:field,exact:true}).selectOption({index:1});
    await next(label);
  }
  await heading("Ajustar las copias y las caras");
  await print.getByLabel("Copias",{exact:true}).fill("3");
  await card.getByRole("button",{name:"Pausar",exact:true}).click();
  await page.reload();
  await expect(card).toHaveCount(0);
  await start();
  await heading("Ajustar las copias y las caras");
  await expect(print.getByLabel("Copias",{exact:true})).toHaveValue("3");
  await next("Ajustar las copias y las caras");
  await heading("Calcular el presupuesto");
  await page.locator("#client-budget-action button").click();
  await next("Leer el subtotal");
  await heading("Elegir cómo recibirlo");
  await page.getByLabel("Modalidad",{exact:true}).selectOption("RETIRO_SUCURSAL");
  await page.getByLabel("Destino de retiro",{exact:true}).selectOption({index:1});
  await next("Elegir cómo recibirlo");
  await heading("Elegir el medio de pago");
  await page.getByLabel("Medio de pago",{exact:true}).selectOption("EFECTIVO");
  await next("Elegir el medio de pago");
  await heading("Revisar todas las páginas");
  assert.ok(await page.locator("#client-create-order input[type=checkbox]").isDisabled());
  await reviewPdf();
  await next("Revisar todas las páginas");
  await heading("Elegir cómo terminar la práctica");
  const prepared=(await progress()).draft.preparacion;
  await page.getByRole("button",{name:"Conservar la preparación y ver el seguimiento"}).click();
  await heading("Consultar la lista");
  assert.equal(await orderCount(),before);
  assert.equal(await usedQuota(),slots);
  const preserved=await read("/api/cliente/preparaciones/"+prepared+"/resumen");
  assert.ok(preserved.preparacion.presupuesto);
  assert.equal(preserved.preparacion.pedido,null);
  await next("Consultar la lista");
  await page.locator("[data-tutorial-consultar-pedido]").first().click();
  await heading("Reconocer el estado y la reserva");

  // Volver al punto de decisión usa exactamente el trabajo que acabamos de conservar.
  await panel.getByRole("button",{name:/Decidir si crear el pedido/}).click();
  await heading("Elegir cómo terminar la práctica");
  const form=page.locator("#client-create-order");
  await form.getByRole("button",{name:"Revisar para crear este pedido"}).click();
  await expect(form.getByRole("checkbox")).toBeEnabled();
  await form.getByRole("checkbox").check();
  await form.getByRole("button",{name:/Confirmar \/ Crear pedido/}).click();
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
  assert.deepEqual(errors,[]);
});
