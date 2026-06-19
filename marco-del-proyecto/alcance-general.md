# Alcance general del proyecto - La Montaña 

| Campo | Valor |
|---|---|
| Versión | 1.0 |
| Estado | Borrador inicial |
| Fecha | 2026-05-20 |
| Responsables | Agustín Tejero y Alejandro Herms |

## 1. Objetivo del documento

Este documento define el alcance general inicial del proyecto **La Montaña**.

Su objetivo es dejar claro:

- qué problema busca resolver el sistema;
- qué usuarios contempla;
- qué funcionalidades generales forman parte del proyecto;
- qué queda fuera del alcance actual;
- qué diferencia existe entre MVP, entrega final y funcionalidades futuras;
- qué reglas de negocio críticas condicionan el diseño del sistema.

Este documento forma parte de la documentación base del proyecto y será utilizado como referencia para requerimientos, historias de usuario, planificación, WBS/EDT, arquitectura y presentación y defensa del sistema.

---

## 2. Descripción general del proyecto

**La Montaña** es un sistema integral para la gestión administrativa, operativa y productiva de una imprenta.

El sistema busca centralizar y ordenar el flujo principal de trabajo de la imprenta, desde la solicitud inicial de un pedido hasta su revisión administrativa, producción, impresión, entrega, cobro, registro de comprobantes y cierre.

- trazabilidad;
- seguridad;
- mantenibilidad;
- claridad operativa;
- control de estados;
- documentación defendible;
- separación adecuada de responsabilidades.

---

## 3. Problema que busca resolver

La imprenta necesita gestionar pedidos, archivos, clientes, estados operativos, estados financieros, entregas y trabajos de impresión de forma ordenada y trazable.

Sin un sistema centralizado, pueden aparecer problemas como:

- pedidos poco claros o incompletos;
- archivos dispersos o difíciles de localizar;
- falta de seguimiento del estado real del pedido;
- confusión entre lo que ve el cliente y lo que gestiona internamente la imprenta;
- dificultad para saber si un pedido fue revisado, producido, entregado y cobrado;
- riesgo de enviar a producción pedidos que todavía no fueron validados;
- falta de auditoría sobre decisiones administrativas;
- dificultad para coordinar impresión física con archivos autorizados.

La Montaña busca resolver estos problemas mediante un sistema integrado con backend central, Web, Android y subsistema de impresión.

---

## 4. Usuarios principales

El sistema contempla inicialmente tres roles principales:

| Rol | Descripción |
|---|---|
| Cliente | Usuario que solicita pedidos, carga archivos y consulta el estado visible de sus trabajos |
| Empleado | Usuario interno que participa en revisión, producción, seguimiento, entrega o tareas operativas |
| Administrador | Usuario con permisos ampliados para gestionar pedidos, usuarios, estados, reglas, auditoría y configuración |

La Web será usada por clientes, empleados y administradores, mostrando funcionalidades según rol y permisos.

La app Android también consumirá el mismo backend y deberá respetar la misma lógica de roles y permisos.

---

## 5. Arquitectura general incluida en el alcance

La arquitectura confirmada del proyecto incluye:

- Supabase como fuente única de verdad.
- PostgreSQL como base de datos principal.
- Supabase Auth para autenticación.
- Supabase Storage para archivos del pedido.
- Supabase Realtime cuando sea útil para seguimiento de estados.
- RPC para operaciones transaccionales cercanas al modelo de datos.
- Edge Functions para lógica server-side que no convenga resolver como RPC puro.
- Web desarrollada con Next.js, React y TypeScript.
- App Android conectada al mismo backend.
- Subsistema de impresión con Raspberry Pi, CUPS, gateway/agente y print jobs.


---

## 6. Alcance funcional general

El sistema incluirá, de forma progresiva, los siguientes bloques funcionales:

### 6.1 Gestión de usuarios y roles

Incluye:

- autenticación de usuarios;
- roles iniciales: cliente, empleado y administrador;
- permisos según rol;
- visualización diferenciada de funciones según rol.

### 6.2 Gestión de pedidos

Incluye:

- creación de pedidos por parte del cliente;
- carga de datos básicos del pedido;
- carga de archivos asociados;
- revisión administrativa obligatoria;
- seguimiento de estados;
- gestión interna del avance del pedido.

### 6.3 Gestión de archivos

Incluye:

- carga de archivos por parte del cliente;
- almacenamiento centralizado en Supabase Storage;
- asociación entre archivos y pedidos;
- acceso autorizado a archivos;
- disponibilidad de archivos para revisión, producción e impresión.

No se usarán rutas locales del cliente como mecanismo de impresión.

### 6.4 Revisión administrativa

Incluye:

- revisión humana antes de avanzar a producción;
- validación de datos del pedido;
- validación de archivos;
- decisión administrativa sobre avance, corrección o rechazo;
- registro de cambios relevantes.

Ningún pedido creado por un cliente pasa automáticamente a producción.

### 6.5 Estados del pedido

El sistema debe distinguir:

- estado interno;
- estado visible al cliente;
- estado financiero.

Esta separación es central para evitar inconsistencias entre operación interna, comunicación al cliente y situación de cobro.

### 6.6 Gestión financiera básica

Incluye:

- identificación de pedidos que requieren seña;
- regla de seña del 30% para pedidos que superen 200 carillas;
- registro del estado financiero;
- registro de cobro;
- asociación con comprobantes cuando corresponda.

### 6.7 Producción e impresión

Incluye:

- preparación del pedido para producción;
- generación o consulta de trabajos de impresión autorizados;
- comunicación con subsistema de impresión;
- ejecución de impresión mediante Raspberry Pi y CUPS;
- registro del estado técnico del trabajo de impresión.

El subsistema de impresión no toma decisiones de negocio. Solo ejecuta trabajos autorizados por el backend.

### 6.8 Entrega y cierre del pedido

Incluye:

- registro de entrega;
- validación de cobro;
- registro o verificación de comprobante;
- auditoría mínima del cierre;
- actualización final de estados.

El cierre del pedido no depende solo de imprimir. Debe existir consistencia entre entrega, cobro, comprobante, auditoría y estado final.

---

## 7. Alcance del MVP

Para este proyecto, el MVP debe demostrar el flujo principal del sistema de forma funcional.

El MVP incluye:

- autenticación básica;
- roles iniciales;
- creación de pedido por cliente;
- carga de archivos;
- pedido en estado pendiente de revisión;
- revisión administrativa;
- avance controlado del pedido;
- separación inicial de estados;
- regla de seña para pedidos mayores a 200 carillas;
- visualización de pedidos en Web;
- app Android consumiendo el mismo backend;
- integración mínima con subsistema de impresión;
- trazabilidad básica de eventos relevantes.

El MVP no implica que todas las funcionalidades comerciales futuras estén completas, sino que el flujo central del negocio esté representado correctamente.

---

## 8. Alcance de la entrega final 

La entrega final deberá incluir más que un MVP mínimo.

Se espera entregar:

- documentación completa del proyecto;
- programa funcional;
- demo defendible;
- arquitectura documentada;
- requerimientos funcionales y no funcionales;
- historias de usuario;
- modelo de datos;
- diagramas en PlantUML;
- Web funcional;
- app Android funcional;
- backend en Supabase;
- flujo principal de pedidos;
- subsistema de impresión integrado de forma demostrable;
- documentación de pruebas;
- evidencia de planificación y seguimiento mediante GitHub Project.


---

## 9. Funcionalidades futuras o post-MVP

Quedan como posibles funcionalidades futuras, sujetas a revisión de alcance:

- inventario completo de insumos;
- facturación formal avanzada;
- reportes analíticos avanzados;
- automatizaciones administrativas complejas;
- paneles estadísticos avanzados;
- integración con medios de pago reales;
- notificaciones avanzadas;
- gestión avanzada de proveedores;
- optimización avanzada de colas de impresión.

Estas funcionalidades pueden documentarse, pero no deben comprometerse como núcleo obligatorio si exceden el tiempo y capacidad del equipo.

---

## 10. Fuera de alcance actual

Queda fuera del alcance actual:

- módulo Desktop;
- uso de Firebase como backend principal;
- impresión basada en rutas locales del cliente;
- paso automático de pedidos de cliente a producción;
- decisiones de negocio tomadas por Raspberry Pi, CUPS o agente de impresión;
- facturación fiscal real si no es definida explícitamente;
- integración con pagos reales si no se define como requerimiento obligatorio;
- soporte multi-sucursal avanzado;

---

## 11. Reglas de negocio críticas

Las reglas de negocio críticas conocidas hasta este momento son:

1. Ningún pedido creado por el cliente pasa automáticamente a producción.
2. Todo pedido nuevo queda en estado pendiente de revisión.
3. Debe existir mediación administrativa humana antes de avanzar a producción.
4. El sistema distingue estado interno, estado visible al cliente y estado financiero.
5. Si el pedido supera 200 carillas, requiere seña del 30%.
6. El cierre del pedido requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final.
7. Los archivos del pedido son parte central del flujo.
8. La Raspberry Pi/CUPS necesita acceso real y autorizado a los archivos.
9. No se usan rutas locales del cliente como mecanismo de impresión.
10. Web y Android consumen el mismo backend desde el MVP.
11. El subsistema de impresión no toma decisiones de negocio.

---

## 12. Criterios generales de éxito

El proyecto será considerado exitoso si logra:

- representar correctamente el flujo principal de una imprenta;
- permitir trazabilidad del pedido desde creación hasta cierre;
- evitar avances automáticos indebidos a producción;
- centralizar archivos y datos en Supabase;
- ofrecer Web y Android conectados al mismo backend;
- integrar un subsistema de impresión autorizado;
- documentar de forma clara las decisiones tomadas;
- mantener coherencia entre planificación, documentación, código y demo.

---
