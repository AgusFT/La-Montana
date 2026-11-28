//#region ENCABEZADO · src/components/demo-tutorial.tsx
// Renderiza el recorrido accesible, la navegación por pasos y las transiciones entre roles.
//#endregion
"use client";
import { useEffect, useMemo, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { useTutorial } from "./tutorial-context";
import {
  focusTarget,
  TutorialHighlight,
  visibleTarget,
} from "./tutorial-highlight";
import { secureMutation } from "@/lib/secure-mutation";
import {
  firstRequired,
  lessons,
  roleNames,
  steps,
  tutorialPath,
  type Step,
  type Tutorial,
} from "@/lib/demo-tutorial";
import type { Role } from "@/lib/roles";
import "./demo-tutorial.css";

function focusPanel() {
  requestAnimationFrame(() => {
    const title = document.getElementById("tutorial-titulo");
    title?.focus({ preventScroll: true });
    if (matchMedia("(max-width: 1080px)").matches)
      title?.closest("aside")?.scrollIntoView({ block: "start" });
  });
}
function canGuide(step: Step, role: Role) {
  return (
    lessons[step].role === role || (step === "entrega" && role === "CLIENTE")
  );
}
export function TutorialButton() {
  const api = useTutorial(),
    [busy, setBusy] = useState(false);
  if (!api?.available) return null;
  const state = api.state;
  async function open() {
    if (!api || busy) return;
    setBusy(true);
    try {
      if (!state) {
        await api.refresh();
        return;
      }
      const progress =
        state.estado === "NUEVO" || state.estado === "PAUSADO"
          ? await api.command({
              accion: state.estado === "NUEVO" ? "iniciar" : "retomar",
            })
          : state;
      if (progress.estado === "ACTIVO" && canGuide(progress.paso, api.role)) {
        const path = tutorialPath(progress.paso, progress, api.role);
        if (!matchesRoute(path)) {
          await api.flush();
          location.assign(path);
        }
      }
      window.dispatchEvent(new Event("demo:tutorial:abrir"));
      focusPanel();
    } catch {
      /* El mensaje visible permite reintentar. */
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <button
        type="button"
        className="tutorial-start"
        disabled={busy || (!state && !api.error)}
        aria-controls="tutorial-panel"
        onClick={() => void open()}
      >
        {!state && api.error
          ? "Reintentar carga del tutorial"
          : state?.estado === "PAUSADO"
            ? "Retomar tutorial"
            : state?.estado === "ACTIVO"
              ? "Ver tutorial"
              : state?.estado === "COMPLETO"
                ? "Ver tutorial completado"
                : "Iniciar Tutorial"}
      </button>
      {api.error && state?.estado !== "ACTIVO" && (
        <span className="tutorial-start-error" role="alert">
          {api.error}
        </span>
      )}
    </>
  );
}
function matchesRoute(path: string) {
  const url = new URL(path, location.origin);
  return (
    location.pathname === url.pathname &&
    (!url.hash || location.hash === url.hash)
  );
}
export function TutorialPanel() {
  const pathname = usePathname();
  const api = useTutorial(),
    state = api?.state,
    role = api?.role;
  const [hidden, setHidden] = useState(false),
    [index, setIndex] = useState(false),
    [hint, setHint] = useState(0),
    [switchTo, setSwitchTo] = useState<Role | null>(null);
  const [notice, setNotice] = useState(""),
    [busy, setBusy] = useState(false),
    [navigation, setNavigation] = useState(0),
    [rateViewed, setRateViewed] = useState(false);
  const startLock = useRef(false);
  const [completedOpen, setCompletedOpen] = useState(false),
    previous = useRef<Tutorial["estado"] | undefined>(undefined);
  useEffect(() => {
    if (state?.estado === "COMPLETO" && previous.current === "ACTIVO")
      setCompletedOpen(true);
    previous.current = state?.estado;
  }, [state?.estado]);
  const active =
    !!state &&
    !!api?.available &&
    !hidden &&
    (state.estado === "ACTIVO" ||
      (state.estado === "COMPLETO" && completedOpen));
  const step = state?.paso ?? "tarifa",
    lesson = lessons[step],
    hints = useMemo(
      () =>
        lesson.hints.filter(([, , forRole]) => !forRole || forRole === role),
      [lesson, role],
    ),
    hintNumber = Math.min(hint, hints.length - 1),
    currentHint = hints[hintNumber];
  const hintSelector = currentHint?.[0];
  const stepNumber = steps.indexOf(step) + 1;
  useEffect(() => {
    try {
      const pending = sessionStorage.getItem("demo:tutorial:prerrequisito");
      if (pending) {
        setNotice(pending);
        sessionStorage.removeItem("demo:tutorial:prerrequisito");
      }
    } catch {
      /* La navegación funciona sin almacenamiento de navegador. */
    }
    const show = () => {
      setHidden(false);
      setCompletedOpen(true);
      setIndex(false);
      setSwitchTo(null);
      focusPanel();
    };
    const route = () => setNavigation((n) => n + 1);
    window.addEventListener("demo:tutorial:abrir", show);
    window.addEventListener("hashchange", route);
    return () => {
      window.removeEventListener("demo:tutorial:abrir", show);
      window.removeEventListener("hashchange", route);
    };
  }, []);
  useEffect(() => {
    setHint(0);
    setSwitchTo(null);
    setRateViewed(false);
  }, [step]);
  useEffect(() => {
    if (!active || step !== "tarifa") return;
    const sync = () =>
      setRateViewed(!!visibleTarget('[data-tutorial-tarifa-leida="true"]'));
    const observer = new MutationObserver(sync);
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ["hidden", "data-tutorial-tarifa-leida"],
    });
    sync();
    return () => observer.disconnect();
  }, [active, step, navigation, pathname]);
  const capable = !!role && canGuide(step, role);
  const href = state && role ? tutorialPath(step, state, role) : "";
  const here =
    active &&
    capable &&
    !!href &&
    typeof location !== "undefined" &&
    matchesRoute(href);
  useEffect(() => {
    if (!here) return;
    let observer: MutationObserver | undefined;
    let frame = 0;
    function stop() {
      observer?.disconnect();
      cancelAnimationFrame(frame);
    }
    function track(event: Event) {
      if (!(event.target instanceof Element)) return;
      const control = event.target;
      if (!control.closest(".admin-main,.client-main")) return;
      const at = hints.findIndex(([selector]) => control.closest(selector));
      if (event.type === "focusin") {
        if (at >= 0) setHint(at);
        return;
      }
      const form =
        event.type === "submit" && control instanceof HTMLFormElement
          ? control
          : null;
      const from = form
        ? hints.reduce(
            (last, [selector], i) => (form.querySelector(selector) ? i : last),
            -1,
          )
        : at;
      if (
        from < 0 ||
        !hints[from + 1] ||
        (!form &&
          !(
            (event.type === "click" && control.closest("button,summary")) ||
            (event.type === "change" &&
              control.matches("select,input[type=checkbox]"))
          ))
      )
        return;
      stop();
      const nextSelector = hints[from + 1][0];
      const advanceWhenReady = () => {
        if (form?.getClientRects().length) return;
        const next = visibleTarget(nextSelector);
        if (!next || next.matches(":disabled")) return;
        stop();
        setHint(from + 1);
      };
      observer = new MutationObserver(advanceWhenReady);
      observer.observe(document.body, {
        subtree: true,
        childList: true,
        attributes: true,
        attributeFilter: ["hidden", "open", "disabled"],
      });
      frame = requestAnimationFrame(advanceWhenReady);
    }
    for (const type of ["focusin", "click", "change", "submit"])
      document.addEventListener(type, track);
    return () => {
      stop();
      for (const type of ["focusin", "click", "change", "submit"])
        document.removeEventListener(type, track);
    };
  }, [here, hints]);
  const done = !!state?.completados.includes(step);
  const [away, setAway] = useState(false);
  useEffect(() => {
    const panel = document.getElementById("tutorial-panel");
    if (!active || !panel) return;
    const observer = new IntersectionObserver(([entry]) => {
      setAway(entry.boundingClientRect.bottom <= 0);
    });
    observer.observe(panel);
    return () => observer.disconnect();
  }, [active]);
  useEffect(() => {
    if (active) focusPanel();
  }, [active, step, here]);

  if (!api || !state || !role || !active) return null;
  async function work(fn: () => Promise<void>) {
    if (startLock.current) return;
    startLock.current = true;
    setBusy(true);
    setNotice("");
    try {
      await api!.flush();
      await fn();
    } catch (e) {
      setNotice(
        e instanceof Error
          ? e.message
          : "No pudimos continuar. Volvé a intentar.",
      );
    } finally {
      startLock.current = false;
      setBusy(false);
    }
  }
  function go(path: string) {
    if (matchesRoute(path)) {
      focusPanel();
      return;
    }
    location.assign(path);
  }
  function openFor(target: Step, data: Tutorial) {
    const destinationRole = lessons[target].role;
    if (role && !canGuide(target, role)) {
      setSwitchTo(destinationRole);
      return;
    }
    go(tutorialPath(target, data, role!));
  }
  async function select(requested: Step) {
    await work(async () => {
      const needed = firstRequired(requested, state!.completados);
      const result = await api!.command({ accion: "ir", paso: needed });
      const actual = result.paso;
      setIndex(false);
      setHint(0);
      if (actual !== requested) {
        const message = `Para «${lessons[requested].title}», primero completá «${lessons[actual].title}».`;
        setNotice(message);
        if (
          role === lessons[actual].role &&
          !matchesRoute(tutorialPath(actual, result, role!))
        ) {
          try {
            sessionStorage.setItem("demo:tutorial:prerrequisito", message);
          } catch {
            /* El paso activo también identifica el requisito. */
          }
        }
      }
      openFor(actual, result);
    });
  }
  async function changeRole(next: Role) {
    await work(async () => {
      await secureMutation(
        "/api/demo/sesion",
        JSON.stringify({
          rol: next === "ADMIN_ADMIN" ? "ADMINISTRADOR" : next,
        }),
        "application/json",
      );
      location.replace(tutorialPath(step, state!, next));
    });
  }
  function advanceHint(by: number) {
    const next = Math.max(0, Math.min(hints.length - 1, hintNumber + by));
    setHint(next);
    setNotice("");
    if (visibleTarget(hints[next][0])) focusTarget(hints[next][0]);
  }
  const next = steps[steps.indexOf(step) + 1];
  return (
    <aside
      id="tutorial-panel"
      className="tutorial-panel"
      aria-label="Tutorial interactivo"
    >
      <header>
        <div>
          <small className="tutorial-stage">
            {state.estado === "COMPLETO"
              ? "Tutorial de La Montaña"
              : `Paso ${stepNumber} de ${steps.length} · ${roleNames[step === "entrega" && capable ? role : lesson.role]}`}
          </small>
        </div>
        <button
          type="button"
          className="tutorial-pause"
          disabled={busy}
          onClick={() =>
            void work(async () => {
              if (state.estado === "ACTIVO")
                await api.command({ accion: "pausar" });
              setHidden(true);
              document
                .querySelector<HTMLButtonElement>(".tutorial-start")
                ?.focus();
            })
          }
        >
          {state.estado === "COMPLETO" ? "Cerrar" : "Pausar"}
        </button>
      </header>
      <h2 tabIndex={-1} id="tutorial-titulo">
        {state.estado === "COMPLETO" ? "Pedido completado" : lesson.title}
      </h2>
      {state.estado === "COMPLETO" ? (
        <>
          <p>
            Tu pedido y sus operaciones quedaron en la imprenta. Podés volver a
            consultarlos o practicar las bajas disponibles desde Cupos de la
            Demo.
          </p>
          {state.pedido && (
            <a href={tutorialPath("entrega", state, role)}>
              Abrir el pedido de práctica
            </a>
          )}
        </>
      ) : (
        <>
          {notice && <p role="status">{notice}</p>}
          {state.bloqueo && <p role="alert">{state.bloqueo}</p>}
          <p>{lesson.intro}</p>
          {switchTo || !capable ? (
            <div className="tutorial-switch">
              <strong>
                Continuar como {roleNames[switchTo ?? lesson.role]}
              </strong>
              <p>
                Este paso se hace desde ese rol. Al confirmar, la demo abrirá su
                sesión y conservará el pedido del tutorial.
              </p>
              <button
                type="button"
                disabled={busy}
                onClick={() => void changeRole(switchTo ?? lesson.role)}
              >
                Confirmar cambio a {roleNames[switchTo ?? lesson.role]}
              </button>
              {switchTo && capable && (
                <button type="button" onClick={() => setSwitchTo(null)}>
                  Seguir en mi rol
                </button>
              )}
            </div>
          ) : (
            <>
              {!here && (
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void work(async () => go(href))}
                >
                  Abrir {lesson.title.toLowerCase()}
                </button>
              )}
              {step === "archivo" && !done && (
                <a href="/tutorial/afiche-senderos.pdf" download>
                  Guardar PDF de práctica
                </a>
              )}
              {here && !done && currentHint && (
                <>
                  <div
                    className="tutorial-instruction"
                    aria-live="polite"
                    aria-atomic="true"
                  >
                    <strong>
                      {hints.length > 1
                        ? `Indicación ${hintNumber + 1} de ${hints.length}`
                        : "Qué hacer ahora"}
                    </strong>
                    <p>{currentHint[1]}</p>
                  </div>
                  <div className="tutorial-controls">
                    <button
                      type="button"
                      onClick={() => {
                        if (visibleTarget(currentHint[0])) {
                          setNotice("");
                          focusTarget(currentHint[0]);
                        } else {
                          setNotice(
                            "El control todavía no está visible. Revisá la indicación anterior y los avisos del formulario.",
                          );
                        }
                      }}
                    >
                      Ir al control
                    </button>
                    {hints.length > 1 && (
                      <>
                        <button
                          type="button"
                          disabled={hintNumber === 0}
                          onClick={() => advanceHint(-1)}
                        >
                          Anterior
                        </button>
                        <button
                          type="button"
                          disabled={hintNumber === hints.length - 1}
                          onClick={() => advanceHint(1)}
                        >
                          Siguiente indicación
                        </button>
                      </>
                    )}
                  </div>
                  {hintSelector && (
                    <TutorialHighlight key={step} selector={hintSelector} />
                  )}
                </>
              )}
              {here && step === "tarifa" && !done && !rateViewed && (
                <p role="status">
                  Para confirmar este paso, abrí «Tarifas de impresión» y buscá
                  una tarjeta con el estado «Guardada · vigente». Si no hay
                  ninguna habilitada, revisá la configuración del catálogo.
                </p>
              )}
              {here && step === "tarifa" && !done && (
                <button
                  type="button"
                  className="tutorial-next"
                  disabled={busy || !rateViewed}
                  onClick={() =>
                    void work(async () => {
                      await api.command({ accion: "tarifa", paso: "tarifa" });
                      setNotice("Tarifa vigente revisada.");
                    })
                  }
                >
                  Confirmar que revisé la tarifa
                </button>
              )}
              {step === "entrega" && !done && (
                <button
                  type="button"
                  onClick={() =>
                    setSwitchTo(role === "CLIENTE" ? "EMPLEADO" : "CLIENTE")
                  }
                >
                  {role === "CLIENTE"
                    ? "Volver a Empleado para registrar la entrega"
                    : "Pasar a Cliente para obtener el código"}
                </button>
              )}
            </>
          )}
          {done && (
            <p className="tutorial-verified" role="status">
              {lesson.result}
            </p>
          )}
          {done && next && (
            <button
              type="button"
              className="tutorial-next"
              disabled={busy}
              onClick={() => void select(next)}
            >
              Continuar con {lessons[next].title.toLowerCase()}
            </button>
          )}
          <button
            type="button"
            className="tutorial-index-toggle"
            aria-expanded={index}
            aria-controls="tutorial-pasos"
            onClick={() => setIndex((v) => !v)}
          >
            {index ? "Ocultar pasos" : "Ver todos los pasos"}
          </button>
          <nav
            id="tutorial-pasos"
            hidden={!index}
            aria-label="Pasos del tutorial"
          >
            <ol>
              {steps.map((id, i) => (
                <li key={id}>
                  <button
                    type="button"
                    aria-current={step === id ? "step" : undefined}
                    disabled={busy}
                    onClick={() => void select(id)}
                  >
                    <span>
                      {i + 1}. {lessons[id].title}
                    </span>
                    <small>
                      {state.completados.includes(id)
                        ? "Completado"
                        : firstRequired(id, state.completados) !== id
                          ? "Requiere pasos previos"
                          : step === id
                            ? "Actual"
                            : "Disponible"}
                    </small>
                  </button>
                </li>
              ))}
            </ol>
          </nav>
          <p className="tutorial-footer" role="status">
            {api.saving
              ? "Guardando borrador…"
              : "Podés pausar y retomar desde este paso."}
          </p>
        </>
      )}
      {away && (
        <button type="button" className="tutorial-return" onClick={focusPanel}>
          Volver a la guía · Paso {stepNumber}
        </button>
      )}

      {api.error && (
        <p role="alert">
          {api.error}{" "}
          <button type="button" onClick={() => void work(api.refresh)}>
            Reintentar
          </button>
        </p>
      )}
    </aside>
  );
}
