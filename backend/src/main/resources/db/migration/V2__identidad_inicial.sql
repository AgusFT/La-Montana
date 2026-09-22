CREATE TABLE lamontana.rol (
    id_rol bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(80) NOT NULL,
    es_interno boolean NOT NULL,
    activo boolean NOT NULL DEFAULT true
);
INSERT INTO lamontana.rol (codigo, nombre, es_interno) VALUES ('ADMIN_ADMIN', 'Administrador', true);

CREATE TABLE lamontana.usuario (
    id_usuario bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_publico uuid NOT NULL UNIQUE,
    id_rol bigint NOT NULL REFERENCES lamontana.rol,
    correo varchar(254) NOT NULL,
    hash_contrasena varchar(255) NOT NULL,
    nombre varchar(100) NOT NULL,
    apellido varchar(100) NOT NULL,
    estado varchar(28) NOT NULL CHECK (estado IN ('ACTIVO', 'DESACTIVADO')),
    correo_verificado_en timestamptz,
    es_administrador_propietario boolean NOT NULL DEFAULT false,
    fecha_alta timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX usuario_correo_unico ON lamontana.usuario (lower(correo));
CREATE UNIQUE INDEX usuario_propietario_unico ON lamontana.usuario (es_administrador_propietario)
    WHERE es_administrador_propietario AND estado = 'ACTIVO';

-- Este registro técnico mantiene cerrado el alta aunque luego se desactive al propietario.
CREATE TABLE lamontana.inicializacion_sistema (
    unica boolean PRIMARY KEY CHECK (unica),
    completada_en timestamptz,
    id_usuario_propietario bigint REFERENCES lamontana.usuario,
    CHECK ((completada_en IS NULL) = (id_usuario_propietario IS NULL))
);
INSERT INTO lamontana.inicializacion_sistema (unica) VALUES (true);
CREATE TABLE lamontana.evento_acceso (
    id_evento bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo varchar(40) NOT NULL,
    id_usuario bigint REFERENCES lamontana.usuario,
    fecha timestamptz NOT NULL DEFAULT now()
);

-- Esquema técnico de Spring Session JDBC, inicializado exclusivamente por Flyway.
CREATE TABLE lamontana.sesion_http (
    primary_id char(36) PRIMARY KEY,
    session_id char(36) NOT NULL,
    creation_time bigint NOT NULL,
    last_access_time bigint NOT NULL,
    max_inactive_interval integer NOT NULL,
    expiry_time bigint NOT NULL,
    principal_name varchar(254)
);
CREATE UNIQUE INDEX sesion_http_id ON lamontana.sesion_http (session_id);
CREATE INDEX sesion_http_expira ON lamontana.sesion_http (expiry_time);
CREATE INDEX sesion_http_principal ON lamontana.sesion_http (principal_name);
CREATE TABLE lamontana.sesion_http_attributes (
    session_primary_id char(36) NOT NULL REFERENCES lamontana.sesion_http (primary_id) ON DELETE CASCADE,
    attribute_name varchar(200) NOT NULL,
    attribute_bytes bytea NOT NULL,
    PRIMARY KEY (session_primary_id, attribute_name)
);
