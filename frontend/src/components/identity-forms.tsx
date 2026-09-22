"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";

import { secureMutation as post } from "@/lib/secure-mutation";
import { isRole, roleHome } from "@/lib/roles";

export function LoginForm() {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const router = useRouter();
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const data = new FormData(event.currentTarget);
    setBusy(true); setMessage("");
    try {
      await post("/api/auth/login", new URLSearchParams({ username: String(data.get("correo")).trim(), password: String(data.get("contrasena")) }), "application/x-www-form-urlencoded");
      const response = await fetch("/api/auth/me", { cache: "no-store", credentials: "same-origin" });
      if (!response.ok) throw new Error("No pudimos verificar tu sesión. Intentá ingresar nuevamente.");
      const profile = await response.json();
      if (!isRole(profile?.rol)) throw new Error("Esta cuenta no tiene un acceso habilitado.");
      router.replace(roleHome(profile.rol)); router.refresh();
    } catch (error) { setMessage(error instanceof Error ? error.message : "No pudimos iniciar sesión."); }
    finally { setBusy(false); }
  }
  return <form className="identity-form" onSubmit={submit}>
    <label>Correo electrónico<input name="correo" type="email" autoComplete="username" required maxLength={254} /></label>
    <label>Contraseña<input name="contrasena" type="password" autoComplete="current-password" required maxLength={128} /></label>
    {message && <p className="form-message error-message" role="alert">{message}</p>}
    <button className="refresh" disabled={busy}>{busy ? "Ingresando…" : "Iniciar sesión"}</button>
    <a className="secondary-link" href="/registro">Crear una cuenta de cliente</a>
    <p className="empty-note">Recuperación de contraseña: en construcción.</p>
  </form>;
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
  if (created) return <div role="status"><h2>Cuenta de cliente creada</h2><p className="empty-note">Ya podés iniciar sesión. Tu correo todavía no fue verificado: la verificación de correo y la recuperación de contraseña están en construcción.</p><a className="refresh" href="/acceso">Iniciar sesión</a></div>;
  return <form className="identity-form" onSubmit={submit}>
    <p className="empty-note">Registro de clientes particulares. Verificación de correo y recuperación de contraseña: en construcción.</p>
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
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const router = useRouter();
  async function logout() {
    if (busy) return;
    setBusy(true); setMessage("");
    try { await post("/api/auth/logout"); router.replace("/acceso"); router.refresh(); }
    catch (error) { setMessage(error instanceof Error ? error.message : "No pudimos cerrar sesión."); }
    finally { setBusy(false); }
  }
  return <div><button className="refresh" onClick={logout} disabled={busy}>{busy ? "Cerrando sesión…" : "Cerrar sesión"}</button>{message && <p className="form-message error-message" role="alert">{message}</p>}</div>;
}
