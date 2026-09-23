<!--
FUNCIÓN: guía de los encabezados y del inventario del backend.
CONTENIDO: alcance, plegado, convenciones y recursos documentados aparte.
MÉTODOS: no declara código ejecutable.
-->
# Lectura rápida del backend

Los archivos Java de producción y pruebas comienzan con una región `ENCABEZADO`, delimitada por `//#region` y `//#endregion`. En VS Code y VSCodium con soporte de Java puede plegarse con la flecha del margen; contraída conserva el nombre del archivo. El encabezado es un comentario y no interviene en la ejecución.

Cada encabezado incluye:

- **Función:** responsabilidad concreta del archivo.
- **Constructores declarados:** firmas de los constructores escritos explícitamente.
- **Métodos declarados:** firmas, parámetros y retorno; incluye privados, sobrecargas y tipos internos. Se indica el acceso y, cuando corresponde, la entrada HTTP, el ciclo de prueba o la tarea de Spring.
- **Tipos declarados:** clases, interfaces, records, enumeraciones y clases anónimas presentes.

Las firmas siguen el orden del código dentro de cada sección. `paquete` indica ausencia de modificador de acceso; en interfaces se señala el contexto. `Tipo :: firma` identifica un miembro de un tipo interno. Los métodos y constructores generados automáticamente por Java no se enumeran como declaraciones del archivo. Las rutas HTTP listadas en un método se combinan con el prefijo del controlador, cuando existe.

Al agregar, quitar o modificar una declaración, actualizar también su entrada en el encabezado. Los comentarios anteriores y las firmas del código se conservan. Estos encabezados no reemplazan la documentación detallada de una regla de negocio.

Dockerfile, `.dockerignore`, `pom.xml` y `application.yaml` describen su función y sus secciones con comentarios de su propio formato. El plegado en esos formatos depende del soporte de lenguaje del editor; los marcadores Java se usan sólo en `.java`.

Las [37 migraciones SQL](src/main/resources/db/migration/README.md) se documentan aparte para conservar sus checksums de Flyway. La [imagen WebP de prueba](src/test/resources/web/README.md) también se conserva intacta porque es binaria.
