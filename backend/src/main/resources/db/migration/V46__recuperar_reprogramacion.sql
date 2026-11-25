-- La aceptación del cliente genera un evento con identidad y conserva el control de pagos de V42.
ALTER TABLE lamontana.historial_estado_pedido DROP CONSTRAINT gestion_pedido_evento_check;
ALTER TABLE lamontana.historial_estado_pedido ADD CONSTRAINT gestion_pedido_evento_check CHECK(
 (version_pedido IS NULL AND accion IS NULL AND estado_origen IS NULL AND mensaje_cliente IS NULL AND actor_nombre IS NULL AND actor_tipo IS NULL AND id_actor IS NOT NULL)
 OR (version_pedido>1 AND accion IS NOT NULL AND estado_origen IS NOT NULL AND mensaje_cliente IS NOT NULL AND length(trim(mensaje_cliente))>0 AND actor_nombre IS NOT NULL
 AND ((actor_tipo IN ('CLIENTE','ADMIN_ADMIN','EMPLEADO') AND id_actor IS NOT NULL
 AND accion IN ('REVISAR','APROBAR','RECHAZAR','CANCELAR','PEDIR_CORRECCION','RESPONDER_CORRECCION','INICIAR_TRABAJO','COMPLETAR_TRABAJO','ERROR_TRABAJO','CANCELAR_TRABAJO','CONTROLAR_CALIDAD','PREPARAR_ENVIO','SALIR_REPARTO','LLEGAR_PUNTO','ENTREGAR','CERRAR'))
 OR (accion='REPROGRAMAR' AND version_pedido IS NOT NULL AND actor_tipo IS NOT NULL
     AND actor_tipo='CLIENTE' AND id_actor IS NOT NULL)
 OR (actor_tipo='SISTEMA' AND id_actor IS NULL AND accion='CANCELAR'))));
