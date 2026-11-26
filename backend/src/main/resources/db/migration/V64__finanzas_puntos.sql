-- Las instrucciones son versiones inmutables. Una rendición conserva el acuerdo vigente al informarse.
CREATE TABLE lamontana.instruccion_punto (
 operacion uuid PRIMARY KEY, secuencia bigint GENERATED ALWAYS AS IDENTITY UNIQUE,
 id_punto_entrega bigint NOT NULL REFERENCES lamontana.punto_entrega,
 id_sucursal bigint REFERENCES lamontana.sucursal,
 uso text NOT NULL CHECK(uso IN ('COBRO_PUNTO','COBRO_ORIGEN','RENDICION')),
 medio text NOT NULL CHECK(medio IN ('EFECTIVO','TRANSFERENCIA')),
 instrucciones varchar(500) NOT NULL, habilitada boolean NOT NULL, verifica_punto boolean NOT NULL,verifica_admin boolean NOT NULL DEFAULT false,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario, fecha timestamptz NOT NULL DEFAULT clock_timestamp(),
 CHECK ((uso='COBRO_PUNTO')=(id_sucursal IS NULL)),
 CHECK (medio='TRANSFERENCIA' OR uso='RENDICION'), CHECK (NOT verifica_punto OR uso='COBRO_PUNTO'),
 CHECK (NOT habilitada OR medio='EFECTIVO' OR length(trim(instrucciones))>0)
);
CREATE VIEW lamontana.instruccion_punto_actual AS
 SELECT DISTINCT ON(id_punto_entrega,id_sucursal,uso,medio) * FROM lamontana.instruccion_punto
 ORDER BY id_punto_entrega,id_sucursal,uso,medio,secuencia DESC;

CREATE TABLE lamontana.aviso_punto (
 id_intento_pago bigint PRIMARY KEY REFERENCES lamontana.intento_pago,
 instruccion uuid NOT NULL REFERENCES lamontana.instruccion_punto,
 operacion uuid NOT NULL UNIQUE, id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 huella char(64) NOT NULL, fecha timestamptz NOT NULL DEFAULT clock_timestamp()
);
ALTER TABLE lamontana.reembolso ADD COLUMN id_punto_pagador bigint REFERENCES lamontana.punto_entrega;
CREATE TABLE lamontana.rendicion_punto (
 operacion uuid PRIMARY KEY, instruccion uuid NOT NULL REFERENCES lamontana.instruccion_punto,
 importe numeric(19,2) NOT NULL CHECK(importe>0), referencia varchar(160), receptor varchar(140),
 observacion varchar(300), comprobante bytea CHECK(octet_length(comprobante) BETWEEN 1 AND 2097152),
 id_actor bigint NOT NULL REFERENCES lamontana.usuario, fecha timestamptz NOT NULL DEFAULT clock_timestamp(),
 huella char(64) NOT NULL
);
CREATE TABLE lamontana.resolucion_rendicion (
 operacion uuid PRIMARY KEY, rendicion uuid NOT NULL UNIQUE REFERENCES lamontana.rendicion_punto,
 estado text NOT NULL CHECK(estado IN ('CONFIRMADA','RECHAZADA')), motivo varchar(300),
 id_actor bigint NOT NULL REFERENCES lamontana.usuario, fecha timestamptz NOT NULL DEFAULT clock_timestamp(),
 CHECK(estado='CONFIRMADA' OR nullif(trim(motivo),'') IS NOT NULL)
);
DO $$ DECLARE tabla text; BEGIN
 FOREACH tabla IN ARRAY ARRAY['aviso_punto','instruccion_punto','rendicion_punto','resolucion_rendicion'] LOOP
 EXECUTE format('CREATE TRIGGER inmutable BEFORE UPDATE OR DELETE ON lamontana.%I FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion()',tabla);
 END LOOP;
END $$;
ALTER TABLE lamontana.caja_egreso DROP CONSTRAINT caja_egreso_tipo_check,
 ADD COLUMN id_punto_beneficiario bigint REFERENCES lamontana.punto_entrega, ADD COLUMN referencia varchar(160),
 ADD CHECK(tipo IN ('PAGO_EMPLEADO','PAGO_PROVEEDOR','RENDICION','PAGO_PUNTO_ENTREGA')),
 ADD CHECK((tipo='PAGO_PUNTO_ENTREGA')=(id_punto_beneficiario IS NOT NULL)),
 ADD CHECK(tipo<>'PAGO_PUNTO_ENTREGA' OR medio='TRANSFERENCIA' AND nullif(trim(referencia),'') IS NOT NULL);
INSERT INTO lamontana.permiso(codigo,nombre,descripcion,alcance) VALUES
 ('INFORMAR_RENDICION_PUNTO','Informar rendiciones por un punto','Informar fondos de un punto con acuerdo en la sucursal asignada.','SUCURSAL'),
 ('CONFIRMAR_RENDICION_PUNTO','Confirmar rendiciones de puntos','Confirmar fondos recibidos en la sucursal asignada.','SUCURSAL'),
 ('PAGAR_PUNTO','Registrar pagos a puntos','Registrar transferencias por servicios del punto.','SUCURSAL');

CREATE TABLE lamontana.revision_destino_historico (
 operacion uuid PRIMARY KEY,id_pago bigint NOT NULL UNIQUE REFERENCES lamontana.pago,
 destino text NOT NULL CHECK(destino IN ('PUNTO_ENTREGA','SUCURSAL_ORIGEN')),
 motivo varchar(300) NOT NULL CHECK(length(trim(motivo))>0),
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,fecha timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TRIGGER inmutable BEFORE UPDATE OR DELETE ON lamontana.revision_destino_historico
 FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();

-- El vínculo inmutable de la cuenta histórica identifica quién gestionó. Sólo V59
-- (destino explícito) o una instrucción firmada identifican al dueño del dinero.
CREATE VIEW lamontana.movimiento_punto AS
 SELECT coalesce(ins.id_punto_entrega,cp.id_punto_entrega) id_punto_entrega,p.id_cotizacion,e.id_evento,
 p.codigo_publico codigo,'COBRO'::text tipo,p.importe,p.medio,p.referencia,p.recibido_en fecha,
 e.actor_nombre verificador,coalesce(gestor.nombre||' '||gestor.apellido,e.actor_nombre) responsable,
 coalesce(p.destino_cobro_punto,h.destino) destino,coalesce(p.nombre_destino_cobro,CASE h.destino WHEN 'PUNTO_ENTREGA' THEN pt.codigo WHEN 'SUCURSAL_ORIGEN' THEN s.nombre END) destinatario,'ACREDITADO'::text estado
 FROM lamontana.pago p JOIN lamontana.evento_financiero e USING(id_evento)
 LEFT JOIN lamontana.cuenta_punto cp ON cp.id_usuario=e.id_actor
 LEFT JOIN lamontana.intento_pago i USING(id_intento_pago)
 LEFT JOIN lamontana.aviso_punto av ON av.id_intento_pago=i.id_intento_pago
 LEFT JOIN lamontana.instruccion_punto ins ON ins.operacion=av.instruccion
 LEFT JOIN lamontana.usuario gestor ON gestor.id_usuario=av.id_actor
 LEFT JOIN lamontana.revision_destino_historico h ON h.id_pago=p.id_pago
 LEFT JOIN lamontana.punto_entrega pt ON pt.id_punto_entrega=cp.id_punto_entrega
 JOIN lamontana.cotizacion c ON c.id_cotizacion=p.id_cotizacion JOIN lamontana.sucursal s ON s.id_sucursal=c.id_sucursal
 WHERE cp.id_punto_entrega IS NOT NULL OR ins.operacion IS NOT NULL
 UNION ALL
 SELECT ins.id_punto_entrega,i.id_cotizacion,e.id_evento,i.codigo_publico,'AVISO',i.importe_informado,'TRANSFERENCIA',
 i.referencia_informada,av.fecha,null,gestor.nombre||' '||gestor.apellido,CASE ins.uso WHEN 'COBRO_PUNTO' THEN 'PUNTO_ENTREGA' ELSE 'SUCURSAL_ORIGEN' END,
 coalesce(s.nombre,pt.codigo),coalesce(res.resultado,'PENDIENTE')
 FROM lamontana.intento_pago i JOIN lamontana.aviso_punto av USING(id_intento_pago) JOIN lamontana.instruccion_punto ins ON ins.operacion=av.instruccion
 JOIN lamontana.evento_financiero e ON e.id_evento=i.id_evento JOIN lamontana.punto_entrega pt USING(id_punto_entrega)
 JOIN lamontana.usuario gestor ON gestor.id_usuario=av.id_actor LEFT JOIN lamontana.sucursal s USING(id_sucursal) LEFT JOIN lamontana.resolucion_intento_pago res USING(id_intento_pago)
 WHERE res.resultado IS DISTINCT FROM 'ACREDITADO'
 UNION ALL
 SELECT coalesce(re.id_punto_pagador,ins.id_punto_entrega,cp.id_punto_entrega),p.id_cotizacion,e.id_evento,re.codigo_publico,'DEVOLUCION',
 -re.importe,re.medio,re.referencia,re.devuelto_en,e.actor_nombre,e.actor_nombre,
 CASE WHEN re.id_punto_pagador IS NOT NULL THEN 'PUNTO_ENTREGA' END,CASE WHEN re.id_punto_pagador IS NOT NULL THEN 'Cliente · devolución con fondos del punto' END,'DEVUELTO'
 FROM lamontana.reembolso re JOIN lamontana.evento_financiero e USING(id_evento)
 JOIN lamontana.pago p USING(id_pago) JOIN lamontana.evento_financiero ep ON ep.id_evento=p.id_evento
 LEFT JOIN lamontana.cuenta_punto cp ON cp.id_usuario=ep.id_actor LEFT JOIN lamontana.intento_pago i USING(id_intento_pago)
 LEFT JOIN lamontana.aviso_punto av ON av.id_intento_pago=i.id_intento_pago
 LEFT JOIN lamontana.instruccion_punto ins ON ins.operacion=av.instruccion
 WHERE coalesce(re.id_punto_pagador,ins.id_punto_entrega,cp.id_punto_entrega) IS NOT NULL;

CREATE VIEW lamontana.saldo_punto AS
 WITH fondos AS (SELECT id_punto_entrega,sum(importe) FILTER(WHERE tipo='COBRO') recibido,
 sum(-importe) FILTER(WHERE tipo='DEVOLUCION') devuelto FROM lamontana.movimiento_punto
 WHERE destino='PUNTO_ENTREGA' GROUP BY id_punto_entrega),
 rendido AS (SELECT i.id_punto_entrega,sum(r.importe) FILTER(WHERE x.estado='CONFIRMADA') confirmado,
 sum(r.importe) FILTER(WHERE x.estado IS NULL) reservado FROM lamontana.rendicion_punto r
 JOIN lamontana.instruccion_punto i ON i.operacion=r.instruccion LEFT JOIN lamontana.resolucion_rendicion x ON x.rendicion=r.operacion
 GROUP BY i.id_punto_entrega)
 SELECT pt.id_punto_entrega,coalesce(recibido,0) recibido,coalesce(devuelto,0) devuelto,
 coalesce(confirmado,0) confirmado,coalesce(reservado,0) reservado,
 coalesce(recibido,0)-coalesce(devuelto,0)-coalesce(confirmado,0) pendiente
 FROM lamontana.punto_entrega pt LEFT JOIN fondos USING(id_punto_entrega) LEFT JOIN rendido USING(id_punto_entrega);

-- La recepción entra en la caja física/bancaria una vez, con un concepto propio.
ALTER VIEW lamontana.caja_movimiento RENAME TO caja_movimiento_externo;
CREATE VIEW lamontana.caja_movimiento AS SELECT * FROM lamontana.caja_movimiento_externo v
 WHERE (v.tipo<>'DEVOLUCION' OR NOT EXISTS(SELECT 1 FROM lamontana.reembolso r WHERE r.codigo_publico=v.codigo AND r.id_punto_pagador IS NOT NULL))
 AND (v.tipo<>'COBRO' OR NOT EXISTS(SELECT 1 FROM lamontana.revision_destino_historico h JOIN lamontana.pago p USING(id_pago) WHERE p.codigo_publico=v.codigo AND h.destino='PUNTO_ENTREGA'))
 UNION ALL SELECT i.id_sucursal,x.operacion,'RECEPCION_PUNTO',i.medio,r.importe,x.fecha,u.nombre||' '||u.apellido,
 coalesce(r.referencia,r.receptor),pt.codigo,false,null::uuid FROM lamontana.resolucion_rendicion x
 JOIN lamontana.rendicion_punto r ON r.operacion=x.rendicion JOIN lamontana.instruccion_punto i ON i.operacion=r.instruccion
 JOIN lamontana.punto_entrega pt USING(id_punto_entrega) JOIN lamontana.usuario u ON u.id_usuario=x.id_actor WHERE x.estado='CONFIRMADA';

CREATE TABLE lamontana.imputacion_rendicion (
 rendicion uuid NOT NULL REFERENCES lamontana.rendicion_punto,
 id_evento bigint NOT NULL REFERENCES lamontana.evento_financiero,
 importe numeric(19,2) NOT NULL CHECK(importe>0), PRIMARY KEY(rendicion,id_evento)
);
CREATE TRIGGER inmutable BEFORE UPDATE OR DELETE ON lamontana.imputacion_rendicion
 FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();

CREATE UNIQUE INDEX egreso_punto_referencia ON lamontana.caja_egreso(id_sucursal,lower(trim(referencia)))
 WHERE tipo='PAGO_PUNTO_ENTREGA';
CREATE INDEX rendicion_instruccion_fecha ON lamontana.rendicion_punto(instruccion,fecha DESC);
CREATE INDEX imputacion_por_cobro ON lamontana.imputacion_rendicion(id_evento);
