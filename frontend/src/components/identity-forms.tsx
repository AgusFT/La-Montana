//#region ENCABEZADO · src/components/identity-forms.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/identity-forms.tsx
 * ========================================================================
 * FUNCIÓN
 * Implementa el selector de roles de Demo, el registro de clientes, la instalación del
 * propietario y el cambio de rol mediante cierre de sesión.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] DemoRoleSelector()
 *   Componente de interfaz.
 * - [async] DemoRoleSelector :: enter(rol)
 * - [export] RegistrationForm()
 *   Componente de interfaz.
 * - [async] RegistrationForm :: submit(event: FormEvent<HTMLFormElement>)
 * - [export] InstallationForm()
 *   Componente de interfaz.
 * - [async] InstallationForm :: submit(event: FormEvent<HTMLFormElement>)
 * - [export] LogoutButton()
 *   Componente de interfaz.
 * - [async] LogoutButton :: logout()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

"use client";
import {useConfirmNavigation} from "@/components/navigation-boundary";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { clearPreparedFiles } from "@/lib/prepared-files";
import { resetTutorialSession, selectedGuide } from "@/lib/tutorial-visibility";
import { useTutorial } from "./tutorial-context";

import { secureMutation as post } from "@/lib/secure-mutation";
import "@/app/access.css";

const demoRoles = [
  { rol: "ADMINISTRADOR", nombre: "Administrador", descripcion: "Elena Senderos · Configuración y pedidos de la imprenta." },
  { rol: "EMPLEADO", nombre: "Empleado", descripcion: "Mateo Tintas · Producción e impresión simulada." },
  { rol: "CLIENTE", nombre: "Cliente", descripcion: "Lucía Papel · Cotizaciones y seguimiento de pedidos." },
] as const;

export function DemoRoleSelector() {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  async function enter(rol: typeof demoRoles[number]["rol"]) {
    if (busy) return;
    setBusy(true);
    setMessage("");
    try {
      await post("/api/demo/sesion", JSON.stringify({ rol }), "application/json");
      resetTutorialSession();
      window.location.replace("/acceso");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "No pudimos iniciar sesión.");
      setBusy(false);
    }
  }

  return <div className="identity-form demo-roles">
    {demoRoles.map(({ rol, nombre, descripcion }) => <div className="demo-role" key={rol}>
      <button type="button" className="refresh" disabled={busy} aria-describedby={`demo-rol-${rol}`} onClick={() => void enter(rol)}>{nombre}</button>
      <p id={`demo-rol-${rol}`} className="empty-note">{descripcion}</p>
    </div>)}
    <div className="demo-role-status" role="status">{busy && "Ingresando…"}</div>
    {message && <p className="form-message error-message" role="alert">{message}</p>}
  </div>;
}

export function RegistrationForm() {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [created, setCreated] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const form = event.currentTarget;
    const data = new FormData(form);
    if (data.get("contrasena") !== data.get("confirmacion")) { setMessage("Las contraseñas no coinciden."); return; }
    setBusy(true); setMessage("");
    try {
      await post("/api/auth/registro", JSON.stringify({ nombre: String(data.get("nombre")).trim(), apellido: String(data.get("apellido")).trim(), correo: String(data.get("correo")).trim(), contrasena: String(data.get("contrasena")) }), "application/json");
      form.reset(); setCreated(true);
    } catch (error) { setMessage(error instanceof Error ? error.message : "No pudimos crear la cuenta."); }
    finally { setBusy(false); }
  }
  if (created) return <div role="status"><h2>Cuenta de cliente creada</h2><p className="empty-note">Ya podés iniciar sesión. Después verificá tu correo desde Seguridad de la cuenta.</p><a className="refresh" href="/acceso">Iniciar sesión</a></div>;
  return <form className="identity-form" onSubmit={submit}>
    <p className="empty-note">Registro de clientes particulares. Podrás verificar tu correo después de iniciar sesión.</p>
    <div className="form-columns"><label>Nombre<input name="nombre" autoComplete="given-name" required maxLength={100} /></label><label>Apellido<input name="apellido" autoComplete="family-name" required maxLength={100} /></label></div>
    <label>Correo electrónico<input name="correo" type="email" autoComplete="username" required maxLength={254} /></label>
    <label>Contraseña<input name="contrasena" type="password" autoComplete="new-password" required minLength={12} maxLength={128} aria-describedby="registration-password-help" /></label>
    <p id="registration-password-help" className="field-help">Entre 12 y 128 caracteres.</p>
    <label>Repetir contraseña<input name="confirmacion" type="password" autoComplete="new-password" required minLength={12} maxLength={128} /></label>
    {message && <p className="form-message error-message" role="alert">{message}</p>}
    <button className="refresh" disabled={busy}>{busy ? "Creando cuenta…" : "Crear cuenta de cliente"}</button>
    <a className="secondary-link" href="/acceso">Ya tengo cuenta</a>
  </form>;
}

export function InstallationForm() {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [created, setCreated] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const form = event.currentTarget;
    const data = new FormData(form);
    if (data.get("contrasena") !== data.get("confirmacion")) { setMessage("Las contraseñas no coinciden."); return; }
    setBusy(true); setMessage("");
    try {
      await post("/api/setup/propietario", JSON.stringify({ token: String(data.get("token")), nombre: String(data.get("nombre")).trim(), apellido: String(data.get("apellido")).trim(), correo: String(data.get("correo")).trim(), contrasena: String(data.get("contrasena")) }), "application/json");
      form.reset(); setCreated(true);
    } catch (error) { setMessage(error instanceof Error ? error.message : "No pudimos crear el propietario."); }
    finally { setBusy(false); }
  }
  if (created) return <div role="status"><h2>Propietario creado</h2><p className="empty-note">Ya podés ingresar con tu correo y contraseña.</p><a className="refresh" href="/acceso">Iniciar sesión</a></div>;
  return <form className="identity-form" onSubmit={submit}>
    <p className="empty-note">Esta alta se realiza una sola vez. El operador debe ingresar el token de instalación configurado en el archivo .env del servidor.</p>
    <label>Token de instalación<input name="token" type="password" autoComplete="off" required maxLength={512} /></label>
    <div className="form-columns"><label>Nombre<input name="nombre" autoComplete="given-name" required maxLength={100} /></label><label>Apellido<input name="apellido" autoComplete="family-name" required maxLength={100} /></label></div>
    <label>Correo electrónico<input name="correo" type="email" autoComplete="username" required maxLength={254} /></label>
    <label>Contraseña<input name="contrasena" type="password" autoComplete="new-password" required minLength={12} maxLength={128} aria-describedby="password-help" /></label>
    <p id="password-help" className="field-help">Entre 12 y 128 caracteres.</p>
    <label>Repetir contraseña<input name="confirmacion" type="password" autoComplete="new-password" required minLength={12} maxLength={128} /></label>
    {message && <p className="form-message error-message" role="alert">{message}</p>}
    <button className="refresh" disabled={busy}>{busy ? "Creando propietario…" : "Crear propietario"}</button>
  </form>;
}

export function LogoutButton() {
  const confirmNavigation=useConfirmNavigation();
  const tutorial=useTutorial();
  const standalone = tutorial === null;
  const [outsideGuide, setOutsideGuide] = useState(false);
  useEffect(() => {
    if (!standalone) return;
    // Seguridad de la cuenta tiene su propio marco, pero comparte la sesión.
    const sync = () => setOutsideGuide(!!selectedGuide());
    sync();
    window.addEventListener("pageshow", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("pageshow", sync);
      window.removeEventListener("storage", sync);
    };
  }, [standalone]);
  const lockedByGuide = !!tutorial?.guide || (standalone && outsideGuide);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const router = useRouter();
  async function logout() {
    if (busy || lockedByGuide || (standalone && selectedGuide()) || !confirmNavigation()) return;
    setBusy(true); setMessage("");
    try {
      await tutorial?.flush();
      await post("/api/auth/logout");
      resetTutorialSession();
      tutorial?.setOpen(false);
      clearPreparedFiles();
      router.replace("/acceso");
      router.refresh();
    }
    catch (error) { setMessage(error instanceof Error ? error.message : "No pudimos cerrar sesión."); }
    finally { setBusy(false); }
  }
  return <div><button className="refresh" data-tutorial-cambiar-rol onClick={logout} disabled={busy || lockedByGuide}>{busy ? "Cerrando sesión…" : "Cambiar rol"}</button>{standalone && outsideGuide && <p>Volvé a tu espacio para pausar el tutorial antes de cambiar de rol.</p>}{message && <p className="form-message error-message" role="alert">{message}</p>}</div>;
}
