export const roles = ["ADMIN_ADMIN", "CLIENTE", "EMPLEADO"] as const;
export type Role = typeof roles[number];
export function isRole(value: unknown): value is Role { return roles.some(role => role === value); }
export function roleHome(role: Role): string {
  return { ADMIN_ADMIN: "/administracion", CLIENTE: "/cliente", EMPLEADO: "/operacion" }[role];
}

export function sessionHome(profile: { rol: Role; debeCambiarContrasena: boolean }): string {
  return profile.debeCambiarContrasena ? "/cuenta/seguridad" : roleHome(profile.rol);
}
