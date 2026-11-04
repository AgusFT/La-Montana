INSERT INTO lamontana.permiso(codigo,nombre,descripcion,alcance)
VALUES ('CREAR_PEDIDOS','Crear pedidos para clientes','Preparar y confirmar pedidos de clientes registrados desde la operación interna.','SUCURSAL');

ALTER TABLE lamontana.preparacion_cliente
 ADD COLUMN id_operador bigint REFERENCES lamontana.usuario,
 ADD CHECK(id_operador IS NULL OR id_operador<>id_usuario);
DROP INDEX lamontana.una_preparacion_cliente;
CREATE UNIQUE INDEX una_preparacion_cliente ON lamontana.preparacion_cliente(id_usuario)
 WHERE estado='ABIERTA' AND id_operador IS NULL;
CREATE UNIQUE INDEX una_preparacion_por_operador_cliente ON lamontana.preparacion_cliente(id_operador,id_usuario)
 WHERE estado='ABIERTA' AND id_operador IS NOT NULL;

ALTER TABLE lamontana.cotizacion ADD COLUMN id_preparacion_interna bigint REFERENCES lamontana.preparacion_cliente;
