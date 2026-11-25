//#region ENCABEZADO · src/lib/argentine-money.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/argentine-money.ts
 * ========================================================================
 * FUNCIÓN
 * Valida y convierte pesos escritos con formato argentino a decimales exactos para la API; rechaza
 * formatos ambiguos y ofrece presentación localizada.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * - parsePesos(value: string, label: string, positive = true): string
 * - pesosDraft(value: string|null|undefined): string
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos propios.
 * VALORES DE MÓDULO
 * - pesosPattern [const].
 * ========================================================================
 */
//#endregion

export const pesosPattern="([0-9]{1,12}|[1-9][0-9]{0,2}(\\.[0-9]{3}){1,3})(,[0-9]{1,2})?";
export function parsePesos(value:string,label:string,positive=true):string{
  if(!new RegExp(`^${pesosPattern}$`).test(value.trim()))throw new Error(`${label}: usá formato argentino, por ejemplo 1.500,50 o 1500,50. Hasta dos decimales, sin escribir el símbolo $.`);
  const [whole,fraction=""]=value.trim().replaceAll(".","").split(",");
  const normalized=`${BigInt(whole)}.${fraction.padEnd(2,"0")}`;
  if(positive&&BigInt(whole)*BigInt(100)+BigInt(fraction.padEnd(2,"0"))===BigInt(0))throw new Error(`${label}: el importe debe ser mayor que cero.`);
  return normalized;
}
export function pesosDraft(value:string|null|undefined):string{
  if(value===null||value===undefined||value==="")return "";
  return new Intl.NumberFormat("es-AR",{minimumFractionDigits:2,maximumFractionDigits:2}).format(Number(value));
}
