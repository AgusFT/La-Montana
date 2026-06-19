# CU-ARC-002 - Consultar archivos del pedido

| Campo | Valor |
|---|---|
| ID | CU-ARC-002 |
| Caso de uso | Consultar archivos del pedido |
| Área | Archivos |
| Actor principal | Usuario autorizado |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-ARC-002, RF-ARC-004, RF-ARC-005, RF-PED-004, RF-PED-005, RF-EST-006 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-005, RNF-RLS-006, RNF-ARC-001, RNF-ARC-002, RNF-ARC-003, RNF-ARC-005, RNF-USA-002 |
| HU relacionadas | HU-CLI-003, HU-EMP-002 |
| Reglas críticas relacionadas | RFC-007, RFC-008, RNFC-001, RNFC-003, RNFC-004, RNFC-005 |

## 1. Caso de Uso

Consultar archivos del pedido.

## 2. Actores

| Actor | Participación |
|---|---|
| Cliente | Consulta archivos asociados a pedidos propios, según permisos y estado del pedido |
| Empleado | Consulta archivos asociados a pedidos internos para revisión u operación |
| Administrador | Consulta archivos asociados a pedidos con permisos ampliados |
| Sistema | Valida autenticación, permisos, propiedad del pedido, asociación archivo-pedido y reglas de acceso |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un usuario autorizado consulta los archivos asociados a un pedido.

Los archivos del pedido son parte central del flujo de La Montaña, porque permiten revisar, validar, preparar, producir e imprimir un pedido. La consulta de archivos debe respetar la propiedad del pedido, el rol del usuario, los permisos internos y las políticas de acceso definidas.

El cliente solo puede consultar archivos asociados a sus propios pedidos. Los empleados y administradores pueden consultar archivos de pedidos internos según permisos.

Este caso de uso es de solo lectura. No carga archivos nuevos, no reemplaza archivos, no elimina archivos, no valida administrativamente archivos, no autoriza producción y no genera trabajos de impresión.

## 4. Precondición

- El usuario está autenticado.
- El pedido existe.
- El pedido tiene archivos asociados o puede devolver una lista vacía.
- El usuario tiene permiso para consultar el pedido.
- El usuario tiene permiso para consultar los archivos asociados al pedido.
- El backend Supabase está disponible.
- Supabase Storage o el mecanismo de almacenamiento definido está disponible.
- La relación entre pedido y archivo está registrada en el sistema.
- Las políticas de acceso deben impedir consultar archivos de pedidos no autorizados.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido cuyos archivos se desean consultar |
| Usuario autenticado | Sí | Usuario que realiza la consulta |
| Rol o permisos del usuario | Sí | Determina el nivel de acceso a los archivos |
| ID del archivo | No | Identificador de un archivo puntual si se consulta un archivo específico |
| Canal de acceso | No | Web o Android, según desde dónde se realiza la consulta |
| Parámetros de listado | No | Paginación, ordenamiento o filtros si existen múltiples archivos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Lista de archivos | Archivos asociados al pedido visibles para el usuario |
| ID del archivo | Identificador del archivo dentro del sistema |
| Nombre del archivo | Nombre visible o normalizado del archivo |
| Tipo de archivo | Tipo MIME o extensión del archivo |
| Tamaño del archivo | Tamaño registrado del archivo |
| Estado del archivo | Estado interno o visible permitido, por ejemplo pendiente de revisión, aceptado o rechazado |
| Fecha de carga | Momento en que el archivo fue cargado |
| Usuario que cargó el archivo | Usuario asociado a la carga, si corresponde y si es visible para el rol |
| URL o acceso autorizado | Mecanismo temporal o autorizado para consultar o descargar el archivo |
| Mensaje de acceso | Confirmación, lista vacía o error controlado |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere usuario autenticado |
| Autorización | El usuario debe tener permiso para consultar el pedido y sus archivos |
| Cliente | Solo puede consultar archivos de pedidos propios |
| Empleado | Puede consultar archivos de pedidos internos según permisos |
| Administrador | Puede consultar archivos de pedidos con permisos ampliados |
| RLS / acceso a datos | La relación pedido-archivo debe protegerse mediante políticas de acceso |
| Storage | El acceso al archivo debe realizarse mediante mecanismo autorizado, no mediante rutas locales |
| URLs o accesos temporales | Si se usan URLs firmadas o mecanismos equivalentes, deben ser limitados y autorizados |
| Información interna | No debe exponerse metadata interna al cliente si no corresponde |
| Validación backend | La autorización no debe depender únicamente del frontend |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El usuario ingresa al sistema desde Web o Android. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a un pedido. | Si el pedido no existe, el sistema informa que no está disponible. |
| 3 | El sistema valida que el usuario tenga permiso para consultar el pedido. | Si no tiene permiso, el sistema rechaza la operación. |
| 4 | El usuario solicita ver los archivos asociados al pedido. | Si la sección de archivos no está disponible, el sistema informa la falla. |
| 5 | El sistema consulta la relación entre pedido y archivos. | Si ocurre un error de consulta, el sistema informa la falla sin exponer datos internos. |
| 6 | El sistema valida permisos sobre cada archivo asociado. | Si algún archivo no es visible para el usuario, no se incluye en la respuesta. |
| 7 | El sistema muestra la lista de archivos permitidos. | Si no hay archivos visibles, muestra una lista vacía o mensaje correspondiente. |
| 8 | El usuario selecciona un archivo para consultar o descargar. | Si el archivo no existe o no está disponible, el sistema informa la situación. |
| 9 | El sistema genera o recupera un acceso autorizado al archivo. | Si no puede generar acceso autorizado, rechaza la consulta. |
| 10 | El sistema entrega el acceso permitido al archivo. | Si el acceso expira o falla, el usuario debe solicitar uno nuevo. |
| 11 | El sistema registra evento de consulta si corresponde. | Si se detecta acceso no autorizado, debe registrarse como evento de seguridad. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios. La consulta de archivos no modifica el estado del pedido |
| Estado visible al cliente | Sin cambios |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Sin cambios. No se genera ni modifica ningún trabajo de impresión |
| Estado del archivo | Sin cambios. El archivo solo se consulta |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Consulta de archivos del pedido | Cuando un usuario autorizado consulta la lista de archivos |
| Consulta o descarga de archivo | Cuando un usuario accede a un archivo específico, si se decide auditar este evento |
| Intento de acceso no autorizado | Cuando un usuario intenta consultar archivos de un pedido no autorizado |
| Error de acceso a archivo | Cuando ocurre una falla técnica al obtener el archivo |
| Acceso autorizado generado | Cuando se genera una URL firmada o mecanismo equivalente de acceso temporal |

## 11. Observaciones

- Este caso de uso no carga archivos nuevos.
- Este caso de uso no reemplaza ni elimina archivos.
- Este caso de uso no valida administrativamente archivos.
- Este caso de uso no habilita archivos para impresión.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no modifica estados del pedido ni del archivo.
- El cliente solo puede ver archivos asociados a sus propios pedidos.
- Los usuarios internos solo pueden ver archivos según permisos.
- El sistema no debe usar rutas locales del cliente como mecanismo de acceso o impresión.
- El acceso a archivos debe estar protegido por Storage policies, RLS, signed URLs o mecanismo equivalente.
- Si un archivo debe ser usado por el agente de impresión, ese acceso se define en casos de uso del dominio `05-impresion`.
- La consulta de archivos debe ser consistente entre Web y Android cuando la funcionalidad esté habilitada en ambos canales.

## 12. Poscondición

Al finalizar correctamente:

- el usuario visualiza la lista de archivos permitidos;
- el usuario puede acceder únicamente a archivos autorizados;
- no se modifican datos del pedido;
- no se modifican estados del pedido;
- no se modifican estados del archivo;
- no se genera ningún trabajo de impresión;
- el acceso queda controlado por permisos, RLS, Storage policies o mecanismo equivalente;
- los eventos relevantes quedan registrados si corresponde.

## 13. Criterios de aceptación

- El usuario autenticado puede consultar archivos solo si tiene permisos sobre el pedido.
- El cliente no puede consultar archivos de pedidos ajenos.
- Los empleados y administradores consultan archivos según permisos internos.
- El sistema rechaza accesos no autorizados.
- El sistema no expone rutas internas ni rutas locales del cliente.
- El sistema puede devolver lista vacía si el pedido no tiene archivos visibles.
- El acceso al archivo se realiza mediante mecanismo autorizado.
- La consulta no modifica estados ni datos del pedido.
- La consulta no autoriza producción ni impresión.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.