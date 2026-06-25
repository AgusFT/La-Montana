import type { NextRequest } from "next/server";

import {
  actualizarSesion,
  redirigirConCookies,
} from "@/lib/supabase/proxy";

const RUTAS_PRIVADAS = ["/dashboard", "/pedidos"];
const RUTAS_PUBLICAS = ["/", "/login", "/signup"];

export async function proxy(request: NextRequest) {
  const { response, user } = await actualizarSesion(request);
  const pathname = request.nextUrl.pathname;
  const esRutaPrivada = coincideConRuta(pathname, RUTAS_PRIVADAS);
  const esRutaPublica = coincideConRuta(pathname, RUTAS_PUBLICAS);

  if (esRutaPrivada && !user) {
    return redirigirConCookies(request, "/login", response);
  }

  if (esRutaPublica && user) {
    return redirigirConCookies(request, "/dashboard", response);
  }

  return response;
}

export const config = {
  matcher: [
    "/((?!api|_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)",
  ],
};

function coincideConRuta(pathname: string, rutas: string[]) {
  return rutas.some((ruta) => {
    if (ruta === "/") {
      return pathname === "/";
    }

    return pathname === ruta || pathname.startsWith(`${ruta}/`);
  });
}
