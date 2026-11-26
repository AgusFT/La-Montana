-- Precarga de una imprenta ficticia. Flyway aplica todos los datos en una sola transacción.
SET LOCAL search_path TO lamontana, public;

DO $$
DECLARE
    admin_publico constant uuid := '4d9fdf39-1f90-4d08-b7b6-0c77fc9f0071';
    empleado_publico constant uuid := '4d9fdf39-1f90-4d08-b7b6-0c77fc9f0072';
    cliente_publico constant uuid := '4d9fdf39-1f90-4d08-b7b6-0c77fc9f0073';
    sucursal_publica constant uuid := '4d9fdf39-1f90-4d08-b7b6-0c77fc9f0074';
    config_publica constant uuid := '4d9fdf39-1f90-4d08-b7b6-0c77fc9f0075';
    catalogo_publico constant uuid := '4d9fdf39-1f90-4d08-b7b6-0c77fc9f0076';
    impresora_publica constant uuid := '4d9fdf39-1f90-4d08-b7b6-0c77fc9f0077';
    pedido_publico constant uuid := '4d9fdf39-1f90-4d08-b7b6-0c77fc9f0078';
    pdf_publico constant uuid := '4d9fdf39-1f90-4d08-b7b6-0c77fc9f0081';
    pdf_nombre constant text := 'afiche-senderos.pdf';
    pdf_bytes constant bigint := 2463;
    pdf_sha constant text := 'fc454af89bab468b8529e0a3348fdb2f28c0ccf0da8d26f41a7c266b2aff3f37';
    sede constant text := 'Imprenta Senderos · Centro';
    direccion constant text := 'Pasaje de las Tintas 120, Ciudad de Buenos Aires, CABA';
    zona constant text := 'America/Argentina/Buenos_Aires';
    fecha_retiro date := (now() AT TIME ZONE zona)::date + 1;
    apertura_atencion constant time := '09:00';
    apertura constant time := '14:00';
    cierre constant time := '18:00';
    cupo_retiro constant integer := 5;
    desde timestamptz := (fecha_retiro + apertura) AT TIME ZONE zona;
    hasta timestamptz := (fecha_retiro + cierre) AT TIME ZONE zona;
    creada timestamptz := now() - interval '15 minutes';
    confirmada timestamptz := creada + interval '5 minutes';
    monto constant numeric(19,2) := 300.00;
    sena constant numeric(19,2) := 100.00;
    politica constant jsonb := '{"plazoMinutos":1440,"recordatorioMinutos":720,"retencionHoras":48}';
    motivo constant text := 'Precarga de la imprenta ficticia Senderos';
    forma_pago constant text := 'EFECTIVO';
    modalidad_retiro constant text := 'RETIRO_SUCURSAL';
    admin_nombre constant text := 'Elena Senderos';
    empleado_nombre constant text := 'Mateo Tintas';
    huella text := encode(sha256(convert_to(motivo,'UTF8')),'hex');
    nuevo_propietario boolean := NOT EXISTS (SELECT FROM inicializacion_sistema WHERE completada_en IS NOT NULL)
        AND NOT EXISTS (SELECT FROM usuario WHERE es_administrador_propietario);
    publicar_config boolean := NOT EXISTS (SELECT FROM configuracion_version WHERE estado='ACTIVA');
    publicar_catalogo boolean := NOT EXISTS (SELECT FROM catalogo_revision WHERE vigente);
    admin_id bigint;
    empleado_id bigint;
    cliente_id bigint;
    sucursal_id bigint;
    formato_id bigint;
    formato_publico uuid := gen_random_uuid();
    papel_id bigint;
    papel_publico uuid := gen_random_uuid();
    servicio_id bigint;
    servicio_publico uuid := gen_random_uuid();
    catalogo_id bigint;
    config_id bigint;
    config_numero bigint;
    impresora_id bigint;
    operacion_config uuid := gen_random_uuid();
    cotizacion_id bigint;
    preparacion_id bigint;
    item_id bigint;
    item_publico uuid := gen_random_uuid();
    archivo_id bigint;
    aceptacion_id bigint;
    pedido_id bigint;
    pago_id bigint;
    evento_id bigint;
    condiciones jsonb;
    oferta_demo jsonb;
BEGIN
    -- Sin una contraseña utilizable. El selector de Demo identifica estas cuentas por UUID.
    INSERT INTO usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado,
                        correo_verificado_en,es_administrador_propietario,fecha_alta)
    SELECT d.publico,r.id_rol,
           CASE WHEN EXISTS (SELECT FROM usuario WHERE lower(usuario.correo)=d.correo)
                THEN gen_random_uuid()::text||'@demo.invalid' ELSE d.correo END,
           '{pbkdf2@SpringSecurity_v5_8}' ||
           replace(gen_random_uuid()::text||gen_random_uuid()::text||gen_random_uuid()::text,'-',''),
           d.nombre,d.apellido,'ACTIVO',creada,nuevo_propietario AND d.publico=admin_publico,creada
    FROM (VALUES
        (admin_publico,'ADMIN_ADMIN','administracion@demo.invalid','Elena','Senderos'),
        (empleado_publico,'EMPLEADO','produccion@demo.invalid','Mateo','Tintas'),
        (cliente_publico,'CLIENTE','cliente@demo.invalid','Lucía','Papel')
    ) AS d(publico,rol_codigo,correo,nombre,apellido)
    JOIN rol r ON r.codigo=d.rol_codigo;
    SELECT id_usuario INTO STRICT admin_id FROM usuario WHERE codigo_publico=admin_publico;
    SELECT id_usuario INTO STRICT empleado_id FROM usuario WHERE codigo_publico=empleado_publico;
    SELECT id_usuario INTO STRICT cliente_id FROM usuario WHERE codigo_publico=cliente_publico;
    IF nuevo_propietario THEN
        UPDATE inicializacion_sistema SET completada_en=creada,id_usuario_propietario=admin_id WHERE unica;
    END IF;

    INSERT INTO sucursal(codigo_publico,codigo,nombre,calle,numero,localidad,provincia,codigo_postal,
                         zona_horaria,estado,id_usuario_alta,propuesta_matriz,fecha_alta)
    VALUES (sucursal_publica,
            CASE WHEN EXISTS (SELECT FROM sucursal WHERE codigo='DEMO-SENDEROS')
                 THEN 'DEMO-'||replace(gen_random_uuid()::text,'-','') ELSE 'DEMO-SENDEROS' END,
            sede,'Pasaje de las Tintas','120','Ciudad de Buenos Aires','CABA','1407',zona,'ACTIVA',admin_id,
            publicar_config AND NOT EXISTS (SELECT FROM sucursal WHERE propuesta_matriz),creada)
    RETURNING id_sucursal INTO sucursal_id;
    INSERT INTO usuario_sucursal(id_usuario,id_sucursal,fecha_desde) VALUES (empleado_id,sucursal_id,creada);
    INSERT INTO usuario_permiso(id_usuario,id_permiso,id_usuario_otorgante,fecha_otorgamiento)
    SELECT empleado_id,id_permiso,admin_id,creada FROM permiso WHERE codigo IN
        ('GESTIONAR_PEDIDOS','GESTIONAR_PRODUCCION','CONTROLAR_CALIDAD',
         'GESTIONAR_ENTREGAS','CERRAR_PEDIDOS','REGISTRAR_COBRO');
    INSERT INTO sucursal_horario_atencion(id_sucursal,dia_semana,habilitado,hora_desde,hora_hasta)
    SELECT sucursal_id,dia,true,apertura_atencion,cierre FROM generate_series(1,7) AS dia;
    INSERT INTO evento_organizacion(id_usuario_actor,tipo,codigo_publico_objeto,version,fecha) VALUES
        (admin_id,'ALTA_SUCURSAL',sucursal_publica,0,creada),
        (admin_id,'ALTA_EMPLEADO',empleado_publico,0,creada);

    -- Una combinación A4, dos precios, un medio de pago y una impresora de producción manual.
    INSERT INTO formato(codigo_publico,codigo,nombre,ancho_mm,alto_mm)
    VALUES (formato_publico,
            CASE WHEN EXISTS (SELECT FROM formato WHERE codigo='DEMO-A4')
                 THEN 'DEMO-'||replace(formato_publico::text,'-','') ELSE 'DEMO-A4' END,'A4',210,297)
    RETURNING id_formato INTO formato_id;
    INSERT INTO papel(codigo_publico,codigo,nombre,gramaje_g_m2,terminacion_tipo)
    VALUES (papel_publico,
            CASE WHEN EXISTS (SELECT FROM papel WHERE codigo='DEMO-OBRA-80')
                 THEN 'DEMO-'||replace(papel_publico::text,'-','') ELSE 'DEMO-OBRA-80' END,'Obra',80,'Mate')
    RETURNING id_papel INTO papel_id;
    INSERT INTO catalogo_papel(id_formato,id_papel) VALUES (formato_id,papel_id);
    INSERT INTO servicio(codigo_publico,codigo,nombre,tipo,descripcion)
    VALUES (servicio_publico,
            CASE WHEN EXISTS (SELECT FROM servicio WHERE codigo='DEMO-IMPRESION')
                 THEN 'DEMO-'||replace(servicio_publico::text,'-','') ELSE 'DEMO-IMPRESION' END,
            'Impresión','IMPRESION','Impresión simulada para demostrar el circuito de trabajo.')
    RETURNING id_servicio INTO servicio_id;

    -- Las versiones vigentes de una instalación previa conservan sus referencias.
    INSERT INTO catalogo_revision(codigo_publico,id_operacion,hash_solicitud,motivo,id_actor,
                                  vigente,estado,creada_en,activada_en)
    VALUES (catalogo_publico,gen_random_uuid(),huella,motivo,admin_id,publicar_catalogo,
            CASE WHEN publicar_catalogo THEN 'VIGENTE' ELSE 'HISTORICA' END,creada,creada)
    RETURNING id_catalogo_revision INTO catalogo_id;
    INSERT INTO configuracion_servicio(id_catalogo_revision,id_servicio,nombre_visible,base_precio,
                                      precio_unitario,preparacion_minutos,habilitado)
    VALUES (catalogo_id,servicio_id,'Impresión','POR_CARILLA',0,5,true);
    INSERT INTO tarifa_impresion(id_catalogo_revision,id_formato,id_papel,modo_color,precio_por_carilla,
                                 recargo_doble_faz,habilitada,grupo,nombre,modo_doble_faz,valor_doble_faz) VALUES
        (catalogo_id,formato_id,papel_id,'BLANCO_NEGRO',100,0,true,gen_random_uuid(),'A4 · Obra · B/N','FIJO',170),
        (catalogo_id,formato_id,papel_id,'COLOR',250,0,true,gen_random_uuid(),'A4 · Obra · Color','FIJO',425);

    SELECT coalesce(max(numero_version),0)+1 INTO config_numero FROM configuracion_version;
    INSERT INTO configuracion_version(codigo_publico,numero_version,id_usuario_creador,estado,modelo,
                                      version,fecha_creacion,fecha_actualizacion)
    VALUES (config_publica,config_numero,admin_id,CASE WHEN publicar_config THEN 'ACTIVA' ELSE 'HISTORICA' END,
            'MANUAL',7,creada,creada)
    RETURNING id_configuracion_version INTO config_id;
    INSERT INTO configuracion_financiera(id_configuracion_version,vigencia_cotizacion_minutos,exigir_sena,
                                         condicion_sena,tipo_sena,valor_sena,politica_pago_archivos)
    VALUES (config_id,1440,true,'SIEMPRE','FIJA',sena,politica);
    INSERT INTO configuracion_medio_pago VALUES (config_id,forma_pago);
    INSERT INTO configuracion_recursos VALUES (config_id,'MANUAL');
    INSERT INTO impresora(codigo_publico,id_usuario_creador,fecha_alta) VALUES (impresora_publica,admin_id,creada)
    RETURNING id_impresora INTO impresora_id;
    INSERT INTO configuracion_impresora(id_configuracion_version,id_impresora,nombre,admite_color,admite_doble_faz,
                                       capacidad_maxima_hojas,estado,id_sucursal_ubicacion)
    VALUES (config_id,impresora_id,'Impresora Senderos (simulada)',true,true,250,'OPERATIVA',sucursal_id);
    INSERT INTO configuracion_impresora_formato VALUES (config_id,impresora_id,formato_id);
    INSERT INTO sucursal_servicio VALUES (config_id,sucursal_id,servicio_id);
    INSERT INTO configuracion_entrega(id_configuracion_version,preparacion_horas,traslado_horas,
                                      id_sucursal_matriz,matriz_confirmada)
    VALUES (config_id,1,0,sucursal_id,true);
    INSERT INTO configuracion_modalidad_entrega VALUES (config_id,modalidad_retiro);
    INSERT INTO configuracion_horario_sucursal VALUES (config_id,sucursal_id,zona);
    INSERT INTO horario_sucursal
    SELECT config_id,sucursal_id,dia,true,apertura_atencion,cierre FROM generate_series(1,7) AS dia;
    INSERT INTO franja_entrega(id_configuracion_retiro,id_sucursal_retiro,dia_semana,hora_desde,
                               hora_hasta,capacidad_pedidos,habilitada)
    SELECT config_id,sucursal_id,dia,apertura,cierre,cupo_retiro,true FROM generate_series(1,7) AS dia;
    INSERT INTO autorizacion_configuracion(id_operacion,proposito,id_configuracion_version,version_configuracion,
                                          id_actor,version_acceso,correo_destino,hash_solicitud,hash_codigo,
                                          fecha_emision,fecha_vencimiento,fecha_consumo)
    SELECT operacion_config,'ACTIVAR_CONFIGURACION',config_id,7,admin_id,version_acceso,correo,huella,
           encode(sha256(uuid_send(gen_random_uuid())),'hex'),creada,creada+interval '15 minutes',creada
    FROM usuario WHERE id_usuario=admin_id;
    INSERT INTO activacion_configuracion(id_configuracion_version,id_catalogo_revision,id_actor,
                                         id_operacion,motivo,fecha_activacion,fin_vigencia)
    VALUES (config_id,catalogo_id,admin_id,operacion_config,motivo,creada,CASE WHEN NOT publicar_config THEN now() END);
    INSERT INTO evento_configuracion(id_configuracion_version,id_actor,tipo,version,motivo,fecha) VALUES
        (config_id,admin_id,'CONFIGURACION_ACTIVADA',7,motivo,creada);

    -- Fotografía de la oferta aceptada: tres copias A4 en blanco y negro.
    condiciones := jsonb_build_object(
        'revisionHumana',true,'cargaRequiereAcreditacion',false,'pagoPrevioRequerido','0.00',
        'senaRequerida',sena::text,'saldo',(monto-sena)::text,'momento','ANTES_APROBACION_Y_PRODUCCION',
        'mediosAcreditacion',jsonb_build_array(forma_pago),'politicaArchivos',politica,
        'instrucciones',jsonb_build_array('Pedido ficticio. El cobro de muestra ya está aplicado.'));

    oferta_demo := jsonb_build_object(
        'configuracion',config_publica,'numeroConfiguracion',config_numero,
        'revisionComercial',catalogo_publico,'numeroRevision',catalogo_id,
        'sucursal',jsonb_build_object('codigoPublico',sucursal_publica,'nombre',sede,'direccion',direccion),
        'items',jsonb_build_array(jsonb_build_object(
            'codigoPublico',item_publico,'formato','A4','papel','Obra',
            'documento',jsonb_build_object('nombre',pdf_nombre,'bytes',pdf_bytes,'sha256',pdf_sha),
            'trabajo',jsonb_build_object('servicio',servicio_publico,'formato',formato_publico,'papel',papel_publico,
                'color','BLANCO_NEGRO','paginas',1,'copias',3,'dobleFaz',false,'terminaciones','[]'::jsonb),
            'precio',jsonb_build_object('paginas',1,'copias',3,'carillas',3,'hojas',3,'subtotal',monto::text,
                'minimoPreparacionMinutos',5,'lineas',jsonb_build_array(jsonb_build_object(
                    'servicio',servicio_publico,'nombre','Impresión','base','POR_CARILLA','unidades',3,
                    'precioUnitario','100.00','importe',monto::text))))),
        'modalidad',modalidad_retiro,'destino',sede||' · '||direccion,'medioPago',forma_pago,
        'subtotal',monto::text,'costoEntrega','0.00','total',monto::text,
        'condiciones',condiciones,'mediosGenerales',jsonb_build_array(forma_pago),'origenGlobal',true,
        'entrega',jsonb_build_object('version',7,'zonaHoraria',zona,'recibidoEn',creada,
            'inicioPreparacion',desde-interval '5 hours','finPreparacion',desde-interval '4 hours','disponibleDesde',desde,
            'enCola',true,'advertencias','[]'::jsonb,
            'destinoSucursal',jsonb_build_object('sucursal',sucursal_publica,'nombre',sede,
                'zonaHoraria',zona,'fecha',fecha_retiro,'apertura',left(apertura::text,5),'cierre',left(cierre::text,5),
                'franjaDesde',desde,'franjaHasta',hasta,'cupoConfigurado',cupo_retiro)));

    INSERT INTO cotizacion(codigo_publico,id_usuario_creador,id_sucursal,id_configuracion_version,
                           id_configuracion_confirmacion,id_catalogo_revision,estado,version,generada_en,
                           aceptada_en,confirmada_en,vigente_hasta,subtotal,costo_entrega,total,oferta)
    VALUES (gen_random_uuid(),cliente_id,sucursal_id,config_id,config_id,catalogo_id,'CONFIRMADA',3,creada,
            creada+interval '1 minute',confirmada,creada+interval '1 day',monto,0,monto,oferta_demo)
    RETURNING id_cotizacion INTO cotizacion_id;
    INSERT INTO evento_cotizacion(id_cotizacion,id_actor,id_operacion,tipo,huella,fecha) VALUES
        (cotizacion_id,cliente_id,gen_random_uuid(),'COTIZAR',huella,creada),
        (cotizacion_id,cliente_id,gen_random_uuid(),'ACEPTAR_OFERTA',huella,creada+interval '1 minute');
    INSERT INTO cotizacion_item(codigo_publico,id_cotizacion,orden,id_formato,id_papel,cantidad_paginas_declaradas,
                                cantidad_copias,cantidad_carillas,cantidad_hojas,modo_color,doble_faz,
                                nombre_archivo_declarado,bytes_declarados,sha256_declarado,subtotal)
    VALUES (item_publico,cotizacion_id,1,formato_id,papel_id,1,3,3,3,'BLANCO_NEGRO',false,pdf_nombre,pdf_bytes,pdf_sha,monto)
    RETURNING id_cotizacion_item INTO item_id;
    INSERT INTO cotizacion_item_servicio(id_cotizacion_item,id_servicio,tipo_servicio,nombre_visible,base_precio,
                                         cantidad,precio_unitario,subtotal)
    VALUES (item_id,servicio_id,'IMPRESION','Impresión','POR_CARILLA',3,100,monto);

    INSERT INTO archivo_almacenado(codigo_publico,id_usuario_cargador,id_operacion,nombre_original,estado,
                                   fecha_creacion,cargar_hasta,fecha_inicio,fecha_fin,cantidad_bytes,sha256,
                                   cantidad_paginas,codigo_resultado,mensaje)
    VALUES (pdf_publico,cliente_id,gen_random_uuid(),pdf_nombre,'VALIDO',creada+interval '2 minutes',
            confirmada,creada+interval '2 minutes',creada+interval '3 minutes',pdf_bytes,pdf_sha,1,
            'PDF_FICTICIO','Original privado incluido en la Demo para el pedido ficticio.')
    RETURNING id_archivo_almacenado INTO archivo_id;
    INSERT INTO preparacion_cliente(codigo_publico,id_usuario,items,preferencias,creada_en,actualizada_en)
    VALUES (gen_random_uuid(),cliente_id,jsonb_build_array(jsonb_build_object(
                'archivo',pdf_publico,'trabajo',oferta_demo->'items'->0->'trabajo')),
            jsonb_build_object('modalidad',modalidad_retiro,'destinoRetiro',sucursal_publica,'medioPago',forma_pago),
            creada,creada+interval '3 minutes')
    RETURNING id_preparacion INTO preparacion_id;
    INSERT INTO archivo_preparado(id_archivo_almacenado,id_preparacion) VALUES (archivo_id,preparacion_id);
    INSERT INTO archivo_trabajo(id_archivo_almacenado,id_cotizacion_item,activo,fecha_asociacion)
    VALUES (archivo_id,item_id,true,creada+interval '3 minutes');
    INSERT INTO validacion_archivo(id_archivo_almacenado,tipo_validacion,resultado,motor,version_motor,codigo_resultado,fecha)
    SELECT archivo_id,tipo,'ACEPTADO','Precarga Demo','1','PDF_GENERADO_LOCALMENTE',creada+interval '3 minutes'
    FROM (VALUES ('SEGURIDAD'),('ESTRUCTURA_RENDER')) AS validacion(tipo);
    INSERT INTO aceptacion_vista_previa(id_archivo_almacenado,id_usuario,id_operacion,sha256,fecha)
    VALUES (archivo_id,cliente_id,gen_random_uuid(),pdf_sha,creada+interval '4 minutes')
    RETURNING id_aceptacion_vista_previa INTO aceptacion_id;

    INSERT INTO pedido(codigo_publico,id_cotizacion,id_usuario_creador,id_sucursal,id_configuracion_version,
                       estado,modo_aprobacion,confirmada_en,total,condiciones_finales)
    VALUES (pedido_publico,cotizacion_id,cliente_id,sucursal_id,config_id,'PENDIENTE_REVISION','HUMANA',confirmada,monto,
            jsonb_build_object('cotizadas',condiciones,'actuales',condiciones||jsonb_build_object(
                'version',7,'total',monto::text,'mediosGenerales',jsonb_build_array(forma_pago)),
                'revisionHumana',true))
    RETURNING id_pedido INTO pedido_id;
    UPDATE preparacion_cliente SET estado='CONFIRMADA',version=4,id_pedido=pedido_id,
        id_ultima_cotizacion=cotizacion_id,actualizada_en=confirmada WHERE id_preparacion=preparacion_id;
    INSERT INTO pedido_item(id_pedido,id_cotizacion_item,id_archivo_almacenado,id_aceptacion_vista_previa)
    VALUES (pedido_id,item_id,archivo_id,aceptacion_id);
    INSERT INTO historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha)
    VALUES (pedido_id,cliente_id,gen_random_uuid(),huella,'PENDIENTE_REVISION',
            'Pedido ficticio confirmado con el afiche de muestra.',confirmada);
    INSERT INTO reserva_entrega(id_pedido,modalidad,destino_publico,origen_publico,nombre_destino,zona_horaria,
                                fecha_local,apertura,cierre,franja_desde,franja_hasta,cupo_confirmado,costo)
    VALUES (pedido_id,modalidad_retiro,sucursal_publica,sucursal_publica,sede,zona,fecha_retiro,
            apertura,cierre,desde,hasta,cupo_retiro,0);

    INSERT INTO cobro_pedido(id_pedido,politica_original,fecha_limite,recordatorio_minutos,retencion_horas,
                             retener_hasta,cobertura_confirmada_en)
    VALUES (pedido_id,politica,confirmada+interval '24 hours',720,48,confirmada+interval '72 hours',
            confirmada+interval '2 minutes');
    INSERT INTO evento_cobro_pedido(id_pedido,id_operacion,id_actor,tipo,motivo,resultado,fecha) VALUES
        (pedido_id,gen_random_uuid(),cliente_id,'PEDIDO_CREADO','Pedido ficticio con política de cobro.',
         jsonb_build_object('simulado',true),confirmada),
        (pedido_id,gen_random_uuid(),empleado_id,'PAGO_APLICADO','Cobro ficticio acreditado y aplicado.',
         jsonb_build_object('simulado',true,'importe',monto::text),confirmada+interval '2 minutes');
    INSERT INTO evento_financiero(id_operacion,id_cotizacion,id_actor,actor_nombre,actor_rol,tipo,huella,motivo,fecha)
    VALUES (gen_random_uuid(),cotizacion_id,empleado_id,empleado_nombre,'EMPLEADO','RECIBIR',huella,
            'Cobro ficticio en efectivo.',confirmada+interval '1 minute')
    RETURNING id_evento INTO evento_id;
    INSERT INTO pago(codigo_publico,id_cotizacion,id_evento,medio,importe,referencia,clave_referencia,recibido_en)
    VALUES (gen_random_uuid(),cotizacion_id,evento_id,forma_pago,monto,'Recibo ficticio Senderos',
            'demo:'||gen_random_uuid()::text,confirmada+interval '1 minute')
    RETURNING id_pago INTO pago_id;
    INSERT INTO evento_financiero(id_operacion,id_cotizacion,id_actor,actor_nombre,actor_rol,tipo,huella,motivo,fecha)
    VALUES (gen_random_uuid(),cotizacion_id,empleado_id,empleado_nombre,'EMPLEADO','APLICAR',huella,
            'Cobro ficticio aplicado al pedido.',confirmada+interval '2 minutes')
    RETURNING id_evento INTO evento_id;
    INSERT INTO aplicacion_pago_cotizacion(id_pago,id_cotizacion,id_evento,importe)
    VALUES (pago_id,cotizacion_id,evento_id,monto);

    UPDATE pedido SET version=2,revisada_en=confirmada+interval '3 minutes',id_revisor=admin_id
    WHERE id_pedido=pedido_id;
    INSERT INTO historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha,
                                        estado_origen,version_pedido,accion,mensaje_cliente,actor_nombre,actor_tipo)
    VALUES (pedido_id,admin_id,gen_random_uuid(),huella,'PENDIENTE_REVISION','Revisión del afiche ficticio.',
            confirmada+interval '3 minutes','PENDIENTE_REVISION',2,'REVISAR',
            'Se revisó el afiche de muestra.',admin_nombre,'ADMIN_ADMIN');
    UPDATE pedido SET estado='APROBADO',version=3,aprobada_en=confirmada+interval '4 minutes',id_aprobador=admin_id
    WHERE id_pedido=pedido_id;
    INSERT INTO historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha,
                                        estado_origen,version_pedido,accion,mensaje_cliente,actor_nombre,actor_tipo)
    VALUES (pedido_id,admin_id,gen_random_uuid(),huella,'APROBADO','Pedido ficticio listo para producir.',
            confirmada+interval '4 minutes','PENDIENTE_REVISION',3,'APROBAR',
            'El afiche está listo para impresión simulada.',admin_nombre,'ADMIN_ADMIN');
END $$;
