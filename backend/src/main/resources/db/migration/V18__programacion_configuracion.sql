ALTER TABLE lamontana.configuracion_version DROP CONSTRAINT configuracion_estado_valido,
 ADD CONSTRAINT configuracion_estado_valido CHECK(estado IN ('EN_PREPARACION','PROGRAMADA','CANCELADA','ACTIVA','HISTORICA'));
DROP INDEX lamontana.configuracion_un_borrador;
CREATE UNIQUE INDEX configuracion_un_pendiente ON lamontana.configuracion_version((true)) WHERE estado IN ('EN_PREPARACION','PROGRAMADA');
CREATE TABLE lamontana.programacion_configuracion (
 id_configuracion_version bigint PRIMARY KEY REFERENCES lamontana.configuracion_version,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 id_operacion uuid NOT NULL UNIQUE REFERENCES lamontana.autorizacion_configuracion(id_operacion),
 fecha_confirmacion timestamptz NOT NULL,
 fecha_programada timestamptz NOT NULL,
 zona_horaria varchar(64) NOT NULL,
 motivo varchar(500) NOT NULL CHECK(length(trim(motivo))>0),
 CHECK(fecha_programada>fecha_confirmacion)
);
CREATE TABLE lamontana.intento_activacion_configuracion (
 id_intento uuid PRIMARY KEY,
 id_version_objetivo bigint NOT NULL REFERENCES lamontana.configuracion_version,
 id_version_anterior bigint REFERENCES lamontana.configuracion_version,
 id_version_resultante bigint UNIQUE REFERENCES lamontana.configuracion_version,
 origen varchar(20) NOT NULL CHECK(origen IN ('PROGRAMACION','REINTENTO','RECUPERACION')),
 estado varchar(16) NOT NULL CHECK(estado IN ('INICIADO','EXITOSO','FALLIDO','INTERRUMPIDO')),
 fecha_inicio timestamptz NOT NULL,
 fecha_fin timestamptz,
 fecha_atraso_detectado timestamptz,
 codigo_resultado varchar(80),
 detalle_sanitizado varchar(1000),
 CHECK((estado='INICIADO' AND fecha_fin IS NULL AND id_version_resultante IS NULL AND codigo_resultado IS NULL)
    OR (estado<>'INICIADO' AND fecha_fin>=fecha_inicio AND fecha_fin IS NOT NULL AND codigo_resultado IS NOT NULL)),
 CHECK((estado='EXITOSO' AND id_version_resultante=id_version_objetivo AND id_version_resultante IS NOT NULL)
    OR (estado<>'EXITOSO' AND id_version_resultante IS NULL)),
 CHECK((origen='RECUPERACION' AND fecha_atraso_detectado IS NOT NULL AND fecha_atraso_detectado<=fecha_inicio)
    OR (origen<>'RECUPERACION' AND fecha_atraso_detectado IS NULL)),
 CHECK(id_version_anterior IS NULL OR id_version_anterior<>id_version_objetivo)
);
CREATE UNIQUE INDEX configuracion_un_intento ON lamontana.intento_activacion_configuracion((true)) WHERE estado='INICIADO';
CREATE INDEX configuracion_intentos_version ON lamontana.intento_activacion_configuracion(id_version_objetivo,fecha_inicio DESC);
ALTER TABLE lamontana.activacion_configuracion ALTER COLUMN id_actor DROP NOT NULL;
ALTER TABLE lamontana.evento_configuracion ALTER COLUMN id_actor DROP NOT NULL;
ALTER TABLE lamontana.autorizacion_configuracion DROP CONSTRAINT autorizacion_configuracion_proposito_check, ADD CONSTRAINT autorizacion_configuracion_proposito_check CHECK(proposito IN ('CANCELAR_BORRADOR','ACTIVAR_CONFIGURACION','PROGRAMAR_CONFIGURACION','CANCELAR_PROGRAMACION'));
ALTER TABLE lamontana.comprobante_configuracion DROP CONSTRAINT comprobante_configuracion_tipo_check, ADD CONSTRAINT comprobante_configuracion_tipo_check CHECK(tipo IN ('CREAR_BORRADOR','GUARDAR_MODELO','CANCELAR_BORRADOR','GUARDAR_PAGOS','GUARDAR_RECURSOS','ALTA_IMPRESORA','EDITAR_IMPRESORA','ESTADO_IMPRESORA','RETIRAR_IMPRESORA','GUARDAR_ENTREGA','ALTA_PUNTO','EDITAR_PUNTO','ALTA_ZONA','EDITAR_ZONA','ACTIVAR_CONFIGURACION','PROGRAMAR_CONFIGURACION','CANCELAR_PROGRAMACION'));
ALTER TABLE lamontana.evento_configuracion DROP CONSTRAINT evento_configuracion_tipo_check, ADD CONSTRAINT evento_configuracion_tipo_check CHECK(tipo IN ('BORRADOR_CREADO','MODELO_SELECCIONADO','CANCELACION_SOLICITADA','BORRADOR_CANCELADO','PAGOS_CONFIGURADOS','RECURSOS_CONFIGURADOS','IMPRESORA_CREADA','IMPRESORA_EDITADA','IMPRESORA_ESTADO_CAMBIADO','IMPRESORA_RETIRADA','ENTREGA_CONFIGURADA','PUNTO_CREADO','PUNTO_EDITADO','ZONA_CREADA','ZONA_EDITADA','ACTIVACION_SOLICITADA','CONFIGURACION_ACTIVADA','CONFIGURACION_HISTORICA','AUTORIZACION_RECHAZADA','PROGRAMACION_SOLICITADA','CONFIGURACION_PROGRAMADA','PROGRAMACION_CANCELADA'));
