"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { MutationError, secureMutation } from "@/lib/secure-mutation";
import { adminLessons } from "@/lib/tutorial-administrador";
import { clientLessons } from "@/lib/tutorial-cliente";
import {
  lessonPath,
  matchesTutorialPath,
  type RoleGuide,
  type RoleProgressList,
  type RoleLesson,
} from "@/lib/tutorial-recorridos";
import { useTutorial } from "./tutorial-context";

export const roleLessons: Record<RoleGuide, RoleLesson[]> = {
  administrador: adminLessons,
  cliente: clientLessons,
};
export const guideRole = {
  administrador: "ADMIN_ADMIN",
  cliente: "CLIENTE",
} as const;
const endpoint = "/api/demo/tutorial/recorridos";
type Patch = {
  lesson?: string;
  task?: number;
  reached?: number;
  complete?: boolean;
  draft?: Record<string, string>;
};
type Store = {
  data: RoleProgressList | null;
  error: string;
  refresh: () => Promise<void>;
  write: (guide: RoleGuide, patch: Patch) => Promise<RoleProgressList>;
  restart: (guide: RoleGuide) => Promise<RoleProgressList>;
  flush: () => Promise<void>;
  go: (guide: RoleGuide, lesson: string, task?: number) => Promise<void>;
};
const StoreContext = createContext<Store | null>(null);
export const useRoleStore = () => useContext(StoreContext);

export function taskNumber(
  lessons: RoleLesson[],
  lesson: string,
  task: number,
) {
  let offset = 0;
  for (const item of lessons) {
    if (item.id === lesson) return offset + task;
    offset += item.tasks.length;
  }
  throw Error("No se encontró la lección.");
}

export function RoleTutorialProvider({ children }: { children: ReactNode }) {
  const tutorial = useTutorial();
  const enabled = !!tutorial?.guideOpen;
  const [data, setData] = useState<RoleProgressList | null>(null);
  const [error, setError] = useState("");
  const queue = useRef<Promise<unknown>>(Promise.resolve());
  const pending = useRef(0);
  const readVersion = useRef(0);
  const restarting = useRef<
    Partial<Record<RoleGuide, Promise<RoleProgressList>>>
  >({});
  const retryRestart = useRef<Partial<Record<RoleGuide, number>>>({});
  const alive = useRef(true);
  const latest = useRef<RoleProgressList | null>(null);
  const failedDrafts = useRef<
    Partial<
      Record<RoleGuide, { intento: number; campos: Record<string, string> }>
    >
  >({});
  const accept = useCallback((next: RoleProgressList) => {
    for (const guide of ["administrador", "cliente"] as const) {
      const received = next?.[guide];
      if (
        !received?.draft ||
        !Number.isSafeInteger(received.intento) ||
        received.intento < 0
      )
        throw Error("El avance recibido está incompleto.");
      const previous = latest.current?.[guide];
      if (previous && previous.intento > received.intento)
        next = { ...next, [guide]: previous };
      if (previous && previous.intento < received.intento)
        delete failedDrafts.current[guide];
    }
    latest.current = next;
    if (alive.current) {
      setData(next);
      setError("");
    }
    return next;
  }, []);
  const refresh = useCallback(async () => {
    const sequence = ++readVersion.current;
    try {
      const r = await fetch(endpoint, {
        cache: "no-store",
        credentials: "same-origin",
      });
      if (!r.ok)
        throw Error("No pudimos cargar los recorridos de esta instalación.");
      const result = await r.json();
      if (alive.current && sequence === readVersion.current) accept(result);
    } catch (e) {
      if (alive.current && sequence === readVersion.current)
        setError(e instanceof Error ? e.message : "No pudimos leer el avance.");
    }
  }, [accept]);
  useEffect(() => {
    alive.current = true;
    if (enabled) void refresh();
    return () => {
      alive.current = false;
      ++readVersion.current;
    };
  }, [enabled, refresh]);
  const adminAttempt = data?.administrador.intento,
    clientAttempt = data?.cliente.intento;
  const write = useCallback(
    (guide: RoleGuide, patch: Patch) => {
      const intento = guide === "administrador" ? adminAttempt : clientAttempt;
      if (
        intento === undefined ||
        latest.current?.[guide].intento !== intento ||
        restarting.current[guide]
      )
        return Promise.reject(
          Error("Volvé al paso actual del tutorial para continuar."),
        );
      pending.current++;
      const run = queue.current.then(async () => {
        ++readVersion.current;
        const failed = failedDrafts.current[guide];
        const oldDraft =
          failed?.intento === intento ? failed.campos : undefined;
        const draft =
          oldDraft || patch.draft ? { ...oldDraft, ...patch.draft } : undefined;
        try {
          const r = await secureMutation(
            endpoint,
            JSON.stringify({ ...patch, draft, recorrido: guide, intento }),
            "application/json",
            "PUT",
          );
          const result = await r.json();
          ++readVersion.current;
          const next = accept(result);
          delete failedDrafts.current[guide];
          const retriedAttempt = retryRestart.current[guide];
          if (retriedAttempt !== undefined && intento > retriedAttempt)
            delete retryRestart.current[guide];
          return next;
        } catch (e) {
          if (draft && latest.current?.[guide].intento === intento)
            failedDrafts.current[guide] = { intento, campos: draft };
          if (e instanceof MutationError && e.status === 409) void refresh();
          if (alive.current)
            setError(
              e instanceof Error ? e.message : "No pudimos guardar el avance.",
            );
          throw e;
        } finally {
          pending.current--;
        }
      });
      queue.current = run.catch(() => {});
      return run;
    },
    [accept, refresh, adminAttempt, clientAttempt],
  );
  const restart = useCallback(
    (guide: RoleGuide) => {
      const active = restarting.current[guide];
      if (active) return active;
      const expected =
        retryRestart.current[guide] ??
        (guide === "administrador" ? adminAttempt : clientAttempt);
      if (expected === undefined)
        return Promise.reject(Error("Esperá a que cargue el tutorial."));
      retryRestart.current[guide] = expected;
      pending.current++;
      const run = queue.current
        .then(async () => {
          ++readVersion.current;
          const response = await secureMutation(
            endpoint,
            JSON.stringify({
              recorrido: guide,
              accion: "reiniciar",
              intento: expected,
            }),
            "application/json",
            "PUT",
          );
          const result = await response.json();
          ++readVersion.current;
          const next = accept(result);
          delete failedDrafts.current[guide];
          delete retryRestart.current[guide];
          return next;
        })
        .catch((error) => {
          if (error instanceof MutationError && !error.uncertain)
            delete retryRestart.current[guide];
          if (error instanceof MutationError && error.status === 409)
            void refresh();
          throw error;
        })
        .finally(() => {
          pending.current--;
          delete restarting.current[guide];
        });
      restarting.current[guide] = run;
      queue.current = run.catch(() => {});
      return run;
    },
    [accept, refresh, adminAttempt, clientAttempt],
  );
  const flush = useCallback(async () => {
    await queue.current;
    for (const key of Object.keys(failedDrafts.current) as RoleGuide[]) {
      await write(key, {});
    }
  }, [write]);
  useEffect(() => {
    const beforeUnload = (event: BeforeUnloadEvent) => {
      if (pending.current || Object.keys(failedDrafts.current).length) {
        event.preventDefault();
        event.returnValue = "";
      }
    };
    window.addEventListener("beforeunload", beforeUnload);
    return () => window.removeEventListener("beforeunload", beforeUnload);
  }, []);
  const go = useCallback(
    async (guide: RoleGuide, id: string, task = 0) => {
      const lessons = roleLessons[guide];
      const lesson = lessons.find((item) => item.id === id);
      if (!lesson?.tasks[task]) throw Error("No se encontró esta indicación.");
      await flush();
      const path = lessonPath(
        lesson,
        task,
        latest.current?.[guide].draft ?? {},
      );
      if (!path)
        throw Error("Primero prepará el trabajo para abrir este paso.");
      await write(guide, {
        lesson: id,
        task,
        reached: taskNumber(lessons, id, task),
        draft: latest.current?.[guide].complete ? { repaso: "si" } : undefined,
      });
      if (!restarting.current[guide] && !matchesTutorialPath(path))
        location.assign(path);
    },
    [flush, write],
  );
  return (
    <StoreContext.Provider
      value={{ data, error, write, refresh, flush, go, restart }}
    >
      {children}
    </StoreContext.Provider>
  );
}

export function useRoleTutorial(guide: RoleGuide) {
  const tutorial = useTutorial(),
    store = useRoleStore();
  const write = store?.write,
    go = store?.go;
  const saveDraft = useCallback(
    async (draft: Record<string, string>) => {
      if (write) await write(guide, { draft });
    },
    [guide, write],
  );
  const goTo = useCallback(
    async (lesson: string, task = 0) => {
      if (go) await go(guide, lesson, task);
    },
    [guide, go],
  );
  const saved = store?.data?.[guide];
  if (
    !tutorial?.guideOpen ||
    tutorial.guide !== guide ||
    tutorial.role !== guideRole[guide] ||
    !saved
  )
    return null;
  return {
    ...saved,
    lesson: saved.lesson || roleLessons[guide][0].id,
    saveDraft,
    goTo,
  };
}
