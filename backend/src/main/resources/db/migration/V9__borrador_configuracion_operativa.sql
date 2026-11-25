CREATE TABLE lamontana.configuracion_version (
    id_configuracion_version bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_publico uuid NOT NULL UNIQUE,
    numero_version bigint NOT NULL UNIQUE CHECK(numero_version>=1),
    id_usuario_creador bigint NOT NULL REFERENCES lamontana.usuario,
    estado varchar(24) NOT NULL CHECK(estado='EN_PREPARACION'),
    modelo varchar(16),
    criterio varchar(24),
    version bigint NOT NULL DEFAULT 1 CHECK(version>=1),
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
    CHECK(fecha_actualizacion>=fecha_creacion),
    CONSTRAINT configuracion_seleccion_valida CHECK(
        (version=1 AND modelo IS NULL AND criterio IS NULL)
        OR (version>1 AND modelo IS NOT NULL AND (
            (modelo='MANUAL' AND criterio IS NULL)
            OR (modelo='CONDICIONAL' AND criterio IS NOT NULL AND criterio IN ('PAGO_PREVIO','SENA','MONTO_TOTAL'))
        ))
    )
);
CREATE UNIQUE INDEX configuracion_un_borrador ON lamontana.configuracion_version(estado) WHERE estado='EN_PREPARACION';

CREATE TABLE lamontana.comprobante_configuracion (
    id_operacion uuid PRIMARY KEY,
    tipo varchar(24) NOT NULL CHECK(tipo IN ('CREAR_BORRADOR','GUARDAR_MODELO')),
    id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
    id_actor bigint NOT NULL REFERENCES lamontana.usuario,
    hash_solicitud varchar(64) NOT NULL,
    fecha timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE lamontana.evento_configuracion (
    id_evento_configuracion bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
    id_actor bigint NOT NULL REFERENCES lamontana.usuario,
    tipo varchar(32) NOT NULL CHECK(tipo IN ('BORRADOR_CREADO','MODELO_SELECCIONADO')),
    version bigint NOT NULL CHECK(version>=1),
    fecha timestamptz NOT NULL DEFAULT now()
);
