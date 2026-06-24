import { createClient } from "npm:@supabase/supabase-js@2";

import {
  ApiError,
  errorResponse,
  methodNotAllowedResponse,
  optionsResponse,
  resolveRequestId,
  successResponse,
} from "../_shared/api-response.ts";

interface PreparacionCargaArchivo {
  id_usuario: number;
  id_pedido: number;
  bucket: string;
  ruta_storage: string;
  limite_bytes: number;
  request_id: string | null;
}

interface ArchivoRegistrado {
  id_archivo: number;
  id_pedido: number;
  bucket: string;
  ruta_storage: string;
  hash_archivo: string;
  estado_archivo: "cargado";
  tamano_bytes: number;
  tamano_original_bytes: number;
  fecha_creacion: string;
}

interface PayloadCargaArchivo {
  idPedido: number;
  nombreOriginal: string;
  mimeOriginal: "application/pdf";
  tamanoBytes: number;
  tamanoOriginalBytes: number;
  hashArchivo: string;
  claveEnvuelta: string;
  iv: string;
  versionCifrado: string;
  archivoCifrado: Uint8Array;
}

interface SupabaseErrorDebug {
  code?: string;
  message?: string;
  details?: unknown;
  hint?: string;
  statusCode?: string;
  name?: string;
}

type ResultadoLimpiezaStorage = "no_requerida" | "ok" | "fallo";

const MIME_ORIGINAL_PERMITIDO = "application/pdf";
const MIME_STORAGE_CIFRADO = "application/octet-stream";
const LIMITE_BYTES_MVP = 10 * 1024 * 1024;
const MAX_NOMBRE_ORIGINAL = 255;
const MAX_HASH = 128;
const MAX_CLAVE_ENVUELTA = 4096;
const MAX_IV = 512;
const MAX_VERSION_CIFRADO = 64;
const HASH_SHA256_HEX_PATTERN = /^[a-f0-9]{64}$/;
const VERSION_CIFRADO_PATTERN = /^[A-Za-z0-9._:-]+$/;

Deno.serve(async (request) => {
  const requestId = resolveRequestId(request);

  try {
    if (request.method === "OPTIONS") {
      return optionsResponse(requestId);
    }

    if (request.method !== "POST") {
      return methodNotAllowedResponse(requestId);
    }

    const token = obtenerBearerToken(request);
    const supabaseAutenticado = crearClienteSupabaseAutenticado(token);

    const { data: usuarioAuth, error: errorUsuarioAuth } =
      await supabaseAutenticado.auth.getUser(token);

    if (errorUsuarioAuth || !usuarioAuth.user) {
      throw new ApiError({ code: "UNAUTHENTICATED" });
    }

    const payload = await parsearCargaArchivo(request);
    const supabaseService = crearClienteSupabaseService();

    const { data: preparacionRpc, error: errorPreparacion } =
      await supabaseService
        .rpc("preparar_carga_archivo_cliente", {
          p_id_usuario_auth_actor: usuarioAuth.user.id,
          p_id_pedido: payload.idPedido,
          p_tamano_bytes: payload.tamanoBytes,
          p_tamano_original_bytes: payload.tamanoOriginalBytes,
          p_request_id: requestId,
        });
    const preparacion = preparacionRpc as PreparacionCargaArchivo | null;

    if (errorPreparacion || !preparacion) {
      throw mapearErrorSupabase(
        errorPreparacion,
        "preparar_carga_archivo",
        "no_requerida",
        requestId,
      );
    }

    const { error: errorStorage } = await supabaseService.storage
      .from(preparacion.bucket)
      .upload(preparacion.ruta_storage, payload.archivoCifrado, {
        contentType: MIME_STORAGE_CIFRADO,
        upsert: false,
      });

    if (errorStorage) {
      throw mapearErrorStorage(errorStorage, "subir_storage", requestId);
    }

    const { data: archivoRegistradoRpc, error: errorRegistro } =
      await supabaseService
        .rpc("registrar_archivo_pedido_cliente", {
          p_id_usuario_auth_actor: usuarioAuth.user.id,
          p_id_pedido: payload.idPedido,
          p_nombre_original: payload.nombreOriginal,
          p_mime_original: payload.mimeOriginal,
          p_tamano_bytes: payload.tamanoBytes,
          p_tamano_original_bytes: payload.tamanoOriginalBytes,
          p_hash_archivo: payload.hashArchivo,
          p_bucket: preparacion.bucket,
          p_ruta_storage: preparacion.ruta_storage,
          p_clave_envuelta: payload.claveEnvuelta,
          p_iv: payload.iv,
          p_version_cifrado: payload.versionCifrado,
          p_request_id: requestId,
        });
    const archivoRegistrado = archivoRegistradoRpc as ArchivoRegistrado | null;

    if (errorRegistro || !archivoRegistrado) {
      const resultadoLimpieza = await eliminarObjetoStorage(
        supabaseService,
        preparacion.bucket,
        preparacion.ruta_storage,
        requestId,
      );

      throw mapearErrorSupabase(
        errorRegistro,
        "insertar_metadata_archivo",
        resultadoLimpieza,
        requestId,
      );
    }

    return successResponse(archivoRegistrado, requestId);
  } catch (error) {
    return errorResponse(error, requestId, { includeDetails: true });
  }
});

function crearClienteSupabaseAutenticado(token: string) {
  const supabaseUrl = obtenerVariableEntorno("SUPABASE_URL");
  const supabaseAnonKey = obtenerVariableEntorno("SUPABASE_ANON_KEY", [
    "SUPABASE_PUBLISHABLE_KEY",
  ]);

  return createClient(supabaseUrl, supabaseAnonKey, {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
    global: {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  });
}

function crearClienteSupabaseService() {
  const supabaseUrl = obtenerVariableEntorno("SUPABASE_URL");
  const supabaseServiceKey = obtenerVariableEntorno(
    "SUPABASE_SERVICE_ROLE_KEY",
    ["SUPABASE_SECRET_KEY"],
  );

  return createClient(supabaseUrl, supabaseServiceKey, {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
  });
}

function obtenerVariableEntorno(
  nombre: string,
  alternativas: string[] = [],
): string {
  for (const variable of [nombre, ...alternativas]) {
    const valor = Deno.env.get(variable);

    if (valor) {
      return valor;
    }
  }

  throw new ApiError({ code: "INTERNAL_ERROR" });
}

function obtenerBearerToken(request: Request): string {
  const authorization = request.headers.get("authorization")?.trim() ?? "";
  const match = authorization.match(/^Bearer\s+(.+)$/i);

  if (!match?.[1]) {
    throw new ApiError({ code: "UNAUTHENTICATED" });
  }

  return match[1];
}

async function parsearCargaArchivo(
  request: Request,
): Promise<PayloadCargaArchivo> {
  const formData = await parsearFormData(request);
  const archivo = leerArchivo(formData, ["archivo", "file"]);

  validarArchivoCifrado(archivo);

  const archivoCifrado = new Uint8Array(await archivo.arrayBuffer());
  const hashCalculado = await calcularSha256Hex(archivoCifrado);
  const hashArchivo = normalizarHashSha256(
    leerTextoRequerido(
      formData,
      ["hash_archivo", "checksum", "sha256"],
      MAX_HASH,
    ),
  );

  if (hashArchivo !== hashCalculado) {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: {
        hash_archivo: "No coincide con el SHA-256 del ciphertext recibido.",
      },
      exposeDetails: true,
    });
  }

  const tamanoOriginalBytes = leerEnteroPositivo(
    formData,
    ["tamano_original_bytes", "tamano_original", "originalSize"],
    "tamano_original_bytes",
  );

  validarLimiteBytes(tamanoOriginalBytes, "tamano_original_bytes");

  return {
    idPedido: leerEnteroPositivo(
      formData,
      ["id_pedido", "orderId"],
      "id_pedido",
    ),
    nombreOriginal: leerTextoRequerido(
      formData,
      ["nombre_original", "originalName", "filename"],
      MAX_NOMBRE_ORIGINAL,
      archivo.name,
    ),
    mimeOriginal: leerMimeOriginal(formData),
    tamanoBytes: archivo.size,
    tamanoOriginalBytes,
    hashArchivo,
    claveEnvuelta: leerTextoRequerido(
      formData,
      ["clave_envuelta", "wrapped_key", "wrappedKey"],
      MAX_CLAVE_ENVUELTA,
    ),
    iv: leerTextoRequerido(formData, ["iv"], MAX_IV),
    versionCifrado: leerVersionCifrado(formData),
    archivoCifrado,
  };
}

async function parsearFormData(request: Request): Promise<FormData> {
  try {
    return await request.formData();
  } catch {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: {
        cuerpo: "La solicitud debe enviarse como multipart/form-data.",
      },
      exposeDetails: true,
    });
  }
}

function leerArchivo(formData: FormData, claves: string[]): File {
  for (const clave of claves) {
    const valor = formData.get(clave);

    if (valor instanceof File) {
      return valor;
    }
  }

  throw new ApiError({
    code: "VALIDATION_ERROR",
    details: { archivo: "Debe enviarse el ciphertext en el campo archivo." },
    exposeDetails: true,
  });
}

function validarArchivoCifrado(archivo: File): void {
  if (archivo.size <= 0) {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { archivo: "El archivo cifrado no puede estar vacio." },
      exposeDetails: true,
    });
  }

  validarLimiteBytes(archivo.size, "archivo");

  if (archivo.type && archivo.type !== MIME_STORAGE_CIFRADO) {
    throw new ApiError({
      code: "UNSUPPORTED_FILE_TYPE",
      details: {
        archivo: "El ciphertext debe enviarse como application/octet-stream.",
      },
      exposeDetails: true,
    });
  }
}

function leerMimeOriginal(formData: FormData): "application/pdf" {
  const mimeOriginal = leerTextoRequerido(
    formData,
    ["mime_original", "mimeOriginal", "originalMimeType"],
    100,
  ).toLowerCase();

  if (mimeOriginal !== MIME_ORIGINAL_PERMITIDO) {
    throw new ApiError({
      code: "UNSUPPORTED_FILE_TYPE",
      details: {
        mime_original: "El MVP solo acepta PDF como archivo original.",
      },
      exposeDetails: true,
    });
  }

  return MIME_ORIGINAL_PERMITIDO;
}

function leerVersionCifrado(formData: FormData): string {
  const versionCifrado = leerTextoRequerido(
    formData,
    ["version_cifrado", "encryption_version", "encryptionVersion"],
    MAX_VERSION_CIFRADO,
  );

  if (!VERSION_CIFRADO_PATTERN.test(versionCifrado)) {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: {
        version_cifrado:
          "Usa solo letras, numeros, punto, guion, guion bajo o dos puntos.",
      },
      exposeDetails: true,
    });
  }

  return versionCifrado;
}

function leerTextoRequerido(
  formData: FormData,
  claves: string[],
  maximo: number,
  valorDefault?: string,
): string {
  const valor = leerValorTexto(formData, claves) ?? valorDefault;

  if (typeof valor !== "string" || valor.trim() === "") {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { [claves[0]]: "Debe ser texto no vacio." },
      exposeDetails: true,
    });
  }

  const texto = valor.trim();

  if (texto.length > maximo) {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { [claves[0]]: `No debe superar ${maximo} caracteres.` },
      exposeDetails: true,
    });
  }

  return texto;
}

function leerEnteroPositivo(
  formData: FormData,
  claves: string[],
  campo: string,
): number {
  const valor = leerValorTexto(formData, claves);
  const numero = Number(valor);

  if (!valor || !Number.isInteger(numero) || numero <= 0) {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { [campo]: "Debe ser un entero mayor a cero." },
      exposeDetails: true,
    });
  }

  return numero;
}

function leerValorTexto(formData: FormData, claves: string[]): string | null {
  for (const clave of claves) {
    const valor = formData.get(clave);

    if (typeof valor === "string") {
      return valor;
    }
  }

  return null;
}

function validarLimiteBytes(tamanoBytes: number, campo: string): void {
  if (tamanoBytes > LIMITE_BYTES_MVP) {
    throw new ApiError({
      code: "FILE_TOO_LARGE",
      details: {
        [campo]: "No debe superar 10 MiB.",
        limite_bytes: LIMITE_BYTES_MVP,
      },
      exposeDetails: true,
    });
  }
}

function normalizarHashSha256(hash: string): string {
  const normalizado = hash.toLowerCase().replace(/^sha256:/, "");

  if (!HASH_SHA256_HEX_PATTERN.test(normalizado)) {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { hash_archivo: "Debe ser SHA-256 hexadecimal del ciphertext." },
      exposeDetails: true,
    });
  }

  return normalizado;
}

async function calcularSha256Hex(bytes: Uint8Array): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", bytes);

  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

async function eliminarObjetoStorage(
  supabaseService: ReturnType<typeof crearClienteSupabaseService>,
  bucket: string,
  rutaStorage: string,
  requestId: string,
): Promise<ResultadoLimpiezaStorage> {
  const { error } = await supabaseService.storage.from(bucket).remove([
    rutaStorage,
  ]);

  if (error) {
    registrarLogSeguro("storage_cleanup_failed", requestId, {
      etapa: "limpiar_storage",
      bucket,
      ruta_storage: rutaStorage,
      storage_message: extraerMensajeSeguro(error),
    });

    return "fallo";
  }

  return "ok";
}

function mapearErrorSupabase(
  error: SupabaseErrorDebug | null,
  etapa: string,
  storageCleanup: ResultadoLimpiezaStorage,
  requestId: string,
): ApiError {
  const detalles = {
    etapa,
    db_code: error?.code ?? null,
    db_message: extraerMensajeSeguro(error),
    storage_cleanup: storageCleanup,
  };

  registrarLogSeguro("supabase_operation_failed", requestId, detalles);

  if (error?.code === "42501") {
    return new ApiError({
      code: "FORBIDDEN",
      details: detalles,
      exposeDetails: true,
    });
  }

  if (
    error?.code === "P0001" && error.message?.toLowerCase().includes("editable")
  ) {
    return new ApiError({
      code: "ORDER_NOT_EDITABLE",
      details: detalles,
      exposeDetails: true,
    });
  }

  if (
    error?.code === "23503" || error?.code === "23514" ||
    error?.code === "22P02"
  ) {
    return new ApiError({
      code: "VALIDATION_ERROR",
      details: detalles,
      exposeDetails: true,
    });
  }

  return new ApiError({
    code: "UPLOAD_FAILED",
    message: etapa === "insertar_metadata_archivo"
      ? "No se pudo registrar el archivo cargado."
      : undefined,
    details: detalles,
    exposeDetails: true,
  });
}

function mapearErrorStorage(
  error: SupabaseErrorDebug,
  etapa: string,
  requestId: string,
): ApiError {
  const detalles = {
    etapa,
    storage_code: error.statusCode ?? error.code ?? null,
    storage_message: extraerMensajeSeguro(error),
    storage_cleanup: "no_requerida",
  };

  registrarLogSeguro("storage_operation_failed", requestId, detalles);

  return new ApiError({
    code: "UPLOAD_FAILED",
    details: detalles,
    exposeDetails: true,
  });
}

function extraerMensajeSeguro(
  error: SupabaseErrorDebug | null | undefined,
): string | null {
  if (!error?.message) {
    return null;
  }

  return error.message.slice(0, 500);
}

function registrarLogSeguro(
  evento: string,
  requestId: string,
  detalles: Record<string, unknown>,
): void {
  console.error(JSON.stringify({
    evento,
    request_id: requestId,
    ...detalles,
  }));
}
