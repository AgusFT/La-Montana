# Auditoria de normalizacion del DER PostgreSQL de La Montana

| Campo | Valor |
|---|---|
| Version | 1.2 |
| Estado | Revisado; escenario conservador publicado en el DER v3 |
| Fecha | 2026-09-09 |
| Rama analizada | `docs/der-modelo-datos-postgresql` |
| Commit base | `09db5ad799362088dafb4bc8f881f432e67dc390` |
| DER relacional | [`der-modelo-datos-postgresql.md`](./der-modelo-datos-postgresql.md) |
| Fuente grafica | [`der-modelo-datos-postgresql.excalidraw`](./der-modelo-datos-postgresql.excalidraw) |
| Vista SVG | [`DER-V3.svg`](./DER-V3.svg) |
| Huella Markdown auditada | `06bf4024941027fc2d757bfb4e1e053ec5c32bc81961ae5582b598c4a959feca` |
| Huella Excalidraw auditada | `0d36c474eb26ddbc70586f80e5b56d1a9238db2b5760a18ea22fca943a7d047b` |
| Huella Markdown iteracion local 2.0 | `075f0e4f76dc9abd89bae4a2b0de713605bcc6cc2887429ae7334d10ffb35199` |
| Huella Excalidraw iteracion local 2.0 | `0f6ef9069d8c19a42ae5fee686b17181d8cbf83d1a011e5b4bc9046a857088e1` |
| Huella Markdown DER v3 | `e18930f5cf82f204ce38b28cfe3108bb45bbc22c3159edbd3c0c6bd3fd77c082` |
| Huella Excalidraw DER v3 | `535bbb966ff316ddbc811ef554328fd6451e4f45bb14070f3a0eb0496af41b1f` |
| Huella SVG DER v3 | `8ed7358ab7e5b64dd65577cac9a24adb8ef16a9e9f1d17de910c1f02ab5b8960` |

## 1. Resumen ejecutivo

El modelo esta ampliamente normalizado y su tamano no proviene principalmente de duplicacion accidental. La mayor parte de las 107 entidades separa catalogos, versiones inmutables, relaciones N:M, intentos, hechos financieros, historiales y evidencias con ciclos de vida diferentes. Reducir tablas por semejanza visual degradaria integridad o reproducibilidad.

La recomendacion conservadora reduce el modelo a **106 entidades, 946 atributos, 229 FK y 217 pares dirigidos**. El unico retiro de tabla unifica las dos formas anteriores de aplicar credito de cuenta corriente. Ademas elimina seis FK transitivas, dos FK de alcance singleton, dos columnas de moneda constantes y un hash duplicado. No elimina snapshots comerciales, financieros ni documentales. El usuario aprobo el escenario completo y ya se encuentra publicado como DER v3; la rotulacion 2.0 se conserva solo como trazabilidad de una iteracion local previa.

Las tres decisiones abiertas quedaron resueltas: las franjas del mismo destino no se superponen y una sola franja expresa capacidad multiple; las resoluciones del mismo tipo pueden repetirse como hechos inmutables; y cada pareja notificacion-dispositivo posee un unico ciclo agregado de envio push. Ninguna modifica los conteos conservadores.

Un escenario agresivo llega a 100 entidades, pero mezcla configuraciones incompletas, presentacion, requisitos financieros, direcciones y control humano dentro de agregados mayores. Se documenta para comparar y **no se recomienda ni adopta**.

### Veredictos

| Veredicto | Cantidad | Lectura |
|---|---:|---|
| MANTENER | 87 | Separacion correcta o redundancia historica deliberada. |
| REESTRUCTURAR | 19 | Ajuste de columnas, claves o responsabilidad sin eliminar el concepto. |
| FUSIONAR | 1 | Una tabla absorbida por otra con el mismo hecho financiero. |
| REVISAR | 0 | No quedan decisiones abiertas de normalizacion en este alcance. |
| ELIMINAR | 0 | Ninguna entidad carece por completo de significado. |

## 2. Linea base y metodo

### Conteos

| Metrica | Definicion | Resultado |
|---|---|---:|
| Entidades | Encabezados de entidad unicos del diccionario | 107 |
| Atributos | Filas de campo de las 107 entidades | 962 |
| FK | Cada columna FK explicita, incluidas varias hacia el mismo padre | 239 |
| Pares dirigidos | Combinaciones unicas `entidad_hija -> entidad_padre` | 227 |
| Autorrelaciones | Pares donde hija y padre son la misma entidad | 8 |
| Dominios | Marcos funcionales del DER | 11 |

El parser temporal de biblioteca estandar reconstruyo entidades, campos y relaciones desde el Markdown y valido los mismos conteos contra los 239 conectores del Excalidraw. Las tablas historicas de Supabase y otros documentos se usaron solo para contexto; no ampliaron el universo auditado.

### Criterios de forma normal

- **1FN:** valores escalares respecto del dominio. Los `jsonb` actuales contienen metadatos variables saneados, no relaciones estables.
- **2FN:** se revisaron claves naturales y compuestas aunque cada tabla posea una PK sustituta `bigint`.
- **3FN:** se marcaron dependencias transitivas y valores constantes o calculados por determinantes no clave.
- **BCNF:** todo determinante conocido debe ser superclave. "Aparente" significa que la documentacion no declara otra dependencia funcional.
- **Excepcion controlada:** snapshots, estados corrientes y agregados calculados pueden incumplir una lectura estricta sin ser un error, siempre que tengan fuente unica, reconstruccion y validacion.
- **Optimizacion:** menos tablas no se considero una mejora si mezcla ciclos de vida, introduce nulabilidad masiva o pierde FK tipadas.

### Distribucion por dominio

| Dominio | Entidades | FK salientes |
|---|---:|---:|
| Instalación, licencias e integraciones | 7 | 6 |
| Identidad, acceso y clientes | 12 | 19 |
| Sucursales y operación | 7 | 6 |
| Configuración, catálogos y reglas | 22 | 34 |
| Cotizaciones, pedidos y correcciones | 14 | 43 |
| Archivos, conversión y validación | 4 | 9 |
| Agentes, impresoras y producción | 9 | 22 |
| Pagos, cuenta corriente y documentos | 15 | 47 |
| Programación y entregas | 6 | 19 |
| Reclamos y compensaciones | 5 | 14 |
| Notificaciones, alertas y auditoría | 6 | 20 |

## 3. Hallazgos globales

### 3.1. Separaciones que deben conservarse

- Los catalogos estables y sus configuraciones versionadas no son duplicados: unos identifican conceptos y las otras congelan disponibilidad, nombre visible, precio y capacidad por version.
- Cotizacion, pedido y sus items repiten datos deliberadamente. La cotizacion es oferta y el pedido es compromiso; ambos son inmutables y la copia completa fue una decision funcional aprobada.
- Los historiales de estado son fuentes de verdad de dominio. `auditoria` registra acciones sensibles transversales y no puede reemplazarlos.
- Intento de pago, pago, aplicacion, movimiento y reembolso representan etapas o hechos distintos. Solo las dos tablas de aplicacion de cuenta corriente duplican el mismo mecanismo.
- Las condiciones comercial, de aprobacion y de asignacion se parecen, pero pertenecen a padres tipados y vocabularios diferentes; una tabla polimorfica comun perderia integridad.
- Los XOR de propietario, pagador, destino, actor y causa mantienen FK reales. Dividirlos produciria mas tablas y unificarlos como `tipo/id` eliminaria integridad referencial.

### 3.2. Dependencias transitivas y alcance singleton corregibles

| Hija | Atributo redundante | Ruta autoritativa |
|---|---|---|
| `impresora` | `id_sucursal` | `impresora -> agente_impresion -> sucursal` |
| `trabajo_impresion` | `id_configuracion_version` | `trabajo -> pedido_item -> pedido -> configuracion_version` |
| `control_calidad` | `id_pedido_item` | `control -> trabajo_impresion -> pedido_item` |
| `linea_documento_cobro` | `id_pedido` | `linea -> movimiento_cargo -> pedido` |
| `entrega` | `id_pedido` | `entrega -> reserva_entrega -> pedido` |
| `archivo_almacenado` resultado | `id_archivo_derivado_de` | `archivo resultado <- conversion_archivo -> archivo origen` |

Estas rutas usan entidades obligatorias e inmutables o conservadas. Quitarlas reduce riesgo de divergencia sin perder una consulta posible.

Ademas, `licencia_local.id_instalacion` e `integracion_externa.id_instalacion` repiten el identificador de la unica fila `instalacion` admitida por la base. Se retiran y `integracion_externa.tipo` pasa a ser suficiente como clave natural local.

### 3.3. Fuente unica para aplicaciones de cuenta

Todo pago destinado a cuenta corriente ya debe originar un `movimiento_cuenta_corriente` de haber. Sin embargo, `aplicacion_pago_cuenta` consume directamente el pago y `aplicacion_credito_cuenta` consume el movimiento para los demas creditos. Eso obliga a calcular disponibilidad con dos fuentes y permite que pago, movimiento y aplicaciones diverjan.

La solucion conservadora conserva el pago y el libro, elimina `aplicacion_pago_cuenta` y generaliza `aplicacion_credito_cuenta` como `aplicacion_movimiento_cuenta`. La misma pareja movimiento de credito-movimiento de cargo cubre pagos, saldo a favor, compensaciones y futuros creditos tipados.

### 3.4. Constantes y copias verificables

- `intento_pago.moneda` y `pago.moneda` se eliminan porque el alcance aprobado es solo ARS. Mantener una columna constante en cada fila no agrega seguridad ni flexibilidad real.
- `documento_cobro.sha256_pdf` se elimina porque `id_archivo_pdf` es unico y `archivo_almacenado.sha256` se conserva aun tras la purga.
- Se mantienen importes, nombres, direcciones, reglas aplicadas, motores y estados derivados cuando reproducen una decision o documento historico.

### 3.5. Claves y restricciones que no cambian conteos

- Implementar la unicidad activa de `usuario_permiso` como indice parcial, permitiendo varios ciclos historicos de concesion y revocacion.
- Agregar UK `(id_agente_impresion, cola_cups)` en `impresora`.
- Agregar UK `(id_cotizacion_item, id_regla_comercial)` en `cotizacion_regla_aplicada`.
- Agregar UK `(id_pedido_item, id_servicio)` en `pedido_item_servicio`.
- Expresar dos UK distintas en `regla_asignacion_impresora`: regla-impresora y regla-orden.
- Agregar UK parciales de identificador externo por integracion en intentos y reembolsos cuando el proveedor entregue ese identificador.
- Agregar UK parcial de un unico movimiento `CARGO` original por pedido a cuenta corriente.
- Materializar exclusiones de solapamiento para horarios de sucursal y para franjas de una misma version, modalidad, destino efectivo y dia.
- Mantener CHECK XOR y coherencia de cuenta, pedido, usuario, sucursal y version mediante restricciones y transacciones diferibles donde una FK simple no alcance.

## 4. Auditoria por entidad

### Instalación, licencias e integraciones

#### 1. `instalacion` - MANTENER

- **Finalidad:** Identifica técnicamente esta instancia local sin representar una imprenta tenant.
- **Estructura:** 8 atributos; 0 FK salientes.
- **Claves candidatas:** PK `(id_instalacion)`; AK `(codigo_publico)`; AK parcial `(dominio_principal) WHERE NOT NULL`; ademas rige la cardinalidad singleton.
- **Dependencias funcionales:** `id_instalacion` y `codigo_publico` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** El registro singleton concentra identidad tecnica local; no representa multi-tenancy.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 2. `modulo` - MANTENER

- **Finalidad:** Catálogo técnico de módulos licenciables conocidos por la aplicación.
- **Estructura:** 6 atributos; 0 FK salientes.
- **Claves candidatas:** PK `(id_modulo)`; AK `(codigo)`.
- **Dependencias funcionales:** `id_modulo` y `codigo` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Nombre y descripcion dependen del codigo estable; la baja logica conserva referencias historicas.
- **Veredicto y cambio:** Mantener el catalogo estable y su baja logica; no convertirlo en texto repetido en entidades operativas.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 3. `licencia_local` - REESTRUCTURAR

- **Finalidad:** Caché local verificable de una licencia emitida por YG; nunca guarda el serial plano.
- **Estructura:** 11 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_licencia_local)`; AK `(identificador_externo)`; AK `(huella_serial)`.
- **Dependencias funcionales:** `id_licencia_local`, `identificador_externo` y `huella_serial` determinan la fila; `id_instalacion` es constante por base.
- **Forma normal:** 2FN; no alcanza 3FN/BCNF porque la base admite una sola instalacion y por tanto `vacio -> id_instalacion`.
- **Redundancia intencional:** Estado, fechas y tolerancia son cache verificable necesario para operar ante indisponibilidad de YG.
- **Veredicto y cambio:** Eliminar `id_instalacion`; la licencia pertenece implicitamente a la unica instalacion de la base. Mantener identificador externo y huella.
- **Efecto conservador:** 0 tablas; -1 atributo; -1 FK; -1 par dirigido.
- **Riesgo:** Una futura base con varias instalaciones seria multi-tenancy y exigiria reintroducir el alcance en todo el modelo, no solo aqui.

#### 4. `licencia_modulo` - MANTENER

- **Finalidad:** Concesiones aditivas de módulos incluidas en cada licencia.
- **Estructura:** 4 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_licencia_modulo)`; AK `(id_licencia_local, id_modulo)`.
- **Dependencias funcionales:** `id_licencia_modulo` y `(id_licencia_local, id_modulo)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 5. `integracion_externa` - REESTRUCTURAR

- **Finalidad:** Configuración local cifrada de Mercado Pago, MODO, correo o push.
- **Estructura:** 9 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_integracion_externa)`; AK actual `(id_instalacion, tipo)`; AK conservadora `(tipo)` al retirar la FK singleton.
- **Dependencias funcionales:** `tipo` determina la integracion local unica; `id_instalacion` es constante por base.
- **Forma normal:** 2FN; no alcanza 3FN/BCNF porque `id_instalacion` es constante en esta base y `tipo` basta como clave natural.
- **Redundancia intencional:** Credencial cifrada, version y metadatos saneados tienen funciones distintas y se conservan.
- **Veredicto y cambio:** Eliminar `id_instalacion` y cambiar la UK `(id_instalacion, tipo)` por `(tipo)`.
- **Efecto conservador:** 0 tablas; -1 atributo; -1 FK; -1 par dirigido.
- **Riesgo:** La importacion de configuracion entre instalaciones debe seguir prohibida o revalidarse; una FK singleton no la vuelve segura.

#### 6. `evento_integracion` - MANTENER

- **Finalidad:** Evento idempotente recibido de un proveedor y evidencia de su procesamiento.
- **Estructura:** 11 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_evento_integracion)`; AK `(id_integracion_externa, identificador_externo)`.
- **Dependencias funcionales:** `id_evento_integracion` y `(id_integracion_externa, identificador_externo)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF: integracion-identificador externo es clave candidata.
- **Redundancia intencional:** Hash, payload saneado y estado conservan evidencia idempotente sin guardar secretos.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 7. `conciliacion_integracion` - MANTENER

- **Finalidad:** Ejecución periódica de respaldo contra la API del proveedor.
- **Estructura:** 8 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_conciliacion_integracion)`.
- **Dependencias funcionales:** `id_conciliacion_integracion` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF por ejecucion; cantidades son resultados agregados congelados.
- **Redundancia intencional:** Los contadores son resultado historico de cada corrida, no saldos vivos.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

### Identidad, acceso y clientes

#### 8. `rol` - MANTENER

- **Finalidad:** Catálogo cerrado de roles principales del sistema.
- **Estructura:** 5 atributos; 0 FK salientes.
- **Claves candidatas:** PK `(id_rol)`; AK `(codigo)`.
- **Dependencias funcionales:** `id_rol` y `codigo` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Nombre y descripcion dependen del codigo estable; la baja logica conserva referencias historicas.
- **Veredicto y cambio:** Mantener el catalogo estable y su baja logica; no convertirlo en texto repetido en entidades operativas.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 9. `permiso` - MANTENER

- **Finalidad:** Catálogo de acciones autorizables por backend.
- **Estructura:** 6 atributos; 0 FK salientes.
- **Claves candidatas:** PK `(id_permiso)`; AK `(codigo)`.
- **Dependencias funcionales:** `id_permiso` y `codigo` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Nombre y descripcion dependen del codigo estable; la baja logica conserva referencias historicas.
- **Veredicto y cambio:** Mantener el catalogo estable y su baja logica; no convertirlo en texto repetido en entidades operativas.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 10. `rol_permiso` - MANTENER

- **Finalidad:** Permisos predeterminados concedidos a cada rol.
- **Estructura:** 3 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_rol_permiso)`; AK `(id_rol, id_permiso)`.
- **Dependencias funcionales:** `id_rol_permiso` y `(id_rol, id_permiso)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 11. `empresa_cliente` - MANTENER

- **Finalidad:** Agrupa usuarios empresariales, pedidos y una posible cuenta corriente.
- **Estructura:** 8 atributos; 0 FK salientes.
- **Claves candidatas:** PK `(id_empresa_cliente)`; AK `(cuit)`.
- **Dependencias funcionales:** `id_empresa_cliente` y `cuit` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 12. `usuario` - MANTENER

- **Finalidad:** Identidad autenticable y actor humano del sistema.
- **Estructura:** 15 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_usuario)`; AK `(correo)`; unicidades parciales separadas para SOPORTE y administrador propietario activos.
- **Dependencias funcionales:** `id_usuario` y `correo` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Rol, empresa y banderas especiales expresan restricciones ortogonales; no se reemplazan por una jerarquia de tablas.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 13. `usuario_permiso` - REESTRUCTURAR

- **Finalidad:** Permiso adicional individual, permitido solamente para empleados.
- **Estructura:** 6 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_usuario_permiso)`; AK parcial activa `(id_usuario, id_permiso) WHERE fecha_revocacion IS NULL`.
- **Dependencias funcionales:** `id_usuario_permiso` determina cada concesion historica; `(id_usuario, id_permiso)` determina como maximo la concesion activa.
- **Forma normal:** BCNF si la historia usa la PK y la unicidad usuario-permiso se limita a filas activas.
- **Redundancia intencional:** Otorgante y fechas preservan ciclos de concesion y revocacion; no deben sobrescribirse al volver a otorgar.
- **Veredicto y cambio:** Expresar la UK como indice parcial `(id_usuario, id_permiso) WHERE fecha_revocacion IS NULL` y no como unicidad global de la pareja.
- **Efecto conservador:** Sin cambio de conteos; corrige el alcance temporal de la UK.
- **Riesgo:** Una UK global impediria registrar una nueva concesion; reutilizar la fila perderia el ciclo anterior.

#### 14. `direccion_cliente` - MANTENER

- **Finalidad:** Dirección reutilizable de un usuario personal o una empresa cliente.
- **Estructura:** 13 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_direccion_cliente)`.
- **Dependencias funcionales:** `id_direccion_cliente` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** El XOR usuario/empresa evita duplicar toda la estructura de direccion y mantiene FK tipadas.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 15. `usuario_sucursal` - MANTENER

- **Finalidad:** Asignación mutable de empleados a una o varias sucursales.
- **Estructura:** 6 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_usuario_sucursal)`; AK `(id_usuario, id_sucursal)` en el modelo mutable actual.
- **Dependencias funcionales:** `id_usuario_sucursal` y `(id_usuario, id_sucursal)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 16. `credencial_temporal` - MANTENER

- **Finalidad:** Credencial de alta, restablecimiento o cambio de llave, de un solo uso.
- **Estructura:** 9 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_credencial_temporal)`.
- **Dependencias funcionales:** `id_credencial_temporal` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 17. `token_verificacion_correo` - MANTENER

- **Finalidad:** Desafío de un solo uso para validar el correo de altas públicas.
- **Estructura:** 7 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_token_verificacion_correo)`; AK `(hash_token)`.
- **Dependencias funcionales:** `id_token_verificacion_correo` y `hash_token` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 18. `sesion_usuario` - MANTENER

- **Finalidad:** Refresh token hasheado por sesión o dispositivo, revocable individualmente.
- **Estructura:** 10 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_sesion_usuario)`; AK `(codigo_publico)`; AK `(hash_refresh_token)`.
- **Dependencias funcionales:** `id_sesion_usuario` y `codigo_publico` y `hash_refresh_token` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Hash y codigo publico tienen funciones distintas; dispositivo es opcional para sesiones sin push.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 19. `autorizacion_soporte` - MANTENER

- **Finalidad:** Ventana temporal auditada durante la cual SOPORTE recupera acceso.
- **Estructura:** 9 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_autorizacion_soporte)`.
- **Dependencias funcionales:** `id_autorizacion_soporte` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

### Sucursales y operación

#### 20. `sucursal` - MANTENER

- **Finalidad:** Local operativo de la imprenta a la que pertenecen pedidos y recursos.
- **Estructura:** 14 atributos; 0 FK salientes.
- **Claves candidatas:** PK `(id_sucursal)`; AK `(codigo)`.
- **Dependencias funcionales:** `id_sucursal` y `codigo` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** La direccion propia del local es un hecho estable distinto de direcciones de clientes o snapshots.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 21. `horario_sucursal` - MANTENER

- **Finalidad:** Intervalos semanales regulares de atención y retiro.
- **Estructura:** 6 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_horario_sucursal)`.
- **Dependencias funcionales:** `id_horario_sucursal` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 22. `excepcion_horario_sucursal` - MANTENER

- **Finalidad:** Feriado, cierre o horario especial de una sucursal.
- **Estructura:** 7 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_excepcion_horario_sucursal)`; AK `(id_sucursal, fecha)`.
- **Dependencias funcionales:** `id_excepcion_horario_sucursal` y `(id_sucursal, fecha)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 23. `pausa_operativa` - MANTENER

- **Finalidad:** Pausa global o de una sucursal que bloquea nuevas cotizaciones y confirmaciones.
- **Estructura:** 8 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_pausa_operativa)`; unicidad parcial de pausa activa por alcance global o sucursal.
- **Dependencias funcionales:** `id_pausa_operativa` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 24. `punto_entrega` - MANTENER

- **Finalidad:** Lugar externo donde un empleado entrega directamente el pedido al cliente.
- **Estructura:** 10 atributos; 0 FK salientes.
- **Claves candidatas:** PK `(id_punto_entrega)`; AK `(codigo)`.
- **Dependencias funcionales:** `id_punto_entrega` y `codigo` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 25. `zona_entrega` - MANTENER

- **Finalidad:** Zona global de envío a domicilio compartida por todas las sucursales.
- **Estructura:** 5 atributos; 0 FK salientes.
- **Claves candidatas:** PK `(id_zona_entrega)`; AK `(codigo)`.
- **Dependencias funcionales:** `id_zona_entrega` y `codigo` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 26. `zona_entrega_codigo_postal` - MANTENER

- **Finalidad:** Códigos postales y localidades que delimitan una zona sin mapas.
- **Estructura:** 5 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_zona_entrega_codigo_postal)`; AK `(codigo_postal, localidad, provincia)`.
- **Dependencias funcionales:** `id_zona_entrega_codigo_postal` y `(codigo_postal, localidad, provincia)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

### Configuración, catálogos y reglas

#### 27. `configuracion_version` - MANTENER

- **Finalidad:** Versión completa e inmutable de las decisiones comerciales y operativas.
- **Estructura:** 9 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_configuracion_version)`; AK `(numero_version)`; unicidades parciales por estados ACTIVA y EN_PREPARACION.
- **Dependencias funcionales:** `id_configuracion_version` y `numero_version` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF; numero_version es clave candidata. Estado actual se limita mediante indices parciales.
- **Redundancia intencional:** La copia completa entre versiones es intencional: cada version activa o historica es inmutable.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionar puede introducir columnas nulas en borradores y acoplar componentes con validacion propia.

#### 28. `politica_operativa` - MANTENER

- **Finalidad:** Parámetros globales explícitos de una versión, sin mapa clave-valor.
- **Estructura:** 8 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_politica_operativa)`; AK `(id_configuracion_version)`.
- **Dependencias funcionales:** `id_politica_operativa` y `id_configuracion_version` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF: configuracion_version es clave candidata.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionar puede introducir columnas nulas en borradores y acoplar componentes con validacion propia.

#### 29. `formato` - MANTENER

- **Finalidad:** Catálogo estable de tamaños de impresión.
- **Estructura:** 6 atributos; 0 FK salientes.
- **Claves candidatas:** PK `(id_formato)`; AK `(codigo)`.
- **Dependencias funcionales:** `id_formato` y `codigo` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Nombre y descripcion dependen del codigo estable; la baja logica conserva referencias historicas.
- **Veredicto y cambio:** Mantener el catalogo estable y su baja logica; no convertirlo en texto repetido en entidades operativas.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 30. `papel` - MANTENER

- **Finalidad:** Catálogo estable de materiales de impresión sin inventario.
- **Estructura:** 6 atributos; 0 FK salientes.
- **Claves candidatas:** PK `(id_papel)`; AK `(codigo)`.
- **Dependencias funcionales:** `id_papel` y `codigo` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Nombre y descripcion dependen del codigo estable; la baja logica conserva referencias historicas.
- **Veredicto y cambio:** Mantener el catalogo estable y su baja logica; no convertirlo en texto repetido en entidades operativas.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 31. `servicio` - MANTENER

- **Finalidad:** Catálogo estable de impresión y terminaciones ofrecibles.
- **Estructura:** 6 atributos; 0 FK salientes.
- **Claves candidatas:** PK `(id_servicio)`; AK `(codigo)`.
- **Dependencias funcionales:** `id_servicio` y `codigo` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Nombre y descripcion dependen del codigo estable; la baja logica conserva referencias historicas.
- **Veredicto y cambio:** Mantener el catalogo estable y su baja logica; no convertirlo en texto repetido en entidades operativas.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 32. `configuracion_servicio` - MANTENER

- **Finalidad:** Versión de disponibilidad, precio base y tiempo de un servicio.
- **Estructura:** 8 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_configuracion_servicio)`; AK `(id_configuracion_version, id_servicio)`.
- **Dependencias funcionales:** `id_configuracion_servicio` y `(id_configuracion_version, id_servicio)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Nombre visible y precio se versionan deliberadamente aunque exista un catalogo estable.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionar puede introducir columnas nulas en borradores y acoplar componentes con validacion propia.

#### 33. `compatibilidad_servicio` - MANTENER

- **Finalidad:** Combinaciones de formato y papel admitidas por un servicio de terminación.
- **Estructura:** 4 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_compatibilidad_servicio)`; AK `(id_configuracion_servicio, id_formato, id_papel)`.
- **Dependencias funcionales:** `id_compatibilidad_servicio` y `(id_configuracion_servicio, id_formato, id_papel)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 34. `tarifa_impresion` - MANTENER

- **Finalidad:** Precio versionado de impresión por combinación material.
- **Estructura:** 8 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_tarifa_impresion)`; AK `(id_configuracion_version, id_formato, id_papel, modo_color)`.
- **Dependencias funcionales:** `id_tarifa_impresion` y `(id_configuracion_version, id_formato, id_papel, modo_color)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Cada version repite la matriz completa para reproducir cotizaciones historicas.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 35. `sucursal_servicio` - MANTENER

- **Finalidad:** Habilitación de un servicio global según capacidad de una sucursal.
- **Estructura:** 5 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_sucursal_servicio)`; AK `(id_configuracion_version, id_sucursal, id_servicio)`.
- **Dependencias funcionales:** `id_sucursal_servicio` y `(id_configuracion_version, id_sucursal, id_servicio)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 36. `configuracion_medio_pago` - MANTENER

- **Finalidad:** Medio de pago habilitado y condición de avance en una versión.
- **Estructura:** 6 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_configuracion_medio_pago)`; AK `(id_configuracion_version, medio_pago)`.
- **Dependencias funcionales:** `id_configuracion_medio_pago` y `(id_configuracion_version, medio_pago)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** La exigencia y el modulo requerido pertenecen a la version y pueden evolucionar entre versiones.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionar puede introducir columnas nulas en borradores y acoplar componentes con validacion propia.

#### 37. `configuracion_modalidad_entrega` - MANTENER

- **Finalidad:** Modalidad de entrega habilitada por versión.
- **Estructura:** 4 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_configuracion_modalidad_entrega)`; AK `(id_configuracion_version, modalidad)`.
- **Dependencias funcionales:** `id_configuracion_modalidad_entrega` y `(id_configuracion_version, modalidad)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** La repeticion por version forma un snapshot completo e inmutable; no debe convertirse en deltas.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionar puede introducir columnas nulas en borradores y acoplar componentes con validacion propia.

#### 38. `configuracion_punto_entrega` - MANTENER

- **Finalidad:** Punto externo habilitado para una sucursal, con costo propio.
- **Estructura:** 6 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_configuracion_punto_entrega)`; AK `(id_configuracion_version, id_sucursal, id_punto_entrega)`.
- **Dependencias funcionales:** `id_configuracion_punto_entrega` y `(id_configuracion_version, id_sucursal, id_punto_entrega)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** La repeticion por version forma un snapshot completo e inmutable; no debe convertirse en deltas.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionar puede introducir columnas nulas en borradores y acoplar componentes con validacion propia.

#### 39. `configuracion_zona_entrega` - MANTENER

- **Finalidad:** Costo y disponibilidad global de una zona de envío en una versión.
- **Estructura:** 5 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_configuracion_zona_entrega)`; AK `(id_configuracion_version, id_zona_entrega)`.
- **Dependencias funcionales:** `id_configuracion_zona_entrega` y `(id_configuracion_version, id_zona_entrega)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** La repeticion por version forma un snapshot completo e inmutable; no debe convertirse en deltas.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionar puede introducir columnas nulas en borradores y acoplar componentes con validacion propia.

#### 40. `configuracion_impresora` - MANTENER

- **Finalidad:** Impresora física habilitada y priorizada en una versión.
- **Estructura:** 5 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_configuracion_impresora)`; AK `(id_configuracion_version, id_impresora)`.
- **Dependencias funcionales:** `id_configuracion_impresora` y `(id_configuracion_version, id_impresora)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** La repeticion por version forma un snapshot completo e inmutable; no debe convertirse en deltas.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionar puede introducir columnas nulas en borradores y acoplar componentes con validacion propia.

#### 41. `regla_comercial` - MANTENER

- **Finalidad:** Regla estructurada por item para descuento, seña o prepago.
- **Estructura:** 5 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_regla_comercial)`.
- **Dependencias funcionales:** `id_regla_comercial` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 42. `condicion_regla` - MANTENER

- **Finalidad:** Condición AND certificada de una regla comercial.
- **Estructura:** 8 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_condicion_regla)`.
- **Dependencias funcionales:** `id_condicion_regla` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Las columnas de valor forman una union tipada controlada por campo y operador; no son grupos repetidos.
- **Veredicto y cambio:** Mantener tabla propia y CHECK de familia de valor; una tabla generica polimorfica perderia FK y claridad.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 43. `accion_regla` - MANTENER

- **Finalidad:** Única consecuencia certificada de una regla comercial.
- **Estructura:** 4 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_accion_regla)`; AK `(id_regla_comercial)`.
- **Dependencias funcionales:** `id_accion_regla` y `id_regla_comercial` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF: regla_comercial es clave candidata.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 44. `regla_aprobacion` - MANTENER

- **Finalidad:** Caso administrable que habilita aprobación automática dentro del modo CONDICIONAL.
- **Estructura:** 5 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_regla_aprobacion)`.
- **Dependencias funcionales:** `id_regla_aprobacion` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 45. `condicion_aprobacion` - MANTENER

- **Finalidad:** Condición AND certificada de una regla de aprobación condicional.
- **Estructura:** 8 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_condicion_aprobacion)`.
- **Dependencias funcionales:** `id_condicion_aprobacion` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Las columnas de valor forman una union tipada controlada por campo y operador; no son grupos repetidos.
- **Veredicto y cambio:** Mantener tabla propia y CHECK de familia de valor; una tabla generica polimorfica perderia FK y claridad.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 46. `regla_asignacion` - MANTENER

- **Finalidad:** Regla ordenada para seleccionar una impresora compatible.
- **Estructura:** 6 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_regla_asignacion)`.
- **Dependencias funcionales:** `id_regla_asignacion` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 47. `condicion_asignacion` - MANTENER

- **Finalidad:** Condición certificada por atributos del item para una regla de impresora.
- **Estructura:** 6 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_condicion_asignacion)`.
- **Dependencias funcionales:** `id_condicion_asignacion` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Las columnas de valor forman una union tipada controlada por campo y operador; no son grupos repetidos.
- **Veredicto y cambio:** Mantener tabla propia y CHECK de familia de valor; una tabla generica polimorfica perderia FK y claridad.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 48. `regla_asignacion_impresora` - MANTENER

- **Finalidad:** Lista ordenada de impresoras candidatas para una regla.
- **Estructura:** 4 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_regla_asignacion_impresora)`; AK `(id_regla_asignacion, id_impresora)` y AK `(id_regla_asignacion, orden)`, no una unica terna.
- **Dependencias funcionales:** `id_regla_asignacion_impresora`, `(id_regla_asignacion, id_impresora)` y `(id_regla_asignacion, orden)` determinan la fila completa.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

### Cotizaciones, pedidos y correcciones

#### 49. `cotizacion` - MANTENER

- **Finalidad:** Oferta persistida que congela configuración, importes, entrega y condiciones.
- **Estructura:** 24 atributos; 8 FK salientes.
- **Claves candidatas:** PK `(id_cotizacion)`; AK `(codigo_publico)`.
- **Dependencias funcionales:** `id_cotizacion` y `codigo_publico` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF interna; sus totales y destino son snapshots interrelacionales deliberados.
- **Redundancia intencional:** Totales, entrega, medio y configuracion se congelan para reproducir la oferta.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 50. `cotizacion_item` - MANTENER

- **Finalidad:** Item homogéneo cotizado a partir de un único archivo.
- **Estructura:** 20 atributos; 4 FK salientes.
- **Claves candidatas:** PK `(id_cotizacion_item)`; AK `(id_cotizacion, orden)`.
- **Dependencias funcionales:** `id_cotizacion_item` y `(id_cotizacion, orden)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** 2FN; mantiene una excepcion controlada a 3FN/BCNF por cantidades e importes calculados.
- **Redundancia intencional:** Cantidades, valores configurados/aplicados, subtotal y sena son evidencia del calculo.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 51. `cotizacion_item_servicio` - MANTENER

- **Finalidad:** Servicio y precio congelados dentro de un item cotizado.
- **Estructura:** 8 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_cotizacion_item_servicio)`; AK `(id_cotizacion_item, id_servicio)`.
- **Dependencias funcionales:** `id_cotizacion_item_servicio` y `(id_cotizacion_item, id_servicio)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** 2FN; subtotal es una desnormalizacion calculada e inmutable de cantidad por precio.
- **Redundancia intencional:** Tipo, base, precio y subtotal congelan el servicio aunque cambie el catalogo.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 52. `cotizacion_regla_aplicada` - REESTRUCTURAR

- **Finalidad:** Evidencia reproducible de la regla ganadora aplicada a un item.
- **Estructura:** 7 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_cotizacion_regla_aplicada)`; falta declarar AK `(id_cotizacion_item, id_regla_comercial)`.
- **Dependencias funcionales:** `id_cotizacion_regla_aplicada` determina la fila; la pareja propuesta `(id_cotizacion_item, id_regla_comercial)` tambien debe determinarla.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Tipo, valor e importe registran la regla tal como se ejecuto, incluso si la configuracion queda historica.
- **Veredicto y cambio:** Agregar unicidad `(id_cotizacion_item, id_regla_comercial)` y, si solo se guarda la ganadora por familia, tambien controlar la cardinalidad por `tipo_accion`.
- **Efecto conservador:** Sin cambio de conteos; agrega integridad.
- **Riesgo:** Una unicidad demasiado amplia impediria conservar reglas ganadoras de familias distintas.

#### 53. `pedido` - MANTENER

- **Finalidad:** Compromiso comercial confirmado y centro del flujo operativo.
- **Estructura:** 26 atributos; 8 FK salientes.
- **Claves candidatas:** PK `(id_pedido)`; AK `(codigo_publico)`; AK `(numero_pedido)`; AK `(id_cotizacion)`.
- **Dependencias funcionales:** `id_pedido` y `codigo_publico` y `numero_pedido` y `id_cotizacion` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF interna porque cotizacion es clave candidata; duplica el snapshot confirmado de forma deliberada.
- **Redundancia intencional:** La copia respecto de cotizacion fue aprobada expresamente y aisla el compromiso comercial.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 54. `pedido_item` - MANTENER

- **Finalidad:** Snapshot operativo de un item confirmado.
- **Estructura:** 18 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_pedido_item)`; AK `(id_cotizacion_item)`; AK `(id_pedido, orden)`.
- **Dependencias funcionales:** `id_pedido_item` y `id_cotizacion_item` y `(id_pedido, orden)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** 2FN; contiene cantidades y subtotal calculados como snapshot historico aprobado.
- **Redundancia intencional:** Codigos, dimensiones, cantidades y precios son el snapshot operativo aprobado.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 55. `pedido_item_servicio` - REESTRUCTURAR

- **Finalidad:** Snapshot de cada servicio incluido en un item confirmado.
- **Estructura:** 10 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_pedido_item_servicio)`; falta declarar AK `(id_pedido_item, id_servicio)`.
- **Dependencias funcionales:** `id_pedido_item_servicio` determina la fila; la pareja propuesta `(id_pedido_item, id_servicio)` tambien debe determinarla.
- **Forma normal:** 2FN; subtotal calculado y datos del servicio son snapshot controlado.
- **Redundancia intencional:** Nombre, tipo, base e importes preservan exactamente lo confirmado.
- **Veredicto y cambio:** Restituir en el snapshot la clave candidata `(id_pedido_item, id_servicio)` que ya existe en la cotizacion; la cantidad expresa repeticiones del mismo servicio.
- **Efecto conservador:** Sin cambio de conteos; agrega una UK compuesta.
- **Riesgo:** Debe confirmarse que dos lineas del mismo servicio nunca tengan parametros distintos; el modelo actual no admite esos parametros.

#### 56. `historial_estado_pedido` - MANTENER

- **Finalidad:** Fuente de verdad append-only de las transiciones del pedido.
- **Estructura:** 8 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_historial_estado_pedido)`.
- **Dependencias funcionales:** `id_historial_estado_pedido` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF por evento; estado actual del pedido es una proyeccion controlada.
- **Redundancia intencional:** Estados anterior/nuevo y actor son evidencia temporal append-only; no duplican la auditoria transversal.
- **Veredicto y cambio:** Mantener separada de la entidad corriente y de auditoria; materializar orden estable e inmutabilidad.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionarla con auditoria perderia semantica de dominio y fuente de verdad de transiciones.

#### 57. `observacion_interna` - MANTENER

- **Finalidad:** Nota append-only visible solamente para personal autorizado.
- **Estructura:** 7 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_observacion_interna)`.
- **Dependencias funcionales:** `id_observacion_interna` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 58. `solicitud_correccion` - MANTENER

- **Finalidad:** Pedido formal de corrección visible al cliente antes de producción.
- **Estructura:** 10 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_solicitud_correccion)`.
- **Dependencias funcionales:** `id_solicitud_correccion` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 59. `solicitud_correccion_item` - MANTENER

- **Finalidad:** Items alcanzados por una solicitud de corrección.
- **Estructura:** 3 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_solicitud_correccion_item)`; AK `(id_solicitud_correccion, id_pedido_item)`.
- **Dependencias funcionales:** `id_solicitud_correccion_item` y `(id_solicitud_correccion, id_pedido_item)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 60. `solicitud_correccion_archivo` - MANTENER

- **Finalidad:** Archivos específicos observados por una solicitud.
- **Estructura:** 3 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_solicitud_correccion_archivo)`; AK `(id_solicitud_correccion, id_archivo_almacenado)`.
- **Dependencias funcionales:** `id_solicitud_correccion_archivo` y `(id_solicitud_correccion, id_archivo_almacenado)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 61. `respuesta_correccion` - MANTENER

- **Finalidad:** Respuesta inmutable del cliente a una solicitud.
- **Estructura:** 5 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_respuesta_correccion)`.
- **Dependencias funcionales:** `id_respuesta_correccion` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 62. `solicitud_cancelacion` - MANTENER

- **Finalidad:** Solicitud del cliente cuando la producción ya impide cancelar directamente.
- **Estructura:** 9 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_solicitud_cancelacion)`.
- **Dependencias funcionales:** `id_solicitud_cancelacion` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

### Archivos, conversión y validación

#### 63. `archivo_almacenado` - REESTRUCTURAR

- **Finalidad:** Metadatos persistentes de un objeto privado cuyo contenido vive fuera de PostgreSQL.
- **Estructura:** 17 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_archivo_almacenado)`; AK `(codigo_publico)`; AK `(clave_storage)`.
- **Dependencias funcionales:** `conversion_archivo.id_archivo_resultado` determina el origen exitoso y duplica `archivo_almacenado.id_archivo_derivado_de`.
- **Forma normal:** BCNF interna. La duplicacion de linaje es interrelacional, no una dependencia interna.
- **Redundancia intencional:** Metadatos y hash sobreviven a la purga; eso es evidencia intencional. El segundo enlace de derivacion no lo es.
- **Veredicto y cambio:** Eliminar `id_archivo_derivado_de`; `conversion_archivo` ya representa todos los derivados actualmente aprobados. Agregar una relacion tipada solo si aparece otra clase real de derivacion.
- **Efecto conservador:** 0 tablas; -1 atributo; -1 FK; -1 par dirigido.
- **Riesgo:** Bajo en el alcance actual; antes de migrar debe comprobarse que no existan derivados ajenos a conversion ofimatica.

#### 64. `archivo_trabajo` - MANTENER

- **Finalidad:** Asocia un original o derivado a exactamente un item de cotización o de pedido.
- **Estructura:** 10 atributos; 4 FK salientes.
- **Claves candidatas:** PK `(id_archivo_trabajo)`; AK `(id_archivo_almacenado)`.
- **Dependencias funcionales:** `id_archivo_trabajo` y `id_archivo_almacenado` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** El XOR cotizacion/pedido representa la transferencia atomica aprobada; reemplazos conservan linaje.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 65. `validacion_archivo` - MANTENER

- **Finalidad:** Resultado inmutable de inspeccionar tipo real, seguridad, legibilidad y renderizado.
- **Estructura:** 13 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_validacion_archivo)`.
- **Dependencias funcionales:** `id_validacion_archivo` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Motor, version y resultado se repiten por corrida para reproducir decisiones tecnicas.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 66. `conversion_archivo` - MANTENER

- **Finalidad:** Conversión reproducible de DOCX u ODT a PDF canónico para cálculo, vista previa y CUPS.
- **Estructura:** 11 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_conversion_archivo)`; AK parcial `(id_archivo_resultado) WHERE NOT NULL`.
- **Dependencias funcionales:** `id_conversion_archivo` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF por resultado cuando existe; fallos solo poseen la PK sustituta.
- **Redundancia intencional:** Motor, version y diagnostico congelan cada intento, exitoso o fallido.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

### Agentes, impresoras y producción

#### 67. `agente_impresion` - MANTENER

- **Finalidad:** Proceso Linux o Raspberry Pi de una sucursal que controla una o varias impresoras.
- **Estructura:** 10 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_agente_impresion)`; AK `(codigo_publico)`; AK `(huella_equipo)`.
- **Dependencias funcionales:** `id_agente_impresion` y `codigo_publico` y `huella_equipo` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 68. `credencial_agente` - MANTENER

- **Finalidad:** Credencial única hasheada, rotativa y revocable de un agente.
- **Estructura:** 8 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_credencial_agente)`; AK `(identificador)`; unicidad parcial de credencial vigente por agente.
- **Dependencias funcionales:** `id_credencial_agente` y `identificador` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 69. `impresora` - REESTRUCTURAR

- **Finalidad:** Equipo físico estable, independiente del versionado de configuración.
- **Estructura:** 16 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_impresora)`; AK `(codigo_publico)`; AK parcial `(numero_serie) WHERE NOT NULL`.
- **Dependencias funcionales:** `id_agente_impresion -> id_sucursal`; ademas `(id_agente_impresion, cola_cups)` debe identificar una cola fisica.
- **Forma normal:** 2FN; no alcanza 3FN porque `id_agente_impresion -> id_sucursal` y el agente es obligatorio.
- **Redundancia intencional:** Estado operativo y ultima observacion online son proyecciones operativas justificadas; la sucursal duplicada no aporta historia propia.
- **Veredicto y cambio:** Eliminar `id_sucursal`, derivarla desde el agente y agregar UK `(id_agente_impresion, cola_cups)`. Una futura impresora sin agente requeriria otro modelado, no una FK duplicada preventiva.
- **Efecto conservador:** 0 tablas; -1 atributo; -1 FK; -1 par dirigido.
- **Riesgo:** La reasignacion de agente pasa a ser tambien la reasignacion de sucursal y debe auditarse como una operacion unica.

#### 70. `impresora_formato` - MANTENER

- **Finalidad:** Formato confirmado como soportado por una impresora.
- **Estructura:** 6 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_impresora_formato)`; AK `(id_impresora, id_formato)`.
- **Dependencias funcionales:** `id_impresora_formato` y `(id_impresora, id_formato)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 71. `impresora_servicio` - MANTENER

- **Finalidad:** Servicio técnico confirmado como realizable por una impresora.
- **Estructura:** 6 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_impresora_servicio)`; AK `(id_impresora, id_servicio)`.
- **Dependencias funcionales:** `id_impresora_servicio` y `(id_impresora, id_servicio)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 72. `historial_estado_impresora` - MANTENER

- **Finalidad:** Cambios auditables del estado operativo de una impresora.
- **Estructura:** 8 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_historial_estado_impresora)`.
- **Dependencias funcionales:** `id_historial_estado_impresora` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF por evento; estado actual de impresora es una proyeccion controlada.
- **Redundancia intencional:** Estados anterior/nuevo y actor son evidencia temporal append-only; no duplican la auditoria transversal.
- **Veredicto y cambio:** Mantener separada de la entidad corriente y de auditoria; materializar orden estable e inmutabilidad.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionarla con auditoria perderia semantica de dominio y fuente de verdad de transiciones.

#### 73. `trabajo_impresion` - REESTRUCTURAR

- **Finalidad:** Intento técnico completo de producir un item sin dividir sus copias.
- **Estructura:** 20 atributos; 6 FK salientes.
- **Claves candidatas:** PK `(id_trabajo_impresion)`; AK `(codigo_publico)`; AK parcial `(token_lease) WHERE NOT NULL`; unicidad parcial de trabajo activo por item.
- **Dependencias funcionales:** `id_pedido_item -> pedido.id_configuracion_version`; impresora y agente pueden repetirse como snapshot historico de ejecucion.
- **Forma normal:** 2FN; no alcanza 3FN porque `id_pedido_item -> id_configuracion_version` mediante el pedido inmutable.
- **Redundancia intencional:** Impresora, agente, modo, lease e identificador CUPS congelan el intento tecnico; no deben derivarse del estado actual de la impresora.
- **Veredicto y cambio:** Eliminar `id_configuracion_version` y obtenerla por item-pedido. Mantener agente e impresora porque pueden cambiar despues del intento.
- **Efecto conservador:** 0 tablas; -1 atributo; -1 FK; -1 par dirigido.
- **Riesgo:** Bajo mientras pedido e item sean inmutables y nunca se eliminen fisicamente.

#### 74. `historial_trabajo_impresion` - MANTENER

- **Finalidad:** Transiciones append-only de un trabajo de impresión.
- **Estructura:** 10 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_historial_trabajo_impresion)`.
- **Dependencias funcionales:** `id_historial_trabajo_impresion` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF por evento; estado actual del trabajo es una proyeccion controlada.
- **Redundancia intencional:** Estados anterior/nuevo y actor son evidencia temporal append-only; no duplican la auditoria transversal.
- **Veredicto y cambio:** Mantener separada de la entidad corriente y de auditoria; materializar orden estable e inmutabilidad.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionarla con auditoria perderia semantica de dominio y fuente de verdad de transiciones.

#### 75. `control_calidad` - REESTRUCTURAR

- **Finalidad:** Revisión humana obligatoria posterior a cada trabajo completado.
- **Estructura:** 7 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_control_calidad)`; AK `(id_trabajo_impresion)`.
- **Dependencias funcionales:** `id_trabajo_impresion -> trabajo_impresion.id_pedido_item`.
- **Forma normal:** BCNF: `id_trabajo_impresion` ya es clave candidata. La FK al item es redundancia interrelacional.
- **Redundancia intencional:** La revision separada conserva actor y resultado humano con ciclo posterior al trabajo.
- **Veredicto y cambio:** Eliminar `id_pedido_item` y resolverlo por el trabajo. No fusionar la revision con el trabajo en el escenario conservador.
- **Efecto conservador:** 0 tablas; -1 atributo; -1 FK; -1 par dirigido.
- **Riesgo:** Bajo; las consultas por item necesitan un join ya respaldado por la FK del trabajo.

### Pagos, cuenta corriente y documentos

#### 76. `intento_pago` - REESTRUCTURAR

- **Finalidad:** Operación solicitada que todavía no constituye dinero confirmado.
- **Estructura:** 17 atributos; 5 FK salientes.
- **Claves candidatas:** PK `(id_intento_pago)`; AK `(codigo_publico)`; AK parcial propuesta `(id_integracion_externa, identificador_externo) WHERE identificador_externo IS NOT NULL`.
- **Dependencias funcionales:** `codigo_publico` determina la fila; para proveedor debe cumplirse `(id_integracion_externa, identificador_externo) -> intento`.
- **Forma normal:** 2FN; con la regla aprobada de una sola moneda, `vacio -> moneda=ARS` impide 3FN/BCNF.
- **Redundancia intencional:** Pagador, destino, importe solicitado y estado son propios del intento y no constituyen dinero confirmado.
- **Veredicto y cambio:** Eliminar `moneda`, declarar ARS como invariante del esquema y agregar UK parcial por integracion e identificador externo.
- **Efecto conservador:** 0 tablas; -1 atributo; sin cambio de FK ni pares.
- **Riesgo:** Una futura segunda moneda seria un cambio material y exigiria reincorporar moneda en importes y reglas, no solo esta columna.

#### 77. `pago` - REESTRUCTURAR

- **Finalidad:** Hecho financiero inmutable nacido únicamente de un intento confirmado.
- **Estructura:** 13 atributos; 5 FK salientes.
- **Claves candidatas:** PK `(id_pago)`; AK `(codigo_publico)`; AK `(id_intento_pago)`.
- **Dependencias funcionales:** `id_intento_pago` y `codigo_publico` determinan la fila completa.
- **Forma normal:** 2FN; con una sola moneda, `vacio -> moneda=ARS` impide 3FN/BCNF. `id_intento_pago` es clave candidata.
- **Redundancia intencional:** Pagador, destino, medio e identificador externo se congelan de nuevo para que el hecho financiero sea autocontenido.
- **Veredicto y cambio:** Eliminar solo `moneda` y conservar el snapshot financiero restante.
- **Efecto conservador:** 0 tablas; -1 atributo; sin cambio de FK ni pares.
- **Riesgo:** El backend debe rechazar cualquier moneda distinta de ARS en el limite de integracion.

#### 78. `aplicacion_pago_pedido` - MANTENER

- **Finalidad:** Parte de un pago normal aplicada a un pedido y clasificada como seña o saldo.
- **Estructura:** 6 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_aplicacion_pago_pedido)`; AK `(id_pago, id_pedido, clasificacion)`.
- **Dependencias funcionales:** `id_aplicacion_pago_pedido` y `(id_pago, id_pedido, clasificacion)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 79. `senia` - MANTENER

- **Finalidad:** Requisito financiero único por pedido, separado para identificar su cobertura.
- **Estructura:** 9 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_senia)`; AK `(id_pedido)`.
- **Dependencias funcionales:** `id_senia` y `id_pedido` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF interna; estado y fechas de cobertura son caches interrelacionales reconstruibles.
- **Redundancia intencional:** Importe requerido y estado derivado permiten identificar cobertura; el dinero sigue en pagos y aplicaciones.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 80. `comprobante_pago` - MANTENER

- **Finalidad:** Evidencia privada informada o recibo generado para un intento o un pago.
- **Estructura:** 6 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_comprobante_pago)`; AK `(id_archivo_almacenado)`.
- **Dependencias funcionales:** `id_comprobante_pago` y `id_archivo_almacenado` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 81. `reembolso` - MANTENER

- **Finalidad:** Devolución que reduce aplicaciones originales sin reescribir pagos ni pedidos.
- **Estructura:** 12 atributos; 4 FK salientes.
- **Claves candidatas:** PK `(id_reembolso)`; AK `(codigo_publico)`; AK parcial propuesta `(id_integracion_externa, identificador_externo) WHERE identificador_externo IS NOT NULL`.
- **Dependencias funcionales:** `id_reembolso` y `codigo_publico` determinan la fila; cuando existe, `id_aplicacion_pago_pedido -> id_pago`; por proveedor debe cumplirse `(id_integracion_externa, identificador_externo) -> reembolso`.
- **Forma normal:** 2FN; no 3FN en filas ligadas a aplicacion porque `id_aplicacion_pago_pedido -> id_pago`; se tolera para soportar tambien devoluciones directas al pago.
- **Redundancia intencional:** Pago directo mas aplicacion opcional cubren devoluciones generales y asignadas; la igualdad se controla.
- **Veredicto y cambio:** Mantener la FK obligatoria a pago y la aplicacion opcional, agregar UK parcial `(id_integracion_externa, identificador_externo)` y comprobar que la aplicacion pertenece al mismo pago.
- **Efecto conservador:** Sin cambio de conteos; agrega idempotencia e integridad condicional.
- **Riesgo:** Es una excepcion controlada a 3FN; separarla en subtipos agregaria tablas sin cambiar el flujo aprobado.

#### 82. `historial_reembolso` - MANTENER

- **Finalidad:** Transiciones append-only de una devolución.
- **Estructura:** 8 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_historial_reembolso)`.
- **Dependencias funcionales:** `id_historial_reembolso` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF por evento; estado actual del reembolso es una proyeccion controlada.
- **Redundancia intencional:** Estados anterior/nuevo y actor son evidencia temporal append-only; no duplican la auditoria transversal.
- **Veredicto y cambio:** Mantener separada de la entidad corriente y de auditoria; materializar orden estable e inmutabilidad.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionarla con auditoria perderia semantica de dominio y fuente de verdad de transiciones.

#### 83. `cuenta_corriente` - MANTENER

- **Finalidad:** Cuenta financiera opcional y única de una empresa cliente.
- **Estructura:** 7 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_cuenta_corriente)`; AK `(id_empresa_cliente)`.
- **Dependencias funcionales:** `id_cuenta_corriente` y `id_empresa_cliente` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF interna; estado y limite actuales se proyectan junto a un historial append-only.
- **Redundancia intencional:** Estado y limite vigentes son proyeccion operativa; el historial conserva cada cambio.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 84. `historial_cuenta_corriente` - MANTENER

- **Finalidad:** Cambios inmutables de límite o estado de una cuenta corriente.
- **Estructura:** 10 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_historial_cuenta_corriente)`.
- **Dependencias funcionales:** `id_historial_cuenta_corriente` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF por evento; columnas opcionales dependen del tipo de cambio y requieren CHECK.
- **Redundancia intencional:** Estados anterior/nuevo y actor son evidencia temporal append-only; no duplican la auditoria transversal.
- **Veredicto y cambio:** Mantener separada de la entidad corriente y de auditoria; materializar orden estable e inmutabilidad.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionarla con auditoria perderia semantica de dominio y fuente de verdad de transiciones.

#### 85. `movimiento_cuenta_corriente` - REESTRUCTURAR

- **Finalidad:** Libro mayor append-only de débitos y créditos de la empresa.
- **Estructura:** 12 atributos; 6 FK salientes.
- **Claves candidatas:** PK `(id_movimiento_cuenta_corriente)`; AK parciales por `id_pago`, `id_reembolso`, `id_resolucion_reclamo` y cargo original por `id_pedido`.
- **Dependencias funcionales:** `id_pago`, `id_reembolso` o `id_resolucion_reclamo` determinan su movimiento cuando corresponden; un pedido determina un unico cargo original.
- **Forma normal:** BCNF por subtipo bajo las unicidades parciales declaradas; el XOR de origen necesita CHECK y validacion diferida.
- **Redundancia intencional:** Concepto, fecha y debe/haber son el libro mayor append-only y no deben sustituirse por saldos mutables.
- **Veredicto y cambio:** Convertirlo en origen unico de todo credito aplicable; agregar UK parcial `id_pedido WHERE tipo=CARGO` y conservar las UK parciales de pago, reembolso y resolucion.
- **Efecto conservador:** Sin cambio propio de conteos; habilita la fusion de las dos tablas de aplicacion.
- **Riesgo:** La transaccion debe asegurar misma cuenta y que cada movimiento fuente se aplique como maximo por su haber disponible.

#### 86. `aplicacion_pago_cuenta` - FUSIONAR

- **Finalidad:** Distribución manual de un pago de cuenta corriente entre cargos seleccionados.
- **Estructura:** 5 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_aplicacion_pago_cuenta)`; AK `(id_pago, id_movimiento_cargo)`.
- **Dependencias funcionales:** `(id_pago, id_movimiento_cargo)` determina importe y fecha. El pago ya determina un movimiento de credito unico.
- **Forma normal:** BCNF de forma aislada, pero crea una segunda ruta para consumir credito fuera del libro mayor.
- **Redundancia intencional:** El monto aplicado por cargo es obligatorio; la referencia directa al pago es el duplicado.
- **Veredicto y cambio:** Absorberla en `aplicacion_credito_cuenta`, renombrada `aplicacion_movimiento_cuenta`; toda aplicacion enlaza movimiento de credito con movimiento de cargo.
- **Efecto conservador:** -1 tabla; -5 atributos; -2 FK; -2 pares dirigidos.
- **Riesgo:** Antes de migrar debe existir exactamente un movimiento de credito por cada pago de cuenta corriente.

#### 87. `aplicacion_credito_cuenta` - REESTRUCTURAR

- **Finalidad:** Distribución de un crédito disponible entre cargos de la misma cuenta.
- **Estructura:** 5 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_aplicacion_credito_cuenta)`; AK `(id_movimiento_credito, id_movimiento_cargo)`.
- **Dependencias funcionales:** `(id_movimiento_credito, id_movimiento_cargo)` determina importe y fecha.
- **Forma normal:** BCNF: la pareja movimiento de origen-cargo es clave candidata.
- **Redundancia intencional:** La aplicacion es necesaria para distribuir un credito entre varios cargos sin alterar el libro.
- **Veredicto y cambio:** Renombrar a `aplicacion_movimiento_cuenta` y admitir cualquier movimiento con haber disponible, incluidos pagos, saldos a favor y compensaciones.
- **Efecto conservador:** Sin reduccion adicional; absorbe la tabla de aplicaciones de pago.
- **Riesgo:** Debe prohibir aplicar un cargo como origen, autoaplicaciones y cruces entre cuentas.

#### 88. `plantilla_documento_cobro` - MANTENER

- **Finalidad:** Plantilla normalizada e inmutable usada para reproducir documentos internos de cobro.
- **Estructura:** 10 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_plantilla_documento_cobro)`; AK `(id_configuracion_version)`.
- **Dependencias funcionales:** `id_plantilla_documento_cobro` y `id_configuracion_version` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF: configuracion_version es clave candidata y determina la plantilla completa.
- **Redundancia intencional:** La plantilla separada evita columnas incompletas durante configuraciones en preparacion y congela identidad emisora.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionar puede introducir columnas nulas en borradores y acoplar componentes con validacion propia.

#### 89. `documento_cobro` - REESTRUCTURAR

- **Finalidad:** Solicitud de pago interna, no fiscal, emitida sobre cargos ya existentes.
- **Estructura:** 20 atributos; 5 FK salientes.
- **Claves candidatas:** PK `(id_documento_cobro)`; AK `(codigo_publico)`; AK `(numero_secuencial)`; AK `(id_archivo_pdf)`; AK `(numero_visible)`.
- **Dependencias funcionales:** `id_archivo_pdf -> archivo_almacenado.sha256`; numero secuencial, visible, codigo publico y archivo son claves alternativas.
- **Forma normal:** BCNF interna: `id_archivo_pdf` es clave candidata. Su hash duplica el hash persistente del archivo.
- **Redundancia intencional:** Identidad del cliente, total, fechas y estado de pago son snapshot o cache reproducible del documento emitido.
- **Veredicto y cambio:** Eliminar `sha256_pdf` y consultar `archivo_almacenado.sha256`, que sobrevive incluso a una purga.
- **Efecto conservador:** 0 tablas; -1 atributo; sin cambio de FK ni pares.
- **Riesgo:** El registro de archivo debe ser inmutable y no eliminarse fisicamente.

#### 90. `linea_documento_cobro` - REESTRUCTURAR

- **Finalidad:** Línea congelada que reserva parte de un cargo para evitar doble documentación.
- **Estructura:** 8 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_linea_documento_cobro)`; AK `(id_documento_cobro, id_movimiento_cargo)`.
- **Dependencias funcionales:** `id_movimiento_cargo -> movimiento_cuenta_corriente.id_pedido`; la pareja documento-cargo determina la linea.
- **Forma normal:** 2FN; no alcanza 3FN porque `id_movimiento_cargo -> id_pedido` y un cargo puede aparecer en varios documentos.
- **Redundancia intencional:** Numero, concepto, importe y fecha son snapshots necesarios para reproducir el PDF.
- **Veredicto y cambio:** Eliminar `id_pedido`; conservar `numero_pedido_snapshot` y los demas valores impresos.
- **Efecto conservador:** 0 tablas; -1 atributo; -1 FK; -1 par dirigido.
- **Riesgo:** Bajo porque el movimiento es append-only; no debe borrarse aunque el pedido se anonimice.

### Programación y entregas

#### 91. `franja_entrega` - REESTRUCTURAR

- **Finalidad:** Plantilla semanal versionada de horario y capacidad para una modalidad y destino.
- **Estructura:** 10 atributos; 4 FK salientes.
- **Claves candidatas:** PK `(id_franja_entrega)`; la exclusion temporal impide intervalos superpuestos para la misma version, modalidad, destino efectivo y dia.
- **Dependencias funcionales:** La PK determina la fila. La combinacion de alcance y rango horario identifica un turno no superpuesto, cuya capacidad admite varios pedidos.
- **Forma normal:** BCNF aparente respecto de las dependencias documentadas; la exclusion temporal expresa una restriccion de intervalo, no una dependencia multivaluada.
- **Redundancia intencional:** Las tres FK de destino forman una union XOR con integridad tipada y evitan referencias polimorficas sin FK.
- **Veredicto y cambio:** Prohibir superposiciones para el mismo alcance. Una unica franja expresa el turno y `capacidad_pedidos` permite cero, uno o varios pedidos completos.
- **Efecto conservador:** Sin cambio de tablas, atributos, FK ni pares dirigidos.
- **Riesgo:** La futura exclusion PostgreSQL debe considerar correctamente el destino XOR y la version para no mezclar alcances distintos.

#### 92. `excepcion_franja_entrega` - MANTENER

- **Finalidad:** Cierre o capacidad especial de una franja en una fecha civil.
- **Estructura:** 6 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_excepcion_franja_entrega)`; AK `(id_franja_entrega, fecha)`.
- **Dependencias funcionales:** `id_excepcion_franja_entrega` y `(id_franja_entrega, fecha)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 93. `reserva_entrega` - MANTENER

- **Finalidad:** Cupo único reservado atómicamente al confirmar un pedido.
- **Estructura:** 18 atributos; 6 FK salientes.
- **Claves candidatas:** PK `(id_reserva_entrega)`; AK `(id_pedido)`.
- **Dependencias funcionales:** `id_reserva_entrega` y `id_pedido` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF: pedido es clave candidata; costo, destino y ventana son snapshot confirmado.
- **Redundancia intencional:** Destino, costo y ventana son snapshot del cupo confirmado y no se derivan de configuracion mutable.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 94. `historial_reserva_entrega` - MANTENER

- **Finalidad:** Historial append-only de reserva, liberación y reprogramación aceptada.
- **Estructura:** 11 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_historial_reserva_entrega)`.
- **Dependencias funcionales:** `id_historial_reserva_entrega` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF por evento; ventana y estado actuales son una proyeccion controlada.
- **Redundancia intencional:** Estados anterior/nuevo y actor son evidencia temporal append-only; no duplican la auditoria transversal.
- **Veredicto y cambio:** Mantener separada de la entidad corriente y de auditoria; materializar orden estable e inmutabilidad.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionarla con auditoria perderia semantica de dominio y fuente de verdad de transiciones.

#### 95. `direccion_entrega_snapshot` - MANTENER

- **Finalidad:** Dirección y contacto inmutables usados por un pedido con envío a domicilio.
- **Estructura:** 12 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_direccion_entrega_snapshot)`; AK `(id_pedido)`.
- **Dependencias funcionales:** `id_direccion_entrega_snapshot` y `id_pedido` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF: pedido es clave candidata y determina el snapshot completo.
- **Redundancia intencional:** La copia completa evita que editar la libreta del cliente cambie un pedido confirmado.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 96. `entrega` - REESTRUCTURAR

- **Finalidad:** Constancia inmutable de una entrega completa o su anulación correctiva.
- **Estructura:** 15 atributos; 4 FK salientes.
- **Claves candidatas:** PK `(id_entrega)`; unicidad parcial de entrega REGISTRADA por pedido; las anuladas conservan identidad propia.
- **Dependencias funcionales:** `id_reserva_entrega -> reserva_entrega.id_pedido`; la entrega registrada es unica parcialmente por pedido.
- **Forma normal:** 2FN; no alcanza 3FN porque `id_reserva_entrega -> id_pedido`.
- **Redundancia intencional:** Modalidad, ubicacion y receptor son evidencia congelada, no datos operativos duplicados.
- **Veredicto y cambio:** Eliminar `id_pedido` y derivarlo por la reserva obligatoria. Mantener snapshots y autorrelacion de anulacion.
- **Efecto conservador:** 0 tablas; -1 atributo; -1 FK; -1 par dirigido.
- **Riesgo:** Exige conservar siempre la reserva, incluso despues de cumplida o liberada.

### Reclamos y compensaciones

#### 97. `reclamo` - MANTENER

- **Finalidad:** Caso posterior que no reabre ni modifica el pedido cerrado.
- **Estructura:** 10 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_reclamo)`; AK `(codigo_publico)`.
- **Dependencias funcionales:** `id_reclamo` y `codigo_publico` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta redundancia interna material fuera de estados, fechas o metadatos propios de su ciclo de vida.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 98. `reclamo_item` - MANTENER

- **Finalidad:** Items específicos incluidos en un reclamo.
- **Estructura:** 3 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_reclamo_item)`; AK `(id_reclamo, id_pedido_item)`.
- **Dependencias funcionales:** `id_reclamo_item` y `(id_reclamo, id_pedido_item)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 99. `evidencia_reclamo` - MANTENER

- **Finalidad:** Archivo privado aportado como evidencia y retenido hasta resolver el caso.
- **Estructura:** 6 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_evidencia_reclamo)`; AK `(id_archivo_almacenado)`.
- **Dependencias funcionales:** `id_evidencia_reclamo` y `id_archivo_almacenado` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** El cargador de la evidencia puede ser quien la asocio, distinto del creador tecnico del archivo.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 100. `resolucion_reclamo` - REESTRUCTURAR

- **Finalidad:** Acción compensatoria inmutable; varias filas permiten combinaciones compatibles.
- **Estructura:** 11 atributos; 5 FK salientes.
- **Claves candidatas:** PK `(id_resolucion_reclamo)`; UK parciales por trabajo y reembolso. Reclamo, tipo e item no forman una clave porque una accion puede repetirse.
- **Dependencias funcionales:** La PK determina cada resolucion inmutable; trabajo y reembolso determinan como maximo una resolucion cuando existen.
- **Forma normal:** BCNF aparente con la PK y las UK de salida; no existe dependencia funcional declarada desde `(reclamo, tipo, item)`.
- **Redundancia intencional:** Las salidas opcionales tipadas preservan FK reales para reimpresion, reembolso o credito.
- **Veredicto y cambio:** Eliminar la UK `(id_reclamo, tipo)` y permitir varias resoluciones del mismo tipo, incluso sobre el mismo item, como hechos independientes ordenables por PK y fecha.
- **Efecto conservador:** Sin cambio de tablas, atributos, FK ni pares dirigidos.
- **Riesgo:** Las salidas tipadas y los limites monetarios deben validarse por resolucion para evitar reutilizar un trabajo o reembolso.

#### 101. `historial_reclamo` - MANTENER

- **Finalidad:** Transiciones append-only del estado de un reclamo.
- **Estructura:** 7 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_historial_reclamo)`.
- **Dependencias funcionales:** `id_historial_reclamo` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF por evento; estado actual del reclamo es una proyeccion controlada.
- **Redundancia intencional:** Estados anterior/nuevo y actor son evidencia temporal append-only; no duplican la auditoria transversal.
- **Veredicto y cambio:** Mantener separada de la entidad corriente y de auditoria; materializar orden estable e inmutabilidad.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Fusionarla con auditoria perderia semantica de dominio y fuente de verdad de transiciones.

### Notificaciones, alertas y auditoría

#### 102. `notificacion` - MANTENER

- **Finalidad:** Contenido persistente de un evento que puede tener uno o varios destinatarios.
- **Estructura:** 11 atributos; 5 FK salientes.
- **Claves candidatas:** PK `(id_notificacion)`.
- **Dependencias funcionales:** `id_notificacion` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Contenido comun separado del inbox evita duplicarlo para cada destinatario.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 103. `notificacion_usuario` - MANTENER

- **Finalidad:** Inbox personal con lectura; al eliminar se borra solo esta relación.
- **Estructura:** 5 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_notificacion_usuario)`; AK `(id_notificacion, id_usuario)`.
- **Dependencias funcionales:** `id_notificacion_usuario` y `(id_notificacion, id_usuario)` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** No se detecta duplicacion material: normaliza una relacion multivaluada o una aplicacion con atributos propios.
- **Veredicto y cambio:** Mantener la relacion explicita y materializar su clave natural compuesta ademas de la PK sustituta.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Eliminarla obligaria a usar listas o JSON y debilitara cardinalidad e integridad referencial.

#### 104. `dispositivo_usuario` - MANTENER

- **Finalidad:** Dispositivo autorizado opcionalmente para recibir push además del inbox.
- **Estructura:** 11 atributos; 1 FK salientes.
- **Claves candidatas:** PK `(id_dispositivo_usuario)`; AK `(codigo_publico)`; AK `(token_hash)`.
- **Dependencias funcionales:** `id_dispositivo_usuario` y `codigo_publico` y `token_hash` determinan todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Token cifrado permite envio y hash permite identificarlo sin exponerlo; ambos son deliberados.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 105. `envio_push` - REESTRUCTURAR

- **Finalidad:** Ciclo tecnico agregado de entrega push para una notificacion y un dispositivo.
- **Estructura:** 9 atributos; 2 FK salientes.
- **Claves candidatas:** PK `(id_envio_push)`; AK `(id_notificacion_usuario, id_dispositivo_usuario)`.
- **Dependencias funcionales:** La pareja notificacion-dispositivo determina estado, contador, identificador externo, resultado y fecha del ultimo intento.
- **Forma normal:** BCNF respecto de ambas claves candidatas; los campos tecnicos pertenecen al unico ciclo agregado.
- **Redundancia intencional:** Estado, contador y ultimo intento resumen los reintentos sin crear un historial tecnico que no fue requerido.
- **Veredicto y cambio:** Agregar UK `(id_notificacion_usuario, id_dispositivo_usuario)` y conservar una fila acumulativa por destino.
- **Efecto conservador:** Sin cambio de tablas, atributos, FK ni pares dirigidos.
- **Riesgo:** Los reintentos concurrentes deben bloquear o actualizar atomicamente la fila para no perder incrementos.

#### 106. `alerta` - MANTENER

- **Finalidad:** Situación operacional visible en contexto y en la vista interna de alertas.
- **Estructura:** 16 atributos; 7 FK salientes.
- **Claves candidatas:** PK `(id_alerta)`; unicidad parcial activa por `(tipo, causa, referencia causante)`.
- **Dependencias funcionales:** `id_alerta` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente por identidad; estado y contador son una proyeccion acumulada intencional.
- **Redundancia intencional:** Contador y fechas agregan recurrencias de una misma causa para no inundar el sistema.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

#### 107. `auditoria` - MANTENER

- **Finalidad:** Registro append-only de acciones sensibles sin duplicar historiales de dominio.
- **Estructura:** 12 atributos; 3 FK salientes.
- **Claves candidatas:** PK `(id_auditoria)`.
- **Dependencias funcionales:** `id_auditoria` determina todos los atributos no clave; no se documentan otras dependencias no triviales.
- **Forma normal:** BCNF aparente respecto de las dependencias funcionales documentadas.
- **Redundancia intencional:** Referencia logica y JSON saneado son deliberados: un log transversal no debe depender de FKs borrables de cada dominio.
- **Veredicto y cambio:** Mantener la entidad y materializar las restricciones declaradas sin fusionarla por similitud nominal.
- **Efecto conservador:** 0 tablas; 0 atributos; 0 FK; 0 pares dirigidos.
- **Riesgo:** Bajo si PK, FK, UK, CHECK y reglas temporales se implementan de forma transaccional.

## 5. Cambios evaluados y rechazados

| Propuesta | Decision | Motivo |
|---|---|---|
| Fusionar `politica_operativa` en `configuracion_version` | Rechazada en el escenario conservador | Una version EN_PREPARACION puede estar vacia; la separacion evita seis columnas condicionalmente nulas y valida el bloque completo. |
| Fusionar `accion_regla` en `regla_comercial` | Rechazada en el escenario conservador | Una regla en preparacion puede existir antes de tener accion; la fila 0..1 expresa completitud sin valores nulos. |
| Fusionar `plantilla_documento_cobro` en configuracion | Rechazada en el escenario conservador | La plantilla tiene validacion y completitud propias y puede no existir al iniciar el borrador. |
| Usar una sola tabla para todas las condiciones | Rechazada | Exigiria padre polimorfico o un supertipo artificial y mezclaria vocabularios de valores diferentes. |
| Fusionar todos los historiales en `auditoria` | Rechazada | Auditoria no es fuente de verdad del dominio y usa referencias logicas deliberadamente debiles. |
| Eliminar snapshots de pedido y usar solo cotizacion | Rechazada | Contradice la copia completa aprobada y acopla oferta con compromiso operativo. |
| Guardar servicios, reglas o relaciones en JSON | Rechazada | Pierde claves, FK, cardinalidad y consultas relacionales; el JSON queda limitado a metadatos variables. |
| Dividir cada XOR en subtablas | Rechazada | Aumenta tablas sin aportar comportamiento; CHECK y FK tipadas cubren el conjunto cerrado actual. |
| Eliminar tablas de historial por existir estado actual | Rechazada | Impediria explicar transiciones, actores y reversiones. |
| Desnormalizar por rendimiento | Rechazada | No existen carga, consultas ni mediciones que demuestren la necesidad. |

## 6. Escenario conservador adoptado

### Operaciones

1. Eliminar `archivo_almacenado.id_archivo_derivado_de`.
2. Eliminar `impresora.id_sucursal`.
3. Eliminar `licencia_local.id_instalacion` e `integracion_externa.id_instalacion`; hacer `integracion_externa.tipo` unico.
4. Eliminar `trabajo_impresion.id_configuracion_version`.
5. Eliminar `control_calidad.id_pedido_item`.
6. Eliminar `linea_documento_cobro.id_pedido`.
7. Eliminar `entrega.id_pedido`.
8. Eliminar `intento_pago.moneda` y `pago.moneda`; declarar ARS como invariante.
9. Eliminar `documento_cobro.sha256_pdf`.
10. Renombrar `aplicacion_credito_cuenta` a `aplicacion_movimiento_cuenta` y generalizar su movimiento origen.
11. Fusionar `aplicacion_pago_cuenta` dentro de esa tabla generalizada.
12. Agregar las claves, CHECK, exclusiones y validaciones cruzadas de la seccion 3.5.
13. Prohibir franjas superpuestas del mismo alcance, permitir resoluciones repetidas e identificar un envio push por notificacion y dispositivo.

### Resultado aplicado al DER v3

| Metrica | Actual | Conservador | Diferencia |
|---|---:|---:|---:|
| Entidades | 107 | 106 | -1 |
| Atributos | 962 | 946 | -16 |
| FK | 239 | 229 | -10 |
| Pares dirigidos | 227 | 217 | -10 |

La reduccion de atributos se compone de cinco columnas de la tabla absorbida, seis FK transitivas, dos FK singleton, dos columnas `moneda` y un hash duplicado. La tabla generalizada conserva sus cinco atributos actuales; no aparece un nuevo concepto.

## 7. Escenario agresivo comparativo

Este escenario parte del conservador y agrega seis fusiones. No se recomienda porque ahorra pocas relaciones a cambio de entidades mas anchas, columnas nulas durante preparacion y ciclos de vida mezclados.

1. Absorber `politica_operativa` en `configuracion_version`.
2. Absorber `accion_regla` en `regla_comercial`.
3. Absorber `plantilla_documento_cobro` en `configuracion_version` y referenciar esa version desde el documento.
4. Absorber `senia` en `pedido`.
5. Absorber `direccion_entrega_snapshot` en `pedido`.
6. Absorber `control_calidad` en `trabajo_impresion`.

| Metrica | Actual | Conservador | Agresivo | Diferencia agresiva total |
|---|---:|---:|---:|---:|
| Entidades | 107 | 106 | 100 | -7 |
| Atributos | 962 | 946 | 934 | -28 |
| FK | 239 | 229 | 223 | -16 |
| Pares dirigidos | 227 | 217 | 210 | -17 |

Los conteos contemplan mover las FK de logo, version documental, regla de sena, direccion origen e inspector a sus tablas supervivientes. La fusion de control de calidad elimina dos pares porque `trabajo_impresion -> usuario` ya existe.

## 8. Reconciliacion numerica

| Operacion conservadora | Tablas | Atributos | FK | Pares |
|---|---:|---:|---:|---:|
| Base | 107 | 962 | 239 | 227 |
| Fusion de aplicaciones de cuenta | -1 | -5 | -2 | -2 |
| Seis FK transitivas eliminadas | 0 | -6 | -6 | -6 |
| Dos FK de alcance singleton | 0 | -2 | -2 | -2 |
| Dos monedas constantes | 0 | -2 | 0 | 0 |
| Hash de PDF duplicado | 0 | -1 | 0 | 0 |
| **Total conservador** | **106** | **946** | **229** | **217** |

| Operacion agresiva adicional | Tablas | Atributos | FK | Pares |
|---|---:|---:|---:|---:|
| Seis fusiones 1:1 o 0..1 | -6 | -12 | -6 | -7 |
| **Total agresivo** | **100** | **934** | **223** | **210** |

## 9. Matriz de regresion de flujos

| Flujo | Invariantes que deben sobrevivir | Efecto conservador |
|---|---|---|
| Alta personal y empresarial | Correo unico, rol/empresa XOR, un administrador empresarial | Sin cambio; identidad permanece separada. |
| SOPORTE y administradores | Ventana maxima, revocacion, propietario unico, verificacion externa | Sin cambio; autorizaciones y credenciales conservan tablas propias. |
| Configuracion | Version completa, inmutable, una activa, borrador inicialmente vacio | Sin fusion de bloques 0..1; se preserva completitud gradual. |
| Cotizacion y confirmacion | Oferta inmutable, copia al pedido, credito y cupo atomicos | Sin eliminar snapshots; solo se refuerzan claves de reglas y servicios. |
| Archivos | Original unico, conversion trazable, transferencia XOR | Conversion queda como unica fuente de derivacion aprobada. |
| Produccion | Un trabajo activo, claim por lease, agente/impresora historicos, calidad humana | Version y item se derivan por rutas inmutables; hechos tecnicos permanecen. |
| Pago normal | Intento no mueve dinero, pago confirmado, aplicaciones limitadas | Solo se elimina moneda constante; snapshots del pago se conservan. |
| Cuenta corriente | Cargo unico por pedido, saldo a favor, aplicacion manual, reversiones | Todo credito se aplica desde movimientos del mismo libro; elimina doble fuente. |
| Documento de cobro | No genera deuda, lineas congeladas, PDF inmutable | Pedido se deriva del cargo y hash del archivo; los valores impresos permanecen. |
| Entrega | Reserva unica, cupo, snapshot, entrega completa y anulacion | Pedido se deriva de reserva; evidencia de entrega no cambia. |
| Reclamos | No reabre pedido, varias acciones compatibles, trazabilidad | Permite acciones repetidas como hechos independientes; conserva salidas unicas. |
| Notificaciones y alertas | Inbox personal, push adicional, alerta operacional, auditoria append-only | Sin fusion; cada notificacion y dispositivo mantiene un ciclo push agregado. |
| Licencias e integraciones | Cache verificable, tolerancia, idempotencia, conciliacion | Sin cambio estructural; se mantienen eventos y corridas separados. |

## 10. Restricciones y fronteras transaccionales futuras

- Confirmar cotizacion debe bloquear cotizacion, credito y cupo, transferir archivos y crear pedido, items, cargo o reserva en una sola transaccion.
- Crear un pedido a cuenta debe insertar exactamente un movimiento `CARGO`; cancelar o rechazar crea un movimiento inverso enlazado, nunca actualiza el original.
- Registrar un pago de cuenta debe crear exactamente un movimiento con haber y las aplicaciones deben bloquear origen y cargos para no exceder saldos concurrentemente.
- Emitir documento debe reservar importes de cargos, generar archivo y persistir documento/lineas atomicamente; una falla no deja reservas parciales.
- Asignar impresora y reclamar un trabajo exige compatibilidad, sucursal derivada, estado, lease atomico y prohibicion de reintento automatico despues de CUPS.
- Reprogramar entrega debe liberar y reservar capacidad con bloqueo de franja y conservar historial y aceptacion.
- Todas las UK de estado activo, XOR y sumas financieras requieren indices parciales, CHECK, triggers o servicios transaccionales explicitamente probados en el futuro DDL.
- Las tablas append-only deben negar `UPDATE` y `DELETE` ordinarios a los roles de aplicacion; las correcciones crean hechos inversos.

## 11. Decisiones cerradas y limites conservados

1. **Franjas de entrega:** ventanas del mismo destino efectivo, version, modalidad y dia se excluyen entre si. Una unica franja expresa capacidad para varios pedidos.
2. **Resoluciones de reclamo:** pueden repetirse acciones del mismo tipo, incluso sobre el mismo item; cada fila es un hecho inmutable independiente.
3. **Envio push:** una fila representa el ciclo agregado por notificacion y dispositivo, con contador y ultimo resultado.
4. **Reembolso ligado a aplicacion:** el modelo conserva la FK directa al pago como excepcion controlada. Si se exige BCNF estricta, habria que separar origen directo y origen por aplicacion, aumentando tablas.
5. **Evolucion multimoneda:** queda fuera del alcance aprobado. Agregar una segunda moneda exigiria modelar moneda en precios, limites, documentos, aplicaciones y reglas, no solo conservar dos columnas.

## 12. Conclusion

La cantidad de tablas resultante es razonable para el alcance y la trazabilidad elegidos. La simplificacion conservadora adoptada es pequena pero elimina las duplicaciones que realmente podian producir dos fuentes de verdad. Las fusiones visualmente tentadoras se mantienen descartadas porque modelan hechos distintos o estados incompletos validos.

El DER v3 aplica el escenario conservador completo y cierra los tres puntos que estaban pendientes. El Excalidraw correspondiente conserva una vista general con relaciones agregadas por dominio y una segunda vista con un grafo repetido por entidad; el SVG permite consultarlo directamente desde el repositorio. El DDL, las migraciones y la implementacion Spring Boot siguen requiriendo una tarea material posterior.
