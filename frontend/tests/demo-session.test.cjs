const { readFileSync } = require("node:fs");
const assert = require("node:assert/strict");
const { test } = require("node:test");
const Module = require("node:module");
const ts = require("typescript");

test("el proxy de Demo sólo acepta POST de sesión y conserva la protección CSRF y la cookie", async (t) => {
  const source = readFileSync("src/app/api/[...path]/route.ts", "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  });
  const proxy = new Module("demo-session-proxy", module);
  proxy.require = (id) => id === "@/lib/organization-types" ? { uuidPattern: /^[0-9a-f-]{36}$/i } : require(id);
  proxy._compile(compiled.outputText, "demo-session-proxy");

  const originalBackend = process.env.BACKEND_INTERNAL_URL;
  process.env.BACKEND_INTERNAL_URL = "http://backend.test";
  t.after(() => {
    if (originalBackend === undefined) delete process.env.BACKEND_INTERNAL_URL;
    else process.env.BACKEND_INTERNAL_URL = originalBackend;
  });

  const body = JSON.stringify({ rol: "EMPLEADO" });
  const fetch = t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "http://backend.test/api/demo/sesion");
    assert.equal(options.method, "POST");
    assert.equal(options.headers.get("X-CSRF-TOKEN"), "token");
    assert.equal(options.headers.get("Cookie"), "DEMOSESSION=anterior");
    assert.equal(options.body, body);
    return Response.json({ mensaje: "Sesión iniciada." }, {
      headers: { "Set-Cookie": "DEMOSESSION=nueva; Path=/; HttpOnly" },
    });
  });

  const url = "http://app.test/api/demo/sesion";
  const context = { params: Promise.resolve({ path: ["demo", "sesion"] }) };
  const denied = await proxy.exports.GET(new Request(url), context);
  assert.equal(denied.status, 405);
  assert.equal(fetch.mock.callCount(), 0);

  const response = await proxy.exports.POST(new Request(url, {
    method: "POST",
    body,
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-TOKEN": "token",
      Cookie: "DEMOSESSION=anterior",
    },
  }), context);
  assert.equal(response.status, 200);
  assert.deepEqual(response.headers.getSetCookie(), ["DEMOSESSION=nueva; Path=/; HttpOnly"]);
  assert.equal(fetch.mock.callCount(), 1);
});
