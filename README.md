# La Montaña · piloto v0.1

Aplicación local con Spring Boot, Next.js y PostgreSQL para una imprenta con varias sucursales. El administrador configura el sistema desde cero; los clientes particulares cotizan PDF y el personal completa revisión, producción manual, calidad, entrega y cierre. No hay cuentas, sucursales, precios, reglas comerciales ni pedidos de muestra precargados. La implementación de Supabase quedó sustituida y permanece solamente en el historial Git.

## Estado actual · demo v0.1 y plan v6 completos

La demo cumple los once criterios originales y los nueve agregados en la revisión 6 del plan. La suite completa aprobó 165 pruebas; backend y frontend compilan, incluido el tipado. La aceptación conserva la evidencia original de modelos manual/condicionales, permisos, D1/D2 y las tres modalidades de entrega, y agrega regresión de pedidos, pruebas del sitio, instalación vacía, migración, persistencia y presentación responsive.

El configurador operativo conserva el menú general en escritorio y móvil e incluye ayudas en cada fase. Un borrador guarda el avance sin activar reglas: la activación se autoriza en el paso 7. Al salir de un formulario con cambios sin guardar se puede seguir editando o descartarlos; las solicitudes y autorizaciones pendientes se resuelven o revocan desde el formulario. El navegador de fases mantiene visible el paso seleccionado en pantallas angostas.

El panel `/` explica el orden de instalación y permanece accesible con sesión. `/web` es la página pública local: antes de publicar muestra una estructura sin contenido comercial. Antes del propietario, login y registro explican el requisito; después, los clientes pueden registrarse sin activar pedidos. El dashboard distingue requisitos pendientes, borrador, versión activa y complementos, a partir de datos reales. El configurador web permite importar imágenes de una carpeta local, guardar identidad/fichas/secciones, revisar en privado y publicar expresamente. Los formularios remotos permanecen deshabilitados con «implementacion en desarrollo».

Este README es el único manual de producto. Describe el comportamiento vigente; los checkpoints históricos de cada entrega están en Git y en el plan de coordinación, fuera del producto.

## Arranque local

Requisitos: Docker Engine y Docker Compose accesibles desde la terminal, Internet para descargar las imágenes/dependencias/firmas iniciales y los puertos locales disponibles. ClamAV tiene un límite de memoria de 4 GiB en Compose; los otros servicios requieren memoria adicional.

```bash
cd /home/agus/proyectos/LaMontaña/desarrollo
./infra/iniciar-local.sh
```

El script verifica Docker, prepara la carpeta de imágenes web y crea `.env` si falta con una contraseña técnica aleatoria de PostgreSQL y agrega `SETUP_TOKEN` si no existe. Es un token para la primera instalación, no la contraseña del administrador. No publicar `.env` ni sus valores. Las imágenes se construyen y los servicios se inician con `docker compose up --build --detach --wait`.

| Servicio | Dirección predeterminada |
|---|---|
| Aplicación | http://localhost:3000 |
| Estado público del backend | http://localhost:8080/api/sistema/estado |
| Salud con conexión a base | http://localhost:8080/actuator/health/readiness |
| Buzón local Mailpit | http://localhost:8025 |
| PostgreSQL | localhost:5432; base/usuario técnicos `lamontana` |

Para cambiar puertos, agregar `FRONTEND_PORT`, `BACKEND_PORT`, `POSTGRES_PORT` o `MAILPIT_PORT` a `.env`; ver `.env.example`. No cambiar `DB_PASSWORD` si la base ya se inicializó: modificar el entorno no cambia la contraseña que conserva PostgreSQL.

```bash
docker compose ps
docker compose logs --tail=100 backend frontend
docker compose stop
docker compose up --detach --wait
```

Base, originales/vistas previas, correo, firmas antivirus e imágenes web utilizan los volúmenes `postgres_data`, `archivos_data`, `mail_data`, `antivirus_data` e `imagenes_web_data`. `docker compose down` los conserva; `docker compose down --volumes` elimina sus datos y no forma parte del arranque habitual. Para una copia recuperable hay que conservar la base, los archivos privados y la biblioteca web conjuntamente.

El acceso a Docker se verificó el 23/09/2026 después de agregar `agus` al grupo `docker` con autorización del usuario. Docker Engine 29.8.1 y Docker Compose 5.5.1 responden correctamente. Las sesiones que todavía conservan los grupos anteriores pueden ejecutar los comandos con `sg docker -c '…'`; desde este directorio, el arranque se puede invocar con `sg docker -c './infra/iniciar-local.sh'`. Cerrar la sesión de Linux y volver a entrar permite reconocer el grupo directamente.

Certificación local del 23/09/2026: `infra/iniciar-local.sh` construyó e inició los cinco servicios saludables en el proyecto aislado `lamontana-certificacion`, con puertos 23000/28080/25432/28025. Tras `docker compose down` (sin eliminar volúmenes) y repetir el script, 89 tablas de dominio y 12 archivos conservaron sus huellas; sesiones de propietario/cliente, precios, historial y PDF permanecieron accesibles. El control de salud del antivirus usa `clamdscan` con su configuración y socket explícitos: el chequeo heredado contra `localhost` no detectaba al daemon disponible. Los datos de esta certificación no se cargan en el proyecto normal.

## Primera instalación y acceso

1. Abrir `.env` localmente y copiar `SETUP_TOKEN` al formulario `/instalacion`. Ingresar nombre, apellido, correo y contraseña propios, de 12 a 128 caracteres. No hay un administrador predeterminado. Un token manual debe tener al menos 32 caracteres.
2. Entrar por `/acceso`. La primera alta se cierra definitivamente; reiniciar o conservar el token no permite crear otro propietario.
3. Verificar el correo desde Seguridad de la cuenta. Mailpit recibe los códigos de verificación, recuperación y autorización; no se envían mensajes a proveedores externos.
4. Crear sucursales y empleados. Cada empleado recibe una contraseña inicial elegida por el administrador, debe cambiarla al ingresar y sólo ve sus sucursales activas asignadas. Elegir expresamente sus permisos de revisión, producción, calidad, entrega, cierre y finanzas.
5. Crear catálogo y completar las fases del configurador. Mientras no haya configuración activa utilizable, el cliente puede acceder a su cuenta pero no cotizar un trabajo ofrecible.

El registro público `/registro` crea únicamente particulares. No inicia sesión ni verifica el correo automáticamente. `/recuperar` y `/cuenta/seguridad` permiten recuperar acceso, verificar correo y cambiar contraseña. Desactivar una cuenta o cambiar su correo revoca sus sesiones; las asignaciones y permisos se revalidan en cada operación.

## Configuración, catálogo y sucursales

**Servicios y precios** abre primero **Catálogo base**. Habilitá papeles habituales con tamaño incluido: A4, A3, A5, A6, Carta, Oficio/Folio (8½ × 13) y Legal (8½ × 14), en papel común blanco sin estucar de 75, 80 o 90 g/m². Las opciones están precargadas localmente, pero ninguna se habilita sola en una instalación nueva. Los papeles se muestran agrupados por tamaño: una tarjeta A4, A3, etc., con sus medidas una sola vez, el contador de variantes habilitadas y casillas independientes para 75, 80 y 90 g/m². Activar o desactivar un gramaje conserva los demás. **Mis papeles registrados** también se agrupa por tamaño y permite desplegar los materiales y gramajes guardados. **Agregar todos los papeles** habilita todas las variantes en una sola acción, sin duplicar las ya registradas. Después se pueden deshabilitar individualmente. La operación es atómica y puede reintentarse; no modifica papeles personalizados ni publica precios. Seleccioná el gramaje que figura en la resma y comprobá que tu equipo admita ese papel. Para otros materiales o medidas, usá **Agregar papel personalizado** e ingresá código, nombre, ancho, alto, gramaje y terminación; el sistema guarda juntos el papel y su tamaño. Los íconos dentro de Código explican su uso y formato por mouse, teclado y toque, tanto en papel personalizado como en servicio.

Después agregá servicios y continuá a **Tarifas y servicios**. En **Agregar servicio**, Nombre permite escribir libremente o elegir entre impresión blanco y negro, impresión a color, anillado, plastificado, encuadernación, corte y refilado, plegado y abrochado. Elegir una sugerencia completa nombre, código legible y tipo; todos siguen editables antes de guardar. Los códigos usados reciben un sufijo numérico sugerido (por ejemplo, `IMP-BN-2`). Escribir o editar el nombre no reemplaza los otros campos. Seleccionar una sugerencia no crea el servicio ni publica precios. Las tarifas seleccionan hojas y variantes por tarjetas; las compatibilidades de terminaciones eligen el papel con su tamaño en un selector con búsqueda. Cambiar entre pestañas conserva los formularios abiertos. Habilitar/deshabilitar en el catálogo base controla qué papeles se pueden usar en nuevas configuraciones; no cambia por sí solo los precios vigentes, las configuraciones programadas ni los pedidos. Para retirar un papel ofrecido, deshabilitá sus tarifas y guardá una nueva configuración comercial. La migración V37 conserva las combinaciones anteriores y sus identificadores.

**Tarifas y servicios** se organiza en tres pasos: tarifas de impresión, servicios ofrecidos y aplicación de la configuración. Al agregar una tarifa, primero elegí color o blanco y negro. Debajo aparecen tarjetas de las hojas habilitadas, agrupadas por tamaño, con sus variantes de gramaje/material. Podés seleccionar todas, tocar el encabezado de una tarjeta para elegir todas sus variantes o marcar sólo algunas. El buscador filtra desde dos caracteres (`A4 80`); seleccionar todas abarca las disponibles aunque haya un filtro. Una combinación de papel y color sólo pertenece a una tarifa: las usadas en otra se indican y no se sobrescriben. El lápiz junto al nombre permite renombrar la tarifa; el nombre y la selección se conservan al guardar y recargar.

Todos los importes finales se muestran en **ARS (pesos argentinos)**. Simple faz es el precio de una hoja impresa en una cara. Para **doble faz por hoja con sus dos caras impresas** elegí:

- **Ingresar precio final:** independiente de simple faz, incluso si es menor.
- **Sumar un importe:** simple faz + un monto fijo en ARS.
- **Sumar un porcentaje:** simple faz + el porcentaje indicado; escribir `10` significa sumar 10 %.

La pantalla muestra siempre ambos resultados. Las modalidades relativas se recalculan al editar simple faz. Se admiten coma o punto decimal, hasta dos decimales, sin separadores de miles, símbolos ni valores negativos. El precio doble faz se redondea a centavos por hoja antes de multiplicar. Si el documento tiene páginas impares, la última hoja se cobra a simple faz **por cada copia**, sin juntar páginas entre ejemplares. Ejemplo: simple ARS 30 y doble ARS 40; tres páginas cuestan ARS 70 por copia y ARS 140 por dos copias, antes de terminaciones.

Las tarifas anteriores conservan su cálculo por carilla y se identifican como tales. **Cambiar al cobro por hoja** propone el importe equivalente de dos caras; revisar el resultado antes de guardar, porque la hoja impar pasa a simple faz. La migración V38 agrega campos sin reescribir importes, historial ni comprobantes. Editar sólo el nombre, un servicio o la selección no convierte implícitamente una tarifa anterior.

Los íconos de información explican tarifas, servicios, importes, preparación, unidades, compatibilidades y guardado; funcionan con mouse, teclado y toque. Los servicios de impresión toman su precio de las tarifas y no agregan un segundo cargo. El nombre de un servicio no restringe el color: los modos disponibles dependen de las tarifas habilitadas. Las terminaciones suman su precio por copia completa del documento, hoja física, carilla o un cargo fijo por ítem (documento), según la base elegida. Para ofrecerlas, cada papel compatible necesita una tarifa habilitada. La preparación se ingresa en minutos; el ítem usa el máximo entre los servicios, no la suma.

Antes de aplicar, completá el motivo y elegí vigencia inmediata o programación. La programación conserva hora UTC con explicación de su equivalencia argentina. Editar o quitar tarjetas no publica cambios hasta confirmar. El borrador se conserva entre pestañas, pero recargar o salir sin guardar lo descarta. Se conservan los controles de concurrencia, reintento del mismo envío y bloqueo ante una programación pendiente. Las configuraciones consultadas muestran importes ARS y detalle de cada papel en tarjetas adaptables.

Dimensiones de referencia: [documentación oficial Brother](https://support.brother.com/g/s/id/htmldoc/mfc/cv_mfc3940dw/chne/html/GUID-746E196D-FA75-481C-9A1D-3C471CD90A96_1.html). Las opciones de gramaje son elecciones iniciales de la demo; no certifican compatibilidad universal de impresoras.

El catálogo mantiene servicios de impresión y terminaciones, tarifas por papel/color, compatibilidades y bases de cobro. Las tarifas nuevas cobran por hoja simple o doble; las anteriores mantienen su cálculo por carilla hasta conversión explícita. Impresión toma la tarifa sin sumar un precio base duplicado. Las terminaciones pueden cobrarse por copia, hoja, carilla o importe fijo por ítem. Los importes ARS se calculan con decimales exactos y hasta dos decimales; no se admiten precios negativos ni duplicados.

Los precios son globales y sus configuraciones comerciales son independientes de las versiones operativas. Se puede publicar de inmediato o programar una fecha UTC, consultar el historial y cancelar una programación con motivo. Hay como máximo una programación comercial pendiente; el servidor reconcilia las fechas al consultar y periódicamente, también después de reiniciar. No se reescribe una configuración anterior.

El dashboard administrativo muestra recuentos y listas reales por sucursal para revisión, correcciones, aprobados, producción y entregas pendientes. Los enlaces abren la bandeja con el filtro correspondiente. Una consulta fallida muestra datos no disponibles, nunca un cero inventado. El marco visual es común con empleados, pero sus menús y accesos siguen limitados por rol/sucursal. Los informes de facturación agregada, alertas automáticas y actividad global del mock no se simulan.

El configurador presenta ocho fases: configuración actual, modelo, pagos, recursos, horarios/entregas, revisión y simulación, activación y versiones/historial. Permite guardar borradores incompletos, pero bloquea la activación mientras falten datos o existan incompatibilidades.

| Modelo | Comportamiento configurable |
|---|---|
| Manual | Revisión humana; elegir sin seña o seña fija/porcentual, siempre o desde carillas/importe. La seña aplicable se exige antes de producir. |
| Pago previo total | Total acreditado y aplicado por transferencia antes de cargar el PDF. |
| Seña previa | Seña fija/porcentual acreditada y aplicada por transferencia antes de cargar; efectivo puede habilitarse para el saldo. |
| Condicional por monto, D1 | Hasta el umbral inclusive no se exige seña por esa regla. Por encima se revisa/corrige el PDF y se aprueba; después se exige seña para producir sólo si fue habilitada. Sin seña conserva revisión humana y cobra el saldo antes de entregar. |

Medios generales, instrucciones de transferencia, vigencia de cotización, umbrales, porcentajes y montos se ingresan expresamente. No existe la antigua regla fija de 200 carillas/30%. La seña fija se limita al total; porcentajes se redondean a centavos con HALF_UP, sin inventar un cargo mínimo si el resultado es cero.

Cada sucursal declara servicios e impresoras/capacidades para asignación manual. Declarar un recurso no afirma conexión con una impresora ni existencia de papel. CUPS y telemetría quedan fuera del piloto. El campo **Código** incluye un ícono de información que explica su uso interno, la unicidad y el formato recomendado (por ejemplo, `CABA-01`). La ayuda se abre al pasar el mouse, con foco de teclado o al tocar el ícono; se puede cerrar con Escape o al salir. El código admite hasta 40 letras sin tildes, números, guiones y guiones bajos, se guarda en mayúsculas y queda fijo al crear la sucursal.

Al crear o editar una sucursal se elige provincia/localidad de Argentina y se marcan los días de atención con Desde/Hasta. La zona horaria se detecta automáticamente y se muestra como GMT; el identificador IANA se conserva internamente. Cada día admite un intervalo continuo, sin cruzar medianoche. **Aplicar al resto de días habilitados** copia Desde/Hasta de ese día a los demás días abiertos y reemplaza sus horarios; los cerrados permanecen sin horas. Requiere un horario de origen válido y al menos otro día abierto. Después se puede editar cada día por separado y guardar la sucursal. Los días no marcados quedan cerrados y debe haber al menos un día abierto.

El horario habitual se guarda en la ficha de la sucursal. Al agregar su calendario en la fase 5 se copian esos horarios; un calendario existente dispone de **Usar horario guardado de la sucursal** con confirmación de reemplazo. Las franjas de retiro se conservan y deben seguir dentro de los horarios elegidos. Guardar la ficha no cambia versiones activas ni pedidos: los cambios operativos requieren guardar/revisar/activar el borrador. Las sucursales anteriores recuperan el horario activo completo cuando existe; en otro caso se indica pendiente, sin inventar horarios.

Los calendarios operativos definen los siete días y horas de atención; preparación y traslado consumen ventanas operativas de la sucursal de origen. Los ejemplos explican el cálculo, sin crear pedidos ni garantizar tiempos reales de transporte.

- **Retiro local:** franjas dentro del calendario, con cupos explícitos de pedidos completos. No hay viaje.
- **Puntos de entrega:** identidad estable, dirección y zona horaria; cada relación con un origen tiene costo y franjas/cupos propios. Su disponibilidad temporal se decide por separado, sin crear otra versión. Deshabilitarlo no cancela compromisos existentes; nuevas ofertas, confirmaciones y reprogramaciones revalidan su disponibilidad.
- **Domicilio:** zonas y cobertura territorial globales, costo y franjas/cupos configurados. La capacidad compartida entre sucursales se reserva bajo la misma identidad de zona.

Cupo cero no significa ilimitado. Las franjas no pueden superponerse; consultar una fecha o simular no reserva. Confirmar reserva atómicamente y reprogramar tras aceptación sustituye la reserva. Los pedidos cancelados/liberados o cumplidos no ocupan capacidad activa.

## Activación, versiones y recuperación

Sólo el borrador es editable. Se revisan los parámetros y condiciones actuales, se reconocen advertencias y se elige activación inmediata o programación. Activar, programar, adelantar, cancelar un borrador/programación o revertir exige las credenciales/autorizaciones propias de cada recorrido: contraseña y código de correo vinculado al propósito y contenido revisados. Cambiar contenido o condiciones relevantes obliga a revisar de nuevo.

La publicación conserva la versión anterior como histórica y registra responsable, motivo, fechas previstas/efectivas e intentos. Una falla no publica contenido parcial. Una programación persiste y se recupera al reiniciar; la fecha efectiva puede ser posterior si el servicio estaba detenido. Un intento manual fallido no se ejecuta otra vez al repetir su comando: se recupera el resultado y un intento nuevo requiere autorización nueva.

El historial permite filtros, paginación, detalle y comparación. **Usar como base** copia una activa/histórica a un borrador con las mismas identidades de recursos; exige volver a guardar/revisar las fases. No copia precios, disponibilidad temporal ni autorizaciones. No se permite abrir otro borrador mientras exista preparación/programación/activación incompatible.

**Reversión manual** publica una nueva versión con el contenido de la predecesora efectiva, validado contra las condiciones actuales. Conserva la historia y los precios comerciales actuales; no restaura una copia de la base ni modifica pedidos/pagos. Si hay una programación, su cancelación debe reconocerse y se confirma junto con la reversión. Un fallo conserva la activa y la programación anteriores.

## Página web local e imágenes

Abrí **Configurador de página web** desde el menú del propietario. El recorrido es identidad → funcionamiento local/carpeta → biblioteca → fichas → contenido/vista previa → revisión/publicación. Se puede guardar un borrador incompleto y retomarlo; guardar e importar nunca publican. La vista previa requiere sesión de propietario. **Publicar página web** exige revisar y confirmar el contenido; registra autor, fecha y versión sin cambiar precios, pedidos ni configuración operativa. Un conflicto de pestañas conserva los campos y permite consultar la versión guardada antes de decidir cómo continuar.

El arranque prepara `datos/imagenes-web/entrada/` dentro del proyecto, fuera de Git. Copiá ahí tus JPEG, PNG o WebP y elegí **Actualizar carpeta**. El panel muestra su ruta absoluta y permite entrar en subcarpetas. **Importar a biblioteca** conserva una copia; **Importar y usar** además prepara su asociación a logotipo, portada o ficha. Completá los textos alternativos, guardá y publicá expresamente cuando esté listo. Las fichas son presentación de productos/servicios; no agregan carrito ni un circuito de compra nuevo.

Cada imagen admite hasta 10 MiB y 25 millones de píxeles. Se valida el contenido real, se decodifica y se generan PNG sin metadatos y miniaturas de hasta 480 px. WebP utiliza el lector local TwelveMonkeys ImageIO 3.14.0. La entrada se monta de sólo lectura; se rechazan rutas fuera de ella, enlaces simbólicos, archivos especiales y contenido inválido. Los errores se muestran por archivo y no eliminan importaciones anteriores. Los PDF de clientes siguen separados y privados.

Reimportar el mismo contenido reutiliza la copia. Reemplazar un original genera otro recurso; renombrarlo o quitarlo no rompe la copia importada. Quitar una asociación sólo cambia el borrador: las copias se conservan para las versiones que las referencian. No hay una acción de borrado de biblioteca en esta demo. La publicación verifica la integridad de sus imágenes y conserva el sitio anterior si falla. Una respuesta incierta permite repetir la misma operación sin duplicar importaciones o publicaciones.

Para otra carpeta, definir `WEB_SOURCE_DIR=/ruta/absoluta` en `.env` o en el entorno y ejecutar otra vez `infra/iniciar-local.sh` para recrear el montaje. Debe tener permisos ordinarios de lectura y acceso para el usuario del contenedor; no usar `chmod 777`. El formulario sólo elige subcarpetas de esa entrada, no monta otras carpetas del host. La biblioteca se conserva en `imagenes_web_data` y no depende de la carpeta original.

La landing está en `http://localhost:3000/web` o `http://127.0.0.1:3000/web`, ajustando el puerto si corresponde. Usá el mismo host durante cada sesión porque las cookies de localhost y 127.0.0.1 son distintas. No requiere IP pública, CDN, fuentes remotas ni Internet durante el uso local de la web ya instalada. Los formularios de dominio/proxy/servidor remoto son informativos, grises y deshabilitados con «implementacion en desarrollo»; no guardan credenciales ni ejecutan despliegues.

## Recorrido del cliente y PDF

El cliente elige sucursal, servicio, formato, papel, páginas/copias, color/doble faz, terminaciones y modalidad/destino de entrega. El navegador puede leer datos del PDF para preparar el formulario; Spring calcula el importe y guarda la oferta. Aceptar condiciones es una acción explícita, separada de cargar, pagar y confirmar el pedido.

Una oferta vigente conserva el precio aceptado; una nueva revisión comercial afecta las nuevas ofertas. Al cargar/confirmar se revalidan configuración, elegibilidad y requisitos financieros actuales sin reescribir ese precio. El vencimiento requiere nueva cotización aceptada. Se conservan las referencias a configuración cotizada, revisión comercial y configuración de validación final.

El PDF se envía desde **Enviar PDF para analizar**, con un límite de 10 MiB. ClamAV local e inspector PDFBox verifican contenido, cifrado, acciones/adjuntos, límites e integridad, y renderizan todas las páginas. Un antivirus caído, archivo corrupto o análisis incompleto nunca habilita el documento. Los originales y PNG permanecen privados, con autorización por dueño/sucursal y sin rutas públicas. El cliente abre todas las páginas y acepta esa versión concreta; el personal no acepta en su nombre.

Cambiar contenido manteniendo páginas/opciones conserva el precio y requiere nueva aceptación de vista previa. Cambiar páginas exige recotizar con los datos inspeccionados y aceptar la nueva oferta. Una carga rechazada no sustituye el último PDF válido. Los archivos y aceptaciones históricos se conservan. Al reiniciar se registran como fallidos los análisis interrumpidos; no se simula éxito.

**Confirmar pedido** comprueba condiciones, archivos aceptados y cupo, y crea un único pedido con reserva e historia. Una respuesta perdida se recupera con la misma operación, sin duplicar pedido/reserva. El cliente consulta activos e históricos, filtros y detalle desde `/cliente/pedidos`.

## Pagos manuales, comprobantes y D2

Informar una transferencia no acredita dinero. El interno con permiso verifica la recepción real, registra referencia, importe, fecha y motivo, y después aplica expresamente el importe a una cotización aceptada. Registrar efectivo requiere su permiso específico. Referencias de transferencia son únicas en la instalación y recibos de efectivo por sucursal. No se genera una transacción bancaria ni se puede aplicar dos veces el mismo dinero.

El panel distingue recibido, aplicado, disponible, pendiente y devuelto. Sólo los medios admitidos pueden cubrir cada requisito; un pago sin aplicar no habilita carga ni producción. Las cotizaciones antecesoras conservan el historial financiero.

**Pago tardío, D2:** el pago permanece aunque venza la oferta. El cliente solicita y acepta una sucesora; un interno autorizado puede aplicar allí el dinero anterior, mostrando diferencias. No se crea un pedido automáticamente ni se cobra nuevamente lo recibido. Sólo se admite la misma oferta o su cadena de sucesoras del mismo particular, no una billetera genérica. La aplicación entre sucursales exige acceso a origen y destino.

Si el cliente abandona o existe excedente, el personal con permiso registra una devolución efectivamente realizada, con constancia y motivo. No se borra el pago ni se simula una devolución bancaria. Una devolución puede reducir cobertura y bloquear un inicio/reimpresión posterior, conservando los hechos físicos ya registrados.

Los comprobantes son PDF privados de hasta 10 MiB, con inspección, versiones, vista previa y descarga. Pueden acompañar transferencias informadas antes de acreditar; un interno autorizado puede adjuntar recibos de efectivo. No sustituyen el PDF de trabajo, no acreditan por sí solos y no son obligatorios para registrar dinero realmente verificado. No se admiten fotos en este piloto.

## Operación, correcciones y producción

Abrir **Operación → sucursal → pedido**. El detalle se organiza en secciones: resumen/historia, revisión, correcciones, producción/calidad, entrega/cierre, reprogramación, PDF y pagos según rol/permisos. Al entrar se muestra la etapa del pedido; los enlaces con fragmento conservan la sección tras recargar. Cambiar de sección conserva borradores en memoria. Recargar vuelve al estado persistido y no conserva cambios sin guardar. Desde calidad el PDF abre en otra pestaña para no perder el formulario.

El personal revisa datos y PDF, registra notas privadas, aprueba, pide correcciones o rechaza. Las notas internas no se entregan al cliente. En manual/D1 la revisión precede a la aprobación; la seña configurada se exige después para producir. Cliente o personal autorizado pueden cancelar antes de producción según las acciones disponibles; se libera cupo y se conservan dinero/historia, sin devolución automática.

La solicitud de corrección tiene mensaje para el cliente y motivo privado. El cliente responde con nuevas versiones de PDF aceptadas, contacto corregido o cotización sucesora para cambios materiales. Se conserva el pedido y su recorrido. Una nueva oferta requiere aceptación, revalidación de cupo y aplicación explícita del dinero cuando corresponda; no cambia silenciosamente el precio original.

1. **Iniciar producción manual:** exige aprobación, PDF confirmado íntegro y anticipo acreditado y aplicado. Elegir impresora declarada compatible o recurso manual fuera del inventario con justificación.
2. **Registrar fin/error/cancelación del trabajo:** son hechos explícitos. Un error permite otro intento enlazado; cancelar un trabajo no cancela automáticamente el pedido ni devuelve dinero.
3. **Controlar calidad:** impresión completa, calidad, alineación, orden y terminaciones. Aprobar exige los cinco controles, observación y confirmación; no vienen marcados. Reimpresión conserva la inspección fallida y enlaza el intento siguiente sin cambiar el precio.
4. **Completar todos los ítems:** retiro queda Listo para entregar; punto/domicilio, Listo para despachar. Superar calidad no registra viaje ni entrega.

Las operaciones exigen permisos independientes y asignación actual a la sucursal. Versiones, bloqueos y comandos idempotentes evitan trabajos/decisiones duplicados. Un empleado puede producir, controlar calidad y entregar sin permisos para modificar finanzas.

## Entrega, reprogramación y cierre

| Modalidad | Recorrido logístico |
|---|---|
| Retiro en sucursal | Listo para entregar → recepción física. Nunca En viaje. |
| Punto de entrega | Preparar envío → salida/En viaje → llegada al punto → recepción física. |
| Domicilio | Preparar envío → salida/En viaje → recepción física. |

El cliente genera el código cuando va a recibir el pedido. Se muestra sólo en esa vista y no se almacena como texto en la base ni en almacenamiento persistente del navegador. Vence en 30 minutos, admite cinco intentos incorrectos y puede renovarse después de 30 segundos, revocando el anterior. Recargar pierde el texto y permite generar otro; recuperar una emisión incierta no vuelve a revelar el secreto.

El interno valida el código presentado. La validación dura como máximo diez minutos, vinculada a actor, reserva y versión. **Confirmar entrega física** exige esa validación, recorrido/calidad completos, saldo y anticipos cubiertos, receptor, motivo y confirmación expresa. El código no cobra ni entrega por sí solo. La constancia cumple la reserva y deja el pedido Entregado.

**Cerrar pedido** es una acción separada con permiso propio. Exige entrega, dinero conciliado, excedentes devueltos e informes pendientes resueltos. Conserva responsable e importes; no crea cobros ficticios. El circuito ordinario no libera dinero aplicado de un pedido cerrado. Un cobro realmente recibido tarde sí puede registrarse y devolverse como disponible; la corrección administrativa del cierre permanece En construcción.

**Reprogramar:** un interno autorizado propone fecha/franja configurada, mensaje y motivo. El cliente acepta/rechaza y el interno puede retirar una propuesta pendiente. Hasta aceptar se conserva la reserva anterior y no se ocupa otro cupo. La aceptación revalida versión activa, destino, disponibilidad y capacidad; cambia la reserva atómicamente, revoca código/validación y conserva historia, precio, pagos y PDF. No reinicia un traslado ya ocurrido. Sólo cambia fecha/franja; modificar trabajo/destino corresponde a corrección material. No se acepta mientras esté en corrección, cancelado, rechazado, entregado o cerrado. Una propuesta obsoleta puede rechazarse o retirarse.

## Alcance excluido y referencia visual

CUPS/agentes, telemetría y automatización de impresoras, pasarelas/conciliación bancaria automática, aplicación mobile, seriales/licencias, reclamos posteriores, empresas/cuenta corriente y conversión DOCX/ODT están **En construcción**. No se simulan mapas, mensajes externos ni movimientos físicos. Producción manual, D2 y correcciones operativas sí son parte funcional de v0.1.

Referencias: DER v3.2 y mocks/flujos de `main` en `3983058e`, más los contratos/mocks de producto configurable en `56f89d40`. Las decisiones posteriores del usuario prevalecen. Se mantienen navegación lateral azul, cabeceras, tarjetas y acciones de Alejandro; el asistente conserva sus fases. El estado vacío reemplaza los datos de ejemplo. Los formularios agregan sucursal, capacidades y cupos necesarios para la operación real. Tablas se desplazan internamente y paneles se apilan en pantallas angostas.

El PDF usa un visor privado compartido; las etapas del pedido se navegan por secciones. El código secreto sólo aparece al cliente, adaptando el ejemplo que lo mostraba al administrador. No hay mock específico del diálogo de reprogramación en main; sigue el contrato del DER §7.6. MC-ADM-001 guió el dashboard con tarjetas de recuentos, cuatro paneles de pedidos y columna de acciones/disponibilidad; se adapta a sucursales y estados efectivos, sin copiar indicadores de facturación o alertas ficticios. MC-ADM-007 guía los tres paneles de calidad, ahora legibles también para empleados. La marca se reconstruyó como SVG compartido desde la silueta, nieve y sol/arco de los mocks; las fuentes consultadas no incluían un logo independiente. El paisaje se adapta a vector decorativo local, sin imágenes remotas. MC-CLI-001 guía acceso/registro/recuperación: cabecera y pie azul, tarjeta central, acción naranja y alta en tostado, conservando etiquetas visibles y errores accesibles. MC-CLI-002 guía el inicio cliente: pedido activo más reciente, últimos cinco hechos registrados, últimos cinco pedidos, recuentos reales y entrega del pedido actual. Los pedidos entregados pendientes de cierre cuentan en curso. La cotización conserva su ruta propia. MC-CLI-003/004/005/006 mantienen carga, opciones, resumen azul y seguimiento; la separación de aceptar oferta, acreditar, inspeccionar PDF y confirmar responde a las decisiones D1/D2. No se incluyen WhatsApp, contacto externo, débito, revisión universal, promesas de tiempo ni indicadores inventados de los ejemplos.

## Desarrollo, datos y pruebas

- `backend/`: Java 17, Spring Boot 4.1.1, Security, JPA, Flyway, PostgreSQL 18.6. El esquema se modifica mediante migraciones V1–V38; Hibernate sólo valida.
- `frontend/`: Next.js 16.3.5, React 19.3.0, TypeScript, pnpm 11.19.0 y Node 22/24. App Router, proxy de rutas permitidas y salida standalone.
- `infra/`, Dockerfiles y `compose.yaml`: servicios, puertos de loopback, salud y volúmenes. Backend/frontend ejecutan como usuarios sin privilegios.

```bash
mvn -f backend/pom.xml verify
cd frontend
pnpm install --frozen-lockfile
pnpm typecheck
pnpm build
```

Las pruebas utilizan PostgreSQL real aislado mediante una dependencia de test; no necesitan Docker ni usan H2 o la base de la demo. Requieren un usuario normal y un sistema compatible con esos binarios; las primeras descargas necesitan Internet. El inspector PDF es real; los tests automatizados de backend usan un servidor controlado del protocolo antivirus para respuestas limpias/rechazadas/incompletas. Los recorridos de navegador también se comprobaron con ClamAV real y firmas oficiales, incluido rechazo de EICAR en entregas anteriores.

Para desarrollo sin contenedores se necesitan PostgreSQL, SMTP local y ClamAV con firmas actualizadas. Configurar `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SETUP_TOKEN`, `FILES_DIR`, `SMTP_HOST`/`SMTP_PORT` y `CLAMAV_HOST`/`CLAMAV_PORT`. Para imágenes web, `WEB_SOURCE_DIR` es la entrada real del proceso, `WEB_SOURCE_HOST` su descripción visible y `WEB_LIBRARY_DIR` el almacenamiento persistente de copias. En Docker esas rutas internas las establece Compose. Empaquetar el backend y definir `PDF_INSPECTOR_JAR` como ruta absoluta al mismo jar al arrancar con `java -jar`. El frontend usa `BACKEND_INTERNAL_URL` y `pnpm dev`. No deshabilitar el antivirus para habilitar cargas. El piloto presupone una sola instancia del backend.

Las contraseñas usan PBKDF2 con sal; sesiones JDBC persistentes, cookie HttpOnly/SameSite=Lax y 30 minutos de inactividad. Las mutaciones requieren CSRF, autorización vigente y controles de versión/operación; ningún secreto se guarda en almacenamiento del navegador. Los códigos de correo y entrega tienen límites de intentos/reenvío. El proxy no publica el token de instalación ni directorios privados. Archivos tienen controles de integridad; la inspección se ejecuta en proceso separado con memoria/tiempo limitados. El piloto es local; sus cookies HTTP y correo capturado no constituyen un despliegue público.

Adaptaciones principales del DER: revisiones comerciales separadas; sesiones JDBC; disponibilidad de puntos fuera del snapshot; anticipos y aplicaciones a cotizaciones para D2; archivos/aceptaciones versionados; pedido con tres referencias de configuración/precio y PDF confirmado; trabajos/inspecciones ligados a la versión efectiva corregida; logística separada de estado general; reservas históricas y reprogramación aceptada. El módulo web agrega borrador, biblioteca, publicaciones inmutables, referencias y comprobantes idempotentes separados de la operación. Historial, pagos y contenidos publicados no se reescriben retroactivamente.

**Certificación de la revisión 6, 23/09/2026:** Maven verify aprobó 165 pruebas en 22 clases, sin fallos/errores/omisiones; build Docker de frontend y TypeScript correctos. Diez pruebas específicas cubren permisos de la web, versiones/reintentos/concurrencia, publicación, JPEG/PNG/WebP, límites, enlaces/rutas, integridad y reinicio. Los recorridos con ClamAV real completaron un pedido nuevo con corrección D1, recepción/aplicación de dinero, producción, calidad, código, entrega y cierre, conservando sitio y configuración operativa.

La instalación aislada nueva inició los cinco servicios y migró desde cero a V35. Se comprobaron los accesos antes/después del propietario y la primera publicación sin activar pedidos. La instalación normal se actualizó desde V33 sin cambiar sus datos anteriores ni incorporar contenido de pruebas. En certificación, recrear contenedores conservó las huellas de 91 tablas de negocio y 38 archivos, incluidos imágenes web y PDF privados. Las tablas de sesiones/eventos se verificaron por acceso persistente, porque las pruebas de login modifican sus registros. Se recuperaron borrador, publicación, imágenes, pedidos históricos y sesiones.

La landing funcionó en localhost y 127.0.0.1 sin solicitudes externas, con los contenedores en redes internas sin ruta de salida. La prueba usó imágenes/dependencias/firmas ya instaladas; no afirma que la primera descarga funcione sin Internet. Las evidencias originales de modelos manual/seña/pago previo, D2, permisos por sucursal, reimpresión y logística siguen identificadas por su fecha y versión en el plan; no se presentan como recorridos íntegramente repetidos en v6.

## Recorrido manual del piloto

1. Iniciar con el comando de arriba y crear el propietario desde `/instalacion`. Completar correo, dos sucursales, empleados y permisos expresos; usar Mailpit para los códigos.
2. Ingresar el catálogo y las tarifas propias. Completar fases2–6 con recursos manuales y calendarios/franjas/cupos para las tres modalidades. Simular, revisar advertencias y activar con contraseña/código.
3. Registrar un particular, acceder y preparar un PDF: elegir sucursal, opciones, pago y entrega. Aceptar la oferta. En pago previo/seña pura, acreditar y aplicar primero desde el usuario autorizado; en D1, revisar/corregir antes de la seña.
4. Enviar el PDF, abrir y aceptar sus páginas, revisar las condiciones finales y confirmar. Recargar y consultar el pedido desde Inicio o Mis pedidos.
5. Entrar con un empleado de la sucursal responsable, revisar y aprobar. Si corresponde, pedir corrección y esperar PDF/respuesta/revisión nuevos. Registrar y aplicar el anticipo necesario desde un usuario con permiso financiero.
6. Registrar inicio/fin manuales; inspeccionar los cinco controles de calidad. Una reimpresión conserva el intento anterior. El resultado listo todavía no constituye una entrega.
7. Completar el recorrido de retiro, punto o domicilio. Conciliar el saldo, generar el código desde el cliente, validarlo desde el interno y registrar entrega física. Cerrar por separado después de resolver excedentes y movimientos pendientes.
8. Abrir Configurador de página web, completar los seis pasos, importar/asociar imágenes y guardar. Revisar en escritorio/móvil y publicar. Comprobar `/web` sin sesión; editar un borrador posterior no debe cambiar la publicación actual.
9. Detener e iniciar con los mismos volúmenes; comprobar acceso, configuración, pedido, historia y PDF. Para explorar D2, conservar una oferta vencida con dinero y aceptar una sucesora antes de aplicar ese dinero; registrar devolución si el cliente abandona.

Los datos sintéticos pertenecen exclusivamente a proyectos aislados de certificación, detenidos al cerrar la verificación y con sus volúmenes conservados. El arranque normal de `lamontana` comienza sin usuarios ni operaciones de ejemplo. No ejecutar `down --volumes` para reiniciar.

Revisión responsive final: 53 vistas/estados generales más los seis pasos del configurador web y la landing, en 320, 360, 390, 600, 768, 820, 1024 y 1440 px (480 comprobaciones). Sin desbordes de página ni errores JavaScript inesperados. Se comprobaron navegación de ida/vuelta, cambios sin guardar, autorizaciones pendientes, tablas/galerías por teclado, menú por toque y diálogos a 320×568, 390×700 y 844×390. Las tablas y las vistas previas grandes mantienen desplazamiento interno; los campos de una línea permiten desplazar texto largo. Se revisaron capturas y se corrigieron menú oculto, espacios de formularios, etiquetas accesibles, fechas que causaban diferencias de renderizado y selección visible de fases. Son pruebas en Chromium con emulación de tamaños/toque; no certifican dispositivos físicos ni Safari/Firefox.


### Revisión posterior: sucursales y hora local (23/09/2026)

La demo contempla Argentina: 23 provincias y CABA, con catálogo local basado en [IANA tzdb](https://data.iana.org/time-zones/tzdb/zone1970.tab), sin geocodificación ni conexión externa. El servidor resuelve la zona a partir de la provincia, suficiente para las localidades argentinas cubiertas; no toma como autoridad una zona horaria enviada desde el formulario. Los alias CABA, Capital Federal y nombres sin acentos se reconocen. Para compatibilidad con integraciones v6, la API anterior sin horario de alta puede conservar su zona IANA explícita; el formulario nuevo exige provincia reconocida y horario completo.

V36 agrega `sucursal_horario_atencion` y sólo copia los calendarios activos completos existentes, sin modificar sucursales, borradores ni versiones publicadas. Pruebas de esta revisión: 9 pruebas backend de organización, calendarios, detección y migración V35→V36; compilación/tipado de frontend; alta/edición por navegador, validaciones, recarga y copia explícita al borrador; 14 comprobaciones responsive entre 320 y 1440 px. Las cifras de certificación v6 anteriores corresponden a esa entrega, no a una repetición íntegra en esta revisión.


### Revisión posterior: catálogo base guiado (23/09/2026)

Plan revisión 9. Pestañas reales en orden Catálogo base, Tarifas y servicios, Historial; formularios conservados al cambiar de pestaña. Selección local de 21 opciones de papel común con tamaño y gramaje incluidos; alta personalizada atómica y selección del papel completo en tarifas y compatibilidades. Los códigos tienen ayuda accesible. V37 conserva los pares existentes sin alterar revisiones publicadas ni pedidos; deshabilitar una selección afecta a las próximas revisiones.

Verificación: 12 pruebas backend en cinco clases (catálogo, programación, migraciones y cálculo); build de producción y tipado frontend; 11 casos funcionales y 16 comprobaciones responsive del catálogo entre 320 y 1440 px, sin errores JavaScript. Regresión adicional de la ayuda de sucursales: 11 interacciones y cuatro anchos. Chromium con emulación de tamaño/toque. Instalación normal migrada a V37, cinco servicios saludables y huellas de nueve tablas anteriores conservadas; sin datos de prueba incorporados.


### Revisión posterior: agregar todos los papeles (23/09/2026)

Plan revisión 10. El botón **Agregar todos los papeles** permite habilitar de una vez todos los tamaños y sus variantes de gramaje precargadas. Muestra progreso, bloquea envíos mientras guarda y queda deshabilitado cuando todos están habilitados. Conserva el control individual, los papeles personalizados y las revisiones comerciales. No requiere migración de base.

Verificación: tres pruebas de integración de catálogo en dos clases, incluyendo permisos/CSRF, rollback ante fallo intermedio, reintentos sin duplicados y persistencia. Build/tipado frontend correctos; siete casos de navegador y cuatro anchos (320/390/768/1440), sin errores JavaScript. Pruebas en entorno aislado.


### Refinamiento visual: papeles agrupados por tamaño (23/09/2026)

Cada papel precargado tiene una sola tarjeta por tamaño, con sus dimensiones, contador de variantes habilitadas y casillas independientes por gramaje. Los siete tamaños reúnen las 21 variantes, sin filtro global de gramaje ni tarjetas repetidas. Mis papeles registrados se agrupa por dimensiones y despliega materiales, códigos y gramajes; dos materiales del mismo gramaje siguen siendo variantes independientes. Agregar todos los papeles conserva su funcionamiento y cuenta variantes con claridad.

Cambio exclusivo de frontend y documentación: se conservan identificadores, APIs y esquema V37. Cada casilla usa el par tamaño/material original; no modifica otras variantes ni publica precios. Verificación: build/tipado de producción, nueve casos de navegador (incluidos error, recarga, teclado y toque) y ocho comprobaciones responsive en 320/390/768/1440 px. Cero errores JavaScript; capturas móvil y escritorio revisadas. Pruebas sólo en entorno aislado.


### Refinamiento UX: sugerencias de servicios (23/09/2026)

Agregar servicio ofrece un Nombre de escritura libre con ocho sugerencias locales. Elegir una completa nombre, código legible y tipo; todos siguen editables antes de guardar. Los códigos ya usados reciben el siguiente sufijo numérico disponible. Editar el nombre conserva el código y tipo manuales; seleccionar otra sugerencia conserva la descripción. No hay altas automáticas ni cambios en tarifas o revisiones comerciales.

Verificación: build/tipado de producción, 8 casos funcionales de navegador y 4 comprobaciones responsive en 320/390/768/1440 px. Incluye selección por teclado/toque, búsqueda sin tildes, edición libre, conflicto real de código con corrección, guardado/persistencia y limpieza del formulario, conservación del borrador entre pestañas y de precios/papeles. Cero errores JavaScript; capturas móvil y escritorio revisadas. Regresión del campo Código en sucursales: 11 interacciones y cuatro anchos. Frontend y documentación únicamente, sin cambios de API ni esquema V37. Pruebas con datos sintéticos en instancia aislada.


### Refinamiento UX: editor de tarifas y servicios (23/09/2026)

Editor reorganizado en tarjetas a ancho completo, tres pasos, campos alineados y buscadores por tamaño, gramaje, nombre y código. Las revisiones consultadas también usan tarjetas. Precios y recargos visibles en ARS, entrada decimal con coma/punto validada, ejemplos simple/doble faz y ayudas por hover, teclado y toque. Se explican las cuatro unidades de terminación, compatibilidades, preparación y efecto del guardado. No se agregó una regla de descuentos ni se alteró el cálculo comercial.

Validación: build de producción y TypeScript, nueve casos funcionales de navegador y trece comprobaciones de presentación; pasada visual posterior de doce comprobaciones a 320/390/768/1440 px y alineación de inputs. Sin desplazamiento horizontal en tarjetas, buscadores o ayudas; cero errores JavaScript. Guardado y recarga reales conservan importes; conflicto concurrente permite comparar y conservar valores; respuesta perdida reutiliza la operación sin duplicar; programación bloquea campos y conserva la vigencia previa. Pruebas en instancia aislada, sin datos sintéticos en la instalación normal. Backend y esquema V37 sin cambios; no se repitieron suites backend.


### Terminología de configuraciones comerciales (23/09/2026)

La interfaz llama **configuración comercial** al conjunto guardado de tarifas y servicios. Encabezados, acciones, historial, programación, confirmaciones, ayudas y referencias del configurador usan este nombre. Cada configuración conserva su número y vigencia; la configuración operativa sigue siendo independiente. «Revisión» se mantiene cuando significa comprobar datos o revisar un pedido. El cambio es de presentación: se conservan las rutas, contratos, identificadores, historial y datos existentes.


### Tarifas agrupadas y precios finales por hoja (23/09/2026)

Plan revisión 11. Selección por color y tarjetas de hojas/variantes, selección total, nombres editables con lápiz y tres formas de definir doble faz: precio final, adicional fijo o porcentaje sobre simple faz. Cotizaciones por pares de páginas, con la última hoja impar a simple faz en cada copia. El desglose explica las hojas y conserva un único servicio de impresión por ítem. V38 conserva los datos anteriores y su cálculo por carilla hasta conversión explícita.

Verificación: 16 pruebas backend pertinentes de catálogo, programación, migración, precios y cotizaciones reales; once casos monetarios frontend y controles de agrupación/duplicados. Build y tipos correctos. Diez casos funcionales de navegador y doce comprobaciones de presentación a 320/390/768/1440 px, teclado y toque emulados, sin errores JavaScript ni desbordamientos del formulario. Demo local migrada a V38, cinco servicios saludables y huellas conservadas en doce tablas de negocio.


### Ajustes de configuración · revisión 12

- En pagos se elige explícitamente usar o no seña. El modelo por monto mantiene su umbral de revisión humana aunque no haya anticipo y permite efectivo sin transferencia en ese caso. Si se desactiva «Seña previa», se avisa y al guardar cambia atómicamente a control manual; pago total previo mantiene su requisito.
- Los importes financieros muestran $ / ARS y usan formato argentino: `1.500,50` o `1500,50`, con punto para miles y coma para centavos. Se transforman a decimales exactos para el backend. Los porcentajes indican `%` y las cantidades de carillas conservan su unidad.
- El alta/edición de impresoras permite seleccionar todos los formatos o quitar la selección, además de marcar cada uno. La asignación automática sigue deshabilitada con un distintivo amarillo «En construcción».
- En fase 5, el selector muestra tanto sucursales pendientes como calendarios agregados. Cambiar de sucursal conserva los datos que se están editando. Los botones de puntos y zonas guardan primero los cambios pendientes y navegan sólo después de confirmar el guardado; los reintentos conservan operación y destino sin duplicar cambios. Los errores mantienen el formulario.
- El calendario se adapta al ancho disponible con fichas por día cuando no cabe la tabla. Guardar permite continuar el borrador: la activación sigue siendo explícita. No se modifica ninguna configuración existente al actualizar la aplicación ni se agregan migraciones.

Verificación de revisión 12: nueve pruebas backend de pagos, entrega y activación/cotización; 18 casos de formato monetario; diez escenarios de navegador y trece comprobaciones de presentación a 320/390/768/1440 px. Flujo por toque móvil, dos sucursales, puntos, zonas, retiro, guardado fallido y reintento idempotente comprobados. Sin errores JavaScript; build y tipos correctos. Instalación normal con cinco servicios saludables y huellas conservadas en dieciséis tablas; sin migraciones nuevas.


### Selección completa de compatibilidades de servicios

En Tarifas y servicios, cada terminación (por ejemplo, anillado) ofrece **Agregar todos los papeles**. Incluye todas las variantes habilitadas de Catálogo base en un solo clic, conserva las selecciones existentes y evita duplicados o filas vacías. Después se puede quitar o cambiar cualquier papel individualmente. El cambio afecta sólo al servicio elegido y se aplica al guardar la configuración comercial. Se conserva el máximo de 300 compatibilidades y el requisito de una tarifa habilitada para cada papel si se habilita la terminación.


### Menú de escritorio y confirmación de tarifas

El menú lateral de escritorio ocupa el alto de la ventana y ajusta espacios a la altura disponible. Las opciones de navegación permanecen accesibles sin desplazamiento vertical interno; en ventanas de poca altura se compacta la marca y se omite el aviso decorativo de funciones futuras. La navegación móvil conserva su disposición horizontal.

Las tarifas guardadas se presentan cerradas, con resumen de tamaños, variantes, importes y estado vigente/programado. **Editar tarifa** abre el formulario; **Aplicar tarifa al borrador** valida y cierra esa edición. Las modificaciones quedan identificadas como **Pendiente de guardar** hasta confirmar **Guardar nueva configuración**, que publica el conjunto de tarifas y servicios o lo programa. Cancelar restaura la tarifa previa; no se puede publicar mientras haya una tarifa abierta sin confirmar. Salir con cambios pendientes muestra la advertencia de navegación.

Verificación: build de producción y tipos correctos; ocho escenarios de navegador (edición, validación, cancelación, aplicación, publicación, regreso, recarga y reintento sin duplicados). Ocho tamaños de escritorio entre 761×480 y 1920×1080, incluida altura de 400 px, con todos los enlaces visibles y sin scroll propio del menú. Resúmenes y editor comprobados a 320/390/768/1440 px, sin desbordamiento horizontal ni errores JavaScript. Pruebas con datos sintéticos sólo en instancia aislada; backend, contratos y migraciones sin cambios.


### Confirmación individual de servicios

Cada servicio ofrecido tiene su propio botón **Guardar servicio al borrador**. Valida nombre, precio, unidad, preparación y compatibilidades; cierra su formulario y permite agregar otro. Los servicios preparados aparecen como resúmenes con estado Pendiente de guardar; los ya publicados se muestran vigentes o programados. **Editar servicio** vuelve a abrirlo y **Cancelar edición de servicio** restaura los valores anteriores (o descarta el alta todavía sin confirmar). La selección completa de papeles permanece disponible.

Se pueden preparar varias tarifas y servicios antes de **Guardar nueva configuración**, que publica o programa el conjunto. No se puede publicar con un servicio o tarifa abierto sin confirmar. El borrador se conserva entre pestañas y salir con cambios muestra una advertencia; no persiste si se abandona o recarga sin guardar la configuración.

Verificación: build de producción y tipos correctos; siete escenarios de navegador con tres servicios confirmados antes de una única publicación. Validación, cancelación, edición, selección completa de papeles, conservación entre pestañas, advertencia al salir, recarga y reintento sin duplicados comprobados. Ocho revisiones de presentación del editor y los resúmenes a 320/390/768/1440 px, sin desbordamientos ni errores JavaScript. Pruebas sólo en instancia aislada; backend, API y migraciones sin cambios.
