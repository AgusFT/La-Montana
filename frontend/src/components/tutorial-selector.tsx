"use client";

import { useEffect, useEffectEvent, useRef, useState } from "react";
import { useTutorial } from "./tutorial-context";
import {
  guideRole,
  roleLessons,
  useRoleStore,
  taskNumber,
} from "./role-tutorial-context";
import {
  guideNames,
  lessonPath,
  matchesTutorialPath,
  type Guide,
  type RoleGuide,
} from "@/lib/tutorial-recorridos";
import {
  lessons as fullLessons,
  roleNames,
  tutorialPath,
} from "@/lib/demo-tutorial";
import { switchTutorialRole } from "@/lib/tutorial-cambio-rol";
import {
  TutorialHighlight,
  visibleTarget,
  focusTarget,
} from "./tutorial-highlight";
import type { Role } from "@/lib/roles";
import { RestartTutorialButton } from "./tutorial-restart";

const introductions: Record<Guide, string> = {
  administrador: "Aprendé a preparar precios y configurar la imprenta.",
  cliente: "Prepará un PDF, cotizá y elegí si querés confirmar el pedido.",
  completo:
    "Seguí un pedido desde la tarifa hasta la entrega con los tres roles.",
};

export function RolePrompt({
  next,
  reason,
  onConfirm,
  busy,
}: {
  next: Role;
  reason: string;
  onConfirm: () => void;
  busy: boolean;
}) {
  return (
    <>
      <p id="tutorial-indicacion" data-tutorial-role-prompt aria-live="polite">
        {reason}
      </p>
      <p>
        La flecha señala dónde se cambia de rol habitualmente. Para conservar
        este recorrido, hacé el cambio con el botón de este cuadro.
      </p>
      <button
        type="button"
        className="tutorial-next"
        disabled={busy}
        onClick={onConfirm}
      >
        {busy ? "Comprobando el rol…" : "Continuar como " + roleNames[next]}
      </button>
    </>
  );
}

export function ChooseAnotherGuide() {
  const api = useTutorial(),
    store = useRoleStore();
  const [error, setError] = useState("");
  async function choose() {
    try {
      await store?.flush();
      await api?.flush();
      api?.choose(null);
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "No pudimos guardar el avance.",
      );
    }
  }
  return (
    <>
      <button
        type="button"
        className="tutorial-back"
        onClick={() => void choose()}
      >
        Elegir otro tutorial
      </button>
      {error && <p role="alert">{error}</p>}
    </>
  );
}

export function GuideSelector() {
  const api = useTutorial(),
    store = useRoleStore();
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const lock = useRef(false),
    title = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    title.current?.focus();
  }, []);
  if (!api || !store) return null;
  async function select(id: Guide) {
    if (!api || lock.current) return;
    lock.current = true;
    setBusy(true);
    setError("");
    try {
      await store!.flush();
      await api.flush();
      if (id === "completo") {
        let progress = api.state;
        if (!progress) throw Error("Todavía no se pudo leer el pedido guiado.");
        if (progress.estado === "NUEVO" || progress.estado === "PAUSADO")
          progress = await api.command({
            accion: progress.estado === "NUEVO" ? "iniciar" : "retomar",
          });
        api.choose(id);
        if (
          progress.estado === "ACTIVO" &&
          (fullLessons[progress.paso].role === api.role ||
            (progress.paso === "entrega" && api.role === "CLIENTE"))
        ) {
          const path = tutorialPath(progress.paso, progress, api.role);
          if (!matchesTutorialPath(path)) location.assign(path);
        }
      } else {
        const saved = store!.data?.[id];
        if (!saved) throw Error("Todavía no se pudo leer este recorrido.");
        api.choose(id);
        const lesson =
          roleLessons[id].find((item) => item.id === saved.lesson) ??
          roleLessons[id][0];
        if (
          api.role === guideRole[id] &&
          (!saved.complete || saved.draft.repaso === "si")
        ) {
          await store!.go(id, lesson.id, saved.task);
        }
      }
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "No pudimos abrir el recorrido.",
      );
    } finally {
      lock.current = false;
      setBusy(false);
    }
  }
  return (
    <aside
      id="tutorial-panel"
      className="tutorial-panel tutorial-picker"
      aria-label="Elegir un tutorial"
    >
      <header>
        <h2 ref={title} tabIndex={-1}>
          Elegí un tutorial
        </h2>
        <button
          className="tutorial-pause"
          type="button"
          onClick={() => api.setOpen(false)}
        >
          Cerrar
        </button>
      </header>
      {(["administrador", "cliente", "completo"] as const).map((id) => {
        const saved = id === "completo" ? null : store.data?.[id];
        const lesson =
          id === "completo"
            ? api.state?.estado === "COMPLETO"
              ? "Completado"
              : api.state?.estado !== "NUEVO" && api.state
                ? fullLessons[api.state.paso].title
                : ""
            : saved?.complete && saved.draft.repaso !== "si"
              ? "Completado"
              : roleLessons[id].find((item) => item.id === saved?.lesson)
                  ?.title;
        return (
          <button
            key={id}
            type="button"
            className="tutorial-choice"
            disabled={busy || (id === "completo" ? !api.state : !store.data)}
            onClick={() => void select(id)}
          >
            <strong>{guideNames[id]}</strong>
            <span>{introductions[id]}</span>
            {lesson && (
              <small>
                {lesson === "Completado" ? lesson : "Retomar · " + lesson}
              </small>
            )}
          </button>
        );
      })}
      {(error || store.error || api.error) && (
        <p role="alert">
          {error || store.error || api.error}{" "}
          <button
            type="button"
            onClick={() => {
              void store.refresh();
              void api.refresh();
            }}
          >
            Reintentar
          </button>
        </p>
      )}
    </aside>
  );
}

export function RoleGuidePanel({ guide, openRestart }: { guide: RoleGuide; openRestart: () => void }) {
  const api = useTutorial(),
    store = useRoleStore();
  const saved = store?.data?.[guide],
    lessons = roleLessons[guide];
  const lessonIndex = Math.max(
    0,
    lessons.findIndex((l) => l.id === saved?.lesson),
  );
  const lesson = lessons[lessonIndex];
  const taskIndex = Math.min(saved?.task ?? 0, lesson.tasks.length - 1),
    task = lesson.tasks[taskIndex];
  const [index, setIndex] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const [ready, setReady] = useState(false),
    [hash, setHash] = useState("");
  const lock = useRef(false);
  const sameRole = api?.role === guideRole[guide],
    complete = !!saved?.complete && saved.draft.repaso !== "si";
  const targetPath = lessonPath(lesson, taskIndex, saved?.draft ?? {});
  const here =
    typeof location !== "undefined" &&
    !!targetPath &&
    matchesTutorialPath(targetPath);
  useEffect(() => {
    const sync = () => setHash(location.hash);
    sync();
    window.addEventListener("hashchange", sync);
    return () => window.removeEventListener("hashchange", sync);
  }, []);
  const [currentCue, setCurrentCue] = useState<{
    task: typeof task;
    index: number;
  } | null>(null);
  const cue =
    (currentCue?.task === task && task.indicaciones?.[currentCue.index]) ||
    task;
  const nativeNext = useEffectEvent(() => {
    void next(true);
  });
  useEffect(() => {
    if (!sameRole || !here || complete || !saved || index) return;
    let frame = 0;
    let awaitingNativeResult = false;
    const sync = () => {
      frame = 0;
      const selected =
        task.indicaciones?.findIndex(
          ({ when, selector }) =>
            document.querySelector(when) && visibleTarget(selector),
        ) ?? -1;
      setCurrentCue((previous) =>
        previous?.task === task && previous.index === selected
          ? previous
          : { task, index: selected },
      );
      setReady(
        !task.readySelector || !!document.querySelector(task.readySelector),
      );
      if (
        awaitingNativeResult &&
        task.advanceOnClick &&
        document.querySelector(task.advanceOnClick.readySelector)
      ) {
        awaitingNativeResult = false;
        nativeNext();
      }
    };
    function schedule() {
      if (!frame) frame = requestAnimationFrame(sync);
    }
    function click(event: Event) {
      if (
        task.advanceOnClick &&
        event.target instanceof Element &&
        event.target.closest(task.advanceOnClick.selector)
      ) {
        awaitingNativeResult = true;
      }
      schedule();
    }
    const observer = new MutationObserver((changes) => {
      if (
        changes.some(
          ({ target, attributeName }) =>
            attributeName !== "aria-describedby" &&
            !(target instanceof Element && target.closest(".tutorial-overlay")),
        )
      )
        schedule();
    });
    observer.observe(document.body, {
      subtree: true,
      childList: true,
      attributes: true,
    });
    for (const event of ["input", "change", "focusin"])
      document.addEventListener(event, schedule, true);
    document.addEventListener("click", click, true);
    sync();
    return () => {
      observer.disconnect();
      cancelAnimationFrame(frame);
      for (const event of ["input", "change", "focusin"])
        document.removeEventListener(event, schedule, true);
      document.removeEventListener("click", click, true);
    };
  }, [here, sameRole, complete, !!saved, task, hash, index]);
  if (!api || !store) return null;
  const work = async (action: () => Promise<unknown>) => {
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    setError("");
    try {
      await store.flush();
      await action();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No pudimos continuar.");
    } finally {
      lock.current = false;
      setBusy(false);
    }
  };
  const go = (id: string, n = 0) =>
    work(async () => {
      await store.go(guide, id, n);
      setIndex(false);
    });
  const pause = () =>
    work(async () => {
      await api.flush();
      api.setOpen(false);
      requestAnimationFrame(() =>
        document.querySelector<HTMLButtonElement>(".tutorial-start")?.focus(),
      );
    });
  const next = (nativeConfirmed = false) =>
    work(async () => {
      if (
        !nativeConfirmed &&
        task.readySelector &&
        !document.querySelector(task.readySelector)
      )
        return;
      if (taskIndex + 1 < lesson.tasks.length)
        await store.go(guide, lesson.id, taskIndex + 1);
      else if (lessonIndex + 1 < lessons.length)
        await store.go(guide, lessons[lessonIndex + 1].id, 0);
      else {
        await store.write(guide, { complete: true, draft: { repaso: "" } });
      }
    });
  const retry = () => void work(store.refresh);
  if (!saved)
    return (
      <aside id="tutorial-panel" className="tutorial-panel">
        <p role="status">{store.error || "Recuperando el avance…"}</p>
        {store.error && <button onClick={retry}>Reintentar</button>}
        <ChooseAnotherGuide />
      </aside>
    );
  const selectTree = (i: number, n: number) => {
    if (
      taskNumber(lessons, lessons[i].id, n) <= saved.reached ||
      saved.complete
    ) {
      void go(lessons[i].id, n);
      return;
    }
    // Al pedir un paso posterior, volver a la primera indicación pendiente.
    for (const item of lessons) {
      const start = taskNumber(lessons, item.id, 0);
      if (saved.reached < start + item.tasks.length) {
        setIndex(false);
        void go(item.id, saved.reached - start).then(() =>
          setError(
            "Primero completá esta indicación para abrir la que elegiste.",
          ),
        );
        return;
      }
    }
  };
  const nextRole = guideRole[guide];
  return (
    <>
      <aside
        id="tutorial-panel"
        className="tutorial-panel"
        aria-label={guideNames[guide]}
      >
        <ChooseAnotherGuide />
        <header>
          <h2>{guideNames[guide]}</h2>
          <button
            type="button"
            className="tutorial-pause"
            onClick={() => void pause()}
            disabled={busy}
          >
            Pausar
          </button>
        </header>
        <p className="tutorial-count">
          {complete
            ? "Recorrido completado"
            : `Paso ${lessonIndex + 1} de ${lessons.length}`}
        </p>
        <RestartTutorialButton open={openRestart} disabled={busy} />
        <button
          type="button"
          className="tutorial-index-toggle"
          aria-expanded={index}
          aria-controls="tutorial-pasos"
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
            {lessons.map((item, i) => (
              <li key={item.id}>
                <button
                  type="button"
                  className="tutorial-branch"
                  aria-current={i === lessonIndex ? "step" : undefined}
                  disabled={!sameRole || busy}
                  onClick={() => selectTree(i, 0)}
                >
                  <span>{i + 1}</span>
                  <span>{item.title}</span>
                </button>
                {i === lessonIndex && (
                  <ol className="tutorial-subtasks">
                    {item.tasks.map((entry, j) => (
                      <li key={j}>
                        <button
                          type="button"
                          disabled={!sameRole || busy}
                          aria-current={j === taskIndex ? "true" : undefined}
                          onClick={() => selectTree(i, j)}
                        >
                          {entry.title}
                        </button>
                      </li>
                    ))}
                  </ol>
                )}
              </li>
            ))}
          </ol>
        </nav>
      </aside>
      {!index && (
        <TutorialHighlight
          selector={
            !sameRole
              ? "[data-tutorial-cambiar-rol]"
              : here && !complete
                ? cue.selector
                : undefined
          }
          anchorKey={
            guide +
            "-" +
            lesson.id +
            "-" +
            taskIndex +
            "-" +
            sameRole +
            "-" +
            here +
            "-" +
            complete
          }
        >
          <header className="tutorial-card-header">
            <span>
              {roleNames[nextRole]} · Paso {lessonIndex + 1} de {lessons.length}
            </span>
            <button
              type="button"
              className="tutorial-card-index"
              aria-label="Ver pasos del tutorial"
              onClick={() => {
                setIndex(true);
                requestAnimationFrame(() =>
                  document
                    .querySelector<HTMLButtonElement>(".tutorial-index-toggle")
                    ?.focus(),
                );
              }}
            >
              Pasos
            </button>
            <button type="button" onClick={() => void pause()} disabled={busy}>
              Pausar
            </button>
          </header>
          <h2 id="tutorial-titulo" tabIndex={-1}>
            {!sameRole
              ? "Continuar como " + roleNames[nextRole]
              : complete
                ? "Tutorial completado"
                : task.title}
          </h2>
          {!sameRole ? (
            <RolePrompt
              next={nextRole}
              busy={busy}
              reason={`Este recorrido se practica desde la cuenta de ${roleNames[nextRole]}. Tu avance queda guardado cuando cambiás de rol.`}
              onConfirm={() =>
                void work(async () => {
                  await switchTutorialRole(nextRole);
                  location.replace(targetPath ?? lesson.path);
                })
              }
            />
          ) : complete ? (
            <>
              <p id="tutorial-indicacion">
                Terminaste el recorrido. Podés repasar un paso desde el índice o
                elegir otro tutorial.
              </p>
              <ChooseAnotherGuide />
            </>
          ) : !here ? (
            <>
              <p id="tutorial-indicacion">
                La próxima indicación está en «{lesson.title}».
              </p>
              <button
                type="button"
                className="tutorial-next"
                onClick={() => void go(lesson.id, taskIndex)}
              >
                Abrir este paso
              </button>
            </>
          ) : (
            <>
              <p id="tutorial-indicacion" aria-live="polite">
                {cue.text}
              </p>
              {task.download && (
                <a download href={task.download.href}>
                  {task.download.label}
                </a>
              )}
              <div className="tutorial-controls">
                {!task.external && (
                  <button
                    type="button"
                    className="tutorial-next"
                    disabled={busy || !ready}
                    onClick={() => void next()}
                  >
                    {task.nextLabel ??
                      (taskIndex === lesson.tasks.length - 1 &&
                      lessonIndex === lessons.length - 1
                        ? "Terminar tutorial"
                        : "Continuar")}
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => {
                    if (visibleTarget(cue.selector)) focusTarget(cue.selector);
                  }}
                >
                  Ir al control
                </button>
              </div>
            </>
          )}
          {(error || store.error) && (
            <p role="alert">
              {error || store.error}{" "}
              <button type="button" onClick={retry}>
                Reintentar
              </button>
            </p>
          )}
          <RestartTutorialButton open={openRestart} disabled={busy} />
        </TutorialHighlight>
      )}
    </>
  );
}
