-- Las suites del producto completo preparan el esquema anterior en una base aislada.
CREATE OR REPLACE VIEW lamontana.modalidades_entrega_demo (modalidad) AS
    VALUES ('RETIRO_SUCURSAL'::text), ('ENVIO_DOMICILIO'::text), ('RETIRO_PUNTO_ENTREGA'::text);
