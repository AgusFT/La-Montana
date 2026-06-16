# Matriz de versionado - La Montaña

| Campo | Valor |
|---|---|
| Documento | Matriz de versionado |
| Versión | 1.0 |
| Estado | Línea base Parcial 2 |
| Fecha | 2026-06-16 |
| Responsables | Agustín Tejero y Alejandro Herms |

## 1. Objetivo del documento

Este documento define el criterio de versionado explícito para los archivos del repositorio **La Montaña**.

Su objetivo es establecer una línea base común para:

- identificar la versión actual del proyecto;
- unificar el formato de encabezado de los documentos y archivos propios;
- registrar qué artefactos forman parte de la versión inicial;
- dejar una regla clara para incrementar versiones cuando cambien documentos, código, diseños, configuraciones o entregables;
- aportar evidencia documental para el parcial y para el seguimiento en GitHub Projects.

---

## 2. Versión global del proyecto

| Campo | Valor |
|---|---|
| Versión global inicial | 1.0 |
| Nombre de línea base | Línea base Parcial 2 |
| Fecha de establecimiento | 2026-06-16 |
| Repositorio | La Montaña |
| Sistema de versionado | Git, GitHub, GitHub Projects e issues |
| Criterio general | Todo artefacto propio debe declarar versión o quedar registrado en esta matriz |

La versión global `1.0` representa el punto de inicio formal para el control de versiones del repositorio.

---

## 3. Regla de numeración

| Tipo de cambio | Regla | Ejemplo |
|---|---|---|
| Línea base inicial | Se establece en `1.0` | Primer registro formal del parcial |
| Cambio menor de contenido | Incrementa el segundo número | `1.0` a `1.1` |
| Corrección menor sin cambio conceptual | Incrementa el tercer número cuando se necesite más detalle | `1.0` a `1.0.1` |
| Cambio mayor de alcance, arquitectura o criterio | Incrementa el primer número | `1.0` a `2.0` |
| Archivo nuevo propio | Inicia en `1.0` | Nuevo caso de uso, vista, componente o documento |
| Archivo reemplazado | El archivo anterior queda como reemplazado u obsoleto en la matriz | WBS anterior reemplazada por WBS V2 |

Para el parcial se prioriza el formato `1.0`, `1.1`, `1.2`. El tercer nivel se reserva para correcciones puntuales si hace falta justificar cambios finos.

---

## 4. Estados permitidos

| Estado | Uso |
|---|---|
| Borrador inicial | Artefacto creado pero todavía sujeto a revisión |
| Línea base Parcial 2 | Artefacto incluido en la versión formal inicial del parcial |
| En revisión | Artefacto modificado y pendiente de validación |
| Actualizado | Artefacto vigente con cambios incorporados |
| Reemplazado | Artefacto sustituido por una versión posterior o alternativa |
| Obsoleto | Artefacto que se conserva por trazabilidad pero no guía el proyecto actual |

---

## 5. Formato de encabezado por tipo de archivo

### 5.1 Documentos Markdown

Los documentos `.md` deben usar una tabla inicial homogénea:

```md
| Campo | Valor |
|---|---|
| Documento | Nombre del documento |
| Versión | 1.0 |
| Estado | Línea base Parcial 2 |
| Fecha | 2026-06-16 |
| Responsables | Agustín Tejero y Alejandro Herms |
```

### 5.2 Código TypeScript, TSX, JavaScript o CSS propio

Los archivos de código propio deben declarar versión mediante comentario inicial. El formato se adapta al lenguaje:

```ts
// Archivo: nombre-del-archivo.tsx
// Versión: 1.0
// Estado: Línea base Parcial 2
// Fecha: 2026-06-16
// Responsables: Agustín Tejero y Alejandro Herms
```

```css
/*
 * Archivo: nombre-del-archivo.css
 * Versión: 1.0
 * Estado: Línea base Parcial 2
 * Fecha: 2026-06-16
 * Responsables: Agustín Tejero y Alejandro Herms
 */
```

### 5.3 Imágenes, PDFs, enlaces y archivos binarios

Los archivos binarios no admiten encabezado interno sin alterar su contenido. Para esos casos, la versión se registra en esta matriz y, cuando corresponda, en el nombre del archivo o en el documento que lo referencia.

### 5.4 Archivos de estructura

Los archivos `.gitkeep` se consideran artefactos estructurales. No requieren encabezado interno; quedan cubiertos por esta matriz como parte de la estructura del repositorio.

---

## 6. Matriz inicial de versionado

| Artefacto o grupo | Tipo | Versión inicial | Estado | Criterio de versionado |
|---|---|---|---|---|
| `README.md` | Documento general | 1.0 | Línea base Parcial 2 | Debe declarar versión del repositorio y orientar a la documentación principal |
| `docs/marco-del-proyecto/control-de-versiones.md` | Documento de control | 1.0 | Línea base Parcial 2 | Matriz rectora del versionado explícito |
| `docs/marco-del-proyecto/objetivos-del-proyecto.md` | Documento de gestión | 1.0 | Línea base Parcial 2 | Incrementa ante cambios de objetivos SMART/OKR |
| `docs/marco-del-proyecto/stakeholders-y-actores.md` | Documento de gestión | 1.0 | Línea base Parcial 2 | Incrementa ante cambios de interesados, actores o perfiles |
| `docs/marco-del-proyecto/alcance-general.md` | Documento de gestión | 1.0 | Línea base Parcial 2 | Incrementa ante cambios de alcance, exclusiones o reglas generales |
| `docs/marco-del-proyecto/guia-uso-github-project.md` | Documento de metodología | 1.0 | Línea base Parcial 2 | Incrementa ante cambios en metodología, flujo Kanban, campos o criterios de Project |
| `docs/marco-del-proyecto/matriz-trazabilidad.md` | Matriz de trazabilidad | 1.0 | Línea base Parcial 2 | Incrementa ante cambios en relaciones entre objetivos, requerimientos, historias y épicas |
| `docs/marco-del-proyecto/WBS.pdf` | Entregable binario | 1.0 | Reemplazado | Conservado por trazabilidad; reemplazado por versión posterior si corresponde |
| `docs/marco-del-proyecto/WBS V2.pdf` | Entregable binario | 1.0 | Línea base Parcial 2 | Versionado registrado en matriz por ser PDF |
| `docs/marco-del-proyecto/WBS (1).jpg` | Imagen de WBS | 1.0 | Reemplazado | Conservado como evidencia o exportación alternativa |
| `docs/marco-del-proyecto/WBS V2.jpg` | Imagen de WBS | 1.0 | Línea base Parcial 2 | Versionado registrado en matriz por ser imagen |
| `docs/marco-del-proyecto/LINK MIRO - WBS` | Enlace externo | 1.0 | Línea base Parcial 2 | Incrementa si cambia el tablero o enlace de referencia |
| `docs/analisis/espesificacion-de-requerimientos/requerimientos-funcionales.md` | Requerimientos | 1.0 | Línea base Parcial 2 | Incrementa ante altas, bajas o cambios de RF |
| `docs/analisis/espesificacion-de-requerimientos/requerimientos-no-funcionales.md` | Requerimientos | 1.0 | Línea base Parcial 2 | Incrementa ante altas, bajas o cambios de RNF |
| `docs/analisis/espesificacion-de-requerimientos/matriz-reglas-de-negocio.md` | Matriz de reglas | 1.0 | Línea base Parcial 2 | Incrementa ante cambios en reglas críticas |
| `docs/analisis/historias-de-usuarios/historias-de-usuario.md` | Historias de usuario | 1.0 | Línea base Parcial 2 | Incrementa ante cambios de HU, criterios o trazabilidad |
| `docs/analisis/casos-de-uso/casos-de-uso.md` | Catálogo de casos de uso | 1.0 | Línea base Parcial 2 | Incrementa cuando se agregan, eliminan o reorganizan casos |
| `docs/analisis/casos-de-uso/plantilla-caso-de-uso.md` | Plantilla documental | 1.0 | Línea base Parcial 2 | Incrementa ante cambios en el formato estándar de casos |
| `docs/analisis/casos-de-uso/01-pedidos/*.md` | Casos de uso de pedidos | 1.0 | Línea base Parcial 2 | Cada caso debe tener encabezado propio y versión individual |
| `docs/analisis/casos-de-uso/02-archivos/*.md` | Casos de uso de archivos | 1.0 | Línea base Parcial 2 | Cada caso debe tener encabezado propio y versión individual |
| `docs/analisis/casos-de-uso/03-revision-administrativa/*.md` | Casos de uso de revisión | 1.0 | Línea base Parcial 2 | Cada caso debe tener encabezado propio y versión individual |
| `docs/analisis/casos-de-uso/04-estados-y-finanzas/*.md` | Casos de uso de estados y finanzas | 1.0 | Línea base Parcial 2 | Cada caso debe tener encabezado propio y versión individual |
| `docs/analisis/casos-de-uso/05-impresion/*.md` | Casos de uso de impresión | 1.0 | Línea base Parcial 2 | Cada caso debe tener encabezado propio y versión individual |
| `docs/analisis/casos-de-uso/06-usuarios-y-permisos/*.md` | Casos de uso de usuarios y permisos | 1.0 | Línea base Parcial 2 | Cada caso debe tener encabezado propio y versión individual |
| `docs/analisis/casos-de-uso/07-trazabilidad-y-cierre/*.md` | Casos de uso de auditoría y cierre | 1.0 | Línea base Parcial 2 | Cada caso debe tener encabezado propio y versión individual |
| `docs/analisis/casos-de-uso/08-web-y-android/*.md` | Casos de uso Web y Android | 1.0 | Línea base Parcial 2 | Cada caso debe tener encabezado propio y versión individual |
| `docs/diseño/Back/arquitectura-del-sistema/diagramas/*.png` | Diagramas de arquitectura | 1.0 | Línea base Parcial 2 | Versionado registrado en matriz por ser imágenes |
| `docs/diseño/Front/ux-ui/documentacion/*.md` | Documentación UX/UI | 1.0 | Línea base Parcial 2 | Incrementa ante cambios de diseño, guidelines o referencia visual |
| `docs/diseño/Front/ux-ui/documentacion/styles/*.css` | Estilos de referencia | 1.0 | Línea base Parcial 2 | Deben usar encabezado CSS si son editados |
| `docs/diseño/Front/ux-ui/documentacion/ejemplo-implementacion/app/**/*.tsx` | Ejemplo de implementación Front | 1.0 | Línea base Parcial 2 | Debe usar encabezado de código si se mantiene como código propio |
| `docs/diseño/Front/ux-ui/documentacion/ejemplo-implementacion/app/components/ui/*.ts` | Utilidades de UI | 1.0 | Línea base Parcial 2 | Debe usar encabezado de código si se mantiene como código propio |
| `docs/diseño/Front/ux-ui/wireflows/*.md` | Wireflows documentados | 1.0 | Línea base Parcial 2 | Incrementa ante cambios de flujo o pantallas |
| `docs/diseño/Front/ux-ui/wireflows/**/*.png` | Wireframes exportados | 1.0 | Línea base Parcial 2 | Versionado registrado en matriz por ser imágenes |
| `docs/diseño/Front/ux-ui/vistas-web-mockups/**/*.png` | Mockups Web exportados | 1.0 | Línea base Parcial 2 | Versionado registrado en matriz por ser imágenes |
| `docs/diseño/Front/ux-ui/vistas-web-mockups/cliente/2-page-cli-dashboard.md` | Mockup documentado | 1.0 | Línea base Parcial 2 | Debe usar encabezado Markdown |
| `docs/marketing/readme.md` | Documento de marketing | 1.0 | Línea base Parcial 2 | Incrementa ante cambios de criterio o piezas de marketing |
| `docs/marketing/flyers-publicitarios/ejemplo-flyer-001.jpeg` | Pieza gráfica | 1.0 | Línea base Parcial 2 | Versionado registrado en matriz por ser imagen |
| `img/logo.jpg` | Imagen institucional | 1.0 | Línea base Parcial 2 | Versionado registrado en matriz por ser imagen |
| `.gitignore` | Configuración de repositorio | 1.0 | Línea base Parcial 2 | Incrementa si cambian reglas de exclusión de archivos |
| `**/.gitkeep` | Estructura de carpetas | 1.0 | Línea base Parcial 2 | No requiere encabezado; queda cubierto por esta matriz |

---

## 7. Inventario observado de la línea base

| Tipo de archivo | Cantidad observada antes de crear esta matriz | Tratamiento |
|---|---:|---|
| Markdown `.md` | 59 | Encabezado interno obligatorio |
| TypeScript React `.tsx` | 48 | Comentario inicial si se mantiene como código propio |
| TypeScript `.ts` | 2 | Comentario inicial si se mantiene como código propio |
| CSS `.css` | 2 | Comentario inicial CSS |
| PNG `.png` | 40 | Registro en matriz |
| JPG/JPEG `.jpg`, `.jpeg` | 4 | Registro en matriz |
| PDF `.pdf` | 2 | Registro en matriz |
| `.gitkeep` | 25 | Cubierto como estructura |
| Otros archivos versionados | 2 | Registro en matriz según función |

Luego de incorporar este documento, la cantidad de archivos Markdown pasa a 60 dentro del repositorio local.

---

## 8. Procedimiento de actualización

1. Todo archivo nuevo propio inicia en versión `1.0`.
2. Todo cambio relevante debe actualizar el encabezado del archivo afectado.
3. Todo cambio relevante debe quedar respaldado por commit, issue o ítem del GitHub Project.
4. Si el cambio modifica alcance, metodología, requerimientos, arquitectura o trazabilidad, también debe actualizarse la matriz correspondiente.
5. Los archivos binarios deben versionarse en esta matriz o en el documento que los referencia.
6. Cuando una versión reemplaza a otra, la versión anterior no se borra de la historia: queda registrada como `Reemplazado` u `Obsoleto`.

---

## 9. Trabajo pendiente asociado

Para completar la adecuación del repositorio a esta matriz, debe crearse una issue o sub-issue de documentación con el siguiente alcance:

- agregar encabezado de versionado a todos los documentos Markdown que todavía no lo tengan;
- agregar comentarios de versionado a archivos de código propio si se decide conservarlos como parte de la entrega;
- registrar explícitamente en esta matriz los binarios o assets que no puedan tener encabezado interno;
- validar que la versión global `1.0` quede reflejada en README, documentos base y GitHub Project.

La tarea debe vincularse preferentemente a una épica de documentación, gestión del proyecto o trazabilidad.
