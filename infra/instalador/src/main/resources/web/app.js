"use strict";
const $=s=>document.querySelector(s), $$=s=>[...document.querySelectorAll(s)];
const token=location.hash.slice(1);history.replaceState(null,"",location.pathname);
const titulos={equipo:"Verificar equipo",destino:"Elegir instalación",opciones:"Modo y acceso",propietario:"Propietario",soporte:"Soporte",operacion:"Elegir operación",respaldo:"Respaldo",revision:"Revisar y ejecutar",red:"Acceso de red","standard-config":"Dominio y correo","standard-arranque":"Arranque automático","standard-publico":"Acceso público","standard-correo":"Probar correo",resultado:"Resultado"};
const completos=new Set();let actual="equipo",app=null,carpeta="",versionApp="",sistema="",flujo="nueva",ejecutado=false,ocupado=false,cerrado=false;
let directorioEditado=false;
const nuevos=["opciones","propietario","soporte","revision"];
function incompleta(){return app?.existe&&["PREPARADA","CUENTAS_PENDIENTES"].includes(app.estado);}
function modo(){return app?.existe?app.modo:$("#modo").value;}
function necesitaRed(){return modo()==="STANDARD"||(app?.existe?app.bind&&app.bind!=="127.0.0.1":$("#lan").checked);}
function ruta(){
 const pasos=["equipo","destino"];
 if(flujo==="nueva")pasos.push(...nuevos);
 else {pasos.push("operacion");if(["reparar","actualizar"].includes($("#operacion-elegida").value))pasos.push("respaldo","revision");}
 const acceso=flujo==="nueva"||$("#operacion-elegida").value==="acceso";
 if(acceso&&necesitaRed())pasos.push("red");
 if(acceso&&modo()==="STANDARD")pasos.push("standard-config","standard-arranque","standard-publico","standard-correo");
 pasos.push("resultado");return pasos;
}
function accesible(id){const r=ruta(),i=r.indexOf(id);return i>=0&&r.slice(0,i).every(p=>completos.has(p));}
function mensaje(texto,error=false){$("#estado").textContent=texto;$("#estado").classList.toggle("error",error);$("#error-paso").hidden=!error;$("#error-paso").textContent=error?texto:"";}
function texto(root,tag,value,cls){const n=document.createElement(tag);n.textContent=value;if(cls)n.className=cls;root.append(n);return n;}
function datos(id){return Object.fromEntries(new FormData($("#"+id)));}
function limpiarClaves(){$$('input[type="password"]').forEach(n=>n.value="");}
function invalidar(id){const r=ruta(),i=r.indexOf(id);r.slice(i).forEach(p=>completos.delete(p));if(id==="equipo")estadoDependencias(false);render();}
async function api(path,datos){const r=await fetch(`/api/${path}`,{method:datos?"POST":"GET",headers:{Authorization:`Bearer ${token}`},body:datos?new URLSearchParams(datos):undefined,credentials:"omit"});const value=await r.json();if(!r.ok)throw new Error(value.error);return value;}
async function accion(fn){
 if(ocupado||cerrado)return;ocupado=true;mensaje("Comprobando…");$("#progreso").hidden=false;
 const prev=$$("button,#directorio").map(n=>[n,n.disabled]);prev.forEach(([n])=>n.disabled=true);$$("form").forEach(n=>n.inert=true);render();
 try{await fn();}catch(e){mensaje(e.message,true);}finally{ocupado=false;prev.forEach(([n,disabled])=>n.disabled=disabled);$$("form").forEach(n=>n.inert=false);$("#progreso").hidden=true;render();}
}
async function esperar(){for(;;){const v=await api("trabajo");mensaje(v.mensaje||v.estado);if(v.estado==="ERROR")throw new Error(v.mensaje);if(v.estado!=="EJECUTANDO")return;await new Promise(r=>setTimeout(r,1500));}}
async function ejecutar(path,data){await api(path,data);await esperar();await refrescar();}
function mostrarModo(){
 const valor=$("#modo").value;
 for(const n of $$("[data-modo]")){n.hidden=n.dataset.modo!==valor;for(const f of n.querySelectorAll("input,select"))f.disabled=n.hidden;}
 const lan=valor==="DESARROLLO"&&$("#lan").checked;$("#campo-ip").hidden=!lan;$("#ipLan").disabled=!lan;$("#ipLan").required=lan;
 $("#descripcion-modo").textContent=valor==="DESARROLLO"?"Pruebas en esta PC, correos en Mailpit y simulaciones identificadas. No publica la aplicación en Internet.":"Alojamiento público con datos nuevos, HTTPS y correo real. Si falta conexión, las comprobaciones quedan pendientes.";
 if(app?.existe)$("#modo").disabled=true;
}
function mostrarSoporte(){
 const crear=$("#crear-soporte").checked;$("#datos-soporte").hidden=!crear;
 for(const n of $$("#datos-soporte input")){n.disabled=!crear||ejecutado;if(!crear){n.value="";n.setCustomValidity("");n.removeAttribute("aria-invalid");}}
}
function render(){
 const r=ruta();if(!r.includes(actual))actual=r.find(p=>!completos.has(p))||"resultado";
 $$('[data-paso]').forEach(n=>n.hidden=n.dataset.paso!==actual);
 const mapa=$("#mapa");mapa.replaceChildren();
 r.forEach((id,i)=>{const li=document.createElement("li"),b=document.createElement("button");b.type="button";b.disabled=ocupado||cerrado||!accesible(id);b.classList.toggle("completo",completos.has(id));b.setAttribute("aria-label",`${i+1}. ${titulos[id]}${completos.has(id)?", completado":""}`);if(id===actual)b.setAttribute("aria-current","step");texto(b,"span",completos.has(id)?"✓":String(i+1),"numero");texto(b,"span",titulos[id],"nombre");b.onclick=()=>ir(id);li.append(b);mapa.append(li);});
 const seleccionado=mapa.querySelector("[aria-current=step]");
 if(seleccionado&&mapa.scrollWidth>mapa.clientWidth){const b=seleccionado.getBoundingClientRect(),m=mapa.getBoundingClientRect();if(b.right>m.right)mapa.scrollLeft+=b.right-m.right+8;if(b.left<m.left)mapa.scrollLeft+=b.left-m.left-8;}
 const i=r.indexOf(actual);$("#contador").textContent=`Paso ${i+1} de ${r.length} · ${titulos[actual]}`;
 $("#anterior").disabled=ocupado||cerrado||i===0;$("#siguiente").hidden=actual==="resultado";$("#siguiente").disabled=ocupado||cerrado;
 $("#ayuda-navegacion").textContent=completos.has(actual)?"Paso completado. Podés continuar.":"Completá este paso para continuar.";
 $("#ejecutar").disabled=ocupado||cerrado||ejecutado;
 if(app?.existe)$("#modo").disabled=true;
 if(ejecutado)for(const id of nuevos.filter(x=>x!=="revision"))$$(`#${id} input,#${id} select`).forEach(n=>n.disabled=true);
 if(actual==="revision")revision();
 if(actual==="operacion")$("#comparacion").textContent=`App instalada: ${app?.version||"—"}. App incluida: ${versionApp}. La versión del asistente es independiente.`;
 for(const b of $$("#busqueda-resultados button"))b.disabled=ocupado||cerrado;
 if(cerrado)$$('button,input,select').forEach(n=>n.disabled=true);
}
function ir(id){if(ocupado||cerrado||!accesible(id))return;actual=id;cerrarAyuda();$("#error-paso").hidden=true;render();$(`[data-paso="${id}"] h2`).focus();}
function validoFormulario(id){
 let primero=null;
 for(const f of $$("#"+id+" input,#"+id+" select")){
  if(f.disabled)continue;
  f.setCustomValidity("");if(f.required&&!f.value.trim())f.setCustomValidity("Completá este campo.");
  if(f.type==="password"&&(f.value.length<12||f.value.length>128))f.setCustomValidity("Usá entre 12 y 128 caracteres.");
  const ok=f.checkValidity();f.setAttribute("aria-invalid",String(!ok));if(!ok&&!primero)primero=f;
 }
 if(primero){primero.focus();primero.reportValidity();return false;}return true;
}
function validar(){
 if(completos.has(actual)&&ejecutado&&nuevos.includes(actual))return true;
 if(["opciones","propietario","soporte","respaldo"].includes(actual)){
  if(!validoFormulario(actual))return false;
  if(actual==="respaldo"&&!rutaAbsoluta(datos("respaldo").respaldo))throw new Error("Ingresá la ruta completa de una carpeta de respaldos.");
  if(actual==="opciones"&&$("#lan").checked&&modo()==="DESARROLLO"&&!/^\d{1,3}(\.\d{1,3}){3}$/.test($("#ipLan").value))throw new Error("Ingresá una IP de la red local, por ejemplo 192.168.1.10. El servidor comprobará que pertenezca a esta PC.");
  return true;
 }
 if(actual==="operacion"){
  const op=$("#operacion-elegida").value,c=compararVersiones(versionApp,app.version);
  if(op==="reparar"&&c!==0)throw new Error("Para reparar necesitás un paquete con la misma versión de la app instalada.");
  if(op==="actualizar"&&c<=0)throw new Error("Para actualizar necesitás un paquete con una versión de app posterior. Este instalador no rebaja versiones.");
  return true;
 }
 const motivos={equipo:"Verificá el equipo y resolvé los componentes pendientes antes de continuar.",destino:"Ingresá y examiná la carpeta antes de continuar.",revision:"Confirmá la operación y esperá su resultado antes de continuar.",red:"Completá la configuración de red antes de continuar.","standard-config":"Guardá una configuración válida o conservá la configuración ya detectada.","standard-arranque":"Registrá el arranque y comprobalo después de reiniciar. Podés cerrar el asistente y retomar después.","standard-publico":"Confirmá la prueba de acceso externo antes de continuar.","standard-correo":"Confirmá la recepción del correo de prueba antes de continuar."};
 if(!completos.has(actual))throw new Error(motivos[actual]||"Completá este paso.");return true;
}
function siguiente(){try{if(!validar())return;completos.add(actual);const r=ruta();ir(r[r.indexOf(actual)+1]);}catch(e){mensaje(e.message,true);}}
function compararVersiones(a,b){const x=a.split('.').map(Number),y=b.split('.').map(Number);for(let i=0;i<3;i++)if(x[i]!==y[i])return x[i]-y[i];return 0;}
function rutaAbsoluta(s){return sistema.startsWith("Windows")?/^[A-Za-z]:[\\/]/.test(s):s.startsWith("/");}
function revision(){
 const root=$("#revision-datos");root.replaceChildren();
 const agregar=(k,v)=>{const row=document.createElement("div");texto(row,"dt",k);texto(row,"dd",v);root.append(row);};
 agregar("Carpeta",carpeta);agregar("Aplicación incluida",versionApp);agregar("Modo",modo()==="STANDARD"?"Standard":"Desarrollo");
 if(flujo==="nueva"){
  agregar("Operación",incompleta()?"Completar instalación inicial":"Instalar y crear cuentas");
  agregar("Propietario",datos("propietario")["propietario.correo"]||"Datos enviados");agregar("SOPORTE",$("#crear-soporte").checked?(datos("soporte")["soporte.correo"]||"Datos enviados"):"No se creará una cuenta de soporte; podés crearla después desde la app");
  $("#ejecutar").textContent=ejecutado?"Instalación completada":"Instalar y crear cuentas";
  $("#revision-aviso").textContent=$("#crear-soporte").checked?"Se preparará la aplicación y se crearán las cuentas pendientes. El acceso de SOPORTE queda bloqueado hasta su autorización desde la app.":"Se preparará la aplicación con el propietario. No hace falta una cuenta de soporte para usar La Montaña.";
 }else{
  agregar("Operación",$("#operacion-elegida").value==="reparar"?"Reparar":"Actualizar");agregar("Respaldo",datos("respaldo").respaldo||"—");
  $("#ejecutar").textContent=ejecutado?"Mantenimiento completado":"Respaldar y ejecutar mantenimiento";
  $("#revision-aviso").textContent="Se detendrán los servicios y se guardará la copia antes de modificar la app. Si falla el respaldo no se continúa. La copia no incluye una restauración automática.";
 }
}
const nombres={sistema:"Sistema operativo",docker:"Docker Engine",compose:"Docker Compose",paquete:"Archivos del instalador",wsl:"WSL2 y Ubuntu",recursos:"Recursos disponibles",virtualizacion:"Virtualización",dockerWindows:"Docker existente",instalacionAnterior:"Instalación anterior"};
function diagnosticoValido(d){const estados={sistema:["COMPATIBLE"],docker:["DISPONIBLE"],compose:["DISPONIBLE"],paquete:["VERIFICADO","SOLO_APP"],wsl:["WSL2_DISPONIBLE"]};return !estados[d.id]||estados[d.id].includes(d.estado);}
function estadoDependencias(ok){$(".resolver").classList.toggle("verificada",ok);$("#dependencias-ok").hidden=!ok;$("#dependencias-aviso").hidden=!ok;}
async function verificar(){
 invalidar("equipo");const v=await api("dependencias"),root=$("#dependencias");root.replaceChildren();
 for(const d of v.dependencias){const row=document.createElement("div");row.className="dependencia"+(diagnosticoValido(d)?"":" pendiente");texto(row,"span",diagnosticoValido(d)?"✓":"!","simbolo");const det=document.createElement("details");const sum=texto(det,"summary",nombres[d.id]||d.id);texto(sum,"small",d.estado.replaceAll("_"," ").toLowerCase());texto(det,"pre",d.detalle);row.append(det);root.append(row);}
 const required=["sistema","docker","compose","paquete",...(sistema.startsWith("Windows")?["wsl"]:[])];
 const ok=required.every(id=>v.dependencias.some(d=>d.id===id&&diagnosticoValido(d)));
 estadoDependencias(ok);if(ok)completos.add("equipo");mensaje(ok?"Equipo verificado. Podés continuar.":"Hay componentes pendientes. Abrí su detalle para resolverlos.",!ok);
}
function enlace(root,label,url){try{const u=new URL(url);if(!["http:","https:"].includes(u.protocol))return;const a=texto(root,"a",label);a.href=u.href;a.target="_blank";a.rel="noreferrer";}catch{}}
function mostrarResumen(root){
 root.replaceChildren();if(!app?.existe){texto(root,"p","No se encontró una instalación identificada. Si la carpeta está vacía, podés preparar una nueva.","explicacion");return;}
 texto(root,"h3",`${app.modo==="STANDARD"?"Standard":"Modo Desarrollo"} · App ${app.version}`);
 texto(root,"p",`Instalación: ${app.estado.replaceAll("_"," ").toLowerCase()}. Servicios: ${app.disponible?"saludables":"detenidos o con comprobaciones pendientes"}.`);
 if(app.url&&app.disponible)enlace(root,"Abrir La Montaña ↗",app.url);
 if(app.urlMailpit&&app.disponible){const p=document.createElement("p");enlace(p,"Ver correos de prueba en Mailpit ↗",app.urlMailpit);root.append(p);}
 if(app.modo==="STANDARD")texto(root,"p",`Acceso público: ${app.publicoConfirmadoEn?"confirmado":"pendiente"}. SMTP: ${app.smtpConfirmadoEn?"confirmado":"pendiente"}. Arranque tras reinicio: ${app.arranqueVerificado==="true"?"comprobado":"pendiente"}.`,"aviso");
 const det=document.createElement("details");texto(det,"summary","Estado de los servicios");texto(det,"pre",app.servicios||"Sin diagnóstico disponible.");root.append(det);
}
async function refrescar(){app=await api("instalacion");mostrarResumen($("#resumen"));mostrarResumen($("#resultado"));const p=$("#enlace-publico");p.replaceChildren();if(app.pruebaPublica)enlace(p,app.pruebaPublica,app.pruebaPublica);if(app.publicoConfirmadoEn)completos.add("standard-publico");else completos.delete("standard-publico");if(app.smtpConfirmadoEn)completos.add("standard-correo");else completos.delete("standard-correo");if(app.arranqueVerificado==="true")completos.add("standard-arranque");else completos.delete("standard-arranque");if(app.publicacionConfigurada)completos.add("standard-config");$("#configuracion-existente").hidden=!app.publicacionConfigurada;}
$("#seleccionar").onclick=()=>accion(async()=>{
 const dir=$("#directorio").value.trim();if(!rutaAbsoluta(dir))throw new Error("Ingresá la ruta completa de la carpeta.");
 app=await api("seleccionar",{directorio:dir});carpeta=dir;ejecutado=false;limpiarClaves();invalidar("destino");flujo=app.existe&&!incompleta()?"existente":"nueva";
 $("#operacion-elegida").value="ver";
 for(const id of nuevos.filter(x=>x!=="revision"))$$(`#${id} input,#${id} select`).forEach(n=>n.disabled=false);
 $("#opcion-acceso").hidden=!app.existe||!(app.modo==="STANDARD"||app.bind&&app.bind!=="127.0.0.1");
 if(app.existe){$("#modo").value=app.modo;$("#puerto").value=app.puerto||3000;$("#lan").checked=!!app.bind&&app.bind!=="127.0.0.1";$("#ipLan").value=app.bind||"";}
 mostrarModo();mostrarSoporte();completos.add("destino");await refrescar();mensaje("Carpeta examinada. Revisá lo detectado y continuá.");
});
$("#directorio").oninput=()=>{directorioEditado=true;if(carpeta||app){invalidar("destino");carpeta="";app=null;ejecutado=false;flujo="nueva";limpiarClaves();$("#resumen").replaceChildren();for(const id of nuevos.filter(x=>x!=="revision"))$$("#"+id+" input,#"+id+" select").forEach(n=>n.disabled=false);mostrarModo();mostrarSoporte();render();}};
function elegirDirectorio(dir){$("#directorio").value=dir;$("#directorio").dispatchEvent(new Event("input",{bubbles:true}));mensaje("Carpeta elegida. Pulsá «Comprobar carpeta» para revisar su contenido.");}
function mostrarBusqueda(v){
 const info=$("#busqueda-estado");info.hidden=false;
 info.textContent=v.mensaje+(v.entradas!==undefined?" Entradas revisadas: "+v.entradas+".":"")+(v.parcial?" La búsqueda fue parcial; podés ampliarla a otra carpeta o disco.":"");
 const root=$("#busqueda-resultados");root.replaceChildren();
 if(v.estado==="EJECUTANDO")return;
 for(const r of v.resultados||[]){
  const item=document.createElement("article");item.className="instalacion-encontrada";
  const tipos={IDENTIFICADA:"Instalación identificada",INCOMPLETA:"Instalación incompleta",INCONSISTENTE:"Carpeta que requiere revisión",SCRIPT_ANTERIOR:"Instalación del script anterior",REFERENCIA:"Referencia de Docker"};
  texto(item,"h3",tipos[r.tipo]||"Carpeta encontrada");texto(item,"p",r.directorio,"ruta-encontrada");
  if(r.version)texto(item,"p","Aplicación "+r.version+" · "+(r.modo==="STANDARD"?"Standard":"Desarrollo"));
  texto(item,"p",r.detalle);
  if(r.elegible){const b=texto(item,"button","Usar esta carpeta","secundario");b.type="button";b.disabled=ocupado;b.onclick=()=>{elegirDirectorio(r.directorio);$("#directorio").focus();};}
  root.append(item);
 }
 if(v.revisadas?.length||Object.keys(v.omisiones||{}).length){
  const d=document.createElement("details");texto(d,"summary","Ubicaciones revisadas y alcance");
  if(v.revisadas?.length){const lista=document.createElement("ul");for(const p of v.revisadas)texto(lista,"li",p);d.append(lista);}
  for(const [motivo,cantidad] of Object.entries(v.omisiones||{}))texto(d,"p",motivo+": "+cantidad);
  root.append(d);
 }
}
async function tareaCarpetas(path,data){
 await api("carpetas/"+path,data);const cancelar=$("#cancelar-carpetas");cancelar.hidden=false;cancelar.disabled=false;
 try{for(;;){
  const v=await api("carpetas/estado");
  if(v.tipo==="BUSQUEDA")mostrarBusqueda(v);mensaje(v.mensaje||"Comprobando carpetas…");
  if(v.estado==="ERROR")throw new Error(v.mensaje);
  if(v.estado!=="EJECUTANDO")return v;
  await new Promise(r=>setTimeout(r,250));
 }}finally{if(document.activeElement===cancelar)setTimeout(()=>$("#buscar-instalaciones").focus({preventScroll:true}),0);cancelar.hidden=true;cancelar.disabled=false;}
}
$("#examinar-carpeta").onclick=()=>accion(async()=>{const v=await tareaCarpetas("examinar",{directorio:$("#directorio").value});if(v.estado==="COMPLETADO")elegirDirectorio(v.directorio);});
$("#buscar-instalaciones").onclick=()=>accion(()=>tareaCarpetas("buscar",{}));
$("#ampliar-busqueda").onclick=()=>accion(async()=>{const v=await tareaCarpetas("examinar",{directorio:$("#directorio").value});if(v.estado==="COMPLETADO")await tareaCarpetas("buscar",{raiz:v.directorio});});
$("#cancelar-carpetas").onclick=async()=>{const b=$("#cancelar-carpetas");b.disabled=true;try{await api("carpetas/cancelar",{});}catch(e){mensaje(e.message,true);b.disabled=false;}};

for(const id of ["opciones","propietario","soporte","operacion","respaldo"])$("#"+id).addEventListener("input",()=>{if(id==="opciones")mostrarModo();if(id==="soporte")mostrarSoporte();invalidar(id);});
for(const f of $$("form[novalidate]"))f.onsubmit=e=>{e.preventDefault();siguiente();};
$("#modo").onchange=()=>{mostrarModo();invalidar("opciones");};$("#lan").onchange=()=>{mostrarModo();invalidar("opciones");};
$("#operacion-elegida").onchange=()=>{ejecutado=false;invalidar("operacion");};
$("#verificar").onclick=()=>accion(verificar);
$("#resolver").onclick=()=>accion(async()=>{invalidar("equipo");await api("dependencia",{accion:$("#dependencia").value,via:$("#via").value});await esperar();await verificar();});
$("#anterior").onclick=()=>ir(ruta()[ruta().indexOf(actual)-1]);$("#siguiente").onclick=siguiente;
$("#ejecutar").onclick=()=>accion(async()=>{
 if(ejecutado||!accesible("revision"))throw new Error("Volvé a completar los pasos previos.");
 let data,path;
 if(flujo==="nueva"){
  for(const id of nuevos.filter(x=>x!=="revision"))if(!validoFormulario(id)){actual=id;throw new Error("Revisá los campos del paso indicado.");}
  data={...datos("opciones"),...datos("propietario"),...datos("soporte"),modo:modo()};path="instalar";
 }else{data={...datos("operacion"),...datos("respaldo")};path="mantener";}
 limpiarClaves();
 try{await ejecutar(path,data);ejecutado=true;completos.add("revision");mensaje("Operación completada. Podés continuar con las comprobaciones finales.");}
 catch(e){await refrescar().catch(()=>{});if(flujo==="nueva"){completos.delete("propietario");completos.delete("soporte");completos.delete("revision");actual="propietario";}throw e;}
});
for(const b of $$('[data-sistema]'))b.onclick=()=>accion(async()=>{await ejecutar("sistema",{accion:b.dataset.sistema});if(b.dataset.sistema==="red")completos.add("red");mensaje("Operación de sistema completada. Revisá el estado para continuar.");});
$("#probar-publico").onclick=()=>accion(()=>ejecutar("publico/iniciar",{}));
for(const [id,path] of [["configurar-publicacion","publico/configurar"],["confirmar-publico","publico/confirmar"],["enviar-correo","correo/enviar"],["confirmar-correo","correo/confirmar"]])$("#"+id).onsubmit=e=>{e.preventDefault();const data=datos(id);$$(`#${id} input[type=password]`).forEach(n=>n.value="");accion(async()=>{await ejecutar(path,data);if(id==="configurar-publicacion")completos.add("standard-config");});};
$("#configurar-publicacion").addEventListener("input",()=>invalidar("standard-config"));
$("#actualizar-estado").onclick=()=>accion(async()=>{await refrescar();mensaje("Estado comprobado.");});
$("#cerrar").onclick=()=>accion(async()=>{const v=await api("cerrar",{});limpiarClaves();cerrado=true;mensaje(v.mensaje);});
window.addEventListener("pagehide",limpiarClaves);

const ayudas={
 dependencias:["Dependencias","Son las herramientas que necesita La Montaña para funcionar. El instalador comprueba si están disponibles y te indica cómo resolver las que falten. Revisar no modifica tu equipo."],
 docker:["Docker y Compose","Docker ejecuta los servicios de La Montaña en entornos separados llamados contenedores. Compose los coordina: base de datos, web, backend y otros componentes. Un Docker detenido no significa que falte instalarlo."],
 wsl:["WSL2","Es la función de Windows que permite ejecutar Linux. El instalador usa una Ubuntu propia llamada LaMontana-Ubuntu para alojar sus servicios, sin reemplazar tus otras distribuciones."],
 carpeta:["Carpeta de la aplicación","Es la ubicación de los archivos de configuración de esta instalación. Para una nueva, elegí una carpeta vacía. Para revisar o mantener una existente, indicá la carpeta que elegiste al instalarla. Los datos de la base se guardan en volúmenes Docker."],
 modo:["Modo de uso","Desarrollo sirve para probar localmente: utiliza correos de prueba y simulaciones identificadas. Standard aloja la app con acceso público, HTTPS y correo real. Una base de pruebas no se convierte automáticamente en Standard."],
 puerto:["Puerto","Es el número que identifica el acceso a la web dentro de tu PC; por ejemplo, http://127.0.0.1:3000. Elegí uno libre entre 1024 y 65535. Si ya está ocupado, la instalación pedirá otro."],
 lan:["Red local (LAN)","Permite que otros equipos de la misma red accedan a La Montaña. Requiere la IP local de esta PC y configurar su firewall en un paso posterior. No publica la app en Internet."],
 ip:["Dirección IP local","Identifica a esta PC dentro de tu red, por ejemplo 192.168.1.10. Debe pertenecer a una interfaz de esta computadora. Si cambia, el acceso puede dejar de funcionar hasta revisar la configuración."],
 propietario:["Propietario","Es la cuenta responsable del negocio. Usá un correo propio y una contraseña segura; esos datos se usarán para ingresar a La Montaña. Navegar por estos pasos todavía no crea la cuenta."],
 soporte:["SOPORTE de YG-Soluciones","Es una cuenta opcional y reservada para asistencia dentro de la app. Podés crearla ahora o después en Administración → Asistencia de YG Soluciones, confirmando la contraseña de un administrador del negocio. Queda bloqueada inicialmente. Un administrador puede autorizarla por hasta 24 horas y revocarla; sus acciones se auditan. No concede control remoto de esta PC."],
 mantenimiento:["Reparar o actualizar","Reparar repone componentes de la misma versión. Actualizar usa la versión de app incluida en este paquete, que debe ser posterior. Ambas operaciones conservan datos y exigen respaldo previo; no rebajan versiones."],
 respaldo:["Respaldo bruto","Es una copia de datos, archivos, configuración e imágenes con los servicios detenidos. Incluye credenciales: guardala en una carpeta privada fuera de la app. Se comprueba su integridad, pero esta versión aún no incorpora un procedimiento de restauración."],
 red:["Firewall y acceso","El firewall controla las conexiones que entran a la computadora. Esta acción habilita únicamente los puertos del acceso elegido; en Windows también prepara su reenvío a WSL2. No cambia tu router ni la configuración DNS."],
 dominio:["Dominio","Es el nombre público de tu aplicación, por ejemplo app.ejemplo.com. Se ingresa sin https://, puerto ni ruta. Debés disponer del dominio y configurar su DNS para que apunte a tu conexión."],
 smtp:["SMTP","Es el servicio que envía los correos reales de la app. Tu proveedor entrega servidor, puerto, usuario y contraseña. La prueba se completa al confirmar el código recibido. En Desarrollo se usa Mailpit, una bandeja local que no envía correos reales."],
 tls:["TLS y STARTTLS","Ambos cifran la conexión de correo. STARTTLS inicia el cifrado dentro de la conexión; TLS implícito lo usa desde el comienzo. Elegí la opción y el puerto indicados por tu proveedor."],
 arranque:["Arranque automático","Permite que Standard funcione después de reiniciar, sin iniciar sesión en el escritorio. Registrar la configuración no demuestra que funcione: hay que reiniciar y comprobar el estado. Esto es distinto de abrir este asistente al instalar el paquete."],
 https:["HTTPS","Cifra el acceso a la web y utiliza un certificado válido para tu dominio. La comprobación desde un celular con datos móviles confirma que se puede acceder desde fuera de tu red local."],
 dns:["DNS y CGNAT","DNS relaciona tu dominio con una IP pública. CGNAT es una configuración del proveedor que puede impedir recibir conexiones desde Internet. Si no podés dirigir tráfico a tu equipo, necesitás consultar a tu proveedor; el asistente no puede resolverlo por sí solo."]
};
let ayudaActiva=null,ayudaFija=false,temporizador;
function cerrarAyuda(){clearTimeout(temporizador);if(ayudaActiva){ayudaActiva.setAttribute("aria-expanded","false");ayudaActiva.removeAttribute("aria-describedby");}ayudaActiva=null;ayudaFija=false;$("#ayuda-ampliada").hidden=true;}
function abrirAyuda(b){clearTimeout(temporizador);if(ayudaActiva!==b)cerrarAyuda();ayudaActiva=b;const caja=$("#ayuda-ampliada"),[titulo,detalle]=ayudas[b.dataset.clave];caja.replaceChildren();texto(caja,"strong",titulo);texto(caja,"p",detalle);caja.hidden=false;b.setAttribute("aria-expanded","true");b.setAttribute("aria-describedby","ayuda-ampliada");posicionarAyuda();}
function posicionarAyuda(){if(!ayudaActiva)return;const caja=$("#ayuda-ampliada"),rect=ayudaActiva.getBoundingClientRect(),alto=caja.offsetHeight;if(rect.bottom<0||rect.top>innerHeight){cerrarAyuda();return;}caja.style.left=`${Math.max(12,Math.min(rect.left,innerWidth-caja.offsetWidth-12))}px`;caja.style.top=`${Math.max(12,rect.bottom+alto+16<innerHeight?rect.bottom+8:rect.top-alto-8)}px`;}
function diferirCierre(){clearTimeout(temporizador);temporizador=setTimeout(()=>{if(!ayudaFija&&document.activeElement!==ayudaActiva)cerrarAyuda();},160);}
for(const lugar of $$('[data-ayuda]')){const b=document.createElement("button");b.type="button";b.className="ayuda";b.dataset.clave=lugar.dataset.ayuda;b.textContent="i";b.setAttribute("aria-label",`Más información: ${ayudas[b.dataset.clave][0]}`);b.setAttribute("aria-expanded","false");b.onmouseenter=()=>abrirAyuda(b);b.onfocus=()=>abrirAyuda(b);b.onmouseleave=diferirCierre;b.onblur=diferirCierre;b.onclick=e=>{e.preventDefault();if(ayudaFija&&ayudaActiva===b)cerrarAyuda();else{abrirAyuda(b);ayudaFija=true;}};lugar.append(b);}
$("#ayuda-ampliada").onmouseenter=()=>clearTimeout(temporizador);$("#ayuda-ampliada").onmouseleave=diferirCierre;
document.addEventListener("keydown",e=>{if(e.key==="Escape")cerrarAyuda();});document.addEventListener("click",e=>{if(ayudaActiva&&!e.target.closest(".ayuda,#ayuda-ampliada"))cerrarAyuda();});window.addEventListener("resize",posicionarAyuda);window.addEventListener("scroll",posicionarAyuda);
mostrarModo();render();
api("estado").then(v=>{versionApp=v.version;sistema=v.sistema;$("#versiones").textContent=`${sistema} · Instalador ${v.versionInstalador} · Aplicación incluida ${v.version}`;for(const n of $$('[data-windows]'))n.hidden=!sistema.startsWith("Windows");if(v.directorioRecomendado){$("#ruta-recomendada").textContent="Ruta recomendada: "+v.directorioRecomendado;if(!directorioEditado&&!$("#directorio").value)$("#directorio").value=v.directorioRecomendado;}mensaje("Listo para comenzar. La revisión del equipo es el primer paso.");}).catch(e=>mensaje(e.message,true));
