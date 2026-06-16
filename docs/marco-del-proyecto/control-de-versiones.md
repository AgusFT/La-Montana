# Sistema de control de versiones - La Montaña

| Campo | Valor |
|---|---|
| Documento | Sistema de control de versiones |
| Versión del criterio | 1.1 |
| Fecha | 2026-06-16 |
| Responsables | Agustín Tejero y Alejandro Herms |

## 1. Objetivo del documento

Este documento define cómo se controla el versionado del proyecto **La Montaña**.

El objetivo no es agregar encabezados obligatorios a cada archivo, sino establecer un criterio claro para que los cambios puedan verificarse desde:

- el historial de commits de Git;
- los mensajes o comentarios de cada commit;
- el historial de archivos modificado por GitHub;
- las issues y sub-issues asociadas al trabajo;
- el GitHub Project usado para seguimiento.

De esta forma, el profesor puede revisar qué cambió, cuándo cambió, quién lo modificó y qué versión se asignó a ese cambio.

---

## 2. Criterio general

El proyecto usa **Git y GitHub** como sistema principal de control de versiones.

Cada cambio relevante debe quedar registrado mediante un commit. El commit debe indicar:

- qué archivo o grupo de archivos cambió;
- qué versión se asigna al cambio;
- qué se modificó;
- por qué se realizó el cambio;
- qué issue o tarea del Project lo respalda, cuando corresponda.

No es obligatorio que cada archivo tenga un encabezado interno con su versión. La versión queda respaldada por el historial de Git y por el mensaje del commit.

---

## 3. Versión del proyecto

| Elemento | Criterio |
|---|---|
| Versión inicial del proyecto | `1.0` |
| Sistema de versionado | Git, GitHub, issues y GitHub Project |
| Registro principal | Historial de commits |
| Registro complementario | Issues, sub-issues y comentarios asociados |
| Ubicación de este documento | `docs/marco-del-proyecto/control-de-versiones.md` |

La versión `1.0` representa el punto de inicio formal del control de versiones del proyecto para el Parcial 2.

Cuando el proyecto tenga una entrega importante, una modificación grande de alcance o una reorganización significativa, se podrá incrementar la versión general del proyecto.

---

## 4. Regla de numeración

Se usa una numeración simple:

| Tipo de cambio | Regla | Ejemplo |
|---|---|---|
| Versión inicial | Se registra como `1.0` | Primer control formal del proyecto |
| Cambio menor | Incrementa el segundo número | `1.0` a `1.1` |
| Corrección puntual | Puede incrementar el tercer número | `1.1` a `1.1.1` |
| Cambio mayor | Incrementa el primer número | `1.1` a `2.0` |

Para el parcial se prioriza el formato `1.0`, `1.1`, `1.2`. El tercer número se reserva para correcciones pequeñas que convenga dejar explícitas.

---

## 5. Versionado de archivos

Cada archivo puede tener su propia evolución, pero esa evolución se registra en Git.

Cuando un commit modifica uno o varios archivos importantes, el mensaje del commit debe aclarar la versión correspondiente.

Ejemplo:

```text
docs(project): actualizar sistema de control de versiones

Versionado:
- Proyecto: 1.0 -> 1.1
- docs/marco-del-proyecto/control-de-versiones.md: 1.0 -> 1.1

Cambios:
- Se elimina la obligatoriedad de encabezados por archivo.
- Se define que la versión se verifica desde historial de commits.
- Se agregan criterios para revisar cambios desde GitHub.

Issue relacionada: #90
```

Si el cambio es pequeño y no modifica la versión general del proyecto, puede indicarse así:

```text
docs(requirements): corregir redacción de requerimientos funcionales

Versionado:
- Proyecto: sin cambio
- docs/analisis/espesificacion-de-requerimientos/requerimientos-funcionales.md: 1.1 -> 1.1.1

Cambios:
- Se corrige redacción sin alterar el alcance funcional.
```

---

## 6. Mensajes de commit

Los commits deben ser claros y verificables.

Formato recomendado:

```text
tipo(area): descripción breve

Versionado:
- Proyecto: versión anterior -> versión nueva
- archivo/modificado.ext: versión anterior -> versión nueva

Cambios:
- Descripción concreta del cambio realizado.
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

## 7. Verificación del historial

El historial puede revisarse desde GitHub o desde la terminal.

Comandos útiles:

```bash
git log --oneline
```

Muestra el historial resumido de commits.

```bash
git show --stat <hash-del-commit>
```

Muestra qué archivos cambió un commit.

```bash
git show <hash-del-commit>
```

Muestra el detalle del cambio y el mensaje completo del commit.

```bash
git log -- docs/marco-del-proyecto/control-de-versiones.md
```

Muestra el historial de cambios de un archivo específico.

En GitHub también puede verificarse desde:

- la pestaña **Commits** del repositorio;
- el botón **History** de cada archivo;
- las issues vinculadas al trabajo;
- el GitHub Project del proyecto.

---

## 8. Relación con issues y Project

Cuando un cambio responda a una tarea planificada, debe vincularse con una issue o sub-issue.

Para este criterio de versionado se usa como referencia:

- Issue: `#90 - [DOC] Adecuar encabezados a la matriz de versionado`
- Épica relacionada: `#14 - E01 - Gestion del proyecto y documentacion academica`
- Project: `La-Montana`

La issue permite ver el objetivo del cambio, el estado dentro del Project y los comentarios de seguimiento. El commit permite ver la modificación concreta en el repositorio.

---

## 9. Criterio para nuevas versiones

Debe registrarse una nueva versión cuando ocurra alguno de estos casos:

- se agrega un documento importante;
- se cambia el alcance del proyecto;
- se modifican requerimientos, historias de usuario o casos de uso;
- se actualiza la metodología de trabajo;
- se reorganiza la estructura del repositorio;
- se agrega código de simulación o funcionalidad;
- se corrige una decisión que afectaba la interpretación del proyecto.

No hace falta incrementar versión por cambios mínimos de formato, tildes, espacios o correcciones que no afecten el contenido, salvo que el equipo decida dejarlo explícito.

---

## 10. Resumen operativo

El sistema de versionado del proyecto queda definido así:

1. Git es la fuente principal del historial.
2. GitHub permite verificar commits, archivos modificados e historial por archivo.
3. Las versiones se declaran en el mensaje del commit.
4. Las issues y el Project dan contexto de planificación y seguimiento.
5. No se exige encabezado de versión dentro de cada archivo.
6. Los cambios importantes deben indicar versión anterior, versión nueva y motivo.

Este criterio permite mantener el proyecto ordenado sin agregar información repetida dentro de todos los archivos.
