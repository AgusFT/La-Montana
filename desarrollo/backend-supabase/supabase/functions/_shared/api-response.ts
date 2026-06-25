export type ApiErrorCode =
  | "UNAUTHENTICATED"
  | "FORBIDDEN"
  | "VALIDATION_ERROR"
  | "ORDER_NOT_FOUND"
  | "ORDER_NOT_EDITABLE"
  | "FILE_TOO_LARGE"
  | "UNSUPPORTED_FILE_TYPE"
  | "UPLOAD_FAILED"
  | "METHOD_NOT_ALLOWED"
  | "INVALID_JSON"
  | "REQUEST_TOO_LARGE"
  | "INTERNAL_ERROR";

export type ErrorDetails = Record<string, unknown> | unknown[] | string | number | boolean | null;

export interface ApiErrorEnvelope {
  ok: false;
  request_id: string;
  code: ApiErrorCode;
  message: string;
  details: ErrorDetails;
}

export interface ApiSuccessEnvelope<TData> {
  ok: true;
  request_id: string;
  data: TData;
}

export type ApiEnvelope<TData> = ApiSuccessEnvelope<TData> | ApiErrorEnvelope;

export interface ApiResponseOptions {
  corsOrigin?: string;
  headers?: HeadersInit;
}

export interface ApiErrorResponseOptions extends ApiResponseOptions {
  includeDetails?: boolean;
}

export interface ApiErrorOptions {
  code: ApiErrorCode;
  message?: string;
  status?: number;
  details?: unknown;
  exposeDetails?: boolean;
}

export const apiErrorStatuses: Record<ApiErrorCode, number> = {
  UNAUTHENTICATED: 401,
  FORBIDDEN: 403,
  VALIDATION_ERROR: 400,
  ORDER_NOT_FOUND: 404,
  ORDER_NOT_EDITABLE: 409,
  FILE_TOO_LARGE: 413,
  UNSUPPORTED_FILE_TYPE: 415,
  UPLOAD_FAILED: 502,
  METHOD_NOT_ALLOWED: 405,
  INVALID_JSON: 400,
  REQUEST_TOO_LARGE: 413,
  INTERNAL_ERROR: 500,
};

export const apiErrorMessages: Record<ApiErrorCode, string> = {
  UNAUTHENTICATED: "Debes iniciar sesion para continuar.",
  FORBIDDEN: "No tenes permisos para realizar esta accion.",
  VALIDATION_ERROR: "Revisa los datos enviados.",
  ORDER_NOT_FOUND: "No encontramos el pedido solicitado.",
  ORDER_NOT_EDITABLE: "No se puede modificar este pedido en su estado actual.",
  FILE_TOO_LARGE: "El archivo supera el tamano permitido.",
  UNSUPPORTED_FILE_TYPE: "El tipo de archivo no esta permitido.",
  UPLOAD_FAILED: "No se pudo cargar el archivo.",
  METHOD_NOT_ALLOWED: "Metodo no permitido.",
  INVALID_JSON: "El cuerpo de la solicitud no es un JSON valido.",
  REQUEST_TOO_LARGE: "La solicitud supera el tamano permitido.",
  INTERNAL_ERROR: "Ocurrio un error interno. Intenta nuevamente.",
};

const REQUEST_ID_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._:-]{7,127}$/;
const SENSITIVE_DETAIL_KEY_PATTERN =
  /(authorization|cookie|password|passwd|secret|token|jwt|apikey|api_key|service_role|private_key|stack)/i;

export class ApiError extends Error {
  readonly code: ApiErrorCode;
  readonly status: number;
  readonly details: unknown;
  readonly exposeDetails: boolean;

  constructor(options: ApiErrorOptions) {
    super(options.message ?? apiErrorMessages[options.code]);
    this.name = "ApiError";
    this.code = options.code;
    this.status = options.status ?? apiErrorStatuses[options.code];
    this.details = options.details ?? null;
    this.exposeDetails = options.exposeDetails ?? false;
  }
}

export function resolveRequestId(request?: Request): string {
  const headerValue = request?.headers.get("x-request-id")?.trim();

  if (headerValue && REQUEST_ID_PATTERN.test(headerValue)) {
    return headerValue;
  }

  return crypto.randomUUID();
}

export function successResponse<TData>(
  data: TData,
  requestId: string,
  options: ApiResponseOptions = {},
): Response {
  return jsonResponse<ApiSuccessEnvelope<TData>>(
    {
      ok: true,
      request_id: requestId,
      data,
    },
    200,
    requestId,
    options,
  );
}

export function errorResponse(
  error: unknown,
  requestId: string,
  options: ApiErrorResponseOptions = {},
): Response {
  const apiError = normalizeApiError(error);
  const details = options.includeDetails && apiError.exposeDetails
    ? sanitizeDetails(apiError.details)
    : null;

  return jsonResponse<ApiErrorEnvelope>(
    {
      ok: false,
      request_id: requestId,
      code: apiError.code,
      message: apiError.message,
      details,
    },
    apiError.status,
    requestId,
    options,
  );
}

export function methodNotAllowedResponse(
  requestId: string,
  options: ApiResponseOptions = {},
): Response {
  return errorResponse(new ApiError({ code: "METHOD_NOT_ALLOWED" }), requestId, options);
}

export function optionsResponse(
  requestId: string,
  options: ApiResponseOptions = {},
): Response {
  return new Response(null, {
    status: 204,
    headers: responseHeaders(requestId, options),
  });
}

export async function parseJsonBody<TBody>(request: Request): Promise<TBody> {
  try {
    return await request.json() as TBody;
  } catch {
    throw new ApiError({ code: "INVALID_JSON" });
  }
}

function normalizeApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }

  return new ApiError({ code: "INTERNAL_ERROR" });
}

function jsonResponse<TBody>(
  body: TBody,
  status: number,
  requestId: string,
  options: ApiResponseOptions,
): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: responseHeaders(requestId, options),
  });
}

function responseHeaders(
  requestId: string,
  options: ApiResponseOptions,
): Headers {
  const headers = new Headers(options.headers);

  headers.set("content-type", "application/json; charset=utf-8");
  headers.set("x-request-id", requestId);
  headers.set("access-control-allow-origin", options.corsOrigin ?? "*");
  headers.set(
    "access-control-allow-headers",
    "authorization, x-client-info, apikey, content-type, x-request-id",
  );
  headers.set("access-control-allow-methods", "GET, POST, PATCH, DELETE, OPTIONS");
  headers.set("access-control-expose-headers", "x-request-id");

  return headers;
}

function sanitizeDetails(value: unknown, depth = 0): ErrorDetails {
  if (depth > 3 || value === undefined || value === null) {
    return null;
  }

  if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
    return value;
  }

  if (Array.isArray(value)) {
    return value.slice(0, 20).map((item) => sanitizeDetails(item, depth + 1));
  }

  if (typeof value === "object") {
    const sanitized: Record<string, ErrorDetails> = {};

    for (const [key, item] of Object.entries(value as Record<string, unknown>)) {
      if (SENSITIVE_DETAIL_KEY_PATTERN.test(key)) {
        continue;
      }

      sanitized[key] = sanitizeDetails(item, depth + 1);
    }

    return sanitized;
  }

  return null;
}
