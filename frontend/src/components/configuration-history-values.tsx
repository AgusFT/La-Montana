import type {Value} from "@/lib/configuration-history-types";
const labels:Record<string,string>={modelo:"Modelo operativo",criterio:"Condición de aprobación",medios:"Medios de pago",instruccionesTransferencia:"Instrucciones de transferencia",vigenciaCotizacionMinutos:"Vigencia de cotización (minutos)",exigirSena:"Exigir seña",condicionSena:"Condición de seña",umbralSena:"Umbral de seña",tipoSena:"Tipo de seña",valorSena:"Valor de seña",umbralAprobacion:"Umbral de aprobación (ARS)",metodoAsignacion:"Asignación",impresoras:"Impresoras declaradas",serviciosPorSucursal:"Servicios por sucursal",sucursal:"Sucursal",servicios:"Servicios",nombre:"Nombre",formatos:"Formatos",admiteColor:"Admite color",admiteDobleFaz:"Admite doble faz",capacidadHojas:"Capacidad máxima (hojas)",estado:"Estado declarado",retiradaEn:"Retirada el",motivoRetiro:"Motivo de retiro",preparacionHoras:"Preparación (horas)",trasladoHoras:"Traslado (horas)",horariosPorSucursal:"Calendarios por sucursal",franjasRetiro:"Franjas de retiro en sucursal",zonaHoraria:"Zona horaria",dias:"Días",dia:"Día",habilitado:"Habilitado",habilitada:"Habilitada",apertura:"Apertura",cierre:"Cierre",modalidades:"Modalidades",puntos:"Puntos de entrega",zonas:"Zonas domiciliarias",codigo:"Código",calle:"Calle",numero:"Número",localidad:"Localidad",provincia:"Provincia",codigoPostal:"Código postal",referencias:"Referencias de ubicación",sucursales:"Condiciones por origen",costo:"Costo (ARS)",franjas:"Franjas",capacidadPedidos:"Cupo configurado (pedidos)",descripcion:"Descripción",territorios:"Cobertura"};
const terms:Record<string,string>={MANUAL:"Control manual",CONDICIONAL:"Control condicional",PAGO_PREVIO:"Pago previo",SENA:"Seña",MONTO_TOTAL:"Umbral de importe",TRANSFERENCIA:"Transferencia",EFECTIVO:"Efectivo",SIEMPRE:"Siempre",DESDE_CARILLAS:"Desde cantidad de carillas",DESDE_MONTO:"Desde importe",SUPERAR_UMBRAL_APROBACION:"Al superar el umbral de aprobación",PORCENTAJE:"Porcentaje",FIJA:"Importe fijo (ARS)",OPERATIVA:"Operativa declarada",DESHABILITADA:"Deshabilitada",RETIRADA:"Retirada",RETIRO_SUCURSAL:"Retiro en sucursal",RETIRO_PUNTO_ENTREGA:"Retiro en punto de entrega",ENVIO_DOMICILIO:"Envío a domicilio"};
export function HistoryValues({value,refs,field=""}:{value:Value;refs:Record<string,string>;field?:string}){
 if(value===null)return <span className="admin-note">Sin configurar / no aplica</span>;
 if(Array.isArray(value))return value.length?<ul className="history-values-list">{value.map((v,i)=><li key={i}><HistoryValues value={v} refs={refs} field={field}/></li>)}</ul>:<span className="admin-note">Sin registros</span>;
 if(typeof value==="object")return <dl className="history-values">{Object.entries(value).filter(([k])=>k!=="codigoPublico").map(([k,v])=><div key={k}><dt>{labels[k]??k}</dt><dd><HistoryValues value={v} refs={refs} field={k}/></dd></div>)}</dl>;
 if(typeof value==="boolean")return <span>{value?"Sí":"No"}</span>;
 if(field==="dia"&&typeof value==="number")return <span>{["","Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"][value]??value}</span>;
 const raw=String(value);return <span>{refs[raw]??terms[raw]??raw}</span>;
}

export function historySummary(code:string,value:Value):string{
 if(value===null||Array.isArray(value)||typeof value!=="object")return "Sin configurar";
 const list=(k:string)=>Array.isArray(value[k])?value[k] as Value[]:[];
 const count=(key:string,one:string,many:string)=>`${list(key).length} ${list(key).length===1?one:many}`;
 const word=(v:Value|undefined)=>v===null||v===undefined?"Sin configurar":terms[String(v)]??String(v);
 if(code==="modelo")return word(value.modelo)+(value.criterio?" · "+word(value.criterio):"");
 if(code==="pagos")return list("medios").map(word).join(" y ")+" · "+(value.exigirSena?"Seña configurada":"Sin seña exigida");
 if(code==="recursos")return `${count("impresoras","impresora declarada","impresoras declaradas")} · ${count("serviciosPorSucursal","sucursal","sucursales")}`;
 if(code==="horarios")return `${count("horariosPorSucursal","calendario","calendarios")} · Preparación ${word(value.preparacionHoras)} h · Traslado ${word(value.trasladoHoras)} h`;
 return list("modalidades").map(word).join(" · ")+` · ${count("puntos","punto","puntos")} · ${count("zonas","zona","zonas")}`;
}
