-- V34 aún no ofrecía importación: las primeras copias nacen con sus huellas verificables.
ALTER TABLE lamontana.web_imagen ADD COLUMN sha256_publico char(64) NOT NULL;
ALTER TABLE lamontana.web_imagen ADD COLUMN sha256_miniatura char(64) NOT NULL;
