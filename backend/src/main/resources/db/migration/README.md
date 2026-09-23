<!--
FUNCIÓN: índice de lectura de migraciones históricas; no es una migración ejecutable.
CONTENIDO: propósito, funciones, tablas y disparadores de V1 a V38.
MÉTODOS: no declara código ejecutable.
-->
# Índice de migraciones del backend

Las migraciones SQL existentes se conservan byte por byte: agregarles incluso comentarios cambiaría los checksums que valida Flyway. Este índice documenta cada archivo sin modificarlo. Los nombres de objetos son técnicos y se mantienen aunque la interfaz use otra terminología.

Cada sección puede plegarse por su encabezado en el editor. Las funciones indicadas son las declaradas o reemplazadas **en esa migración**; versiones posteriores pueden modificarlas. Los objetos listados se extraen de las declaraciones SQL.

## V1__base_tecnica.sql

Reserva la primera versión técnica de Flyway. No crea cuentas ni precarga datos de negocio.

[Abrir SQL](V1__base_tecnica.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

---

## V2__identidad_inicial.sql

Crea identidad inicial, roles, usuarios, control de instalación única, eventos de acceso y tablas de sesiones JDBC.

[Abrir SQL](V2__identidad_inicial.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.rol`.
- `lamontana.usuario`.
- `lamontana.inicializacion_sistema`.
- `lamontana.evento_acceso`.
- `lamontana.sesion_http`.
- `lamontana.sesion_http_attributes`.

**Índices creados**

- `usuario_correo_unico`.
- `usuario_propietario_unico`.
- `sesion_http_id`.
- `sesion_http_expira`.
- `sesion_http_principal`.

---

## V3__clientes_particulares.sql

Registra el rol técnico del cliente particular sin crear cuentas de clientes.

[Abrir SQL](V3__clientes_particulares.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Datos técnicos insertados en**

- `lamontana.rol`.

---

## V4__sucursales.sql

Crea las sucursales con identidad, dirección, estado y zona horaria.

[Abrir SQL](V4__sucursales.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.sucursal`.

---

## V5__empleados_y_permisos.sql

Incorpora empleados, permisos y asignaciones por sucursal, control de versión y auditoría de organización.

[Abrir SQL](V5__empleados_y_permisos.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.permiso`.
- `lamontana.usuario_permiso`.
- `lamontana.usuario_sucursal`.
- `lamontana.evento_organizacion`.

**Tablas modificadas**

- `lamontana.usuario`.
- `lamontana.sucursal`.

**Índices creados**

- `usuario_permiso_vigente`.
- `usuario_sucursal_vigente`.
- `usuario_sucursal_sucursal`.

---

## V6__correo_y_recuperacion.sql

Agrega cambio obligatorio de contraseña, versión de acceso y credenciales temporales para verificación y recuperación.

[Abrir SQL](V6__correo_y_recuperacion.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.credencial_temporal`.
- `lamontana.token_verificacion_correo`.

**Tablas modificadas**

- `lamontana.usuario`.

**Índices creados**

- `credencial_usuario`.
- `verificacion_usuario`.

---

## V7__catalogo_comercial.sql

Crea catálogo base y configuraciones comerciales con tarifas, servicios, compatibilidades y auditoría.

[Abrir SQL](V7__catalogo_comercial.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.formato`.
- `lamontana.papel`.
- `lamontana.servicio`.
- `lamontana.catalogo_revision`.
- `lamontana.tarifa_impresion`.
- `lamontana.configuracion_servicio`.
- `lamontana.compatibilidad_servicio`.
- `lamontana.evento_catalogo`.

**Índices creados**

- `catalogo_revision_vigente`.

---

## V8__programacion_catalogo.sql

Extiende el catálogo con vigencia programada y cancelación, conservando las configuraciones y comprobantes anteriores.

[Abrir SQL](V8__programacion_catalogo.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.catalogo_cancelacion`.

**Tablas modificadas**

- `lamontana.catalogo_revision`.

**Índices creados**

- `catalogo_una_programada`.

---

## V9__borrador_configuracion_operativa.sql

Crea versiones y borradores de configuración operativa, comprobantes de operación y eventos.

[Abrir SQL](V9__borrador_configuracion_operativa.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.configuracion_version`.
- `lamontana.comprobante_configuracion`.
- `lamontana.evento_configuracion`.

**Índices creados**

- `configuracion_un_borrador`.

---

## V10__cancelacion_segura_configuracion.sql

Agrega cancelación de borradores y desafíos de autorización vinculados al propósito, usuario y versión.

[Abrir SQL](V10__cancelacion_segura_configuracion.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.autorizacion_configuracion`.

**Tablas modificadas**

- `lamontana.configuracion_version`.
- `lamontana.comprobante_configuracion`.
- `lamontana.evento_configuracion`.

**Índices creados**

- `autorizacion_configuracion_actor`.
- `autorizacion_configuracion_borrador`.

---

## V11__parametros_financieros.sql

Incorpora reglas financieras y medios de pago por configuración, junto con sus operaciones y eventos.

[Abrir SQL](V11__parametros_financieros.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.configuracion_financiera`.
- `lamontana.configuracion_medio_pago`.

**Tablas modificadas**

- `lamontana.comprobante_configuracion`.
- `lamontana.evento_configuracion`.

---

## V12__recursos_manuales_versionados.sql

Incorpora identidad de impresoras, capacidades y estados por versión y habilitación de servicios por sucursal.

[Abrir SQL](V12__recursos_manuales_versionados.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.impresora`.
- `lamontana.configuracion_recursos`.
- `lamontana.configuracion_impresora`.
- `lamontana.configuracion_impresora_formato`.
- `lamontana.sucursal_servicio`.

**Tablas modificadas**

- `lamontana.comprobante_configuracion`.
- `lamontana.evento_configuracion`.

---

## V13__horarios_y_entrega_versionados.sql

Agrega tiempos de preparación y traslado, modalidades y calendarios de entrega por configuración.

[Abrir SQL](V13__horarios_y_entrega_versionados.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.configuracion_entrega`.
- `lamontana.configuracion_modalidad_entrega`.
- `lamontana.configuracion_horario_sucursal`.
- `lamontana.horario_sucursal`.

**Tablas modificadas**

- `lamontana.comprobante_configuracion`.
- `lamontana.evento_configuracion`.

---

## V14__puntos_entrega_estructurales.sql

Crea identidades y definiciones versionadas de puntos de entrega, relaciones por sucursal, costos y franjas.

[Abrir SQL](V14__puntos_entrega_estructurales.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.punto_entrega`.
- `lamontana.configuracion_definicion_punto`.
- `lamontana.configuracion_punto_entrega`.
- `lamontana.franja_entrega`.

**Tablas modificadas**

- `lamontana.comprobante_configuracion`.
- `lamontana.evento_configuracion`.

---

## V15__disponibilidad_temporal_puntos.sql

Registra disponibilidad temporal y eventos de puntos de entrega, independientes de los parámetros versionados.

[Abrir SQL](V15__disponibilidad_temporal_puntos.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.disponibilidad_punto_entrega`.
- `lamontana.evento_disponibilidad_punto`.

---

## V16__zonas_entrega_versionadas.sql

Incorpora zonas de envío, territorios con exclusividad de cobertura y franjas asociadas a la zona.

[Abrir SQL](V16__zonas_entrega_versionadas.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.zona_entrega`.
- `lamontana.configuracion_zona_entrega`.
- `lamontana.zona_entrega_codigo_postal`.

**Tablas modificadas**

- `lamontana.franja_entrega`.
- `lamontana.comprobante_configuracion`.
- `lamontana.evento_configuracion`.

**Índices creados**

- `cobertura_habilitada_unica`.

---

## V17__activacion_configuracion.sql

Agrega activación y fin de vigencia de configuraciones operativas, con una única activa y autorización específica.

[Abrir SQL](V17__activacion_configuracion.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.activacion_configuracion`.

**Tablas modificadas**

- `lamontana.configuracion_version`.
- `lamontana.autorizacion_configuracion`.
- `lamontana.comprobante_configuracion`.
- `lamontana.evento_configuracion`.

**Índices creados**

- `configuracion_una_activa`.

---

## V18__programacion_configuracion.sql

Incorpora programación e intentos de activación, exclusión entre cambios pendientes y eventos de ejecución automática.

[Abrir SQL](V18__programacion_configuracion.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.programacion_configuracion`.
- `lamontana.intento_activacion_configuracion`.

**Tablas modificadas**

- `lamontana.configuracion_version`.
- `lamontana.activacion_configuracion`.
- `lamontana.evento_configuracion`.
- `lamontana.autorizacion_configuracion`.
- `lamontana.comprobante_configuracion`.

**Índices creados**

- `configuracion_un_pendiente`.
- `configuracion_un_intento`.
- `configuracion_intentos_version`.

---

## V19__intentos_manuales_configuracion.sql

Extiende los intentos de activación para registrar ejecuciones manuales y sus resultados durables.

[Abrir SQL](V19__intentos_manuales_configuracion.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas modificadas**

- `lamontana.intento_activacion_configuracion`.
- `lamontana.autorizacion_configuracion`.
- `lamontana.comprobante_configuracion`.
- `lamontana.evento_configuracion`.

---

## V20__identidad_auditoria_configuracion.sql

Captura nombre y rol del actor en los hechos de configuración para conservar la identidad histórica de auditoría.

[Abrir SQL](V20__identidad_auditoria_configuracion.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.capturar_actor_configuracion() → trigger`.

**Tablas modificadas**

- `lamontana.configuracion_version`.
- `lamontana.evento_configuracion`.
- `lamontana.programacion_configuracion`.
- `lamontana.activacion_configuracion`.
- `lamontana.intento_activacion_configuracion`.

**Disparadores declarados**

- `capturar_creador`.
- `capturar_evento`.
- `capturar_programador`.
- `capturar_activador`.
- `capturar_intento`.

**Índices creados**

- `configuracion_eventos_orden`.

---

## V21__copia_y_reconfirmacion_configuracion.sql

Registra el origen de configuraciones copiadas y la reconfirmación requerida antes de usarlas.

[Abrir SQL](V21__copia_y_reconfirmacion_configuracion.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.origen_configuracion`.
- `lamontana.reconfirmacion_configuracion`.

**Tablas modificadas**

- `lamontana.comprobante_configuracion`.
- `lamontana.evento_configuracion`.

---

## V22__rollback_manual_configuracion.sql

Incorpora reversión manual de configuración y verifica la coherencia del intento con su origen y objetivo.

[Abrir SQL](V22__rollback_manual_configuracion.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.comprobar_rollback() → trigger`.

**Tablas modificadas**

- `lamontana.origen_configuracion`.
- `lamontana.intento_activacion_configuracion`.
- `lamontana.autorizacion_configuracion`.
- `lamontana.comprobante_configuracion`.
- `lamontana.evento_configuracion`.

**Disparadores declarados**

- `rollback_coherente`.

---

## V23__cotizaciones_particulares.sql

Crea cotizaciones e ítems con precios y condiciones conservados, eventos y protecciones de inmutabilidad.

[Abrir SQL](V23__cotizaciones_particulares.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.proteger_cotizacion() → trigger`.
- `lamontana.proteger_detalle_cotizacion() → trigger`.

**Tablas creadas**

- `lamontana.cotizacion`.
- `lamontana.cotizacion_item`.
- `lamontana.cotizacion_item_servicio`.
- `lamontana.evento_cotizacion`.

**Disparadores declarados**

- `cotizacion_inmutable`.
- `cotizacion_item_inmutable`.
- `cotizacion_servicio_inmutable`.
- `evento_cotizacion_inmutable`.

**Índices creados**

- `cotizacion_cliente_reciente`.
- `cotizacion_item_impresion_unica`.

---

## V24__archivos_pdf_privados.sql

Crea registros de PDF privados, resultados de validación y comprobantes de operaciones sobre archivos.

[Abrir SQL](V24__archivos_pdf_privados.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.proteger_archivo_pdf() → trigger`.
- `lamontana.proteger_relacion_archivo() → trigger`.
- `lamontana.exigir_original_validado() → trigger`.
- `lamontana.exigir_preview_validada() → trigger`.

**Tablas creadas**

- `lamontana.archivo_almacenado`.
- `lamontana.archivo_trabajo`.
- `lamontana.validacion_archivo`.
- `lamontana.aceptacion_vista_previa`.

**Disparadores declarados**

- `archivo_pdf_inmutable`.
- `archivo_relacion_inmutable`.
- `archivo_validacion_inmutable`.
- `archivo_aceptacion_inmutable`.
- `archivo_activo_validado`.
- `preview_validada`.

**Índices creados**

- `archivo_original_activo`.
- `archivo_por_item`.

---

## V25__dinero_recibido_y_aplicacion.sql

Distingue avisos de pago, dinero recibido, aplicaciones, liberaciones y devoluciones, con funciones de cobertura y restricciones de integridad.

[Abrir SQL](V25__dinero_recibido_y_aplicacion.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.cotizacion_descendiente(destino bigint, origen bigint) → boolean`.
- `lamontana.validar_libro_pagos() → trigger`.
- `lamontana.bloquear_libro_pagos() → trigger`.

**Tablas creadas**

- `lamontana.evento_financiero`.
- `lamontana.intento_pago`.
- `lamontana.resolucion_intento_pago`.
- `lamontana.pago`.
- `lamontana.aplicacion_pago_cotizacion`.
- `lamontana.liberacion_aplicacion_pago`.
- `lamontana.reembolso`.
- `lamontana.reembolso_aplicacion`.

**Índices creados**

- `pago_cotizacion`.
- `intento_cotizacion`.
- `aplicacion_cotizacion`.
- `aplicacion_pago`.

---

## V26__comprobantes_privados.sql

Agrega comprobantes financieros privados vinculados a intentos o pagos, resultados de validación y trazabilidad de cargas.

[Abrir SQL](V26__comprobantes_privados.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.exigir_finalidad_archivo() → trigger`.

**Tablas creadas**

- `lamontana.comprobante_pago`.

**Tablas modificadas**

- `lamontana.archivo_almacenado`.

**Disparadores declarados**

- `comprobante_inmutable`.
- `trabajo_finalidad`.
- `comprobante_finalidad`.

**Índices creados**

- `comprobante_intento`.
- `comprobante_por_pago`.

---

## V27__franjas_retiro_sucursal.sql

Incorpora franjas y cupos de retiro por sucursal dentro de las versiones de configuración.

[Abrir SQL](V27__franjas_retiro_sucursal.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas modificadas**

- `lamontana.franja_entrega`.

---

## V28__confirmacion_y_reserva_pedido.sql

Crea pedidos confirmados, su vínculo con PDF, historial y reservas de entrega, y funciones que protegen capacidad y confirmación.

[Abrir SQL](V28__confirmacion_y_reserva_pedido.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.reservas_ocupadas(modo text,destino uuid,origen uuid,fecha date,abre time,cierra time,desde timestamptz,hasta timestamptz) → bigint`.
- `lamontana.validar_pedido_completo() → trigger`.
- `lamontana.validar_cupo_reserva() → trigger`.
- `lamontana.exigir_pedido_confirmado() → trigger`.

**Tablas creadas**

- `lamontana.pedido`.
- `lamontana.pedido_item`.
- `lamontana.reserva_entrega`.
- `lamontana.historial_estado_pedido`.

**Disparadores declarados**

- `pedido_completo`.
- `reserva_cupo`.
- `pedido_inmutable`.
- `pedido_item_inmutable`.
- `reserva_inmutable`.
- `pedido_historia_inmutable`.
- `cotizacion_con_pedido`.

**Índices creados**

- `pedido_cliente`.
- `pedido_sucursal`.
- `reserva_destino`.

---

## V29__revision_administrativa_pedido.sql

Incorpora revisión administrativa y observaciones de pedidos, permisos y controles para transiciones y liberación de reservas.

[Abrir SQL](V29__revision_administrativa_pedido.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.controlar_transicion_pedido() → trigger`.
- `lamontana.controlar_liberacion_reserva() → trigger`.
- `lamontana.validar_transicion_pedido_completa() → trigger`.

**Tablas creadas**

- `lamontana.observacion_interna`.

**Tablas modificadas**

- `lamontana.pedido`.
- `lamontana.reserva_entrega`.
- `lamontana.historial_estado_pedido`.

**Disparadores declarados**

- `observacion_inmutable`.
- `pedido_transicion`.
- `reserva_transicion`.
- `pedido_transicion_completa`.

**Índices creados**

- `pedido_evento_version`.
- `observacion_pedido`.

---

## V30__correcciones_y_versiones_de_trabajo.sql

Agrega solicitudes y respuestas de corrección, cotizaciones asociadas y versiones de trabajo, conservando historia y coherencia de reservas.

[Abrir SQL](V30__correcciones_y_versiones_de_trabajo.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.proteger_solicitud_correccion() → trigger`.
- `lamontana.reservas_ocupadas(modo text,destino uuid,origen uuid,fecha date,abre time,cierra time,desde timestamptz,hasta timestamptz,excluir_pedido bigint) → bigint`.
- `lamontana.reservas_ocupadas(modo text,destino uuid,origen uuid,fecha date,abre time,cierra time,desde timestamptz,hasta timestamptz) → bigint`.
- `lamontana.controlar_transicion_pedido() → trigger`.
- `lamontana.controlar_liberacion_reserva() → trigger`.
- `lamontana.validar_transicion_pedido_completa() → trigger`.
- `lamontana.exigir_pedido_confirmado() → trigger`.
- `lamontana.validar_cotizacion_de_correccion() → trigger`.
- `lamontana.impedir_segundo_pedido() → trigger`.
- `lamontana.validar_solicitud_completa() → trigger`.
- `lamontana.validar_respuesta_completa() → trigger`.
- `lamontana.validar_reserva_actual_pedido() → trigger`.
- `lamontana.validar_reserva_version_trabajo() → trigger`.

**Tablas creadas**

- `lamontana.solicitud_correccion`.
- `lamontana.solicitud_correccion_item`.
- `lamontana.cotizacion_correccion`.
- `lamontana.respuesta_correccion`.
- `lamontana.respuesta_correccion_archivo`.

**Tablas modificadas**

- `lamontana.archivo_trabajo`.
- `lamontana.pedido`.
- `lamontana.historial_estado_pedido`.
- `lamontana.reserva_entrega`.

**Disparadores declarados**

- `solicitud_correccion_historia`.
- `cotizacion_correccion_valida`.
- `pedido_cotizacion_original`.
- `solicitud_correccion_completa`.
- `respuesta_correccion_completa`.
- `reserva_pedido_actual`.
- `reserva_version_trabajo`.

**Índices creados**

- `correccion_activa_pedido`.
- `reserva_activa_pedido`.

---

## V31__produccion_manual_y_calidad.sql

Incorpora trabajos de impresión manual, historial y calidad, con validaciones de cobertura, estados y transiciones del pedido.

[Abrir SQL](V31__produccion_manual_y_calidad.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.produccion_cubierta(pedido bigint) → boolean`.
- `lamontana.calidad_pedido_aprobada(pedido bigint) → boolean`.
- `lamontana.validar_trabajo_manual() → trigger`.
- `lamontana.validar_calidad_manual() → trigger`.
- `lamontana.controlar_transicion_pedido() → trigger`.
- `lamontana.validar_transicion_pedido_completa() → trigger`.
- `lamontana.validar_evento_trabajo() → trigger`.
- `lamontana.exigir_historia_trabajo() → trigger`.

**Tablas creadas**

- `lamontana.trabajo_impresion`.
- `lamontana.historial_trabajo_impresion`.
- `lamontana.control_calidad`.

**Tablas modificadas**

- `lamontana.pedido`.
- `lamontana.historial_estado_pedido`.

**Disparadores declarados**

- `trabajo_historial_inmutable`.
- `control_calidad_inmutable`.
- `trabajo_manual_valido`.
- `calidad_manual_valida`.
- `evento_trabajo_completo`.
- `trabajo_con_historia`.
- `calidad_con_historia`.

**Índices creados**

- `trabajo_activo_item`.
- `trabajo_pedido`.

---

## V32__logistica_entrega_y_cierre.sql

Registra códigos, movimientos, entrega física y cierre de pedidos, verificando logística, saldo y conciliación y protegiendo operaciones cerradas.

[Abrir SQL](V32__logistica_entrega_y_cierre.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.estado_logistico(pedido bigint) → text`.
- `lamontana.saldo_entrega_cubierto(pedido bigint) → boolean`.
- `lamontana.validar_codigo_entrega() → trigger`.
- `lamontana.validar_movimiento_entrega() → trigger`.
- `lamontana.validar_entrega_registrada() → trigger`.
- `lamontana.dinero_pendiente_cierre(pedido bigint) → numeric`.
- `lamontana.informes_pendientes_cierre(pedido bigint) → bigint`.
- `lamontana.validar_cierre_pedido() → trigger`.
- `lamontana.controlar_transicion_pedido() → trigger`.
- `lamontana.validar_transicion_pedido_completa() → trigger`.
- `lamontana.controlar_liberacion_reserva() → trigger`.
- `lamontana.proteger_aplicacion_cerrada() → trigger`.
- `lamontana.validar_reserva_actual_pedido() → trigger`.

**Tablas creadas**

- `lamontana.codigo_entrega`.
- `lamontana.validacion_codigo_entrega`.
- `lamontana.movimiento_entrega`.
- `lamontana.entrega`.
- `lamontana.cierre_pedido`.

**Tablas modificadas**

- `lamontana.pedido`.
- `lamontana.reserva_entrega`.
- `lamontana.historial_estado_pedido`.

**Disparadores declarados**

- `codigo_entrega_control`.
- `movimiento_entrega_valido`.
- `entrega_registrada_completa`.
- `cierre_pedido_completo`.
- `reembolso_cierre_guard`.
- `liberacion_cierre_guard`.

**Índices creados**

- `codigo_entrega_actual`.

---

## V33__reprogramacion_aceptada.sql

Agrega propuestas y resoluciones de reprogramación: la aceptación reemplaza la reserva y preserva coherencia con versión de trabajo y estados.

[Abrir SQL](V33__reprogramacion_aceptada.sql)

**Funciones declaradas o reemplazadas**

- `lamontana.proteger_propuesta_reprogramacion() → trigger`.
- `lamontana.validar_resolucion_reprogramacion() → trigger`.
- `lamontana.validar_reserva_reprogramada() → trigger`.
- `lamontana.controlar_transicion_pedido() → trigger`.
- `lamontana.validar_transicion_pedido_completa() → trigger`.
- `lamontana.controlar_liberacion_reserva() → trigger`.
- `lamontana.validar_reserva_version_trabajo() → trigger`.

**Tablas creadas**

- `lamontana.propuesta_reprogramacion`.
- `lamontana.resolucion_reprogramacion`.

**Tablas modificadas**

- `lamontana.reserva_entrega`.
- `lamontana.historial_estado_pedido`.

**Disparadores declarados**

- `resolucion_reprogramacion_inmutable`.
- `propuesta_reprogramacion_control`.
- `reprogramacion_resuelta`.
- `reprogramacion_aceptada`.
- `reserva_reprogramada_aceptada`.

**Índices creados**

- `una_reprogramacion_pendiente`.

---

## V34__pagina_web.sql

Crea biblioteca de imágenes, borrador web, publicaciones, vínculos de imágenes y comprobantes idempotentes, conservando las publicaciones inmutables.

[Abrir SQL](V34__pagina_web.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.web_imagen`.
- `lamontana.web_publicacion`.
- `lamontana.web_publicacion_imagen`.
- `lamontana.web_borrador`.
- `lamontana.web_operacion`.

**Disparadores declarados**

- `web_publicacion_inmutable`.
- `web_publicacion_imagen_inmutable`.
- `web_operacion_inmutable`.

---

## V35__integridad_imagenes_web.sql

Agrega huellas SHA-256 para verificar la integridad de las imágenes públicas normalizadas y sus miniaturas.

[Abrir SQL](V35__integridad_imagenes_web.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas modificadas**

- `lamontana.web_imagen`.

---

## V36__horario_atencion_sucursal.sql

Crea el horario habitual de las sucursales y recupera únicamente calendarios activos completos, sin alterar las versiones operativas.

[Abrir SQL](V36__horario_atencion_sucursal.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.sucursal_horario_atencion`.

---

## V37__papeles_habilitados_catalogo.sql

Crea la selección explícita de combinaciones de formato y papel; conserva las combinaciones disponibles antes de migrar.

[Abrir SQL](V37__papeles_habilitados_catalogo.sql)

**Funciones declaradas o reemplazadas**

- No declara funciones.

**Tablas creadas**

- `lamontana.catalogo_papel`.

---


---

## V38__tarifas_agrupadas_por_hoja.sql

Agrega grupo y nombre de tarifa y el modo/valor para calcular el precio final doble faz por hoja. Los campos nulos conservan el cálculo anterior por carilla. No reescribe tarifas, importes ni huellas de solicitudes. El encabezado se incorpora al crear la migración.

[Abrir SQL](V38__tarifas_agrupadas_por_hoja.sql)

**Tabla modificada:** `lamontana.tarifa_impresion`.

**Restricciones:** grupo/nombre completos, modo/valor completos y válidos, importes no negativos y ausencia del recargo antiguo en tarifas por hoja.

**Funciones declaradas:** ninguna.
