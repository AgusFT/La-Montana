export type RefillMode='LLENAR'|'RESMA'|'MANUAL';
/** Filling records only the difference to the declared capacity, never another full load. */
export function printerRefill(current:number,capacity:number,mode:RefillMode,manual:string){
 const amount=mode==='LLENAR'?capacity-current:mode==='RESMA'?500:manual.trim()===''?NaN:Number(manual);
 if(!Number.isSafeInteger(amount)||amount<1||amount>1000000||!Number.isSafeInteger(current+amount))return null;
 return {amount,total:current+amount};
}
