import type {Order} from './order-types';

/** The delivery task follows the agreed mode and the recorded physical journey. */
export function deliveryExperience(order:Pick<Order,'reserva'|'retiroConTraslado'|'oferta'>,state:string){
 const home=order.reserva.modalidad==='ENVIO_DOMICILIO';
 const local=order.reserva.modalidad==='RETIRO_SUCURSAL'&&!order.retiroConTraslado;
 const absent=home&&state==='CLIENTE_AUSENTE';
 const canReceive=local?state==='LISTO_RETIRO':home?state==='EN_VIAJE':state==='DISPONIBLE_PUNTO';
 let title='Preparar para el despacho',description='Confirmá que el pedido completo está preparado. Después aparecerá la acción “Despachar · poner en viaje”.';
 if(local){title=`Disponible para retirar en ${order.oferta?.sucursal?.nombre??'la sucursal'}`;description='No requiere despacho ni viaje. Cuando llegue la persona, ingresá su código y confirmá la entrega; el sistema comprobará el saldo.';}
 else if(absent){title='Entrega pendiente · cliente ausente';description='El intento quedó registrado y el código se conserva. Coordiná el próximo intento con el cliente y registrá su inicio cuando vuelvan a salir.';}
 else if(state==='PREPARADO'){title='Despachar · poner en viaje';description='Confirmá la salida hacia el destino acordado. Se generará el código de entrega y el cliente podrá verlo desde su pedido.';}
 else if(state==='EN_VIAJE'){title=home?'Entrega a domicilio en curso':'Registrar llegada al destino';description=home?'En el domicilio, ingresá el código del cliente para entregar. Si nadie puede recibirlo, registrá el intento sin entrega.':'El pedido está en viaje hacia la sucursal o punto acordado. Registrá su llegada para habilitar el retiro.';}
 else if(state==='DISPONIBLE_PUNTO'){title='Disponible para retirar';description='Ingresá el código presentado y confirmá la entrega; el sistema comprobará el saldo.';}
 return {home,local,absent,canReceive,title,description,receiveTitle:home?'Entregar en el domicilio':local?`Retiro en ${order.oferta?.sucursal?.nombre??'la sucursal'}`:'Entrega en el punto de retiro',receiveDescription:canReceive?home?'Pedile el código a la persona que recibe y entregá el pedido completo. No necesita mostrar el celular.':'Ingresá el código que presenta la persona y entregá el pedido completo. No necesita mostrar el celular.':absent?'No se registró una entrega. El mismo código servirá para el próximo intento.':'El ingreso del código se habilita al completar el recorrido de esta modalidad.'};
}
