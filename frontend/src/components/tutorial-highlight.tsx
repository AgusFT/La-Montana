//#region ENCABEZADO · src/components/tutorial-highlight.tsx
// Resalta el control de la operación y permite llevar el foco al formulario.
//#endregion
"use client";
import { useEffect, useState } from "react";
export function visibleTarget(selector: string) {
  return [...document.querySelectorAll<HTMLElement>(selector)].find(
    (element) => element.getClientRects().length > 0,
  );
}
export function focusTarget(selector: string) {
  const group = visibleTarget(selector);
  if (!group) return;
  const target = group.matches("button,a,input,textarea,select,summary")
    ? group
    : (group.querySelector<HTMLElement>(
        "button:not(:disabled),a,input:not(:disabled),textarea:not(:disabled),select:not(:disabled),summary",
      ) ?? group);
  if (!target.matches("button,a,input,textarea,select,summary,[tabindex]"))
    target.tabIndex = -1;
  target.focus({ preventScroll: true });
  target.scrollIntoView({ block: "center", behavior: "instant" });
}
export function TutorialHighlight({ selector }: { selector: string }) {
  const [rect, setRect] = useState<{
    top: number;
    left: number;
    width: number;
    height: number;
  } | null>(null);
  useEffect(() => {
    let frame = 0;
    const measure = () => {
      frame = 0;
      const element = visibleTarget(selector);
      if (!element || document.querySelector("dialog[open]")) {
        setRect(null);
        return;
      }
      const r = element.getBoundingClientRect();
      if (r.bottom < 0 || r.top > innerHeight) {
        setRect(null);
        return;
      }
      const top = Math.max(0, r.top - 3),
        left = Math.max(0, r.left - 3);
      const next = {
        top,
        left,
        width: Math.min(innerWidth, r.right + 3) - left,
        height: Math.min(innerHeight, r.bottom + 3) - top,
      };
      setRect((last) =>
        last &&
        last.top === top &&
        last.left === left &&
        last.width === next.width &&
        last.height === next.height
          ? last
          : next,
      );
    };
    const schedule = () => {
      if (!frame) frame = requestAnimationFrame(measure);
    };
    const observer = new MutationObserver((changes) => {
      if (
        changes.some(
          (change) =>
            !(change.target as Element)?.closest?.(
              ".tutorial-panel,.tutorial-highlight",
            ),
        )
      )
        schedule();
    });
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ["hidden", "open", "class", "disabled"],
    });
    window.addEventListener("scroll", schedule, true);
    window.addEventListener("resize", schedule);
    schedule();
    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
      window.removeEventListener("scroll", schedule, true);
      window.removeEventListener("resize", schedule);
    };
  }, [selector]);
  return rect ? (
    <div aria-hidden="true" className="tutorial-highlight" style={rect} />
  ) : null;
}
