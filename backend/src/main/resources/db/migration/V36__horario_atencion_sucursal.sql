-- Horario habitual ingresado al crear/editar la sucursal. Las versiones operativas
-- conservan su calendario inmutable hasta revisar y activar otro borrador.
CREATE TABLE lamontana.sucursal_horario_atencion (
    id_sucursal bigint NOT NULL REFERENCES lamontana.sucursal,
    dia_semana smallint NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    habilitado boolean NOT NULL,
    hora_desde time,
    hora_hasta time,
    PRIMARY KEY (id_sucursal, dia_semana),
    CHECK ((habilitado AND hora_desde IS NOT NULL AND hora_hasta IS NOT NULL AND hora_desde < hora_hasta)
        OR (NOT habilitado AND hora_desde IS NULL AND hora_hasta IS NULL))
);

-- Sólo recuperar calendarios activos completos. No inventar horarios para altas
-- anteriores ni convertir un borrador pendiente en una decisión publicada.
INSERT INTO lamontana.sucursal_horario_atencion
    (id_sucursal,dia_semana,habilitado,hora_desde,hora_hasta)
SELECT h.id_sucursal,h.dia_semana,h.habilitado,h.hora_desde,h.hora_hasta
FROM lamontana.horario_sucursal h
JOIN lamontana.configuracion_version c USING(id_configuracion_version)
WHERE c.estado='ACTIVA' AND h.id_sucursal IN (
    SELECT v.id_sucursal FROM lamontana.horario_sucursal v
    WHERE v.id_configuracion_version=h.id_configuracion_version
    GROUP BY v.id_sucursal
    HAVING count(*)=7 AND count(v.habilitado)=7 AND bool_or(v.habilitado)
       AND bool_and(NOT v.habilitado OR (v.hora_desde IS NOT NULL AND v.hora_hasta IS NOT NULL AND v.hora_desde<v.hora_hasta))
);
