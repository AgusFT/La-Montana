-- La selección administrativa no cambia snapshots de precios ni pedidos.
CREATE TABLE lamontana.catalogo_papel (
    id_formato bigint NOT NULL REFERENCES lamontana.formato,
    id_papel bigint NOT NULL REFERENCES lamontana.papel,
    habilitado boolean NOT NULL DEFAULT true,
    predefinido varchar(40) UNIQUE,
    PRIMARY KEY(id_formato,id_papel)
);
-- Antes podían elegirse todos los pares; conservar esa disponibilidad al migrar.
INSERT INTO lamontana.catalogo_papel(id_formato,id_papel)
SELECT id_formato,id_papel FROM lamontana.formato CROSS JOIN lamontana.papel;
