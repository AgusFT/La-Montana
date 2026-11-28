//#region ENCABEZADO · tests/demo-tutorial.browser.cjs
// Recorre los tres roles en una demo local descartable y verifica los hechos del pedido.
// Requiere un backend con PostgreSQL, ClamAV y el inspector PDF y un frontend compilado.
//#endregion
const { test } = require("node:test");
const assert = require("node:assert/strict");
const path = require("node:path");
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
      viewport: { width: 1440, height: 960 },
      reducedMotion: "reduce",
    });
    page.setDefaultTimeout(20000);
    const errores = [];
    page.on("pageerror", (error) => errores.push(error.message));
    async function check() {
      const r = await page.request.get(root + "/api/demo/tutorial");
      if (!r.ok()) throw Error("HTTP " + r.status());
      return await r.json();
    }
    const panel = () => page.locator("#tutorial-panel");
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
      await page.waitForSelector(".tutorial-start");
    }
    async function change(role) {
      await panel()
        .getByRole("button", { name: "Confirmar cambio a " + role })
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
      await expect(panel()).toBeVisible();
    }
    async function next() {
      await panel()
        .getByRole("button", { name: /^Continuar con/ })
        .click();
    }
    async function option(loc) {
      await expect.poll(() => loc.locator("option").count()).toBeGreaterThan(1);
      const v = await loc
        .locator("option")
        .evaluateAll((opts) => opts.find((o) => o.value && !o.disabled).value);
      await loc.selectOption(v);
    }
    async function submit(form) {
      await form.locator("button:not([type=button])").last().click();
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
      await expect(
        page.getByRole("link", { name: "Manual de usuario", exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Iniciar Tutorial", exact: true }),
      ).toBeEnabled();
      await page
        .getByRole("button", { name: "Iniciar Tutorial", exact: true })
        .focus();
      await page.keyboard.press("Enter");
      await expect(
        panel().getByRole("heading", { name: "Consultar la tarifa" }),
      ).toBeVisible();
      await panel()
        .getByRole("button", { name: /9\. Entregar y cerrar/ })
        .click();
      await expect(page).toHaveURL(/catalogo#historial-comercial/);
      await expect(panel()).toContainText("primero completá");
      await page.locator("[data-tutorial-tarifa]").click();
      await panel()
        .getByRole("button", { name: "Confirmar que revisé la tarifa" })
        .click();
      await done("tarifa");
      await next();
      await change("Cliente");
    });
    await stage("archivo y presupuesto", async () => {
      await expect.poll(() => page.url()).toContain("/cliente");
      let st = await check();
      if (st.paso === "tarifa") await next();
      await page.goto(root + "/cliente/cotizaciones/nueva");
      const upload = page.locator("#client-upload input[type=file]");
      if (!st.completados.includes("archivo")) {
        await expect(upload).toBeEnabled();
        await expect(panel()).toContainText("PDF de práctica");
        await upload.setInputFiles(
          path.resolve(__dirname, "../public/tutorial/afiche-senderos.pdf"),
        );
        await done("archivo");
      }
      st = await check();
      if (st.paso === "archivo") {
        await next();
      }
      const area = page.locator("#client-work-options");
      for (const label of [
        "Servicio de impresión",
        "Tamaño de hoja",
        "Papel y gramaje",
        "Impresión",
      ])
        await option(area.getByLabel(label, { exact: true }));
      await area.getByLabel("Copias", { exact: true }).fill("3");
      await expect
        .poll(
          async () =>
            JSON.parse((await check()).borradores.archivo.opciones)[0].copies,
        )
        .toBe("3");
      await panel()
        .getByRole("button", { name: "Pausar", exact: true })
        .click();
      await page.reload();
      await page.getByRole("button", { name: "Retomar tutorial" }).click();
      await expect(area.getByLabel("Copias", { exact: true })).toHaveValue("3");
      await page.setViewportSize({ width: 390, height: 845 });
      await expect(
        panel().getByRole("button", { name: "Ver todos los pasos" }),
      ).toBeVisible();
      await panel()
        .getByRole("button", { name: "Ver todos los pasos" })
        .press("Enter");
      await expect(panel().getByRole("navigation")).toBeVisible();

      if (
        await page.evaluate(
          () => document.documentElement.scrollWidth > innerWidth,
        )
      )
        throw Error("Overflow móvil");
      await page.setViewportSize({ width: 1440, height: 960 });
      await page.locator("#client-budget-action button").click();
      await done("presupuesto");
      await next();
    });
    await stage("confirmacion", async () => {
      await page
        .getByLabel("Modalidad", { exact: true })
        .selectOption("RETIRO_SUCURSAL");
      await option(page.getByLabel("Destino de retiro", { exact: true }));
      await page
        .getByLabel("Medio de pago", { exact: true })
        .selectOption("EFECTIVO");
      await page.locator("#client-review-files button").first().click();
      const dialog = page.getByRole("dialog");

      await dialog
        .getByRole("button", { name: /Ver páginas/ })
        .first()
        .click();
      let acc = dialog
        .getByRole("button", { name: /Aceptar vista previa/ })
        .last();
      await expect(dialog.locator("input[type=checkbox]").first()).toBeEnabled({
        timeout: 80000,
      });
      await dialog.locator("input[type=checkbox]").first().check();
      await acc.click();
      await expect(dialog).toContainText(/aceptad/i);
      await dialog.getByRole("button", { name: /Volver al resumen/ }).click();
      await page.locator("#client-create-order input[type=checkbox]").check();
      await page.locator("#client-create-order button").click();
      await done("confirmacion");
      await next();
      await change("Empleado");
    });
    await stage("cobro", async () => {
      const st = await check(),
        fin = await (
          await page.request.get(
            root + "/api/operacion/cotizaciones/" + st.cotizacion + "/pagos",
          )
        ).json();
      if (!fin.pagos.length) {
        await page.getByRole("button", { name: "Registrar efectivo" }).click();
        const form = page.locator(".payment-form");
        await form.locator("[name=importe]").fill(fin.total);
        await form.locator("[name=referencia]").fill("PRACTICA-" + Date.now());
        await form
          .locator("[name=fecha]")
          .fill(
            new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
              .toISOString()
              .slice(0, 16),
          );
        await form
          .locator("[name=motivo]")
          .fill("Recibo del pedido del tutorial");
        await expect
          .poll(async () => (await check()).borradores.cobro?.observacion)
          .toBe("Recibo del pedido del tutorial");
        await page.reload();
        await page.getByRole("button", { name: "Registrar efectivo" }).click();
        await expect(form.locator("[name=importe]")).toHaveValue(fin.total);
        await expect(form.locator("[name=motivo]")).toHaveValue(
          "Recibo del pedido del tutorial",
        );
        await form.locator("input[type=checkbox]").check();
        await submit(form);
        await expect(form).toHaveCount(0);
      }
      const form = page.locator(".payment-form");
      await page.locator("[data-tutorial-aplicar]").first().click();
      await form.locator("[name=importe]").fill(fin.total);
      await form
        .locator("[name=motivo]")
        .fill("Aplicar al trabajo de práctica");
      await submit(form);
      await done("cobro");
      await next();
    });
    await stage("revision", async () => {
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
        await btn.click();
        const form = page.locator(".order-decision-form");
        await form
          .locator("[name=motivo]")
          .fill("Verifiqué los detalles del pedido de práctica");
        for (const ch of await form.locator("input[type=checkbox]").all())
          await ch.check();
        await submit(form);
        await expect(form).toHaveCount(0);
      }
      await done("revision");
      await next();
    });
    await stage("produccion y calidad", async () => {
      for (const cmd of ["INICIAR", "COMPLETAR", "CALIDAD"]) {
        const btn = page
          .locator('[data-tutorial-movimiento="' + cmd + '"]')
          .first();
        await btn.click();
        const form = page.locator(".production-form");
        if (cmd === "INICIAR") {
          const s = form.getByLabel("Impresora o recurso simulado");
          const v = await s
            .locator("option")
            .evaluateAll(
              (os) => os.find((o) => o.value && o.value !== "MANUAL").value,
            );
          await s.selectOption(v);
        }
        if (cmd === "CALIDAD") {
          await form
            .getByLabel("Resultado de calidad")
            .selectOption("APROBADO");
        }
        await form
          .locator("[name=motivo]")
          .fill("Operación simulada según el pedido de práctica");
        for (const ch of await form.locator("input[type=checkbox]").all())
          await ch.check();
        await submit(form);
        await expect(form).toHaveCount(0);
        if (cmd === "COMPLETAR") {
          await done("produccion");
          await next();
        }
      }
      await done("calidad");
      await next();
    });
    await stage("entrega", async () => {
      await panel()
        .getByRole("button", { name: "Pasar a Cliente para obtener el código" })
        .click();
      await change("Cliente");
      await page.locator("[data-tutorial-codigo]").click();
      const code = await page
        .getByLabel("Código de entrega", { exact: true })
        .innerText();
      await panel()
        .getByRole("button", {
          name: "Volver a Empleado para registrar la entrega",
        })
        .click();
      await change("Empleado");
      for (const cmd of ["VALIDAR", "ENTREGAR", "CERRAR"]) {
        await page.locator('[data-tutorial-entrega="' + cmd + '"]').click();
        const form = page.locator(".delivery-dialog[open] form");
        if (cmd === "VALIDAR") await form.locator("[name=codigo]").fill(code);
        else {
          await form
            .locator("[name=motivo]")
            .fill("Entrega del pedido de práctica verificada");
          for (const ch of await form.locator("input[type=checkbox]").all())
            await ch.check();
        }
        await submit(form);
        await expect(form).toHaveCount(0);
      }
      await done("entrega");
      await expect(
        panel().getByRole("heading", { name: "Pedido completado" }),
      ).toBeVisible();
      const final = await check();
      assert.equal(final.estado, "COMPLETO");
      assert.equal(final.completados.length, 9);
      t.diagnostic(
        "Pedido de práctica " + final.pedido + " completado en los tres roles.",
      );
    });

    assert.deepEqual(errores, []);
  },
);
