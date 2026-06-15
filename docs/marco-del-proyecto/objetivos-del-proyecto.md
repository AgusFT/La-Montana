# Objetivos del proyecto - La Montaña

| Campo | Valor |
|---|---|
| Versión | 1.0 |
| Estado | Borrador inicial |
| Fecha | 2026-05-20 |
| Responsables | Agustín Tejero y Alejandro Herms |

## 1. Objetivo del documento

Este documento define el objetivo general y los objetivos específicos del sistema **La Montaña**.

Su finalidad es establecer con claridad qué busca lograr el sistema y qué resultados concretos deben alcanzarse para considerar que el producto cumple su propósito.

Los objetivos definidos en este documento toman como base el alcance general del proyecto documentado en `docs/marco-del-proyecto/alcance-general.md`.

---

## 2. Objetivo general

Desarrollar un sistema integral de gestión para una imprenta que permita centralizar, ordenar y controlar el flujo administrativo, operativo y productivo de los pedidos, desde su solicitud inicial hasta su revisión, producción, impresión, entrega, cobro, comprobante y cierre, utilizando Supabase como backend central, una aplicación Web para clientes, empleados y administradores, una aplicación Android conectada al mismo backend y un subsistema de impresión autorizado basado en Raspberry Pi, CUPS y un agente/gateway de impresión.

---

## 3. Objetivos específicos

### 3.1 Centralizar la gestión del pedido

Permitir que los pedidos sean registrados, consultados y gestionados desde una fuente única de verdad, evitando información dispersa, duplicada o inconsistente.

Este objetivo implica que el pedido tenga trazabilidad desde su creación hasta su cierre.

### 3.2 Garantizar revisión administrativa antes de producción

Asegurar que ningún pedido creado por un cliente avance automáticamente a producción.

Todo pedido nuevo debe quedar inicialmente pendiente de revisión y requerir una intervención administrativa antes de continuar el flujo operativo.

### 3.3 Gestionar usuarios, roles y permisos

Implementar una base inicial de roles y permisos que permita diferenciar las funciones disponibles para clientes, empleados y administradores.

El sistema debe mostrar vistas, acciones y datos según el rol y los permisos correspondientes.

### 3.4 Centralizar los archivos del pedido

Permitir la carga, asociación, consulta y uso autorizado de archivos vinculados a cada pedido.

Los archivos deben ser almacenados de forma centralizada y segura, evitando depender de rutas locales del cliente como mecanismo de impresión.

### 3.5 Separar los estados principales del pedido

Representar de forma diferenciada:

- el estado interno;
- el estado visible al cliente;
- el estado financiero.

Esta separación permite evitar inconsistencias entre la operación interna, la comunicación con el cliente y la situación de cobro.

### 3.6 Implementar reglas de negocio críticas

Incorporar las reglas centrales del negocio dentro del flujo del sistema.

Entre ellas:

- pedidos nuevos pendientes de revisión;
- revisión administrativa obligatoria;
- seña del 30% para pedidos mayores a 200 carillas;
- consistencia entre entrega, cobro, comprobante, auditoría y cierre;
- autorización previa antes de imprimir.

### 3.7 Desarrollar una aplicación Web multirol

Construir una aplicación Web que permita el acceso de clientes, empleados y administradores.

La Web debe permitir, según el rol:

- consultar información relevante;
- crear o gestionar pedidos;
- visualizar estados;
- trabajar con archivos;
- operar funciones administrativas;
- dar soporte al flujo principal del sistema.

### 3.8 Desarrollar una aplicación Android conectada al mismo backend

Construir una aplicación Android que consuma el mismo backend central utilizado por la Web.

La app debe respetar la misma lógica de autenticación, permisos, pedidos, archivos y estados definidos para el sistema.

### 3.9 Implementar backend central con Supabase

Utilizar Supabase como backend central del sistema, incluyendo:

- PostgreSQL;
- Auth;
- Storage;
- Realtime cuando corresponda;
- RPC para operaciones transaccionales;
- Edge Functions para lógica server-side específica.

El backend debe actuar como fuente única de verdad para Web, Android y subsistema de impresión.

### 3.10 Integrar un subsistema de impresión autorizado

Implementar un subsistema de impresión que permita ejecutar trabajos autorizados mediante Raspberry Pi, CUPS y un agente/gateway de impresión.

Este subsistema debe consultar o recibir trabajos autorizados, acceder a los archivos correspondientes de forma segura y reportar estados técnicos relevantes.

El subsistema de impresión no debe tomar decisiones de negocio.

### 3.11 Registrar trazabilidad y eventos relevantes

Mantener registro de eventos importantes del flujo del pedido, especialmente aquellos relacionados con revisión administrativa, cambios de estado, archivos, impresión, entrega, cobro, comprobantes y cierre.

La trazabilidad debe permitir entender qué ocurrió, cuándo ocurrió y bajo qué contexto operativo.

### 3.12 Producir documentación clara y mantenible

Generar documentación suficiente para explicar el producto, su alcance, sus decisiones principales, su arquitectura, su modelo de datos, sus requerimientos, sus flujos y sus criterios de validación.

La documentación debe acompañar al desarrollo y mantener coherencia con el código y el GitHub Project.

---

## 4. Clasificación de objetivos

| Tipo de objetivo | Objetivos relacionados |
|---|---|
| Funcionales | Gestión de pedidos, usuarios, archivos, estados, Web, Android e impresión |
| Operativos | Revisión administrativa, producción, entrega, cobro y cierre |
| Técnicos | Supabase, Web, Android, Raspberry Pi, CUPS, RPC, Edge Functions y Storage |
| Seguridad y control | Roles, permisos, acceso autorizado a archivos, trazabilidad y auditoría |
| Documentación | Alcance, requerimientos, planificación, arquitectura, modelo de datos y validación |

---

## 5. Relación con el alcance general

Los objetivos definidos en este documento se desprenden del alcance general del proyecto.

El alcance describe qué incluye y qué no incluye el sistema.

Los objetivos explican qué resultados se buscan lograr dentro de ese alcance.

Por lo tanto, cualquier objetivo nuevo que se agregue en el futuro deberá ser revisado contra el documento de alcance general para evitar contradicciones o crecimiento descontrolado del proyecto.

---

## 6. Criterios de cumplimiento

Los objetivos se considerarán cumplidos cuando el sistema logre:

- representar el flujo principal de pedidos de la imprenta;
- evitar el avance automático de pedidos a producción;
- permitir revisión administrativa;
- gestionar archivos asociados a pedidos;
- diferenciar estados internos, visibles al cliente y financieros;
- aplicar la regla de seña para pedidos mayores a 200 carillas;
- permitir acceso Web según rol;
- permitir acceso Android al mismo backend;
- centralizar datos y archivos en Supabase;
- integrar impresión autorizada mediante Raspberry Pi y CUPS;
- registrar eventos relevantes del flujo;
- mantener documentación coherente con la planificación y el desarrollo.

---
