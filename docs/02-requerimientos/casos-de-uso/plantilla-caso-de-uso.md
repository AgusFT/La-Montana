# CU-XXX-000 - Nombre del caso de uso

| Campo | Valor |
|---|---|
| ID | CU-XXX-000 |
| Caso de uso | Nombre del caso de uso |
| Área | Área funcional |
| Actor principal | Actor principal |
| Actores secundarios | Actores secundarios si corresponde |
| Prioridad | P0 Crítica / P1 Alta / P2 Media / P3 Baja |
| Alcance | MVP / Producto base / Post-MVP |
| RF relacionados | RF-XXX-000 |
| RNF relacionados | RNF-XXX-000 |
| HU relacionadas | HU-XXX-000 |
| Reglas críticas relacionadas | RFC-000 / RNFC-000 |

## 1. Caso de Uso

Nombre breve y claro del caso de uso.

## 2. Actores

| Actor | Participación |
|---|---|
| Actor principal | Describe qué hace o qué necesita |
| Actor secundario | Describe participación si corresponde |

## 3. Descripción

Descripción breve del caso de uso.

Debe explicar qué interacción ocurre entre el actor y el sistema, qué necesidad resuelve y por qué es relevante dentro del flujo de La Montaña.

## 4. Precondición

Condiciones que deben cumplirse antes de iniciar el caso de uso.

Ejemplos:

- El usuario está autenticado.
- El pedido existe.
- El pedido pertenece al cliente autenticado.
- El pedido está pendiente de revisión.
- El usuario tiene permisos suficientes.
- El archivo pertenece al pedido indicado.
- El trabajo de impresión fue autorizado por el backend.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| Dato de entrada | Sí / No | Descripción del dato requerido por el caso de uso |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Dato de salida | Resultado o información generada por el caso de uso |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Indicar si requiere usuario autenticado |
| Autorización | Indicar rol o permiso requerido |
| RLS / acceso a datos | Indicar restricción esperada sobre datos |
| Archivos | Indicar restricción esperada sobre Storage si aplica |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El actor inicia la acción. | Si el actor no tiene permisos, el sistema rechaza la operación. |
| 2 | El sistema muestra o solicita la información necesaria. | Si faltan datos obligatorios, el sistema informa el error. |
| 3 | El actor completa o confirma la operación. | Si los datos son inválidos, el sistema solicita corrección. |
| 4 | El sistema valida reglas de negocio y permisos. | Si alguna regla crítica falla, el flujo se detiene. |
| 5 | El sistema registra el resultado de la operación. | Si ocurre un error técnico, el sistema informa la falla y evita inconsistencias. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios / cambia a... |
| Estado visible al cliente | Sin cambios / cambia a... |
| Estado financiero | Sin cambios / cambia a... |
| Estado técnico de impresión | Sin cambios / cambia a... |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Evento relevante | Condición que dispara el registro |

## 11. Observaciones

Notas relevantes del caso de uso.

Pueden incluir:

- reglas de negocio aplicables;
- restricciones de seguridad;
- validaciones importantes;
- relación con Supabase, Storage, RPC, Edge Functions o agente de impresión;
- decisiones pendientes;
- aclaraciones para diseño, desarrollo o pruebas.

## 12. Poscondición

Estado esperado al finalizar el caso de uso.

Ejemplos:

- El pedido queda creado.
- El pedido queda pendiente de revisión.
- El archivo queda asociado al pedido.
- La decisión administrativa queda registrada.
- El trabajo de impresión queda autorizado.
- El pedido queda cerrado de forma consistente.

## 13. Criterios de aceptación

- El caso de uso respeta las reglas críticas del negocio.
- El actor solo puede ejecutar acciones permitidas por su rol y permisos.
- El sistema valida datos obligatorios.
- El sistema evita estados inconsistentes.
- El resultado queda registrado cuando corresponde.
- La operación no depende únicamente del frontend para ser segura.
