CREATE TABLE lamontana.caja_egreso (
 id_operacion uuid PRIMARY KEY,
 id_sucursal bigint NOT NULL REFERENCES lamontana.sucursal,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 actor_nombre varchar(300) NOT NULL,
 tipo varchar(24) NOT NULL CHECK(tipo IN ('PAGO_EMPLEADO','PAGO_PROVEEDOR','RENDICION')),
 medio varchar(16) NOT NULL CHECK(medio IN ('EFECTIVO','TRANSFERENCIA')),
 importe numeric(19,2) NOT NULL CHECK(importe>0),
 destinatario varchar(140) NOT NULL CHECK(length(trim(destinatario))>0),
 concepto varchar(300) NOT NULL CHECK(length(trim(concepto))>0),
 fecha timestamptz NOT NULL,
 huella char(64) NOT NULL,
 comprobante_tipo varchar(4) CHECK(comprobante_tipo IN ('pdf','jpg','png')),
 comprobante bytea CHECK(octet_length(comprobante) BETWEEN 1 AND 2097152),
 CHECK((comprobante IS NULL)=(comprobante_tipo IS NULL)),
 CHECK(tipo<>'RENDICION' OR medio='EFECTIVO')
);
CREATE INDEX caja_egreso_sucursal_fecha ON lamontana.caja_egreso(id_sucursal,fecha DESC);

CREATE TABLE lamontana.caja_arqueo (
 id_operacion uuid PRIMARY KEY,
 id_sucursal bigint NOT NULL REFERENCES lamontana.sucursal,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 actor_nombre varchar(300) NOT NULL,
 fecha timestamptz NOT NULL,
 esperado numeric NOT NULL,
 contado numeric(19,2) NOT NULL CHECK(contado>=0),
 diferencia numeric GENERATED ALWAYS AS (contado-esperado) STORED,
 observacion varchar(300) NOT NULL,
 huella char(64) NOT NULL
);
CREATE INDEX caja_arqueo_sucursal_fecha ON lamontana.caja_arqueo(id_sucursal,fecha DESC);

CREATE TRIGGER caja_egreso_inmutable BEFORE UPDATE OR DELETE ON lamontana.caja_egreso
 FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TRIGGER caja_arqueo_inmutable BEFORE UPDATE OR DELETE ON lamontana.caja_arqueo
 FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();

-- El pago original cuenta una vez. Aplicarlo a uno o más pedidos no mueve dinero.
-- El cobro atómico de mostrador identifica el destino real por su operación de entrega,
-- aun cuando el pedido se haya producido en otra sucursal.
CREATE VIEW lamontana.caja_movimiento AS
WITH cobro_en_entrega AS (
 SELECT 'Entrega PED-' || en.id_pedido || ' · ' || h.id_operacion AS referencia,
        r.modalidad, s.id_sucursal, p.id_cotizacion, h.id_actor
 FROM lamontana.entrega en
 JOIN lamontana.historial_estado_pedido h ON h.id_evento=en.id_evento_pedido
 JOIN lamontana.reserva_entrega r ON r.id_reserva_entrega=en.id_reserva_entrega
 JOIN lamontana.pedido_actual p ON p.id_pedido=en.id_pedido
 LEFT JOIN lamontana.sucursal s ON r.modalidad='RETIRO_SUCURSAL' AND s.codigo_publico=r.destino_publico
)
SELECT CASE WHEN p.medio='EFECTIVO' THEN coalesce(d.id_sucursal,c.id_sucursal) ELSE c.id_sucursal END AS id_sucursal,
       p.codigo_publico AS codigo, 'COBRO'::text AS tipo, p.medio, p.importe,
       p.recibido_en AS fecha, e.actor_nombre AS actor, p.referencia AS concepto,
       null::varchar AS destinatario, false AS adjunto, c.codigo_publico AS cotizacion
FROM lamontana.pago p
JOIN lamontana.evento_financiero e USING(id_evento)
JOIN lamontana.cotizacion c ON c.id_cotizacion=p.id_cotizacion
LEFT JOIN cobro_en_entrega d ON d.referencia=p.referencia AND d.id_cotizacion=p.id_cotizacion AND d.id_actor=e.id_actor
WHERE p.medio<>'EFECTIVO' OR (
 (d.modalidad IS NULL OR d.id_sucursal IS NOT NULL)
 AND NOT EXISTS(SELECT 1 FROM lamontana.cuenta_punto cp WHERE cp.id_usuario=e.id_actor)
)
UNION ALL
SELECT c.id_sucursal,r.codigo_publico,'DEVOLUCION',r.medio,-r.importe,
       r.devuelto_en,e.actor_nombre,r.referencia,null::varchar,false,c.codigo_publico
FROM lamontana.reembolso r
JOIN lamontana.evento_financiero e USING(id_evento)
JOIN lamontana.cotizacion c ON c.id_cotizacion=e.id_cotizacion
WHERE r.medio<>'EFECTIVO' OR NOT EXISTS(
 SELECT 1 FROM lamontana.cuenta_punto cp WHERE cp.id_usuario=e.id_actor
)
UNION ALL
SELECT g.id_sucursal,g.id_operacion,g.tipo,g.medio,-g.importe,g.fecha,g.actor_nombre,
       g.concepto,g.destinatario,g.comprobante_tipo IS NOT NULL,null::uuid
FROM lamontana.caja_egreso g;
