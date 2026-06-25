"use client";

import { useEffect, useState } from "react";

import { obtenerPerfilAutenticado, type PerfilUsuario } from "@/lib/auth/profile";
import { crearClienteSupabaseBrowser } from "@/lib/supabase/client";

export function PerfilClienteActual() {
  const [perfil, setPerfil] = useState<PerfilUsuario | null>(null);

  useEffect(() => {
    let activo = true;

    async function cargarPerfil() {
      const supabase = crearClienteSupabaseBrowser();
      const perfilActual = await obtenerPerfilAutenticado(supabase);

      if (activo) {
        setPerfil(perfilActual);
      }
    }

    void cargarPerfil();

    return () => {
      activo = false;
    };
  }, []);

  return (
    <>
      <span className="avatar" aria-label={perfil?.nombreVisible ?? "Cliente"}>
        {perfil?.inicial ?? "C"}
      </span>

      <span>{perfil?.nombreVisible ?? "Cliente"}</span>
    </>
  );
}
