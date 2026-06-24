# Contrato De Errores Edge Functions

| Campo | Valor |
|---|---|
| Version | 1.0 |
| Estado | Inicial |
| Fecha | 2026-06-24 |
| Responsable | Agustin Tejero |

## 1. Objetivo

Este documento define el contrato estandar de errores para las Edge Functions
del backend Supabase del MVP de La Montana.

El contrato busca que el frontend pueda manejar errores de forma consistente
sin depender de textos variables, stack traces ni detalles internos del backend.

## 2. Alcance

Aplica a las Edge Functions del flujo minimo de pedidos:

- `orders-create`;
- `order-file-upload`;
- `orders-confirm`.

Tambien aplica a nuevas Edge Functions del backend salvo que una issue tecnica
apruebe una excepcion explicita.

## 3. Envelope De Error

Toda respuesta de error debe devolver JSON con esta forma:

```json
{
  "ok": false,
  "request_id": "uuid",
  "code": "ORDER_NOT_EDITABLE",
  "message": "No se puede confirmar este pedido.",
  "details": null
}
```

Reglas:

- `ok` siempre debe ser `false` en errores.
- `request_id` debe estar disponible en la respuesta y en logs/auditoria.
- `code` es estable y debe ser la referencia principal del frontend.
- `message` debe ser seguro para mostrar en UI.
- `details` solo puede aparecer en desarrollo local y cuando sea seguro.
- nunca se deben devolver secretos, tokens, claves, cookies ni stack traces.

## 4. Envelope De Exito

Las Edge Functions deben usar esta forma recomendada para respuestas exitosas:

```json
{
  "ok": true,
  "request_id": "uuid",
  "data": {}
}
```

Reglas:

- `ok` siempre debe ser `true` en respuestas exitosas.
- `request_id` debe coincidir con el mismo identificador usado para logs.
- `data` contiene el payload especifico de la funcion.

## 5. Codigos Iniciales

| Code | HTTP | Uso esperado |
|---|---:|---|
| `UNAUTHENTICATED` | 401 | Falta sesion valida o token JWT. |
| `FORBIDDEN` | 403 | El usuario no tiene permiso para la accion. |
| `VALIDATION_ERROR` | 400 | Datos obligatorios ausentes o invalidos. |
| `ORDER_NOT_FOUND` | 404 | El pedido no existe o no pertenece al usuario. |
| `ORDER_NOT_EDITABLE` | 409 | El estado del pedido no permite la operacion. |
| `FILE_TOO_LARGE` | 413 | El archivo supera el limite permitido. |
| `UNSUPPORTED_FILE_TYPE` | 415 | El MIME o tipo de archivo no esta permitido. |
| `UPLOAD_FAILED` | 502 | Fallo la carga del archivo en Storage. |
| `METHOD_NOT_ALLOWED` | 405 | Metodo HTTP no soportado por la funcion. |
| `INVALID_JSON` | 400 | El body no se pudo parsear como JSON. |
| `REQUEST_TOO_LARGE` | 413 | La solicitud supera el limite de tamano. |
| `INTERNAL_ERROR` | 500 | Error inesperado del backend. |

El frontend debe tomar decisiones por `code`, no por `message`.

## 6. Mensajes Para UI

Los mensajes deben:

- estar en espanol;
- ser breves y accionables;
- no revelar SQL, rutas internas, stack traces ni nombres de secretos;
- no incluir datos sensibles del usuario;
- poder mostrarse directamente en pantallas o toasts.

Ejemplos:

```json
{
  "ok": false,
  "request_id": "2f0f33e0-d4db-4a28-86f1-7977ecb0c5f1",
  "code": "VALIDATION_ERROR",
  "message": "Revisa los datos enviados.",
  "details": {
    "fields": {
      "copies": "Debe ser mayor o igual a 1."
    }
  }
}
```

```json
{
  "ok": false,
  "request_id": "fb327d99-653b-4db0-9f32-5bc17a1f50f5",
  "code": "UNAUTHENTICATED",
  "message": "Debes iniciar sesion para continuar.",
  "details": null
}
```

## 7. Request Id

Cada Edge Function debe resolver `request_id` con este criterio:

1. usar `x-request-id` si el cliente lo envia y tiene formato valido;
2. generar un UUID si el header no existe o no es valido;
3. devolver el valor en el body y en el header `x-request-id`;
4. usar el mismo valor en auditoria y logs tecnicos.

## 8. Details

`details` debe ser `null` por defecto.

Solo puede incluir informacion adicional cuando se cumplan todas estas
condiciones:

- el entorno es local o de desarrollo;
- la informacion ayuda a depurar validaciones o reglas de negocio;
- no contiene secretos, tokens, claves, cookies, stack traces ni payloads
  completos;
- la Edge Function lo marca explicitamente como seguro.

En produccion, `details` debe responder `null`.

## 9. Helper Compartido

El helper versionado queda en:

```txt
desarrollo/backend-supabase/supabase/functions/_shared/api-response.ts
```

Las Edge Functions deben usarlo para:

- definir `ApiErrorCode`;
- lanzar errores controlados con `ApiError`;
- generar `request_id`;
- responder errores con `errorResponse`;
- responder exito con `successResponse`;
- parsear JSON con `parseJsonBody`;
- responder `OPTIONS` y CORS de forma consistente.

## 10. Trazabilidad

- Issue: #114 - [API] Definir contrato estandar de errores Edge Functions.
- Epica padre: #45 - E05 - Backend MVP en Supabase.
- Subissue tecnica de: #99 - Edge Functions del flujo minimo de pedidos.
- Objetivos: OBJ-006, OBJ-007, OBJ-010.
- RF/RNF: RF-AUD, RF-WEB, RNF-SEG, RNF-AUD, RNF-USA.
- HU: HU-CLI-007, HU-SIS-003, HU-SIS-004.
- Reglas criticas: RNFC-001, RNFC-007, RNFC-009.
