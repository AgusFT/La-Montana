import test from 'node:test';
import assert from 'node:assert/strict';
import { promises as fs } from 'node:fs';
import { createHash } from 'node:crypto';
import os from 'node:os';
import path from 'node:path';
import { gzipSync } from 'node:zlib';
import { install, inventory, removeKnown } from '../../../nativo/archivos.mjs';

const digest = bytes => createHash('sha256').update(bytes).digest('hex');
const list = records => new Map(Object.entries(records).map(([name,bytes]) => [name,digest(bytes)]));
async function put(base, name, bytes) {
  const file = path.join(base,name);
  await fs.mkdir(path.dirname(file),{recursive:true});
  await fs.writeFile(file,bytes);
}
async function sandbox(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(),'montana-instalar-'));
  t.after(()=>fs.rm(root,{recursive:true,force:true}));
  return root;
}

test('conserva datos ajenos en carpeta con espacios y tildes al actualizar, desinstalar y reinstalar', async t => {
  const temp = await sandbox(t);
  const base = path.join(temp,'Sofía López','Aplicación');
  const previous = {'panel.jar':'anterior','pg/servidor.exe':'anterior'};
  const next = {'panel.jar':'nuevo','otro/servidor.exe':'nuevo'};
  const extract = async stage => { for (const [name,bytes] of Object.entries(next)) await put(stage,name,bytes); };
  for (const [name,bytes] of Object.entries(previous)) await put(base,'app/'+name,bytes);
  await put(base,'app/mis archivos/decisión.pdf','acuerdo');
  await put(base,'app/pg/nota.txt','comentario');
  await fs.mkdir(path.join(base,'app/mis carpetas vacías'));
  await put(base,'contrato del usuario.pdf','documento');
  await put(base,'nueva/documento.txt','ajeno');
  await install(base,list(previous),extract);
  assert.equal(await fs.readFile(path.join(base,'app/panel.jar'),'utf8'),'nuevo');
  await assert.rejects(fs.stat(path.join(base,'app/pg/servidor.exe')),{code:'ENOENT'});
  await removeKnown(path.join(base,'app'),list(next));
  for (const [name,expected] of Object.entries({'contrato del usuario.pdf':'documento','nueva/documento.txt':'ajeno',
      'app/mis archivos/decisión.pdf':'acuerdo','app/pg/nota.txt':'comentario'}))
    assert.equal(await fs.readFile(path.join(base,name),'utf8'),expected);
  assert.ok((await fs.stat(path.join(base,'app/mis carpetas vacías'))).isDirectory());
  await install(base,new Map(),extract);
  assert.equal(await fs.readFile(path.join(base,'app/mis archivos/decisión.pdf'),'utf8'),'acuerdo');
  await put(base,'app/panel.jar','copia intervenida');
  await removeKnown(path.join(base,'app'),list(next));
  assert.equal(await fs.readFile(path.join(base,'app/panel.jar'),'utf8'),'copia intervenida');
});

test('una colisión o error de extracción conserva la instalación anterior y se puede reintentar', async t => {
  const base = await sandbox(t);
  const known = list({'panel.jar':'v1'});
  await put(base,'app/panel.jar','v1');
  await put(base,'app/tarifa','propietario');
  await assert.rejects(install(base,known,async stage=>{
    await put(stage,'tarifa','paquete'); await put(stage,'panel.jar','v2');
  }), /archivo personal/);
  await assert.rejects(install(base,known,async stage=>{
    await put(stage,'panel.jar','incompleto'); throw Error('interrupción');
  }),/interrupción/);
  assert.equal(await fs.readFile(path.join(base,'app/panel.jar'),'utf8'),'v1');
  assert.equal(await fs.readFile(path.join(base,'app/tarifa'),'utf8'),'propietario');
  assert.deepEqual(await fs.readdir(base),['app']);
  await install(base,known,stage=>put(stage,'panel.jar','v2'));
  assert.equal(await fs.readFile(path.join(base,'app/panel.jar'),'utf8'),'v2');
});

test('recupera la edición anterior si Windows cerró después de apartar app', async t => {
  const base = await sandbox(t);
  const etapa = '.yg-preparacion-a134bc';
  await put(base,etapa+'-anterior/panel.jar','v1');
  await put(base,etapa+'-anterior/mío','documento');
  await fs.writeFile(path.join(base,'.yg-actualizacion-demo.json'),JSON.stringify({etapa}));
  await install(base,list({'panel.jar':'v1'}),stage=>put(stage,'panel.jar','v2'));
  assert.equal(await fs.readFile(path.join(base,'app/mío'),'utf8'),'documento');
});

test('un enlace no autoriza retirar contenido de otro directorio', async t=>{
  const base=await sandbox(t), expected='documento';
  await put(base,'externa/archivo',expected);
  await put(base,'app/panel.jar','v1');
  await fs.symlink(path.join(base,'externa'),path.join(base,'app/enlace'),'junction');
  await assert.rejects(removeKnown(path.join(base,'app'),list({'enlace/archivo':expected,'panel.jar':'v1'})),/enlace/);
  await assert.rejects(install(base,list({'panel.jar':'v1'}),stage=>put(stage,'panel.jar','v2')),/enlace/);
  assert.equal(await fs.readFile(path.join(base,'externa/archivo'),'utf8'),expected);
  assert.equal(await fs.readFile(path.join(base,'app/panel.jar'),'utf8'),'v1');
});

test('los inventarios de las ediciones publicadas tienen el panel, Node y Java; rechaza rutas externas', async()=>{
  for (const release of ['0.1.5','0.1.6']) {
    const data=await fs.readFile(new URL('../../../nativo/archivos-'+release+'.sha256.gz',import.meta.url));
    const known=inventory(data);
    for (const key of ['panel.jar','node/node.exe','java/bin/javaw.exe']) assert.match(known.get(key),/^[0-9a-f]{64}$/);
  }
  assert.throws(()=>inventory(gzipSync(digest('')+'  ../archivo\n')),/inválido/);
});
