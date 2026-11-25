-- El calendario y las estimaciones son compromisos estructurales del borrador.
-- Los nulos expresan decisiones pendientes; no se habilitan días ni modalidades por omisión.
CREATE TABLE lamontana.configuracion_entrega (
    id_configuracion_version bigint PRIMARY KEY REFERENCES lamontana.configuracion_version,
    preparacion_horas numeric(8,2) CHECK(preparacion_horas>0 AND preparacion_horas<=10000),
    traslado_horas numeric(8,2) CHECK(traslado_horas>=0 AND traslado_horas<=10000)
);
CREATE TABLE lamontana.configuracion_modalidad_entrega (
    id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
    modalidad varchar(28) NOT NULL CHECK(modalidad IN ('RETIRO_SUCURSAL','RETIRO_PUNTO_ENTREGA','ENVIO_DOMICILIO')),
    PRIMARY KEY(id_configuracion_version,modalidad)
);
CREATE TABLE lamontana.configuracion_horario_sucursal (
    id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
    id_sucursal bigint NOT NULL REFERENCES lamontana.sucursal,
    zona_horaria varchar(64) NOT NULL,
    PRIMARY KEY(id_configuracion_version,id_sucursal)
);
CREATE TABLE lamontana.horario_sucursal (
    id_configuracion_version bigint NOT NULL,
    id_sucursal bigint NOT NULL,
    dia_semana smallint NOT NULL CHECK(dia_semana BETWEEN 1 AND 7),
    habilitado boolean,
    hora_desde time,
    hora_hasta time,
    PRIMARY KEY(id_configuracion_version,id_sucursal,dia_semana),
    FOREIGN KEY(id_configuracion_version,id_sucursal) REFERENCES lamontana.configuracion_horario_sucursal ON DELETE CASCADE,
    CHECK(habilitado IS TRUE OR (hora_desde IS NULL AND hora_hasta IS NULL)),
    CHECK(hora_desde IS NULL OR hora_hasta IS NULL OR hora_desde<hora_hasta)
);
ALTER TABLE lamontana.comprobante_configuracion
    DROP CONSTRAINT comprobante_configuracion_tipo_check,
    ADD CONSTRAINT comprobante_configuracion_tipo_check CHECK(tipo IN ('CREAR_BORRADOR','GUARDAR_MODELO','CANCELAR_BORRADOR','GUARDAR_PAGOS',
        'GUARDAR_RECURSOS','ALTA_IMPRESORA','EDITAR_IMPRESORA','ESTADO_IMPRESORA','RETIRAR_IMPRESORA','GUARDAR_ENTREGA'));
ALTER TABLE lamontana.evento_configuracion
    DROP CONSTRAINT evento_configuracion_tipo_check,
    ADD CONSTRAINT evento_configuracion_tipo_check CHECK(tipo IN ('BORRADOR_CREADO','MODELO_SELECCIONADO','CANCELACION_SOLICITADA','BORRADOR_CANCELADO','PAGOS_CONFIGURADOS',
        'RECURSOS_CONFIGURADOS','IMPRESORA_CREADA','IMPRESORA_EDITADA','IMPRESORA_ESTADO_CAMBIADO','IMPRESORA_RETIRADA','ENTREGA_CONFIGURADA'));
