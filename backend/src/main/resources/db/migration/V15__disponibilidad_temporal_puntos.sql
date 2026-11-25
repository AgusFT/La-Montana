-- Disponibilidad cotidiana por identidad: no pertenece al snapshot de configuración.
-- La ausencia de fila significa Sin definir; no se habilita ningún punto por defecto.
CREATE TABLE lamontana.disponibilidad_punto_entrega (
    id_punto_entrega bigint PRIMARY KEY REFERENCES lamontana.punto_entrega,
    estado varchar(20) NOT NULL CHECK(estado IN ('HABILITADO','DESHABILITADO')),
    version bigint NOT NULL CHECK(version>0),
    id_actor bigint NOT NULL REFERENCES lamontana.usuario,
    fecha_actualizacion timestamptz NOT NULL
);
CREATE TABLE lamontana.evento_disponibilidad_punto (
    id_operacion uuid PRIMARY KEY,
    id_punto_entrega bigint NOT NULL REFERENCES lamontana.punto_entrega,
    id_actor bigint NOT NULL REFERENCES lamontana.usuario,
    hash_solicitud char(64) NOT NULL,
    estado_anterior varchar(20) CHECK(estado_anterior IN ('HABILITADO','DESHABILITADO')),
    estado_nuevo varchar(20) NOT NULL CHECK(estado_nuevo IN ('HABILITADO','DESHABILITADO')),
    version_anterior bigint NOT NULL CHECK(version_anterior>=0),
    version_nueva bigint NOT NULL CHECK(version_nueva=version_anterior+1),
    fecha timestamptz NOT NULL DEFAULT clock_timestamp(),
    UNIQUE(id_punto_entrega,version_nueva)
);
