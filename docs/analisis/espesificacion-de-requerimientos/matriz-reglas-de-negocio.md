# Matriz de reglas de negocio

## 1. Información del documento

| Campo                | Valor                                              |
| -------------------- | -------------------------------------------------- |
| Documento            | Matriz de reglas de negocio                        |
| Proyecto             | La Montaña                                         |
| Épica relacionada    | E02 - Alcance, requerimientos y planificación base |
| Estado               | Borrador inicial                                   |
| Alcance              | MVP                                                |
| Última actualización | Pendiente de completar                             |
| Responsable          | Equipo del proyecto                                |

## 2. Objetivo

Este documento define la matriz de reglas de negocio del sistema La Montaña.

La matriz de reglas de negocio tiene como objetivo registrar, ordenar y hacer trazables las reglas operativas que gobiernan el comportamiento del sistema. Estas reglas determinan qué acciones están permitidas, bajo qué condiciones, quién puede ejecutarlas, qué estados se ven afectados y qué validaciones debe aplicar el sistema.

A diferencia de la matriz de trazabilidad, que demuestra la cobertura entre requerimientos, historias de usuario y casos de uso, esta matriz documenta las reglas propias del negocio de la imprenta.

## 3. Alcance de la matriz

Esta matriz cubre las reglas de negocio principales del MVP, especialmente las relacionadas con:

* gestión de pedidos;
* revisión administrativa;
* archivos asociados al pedido;
* estados internos, visibles y financieros;
* cotización;
* seña;
* producción;
* impresión;
* entrega;
* cobro;
* comprobantes;
* auditoría;
* cierre del pedido;
* roles y permisos base.

## 4. Criterios de lectura

Cada regla se identifica mediante un código único con el siguiente formato:

```text
RN-AREA-000
```

Donde:

* `RN` significa regla de negocio;
* `AREA` identifica el módulo o dominio funcional;
* `000` identifica el número secuencial de la regla.

Áreas iniciales utilizadas:

| Código | Área                     |
| ------ | ------------------------ |
| PED    | Pedidos                  |
| EST    | Estados                  |
| FIN    | Finanzas                 |
| ARC    | Archivos                 |
| IMP    | Impresión                |
| CIE    | Cierre del pedido        |
| AUT    | Autenticación y permisos |
| AUD    | Auditoría                |
| CAN    | Canales del sistema      |

## 5. Matriz de reglas de negocio

| ID         | Área                     | Regla de negocio                                                                                                                       | Condición de aplicación                                                              | Resultado esperado                                                                                       | Actor responsable        | Estados impactados                                | Implementación sugerida                                               | Prioridad  | Riesgo si se viola                                                          |
| ---------- | ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------- | ------------------------ | ------------------------------------------------- | --------------------------------------------------------------------- | ---------- | --------------------------------------------------------------------------- |
| RN-PED-001 | Pedidos                  | Ningún pedido creado por un cliente pasa automáticamente a producción.                                                                 | Un cliente crea un nuevo pedido.                                                     | El pedido queda registrado en estado interno `pendiente_de_revision`.                                    | Sistema                  | Estado interno                                    | RPC transaccional de creación de pedido + validación en base de datos | P0 Crítica | Un pedido podría producirse sin control administrativo.                     |
| RN-PED-002 | Pedidos                  | Todo pedido nuevo requiere revisión administrativa humana antes de avanzar hacia producción.                                           | Pedido en estado `pendiente_de_revision`.                                            | Un empleado o administrador debe revisar el pedido antes de permitir cualquier avance productivo.        | Empleado / Administrador | Estado interno, estado visible                    | RPC de revisión administrativa + auditoría de transición              | P0 Crítica | Se elimina la mediación humana obligatoria del flujo operativo.             |
| RN-PED-003 | Pedidos                  | Un pedido observado no debe avanzar hasta que la observación sea resuelta.                                                             | El empleado o administrador detecta datos incompletos, errores o archivos inválidos. | El pedido queda en un estado de revisión u observación y el cliente debe corregirlo o completarlo.       | Empleado / Administrador | Estado interno, estado visible                    | RPC de observación de pedido + registro de motivo                     | P1 Alta    | El pedido podría cotizarse o producirse con información incorrecta.         |
| RN-PED-004 | Pedidos                  | La modificación de un pedido debe respetar el estado actual y los permisos del usuario.                                                | Cliente, empleado o administrador intenta modificar un pedido existente.             | El sistema permite o bloquea la modificación según estado, rol y reglas del flujo.                       | Sistema                  | Estado interno, estado financiero                 | RPC de modificación controlada + RLS                                  | P1 Alta    | Un usuario podría alterar pedidos fuera de tiempo o sin autorización.       |
| RN-PED-005 | Pedidos                  | Si una modificación afecta cantidades, carillas, archivos o características productivas, puede requerir nueva revisión o recotización. | Se modifica un pedido ya revisado, cotizado o pendiente de confirmación.             | El sistema debe invalidar o recalcular información dependiente si corresponde.                           | Sistema / Administrador  | Estado interno, estado financiero                 | RPC transaccional con validación de impacto                           | P1 Alta    | El sistema podría mantener una cotización inválida o una seña incorrecta.   |
| RN-EST-001 | Estados                  | El sistema debe distinguir entre estado interno, estado visible al cliente y estado financiero.                                        | Cualquier operación que consulte o modifique un pedido.                              | Cada tipo de estado se mantiene separado y cumple una función específica.                                | Sistema                  | Estado interno, estado visible, estado financiero | Modelo de datos separado + constraints + RPC                          | P0 Crítica | Se mezclaría información operativa, comercial y financiera.                 |
| RN-EST-002 | Estados                  | El cliente solo debe visualizar estados adecuados para su rol.                                                                         | Cliente consulta el detalle o listado de pedidos.                                    | El sistema muestra el estado visible al cliente, no necesariamente el estado operativo interno completo. | Sistema                  | Estado visible                                    | Vistas, RPC de consulta según rol o políticas RLS                     | P1 Alta    | El cliente podría ver información interna innecesaria o confusa.            |
| RN-EST-003 | Estados                  | Toda transición crítica de estado debe ser validada por backend.                                                                       | Se intenta cambiar el estado de un pedido.                                           | El cambio solo se aplica si la transición es válida según el flujo definido.                             | Sistema                  | Estado interno, visible y financiero              | RPC transaccional + tabla de historial de estados                     | P0 Crítica | El frontend podría forzar estados inválidos.                                |
| RN-FIN-001 | Finanzas                 | Si un pedido supera las 200 carillas, requiere una seña del 30%.                                                                       | El total de carillas del pedido es mayor a 200.                                      | El estado financiero debe reflejar que existe una seña pendiente.                                        | Sistema                  | Estado financiero                                 | RPC de cálculo financiero + validación de umbral                      | P0 Crítica | El sistema podría habilitar producción sin seña obligatoria.                |
| RN-FIN-002 | Finanzas                 | Un pedido con seña obligatoria pendiente no puede quedar habilitado para producción.                                                   | Pedido con más de 200 carillas y seña no confirmada.                                 | El sistema bloquea el avance a producción.                                                               | Sistema / Administrador  | Estado interno, estado financiero                 | RPC de habilitación para producción                                   | P0 Crítica | Se producirían pedidos grandes sin respaldo financiero mínimo.              |
| RN-FIN-003 | Finanzas                 | El registro de pagos debe impactar el estado financiero del pedido.                                                                    | Se confirma una seña o cobro final.                                                  | El estado financiero se actualiza de forma consistente.                                                  | Empleado / Administrador | Estado financiero                                 | RPC de registro de pago + auditoría                                   | P1 Alta    | El pedido podría mostrar deuda o saldo incorrecto.                          |
| RN-FIN-004 | Finanzas                 | El cobro final debe estar registrado antes del cierre definitivo del pedido.                                                           | Pedido listo para cerrar.                                                            | El sistema valida que la situación financiera esté completa o en estado aceptado por administración.     | Administrador            | Estado financiero, estado interno                 | RPC de cierre de pedido                                               | P0 Crítica | Se podrían cerrar pedidos sin control de cobro.                             |
| RN-ARC-001 | Archivos                 | Los archivos del pedido son parte central del flujo operativo.                                                                         | Cliente crea o completa un pedido.                                                   | Los archivos deben asociarse formalmente al pedido.                                                      | Cliente / Sistema        | Estado interno                                    | Supabase Storage + tabla de archivos asociados                        | P0 Crítica | El pedido no podría revisarse ni imprimirse correctamente.                  |
| RN-ARC-002 | Archivos                 | No se deben usar rutas locales del cliente como mecanismo de impresión.                                                                | Se requiere acceder a archivos para revisión o impresión.                            | El sistema debe usar archivos almacenados y autorizados desde el backend.                                | Sistema                  | N/A                                               | Supabase Storage + signed URLs o mecanismo autorizado equivalente     | P0 Crítica | La Raspberry no tendría acceso real, seguro ni reproducible a los archivos. |
| RN-ARC-003 | Archivos                 | Todo archivo asociado a un pedido debe respetar ownership y permisos de acceso.                                                        | Un usuario intenta subir, consultar o descargar un archivo.                          | El acceso se permite solo si el usuario tiene autorización sobre el pedido.                              | Sistema                  | N/A                                               | Storage policies + RLS + validaciones backend                         | P0 Crítica | Un usuario podría acceder a archivos de pedidos ajenos.                     |
| RN-ARC-004 | Archivos                 | Los archivos deben ser revisables antes de avanzar a producción.                                                                       | Pedido pendiente de revisión o revisión administrativa.                              | Empleado o administrador puede validar si los archivos son correctos para cotizar o producir.            | Empleado / Administrador | Estado interno                                    | Vista interna o RPC de detalle de pedido con archivos                 | P1 Alta    | Se podrían cotizar o imprimir archivos incorrectos.                         |
| RN-IMP-001 | Impresión                | El subsistema de impresión no toma decisiones de negocio.                                                                              | Raspberry, CUPS o gateway reciben un trabajo de impresión.                           | El subsistema solo ejecuta trabajos previamente autorizados por el backend.                              | Gateway de impresión     | Estado técnico de impresión                       | Print jobs autorizados + Edge Function o RPC de generación            | P0 Crítica | La impresión podría saltarse reglas comerciales o administrativas.          |
| RN-IMP-002 | Impresión                | Solo se deben generar print jobs para pedidos habilitados para producción.                                                             | Se solicita imprimir un pedido.                                                      | El backend valida que el pedido esté en estado productivo válido.                                        | Sistema                  | Estado interno, estado técnico de impresión       | RPC o Edge Function de generación de print job                        | P0 Crítica | Se podrían imprimir pedidos no aprobados.                                   |
| RN-IMP-003 | Impresión                | La Raspberry/CUPS debe acceder a los archivos mediante un mecanismo real y autorizado.                                                 | Gateway necesita descargar o leer archivos para imprimir.                            | El backend entrega acceso temporal, seguro y controlado.                                                 | Sistema / Gateway        | N/A                                               | Signed URLs o mecanismo equivalente controlado por backend            | P0 Crítica | Fallaría la impresión o se expondrían archivos sin control.                 |
| RN-IMP-004 | Impresión                | El estado técnico del print job no equivale automáticamente al cierre del pedido.                                                      | Un print job finaliza correctamente.                                                 | El pedido puede avanzar a control de calidad, pero no cerrarse automáticamente.                          | Sistema                  | Estado interno, estado técnico                    | RPC de actualización de print job + transición controlada             | P1 Alta    | Se cerrarían pedidos sin entrega, cobro ni comprobante.                     |
| RN-CIE-001 | Cierre del pedido        | El cierre del pedido no depende solo de imprimir.                                                                                      | Pedido impreso o listo para entregar.                                                | Debe existir consistencia entre entrega, cobro, comprobante, auditoría y estado final.                   | Administrador / Sistema  | Estado interno, estado visible, estado financiero | RPC transaccional de cierre                                           | P0 Crítica | Se cerrarían pedidos incompletos o inconsistentes.                          |
| RN-CIE-002 | Cierre del pedido        | La entrega debe registrarse antes o durante el proceso de cierre.                                                                      | Pedido listo para entregar.                                                          | El sistema registra la entrega y su responsable.                                                         | Empleado / Administrador | Estado interno, estado visible                    | RPC de registro de entrega + auditoría                                | P1 Alta    | No habría evidencia operativa de entrega.                                   |
| RN-CIE-003 | Cierre del pedido        | Todo pedido cerrado debe tener comprobante asociado cuando corresponda.                                                                | Pedido con cobro final registrado y cierre operativo.                                | El comprobante queda asociado documentalmente al pedido.                                                 | Administrador / Sistema  | Estado financiero, estado interno                 | Storage + tabla de comprobantes + RPC de cierre                       | P1 Alta    | El cierre carecería de respaldo documental.                                 |
| RN-AUT-001 | Autenticación y permisos | Las funciones disponibles dependen del rol del usuario.                                                                                | Usuario autenticado accede al sistema.                                               | Cliente, empleado y administrador ven y ejecutan acciones según permisos.                                | Sistema                  | N/A                                               | Supabase Auth + perfiles + RLS                                        | P0 Crítica | Usuarios podrían acceder a funciones no autorizadas.                        |
| RN-AUT-002 | Autenticación y permisos | La seguridad y el ownership no deben depender solo del frontend.                                                                       | Cualquier operación sobre pedidos, archivos, pagos o estados.                        | El backend valida permisos y propiedad de los datos.                                                     | Sistema                  | N/A                                               | RLS + RPC con validación de usuario autenticado                       | P0 Crítica | Un cliente podría manipular datos ajenos mediante llamadas directas.        |
| RN-AUT-003 | Autenticación y permisos | Los administradores tienen mayor capacidad operativa que empleados y clientes.                                                         | Usuario administrador opera el sistema.                                              | El sistema permite acciones administrativas restringidas.                                                | Administrador            | Según operación                                   | RLS + permisos por rol                                                | P1 Alta    | No habría diferenciación real entre operación y administración.             |
| RN-AUD-001 | Auditoría                | Toda operación crítica debe dejar evidencia auditable.                                                                                 | Cambio de estado, revisión, pago, entrega, generación de print job o cierre.         | Se registra quién realizó la acción, cuándo y qué cambió.                                                | Sistema                  | Historial de estados / auditoría                  | Tabla de auditoría + RPC transaccional                                | P0 Crítica | No se podría reconstruir el historial del pedido.                           |
| RN-AUD-002 | Auditoría                | El historial de estados debe conservar las transiciones relevantes del pedido.                                                         | El pedido cambia de estado interno, visible o financiero.                            | Se registra la transición anterior y nueva.                                                              | Sistema                  | Estado interno, visible y financiero              | Tabla de historial de estados                                         | P1 Alta    | No habría trazabilidad operativa del flujo.                                 |
| RN-CAN-001 | Canales del sistema      | Web y Android deben consumir el mismo backend desde el MVP.                                                                            | Usuario opera desde Web o Android.                                                   | Ambos canales usan Supabase como fuente única de verdad.                                                 | Sistema                  | N/A                                               | Supabase Auth, PostgreSQL, Storage, RPC, Realtime y Edge Functions    | P0 Crítica | Se duplicaría lógica o se generarían inconsistencias entre canales.         |
| RN-CAN-002 | Canales del sistema      | La web debe adaptar dashboard y funciones según rol.                                                                                   | Usuario ingresa a la aplicación web.                                                 | Cliente, empleado y administrador ven vistas y acciones diferenciadas.                                   | Sistema                  | N/A                                               | Frontend con control de rol + backend con RLS/RPC                     | P1 Alta    | Usuarios verían pantallas o acciones incorrectas.                           |

## 6. Relación con otros documentos

Esta matriz debe mantenerse alineada con:

* requerimientos funcionales;
* requerimientos no funcionales;
* historias de usuario;
* casos de uso;
* matriz de trazabilidad;
* modelo de datos;
* diseño de estados;
* diseño de permisos;
* diseño del subsistema de impresión;
* documentación de Web y Android.

## 7. Criterio de mantenimiento

Cada vez que se agregue, modifique o elimine un requerimiento, historia de usuario, caso de uso o regla crítica del flujo de pedidos, debe revisarse esta matriz.

Una regla de negocio no debe considerarse válida para implementación si contradice:

* la mediación administrativa obligatoria antes de producción;
* la separación entre estado interno, estado visible y estado financiero;
* la regla de seña del 30% para pedidos de más de 200 carillas;
* el acceso seguro a archivos mediante backend;
* la ejecución de impresión solo mediante trabajos autorizados;
* el cierre consistente con entrega, cobro, comprobante, auditoría y estado final.

## 8. Estado del documento

Este documento representa la versión inicial de la matriz de reglas de negocio del MVP.

Las referencias exactas a RF, RNF, HU y CU deberán completarse o ajustarse al cruzar esta matriz con la matriz de trazabilidad consolidada.
