"use client";

import { useEffect, useState } from "react";
import { consultarVigencia, type Vigencia } from "@/lib/demo-vigencia";

export function EntornoAviso({ inicial, children }: { inicial: Vigencia; children?: React.ReactNode }) {
  const [vigencia, setVigencia] = useState(inicial);

  useEffect(() => {
    let activa = true;
    let peticion: AbortController | undefined;
    let proxima: ReturnType<typeof setTimeout>;
    let corte: ReturnType<typeof setTimeout>;

    function cerrarAlVencer(restante: number) {
      clearTimeout(corte);
      corte = setTimeout(() => {
        setVigencia({ estado: "NO_DISPONIBLE", diasRestantes: 0, restanteMillis: 0, mensaje: "Verificando el vencimiento de la Demo…" });
        void actualizar();
      }, restante);
    }

    function programar(v: Vigencia) {
      clearTimeout(proxima);
      if (v.estado === "VENCIDA") return;
      const demora = v.estado === "VIGENTE" ? Math.min(15000, v.restanteMillis) : 5000;
      proxima = setTimeout(actualizar, demora);
    }
    async function actualizar() {
      clearTimeout(proxima);
      peticion?.abort();
      const actual = new AbortController();
      peticion = actual;
      const solicitada = performance.now();
      const v = await consultarVigencia("/api/demo/vigencia", AbortSignal.any([actual.signal, AbortSignal.timeout(5000)]));
      if (!activa || actual.signal.aborted) return;
      if (v.estado === "VIGENTE" && inicial.estado !== "VIGENTE") {
        // El servidor omitió el contenido protegido mientras el reloj estaba bloqueado.
        window.location.reload();
        return;
      }
      clearTimeout(corte);
      if (v.estado === "VIGENTE") cerrarAlVencer(Math.max(0, v.restanteMillis - (performance.now() - solicitada)));
      setVigencia(v);
      programar(v);
    }
    function alVolver() { if (!document.hidden) void actualizar(); }

    if (inicial.estado === "VIGENTE") cerrarAlVencer(inicial.restanteMillis);
    void actualizar();
    window.addEventListener("focus", alVolver);
    window.addEventListener("pageshow", alVolver);
    window.addEventListener("demo:verificar-vigencia", actualizar);
    document.addEventListener("visibilitychange", alVolver);
    return () => {
      activa = false;
      peticion?.abort();
      clearTimeout(proxima);
      clearTimeout(corte);
      window.removeEventListener("focus", alVolver);
      window.removeEventListener("pageshow", alVolver);
      window.removeEventListener("demo:verificar-vigencia", actualizar);
      document.removeEventListener("visibilitychange", alVolver);
    };
  }, [inicial]);

  const { estado, diasRestantes, mensaje } = vigencia;
  return <>
    <aside className="entorno-aviso" role="note"><strong>Demo de La Montaña.</strong>{" "}
      {estado === "VIGENTE" ? `${diasRestantes} ${diasRestantes === 1 ? "día restante" : "días restantes"}. Usá datos ficticios.`
        : estado === "VENCIDA" ? "0 días restantes." : estado === "RELOJ_ATRASADO" ? `${diasRestantes} ${diasRestantes===1?"día restante":"días restantes"} al último registro válido.` : "Acceso en pausa."}
    </aside>
    {estado === "VIGENTE" ? children : <main className="demo-acceso-bloqueado">
      <h1>{estado === "VENCIDA" ? "Demo finalizada" : estado === "RELOJ_ATRASADO" ? "Revisá el reloj del equipo" : "Vigencia pendiente de verificación"}</h1>
      <p role="status">{mensaje}</p>
      {estado !== "VENCIDA" && <button type="button" className="refresh" onClick={() => window.dispatchEvent(new Event("demo:verificar-vigencia"))}>Volver a comprobar</button>}
    </main>}
  </>;
}
