ALTER TABLE lamontana.usuario ADD COLUMN es_soporte boolean NOT NULL DEFAULT false;
CREATE UNIQUE INDEX usuario_soporte_unico ON lamontana.usuario(es_soporte) WHERE es_soporte;
ALTER TABLE lamontana.usuario ADD CONSTRAINT soporte_no_propietario CHECK (NOT(es_soporte AND es_administrador_propietario));
CREATE TABLE lamontana.autorizacion_soporte (
    id_autorizacion_soporte bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario_soporte bigint NOT NULL REFERENCES lamontana.usuario,
    id_usuario_habilitante bigint NOT NULL REFERENCES lamontana.usuario,
    motivo text NOT NULL CHECK (length(trim(motivo)) BETWEEN 1 AND 2000),
    estado varchar(20) NOT NULL CHECK (estado IN ('VIGENTE','VENCIDA','REVOCADA')),
    fecha_inicio timestamptz NOT NULL,
    fecha_fin timestamptz NOT NULL,
    id_usuario_revocante bigint REFERENCES lamontana.usuario,
    fecha_revocacion timestamptz,
    CHECK (fecha_fin>fecha_inicio AND fecha_fin<=fecha_inicio+interval '24 hours'),
    CHECK (id_usuario_soporte<>id_usuario_habilitante)
);
CREATE INDEX soporte_ventana ON lamontana.autorizacion_soporte(id_usuario_soporte,fecha_fin) WHERE estado='VIGENTE';
CREATE FUNCTION lamontana.soporte_habilitado(usuario_id bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
    SELECT EXISTS(SELECT 1 FROM lamontana.autorizacion_soporte
       WHERE id_usuario_soporte=usuario_id AND estado='VIGENTE' AND fecha_revocacion IS NULL
       AND fecha_inicio<=statement_timestamp() AND fecha_fin>statement_timestamp());
$$;
CREATE TABLE lamontana.evento_soporte (
    id_evento_soporte bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario bigint NOT NULL REFERENCES lamontana.usuario,
    tipo varchar(40) NOT NULL,
    recurso varchar(300),
    resultado integer,
    fecha timestamptz NOT NULL DEFAULT clock_timestamp()
);
