//#region ENCABEZADO · src/components/tutorial-draft-fields.tsx
// Restaura y guarda los campos editables autorizados del paso en curso.
//#endregion
"use client";
import { useEffect, useRef } from "react";
import type { Draft, Step } from "@/lib/demo-tutorial";
import { tutorialBusinessChanged, useTutorial } from "./tutorial-context";

type Field = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;
const definitions: Partial<Record<Step, Record<string, string>>> = {
  cobro: {
    importe: '[name="importe"]',
    fecha: '[name="fecha"]',
    referencia: '[name="referencia"]',
    observacion: '[name="motivo"]',
  },
  revision: { observacion: '[name="motivo"]' },
  produccion: {
    impresora: 'select[aria-label="Impresora o recurso simulado"]',
    observacion: '[name="motivo"]',
  },
  calidad: {
    resultado: 'select[aria-label="Resultado de calidad"]',
    observacion: '[name="motivo"]',
  },
  entrega: { receptor: '[name="receptor"]', observacion: '[name="motivo"]' },
};
const mutations: Partial<Record<Step, RegExp>> = {
  cobro: /\/pagos\/(recibir|aplicar)$/,
  revision: /\/gestion$/,
  produccion: /\/produccion\/(iniciar|trabajos\/[^/]+\/estado)$/,
  calidad: /\/produccion\/trabajos\/[^/]+\/calidad$/,
  entrega: /\/entrega\/(confirmar|cerrar|movimientos)$/,
};
// Only non-authorizing fields from this specific operation can be restored.
// React-controlled selects use their native setter and a change event.
function restore(field: Field, value: string) {
  if (
    field instanceof HTMLSelectElement &&
    !Array.from(field.options).some((o) => o.value === value)
  )
    return;
  const prototype =
    field instanceof HTMLSelectElement
      ? HTMLSelectElement.prototype
      : field instanceof HTMLTextAreaElement
        ? HTMLTextAreaElement.prototype
        : HTMLInputElement.prototype;
  Object.getOwnPropertyDescriptor(prototype, "value")?.set?.call(field, value);
  if (field instanceof HTMLSelectElement)
    field.dispatchEvent(new Event("change", { bubbles: true }));
}
export function TutorialDraftFields({
  step,
  operation,
}: {
  step: Step;
  operation: string;
}) {
  const api = useTutorial(),
    ref = useRef<HTMLSpanElement>(null),
    current = useRef(api);
  current.current = api;
  const active =
    api?.state?.estado === "ACTIVO" &&
    api.role === "EMPLEADO" &&
    !!api.state.pedido &&
    typeof location !== "undefined" &&
    location.pathname === `/operacion/pedidos/${api.state.pedido}`;
  useEffect(() => {
    const form = ref.current?.closest("form"),
      map = definitions[step];
    if (!form || !active || !map) return;
    const fields = Object.entries(map).map(
      ([key, selector]) => [key, form.querySelector<Field>(selector)] as const,
    );
    const previous = current.current?.state?.borradores[step];
    if (previous?.formulario === operation) {
      for (const [key, field] of fields)
        if (field && previous[key] !== undefined) restore(field, previous[key]);
    }
    const record = (event: Event) => {
      if (!fields.some(([, field]) => field === event.target)) return;
      const values: Draft = { formulario: operation };
      for (const [key, field] of fields) values[key] = field?.value ?? "";
      current.current?.draft(step, values);
    };
    const completed = (event: Event) => {
      const path = (event as CustomEvent<{ path: string }>).detail?.path ?? "";
      const data = current.current?.state;
      if (
        !data ||
        (!path.includes("/" + data.pedido + "/") &&
          !path.includes("/" + data.cotizacion + "/")) ||
        !mutations[step]?.test(path)
      )
        return;
      const empty: Draft = { formulario: "" };
      for (const [key] of fields) empty[key] = "";
      current.current?.draft(step, empty);
    };
    form.addEventListener("input", record);
    form.addEventListener("change", record);
    window.addEventListener(tutorialBusinessChanged, completed);
    return () => {
      form.removeEventListener("input", record);
      form.removeEventListener("change", record);
      window.removeEventListener(tutorialBusinessChanged, completed);
    };
  }, [active, step, operation]);
  return <span hidden ref={ref} />;
}
