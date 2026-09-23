-- El sitio público es independiente de configuración operativa, precios y PDF privados.
CREATE TABLE lamontana.web_imagen (
 codigo uuid PRIMARY KEY,
 sha256 char(64) NOT NULL UNIQUE,
 nombre varchar(255) NOT NULL,
 tipo_origen varchar(30) NOT NULL CHECK(tipo_origen IN ('image/jpeg','image/png','image/webp')),
 bytes_origen bigint NOT NULL CHECK(bytes_origen BETWEEN 1 AND 10485760),
 ancho integer NOT NULL CHECK(ancho>0),alto integer NOT NULL CHECK(alto>0),
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 creada_en timestamptz NOT NULL DEFAULT clock_timestamp(),
 CHECK(ancho::bigint*alto<=25000000)
);
CREATE TABLE lamontana.web_publicacion (
 codigo uuid PRIMARY KEY,numero bigint NOT NULL UNIQUE CHECK(numero>0),
 version_borrador bigint NOT NULL UNIQUE CHECK(version_borrador>0),
 contenido jsonb NOT NULL CHECK(jsonb_typeof(contenido)='object'),
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,autor varchar(300) NOT NULL,
 publicada_en timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE lamontana.web_publicacion_imagen (
 publicacion uuid NOT NULL REFERENCES lamontana.web_publicacion,
 imagen uuid NOT NULL REFERENCES lamontana.web_imagen,
 PRIMARY KEY(publicacion,imagen)
);
CREATE TABLE lamontana.web_borrador (
 unica boolean PRIMARY KEY DEFAULT true CHECK(unica),version bigint NOT NULL DEFAULT 1 CHECK(version>0),
 contenido jsonb NOT NULL DEFAULT '{"nombre":"","titulo":"","descripcion":"","logo":null,"logoAlt":"","portada":null,"portadaAlt":"","correo":"","telefono":"","direccion":"","secciones":["PORTADA","CATALOGO","CONTACTO"],"fichas":[]}',
 carpeta varchar(500) NOT NULL DEFAULT '',
 actualizada_en timestamptz NOT NULL DEFAULT clock_timestamp(),
 id_actor bigint REFERENCES lamontana.usuario,
 publicacion uuid REFERENCES lamontana.web_publicacion,
 CHECK(jsonb_typeof(contenido)='object')
);
INSERT INTO lamontana.web_borrador(unica) VALUES(true);
CREATE TABLE lamontana.web_operacion (
 codigo uuid PRIMARY KEY,tipo varchar(30) NOT NULL CHECK(tipo IN ('GUARDAR','PUBLICAR','IMPORTAR')),
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 huella char(64) NOT NULL,respuesta jsonb NOT NULL,
 fecha timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TRIGGER web_publicacion_inmutable BEFORE UPDATE OR DELETE ON lamontana.web_publicacion FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TRIGGER web_publicacion_imagen_inmutable BEFORE UPDATE OR DELETE ON lamontana.web_publicacion_imagen FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TRIGGER web_operacion_inmutable BEFORE UPDATE OR DELETE ON lamontana.web_operacion FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
