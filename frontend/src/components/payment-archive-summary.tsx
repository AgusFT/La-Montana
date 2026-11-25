import {policyLines,type ArchivePaymentPolicy} from "@/lib/payment-archive-policy";

export function PaymentArchiveSummary({policy}:{policy:ArchivePaymentPolicy|null|undefined}){
 return <section className="admin-card"><h3>Pagos y archivos · política que se aplicará</h3><ul>{policyLines(policy).map(line=><li key={line}>{line}</li>)}</ul><p className="admin-note">Carga del PDF → almacenamiento privado → validaciones/cálculo → condición económica → aprobación/autorización → producción. Guardar o informar un pago no autoriza imprimir.</p></section>;
}
