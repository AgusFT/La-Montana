//#region ENCABEZADO · tests/demo-tutorial.test.cjs
// Comprueba los requisitos entre pasos, las rutas por rol y las fechas del catálogo.
//#endregion
const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const ts = require("typescript");
require.extensions[".ts"] = (module, file) =>
  module._compile(
    ts.transpileModule(fs.readFileSync(file, "utf8"), {
      compilerOptions: { module: ts.ModuleKind.CommonJS },
    }).outputText,
    file,
  );
const {
  firstRequired,
  steps,
  tutorialPath,
} = require("../src/lib/demo-tutorial.ts");

test("saltar a un paso avanzado vuelve al primer requisito que todavía falta", () => {
  assert.equal(firstRequired("calidad", []), "tarifa");
  assert.equal(
    firstRequired("calidad", ["tarifa", "archivo", "confirmacion"]),
    "presupuesto",
  );
  assert.equal(firstRequired("archivo", steps), "archivo");
});
test("al consultar un paso ya realizado se abre el mismo pedido en el área del rol", () => {
  const state = { preparacion: "preparacion", pedido: "pedido", bloqueo: null };
  assert.equal(
    tutorialPath("archivo", state, "CLIENTE"),
    "/cliente/pedidos/pedido#pedido-pdf",
  );
  assert.equal(
    tutorialPath("confirmacion", state, "CLIENTE"),
    "/cliente/pedidos/pedido#pedido-resumen",
  );
  assert.equal(
    tutorialPath(
      "presupuesto",
      { ...state, bloqueo: "Pedido de baja" },
      "CLIENTE",
    ),
    "/cliente/cotizaciones/nueva",
  );
  assert.equal(
    tutorialPath("confirmacion", { ...state, pedido: null }, "CLIENTE"),
    "/cliente/preparaciones/preparacion",
  );
  assert.equal(
    tutorialPath("entrega", state, "CLIENTE"),
    "/cliente/pedidos/pedido#pedido-entrega",
  );
  assert.equal(
    tutorialPath("entrega", state, "EMPLEADO"),
    "/operacion/pedidos/pedido#pedido-entrega",
  );
});
test("la fecha de la tarifa usa espacios consistentes en el servidor y el navegador", () => {
  const { catalogDate } = require("../src/lib/catalog-types.ts");
  const date = catalogDate("2026-11-27T23:37:00Z");
  assert.match(date, / UTC$/);
  assert.doesNotMatch(date, /[\u00a0\u202f]/);
});
