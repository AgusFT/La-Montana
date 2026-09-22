INSERT INTO lamontana.rol (codigo,nombre,es_interno) VALUES ('EMPLEADO','Empleado',true);
ALTER TABLE lamontana.usuario ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE lamontana.usuario ADD COLUMN fecha_desactivacion timestamptz;
ALTER TABLE lamontana.sucursal ADD COLUMN version bigint NOT NULL DEFAULT 0;
CREATE TABLE lamontana.permiso (
    id_permiso bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo varchar(80) NOT NULL UNIQUE,
    nombre varchar(120) NOT NULL,
    descripcion text NOT NULL,
    alcance varchar(24) NOT NULL CHECK (alcance IN ('GLOBAL','SUCURSAL','PROPIO')),
    activo boolean NOT NULL DEFAULT true
);
-- Constantes técnicas; ningún empleado obtiene permisos financieros por defecto.
INSERT INTO lamontana.permiso (codigo,nombre,descripcion,alcance) VALUES
 ('REGISTRAR_COBRO','Registrar cobros','Registrar dinero recibido según el alcance autorizado.','GLOBAL'),
 ('ACREDITAR_PAGO','Acreditar pagos','Confirmar recepción efectiva de dinero.','GLOBAL'),
 ('REGISTRAR_DEVOLUCION','Registrar devoluciones','Registrar devolución de dinero recibido.','GLOBAL');
CREATE TABLE lamontana.usuario_permiso (
    id_usuario_permiso bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario bigint NOT NULL REFERENCES lamontana.usuario,
    id_permiso bigint NOT NULL REFERENCES lamontana.permiso,
    id_usuario_otorgante bigint NOT NULL REFERENCES lamontana.usuario,
    fecha_otorgamiento timestamptz NOT NULL DEFAULT now(),
    fecha_revocacion timestamptz,
    CHECK (fecha_revocacion IS NULL OR fecha_revocacion >= fecha_otorgamiento)
);
CREATE UNIQUE INDEX usuario_permiso_vigente ON lamontana.usuario_permiso (id_usuario,id_permiso) WHERE fecha_revocacion IS NULL;
CREATE TABLE lamontana.usuario_sucursal (
    id_usuario_sucursal bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario bigint NOT NULL REFERENCES lamontana.usuario,
    id_sucursal bigint NOT NULL REFERENCES lamontana.sucursal,
    fecha_desde timestamptz NOT NULL DEFAULT now(),
    fecha_hasta timestamptz,
    activa boolean NOT NULL DEFAULT true,
    CHECK (activa = (fecha_hasta IS NULL)),
    CHECK (fecha_hasta IS NULL OR fecha_hasta >= fecha_desde)
);
CREATE UNIQUE INDEX usuario_sucursal_vigente ON lamontana.usuario_sucursal (id_usuario,id_sucursal) WHERE activa;
CREATE INDEX usuario_sucursal_sucursal ON lamontana.usuario_sucursal (id_sucursal) WHERE activa;
CREATE TABLE lamontana.evento_organizacion (
    id_evento bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario_actor bigint NOT NULL REFERENCES lamontana.usuario,
    tipo varchar(40) NOT NULL,
    codigo_publico_objeto uuid NOT NULL,
    version bigint NOT NULL,
    fecha timestamptz NOT NULL DEFAULT now()
);
