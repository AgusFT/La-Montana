import { createServerClient } from "@supabase/ssr";
import { cookies } from "next/headers";

import { obtenerConfigSupabase } from "./config";

export async function crearClienteSupabaseServidor() {
  const { url, key } = obtenerConfigSupabase();
  const cookieStore = await cookies();

  return createServerClient(url, key, {
    cookies: {
      getAll() {
        return cookieStore.getAll();
      },
      setAll(cookiesToSet) {
        try {
          cookiesToSet.forEach(({ name, value, options }) => {
            cookieStore.set(name, value, options);
          });
        } catch {
          // El Proxy refresca la sesion cuando un Server Component no puede escribir cookies.
        }
      },
    },
  });
}
