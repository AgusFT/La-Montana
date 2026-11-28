const {test} = require("node:test");
const assert = require("node:assert/strict");
const endpoint = process.env.DEMO_ADMIN_E2E_URL;

// Este recorrido publica una tarifa al final. Usar una demo local descartable.
test("el administrador retoma el borrador, simula y decide si publica", {skip:!endpoint,timeout:360000}, async t => {
  const url = new URL(endpoint);
  assert.ok(url.protocol==="http:" && ["127.0.0.1","localhost","[::1]"].includes(url.hostname) && url.port);
  const {chromium,expect} = require("@playwright/test");
  const browser = await chromium.launch({executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE||undefined});
  t.after(()=>browser.close());
  const page = await browser.newPage({viewport:{width:1440,height:960},reducedMotion:"reduce"});
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
  const card=()=>page.locator(".tutorial-callout:visible");
  async function choose(){await page.getByRole("button",{name:"Empezar tutorial",exact:true}).click();await page.getByRole("button",{name:/Tutorial del lado administrador/}).click();}
  async function heading(text){await expect(card().getByRole("heading",{name:text,exact:true})).toBeVisible();}
  async function next(title){await heading(title);await card().getByRole("button",{name:/^(Continuar|Terminar tutorial)$/}).click();}
  async function action(element){
    // En los formularios largos la guía ofrece minimizar la explicación para operar varios campos.
    await card().getByRole("button",{name:"Minimizar",exact:true}).click();
    await element.click();
    const restore=card().getByRole("button",{name:/Mostrar explicación|Restaurar|Maximizar/});
    if(await restore.isVisible())await restore.click();
  }
  async function selectFirst(control){const options=await control.locator('option:not([value=""])').evaluateAll(items=>items.map(o=>o.value));assert.ok(options.length);await control.selectOption(options[0]);}
  await page.goto(root+"/acceso");await page.getByRole("button",{name:"Administrador",exact:true}).click();
  await page.waitForURL(/\/administracion$/);
  const catalog=()=>read("/api/admin/catalogo"), config=()=>read("/api/admin/configuracion");
  const initial=(await catalog()).actual, operational=(await config()).activa.configuracion.codigoPublico;
  // Dejar un borrador anterior para comprobar que la práctica usa el mismo.
  await page.goto(root+"/administracion/configuracion");
  if(!(await config()).borrador)await page.getByRole("button",{name:"Crear borrador",exact:true}).click();
  await expect.poll(async()=>(await config()).borrador?.codigoPublico).toBeTruthy();
  const prior=(await config()).borrador.codigoPublico;
  await page.goto(root+"/administracion"); await choose();
  await page.locator(".tutorial-branch",{hasText:"Interpretar el tablero"}).click();
  await next("Leer un indicador"); await next("Comparar las bandejas");
  await heading("Elegir el servicio");
  const stale=page.locator("#tutorial-tarifa-restaurar");if(await stale.isVisible())await stale.click();
  await selectFirst(page.locator("#tutorial-tarifa-servicio"));await next("Elegir el servicio");
  await selectFirst(page.locator("#tutorial-tarifa-papel"));await next("Elegir papel y color");
  const input=page.locator("#tutorial-tarifa-precio input"), price=String(Math.ceil(Number((await input.inputValue()).replace(",",".")))+3);
  await input.fill(price);
  await card().getByRole("button",{name:"Pausar",exact:true}).click();await choose();
  await expect(input).toHaveValue(price);await next("Cambiar el precio por hoja");
  await page.locator("#tutorial-tarifa-aplicar").click();await next("Aplicar al borrador");
  await expect(page.locator("#tutorial-tarifa-resultado")).toContainText("tres hojas");
  assert.equal((await catalog()).actual.codigoPublico,initial.codigoPublico);await next("Comparar y decidir");
  await next("Consultar papeles");await page.locator("#catalog-services-tab").click();await next("Consultar servicios");await next("Comparar versiones");
  await next("Reconocer la configuración vigente");
  if(await page.locator(".configuration-layout").getAttribute("data-fase")!=="1")await action(page.locator("#config-tour-phases nav button").first());
  await page.getByRole("button",{name:"Editar borrador"}).click();await next("Abrir el borrador de trabajo");
  assert.equal((await config()).borrador.codigoPublico,prior);
  async function phase(n){await action(page.locator(`#config-tour-phases nav button:nth-child(${n})`));await expect(page.locator(".configuration-layout")).toHaveAttribute("data-fase",String(n));}
  await action(page.getByRole("button",{name:"Guardar selección"})); await next("Revisar el modelo de aprobación");
  await phase(3);await action(page.getByRole("button",{name:"Guardar pagos y reglas"}));await next("Revisar pagos y seña");
  await phase(4);await action(page.getByRole("button",{name:"Guardar método de asignación"}));await next("Comprobar recursos");
  await phase(5);const matrix=page.locator("#config-tour-matrix input[type=checkbox]");if(!(await matrix.isChecked()))await action(matrix);await action(page.getByRole("button",{name:"Guardar horarios y modalidades"}));await next("Comprobar horarios y entrega");
  await phase(6);await page.locator(".optional-simulation summary").click();await next("Abrir una simulación");
  await card().getByRole("button",{name:"Minimizar",exact:true}).click();
  const sim=page.locator(".review-simulator"); await selectFirst(sim.locator("#review-combination"));
  await sim.getByLabel("Páginas del PDF").fill("3");await sim.getByLabel("Copias del ejemplo").fill("2");
  await sim.locator("#review-sides").selectOption("NO");await sim.locator("#review-mode").selectOption("RETIRO_SUCURSAL");
  await sim.locator('input[type="datetime-local"]').fill(new Date().toISOString().slice(0,16));
  await card().getByRole("button",{name:/Mostrar explicación|Restaurar|Maximizar/}).click();
  await card().getByRole("button",{name:"Pausar",exact:true}).click();await expect(page.getByRole("button",{name:"Empezar tutorial",exact:true})).toBeVisible();
  await page.goto(root+"/administracion");await choose();
  await heading("Simular y leer el resultado");await expect(page.locator(".configuration-layout")).toHaveAttribute("data-fase","6");
  const disclosure=page.locator(".optional-simulation").last();if(!await disclosure.evaluate(el=>el.open))await disclosure.locator("summary").click();
  await expect(sim.getByLabel("Páginas del PDF")).toHaveValue("3");await expect(sim.getByLabel("Copias del ejemplo")).toHaveValue("2");
  await card().getByRole("button",{name:"Minimizar",exact:true}).click();
  await sim.getByRole("button",{name:"Simular recorrido completo"}).click();await expect(page.locator(".review-result")).toBeVisible();
  const restore=card().getByRole("button",{name:/Mostrar explicación|Restaurar|Maximizar/});await restore.click();
  await next("Simular y leer el resultado");await next("Decidir sobre la activación");await phase(8);await next("Abrir el historial de versiones");
  for(const title of ["Reconocer la identidad pública","Ubicar las imágenes de identidad","Preparar una tarjeta del catálogo","Ubicar la vista previa","Distinguir borrador y publicación local","Revisar dirección y horarios","Filtrar la bandeja","Interpretar el total y el saldo","Consultar el saldo de recursos","Reconocer las funciones bloqueadas","Ubicar la asistencia","Encontrar el manual","Identificar la seguridad de la cuenta"])await next(title);
  await heading("Tutorial completado");assert.equal((await config()).activa.configuracion.codigoPublico,operational);
  assert.equal((await config()).borrador.codigoPublico,prior);assert.equal((await catalog()).actual.codigoPublico,initial.codigoPublico);
  await page.getByRole("button",{name:/Preparar una tarifa/}).click(); await heading("Elegir el servicio");
  await action(page.getByRole("button",{name:"Preparar publicación opcional"}));
  await page.locator("#catalog-reason").fill("Verificación de la publicación opcional en base descartable");
  const save=page.getByRole("button",{name:"Guardar nueva configuración",exact:true});await action(save);
  assert.equal((await catalog()).actual.codigoPublico,initial.codigoPublico);publish=true;await action(save);
  await expect.poll(async()=>(await catalog()).actual.codigoPublico).not.toBe(initial.codigoPublico);
  assert.ok((await catalog()).actual.tarifas.some(row=>Number(row.precio)===Number(price)));
  assert.deepEqual(errors,[]);
});
