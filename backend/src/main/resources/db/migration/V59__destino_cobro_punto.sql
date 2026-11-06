ALTER TABLE lamontana.pago
 ADD COLUMN destino_cobro_punto varchar(20),
 ADD COLUMN nombre_destino_cobro varchar(160),
 ADD CONSTRAINT pago_destino_cobro_punto_valido CHECK (
  (destino_cobro_punto IS NULL AND nombre_destino_cobro IS NULL)
  OR (nombre_destino_cobro IS NOT NULL AND length(trim(nombre_destino_cobro))>0
      AND destino_cobro_punto IS NOT NULL
      AND (destino_cobro_punto='PUNTO_ENTREGA'
           OR (destino_cobro_punto='SUCURSAL_ORIGEN' AND medio='TRANSFERENCIA')))
 );

-- Las transferencias recibidas por el punto se ven en sus finanzas.
-- Las acreditadas en origen siguen en la caja de la sucursal que generó la cotización.
CREATE OR REPLACE VIEW lamontana.caja_movimiento AS
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
WHERE p.destino_cobro_punto IS DISTINCT FROM 'PUNTO_ENTREGA' AND (p.medio<>'EFECTIVO' OR (
 (d.modalidad IS NULL OR d.id_sucursal IS NOT NULL)
 AND NOT EXISTS(SELECT 1 FROM lamontana.cuenta_punto cp WHERE cp.id_usuario=e.id_actor)
))
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
