import type {QuoteOptions} from './quote-types';
type Combination=QuoteOptions['combinaciones'][number];
export const printChoiceKey=(o:Combination)=>[o.servicio,o.formato,o.papel,o.color].join(':');

/** Destinations repeat the global offer. Retain every offered finishing ID when deduplicating. */
export function globalPrintChoices(combinations:Combination[]):Combination[]{
 const result=new Map<string,Combination>();
 for(const option of combinations){
  const key=printChoiceKey(option),previous=result.get(key);
  result.set(key,{...option,dobleFaz:option.dobleFaz||!!previous?.dobleFaz,terminaciones:Array.from(new Set([...(previous?.terminaciones??[]),...option.terminaciones]))});
 }
 return Array.from(result.values());
}

/** A sole service is implicit, including restored rows; sizes/papers/colors remain explicit choices. */
export function defaultServiceChoice(choice:string,options:Combination[]):string{
 const services=Array.from(new Set(options.map(o=>o.servicio)));
 if(services.length!==1)return choice;
 const parts=Array.from({length:4},(_,i)=>choice.split(':')[i]??'');
 parts[0]=services[0];
 for(let i=1;i<4;i++)if(parts[i]&&!options.some(o=>printChoiceKey(o).split(':').every((value,n)=>n>i||!parts[n]||parts[n]===value))){parts.fill('',i);break;}
 return parts.join(':');
}
