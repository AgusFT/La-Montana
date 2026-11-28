//#region ENCABEZADO · tests/demo-tutorial.browser.cjs
// Recorre los tres roles en una demo local descartable y verifica los hechos del pedido.
// Requiere un backend con PostgreSQL, ClamAV y el inspector PDF y un frontend compilado.
//#endregion
const { test } = require("node:test");
const assert = require("node:assert/strict");
const path = require("node:path");
const { restartDialog } = require("./helpers/tutorial-restart.cjs");
const endpoint = process.env.DEMO_TUTORIAL_E2E_URL;
test(
  "tutorial del pedido completo con teclado, pausa y cambio de rol",
  { skip: !endpoint },
  async (t) => {
    const url = new URL(endpoint);
    assert.ok(
      ["127.0.0.1", "localhost", "[::1]"].includes(url.hostname) &&
        url.protocol === "http:" &&
        url.port &&
        !url.username &&
        !url.password,
      "Usá la URL HTTP de un entorno de prueba local aislado.",
    );
    const root = url.origin;
    const { chromium, expect } = require("@playwright/test");
    const browser = await chromium.launch({
      headless: true,
      executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || undefined,
    });
    t.after(() => browser.close());
    const page = await browser.newPage({
      viewport: { width: 1125, height: 840 },
      reducedMotion: "reduce",
    });
    page.setDefaultTimeout(20000);
    const errores = [];
    page.on("pageerror", (error) => errores.push(error.message));
    page.on("dialog", async (dialog) => {
      if (dialog.type() === "beforeunload" ||
          (dialog.type() === "confirm" && /cambios sin guardar/i.test(dialog.message()))) {
        await dialog.accept();
      } else {
        errores.push(dialog.message());
        await dialog.dismiss();
      }
    });
    async function check() {
      const r = await page.request.get(root + "/api/demo/tutorial");
      if (!r.ok()) throw Error("HTTP " + r.status());
      return await r.json();
    }
    const panel = () => page.locator("#tutorial-panel");
    const card = () => page.locator(".tutorial-callout:visible");
    const start = () => page.getByRole("button", { name: "Empezar tutorial", exact: true });
    const existingFile = process.env.DEMO_TUTORIAL_E2E_ARCHIVO !== "nuevo";
    // Un PDF de tres o más páginas permite probar el salto directo al final.
    const testPdf = process.env.DEMO_TUTORIAL_E2E_PDF ||
      path.resolve(__dirname, "fixtures/afiche-dos-paginas.pdf");
    const fields = ["Servicio de impresión", "Tamaño de hoja", "Papel y gramaje", "Impresión"];
    const {expectTutorialAnchor} = require("./helpers/tutorial-anchor.cjs");
    const anchored = (target) => expectTutorialAnchor(page, target);
    async function guidedClick(target) {
      await anchored(target);
      await target.click();
    }
    async function guidedEdit(target, value) {
      await anchored(target);
      await target.fill(value);
      await page.keyboard.press("Tab");
    }
    async function guidedCheck(target) {
      await anchored(target);
      await target.check();
    }
    async function nextHint() {
      await card().getByRole("button", {name:"Siguiente indicación"}).click();
    }
    async function closed() {
      await expect(start()).toBeEnabled();
      await expect(panel()).toHaveCount(0);
      await expect(card()).toHaveCount(0);
      await expect(page.locator(".tutorial-highlight:visible")).toHaveCount(0);
      await expect(page.locator(".guided-tour[role=dialog]:visible")).toHaveCount(0);
    }
    async function checkMenu() {
      const sidebar = page.locator(".admin-sidebar, .client-sidebar");
      const locked = sidebar.getByRole("group", {
        name: "Funciones bloqueadas en la demo",
      });
      await expect(locked).toHaveCount(1);
      assert.ok(
        await locked.evaluate(
          (group) =>
            group.parentElement.tagName === "NAV" && !group.nextElementSibling,
        ),
        "Los módulos bloqueados cierran la lista del rol.",
      );
      await expect(
        sidebar.getByRole("button", { name: "Más de la Demo" }),
      ).toHaveCount(0);
      await expect(
        sidebar.getByRole("link", { name: "Manual de usuario", exact: true }),
      ).toHaveCount(1);
    }
    async function done(s) {
      await expect
        .poll(async () => (await check()).completados.includes(s), {
          timeout: 90000,
        })
        .toBe(true);
      t.diagnostic("Hecho confirmado: " + s);
    }
    async function login(role) {
      await page.goto(root + "/acceso");
      await page.getByRole("button", { name: role, exact: true }).click();
      await page.waitForURL((u) => !u.pathname.endsWith("/acceso"));
      await closed();
    }
    async function startFull() {
      await expect(start()).toBeEnabled();
      await start().press("Enter");
      const choices = panel().locator(".tutorial-choice strong");
      await expect(choices).toHaveText(["Tutorial del lado administrador","Tutorial del lado cliente","Tutorial de flujo completo"]);
      await panel().getByRole("button",{name:/Tutorial de flujo completo/}).press("Enter");
    }
    async function manualRole(role) {
      const headerButton=page.getByRole("button",{name:"Cambiar rol",exact:true});
      if (await headerButton.isDisabled()) {
        // Salir por el selector conserva el recorrido abierto en la base.
        await page.getByRole("button",{name:"Ir al tutorial",exact:true}).click();
        await panel().getByRole("button",{name:"Cerrar",exact:true}).click();
      }
      await page.getByRole("button", { name: "Cambiar rol", exact: true }).click();
      await page.waitForURL(/\/acceso$/);
      await login(role);
    }
    async function change(role) {
      const control=page.locator("[data-tutorial-cambiar-rol]");
      await expect(control).toBeDisabled();
      await anchored(control);
      await card()
        .getByRole("button", { name: "Continuar como " + role })
        .click();
      await page.waitForURL((url) =>
        url.pathname.startsWith(
          role === "Cliente"
            ? "/cliente"
            : role === "Empleado"
              ? "/operacion"
              : "/administracion",
        ),
      );
      assert.equal((await (await page.request.get(root+"/api/auth/me")).json()).rol, role==="Administrador"?"ADMIN_ADMIN":role.toUpperCase());
      await expect(panel()).toBeVisible();
      await checkMenu();
    }
    async function next() {
      await card()
        .getByRole("button", { name: /^Continuar con/ })
        .click();
    }
    async function expectHint(text) {
      await expect(card()).toContainText(text);
    }
    async function option(loc) {
      await expect.poll(() => loc.locator("option").count()).toBeGreaterThan(1);
      const v = await loc
        .locator("option")
        .evaluateAll((opts) => opts.find((o) => o.value && !o.disabled).value);
      await loc.selectOption(v);
    }
    async function submit(form) {
      await guidedClick(form.locator("button:not([type=button])").last());
    }
    async function stage(s, f) {
      try {
        await f();
      } catch (error) {
        throw new Error(s + ": " + error.message, { cause: error });
      }
    }
    await stage("tarifa", async () => {
      await login("Administrador");
      assert.equal(
        (await check()).estado,
        "NUEVO",
        "Esta prueba requiere una base de demo recién inicializada y descartable.",
      );
      await checkMenu();
      if (existingFile) {
        await manualRole("Cliente");
        await page.goto(root + "/cliente/cotizaciones/nueva");
        await closed();
        const manualUpload = page.locator("#client-upload input[type=file]");
        await expect(manualUpload).toBeEnabled();
        await manualUpload.setInputFiles(testPdf);
        await expect(page.getByRole("button", { name: /Configurar PDF 1/ })).toBeEnabled({ timeout: 90000 });
        assert.equal((await check()).preparacion, null);
        await manualRole("Administrador");
      }
      await startFull();
      await expect(page).toHaveURL(/\/administracion\/catalogo#revision-comercial$/);
      const nav = panel().getByRole("navigation", { name: "Pasos del tutorial" });
      await expect(nav).toBeVisible();
      await expect(nav.locator('[aria-current="step"]')).toContainText(/consultar la tarifa/i);
      await expect(nav.getByRole("button", { name: /leer el precio por hoja/i })).toBeVisible();
      const prices = page.locator('[data-tutorial-tarifa-leida="true"]').first();
      const confirm = card().getByRole("button", { name: "Confirmar que revisé la tarifa" });
      await expect(card()).toContainText(/calcular los presupuestos/i);
      await expectHint(/simple faz/i);
      await expect(prices).toContainText(/ARS\s*\d/);
      await anchored(prices);
      await nav.getByRole("button", { name: /leer el precio por hoja/i }).press("Enter");
      await expect(card().getByRole("heading", { name: /leer el precio por hoja/i })).toBeVisible();
      await anchored(prices);
      await nav.getByRole("button", { name: /9\. Entregar y cerrar/i }).click();
      await expect(page).toHaveURL(/catalogo#revision-comercial/);
      await expect(page.getByText(/primero completá/i).first()).toBeVisible();
      await panel().getByRole("button", { name: "Pausar", exact: true }).click();
      await closed();
      await page.goto(root + "/administracion");
      await page.setViewportSize({ width: 390, height: 845 });
      await closed();
      await startFull();
      await expect(page).toHaveURL(/catalogo#revision-comercial/);
      await anchored(prices);
      await expect(confirm).toBeInViewport({ ratio: 1 });
      await card().getByRole("button", { name: "Ver pasos del tutorial" }).press("Enter");
      await expect(nav).toBeInViewport();
      await panel().getByRole("button", { name: "Volver a la indicación" }).press("Enter");
      await anchored(prices);
      assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
      await confirm.press("Enter");
      await done("tarifa");
      const before = await check();
      // Una lectura desactualizada de sesión no permite avanzar ni saltar el archivo.
      await page.route("**/api/auth/me", async route => {
        await route.fulfill({json:{rol:"ADMIN_ADMIN"}});
      },{times:1});
      await card().getByRole("button",{name:"Continuar como Cliente",exact:true}).click();
      await expect(card()).toContainText(/no pudimos comprobar el cambio/i);
      assert.equal((await check()).paso,before.paso);
      await change("Cliente");
      await page.setViewportSize({ width: 1440, height: 960 });
    });
    await stage("archivo y presupuesto", async () => {
      await expect(page).toHaveURL(/\/cliente\/cotizaciones\/nueva/);
      const rolesPath=root+"/api/demo/tutorial/recorridos";
      const independent=await (await page.request.get(rolesPath)).json();
      const active=await check();
      await restartDialog(page,card(),"flujo completo","cancelar");
      assert.deepEqual(await check(),active);
      await restartDialog(page,card(),"flujo completo","reiniciar");
      await change("Administrador");
      await expect(page).toHaveURL(/catalogo#revision-comercial$/);
      const restarted=await check();
      assert.equal(restarted.paso,"tarifa");
      assert.equal(restarted.intento,active.intento+1);
      assert.deepEqual(restarted.completados,[]);
      assert.deepEqual(restarted.borradores,{});
      assert.deepEqual(await (await page.request.get(rolesPath)).json(),independent);
      await card().getByRole("button",{name:"Confirmar que revisé la tarifa"}).click();
      await change("Cliente");
      const upload = page.locator("#client-upload input[type=file]");
      const consent = page.getByRole("button", { name: "Usar esta preparación en el tutorial" });
      if (existingFile) {
        await expect(consent).toBeEnabled();
        await expect(upload).toBeDisabled();
        await anchored(consent);
        assert.equal((await check()).preparacion, null);
        await consent.click();
      } else {
        const selectFile = page.getByRole("button", { name: "Seleccionar PDF", exact: true });
        await anchored(selectFile);
        const chooser = page.waitForEvent("filechooser");
        await selectFile.press("Enter");
        await (await chooser).setFiles(testPdf);
      }
      await done("archivo");
      await next();
      await expectHint(/servicio de impresión/i);
      const area = page.locator("#client-work-options");
      const taskNav = panel().getByRole("navigation", { name: "Pasos del tutorial" });
      await expect(area.getByLabel("Copias", { exact: true })).toBeEnabled();
      await taskNav.getByRole("button", { name: /indicar las copias/i }).press("Enter");
      await expectHint(/cantidad de copias/i);
      await taskNav.getByRole("button", { name: /elegir el servicio/i }).press("Enter");
      await expectHint(/servicio de impresión/i);
      await page.setViewportSize({ width: 820, height: 1180 });
      await anchored(area.getByLabel("Servicio de impresión", { exact: true }));
      for (const [label, nextInstruction] of [
        ["Servicio de impresión", /elegí el tamaño/i],
        ["Tamaño de hoja", /seleccioná el papel/i],
        ["Papel y gramaje", /elegí blanco y negro o color/i],
        ["Impresión", /cantidad de copias/i],
      ]) {
        await anchored(area.getByLabel(label, { exact: true }));
        await option(area.getByLabel(label, { exact: true }));
        await expectHint(nextInstruction);
      }
      await guidedEdit(area.getByLabel("Copias", { exact: true }), "3");
      await anchored(area.getByLabel("Caras", { exact: true }));
      await expect
        .poll(
          async () =>
            JSON.parse((await check()).borradores.archivo?.opciones || "[]")[0]
              ?.copies,
        )
        .toBe("3");
      const saved = await check();
      for (const role of ["Administrador", "Empleado", "Cliente"]) {
        await manualRole(role);
        const state = await check();
        assert.equal(state.estado, "ACTIVO");
        assert.equal(state.paso, saved.paso);
        assert.equal(state.preparacion, saved.preparacion);
        if (role === "Cliente") assert.deepEqual(state.borradores, saved.borradores);
        else assert.equal(state.borradores.archivo, undefined);
      }
      await page.goto(root + "/cliente/cotizaciones/nueva");
      await closed();
      const existingPreparation = page.getByRole("region", { name: "Preparación en curso" });
      await expect(existingPreparation).toContainText("Retomaste la preparación abierta del tutorial");
      await expect(existingPreparation).toContainText(path.basename(testPdf));
      await expect(area.getByLabel("Copias", { exact: true })).toHaveValue("1");
      assert.deepEqual((await check()).borradores, saved.borradores);
      await startFull();
      await expect(page).toHaveURL(/\/cliente\/cotizaciones\/nueva/);
      await expect(area.getByLabel("Copias", { exact: true })).toHaveValue("3");
      await page.setViewportSize({ width: 1440, height: 960 });
      await panel().getByRole("button", {name:"Calcular el subtotal"}).click();
      await guidedClick(page.locator("#client-budget-action button"));
      await done("presupuesto");
      await next();
    });
    async function confirmOrder(guided) {
      async function act(control, action) {
        if (guided) await anchored(control);
        await action(control);
      }
      const click = (control) => act(control, (target) => target.click());
      const checkBox = (control) => act(control, (target) => target.check());
      const select = (control, value) =>
        act(control, (target) => target.selectOption(value));
      await select(page.getByLabel("Modalidad", { exact: true }), "RETIRO_SUCURSAL");
      await act(page.getByLabel("Destino de retiro", { exact: true }), option);
      await select(page.getByLabel("Medio de pago", { exact: true }), "EFECTIVO");
      await click(page.locator("#client-review-files button").first());
      const dialog = page.getByRole("dialog");

      await click(dialog.getByRole("button", {name:/Ver páginas/}).first());
      const preview = dialog.getByRole("region", { name: "Vista previa del PDF" });
      const pageNumber = preview.getByRole("spinbutton");
      await expect(pageNumber).toBeVisible();
      const totalPages = Number(await pageNumber.getAttribute("max"));
      assert.ok(totalPages >= 2, "El PDF de prueba debe tener al menos dos páginas.");
      if (guided && totalPages > 2) {
        // Un usuario puede saltar por número aunque la guía señale la página siguiente.
        await expect(preview).toContainText("1 de " + totalPages + " páginas abiertas");
        await anchored(preview.getByRole("button", { name: "Página siguiente" }));
        await pageNumber.fill(String(totalPages));
        for (let seen = 2; seen < totalPages; seen += 1) {
          await expect(preview).toContainText(seen + " de " + totalPages + " páginas abiertas");
          await guidedClick(preview.getByRole("button", { name: "Página anterior" }));
        }
      } else {
        for (let seen = 1; seen < totalPages; seen += 1) {
          await expect(preview).toContainText(seen + " de " + totalPages + " páginas abiertas");
          await click(preview.getByRole("button", { name: "Página siguiente" }));
        }
      }
      const acc = dialog
        .getByRole("button", { name: /Aceptar vista previa/ })
        .last();
      await expect(dialog.locator("input[type=checkbox]").first()).toBeEnabled({
        timeout: 80000,
      });
      if (guided) await expect(dialog.locator(".tutorial-callout")).toBeVisible();
      await checkBox(dialog.locator("input[type=checkbox]").first());
      await click(acc);
      await expect(dialog).toContainText(/aceptad/i);
      await click(dialog.getByRole("button", { name: /Volver al resumen/ }));
      await checkBox(page.locator("#client-create-order input[type=checkbox]"));
      await click(page.locator("#client-create-order button"));
      await page.waitForURL(/\/cliente\/pedidos\/[^/]+/);
      return new URL(page.url()).pathname.split("/").at(-1);
    }
    await stage("confirmacion", async () => {
      await expectHint(/retiro en sucursal/i);
      const guidedOrder = await confirmOrder(true);
      await done("confirmacion");
      const saved = await check();
      assert.equal(saved.pedido, guidedOrder);
      await manualRole("Cliente");
      const area = page.locator("#client-work-options");
      const upload = page.locator("#client-upload input[type=file]");
      await page.goto(root + "/cliente/cotizaciones/nueva");
      await closed();
      await expect(upload).toBeEnabled();
      await upload.setInputFiles(testPdf);
      for (const field of fields) await option(area.getByLabel(field, { exact: true }));
      await area.getByLabel("Copias", { exact: true }).fill("2");
      await page.locator("#client-budget-action button").click();
      await page.waitForURL(/\/cliente\/preparaciones\/[^/]+/);
      assert.notEqual(new URL(page.url()).pathname.split("/").at(-1), saved.preparacion);
      await closed();
      assert.notEqual(await confirmOrder(false), guidedOrder);
      const after = await check();
      for (const key of ["estado", "paso", "completados", "preparacion", "cotizacion", "pedido", "borradores"]) {
        assert.deepEqual(after[key], saved[key], "El pedido normal debe dejar intacto " + key);
      }
      await startFull();
      await expect(page).toHaveURL(new RegExp("/cliente/pedidos/" + guidedOrder));
      await change("Empleado");
    });
    await stage("cobro", async () => {
      await expectHint(/registrar efectivo/i);
      const st = await check(),
        fin = await (
          await page.request.get(
            root + "/api/operacion/cotizaciones/" + st.cotizacion + "/pagos",
          )
        ).json();
      if (!fin.pagos.length) {
        await guidedClick(page.getByRole("button", { name: "Registrar efectivo" }));
        const form = page.locator(".payment-form");
        await expectHint(/saldo pendiente completo/i);
        await guidedEdit(form.locator("[name=importe]"),fin.total);
        await guidedEdit(form.locator("[name=referencia]"),"PRACTICA-" + Date.now());
        await guidedEdit(form.locator("[name=fecha]"),
            new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
              .toISOString()
              .slice(0, 16),
          );
        await guidedEdit(form.locator("[name=motivo]"),"Recibo del pedido del tutorial");
        await expect
          .poll(async () => (await check()).borradores.cobro?.observacion)
          .toBe("Recibo del pedido del tutorial");
        await page.reload();
        await guidedClick(page.getByRole("button", { name: "Registrar efectivo" }));
        await expect(form.locator("[name=importe]")).toHaveValue(fin.total);
        await expect(form.locator("[name=motivo]")).toHaveValue(
          "Recibo del pedido del tutorial",
        );
        await panel().getByRole("button",{name:"Verificar el dinero",exact:true}).click();
        await guidedCheck(form.locator("input[type=checkbox]"));
        await submit(form);
        await expect(form).toHaveCount(0);
      }
      const form = page.locator(".payment-form");
      await guidedClick(page.locator("[data-tutorial-aplicar]").first());
      await guidedEdit(form.locator("[name=importe]"),fin.total);
      await guidedEdit(form.locator("[name=motivo]"),"Aplicar al trabajo de práctica");
      // «Volver sin guardar» comparte la fila con el botón de confirmación.
      await expect(card()).toContainText(/Aplicar dinero a esta cotización/i);
      await submit(form);
      await done("cobro");
      await next();
    });
    await stage("revision", async () => {
      await expectHint(/registrar revisión/i);
      for (const cmd of ["REVISAR", "APROBAR"]) {
        const btn = page.locator('[data-tutorial-gestion="' + cmd + '"]');
        const st = await check();
        const ops = await (
          await page.request.get(
            root + "/api/operacion/pedidos/" + st.pedido + "/gestion",
          )
        ).json();
        if (!ops.acciones.find((a) => a.accion === cmd)?.habilitada) continue;
        await expect(btn).toBeEnabled();
        await guidedClick(btn);
        const form = page.locator(".order-decision-form");
        await guidedEdit(form.locator("[name=motivo]"),"Verifiqué los detalles del pedido de práctica");
        for (const ch of await form.locator("input[type=checkbox]").all())
          await guidedCheck(ch);
        await submit(form);
        await expect(form).toHaveCount(0);
      }
      await done("revision");
      await next();
    });
    await stage("produccion y calidad", async () => {
      await expectHint(/iniciar impresión simulada/i);
      for (const cmd of ["INICIAR", "COMPLETAR", "CALIDAD"]) {
        if (cmd === "COMPLETAR") {
          await expectHint(/el trabajo ya figura en impresión/i);
        }
        const btn = page
          .locator('[data-tutorial-movimiento="' + cmd + '"]')
          .first();
        await guidedClick(btn);
        const form = page.locator(".production-form");
        if (cmd === "INICIAR") {
          const s = form.getByLabel("Impresora o recurso simulado");
          await anchored(s);
          const v = await s
            .locator("option")
            .evaluateAll(
              (os) => os.find((o) => o.value && o.value !== "MANUAL").value,
            );
          await s.selectOption(v);
        }
        if (cmd === "CALIDAD") {
          for (const ch of await form.locator(".production-quality-checks input").all()) await guidedCheck(ch);
          const result = form.getByLabel("Resultado de calidad");
          await anchored(result);
          await result.selectOption("APROBADO");
        }
        await guidedEdit(form.locator("[name=motivo]"),"Operación simulada según el pedido de práctica");
        await guidedCheck(form.locator("[name=confirmacion]"));
        await submit(form);
        await expect(form).toHaveCount(0);
        if (cmd === "COMPLETAR") {
          await done("produccion");
          await next();
          await expectHint(/controlar calidad/i);
        }
      }
      await done("calidad");
      await next();
    });
    await stage("entrega", async () => {
      await card()
        .getByRole("button", { name: "Pasar a Cliente para obtener el código" })
        .click();
      await change("Cliente");
      await guidedClick(page.locator("[data-tutorial-codigo]"));
      await anchored(page.locator(".delivery-secret"));
      const code = await page
        .getByLabel("Código de entrega", { exact: true })
        .innerText();
      await card()
        .getByRole("button", {
          name: "Volver a Empleado para registrar la entrega",
        })
        .click();
      await change("Empleado");
      for (const cmd of ["VALIDAR", "ENTREGAR", "CERRAR"]) {
        if (cmd === "CERRAR") {
          await expect(card().getByRole("heading", { name: "Abrir el cierre" })).toBeVisible();
        }
        await guidedClick(page.locator('[data-tutorial-entrega="' + cmd + '"]'));
        const form = page.locator(".delivery-dialog[open] form");
        if (cmd === "VALIDAR") {
          const codeField = form.locator("[name=codigo]");
          await expect(page.locator(".delivery-dialog[open] .tutorial-callout")).toBeVisible();
          await anchored(codeField);
          await codeField.fill(code);
          await expect(codeField).toBeFocused();
        }
        else {
          if(cmd==="ENTREGAR") {
            await anchored(form.locator("[name=receptor]"));
            await nextHint();
          }
          await guidedEdit(form.locator("[name=motivo]"),"Entrega del pedido de práctica verificada");
          await guidedCheck(form.locator("[name=confirmado]"));
        }
        await anchored(form.getByRole("button", {
          name: cmd === "VALIDAR" ? "Validar código" : "Confirmar registro",
        }));
        await submit(form);
        await expect(form).toHaveCount(0);
      }
      await done("entrega");
      await expect(
        page.getByRole("heading", { name: "Pedido completado" }).first(),
      ).toBeVisible();
      const sidebar = page.locator(".admin-sidebar");
      const close = panel().getByRole("button", {
        name: "Cerrar",
        exact: true,
      });
      for (const viewport of [
        { width: 1440, height: 960 },
        { width: 390, height: 845 },
      ]) {
        await page.setViewportSize(viewport);
        await checkMenu();
        const lines = await close.evaluate((button) => {
          const text = document.createRange();
          text.selectNodeContents(button);
          return text.getClientRects().length;
        });
        assert.equal(lines, 1);
        const heights = await sidebar
          .getByRole("navigation")
          .getByRole("link")
          .evaluateAll((links) =>
            links.map((link) => link.getBoundingClientRect().height),
          );
        assert.ok(
          heights.length > 0 &&
            heights.every((height) => height > 0 && height <= 60),
        );
      }
      const final = await check();
      assert.equal(final.estado, "COMPLETO");
      assert.equal(final.completados.length, 9);
      await close.click();
      await closed();
      await startFull();
      await expect(card().getByRole("heading", { name: "Pedido completado" })).toBeVisible();
      await restartDialog(page,card(),"flujo completo","reiniciar");
      const fresh=await check();
      assert.equal(fresh.intento,final.intento+1);
      assert.equal(fresh.paso,"tarifa");
      assert.equal(fresh.estado,"ACTIVO");
      assert.equal(fresh.preparacion,null);
      assert.equal(fresh.pedido,null);
      assert.deepEqual(fresh.borradores,{});
      assert.deepEqual(fresh.completados,[]);
      const delivery=await page.request.get(root+"/api/operacion/pedidos/"+final.pedido+"/produccion");
      assert.equal(delivery.status(),200,"El pedido completado sigue accesible.");
      await change("Administrador");
      await page.reload();
      await expect(card().getByRole("button",{name:"Confirmar que revisé la tarifa"})).toBeEnabled();
      t.diagnostic(
        "Pedido de práctica " + final.pedido + " completado en los tres roles.",
      );
    });

    assert.deepEqual(errores, []);
  },
);
