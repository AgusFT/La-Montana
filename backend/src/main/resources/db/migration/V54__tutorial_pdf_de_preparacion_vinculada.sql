-- El cliente puede elegir una preparación cuyo PDF ya se validó antes de iniciar el tutorial.
CREATE OR REPLACE VIEW lamontana.tutorial_progreso AS
SELECT t.estado, t.paso, t.preparacion, c.codigo_publico AS cotizacion, p.codigo_publico AS pedido,
       s.codigo_publico AS destino, pc.id_usuario, p.id_sucursal,
       t.tarifa_revisada AND EXISTS (
           SELECT FROM lamontana.catalogo_revision r JOIN lamontana.tarifa_impresion ti USING(id_catalogo_revision)
           WHERE r.vigente) AS tarifa,
       EXISTS (SELECT FROM lamontana.archivo_preparado ap JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado)
               WHERE ap.id_preparacion=pc.id_preparacion AND ap.retirado_en IS NULL AND a.estado='VALIDO') AS archivo,
       pc.presupuesto IS NOT NULL AS presupuesto,
       p.id_pedido IS NOT NULL AS confirmacion,
       coalesce(lamontana.saldo_entrega_cubierto(p.id_pedido),false) AS cobro,
       p.aprobada_en IS NOT NULL AS revision,
       EXISTS (SELECT FROM lamontana.pedido_archivo_actual a WHERE a.id_pedido=p.id_pedido)
       AND NOT EXISTS (
           SELECT FROM lamontana.pedido_archivo_actual a
           LEFT JOIN LATERAL (SELECT tr.estado FROM lamontana.trabajo_impresion tr
              WHERE tr.id_pedido=a.id_pedido AND tr.id_cotizacion_item=a.id_cotizacion_item
              ORDER BY tr.id_trabajo_impresion DESC LIMIT 1) ultimo ON true
           WHERE a.id_pedido=p.id_pedido AND ultimo.estado IS DISTINCT FROM 'COMPLETADO') AS produccion,
       lamontana.calidad_pedido_aprobada(p.id_pedido) AS calidad,
       EXISTS (SELECT FROM lamontana.cierre_pedido cierre WHERE cierre.id_pedido=p.id_pedido) AS entrega,
       (p.estado IN ('RECHAZADO','CANCELADO') OR cupo.baja_en IS NOT NULL) IS TRUE AS inactivo
FROM lamontana.tutorial_demo t
LEFT JOIN lamontana.preparacion_cliente pc ON pc.codigo_publico=t.preparacion
LEFT JOIN lamontana.cotizacion c ON c.id_cotizacion=pc.id_ultima_cotizacion
LEFT JOIN lamontana.pedido_actual p ON p.id_pedido=pc.id_pedido
LEFT JOIN lamontana.sucursal s ON s.id_sucursal=p.id_sucursal
LEFT JOIN lamontana.demo_cupo_registro cupo ON cupo.tipo='PEDIDO' AND cupo.codigo_publico=p.codigo_publico;
