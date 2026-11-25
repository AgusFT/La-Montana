-- Identidad permanente separada de la dirección y las condiciones de cada versión.
-- La disponibilidad temporal se incorporará con sus acciones operativas; no se simula un estado ACTIVO.
CREATE TABLE lamontana.punto_entrega (
    id_punto_entrega bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_publico uuid NOT NULL UNIQUE,
    codigo varchar(40) NOT NULL UNIQUE CHECK(codigo=upper(codigo) AND codigo ~ '^[A-Z0-9_-]{1,40}$'),
    id_usuario_creador bigint NOT NULL REFERENCES lamontana.usuario,
    fecha_creacion timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE lamontana.configuracion_definicion_punto (
    id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
    id_punto_entrega bigint NOT NULL REFERENCES lamontana.punto_entrega,
    nombre varchar(160) NOT NULL CHECK(length(trim(nombre))>0),
    calle varchar(160) NOT NULL CHECK(length(trim(calle))>0),
    numero varchar(20) NOT NULL CHECK(length(trim(numero))>0),
    localidad varchar(120) NOT NULL CHECK(length(trim(localidad))>0),
    provincia varchar(120) NOT NULL CHECK(length(trim(provincia))>0),
    codigo_postal varchar(12) NOT NULL CHECK(length(trim(codigo_postal))>0),
    referencias varchar(2000),
    zona_horaria varchar(64) NOT NULL,
    PRIMARY KEY(id_configuracion_version,id_punto_entrega)
);
CREATE TABLE lamontana.configuracion_punto_entrega (
    id_configuracion_punto_entrega bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_configuracion_version bigint NOT NULL,
    id_punto_entrega bigint NOT NULL,
    id_sucursal bigint NOT NULL REFERENCES lamontana.sucursal,
    costo numeric(19,2) NOT NULL CHECK(costo>=0),
    habilitado boolean NOT NULL,
    UNIQUE(id_configuracion_version,id_punto_entrega,id_sucursal),
    FOREIGN KEY(id_configuracion_version,id_punto_entrega) REFERENCES lamontana.configuracion_definicion_punto
);
CREATE TABLE lamontana.franja_entrega (
    id_franja_entrega bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_configuracion_punto_entrega bigint NOT NULL REFERENCES lamontana.configuracion_punto_entrega ON DELETE CASCADE,
    dia_semana smallint NOT NULL CHECK(dia_semana BETWEEN 1 AND 7),
    hora_desde time NOT NULL,
    hora_hasta time NOT NULL,
    capacidad_pedidos integer NOT NULL CHECK(capacidad_pedidos>=0),
    habilitada boolean NOT NULL,
    CHECK(hora_desde<hora_hasta),
    UNIQUE(id_configuracion_punto_entrega,dia_semana,hora_desde,hora_hasta)
);
ALTER TABLE lamontana.comprobante_configuracion
    DROP CONSTRAINT comprobante_configuracion_tipo_check,
    ADD CONSTRAINT comprobante_configuracion_tipo_check CHECK(tipo IN ('CREAR_BORRADOR','GUARDAR_MODELO','CANCELAR_BORRADOR','GUARDAR_PAGOS',
        'GUARDAR_RECURSOS','ALTA_IMPRESORA','EDITAR_IMPRESORA','ESTADO_IMPRESORA','RETIRAR_IMPRESORA','GUARDAR_ENTREGA','ALTA_PUNTO','EDITAR_PUNTO'));
ALTER TABLE lamontana.evento_configuracion
    DROP CONSTRAINT evento_configuracion_tipo_check,
    ADD CONSTRAINT evento_configuracion_tipo_check CHECK(tipo IN ('BORRADOR_CREADO','MODELO_SELECCIONADO','CANCELACION_SOLICITADA','BORRADOR_CANCELADO','PAGOS_CONFIGURADOS',
        'RECURSOS_CONFIGURADOS','IMPRESORA_CREADA','IMPRESORA_EDITADA','IMPRESORA_ESTADO_CAMBIADO','IMPRESORA_RETIRADA','ENTREGA_CONFIGURADA','PUNTO_CREADO','PUNTO_EDITADO'));
