# CU-PED-004 — Modificar pedido antes de revisión

| Campo                        | Valor                                                                 |
| ---------------------------- | --------------------------------------------------------------------- |
| **ID**                       | CU-PED-004                                                             |
| **Caso de uso**              | Modificar pedido antes de revisión                                     |
| **Área**                     | Pedidos                                                                |
| **Actor principal**          | Cliente                                                                |
| **Actores secundarios**      | Sistema                                                                |
| **Prioridad**                | P1 Alta                                                                |
| **Alcance**                  | MVP                                                                    |
| **RF relacionados**          | RF-PED-004, RF-PED-006, RF-ARC-002, RF-EST-003                         |
| **RNF relacionados**         | RNF-SEG-003, RNF-SEG-004, RNF-AUD-001, RNF-RLS-002                     |
| **HU relacionadas**          | HU-CLI-005, HU-CLI-012                                                 |
| **Reglas críticas**          | RFC-001, RFC-004, RFC-007, RNFC-001, RNFC-003                          |

---

## 1. Caso de uso
El cliente modifica un pedido propio que aún no fue revisado por el personal interno ni autorizado para producción.

---

## 2. Actores
| Actor    | Participación                                                                                         |
|----------|---------------------------------------------------------------------------------------------------------|
| Cliente  | Edita archivos y detalles permitidos del pedido.                                                        |
| Sistema  | Valida autenticación, permisos, estado del pedido, propiedad y reglas de negocio; registra auditoría.   |

---

## 3. Descripción
Este caso de uso permite que un cliente realice modificaciones a un pedido que permanece en **Pendiente de revisión**, siempre que el pedido aún no haya sido revisado, cotizado, confirmado ni enviado a impresión.

El cliente puede modificar únicamente:
- archivos adjuntos (agregar, reemplazar, eliminar);
- descripción o aclaraciones;
- cantidad estimada;
- datos de contacto o punto de retiro;
- observaciones del cliente.

El cliente **no puede**:
- modificar precios;
- alterar estados internos, visibles o financieros;
- cambiar cantidades definidas por el negocio;
- acceder o modificar información administrativa;
- revertir pasos avanzados del flujo.

El pedido conserva el estado **Pendiente de revisión** luego de la modificación.

---

## 4. Precondición
- El cliente está autenticado.
- El pedido existe.
- El pedido pertenece al cliente autenticado.
- El pedido está en estado **Pendiente de revisión**.
- El pedido no fue revisado, cotizado, confirmado ni enviado a impresión.
- Las políticas RLS del backend permiten acceso solo a pedidos propios.
- Supabase (auth, storage y base de datos) está disponible.
- El backend puede validar propiedad, permisos y estado antes de permitir cualquier modificación.

---

## 5. Datos de entrada
| Dato                | Obligatorio | Descripción                                                            |
|---------------------|-------------|------------------------------------------------------------------------|
| ID del pedido       | Sí          | Identificador del pedido a modificar                                   |
| Archivos del pedido | No          | Archivos a añadir, reemplazar o eliminar                               |
| Detalles del pedido | No          | Campos editables permitidos                                             |
| Canal de acceso     | Sí          | Web o Android                                                           |

---

## 6. Datos de salida
| Dato                  | Descripción                                                     |
|-----------------------|-----------------------------------------------------------------|
| Pedido actualizado    | Pedido registrado con los cambios permitidos                    |
| Estado visible        | Permanece **Pendiente de revisión**                             |
| Registro de auditoría | Evento de modificación guardado                                 |
| Archivos actualizados | Archivos agregados, reemplazados o eliminados correctamente     |

---

## 7. Permisos y seguridad
| Aspecto              | Regla                                                                 |
|----------------------|-----------------------------------------------------------------------|
| Autenticación        | Requiere cliente autenticado                                           |
| Autorización         | Solo el cliente propietario del pedido puede modificarlo              |
| RLS / acceso a datos | Validación de propiedad y estado exclusivamente del lado backend       |
| Estados              | Solo permitido en **Pendiente de revisión**                            |
| Archivos             | Storage accesible por políticas autorizadas                            |
| Backend-first        | Toda validación debe ejecutarse en RPC/Edge Function, nunca en client |
| Seguridad            | Modificaciones no autorizadas deben registrarse como auditoría         |

---

## 8. Flujo principal
| Paso | Flujo principal                                                                   | Alternativa / Excepción                                                                           |
|------|-----------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| 1    | El cliente accede a “Mis pedidos”.                                                | Si no está autenticado → solicitar inicio de sesión.                                               |
| 2    | Selecciona un pedido en estado **Pendiente de revisión**.                         | Si el pedido está en otro estado → operación rechazada.                                            |
| 3    | El sistema valida propiedad, estado y permisos.                                   | Si no pertenece al cliente → bloquear, auditar intento y denegar acceso.                           |
| 4    | El sistema muestra archivos existentes y campos editables.                        | Si hay error técnico → notificar al cliente.                                                        |
| 5    | El cliente edita archivos y/o detalles permitidos.                                | Si faltan datos requeridos → indicar corrección.                                                    |
| 6    | El cliente confirma las modificaciones.                                           | Si cancela → no se registran cambios.                                                               |
| 7    | El sistema valida las modificaciones y actualiza el pedido.                       | Si un archivo falla → indicar error sin exponer rutas internas.                                    |
| 8    | El pedido permanece en **Pendiente de revisión**.                                 | —                                                                                                  |
| 9    | El sistema registra auditoría de modificación.                                    | Si auditoría falla → registrar alerta técnica.                                                      |
| 10   | El sistema confirma al cliente la modificación exitosa.                           | Si ocurre error interno → revertir cambios y permitir reintento.                                   |

---

## 9. Impacto en estados
| Estado                      | Impacto                                                    |
|-----------------------------|------------------------------------------------------------|
| Estado interno              | Sin cambios (Pendiente de revisión)                        |
| Estado visible al cliente   | Sin cambios (Pendiente de revisión)                        |
| Estado financiero           | Sin cambios                                                |
| Estado técnico de impresión | Sin cambios; no se genera trabajo de impresión            |

---

## 10. Eventos de auditoría
| Evento                    | Momento                                                     |
|---------------------------|--------------------------------------------------------------|
| Modificación de pedido    | Cada vez que se edita un detalle o archivo                   |
| Modificación no permitida | Intento sobre pedido ajeno o en estado no editable           |
| Error al actualizar       | Fallas técnicas                                              |
| Archivo modificado        | Alta, reemplazo o eliminación de archivo                     |

---

## 11. Observaciones
- No se modifica información administrativa ni financiera.
- El cliente solo altera elementos no críticos.
- Toda validación es backend-first (Supabase RLS, RPC, Edge Functions).
- No se genera producción ni estados nuevos.
- Complementa el **CU-ARC-002** para manejo de archivos.
- La experiencia debe ser equivalente entre Web y Android.

---

## 12. Poscondición
- El pedido queda actualizado con los cambios permitidos.
- El estado sigue siendo **Pendiente de revisión**.
- Se registra auditoría.
- No se generan trabajos de impresión.
- No se alteran estados internos, visibles o financieros.

---

## 13. Criterios de aceptación
- El cliente autenticado puede modificar solo pedidos propios.
- El pedido debe estar en **Pendiente de revisión**.
- Cambios sobre pedidos confirmados o enviados a impresión deben rechazarse.
- Solo se permiten modificaciones en archivos y detalles no críticos.
- El sistema debe registrar auditoría.
- No debe modificarse precio, estados ni datos internos.
- Validaciones obligatoriamente en backend.
- Cumplimiento completo de RF, RNF, HU y reglas críticas.

