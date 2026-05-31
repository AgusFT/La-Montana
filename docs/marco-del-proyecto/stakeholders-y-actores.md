# Stakeholders, actores y cliente piloto - La Montaña

| Campo | Valor |
|---|---|
| Versión | 1.0 |
| Estado | Borrador inicial |
| Fecha | 2026-05-20 |
| Responsables | Agustín Tejero y Alejandro Herms |

## 1. Objetivo del documento

Este documento identifica los stakeholders, actores principales y contexto del cliente piloto del sistema **La Montaña**.

Su objetivo es establecer quiénes intervienen en el producto, quiénes lo usan directamente, quiénes son afectados por su funcionamiento y qué necesidades deben contemplarse al definir requerimientos funcionales, requerimientos no funcionales e historias de usuario.

La identificación de stakeholders y actores permite que el sistema se diseñe con una base realista, evitando requerimientos genéricos o desconectados del flujo operativo de una imprenta.

---

## 2. Enfoque del producto

La Montaña se plantea como un sistema modular y escalable para la gestión administrativa, operativa y productiva de imprentas.

El producto debe poder adaptarse a distintos negocios de impresión, partiendo de una base común que contemple:

- gestión de pedidos;
- gestión de archivos;
- revisión administrativa;
- control de estados;
- seguimiento de producción;
- impresión autorizada;
- entrega;
- cobro;
- comprobantes;
- cierre del pedido;
- trazabilidad del flujo.

El primer caso de uso será validado con un cliente piloto de baja escala, pero el diseño debe evitar quedar limitado exclusivamente a ese caso particular.

---

## 3. Dueños del producto

Los dueños del producto son:

- Agustín Tejero;
- Alejandro Herms.

Su rol es definir, diseñar, desarrollar, documentar y evolucionar La Montaña como producto base para negocios de impresión.

Sus responsabilidades principales son:

- definir el alcance del producto;
- priorizar funcionalidades;
- mantener coherencia entre planificación, documentación y desarrollo;
- tomar decisiones técnicas y funcionales;
- validar el producto con el cliente piloto;
- proyectar una arquitectura adaptable a clientes futuros;
- asegurar que el sistema sea mantenible, seguro y escalable.

---

## 4. Cliente piloto

El cliente piloto es un emprendimiento de impresión de baja escala llamado "La Montaña" orientado principalmente a estudiantes universitarios.

Su operación actual se basa en:

- recepción de pedidos de impresión;
- intercambio de archivos digitales;
- impresión bajo demanda;
- uso de varias impresoras;
- coordinación manual con clientes;
- entregas en puntos previamente acordados;
- seguimiento informal de pedidos y cobros.

El cliente piloto permite validar el flujo principal del sistema en un contexto real, pero no representa necesariamente todos los casos futuros del producto.

Por ese motivo, el sistema debe contemplar sus necesidades actuales sin perder modularidad ni capacidad de adaptación a otras imprentas.

---

## 5. Clientes futuros

Los clientes futuros son negocios de impresión que podrían adoptar La Montaña como sistema de gestión.

Pueden variar en:

- tamaño del negocio;
- cantidad de impresoras;
- cantidad de empleados;
- volumen de pedidos;
- tipos de servicios ofrecidos;
- puntos de entrega;
- reglas comerciales;
- nivel de digitalización;
- necesidad de reportes;
- requerimientos de configuración.

El diseño del sistema debe permitir que la base común pueda adaptarse progresivamente a distintos contextos sin reescribir el producto desde cero.

---

## 6. Actores del sistema

Los actores son las personas, roles o componentes que interactúan directamente con el sistema.

### 6.1 Cliente final

Es la persona que solicita un pedido de impresión.

Puede:

- registrarse o iniciar sesión;
- crear pedidos;
- cargar archivos;
- consultar el estado visible de sus pedidos;
- recibir indicaciones sobre revisión, pago, producción o entrega;
- entregar información necesaria para completar el pedido.

El cliente final no puede enviar pedidos directamente a producción.

### 6.2 Empleado

Es un usuario interno del negocio de impresión.

Puede participar en tareas como:

- revisar pedidos;
- validar datos del pedido;
- revisar archivos;
- preparar trabajos para producción;
- consultar estados internos;
- registrar avances operativos;
- participar en entrega o control del pedido.

Sus permisos deben depender del rol asignado.

### 6.3 Administrador

Es un usuario interno con permisos ampliados.

Puede:

- gestionar usuarios;
- revisar y aprobar pedidos;
- modificar estados internos;
- controlar estados visibles al cliente;
- gestionar estados financieros;
- validar condiciones de seña;
- autorizar trabajos de impresión;
- revisar trazabilidad;
- acceder a información operativa y administrativa del sistema.

El administrador es clave porque el sistema requiere mediación humana antes de avanzar a producción.

### 6.4 Sistema Web

Es la interfaz principal del producto para clientes, empleados y administradores.

Debe mostrar información y acciones según rol y permisos.

La Web permite operar el flujo principal de pedidos, archivos, revisión, seguimiento, administración y cierre.

### 6.5 Aplicación Android

Es una aplicación conectada al mismo backend que la Web.

Debe consumir la misma fuente de verdad y respetar las mismas reglas de autenticación, permisos, pedidos, archivos y estados.

Su alcance inicial debe estar alineado con el MVP y con el flujo principal del sistema.

### 6.6 Backend Supabase

Es el backend central y fuente única de verdad del sistema.

Incluye:

- PostgreSQL;
- Auth;
- Storage;
- Realtime;
- RPC;
- Edge Functions.

Debe concentrar datos, archivos, reglas críticas, permisos y operaciones transaccionales del sistema.

### 6.7 Subsistema de impresión

Es el conjunto formado por Raspberry Pi, CUPS y el agente/gateway de impresión.

Interactúa con el sistema para ejecutar trabajos de impresión autorizados.

No toma decisiones de negocio.

Sus responsabilidades son:

- consultar o recibir print jobs autorizados;
- acceder de forma segura a los archivos correspondientes;
- enviar trabajos a CUPS;
- reportar estados técnicos;
- informar errores o cancelaciones cuando corresponda.

---

## 7. Stakeholders principales

Los stakeholders son personas, grupos o entidades que influyen en el producto o son afectados por su funcionamiento, aunque no todos interactúen directamente con el sistema.

| Stakeholder | Interés principal |
|---|---|
| Dueños del producto | Construir un sistema modular, mantenible y adaptable a imprentas |
| Cliente piloto | Ordenar pedidos, archivos, producción, entregas y cobros |
| Clientes finales | Solicitar pedidos y consultar estados de forma clara |
| Empleados de imprenta | Gestionar tareas operativas con menos desorden y mayor trazabilidad |
| Administradores de imprenta | Controlar flujo, estados, cobros, autorizaciones y cierre de pedidos |
| Clientes futuros | Adoptar una solución adaptable a su operación particular |
| Equipo técnico | Mantener una arquitectura segura, escalable y documentada |

---

## 8. Necesidades principales detectadas

A partir del contexto del producto y del cliente piloto, se identifican las siguientes necesidades generales:

- centralizar pedidos y archivos;
- evitar pérdida o dispersión de información;
- impedir que pedidos no revisados avancen a producción;
- diferenciar estados internos, visibles al cliente y financieros;
- registrar trazabilidad de cambios importantes;
- gestionar archivos de forma segura;
- permitir acceso Web y Android al mismo backend;
- autorizar trabajos de impresión antes de ejecutarlos;
- registrar entrega, cobro, comprobante y cierre;
- mantener una base adaptable a otros negocios de impresión.

---

## 9. Relación entre actores y permisos

La Montaña debe aplicar una separación clara entre actores y permisos.

No todos los usuarios pueden realizar las mismas acciones.

Como mínimo, el sistema debe contemplar:

| Actor | Acceso esperado |
|---|---|
| Cliente final | Crear pedidos, cargar archivos y consultar estados visibles |
| Empleado | Revisar, operar y actualizar información según permisos internos |
| Administrador | Gestionar pedidos, usuarios, estados, reglas, autorizaciones y trazabilidad |
| Agente de impresión | Acceder solo a trabajos y archivos autorizados |
| Backend | Validar reglas, permisos, estados y operaciones críticas |

Esta separación permite reducir errores operativos, proteger datos y evitar acciones no autorizadas.

---

## 10. Consideraciones para requerimientos

Los requerimientos funcionales y no funcionales deberán contemplar que La Montaña no es solo una solución puntual para un único negocio.

El sistema debe partir del cliente piloto como caso real de validación, pero mantenerse preparado para evolucionar hacia un producto configurable para otros clientes.

Esto implica que los requerimientos deben cuidar especialmente:

- modularidad;
- trazabilidad;
- seguridad;
- control de permisos;
- gestión centralizada de archivos;
- separación de responsabilidades;
- claridad de estados;
- posibilidad de adaptación futura;
- consistencia entre Web, Android y backend.

---

## 11. Criterios de aceptación del documento

Este documento se considera completo cuando:

- identifica a los dueños del producto;
- describe el cliente piloto sin exponer datos personales innecesarios;
- diferencia cliente piloto de clientes futuros;
- identifica actores directos del sistema;
- identifica stakeholders principales;
- explica necesidades generales del contexto;
- sirve como base para redactar requerimientos e historias de usuario.