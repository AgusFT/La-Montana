# CU-REV-001 - Revisar pedido nuevo

| Campo | Valor |
|---|---|
| ID | CU-REV-001 |
| Caso de uso | Revisar pedido nuevo |
| Área | Revisión administrativa |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-REV-001, RF-REV-002, RF-REV-003, RF-REV-005, RF-PED-003, RF-PED-005, RF-ARC-005, RF-ARC-007, RF-EST-001, RF-EST-002, RF-EST-003, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-002, RNF-RLS-004, RNF-ARC-002, RNF-AUD-001, RNF-AUD-002, RNF-AUD-004 |
| HU relacionadas | HU-ADM-001, HU-ADM-002, HU-EMP-002, HU-SIS-001 |
| Reglas críticas relacionadas | RFC-001, RFC-002, RFC-003, RFC-004, RFC-007, RNFC-001, RNFC-003, RNFC-004, RNFC-005 |

## 1. Caso de Uso

Revisar pedido nuevo.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Revisa datos y archivos de un pedido nuevo según permisos internos |
| Administrador | Revisa pedidos nuevos con permisos ampliados y puede decidir el próximo paso administrativo |
| Sistema | Valida permisos, estado del pedido, acceso a archivos, reglas de negocio y registra trazabilidad |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador revisa un pedido nuevo creado por un cliente.

Todo pedido creado por un cliente debe quedar inicialmente pendiente de revisión. Ningún pedido nuevo puede avanzar automáticamente a producción.

La revisión administrativa tiene como objetivo verificar si el pedido cuenta con información suficiente, archivos asociados válidos o revisables, condiciones mínimas para continuar y ausencia de inconsistencias evidentes.

El resultado de la revisión puede derivar en distintos caminos posteriores, como aprobar el pedido para continuar el flujo, solicitar correcciones al cliente o rechazar el pedido. Esos caminos se documentan en casos de uso específicos.

Este caso de uso se enfoca en la acción de revisar y evaluar el pedido, no en aprobarlo definitivamente, solicitar corrección o rechazarlo.

## 4. Precondición

- El empleado o administrador está autenticado.
- El usuario tiene permisos internos para revisar pedidos.
- El pedido existe.
- El pedido fue creado por un cliente.
- El pedido se encuentra en estado pendiente de revisión o equivalente.
- El pedido no fue habilitado para producción.
- El pedido no tiene trabajos de impresión autorizados.
- El backend Supabase está disponible.
- Los archivos asociados, si existen, están disponibles mediante acceso autorizado.
- Las políticas de acceso impiden revisar pedidos fuera del alcance permitido del usuario.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido que se revisa |
| Usuario interno autenticado | Sí | Empleado o administrador que realiza la revisión |
| Datos del pedido | Sí | Información declarada por el cliente y datos registrados por el sistema |
| Archivos asociados | No | Archivos cargados por el cliente, si existen |
| Observaciones de revisión | No | Comentarios internos registrados durante la revisión |
| Resultado preliminar | No | Evaluación inicial: apto para aprobar, requiere corrección, requiere rechazo o requiere análisis adicional |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Pedido revisado | Pedido evaluado por usuario interno |
| Resultado preliminar de revisión | Conclusión inicial de la revisión administrativa |
| Observación interna | Comentario interno asociado al pedido, si corresponde |
| Archivos evaluados | Archivos consultados o marcados para validación, aceptación o rechazo |
| Evento de auditoría | Registro de la revisión realizada |
| Próximo paso sugerido | Aprobar, solicitar corrección, rechazar o continuar análisis, según resultado |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere empleado o administrador autenticado |
| Autorización | Solo usuarios internos con permisos de revisión pueden revisar pedidos nuevos |
| RLS / acceso a datos | El pedido y sus datos asociados deben protegerse mediante políticas de acceso |
| Archivos | El acceso a archivos debe realizarse mediante mecanismos autorizados |
| Estados internos | El usuario puede consultar estados internos solo si tiene permisos |
| Información financiera | Solo se muestra si el rol o permiso lo habilita |
| Cliente final | El cliente no puede realizar revisión administrativa |
| Validación backend | La revisión no debe depender únicamente del frontend |
| Auditoría | La revisión debe quedar registrada con usuario, fecha y contexto |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado o administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a la sección interna de pedidos pendientes de revisión. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El sistema lista los pedidos pendientes de revisión visibles para el usuario. | Si no hay pedidos pendientes, muestra una vista vacía o mensaje correspondiente. |
| 4 | El usuario selecciona un pedido nuevo para revisar. | Si el pedido no existe o ya no está pendiente de revisión, el sistema informa el estado actual. |
| 5 | El sistema valida permisos sobre el pedido seleccionado. | Si el usuario no tiene permiso sobre ese pedido, rechaza la operación. |
| 6 | El sistema muestra los datos del pedido y archivos asociados permitidos. | Si algún archivo no está disponible, informa la situación sin exponer rutas internas. |
| 7 | El usuario revisa datos declarados por el cliente, observaciones y archivos asociados. | Si faltan datos o archivos, puede registrar observación y derivar a solicitud de corrección. |
| 8 | El usuario evalúa si el pedido puede continuar el flujo o requiere acción adicional. | Si detecta inconsistencias críticas, puede derivar a rechazo o análisis adicional. |
| 9 | El usuario registra observaciones internas si corresponde. | Si no tiene permiso para registrar observaciones, el sistema bloquea esa acción. |
| 10 | El sistema registra el evento de revisión administrativa. | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente. |
| 11 | El sistema deja disponible el pedido para el próximo caso de uso correspondiente. | Si el sistema no puede determinar próximo paso, mantiene el pedido pendiente de revisión o requiere intervención. |
| 12 | El sistema informa que la revisión fue registrada. | Si ocurre un error final, informa la falla y evita estados inconsistentes. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Puede mantenerse pendiente de revisión o quedar listo para una acción posterior según resultado preliminar |
| Estado visible al cliente | Sin cambios directo, salvo que luego se solicite corrección, rechazo o comunicación visible |
| Estado financiero | Sin cambios directo, salvo que la revisión determine que debe evaluarse seña o condición financiera |
| Estado técnico de impresión | Sin cambios. No se genera ni modifica ningún trabajo de impresión |
| Estado de archivos | Sin cambios directo, salvo que se derive a validación o rechazo de archivos en casos específicos |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Pedido revisado | Cuando un usuario interno registra la revisión administrativa |
| Observación de revisión registrada | Cuando se deja una observación interna durante la revisión |
| Acceso a archivos durante revisión | Puede registrarse cuando se consultan archivos asociados |
| Falta de información detectada | Cuando la revisión identifica datos o archivos faltantes |
| Inconsistencia detectada | Cuando la revisión identifica problemas que impiden continuar |
| Intento de revisión no autorizada | Cuando un usuario sin permisos intenta revisar un pedido |
| Error de revisión | Cuando ocurre una falla técnica durante la revisión |

## 11. Observaciones

- Este caso de uso no aprueba automáticamente el pedido para producción.
- Este caso de uso no solicita corrección por sí solo; esa acción se documenta en `CU-REV-003-solicitar-correccion-de-pedido.md`.
- Este caso de uso no rechaza el pedido por sí solo; esa acción se documenta en `CU-REV-004-rechazar-pedido.md`.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no ejecuta impresión.
- Este caso de uso no registra cobros ni comprobantes.
- La revisión administrativa es obligatoria antes de producción.
- Los archivos del pedido deben consultarse mediante acceso autorizado.
- El cliente final no puede realizar revisión administrativa.
- La seguridad debe ser backend-first: RLS, RPC, políticas y validaciones de permisos.
- El resultado de la revisión debe servir para decidir el próximo paso del flujo.

## 12. Poscondición

Al finalizar correctamente:

- el pedido queda revisado por un usuario interno autorizado;
- la revisión queda registrada;
- las observaciones internas quedan asociadas al pedido si corresponde;
- el pedido no queda aprobado automáticamente para producción;
- no se genera ningún trabajo de impresión;
- no se modifica información financiera de forma directa;
- el sistema deja disponible el pedido para aprobar, solicitar corrección, rechazar o continuar análisis;
- los eventos relevantes quedan registrados para trazabilidad.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede revisar pedidos nuevos si tiene permisos.
- El sistema rechaza revisiones de usuarios no autorizados.
- El sistema solo permite revisar pedidos visibles para el usuario interno.
- El sistema muestra datos y archivos permitidos del pedido.
- El sistema registra la revisión administrativa.
- La revisión no autoriza producción automáticamente.
- La revisión no genera trabajos de impresión.
- La revisión puede derivar a aprobación, corrección, rechazo o análisis adicional.
- La revisión queda auditada con usuario, fecha y contexto.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.