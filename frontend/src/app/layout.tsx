//#region ENCABEZADO · src/app/layout.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/layout.tsx
 * ========================================================================
 * FUNCIÓN
 * Define el documento HTML raíz, el idioma español argentino, los metadatos generales y los
 * estilos globales y la preferencia visual antes del primer pintado.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default] RootLayout({ children }: Readonly<{ children: React.ReactNode }>)
 *   Componente de interfaz.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - metadata [const, exportado].
 * ========================================================================
 */
//#endregion

import type { Metadata } from "next";
import "./globals.css";
import "./theme.css";
import {themeBootstrap} from "@/lib/theme";

export const metadata: Metadata = {
  title: "La Montaña · Impresiones",
  description: "Cotización, producción y entrega de pedidos de impresión.",
  robots: { index: false, follow: false },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="es-AR" suppressHydrationWarning><head><script dangerouslySetInnerHTML={{__html:themeBootstrap}}/></head><body>{children}</body></html>;
}
