//#region ENCABEZADO · src/components/security-forms.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/security-forms.tsx
 * ========================================================================
 * FUNCIÓN
 * Implementa verificación de correo, cambio de contraseña y recuperación de acceso, con
 * solicitudes de código, confirmaciones y mensajes de error.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - errorMessage(error: unknown)
 * - [async] send(path: string, data: object)
 * - [export] EmailVerificationForm({ correo, verified }: { correo: string; verified: boolean })
 *   Componente de interfaz.
 * - [async] EmailVerificationForm :: requestCode()
 * - [async] EmailVerificationForm :: confirm(event: FormEvent<HTMLFormElement>)
 * - [export] PasswordChangeForm()
 *   Componente de interfaz.
 * - [async] PasswordChangeForm :: submit(event: FormEvent<HTMLFormElement>)
 * - [export] RecoveryForms()
 *   Componente de interfaz.
 * - [async] RecoveryForms :: request(event: FormEvent<HTMLFormElement>)
 * - [async] RecoveryForms :: confirm(event: FormEvent<HTMLFormElement>)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

"use client";

import { useId, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { secureMutation } from "@/lib/secure-mutation";

function errorMessage(error: unknown) { return error instanceof Error ? error.message : "No pudimos completar la operación. Intentá nuevamente."; }
async function send(path: string, data: object) { await secureMutation(path, JSON.stringify(data), "application/json"); }

export function EmailVerificationForm({ correo, verified }: { correo: string; verified: boolean }) {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [sent, setSent] = useState(false);
  const router = useRouter();
  const helpId = useId();
  async function requestCode() {
    if (busy) return;
    setBusy(true); setMessage("");
    try { await send("/api/auth/correo/solicitar", {}); setSent(true); }
    catch (error) { setMessage(errorMessage(error)); }
    finally { setBusy(false); }
  }
  async function confirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const form = event.currentTarget;
    const token = String(new FormData(form).get("token")).trim();
    setBusy(true); setMessage("");
    try { await send("/api/auth/correo/confirmar", { token }); form.reset(); router.refresh(); }
    catch (error) { setMessage(errorMessage(error)); }
    finally { setBusy(false); }
  }
  return <section className="security-section"><h2>Verificar correo</h2><p className="account-email">{correo}</p>
    {verified ? <p className="form-message" role="status">Tu correo está verificado.</p> : <>
      <p className="empty-note">Solicitá un código y copialo desde el correo recibido. Podés pedir otro después de un minuto.</p>
      <button type="button" className="refresh" onClick={requestCode} disabled={busy}>{busy ? "Procesando…" : sent ? "Reenviar código" : "Enviar código de verificación"}</button>
      {sent && <p className="empty-note" role="status">Código enviado. Revisá tu correo.</p>}
      <form className="identity-form security-code-form" onSubmit={confirm}>
        <label>Código de verificación<input name="token" required minLength={43} maxLength={43} autoComplete="one-time-code" spellCheck={false} autoCapitalize="none" aria-describedby={helpId} /></label>
        <p className="field-help" id={helpId}>El código tiene 43 caracteres y vence en 15 minutos. Usá el último que recibiste.</p>
        <button className="refresh" disabled={busy}>{busy ? "Procesando…" : "Verificar correo"}</button>
      </form>
      {message && <p className="form-message error-message security-message" role="alert">{message}</p>}
    </>}
  </section>;
}

export function PasswordChangeForm() {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const router = useRouter();
  const helpId = useId();
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const data = new FormData(event.currentTarget);
    const contrasenaActual = String(data.get("contrasenaActual")), nuevaContrasena = String(data.get("nuevaContrasena"));
    if (nuevaContrasena !== data.get("confirmacion")) { setMessage("Las contraseñas nuevas no coinciden."); return; }
    if (nuevaContrasena === contrasenaActual) { setMessage("La contraseña nueva debe ser distinta de la actual."); return; }
    setBusy(true); setMessage("");
    try { await send("/api/auth/contrasena", { contrasenaActual, nuevaContrasena }); router.replace("/acceso"); router.refresh(); }
    catch (error) { setMessage(errorMessage(error)); }
    finally { setBusy(false); }
  }
  return <section className="security-section"><form className="identity-form" onSubmit={submit}>
    <h2>Cambiar contraseña</h2>
    <p className="empty-note">Al guardar, se cerrarán todas tus sesiones. Volvé a ingresar con la nueva contraseña.</p>
    <label>Contraseña actual<input name="contrasenaActual" type="password" autoComplete="current-password" required maxLength={128} /></label>
    <label>Nueva contraseña<input name="nuevaContrasena" type="password" autoComplete="new-password" required minLength={12} maxLength={128} aria-describedby={helpId} /></label>
    <p className="field-help" id={helpId}>Entre 12 y 128 caracteres; debe ser distinta de la contraseña actual.</p>
    <label>Repetir nueva contraseña<input name="confirmacion" type="password" autoComplete="new-password" required minLength={12} maxLength={128} /></label>
    {message && <p className="form-message error-message" role="alert">{message}</p>}
    <button className="refresh" disabled={busy}>{busy ? "Guardando…" : "Cambiar contraseña y cerrar sesiones"}</button>
  </form></section>;
}

export function RecoveryForms() {
  const [correo, setCorreo] = useState("");
  const [busy, setBusy] = useState(false);
  const [requestError, setRequestError] = useState("");
  const [confirmError, setConfirmError] = useState("");
  const [sent, setSent] = useState(false);
  const helpId = useId();
  const router = useRouter();
  async function request(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setBusy(true); setRequestError("");
    try { await send("/api/auth/recuperacion/solicitar", { correo: correo.trim() }); setSent(true); }
    catch (error) { setRequestError(errorMessage(error)); }
    finally { setBusy(false); }
  }
  async function confirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const data = new FormData(event.currentTarget);
    const nuevaContrasena = String(data.get("nuevaContrasena"));
    if (nuevaContrasena !== data.get("confirmacion")) { setConfirmError("Las contraseñas nuevas no coinciden."); return; }
    setBusy(true); setConfirmError("");
    try { await send("/api/auth/recuperacion/confirmar", { correo: correo.trim(), token: String(data.get("token")).trim(), nuevaContrasena }); router.replace("/acceso"); router.refresh(); }
    catch (error) { setConfirmError(errorMessage(error)); }
    finally { setBusy(false); }
  }
  return <>
    <section className="security-section"><form className="identity-form" onSubmit={request}>
      <h2>Solicitar código</h2>
      <label>Correo electrónico<input name="correo" type="email" autoComplete="email" required maxLength={254} value={correo} onChange={event => setCorreo(event.target.value)} /></label>
      {requestError && <p className="form-message error-message" role="alert">{requestError}</p>}
      {sent && <p className="form-message" role="status">Si la cuenta está habilitada para recuperar el acceso, recibirás un código por correo. Revisá también la carpeta de spam.</p>}
      <button className="refresh" disabled={busy}>{busy ? "Procesando…" : sent ? "Solicitar otro código" : "Enviar código de recuperación"}</button>
    </form></section>
    <section className="security-section"><form className="identity-form" onSubmit={confirm}>
      <h2>Elegir nueva contraseña</h2>
      <p className="empty-note">Copiá el código del correo recibido. Al confirmar, se cerrarán todas las sesiones de esa cuenta y podrás ingresar con la nueva contraseña.</p>
      <label>Correo de la cuenta<input name="correo" type="email" autoComplete="email" required maxLength={254} value={correo} onChange={event => setCorreo(event.target.value)} /></label>
      <label>Código de recuperación<input name="token" required minLength={43} maxLength={43} autoComplete="one-time-code" spellCheck={false} autoCapitalize="none" /></label>
      <label>Nueva contraseña<input name="nuevaContrasena" type="password" autoComplete="new-password" required minLength={12} maxLength={128} aria-describedby={helpId} /></label>
      <p className="field-help" id={helpId}>Entre 12 y 128 caracteres; debe ser distinta de la contraseña anterior.</p>
      <label>Repetir nueva contraseña<input name="confirmacion" type="password" autoComplete="new-password" required minLength={12} maxLength={128} /></label>
      {confirmError && <p className="form-message error-message" role="alert">{confirmError}</p>}
      <button className="refresh" disabled={busy}>{busy ? "Guardando…" : "Guardar nueva contraseña"}</button>
    </form></section>
  </>;
}
