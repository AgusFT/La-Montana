CREATE TABLE lamontana.cotizacion (
 id_cotizacion bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_usuario_creador bigint NOT NULL REFERENCES lamontana.usuario,
 id_sucursal bigint NOT NULL REFERENCES lamontana.sucursal,
 id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
 id_catalogo_revision bigint NOT NULL REFERENCES lamontana.catalogo_revision,
 id_configuracion_confirmacion bigint REFERENCES lamontana.configuracion_version,
 id_cotizacion_reemplazada bigint UNIQUE REFERENCES lamontana.cotizacion,
 estado varchar(16) NOT NULL CHECK(estado IN ('VIGENTE','CANCELADA','EXPIRADA','CONFIRMADA')),
 version bigint NOT NULL DEFAULT 1 CHECK(version>0),
 generada_en timestamptz NOT NULL,
 vigente_hasta timestamptz NOT NULL CHECK(vigente_hasta>generada_en),
 aceptada_en timestamptz,
 cancelada_en timestamptz,
 confirmada_en timestamptz,
 motivo_cancelacion varchar(300),
 subtotal numeric(19,2) NOT NULL CHECK(subtotal>=0),
 costo_entrega numeric(19,2) NOT NULL CHECK(costo_entrega>=0),
 total numeric(19,2) NOT NULL CHECK(total=subtotal+costo_entrega),
 oferta jsonb NOT NULL CHECK(jsonb_typeof(oferta)='object'),
 CHECK((estado='CANCELADA')=(cancelada_en IS NOT NULL)),
 CHECK((estado='CANCELADA')=(motivo_cancelacion IS NOT NULL)),
 CHECK((estado='CONFIRMADA')=(confirmada_en IS NOT NULL)),
 CHECK((estado='CONFIRMADA')=(id_configuracion_confirmacion IS NOT NULL)),
 CHECK(aceptada_en IS NULL OR (aceptada_en>=generada_en AND aceptada_en<vigente_hasta))
);
CREATE INDEX cotizacion_cliente_reciente ON lamontana.cotizacion(id_usuario_creador,id_cotizacion DESC);
CREATE TABLE lamontana.cotizacion_item (
 id_cotizacion_item bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_cotizacion bigint NOT NULL REFERENCES lamontana.cotizacion,
 orden integer NOT NULL CHECK(orden BETWEEN 1 AND 20),
 id_formato bigint NOT NULL REFERENCES lamontana.formato,
 id_papel bigint NOT NULL REFERENCES lamontana.papel,
 cantidad_paginas_declaradas integer NOT NULL CHECK(cantidad_paginas_declaradas BETWEEN 1 AND 10000),
 cantidad_copias integer NOT NULL CHECK(cantidad_copias BETWEEN 1 AND 10000),
 cantidad_carillas integer NOT NULL CHECK(cantidad_carillas=cantidad_paginas_declaradas*cantidad_copias),
 cantidad_hojas integer NOT NULL CHECK(cantidad_hojas>0),
 modo_color varchar(16) NOT NULL CHECK(modo_color IN ('BLANCO_NEGRO','COLOR')),
 doble_faz boolean NOT NULL,
 nombre_archivo_declarado varchar(255) NOT NULL,
 bytes_declarados bigint NOT NULL CHECK(bytes_declarados BETWEEN 1 AND 10485760),
 sha256_declarado char(64) NOT NULL CHECK(sha256_declarado ~ '^[a-f0-9]{64}$'),
 subtotal numeric(19,2) NOT NULL CHECK(subtotal>=0),
 UNIQUE(id_cotizacion,orden),UNIQUE(id_cotizacion,sha256_declarado),
 CHECK(cantidad_hojas=(CASE WHEN doble_faz THEN (cantidad_paginas_declaradas+1)/2 ELSE cantidad_paginas_declaradas END)*cantidad_copias)
);
CREATE TABLE lamontana.cotizacion_item_servicio (
 id_cotizacion_item bigint NOT NULL REFERENCES lamontana.cotizacion_item,
 id_servicio bigint NOT NULL REFERENCES lamontana.servicio,
 tipo_servicio varchar(20) NOT NULL CHECK(tipo_servicio IN ('IMPRESION','TERMINACION')),
 nombre_visible varchar(140) NOT NULL,
 base_precio varchar(24) NOT NULL CHECK(base_precio IN ('POR_COPIA','POR_HOJA','POR_CARILLA','FIJO_POR_ITEM')),
 cantidad bigint NOT NULL CHECK(cantidad>0),
 precio_unitario numeric(19,2) NOT NULL CHECK(precio_unitario>=0),
 subtotal numeric(19,2) NOT NULL CHECK(subtotal=precio_unitario*cantidad),
 PRIMARY KEY(id_cotizacion_item,id_servicio)
);
CREATE UNIQUE INDEX cotizacion_item_impresion_unica ON lamontana.cotizacion_item_servicio(id_cotizacion_item) WHERE tipo_servicio='IMPRESION';
CREATE TABLE lamontana.evento_cotizacion (
 id_evento bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_cotizacion bigint NOT NULL REFERENCES lamontana.cotizacion,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 id_operacion uuid NOT NULL UNIQUE,
 tipo varchar(24) NOT NULL CHECK(tipo IN ('COTIZAR','ACEPTAR_OFERTA','CANCELAR_OFERTA')),
 huella char(64) NOT NULL,
 fecha timestamptz NOT NULL DEFAULT clock_timestamp(),
 motivo varchar(300)
);
CREATE FUNCTION lamontana.proteger_cotizacion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR (to_jsonb(NEW)-ARRAY['estado','version','aceptada_en','cancelada_en','motivo_cancelacion','confirmada_en','id_configuracion_confirmacion']) IS DISTINCT FROM
 (to_jsonb(OLD)-ARRAY['estado','version','aceptada_en','cancelada_en','motivo_cancelacion','confirmada_en','id_configuracion_confirmacion']) THEN
  RAISE EXCEPTION 'El contenido de una oferta emitida es inmutable' USING ERRCODE='23514';
 END IF;
 IF NEW.version<>OLD.version+1 OR OLD.estado<>'VIGENTE' OR (OLD.aceptada_en IS NOT NULL AND NEW.aceptada_en IS DISTINCT FROM OLD.aceptada_en) THEN
  RAISE EXCEPTION 'Transición de cotización inválida' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER cotizacion_inmutable BEFORE UPDATE OR DELETE ON lamontana.cotizacion FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_cotizacion();
CREATE FUNCTION lamontana.proteger_detalle_cotizacion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'El detalle cotizado es inmutable' USING ERRCODE='23514'; END $$;
CREATE TRIGGER cotizacion_item_inmutable BEFORE UPDATE OR DELETE ON lamontana.cotizacion_item FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TRIGGER cotizacion_servicio_inmutable BEFORE UPDATE OR DELETE ON lamontana.cotizacion_item_servicio FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TRIGGER evento_cotizacion_inmutable BEFORE UPDATE OR DELETE ON lamontana.evento_cotizacion FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
