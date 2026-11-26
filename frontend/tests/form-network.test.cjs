const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const ts = require("typescript");

require.extensions[".ts"] = (module, filename) => {
  const compiled = ts.transpileModule(readFileSync(filename, "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  });
  module._compile(compiled.outputText, filename);
};

const { secureMutation, MutationError } = require("../src/lib/secure-mutation.ts");
const { quoteRead } = require("../src/lib/quote-types.ts");

test("un corte antes del envío orienta el reintento y evita enviar la ficha", async (t) => {
  const fetch = t.mock.method(globalThis, "fetch", async () => { throw new TypeError("Failed to fetch"); });
  await assert.rejects(secureMutation("/api/demo/clientes"), /No pudimos conectar.*volvé a intentar/);
  assert.deepEqual(fetch.mock.calls.map(call => call.arguments[0]), ["/api/auth/csrf"]);
});

test("perder la respuesta del envío conserva la operación como incierta", async (t) => {
  t.mock.method(globalThis, "fetch", async (path) => {
    if (path === "/api/auth/csrf") return Response.json({ token: "prueba", headerName: "X-CSRF-TOKEN" });
    throw new TypeError("Failed to fetch");
  });
  await assert.rejects(secureMutation("/api/demo/clientes"), error => {
    assert.ok(error instanceof MutationError && error.uncertain);
    assert.match(error.message, /no pudimos confirmar.*misma operación/);
    return true;
  });
});

test("una consulta distingue la desconexión del motivo informado por la imprenta", async (t) => {
  const fetch = t.mock.method(globalThis, "fetch", async () => { throw new TypeError("Failed to fetch"); });
  const resumen = () => quoteRead("/api/cliente/preparaciones/prueba/resumen", () => true);
  await assert.rejects(resumen(), /No pudimos conectar.*volvé a intentar/);
  fetch.mock.mockImplementation(async () => Response.json({ mensaje: "El trabajo ya tiene un pedido." }, { status: 409 }));
  await assert.rejects(resumen(), /El trabajo ya tiene un pedido/);
});
