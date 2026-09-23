//#region ENCABEZADO · src/components/branch-code-field.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/branch-code-field.tsx
 * ========================================================================
 * FUNCIÓN
 * Especializa el campo de código con la explicación de identificación de sucursales, usos y
 * formato recomendado.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] BranchCodeField({code}: {code?:string})
 *   Componente de interfaz.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

"use client";
import {CodeField} from "@/components/code-field";
export function BranchCodeField({code}:{code?:string}) {
  return <CodeField code={code} subject="sucursal">
        <p>Es un identificador único que elegís para distinguir esta sucursal. Se muestra en los listados de sucursales, la asignación de empleados y la operación interna.</p>
        <p><strong>Formato recomendado:</strong> localidad o barrio + número. Por ejemplo, <code>CABA-01</code> o <code>PALERMO-02</code>.</p>
        <p>Hasta 40 caracteres: letras sin tildes (A–Z), números, guion (-) o guion bajo (_), sin espacios. Se guarda en mayúsculas y no se puede cambiar después de crear la sucursal.</p>
  </CodeField>;
}
