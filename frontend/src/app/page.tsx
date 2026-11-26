//#region ENCABEZADO · src/app/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta la edición Demo y permite elegir un rol o volver a la sesión actual.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] HomePage()
 *   Componente de página exportado por la ruta de Next.js.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - dynamic [const, exportado].
 * ========================================================================
 */
//#endregion

import { redirect } from "next/navigation";
import { MountainMark } from "@/components/mountain-brand";
import { DemoRoleSelector } from "@/components/identity-forms";
import { sessionHome } from "@/lib/roles";
import { getSession } from "@/lib/identity-server";

export const dynamic = "force-dynamic";

export default async function HomePage() {
  const session = await getSession();
  if (session.state === "authenticated") redirect(sessionHome(session.profile));
  return (
    <main className="shell">
      <header className="brand-row">
        <div className="brand"><MountainMark/><span>La Montaña<small>IMPRESIONES</small></span></div>
      </header>
      <section className="intro" aria-labelledby="page-title">
        <h1 id="page-title">Imprenta Senderos</h1>
        <p>Elegí un rol para explorar la imprenta de muestra. Las tres cuentas comparten los pedidos de esta instalación.</p>
      </section>
      <section className="status-panel demo-access" aria-label="Elegí un rol">
        {session.state === "unavailable" && <p className="form-message error-message" role="alert">Todavía no se pudo conectar con la imprenta. Volvé a elegir un rol cuando esté disponible.</p>}
        <DemoRoleSelector />
      </section>
    </main>
  );
}
