
import { DashboardView } from "@/features/dashboard/pages/DashboardPage";
import { exigirClienteAutenticado } from "@/lib/auth/require-client";

export default async function DashboardPage() {
  await exigirClienteAutenticado();

  return <DashboardView />;
}
