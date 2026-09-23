"use client";
import {CodeField} from "@/components/code-field";
export function BranchCodeField({code}:{code?:string}) {
  return <CodeField code={code} subject="sucursal">
        <p>Es un identificador único que elegís para distinguir esta sucursal. Se muestra en los listados de sucursales, la asignación de empleados y la operación interna.</p>
        <p><strong>Formato recomendado:</strong> localidad o barrio + número. Por ejemplo, <code>CABA-01</code> o <code>PALERMO-02</code>.</p>
        <p>Hasta 40 caracteres: letras sin tildes (A–Z), números, guion (-) o guion bajo (_), sin espacios. Se guarda en mayúsculas y no se puede cambiar después de crear la sucursal.</p>
  </CodeField>;
}
