# CU-ARC-001 - Cargar archivo al pedido

| Campo | Valor |
|---|---|
| ID | CU-ARC-001 |
| Caso de uso | Cargar archivo al pedido |
| Área | Archivos |
| Actor principal | Cliente |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-ARC-001, RF-ARC-002, RF-ARC-003, RF-ARC-004, RF-PED-004, RF-AUD-001, RF-AUD-003 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-RLS-006, RNF-ARC-001, RNF-ARC-002, RNF-ARC-003, RNF-ARC-005, RNF-ARC-006 |
| HU relacionadas | HU-CLI-003 |
| Reglas críticas relacionadas | RFC-007, RFC-008, RNFC-001, RNFC-003, RNFC-004 |

## 1. Caso de Uso

Cargar archivo al pedido.

## 2. Actores

| Actor | Participación |
|---|---|
| Cliente | Selecciona y carga uno o más archivos asociados a un pedido propio |
| Sistema | Valida autenticación, propiedad del pedido, permisos, estado del pedido, archivo recibido, almacenamiento y asociación con el pedido |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un cliente carga archivos asociados a un pedido propio.

Los archivos son parte central del flujo de La Montaña, ya que permiten la revisión administrativa, la validación operativa y la posterior producción o impresión del pedido.

La carga de archivos debe realizarse mediante mecanismos autorizados del sistema y almacenarse de forma centralizada en Supabase Storage o mecanismo equivalente definido por la arquitectura.

El sistema no debe depender de rutas locales del dispositivo del cliente como mecanismo de impresión. Una vez cargado, cada archivo debe quedar asociado al pedido correspondiente y disponible solo para usuarios o componentes autorizados.

Este caso de uso no valida administrativamente el archivo, no autoriza producción y no genera trabajos de impresión. Esos pasos se trabajan en casos de uso posteriores.

## 4. Precondición

- El cliente está autenticado.
- El pedido existe.
- El pedido pertenece al cliente autenticado.
- El pedido se encuentra en un estado que permite carga o modificación de archivos.
- El backend Supabase está disponible.
- Supabase Storage o el mecanismo de almacenamiento definido está disponible.
- Las políticas de acceso deben impedir que el cliente cargue archivos en pedidos ajenos.
- El sistema tiene definidas las restricciones básicas de archivos permitidos, como tipo, tamaño o cantidad máxima, si corresponde.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido al que se asociará el archivo |
| Usuario autenticado | Sí | Cliente que realiza la carga del archivo |
| Archivo | Sí | Archivo seleccionado por el cliente para asociarlo al pedido |
| Nombre del archivo | Sí | Nombre original o nombre normalizado del archivo |
| Tipo de archivo | Sí | Tipo MIME o extensión del archivo |
| Tamaño del archivo | Sí | Tamaño del archivo para validaciones de límite |
| Descripción del archivo | No | Comentario opcional del cliente sobre el archivo |
| Canal de acceso | No | Web o Android, según desde dónde se realiza la operación |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Archivo cargado | Archivo almacenado correctamente en el sistema |
| ID del archivo | Identificador único del archivo dentro del sistema |
| Pedido asociado | Pedido al que queda vinculado el archivo |
| Ruta o referencia interna | Referencia segura utilizada por el sistema para ubicar el archivo |
| Estado inicial del archivo | Estado inicial asociado al archivo, por ejemplo pendiente de revisión |
| Evento de auditoría | Registro de carga del archivo |
| Mensaje al cliente | Confirmación de carga exitosa o error controlado |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere cliente autenticado |
| Autorización | Solo el cliente propietario del pedido puede cargar archivos en ese pedido |
| RLS / acceso a datos | La asociación archivo-pedido debe estar protegida por políticas de acceso |
| Storage | El archivo debe almacenarse mediante mecanismo autorizado y no mediante rutas locales del cliente |
| Propiedad del pedido | El sistema debe validar que el pedido pertenece al cliente autenticado |
| Estado del pedido | Solo se permite cargar archivos si el pedido se encuentra en una etapa editable |
| Validación backend | La validación de permisos, pedido y archivo no debe depender únicamente del frontend |
| Acceso posterior | El archivo solo debe ser visible o accesible para usuarios y componentes autorizados |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El cliente ingresa al sistema desde Web o Android. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El cliente accede a la sección de sus pedidos. | Si no tiene pedidos, el sistema muestra una vista vacía o mensaje correspondiente. |
| 3 | El cliente selecciona el pedido al que desea agregar archivos. | Si el pedido no existe, el sistema informa que no está disponible. |
| 4 | El sistema valida que el pedido pertenece al cliente autenticado. | Si el pedido no pertenece al cliente, el sistema bloquea la operación y registra el intento si corresponde. |
| 5 | El sistema valida que el pedido admite carga de archivos. | Si el pedido ya no permite cambios de archivos, el sistema rechaza la operación. |
| 6 | El cliente selecciona uno o más archivos desde su dispositivo. | Si el cliente cancela la selección, no se realiza ninguna carga. |
| 7 | El sistema valida tipo, tamaño y condiciones básicas del archivo. | Si el archivo no cumple las restricciones, el sistema informa el motivo del rechazo. |
| 8 | El sistema carga el archivo al almacenamiento autorizado. | Si falla la carga, el sistema informa la falla y evita asociar archivos incompletos. |
| 9 | El sistema registra la metadata del archivo. | Si la metadata no puede registrarse, el sistema debe evitar inconsistencias entre Storage y base de datos. |
| 10 | El sistema asocia el archivo al pedido correspondiente. | Si no puede asociarse el archivo al pedido, la operación debe revertirse o quedar marcada para revisión técnica. |
| 11 | El sistema asigna un estado inicial al archivo, por ejemplo pendiente de revisión. | Si no existe estado inicial válido, debe evitarse que el archivo quede en estado ambiguo. |
| 12 | El sistema registra evento de auditoría por carga de archivo. | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente. |
| 13 | El sistema informa al cliente que el archivo fue cargado correctamente. | Si ocurre un error final, el sistema informa el problema y evita duplicar archivos ante reintentos. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Puede mantenerse sin cambios o marcar que el pedido tiene archivos pendientes de revisión, según modelo de estados |
| Estado visible al cliente | Puede mantenerse como pendiente de revisión o indicar que el archivo fue recibido |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Sin cambios. No se genera trabajo de impresión |
| Estado del archivo | Se asigna un estado inicial equivalente a pendiente de revisión |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Archivo cargado | Cuando el archivo se almacena y asocia correctamente al pedido |
| Archivo rechazado por validación | Cuando el archivo no cumple tipo, tamaño u otra restricción definida |
| Intento de carga no autorizada | Cuando un usuario intenta cargar archivos en un pedido ajeno o no editable |
| Error de carga de archivo | Cuando ocurre una falla técnica durante la carga |
| Asociación archivo-pedido creada | Cuando el archivo queda vinculado al pedido correspondiente |

## 11. Observaciones

- Este caso de uso no valida administrativamente el contenido del archivo.
- Este caso de uso no aprueba el archivo para producción.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no cambia estados financieros.
- El archivo cargado debe quedar asociado al pedido correspondiente.
- El sistema no debe depender de rutas locales del dispositivo del cliente para imprimir.
- La validación de archivos debe contemplar tipo, tamaño y restricciones definidas por el producto.
- La revisión administrativa del archivo se trabaja en casos de uso posteriores.
- El acceso al archivo debe estar protegido mediante permisos, Storage policies, RLS o mecanismo equivalente.
- Si la carga se realiza desde Android, debe respetar las mismas reglas de backend que la Web.
- La carga de archivos puede ocurrir durante la creación del pedido o como operación posterior mientras el pedido siga siendo editable.

## 12. Poscondición

Al finalizar correctamente:

- el archivo queda cargado en el almacenamiento autorizado;
- el archivo queda asociado al pedido correcto;
- el archivo queda vinculado al cliente propietario del pedido mediante la relación pedido-cliente;
- el archivo queda en estado inicial pendiente de revisión o equivalente;
- el archivo no queda autorizado automáticamente para producción;
- no se genera ningún trabajo de impresión;
- el sistema registra trazabilidad de la carga;
- el cliente recibe confirmación de la carga.

## 13. Criterios de aceptación

- El cliente autenticado puede cargar archivos en pedidos propios.
- El sistema rechaza cargas en pedidos ajenos.
- El sistema rechaza cargas en pedidos que ya no admiten cambios de archivos.
- El sistema valida tipo, tamaño y condiciones básicas del archivo.
- El archivo se almacena de forma centralizada y autorizada.
- El archivo queda asociado al pedido correspondiente.
- El sistema no usa rutas locales del cliente como mecanismo de impresión.
- El archivo no queda aprobado automáticamente para producción.
- El sistema registra evento de auditoría por la carga.
- La operación se valida en backend, RLS, Storage policies, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.