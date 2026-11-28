import { secureMutation } from "./secure-mutation";
import type { Role } from "./roles";

export async function switchTutorialRole(next: Role) {
  await secureMutation(
    "/api/demo/sesion",
    JSON.stringify({
      rol: next === "ADMIN_ADMIN" ? "ADMINISTRADOR" : next,
    }),
    "application/json",
  );
  const response = await fetch("/api/auth/me", {
    cache: "no-store",
    credentials: "same-origin",
  });
  if (!response.ok || (await response.json()).rol !== next) {
    throw new Error(
      "Todavía no pudimos comprobar el cambio de rol. Conservamos tu avance. Volvé a intentar.",
    );
  }
}
