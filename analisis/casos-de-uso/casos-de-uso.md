# Casos de uso - La Montaña

| Campo | Valor |
|---|---|
| Versión | 1.0 |
| Estado | Borrador inicial |
| Fecha | 2026-05-24 |
| Responsables | Agustín Tejero y Alejandro Herms |

## 1. Objetivo

Esta carpeta contiene los casos de uso del sistema **La Montaña**.

Los casos de uso describen flujos concretos de interacción entre actores y sistema. Su objetivo es transformar requerimientos e historias de usuario en procesos operativos detallados que puedan usarse luego como base para diseño de pantallas, modelo de datos, backend, validaciones, permisos, auditoría, pruebas y desarrollo.

## 2. Criterio de documentación

Los casos de uso deben documentarse de forma detallada y separada.

No se debe agrupar demasiada lógica en un único caso de uso genérico.

Cada caso de uso debe representar una intención operativa clara:

- qué actor interviene;
- qué quiere hacer;
- qué datos necesita;
- qué validaciones ocurren;
- qué permisos aplican;
- qué cambia en el sistema;
- qué eventos deberían auditarse;
- qué errores pueden ocurrir;
- qué estado final queda.

## 3. Documentos relacionados

Los casos de uso toman como base:

- `../../marco-del-proyecto/alcance-general.md`
- `../../marco-del-proyecto/objetivos-del-proyecto.md`
- `../../marco-del-proyecto/stakeholders-y-actores.md`
- `../espesificacion-de-requerimientos/requerimientos-funcionales.md`
- `../espesificacion-de-requerimientos/requerimientos-no-funcionales.md`
- `../historias-de-usuarios/historias-de-usuario.md`
- `../../marco-del-proyecto/matriz-trazabilidad.md`

## 4. Organización

Los casos de uso se organizan por dominio funcional:

| Carpeta | Dominio |
|---|---|
| `01-pedidos/` | Creación, consulta y gestión general de pedidos |
| `02-archivos/` | Carga, revisión, validación y acceso a archivos del pedido |
| `03-revision-administrativa/` | Revisión humana, aprobación, rechazo o solicitud de corrección |
| `04-estados-y-finanzas/` | Estados internos, estados visibles, estado financiero, seña, cobros y comprobantes |
| `05-impresion/` | Trabajos de impresión, agente/gateway, CUPS y ejecución autorizada |
| `06-usuarios-y-permisos/` | Autenticación, roles, permisos y control de acceso |
| `07-trazabilidad-y-cierre/` | Auditoría, entrega, comprobantes y cierre del pedido |
| `08-web-y-android/` | Flujos específicos de uso desde Web y Android |

## 5. Convención de nombres

Cada caso de uso debe tener un código único.

Formato:

    CU-AREA-NNN-nombre-descriptivo.md

Ejemplos:

    CU-PED-001-crear-pedido.md
    CU-ARC-001-cargar-archivo-al-pedido.md
    CU-REV-001-revisar-pedido-nuevo.md
    CU-FIN-001-detectar-pedido-con-senia-obligatoria.md
    CU-IMP-001-generar-trabajo-de-impresion-autorizado.md
    CU-CIE-001-cerrar-pedido.md

## 6. Códigos de área

| Código | Área |
|---|---|
| CU-PED | Pedidos |
| CU-ARC | Archivos |
| CU-REV | Revisión administrativa |
| CU-EST | Estados |
| CU-FIN | Finanzas |
| CU-IMP | Impresión |
| CU-AUT | Autenticación, usuarios y permisos |
| CU-AUD | Auditoría y trazabilidad |
| CU-CIE | Cierre del pedido |
| CU-WEB | Web |
| CU-AND | Android |

## 7. Catálogo inicial de casos de uso

El catálogo se completará progresivamente. La primera versión debe priorizar los flujos necesarios para entender y construir el MVP.

### 7.1 Pedidos

| Código | Caso de uso | Archivo |
|---|---|---|
| CU-PED-001 | Crear pedido | `01-pedidos/CU-PED-001-crear-pedido.md` |
| CU-PED-002 | Consultar pedido propio | `01-pedidos/CU-PED-002-consultar-pedido-propio.md` |
| CU-PED-003 | Consultar pedidos internos | `01-pedidos/CU-PED-003-consultar-pedidos-internos.md` |
| CU-PED-004 | Modificar pedido antes de producción | `01-pedidos/CU-PED-004-modificar-pedido-antes-de-produccion.md` |
| CU-PED-005 | Registrar observación interna | `01-pedidos/CU-PED-005-registrar-observacion-interna.md` |

### 7.2 Archivos

| Código | Caso de uso | Archivo |
|---|---|---|
| CU-ARC-001 | Cargar archivo al pedido | `02-archivos/CU-ARC-001-cargar-archivo-al-pedido.md` |
| CU-ARC-002 | Consultar archivos del pedido | `02-archivos/CU-ARC-002-consultar-archivos-del-pedido.md` |
| CU-ARC-003 | Validar archivo del pedido | `02-archivos/CU-ARC-003-validar-archivo-del-pedido.md` |
| CU-ARC-004 | Rechazar archivo del pedido | `02-archivos/CU-ARC-004-rechazar-archivo-del-pedido.md` |
| CU-ARC-005 | Habilitar archivo para impresión | `02-archivos/CU-ARC-005-habilitar-archivo-para-impresion.md` |

### 7.3 Revisión administrativa

| Código | Caso de uso | Archivo |
|---|---|---|
| CU-REV-001 | Revisar pedido nuevo | `03-revision-administrativa/CU-REV-001-revisar-pedido-nuevo.md` |
| CU-REV-002 | Aprobar pedido para producción | `03-revision-administrativa/CU-REV-002-aprobar-pedido-para-produccion.md` |
| CU-REV-003 | Solicitar corrección de pedido | `03-revision-administrativa/CU-REV-003-solicitar-correccion-de-pedido.md` |
| CU-REV-004 | Rechazar pedido | `03-revision-administrativa/CU-REV-004-rechazar-pedido.md` |

### 7.4 Estados y finanzas

| Código | Caso de uso | Archivo |
|---|---|---|
| CU-EST-001 | Consultar estado visible del pedido | `04-estados-y-finanzas/CU-EST-001-consultar-estado-visible-del-pedido.md` |
| CU-EST-002 | Actualizar estado interno del pedido | `04-estados-y-finanzas/CU-EST-002-actualizar-estado-interno-del-pedido.md` |
| CU-FIN-001 | Detectar pedido con seña obligatoria | `04-estados-y-finanzas/CU-FIN-001-detectar-pedido-con-senia-obligatoria.md` |
| CU-FIN-002 | Registrar seña | `04-estados-y-finanzas/CU-FIN-002-registrar-senia.md` |
| CU-FIN-003 | Registrar cobro final | `04-estados-y-finanzas/CU-FIN-003-registrar-cobro-final.md` |
| CU-FIN-004 | Registrar comprobante | `04-estados-y-finanzas/CU-FIN-004-registrar-comprobante.md` |

### 7.5 Impresión

| Código | Caso de uso | Archivo |
|---|---|---|
| CU-IMP-001 | Generar trabajo de impresión autorizado | `05-impresion/CU-IMP-001-generar-trabajo-de-impresion-autorizado.md` |
| CU-IMP-002 | Consultar trabajos pendientes por agente | `05-impresion/CU-IMP-002-consultar-trabajos-pendientes-por-agente.md` |
| CU-IMP-003 | Ejecutar trabajo de impresión | `05-impresion/CU-IMP-003-ejecutar-trabajo-de-impresion.md` |
| CU-IMP-004 | Reportar estado técnico de impresión | `05-impresion/CU-IMP-004-reportar-estado-tecnico-de-impresion.md` |
| CU-IMP-005 | Reportar error de impresión | `05-impresion/CU-IMP-005-reportar-error-de-impresion.md` |
| CU-IMP-006 | Cancelar trabajo de impresión | `05-impresion/CU-IMP-006-cancelar-trabajo-de-impresion.md` |

### 7.6 Usuarios y permisos

| Código | Caso de uso | Archivo |
|---|---|---|
| CU-AUT-001 | Iniciar sesión | `06-usuarios-y-permisos/CU-AUT-001-iniciar-sesion.md` |
| CU-AUT-002 | Cerrar sesión | `06-usuarios-y-permisos/CU-AUT-002-cerrar-sesion.md` |
| CU-AUT-003 | Consultar perfil propio | `06-usuarios-y-permisos/CU-AUT-003-consultar-perfil-propio.md` |
| CU-AUT-004 | Gestionar usuario interno | `06-usuarios-y-permisos/CU-AUT-004-gestionar-usuario-interno.md` |
| CU-AUT-005 | Asignar rol o permiso | `06-usuarios-y-permisos/CU-AUT-005-asignar-rol-o-permiso.md` |

### 7.7 Trazabilidad y cierre

| Código | Caso de uso | Archivo |
|---|---|---|
| CU-AUD-001 | Registrar evento crítico del pedido | `07-trazabilidad-y-cierre/CU-AUD-001-registrar-evento-critico-del-pedido.md` |
| CU-AUD-002 | Consultar historial del pedido | `07-trazabilidad-y-cierre/CU-AUD-002-consultar-historial-del-pedido.md` |
| CU-CIE-001 | Registrar entrega del pedido | `07-trazabilidad-y-cierre/CU-CIE-001-registrar-entrega-del-pedido.md` |
| CU-CIE-002 | Validar condiciones de cierre | `07-trazabilidad-y-cierre/CU-CIE-002-validar-condiciones-de-cierre.md` |
| CU-CIE-003 | Cerrar pedido | `07-trazabilidad-y-cierre/CU-CIE-003-cerrar-pedido.md` |

### 7.8 Web y Android

| Código | Caso de uso | Archivo |
|---|---|---|
| CU-WEB-001 | Visualizar dashboard según rol | `08-web-y-android/CU-WEB-001-visualizar-dashboard-segun-rol.md` |
| CU-WEB-002 | Gestionar pedidos desde panel administrativo | `08-web-y-android/CU-WEB-002-gestionar-pedidos-desde-panel-admin.md` |
| CU-AND-001 | Consultar pedidos desde Android | `08-web-y-android/CU-AND-001-consultar-pedidos-desde-android.md` |
| CU-AND-002 | Consultar detalle de pedido desde Android | `08-web-y-android/CU-AND-002-consultar-detalle-de-pedido-desde-android.md` |

## 8. Criterio de mantenimiento

Cuando se agregue, modifique o elimine un caso de uso:

- debe mantenerse su código único;
- debe respetar la plantilla definida;
- debe actualizarse este índice si corresponde;
- debe mantenerse trazabilidad con RF, RNF, HU y reglas críticas;
- debe revisarse si impacta en la matriz de trazabilidad.
