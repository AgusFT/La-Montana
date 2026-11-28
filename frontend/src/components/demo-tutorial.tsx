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
import { switchTutorialRole } from "@/lib/tutorial-cambio-rol";
import {
  GuideSelector,
  ChooseAnotherGuide,
  RolePrompt,
  RoleGuidePanel,
} from "./tutorial-selector";
import { useRoleStore } from "./role-tutorial-context";
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

function focusGuide() {
  requestAnimationFrame(() =>
    document.getElementById("tutorial-titulo")?.focus({ preventScroll: true }),
  );
}
function canGuide(step: Step, role: Role) {
  return (
    lessons[step].role === role || (step === "entrega" && role === "CLIENTE")
  );
}
function selectedDeliveryMode() {
  return (
    document.querySelector<HTMLSelectElement>('#client-delivery select[aria-label="Modalidad"]')?.value ?? ""
  );
}
export function TutorialButton() {
  const api = useTutorial(),
    store = useRoleStore();
  const [busy, setBusy] = useState(false),
    [message, setMessage] = useState("");
  if (!api?.available) return null;
  const state = api.state;
  async function open() {
    if (!api || busy) return;
    setBusy(true);
    setMessage("");
    try {
      await store?.flush();
      await api.flush();
      api.choose(null);
      api.setOpen(true);
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "No pudimos recuperar el tutorial.",
      );
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
        aria-expanded={api.guideOpen}
        aria-controls="tutorial-panel"
        onClick={() => void open()}
      >
        {api.guideOpen ? "Ir al tutorial" : "Empezar tutorial"}
      </button>
      {(message || api.error) && !api.guideOpen && (
        <span className="tutorial-start-error" role="alert">
          {message || api.error}
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
  const api = useTutorial();
  if (!api?.guideOpen) return null;
  if (!api.guide) return <GuideSelector />;
  if (api.guide !== "completo")
    return <RoleGuidePanel key={api.guide} guide={api.guide} />;
  return <FullTutorialPanel />;
}

function FullTutorialPanel() {
  const pathname = usePathname();
  const api = useTutorial(),
    state = api?.state,
    role = api?.role;
  const [index, setIndex] = useState(false),
    [hint, setHint] = useState(0),
    [switchTo, setSwitchTo] = useState<Role | null>(null);
  const [deliveryMode, setDeliveryMode] = useState("");
  const [notice, setNotice] = useState(""),
    [busy, setBusy] = useState(false),
    [navigation, setNavigation] = useState(0),
    [rateViewed, setRateViewed] = useState(false);
  const startLock = useRef(false);
  const [deferredStep, setDeferredStep] = useState<Step | null>(null);
  useEffect(() => {
    const compact = matchMedia("(max-width: 1080px)");
    const resetIndex = () => setIndex(false);
    compact.addEventListener("change", resetIndex);
    return () => compact.removeEventListener("change", resetIndex);
  }, []);
  const active =
    api?.available &&
    api.open &&
    (state?.estado === "ACTIVO" || state?.estado === "COMPLETO");
  const step = state?.paso ?? "tarifa",
    lesson = lessons[step],
    hints = useMemo(
      () => lesson.hints.filter((h) => !h.role || h.role === role),
      [lesson, role],
    ),
    hintNumber = Math.min(hint, hints.length - 1),
    currentHint = hints[hintNumber];

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
      setNavigation((n) => n + 1);
      setIndex(false);
      setSwitchTo(null);
      focusGuide();
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
    if (!here || step !== "confirmacion") return;
    const read = () => setDeliveryMode(selectedDeliveryMode());
    const observer = new MutationObserver(read);
    observer.observe(document.body, { childList: true, subtree: true });
    document.addEventListener("change", read);
    read();
    return () => {
      observer.disconnect();
      document.removeEventListener("change", read);
    };
  }, [here, step]);
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
      if (
        control.closest(".tutorial-callout") ||
        !control.closest(".admin-main,.client-main,.workspace-review-dialog")
      )
        return;
      const at = hints.findIndex(({ selector }) => control.closest(selector));
      if (event.type === "focusin") {
        if (at >= 0) setHint(at);
        return;
      }
      const clickedButton = control.closest("button");
      if (
        event.type === "click" &&
        clickedButton?.type === "submit" &&
        clickedButton.form
      )
        return;
      const form =
        event.type === "submit" && control instanceof HTMLFormElement
          ? control
          : null;
      const from = form
        ? hints.reduce(
            (last, { selector }, i) =>
              form.querySelector(selector) ? i : last,
            -1,
          )
        : at;
      const codeReady =
        event.type === "input" &&
        control instanceof HTMLInputElement &&
        control.matches(
          '[data-tutorial-dialogo="VALIDAR"] input[name="codigo"]',
        ) &&
        control.value.length === control.maxLength &&
        control.validity.valid;
      if (
        from < 0 ||
        !hints[from + 1] ||
        (!form &&
          !codeReady &&
          !(
            (event.type === "click" && control.closest("button,summary")) ||
            (event.type === "change" &&
              control.matches("select:valid,input:valid,textarea:valid") &&
              (!control.matches('input[type="checkbox"]') || control.matches(":checked")))
          ))
      )
        return;
      stop();
      const advanceWhenReady = () => {
        if (form?.getClientRects().length) return;
        const mode = selectedDeliveryMode();
        const following = hints.findIndex(
          (hint, i) => i > from && (!hint.mode || hint.mode === mode),
        );
        if (following < 0) return;
        const next = visibleTarget(hints[following].selector);
        if (!next || next.matches(":disabled")) return;
        stop();
        setHint(following);
      };
      observer = new MutationObserver(advanceWhenReady);
      observer.observe(document.body, {
        subtree: true,
        childList: true,
        attributes: true,
        attributeFilter: ["hidden", "open", "disabled", "data-tutorial-revision", "data-tutorial-pdf", "data-aceptados"],
      });
      frame = requestAnimationFrame(advanceWhenReady);
    }
    for (const type of ["focusin", "click", "change", "input", "submit"])
      document.addEventListener(type, track, true);
    return () => {
      stop();
      for (const type of ["focusin", "click", "change", "input", "submit"])
        document.removeEventListener(type, track, true);
    };
  }, [here, hints]);
  const done = !!state?.completados.includes(step);
  useEffect(() => {
    if (active) focusGuide();
  }, [active, step]);
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
      focusGuide();
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
      if (role && !canGuide(needed, role)) {
        setDeferredStep(needed);
        setSwitchTo(lessons[needed].role);
        setIndex(false);
        return;
      }
      setDeferredStep(null);
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
      await switchTutorialRole(next);
      const destination = deferredStep ?? automaticHandoff ?? step;
      const progress =
        destination === step
          ? state!
          : await api!.command({ accion: "ir", paso: destination });
      location.replace(tutorialPath(progress.paso, progress, next));
    });
  }
  function chooseHint(number: number) {
    let available = number;
    while (available > 0 && !visibleTarget(hints[available].selector))
      available--;
    setNotice(
      available === number
        ? ""
        : `Para «${hints[number].title}», primero completá «${hints[available].title}».`,
    );
    setHint(available);
    setIndex(false);
    if (visibleTarget(hints[available].selector))
      focusTarget(hints[available].selector);
  }
  async function closeGuide() {
    await work(async () => {
      if (state!.estado === "ACTIVO") await api!.command({ accion: "pausar" });
      api!.setOpen(false);
      document.querySelector<HTMLButtonElement>(".tutorial-start")?.focus();
    });
  }
  const complete = state.estado === "COMPLETO";
  const next = steps[stepNumber];
  const automaticHandoff =
    !complete && done && next && capable && !canGuide(next, role) ? next : null;
  const handoffStep = deferredStep ?? automaticHandoff ?? step;
  const destinationRole = switchTo ?? lessons[handoffStep].role;
  const needsRole = !!switchTo || !capable || !!automaticHandoff;
  const guiding = !complete && !needsRole && here && !done;
  const activeHints = hints
    .map((item, n) => ({ item, n }))
    .filter(({ item }) => !item.mode || item.mode === deliveryMode);
  const previousHint = activeHints.filter(({ n }) => n < hintNumber).at(-1);
  const followingHint = activeHints.find(({ n }) => n > hintNumber);
  const heading = complete
    ? "Pedido completado"
    : needsRole
      ? `Continuar como ${roleNames[destinationRole]}`
      : done || !here
        ? lesson.title
        : (currentHint?.title ?? lesson.title);
  const progress = `Paso ${stepNumber} de ${steps.length} · ${roleNames[step === "entrega" && capable ? role : lesson.role]}`;
  return (
    <>
      <aside
        id="tutorial-panel"
        className="tutorial-panel"
        aria-label="Tutorial interactivo"
      >
        <ChooseAnotherGuide />
        <header>
          <h2>Recorrido del pedido</h2>
          <button
            type="button"
            className="tutorial-pause"
            onClick={() => void closeGuide()}
            disabled={busy}
          >
            {complete ? "Cerrar" : "Pausar"}
          </button>
        </header>
        <p className="tutorial-count">
          {state.completados.length} de {steps.length} pasos completados
        </p>
        <button
          type="button"
          className="tutorial-index-toggle"
          aria-controls="tutorial-pasos"
          aria-expanded={index}
          onClick={() => setIndex(!index)}
        >
          {index ? "Volver a la indicación" : "Ver todos los pasos"}
        </button>
        <nav
          id="tutorial-pasos"
          className={index ? "is-open" : ""}
          aria-label="Pasos del tutorial"
        >
          <ol>
            {steps.map((id, i) => {
              const selected = step === id;
              const checked = state.completados.includes(id);
              const unavailable = firstRequired(id, state.completados) !== id;
              return (
                <li key={id}>
                  <button
                    type="button"
                    aria-current={selected ? "step" : undefined}
                    className="tutorial-branch"
                    disabled={busy || complete}
                    onClick={() => void select(id)}
                  >
                    <span className="tutorial-step-number" aria-hidden="true">
                      {checked ? "✓" : i + 1}
                    </span>
                    <span>
                      <span className="tutorial-sr">{i + 1}. </span>
                      {lessons[id].title}
                      <small>
                        {checked
                          ? "Completado"
                          : selected
                            ? "Paso actual"
                            : unavailable
                              ? "Requiere pasos previos"
                              : "Disponible"}
                      </small>
                    </span>
                  </button>
                  {selected && !complete && (
                    <ol
                      className="tutorial-subtasks"
                      aria-label={`Subtareas de ${lesson.title}`}
                    >
                      {(capable ? activeHints : lesson.hints.map((item, n) => ({ item, n }))).map(({ item, n }) => (
                        <li key={item.selector}>
                          <button
                            type="button"
                            aria-current={
                              guiding && hintNumber === n ? "true" : undefined
                            }
                            disabled={busy || !guiding}
                            onClick={() => chooseHint(n)}
                          >
                            {item.title}
                          </button>
                        </li>
                      ))}
                    </ol>
                  )}
                </li>
              );
            })}
          </ol>
        </nav>
      </aside>
      {!index && (
        <TutorialHighlight
          selector={
            needsRole && !complete
              ? "[data-tutorial-cambiar-rol]"
              : guiding
                ? currentHint?.selector
                : undefined
          }
          anchorKey={`${step}-${role}-${hintNumber}-${heading}-${navigation}`}
        >
          <header className="tutorial-card-header">
            <span>{complete ? "Tutorial de La Montaña" : progress}</span>
            <button
              type="button"
              className="tutorial-card-index"
              aria-label="Ver pasos del tutorial"
              aria-controls="tutorial-pasos"
              onClick={() => {
                setIndex(true);
                requestAnimationFrame(() => {
                  const toggle = document.querySelector<HTMLButtonElement>(
                    ".tutorial-index-toggle",
                  );
                  toggle?.closest("aside")?.scrollIntoView({ block: "start" });
                  toggle?.focus({ preventScroll: true });
                });
              }}
            >
              Pasos
            </button>
            <button
              type="button"
              className="tutorial-card-dismiss"
              aria-label={complete ? "Cerrar guía" : "Pausar tutorial"}
              disabled={busy}
              onClick={() => void closeGuide()}
            >
              {complete ? "Cerrar" : "Pausar"}
            </button>
          </header>
          <h2 id="tutorial-titulo" tabIndex={-1}>
            {heading}
          </h2>
          {notice && <p role="status">{notice}</p>}
          {state.bloqueo && <p role="alert">{state.bloqueo}</p>}
          {complete ? (
            <>
              <p>
                Tu pedido y sus operaciones quedaron guardados en la imprenta.
              </p>
              {state.pedido && (
                <a href={tutorialPath("entrega", state, role)}>
                  Abrir el pedido de práctica
                </a>
              )}
            </>
          ) : needsRole ? (
            <>
              <RolePrompt
                next={destinationRole}
                busy={busy}
                reason={
                  handoffStep !== step && done
                    ? lesson.result + " " + lessons[handoffStep].intro
                    : step === "entrega"
                      ? destinationRole === "CLIENTE"
                        ? "El cliente es quien recibe el código del pedido. Abrí su cuenta para obtenerlo."
                        : "El personal de la imprenta valida el código presentado por el cliente y registra la entrega."
                      : lessons[handoffStep].intro
                }
                onConfirm={() => void changeRole(destinationRole)}
              />
              {switchTo && capable && (
                <button
                  type="button"
                  onClick={() => {
                    setSwitchTo(null);
                    setDeferredStep(null);
                  }}
                >
                  Seguir en mi rol
                </button>
              )}
            </>
          ) : done ? (
            <>
              <p className="tutorial-verified" role="status">
                {lesson.result}
              </p>
              {next && (
                <button
                  type="button"
                  className="tutorial-next"
                  disabled={busy}
                  onClick={() => void select(next)}
                >
                  Continuar con {lessons[next].title.toLowerCase()}
                </button>
              )}
            </>
          ) : !here ? (
            <>
              <p>{lesson.intro}</p>
              <button
                type="button"
                className="tutorial-next"
                disabled={busy}
                onClick={() => void work(async () => go(href))}
              >
                Abrir {lesson.title.toLowerCase()}
              </button>
            </>
          ) : (
            currentHint && (
              <>
                {hintNumber === 0 && (
                  <p className="tutorial-purpose">{lesson.intro}</p>
                )}
                <p
                  className="tutorial-instruction"
                  id="tutorial-indicacion"
                  aria-live="polite"
                  aria-atomic="true"
                >
                  {currentHint.text}
                </p>
                {step === "tarifa" && (
                  <>
                    {!rateViewed && (
                      <p role="status">
                        Para confirmar, buscá una tarifa de impresión habilitada
                        con el estado «Guardada · vigente».
                      </p>
                    )}
                    <button
                      type="button"
                      className="tutorial-next"
                      disabled={busy || !rateViewed}
                      onClick={() =>
                        void work(async () => {
                          await api.command({
                            accion: "tarifa",
                            paso: "tarifa",
                          });
                        })
                      }
                    >
                      Confirmar que revisé la tarifa
                    </button>
                  </>
                )}
                {step === "archivo" && hintNumber === 0 && (
                  <a href="/tutorial/afiche-senderos.pdf" download>
                    Guardar PDF de práctica
                  </a>
                )}
                {step === "entrega" &&
                  (role === "CLIENTE" || hintNumber === 0) && (
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
                <div className="tutorial-controls">
                  <button type="button" onClick={() => chooseHint(hintNumber)}>
                    Ir al control
                  </button>
                  {previousHint && (
                    <button
                      type="button"
                      onClick={() => chooseHint(previousHint.n)}
                    >
                      Anterior
                    </button>
                  )}
                  {followingHint && (
                    <button
                      type="button"
                      onClick={() => chooseHint(followingHint.n)}
                    >
                      Siguiente indicación
                    </button>
                  )}
                </div>
              </>
            )
          )}
          {api.error && (
            <p role="alert">
              {api.error}{" "}
              <button type="button" onClick={() => void work(api.refresh)}>
                Reintentar
              </button>
            </p>
          )}
        </TutorialHighlight>
      )}
    </>
  );
}
