CREATE TABLE lamontana.origen_configuracion (
 id_configuracion_version bigint PRIMARY KEY REFERENCES lamontana.configuracion_version,
 id_version_origen bigint NOT NULL REFERENCES lamontana.configuracion_version,
 creada_en timestamptz NOT NULL DEFAULT clock_timestamp(),
 barrido_inicial jsonb NOT NULL DEFAULT '[]'::jsonb CHECK(jsonb_typeof(barrido_inicial)='array'),
 CHECK(id_configuracion_version<>id_version_origen)
);
CREATE TABLE lamontana.reconfirmacion_configuracion (
 id_configuracion_version bigint NOT NULL REFERENCES lamontana.origen_configuracion,
 fase smallint NOT NULL CHECK(fase BETWEEN 2 AND 6),
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 version bigint NOT NULL CHECK(version>0),
 fecha timestamptz NOT NULL DEFAULT clock_timestamp(),
 huella_revision varchar(64),
 PRIMARY KEY(id_configuracion_version,fase),
 CHECK(huella_revision IS NULL OR fase=6)
);
ALTER TABLE lamontana.comprobante_configuracion DROP CONSTRAINT comprobante_configuracion_tipo_check, ADD CONSTRAINT comprobante_configuracion_tipo_check CHECK(tipo IN ('CREAR_BORRADOR','GUARDAR_MODELO','CANCELAR_BORRADOR','GUARDAR_PAGOS','GUARDAR_RECURSOS','ALTA_IMPRESORA','EDITAR_IMPRESORA','ESTADO_IMPRESORA','RETIRAR_IMPRESORA','GUARDAR_ENTREGA','ALTA_PUNTO','EDITAR_PUNTO','ALTA_ZONA','EDITAR_ZONA','ACTIVAR_CONFIGURACION','PROGRAMAR_CONFIGURACION','CANCELAR_PROGRAMACION','ADELANTAR_CONFIGURACION','USAR_COMO_BASE','RECONFIRMAR_REVISION'));
ALTER TABLE lamontana.evento_configuracion DROP CONSTRAINT evento_configuracion_tipo_check, ADD CONSTRAINT evento_configuracion_tipo_check CHECK(tipo IN ('BORRADOR_CREADO','MODELO_SELECCIONADO','CANCELACION_SOLICITADA','BORRADOR_CANCELADO','PAGOS_CONFIGURADOS','RECURSOS_CONFIGURADOS','IMPRESORA_CREADA','IMPRESORA_EDITADA','IMPRESORA_ESTADO_CAMBIADO','IMPRESORA_RETIRADA','ENTREGA_CONFIGURADA','PUNTO_CREADO','PUNTO_EDITADO','ZONA_CREADA','ZONA_EDITADA','ACTIVACION_SOLICITADA','CONFIGURACION_ACTIVADA','CONFIGURACION_HISTORICA','AUTORIZACION_RECHAZADA','PROGRAMACION_SOLICITADA','CONFIGURACION_PROGRAMADA','PROGRAMACION_CANCELADA','INTENTO_MANUAL_INICIADO','BORRADOR_DESDE_BASE','REVISION_RECONFIRMADA'));
