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
import { secureMutation } from "@/lib/secure-mutation";
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
  const alive = useRef(true);
  const latest = useRef<RoleProgressList | null>(null);
  const failedDrafts = useRef<
    Partial<Record<RoleGuide, Record<string, string>>>
  >({});
  const accept = useCallback((next: RoleProgressList) => {
    if (!next?.administrador?.draft || !next?.cliente?.draft)
      throw Error("El avance recibido está incompleto.");
    latest.current = next;
    if (alive.current) {
      setData(next);
      setError("");
    }
    return next;
  }, []);
  const refresh = useCallback(async () => {
    try {
      const r = await fetch(endpoint, {
        cache: "no-store",
        credentials: "same-origin",
      });
      if (!r.ok)
        throw Error("No pudimos cargar los recorridos de esta instalación.");
      accept(await r.json());
    } catch (e) {
      if (alive.current)
        setError(e instanceof Error ? e.message : "No pudimos leer el avance.");
    }
  }, [accept]);
  useEffect(() => {
    alive.current = true;
    if (enabled) void refresh();
    return () => {
      alive.current = false;
    };
  }, [enabled, refresh]);
  const write = useCallback(
    (guide: RoleGuide, patch: Patch) => {
      pending.current++;
      const run = queue.current.then(async () => {
        const oldDraft = failedDrafts.current[guide];
        const draft =
          oldDraft || patch.draft ? { ...oldDraft, ...patch.draft } : undefined;
        try {
          const r = await secureMutation(
            endpoint,
            JSON.stringify({ ...patch, draft, recorrido: guide }),
            "application/json",
            "PUT",
          );
          const next = accept(await r.json());
          delete failedDrafts.current[guide];
          return next;
        } catch (e) {
          if (draft) failedDrafts.current[guide] = draft;
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
    [accept],
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
      if (!matchesTutorialPath(path)) location.assign(path);
    },
    [flush, write],
  );
  return (
    <StoreContext.Provider value={{ data, error, write, refresh, flush, go }}>
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
