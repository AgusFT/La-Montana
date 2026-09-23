export const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export const weekDays = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"];
export type BranchDay = { dia: number; habilitado: boolean; apertura: string | null; cierre: string | null };
export type BranchLocation = { provincia: string; zonaHoraria: string; desfase: string; alias: string[] };
export function timeZoneLabel(zone: string): string {
  try { return new Intl.DateTimeFormat("es-AR", { timeZone: zone, timeZoneName: "shortOffset" }).formatToParts(new Date()).find(p => p.type === "timeZoneName")?.value ?? "Zona horaria registrada"; }
  catch { return "Zona horaria registrada"; }
}
export function isBranchLocation(value: unknown): value is BranchLocation {
  if (!value || typeof value !== "object") return false;
  const v = value as Record<string, unknown>;
  return ["provincia", "zonaHoraria", "desfase"].every(k => typeof v[k] === "string") && Array.isArray(v.alias) && v.alias.every(a => typeof a === "string");
}
export type Branch = {
  codigoPublico: string; codigo: string; nombre: string; calle: string; numero: string;
  localidad: string; provincia: string; codigoPostal: string; zonaHoraria: string;
  horarioAtencion: BranchDay[];
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
    Array.isArray(b.horarioAtencion) && b.horarioAtencion.every(d => !!d && typeof d === "object" && Number.isInteger(d.dia) && d.dia >= 1 && d.dia <= 7 && typeof d.habilitado === "boolean" && ["apertura", "cierre"].every(k => d[k] === null || typeof d[k] === "string")) &&
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
