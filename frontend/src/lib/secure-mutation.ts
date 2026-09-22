export async function secureMutation(path: string, body?: BodyInit, contentType?: string, method: "POST" | "PUT" = "POST") {
  const csrfResponse = await fetch("/api/auth/csrf", { cache: "no-store", credentials: "same-origin" });
  if (!csrfResponse.ok) throw new Error("No pudimos preparar una operación segura. Intentá nuevamente.");
  const csrf = await csrfResponse.json();
  if (typeof csrf.token !== "string" || csrf.headerName?.toUpperCase() !== "X-CSRF-TOKEN") throw new Error("La protección de sesión no está disponible.");
  const response = await fetch(path, {
    method, credentials: "same-origin", cache: "no-store",
    headers: { "X-CSRF-TOKEN": csrf.token, ...(contentType ? { "Content-Type": contentType } : {}) }, body,
  });
  if (!response.ok) {
    const data = await response.json().catch(() => null);
    throw new Error(typeof data?.mensaje === "string" ? data.mensaje : "No fue posible completar la operación.");
  }
}
