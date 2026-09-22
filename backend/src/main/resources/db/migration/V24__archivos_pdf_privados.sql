CREATE TABLE lamontana.archivo_almacenado (
 id_archivo_almacenado bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_usuario_cargador bigint NOT NULL REFERENCES lamontana.usuario,
 id_operacion uuid NOT NULL UNIQUE,
 nombre_original varchar(255) NOT NULL,
 estado varchar(28) NOT NULL CHECK (estado IN ('PENDIENTE','VALIDANDO','VALIDO','REQUIERE_COTIZACION','RECHAZADO','FALLIDO')),
 fecha_creacion timestamptz NOT NULL DEFAULT now(),
 cargar_hasta timestamptz NOT NULL,
 fecha_inicio timestamptz,
 fecha_fin timestamptz,
 cantidad_bytes bigint CHECK (cantidad_bytes BETWEEN 1 AND 10485760),
 sha256 char(64) CHECK (sha256 ~ '^[a-f0-9]{64}$'),
 cantidad_paginas integer CHECK (cantidad_paginas BETWEEN 1 AND 10000),
 codigo_resultado varchar(80),
 mensaje varchar(500),
 CHECK (cargar_hasta>fecha_creacion),
 CHECK ((estado IN ('PENDIENTE','VALIDANDO'))=(fecha_fin IS NULL)),
 CHECK (estado NOT IN ('VALIDO','REQUIERE_COTIZACION') OR
   (cantidad_bytes IS NOT NULL AND sha256 IS NOT NULL AND cantidad_paginas IS NOT NULL AND fecha_inicio IS NOT NULL))
);
CREATE TABLE lamontana.archivo_trabajo (
 id_archivo_trabajo bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_archivo_almacenado bigint NOT NULL UNIQUE REFERENCES lamontana.archivo_almacenado,
 id_cotizacion_item bigint NOT NULL REFERENCES lamontana.cotizacion_item,
 id_archivo_trabajo_reemplazado bigint REFERENCES lamontana.archivo_trabajo,
 activo boolean NOT NULL DEFAULT false,
 fecha_asociacion timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX archivo_original_activo ON lamontana.archivo_trabajo(id_cotizacion_item) WHERE activo;
CREATE INDEX archivo_por_item ON lamontana.archivo_trabajo(id_cotizacion_item,id_archivo_trabajo DESC);
CREATE TABLE lamontana.validacion_archivo (
 id_validacion_archivo bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_archivo_almacenado bigint NOT NULL REFERENCES lamontana.archivo_almacenado,
 tipo_validacion varchar(24) NOT NULL CHECK(tipo_validacion IN ('SEGURIDAD','ESTRUCTURA_RENDER')),
 resultado varchar(20) NOT NULL CHECK(resultado IN ('ACEPTADO','RECHAZADO','INCOMPLETO')),
 motor varchar(100) NOT NULL,
 version_motor varchar(180) NOT NULL,
 codigo_resultado varchar(80) NOT NULL,
 fecha timestamptz NOT NULL DEFAULT now(),
 UNIQUE(id_archivo_almacenado,tipo_validacion)
);
CREATE TABLE lamontana.aceptacion_vista_previa (
 id_aceptacion_vista_previa bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_archivo_almacenado bigint NOT NULL UNIQUE REFERENCES lamontana.archivo_almacenado,
 id_usuario bigint NOT NULL REFERENCES lamontana.usuario,
 id_operacion uuid NOT NULL UNIQUE,
 sha256 char(64) NOT NULL,
 fecha timestamptz NOT NULL DEFAULT now()
);
CREATE FUNCTION lamontana.proteger_archivo_pdf() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Archivo con historia no se elimina'; END IF;
 IF OLD.estado NOT IN ('PENDIENTE','VALIDANDO') OR
    (to_jsonb(NEW)-ARRAY['estado','fecha_inicio','fecha_fin','cantidad_bytes','sha256','cantidad_paginas','codigo_resultado','mensaje']) IS DISTINCT FROM
    (to_jsonb(OLD)-ARRAY['estado','fecha_inicio','fecha_fin','cantidad_bytes','sha256','cantidad_paginas','codigo_resultado','mensaje']) OR
    (OLD.estado='PENDIENTE' AND NEW.estado NOT IN ('VALIDANDO','FALLIDO')) OR
    (OLD.estado='VALIDANDO' AND NEW.estado IN ('PENDIENTE','VALIDANDO')) THEN
   RAISE EXCEPTION 'Transición de archivo inválida';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER archivo_pdf_inmutable BEFORE UPDATE OR DELETE ON lamontana.archivo_almacenado FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_archivo_pdf();
CREATE FUNCTION lamontana.proteger_relacion_archivo() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR (to_jsonb(NEW)-'activo') IS DISTINCT FROM (to_jsonb(OLD)-'activo') THEN RAISE EXCEPTION 'Historia de archivo inmutable'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER archivo_relacion_inmutable BEFORE UPDATE OR DELETE ON lamontana.archivo_trabajo FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_relacion_archivo();
CREATE TRIGGER archivo_validacion_inmutable BEFORE UPDATE OR DELETE ON lamontana.validacion_archivo FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TRIGGER archivo_aceptacion_inmutable BEFORE UPDATE OR DELETE ON lamontana.aceptacion_vista_previa FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE FUNCTION lamontana.exigir_original_validado() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.activo AND NOT EXISTS (
   SELECT 1 FROM lamontana.archivo_almacenado a JOIN lamontana.cotizacion_item i ON i.id_cotizacion_item=NEW.id_cotizacion_item
   JOIN lamontana.cotizacion c USING(id_cotizacion)
   WHERE a.id_archivo_almacenado=NEW.id_archivo_almacenado AND a.estado='VALIDO'
   AND a.cantidad_paginas=i.cantidad_paginas_declaradas AND a.id_usuario_cargador=c.id_usuario_creador
 ) THEN RAISE EXCEPTION 'Original no validado para el ítem'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER archivo_activo_validado BEFORE INSERT OR UPDATE ON lamontana.archivo_trabajo FOR EACH ROW EXECUTE FUNCTION lamontana.exigir_original_validado();
CREATE FUNCTION lamontana.exigir_preview_validada() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS (SELECT 1 FROM lamontana.archivo_almacenado a JOIN lamontana.archivo_trabajo t USING(id_archivo_almacenado)
   WHERE a.id_archivo_almacenado=NEW.id_archivo_almacenado AND a.estado='VALIDO' AND t.activo
   AND a.sha256=NEW.sha256 AND a.id_usuario_cargador=NEW.id_usuario)
 THEN RAISE EXCEPTION 'Vista previa no aceptable'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER preview_validada BEFORE INSERT ON lamontana.aceptacion_vista_previa FOR EACH ROW EXECUTE FUNCTION lamontana.exigir_preview_validada();
