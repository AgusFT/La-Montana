-- Datos comerciales de prueba compatibles desde V43. Las claves foráneas y los triggers diferidos siguen activos.
SET LOCAL search_path TO lamontana, public;
DO $$
DECLARE
    actor bigint; suc bigint; config bigint; catalogo bigint; cot bigint; pedido_id bigint; evento bigint;
    suc_publica uuid := gen_random_uuid();
BEGIN
    INSERT INTO usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado)
    SELECT gen_random_uuid(),id_rol,'admin@example.test','hash-prueba','Admin','Test','ACTIVO' FROM rol WHERE codigo='ADMIN_ADMIN'
    RETURNING id_usuario INTO actor;
    INSERT INTO sucursal(codigo_publico,codigo,nombre,calle,numero,localidad,provincia,codigo_postal,zona_horaria,estado,id_usuario_alta)
    VALUES (suc_publica,'CENTRO','Centro','Calle','12','CABA','CABA','1000','UTC','ACTIVA',actor) RETURNING id_sucursal INTO suc;
    INSERT INTO impresora(codigo_publico,id_sucursal,id_usuario_creador) VALUES (gen_random_uuid(),suc,actor);
    INSERT INTO configuracion_version(codigo_publico,numero_version,id_usuario_creador,estado)
    VALUES (gen_random_uuid(),1,actor,'EN_PREPARACION') RETURNING id_configuracion_version INTO config;
    INSERT INTO configuracion_horario_sucursal VALUES (config,suc,'UTC');
    INSERT INTO franja_entrega(id_configuracion_retiro,id_sucursal_retiro,dia_semana,hora_desde,hora_hasta,capacidad_pedidos,habilitada)
    VALUES (config,suc,1,'09:00','10:00',2,true);
    INSERT INTO catalogo_revision(codigo_publico,id_operacion,hash_solicitud,motivo,id_actor,estado,vigente,activada_en)
    VALUES (gen_random_uuid(),gen_random_uuid(),repeat('a',64),'Prueba de actualización',actor,'VIGENTE',true,now())
    RETURNING id_catalogo_revision INTO catalogo;
    INSERT INTO cotizacion(codigo_publico,id_usuario_creador,id_sucursal,id_configuracion_version,id_configuracion_confirmacion,
                          id_catalogo_revision,estado,generada_en,aceptada_en,confirmada_en,vigente_hasta,subtotal,costo_entrega,total,oferta)
    VALUES (gen_random_uuid(),actor,suc,config,config,catalogo,'CONFIRMADA',now(),now(),now(),now()+interval '1 day',
            125,0,125,'{"modalidad":"RETIRO_SUCURSAL"}') RETURNING id_cotizacion INTO cot;
    INSERT INTO evento_financiero(id_operacion,id_cotizacion,id_actor,actor_nombre,actor_rol,tipo,huella,motivo)
    VALUES (gen_random_uuid(),cot,actor,'Admin','ADMIN_ADMIN','RECIBIR',repeat('a',64),'Cobro de prueba') RETURNING id_evento INTO evento;
    INSERT INTO pago(codigo_publico,id_cotizacion,id_evento,medio,importe,referencia,clave_referencia,recibido_en)
    VALUES (gen_random_uuid(),cot,evento,'EFECTIVO',125,'recibo-prueba','recibo-prueba',now());
    INSERT INTO pedido(codigo_publico,id_cotizacion,id_usuario_creador,id_sucursal,id_configuracion_version,estado,modo_aprobacion,confirmada_en,total,condiciones_finales)
    VALUES (gen_random_uuid(),cot,actor,suc,config,'PENDIENTE_REVISION','HUMANA',now(),125,'{}') RETURNING id_pedido INTO pedido_id;
    INSERT INTO historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha)
    VALUES (pedido_id,actor,gen_random_uuid(),repeat('a',64),'PENDIENTE_REVISION','Confirmación',now());
    INSERT INTO reserva_entrega(id_pedido,modalidad,destino_publico,origen_publico,nombre_destino,zona_horaria,fecha_local,
                              apertura,cierre,franja_desde,franja_hasta,cupo_confirmado,costo)
    VALUES (pedido_id,'RETIRO_SUCURSAL',suc_publica,suc_publica,'Centro','UTC','2026-11-30','09:00','10:00',
            '2026-11-30 09:00Z','2026-11-30 10:00Z',2,0);
END $$;
