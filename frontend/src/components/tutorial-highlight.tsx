//#region ENCABEZADO · src/components/tutorial-highlight.tsx
// Destaca la operación dentro del área de trabajo sin tapar la guía ni los menús.
//#endregion
"use client";
import { useEffect, useState } from "react";
import { createPortal } from "react-dom";

export function visibleTarget(selector: string) {
  return [...document.querySelectorAll<HTMLElement>(selector)].find(
    (element) => element.getClientRects().length > 0,
  );
}
export function focusTarget(selector: string) {
  const target = visibleTarget(selector);
  if (!target) return;
  if (!target.matches("button,a,input,textarea,select,summary,[tabindex]"))
    target.tabIndex = -1;
  target.focus({ preventScroll: true });
  target.scrollIntoView({
    block: "center",
    inline: "nearest",
    behavior: "instant",
  });
}

type Box = { left: number; top: number; width: number; height: number };
type Highlight = { surface: Box; target: Box };

export function TutorialHighlight({ selector }: { selector: string }) {
  const [highlight, setHighlight] = useState<Highlight | null>(null);
  useEffect(() => {
    let frame = 0;
    let first = true;
    function measure() {
      frame = 0;
      const element = visibleTarget(selector);
      const surface = element?.closest(".admin-main,.client-main");
      if (
        !element ||
        !surface ||
        document.querySelector('dialog[open],[aria-modal="true"]:not([hidden])')
      ) {
        setHighlight(null);
        return;
      }
      const area = surface.getBoundingClientRect();
      const left = Math.max(0, area.left),
        top = Math.max(0, area.top);
      const right = Math.min(innerWidth, area.right),
        bottom = Math.min(innerHeight, area.bottom);
      const rect = element.getBoundingClientRect();
      if (first) {
        first = false;
        if (
          matchMedia("(min-width: 1081px)").matches &&
          (rect.bottom <= top || rect.top >= bottom)
        ) {
          element.scrollIntoView({
            block: "center",
            inline: "nearest",
            behavior: "instant",
          });
          schedule();
          return;
        }
      }
      if (
        left >= right ||
        top >= bottom ||
        rect.bottom <= top ||
        rect.top >= bottom ||
        rect.right <= left ||
        rect.left >= right
      ) {
        setHighlight(null);
        return;
      }
      setHighlight({
        surface: { top, left, width: right - left, height: bottom - top },
        target: {
          top: rect.top - top - 3,
          left: rect.left - left - 3,
          width: rect.width + 6,
          height: rect.height + 6,
        },
      });
    }
    function schedule() {
      if (!frame) frame = requestAnimationFrame(measure);
    }
    const observer = new MutationObserver((changes) => {
      if (
        changes.some(
          ({ target }) =>
            !(
              target instanceof Element &&
              target.closest(".tutorial-panel,.tutorial-highlight-area")
            ),
        )
      )
        schedule();
    });
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ["hidden", "open", "class", "disabled", "aria-expanded"],
    });
    const resize = new ResizeObserver(schedule);
    const main = document.querySelector(".admin-main,.client-main");
    if (main) resize.observe(main);
    window.addEventListener("scroll", schedule, true);
    window.addEventListener("resize", schedule);
    schedule();
    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
      resize.disconnect();
      window.removeEventListener("scroll", schedule, true);
      window.removeEventListener("resize", schedule);
    };
  }, [selector]);
  return highlight
    ? createPortal(
        <div
          aria-hidden="true"
          className="tutorial-highlight-area"
          style={highlight.surface}
        >
          <div className="tutorial-highlight" style={highlight.target} />
        </div>,
        document.body,
      )
    : null;
}
