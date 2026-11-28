"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { DemoClientForm } from "./demo-client-form";
import { secureMutation } from "@/lib/secure-mutation";
import "./demo-quotas.css";

const groups = {
  PEDIDOS: { title: "Pedidos", rule: "Cada pedido ocupa un lugar, incluso si se cancela." },
  CLIENTES: { title: "Clientes", rule: "Cada ficha de cliente ocupa un lugar, incluida la de la imprenta de muestra." },
  CAJA: { title: "Caja", rule: "Cada cobro y cada devolución ocupa un lugar. Aplicar saldo ya recibido conserva el cupo." },
  IMPRESORAS: { title: "Impresoras", rule: "Cada impresora simulada ocupa un lugar, aunque aparezca en varias versiones de configuración." },
};
type Group = keyof typeof groups;
type Entry = {
  tipo: "PEDIDO" | "CLIENTE" | "COBRO" | "DEVOLUCION" | "IMPRESORA";
  codigo: string;
  concepto: string;
  detalle: string;
  reservado: boolean;
  bajaEn: string | null;
  bajaPor: string | null;
};
type Quota = { tipo: Group; limite: number; usados: number; registros: Entry[] };

const date = new Intl.DateTimeFormat("es-AR", { dateStyle: "short", timeStyle: "short" });

export function DemoQuotas({ initialGroup }: { initialGroup: Group }) {
  const [quotas, setQuotas] = useState<Quota[]>([]);
  const [selected, setSelected] = useState(initialGroup);
  const [pending, setPending] = useState<Entry | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const request = useRef<AbortController | null>(null);

  const load = useCallback(async () => {
    request.current?.abort();
    const controller = new AbortController();
    request.current = controller;
    setLoading(true);
    setError("");
    try {
      const response = await fetch("/api/demo/cupos", { cache: "no-store", signal: controller.signal });
      const data = await response.json();
      if (!response.ok) throw new Error(data?.mensaje || "No pudimos consultar los cupos.");
      if (!Array.isArray(data?.cupos) || data.cupos.length !== 4
          || !data.cupos.every((quota: Quota) => Object.hasOwn(groups, quota.tipo)
            && Number.isInteger(quota.usados) && Number.isInteger(quota.limite) && Array.isArray(quota.registros))) {
        throw new Error("No pudimos verificar los cupos recibidos.");
      }
      if (!controller.signal.aborted) setQuotas(data.cupos);
    } catch (cause) {
      if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : "No pudimos consultar los cupos.");
    } finally {
      if (!controller.signal.aborted) setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    return () => request.current?.abort();
  }, [load]);

  async function archive(entry: Entry) {
    if (busy) return;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await secureMutation(`/api/demo/cupos/${entry.tipo}/${entry.codigo}/baja`, "{}", "application/json");
      setPending(null);
      setNotice(`«${entry.concepto}» figura de baja. Su historial se conserva.`);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo confirmar la baja.");
    } finally {
      setBusy(false);
    }
  }

  const quota = quotas.find(item => item.tipo === selected);
  const active = quota?.registros.filter(row => !row.bajaEn) ?? [];
  const archived = quota?.registros.filter(row => row.bajaEn) ?? [];

  return <div className="demo-quotas">
    {quotas.length > 0 && <div className="demo-quota-cards" role="group" aria-label="Elegir un recurso">
      {quotas.map(item => <button key={item.tipo} type="button" data-recurso={item.tipo} aria-pressed={selected === item.tipo} disabled={busy}
        onClick={() => { setSelected(item.tipo); setPending(null); setNotice(""); }}>
        <strong>{groups[item.tipo].title}</strong>
        <span>{item.usados} de {item.limite} en uso</span>
      </button>)}
    </div>}
    <div className="demo-quota-feedback">
      <button className="admin-button secondary" type="button" disabled={loading || busy} onClick={() => void load()}>Actualizar cupos</button>
      <span role="status">{loading ? "Consultando cupos…" : notice}</span>
    </div>
    {error && <p className="form-message error-message" role="alert">{error}</p>}
    {quota && <section className="admin-card" aria-labelledby="quota-title">
      <h2 id="quota-title">{groups[selected].title}</h2>
      <p>{groups[selected].rule}</p>
      {selected === "CLIENTES" && <details className="demo-client-entry"><summary>Crear ficha de cliente ficticio</summary>
        <DemoClientForm onCreated={() => void load()} />
      </details>}
      <h3>En uso</h3>
      {active.length === 0 ? <p>No hay registros que ocupen este cupo.</p> :
        <ul className="demo-quota-list">
          {active.map(entry => <li key={entry.tipo + entry.codigo}>
            <div><strong>{entry.concepto}</strong><small>{entry.detalle}</small></div>
            {entry.reservado
              ? <span>Reservada para el selector de Cliente</span>
              : pending?.codigo === entry.codigo && pending.tipo === entry.tipo
              ? <div className="demo-confirm" role="group" aria-label={`Confirmar baja de ${entry.concepto}`}>
                  <p>Se liberará un lugar. El registro y sus vínculos seguirán en el historial.</p>
                  <button type="button" className="admin-button" disabled={busy} onClick={() => void archive(entry)}>Confirmar baja</button>
                  <button type="button" className="admin-button secondary" disabled={busy} onClick={() => setPending(null)}>Volver</button>
                </div>
              : <button type="button" className="admin-button secondary" disabled={busy || loading}
                  aria-label={`Dar de baja ${entry.concepto}`} onClick={() => { setPending(entry); setNotice(""); }}>Dar de baja</button>}
          </li>)}
        </ul>}
      <h3>Historial de bajas</h3>
      {archived.length === 0 ? <p>Todavía no hay bajas de este recurso.</p> :
        <ul className="demo-quota-list">{archived.map(entry => <li key={entry.tipo + entry.codigo}>
          <div><strong>{entry.concepto}</strong><small>{entry.detalle}</small></div>
          <span>{entry.bajaPor} · <time dateTime={entry.bajaEn!}>{date.format(new Date(entry.bajaEn!))}</time></span>
        </li>)}</ul>}
    </section>}
  </div>;
}
