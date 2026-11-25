ALTER TABLE lamontana.configuracion_version
 DROP CONSTRAINT configuracion_estado_valido,
 DROP CONSTRAINT configuracion_cancelacion_completa,
 ADD CONSTRAINT configuracion_estado_valido CHECK(estado IN ('EN_PREPARACION','CANCELADA','ACTIVA','HISTORICA')),
 ADD CONSTRAINT configuracion_cancelacion_completa CHECK(
  (estado<>'CANCELADA' AND id_usuario_cancelador IS NULL AND fecha_cancelacion IS NULL AND motivo_cancelacion IS NULL)
  OR (estado='CANCELADA' AND id_usuario_cancelador IS NOT NULL AND fecha_cancelacion IS NOT NULL AND motivo_cancelacion IS NOT NULL AND length(trim(motivo_cancelacion))>0 AND fecha_cancelacion>=fecha_creacion)
 );
CREATE UNIQUE INDEX configuracion_una_activa ON lamontana.configuracion_version(estado) WHERE estado='ACTIVA';
ALTER TABLE lamontana.autorizacion_configuracion DROP CONSTRAINT autorizacion_configuracion_proposito_check,
 ADD CONSTRAINT autorizacion_configuracion_proposito_check CHECK(proposito IN ('CANCELAR_BORRADOR','ACTIVAR_CONFIGURACION'));
CREATE TABLE lamontana.activacion_configuracion (
 id_configuracion_version bigint PRIMARY KEY REFERENCES lamontana.configuracion_version,
 id_predecesora bigint REFERENCES lamontana.configuracion_version,
 id_catalogo_revision bigint NOT NULL REFERENCES lamontana.catalogo_revision,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 id_operacion uuid NOT NULL UNIQUE REFERENCES lamontana.autorizacion_configuracion(id_operacion),
 motivo varchar(500) NOT NULL CHECK(length(trim(motivo))>0),
 fecha_activacion timestamptz NOT NULL,
 fin_vigencia timestamptz,
 CHECK(id_predecesora IS NULL OR id_predecesora<>id_configuracion_version),
 CHECK(fin_vigencia IS NULL OR fin_vigencia>=fecha_activacion)
);
ALTER TABLE lamontana.comprobante_configuracion DROP CONSTRAINT comprobante_configuracion_tipo_check,
 ADD CONSTRAINT comprobante_configuracion_tipo_check CHECK(tipo IN ('CREAR_BORRADOR','GUARDAR_MODELO','CANCELAR_BORRADOR','GUARDAR_PAGOS','GUARDAR_RECURSOS','ALTA_IMPRESORA','EDITAR_IMPRESORA','ESTADO_IMPRESORA','RETIRAR_IMPRESORA','GUARDAR_ENTREGA','ALTA_PUNTO','EDITAR_PUNTO','ALTA_ZONA','EDITAR_ZONA','ACTIVAR_CONFIGURACION'));
ALTER TABLE lamontana.evento_configuracion DROP CONSTRAINT evento_configuracion_tipo_check,
 ADD CONSTRAINT evento_configuracion_tipo_check CHECK(tipo IN ('BORRADOR_CREADO','MODELO_SELECCIONADO','CANCELACION_SOLICITADA','BORRADOR_CANCELADO','PAGOS_CONFIGURADOS','RECURSOS_CONFIGURADOS','IMPRESORA_CREADA','IMPRESORA_EDITADA','IMPRESORA_ESTADO_CAMBIADO','IMPRESORA_RETIRADA','ENTREGA_CONFIGURADA','PUNTO_CREADO','PUNTO_EDITADO','ZONA_CREADA','ZONA_EDITADA','ACTIVACION_SOLICITADA','CONFIGURACION_ACTIVADA','CONFIGURACION_HISTORICA','AUTORIZACION_RECHAZADA'));
