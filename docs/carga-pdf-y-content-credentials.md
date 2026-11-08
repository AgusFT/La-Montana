# Carga de PDF con Content Credentials

Un PDF exportado desde un editor puede conservar las credenciales de procedencia de las imágenes como un manifiesto C2PA asociado. El rechazo general de archivos incrustados impedía configurar estos documentos aunque sus páginas fueran imprimibles.

El inspector admite exclusivamente manifiestos asociados mediante `AFRelationship=C2PA_Manifest`, con tipo `EmbeddedFile`, MIME `application/c2pa` y cabecera JUMBF/C2PA y longitud coherentes. Los inspecciona en el mismo proceso aislado y con los mismos límites de recursos. Conserva el archivo original y su huella; aceptar estos metadatos no autentica sus afirmaciones.

Los adjuntos comunes, las anotaciones de archivo, JavaScript, acciones adicionales y acciones de ejecución o apertura externa continúan rechazándose. El antivirus sigue siendo obligatorio.

La preparación indica claramente si el archivo fue rechazado o no pudo analizarse, muestra el motivo y permite quitarlo. Las opciones de impresión permanecen deshabilitadas hasta obtener un archivo válido. El presupuesto se habilita cuando todos los PDF tienen opciones completas.

Después de actualizar el sistema, los intentos que ya quedaron rechazados deben quitarse y cargarse nuevamente para ejecutar el nuevo análisis.

Validación: inspector aislado con documentos sintéticos aceptados y rechazados; análisis de un PDF de diez páginas exportado por LibreOffice con Content Credentials; pruebas de renderizado del formulario y de la oferta de impresión.
