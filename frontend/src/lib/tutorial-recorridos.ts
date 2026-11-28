export type RoleGuide = "administrador" | "cliente";
export type Guide = RoleGuide | "completo";

export type RoleTask = {
  title: string;
  text: string;
  selector: string;
  readySelector?: string;
  nextLabel?: string;
  external?: boolean;
  path?: string;
  download?: { href: string; label: string };
};
export type RoleLesson = {
  id: string;
  title: string;
  path: string;
  tasks: RoleTask[];
};
export type RoleProgress = {
  lesson: string;
  task: number;
  reached: number;
  complete: boolean;
  draft: Record<string, string>;
};
export type RoleProgressList = Record<RoleGuide, RoleProgress>;

export function lessonPath(
  lesson: RoleLesson,
  task: number,
  draft: Record<string, string>,
) {
  const route = lesson.tasks[task]?.path ?? lesson.path;
  const id = /:(preparacion|pedido)\b/.exec(route);
  if (!id) return route;
  const value = draft[id[1]];
  return /^[a-f\d]{8}(?:-[a-f\d]{4}){3}-[a-f\d]{12}$/i.test(value ?? "")
    ? route.replace(id[0], value)
    : null;
}

export function matchesTutorialPath(path: string) {
  const url = new URL(path, location.origin);
  return (
    location.pathname === url.pathname &&
    (!url.hash || location.hash === url.hash) &&
    (!url.search || location.search === url.search)
  );
}

export const guideNames: Record<Guide, string> = {
  administrador: "Tutorial del lado administrador",
  cliente: "Tutorial del lado cliente",
  completo: "Tutorial de flujo completo",
};
