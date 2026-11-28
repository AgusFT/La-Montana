import type { Preparation } from "@/lib/prepared-work";

export function TutorialPreparationNotice({ preparation, onLink, busy }: {
  preparation: Preparation;
  onLink?: () => void;
  busy?: boolean;
}) {
  const files = preparation.archivos;
  return (
    <section className="client-note" aria-label="Preparación en curso">
      <strong>{onLink ? "Ya tenés una preparación abierta." : "Retomaste la preparación abierta del tutorial."}</strong>
      <p>
        Cada cliente tiene una preparación en curso. {files.length > 0 && <>
          {files.length === 1 ? "El PDF guardado es " : "Los PDF guardados son "}
          {files.map(file => file.nombre).join(", ")}.{" "}
        </>}
        {preparation.presupuesto && "El presupuesto guardado también está disponible. "}
        {onLink
          ? "Podés usar este mismo trabajo para practicar, o pausar la guía para continuar por tu cuenta."
          : "Los cambios que confirmes acá quedarán en este trabajo. Cuando confirmes el pedido, podés iniciar otro."}
      </p>
      {onLink && <button type="button" className="client-button" data-tutorial-vincular disabled={busy} onClick={onLink}>
        Usar esta preparación en el tutorial
      </button>}
    </section>
  );
}
