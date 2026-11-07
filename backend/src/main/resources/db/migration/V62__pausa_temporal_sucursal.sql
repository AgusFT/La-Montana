-- La pausa comercial conserva el acceso a los pedidos y reservas existentes.
CREATE TABLE lamontana.disponibilidad_sucursal (
    id_sucursal bigint PRIMARY KEY REFERENCES lamontana.sucursal,
    estado text NOT NULL CHECK (estado IN ('ABIERTA','PAUSADA')),
    version bigint NOT NULL CHECK (version > 0),
    id_actor bigint NOT NULL REFERENCES lamontana.usuario,
    actualizada_en timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE lamontana.evento_disponibilidad_sucursal (
    id_operacion uuid PRIMARY KEY,
    id_sucursal bigint NOT NULL REFERENCES lamontana.sucursal,
    id_actor bigint NOT NULL REFERENCES lamontana.usuario,
    huella text NOT NULL,
    estado_anterior text NOT NULL CHECK (estado_anterior IN ('ABIERTA','PAUSADA')),
    estado_nuevo text NOT NULL CHECK (estado_nuevo IN ('ABIERTA','PAUSADA')),
    version_anterior bigint NOT NULL,
    version_nueva bigint NOT NULL,
    fecha timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX evento_disponibilidad_sucursal_version ON lamontana.evento_disponibilidad_sucursal(id_sucursal,version_nueva DESC);
