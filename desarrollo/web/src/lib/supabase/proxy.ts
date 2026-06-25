import { createServerClient } from "@supabase/ssr";
import { NextResponse, type NextRequest } from "next/server";

import { obtenerConfigSupabase } from "./config";

export async function actualizarSesion(request: NextRequest) {
  const { url, key } = obtenerConfigSupabase();
  let response = NextResponse.next({ request });

  const supabase = createServerClient(url, key, {
    cookies: {
      getAll() {
        return request.cookies.getAll();
      },
      setAll(cookiesToSet) {
        cookiesToSet.forEach(({ name, value }) => {
          request.cookies.set(name, value);
        });

        response = NextResponse.next({ request });

        cookiesToSet.forEach(({ name, value, options }) => {
          response.cookies.set(name, value, options);
        });
      },
    },
  });

  try {
    const {
      data: { user },
    } = await supabase.auth.getUser();

    return { response, user };
  } catch {
    return { response, user: null };
  }
}

export function redirigirConCookies(
  request: NextRequest,
  pathname: string,
  response: NextResponse,
) {
  const url = request.nextUrl.clone();

  url.pathname = pathname;
  url.search = "";

  const redirectResponse = NextResponse.redirect(url);

  response.cookies.getAll().forEach((cookie) => {
    redirectResponse.cookies.set(cookie);
  });

  return redirectResponse;
}
