-- Identidad local estable: sin agente, cola CUPS, telemetría ni existencias inventadas.
CREATE TABLE lamontana.impresora (
    id_impresora bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_publico uuid NOT NULL UNIQUE,
    id_sucursal bigint NOT NULL REFERENCES lamontana.sucursal,
    id_usuario_creador bigint NOT NULL REFERENCES lamontana.usuario,
    fecha_alta timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE lamontana.configuracion_recursos (
    id_configuracion_version bigint PRIMARY KEY REFERENCES lamontana.configuracion_version,
    metodo_asignacion varchar(20) NOT NULL CHECK(metodo_asignacion='MANUAL')
);
CREATE TABLE lamontana.configuracion_impresora (
    id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
    id_impresora bigint NOT NULL REFERENCES lamontana.impresora,
    nombre varchar(120) NOT NULL CHECK(length(trim(nombre))>0),
    admite_color boolean NOT NULL,
    admite_doble_faz boolean NOT NULL,
    capacidad_maxima_hojas integer NOT NULL CHECK(capacidad_maxima_hojas>0),
    estado varchar(24) NOT NULL CHECK(estado IN ('OPERATIVA','DESHABILITADA','RETIRADA')),
    retirada_en timestamptz,
    motivo_retiro varchar(500),
    PRIMARY KEY(id_configuracion_version,id_impresora),
    CHECK((estado='RETIRADA' AND retirada_en IS NOT NULL AND motivo_retiro IS NOT NULL AND length(trim(motivo_retiro))>0)
        OR (estado<>'RETIRADA' AND retirada_en IS NULL AND motivo_retiro IS NULL))
);
CREATE TABLE lamontana.configuracion_impresora_formato (
    id_configuracion_version bigint NOT NULL,
    id_impresora bigint NOT NULL,
    id_formato bigint NOT NULL REFERENCES lamontana.formato,
    PRIMARY KEY(id_configuracion_version,id_impresora,id_formato),
    FOREIGN KEY(id_configuracion_version,id_impresora) REFERENCES lamontana.configuracion_impresora
);
-- La presencia expresa habilitación; la lista vacía deshabilita todos los servicios.
CREATE TABLE lamontana.sucursal_servicio (
    id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
    id_sucursal bigint NOT NULL REFERENCES lamontana.sucursal,
    id_servicio bigint NOT NULL REFERENCES lamontana.servicio,
    PRIMARY KEY(id_configuracion_version,id_sucursal,id_servicio)
);

ALTER TABLE lamontana.comprobante_configuracion
    DROP CONSTRAINT comprobante_configuracion_tipo_check,
    ADD CONSTRAINT comprobante_configuracion_tipo_check CHECK(tipo IN ('CREAR_BORRADOR','GUARDAR_MODELO','CANCELAR_BORRADOR','GUARDAR_PAGOS',
        'GUARDAR_RECURSOS','ALTA_IMPRESORA','EDITAR_IMPRESORA','ESTADO_IMPRESORA','RETIRAR_IMPRESORA'));
ALTER TABLE lamontana.evento_configuracion
    ADD COLUMN codigo_recurso uuid,
    ADD COLUMN motivo varchar(500),
    ADD COLUMN estado_anterior varchar(24) CHECK(estado_anterior IN ('OPERATIVA','DESHABILITADA','RETIRADA')),
    ADD COLUMN estado_nuevo varchar(24) CHECK(estado_nuevo IN ('OPERATIVA','DESHABILITADA','RETIRADA')),
    DROP CONSTRAINT evento_configuracion_tipo_check,
    ADD CONSTRAINT evento_configuracion_tipo_check CHECK(tipo IN ('BORRADOR_CREADO','MODELO_SELECCIONADO','CANCELACION_SOLICITADA','BORRADOR_CANCELADO','PAGOS_CONFIGURADOS',
        'RECURSOS_CONFIGURADOS','IMPRESORA_CREADA','IMPRESORA_EDITADA','IMPRESORA_ESTADO_CAMBIADO','IMPRESORA_RETIRADA'));
