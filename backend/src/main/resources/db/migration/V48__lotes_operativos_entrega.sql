-- Un grupo sólo reúne preparaciones o despachos. Cada pedido conserva su movimiento y código.
CREATE TABLE lamontana.lote_movimiento_entrega (
 id_operacion uuid PRIMARY KEY,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 actor_nombre varchar(300) NOT NULL,
 huella char(64) NOT NULL,
 accion varchar(24) NOT NULL CHECK(accion IN ('PREPARAR_ENVIO','SALIR_REPARTO')),
 pedidos jsonb NOT NULL CHECK(jsonb_typeof(pedidos)='array' AND jsonb_array_length(pedidos) BETWEEN 1 AND 25),
 fecha timestamptz NOT NULL
);
CREATE TRIGGER lote_movimiento_entrega_inmutable BEFORE UPDATE OR DELETE ON lamontana.lote_movimiento_entrega
 FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
