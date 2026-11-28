"use client";

import { useLayoutEffect, useRef, useState } from "react";
import { roleHome } from "@/lib/roles";
import { roleNames, tutorialPath } from "@/lib/demo-tutorial";
import { switchTutorialRole } from "@/lib/tutorial-cambio-rol";
import { guideNames, type Guide } from "@/lib/tutorial-recorridos";
import {
  useAllowConfirmedDeparture,
  useConfirmNavigation,
} from "./navigation-boundary";
import { guideRole, roleLessons, useRoleStore } from "./role-tutorial-context";
import { useTutorial } from "./tutorial-context";

export function RestartTutorialButton({
  open,
  disabled,
  label = "Reiniciar tutorial",
}: {
  open: () => void;
  disabled: boolean;
  label?: string;
}) {
  return (
    <button
      type="button"
      className="tutorial-restart-button"
      aria-haspopup="dialog"
      disabled={disabled}
      onClick={open}
    >
      {label}
    </button>
  );
}

export function RestartTutorialDialog({
  guide,
  onClose,
}: {
  guide: Guide;
  onClose: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  useLayoutEffect(() => {
    dialog.current?.showModal();
  }, []);
  const full = useTutorial(),
    roles = useRoleStore();
  const confirmNavigation = useConfirmNavigation();
  const allowDeparture = useAllowConfirmedDeparture();
  const lock = useRef(false);
  const checkRoleOnClose = useRef(false);
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const requiredRole = guide === "completo" ? null : guideRole[guide];
  const changeRole = requiredRole && full?.role !== requiredRole;

  async function restart() {
    if (!full || !roles || lock.current || !confirmNavigation()) return;
    lock.current = true;
    setBusy(true);
    setError("");
    try {
      let path: string;
      if (guide === "completo") {
        const progress = await full.restart();
        path =
          full.role === "ADMIN_ADMIN"
            ? tutorialPath(progress.paso, progress, full.role)
            : roleHome(full.role);
      } else {
        if (changeRole) {
          checkRoleOnClose.current = true;
          await switchTutorialRole(requiredRole);
        }
        await roles.restart(guide);
        path = roleLessons[guide][0].path;
      }
      // Recargar también en el mismo paso descarta campos locales y cierra el visor.
      const destination = new URL(path, location.href);
      checkRoleOnClose.current = false;
      lock.current = false;
      setBusy(false);
      dialog.current?.close();
      allowDeparture();
      full.choose(guide);
      if (
        location.pathname === destination.pathname &&
        location.search === destination.search
      ) {
        history.replaceState(history.state, "", destination);
        location.reload();
      } else {
        location.replace(destination);
      }
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : "No pudimos reiniciar el tutorial. Volvé a intentarlo.",
      );
      lock.current = false;
      setBusy(false);
    }
  }

  return (
    <dialog
      ref={dialog}
      id="tutorial-restart-dialog"
      className="tutorial-restart-dialog"
      aria-labelledby="tutorial-restart-title"
      aria-describedby="tutorial-restart-description"
      onCancel={(event) => {
        if (lock.current) event.preventDefault();
      }}
      onClose={() => {
        setError("");
        // Si el reinicio falló después del relevo, volver a la sesión que realmente está abierta.
        if (checkRoleOnClose.current) {
          checkRoleOnClose.current = false;
          allowDeparture();
          location.reload();
        }
        onClose();
      }}
    >
      <h2 id="tutorial-restart-title">
        ¿Reiniciar el {guideNames[guide].toLocaleLowerCase("es")}?
      </h2>
      <p id="tutorial-restart-description">
        Vas a volver al primer paso. Se borrarán el avance y los campos de
        práctica de este tutorial. Los otros tutoriales y el trabajo guardado en
        la imprenta se conservan.
        {changeRole &&
          ` Para reiniciarlo vas a continuar como ${roleNames[requiredRole]}.`}
      </p>
      {error && <p role="alert">{error}</p>}
      <div className="tutorial-restart-actions">
        <button
          type="button"
          disabled={busy}
          onClick={() => dialog.current?.close()}
        >
          Cancelar
        </button>
        <button type="button" disabled={busy} onClick={() => void restart()}>
          {busy ? "Reiniciando…" : "Reiniciar tutorial"}
        </button>
      </div>
    </dialog>
  );
}
