-- Identidad global; cobertura y condiciones congeladas por versión.
CREATE TABLE lamontana.zona_entrega (
 id_zona_entrega bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 codigo varchar(40) NOT NULL UNIQUE CHECK(codigo ~ '^[A-Z0-9_-]{1,40}$'),
 id_usuario_creador bigint NOT NULL REFERENCES lamontana.usuario,
 fecha_creacion timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE lamontana.configuracion_zona_entrega (
 id_configuracion_zona_entrega bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
 id_zona_entrega bigint NOT NULL REFERENCES lamontana.zona_entrega,
 nombre varchar(140) NOT NULL CHECK(length(trim(nombre))>0),
 descripcion varchar(2000),zona_horaria varchar(64) NOT NULL,
 costo numeric(19,2) NOT NULL CHECK(costo>=0),habilitada boolean NOT NULL,
 UNIQUE(id_configuracion_version,id_zona_entrega),
 UNIQUE(id_configuracion_zona_entrega,id_configuracion_version,habilitada)
);
CREATE TABLE lamontana.zona_entrega_codigo_postal (
 id_configuracion_zona_entrega bigint NOT NULL,
 id_configuracion_version bigint NOT NULL,
 -- Copia controlada por FK para imponer exclusividad territorial en PostgreSQL.
 habilitada boolean NOT NULL,
 codigo_postal varchar(12) NOT NULL CHECK(codigo_postal ~ '^[A-Z0-9]{1,12}$'),
 localidad varchar(120) NOT NULL CHECK(length(trim(localidad))>0 AND localidad=upper(localidad)),
 provincia varchar(120) NOT NULL CHECK(length(trim(provincia))>0 AND provincia=upper(provincia)),
 PRIMARY KEY(id_configuracion_zona_entrega,codigo_postal,localidad,provincia),
 FOREIGN KEY(id_configuracion_zona_entrega,id_configuracion_version,habilitada)
 REFERENCES lamontana.configuracion_zona_entrega(id_configuracion_zona_entrega,id_configuracion_version,habilitada) ON UPDATE CASCADE ON DELETE CASCADE
);
CREATE UNIQUE INDEX cobertura_habilitada_unica ON lamontana.zona_entrega_codigo_postal(id_configuracion_version,codigo_postal,localidad,provincia) WHERE habilitada;
ALTER TABLE lamontana.franja_entrega
 ALTER COLUMN id_configuracion_punto_entrega DROP NOT NULL,
 ADD COLUMN id_configuracion_zona_entrega bigint REFERENCES lamontana.configuracion_zona_entrega ON DELETE CASCADE,
 ADD CONSTRAINT franja_destino_exclusivo CHECK(num_nonnulls(id_configuracion_punto_entrega,id_configuracion_zona_entrega)=1),
 ADD CONSTRAINT franja_zona_unica UNIQUE(id_configuracion_zona_entrega,dia_semana,hora_desde,hora_hasta);
ALTER TABLE lamontana.comprobante_configuracion DROP CONSTRAINT comprobante_configuracion_tipo_check,
 ADD CONSTRAINT comprobante_configuracion_tipo_check CHECK(tipo IN ('CREAR_BORRADOR','GUARDAR_MODELO','CANCELAR_BORRADOR','GUARDAR_PAGOS','GUARDAR_RECURSOS','ALTA_IMPRESORA','EDITAR_IMPRESORA','ESTADO_IMPRESORA','RETIRAR_IMPRESORA','GUARDAR_ENTREGA','ALTA_PUNTO','EDITAR_PUNTO','ALTA_ZONA','EDITAR_ZONA'));
ALTER TABLE lamontana.evento_configuracion DROP CONSTRAINT evento_configuracion_tipo_check,
 ADD CONSTRAINT evento_configuracion_tipo_check CHECK(tipo IN ('BORRADOR_CREADO','MODELO_SELECCIONADO','CANCELACION_SOLICITADA','BORRADOR_CANCELADO','PAGOS_CONFIGURADOS','RECURSOS_CONFIGURADOS','IMPRESORA_CREADA','IMPRESORA_EDITADA','IMPRESORA_ESTADO_CAMBIADO','IMPRESORA_RETIRADA','ENTREGA_CONFIGURADA','PUNTO_CREADO','PUNTO_EDITADO','ZONA_CREADA','ZONA_EDITADA'));
