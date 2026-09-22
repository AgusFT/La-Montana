package ar.com.lamontana.pagos;

import static ar.com.lamontana.pagos.PagoController.*;
import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.organizacion.OrganizacionService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PagoService {
    private final JdbcTemplate jdbc;private final OrganizacionService organizacion;private final OfertaOperativaService operativa;
    private final EvaluadorFinanciero reglas;private final CoberturaPagos cobertura;
    private final JsonMapper json=JsonMapper.builder().build();private final Clock clock=Clock.systemUTC();
    public PagoService(JdbcTemplate jdbc,OrganizacionService organizacion,OfertaOperativaService operativa,EvaluadorFinanciero reglas,CoberturaPagos cobertura){this.jdbc=jdbc;this.organizacion=organizacion;this.operativa=operativa;this.reglas=reglas;this.cobertura=cobertura;}
    private record Actor(long id,String nombre,String rol){}
    private record Oferta(long id,UUID codigo,long cliente,UUID sucursal,boolean activa,String estado,Instant vence,Instant aceptada,CotizacionService.Oferta datos){}
    private record Fondos(long id,UUID codigo,long origen,MedioPago medio,BigDecimal importe,BigDecimal devuelto,BigDecimal aplicado,BigDecimal disponible){}
    private record Asignacion(long id,long cotizacion,UUID codigo,BigDecimal neto){}
    public record Intento(UUID codigoPublico,UUID cotizacion,String importe,String referencia,String estado,Instant fecha){}
    public record Aplicacion(UUID cotizacion,long numero,String importe,String vigente,Instant fecha,String motivo){}
    public record Devolucion(UUID codigoPublico,String importe,String medio,String referencia,Instant fecha,String motivo){}
    public record Pago(UUID codigoPublico,UUID cotizacionOrigen,long numeroOrigen,String sucursal,String medio,String importe,String referencia,Instant recibidoEn,
        String aplicado,String devuelto,String disponible,String maximoAplicable,long version,List<Aplicacion> aplicaciones,List<Devolucion> devoluciones,boolean puedeAplicar,boolean puedeDevolver){}
    public record OfertaRelacionada(UUID codigoPublico,long numero,String total,String estado,boolean aceptada,String sucursal){}
    public record Evento(String tipo,String actor,String rol,Instant fecha,String motivo){}
    public record Vista(UUID cotizacion,long numero,String cliente,String sucursal,String estado,boolean aceptada,String total,String aplicado,String pendiente,
        String anticipoOfertado,String coberturaAnticipoOfertado,String anticipoActual,String coberturaAnticipoActual,List<MedioPago> mediosAnticipo,
        boolean puedeInformar,List<MedioPago> mediosRecepcion,List<String> permisos,List<Intento> intentos,List<Pago> pagos,List<OfertaRelacionada> relacionadas,List<Evento> historial){}
    public record Fila(UUID cotizacion,long numero,String cliente,String estado,String total,String aplicado,long pendientes){}
    public record Bandeja(List<Fila> elementos,long total,int pagina){}

    public record AccesoComprobante(long cotizacion,long usuario,String actor,String rol,Long intento,Long pago,Long intentoVinculado,String tipo,boolean puedeCargar){}
    /** Acceso financiero al padre del documento; no aplica reglas de carga de trabajos. */
    @Transactional(readOnly=true)
    public AccesoComprobante accesoComprobante(UUID quote,UUID intento,UUID pago,String correo,boolean interno){
        exigir((intento==null)!=(pago==null),"Seleccioná un intento o un pago, nunca ambos.");
        var a=actor(correo,interno);var q=autorizar(quote,a,correo);Long idIntento=null,idPago=null,vinculado=null;MedioPago medio=MedioPago.TRANSFERENCIA;
        if(intento!=null){var ids=jdbc.query("SELECT id_intento_pago FROM lamontana.intento_pago WHERE codigo_publico=? AND id_cotizacion=?",(r,n)->r.getLong(1),intento,q.id());if(ids.isEmpty())throw error(HttpStatus.NOT_FOUND,"La transferencia informada no está disponible.");idIntento=ids.get(0);}
        else{var ids=jdbc.query("SELECT id_pago,medio,id_intento_pago FROM lamontana.pago WHERE codigo_publico=? AND id_cotizacion=?",(r,n)->new Object[]{r.getLong(1),r.getString(2),r.getObject(3,Long.class)},pago,q.id());if(ids.isEmpty())throw error(HttpStatus.NOT_FOUND,"El pago no está disponible.");idPago=(Long)ids.get(0)[0];medio=MedioPago.valueOf((String)ids.get(0)[1]);vinculado=(Long)ids.get(0)[2];}
        boolean cargar=interno?organizacion.contexto(correo).permisos().contains(permiso(medio)):medio==MedioPago.TRANSFERENCIA;
        return new AccesoComprobante(q.id(),a.id(),a.nombre(),a.rol(),idIntento,idPago,vinculado,medio==MedioPago.TRANSFERENCIA?"EVIDENCIA_TRANSFERENCIA":"RECIBO_INTERNO",cargar);
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Vista vista(UUID quote,String correo,boolean interno){var a=actor(correo,interno);var q=autorizar(quote,a,correo);return leer(q,a,correo,interno);}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Bandeja bandeja(UUID branch,int pagina,String correo){
        var a=actor(correo,true);permisoFinanciero(correo);organizacion.sucursalAutorizada(correo,branch);exigir(pagina>=0&&pagina<=100000,"Página inválida.");
        String from=" FROM lamontana.cotizacion c JOIN lamontana.sucursal s USING(id_sucursal) JOIN lamontana.usuario u ON u.id_usuario=c.id_usuario_creador WHERE s.codigo_publico=? AND c.aceptada_en IS NOT NULL";
        long count=jdbc.queryForObject("SELECT count(*)"+from,Long.class,branch);
        var list=jdbc.query("SELECT c.*,u.nombre,u.apellido"+from+" ORDER BY c.id_cotizacion DESC LIMIT 25 OFFSET ?",(r,n)->new Fila(r.getObject("codigo_publico",UUID.class),r.getLong("id_cotizacion"),r.getString("nombre")+" "+r.getString("apellido"),estado(r.getString("estado"),r.getTimestamp("vigente_hasta").toInstant()),r.getBigDecimal("total").toPlainString(),dinero(cobertura.acreditado(r.getLong("id_cotizacion"),List.of(MedioPago.values()))),jdbc.queryForObject("SELECT count(*) FROM lamontana.intento_pago i LEFT JOIN lamontana.resolucion_intento_pago r USING(id_intento_pago) WHERE i.id_cotizacion=? AND r.id_intento_pago IS NULL",Long.class,r.getLong("id_cotizacion"))),branch,(long)pagina*25);
        return new Bandeja(list,count,pagina);
    }
    @Transactional public Vista informar(UUID quote,Informar in,String correo){
        bloquear();var a=actor(correo,false);var q=autorizar(quote,a,correo);String hash=huella(quote,in);
        if(replay(in.operacion(),"INFORMAR",hash,a))return leer(q,a,correo,false);
        exigir(q.aceptada()!=null,"Aceptá la oferta antes de informar una transferencia.");exigir(admite(q,MedioPago.TRANSFERENCIA),"Esta cotización no admite transferencia.");
        BigDecimal importe=importe(in.importe());texto(in.referencia());texto(in.motivo());
        if(jdbc.queryForObject("SELECT count(*) FROM lamontana.intento_pago i LEFT JOIN lamontana.resolucion_intento_pago r USING(id_intento_pago) WHERE i.id_cotizacion=? AND r.id_intento_pago IS NULL",Integer.class,q.id())>=20)throw conflicto("Hay veinte transferencias pendientes de verificar. Revisá las existentes antes de informar otra.");
        long e=evento(in.operacion(),q.id(),a,"INFORMAR",hash,in.motivo());
        jdbc.update("INSERT INTO lamontana.intento_pago(codigo_publico,id_cotizacion,id_evento,importe_informado,referencia_informada) VALUES (?,?,?,?,?)",UUID.randomUUID(),q.id(),e,importe,in.referencia().strip());
        return leer(q,a,correo,false);
    }
    @Transactional public Vista descartar(UUID quote,Descartar in,String correo,boolean interno){
        bloquear();var a=actor(correo,interno);var q=autorizar(quote,a,correo);if(interno)organizacion.exigirPermiso(correo,"ACREDITAR_PAGO");String hash=huella(quote,in);
        if(replay(in.operacion(),"DESCARTAR",hash,a))return leer(q,a,correo,interno);long intento=intentoPendiente(q,in.intento());texto(in.motivo());
        long e=evento(in.operacion(),q.id(),a,"DESCARTAR",hash,in.motivo());jdbc.update("INSERT INTO lamontana.resolucion_intento_pago(id_intento_pago,id_evento,resultado) VALUES (?,?,'DESCARTADO')",intento,e);
        return leer(q,a,correo,interno);
    }
    @Transactional public Vista recibir(UUID quote,Recibir in,String correo){
        bloquear();var a=actor(correo,true);var q=autorizar(quote,a,correo);organizacion.exigirPermiso(correo,permiso(in.medio()));String hash=huella(quote,in);
        if(replay(in.operacion(),"RECIBIR",hash,a))return leer(q,a,correo,true);
        exigir(in.verificado(),"Confirmá que verificaste el dinero efectivamente recibido.");BigDecimal importe=importe(in.importe());texto(in.referencia());texto(in.motivo());fecha(in.recibidoEn());
        exigir(q.aceptada()!=null,"El dinero debe vincularse a una cotización aceptada por su cliente, incluso si ya venció.");
        exigir(in.intento()==null||in.medio()==MedioPago.TRANSFERENCIA,"Una transferencia informada no puede convertirse en efectivo.");
        // Un hecho real tardío se registra con sus condiciones históricas, sin activar por sí mismo producción.
        exigir(admite(q,in.medio()),"El medio no corresponde a esta oferta ni a la configuración vigente.");
        Long intento=in.intento()==null?null:intentoPendiente(q,in.intento());String clave=clave(q,in.medio(),in.referencia());
        if(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.pago WHERE clave_referencia=?)",Boolean.class,clave))throw conflicto("Ese movimiento o recibo ya fue registrado. Consultá el pago existente.");
        long e=evento(in.operacion(),q.id(),a,"RECIBIR",hash,in.motivo());
        if(intento!=null)jdbc.update("INSERT INTO lamontana.resolucion_intento_pago(id_intento_pago,id_evento,resultado) VALUES (?,?,'ACREDITADO')",intento,e);
        jdbc.update("INSERT INTO lamontana.pago(codigo_publico,id_cotizacion,id_intento_pago,id_evento,medio,importe,referencia,clave_referencia,recibido_en) VALUES (?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),q.id(),intento,e,in.medio().name(),importe,in.referencia().strip(),clave,Timestamp.from(in.recibidoEn()));
        return leer(q,a,correo,true);
    }
    @Transactional public Vista aplicar(UUID quote,Aplicar in,String correo){
        bloquear();var a=actor(correo,true);var q=autorizar(quote,a,correo);var p=fondos(in.pago());var origen=autorizar(codigo(p.origen()),a,correo);organizacion.exigirPermiso(correo,permiso(p.medio()));
        String hash=huella(quote,in);if(replay(in.operacion(),"APLICAR",hash,a))return leer(q,a,correo,true);version(p,in.version());
        exigir(q.cliente()==origen.cliente()&&descendiente(q.id(),p.origen()),"Sólo se puede aplicar a la misma cotización o a una sucesora de ese trabajo.");
        if(!vigente(q)||q.aceptada()==null||!q.activa())throw conflicto("La cotización destino debe estar vigente, aceptada y con sucursal activa.");
        BigDecimal cantidad=importe(in.importe());texto(in.motivo());var liberar=new ArrayList<Asignacion>();BigDecimal disponible=p.disponible();
        for(var asignacion:asignaciones(p.id()))if(asignacion.cotizacion()!=q.id()){
            var vieja=autorizar(asignacion.codigo(),a,correo);
            if(vigente(vieja)||vieja.estado().equals("CONFIRMADA")||!descendiente(q.id(),vieja.id()))throw conflicto("El pago todavía cubre una oferta vigente, un pedido o una cotización ajena a esta sucesión.");
            liberar.add(asignacion);disponible=disponible.add(asignacion.neto());
        }
        if(cantidad.compareTo(disponible)>0)throw conflicto("El importe supera el dinero disponible después de las devoluciones y aplicaciones.");
        BigDecimal pendiente=new BigDecimal(q.datos().total()).subtract(cobertura.acreditado(q.id(),List.of(MedioPago.values())));
        if(cantidad.compareTo(pendiente)>0)throw conflicto("El importe supera el saldo de esta cotización. El excedente debe conservarse o devolverse.");
        reservarAnticipo(q,p.medio(),cantidad);
        long e=evento(in.operacion(),q.id(),a,"APLICAR",hash,in.motivo());
        for(var old:liberar)jdbc.update("INSERT INTO lamontana.liberacion_aplicacion_pago(id_aplicacion,id_evento,importe) VALUES (?,?,?)",old.id(),e,old.neto());
        jdbc.update("INSERT INTO lamontana.aplicacion_pago_cotizacion(id_pago,id_cotizacion,id_evento,importe) VALUES (?,?,?,?)",p.id(),q.id(),e,cantidad);
        return leer(q,a,correo,true);
    }
    @Transactional public Vista devolver(UUID quote,Devolver in,String correo){
        bloquear();var a=actor(correo,true);var q=autorizar(quote,a,correo);var p=fondos(in.pago());var origen=autorizar(codigo(p.origen()),a,correo);organizacion.exigirPermiso(correo,"REGISTRAR_DEVOLUCION");
        exigir(q.cliente()==origen.cliente()&&descendiente(q.id(),p.origen()),"El pago no pertenece a esta cotización ni a sus predecesoras.");String hash=huella(quote,in);
        if(replay(in.operacion(),"DEVOLVER",hash,a))return leer(q,a,correo,true);version(p,in.version());
        exigir(in.verificado(),"Confirmá la devolución efectivamente realizada.");BigDecimal cantidad=importe(in.importe());texto(in.motivo());texto(in.referencia());fecha(in.devueltoEn());
        if(in.devueltoEn().isBefore(jdbc.queryForObject("SELECT recibido_en FROM lamontana.pago WHERE id_pago=?",Timestamp.class,p.id()).toInstant()))throw conflicto("La devolución no puede ser anterior al cobro.");
        if(cantidad.compareTo(p.importe().subtract(p.devuelto()))>0)throw conflicto("El importe supera el dinero que todavía puede devolverse.");
        String clave=clave(origen,in.medio(),in.referencia());if(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.reembolso WHERE clave_referencia=?)",Boolean.class,clave))throw conflicto("Esa devolución ya fue registrada.");
        var asignaciones=asignaciones(p.id());for(var asignacion:asignaciones)autorizar(asignacion.codigo(),a,correo);
        long e=evento(in.operacion(),q.id(),a,"DEVOLVER",hash,in.motivo());long id=jdbc.queryForObject("INSERT INTO lamontana.reembolso(codigo_publico,id_pago,id_evento,importe,medio,referencia,clave_referencia,devuelto_en) VALUES (?,?,?,?,?,?,?,?) RETURNING id_reembolso",Long.class,UUID.randomUUID(),p.id(),e,cantidad,in.medio().name(),in.referencia().strip(),clave,Timestamp.from(in.devueltoEn()));
        BigDecimal restar=cantidad.subtract(p.disponible()).max(BigDecimal.ZERO);
        for(var asignacion:asignaciones){if(restar.signum()==0)break;BigDecimal parte=restar.min(asignacion.neto());jdbc.update("INSERT INTO lamontana.reembolso_aplicacion(id_reembolso,id_aplicacion,importe) VALUES (?,?,?)",id,asignacion.id(),parte);restar=restar.subtract(parte);}
        return leer(q,a,correo,true);
    }
    private void reservarAnticipo(Oferta q,MedioPago medio,BigDecimal cantidad){
        var ofrecida=q.datos().condiciones();
        reservarMedios(q,medio,cantidad,ofrecida.pagoPrevioRequerido(),ofrecida.senaRequerida(),ofrecida.mediosAcreditacion());
        var b=operativa.leer().configuracion();if(b!=null){var actual=reglas.evaluar(b.modelo(),b.criterio(),b.pagos(),b.version(),new BigDecimal(q.datos().total()),q.datos().items().stream().mapToInt(i->i.precio().carillas()).sum());reservarMedios(q,medio,cantidad,actual.pagoPrevioRequerido(),actual.senaRequerida(),actual.mediosAcreditacion());}
    }
    private void reservarMedios(Oferta q,MedioPago medio,BigDecimal cantidad,String previo,String sena,List<MedioPago> admitidos){
        BigDecimal anticipo=new BigDecimal(previo).add(new BigDecimal(sena));if(anticipo.signum()==0||admitidos.contains(medio))return;
        var otros=Arrays.stream(MedioPago.values()).filter(m->!admitidos.contains(m)).toList();
        if(cobertura.acreditado(q.id(),otros).add(cantidad).compareTo(new BigDecimal(q.datos().total()).subtract(anticipo))>0)
            throw conflicto("Este medio sólo puede cubrir el saldo posterior al anticipo. Conservá o devolvé el excedente; el anticipo requiere los medios configurados.");
    }
    private Vista leer(Oferta q,Actor a,String correo,boolean interno){
        if(interno)permisoFinanciero(correo);var permisos=interno?organizacion.contexto(correo).permisos():List.<String>of();
        var asignado=cobertura.acreditado(q.id(),List.of(MedioPago.values()));var c=q.datos().condiciones();String anticipo=dinero(new BigDecimal(c.pagoPrevioRequerido()).add(new BigDecimal(c.senaRequerida())));
        var cfg=operativa.leer().configuracion();var actual=cfg==null?null:reglas.evaluar(cfg.modelo(),cfg.criterio(),cfg.pagos(),cfg.version(),new BigDecimal(q.datos().total()),q.datos().items().stream().mapToInt(i->i.precio().carillas()).sum());
        var intentos=jdbc.query("SELECT i.*,c.codigo_publico AS cotizacion,e.fecha,coalesce(r.resultado,'PENDIENTE') AS estado FROM lamontana.intento_pago i JOIN lamontana.cotizacion c USING(id_cotizacion) JOIN lamontana.evento_financiero e USING(id_evento) LEFT JOIN lamontana.resolucion_intento_pago r USING(id_intento_pago) WHERE i.id_cotizacion=? ORDER BY i.id_intento_pago DESC LIMIT 100",(r,n)->new Intento(r.getObject("codigo_publico",UUID.class),r.getObject("cotizacion",UUID.class),r.getBigDecimal("importe_informado").toPlainString(),r.getString("referencia_informada"),r.getString("estado"),r.getTimestamp("fecha").toInstant()),q.id());
        var pagos=new ArrayList<Pago>();var ids=jdbc.query("SELECT p.codigo_publico FROM lamontana.pago p WHERE lamontana.cotizacion_descendiente(?,p.id_cotizacion) ORDER BY p.id_pago",(r,n)->r.getObject(1,UUID.class),q.id());
        for(var id:ids){var p=fondos(id);var origen=oferta(codigo(p.origen()));if(interno&&!acceso(origen,a,correo))continue;
            var apps=jdbc.query("SELECT c.codigo_publico,c.id_cotizacion,a.importe,a.neto,e.fecha,e.motivo FROM lamontana.aplicacion_pago_neta a JOIN lamontana.cotizacion c USING(id_cotizacion) JOIN lamontana.evento_financiero e USING(id_evento) WHERE a.id_pago=? ORDER BY a.id_aplicacion",(r,n)->new Aplicacion(r.getObject(1,UUID.class),r.getLong(2),r.getBigDecimal(3).toPlainString(),r.getBigDecimal(4).toPlainString(),r.getTimestamp(5).toInstant(),r.getString(6)),p.id());
            var dev=jdbc.query("SELECT r.*,e.motivo FROM lamontana.reembolso r JOIN lamontana.evento_financiero e USING(id_evento) WHERE r.id_pago=? ORDER BY r.id_reembolso",(r,n)->new Devolucion(r.getObject("codigo_publico",UUID.class),r.getBigDecimal("importe").toPlainString(),r.getString("medio"),r.getString("referencia"),r.getTimestamp("devuelto_en").toInstant(),r.getString("motivo")),p.id());
            BigDecimal maximo=maximoAplicable(q,p,a,correo,interno);
            boolean puedeDevolver=interno&&permisos.contains("REGISTRAR_DEVOLUCION")&&p.importe().compareTo(p.devuelto())>0&&asignaciones(p.id()).stream().allMatch(x->acceso(oferta(x.codigo()),a,correo));
            var pago=jdbc.queryForObject("SELECT referencia,recibido_en FROM lamontana.pago WHERE id_pago=?",(r,n)->new Pago(p.codigo(),origen.codigo(),origen.id(),origen.datos().sucursal().nombre(),p.medio().name(),dinero(p.importe()),r.getString(1),r.getTimestamp(2).toInstant(),dinero(p.aplicado()),dinero(p.devuelto()),dinero(p.disponible()),dinero(maximo),version(p),apps.stream().filter(v->!interno||acceso(oferta(v.cotizacion()),a,correo)).toList(),dev,maximo.signum()>0,puedeDevolver),p.id());pagos.add(pago);
        }
        // Sólo predecesoras/sucesoras del trabajo, nunca una billetera de otros pedidos del cliente.
        var relacionadas=jdbc.query("SELECT codigo_publico FROM lamontana.cotizacion WHERE id_cotizacion<>? AND (lamontana.cotizacion_descendiente(id_cotizacion,?) OR lamontana.cotizacion_descendiente(?,id_cotizacion)) ORDER BY id_cotizacion",(r,n)->r.getObject(1,UUID.class),q.id(),q.id(),q.id()).stream().map(this::oferta).filter(o->!interno||acceso(o,a,correo)).map(o->new OfertaRelacionada(o.codigo(),o.id(),o.datos().total(),estado(o.estado(),o.vence()),o.aceptada()!=null,o.datos().sucursal().nombre())).toList();
        var eventos=jdbc.query("SELECT tipo,actor_nombre,actor_rol,fecha,motivo FROM lamontana.evento_financiero WHERE id_cotizacion=? ORDER BY id_evento DESC LIMIT 100",(r,n)->new Evento(r.getString(1),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),r.getString(5)),q.id());
        String cliente=jdbc.queryForObject("SELECT nombre||' '||apellido FROM lamontana.usuario WHERE id_usuario=?",String.class,q.cliente());
        return new Vista(q.codigo(),q.id(),cliente,q.datos().sucursal().nombre(),estado(q.estado(),q.vence()),q.aceptada()!=null,q.datos().total(),dinero(asignado),dinero(new BigDecimal(q.datos().total()).subtract(asignado)),anticipo,dinero(cobertura.acreditado(q.id(),c.mediosAcreditacion())),actual==null?null:dinero(new BigDecimal(actual.pagoPrevioRequerido()).add(new BigDecimal(actual.senaRequerida()))),actual==null?null:dinero(cobertura.acreditado(q.id(),actual.mediosAcreditacion())),actual==null?List.of():actual.mediosAcreditacion(),!interno&&q.aceptada()!=null&&admite(q,MedioPago.TRANSFERENCIA),Arrays.stream(MedioPago.values()).filter(m->admite(q,m)).toList(),permisos,intentos,pagos,relacionadas,eventos);
    }
    private BigDecimal maximoAplicable(Oferta q,Fondos p,Actor a,String correo,boolean interno){
        if(!interno||!organizacion.contexto(correo).permisos().contains(permiso(p.medio()))||!vigente(q)||q.aceptada()==null||!q.activa())return BigDecimal.ZERO;
        BigDecimal disponible=p.disponible();for(var x:asignaciones(p.id()))if(x.cotizacion()!=q.id()){
            var origen=oferta(x.codigo());if(!acceso(origen,a,correo)||vigente(origen)||origen.estado().equals("CONFIRMADA")||!descendiente(q.id(),origen.id()))return BigDecimal.ZERO;disponible=disponible.add(x.neto());
        }
        BigDecimal pendiente=new BigDecimal(q.datos().total()).subtract(cobertura.acreditado(q.id(),List.of(MedioPago.values())));
        var c=q.datos().condiciones();BigDecimal maximo=disponible.min(pendiente).min(topeMedios(q,p.medio(),c.pagoPrevioRequerido(),c.senaRequerida(),c.mediosAcreditacion()));
        var b=operativa.leer().configuracion();if(b!=null){var actual=reglas.evaluar(b.modelo(),b.criterio(),b.pagos(),b.version(),new BigDecimal(q.datos().total()),q.datos().items().stream().mapToInt(i->i.precio().carillas()).sum());maximo=maximo.min(topeMedios(q,p.medio(),actual.pagoPrevioRequerido(),actual.senaRequerida(),actual.mediosAcreditacion()));}
        return maximo.max(BigDecimal.ZERO);
    }
    private BigDecimal topeMedios(Oferta q,MedioPago medio,String previo,String sena,List<MedioPago> admitidos){
        BigDecimal total=new BigDecimal(q.datos().total()),anticipo=new BigDecimal(previo).add(new BigDecimal(sena));if(anticipo.signum()==0||admitidos.contains(medio))return total;
        return total.subtract(anticipo).subtract(cobertura.acreditado(q.id(),Arrays.stream(MedioPago.values()).filter(m->!admitidos.contains(m)).toList())).max(BigDecimal.ZERO);
    }
    private Actor actor(String correo,boolean interno){var rows=jdbc.query("SELECT u.id_usuario,u.nombre||' '||u.apellido,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.activo",(r,n)->new Actor(r.getLong(1),r.getString(2),r.getString(3)),correo);if(rows.isEmpty())throw error(HttpStatus.FORBIDDEN,"La cuenta no está activa.");var a=rows.get(0);if(interno?!List.of("ADMIN_ADMIN","EMPLEADO").contains(a.rol()):!a.rol().equals("CLIENTE"))throw error(HttpStatus.FORBIDDEN,"La cuenta no tiene acceso a esta operación.");return a;}
    private Oferta autorizar(UUID quote,Actor a,String correo){var q=oferta(quote);if(a.rol().equals("CLIENTE")){if(q.cliente()!=a.id())throw error(HttpStatus.NOT_FOUND,"La cotización no está disponible.");}else{permisoFinanciero(correo);if(!acceso(q,a,correo))throw error(HttpStatus.FORBIDDEN,"No tenés acceso a las finanzas de esta sucursal.");}return q;}
    private boolean acceso(Oferta q,Actor a,String correo){return a.rol().equals("ADMIN_ADMIN")||organizacion.contexto(correo).sucursales().stream().anyMatch(s->s.codigoPublico().equals(q.sucursal()));}
    private void permisoFinanciero(String correo){if(organizacion.contexto(correo).permisos().stream().noneMatch(p->List.of("ACREDITAR_PAGO","REGISTRAR_COBRO","REGISTRAR_DEVOLUCION").contains(p)))throw error(HttpStatus.FORBIDDEN,"Se requiere un permiso financiero explícito.");}
    private Oferta oferta(UUID quote){var rows=jdbc.query("SELECT c.*,s.codigo_publico AS sucursal,s.estado AS sucursal_estado FROM lamontana.cotizacion c JOIN lamontana.sucursal s USING(id_sucursal) WHERE c.codigo_publico=?",(r,n)->new Oferta(r.getLong("id_cotizacion"),quote,r.getLong("id_usuario_creador"),r.getObject("sucursal",UUID.class),r.getString("sucursal_estado").equals("ACTIVA"),r.getString("estado"),r.getTimestamp("vigente_hasta").toInstant(),r.getTimestamp("aceptada_en")==null?null:r.getTimestamp("aceptada_en").toInstant(),json.readValue(r.getString("oferta"),CotizacionService.Oferta.class)),quote);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"La cotización no está disponible.");return rows.get(0);}
    private Fondos fondos(UUID pago){var rows=jdbc.query("SELECT * FROM lamontana.pago_disponible WHERE codigo_publico=?",(r,n)->new Fondos(r.getLong("id_pago"),pago,r.getLong("id_cotizacion"),MedioPago.valueOf(r.getString("medio")),r.getBigDecimal("importe"),r.getBigDecimal("devuelto"),r.getBigDecimal("aplicado"),r.getBigDecimal("disponible")),pago);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"El pago no está disponible.");return rows.get(0);}
    private List<Asignacion> asignaciones(long pago){return jdbc.query("SELECT a.id_aplicacion,a.id_cotizacion,c.codigo_publico,a.neto FROM lamontana.aplicacion_pago_neta a JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE a.id_pago=? AND a.neto>0 ORDER BY a.id_aplicacion DESC",(r,n)->new Asignacion(r.getLong(1),r.getLong(2),r.getObject(3,UUID.class),r.getBigDecimal(4)),pago);}
    private UUID codigo(long id){return jdbc.queryForObject("SELECT codigo_publico FROM lamontana.cotizacion WHERE id_cotizacion=?",UUID.class,id);}
    private boolean descendiente(long destino,long origen){return jdbc.queryForObject("SELECT lamontana.cotizacion_descendiente(?,?)",Boolean.class,destino,origen);}
    private long intentoPendiente(Oferta q,UUID id){var ids=jdbc.query("SELECT i.id_intento_pago FROM lamontana.intento_pago i LEFT JOIN lamontana.resolucion_intento_pago r USING(id_intento_pago) WHERE i.codigo_publico=? AND i.id_cotizacion=? AND r.id_intento_pago IS NULL",(r,n)->r.getLong(1),id,q.id());if(ids.isEmpty())throw conflicto("La transferencia no está pendiente en esta cotización. Consultá el resultado guardado.");return ids.get(0);}
    private long version(Fondos p){return jdbc.queryForObject("SELECT count(*) FROM (SELECT id_evento FROM lamontana.pago WHERE id_pago=? UNION SELECT id_evento FROM lamontana.aplicacion_pago_cotizacion WHERE id_pago=? UNION SELECT id_evento FROM lamontana.reembolso WHERE id_pago=?) e",Long.class,p.id(),p.id(),p.id());}
    private void version(Fondos p,long version){if(version(p)!=version)throw conflicto("El pago cambió. Actualizá sus aplicaciones y devoluciones antes de continuar.");}
    private boolean replay(UUID op,String tipo,String hash,Actor a){var rows=jdbc.query("SELECT tipo,huella,id_actor FROM lamontana.evento_financiero WHERE id_operacion=?",(r,n)->new Object[]{r.getString(1),r.getString(2),r.getLong(3)},op);if(rows.isEmpty())return false;var r=rows.get(0);if(!tipo.equals(r[0])||!hash.equals(r[1])||a.id()!=(long)r[2])throw conflicto("La operación ya fue utilizada con otros datos.");return true;}
    private long evento(UUID op,long quote,Actor a,String tipo,String hash,String motivo){return jdbc.queryForObject("INSERT INTO lamontana.evento_financiero(id_operacion,id_cotizacion,id_actor,actor_nombre,actor_rol,tipo,huella,motivo) VALUES (?,?,?,?,?,?,?,?) RETURNING id_evento",Long.class,op,quote,a.id(),a.nombre(),a.rol(),tipo,hash,motivo.strip());}
    private boolean admite(Oferta q,MedioPago medio){var b=operativa.leer().configuracion();return q.datos().medioPago()==medio||q.datos().mediosGenerales()!=null&&q.datos().mediosGenerales().contains(medio)||q.datos().condiciones().mediosAcreditacion().contains(medio)||b!=null&&b.pagos().medios().contains(medio);}
    private String clave(Oferta q,MedioPago medio,String ref){return medio.name()+":"+(medio==MedioPago.EFECTIVO?q.sucursal()+":":"")+huella(null,ref.strip().toUpperCase(Locale.ROOT).replaceAll("\\s+"," "));}
    private String permiso(MedioPago medio){return medio==MedioPago.EFECTIVO?"REGISTRAR_COBRO":"ACREDITAR_PAGO";}
    private boolean vigente(Oferta q){return q.estado().equals("VIGENTE")&&clock.instant().isBefore(q.vence());}
    private String estado(String estado,Instant vence){return estado.equals("VIGENTE")&&!clock.instant().isBefore(vence)?"EXPIRADA":estado;}
    private void fecha(Instant t){exigir(t!=null&&!t.isAfter(clock.instant()),"La fecha debe corresponder a un movimiento ya realizado.");}
    private BigDecimal importe(String valor){BigDecimal n=new BigDecimal(valor);exigir(n.signum()>0&&n.scale()<=2&&n.precision()-n.scale()<=17,"El importe debe ser positivo, con hasta dos decimales.");return n.setScale(2);}
    private String dinero(BigDecimal n){return n.setScale(2).toPlainString();}
    private void texto(String v){exigir(v!=null&&!v.isBlank()&&v.codePoints().noneMatch(Character::isISOControl),"Completá referencia y motivo sin caracteres de control.");}
    private String huella(UUID quote,Object in){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((quote+":"+json.writeValueAsString(in)).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
    private void bloquear(){jdbc.execute("SELECT pg_advisory_xact_lock(764003)");jdbc.execute("SELECT pg_advisory_xact_lock(764001)");jdbc.execute("SELECT pg_advisory_xact_lock(764002)");jdbc.execute("SELECT pg_advisory_xact_lock(764004)");}
    private void exigir(boolean ok,String msg){if(!ok)throw error(HttpStatus.BAD_REQUEST,msg);}
    private ResponseStatusException conflicto(String msg){return error(HttpStatus.CONFLICT,msg);}
    private ResponseStatusException error(HttpStatus status,String msg){return new ResponseStatusException(status,msg);}
}
