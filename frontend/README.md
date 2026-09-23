<!--
FUNCIÓN: guía de los encabezados e índice de archivos especiales del frontend.
CONTENIDO: plegado, inventario, mantenimiento, configuración y generados.
MÉTODOS: no declara código ejecutable.
-->
# Lectura rápida del frontend

Los archivos TypeScript, TSX y el script JavaScript comienzan con una región `ENCABEZADO`, delimitada por `//#region` y `//#endregion`. En VS Code o VSCodium con soporte del lenguaje, la flecha del margen permite contraerla y conservar visible la ruta del archivo.

Cada encabezado explica la función del archivo y enumera sus componentes, hooks, funciones nombradas, constructores, métodos y tipos. Las firmas incluyen parámetros y el retorno explícito, cuando existe. `Ámbito :: firma` identifica auxiliares dentro de componentes o clases. Se incluyen funciones asignadas a variables o propiedades y las creadas con `useCallback`; no se enumeran por separado todos los callbacks anónimos de JSX, efectos o recorridos de listas.

La sección de valores de módulo enumera constantes, exportaciones y reexportaciones sin duplicar su contenido. Un archivo que sólo exporta datos o reexporta una función no se presenta como si declarara una función nueva. Las directivas `"use client"` y las importaciones `server-only` se conservan en el código original; los encabezados son comentarios.

Las hojas CSS tienen comentarios plegables con su propósito, clases por prefijo y condiciones responsive. Dockerfile y archivos ignore usan comentarios `#`; el SVG usa un comentario XML. Su plegado depende del soporte del formato en el editor. No se cambian selectores, reglas, trazados ni rutas.

Al agregar, quitar o modificar una declaración, actualizar también el inventario del encabezado. No incluir secretos, valores de entorno ni datos de usuarios en esta documentación.

## Archivos documentados aquí sin modificar su contenido

### package.json

Manifiesto de la aplicación: nombre y versión, versión de pnpm, rango de Node.js, scripts y dependencias. JSON no permite comentarios; su explicación se mantiene aquí.

- `dev`: prepara el worker local de PDF.js e inicia Next.js en desarrollo.
- `build`: prepara PDF.js y construye la aplicación de producción.
- `start`: inicia Next.js en producción.
- `typecheck`: ejecuta TypeScript sin emitir archivos.
- `dependencies` y `devDependencies`: bibliotecas de ejecución y herramientas de desarrollo.

No declara métodos ni funciones de aplicación; los scripts son comandos.

### tsconfig.json

Configuración del compilador TypeScript: comprobación estricta, JSX de React, resolución para bundler, alias `@/*`, plugin de Next.js, archivos incluidos y exclusiones. Se conserva su formato original y se documenta aquí junto al resto de configuración JSON. No declara funciones.

### pnpm-lock.yaml

Lockfile generado por pnpm: fija versiones, integridad y resolución de dependencias para instalaciones reproducibles. Secciones principales: versión del lockfile, ajustes, importadores, paquetes y resoluciones. No se modifica manualmente ni se le agregan encabezados que se perderían al regenerarlo. No declara funciones de aplicación.

### next-env.d.ts

Archivo generado por Next.js: referencias de tipos del framework, imágenes y rutas. Su propio aviso indica que no debe editarse. Se conserva sin encabezado manual; no declara funciones propias.

### public/.gitkeep

Archivo vacío que permite conservar el directorio de recursos públicos en Git. No declara funciones ni contiene lógica; se mantiene vacío.

## Recursos generados fuera del inventario de fuentes

`node_modules/`, `.next/` y los recursos copiados de PDF.js pertenecen a dependencias o al proceso de construcción. El script `scripts/prepare-pdf.mjs` conserva el worker y su licencia en la ruta pública versionada. No se insertan comentarios manuales en código de terceros ni en resultados generados.
