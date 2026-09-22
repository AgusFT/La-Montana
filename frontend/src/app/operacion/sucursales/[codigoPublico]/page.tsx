import { notFound, redirect } from "next/navigation";
import { IdentityShell } from "@/components/identity-shell";
import { getSession } from "@/lib/identity-server";
import { getOperationBranch, getOperationContext } from "@/lib/organization-server";
import { uuidPattern } from "@/lib/organization-types";

import {OrderListView} from "@/components/order-list";
import {PaymentQueue} from "@/components/payment-queue";
import {ReceivedFiles} from "@/components/received-files";

export const dynamic = "force-dynamic";
export default async function OperationBranchPage({ params }: { params: Promise<{ codigoPublico: string }> }) {
  const session = await getSession();
  if (session.state === "anonymous") redirect("/acceso");
  if (session.state === "unavailable") return <IdentityShell title="Sucursal" description="Verificación de tu sesión."><p className="form-message error-message" role="alert">No pudimos verificar la sesión.</p><a className="secondary-link" href="/operacion">Volver a operación</a></IdentityShell>;
  if (session.state !== "authenticated") return null;
  if (session.profile.debeCambiarContrasena) redirect("/cuenta/seguridad");
  if (session.profile.rol === "CLIENTE") redirect("/cliente");
  const { codigoPublico } = await params;
  if (!uuidPattern.test(codigoPublico)) notFound();
  const result = await getOperationBranch(codigoPublico);
  if (result.state === "not-found") notFound();
  const context=await getOperationContext();
  const financial=context?.permisos.some(p=>["ACREDITAR_PAGO","REGISTRAR_COBRO","REGISTRAR_DEVOLUCION"].includes(p));
  const branch = result.state === "found" ? result.branch : null;
  return <IdentityShell title={branch?.nombre ?? (result.state === "forbidden" ? "Acceso denegado" : "Sucursal no disponible")} description="Pedidos confirmados, archivos y pagos de esta sucursal.">
    {branch ? <article className="branch-card"><h2>{branch.nombre}</h2><p>Código: {branch.codigo}</p><p>{branch.calle} {branch.numero}, {branch.localidad}, {branch.provincia} · CP {branch.codigoPostal}</p><p>Zona horaria: {branch.zonaHoraria}</p>{branch.correo && <p>Correo: {branch.correo}</p>}{branch.telefono && <p>Teléfono: {branch.telefono}</p>}</article> : <p className="form-message error-message" role="alert">{result.state === "forbidden" ? "No tenés acceso a esta sucursal." : "No pudimos conectar con el sistema para consultar esta sucursal. Intentá nuevamente."}</p>}
    {branch&&<><OrderListView branch={codigoPublico}/><ReceivedFiles branch={codigoPublico}/>{financial&&<PaymentQueue branch={codigoPublico}/>}</>}
    <a className="secondary-link" href="/operacion">Volver a tus sucursales</a>
  </IdentityShell>;
}
