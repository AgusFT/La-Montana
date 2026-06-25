import { createBrowserClient } from "@supabase/ssr";

import { obtenerConfigSupabase } from "./config";

export function crearClienteSupabaseBrowser() {
  const { url, key } = obtenerConfigSupabase();

  return createBrowserClient(url, key);
}
