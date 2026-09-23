//#region ENCABEZADO · src/lib/roles.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/roles.ts
 * ========================================================================
 * FUNCIÓN
 * Define los roles reconocidos y resuelve la página de inicio de cada sesión, dando prioridad al
 * cambio obligatorio de contraseña.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] isRole(value: unknown): value is Role
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] roleHome(role: Role): string
 * - [export] sessionHome(profile: { rol: Role; debeCambiarContrasena: boolean }): string
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Role (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - roles [const, exportado].
 * ========================================================================
 */
//#endregion

export const roles = ["ADMIN_ADMIN", "CLIENTE", "EMPLEADO"] as const;
export type Role = typeof roles[number];
export function isRole(value: unknown): value is Role { return roles.some(role => role === value); }
export function roleHome(role: Role): string {
  return { ADMIN_ADMIN: "/administracion", CLIENTE: "/cliente", EMPLEADO: "/operacion" }[role];
}

export function sessionHome(profile: { rol: Role; debeCambiarContrasena: boolean }): string {
  return profile.debeCambiarContrasena ? "/cuenta/seguridad" : roleHome(profile.rol);
}
