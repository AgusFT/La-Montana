# WBS/EDT V4 - La Montana

| Campo | Valor |
|---|---|
| Version | 1.0 |
| Estado | Creada en Lucidchart |
| Fecha | 2026-08-12 |
| Herramienta editable | Lucidchart |
| Documento Lucid | La Montana - WBS EDT V4 - Cascada con flechas Spring Boot PostgreSQL |
| ID Lucid | bd1c04ca-5321-4023-ae84-273b249efed8 |
| Link de edicion | https://lucid.app/lucidchart/bd1c04ca-5321-4023-ae84-273b249efed8/edit?invitationId=inv_e5bc76ee-e311-4a4d-943d-a77d94665d48 |
| Link de vista | https://lucid.app/lucidchart/bd1c04ca-5321-4023-ae84-273b249efed8/edit?invitationId=inv_7af93128-f689-4903-a6f1-14993e6769a0 |
| Artefacto SVG | WBS V4.svg |
| Issue relacionada | #150 |
| Epica padre | #140 |
| Milestone | M9 - Replanteo arquitectonico y deprecacion |

## Criterio

Esta WBS/EDT V4 mantiene la forma de cascada del WBS V3 original: un nodo central superior `Sistema de Imprenta` y cinco ramas principales del mismo nivel, con flechas entre cada nodo padre y sus elementos hijos.

La actualizacion conserva los componentes documentales y de negocio vigentes. Los nodos tecnicos asociados a la arquitectura anterior fueron reemplazados por equivalentes Spring Boot/PostgreSQL/API REST, permisos backend y almacenamiento de archivos gestionado por backend.

## Estructura WBS/EDT V4

```text
Sistema de Imprenta
├── Marco del Proyecto
│   ├── Alcance del Proyecto
│   ├── Guia de uso de github-projects
│   ├── Identificacion y Analisis de Stakeholders
│   ├── Documento MVP
│   ├── Matriz de trazabilidad
│   └── Diagrama WBS V4
├── Analisis
│   ├── Informe de Relevamiento
│   │   ├── Informe clientes internos (ventas / diseno / produccion)
│   │   ├── Informe del proceso actual
│   │   └── Listado de problemas y oportunidades
│   ├── Especificacion de Requerimientos
│   │   ├── Documento de requerimientos funcionales
│   │   ├── Documento de requerimientos no funcionales
│   │   └── Matriz de reglas de negocio
│   ├── Modelos de Sistema
│   │   ├── Modelo de estados: interno / visible al cliente / financiero
│   │   ├── Modelo del flujo completo del pedido
│   │   └── Modelo de archivos asociados (upload, validacion, uso en impresion)
│   ├── Casos de Uso
│   │   └── Catalogo de casos de uso
│   └── Historias de usuarios
│       └── Catalogo de historias de usuarios
├── Diseno
│   ├── Front
│   │   └── Diseno UX/UI
│   │       ├── documentacion
│   │       ├── wireflows
│   │       ├── Vistas Web (mockups): Cliente / Empleado / Administrador
│   │       └── Vistas Android (mockups)
│   └── Back
│       ├── Arquitectura del Sistema
│       │   ├── Diagramas (Arquitectura, Componentes, Despliegue)
│       │   ├── Documento de arquitectura
│       │   └── Especificacion de roles y permisos (RBAC)
│       ├── Diseno de Datos
│       │   ├── Modelo entidad-relacion (PostgreSQL / Spring Boot)
│       │   ├── Definicion de tablas y permisos backend
│       │   └── Estructura de archivos y acceso seguro
│       └── Diseno de Subsistemas
│           ├── Subsistema Web
│           ├── Subsistema Android
│           ├── Subsistema Backend (Spring Boot)
│           └── Subsistema de Impresion (Raspberry, CUPS, agente)
├── Desarrollo
│   ├── Backend - Spring Boot
│   │   ├── README backend-springboot
│   │   ├── Configuracion de variables de entorno
│   │   └── Configuracion del proyecto Spring Boot
│   ├── spring-boot
│   │   ├── Configuracion del proyecto Spring Boot
│   │   └── Datos iniciales de desarrollo
│   ├── migrations
│   │   ├── Spring Security / perfiles / roles
│   │   ├── Base de datos PostgreSQL
│   │   ├── Archivos y almacenamiento backend
│   │   ├── Permisos backend y reglas de acceso
│   │   ├── API REST definida
│   │   ├── Auditoria de eventos criticos
│   │   ├── Acceso autorizado a archivos
│   │   └── Flujo de revision administrativa
│   ├── services / jobs
│   │   └── Servicios backend implementados
│   ├── tests
│   │   └── database
│   │       └── pruebas, api, bdd, roles, unitarias, funcionales, otras
│   ├── Desarrollo Web (Entregables)
│   │   ├── Frontend cliente implementado
│   │   ├── Frontend empleado implementado
│   │   ├── Panel administrativo implementado
│   │   └── Integracion con backend implementada
│   ├── Desarrollo Android
│   │   ├── App Android Empleado / Administrador
│   │   ├── Autenticacion Android
│   │   ├── Visualizacion de pedidos y estados
│   │   └── Integracion con API REST backend
│   ├── Subsistema de Impresion
│   │   ├── Raspberry Pi configurada
│   │   ├── CUPS configurado
│   │   ├── Agente/gateway implementado
│   │   ├── Cola de impresion integrada
│   │   ├── Gestion de errores y cancelaciones
│   │   └── Acceso seguro a archivos de pedido
│   └── Modulos Extra (marcados como post-MVP)
│       ├── Inventario
│       ├── Facturacion
│       ├── Reportes avanzados
│       └── Estado financiero, sena y comprobantes implementados
└── Validacion y Lanzamiento
    ├── Plan y Resultados de Pruebas
    │   ├── Pruebas unitarias
    │   ├── Pruebas integrales
    │   ├── Validacion de reglas de negocio
    │   ├── Pruebas de flujo completo del pedido
    │   └── Pruebas de impresion
    ├── Documentacion Final
    │   ├── Manual administrativo
    │   ├── Documentacion tecnica
    │   ├── Guia de operacion del subsistema de impresion
    │   └── Manual de usuario
    └── Puesta en Produccion
        ├── Entorno productivo configurado
        ├── Publicacion Web
        ├── Publicacion App Android (beta)
        ├── Sistema puesto en marcha
        └── Informe de cierre y auditoria
```

## Validacion del alcance

- Existe un documento Lucidchart editable para la WBS/EDT V4.
- Existe un artefacto SVG versionado para visualizar el diagrama directamente en el repo.
- La forma general replica la cascada del WBS V3.
- Las flechas conectan el nodo principal, las cinco categorias y los elementos internos de cada columna.
- Se conservan las ramas documentales y funcionales vigentes.
- Los componentes del backend se expresan con Spring Boot, PostgreSQL, API REST, permisos backend y gestion de archivos desde backend.
