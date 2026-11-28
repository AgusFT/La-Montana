//#region ENCABEZADO · src/components/tutorial-context.tsx
// Coordina estado, mutaciones y borradores persistentes del tutorial.
//#endregion
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
import {
  rememberTutorialOpen,
  tutorialIsOpen,
  tutorialSessionKey,
  rememberGuide,
  selectedGuide,
} from "@/lib/tutorial-visibility";
import {
  steps,
  type Command,
  type Draft,
  type Step,
  type Tutorial,
} from "@/lib/demo-tutorial";
import type { Guide } from "@/lib/tutorial-recorridos";
import type { Role } from "@/lib/roles";

type Access = {
  state: Tutorial | null;
  open: boolean;
  guideOpen: boolean;
  guide: Guide | null;
  choose: (guide: Guide | null) => void;
  setOpen: (value: boolean) => void;
  role: Role;
  error: string;
  saving: boolean;
  available: boolean;
  command: (body: Command) => Promise<Tutorial>;
  refresh: () => Promise<void>;
  draft: (step: Step, fields: Draft) => void;
  flush: () => Promise<void>;
};
const Context = createContext<Access | null>(null);
export const useTutorial = () => useContext(Context);
export const tutorialBusinessChanged = "demo:tutorial:operacion";
function valid(value: unknown): value is Tutorial {
  if (!value || typeof value !== "object") return false;
  const v = value as Tutorial;
  return (
    ["NUEVO", "ACTIVO", "PAUSADO", "COMPLETO"].includes(v.estado) &&
    steps.includes(v.paso) &&
    Array.isArray(v.completados) &&
    v.completados.every((s) => steps.includes(s)) &&
    !!v.borradores &&
    typeof v.borradores === "object"
  );
}
export function TutorialProvider({
  role,
  children,
}: {
  role: Role;
  children: ReactNode;
}) {
  const [state, setState] = useState<Tutorial | null>(null),
    [guideOpen, setVisible] = useState(false),
    [guide, setGuide] = useState<Guide | null>(null),
    [error, setError] = useState(""),
    [saving, setSaving] = useState(false),
    [available, setAvailable] = useState(true);
  const latest = useRef<Tutorial | null>(null),
    alive = useRef(true),
    serial = useRef<Promise<unknown>>(Promise.resolve());
  const pending = useRef(new Map<Step, Draft>()),
    drain = useRef<Promise<void> | null>(null),
    revision = useRef(0);
  const open = guideOpen && guide === "completo";
  const choose = useCallback((value: Guide | null) => {
    rememberGuide(value);
    setGuide(value);
  }, []);
  const setOpen = useCallback(
    (value: boolean) => {
      rememberTutorialOpen(value);
      setVisible(value);
      if (!value) choose(null);
    },
    [choose],
  );
  useEffect(() => {
    const sync = () => {
      setVisible(tutorialIsOpen());
      setGuide(selectedGuide());
    };
    const sessionChanged = (event: StorageEvent) => {
      if (event.key === tutorialSessionKey || event.key === null) sync();
    };
    sync();
    window.addEventListener("storage", sessionChanged);
    window.addEventListener("pageshow", sync);
    return () => {
      window.removeEventListener("storage", sessionChanged);
      window.removeEventListener("pageshow", sync);
    };
  }, []);
  const accept = useCallback((next: unknown) => {
    if (!valid(next)) throw Error("No pudimos leer el avance del tutorial.");
    if (alive.current) {
      latest.current = next;
      setState(next);
      setError("");
    }
    return next;
  }, []);
  const refresh = useCallback(async () => {
    const sequence = ++revision.current;
    try {
      const response = await fetch("/api/demo/tutorial", {
        cache: "no-store",
        credentials: "same-origin",
      });
      if (!alive.current || sequence !== revision.current) return;
      if ([403, 404].includes(response.status)) {
        setAvailable(false);
        setOpen(false);
        return;
      }
      if (!response.ok)
        throw Error(
          response.status === 423
            ? "La vigencia de la demo terminó."
            : "No pudimos consultar el avance guardado.",
        );
      const result = await response.json();
      if (alive.current && sequence === revision.current) accept(result);
    } catch (e) {
      if (alive.current && sequence === revision.current)
        setError(
          e instanceof Error ? e.message : "No pudimos consultar el tutorial.",
        );
    }
  }, [accept, setOpen]);
  const command = useCallback(
    (body: Command) => {
      const next = serial.current.then(async () => {
        ++revision.current;
        try {
          const response = await secureMutation(
            "/api/demo/tutorial",
            JSON.stringify(body),
            "application/json",
            "PUT",
          );
          const data = await response.json();
          ++revision.current;
          return accept(data);
        } catch (e) {
          if (alive.current)
            setError(
              e instanceof Error
                ? e.message
                : "No pudimos guardar el tutorial.",
            );
          throw e;
        }
      });
      serial.current = next.catch(() => {});
      return next;
    },
    [accept],
  );
  const flush = useCallback((): Promise<void> => {
    if (drain.current) return drain.current;
    if (!pending.current.size) return Promise.resolve();
    if (alive.current) setSaving(true);
    const run = async () => {
      while (pending.current.size) {
        const [paso, campos] = pending.current.entries().next().value!;
        pending.current.delete(paso);
        try {
          await command({ accion: "borrador", paso, campos });
        } catch (e) {
          pending.current.set(paso, {
            ...campos,
            ...pending.current.get(paso),
          });
          throw e;
        }
      }
    };
    drain.current = run().finally(() => {
      drain.current = null;
      if (alive.current) setSaving(false);
    });
    return drain.current;
  }, [command]);
  const draft = useCallback(
    (step: Step, fields: Draft) => {
      if (!open || latest.current?.estado !== "ACTIVO") return;
      pending.current.set(step, { ...pending.current.get(step), ...fields });
      void flush().catch(() => {});
    },
    [flush, open],
  );
  useEffect(() => {
    alive.current = true;
    void refresh();
    const closing = (event: BeforeUnloadEvent) => {
      if (drain.current || pending.current.size) {
        void flush().catch(() => {});
        event.preventDefault();
        event.returnValue = "";
      }
    };
    window.addEventListener("beforeunload", closing);
    return () => {
      alive.current = false;
      revision.current++;
      window.removeEventListener("beforeunload", closing);
    };
  }, [refresh, flush]);
  useEffect(() => {
    if (!open) return;
    const changed = () => {
      if (latest.current?.estado === "ACTIVO") void refresh();
    };
    window.addEventListener(tutorialBusinessChanged, changed);
    window.addEventListener("focus", changed);
    return () => {
      window.removeEventListener(tutorialBusinessChanged, changed);
      window.removeEventListener("focus", changed);
    };
  }, [open, refresh]);
  return (
    <Context.Provider
      value={{
        state,
        open,
        guideOpen,
        guide,
        choose,
        setOpen,
        role,
        error,
        saving,
        available,
        command,
        refresh,
        draft,
        flush,
      }}
    >
      {children}
    </Context.Provider>
  );
}
