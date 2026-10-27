/** Políticas certificadas: plazo comercial explícito y retención técnica independiente. */
export type ArchivePaymentPolicy={plazoMinutos:number;recordatorioMinutos:number|null;retencionHoras:24|48|72};
export type DurationUnit="MINUTOS"|"HORAS"|"DIAS";
export function durationMinutes(value:string,unit:DurationUnit){
 if(!/^[1-9]\d{0,5}$/.test(value))throw new Error("Ingresá un plazo entero positivo.");
 const n=Number(value)*({MINUTOS:1,HORAS:60,DIAS:1440}[unit]);
 if(!Number.isSafeInteger(n)||n>43200)throw new Error("El plazo comercial admite entre 1 minuto y 30 días.");return n;
}
export function durationInput(minutes:number|undefined){return minutes&&minutes%1440===0?{value:String(minutes/1440),unit:"DIAS" as const}:minutes&&minutes%60===0?{value:String(minutes/60),unit:"HORAS" as const}:{value:minutes?String(minutes):"",unit:"MINUTOS" as const};}
export function validArchivePolicy(p:ArchivePaymentPolicy){return Number.isInteger(p.plazoMinutos)&&p.plazoMinutos>=1&&p.plazoMinutos<=43200&&[24,48,72].includes(p.retencionHoras)&&(p.recordatorioMinutos===null||Number.isInteger(p.recordatorioMinutos)&&p.recordatorioMinutos>0&&p.recordatorioMinutos<p.plazoMinutos);}
export function policyLines(p:ArchivePaymentPolicy|null|undefined){return p?[
 `Plazo para acreditar pago: ${p.plazoMinutos} min desde la creación del pedido`,
 p.recordatorioMinutos?`Recordatorio: ${p.recordatorioMinutos} min antes`:"Recordatorio desactivado",
 `Conservación después del vencimiento: ${p.retencionHoras} h${p.retencionHoras===72?" · Recomendada":""}`,
 "Pago informado o gestión pendiente suspende siempre la eliminación",
 "Pagos tardíos/rechazados: resolución administrativa; no autorizan impresión"
 ]:["Falta definir el plazo comercial y la retención certificada en Fase 3."];}
