ALTER TABLE lamontana.catalogo_revision
    ADD COLUMN estado varchar(16),
    ADD COLUMN programada_para timestamptz,
    ADD COLUMN activada_en timestamptz,
    ADD COLUMN cancelada_en timestamptz;

-- V7 sólo publicaba inmediatamente; conserva íntegros contenido, identidad y comprobante.
UPDATE lamontana.catalogo_revision
SET estado=CASE WHEN vigente THEN 'VIGENTE' ELSE 'HISTORICA' END,
    activada_en=creada_en;

ALTER TABLE lamontana.catalogo_revision
    ALTER COLUMN estado SET NOT NULL,
    ADD CONSTRAINT catalogo_estado_valido CHECK(estado IN ('VIGENTE','HISTORICA','PROGRAMADA','CANCELADA')),
    ADD CONSTRAINT catalogo_estado_vigente CHECK(vigente=(estado='VIGENTE')),
    ADD CONSTRAINT catalogo_programacion_futura CHECK(programada_para IS NULL OR programada_para>creada_en),
    ADD CONSTRAINT catalogo_fechas_estado CHECK(
        (estado IN ('VIGENTE','HISTORICA') AND activada_en IS NOT NULL AND cancelada_en IS NULL)
        OR (estado='PROGRAMADA' AND programada_para IS NOT NULL AND activada_en IS NULL AND cancelada_en IS NULL)
        OR (estado='CANCELADA' AND programada_para IS NOT NULL AND activada_en IS NULL AND cancelada_en IS NOT NULL)
    );
CREATE UNIQUE INDEX catalogo_una_programada ON lamontana.catalogo_revision(estado) WHERE estado='PROGRAMADA';

CREATE TABLE lamontana.catalogo_cancelacion (
    id_operacion uuid PRIMARY KEY,
    id_catalogo_revision bigint NOT NULL UNIQUE REFERENCES lamontana.catalogo_revision,
    hash_solicitud varchar(64) NOT NULL,
    id_actor bigint NOT NULL REFERENCES lamontana.usuario,
    motivo varchar(500) NOT NULL,
    fecha timestamptz NOT NULL DEFAULT clock_timestamp()
);
