-- Una guía financiera explícita por borrador; no se precargan medios, señas ni plazos.
CREATE TABLE lamontana.configuracion_financiera (
    id_configuracion_version bigint PRIMARY KEY REFERENCES lamontana.configuracion_version,
    vigencia_cotizacion_minutos integer NOT NULL CHECK(vigencia_cotizacion_minutos>0),
    instrucciones_transferencia varchar(2000),
    exigir_sena boolean NOT NULL,
    condicion_sena varchar(40),
    umbral_sena numeric(19,2),
    tipo_sena varchar(16),
    valor_sena numeric(19,4),
    umbral_aprobacion numeric(19,2) CHECK(umbral_aprobacion>0),
    CHECK(instrucciones_transferencia IS NULL OR length(trim(instrucciones_transferencia))>0),
    CHECK(
        (NOT exigir_sena AND condicion_sena IS NULL AND umbral_sena IS NULL AND tipo_sena IS NULL AND valor_sena IS NULL)
        OR (exigir_sena AND condicion_sena IS NOT NULL AND tipo_sena IS NOT NULL AND valor_sena IS NOT NULL AND valor_sena>0
            AND ((tipo_sena='PORCENTAJE' AND valor_sena<=100) OR (tipo_sena='FIJA' AND valor_sena=round(valor_sena,2)))
            AND ((condicion_sena IN ('SIEMPRE','SUPERAR_UMBRAL_APROBACION') AND umbral_sena IS NULL)
                OR (condicion_sena='DESDE_MONTO' AND umbral_sena IS NOT NULL AND umbral_sena>0)
                OR (condicion_sena='DESDE_CARILLAS' AND umbral_sena IS NOT NULL AND umbral_sena>0
                    AND umbral_sena=trunc(umbral_sena) AND umbral_sena<=2147483647)))
    )
);
CREATE TABLE lamontana.configuracion_medio_pago (
    id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_financiera ON DELETE CASCADE,
    medio_pago varchar(24) NOT NULL CHECK(medio_pago IN ('TRANSFERENCIA','EFECTIVO')),
    PRIMARY KEY(id_configuracion_version,medio_pago)
);

ALTER TABLE lamontana.comprobante_configuracion
    DROP CONSTRAINT comprobante_configuracion_tipo_check,
    ADD CONSTRAINT comprobante_configuracion_tipo_check CHECK(tipo IN ('CREAR_BORRADOR','GUARDAR_MODELO','CANCELAR_BORRADOR','GUARDAR_PAGOS'));
ALTER TABLE lamontana.evento_configuracion
    DROP CONSTRAINT evento_configuracion_tipo_check,
    ADD CONSTRAINT evento_configuracion_tipo_check CHECK(tipo IN ('BORRADOR_CREADO','MODELO_SELECCIONADO','CANCELACION_SOLICITADA','BORRADOR_CANCELADO','PAGOS_CONFIGURADOS'));
