import type { SupabaseClient, User } from "@supabase/supabase-js";

interface UsuarioBackend {
  id_usuario: number;
  id_usuario_auth: string;
  nombre: string;
  apellido: string | null;
  email: string;
  estado: string;
}

export interface PerfilUsuario {
  idUsuario: number;
  idUsuarioAuth: string;
  nombre: string;
  apellido: string | null;
  email: string;
  estado: string;
  rolCodigo: string | null;
  nombreVisible: string;
  inicial: string;
}

export async function obtenerPerfilAutenticado(
  supabase: SupabaseClient,
): Promise<PerfilUsuario | null> {
  let user: User | null = null;

  try {
    const { data, error } = await supabase.auth.getUser();

    if (error) {
      return null;
    }

    user = data.user;
  } catch {
    return null;
  }

  if (!user) {
    return null;
  }

  return obtenerPerfilUsuario(supabase, user);
}

export async function obtenerPerfilUsuario(
  supabase: SupabaseClient,
  user: User,
): Promise<PerfilUsuario | null> {
  const { data: usuario, error: usuarioError } = await supabase
    .from("usuario")
    .select("id_usuario,id_usuario_auth,nombre,apellido,email,estado")
    .eq("id_usuario_auth", user.id)
    .maybeSingle<UsuarioBackend>();

  if (usuarioError || !usuario) {
    return null;
  }

  const { data: rolCodigo } = await supabase.rpc("usuario_actual_rol");

  const nombreVisible = [usuario.nombre, usuario.apellido]
    .filter(Boolean)
    .join(" ")
    .trim();
  const inicial = (usuario.nombre[0] ?? usuario.email[0] ?? "C").toUpperCase();

  return {
    idUsuario: usuario.id_usuario,
    idUsuarioAuth: usuario.id_usuario_auth,
    nombre: usuario.nombre,
    apellido: usuario.apellido,
    email: usuario.email,
    estado: usuario.estado,
    rolCodigo: typeof rolCodigo === "string" ? rolCodigo : null,
    nombreVisible: nombreVisible || usuario.email,
    inicial,
  };
}
