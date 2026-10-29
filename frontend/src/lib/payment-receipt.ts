import type {PaymentView} from "./payment-types";
const ZERO=BigInt(0),HUNDRED=BigInt(100);

// Work in cents: prices may exceed the exact integer range of a JS number.
function cents(value:string|null|undefined):bigint{
 if(!value||!/^\d+(\.\d{1,2})?$/.test(value))return ZERO;
 const [whole,fraction=""]=value.split(".");return BigInt(whole)*HUNDRED+BigInt(fraction.padEnd(2,"0"));
}
function pesos(value:bigint):string{return `${value/HUNDRED}.${String(value%HUNDRED).padStart(2,"0")}`;}
export function receiptSuggestion(data:PaymentView,medio:string,intento?:string):{importe:string;label:string}{
 const informed=data.intentos.find(i=>i.codigoPublico===intento&&i.estado==="PENDIENTE");
 if(informed)return {importe:informed.importe,label:"Importe informado por el cliente · comprobá lo recibido"};
 const pending=cents(data.pendiente);
 if(data.pedidoFinalizado||data.pedidoCerrado||data.cotizacionSustituida)return {importe:"",label:"Ingresá el importe que recibiste"};
 const offered=cents(data.anticipoOfertado)-cents(data.coberturaAnticipoOfertado);
 const current=cents(data.anticipoActual)-cents(data.coberturaAnticipoActual);
 const required=offered>current?offered:current;
 if(required>ZERO&&pending>ZERO&&data.mediosAnticipo.includes(medio))return {importe:pesos(required<pending?required:pending),label:"Seña o pago previo pendiente"};
 if(current>ZERO&&!data.mediosAnticipo.includes(medio))return {importe:pending>current?pesos(pending-current):"",label:"Saldo posterior a la seña · la seña requiere otro medio"};
 return {importe:pending>ZERO?pesos(pending):"",label:"Saldo pendiente del trabajo"};
}
export function normalizeReceiptAmount(value:string):string{
 const normalized=value.trim().replace(",",".");
 return /^\d{1,17}(\.\d{1,2})?$/.test(normalized)&&cents(normalized)>ZERO?pesos(cents(normalized)):"";
}
export const financialEventLabels:Record<string,string>={INFORMAR:"Transferencia informada",DESCARTAR:"Informe de transferencia descartado",RECIBIR:"Dinero recibido",APLICAR:"Pago descontado del saldo",DEVOLVER:"Devolución registrada"};
