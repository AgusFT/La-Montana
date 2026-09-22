ALTER TABLE lamontana.configuracion_version
    DROP CONSTRAINT configuracion_version_estado_check,
    DROP CONSTRAINT configuracion_seleccion_valida,
    ADD COLUMN id_usuario_cancelador bigint REFERENCES lamontana.usuario,
    ADD COLUMN fecha_cancelacion timestamptz,
    ADD COLUMN motivo_cancelacion varchar(500),
    ADD CONSTRAINT configuracion_estado_valido CHECK(estado IN ('EN_PREPARACION','CANCELADA')),
    ADD CONSTRAINT configuracion_seleccion_valida CHECK(
        (modelo IS NULL AND criterio IS NULL AND (version=1 OR estado='CANCELADA'))
        OR (version>1 AND modelo IS NOT NULL AND (
            (modelo='MANUAL' AND criterio IS NULL)
            OR (modelo='CONDICIONAL' AND criterio IS NOT NULL AND criterio IN ('PAGO_PREVIO','SENA','MONTO_TOTAL'))
        ))
    ),
    ADD CONSTRAINT configuracion_cancelacion_completa CHECK(
        (estado='EN_PREPARACION' AND id_usuario_cancelador IS NULL AND fecha_cancelacion IS NULL AND motivo_cancelacion IS NULL)
        OR (estado='CANCELADA' AND id_usuario_cancelador IS NOT NULL AND fecha_cancelacion IS NOT NULL
            AND motivo_cancelacion IS NOT NULL AND length(trim(motivo_cancelacion))>0 AND fecha_cancelacion>=fecha_creacion)
    );

ALTER TABLE lamontana.comprobante_configuracion
    DROP CONSTRAINT comprobante_configuracion_tipo_check,
    ADD CONSTRAINT comprobante_configuracion_tipo_check CHECK(tipo IN ('CREAR_BORRADOR','GUARDAR_MODELO','CANCELAR_BORRADOR'));
ALTER TABLE lamontana.evento_configuracion
    DROP CONSTRAINT evento_configuracion_tipo_check,
    ADD CONSTRAINT evento_configuracion_tipo_check CHECK(tipo IN ('BORRADOR_CREADO','MODELO_SELECCIONADO','CANCELACION_SOLICITADA','BORRADOR_CANCELADO'));

-- Desafío exclusivo de este propósito: sólo persiste la huella del token aleatorio, nunca la contraseña.
CREATE TABLE lamontana.autorizacion_configuracion (
    id_operacion uuid PRIMARY KEY,
    proposito varchar(24) NOT NULL CHECK(proposito='CANCELAR_BORRADOR'),
    id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
    version_configuracion bigint NOT NULL CHECK(version_configuracion>=1),
    id_actor bigint NOT NULL REFERENCES lamontana.usuario,
    version_acceso bigint NOT NULL,
    correo_destino varchar(254) NOT NULL,
    hash_solicitud varchar(64) NOT NULL,
    hash_codigo varchar(64) NOT NULL UNIQUE,
    intentos integer NOT NULL DEFAULT 0 CHECK(intentos BETWEEN 0 AND 5),
    fecha_emision timestamptz NOT NULL DEFAULT clock_timestamp(),
    fecha_vencimiento timestamptz NOT NULL,
    fecha_consumo timestamptz,
    fecha_revocacion timestamptz,
    CHECK(fecha_vencimiento>fecha_emision),
    CHECK(fecha_consumo IS NULL OR fecha_revocacion IS NULL)
);
CREATE INDEX autorizacion_configuracion_actor ON lamontana.autorizacion_configuracion(id_actor,fecha_emision DESC);
CREATE INDEX autorizacion_configuracion_borrador ON lamontana.autorizacion_configuracion(id_configuracion_version) WHERE fecha_consumo IS NULL AND fecha_revocacion IS NULL;
