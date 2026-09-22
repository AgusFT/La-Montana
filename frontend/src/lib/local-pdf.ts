import type {DocumentInfo} from "./quote-types";

/** Sólo prepara datos declarados. El servidor debe inspeccionar el PDF antes de crear un pedido. */
export async function readLocalPdf(file:File,signal:AbortSignal):Promise<{documento:DocumentInfo;paginas:number}>{
 if(!file.name.toLowerCase().endsWith(".pdf")||file.size<1||file.size>10*1024*1024)throw new Error("Seleccioná un PDF de hasta 10 MB.");
 signal.throwIfAborted();const data=new Uint8Array(await file.arrayBuffer());
 if(new TextDecoder().decode(data.slice(0,5))!=="%PDF-")throw new Error("El archivo no tiene una cabecera PDF válida.");
 const hash=await crypto.subtle.digest("SHA-256",data);signal.throwIfAborted();
 const pdf=await import("pdfjs-dist");pdf.GlobalWorkerOptions.workerSrc=`/pdfjs/${pdf.version}/pdf.worker.min.mjs`;
 signal.throwIfAborted();const task=pdf.getDocument({data,stopAtErrors:true,useWorkerFetch:false,useWasm:false,enableXfa:false});
 let timer:ReturnType<typeof setTimeout>|undefined;
 const abort=()=>{void task.destroy()};signal.addEventListener("abort",abort,{once:true});
 try{
  const rejection=new Promise<never>((_,reject)=>{timer=setTimeout(()=>reject(new Error("No se pudo leer el PDF dentro del tiempo permitido.")),15000);task.onPassword=()=>reject(new Error("Los PDF protegidos con contraseña no están admitidos."));});
  const document=await Promise.race([task.promise,rejection]);signal.throwIfAborted();
  if(document.numPages<1||document.numPages>10000)throw new Error("El PDF debe tener entre 1 y 10000 páginas.");
  return {documento:{nombre:file.name,bytes:file.size,sha256:Array.from(new Uint8Array(hash)).map(b=>b.toString(16).padStart(2,"0")).join("")},paginas:document.numPages};
 }catch(error){if(signal.aborted)throw error;throw new Error(error instanceof Error&&["Los PDF","El PDF","No se pudo"].some(x=>error.message.startsWith(x))?error.message:"No pudimos leer este PDF. Comprobá que no esté dañado o cifrado.");}
 finally{if(timer)clearTimeout(timer);signal.removeEventListener("abort",abort);await task.destroy();}
}
