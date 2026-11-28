//#region ENCABEZADO · src/components/tutorial-highlight.tsx
// Vincula la explicación del tutorial con el control real que se está usando.
//#endregion
"use client";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import "./tutorial-highlight.css";

const dialogs =
  'dialog[open], [role="dialog"][aria-modal="true"]:not([hidden])';
function openDialog() {
  return [...document.querySelectorAll<HTMLElement>(dialogs)]
    .filter((dialog) => dialog.getClientRects().length > 0)
    .at(-1);
}

export function visibleTarget(selector: string) {
  const scope = openDialog() ?? document;
  return [...scope.querySelectorAll<HTMLElement>(selector)].find(
    (element) =>
      element.getClientRects().length > 0 &&
      !element.closest(".tutorial-callout"),
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

type Rect = { left: number; top: number; right: number; bottom: number };
type Box = { left: number; top: number; width: number; height: number };
type Point = { x: number; y: number };
type Layout = {
  host: HTMLElement;
  area: Box;
  target?: Box;
  card: { left: number; top: number; width: number; maxHeight: number };
  arrow?: [Point, Point];
};

const clamp = (value: number, min: number, max: number) =>
  Math.max(min, Math.min(value, max));
const box = (r: Rect): Box => ({
  left: r.left,
  top: r.top,
  width: r.right - r.left,
  height: r.bottom - r.top,
});
function intersect(a: Rect, b: Rect): Rect | undefined {
  const left = Math.max(a.left, b.left),
    right = Math.min(a.right, b.right);
  const top = Math.max(a.top, b.top),
    bottom = Math.min(a.bottom, b.bottom);
  if (right > left && bottom > top) return { left, right, top, bottom };
}

// Las coordenadas de la tarjeta y de la flecha son relativas a la superficie visible.
function position(
  area: Box,
  target: Box | undefined,
  naturalHeight: number,
  compact: boolean,
) {
  const inset = 10,
    gap = 24;
  const width = Math.min(
    compact ? 154 : 352,
    Math.max(1, area.width - 2 * inset),
  );
  const height = Math.min(naturalHeight, Math.max(1, area.height - 2 * inset));
  if (!target)
    return {
      card: {
        left: Math.max(inset, (area.width - width) / 2),
        top: inset,
        width,
        maxHeight: area.height - 2 * inset,
      },
    };
  const right = target.left + target.width,
    bottom = target.top + target.height;
  const cx = target.left + target.width / 2,
    cy = target.top + target.height / 2;
  // Plegada, la ayuda queda en el borde opuesto de la pantalla y deja libre el formulario.
  if (compact) {
    const left = cx < area.width / 2 ? area.width - width - inset : inset;
    const top = cy < area.height / 2 ? area.height - height - inset : inset;
    const belowTarget = top >= bottom;
    return {
      card: { left, top, width, maxHeight: area.height - 2 * inset },
      arrow: [
        {
          x: clamp(cx, left + 8, left + width - 8),
          y: belowTarget ? top : top + height,
        },
        { x: cx, y: belowTarget ? bottom + 2 : target.top - 2 },
      ] as [Point, Point],
    };
  }
  const above = target.top - gap - inset,
    below = area.height - bottom - gap - inset;
  const alignX = clamp(cx - width / 2, inset, area.width - width - inset);
  const alignY = clamp(cy - height / 2, inset, area.height - height - inset);
  let left = alignX,
    top = alignY,
    maxHeight = area.height - 2 * inset;
  let start: Point, end: Point;
  if (
    above < height &&
    below < height &&
    (area.width - right >= width + gap + inset ||
      target.left >= width + gap + inset)
  ) {
    const onRight = area.width - right >= width + gap + inset;
    left = onRight ? right + gap : target.left - gap - width;
    start = {
      x: onRight ? left : left + width,
      y: clamp(cy, top + 8, top + height - 8),
    };
    end = { x: onRight ? right + 2 : target.left - 2, y: cy };
  } else {
    const under = below >= height || (above < height && below >= above);
    maxHeight = Math.max(1, under ? below : above);
    top = under ? bottom + gap : target.top - gap - Math.min(height, maxHeight);
    start = {
      x: clamp(cx, left + 8, left + width - 8),
      y: under ? top : target.top - gap,
    };
    end = { x: cx, y: under ? bottom + 2 : target.top - 2 };
  }
  return {
    card: { left, top, width, maxHeight },
    arrow: [start, end] as [Point, Point],
  };
}

// Ajusta una sola vez el encuadre de la nueva indicación. La tarjeta sólo necesita
// el espacio que le falta, y el campo sigue a la vista en su propio contenedor.
function makeRoom(
  element: HTMLElement,
  dialog: HTMLElement | undefined,
  area: Box,
  target: Box,
  height: number,
) {
  if (height > area.height - target.height - 48) return;
  let scroll: Element | null = element.parentElement;
  while (scroll && scroll !== document.body) {
    if (
      scroll.scrollHeight > scroll.clientHeight + 1 &&
      /auto|scroll/.test(getComputedStyle(scroll).overflowY)
    )
      break;
    if (scroll === dialog) return;
    scroll = scroll.parentElement;
  }
  if (!scroll || scroll === document.body)
    scroll = dialog ? null : document.scrollingElement;
  if (!scroll) return;
  // Distancia necesaria para poner el cuadro encima o debajo del objetivo.
  const up = Math.max(0, height + 40 - target.top);
  const down = Math.max(
    0,
    target.top + target.height + height + 40 - area.height,
  );
  const roomDown = scroll.scrollHeight - scroll.clientHeight - scroll.scrollTop;
  if (down <= roomDown && (down <= up || up > scroll.scrollTop))
    scroll.scrollBy({ top: down, behavior: "instant" });
  else if (up <= scroll.scrollTop)
    scroll.scrollBy({ top: -up, behavior: "instant" });
}

export function TutorialHighlight({
  selector,
  anchorKey,
  children,
}: {
  selector?: string;
  anchorKey: string;
  children: ReactNode;
}) {
  const card = useRef<HTMLElement>(null);
  const resizeContent = useRef<() => void>(() => {});
  const [layout, setLayout] = useState<Layout | null>(null);
  const [compact, setCompact] = useState(false);
  useEffect(() => {
    let frame = 0,
      first = true,
      framed = false;
    let width = window.innerWidth;
    let previousTarget: HTMLElement | undefined;
    let observedDialog: HTMLElement | undefined;
    let described: HTMLElement | undefined;
    function describe(target?: HTMLElement) {
      if (target === described) return;
      if (described) {
        const ids = (described.getAttribute("aria-describedby") ?? "")
          .split(/\s+/)
          .filter((id) => id && id !== "tutorial-indicacion");
        if (ids.length)
          described.setAttribute("aria-describedby", ids.join(" "));
        else described.removeAttribute("aria-describedby");
      }
      described = target;
      if (target) {
        const ids = new Set(
          (target.getAttribute("aria-describedby") ?? "")
            .split(/\s+/)
            .filter(Boolean),
        );
        ids.add("tutorial-indicacion");
        target.setAttribute("aria-describedby", [...ids].join(" "));
      }
    }
    function measure() {
      frame = 0;
      const dialog = openDialog();
      if (observedDialog !== dialog) {
        if (observedDialog) resize.unobserve(observedDialog);
        if (dialog) resize.observe(dialog);
        observedDialog = dialog;
      }
      const main = document.querySelector<HTMLElement>(
        ".admin-main,.client-main",
      );
      if (!main) {
        setLayout(null);
        return;
      }
      const viewport = window.visualViewport;
      const left = viewport?.offsetLeft ?? 0,
        top = viewport?.offsetTop ?? 0;
      const screen = {
        left,
        top,
        right: left + (viewport?.width ?? innerWidth),
        bottom: top + (viewport?.height ?? innerHeight),
      };
      const surface = dialog ?? main;
      const bounds = intersect(screen, surface.getBoundingClientRect());
      const element = selector ? visibleTarget(selector) : undefined;
      const rect = element?.getBoundingClientRect();
      if (element && element !== previousTarget) {
        first = true;
        framed = false;
      }
      previousTarget = element;
      if (first && (element || !selector)) {
        first = false;
        if (
          element &&
          rect &&
          (!bounds || rect.top < bounds.top || rect.bottom > bounds.bottom)
        )
          element.scrollIntoView({
            block: "center",
            inline: "nearest",
            behavior: "instant",
          });
        else if (!bounds && !dialog)
          main.scrollIntoView({ block: "start", behavior: "instant" });
        schedule();
      }
      describe(
        element && document.getElementById("tutorial-indicacion")
          ? element
          : undefined,
      );
      if (!bounds || bounds.bottom - bounds.top < 44) {
        setLayout(null);
        return;
      }
      // Un diálogo nativo queda en la capa superior del navegador. La ayuda se monta
      // dentro de él y puede ocupar el espacio libre de la pantalla a sus lados.
      const area = box(dialog ? screen : bounds);
      let clipped = rect && intersect(rect, bounds);
      // Un grupo de casillas puede ser más alto que la pantalla con zoom o con
      // el teclado abierto. Guiar la primera casilla pendiente deja espacio para leer.
      const anchorHeight = Math.max(30, area.height * 0.42);
      if (clipped && element && clipped.bottom - clipped.top > anchorHeight) {
        const field = element.querySelector<HTMLElement>(
          "input:not(:checked):enabled,select:enabled,textarea:enabled,button:enabled",
        );
        const fieldRect =
          field && intersect(field.getBoundingClientRect(), clipped);
        clipped = fieldRect ?? {
          ...clipped,
          bottom: clipped.top + anchorHeight,
        };
      }
      const target = clipped
        ? {
            left: Math.max(0, clipped.left - area.left - 3),
            top: Math.max(0, clipped.top - area.top - 3),
            width: clipped.right - clipped.left + 6,
            height: clipped.bottom - clipped.top + 6,
          }
        : undefined;
      const naturalHeight =
        card.current?.firstElementChild?.getBoundingClientRect().height ?? 260;
      const measuredCard = card.current;
      const compactView = measuredCard?.dataset.compact === "true";
      const placement = position(area, target, naturalHeight + 4, compactView);
      if (
        !framed &&
        element &&
        target &&
        measuredCard?.dataset.anchor === anchorKey
      ) {
        framed = true;
        if (
          !compactView &&
          placement.card.maxHeight < naturalHeight + 4 &&
          !document.activeElement?.matches(
            "input, textarea, select, [contenteditable=true]",
          )
        ) {
          makeRoom(element, dialog, area, target, naturalHeight + 4);
        }
      }
      setLayout({ host: dialog ?? document.body, area, target, ...placement });
    }
    function schedule() {
      if (!frame) frame = requestAnimationFrame(measure);
    }
    const observer = new MutationObserver((changes) => {
      if (
        changes.some(
          ({ target }) =>
            !(target instanceof Element && target.closest(".tutorial-overlay")),
        )
      )
        schedule();
    });
    observer.observe(document.body, {
      subtree: true,
      childList: true,
      attributes: true,
      attributeFilter: ["hidden", "open", "class", "disabled", "aria-expanded"],
    });
    const resize = new ResizeObserver(schedule);
    const main = document.querySelector(".admin-main,.client-main");
    if (main) resize.observe(main);
    // El contenido puede ensancharse, plegarse o cambiar de rol sin un cambio de selector.
    resizeContent.current = schedule;
    // El PDF rasterizado puede desplazar el paginador sin cambiar el tamaño del botón.
    document.addEventListener("load", schedule, true);
    window.addEventListener("scroll", schedule, true);
    const onResize = () => {
      // Un cambio de ancho puede mover la columna de trabajo debajo del menú.
      // El teclado virtual suele cambiar solo la altura.
      if (window.innerWidth !== width) {
        width = window.innerWidth;
        first = true;
        framed = false;
      }
      schedule();
    };
    window.addEventListener("resize", onResize);
    window.visualViewport?.addEventListener("resize", schedule);
    window.visualViewport?.addEventListener("scroll", schedule);
    schedule();
    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
      resize.disconnect();
      describe();
      document.removeEventListener("load", schedule, true);
      window.removeEventListener("scroll", schedule, true);
      window.removeEventListener("resize", onResize);
      window.visualViewport?.removeEventListener("resize", schedule);
      window.visualViewport?.removeEventListener("scroll", schedule);
      resizeContent.current = () => {};
    };
  }, [selector, anchorKey]);
  const host = layout?.host;
  useEffect(() => {
    if (!card.current) return;
    const observe = new ResizeObserver(() => resizeContent.current());
    observe.observe(card.current.firstElementChild ?? card.current);
    return () => observe.disconnect();
  }, [host]);
  if (!layout) return null;
  return createPortal(
    <div
      className="tutorial-overlay tutorial-highlight-area"
      style={layout.area}
    >
      {layout.target && (
        <div
          aria-hidden="true"
          className="tutorial-highlight"
          style={layout.target}
        />
      )}
      {layout.arrow && (
        <svg aria-hidden="true" className="tutorial-arrow">
          <defs>
            <marker
              id="tutorial-punta"
              markerWidth="6"
              markerHeight="6"
              refX="5"
              refY="3"
              orient="auto"
            >
              <path d="M0 0 L6 3 L0 6 Z" />
            </marker>
          </defs>
          <line
            x1={layout.arrow[0].x}
            y1={layout.arrow[0].y}
            x2={layout.arrow[1].x}
            y2={layout.arrow[1].y}
            markerEnd="url(#tutorial-punta)"
          />
        </svg>
      )}
      <section
        ref={card}
        className="tutorial-callout"
        data-compact={compact}
        data-anchor={anchorKey}
        aria-label="Indicación del tutorial"
        style={layout.card}
      >
        <div className="tutorial-callout-content">
          <button
            type="button"
            className="tutorial-fold"
            aria-expanded={!compact}
            aria-controls="tutorial-explicacion"
            onClick={() => setCompact(!compact)}
          >
            {compact ? "Mostrar explicación" : "Plegar"}
          </button>
          <div id="tutorial-explicacion" hidden={compact}>
            {children}
          </div>
        </div>
      </section>
    </div>,
    layout.host,
  );
}
