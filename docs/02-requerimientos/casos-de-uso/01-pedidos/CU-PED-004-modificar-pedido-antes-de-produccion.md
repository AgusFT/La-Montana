# CU-PED-004 - Modificar pedido antes de producción

| Campo                        | Valor                                              |
| ---------------------------- | -------------------------------------------------- |
| ID                           | CU-PED-004                                         |
| Caso de uso                  | Modificar pedido antes de producción               |
| Área                         | Pedidos                                            |
| Actor principal              | Cliente                                            |
| Actores secundarios          | Sistema                                            |
| Prioridad                    | P1 Alta                                            |
| Alcance                      | MVP                                                |
| RF relacionados              | RF-PED-004, RF-ARC-002, RF-EST-003                 |
| RNF relacionados             | RNF-SEG-003, RNF-SEG-004, RNF-AUD-001, RNF-RLS-002 |
| HU relacionadas              | HU-CLI-004, HU-CLI-005                             |
| Reglas críticas relacionadas | RFC-001, RFC-004, RFC-007, RNFC-001, RNFC-003      |
| ---------------------------- | -------------------------------------------------- |

## 1. Caso de Uso

Modificar un pedido propio.

## 2. Actores
| Actor   | Participación                                                                                        |
| ------- | ---------------------------------------------------------------------------------------------------- |
| Cliente | Modifica archivos y detalles permitidos del pedido antes de la revisión                              |
| Sistema | Valida autenticación, permisos, estado del pedido, propiedad, reglas de negocio y registra auditoría |


## 3. Descripción

Este caso de uso describe el proceso mediante el cual un cliente modifica un pedido que todavía no fue revisado, confirmado ni enviado a impresión.

El cliente solo puede editar archivos y ciertos detalles no críticos del pedido (por ejemplo: nombre de archivo, aclaraciones, cantidad estimada, método de contacto, punto de entrega, observaciones del cliente).

El cliente no puede modificar precios, estados, cantidades finales calculadas por el negocio, variaciones internas, información administrativa, financiera ni ningún dato controlado por el sistema o por el personal interno.

El pedido permanece en estado Pendiente de revisión hasta que el cliente finaliza y guarda los cambios.

## 4. Precondición
- El cliente está autenticado.
- El pedido existe.
- El pedido pertenece al cliente autenticado.
- El pedido se encuentra en estado Pendiente de revisión.
- El pedido no fue revisado, confirmado ni enviado a impresión.
- El backend Supabase está disponible.
- El sistema puede recuperar y validar la propiedad del pedido mediante RLS, RPC o políticas equivalentes.
- Las políticas de acceso deben impedir que el cliente modifique pedidos ajenos o pedidos ya confirmados.
- El sistema debe verificar permisos, propiedad y estado antes de permitir modificaciones.

## 5. Datos de entrada
| Dato                | Obligatorio | Descripción                                                            |
| ------------------- | ----------- | ---------------------------------------------------------------------- |
| ID del pedido       | Sí          | Identificador del pedido que se desea modificar                        |
| Archivos del pedido | No          | Archivos a añadir, reemplazar o eliminar                               |
| Detalles del pedido | No          | Aclaraciones, descripción, cantidad estimada u otros campos permitidos |
| Canal de acceso     | Sí          | Web o Android (cliente final)                                          |

## 6. Datos de salida
| Dato                  | Descripción                                                 |
| --------------------- | ----------------------------------------------------------- |
| Pedido actualizado    | El pedido queda actualizado con los cambios permitidos      |
| Estado visible        | Permanece en Pendiente de revisión                          |
| Registro de auditoría | Auditoría de modificaciones realizadas                      |
| Archivos actualizados | Archivos agregados, reemplazados o eliminados correctamente |

## 7. Permisos y seguridad
| Aspecto              | Regla                                                                       |
| -------------------- | --------------------------------------------------------------------------- |
| Autenticación        | Requiere cliente autenticado                                                |
| Autorización         | Solo el cliente propietario del pedido puede modificarlo                    |
| RLS / acceso a datos | El backend debe validar propiedad y estado; prohibido depender del frontend |
| Estados              | Solo pedidos en Pendiente de revisión pueden modificarse                    |
| Archivos             | Deben almacenarse y asociarse mediante mecanismos autorizados               |
| Validación backend   | Toda la lógica de verificación debe ejecutarse en backend/RPC               |
| Seguridad            | Se deben registrar intentos de modificación no autorizados                  |

## 8. Flujo principal
| Paso | Flujo principal                                                           | Flujo alternativo / excepciones                                                     |
| ---- | ------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| 1    | El cliente accede a la sección “Mis pedidos”.                             | Si el cliente no está autenticado, se solicita iniciar sesión.                      |
| 2    | El cliente selecciona un pedido en estado Pendiente de revisión.          | Si el pedido no está en ese estado, el sistema rechaza la operación.                |
| 3    | El sistema valida propiedad, autenticación y estado del pedido.           | Si el pedido no pertenece al cliente, se bloquea el acceso y se registra auditoría. |
| 4    | El sistema muestra los archivos y detalles editables.                     | Si el pedido está bloqueado por error técnico, se informa al cliente.               |
| 5    | El cliente edita archivos y/o detalles permitidos.                        | Si faltan datos requeridos (si aplica), el sistema indica qué datos corregir.       |
| 6    | El cliente confirma los cambios.                                          | Si el cliente cancela, no se aplican modificaciones.                                |
| 7    | El sistema valida las modificaciones y actualiza el pedido.               | Si los archivos tienen errores, se informa la falla sin exponer rutas internas.     |
| 8    | El pedido permanece en estado Pendiente de revisión.                      | -                                                                                   |
| 9    | El sistema registra un evento de auditoría correspondiente.               | Si la auditoría falla, se registra alerta técnica.                                  |
| 10   | El sistema informa al cliente que el pedido fue modificado correctamente. | Si ocurre un error de guardado, se evita inconsistencia y se permite reintentar.    |

## 9. Impacto en estados
| Estado                      | Impacto                                                      |
| --------------------------- | ------------------------------------------------------------ |
| Estado interno              | Sin cambios: permanece Pendiente de revisión                 |
| Estado visible al cliente   | Permanece Pendiente de revisión                              |
| Estado financiero           | Sin cambios                                                  |
| Estado técnico de impresión | Sin cambios; no se genera ni modifica ningún trabajo técnico |

## 10. Eventos de auditoría
| Evento                    | Cuándo se registra                                   |
| ------------------------- | ---------------------------------------------------- |
| Modificación de pedido    | Cuando el cliente modifica detalles o archivos       |
| Modificación no permitida | Si se intenta modificar un pedido confirmado o ajeno |
| Error al actualizar       | Cuando ocurre una falla técnica en el proceso        |
| Archivo modificado        | Cuando se agrega, reemplaza o elimina un archivo     |

## 11. Observaciones
- Este caso de uso no modifica precios, estados, información financiera ni datos administrativos.
- El cliente solo puede modificar detalles no críticos y archivos.
- La validación debe ser obligatoriamente backend-first (RLS, RPC, políticas).
- No se autoriza la producción ni se generan trabajos de impresión.
- Este caso de uso se complementa con CU-ARC-002 (actualización de archivos).
- Los cambios no deben exponer información interna del negocio.
- La experiencia debe ser consistente entre Web y Android.

## 12. Poscondición
Al finalizar correctamente:

- el pedido queda modificado con los cambios permitidos;
- el pedido permanece en estado Pendiente de revisión;
- no se modifican datos críticos ni administrativos;
- el sistema registra la auditoría correspondiente;
- no se generan trabajos de impresión ni estados internos nuevos.

## 13. Criterios de aceptación
- El cliente autenticado puede modificar únicamente pedidos propios.
- El pedido solo puede modificarse si está en Pendiente de revisión.
- El sistema rechaza modificaciones a pedidos confirmados o enviados a impresión.
- El cliente solo puede modificar archivos y detalles permitidos.
- Los cambios se guardan correctamente y se registra auditoría.
- Los intentos no autorizados son bloqueados y auditados.
- El pedido no cambia de estado ni modifica datos críticos.
- El flujo cumple con RF, RNF, HU y reglas críticas asociadas.
