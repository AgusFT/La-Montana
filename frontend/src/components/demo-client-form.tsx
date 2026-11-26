"use client";

import { useRef, useState, type FormEvent } from "react";
import { uuidPattern } from "@/lib/organization-types";
import { secureMutation } from "@/lib/secure-mutation";

export function DemoClientForm({ onCreated }: { onCreated?: () => void }) {
  const command = useRef<{ nombre: string; apellido: string; operacion: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const form = event.currentTarget;
    const fields = new FormData(form);
    const nombre = String(fields.get("nombre")).trim();
    const apellido = String(fields.get("apellido")).trim();
    setError("");
    setNotice("");
    if (!nombre || !apellido) {
      setError("Completá el nombre y el apellido ficticios.");
      return;
    }
    if (command.current?.nombre !== nombre || command.current.apellido !== apellido) {
      command.current = { nombre, apellido, operacion: crypto.randomUUID() };
    }
    setBusy(true);
    try {
      const response = await secureMutation("/api/demo/clientes", JSON.stringify(command.current), "application/json");
      const client = await response.json();
      if (typeof client?.codigo !== "string" || !uuidPattern.test(client.codigo)) {
        throw new Error("No pudimos confirmar el alta. Volvé a enviar la ficha para comprobarla.");
      }
      command.current = null;
      form.reset();
      setNotice(`Ficha creada para ${nombre} ${apellido}.`);
      onCreated?.();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo crear la ficha.");
    } finally {
      setBusy(false);
    }
  }

  return <form onSubmit={create} className="admin-form">
    <p>Usá un nombre y un apellido ficticios. La ficha ocupa un lugar en el cupo compartido de clientes.</p>
    <div className="form-columns">
      <label>Nombre ficticio<input name="nombre" autoComplete="off" required maxLength={100} disabled={busy} /></label>
      <label>Apellido ficticio<input name="apellido" autoComplete="off" required maxLength={100} disabled={busy} /></label>
    </div>
    <button type="submit" className="admin-button" disabled={busy}>{busy ? "Guardando…" : "Crear ficha"}</button>
    {error && <p className="form-message error-message" role="alert">{error}</p>}
    <p role="status">{notice}</p>
  </form>;
}
