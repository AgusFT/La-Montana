-- Cierra únicamente la intervención iniciada por un comprobante de un cobro en término.
CREATE FUNCTION lamontana.resolver_revision_comprobante(pedido bigint) RETURNS boolean LANGUAGE sql AS $$
 WITH resuelto AS (
  UPDATE lamontana.cobro_pedido cp SET intervencion_pendiente=false,version=cp.version+1
  FROM lamontana.pedido_actual p
  WHERE cp.id_pedido=pedido AND p.id_pedido=cp.id_pedido AND p.estado NOT IN ('CANCELADO','RECHAZADO')
   AND cp.intervencion_pendiente AND cp.definitivo_en IS NULL
   AND cp.cobertura_confirmada_en<cp.fecha_limite
   AND lamontana.produccion_cubierta(pedido)
   AND NOT lamontana.cobro_informado_pendiente(pedido)
   AND NOT lamontana.cobro_comprobante_validando(pedido)
   AND NOT EXISTS(SELECT 1 FROM lamontana.eliminacion_archivo a WHERE a.id_pedido=pedido)
   AND EXISTS(SELECT 1 FROM lamontana.evento_cobro_pedido e
    WHERE e.id_pedido=pedido AND e.tipo='COMPROBANTE_PRESENTADO'
    AND EXISTS(SELECT 1 FROM lamontana.evento_cobro_pedido a
     WHERE a.id_pedido=pedido AND a.tipo='APLICAR' AND a.id_evento>e.id_evento AND a.fecha<cp.fecha_limite)
    AND e.id_evento>coalesce((SELECT max(r.id_evento) FROM lamontana.evento_cobro_pedido r
     WHERE r.id_pedido=pedido AND r.resultado #>> '{intervencion_pendiente}'='false'),0))
   AND NOT EXISTS(SELECT 1 FROM lamontana.evento_cobro_pedido e
    WHERE e.id_pedido=pedido AND (e.tipo='DESCARTAR' OR e.resultado ? 'solicitud')
    AND e.resultado #>> '{intervencion_pendiente}' IS DISTINCT FROM 'false'
    AND e.id_evento>coalesce((SELECT max(r.id_evento) FROM lamontana.evento_cobro_pedido r
     WHERE r.id_pedido=pedido AND r.resultado #>> '{intervencion_pendiente}'='false'),0))
  RETURNING cp.id_pedido
 )
 SELECT EXISTS(SELECT 1 FROM resuelto)
$$;

INSERT INTO lamontana.evento_cobro_pedido(id_pedido,id_operacion,tipo,motivo,resultado)
SELECT cp.id_pedido,gen_random_uuid(),'APLICAR',
 'Revisión de comprobante cerrada tras la acreditación en término.',jsonb_build_object('intervencion_pendiente',false)
FROM lamontana.cobro_pedido cp
WHERE cp.intervencion_pendiente AND lamontana.resolver_revision_comprobante(cp.id_pedido);
