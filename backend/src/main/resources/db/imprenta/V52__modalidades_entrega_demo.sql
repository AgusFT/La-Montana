-- Las configuraciones y operaciones históricas conservan su modalidad y su destino.
CREATE VIEW lamontana.modalidades_entrega_demo (modalidad) AS
    VALUES ('RETIRO_SUCURSAL'::text), ('ENVIO_DOMICILIO'::text);

CREATE FUNCTION lamontana.rechazar_punto_movil_demo() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'El retiro en puntos de entrega móviles no está disponible en la Demo.'
        USING ERRCODE = '23D16';
END;
$$;

CREATE TRIGGER demo_punto_cotizacion BEFORE INSERT ON lamontana.cotizacion
    FOR EACH ROW WHEN (NEW.oferta->>'modalidad' = 'RETIRO_PUNTO_ENTREGA')
    EXECUTE FUNCTION lamontana.rechazar_punto_movil_demo();
CREATE TRIGGER demo_punto_reserva BEFORE INSERT ON lamontana.reserva_entrega
    FOR EACH ROW WHEN (NEW.modalidad = 'RETIRO_PUNTO_ENTREGA')
    EXECUTE FUNCTION lamontana.rechazar_punto_movil_demo();
CREATE TRIGGER demo_punto_reprogramacion BEFORE INSERT ON lamontana.propuesta_reprogramacion
    FOR EACH ROW WHEN (NEW.franja_nueva->>'modalidad' = 'RETIRO_PUNTO_ENTREGA')
    EXECUTE FUNCTION lamontana.rechazar_punto_movil_demo();
