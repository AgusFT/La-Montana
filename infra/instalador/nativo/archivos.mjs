// El instalador y el desinstalador ejecutan este programa desde su carpeta temporal.
// Los inventarios provienen del EXE. Los archivos ajenos se conservan, incluso dentro de app.
import { createHash } from 'node:crypto';
import { createReadStream, promises as fs } from 'node:fs';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { gunzipSync } from 'node:zlib';
import { pathToFileURL } from 'node:url';

const exists = async file => {
  try { return await fs.lstat(file); }
  catch (err) { if (err.code !== 'ENOENT') throw err; return null; }
};

async function checksum(file) {
  const hash = createHash('sha256');
  for await (const chunk of createReadStream(file)) hash.update(chunk);
  return hash.digest('hex');
}

export function inventory(bytes) {
  const files = new Map();
  for (const line of gunzipSync(bytes).toString('utf8').split('\n')) {
    if (!line) continue;
    const match = /^([a-f0-9]{64})  ([^\\:\r\n]+)$/.exec(line);
    if (!match || match[2].split('/').some(part => !part || part === '.' || part === '..'))
      throw Error('Inventario de componentes inválido.');
    const key = match[2].toLowerCase();
    if (files.has(key)) throw Error('El instalador tiene dos componentes con el mismo nombre.');
    files.set(key, match[1]);
  }
  return files;
}

async function localDirectory(dir) {
  const base = path.resolve(dir);
  const volume = path.parse(base).root;
  if (base === volume || (process.platform === 'win32' && !/^[a-z]:\\/i.test(base)))
    throw Error('Elegí una carpeta de una unidad local, distinta de la raíz.');
  for (let entry = base; entry !== volume; entry = path.dirname(entry)) {
    const info = await exists(entry);
    if (info && (!info.isDirectory() || info.isSymbolicLink()))
      throw Error('Elegí una carpeta local sin enlaces ni uniones de directorio.');
  }
  return base;
}

async function visit(dir, callback, rel = '') {
  for (const name of await fs.readdir(path.join(dir, rel))) {
    const relative = rel ? rel + '/' + name : name;
    const item = await fs.lstat(path.join(dir, relative));
    if (item.isSymbolicLink() || (!item.isFile() && !item.isDirectory()))
      throw Error('Hay un enlace o archivo especial en los componentes. Movelo y reintentá: ' + relative);
    await callback(relative, item);
    if (item.isDirectory()) await visit(dir, callback, relative);
  }
}

async function removeEmpty(dir) {
  try { await fs.rmdir(dir); }
  catch (err) { if (!['ENOTEMPTY','EEXIST','ENOENT'].includes(err.code)) throw err; }
}

// Solo se elimina una ruta enumerada que conserve el contenido que verificamos.
export async function removeKnown(dir, allowed, directories = new Set()) {
  const info = await exists(dir);
  if (!info) return;
  if (!info.isDirectory() || info.isSymbolicLink()) throw Error('Carpeta de componentes inválida.');
  const entries = [];
  for (const key of allowed.keys()) {
    for (let parent = path.posix.dirname(key); parent !== '.'; parent = path.posix.dirname(parent))
      directories.add(parent);
  }
  await visit(dir, (rel, stat) => entries.push({rel, stat}));
  for (const {rel, stat} of entries.reverse()) {
    const location = path.join(dir, rel);
    if (stat.isDirectory()) {
      if (directories.has(rel.toLowerCase())) await removeEmpty(location);
    }
    else {
      const approved = allowed.get(rel.toLowerCase());
      if (approved && await checksum(location) === approved) await fs.unlink(location);
    }
  }
  await removeEmpty(dir);
}

const journalName = '.yg-actualizacion-demo.json';
async function recover(base) {
  const journal = path.join(base, journalName);
  const info = await exists(journal);
  if (!info) return;
  if (!info.isFile() || info.size > 250) throw Error('Hay un archivo ajeno en la ubicación del registro de instalación.');
  const id = JSON.parse(await fs.readFile(journal, 'utf8')).etapa;
  if (!/^\.yg-preparacion-[a-zA-Z0-9]{6}$/.test(id)) throw Error('El registro de instalación es inválido.');
  const backup = path.join(base, id + '-anterior');
  const app = path.join(base, 'app');
  const previous = await exists(backup);
  if (!(await exists(app)) && previous?.isDirectory() && !previous.isSymbolicLink()) await fs.rename(backup, app);
  // Las carpetas de una operación interrumpida pueden incluir archivos del usuario.
  // Nunca se borran automáticamente a partir del nombre guardado en el registro.
  await fs.unlink(journal);
}

export async function install(base, old, extract, requiredBytes = 0) {
  base = await localDirectory(base);
  await fs.mkdir(base, {recursive:true});
  const space = await fs.statfs(base);
  if (space.bavail * space.bsize < requiredBytes) throw Error('No hay espacio suficiente en la unidad elegida.');
  await recover(base);
  const app = path.join(base, 'app');
  const prior = await exists(app);
  if (prior && (!prior.isDirectory() || prior.isSymbolicLink())) throw Error('El destino app ya está ocupado.');
  if (!old.size && await exists(path.join(base,'Desinstalar.exe')))
    throw Error('Hay otro desinstalador en esta carpeta. Elegí una carpeta nueva.');
  const stage = await fs.mkdtemp(path.join(base,'.yg-preparacion-'));
  const backup = stage + '-anterior';
  const journal = path.join(base,journalName);
  let activated = false;
  const preserved = new Map(old);
  const copiedDirectories = new Set();
  try {
    await extract(stage);
    if (prior) {
      await visit(app, async (rel, stat) => {
        const source = path.join(app,rel), target = path.join(stage,rel);
        if (stat.isDirectory()) {
          await fs.mkdir(target,{recursive:true});
          copiedDirectories.add(rel.toLowerCase());
          return;
        }
        const digest = await checksum(source);
        const key = rel.toLowerCase();
        if (old.get(key) === digest) return;
        const occupied = await exists(target);
        if (occupied) {
          if (!occupied.isFile() || await checksum(target) !== digest)
            throw Error('Un archivo personal ocupa el lugar de un componente. Movelo y reintentá: ' + rel);
        } else await fs.copyFile(source,target,fs.constants.COPYFILE_EXCL);
        preserved.set(key,digest);
      });
    }
    if (await exists(backup)) throw Error('Ya existe la carpeta de recuperación.');
    await fs.writeFile(journal, JSON.stringify({etapa:path.basename(stage)}), {flag:'wx'});
    if (prior) await fs.rename(app,backup);
    try { await fs.rename(stage,app); activated = true; }
    catch (err) { if (prior) await fs.rename(backup,app); throw err; }
    await fs.unlink(journal);
  } finally {
    if (!activated) await fs.rm(stage,{recursive:true,force:true});
  }
  if (prior) {
    try { await removeKnown(backup,preserved,copiedDirectories); }
    catch (err) { console.error('Se conservó la carpeta de la versión anterior: ' + backup); }
  }
}

async function run([action, root, activeManifest, zip, size]) {
  const known = activeManifest === '-' ? new Map() : inventory(await fs.readFile(activeManifest));
  if (action === 'validar') {
    const base = await localDirectory(root);
    await recover(base);
    const app = path.join(base,'app');
    if (await exists(app)) {
      await localDirectory(app);
      for (const name of ['panel.jar','java/bin/javaw.exe']) {
        const file = path.join(app,name);
        await localDirectory(path.dirname(file));
        const info = await exists(file);
        if (info && (!info.isFile() || await checksum(file) !== known.get(name)))
          throw Error('No se reconoce el componente instalado. Mové el archivo antes de reparar: ' + name);
      }
    }
    return;
  }
  if (action === 'retirar') {
    const base = await localDirectory(root);
    await recover(base);
    return removeKnown(path.join(base,'app'), known);
  }
  if (action !== 'instalar') throw Error('Operación desconocida.');
  return install(root, known, async stage => {
    const tar = path.join(process.env.SystemRoot, 'System32','tar.exe');
    const result = spawnSync(tar, ['-xf', zip, '-C', stage], {stdio:'inherit',windowsHide:true});
    if (result.error || result.status !== 0) throw Error('Windows no pudo extraer los componentes.');
    if (!(await exists(path.join(stage,'panel.jar')))?.isFile()) throw Error('Falta el panel de la demo.');
  },Number(size));
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  run(process.argv.slice(2)).catch(err => { console.error(err.message); process.exitCode=1; });
}
