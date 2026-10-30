import {isPaymentView,type PaymentView} from "./payment-types";

type Mutate=(path:string,body?:BodyInit,contentType?:string,method?:"POST"|"PUT")=>Promise<Response>;
type Evidence={codigoPublico:string;estado:string;mensaje?:string|null};
const isEvidence=(v:unknown):v is Evidence=>!!v&&typeof v==="object"&&typeof(v as Evidence).codigoPublico==="string"&&typeof(v as Evidence).estado==="string";
export function transferFileError(file:File|null):string{
 return file&&(!file.name.toLowerCase().endsWith(".pdf")||file.name.length>255||file.size===0||file.size>10485760)?"Elegí un comprobante PDF de hasta 10 MiB, con un nombre de hasta 255 caracteres.":"";
}

/** One report and, optionally, one private attachment. Retries retain both operation IDs. */
export class ClientTransferCommand{
 readonly body:string;
 readonly reference:string;
 readonly evidenceOperation:string;
 view:PaymentView|null=null;
 evidenceId:string|null=null;
 evidenceBody:string|null=null;
 constructor(readonly quote:string,importe:string,readonly file:File|null){
  const operacion=crypto.randomUUID();
  this.reference=`Aviso web ${operacion}`;
  this.evidenceOperation=crypto.randomUUID();
  this.body=JSON.stringify({operacion,importe,referencia:this.reference,motivo:"Transferencia informada por el cliente desde la web."});
 }
 async execute(mutate:Mutate):Promise<{view:PaymentView;evidence:Evidence|null}>{
  const base=`/api/cliente/cotizaciones/${this.quote}`;
  if(!this.view){
   const v:unknown=await(await mutate(`${base}/pagos/informar`,this.body,"application/json")).json();
   if(!isPaymentView(v)||v.cotizacion!==this.quote||!v.intentos.some(i=>i.referencia===this.reference))throw new Error("No pudimos confirmar el aviso de transferencia. Recuperá la misma operación.");
   this.view=v;
  }
  if(!this.file)return {view:this.view,evidence:null};
  if(!this.evidenceId){
   const attempt=this.view.intentos.find(i=>i.referencia===this.reference)!;
   this.evidenceBody??=JSON.stringify({operacion:this.evidenceOperation,intento:attempt.codigoPublico,nombre:this.file.name,detalle:"Comprobante adjuntado por el cliente al informar su transferencia."});
   const v:unknown=await(await mutate(`${base}/comprobantes`,this.evidenceBody,"application/json")).json();
   if(!isEvidence(v))throw new Error("No pudimos confirmar la preparación del comprobante.");
   this.evidenceId=v.codigoPublico;
  }
  const evidence:unknown=await(await mutate(`${base}/comprobantes/${this.evidenceId}/contenido`,this.file,"application/pdf","PUT")).json();
  if(!isEvidence(evidence)||evidence.codigoPublico!==this.evidenceId||!["VALIDO","VALIDANDO","RECHAZADO","FALLIDO"].includes(evidence.estado))throw new Error("No pudimos confirmar el resultado del comprobante.");
  return {view:this.view,evidence};
 }
}
