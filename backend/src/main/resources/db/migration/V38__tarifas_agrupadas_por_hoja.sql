-- ========================================================================
-- FUNCIÓN: incorpora nombres y agrupación de tarifas y tres formas de definir
-- el precio final de una hoja doble faz. Los valores nulos conservan el cálculo
-- histórico por carilla; no se reescribe ninguna tarifa ni migración anterior.
-- TABLA: lamontana.tarifa_impresion. FUNCIONES: no declara.
-- ========================================================================
ALTER TABLE lamontana.tarifa_impresion
    ADD COLUMN grupo uuid,
    ADD COLUMN nombre varchar(140),
    ADD COLUMN modo_doble_faz varchar(16),
    ADD COLUMN valor_doble_faz numeric(14,2),
    ADD CONSTRAINT tarifa_grupo_nombre CHECK (
        (grupo IS NULL AND nombre IS NULL) OR
        (grupo IS NOT NULL AND nombre IS NOT NULL AND length(btrim(nombre)) > 0)),
    ADD CONSTRAINT tarifa_precio_doble_faz CHECK (
        (modo_doble_faz IS NULL AND valor_doble_faz IS NULL) OR
        (modo_doble_faz IS NOT NULL AND modo_doble_faz IN ('FIJO','ADICIONAL','PORCENTAJE')
         AND valor_doble_faz IS NOT NULL AND valor_doble_faz >= 0
         AND recargo_doble_faz = 0 AND grupo IS NOT NULL));
