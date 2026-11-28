"use client";

import { useEffect, useState } from "react";
import type { ReviewField } from "@/lib/tutorial-recorridos";
import { readReviewPosition, rememberReviewPosition } from "@/lib/tutorial-visibility";
import { visibleTarget } from "./tutorial-highlight";

type Reading = { key: string; visible: number[]; position: number };

export function useTutorialReview(key: string, fields?: ReviewField[]) {
  const [reading, setReading] = useState<Reading | null>(null);

  useEffect(() => {
    if (!fields?.length) return;
    const initial = readReviewPosition(key);
    let frame = 0;
    function sync() {
      frame = 0;
      const visible = fields!.flatMap(({ selector, when }, i) =>
        (!when || document.querySelector(when)) && visibleTarget(selector) ? [i] : [],
      );
      setReading((previous) => {
        if (previous?.key !== key) return { key, visible, position: initial };
        return previous.visible.join() === visible.join() ? previous : { ...previous, visible };
      });
    }
    function schedule() {
      if (!frame) frame = requestAnimationFrame(sync);
    }
    const observer = new MutationObserver((changes) => {
      if (changes.some(({ target, attributeName }) =>
        attributeName !== "aria-describedby" && !(target instanceof Element && target.closest(".tutorial-overlay")),
      )) schedule();
    });
    observer.observe(document.body, { childList: true, subtree: true, attributes: true });
    document.addEventListener("input", schedule, true);
    document.addEventListener("change", schedule, true);
    sync();
    return () => {
      observer.disconnect();
      cancelAnimationFrame(frame);
      document.removeEventListener("input", schedule, true);
      document.removeEventListener("change", schedule, true);
    };
  }, [key, fields]);

  function go(position: number) {
    rememberReviewPosition(key, position);
    setReading((current) => current?.key === key ? { ...current, position } : current);
    requestAnimationFrame(() => {
      if (document.activeElement === document.body)
        document.getElementById("tutorial-titulo")?.focus({ preventScroll: true });
    });
  }

  const current = fields && reading?.key === key ? reading : null;
  const visible = current?.visible ?? [];
  const index = current ? visible.find((i) => i >= current.position) : undefined;
  const number = index === undefined ? 0 : visible.indexOf(index) + 1;
  return {
    waiting: !!fields?.length && !current,
    field: index === undefined ? undefined : fields?.[index],
    label: number ? `Revisión ${number} de ${visible.length}. ` : "",
    last: number === visible.length,
    next: index === undefined ? undefined : () => go(index + 1),
    back: number > 1 ? () => go(visible[number - 2]) : undefined,
    again: number === 0 && visible.length ? () => go(visible[0]) : undefined,
  };
}

export function TutorialReviewButtons({ review }: { review: ReturnType<typeof useTutorialReview> }) {
  return (
    <>
      {review.back && <button type="button" onClick={review.back}>Anterior</button>}
      {review.next && <button type="button" className="tutorial-next" onClick={review.next}>
        {review.last ? "Terminar revisión" : "Siguiente"}
      </button>}
      {review.again && <button type="button" onClick={review.again}>Volver a revisar</button>}
    </>
  );
}
