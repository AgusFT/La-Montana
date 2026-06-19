# Sistema de control de versiones - La Montaña


## 1. Objetivo del documento

Este documento define cómo se controla el versionado del proyecto **La Montaña**.

El objetivo es  establecer un criterio claro para que los cambios puedan verificarse desde:

- el historial de commits de Git;
- los mensajes o comentarios de cada commit;
- el historial de archivos modificado por GitHub;
- las issues y sub-issues asociadas al trabajo;
- el GitHub Project usado para seguimiento.

La versión general del proyecto se administra fuera de los mensajes de commit, mediante tags de Git/GitHub. Los mensajes de commit se usan para registrar la versión de los archivos modificados por ese commit.

---

## 2. Vigencia del criterio

Este criterio de control de versiones entra en vigencia a partir del commit:

`e1f31023 - docs(project): actualizar criterio de versionado`

A partir de ese commit, los cambios relevantes de archivos deben documentar su versionado dentro del mensaje del commit, indicando la versión anterior, la nueva versión y el motivo del cambio. Cuando un archivo no cambie de versión, debe indicarse como `sin cambio`.

Los commits anteriores a `e1f31023` forman parte del historial previo del proyecto y no necesariamente respetan este formato, porque fueron realizados antes de que el equipo definiera formalmente esta metodología de control de versiones.

Este criterio no se aplica de manera retroactiva a commits anteriores.

---

## 3. Criterio general

El proyecto usa **Git y GitHub** como sistema principal de control de versiones.

Cada cambio relevante debe quedar registrado mediante un commit. El commit debe indicar:

- qué archivo o grupo de archivos cambió;
- si la versión de cada archivo cambió o se mantiene sin cambio;
- qué se modificó;
- por qué se realizó el cambio;
- qué issue o tarea del Project lo respalda, cuando corresponda.

No es necesario que cada archivo tenga un encabezado interno con su versión. La versión queda respaldada por el historial de Git y por el mensaje del commit.


---

## 5. Regla de numeración

Se usa una numeración simple:

| Tipo de cambio | Regla | Ejemplo |
|---|---|---|
| Versión inicial | Se registra como `1.0` | Primer control formal del proyecto |
| Cambio menor | Incrementa el segundo número | `1.0` a `1.1` |
| Corrección puntual | Puede incrementar el tercer número | `1.1` a `1.1.1` |
| Cambio mayor | Incrementa el primer número | `1.1` a `2.0` |

 Se prioriza el formato `1.0`, `1.1`, `1.2`. El tercer número se reserva para correcciones pequeñas que convenga dejar explícitas.

La misma numeración puede usarse en dos niveles distintos:

- en los tags de Git/GitHub, para la versión general del proyecto;
- en los mensajes de commit, para la versión del archivo o grupo de archivos modificados.


---

## 7. Mensajes de commit

Los commits deben ser claros y verificables.

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

Tipos sugeridos:

| Tipo | Uso |
|---|---|
| `docs` | Cambios en documentación |
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de error |
| `chore` | Organización, configuración o mantenimiento |
| `refactor` | Reorganización sin cambio funcional |
| `test` | Pruebas o validaciones |

Ejemplos de asunto:

```text
docs(project): actualizar criterio de versionado
docs(requirements): agregar requerimientos no funcionales
feat(web): simular vista de dashboard cliente
chore(repo): reorganizar estructura documental
```

---

## 9. Relación con issues y Project

Cuando un cambio responda a una tarea planificada, debe vincularse con una issue o sub-issue.

Para este criterio de versionado se usa como referencia:

- Issue: `#90 - [DOC] Definir criterio de control de versiones`
- Épica relacionada: `#14 - E01 - Gestion del proyecto y documentacion academica`
- Project: `La-Montana`

La issue/subissue permite ver el objetivo del cambio, el estado dentro del Project y los comentarios de seguimiento. El commit permite ver la modificación concreta en el repositorio.

---



## 11. Resumen operativo

El sistema de versionado del proyecto queda definido así:

1. Git es la fuente principal del historial.
2. GitHub permite verificar commits, archivos modificados e historial por archivo.
3. La versión general del proyecto se declara mediante tags de Git/GitHub.
4. Las issues y el Project dan contexto de planificación y seguimiento.
5. No se exige encabezado de versión dentro de cada archivo.
6. Cada commit relevante debe indicar si la versión de los archivos importantes cambió o quedó sin cambio.
7. Este criterio rige desde el commit `e1f31023` y no se aplica retroactivamente.
