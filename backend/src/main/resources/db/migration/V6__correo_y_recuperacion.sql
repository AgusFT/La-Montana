ALTER TABLE lamontana.usuario ADD COLUMN debe_cambiar_contrasena boolean NOT NULL DEFAULT false;
ALTER TABLE lamontana.usuario ADD COLUMN version_acceso bigint NOT NULL DEFAULT 0;
CREATE TABLE lamontana.credencial_temporal (
    id_credencial_temporal bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario bigint NOT NULL REFERENCES lamontana.usuario,
    proposito varchar(32) NOT NULL CHECK(proposito='RESTABLECIMIENTO'),
    hash_codigo varchar(64) NOT NULL UNIQUE,
    correo_destino varchar(254) NOT NULL,
    intentos integer NOT NULL DEFAULT 0 CHECK(intentos BETWEEN 0 AND 5),
    fecha_emision timestamptz NOT NULL DEFAULT now(),
    fecha_vencimiento timestamptz NOT NULL,
    fecha_consumo timestamptz,
    fecha_revocacion timestamptz,
    CHECK(fecha_vencimiento>fecha_emision)
);
CREATE INDEX credencial_usuario ON lamontana.credencial_temporal(id_usuario,fecha_emision DESC);
CREATE TABLE lamontana.token_verificacion_correo (
    id_token_verificacion_correo bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario bigint NOT NULL REFERENCES lamontana.usuario,
    hash_token varchar(64) NOT NULL UNIQUE,
    correo_destino varchar(254) NOT NULL,
    intentos integer NOT NULL DEFAULT 0 CHECK(intentos BETWEEN 0 AND 5),
    fecha_emision timestamptz NOT NULL DEFAULT now(),
    fecha_vencimiento timestamptz NOT NULL,
    fecha_consumo timestamptz,
    fecha_revocacion timestamptz,
    CHECK(fecha_vencimiento>fecha_emision)
);
CREATE INDEX verificacion_usuario ON lamontana.token_verificacion_correo(id_usuario,fecha_emision DESC);
