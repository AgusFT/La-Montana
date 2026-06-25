import { redirect } from "next/navigation";

import { obtenerPerfilAutenticado } from "./profile";
import { crearClienteSupabaseServidor } from "@/lib/supabase/server";

export async function exigirClienteAutenticado() {
  const supabase = await crearClienteSupabaseServidor();
  const perfil = await obtenerPerfilAutenticado(supabase);

  if (!perfil) {
    redirect("/login");
  }

  if (perfil.rolCodigo !== "cliente") {
    redirect("/login");
  }

  return perfil;
}
