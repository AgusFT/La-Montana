CREATE TABLE lamontana.formato (
    id_formato bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_publico uuid NOT NULL UNIQUE,
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(100) NOT NULL,
    ancho_mm numeric(8,2) NOT NULL CHECK(ancho_mm>0),
    alto_mm numeric(8,2) NOT NULL CHECK(alto_mm>0)
);
CREATE TABLE lamontana.papel (
    id_papel bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_publico uuid NOT NULL UNIQUE,
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(120) NOT NULL,
    gramaje_g_m2 numeric(8,2) NOT NULL CHECK(gramaje_g_m2>0),
    terminacion_tipo varchar(100) NOT NULL
);
CREATE TABLE lamontana.servicio (
    id_servicio bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_publico uuid NOT NULL UNIQUE,
    codigo varchar(50) NOT NULL UNIQUE,
    nombre varchar(140) NOT NULL,
    tipo varchar(20) NOT NULL CHECK(tipo IN ('IMPRESION','TERMINACION')),
    descripcion varchar(1000)
);
CREATE TABLE lamontana.catalogo_revision (
    id_catalogo_revision bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_publico uuid NOT NULL UNIQUE,
    id_revision_base bigint REFERENCES lamontana.catalogo_revision,
    id_operacion uuid NOT NULL UNIQUE,
    hash_solicitud varchar(64) NOT NULL,
    motivo varchar(500) NOT NULL,
    id_actor bigint NOT NULL REFERENCES lamontana.usuario,
    creada_en timestamptz NOT NULL DEFAULT now(),
    vigente boolean NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX catalogo_revision_vigente ON lamontana.catalogo_revision(vigente) WHERE vigente;
CREATE TABLE lamontana.tarifa_impresion (
    id_tarifa_impresion bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_catalogo_revision bigint NOT NULL REFERENCES lamontana.catalogo_revision,
    id_formato bigint NOT NULL REFERENCES lamontana.formato,
    id_papel bigint NOT NULL REFERENCES lamontana.papel,
    modo_color varchar(16) NOT NULL CHECK(modo_color IN ('BLANCO_NEGRO','COLOR')),
    precio_por_carilla numeric(19,2) NOT NULL CHECK(precio_por_carilla>=0),
    recargo_doble_faz numeric(19,2) NOT NULL CHECK(recargo_doble_faz>=0),
    habilitada boolean NOT NULL,
    UNIQUE(id_catalogo_revision,id_formato,id_papel,modo_color)
);
CREATE TABLE lamontana.configuracion_servicio (
    id_configuracion_servicio bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_catalogo_revision bigint NOT NULL REFERENCES lamontana.catalogo_revision,
    id_servicio bigint NOT NULL REFERENCES lamontana.servicio,
    nombre_visible varchar(140) NOT NULL,
    base_precio varchar(24) NOT NULL CHECK(base_precio IN ('POR_COPIA','POR_HOJA','POR_CARILLA','FIJO_POR_ITEM')),
    precio_unitario numeric(19,2) NOT NULL CHECK(precio_unitario>=0),
    preparacion_minutos integer NOT NULL CHECK(preparacion_minutos BETWEEN 0 AND 10080),
    habilitado boolean NOT NULL,
    UNIQUE(id_catalogo_revision,id_servicio)
);
CREATE TABLE lamontana.compatibilidad_servicio (
    id_configuracion_servicio bigint NOT NULL REFERENCES lamontana.configuracion_servicio,
    id_formato bigint NOT NULL REFERENCES lamontana.formato,
    id_papel bigint NOT NULL REFERENCES lamontana.papel,
    PRIMARY KEY(id_configuracion_servicio,id_formato,id_papel)
);
CREATE TABLE lamontana.evento_catalogo (
    id_evento_catalogo bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_actor bigint NOT NULL REFERENCES lamontana.usuario,
    tipo varchar(32) NOT NULL,
    codigo_objeto uuid NOT NULL,
    fecha timestamptz NOT NULL DEFAULT now()
);
