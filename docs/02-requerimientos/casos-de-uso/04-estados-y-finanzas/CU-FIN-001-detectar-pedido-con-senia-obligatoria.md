# CU-FIN-001 - Detectar pedido con seña obligatoria

| Campo                        | Valor                                                                                                      |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------- |
| ID                           | CU-FIN-001                                                                                                 |
| Caso de uso                  | Detectar pedido con seña obligatoria                                                                       |
| Área                         | Estados y finanzas                                                                                         |
| Actor principal              | Sistema                                                                                                    |
| Actores secundarios          | Cliente, Empleado o administrador                                                                          |
| Prioridad                    | P0 Crítica                                                                                                 |
| Alcance                      | MVP                                                                                                        |
| RF relacionados              | RF-FIN-001, RF-FIN-002, RF-EST-003, RF-EST-004, RF-EST-005, RF-PED-002, RF-PED-006, RF-AUD-001, RF-AUD-002 |
| RNF relacionados             | RNF-SEG-004, RNF-AUT-002, RNF-RLS-004, RNF-AUD-001, RNF-AUD-004, RNF-REN-004, RNF-USA-003                  |
| HU relacionadas              | HU-CLI-006, HU-SIS-003                                                                                     |
| Reglas críticas relacionadas | RFC-005, RFC-006, RNFC-001, RNFC-003, RNFC-007, RNFC-008                                                   |

## 1. Caso de Uso

Detectar pedido con seña obligatoria.

## 2. Actores

| Actor                    | Participación                                                                                 |
| ------------------------ | --------------------------------------------------------------------------------------------- |
| Sistema                  | Evalúa si el pedido supera 200 carillas y determina si requiere seña del 30%                  |
| Cliente                  | Recibe información visible cuando el pedido requiere seña, si corresponde                     |
| Empleado o administrador | Puede consultar la condición financiera del pedido y revisar casos que requieran intervención |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual el sistema detecta si un pedido requiere seña obligatoria.

La regla de negocio definida indica que si un pedido supera 200 carillas, requiere una seña del 30%.

La detección puede ocurrir cuando:

* el cliente crea el pedido;
* el cliente modifica datos que afectan cantidad o carillas;
* un empleado o administrador revisa el pedido;
* el sistema recalcula o valida información del pedido;
* se actualizan datos que impactan la cantidad de carillas.

Este caso de uso no registra el pago de la seña. Solo detecta la condición y actualiza la información financiera correspondiente para que el flujo posterior pueda solicitar, registrar o validar la seña.

La detección debe realizarse en backend, RPC, Edge Function o mecanismo equivalente. No debe depender únicamente del frontend.

## 4. Precondición

* El pedido existe.
* El pedido tiene información suficiente para calcular o estimar la cantidad de carillas.
* El backend Supabase está disponible.
* El sistema tiene definida la regla de negocio de seña obligatoria.
* El pedido no está cerrado.
* El sistema puede actualizar o marcar el estado financiero del pedido.
* El sistema puede registrar auditoría del resultado de evaluación.
* Las políticas de acceso protegen los datos financieros del pedido.

## 5. Datos de entrada

| Dato                          | Obligatorio | Descripción                                                                |
| ----------------------------- | ----------- | -------------------------------------------------------------------------- |
| ID del pedido                 | Sí          | Identificador del pedido a evaluar                                         |
| Cantidad de carillas          | Sí          | Cantidad total o estimada de carillas del pedido                           |
| Usuario o proceso solicitante | No          | Usuario interno, cliente o proceso del sistema que dispara la evaluación   |
| Estado financiero actual      | No          | Estado financiero vigente antes de la evaluación                           |
| Datos actualizados del pedido | No          | Información modificada que puede afectar la cantidad de carillas           |
| Canal de origen               | No          | Web, Android, revisión administrativa, RPC, Edge Function u otro mecanismo |

## 6. Datos de salida

| Dato                          | Descripción                                                                |
| ----------------------------- | -------------------------------------------------------------------------- |
| Resultado de evaluación       | Indica si el pedido requiere o no seña                                     |
| Porcentaje de seña requerido  | 30% cuando supera 200 carillas                                             |
| Estado financiero actualizado | Estado financiero marcado como requiere seña, seña pendiente o equivalente |
| Mensaje visible al cliente    | Indicación visible si corresponde informar que se requiere seña            |
| Evento de auditoría           | Registro de evaluación financiera                                          |
| Pedido evaluado               | Pedido con condición financiera actualizada o confirmada                   |

## 7. Permisos y seguridad

| Aspecto              | Regla                                                                                  |
| -------------------- | -------------------------------------------------------------------------------------- |
| Autenticación        | Puede ejecutarse por usuario autenticado o proceso interno autorizado                  |
| Autorización         | Solo usuarios o procesos autorizados pueden recalcular o modificar estado financiero   |
| RLS / acceso a datos | El estado financiero del pedido debe estar protegido por políticas de acceso           |
| Cliente final        | El cliente solo ve información financiera visible, no reglas internas ni auditoría     |
| Validación backend   | La regla de seña debe validarse en backend, RPC, Edge Function o mecanismo equivalente |
| Integridad           | El cálculo no debe dejar estados financieros contradictorios                           |
| Auditoría            | La evaluación y los cambios financieros relevantes deben quedar registrados            |

## 8. Flujo principal

| Paso | Flujo principal                                                                     | Flujo alternativo / excepciones                                                                           |
| ---- | ----------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| 1    | Se dispara la evaluación financiera del pedido.                                     | Si no existe pedido, el sistema rechaza la evaluación.                                                    |
| 2    | El sistema recupera los datos necesarios del pedido.                                | Si faltan datos para calcular carillas, marca el pedido como pendiente de revisión o evaluación.          |
| 3    | El sistema valida que el pedido pueda ser evaluado.                                 | Si el pedido está cerrado o fuera del flujo permitido, bloquea la evaluación.                             |
| 4    | El sistema calcula o toma la cantidad de carillas del pedido.                       | Si la cantidad es inválida o inconsistente, registra el problema y requiere corrección.                   |
| 5    | El sistema compara la cantidad de carillas contra el umbral de 200.                 | Si no supera 200 carillas, no marca seña obligatoria.                                                     |
| 6    | Si el pedido supera 200 carillas, el sistema determina que requiere seña del 30%.   | Si la regla no puede aplicarse por datos incompletos, el pedido queda pendiente de evaluación financiera. |
| 7    | El sistema actualiza el estado financiero correspondiente.                          | Si no puede actualizarlo consistentemente, revierte o bloquea la operación.                               |
| 8    | El sistema define la información visible para el cliente, si corresponde.           | Si no corresponde mostrar información todavía, mantiene la información interna.                           |
| 9    | El sistema registra evento de auditoría de la evaluación.                           | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente.                          |
| 10   | El sistema informa o deja disponible el resultado para el siguiente paso del flujo. | Si ocurre un error final, informa la falla y evita duplicar evaluaciones inconsistentes.                  |

## 9. Impacto en estados

| Estado                      | Impacto                                                                                         |
| --------------------------- | ----------------------------------------------------------------------------------------------- |
| Estado interno              | Sin cambios directo, salvo que el modelo defina una marca de pendiente de evaluación financiera |
| Estado visible al cliente   | Puede mostrar que el pedido requiere seña, si corresponde comunicarlo                           |
| Estado financiero           | Cambia a requiere seña, seña pendiente o equivalente si supera 200 carillas                     |
| Estado técnico de impresión | Sin cambios. No se genera ni modifica ningún trabajo de impresión                               |
| Estado de cierre            | No puede cerrarse un pedido con condición financiera inconsistente                              |

## 10. Eventos de auditoría

| Evento                           | Cuándo se registra                                                 |
| -------------------------------- | ------------------------------------------------------------------ |
| Evaluación de seña realizada     | Cuando el sistema evalúa si el pedido requiere seña                |
| Seña obligatoria detectada       | Cuando el pedido supera 200 carillas                               |
| Seña no requerida                | Cuando el pedido no supera el umbral definido                      |
| Evaluación financiera incompleta | Cuando faltan datos para aplicar la regla                          |
| Estado financiero actualizado    | Cuando el sistema modifica el estado financiero                    |
| Error de evaluación financiera   | Cuando ocurre una falla técnica durante el cálculo o actualización |

## 11. Observaciones

* Este caso de uso no registra el pago de la seña.
* Este caso de uso no confirma la seña.
* Este caso de uso no autoriza producción por sí solo.
* Este caso de uso no genera trabajos de impresión.
* Este caso de uso no ejecuta impresión.
* La regla de seña del 30% aplica cuando el pedido supera 200 carillas.
* Si la cantidad de carillas cambia luego de una modificación del pedido, debe reevaluarse esta regla.
* El estado financiero debe mantenerse separado del estado interno y del estado visible al cliente.
* El cliente solo debe ver información financiera visible y comprensible.
* La evaluación debe hacerse en backend, RPC, Edge Function o mecanismo equivalente.
* La seguridad no debe depender de cálculos o validaciones exclusivamente en frontend.
* Si la regla requiere parámetros configurables en el futuro, deberán documentarse en configuración del producto.

## 12. Poscondición

Al finalizar correctamente:

* el pedido queda evaluado respecto a la regla de seña;
* si supera 200 carillas, queda marcado como requiere seña del 30% o equivalente;
* si no supera 200 carillas, no se marca seña obligatoria;
* el estado financiero queda actualizado o confirmado;
* el cliente puede recibir información visible si corresponde;
* el evento queda auditado;
* no se genera ningún trabajo de impresión;
* no se autoriza producción por este caso de uso.

## 13. Criterios de aceptación

* El sistema detecta si un pedido supera 200 carillas.
* Si el pedido supera 200 carillas, el sistema marca que requiere seña del 30%.
* Si el pedido no supera 200 carillas, el sistema no marca seña obligatoria.
* Si faltan datos, el sistema no aplica la regla de forma inconsistente.
* El estado financiero se actualiza de forma separada del estado interno y visible.
* El cliente solo ve información financiera visible y autorizada.
* La evaluación queda registrada para trazabilidad.
* La evaluación no registra pagos ni confirma señas.
* La evaluación no genera trabajos de impresión.
* La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
* El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.

```
```
