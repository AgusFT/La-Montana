-- El pedido guiado y los borradores de la V53 siguen perteneciendo al flujo completo.
CREATE TABLE lamontana.tutorial_recorrido (
    recorrido text PRIMARY KEY CHECK (recorrido IN ('administrador', 'cliente')),
    leccion text NOT NULL DEFAULT '',
    tarea integer NOT NULL DEFAULT 0 CHECK (tarea >= 0),
    alcanzada integer NOT NULL DEFAULT 0 CHECK (alcanzada >= 0),
    completo boolean NOT NULL DEFAULT false,
    borrador jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(borrador) = 'object')
);
INSERT INTO lamontana.tutorial_recorrido (recorrido) VALUES ('administrador'), ('cliente');
