# La Montaña · piloto v0.1

Aplicación local con Spring Boot, Next.js y PostgreSQL para una imprenta con varias sucursales. El administrador configura el sistema desde cero; los clientes particulares cotizan PDF y el personal completa revisión, producción manual, calidad, entrega y cierre. No hay cuentas, sucursales, precios, reglas comerciales ni pedidos de muestra precargados. La implementación de Supabase quedó sustituida y permanece solamente en el historial Git.

## Estado actual · v0.1 verificada

Los once criterios de aceptación del plan v5 están verificados para el piloto local. La suite vigente aprobó 154 pruebas con PostgreSQL real; el frontend compiló y pasó el tipado. Los recorridos de navegador cubren empleados de dos sucursales, modelos manual/condicionales, corrección D1, recuperación D2, producción/calidad y las tres modalidades hasta cierre. Docker Compose se construyó desde una instalación vacía y conservó datos, archivos y sesiones al recrear los cinco contenedores. La revisión visual incluye escritorio de1536px y móvil de390px frente a los mocks, con las adaptaciones de alcance detalladas abajo.

Este README es el único manual de producto. Describe el comportamiento vigente; los checkpoints históricos de cada entrega están en Git y en el plan de coordinación, fuera del producto.

## Arranque local

Requisitos: Docker Engine y Docker Compose accesibles desde la terminal, Internet para descargar las imágenes/dependencias/firmas iniciales y los puertos locales disponibles. ClamAV tiene un límite de memoria de 4 GiB en Compose; los otros servicios requieren memoria adicional.

```bash
cd /home/agus/proyectos/LaMontaña/desarrollo
./infra/iniciar-local.sh
```

El script verifica Docker, crea `.env` si falta con una contraseña técnica aleatoria de PostgreSQL y agrega `SETUP_TOKEN` si no existe. Es un token para la primera instalación, no la contraseña del administrador. No publicar `.env` ni sus valores. Las imágenes se construyen y los servicios se inician con `docker compose up --build --detach --wait`.

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

Base, originales/vistas previas, correo y firmas antivirus utilizan los volúmenes `postgres_data`, `archivos_data`, `mail_data` y `antivirus_data`. `docker compose down` los conserva; `docker compose down --volumes` elimina sus datos y no forma parte del arranque habitual. Para una copia recuperable hay que conservar la base y los archivos privados conjuntamente.

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

**Servicios y precios** mantiene formatos, papeles, servicios de impresión y terminaciones, tarifas por formato/papel/color, compatibilidades y bases de cobro. El recargo doble faz es por carilla impresa; impresión toma la tarifa sin sumar un precio base duplicado. Las terminaciones pueden cobrarse por copia, hoja, carilla o importe fijo por ítem. Los importes ARS se calculan con decimales exactos y hasta dos decimales; no se admiten precios negativos ni duplicados.

Los precios son globales y sus revisiones comerciales son independientes de las versiones operativas. Se puede publicar de inmediato o programar una fecha UTC, consultar el historial y cancelar una programación con motivo. Hay como máximo una programación comercial pendiente; el servidor reconcilia las fechas al consultar y periódicamente, también después de reiniciar. No se reescribe una revisión anterior.

El dashboard administrativo muestra recuentos y listas reales por sucursal para revisión, correcciones, aprobados, producción y entregas pendientes. Los enlaces abren la bandeja con el filtro correspondiente. Una consulta fallida muestra datos no disponibles, nunca un cero inventado. El marco visual es común con empleados, pero sus menús y accesos siguen limitados por rol/sucursal. Los informes de facturación agregada, alertas automáticas y actividad global del mock no se simulan.

El configurador presenta ocho fases: configuración actual, modelo, pagos, recursos, horarios/entregas, revisión y simulación, activación y versiones/historial. Permite guardar borradores incompletos, pero bloquea la activación mientras falten datos o existan incompatibilidades.

| Modelo | Comportamiento configurable |
|---|---|
| Manual | Revisión humana; elegir sin seña o seña fija/porcentual, siempre o desde carillas/importe. La seña aplicable se exige antes de producir. |
| Pago previo total | Total acreditado y aplicado por transferencia antes de cargar el PDF. |
| Seña previa | Seña fija/porcentual acreditada y aplicada por transferencia antes de cargar; efectivo puede habilitarse para el saldo. |
| Condicional por monto, D1 | Hasta el umbral inclusive no se exige seña por esa regla. Por encima se revisa/corrige el PDF y se aprueba; después se exige la seña configurada para producir. |

Medios generales, instrucciones de transferencia, vigencia de cotización, umbrales, porcentajes y montos se ingresan expresamente. No existe la antigua regla fija de 200 carillas/30%. La seña fija se limita al total; porcentajes se redondean a centavos con HALF_UP, sin inventar un cargo mínimo si el resultado es cero.

Cada sucursal declara servicios e impresoras/capacidades para asignación manual. Declarar un recurso no afirma conexión con una impresora ni existencia de papel. CUPS y telemetría quedan fuera del piloto. Los calendarios definen los siete días, zona IANA y horas de atención; preparación y traslado consumen ventanas operativas de la sucursal de origen. Los ejemplos explican el cálculo, sin crear pedidos ni garantizar tiempos reales de transporte.

- **Retiro local:** franjas dentro del calendario, con cupos explícitos de pedidos completos. No hay viaje.
- **Puntos de entrega:** identidad estable, dirección y zona horaria; cada relación con un origen tiene costo y franjas/cupos propios. Su disponibilidad temporal se decide por separado, sin crear otra versión. Deshabilitarlo no cancela compromisos existentes; nuevas ofertas, confirmaciones y reprogramaciones revalidan su disponibilidad.
- **Domicilio:** zonas y cobertura territorial globales, costo y franjas/cupos configurados. La capacidad compartida entre sucursales se reserva bajo la misma identidad de zona.

Cupo cero no significa ilimitado. Las franjas no pueden superponerse; consultar una fecha o simular no reserva. Confirmar reserva atómicamente y reprogramar tras aceptación sustituye la reserva. Los pedidos cancelados/liberados o cumplidos no ocupan capacidad activa.

## Activación, versiones y recuperación

Sólo el borrador es editable. Se revisan los parámetros y condiciones actuales, se reconocen advertencias y se elige activación inmediata o programación. Activar, programar, adelantar, cancelar un borrador/programación o revertir exige las credenciales/autorizaciones propias de cada recorrido: contraseña y código de correo vinculado al propósito y contenido revisados. Cambiar contenido o condiciones relevantes obliga a revisar de nuevo.

La publicación conserva la versión anterior como histórica y registra responsable, motivo, fechas previstas/efectivas e intentos. Una falla no publica contenido parcial. Una programación persiste y se recupera al reiniciar; la fecha efectiva puede ser posterior si el servicio estaba detenido. Un intento manual fallido no se ejecuta otra vez al repetir su comando: se recupera el resultado y un intento nuevo requiere autorización nueva.

El historial permite filtros, paginación, detalle y comparación. **Usar como base** copia una activa/histórica a un borrador con las mismas identidades de recursos; exige volver a guardar/revisar las fases. No copia precios, disponibilidad temporal ni autorizaciones. No se permite abrir otro borrador mientras exista preparación/programación/activación incompatible.

**Reversión manual** publica una nueva versión con el contenido de la predecesora efectiva, validado contra las condiciones actuales. Conserva la historia y los precios comerciales actuales; no restaura una copia de la base ni modifica pedidos/pagos. Si hay una programación, su cancelación debe reconocerse y se confirma junto con la reversión. Un fallo conserva la activa y la programación anteriores.

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

- `backend/`: Java 17, Spring Boot 4.1.1, Security, JPA, Flyway, PostgreSQL 18.6. El esquema se modifica mediante migraciones V1–V33; Hibernate sólo valida.
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

Para desarrollo sin contenedores se necesitan PostgreSQL, SMTP local y ClamAV con firmas actualizadas. Configurar `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SETUP_TOKEN`, `FILES_DIR`, `SMTP_HOST`/`SMTP_PORT` y `CLAMAV_HOST`/`CLAMAV_PORT`. Empaquetar el backend y definir `PDF_INSPECTOR_JAR` como ruta absoluta al mismo jar al arrancar con `java -jar`. El frontend usa `BACKEND_INTERNAL_URL` y `pnpm dev`. No deshabilitar el antivirus para habilitar cargas. El piloto presupone una sola instancia del backend.

Las contraseñas usan PBKDF2 con sal; sesiones JDBC persistentes, cookie HttpOnly/SameSite=Lax y 30 minutos de inactividad. Las mutaciones requieren CSRF, autorización vigente y controles de versión/operación; ningún secreto se guarda en almacenamiento del navegador. Los códigos de correo y entrega tienen límites de intentos/reenvío. El proxy no publica el token de instalación ni directorios privados. Archivos tienen controles de integridad; la inspección se ejecuta en proceso separado con memoria/tiempo limitados. El piloto es local; sus cookies HTTP y correo capturado no constituyen un despliegue público.

Adaptaciones principales del DER: revisiones comerciales separadas; sesiones JDBC; disponibilidad de puntos fuera del snapshot; anticipos y aplicaciones a cotizaciones para D2; archivos/aceptaciones versionados; pedido con tres referencias de configuración/precio y PDF confirmado; trabajos/inspecciones ligados a la versión efectiva corregida; logística separada de estado general; reservas históricas y reprogramación aceptada. Historial, pagos y contenidos publicados no se reescriben retroactivamente.

**Última evidencia integrada:** Maven verify con 154 pruebas sin fallos/errores/omisiones y build/tipado de frontend correctos. Navegador desde instalación vacía: dos sucursales y empleados, permisos, PDF real, tres pedidos D1 hasta cierre, calidad/reimpresión, validaciones y recuperación de respuestas perdidas sin duplicados; vistas de 1536 y 390 px sin desbordes ni errores JS/5xx en esos recorridos. Reinicio de PostgreSQL/Spring/Next/Mailpit/ClamAV sobre los mismos datos: huellas idénticas de 17 tablas y 9 archivos, sesiones de propietario/empleado/cliente vigentes, precios/historia/PDF conservados. Los datos de prueba pertenecen a un entorno aislado y no se incorporan a la instalación. La certificación adicional con Compose conserva 89 tablas y 12 archivos, incluidos pagos y pedidos D1/D2, tras recrear contenedores. Backend idéntico al que pasó la suite154; no se repitieron pruebas sin cambios.

Verificación de presentación de la entrega 37: dashboard contrastado con cinco recuentos de la API, filtros persistidos al abrir la bandeja, sucursal vacía y recuperación de una consulta fallida; organización, operación y calidad en 1536/390 px. Empleado sin enlaces administrativos, API financiera/sucursal ajena rechazadas, cliente excluido de administración y borrador de calidad conservado al abrir el PDF en otra pestaña. No cambian contratos ni reglas de negocio.

Regresión integrada D2/versiones en Compose: ofertas anteriores y pedidos conservan precio tras una nueva tarifa; el cambio operativo revalida la carga. Un vencimiento real conserva dinero previo y tardío; la oferta sucesora exige aceptación y aplicación explícitas, muestra la diferencia y permite confirmar el PDF. Se registraron devoluciones por excedente y abandono, y el pedido llegó a entrega/cierre con saldo cero. No hubo cambios de backend desde la suite de 154 pruebas.

Cierre de modelos y presentación: manual sin anticipo, seña pura con upload bloqueado y corrección D1 antes del cobro llegaron a producción/calidad mediante UI; recibir dinero sin aplicarlo no habilita. La API rechaza eludir ambos bloqueos. El frontend final se compiló en su imagen Docker; acceso, registro corregible, recuperación por Mailpit, dashboard con datos/vacío/error recuperable y enlaces se comprobaron en navegador. La última pasada no detectó errores JS ni desbordes; el503 inducido para comprobar recuperación fue esperado. Capturas, resultados y matriz completa se guardan con el plan de coordinación fuera del producto.

## Recorrido manual del piloto

1. Iniciar con el comando de arriba y crear el propietario desde `/instalacion`. Completar correo, dos sucursales, empleados y permisos expresos; usar Mailpit para los códigos.
2. Ingresar el catálogo y las tarifas propias. Completar fases2–6 con recursos manuales y calendarios/franjas/cupos para las tres modalidades. Simular, revisar advertencias y activar con contraseña/código.
3. Registrar un particular, acceder y preparar un PDF: elegir sucursal, opciones, pago y entrega. Aceptar la oferta. En pago previo/seña pura, acreditar y aplicar primero desde el usuario autorizado; en D1, revisar/corregir antes de la seña.
4. Enviar el PDF, abrir y aceptar sus páginas, revisar las condiciones finales y confirmar. Recargar y consultar el pedido desde Inicio o Mis pedidos.
5. Entrar con un empleado de la sucursal responsable, revisar y aprobar. Si corresponde, pedir corrección y esperar PDF/respuesta/revisión nuevos. Registrar y aplicar el anticipo necesario desde un usuario con permiso financiero.
6. Registrar inicio/fin manuales; inspeccionar los cinco controles de calidad. Una reimpresión conserva el intento anterior. El resultado listo todavía no constituye una entrega.
7. Completar el recorrido de retiro, punto o domicilio. Conciliar el saldo, generar el código desde el cliente, validarlo desde el interno y registrar entrega física. Cerrar por separado después de resolver excedentes y movimientos pendientes.
8. Detener e iniciar con los mismos volúmenes; comprobar acceso, configuración, pedido, historia y PDF. Para explorar D2, conservar una oferta vencida con dinero y aceptar una sucesora antes de aplicar ese dinero; registrar devolución si el cliente abandona.

Los datos sintéticos de certificación pertenecen exclusivamente al proyecto aislado `lamontana-certificacion`, que se detiene al cerrar la verificación. El arranque normal de `lamontana` comienza sin usuarios ni operaciones de ejemplo. No ejecutar `down --volumes` para reiniciar.

Revisión responsive adicional del 23/09/2026: 53 vistas/estados comprobados en Chromium a 320, 360, 390, 600, 768, 820, 1024 y 1440 px. Se corrigieron una etiqueta de portada sin salto, los formularios de sucursales comprimidos en tabletas, las tarjetas del inicio cliente y el ancho del selector de color del catálogo. Las tablas conservan desplazamiento interno; los formularios se adaptan al ancho disponible. Navegación con toque/teclado y diálogos a 320×568, 390×700 y 844×390 comprobados. La comprobación usa tamaños y emulación móvil de navegador, no dispositivos físicos ni Safari/Firefox. Sin cambio de reglas, datos comerciales activos, API o backend; la suite backend previa sigue vigente.
