// Each tab opens the guide explicitly. A normal login starts a new session for
// the shared authentication cookie, including tabs still showing the old role.
export const tutorialSessionKey = "demo:tutorial:sesion";
const openKey = "demo:tutorial:abierto";

export function tutorialIsOpen() {
  try {
    return sessionStorage.getItem(openKey) === (localStorage.getItem(tutorialSessionKey) ?? "inicial");
  } catch {
    return false;
  }
}

export function rememberTutorialOpen(open: boolean) {
  try {
    if (open) sessionStorage.setItem(openKey, localStorage.getItem(tutorialSessionKey) ?? "inicial");
    else sessionStorage.removeItem(openKey);
  } catch {
    // The current screen still works when the browser disables storage.
  }
}

export function resetTutorialSession() {
  rememberTutorialOpen(false);
  try {
    sessionStorage.removeItem("demo:tutorial:prerrequisito");
    localStorage.setItem(tutorialSessionKey, crypto.randomUUID());
  } catch {
    // With storage disabled, new screens already start closed.
  }
}
