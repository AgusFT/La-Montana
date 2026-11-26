CREATE FUNCTION lamontana.exigir_cliente_demo_vigente() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    baja timestamptz;
BEGIN
    -- Una nueva operación comercial y la baja de la ficha se ordenan sobre la misma fila.
    SELECT r.baja_en INTO baja
    FROM lamontana.demo_cupo_registro r
    JOIN lamontana.usuario u ON u.codigo_publico = r.codigo_publico
    WHERE r.tipo = 'CLIENTE' AND u.id_usuario = NEW.id_usuario_creador
    FOR SHARE OF r;
    IF baja IS NOT NULL THEN
        RAISE EXCEPTION 'La ficha de cliente está dada de baja en la Demo.' USING ERRCODE = '23D12';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER demo_cliente_vigente BEFORE INSERT ON lamontana.cotizacion
    FOR EACH ROW EXECUTE FUNCTION lamontana.exigir_cliente_demo_vigente();
CREATE TRIGGER demo_cliente_vigente BEFORE INSERT ON lamontana.pedido
    FOR EACH ROW EXECUTE FUNCTION lamontana.exigir_cliente_demo_vigente();

-- La ficha vinculada al botón Cliente de V48 siempre conserva su lugar y su acceso.
CREATE FUNCTION lamontana.es_ficha_reservada_demo(tipo text, codigo uuid) RETURNS boolean
    LANGUAGE sql IMMUTABLE AS $$
        SELECT tipo='CLIENTE' AND codigo='4d9fdf39-1f90-4d08-b7b6-0c77fc9f0073'::uuid
    $$;

CREATE FUNCTION lamontana.conservar_ficha_selector_demo() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'La ficha de Lucía Papel está reservada para el selector de Cliente.'
        USING ERRCODE='23D15';
END $$;

CREATE TRIGGER demo_conservar_ficha_selector BEFORE UPDATE OF baja_en ON lamontana.demo_cupo_registro
    FOR EACH ROW WHEN (NEW.baja_en IS NOT NULL AND lamontana.es_ficha_reservada_demo(NEW.tipo, NEW.codigo_publico))
    EXECUTE FUNCTION lamontana.conservar_ficha_selector_demo();
