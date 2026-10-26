-- La impresora pertenece a la imprenta. La sucursal es únicamente una ubicación interna
-- opcional y versionada; no habilita servicios ni limita la compatibilidad comercial.
ALTER TABLE lamontana.impresora
    ALTER COLUMN id_sucursal DROP NOT NULL;

ALTER TABLE lamontana.configuracion_impresora
    ADD COLUMN id_sucursal_ubicacion bigint REFERENCES lamontana.sucursal;

UPDATE lamontana.configuracion_impresora c
SET id_sucursal_ubicacion=i.id_sucursal
FROM lamontana.impresora i
WHERE i.id_impresora=c.id_impresora;

COMMENT ON COLUMN lamontana.impresora.id_sucursal IS
    'Referencia heredada anterior a V40. Las versiones nuevas usan configuracion_impresora.id_sucursal_ubicacion.';
COMMENT ON COLUMN lamontana.configuracion_impresora.id_sucursal_ubicacion IS
    'Ubicación física interna opcional. No limita catálogo, cotización, compatibilidad ni elección productiva.';
