import {
  ApiError,
  errorResponse,
  methodNotAllowedResponse,
  optionsResponse,
  resolveRequestId,
  successResponse,
} from "../_shared/api-response.ts";

interface ContratoCifradoArchivo {
  algoritmo_archivo: "AES-GCM";
  longitud_clave_archivo_bits: 256;
  algoritmo_envoltura_clave: "RSA-OAEP";
  hash_envoltura_clave: "SHA-256";
  version_cifrado: string;
  formato_clave_publica: "spki-pem";
  clave_publica_spki_pem: string;
  campos_cargar_archivo: {
    archivo: "ciphertext application/octet-stream";
    hash_archivo: "sha256_hex_ciphertext";
    clave_envuelta: "base64_rsa_oaep";
    iv: "base64_aes_gcm";
    version_cifrado: string;
  };
}

const VERSION_CIFRADO_DEFAULT = "aes-256-gcm+rsa-oaep-sha256:v1";
const MAX_CLAVE_PUBLICA_PEM = 8192;

Deno.serve(async (request) => {
  const requestId = resolveRequestId(request);

  try {
    if (request.method === "OPTIONS") {
      return optionsResponse(requestId);
    }

    if (request.method !== "GET") {
      return methodNotAllowedResponse(requestId);
    }

    const versionCifrado = obtenerVariableEntornoOpcional(
      "VERSION_CIFRADO_ARCHIVOS",
    ) ?? VERSION_CIFRADO_DEFAULT;
    const clavePublica = normalizarClavePublicaPem(
      obtenerVariableEntorno("CLAVE_PUBLICA_CIFRADO_ARCHIVOS", [
        "ARCHIVOS_CIFRADO_PUBLIC_KEY",
        "FILE_ENCRYPTION_PUBLIC_KEY",
      ]),
    );

    const contrato: ContratoCifradoArchivo = {
      algoritmo_archivo: "AES-GCM",
      longitud_clave_archivo_bits: 256,
      algoritmo_envoltura_clave: "RSA-OAEP",
      hash_envoltura_clave: "SHA-256",
      version_cifrado: versionCifrado,
      formato_clave_publica: "spki-pem",
      clave_publica_spki_pem: clavePublica,
      campos_cargar_archivo: {
        archivo: "ciphertext application/octet-stream",
        hash_archivo: "sha256_hex_ciphertext",
        clave_envuelta: "base64_rsa_oaep",
        iv: "base64_aes_gcm",
        version_cifrado: versionCifrado,
      },
    };

    return successResponse(contrato, requestId, {
      headers: {
        "cache-control": "public, max-age=300",
      },
    });
  } catch (error) {
    return errorResponse(error, requestId);
  }
});

function obtenerVariableEntorno(
  nombre: string,
  alternativas: string[] = [],
): string {
  for (const variable of [nombre, ...alternativas]) {
    const valor = Deno.env.get(variable);

    if (valor?.trim()) {
      return valor;
    }
  }

  console.error(JSON.stringify({
    evento: "configuracion_cifrado_incompleta",
    variable: nombre,
  }));

  throw new ApiError({ code: "INTERNAL_ERROR" });
}

function obtenerVariableEntornoOpcional(nombre: string): string | null {
  const valor = Deno.env.get(nombre)?.trim();

  return valor || null;
}

function normalizarClavePublicaPem(valor: string): string {
  const pem = valor.trim().replaceAll("\\n", "\n").replaceAll("\r\n", "\n");

  if (
    pem.length > MAX_CLAVE_PUBLICA_PEM ||
    !pem.includes("-----BEGIN PUBLIC KEY-----") ||
    !pem.includes("-----END PUBLIC KEY-----") ||
    pem.toUpperCase().includes("PRIVATE KEY")
  ) {
    console.error(JSON.stringify({
      evento: "configuracion_cifrado_invalida",
      motivo: "clave_publica_pem_invalida",
    }));

    throw new ApiError({ code: "INTERNAL_ERROR" });
  }

  return pem;
}
