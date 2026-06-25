export interface ContratoCifradoArchivo {
  algoritmo_archivo: "AES-GCM";
  longitud_clave_archivo_bits: 256;
  algoritmo_envoltura_clave: "RSA-OAEP";
  hash_envoltura_clave: "SHA-256";
  version_cifrado: string;
  formato_clave_publica: "spki-pem";
  clave_publica_spki_pem: string;
}

export interface ArchivoCifradoPedido {
  archivo: File;
  hashArchivo: string;
  claveEnvuelta: string;
  iv: string;
  versionCifrado: string;
  nombreOriginal: string;
  mimeOriginal: "application/pdf";
  tamanoOriginalBytes: number;
}

export async function cifrarArchivoPedido(
  archivoOriginal: File,
  contrato: ContratoCifradoArchivo,
): Promise<ArchivoCifradoPedido> {
  validarContratoCifrado(contrato);

  const claveArchivo = await crypto.subtle.generateKey(
    {
      name: "AES-GCM",
      length: contrato.longitud_clave_archivo_bits,
    },
    true,
    ["encrypt", "wrapKey"],
  );
  const ivBytes = crypto.getRandomValues(new Uint8Array(12));
  const bytesOriginales = await archivoOriginal.arrayBuffer();
  const bytesCifrados = await crypto.subtle.encrypt(
    {
      name: "AES-GCM",
      iv: ivBytes,
    },
    claveArchivo,
    bytesOriginales,
  );
  const clavePublica = await importarClavePublica(contrato.clave_publica_spki_pem);
  const claveEnvuelta = await crypto.subtle.wrapKey(
    "raw",
    claveArchivo,
    clavePublica,
    {
      name: "RSA-OAEP",
    },
  );
  const hashArchivo = await calcularSha256Hex(bytesCifrados);
  const archivo = new File(
    [bytesCifrados],
    `${archivoOriginal.name}.bin`,
    {
      type: "application/octet-stream",
    },
  );

  return {
    archivo,
    hashArchivo,
    claveEnvuelta: bytesABase64(new Uint8Array(claveEnvuelta)),
    iv: bytesABase64(ivBytes),
    versionCifrado: contrato.version_cifrado,
    nombreOriginal: archivoOriginal.name,
    mimeOriginal: "application/pdf",
    tamanoOriginalBytes: archivoOriginal.size,
  };
}

function validarContratoCifrado(contrato: ContratoCifradoArchivo): void {
  if (
    contrato.algoritmo_archivo !== "AES-GCM" ||
    contrato.longitud_clave_archivo_bits !== 256 ||
    contrato.algoritmo_envoltura_clave !== "RSA-OAEP" ||
    contrato.hash_envoltura_clave !== "SHA-256" ||
    contrato.formato_clave_publica !== "spki-pem" ||
    !contrato.clave_publica_spki_pem
  ) {
    throw new Error("El contrato de cifrado recibido no es compatible.");
  }
}

async function importarClavePublica(pem: string): Promise<CryptoKey> {
  const claveSpki = pemAArrayBuffer(pem);

  return crypto.subtle.importKey(
    "spki",
    claveSpki,
    {
      name: "RSA-OAEP",
      hash: "SHA-256",
    },
    false,
    ["wrapKey"],
  );
}

function pemAArrayBuffer(pem: string): ArrayBuffer {
  const base64 = pem
    .replace("-----BEGIN PUBLIC KEY-----", "")
    .replace("-----END PUBLIC KEY-----", "")
    .replace(/\s/g, "");
  const binario = atob(base64);
  const bytes = new Uint8Array(binario.length);

  for (let i = 0; i < binario.length; i += 1) {
    bytes[i] = binario.charCodeAt(i);
  }

  return bytes.buffer;
}

async function calcularSha256Hex(buffer: ArrayBuffer): Promise<string> {
  const hash = await crypto.subtle.digest("SHA-256", buffer);

  return Array.from(new Uint8Array(hash))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

function bytesABase64(bytes: Uint8Array): string {
  let binario = "";
  const tamanoBloque = 0x8000;

  for (let i = 0; i < bytes.length; i += tamanoBloque) {
    const bloque = bytes.subarray(i, i + tamanoBloque);
    binario += String.fromCharCode(...bloque);
  }

  return btoa(binario);
}
