-- La marca del equipo conserva el plazo cuando se reemplaza la base.
-- Esta constancia distingue la actualización desde V47–V50 de una marca en disco perdida.
ALTER TABLE lamontana.entorno_instalacion ADD COLUMN vigencia_inicio timestamptz;
