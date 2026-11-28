const assert = require("node:assert/strict");
const path = require("node:path");
const { expectTutorialAnchor } = require("./tutorial-anchor.cjs");

async function reviewFields(page, selectors, { capture, inspect } = {}) {
  const { expect } = require("@playwright/test");
  const card = page.locator(".tutorial-callout:visible");
  const changes = [];
  function track(request) {
    const route = new URL(request.url()).pathname;
    if (!["GET", "HEAD", "OPTIONS"].includes(request.method()) && /^\/api\/(cliente|operacion)\//.test(route)) {
      changes.push(request.method() + " " + route);
    }
  }
  page.on("request", track);
  try {
    for (const [i, selector] of selectors.entries()) {
      const target = page.locator(selector);
      await expectTutorialAnchor(page, target);
      if (i === 1) {
        await card.getByRole("button", { name: "Anterior", exact: true }).click();
        await expectTutorialAnchor(page, page.locator(selectors[0]));
        await card.getByRole("button", { name: "Siguiente", exact: true }).click();
        await expectTutorialAnchor(page, target);
      }
      if (inspect) await inspect(target, selector);
      const next = card.getByRole("button", {
        name: i === selectors.length - 1 ? "Terminar revisión" : "Siguiente",
        exact: true,
      });
      await expect(next).toBeEnabled();
      await expect(next, "El botón de la guía debe estar visible junto a los datos.").toBeInViewport({ ratio: 1 });
      if (capture && i === 0 && process.env.DEMO_E2E_CAPTURAS) {
        await page.screenshot({ path: path.join(process.env.DEMO_E2E_CAPTURAS, capture + ".png") });
      }
      await next.click();
    }
    const again = card.getByRole("button", { name: "Volver a revisar", exact: true });
    await expect(again).toBeEnabled();
    if (capture === "pedido") {
      await again.click();
      for (const [i, selector] of selectors.entries()) {
        await expectTutorialAnchor(page, page.locator(selector));
        const name = i === selectors.length - 1 ? "Terminar revisión" : "Siguiente";
        await card.getByRole("button", { name, exact: true }).click();
      }
      await expect(again).toBeEnabled();
    }
    assert.deepEqual(changes, [], "Revisar los datos no debe registrar operaciones.");
  } finally {
    page.off("request", track);
  }
}

function orderReview(shipping) {
  return [
    "#client-budget .workspace-budget-items",
    ".workspace-price .workspace-delivery-summary",
    ...(shipping ? ["#client-create-order .workspace-contact"] : []),
    '#client-delivery select[aria-label="Medio de pago"]',
    ".workspace-price dl",
    "#client-order-conditions",
  ];
}

async function openGuideIndex(page) {
  if (page.viewportSize().width > 1080) return;
  const toggle = page.locator("#tutorial-panel .tutorial-index-toggle");
  await toggle.waitFor({ state: "visible" });
  if (await toggle.getAttribute("aria-expanded") === "false") await toggle.click();
}

module.exports = { openGuideIndex, orderReview, reviewFields };
