-- Un empleado puede habilitarse como rider sin crear una segunda identidad.
CREATE TABLE lamontana.cuenta_repartidor (
 id_usuario bigint PRIMARY KEY REFERENCES lamontana.usuario,
 activa boolean NOT NULL DEFAULT true,
 creado_en timestamptz NOT NULL DEFAULT now(),
 actualizado_en timestamptz NOT NULL DEFAULT now()
);

-- La salida conjunta crea un recorrido y transfiere la custodia al rider elegido.
CREATE TABLE lamontana.recorrido_reparto (
 id_recorrido bigserial PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_operacion uuid NOT NULL UNIQUE,
 id_repartidor bigint NOT NULL REFERENCES lamontana.cuenta_repartidor(id_usuario),
 id_actor_asignador bigint NOT NULL REFERENCES lamontana.usuario,
 actor_asignador_nombre varchar(300) NOT NULL,
 estado varchar(24) NOT NULL CHECK (estado IN ('EN_REPARTO','FINALIZADO','CANCELADO')),
 fecha_asignacion timestamptz NOT NULL
);

CREATE TABLE lamontana.recorrido_reparto_pedido (
 id_recorrido bigint NOT NULL REFERENCES lamontana.recorrido_reparto,
 id_pedido bigint NOT NULL UNIQUE REFERENCES lamontana.pedido,
 orden smallint NOT NULL CHECK (orden BETWEEN 1 AND 25),
 PRIMARY KEY (id_recorrido,id_pedido),
 UNIQUE (id_recorrido,orden)
);

CREATE TABLE lamontana.evento_recorrido_reparto (
 id_evento bigserial PRIMARY KEY,
 id_recorrido bigint NOT NULL REFERENCES lamontana.recorrido_reparto,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 actor_nombre varchar(300) NOT NULL,
 accion varchar(32) NOT NULL CHECK (accion IN ('ASIGNAR_E_INICIAR','REASIGNAR','FINALIZAR','CANCELAR')),
 id_repartidor bigint REFERENCES lamontana.usuario,
 fecha timestamptz NOT NULL
);
CREATE TRIGGER evento_recorrido_reparto_inmutable BEFORE UPDATE OR DELETE ON lamontana.evento_recorrido_reparto
 FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();

ALTER TABLE lamontana.lote_movimiento_entrega
 ADD COLUMN id_recorrido bigint REFERENCES lamontana.recorrido_reparto;

