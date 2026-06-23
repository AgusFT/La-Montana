# Guía de Pull Requests - La Montaña

| Campo | Valor |
|---|---|
| Versión | 1.0 |
| Estado | Vigente |
| Fecha | 2026-06-23 |
| Responsables | Agustín Tejero y Alejandro Herms |

## 1. Objetivo

Este documento define cómo deben crearse, revisar e integrar Pull Requests dentro del proyecto **La Montaña**.

Su objetivo es mantener un flujo de trabajo trazable entre:

- ramas de desarrollo;
- commits con versionado;
- issues y sub-issues;
- GitHub Project;
- revisión cruzada;
- cambios de código o documentación.

Esta guía complementa:

- `marco-del-proyecto/control-de-versiones.md`;
- `marco-del-proyecto/guia-uso-github-project.md`.

---

## 2. Criterio general

Un Pull Request debe representar una unidad de trabajo entendible y verificable.

Puede corresponder a:

- una historia de usuario implementable;
- una tarea técnica;
- un ajuste documental;
- una corrección;
- un spike con resultado versionable;
- una mejora de estructura o mantenimiento.

El PR no debe mezclar cambios no relacionados. Si una tarea afecta varias áreas, debe dividirse en PRs separados o en commits claramente agrupados dentro del mismo PR, siempre que el alcance siga siendo defendible.

---

## 3. Relación con el GitHub Project

Antes de abrir un PR, la issue vinculada debe estar en el Project y tener, como mínimo:

- `Status`;
- `Area`;
- `Tipo`;
- `Prioridad`;
- `MoSCoW`;
- `Puntos`;
- `Horas`;
- `FechaInicio`;
- `FechaObjetivo`;
- `Assignees`;
- `Milestone`.

Flujo recomendado de estados:

| Momento | Status sugerido |
|---|---|
| La issue está refinada y lista | Para hacer |
| Se crea la rama y comienza el trabajo | En desarrollo |
| El PR está abierto y listo para revisar | En revision |
| El cambio necesita prueba funcional | Testeo |
| El PR fue aceptado o el cambio quedó integrado | Terminado |
| Existe una dependencia o bloqueo real | Bloqueado |

La issue debe quedar vinculada al PR o, como mínimo, a los commits que resuelven el trabajo.

---

## 4. Creación de ramas

Toda rama de trabajo debe salir de `main` actualizado, salvo que exista una decisión explícita distinta.

Formato recomendado:

```text
tipo/area-identificador-descripcion
```

Ejemplos:

```text
feat/web-hu-cli-009-resumen-pedido
feat/backend-orders-create
docs/project-guia-pr
fix/web-validacion-pdf
chore/repo-limpieza-logs
```

Tipos sugeridos:

| Tipo | Uso |
|---|---|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de error |
| `docs` | Documentación |
| `chore` | Organización o mantenimiento |
| `refactor` | Reorganización sin cambio funcional |
| `test` | Pruebas o validaciones |
| `spike` | Investigación con salida verificable |

La rama debe tener un nombre corto, en minúsculas y con guiones medios.

---

## 5. Commits dentro del PR

Los commits deben respetar el criterio definido en `control-de-versiones.md`.

Formato recomendado:

```text
tipo(area): descripción breve

Versionado:
- archivo/modificado.ext: versión anterior -> versión nueva
- archivo/modificado-sin-version.ext: sin cambio

Cambios:
- Descripción concreta del cambio realizado.

Motivo:
- Motivo del cambio.

Issue relacionada: #número
```

Criterios:

- usar mensajes claros y verificables;
- indicar versionado cuando corresponda;
- referenciar la issue vinculada;
- evitar commits genéricos como `change`, `update` o `fix cosas`;
- no incluir secretos, credenciales, archivos temporales ni salidas locales innecesarias;
- no mezclar cambios de documentación, backend y frontend si no pertenecen al mismo objetivo.

---

## 6. Contenido esperado del PR

El PR debe tener un título claro.

Formatos aceptados:

```text
HU-CLI-009 - Resumen de pedido
feat(web): implementar resumen de pedido
docs(project): documentar flujo de Pull Requests
fix(web): corregir validación de archivo PDF
```

El cuerpo del PR debe incluir:

```markdown
## Objetivo

Qué busca resolver este PR.

## Issue relacionada

- #número

## Cambios

- Cambio concreto 1.
- Cambio concreto 2.

## Versionado

- archivo.ext: versión anterior -> versión nueva
- archivo-sin-version.ext: sin cambio

## Validación

- Comando ejecutado, prueba manual o revisión realizada.

## Riesgos o notas

- Riesgo conocido, decisión tomada o pendiente relevante.
```

Si el PR no tiene pruebas automatizadas, debe dejar indicada la validación manual realizada o explicar por qué no aplica.

---

## 7. Checklist antes de abrir el PR

Antes de abrir un PR se debe revisar:

- la rama sale de `main` actualizado;
- el cambio corresponde a una issue o tarea clara;
- el Project refleja el trabajo en `En desarrollo`;
- los commits tienen mensajes coherentes;
- no se agregan archivos ajenos al alcance;
- no se agregan credenciales ni datos sensibles;
- no se eliminan documentos o carpetas vigentes sin motivo explícito;
- el README o documentación se actualiza si el cambio altera estructura, uso o comportamiento;
- el código queda ubicado en la carpeta correspondiente;
- el diff puede explicarse sin depender de contexto externo.

---

## 8. Revisión del PR

La revisión debe priorizar:

- coherencia con el alcance de la issue;
- cumplimiento de criterios de aceptación;
- trazabilidad con requerimientos, HU, casos de uso o reglas críticas;
- consistencia con la arquitectura definida;
- claridad del versionado;
- ausencia de cambios no relacionados;
- ausencia de secretos o archivos temporales;
- impacto sobre `main`.

La revisión puede resolverse de tres formas:

| Resultado | Acción |
|---|---|
| Aprobado | El PR puede integrarse |
| Cambios solicitados | La rama debe actualizarse antes de integrar |
| Cerrado sin merge | Se descarta o reemplaza por otro enfoque |

Cuando se pidan cambios, deben resolverse con nuevos commits en la misma rama del PR, salvo que sea más claro cerrar el PR y abrir uno nuevo.

---

## 9. Criterios para integrar a main

Un PR puede integrarse cuando:

- tiene alcance claro;
- no contiene cambios ajenos;
- cumple la issue o deja explícito qué queda fuera;
- fue revisado por el otro integrante o validado manualmente;
- el Project está actualizado;
- no rompe reglas críticas del negocio;
- los commits o el PR dejan evidencia suficiente del cambio;
- no hay conflictos pendientes.

Después de integrar:

- mover la issue a `Terminado`, si corresponde;
- dejar comentario de evidencia en la issue si aporta claridad;
- verificar que `main` conserve la estructura esperada;
- eliminar la rama remota si ya no se necesita;
- actualizar documentación si el merge cambió alcance, arquitectura o estructura.

---

## 10. PRs de documentación

Los PRs documentales deben seguir los mismos criterios de trazabilidad.

Deben indicar:

- documento modificado;
- versión anterior y nueva, si corresponde;
- motivo del cambio;
- issue relacionada;
- impacto sobre Project, alcance, requerimientos o arquitectura.

Si el documento nuevo no modifica una versión previa, se registra como `1.0`.

---

## 11. PRs de código

Los PRs de código deben indicar:

- carpeta afectada;
- funcionalidad o flujo representado;
- HU, tarea técnica o épica relacionada;
- validación realizada;
- decisiones o límites conocidos.

El código debe respetar la organización vigente:

| Área | Ubicación esperada |
|---|---|
| Web MVP | `desarrollo/web/` |
| Prototipo visual | `desarrollo/prototipo-figma/` |
| Backend Supabase | `desarrollo/backend-supabase/` |
| Simulación aislada o evidencia de código | `Código Fuente/`, si se requiere como artefacto separado |

---

## 12. Situaciones donde conviene abrir PR separado

Abrir un PR separado cuando:

- se cambia documentación y además se implementa código no relacionado;
- se modifican backend y frontend sin una issue común;
- se agrega o elimina estructura de carpetas;
- se toca configuración sensible;
- se corrige un bug mientras se implementa una funcionalidad distinta;
- se detecta una mejora que excede el alcance de la issue original.

---

## 13. Situaciones a evitar

Evitar:

- PRs enormes sin objetivo único;
- títulos genéricos;
- bodies vacíos;
- commits sin versionado cuando corresponde;
- cambios directos a `main` para trabajo funcional;
- cerrar PRs sin dejar claro si fueron reemplazados;
- marcar issues como terminadas si el PR no fue revisado o integrado;
- usar el PR como lugar para decidir alcance que todavía no fue refinado en el Project.

---

## 14. Resumen operativo

1. Refinar issue en Project.
2. Crear rama desde `main`.
3. Implementar cambios acotados.
4. Committear respetando `control-de-versiones.md`.
5. Abrir PR con título, descripción, issue, cambios, versionado y validación.
6. Mover la issue a `En revision`.
7. Revisar el PR.
8. Integrar cuando cumpla criterios.
9. Actualizar Project, evidencia y documentación.
10. Cerrar o limpiar ramas que ya no se usen.
