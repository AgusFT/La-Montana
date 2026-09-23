export const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export type Branch = {
  codigoPublico: string; codigo: string; nombre: string; calle: string; numero: string;
  localidad: string; provincia: string; codigoPostal: string; zonaHoraria: string;
  correo: string | null; telefono: string | null; estado: "ACTIVA" | "DESACTIVADA"; version: number;
};
export const permissionLabels = {
  GESTIONAR_PRODUCCION: "Registrar producción manual", CONTROLAR_CALIDAD: "Controlar calidad",
  GESTIONAR_ENTREGAS: "Gestionar entregas", CERRAR_PEDIDOS: "Cerrar pedidos",
  GESTIONAR_PEDIDOS: "Revisar y decidir pedidos",
  REGISTRAR_COBRO: "Registrar cobros", ACREDITAR_PAGO: "Acreditar pagos", REGISTRAR_DEVOLUCION: "Registrar devoluciones",
} as const;
export type Permission = keyof typeof permissionLabels;
export type Employee = {
  codigoPublico: string; nombre: string; apellido: string; correo: string;
  estado: "ACTIVO" | "DESACTIVADO"; version: number; sucursales: string[]; permisos: Permission[];
};
export type OperationContext = { sucursales: Branch[]; permisos: Permission[] };
export function isBranch(value: unknown): value is Branch {
  if (!value || typeof value !== "object") return false;
  const b = value as Record<string, unknown>;
  return ["codigoPublico", "codigo", "nombre", "calle", "numero", "localidad", "provincia", "codigoPostal", "zonaHoraria"].every(k => typeof b[k] === "string") &&
    ["correo", "telefono"].every(k => b[k] === null || typeof b[k] === "string") &&
    (b.estado === "ACTIVA" || b.estado === "DESACTIVADA") && Number.isInteger(b.version) && Number(b.version) >= 0;
}
export function isPermissions(value: unknown): value is Permission[] {
  return Array.isArray(value) && value.every(p => typeof p === "string" && Object.hasOwn(permissionLabels, p));
}
export function isEmployee(value: unknown): value is Employee {
  if (!value || typeof value !== "object") return false;
  const e = value as Record<string, unknown>;
  return ["codigoPublico", "nombre", "apellido", "correo"].every(k => typeof e[k] === "string") &&
    (e.estado === "ACTIVO" || e.estado === "DESACTIVADO") && Number.isInteger(e.version) && Number(e.version) >= 0 &&
    Array.isArray(e.sucursales) && e.sucursales.every(id => typeof id === "string" && uuidPattern.test(id)) && isPermissions(e.permisos);
}
