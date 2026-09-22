CREATE TABLE lamontana.sucursal (
    id_sucursal bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario_alta bigint NOT NULL REFERENCES lamontana.usuario,
    codigo_publico uuid NOT NULL UNIQUE,
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(140) NOT NULL,
    calle varchar(160) NOT NULL,
    numero varchar(20) NOT NULL,
    localidad varchar(120) NOT NULL,
    provincia varchar(120) NOT NULL,
    codigo_postal varchar(12) NOT NULL,
    correo varchar(254),
    telefono varchar(40),
    zona_horaria varchar(64) NOT NULL,
    estado varchar(20) NOT NULL CHECK (estado IN ('ACTIVA','DESACTIVADA')),
    fecha_alta timestamptz NOT NULL DEFAULT now(),
    fecha_desactivacion timestamptz
);
