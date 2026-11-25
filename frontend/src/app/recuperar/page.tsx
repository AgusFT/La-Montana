//#region ENCABEZADO · src/app/recuperar/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/recuperar/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta el flujo de recuperación de acceso por código de correo dentro de la pantalla de
 * identidad.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default] RecoveryPage()
 *   Componente de página exportado por la ruta de Next.js.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

import { IdentityShell } from "@/components/identity-shell";
import { RecoveryForms } from "@/components/security-forms";

export default function RecoveryPage() {
  return <IdentityShell access title="Recuperar acceso" description="Recibí un código por correo para elegir una nueva contraseña.">
    <a className="secondary-link" href="/acceso">Volver a iniciar sesión</a>
    <RecoveryForms />
  </IdentityShell>;
}
