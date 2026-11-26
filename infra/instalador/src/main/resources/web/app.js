"use strict";
const $=s=>document.querySelector(s), $$=s=>[...document.querySelectorAll(s)];
const token=location.hash.slice(1);history.replaceState(null,"",location.pathname);
const titulos={equipo:"Verificar equipo",destino:"Elegir instalación",opciones:"Acceso local",operacion:"Elegir operación",respaldo:"Respaldo",firmas:"Protección de archivos",revision:"Revisar y ejecutar",resultado:"Resultado"};
const completos=new Set();let actual="equipo",app=null,carpeta="",versionApp="",sistema="",flujo="nueva",ejecutado=false,ocupado=false,cerrado=false;
let directorioEditado=false,firmasExternas=false;
const nuevos=["opciones","revision"];
function incompleta(){return app?.existe&&["PREPARADA","DESINSTALADA"].includes(app.estado);}
function requiereFirmas(){return firmasExternas&&app?.firmasGuardadas!==true;}
function ruta(){
 const pasos=["equipo","destino"];
 if(flujo==="nueva")pasos.push(...nuevos);
 else {
  pasos.push("operacion");
  const op=$("#operacion-elegida").value;
  if(["reparar","actualizar"].includes(op))pasos.push("respaldo");
  if(op!=="ver")pasos.push("revision");
 }
 if(requiereFirmas()&&(flujo==="nueva"||["reparar","actualizar"].includes($("#operacion-elegida").value))){
  pasos.splice(pasos.indexOf("revision"),0,"firmas");
 }
 pasos.push("resultado");return pasos;
}
function accesible(id){const r=ruta(),i=r.indexOf(id);return i>=0&&r.slice(0,i).every(p=>completos.has(p));}
function mensaje(texto,error=false){$("#estado").textContent=texto;$("#estado").classList.toggle("error",error);$("#error-paso").hidden=!error;$("#error-paso").textContent=error?texto:"";}
function texto(root,tag,value,cls){const n=document.createElement(tag);n.textContent=value;if(cls)n.className=cls;root.append(n);return n;}
function datos(id){return Object.fromEntries(new FormData($("#"+id)));}
function invalidar(id){const r=ruta(),i=r.indexOf(id);r.slice(i).forEach(p=>completos.delete(p));if(id==="equipo")estadoDependencias(false);render();}
async function api(path,datos){const r=await fetch(`/api/${path}`,{method:datos?"POST":"GET",headers:{Authorization:`Bearer ${token}`},body:datos?new URLSearchParams(datos):undefined,credentials:"omit"});const value=await r.json();if(!r.ok)throw new Error(value.error);return value;}
async function accion(fn){
 if(ocupado||cerrado)return;ocupado=true;mensaje("Comprobando…");$("#progreso").hidden=false;
 const prev=$$("button,#directorio").map(n=>[n,n.disabled]);prev.forEach(([n])=>n.disabled=true);$$("form").forEach(n=>n.inert=true);render();
 try{await fn();}catch(e){mensaje(e.message,true);}finally{ocupado=false;prev.forEach(([n,disabled])=>n.disabled=disabled);$$("form").forEach(n=>n.inert=false);$("#progreso").hidden=true;render();}
}
async function esperar(){for(;;){const v=await api("trabajo");mensaje(v.mensaje||v.estado);if(v.estado==="ERROR")throw new Error(v.mensaje);if(v.estado!=="EJECUTANDO")return;await new Promise(r=>setTimeout(r,1500));}}
async function ejecutar(path,data){await api(path,data);await esperar();await refrescar();}
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
  const ok=f.checkValidity();f.setAttribute("aria-invalid",String(!ok));if(!ok&&!primero)primero=f;
 }
 if(primero){primero.focus();primero.reportValidity();return false;}return true;
}
function validar(){
 if(completos.has(actual)&&ejecutado&&nuevos.includes(actual))return true;
 if(["opciones","respaldo","firmas"].includes(actual)){
  if(actual==="firmas")$("#archivo-firmas").value=$("#archivo-firmas").value.trim().replace(/^"([^"]+)"$/,"$1");
  if(!validoFormulario(actual))return false;
  if(actual==="firmas"&&$("#via-firmas").value==="archivo"&&!rutaAbsoluta($("#archivo-firmas").value)){
   $("#archivo-firmas").setAttribute("aria-invalid","true");$("#archivo-firmas").focus();
   throw new Error("Ingresá la ruta completa de bytecode.cvd en un disco de esta PC o en un USB.");
  }
  if(actual==="respaldo"&&!rutaAbsoluta(datos("respaldo").respaldo))throw new Error("Ingresá la ruta completa de una carpeta de respaldos.");
  return true;
 }
 if(actual==="operacion"){
  const op=$("#operacion-elegida").value,c=compararVersiones(versionApp,app.version);
  if(op==="reparar"&&c!==0)throw new Error("Para reparar necesitás un paquete con la misma versión de la app instalada.");
  if(op==="actualizar"&&c<=0)throw new Error("Para actualizar necesitás un paquete con una versión de app posterior. Este instalador no rebaja versiones.");
  return true;
 }
 const motivos={equipo:"Verificá el equipo y resolvé los componentes pendientes antes de continuar.",destino:"Ingresá y examiná la carpeta antes de continuar.",revision:"Confirmá la operación y esperá a que termine."};
 if(!completos.has(actual))throw new Error(motivos[actual]||"Completá este paso.");return true;
}
function siguiente(){try{if(!validar())return;completos.add(actual);const r=ruta();ir(r[r.indexOf(actual)+1]);}catch(e){mensaje(e.message,true);}}
function compararVersiones(a,b){const x=a.split('.').map(Number),y=b.split('.').map(Number);for(let i=0;i<3;i++)if(x[i]!==y[i])return x[i]-y[i];return 0;}
function rutaAbsoluta(s){return sistema.startsWith("Windows")?/^[A-Za-z]:[\\/]/.test(s):s.startsWith("/");}
function revision(){
 const root=$("#revision-datos");root.replaceChildren();
 const agregar=(k,v)=>{const row=document.createElement("div");texto(row,"dt",k);texto(row,"dd",v);root.append(row);};
 agregar("Carpeta",carpeta);agregar("Aplicación incluida",versionApp);
 const op=flujo==="nueva"?"instalar":$("#operacion-elegida").value;
 const acciones={instalar:"Instalar la Demo",iniciar:"Iniciar la Demo",desinstalar:"Desinstalar la Demo",reparar:"Reparar la Demo",actualizar:"Actualizar la Demo"};
 agregar("Operación",acciones[op]);
 if(op==="instalar")agregar("Dirección local","http://127.0.0.1:"+$("#puerto").value);
 if(firmasExternas&&["instalar","reparar","actualizar"].includes(op)){
  const origen=!requiereFirmas()?"Se reutilizará la copia verificada de esta instalación.":
   $("#via-firmas").value==="archivo"?$("#archivo-firmas").value:"Descarga oficial de ClamAV con FreshClam.";
  agregar("Firmas del antivirus",origen);
 }
 if(["reparar","actualizar"].includes(op))agregar("Respaldo",datos("respaldo").respaldo);
 $("#ejecutar").textContent=ejecutado?"Operación completada":acciones[op];
 const avisos={instalar:"La imprenta de muestra incluye tres cuentas para entrar desde el selector.",iniciar:"Se iniciarán los servicios locales.",desinstalar:"Se retirarán los servicios de esta instalación. Los pedidos, archivos y fecha de vencimiento se conservarán para reinstalar. Después podés quitar el asistente desde Aplicaciones instaladas en Windows."};
 $("#revision-aviso").textContent=avisos[op]||"Se detendrán los servicios y se verificará el respaldo antes de modificar la Demo.";
 const acceso=$("#acceso-listo");acceso.replaceChildren();
 if(ejecutado&&app?.disponible)enlace(acceso,"Abrir la Demo y elegir rol ↗",app.url);
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
 texto(root,"h3",`La Montaña Demo · App ${app.version}`);
 texto(root,"p",`Instalación: ${app.estado.replaceAll("_"," ").toLowerCase()}. Servicios: ${app.disponible?"saludables":"detenidos o con comprobaciones pendientes"}.`);
 if(app.url&&app.disponible)enlace(root,"Abrir la Demo y elegir rol ↗",app.url);
 const det=document.createElement("details");texto(det,"summary","Estado de los servicios");texto(det,"pre",app.servicios||"Sin diagnóstico disponible.");root.append(det);
}
async function refrescar(){app=await api("instalacion");mostrarResumen($("#resumen"));mostrarResumen($("#resultado"));}
$("#seleccionar").onclick=()=>accion(async()=>{
 const dir=$("#directorio").value.trim();if(!rutaAbsoluta(dir))throw new Error("Ingresá la ruta completa de la carpeta.");
 app=await api("seleccionar",{directorio:dir});carpeta=dir;ejecutado=false;invalidar("destino");flujo=app.existe&&!incompleta()?"existente":"nueva";
 $("#operacion-elegida").value="ver";
 for(const id of nuevos.filter(x=>x!=="revision"))$$(`#${id} input,#${id} select`).forEach(n=>n.disabled=false);
 if(app.existe)$("#puerto").value=app.puerto||3200;$("#puerto").disabled=app.existe;
 completos.add("destino");await refrescar();mensaje("Carpeta examinada. Revisá lo detectado y continuá.");
});
$("#directorio").oninput=()=>{directorioEditado=true;if(carpeta||app){invalidar("destino");carpeta="";app=null;ejecutado=false;flujo="nueva";$("#resumen").replaceChildren();for(const id of nuevos.filter(x=>x!=="revision"))$$("#"+id+" input,#"+id+" select").forEach(n=>n.disabled=false);render();}};
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
  if(r.version)texto(item,"p","La Montaña Demo "+r.version);
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
async function tareaCarpetas(path,data,cancelar=$("#cancelar-carpetas"),foco=$("#buscar-instalaciones")){
 await api("carpetas/"+path,data);cancelar.hidden=false;cancelar.disabled=false;
 try{for(;;){
  const v=await api("carpetas/estado");
  if(v.tipo==="BUSQUEDA")mostrarBusqueda(v);mensaje(v.mensaje||"Comprobando carpetas…");
  if(v.estado==="ERROR")throw new Error(v.mensaje);
  if(v.estado!=="EJECUTANDO")return v;
  await new Promise(r=>setTimeout(r,250));
 }}finally{if(document.activeElement===cancelar)setTimeout(()=>foco.focus({preventScroll:true}),0);cancelar.hidden=true;cancelar.disabled=false;}
}
$("#examinar-carpeta").onclick=()=>accion(async()=>{const v=await tareaCarpetas("examinar",{directorio:$("#directorio").value});if(v.estado==="COMPLETADO")elegirDirectorio(v.directorio);});
$("#buscar-instalaciones").onclick=()=>accion(()=>tareaCarpetas("buscar",{}));
$("#ampliar-busqueda").onclick=()=>accion(async()=>{const v=await tareaCarpetas("examinar",{directorio:$("#directorio").value});if(v.estado==="COMPLETADO")await tareaCarpetas("buscar",{raiz:v.directorio});});
for(const b of [$("#cancelar-carpetas"),$("#cancelar-firmas")]){
 b.onclick=async()=>{b.disabled=true;try{await api("carpetas/cancelar",{});}catch(e){mensaje(e.message,true);b.disabled=false;}};
}
$("#via-firmas").onchange=()=>{
 const archivo=$("#via-firmas").value==="archivo";
 $("#origen-archivo-firmas").hidden=!archivo;
 $("#archivo-firmas").disabled=!archivo;$("#archivo-firmas").required=archivo;
 $("#detalle-firmas").textContent=archivo
  ?"Copiá bytecode.cvd desde un equipo con ClamAV actualizado por FreshClam. Podés elegir su carpeta o pegar la ruta del archivo. Esta opción funciona sin Internet."
  :"FreshClam descargará las firmas al confirmar. Necesitás Internet sólo en ese momento. La descarga funciona sin cuenta y los datos de la imprenta quedan en tu PC.";
};
$("#examinar-firmas").onclick=()=>accion(async()=>{
 const campo=$("#archivo-firmas"),inicial=campo.value.trim();
 const v=await tareaCarpetas("examinar",{directorio:rutaAbsoluta(inicial)?inicial:""},$("#cancelar-firmas"),$("#examinar-firmas"));
 if(v.estado==="COMPLETADO"){
  campo.value=v.directorio.replace(/[\\/]+$/,"")+"\\bytecode.cvd";
  invalidar("firmas");mensaje("Carpeta elegida. Revisá la ruta de bytecode.cvd.");
 }
});

for(const id of ["opciones","operacion","respaldo","firmas"])$("#"+id).addEventListener("input",()=>invalidar(id));
for(const f of $$("form[novalidate]"))f.onsubmit=e=>{e.preventDefault();siguiente();};
$("#operacion-elegida").onchange=()=>{ejecutado=false;invalidar("operacion");};
$("#verificar").onclick=()=>accion(verificar);
$("#resolver").onclick=()=>accion(async()=>{invalidar("equipo");await api("dependencia",{accion:$("#dependencia").value,via:$("#via").value});await esperar();await verificar();});
$("#anterior").onclick=()=>ir(ruta()[ruta().indexOf(actual)-1]);$("#siguiente").onclick=siguiente;
$("#ejecutar").onclick=()=>accion(async()=>{
 if(ejecutado||!accesible("revision"))throw new Error("Volvé a completar los pasos previos.");
 let data,path;
 if(flujo==="nueva"){
  for(const id of nuevos.filter(x=>x!=="revision"))if(!validoFormulario(id)){actual=id;throw new Error("Revisá los campos del paso indicado.");}
  data={modo:"DEMO",...datos("opciones")};path="instalar";
 }else{
  const op=$("#operacion-elegida").value;
  if(op==="desinstalar"){path="desinstalar";data={confirmar:"DESINSTALAR"};}
  else if(op==="iniciar"){path="iniciar";data={};}
  else{path="mantener";data={operacion:op,...datos("respaldo")};}
 }
 if(ruta().includes("firmas")){
  if(!completos.has("firmas"))throw new Error("Elegí el origen de las firmas antes de continuar.");
  Object.assign(data,datos("firmas"));
 }
 try{await ejecutar(path,data);ejecutado=true;completos.add("revision");mensaje("Operación completada. Podés continuar con las comprobaciones finales.");}
 catch(e){
  await refrescar().catch(()=>{});
  if(ruta().includes("firmas")){
   actual="firmas";invalidar("firmas");$('[data-paso="firmas"] h2').focus();
   throw new Error(e.message+" Revisá el origen de las firmas y volvé a intentar.");
  }
  throw e;
 }
});
$("#actualizar-estado").onclick=()=>accion(async()=>{await refrescar();mensaje("Estado comprobado.");});
$("#cerrar").onclick=()=>accion(async()=>{const v=await api("cerrar",{});cerrado=true;mensaje(v.mensaje);});

const ayudas={
 dependencias:["Dependencias","Son las herramientas que necesita La Montaña para funcionar. El instalador comprueba si están disponibles y te indica cómo resolver las que falten. Revisar no modifica tu equipo."],
 docker:["Docker y Compose","Docker ejecuta los servicios de La Montaña en entornos separados llamados contenedores. Compose los coordina: base de datos, web, backend y otros componentes. Un Docker detenido no significa que falte instalarlo."],
 wsl:["WSL2","Es la función de Windows que permite ejecutar Linux. El instalador usa una Ubuntu propia llamada LaMontanaDemo-Ubuntu para alojar sus servicios, sin reemplazar tus otras distribuciones."],
 carpeta:["Carpeta de la aplicación","Es la ubicación de los archivos de configuración de esta instalación. Para una nueva, elegí una carpeta vacía. Para revisar o mantener una existente, indicá la carpeta que elegiste al instalarla. Los datos de la base se guardan en volúmenes Docker."],
 puerto:["Puerto","Es el número que identifica el acceso a la web dentro de tu PC; por ejemplo, http://127.0.0.1:3200. Elegí uno libre entre 1024 y 65535. Si ya está ocupado, la instalación pedirá otro."],
 mantenimiento:["Reparar o actualizar","Reparar repone componentes de la misma versión. Actualizar usa la versión de app incluida en este paquete, que debe ser posterior. Ambas operaciones conservan datos y exigen respaldo previo; no rebajan versiones."],
 respaldo:["Respaldo bruto","Es una copia de datos, archivos, configuración e imágenes con los servicios detenidos. Incluye credenciales: guardala en una carpeta privada fuera de la app. Se comprueba su integridad, pero esta versión aún no incorpora un procedimiento de restauración."],
};
let ayudaActiva=null,ayudaFija=false,temporizador;
function cerrarAyuda(){clearTimeout(temporizador);if(ayudaActiva){ayudaActiva.setAttribute("aria-expanded","false");ayudaActiva.removeAttribute("aria-describedby");}ayudaActiva=null;ayudaFija=false;$("#ayuda-ampliada").hidden=true;}
function abrirAyuda(b){clearTimeout(temporizador);if(ayudaActiva!==b)cerrarAyuda();ayudaActiva=b;const caja=$("#ayuda-ampliada"),[titulo,detalle]=ayudas[b.dataset.clave];caja.replaceChildren();texto(caja,"strong",titulo);texto(caja,"p",detalle);caja.hidden=false;b.setAttribute("aria-expanded","true");b.setAttribute("aria-describedby","ayuda-ampliada");posicionarAyuda();}
function posicionarAyuda(){if(!ayudaActiva)return;const caja=$("#ayuda-ampliada"),rect=ayudaActiva.getBoundingClientRect(),alto=caja.offsetHeight;if(rect.bottom<0||rect.top>innerHeight){cerrarAyuda();return;}caja.style.left=`${Math.max(12,Math.min(rect.left,innerWidth-caja.offsetWidth-12))}px`;caja.style.top=`${Math.max(12,rect.bottom+alto+16<innerHeight?rect.bottom+8:rect.top-alto-8)}px`;}
function diferirCierre(){clearTimeout(temporizador);temporizador=setTimeout(()=>{if(!ayudaFija&&document.activeElement!==ayudaActiva)cerrarAyuda();},160);}
for(const lugar of $$('[data-ayuda]')){const b=document.createElement("button");b.type="button";b.className="ayuda";b.dataset.clave=lugar.dataset.ayuda;b.textContent="i";b.setAttribute("aria-label",`Más información: ${ayudas[b.dataset.clave][0]}`);b.setAttribute("aria-expanded","false");b.onmouseenter=()=>abrirAyuda(b);b.onfocus=()=>abrirAyuda(b);b.onmouseleave=diferirCierre;b.onblur=diferirCierre;b.onclick=e=>{e.preventDefault();if(ayudaFija&&ayudaActiva===b)cerrarAyuda();else{abrirAyuda(b);ayudaFija=true;}};lugar.append(b);}
$("#ayuda-ampliada").onmouseenter=()=>clearTimeout(temporizador);$("#ayuda-ampliada").onmouseleave=diferirCierre;
document.addEventListener("keydown",e=>{if(e.key==="Escape")cerrarAyuda();});document.addEventListener("click",e=>{if(ayudaActiva&&!e.target.closest(".ayuda,#ayuda-ampliada"))cerrarAyuda();});window.addEventListener("resize",posicionarAyuda);window.addEventListener("scroll",posicionarAyuda);
render();
api("estado").then(v=>{versionApp=v.version;sistema=v.sistema;firmasExternas=v.firmasExternas===true;render();$("#versiones").textContent=`${sistema} · Instalador ${v.versionInstalador} · Aplicación incluida ${v.version}`;for(const n of $$('[data-windows]'))n.hidden=!sistema.startsWith("Windows");if(v.directorioRecomendado){$("#ruta-recomendada").textContent="Ruta recomendada: "+v.directorioRecomendado;if(!directorioEditado&&!$("#directorio").value)$("#directorio").value=v.directorioRecomendado;}mensaje("Listo para comenzar. La revisión del equipo es el primer paso.");}).catch(e=>mensaje(e.message,true));
