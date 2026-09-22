-- Dinero previo al pedido: siempre ligado a una cotización del particular.
CREATE TABLE lamontana.evento_financiero (
 id_evento bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_operacion uuid NOT NULL UNIQUE,
 id_cotizacion bigint NOT NULL REFERENCES lamontana.cotizacion,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 actor_nombre varchar(300) NOT NULL, actor_rol varchar(40) NOT NULL,
 tipo varchar(24) NOT NULL CHECK(tipo IN ('INFORMAR','DESCARTAR','RECIBIR','APLICAR','DEVOLVER')),
 huella char(64) NOT NULL, motivo varchar(300) NOT NULL,
 fecha timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE lamontana.intento_pago (
 id_intento_pago bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_cotizacion bigint NOT NULL REFERENCES lamontana.cotizacion,
 id_evento bigint NOT NULL UNIQUE REFERENCES lamontana.evento_financiero,
 importe_informado numeric(19,2) NOT NULL CHECK(importe_informado>0),
 referencia_informada varchar(160) NOT NULL
);
CREATE TABLE lamontana.resolucion_intento_pago (
 id_intento_pago bigint PRIMARY KEY REFERENCES lamontana.intento_pago,
 id_evento bigint NOT NULL UNIQUE REFERENCES lamontana.evento_financiero,
 resultado varchar(16) NOT NULL CHECK(resultado IN ('ACREDITADO','DESCARTADO'))
);
CREATE TABLE lamontana.pago (
 id_pago bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_cotizacion bigint NOT NULL REFERENCES lamontana.cotizacion,
 id_intento_pago bigint UNIQUE REFERENCES lamontana.intento_pago,
 id_evento bigint NOT NULL UNIQUE REFERENCES lamontana.evento_financiero,
 medio varchar(16) NOT NULL CHECK(medio IN ('EFECTIVO','TRANSFERENCIA')),
 importe numeric(19,2) NOT NULL CHECK(importe>0),
 moneda char(3) NOT NULL DEFAULT 'ARS' CHECK(moneda='ARS'),
 referencia varchar(160) NOT NULL,
 clave_referencia varchar(230) NOT NULL UNIQUE,
 recibido_en timestamptz NOT NULL,
 CHECK(id_intento_pago IS NULL OR medio='TRANSFERENCIA')
);
CREATE TABLE lamontana.aplicacion_pago_cotizacion (
 id_aplicacion bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_pago bigint NOT NULL REFERENCES lamontana.pago,
 id_cotizacion bigint NOT NULL REFERENCES lamontana.cotizacion,
 id_evento bigint NOT NULL UNIQUE REFERENCES lamontana.evento_financiero,
 importe numeric(19,2) NOT NULL CHECK(importe>0)
);
CREATE TABLE lamontana.liberacion_aplicacion_pago (
 id_liberacion bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_aplicacion bigint NOT NULL REFERENCES lamontana.aplicacion_pago_cotizacion,
 id_evento bigint NOT NULL REFERENCES lamontana.evento_financiero,
 importe numeric(19,2) NOT NULL CHECK(importe>0), UNIQUE(id_aplicacion,id_evento)
);
CREATE TABLE lamontana.reembolso (
 id_reembolso bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_pago bigint NOT NULL REFERENCES lamontana.pago,
 id_evento bigint NOT NULL UNIQUE REFERENCES lamontana.evento_financiero,
 importe numeric(19,2) NOT NULL CHECK(importe>0),
 medio varchar(16) NOT NULL CHECK(medio IN ('EFECTIVO','TRANSFERENCIA')),
 referencia varchar(160) NOT NULL,
 clave_referencia varchar(230) NOT NULL UNIQUE,
 devuelto_en timestamptz NOT NULL
);
CREATE TABLE lamontana.reembolso_aplicacion (
 id_reembolso bigint NOT NULL REFERENCES lamontana.reembolso,
 id_aplicacion bigint NOT NULL REFERENCES lamontana.aplicacion_pago_cotizacion,
 importe numeric(19,2) NOT NULL CHECK(importe>0), PRIMARY KEY(id_reembolso,id_aplicacion)
);
CREATE INDEX pago_cotizacion ON lamontana.pago(id_cotizacion);
CREATE INDEX intento_cotizacion ON lamontana.intento_pago(id_cotizacion);
CREATE INDEX aplicacion_cotizacion ON lamontana.aplicacion_pago_cotizacion(id_cotizacion);
CREATE INDEX aplicacion_pago ON lamontana.aplicacion_pago_cotizacion(id_pago);
CREATE VIEW lamontana.aplicacion_pago_neta AS
 SELECT a.*, a.importe
  -coalesce((SELECT sum(l.importe) FROM lamontana.liberacion_aplicacion_pago l WHERE l.id_aplicacion=a.id_aplicacion),0)
  -coalesce((SELECT sum(r.importe) FROM lamontana.reembolso_aplicacion r WHERE r.id_aplicacion=a.id_aplicacion),0) AS neto
 FROM lamontana.aplicacion_pago_cotizacion a;
CREATE VIEW lamontana.pago_disponible AS
 SELECT p.*,coalesce((SELECT sum(r.importe) FROM lamontana.reembolso r WHERE r.id_pago=p.id_pago),0) AS devuelto,
 coalesce((SELECT sum(a.neto) FROM lamontana.aplicacion_pago_neta a WHERE a.id_pago=p.id_pago),0) AS aplicado,
 p.importe-coalesce((SELECT sum(r.importe) FROM lamontana.reembolso r WHERE r.id_pago=p.id_pago),0)
 -coalesce((SELECT sum(a.neto) FROM lamontana.aplicacion_pago_neta a WHERE a.id_pago=p.id_pago),0) AS disponible
 FROM lamontana.pago p;
CREATE FUNCTION lamontana.cotizacion_descendiente(destino bigint, origen bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 WITH RECURSIVE cadena AS (
 SELECT id_cotizacion,id_cotizacion_reemplazada FROM lamontana.cotizacion WHERE id_cotizacion=destino
 UNION ALL SELECT c.id_cotizacion,c.id_cotizacion_reemplazada FROM lamontana.cotizacion c JOIN cadena a ON a.id_cotizacion_reemplazada=c.id_cotizacion
 ) SELECT EXISTS(SELECT 1 FROM cadena WHERE id_cotizacion=origen)
$$;
CREATE FUNCTION lamontana.validar_libro_pagos() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM lamontana.pago_disponible WHERE disponible<0) OR
 EXISTS(SELECT 1 FROM lamontana.aplicacion_pago_neta WHERE neto<0) OR
 EXISTS(SELECT 1 FROM lamontana.cotizacion c JOIN lamontana.aplicacion_pago_neta a USING(id_cotizacion) GROUP BY c.id_cotizacion,c.total HAVING sum(a.neto)>c.total) OR
 EXISTS(SELECT 1 FROM lamontana.reembolso r JOIN lamontana.reembolso_aplicacion a USING(id_reembolso) GROUP BY r.id_reembolso,r.importe HAVING sum(a.importe)>r.importe) OR
 EXISTS(SELECT 1 FROM lamontana.reembolso_aplicacion x JOIN lamontana.reembolso r USING(id_reembolso) JOIN lamontana.aplicacion_pago_cotizacion a USING(id_aplicacion) WHERE r.id_pago<>a.id_pago) THEN
  RAISE EXCEPTION 'El dinero no puede aplicarse o devolverse más de una vez' USING ERRCODE='23514';
 END IF;
 IF EXISTS(SELECT 1 FROM lamontana.aplicacion_pago_cotizacion a JOIN lamontana.pago p USING(id_pago)
  JOIN lamontana.cotizacion o ON o.id_cotizacion=p.id_cotizacion JOIN lamontana.cotizacion d ON d.id_cotizacion=a.id_cotizacion
  WHERE o.id_usuario_creador<>d.id_usuario_creador OR NOT lamontana.cotizacion_descendiente(d.id_cotizacion,o.id_cotizacion)) OR
 EXISTS(SELECT 1 FROM lamontana.pago p JOIN lamontana.intento_pago i USING(id_intento_pago)
 LEFT JOIN lamontana.resolucion_intento_pago r USING(id_intento_pago)
 WHERE p.id_cotizacion<>i.id_cotizacion OR r.resultado IS DISTINCT FROM 'ACREDITADO') OR
 EXISTS(SELECT 1 FROM lamontana.resolucion_intento_pago r WHERE (r.resultado='ACREDITADO') IS DISTINCT FROM EXISTS(SELECT 1 FROM lamontana.pago p WHERE p.id_intento_pago=r.id_intento_pago)) THEN
  RAISE EXCEPTION 'El pago debe conservar su cotización, pagador e intento confirmado' USING ERRCODE='23514';
 END IF;
 RETURN NULL;
END $$;
CREATE FUNCTION lamontana.bloquear_libro_pagos() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN PERFORM pg_advisory_xact_lock(764004); RETURN NEW; END $$;
DO $$ DECLARE tabla text; BEGIN
 FOREACH tabla IN ARRAY ARRAY['evento_financiero','intento_pago','resolucion_intento_pago','pago','aplicacion_pago_cotizacion','liberacion_aplicacion_pago','reembolso','reembolso_aplicacion'] LOOP
  EXECUTE format('CREATE TRIGGER %I BEFORE UPDATE OR DELETE ON lamontana.%I FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion()',tabla||'_inmutable',tabla);
  EXECUTE format('CREATE TRIGGER %I BEFORE INSERT ON lamontana.%I FOR EACH ROW EXECUTE FUNCTION lamontana.bloquear_libro_pagos()',tabla||'_bloqueo',tabla);
  EXECUTE format('CREATE CONSTRAINT TRIGGER %I AFTER INSERT ON lamontana.%I DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_libro_pagos()',tabla||'_coherente',tabla);
 END LOOP;
END $$;
