const { readFileSync } = require("node:fs");
const assert = require("node:assert/strict");
const { test } = require("node:test");
const Module = require("node:module");
const ts = require("typescript");

function loadProxy() {
  const source = readFileSync("src/app/api/[...path]/route.ts", "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  });
  const proxy = new Module("demo-session-proxy", module);
  proxy.require = (id) => id === "@/lib/organization-types" ? { uuidPattern: /^[0-9a-f-]{36}$/i } : require(id);
  proxy._compile(compiled.outputText, "demo-session-proxy");

  return proxy.exports;
}

function mockBackend(t) {
  const originalBackend = process.env.BACKEND_INTERNAL_URL;
  process.env.BACKEND_INTERNAL_URL = "http://backend.test";
  t.after(() => {
    if (originalBackend === undefined) delete process.env.BACKEND_INTERNAL_URL;
    else process.env.BACKEND_INTERNAL_URL = originalBackend;
  });

}

test("el proxy de Demo sólo acepta POST de sesión y conserva la protección CSRF y la cookie", async (t) => {
  const proxy = loadProxy();
  mockBackend(t);
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
  const denied = await proxy.GET(new Request(url), context);
  assert.equal(denied.status, 405);
  assert.equal(fetch.mock.callCount(), 0);

  const response = await proxy.POST(new Request(url, {
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

test("el proxy de cupos admite las rutas de inventario, alta y baja y comunica el cupo lleno", async (t) => {
  const proxy = loadProxy();
  mockBackend(t);
  const id = "a2662894-c40c-4e31-9041-3daa40947bc8";
  const fetch = t.mock.method(globalThis, "fetch", async () => Response.json(
    { mensaje: "Se alcanzó el cupo de clientes.", codigo: "CUPO_DEMO" }, { status: 409 }
  ));
  async function request(method, ruta) {
    const url = "http://app.test/api/" + ruta;
    return proxy[method](new Request(url, { method }), { params: Promise.resolve({ path: ruta.split("/") }) });
  }
  assert.equal((await request("POST", "demo/cupos")).status, 405);
  assert.equal((await request("GET", "demo/clientes")).status, 405);
  assert.equal((await request("POST", `demo/cupos/CAJA/${id}/baja`)).status, 404);
  assert.equal((await request("POST", "demo/cupos/CLIENTE/1/baja")).status, 404);
  assert.equal(fetch.mock.callCount(), 0);
  const create = await request("POST", "demo/clientes");
  assert.equal(create.status, 409);
  assert.deepEqual(await create.json(), { mensaje: "Se alcanzó el cupo de clientes.", codigo: "CUPO_DEMO" });
  await request("GET", "demo/cupos");
  for (const tipo of ["CLIENTE", "PEDIDO", "COBRO", "DEVOLUCION", "IMPRESORA"]) {
    await request("POST", `demo/cupos/${tipo}/${id}/baja`);
  }
  assert.equal(fetch.mock.callCount(), 7);
});

test("el proxy de Demo conserva la vigencia pública y excluye las operaciones de los módulos de presentación", async t => {
  const proxy = loadProxy();
  mockBackend(t);
  const estado = {estado:"VIGENTE",diasRestantes:1,restanteMillis:999,inicio:null,venceEn:null,mensaje:""};
  const backend = t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "http://backend.test/api/demo/vigencia");
    assert.equal(options.cache, "no-store");
    return Response.json(estado);
  });
  async function request(method, path) {
    return proxy[method](new Request("http://app.test/api/" + path, { method }), { params: Promise.resolve({ path:path.split("/") }) });
  }
  const id = "4d9fdf39-1f90-4d08-b7b6-0c77fc9f0079";
  for (const [method,path] of [
    ["GET","admin/empleados"],["POST","admin/empleados"],["PUT","admin/empleados/"+id],
    ["GET","admin/puntos-entrega/disponibilidad"],["PUT","admin/puntos-entrega/disponibilidad/"+id],
    ["POST",`admin/configuracion/borradores/${id}/entrega/puntos`],
    ["PUT",`admin/configuracion/borradores/${id}/entrega/puntos/${id}`],["GET","admin/riders"],
    ["GET","admin/analytics"],["GET","admin/chat-bot"],
  ]) assert.equal((await request(method,path)).status,404,path);
  assert.equal((await request("POST","demo/vigencia")).status,405);
  const response = await request("GET","demo/vigencia");
  assert.deepEqual(await response.json(),estado);
  assert.equal(response.headers.get("Cache-Control"),"no-store");
  assert.equal(backend.mock.callCount(),1);
});
