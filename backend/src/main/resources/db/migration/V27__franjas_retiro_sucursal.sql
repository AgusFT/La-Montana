-- Sin backfill: una sucursal histórica no recibe capacidad comercial inventada.
ALTER TABLE lamontana.franja_entrega
 ADD COLUMN id_configuracion_retiro bigint,
 ADD COLUMN id_sucursal_retiro bigint,
 ADD CONSTRAINT franja_retiro_horario FOREIGN KEY(id_configuracion_retiro,id_sucursal_retiro)
   REFERENCES lamontana.configuracion_horario_sucursal(id_configuracion_version,id_sucursal) ON DELETE CASCADE,
 ADD CONSTRAINT franja_retiro_completo CHECK((id_configuracion_retiro IS NULL)=(id_sucursal_retiro IS NULL)),
 DROP CONSTRAINT franja_destino_exclusivo,
 ADD CONSTRAINT franja_destino_exclusivo CHECK(num_nonnulls(id_configuracion_punto_entrega,id_configuracion_zona_entrega,id_configuracion_retiro)=1),
 ADD CONSTRAINT franja_retiro_unica UNIQUE(id_configuracion_retiro,id_sucursal_retiro,dia_semana,hora_desde,hora_hasta);
