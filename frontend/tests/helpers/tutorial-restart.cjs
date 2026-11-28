const assert = require("node:assert/strict");

async function restartDialog(page, scope, tutorial, action, { lostResponse } = {}) {
  const { expect } = require("@playwright/test");
  await expect(scope).toBeVisible();
  const button = { name: "Reiniciar tutorial", exact: true };
  const fromCard = scope.getByRole("button", button);
  const trigger = await fromCard.count() ? fromCard : page.locator("#tutorial-panel").getByRole("button", button);
  await expect(trigger).toBeEnabled();
  await trigger.press("Enter");
  const dialog = page.getByRole("dialog", { name: new RegExp(tutorial, "i") });
  await expect(dialog).toBeVisible();
  const cancel = dialog.getByRole("button", { name: "Cancelar", exact: true });
  await expect(cancel).toBeFocused();
  if (action === "reiniciar") {
    const confirm = dialog.getByRole("button", { name: "Reiniciar tutorial", exact: true });
    if (lostResponse) {
      let applied = false;
      const dropResponse = async route => {
        const request = route.request();
        if (applied || request.method() !== "PUT" || request.postDataJSON()?.accion !== "reiniciar") {
          return route.continue();
        }
        applied = true;
        const response = await route.fetch();
        assert.equal(response.status(), 200);
        await route.abort("failed");
      };
      await page.route(lostResponse, dropResponse);
      try {
        await confirm.press("Enter");
        await expect(dialog.getByRole("alert")).toBeVisible();
      } finally {
        await page.unroute(lostResponse, dropResponse);
      }
    }
    await confirm.press("Enter");
  } else {
    assert.ok(action === "cancelar" || action === "escape");
    await cancel.press(action === "escape" ? "Escape" : "Enter");
    await expect(trigger).toBeFocused();
  }
  await expect(dialog).toHaveCount(0);
}

module.exports = { restartDialog };
