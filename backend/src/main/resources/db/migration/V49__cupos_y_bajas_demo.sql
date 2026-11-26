CREATE TABLE lamontana.demo_cupo (
    recurso text PRIMARY KEY,
    limite integer NOT NULL CHECK (limite > 0),
    ocupados bigint NOT NULL DEFAULT 0 CHECK (ocupados >= 0)
);

INSERT INTO lamontana.demo_cupo (recurso, limite) VALUES
    ('PEDIDOS', 20), ('CLIENTES', 10), ('CAJA', 30), ('IMPRESORAS', 2);

CREATE TABLE lamontana.demo_cupo_registro (
    tipo text NOT NULL CHECK (tipo IN ('PEDIDO','CLIENTE','COBRO','DEVOLUCION','IMPRESORA')),
    codigo_publico uuid NOT NULL,
    recurso text GENERATED ALWAYS AS (
        CASE tipo WHEN 'PEDIDO' THEN 'PEDIDOS' WHEN 'CLIENTE' THEN 'CLIENTES'
                  WHEN 'IMPRESORA' THEN 'IMPRESORAS' ELSE 'CAJA' END
    ) STORED REFERENCES lamontana.demo_cupo(recurso),
    baja_en timestamptz,
    id_actor_baja bigint REFERENCES lamontana.usuario(id_usuario),
    PRIMARY KEY (tipo, codigo_publico),
    CHECK ((baja_en IS NULL) = (id_actor_baja IS NULL))
);

-- El alta comercial reserva su operación y crea la ficha en la misma transacción.
CREATE TABLE lamontana.demo_alta_cliente (
    operacion uuid PRIMARY KEY,
    id_actor bigint NOT NULL REFERENCES lamontana.usuario(id_usuario),
    codigo_cliente uuid NOT NULL UNIQUE REFERENCES lamontana.usuario(codigo_publico) DEFERRABLE INITIALLY DEFERRED,
    nombre varchar(100) NOT NULL,
    apellido varchar(100) NOT NULL
);

-- La lectura de V48 y la instalación de los disparadores comparten la transacción de Flyway.
LOCK TABLE lamontana.usuario, lamontana.pedido, lamontana.pago, lamontana.reembolso,
           lamontana.impresora IN SHARE ROW EXCLUSIVE MODE;

INSERT INTO lamontana.demo_cupo_registro (tipo, codigo_publico)
    SELECT 'PEDIDO', codigo_publico FROM lamontana.pedido
    UNION ALL SELECT 'CLIENTE', u.codigo_publico FROM lamontana.usuario u
        JOIN lamontana.rol r USING (id_rol) WHERE r.codigo = 'CLIENTE'
    UNION ALL SELECT 'COBRO', codigo_publico FROM lamontana.pago
    UNION ALL SELECT 'DEVOLUCION', codigo_publico FROM lamontana.reembolso
    UNION ALL SELECT 'IMPRESORA', codigo_publico FROM lamontana.impresora;

-- Si una instalación ya excedía un límite, conserva todos sus registros.
UPDATE lamontana.demo_cupo c SET ocupados =
    (SELECT count(*) FROM lamontana.demo_cupo_registro r WHERE r.recurso = c.recurso);

CREATE FUNCTION lamontana.registrar_alta_cupo_demo() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    categoria text;
BEGIN
    IF TG_ARGV[0] = 'CLIENTE' THEN
        IF NOT EXISTS (SELECT FROM lamontana.rol WHERE id_rol = NEW.id_rol AND codigo = 'CLIENTE') THEN
            RETURN NULL;
        END IF;
    END IF;

    INSERT INTO lamontana.demo_cupo_registro (tipo, codigo_publico)
        VALUES (TG_ARGV[0], NEW.codigo_publico) RETURNING recurso INTO categoria;

    -- Las dos altas que compiten actualizan la misma fila. PostgreSQL vuelve a
    -- evaluar la condición al obtenerla y revierte también el alta si se agotó.
    UPDATE lamontana.demo_cupo SET ocupados = ocupados + 1
        WHERE recurso = categoria AND ocupados < limite;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Se alcanzó el cupo de la Demo para %.', lower(categoria)
            USING ERRCODE = CASE categoria WHEN 'PEDIDOS' THEN '23D01' WHEN 'CLIENTES' THEN '23D02'
                                           WHEN 'CAJA' THEN '23D03' WHEN 'IMPRESORAS' THEN '23D04' END,
                  CONSTRAINT = 'demo_cupo_' || lower(categoria);
    END IF;
    RETURN NULL;
END $$;

-- El bloqueo compartido ordena un inicio concurrente con la baja y deja que un
-- trabajo iniciado se complete, se controle y conserve toda su trazabilidad.
CREATE FUNCTION lamontana.exigir_trabajo_demo_activo() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    registro record;
BEGIN
    FOR registro IN
        SELECT tipo, baja_en FROM lamontana.demo_cupo_registro
        WHERE (tipo, codigo_publico) IN (
            ('PEDIDO', (SELECT codigo_publico FROM lamontana.pedido WHERE id_pedido = NEW.id_pedido)),
            ('IMPRESORA', (SELECT codigo_publico FROM lamontana.impresora WHERE id_impresora = NEW.id_impresora))
        ) ORDER BY tipo FOR SHARE
    LOOP
        IF registro.baja_en IS NOT NULL THEN
            RAISE EXCEPTION 'El recurso de Demo % está dado de baja.', registro.tipo
                USING ERRCODE = CASE registro.tipo WHEN 'PEDIDO' THEN '23D11' ELSE '23D14' END;
        END IF;
    END LOOP;
    RETURN NEW;
END $$;

CREATE TRIGGER demo_validar_inicio BEFORE INSERT ON lamontana.trabajo_impresion
    FOR EACH ROW EXECUTE FUNCTION lamontana.exigir_trabajo_demo_activo();

CREATE TRIGGER demo_cupo_alta AFTER INSERT ON lamontana.pedido
    FOR EACH ROW EXECUTE FUNCTION lamontana.registrar_alta_cupo_demo('PEDIDO');
CREATE TRIGGER demo_cupo_alta AFTER INSERT ON lamontana.usuario
    FOR EACH ROW EXECUTE FUNCTION lamontana.registrar_alta_cupo_demo('CLIENTE');
CREATE TRIGGER demo_cupo_alta AFTER INSERT ON lamontana.pago
    FOR EACH ROW EXECUTE FUNCTION lamontana.registrar_alta_cupo_demo('COBRO');
CREATE TRIGGER demo_cupo_alta AFTER INSERT ON lamontana.reembolso
    FOR EACH ROW EXECUTE FUNCTION lamontana.registrar_alta_cupo_demo('DEVOLUCION');
CREATE TRIGGER demo_cupo_alta AFTER INSERT ON lamontana.impresora
    FOR EACH ROW EXECUTE FUNCTION lamontana.registrar_alta_cupo_demo('IMPRESORA');

CREATE FUNCTION lamontana.dar_baja_cupo_demo(p_tipo text, p_codigo uuid, p_actor bigint)
RETURNS boolean LANGUAGE plpgsql AS $$
DECLARE
    categoria text;
BEGIN
    UPDATE lamontana.demo_cupo_registro SET baja_en = clock_timestamp(), id_actor_baja = p_actor
        WHERE tipo = p_tipo AND codigo_publico = p_codigo AND baja_en IS NULL
        RETURNING recurso INTO categoria;
    IF FOUND THEN
        UPDATE lamontana.demo_cupo SET ocupados = ocupados - 1 WHERE recurso = categoria;
        RETURN true;
    END IF;

    -- Distingue una baja repetida de un identificador desconocido.
    RETURN (SELECT false FROM lamontana.demo_cupo_registro WHERE tipo = p_tipo AND codigo_publico = p_codigo);
END $$;
