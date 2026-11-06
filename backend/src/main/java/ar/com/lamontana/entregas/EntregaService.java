//#region ENCABEZADO · EntregaService.java
/*
 * ========================================================================
 * ARCHIVO: EntregaService.java
 * ========================================================================
 * FUNCIÓN
 * Gestiona códigos de entrega, movimientos logísticos, entrega física y cierre. Revalida permisos,
 * dinero, producción, reserva y versión, conservando auditoría y resultados idempotentes.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] EntregaService(JdbcTemplate jdbc, PedidoService pedidos, OrganizacionService
 *   organizacion, CoberturaPagos cobertura, PasswordEncoder encoder)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] Generacion :: String toString()
 * - [public] Vista consultar(UUID pedido, String correo, boolean interno)
 * - [private] Vista vista(PedidoService.Detalle p, String correo, boolean interno)
 * - [public] Generacion emitir(UUID pedido, Emitir in, String correo)
 * - [public] Vista mover(UUID pedido, Movimiento in, String correo)
 * - [public] Validacion validar(UUID pedido, Validar in, String correo)
 * - [public] Vista entregar(UUID pedido, Entregar in, String correo)
 * - [public] Vista cerrar(UUID pedido, Cerrar in, String correo)
 * - [private] List<String> bloqueosEntrega(PedidoService.Detalle p)
 * - [private] List<String> bloqueosCierre(PedidoService.Detalle p)
 * - [private] boolean disponible(PedidoService.Detalle p)
 * - [private] String bloqueoMovimiento(PedidoService.Detalle p, String estado, Accion op, boolean
 *   permiso)
 * - [private] String logistica(PedidoService.Detalle p)
 * - [private] long reserva(PedidoService.Detalle p)
 * - [private] BigDecimal aplicado(PedidoService.Detalle p)
 * - [private] Secreto ultimoCodigo(long pedido)
 * - [private] String estadoCodigo(Secreto c, Instant now)
 * - [private] Verificacion verificacion(PedidoService.Detalle p, Secreto c, long actor, Instant
 *   now)
 * - [private] long evento(PedidoService.Detalle p, Actor a, UUID op, String hash, String accion,
 *   String destino, String motivo, String mensaje, Instant ahora)
 * - [private] boolean recuperar(UUID op, String hash, PedidoService.Detalle p, Actor a)
 * - [private] Actor actor(String correo)
 * - [private] void bloquear()
 *   Toma el bloqueo transaccional de PostgreSQL.
 * - [private] void version(PedidoService.Detalle p, long v)
 * - [private] void texto(String v, int max)
 * - [private] void confirmar(boolean v)
 * - [private] String dinero(BigDecimal v)
 * - [private] String huella(Object v)
 *   Calcula o prepara la huella SHA-256 del contenido.
 * - [private] ResponseStatusException conflicto(String m)
 * - [private] ResponseStatusException error(HttpStatus s, String m)
 *   Construye un error HTTP controlado.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - EntregaService (clase).
 * - EntregaService.Opcion (record).
 * - EntregaService.Codigo (record).
 * - EntregaService.Verificacion (record).
 * - EntregaService.Constancia (record).
 * - EntregaService.Evento (record).
 * - EntregaService.Cierre (record).
 * - EntregaService.Vista (record).
 * - EntregaService.Generacion (record).
 * - EntregaService.Validacion (record).
 * - EntregaService.Actor (record).
 * - EntregaService.Secreto (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.entregas;
import static ar.com.lamontana.entregas.EntregaController.*;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad;
import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.CoberturaPagos;
import ar.com.lamontana.pagos.PagoService;
import ar.com.lamontana.pagos.PagoController;
import ar.com.lamontana.pedidos.PedidoService;
import ar.com.lamontana.puntos.CuentaPuntoAcceso;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
@Service
public class EntregaService {
 private final JdbcTemplate jdbc;private final PedidoService pedidos;private final OrganizacionService organizacion;private final CoberturaPagos cobertura;private final PasswordEncoder encoder;
 private final SecureRandom random=new SecureRandom();private final Clock clock=Clock.systemUTC();private final JsonMapper json=JsonMapper.builder().build();private final CodigoRetiroService codigos;private final PagoService pagos;
 public EntregaService(JdbcTemplate jdbc,PedidoService pedidos,OrganizacionService organizacion,CoberturaPagos cobertura,PasswordEncoder encoder,CodigoRetiroService codigos,PagoService pagos){this.jdbc=jdbc;this.pedidos=pedidos;this.organizacion=organizacion;this.cobertura=cobertura;this.encoder=encoder;this.codigos=codigos;this.pagos=pagos;}
 public record Opcion(Accion accion,boolean habilitada,String motivo){}
 public record Codigo(UUID codigoPublico,String estado,Instant emitidoEn,Instant venceEn,int intentosRestantes,Instant puedeRenovarEn,String texto){@Override public String toString(){return "Codigo[código privado]";}}
 public record Verificacion(UUID codigoPublico,Instant fecha,Instant validoHasta){}
 public record Constancia(UUID codigoPublico,String receptor,String responsable,Instant fecha,String motivo){}
 public record Evento(String accion,String estadoOrigen,String estadoDestino,String actor,String motivo,Instant fecha){}
 public record Cierre(Instant fecha,String responsable,String motivo,String total,String aplicado){}
 public record CobroEfectivo(String importe,boolean habilitado,boolean puedeCerrar,String bloqueo){}
 public record ReciboEfectivo(UUID pago,String importe,String referencia,String responsable,Instant fecha){}
 public record Vista(long version,String estadoPedido,String estadoLogistico,String cliente,String correo,boolean puedeGestionar,boolean puedeCerrar,boolean puedeEmitirCodigo,List<Opcion> acciones,
  Codigo codigo,Verificacion validacion,Constancia entrega,Cierre cierre,String aplicado,String saldo,List<String> bloqueosEntrega,List<String> bloqueosCierre,List<Evento> historial,CobroEfectivo cobroEfectivo,ReciboEfectivo reciboEfectivo){}
 public record Generacion(Vista vista,String codigo,String aviso){@Override public String toString(){return "Generacion[código privado]";}}
 public record Validacion(Vista vista,boolean aceptado,String mensaje){}
 public record Repartidor(UUID codigoPublico,String nombre,String correo){}
 public record ResultadoLote(UUID operacion,Accion accion,int cantidad,Instant fecha,UUID recorrido,Repartidor repartidor){}
 private record Actor(long id,String nombre,String rol){}
 public List<Repartidor> repartidores(String correo){
  organizacion.exigirPermiso(correo,"GESTIONAR_ENTREGAS");
  return jdbc.query("""
   SELECT u.codigo_publico,concat_ws(' ',u.nombre,u.apellido),u.correo
   FROM lamontana.cuenta_repartidor cr JOIN lamontana.usuario u USING(id_usuario)
   JOIN lamontana.rol r USING(id_rol)
   WHERE cr.activa AND u.estado='ACTIVO' AND r.activo AND r.codigo IN ('EMPLEADO','ADMIN_ADMIN')
   ORDER BY u.apellido,u.nombre
   """,
   (r,n)->new Repartidor(r.getObject(1,UUID.class),r.getString(2),r.getString(3)));
 }
 private record Secreto(long id,UUID codigo,String hash,long reserva,Instant emision,Instant vence,Instant revocado,Instant utilizado,int intentos){}
 @Transactional public Vista consultar(UUID pedido,String correo,boolean interno){bloquear();var p=pedidos.detalle(pedido,correo,interno);codigos.asegurar(p.numero());return vista(p,correo,interno);}
 @Transactional public Vista consultarRetiro(UUID sucursal,UUID pedido,String correo){
  bloquear();var p=pedidos.detalleRetiroSucursal(pedido,sucursal,correo);
  codigos.asegurar(p.numero());
  return vista(p,correo,true,sucursal);
 }
 private Vista vista(PedidoService.Detalle p,String correo,boolean interno){return vista(p,correo,interno,null);}
 private Vista vista(PedidoService.Detalle p,String correo,boolean interno,UUID retiro){
  var a=actor(correo);var permisos=interno?organizacion.contexto(correo).permisos():List.<String>of();boolean gestiona=permisos.contains("GESTIONAR_ENTREGAS"),cierra=permisos.contains("CERRAR_PEDIDOS");String estado=logistica(p);
  var code=ultimoCodigo(p.numero());Instant ahora=clock.instant();String plain=null;if(!interno&&p.estado().equals("LISTO_PARA_ENTREGA")&&code!=null&&estadoCodigo(code,ahora).equals("ACTIVO")){var encrypted=jdbc.query("SELECT codigo_cifrado FROM lamontana.codigo_entrega WHERE id_codigo_entrega=?",(r,n)->r.getString(1),code.id());if(!encrypted.isEmpty()&&encrypted.get(0)!=null)plain=codigos.texto(code.codigo(),encrypted.get(0));}Codigo codigo=code==null?null:new Codigo(code.codigo(),estadoCodigo(code,ahora),code.emision(),code.vence(),Math.max(0,5-code.intentos()),code.emision().plusSeconds(30),plain);
  var validation=interno?verificacion(p,code,a.id(),ahora):null;
  var entrega=jdbc.query("SELECT e.codigo_publico,e.receptor_nombre,h.actor_nombre,e.fecha_entrega,e.nota FROM lamontana.entrega e JOIN lamontana.historial_estado_pedido h ON h.id_evento=e.id_evento_pedido WHERE e.id_pedido=?",(r,n)->new Constancia(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),interno?r.getString(5):null),p.numero());
  var cierre=jdbc.query("SELECT c.fecha_cierre,h.actor_nombre,h.motivo,c.total,c.aplicado FROM lamontana.cierre_pedido c JOIN lamontana.historial_estado_pedido h ON h.id_evento=c.id_evento_pedido WHERE c.id_pedido=?",(r,n)->new Cierre(r.getTimestamp(1).toInstant(),r.getString(2),interno?r.getString(3):null,r.getBigDecimal(4).toPlainString(),r.getBigDecimal(5).toPlainString()),p.numero());
  var history=jdbc.query("SELECT h.accion,m.estado_origen,m.estado_destino,h.actor_nombre,h.motivo,h.mensaje_cliente,h.fecha FROM lamontana.movimiento_entrega m JOIN lamontana.historial_estado_pedido h ON h.id_evento=m.id_evento_pedido WHERE m.id_pedido=? ORDER BY m.id_movimiento_entrega",(r,n)->new Evento(r.getString(1),r.getString(2),r.getString(3),interno?r.getString(4):"La imprenta",interno?r.getString(5):r.getString(6),r.getTimestamp(7).toInstant()),p.numero());
  var options=Arrays.stream(Accion.values()).map(op->{
   String motivo=bloqueoMovimiento(p,estado,op,gestiona);
   if(motivo==null)motivo=bloqueoDespacho(p,op,correo,a);
   return new Opcion(op,motivo==null,motivo);
  }).toList();
  var cliente=jdbc.queryForObject("SELECT d.nombre,d.correo FROM lamontana.pedido_actual p JOIN lamontana.destinatario_cotizacion d USING(id_cotizacion) WHERE p.id_pedido=?",(r,n)->new String[]{r.getString(1),r.getString(2)},p.numero());
  String bloqueoPunto=interno&&p.estado().equals("LISTO_PARA_ENTREGA")?bloqueoResponsablePunto(p,correo):null;
  var b=new ArrayList<>(bloqueosEntrega(p));
  if(bloqueoPunto!=null)b.add(0,bloqueoPunto);
  if(interno&&!gestiona)b.add("Se requiere permiso de gestión de entregas.");
  var bc=new ArrayList<>(bloqueosCierre(p));if(interno&&!cierra)bc.add("Se requiere permiso para cerrar pedidos.");
  BigDecimal aplicado=aplicado(p);return new Vista(p.version(),p.estado(),estado,cliente[0],interno?cliente[1]:null,gestiona,cierra,false,options,codigo,validation,entrega.isEmpty()?null:entrega.get(0),cierre.isEmpty()?null:cierre.get(0),dinero(aplicado),dinero(new BigDecimal(p.oferta().total()).subtract(aplicado).max(BigDecimal.ZERO)),List.copyOf(b),List.copyOf(bc),history,interno&&bloqueoPunto==null?cobroEfectivo(p,correo,permisos,retiro):null,interno?reciboEfectivo(p):null);
 }
 @Transactional public Generacion emitir(UUID pedido,Emitir in,String correo){
  bloquear();var p=pedidos.detalle(pedido,correo,false);codigos.asegurar(p.numero());var view=vista(p,correo,false);
  if(view.codigo()==null||view.codigo().texto()==null)throw conflicto("El código se genera al despachar o cuando el retiro en imprenta está listo.");
  return new Generacion(view,view.codigo().texto(),"Este es el código automático del pedido. Se conserva hasta su entrega.");
 }
 @Transactional public Vista mover(UUID pedido,Movimiento in,String correo){
  if(in.accion()==Accion.SALIR_REPARTO){
   organizacion.exigirPermiso(correo,"GESTIONAR_ENTREGAS");
   throw conflicto("Asigná un rider desde Entregas pendientes para iniciar el reparto.");
  }
  return registrarMovimiento(pedido,in,correo,false,null);
 }
 @Transactional public Vista recibirEnSucursal(UUID sucursal,UUID pedido,UUID operacion,long version,boolean confirmado,String correo){
  return registrarMovimiento(pedido,new Movimiento(operacion,version,Accion.LLEGAR_PUNTO,null,confirmado),correo,false,sucursal);
 }
 private Vista registrarMovimiento(UUID pedido,Movimiento in,String correo,boolean rider,UUID retiro){
  bloquear();var p=detalleEntrega(pedido,correo,rider,retiro);var a=actor(correo);String hash=huella(List.of(pedido,"MOVIMIENTO",in));
  exigirDespacho(p,in.accion(),correo,a);
  if(recuperar(in.operacion(),hash,p,a))return vista(p,correo,true,retiro);version(p,in.version());String motivo=nota(in.motivo(),switch(in.accion()){case PREPARAR_ENVIO->"Pedido preparado para despacho.";case SALIR_REPARTO->"Salida al destino acordado confirmada.";case LLEGAR_PUNTO->"Llegada al destino de retiro confirmada.";case CLIENTE_AUSENTE->"Intento de entrega a domicilio sin receptor.";case REINTENTAR_ENTREGA->"Nuevo intento de entrega a domicilio iniciado.";});confirmar(in.confirmado());String estado=logistica(p),bloqueo=bloqueoMovimiento(p,estado,in.accion(),true);if(bloqueo!=null)throw conflicto(bloqueo);
  String destino=switch(in.accion()){case PREPARAR_ENVIO->"PREPARADO";case SALIR_REPARTO->"EN_VIAJE";case LLEGAR_PUNTO->"DISPONIBLE_PUNTO";case CLIENTE_AUSENTE->"CLIENTE_AUSENTE";case REINTENTAR_ENTREGA->"EN_VIAJE";};String mensaje=switch(in.accion()){case PREPARAR_ENVIO->"El pedido está preparado para salir hacia el destino acordado.";case SALIR_REPARTO->"El pedido salió de la sucursal y está en viaje hacia el destino acordado.";case LLEGAR_PUNTO->"El pedido llegó al destino de retiro acordado. Consultá su franja de atención para retirarlo. Todavía no fue entregado al cliente.";case CLIENTE_AUSENTE->"No encontramos a una persona para recibir el pedido en el domicilio. La entrega sigue pendiente; coordiná un nuevo intento con la imprenta. Tu código se conserva.";case REINTENTAR_ENTREGA->"Comenzó un nuevo intento de entrega a domicilio. Conservá tu código para recibir el pedido.";};Instant ahora=clock.instant();
  long event=evento(p,a,in.operacion(),hash,in.accion().name(),p.estado(),motivo,mensaje,ahora);
  jdbc.update("INSERT INTO lamontana.movimiento_entrega(id_pedido,id_reserva_entrega,id_evento_pedido,estado_origen,estado_destino) VALUES (?,?,?,?,?)",p.numero(),reserva(p),event,estado,destino);
  if(in.accion()==Accion.SALIR_REPARTO)codigos.asegurar(p.numero());
  return vista(detalleEntrega(pedido,correo,rider,retiro),correo,true,retiro);
 }
 private PedidoService.Detalle detalleEntrega(UUID pedido,String correo,boolean rider,UUID retiro){
  if(retiro!=null)return pedidos.detalleRetiroSucursal(pedido,retiro,correo);
  if(rider)return pedidos.detalleReparto(pedido,correo);
  var detalle=pedidos.detalle(pedido,correo,true);
  organizacion.exigirPermiso(correo,"GESTIONAR_ENTREGAS");
  return detalle;
 }
 @Transactional public void paradaRider(UUID pedido,Movimiento in,String correo){
  bloquear();
  var p=pedidos.detalleReparto(pedido,correo);
  boolean punto=p.reserva().modalidad().name().equals("RETIRO_PUNTO_ENTREGA");
  if(in.accion()==Accion.LLEGAR_PUNTO&&punto){
   confirmar(in.confirmado());
   presentarPedidos(null,List.of(new PedidoLote(pedido,in.version())),correo);
   return;
  }
  if(!List.of(Accion.LLEGAR_PUNTO,Accion.CLIENTE_AUSENTE,Accion.REINTENTAR_ENTREGA).contains(in.accion()))
   throw error(HttpStatus.FORBIDDEN,"Esta acción no está disponible durante el reparto.");
  registrarMovimiento(pedido,in,correo,true,null);
 }
 @Transactional public void presentarEnPunto(UUID recorrido,List<PedidoLote> items,String correo){
  bloquear();
  presentarPedidos(recorrido,items,correo);
 }
 private record Presentacion(long pedido,UUID punto,long version,boolean presentada,boolean enViaje){}
 private void presentarPedidos(UUID recorrido,List<PedidoLote> items,String correo){
  if(items.isEmpty()||items.size()>25||items.stream().map(PedidoLote::pedido).distinct().count()!=items.size())
   throw error(HttpStatus.BAD_REQUEST,"Seleccioná entre 1 y 25 pedidos sin repetir.");
  UUID destino=null;
  var pendientes=new ArrayList<Long>();
  for(var item:items){
   var orden=jdbc.query("""
    SELECT p.id_pedido,r.destino_publico,p.version,rp.presentado_en IS NOT NULL,
           rr.estado='EN_REPARTO' AND r.estado='ACTIVA' AND p.estado='LISTO_PARA_ENTREGA'
             AND lamontana.estado_logistico(p.id_pedido)='EN_VIAJE'
    FROM lamontana.recorrido_reparto_pedido rp
    JOIN lamontana.recorrido_reparto rr USING(id_recorrido)
    JOIN lamontana.pedido p USING(id_pedido)
    JOIN lamontana.cuenta_repartidor cr ON cr.id_usuario=rr.id_repartidor
    JOIN lamontana.usuario u ON u.id_usuario=cr.id_usuario
    JOIN LATERAL (SELECT modalidad,destino_publico,estado FROM lamontana.reserva_entrega
                  WHERE id_pedido=p.id_pedido ORDER BY id_reserva_entrega DESC LIMIT 1) r ON true
    WHERE p.codigo_publico=? AND (?::uuid IS NULL OR rr.codigo_publico=?)
      AND lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND cr.activa
      AND r.modalidad='RETIRO_PUNTO_ENTREGA'
    """,(r,n)->new Presentacion(r.getLong(1),r.getObject(2,UUID.class),r.getLong(3),r.getBoolean(4),r.getBoolean(5)),
    item.pedido(),recorrido,recorrido,correo);
   if(orden.isEmpty())throw error(HttpStatus.FORBIDDEN,"El pedido no pertenece a esta parada de tu recorrido.");
   var p=orden.get(0);
   if(destino==null)destino=p.punto();
   if(!destino.equals(p.punto()))throw conflicto("Seleccioná pedidos del mismo punto de retiro.");
   if(p.presentada())continue;
   if(!p.enViaje()||p.version()!=item.version())
    throw conflicto("PED-"+p.pedido()+": el pedido cambió o ya no está en viaje. Actualizá el recorrido.");
   pendientes.add(p.pedido());
  }
  for(long pedido:pendientes)jdbc.update(
   "UPDATE lamontana.recorrido_reparto_pedido SET presentado_en=now() WHERE id_pedido=? AND presentado_en IS NULL",pedido);
 }
 /** All selected orders are checked before the first write; retries recover the same audited group. */
 @Transactional public ResultadoLote moverLote(MovimientoLote in,String correo){
  bloquear();organizacion.exigirPermiso(correo,"GESTIONAR_ENTREGAS");var a=actor(correo);
  if(in.accion()!=Accion.PREPARAR_ENVIO&&in.accion()!=Accion.SALIR_REPARTO)throw error(HttpStatus.BAD_REQUEST,"El grupo sólo permite preparar pedidos o confirmar su salida.");
  if(in.pedidos()==null||in.pedidos().isEmpty()||in.pedidos().size()>25||in.pedidos().stream().map(PedidoLote::pedido).distinct().count()!=in.pedidos().size())throw error(HttpStatus.BAD_REQUEST,"Seleccioná entre 1 y 25 pedidos sin repetir.");
  if(in.accion()==Accion.SALIR_REPARTO&&in.repartidor()==null)throw error(HttpStatus.BAD_REQUEST,"Elegí el rider que recibe físicamente estos pedidos.");
  if(in.accion()==Accion.PREPARAR_ENVIO&&in.repartidor()!=null)throw error(HttpStatus.BAD_REQUEST,"La preparación todavía no asigna un rider.");
  var ordered=in.pedidos().stream().sorted(Comparator.comparing(PedidoLote::pedido)).toList();String hash=huella(Arrays.asList(in.accion(),ordered,in.repartidor()));
  var saved=jdbc.query("SELECT l.id_actor,l.huella,l.fecha,r.codigo_publico,u.codigo_publico,concat_ws(' ',u.nombre,u.apellido),u.correo FROM lamontana.lote_movimiento_entrega l LEFT JOIN lamontana.recorrido_reparto r ON r.id_recorrido=l.id_recorrido LEFT JOIN lamontana.usuario u ON u.id_usuario=r.id_repartidor WHERE l.id_operacion=?",(r,n)->{
   if(r.getLong(1)!=a.id()||!hash.equals(r.getString(2)))throw conflicto("El grupo ya fue utilizado con otros datos.");UUID riderCode=r.getObject(5,UUID.class);return new ResultadoLote(in.operacion(),in.accion(),ordered.size(),r.getTimestamp(3).toInstant(),r.getObject(4,UUID.class),riderCode==null?null:new Repartidor(riderCode,r.getString(6),r.getString(7)));
  },in.operacion());if(!saved.isEmpty())return saved.get(0);
  Repartidor rider=in.repartidor()==null?null:repartidorActivo(in.repartidor());
  for(var item:ordered){
   var p=pedidos.detalle(item.pedido(),correo,true);version(p,item.version());
   exigirDespacho(p,in.accion(),correo,a);
   if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT lamontana.calidad_pedido_aprobada(?)",Boolean.class,p.numero())))throw conflicto("PED-"+p.numero()+": falta aprobar la calidad de todos sus trabajos. Actualizá la lista.");
   String blocked=bloqueoMovimiento(p,logistica(p),in.accion(),true);if(blocked!=null)throw conflicto("PED-"+p.numero()+": "+blocked+" No se modificó ningún pedido del grupo.");
  }
  Instant now=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);Long routeId=null;UUID routeCode=null;
  if(rider!=null){routeCode=UUID.randomUUID();routeId=jdbc.queryForObject("INSERT INTO lamontana.recorrido_reparto(codigo_publico,id_operacion,id_repartidor,id_actor_asignador,actor_asignador_nombre,estado,fecha_asignacion) VALUES (?, ?, (SELECT id_usuario FROM lamontana.usuario WHERE codigo_publico=?), ?, ?, 'EN_REPARTO', ?) RETURNING id_recorrido",Long.class,routeCode,in.operacion(),rider.codigoPublico(),a.id(),a.nombre(),Timestamp.from(now));}
  int order=0;
  for(var item:ordered){UUID operation=UUID.nameUUIDFromBytes((in.operacion()+":"+item.pedido()).getBytes(StandardCharsets.UTF_8));
   registrarMovimiento(item.pedido(),new Movimiento(operation,item.version(),in.accion(),(in.accion()==Accion.SALIR_REPARTO?"Salida conjunta confirmada":"Preparación conjunta confirmada")+" · grupo "+in.operacion(),true),correo,false,null);
   if(routeId!=null)jdbc.update("INSERT INTO lamontana.recorrido_reparto_pedido(id_recorrido,id_pedido,orden) VALUES (?,(SELECT id_pedido FROM lamontana.pedido WHERE codigo_publico=?),?)",routeId,item.pedido(),++order);
  }
  if(routeId!=null)jdbc.update("INSERT INTO lamontana.evento_recorrido_reparto(id_recorrido,id_actor,actor_nombre,accion,id_repartidor,fecha) VALUES (?,?,?,'ASIGNAR_E_INICIAR',(SELECT id_usuario FROM lamontana.usuario WHERE codigo_publico=?),?)",routeId,a.id(),a.nombre(),rider.codigoPublico(),Timestamp.from(now));
  jdbc.update("INSERT INTO lamontana.lote_movimiento_entrega(id_operacion,id_actor,actor_nombre,huella,accion,pedidos,fecha,id_recorrido) VALUES (?,?,?,?,?,?::jsonb,?,?)",in.operacion(),a.id(),a.nombre(),hash,in.accion().name(),json.writeValueAsString(ordered),Timestamp.from(now),routeId);
  return new ResultadoLote(in.operacion(),in.accion(),ordered.size(),now,routeCode,rider);
 }
 private void exigirDespacho(PedidoService.Detalle pedido,Accion accion,String correo,Actor actor){
  String motivo=bloqueoDespacho(pedido,accion,correo,actor);
  if(motivo!=null)throw error(HttpStatus.FORBIDDEN,motivo);
 }
 private String bloqueoDespacho(PedidoService.Detalle pedido,Accion accion,String correo,Actor actor){
  if(accion!=Accion.PREPARAR_ENVIO&&accion!=Accion.SALIR_REPARTO)return null;
  if(actor.rol().equals("ADMIN_ADMIN"))return null;
  if(CuentaPuntoAcceso.punto(jdbc,correo)!=null)return "El despacho se registra desde la sucursal de origen.";
  if(pedido.reserva().modalidad().name().equals("RETIRO_PUNTO_ENTREGA")&&!pedidos.rutaDespachoHabilitada(pedido.numero()))
   return "El punto de entrega no está asignado a la sucursal de origen de este pedido.";
  return null;
 }
 private Repartidor repartidorActivo(UUID codigo){return jdbc.query("SELECT u.codigo_publico,concat_ws(' ',u.nombre,u.apellido),u.correo FROM lamontana.cuenta_repartidor cr JOIN lamontana.usuario u USING(id_usuario) JOIN lamontana.rol r USING(id_rol) WHERE u.codigo_publico=? AND cr.activa AND u.estado='ACTIVO' AND r.activo AND r.codigo IN ('EMPLEADO','ADMIN_ADMIN')",(r,n)->new Repartidor(r.getObject(1,UUID.class),r.getString(2),r.getString(3)),codigo).stream().findFirst().orElseThrow(()->conflicto("El rider elegido ya no está activo. Actualizá la lista."));}
 @Transactional public Validacion validar(UUID pedido,Validar in,String correo){return validar(pedido,in,correo,huella(List.of(pedido,"VALIDAR",in)),false,null);}
 private Validacion validar(UUID pedido,Validar in,String correo,String hash,boolean rider,UUID retiro){
  bloquear();var p=detalleEntrega(pedido,correo,rider,retiro);var a=actor(correo);
  if(recuperar(in.operacion(),hash,p,a)){
   boolean valid=jdbc.queryForObject("SELECT resultado='VALIDO' FROM lamontana.validacion_codigo_entrega WHERE id_operacion=?",Boolean.class,in.operacion());var view=vista(p,correo,true,retiro);return new Validacion(view,valid&&view.validacion()!=null,valid?"La validación está registrada; consultá si continúa vigente para entregar.":"El intento anterior no validó el código. No se volvió a descontar un intento.");
  }
  exigirResponsablePunto(p,correo);
  version(p,in.version());if(!disponible(p))throw conflicto("El pedido todavía no está disponible para entrega presencial según su modalidad.");var c=ultimoCodigo(p.numero());if(c==null||c.revocado()!=null||c.utilizado()!=null||c.reserva()!=reserva(p))throw conflicto("El cliente debe generar un código vigente desde su pedido.");
  Instant now=clock.instant();String resultado=!now.isBefore(c.vence())?"VENCIDO":c.intentos()>=5?"BLOQUEADO":encoder.matches(in.codigo(),c.hash())?"VALIDO":"INVALIDO";
  if(resultado.equals("INVALIDO"))jdbc.update("UPDATE lamontana.codigo_entrega SET intentos=intentos+1 WHERE id_codigo_entrega=?",c.id());
  Instant hasta=now.plusSeconds(600).isBefore(c.vence())?now.plusSeconds(600):c.vence();
  jdbc.update("INSERT INTO lamontana.validacion_codigo_entrega(codigo_publico,id_codigo_entrega,id_actor,actor_nombre,id_operacion,huella,version_pedido,resultado,fecha,valido_hasta) VALUES (?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),c.id(),a.id(),a.nombre(),in.operacion(),hash,p.version(),resultado,Timestamp.from(now),resultado.equals("VALIDO")?Timestamp.from(hasta):null);
  return new Validacion(vista(p,correo,true,retiro),resultado.equals("VALIDO"),switch(resultado){case "VALIDO"->"Código comprobado para registrar la entrega.";case "INVALIDO"->"Código incorrecto. Comprobá el que presenta la persona.";case "BLOQUEADO"->"Se agotaron los intentos. Solicitá asistencia a la imprenta.";default->"El código ya no está vigente. Consultá a la imprenta.";});
 }
 @Transactional public Vista entregar(UUID pedido,Entregar in,String correo){
  return registrarEntrega(pedido,in,correo,huella(List.of(pedido,"ENTREGAR",in)),false,null);
 }
 @Transactional public Validacion recibir(UUID pedido,Recibir in,String correo){
  return recibir(pedido,in,correo,false,null);
 }
 @Transactional public Validacion recibirRider(UUID pedido,Recibir in,String correo){
  return recibir(pedido,in,correo,true,null);
 }
 @Transactional public Validacion entregarRetiro(UUID sucursal,UUID pedido,Recibir in,String correo){
  return recibir(pedido,in,correo,false,sucursal);
 }
 private Validacion recibir(UUID pedido,Recibir in,String correo,boolean rider,UUID retiro){
  bloquear();var p=detalleEntrega(pedido,correo,rider,retiro);var a=actor(correo);String hash=huella(List.of(pedido,"RECIBIR",in));
  if(rider&&!p.reserva().modalidad().name().equals("ENVIO_DOMICILIO"))
   throw error(HttpStatus.FORBIDDEN,"La entrega con código del rider es sólo a domicilio.");
  if(recuperar(in.operacion(),hash,p,a))return new Validacion(vista(p,correo,true,retiro),true,"Entrega ya registrada; no se duplicó.");
  exigirResponsablePunto(p,correo);
  version(p,in.version());confirmar(in.confirmado());nota(in.motivo(),"Entrega física confirmada con el código presentado.");var blocks=bloqueosEntrega(p);if(!blocks.isEmpty())throw conflicto(String.join(" ",blocks));
  UUID op=UUID.nameUUIDFromBytes((in.operacion()+":codigo").getBytes(StandardCharsets.UTF_8));
  var validacion=new Validar(op,p.version(),codigoPresentado(p,in.codigo()));
  var check=validar(pedido,validacion,correo,huella(List.of(pedido,"VALIDAR",validacion)),rider,retiro);if(!check.aceptado())return check;
  String receptor=in.receptor();if(receptor==null||receptor.isBlank())receptor=receptorRegistrado(p);
  var out=registrarEntrega(pedido,new Entregar(in.operacion(),p.version(),check.vista().validacion().codigoPublico(),receptor,in.motivo(),true),correo,hash,rider,retiro);
  return new Validacion(out,true,"Pedido entregado. Se guardaron responsable, fecha y hora.");
 }
 /** Cash, handover and optional close are one transaction. Invalid code never records cash. */
 @Transactional public Validacion cobrarEntregar(UUID pedido,CobrarEntregar in,String correo){
  return cobrarEntregar(pedido,in,correo,null);
 }
 @Transactional public Validacion cobrarRetiro(UUID sucursal,UUID pedido,CobrarEntregar in,String correo){
  return cobrarEntregar(pedido,in,correo,sucursal);
 }
 private Validacion cobrarEntregar(UUID pedido,CobrarEntregar in,String correo,UUID retiro){
  bloquear();var p=detalleEntrega(pedido,correo,false,retiro);organizacion.exigirPermiso(correo,"REGISTRAR_COBRO");if(in.cerrar())organizacion.exigirPermiso(correo,"CERRAR_PEDIDOS");
  var a=actor(correo);String hash=huella(List.of(pedido,"COBRAR_ENTREGAR",in));
  if(recuperar(in.operacion(),hash,p,a))return new Validacion(vista(p,correo,true,retiro),true,"Cobro y entrega ya registrados; no se duplicó el dinero.");
  exigirResponsablePunto(p,correo);
  version(p,in.version());confirmar(in.confirmado());nota(in.motivo(),"Cobro en efectivo y entrega confirmados.");if(in.receptor()!=null&&!in.receptor().isBlank())texto(in.receptor(),140);
  var offer=cobroEfectivo(p,correo,organizacion.contexto(correo).permisos(),retiro);if(offer==null||!offer.habilitado())throw conflicto(offer==null?"El pedido no admite cobro en efectivo durante la entrega.":offer.bloqueo());
  if(new BigDecimal(in.importe()).compareTo(new BigDecimal(offer.importe()))!=0)throw conflicto("El saldo cambió. Actualizá antes de confirmar el efectivo recibido.");
  if(in.cerrar()&&!offer.puedeCerrar())throw conflicto("Hay movimientos financieros pendientes. Revisá cobros antes de cerrar.");
  var check=validar(pedido,new Validar(derivada(in.operacion(),"codigo"),p.version(),codigoPresentado(p,in.codigo())),correo,hash,false,retiro);if(!check.aceptado())return check;
  var cobro=new PagoController.Recibir(derivada(in.operacion(),"efectivo"),null,MedioPago.EFECTIVO,offer.importe(),"Entrega PED-"+p.numero()+" · "+in.operacion(),null,true,"Efectivo recibido al entregar el pedido PED-"+p.numero()+".",true);
  if(retiro==null)pagos.recibir(p.cotizacion(),cobro,correo);
  else pagos.recibirEfectivoRetiro(p.cotizacion(),cobro,correo,retiro);
  String receptor=in.receptor()==null||in.receptor().isBlank()?receptorRegistrado(p):in.receptor().strip();
  var out=registrarEntrega(pedido,new Entregar(in.operacion(),p.version(),check.vista().validacion().codigoPublico(),receptor,in.motivo(),true),correo,hash,false,retiro);
  if(in.cerrar())out=cerrar(pedido,new Cerrar(derivada(in.operacion(),"cerrar"),out.version(),null,true),correo,retiro);
  return new Validacion(out,true,in.cerrar()?"Efectivo registrado, pedido entregado y cerrado. El comprobante interno quedó disponible.":"Efectivo registrado y pedido entregado. El responsable de cierre puede completar el cierre administrativo.");
 }
 private String receptorRegistrado(PedidoService.Detalle p){
  return p.nombreInvitado()==null?"Persona que presentó el código de entrega":p.nombreInvitado();
 }
 private String codigoPresentado(PedidoService.Detalle p,String codigo){
  if(codigo!=null)return codigo;
  if(p.nombreInvitado()==null||!logistica(p).equals("LISTO_RETIRO"))
   throw error(HttpStatus.BAD_REQUEST,"Ingresá el código de entrega del pedido.");
  codigos.asegurar(p.numero());
  var actual=ultimoCodigo(p.numero());
  if(actual==null||!estadoCodigo(actual,clock.instant()).equals("ACTIVO"))
   throw conflicto("No pudimos verificar la entrega en mostrador. Actualizá el pedido.");
  String cifrado=jdbc.queryForObject("SELECT codigo_cifrado FROM lamontana.codigo_entrega WHERE id_codigo_entrega=?",String.class,actual.id());
  if(cifrado==null)throw conflicto("No pudimos verificar el código de mostrador.");
  return codigos.texto(actual.codigo(),cifrado);
 }
 private UUID derivada(UUID operation,String step){return UUID.nameUUIDFromBytes((operation+":"+step).getBytes(StandardCharsets.UTF_8));}
 private CobroEfectivo cobroEfectivo(PedidoService.Detalle p,String correo,List<String> permisos,UUID retiro){
  BigDecimal saldo=new BigDecimal(p.oferta().total()).subtract(aplicado(p));
  if(!disponible(p)||p.oferta().medioPago()!=MedioPago.EFECTIVO||saldo.signum()<=0)return null;
  String bloqueo=null;
  if(!permisos.contains("GESTIONAR_ENTREGAS")||!permisos.contains("REGISTRAR_COBRO"))bloqueo="Necesitás permiso de cobros y de entregas para confirmar ambas acciones.";
  else try{bloqueo=retiro==null?pagos.bloqueoSaldoEfectivo(p.cotizacion(),correo):pagos.bloqueoSaldoEfectivo(p.cotizacion(),correo,retiro);}catch(ResponseStatusException e){if(e.getStatusCode()!=HttpStatus.FORBIDDEN)throw e;bloqueo="Tu cuenta no tiene acceso a los cobros de esta sucursal.";}
  if(bloqueo==null&&jdbc.queryForObject("SELECT lamontana.dinero_pendiente_cierre(?)",BigDecimal.class,p.numero()).signum()>0)bloqueo="Hay dinero recibido sin aplicar. Revisá cobros para evitar cobrarlo otra vez.";
  boolean cerrar=permisos.contains("CERRAR_PEDIDOS")&&jdbc.queryForObject("SELECT lamontana.informes_pendientes_cierre(?)",Long.class,p.numero())==0;
  return new CobroEfectivo(dinero(saldo),bloqueo==null,cerrar,bloqueo);
 }
 private ReciboEfectivo reciboEfectivo(PedidoService.Detalle p){
  var roots=jdbc.query("SELECT id_operacion FROM lamontana.historial_estado_pedido WHERE id_pedido=? AND accion='ENTREGAR' ORDER BY id_evento DESC LIMIT 1",(r,n)->r.getObject(1,UUID.class),p.numero());if(roots.isEmpty())return null;
  var receipts=jdbc.query("SELECT pa.codigo_publico,pa.importe,pa.referencia,e.actor_nombre,pa.recibido_en FROM lamontana.pago pa JOIN lamontana.evento_financiero e ON e.id_evento=pa.id_evento WHERE e.id_operacion=? AND pa.medio='EFECTIVO' AND pa.id_cotizacion=(SELECT id_cotizacion FROM lamontana.pedido_actual WHERE id_pedido=?)",(r,n)->new ReciboEfectivo(r.getObject(1,UUID.class),r.getBigDecimal(2).toPlainString(),r.getString(3),r.getString(4),r.getTimestamp(5).toInstant()),derivada(roots.get(0),"efectivo"),p.numero());
  return receipts.isEmpty()?null:receipts.get(0);
 }
 private Vista registrarEntrega(UUID pedido,Entregar in,String correo,String hash,boolean rider,UUID retiro){
  bloquear();var p=detalleEntrega(pedido,correo,rider,retiro);var a=actor(correo);
  if(recuperar(in.operacion(),hash,p,a))return vista(p,correo,true,retiro);
  exigirResponsablePunto(p,correo);
  version(p,in.version());texto(in.receptor(),140);String motivo=nota(in.motivo(),"Entrega física confirmada con el código presentado.");confirmar(in.confirmado());var b=bloqueosEntrega(p);if(!b.isEmpty())throw conflicto(String.join(" ",b));Instant now=clock.instant();var code=ultimoCodigo(p.numero());var validation=verificacion(p,code,a.id(),now);
  if(validation==null||!validation.codigoPublico().equals(in.validacion()))throw conflicto("Validá nuevamente el código presentado por el cliente con tu propia sesión.");
  long event=evento(p,a,in.operacion(),hash,"ENTREGAR","ENTREGADO",motivo,"El pedido completo fue entregado a "+in.receptor().strip()+". El cierre administrativo se verifica por separado.",now);
  jdbc.update("UPDATE lamontana.reserva_entrega SET estado='CUMPLIDA' WHERE id_pedido=? AND estado='ACTIVA'",p.numero());
  jdbc.update("UPDATE lamontana.codigo_entrega SET utilizado_en=? WHERE id_codigo_entrega=?",Timestamp.from(now),code.id());
  jdbc.update("INSERT INTO lamontana.entrega(codigo_publico,id_pedido,id_reserva_entrega,id_evento_pedido,id_validacion_codigo,receptor_nombre,nota,fecha_entrega) SELECT ?,?,?,?,?,?,?,?",UUID.randomUUID(),p.numero(),code.reserva(),event,jdbc.queryForObject("SELECT id_validacion_codigo FROM lamontana.validacion_codigo_entrega WHERE codigo_publico=?",Long.class,validation.codigoPublico()),in.receptor().strip(),motivo,Timestamp.from(now));
  return vista(detalleEntrega(pedido,correo,rider,retiro),correo,true,retiro);
 }
 @Transactional public Vista cerrar(UUID pedido,Cerrar in,String correo){
  return cerrar(pedido,in,correo,null);
 }
 private Vista cerrar(UUID pedido,Cerrar in,String correo,UUID retiro){
  bloquear();var p=retiro==null?pedidos.detalle(pedido,correo,true):pedidos.detalleRetiroSucursal(pedido,retiro,correo);organizacion.exigirPermiso(correo,"CERRAR_PEDIDOS");var a=actor(correo);String hash=huella(List.of(pedido,"CERRAR",in));
  if(recuperar(in.operacion(),hash,p,a))return vista(p,correo,true,retiro);version(p,in.version());String motivo=nota(in.motivo(),"Pedido cerrado después de comprobar entrega y conciliación.");confirmar(in.confirmado());var b=bloqueosCierre(p);if(!b.isEmpty())throw conflicto(String.join(" ",b));Instant now=clock.instant();
  long event=evento(p,a,in.operacion(),hash,"CERRAR","CERRADO",motivo,"El pedido fue cerrado después de verificar su entrega y los movimientos financieros.",now);
  jdbc.update("INSERT INTO lamontana.cierre_pedido(id_pedido,id_evento_pedido,total,aplicado,fecha_cierre) VALUES (?,?,?,?,?)",p.numero(),event,new BigDecimal(p.oferta().total()),aplicado(p),Timestamp.from(now));
  var cerrado=retiro==null?pedidos.detalle(pedido,correo,true):pedidos.detalleRetiroSucursal(pedido,retiro,correo);
  return vista(cerrado,correo,true,retiro);
 }
 private String bloqueoResponsablePunto(PedidoService.Detalle p,String correo){
  if(p.reserva().modalidad()!=Modalidad.RETIRO_PUNTO_ENTREGA||CuentaPuntoAcceso.punto(jdbc,correo)!=null)return null;
  return "La entrega al cliente debe confirmarla el responsable de "+p.reserva().nombre()+" desde su cuenta del punto de retiro.";
 }
 private void exigirResponsablePunto(PedidoService.Detalle p,String correo){
  String bloqueo=bloqueoResponsablePunto(p,correo);
  if(bloqueo!=null)throw error(HttpStatus.FORBIDDEN,bloqueo);
 }
 private List<String> bloqueosEntrega(PedidoService.Detalle p){var b=new ArrayList<String>();if(!disponible(p))b.add("Falta completar calidad o el recorrido de entrega de esta modalidad.");if(!jdbc.queryForObject("SELECT lamontana.saldo_entrega_cubierto(?)",Boolean.class,p.numero()))b.add("Falta acreditar y aplicar el saldo y los anticipos por los medios aceptados antes de entregar.");return List.copyOf(b);}
 private List<String> bloqueosCierre(PedidoService.Detalle p){var b=new ArrayList<String>();if(!p.estado().equals("ENTREGADO"))b.add(p.estado().equals("CERRADO")?"El pedido ya está cerrado.":"Registrá primero la entrega física completa.");if(!jdbc.queryForObject("SELECT lamontana.saldo_entrega_cubierto(?)",Boolean.class,p.numero()))b.add("La situación financiera no cubre el total y las condiciones aceptadas.");if(jdbc.queryForObject("SELECT lamontana.dinero_pendiente_cierre(?)",BigDecimal.class,p.numero()).signum()>0)b.add("Hay dinero recibido sin aplicar. Registrá la devolución del excedente antes de cerrar.");if(jdbc.queryForObject("SELECT lamontana.informes_pendientes_cierre(?)",Long.class,p.numero())>0)b.add("Hay transferencias informadas pendientes de resolver. Verificá o descartá esos informes antes de cerrar.");return List.copyOf(b);}
 private boolean disponible(PedidoService.Detalle p){if(!p.estado().equals("LISTO_PARA_ENTREGA"))return false;String l=logistica(p);return switch(p.reserva().modalidad().name()){case "RETIRO_SUCURSAL"->l.equals(p.retiroConTraslado()?"DISPONIBLE_PUNTO":"LISTO_RETIRO");case "RETIRO_PUNTO_ENTREGA"->l.equals("DISPONIBLE_PUNTO");default->l.equals("EN_VIAJE");};}
 private String bloqueoMovimiento(PedidoService.Detalle p,String estado,Accion op,boolean permiso){if(!permiso)return "Se requiere permiso de gestión de entregas.";if(!p.estado().equals("LISTO_PARA_ENTREGA"))return "La logística comienza después de superar calidad y termina al entregar.";if(p.reserva().modalidad().name().equals("RETIRO_SUCURSAL")&&!p.retiroConTraslado())return "El retiro en la casa matriz no requiere despacho ni viaje.";return switch(op){case PREPARAR_ENVIO->estado.equals("PENDIENTE_PREPARACION")?null:"La preparación ya fue registrada.";case SALIR_REPARTO->estado.equals("PREPARADO")?null:"Registrá primero la preparación del envío.";case CLIENTE_AUSENTE->!p.reserva().modalidad().name().equals("ENVIO_DOMICILIO")?"La ausencia de receptor corresponde sólo al envío a domicilio.":estado.equals("EN_VIAJE")?null:"La ausencia se registra durante un intento de entrega en viaje.";case REINTENTAR_ENTREGA->!p.reserva().modalidad().name().equals("ENVIO_DOMICILIO")?"El reintento corresponde sólo al envío a domicilio.":estado.equals("CLIENTE_AUSENTE")?null:"Registrá primero el intento sin receptor.";case LLEGAR_PUNTO->!p.reserva().modalidad().name().equals("RETIRO_PUNTO_ENTREGA")&&!p.retiroConTraslado()?"La llegada corresponde al retiro en otra sucursal o punto de entrega.":estado.equals("EN_VIAJE")?null:"Registrá la salida hacia el punto antes de su llegada.";};}
 private String logistica(PedidoService.Detalle p){return jdbc.queryForObject("SELECT lamontana.estado_logistico(?)",String.class,p.numero());}
 private long reserva(PedidoService.Detalle p){return jdbc.queryForObject("SELECT id_reserva_entrega FROM lamontana.reserva_entrega WHERE id_pedido=? AND estado='ACTIVA'",Long.class,p.numero());}
 private BigDecimal aplicado(PedidoService.Detalle p){return cobertura.acreditado(jdbc.queryForObject("SELECT id_cotizacion FROM lamontana.pedido_actual WHERE id_pedido=?",Long.class,p.numero()),List.of(MedioPago.values()));}
 private Secreto ultimoCodigo(long pedido){var list=jdbc.query("SELECT * FROM lamontana.codigo_entrega WHERE id_pedido=? ORDER BY id_codigo_entrega DESC LIMIT 1",(r,n)->new Secreto(r.getLong("id_codigo_entrega"),r.getObject("codigo_publico",UUID.class),r.getString("codigo_hash"),r.getLong("id_reserva_entrega"),r.getTimestamp("emitido_en").toInstant(),r.getTimestamp("vence_en").toInstant(),r.getTimestamp("revocado_en")==null?null:r.getTimestamp("revocado_en").toInstant(),r.getTimestamp("utilizado_en")==null?null:r.getTimestamp("utilizado_en").toInstant(),r.getInt("intentos")),pedido);return list.isEmpty()?null:list.get(0);}
 private String estadoCodigo(Secreto c,Instant now){return c.utilizado()!=null?"UTILIZADO":c.revocado()!=null?"REVOCADO":!now.isBefore(c.vence())?"VENCIDO":c.intentos()>=5?"BLOQUEADO":"ACTIVO";}
 private Verificacion verificacion(PedidoService.Detalle p,Secreto c,long actor,Instant now){if(c==null||!estadoCodigo(c,now).equals("ACTIVO")||!p.estado().equals("LISTO_PARA_ENTREGA")||c.reserva()!=reserva(p))return null;var list=jdbc.query("SELECT codigo_publico,fecha,valido_hasta FROM lamontana.validacion_codigo_entrega WHERE id_codigo_entrega=? AND id_actor=? AND version_pedido=? AND resultado='VALIDO' AND valido_hasta>? ORDER BY id_validacion_codigo DESC LIMIT 1",(r,n)->new Verificacion(r.getObject(1,UUID.class),r.getTimestamp(2).toInstant(),r.getTimestamp(3).toInstant()),c.id(),actor,p.version(),Timestamp.from(now));return list.isEmpty()?null:list.get(0);}
 private long evento(PedidoService.Detalle p,Actor a,UUID op,String hash,String accion,String destino,String motivo,String mensaje,Instant ahora){jdbc.update("UPDATE lamontana.pedido SET estado=?,version=version+1,finalizada_en=CASE WHEN ?='CERRADO' THEN ? ELSE finalizada_en END WHERE id_pedido=?",destino,destino,Timestamp.from(ahora),p.numero());return jdbc.queryForObject("INSERT INTO lamontana.historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha,estado_origen,version_pedido,accion,mensaje_cliente,actor_nombre,actor_tipo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id_evento",Long.class,p.numero(),a.id(),op,hash,destino,motivo.strip(),Timestamp.from(ahora),p.estado(),p.version()+1,accion,mensaje,a.nombre(),a.rol());}
 private boolean recuperar(UUID op,String hash,PedidoService.Detalle p,Actor a){var rows=jdbc.query("SELECT id_pedido,id_actor,huella FROM lamontana.historial_estado_pedido WHERE id_operacion=? UNION ALL SELECT id_pedido,id_autor,huella FROM lamontana.observacion_interna WHERE id_operacion=? UNION ALL SELECT id_pedido,id_emisor,huella FROM lamontana.codigo_entrega WHERE id_operacion=? UNION ALL SELECT c.id_pedido,v.id_actor,v.huella FROM lamontana.validacion_codigo_entrega v JOIN lamontana.codigo_entrega c USING(id_codigo_entrega) WHERE v.id_operacion=?",(r,n)->new Object[]{r.getLong(1),r.getLong(2),r.getString(3)},op,op,op,op);if(rows.isEmpty())return false;var r=rows.get(0);if(rows.size()!=1||(long)r[0]!=p.numero()||(long)r[1]!=a.id()||!hash.equals(r[2]))throw conflicto("La operación ya fue utilizada con otros datos.");return true;}
 private Actor actor(String correo){return jdbc.queryForObject("SELECT u.id_usuario,u.nombre||' '||u.apellido,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.correo=? AND u.estado='ACTIVO' AND r.activo",(r,n)->new Actor(r.getLong(1),r.getString(2),r.getString(3)),correo);}
 private void bloquear(){for(long id:new long[]{764003,764001,764002,764004})jdbc.execute("SELECT pg_advisory_xact_lock("+id+")");}
 private void version(PedidoService.Detalle p,long v){if(p.version()!=v)throw conflicto("El pedido cambió. Actualizá su estado antes de continuar.");}
 private void texto(String v,int max){if(v==null||v.isBlank()||v.length()>max||v.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw error(HttpStatus.BAD_REQUEST,"Completá los datos sin caracteres de control.");}
 private String nota(String value,String fallback){if(value==null||value.isBlank())return fallback;texto(value,500);return value.strip();}
 private void confirmar(boolean v){if(!v)throw error(HttpStatus.BAD_REQUEST,"Confirmá el hecho que vas a registrar.");}
 private String dinero(BigDecimal v){return v.setScale(2).toPlainString();}
 private String huella(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private ResponseStatusException conflicto(String m){return error(HttpStatus.CONFLICT,m);}
 private ResponseStatusException error(HttpStatus s,String m){return new ResponseStatusException(s,m);}
}
