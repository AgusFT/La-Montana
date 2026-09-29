-- El estado del maestro afecta nuevas configuraciones; las revisiones guardadas
-- conservan sus referencias y no se modifican retroactivamente.
ALTER TABLE lamontana.servicio ADD COLUMN activo boolean NOT NULL DEFAULT true;
