"use client";
import {useEffect,useRef,useState,type FormEvent} from "react";
import {secureMutation} from "@/lib/secure-mutation";
type Estado={cuenta:{correo:string;habilitado:boolean}[];autorizaciones:{id_autorizacion_soporte:number;motivo:string;estado:string;fecha_inicio:string;fecha_fin:string;autorizante:string}[]};
export function SoportePanel(){
  const [estado,setEstado]=useState<Estado|null>(null),[mensaje,setMensaje]=useState(""),[busy,setBusy]=useState(false);
  const pendiente=useRef(false);
  async function cargar(){const r=await fetch("/api/admin/soporte",{cache:"no-store"});const d=await r.json();if(!r.ok)throw new Error(d.mensaje);setEstado(d);}
  useEffect(()=>{cargar().catch(e=>setMensaje(e.message));},[]);
  async function enviar(e:FormEvent<HTMLFormElement>){
    e.preventDefault();if(pendiente.current)return;
    const f=e.currentTarget,d=new FormData(f),accion=d.get("accion"),crear=accion==="crear",revocar=accion==="revocar";
    const cuerpo=crear?{nombre:d.get("nombre"),apellido:d.get("apellido"),correo:d.get("correo"),contrasena:d.get("contrasenaNueva"),contrasenaActual:d.get("contrasena")}:{contrasena:d.get("contrasena"),...(revocar?{}:{motivo:d.get("motivo"),horas:Number(d.get("horas"))})};
    pendiente.current=true;setBusy(true);setMensaje("");
    f.querySelectorAll<HTMLInputElement>('input[type="password"]').forEach(n=>n.value="");
    try{
      await secureMutation(`/api/admin/soporte${crear?"":revocar?"/revocar":"/autorizar"}`,JSON.stringify(cuerpo),"application/json");
      f.reset();await cargar();
      setMensaje(crear?"Cuenta de soporte creada. Su acceso sigue bloqueado; autorizalo por separado cuando necesites asistencia.":revocar?"Acceso revocado; las sesiones fueron invalidadas.":"Acceso autorizado por el plazo indicado.");
    }catch(e){setMensaje(e instanceof Error?e.message:"No se pudo completar la operación.");}
    finally{f.querySelectorAll<HTMLInputElement>('input[type="password"]').forEach(n=>n.value="");pendiente.current=false;setBusy(false);}
  }
  return <><p role="status" aria-live="polite">{mensaje}</p>{estado&&<>
    <p><strong>{!estado.cuenta.length?"Sin cuenta de soporte":estado.cuenta[0].habilitado?"Soporte habilitado":"Soporte bloqueado"}</strong>{estado.cuenta[0]&&<> · {estado.cuenta[0].correo}</>}</p>
    {!estado.cuenta.length?<form onSubmit={enviar}>
      <h2>Crear cuenta de soporte</h2><p>La Montaña funciona sin esta cuenta. Si decidís crearla, quedará bloqueada hasta que autorices su acceso por separado.</p>
      <input type="hidden" name="accion" value="crear"/>
      <fieldset disabled={busy}><legend>Datos de la nueva cuenta de soporte</legend>
        <label>Nombre<input required name="nombre" maxLength={100} autoComplete="off"/></label>
        <label>Apellido<input required name="apellido" maxLength={100} autoComplete="off"/></label>
        <label>Correo de soporte<input required type="email" name="correo" maxLength={254} autoComplete="off"/></label>
        <label>Contraseña de la nueva cuenta de soporte<input required type="password" name="contrasenaNueva" minLength={12} maxLength={128} autoComplete="new-password" aria-describedby="clave-soporte-ayuda"/></label>
        <p id="clave-soporte-ayuda">Entre 12 y 128 caracteres. Usá un correo distinto al de las cuentas existentes.</p>
        <label>Tu contraseña actual de administrador<input required type="password" name="contrasena" maxLength={128} autoComplete="current-password"/></label>
        <button disabled={busy}>Crear cuenta de soporte</button>
      </fieldset>
    </form>:<>
      <form onSubmit={enviar}><h2>Autorizar asistencia</h2><input type="hidden" name="accion" value="autorizar"/><label>Motivo<textarea required name="motivo" maxLength={2000}/></label><label>Duración en horas (máximo 24)<input required type="number" name="horas" min={1} max={24} defaultValue={1}/></label><label>Tu contraseña actual<input required type="password" autoComplete="current-password" name="contrasena" maxLength={128}/></label><button disabled={busy}>Autorizar acceso de soporte</button></form>
      <form onSubmit={enviar}><h2>Revocar acceso</h2><input type="hidden" name="accion" value="revocar"/><label>Tu contraseña actual<input required type="password" autoComplete="current-password" name="contrasena" maxLength={128}/></label><button disabled={busy}>Revocar y cerrar sesiones</button></form>
    </>}
    <h2>Últimas autorizaciones</h2>{estado.autorizaciones.length?<ul>{estado.autorizaciones.map(a=><li key={a.id_autorizacion_soporte}>{a.estado} · {a.motivo} · {a.autorizante} · Hasta {new Date(a.fecha_fin).toLocaleString()}</li>)}</ul>:<p>Todavía no hay autorizaciones de acceso.</p>}
  </>}</>;
}
